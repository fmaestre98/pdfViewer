package com.fmaestre98.pdfviewer.ui.screens.reader

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fmaestre98.pdfviewer.R
import com.fmaestre98.pdfviewer.repository.PDFRepository
import com.fmaestre98.pdfviewer.repository.BookmarkRepository
import com.fmaestre98.pdfviewer.repository.HighlightRepository
import com.fmaestre98.pdfviewer.repository.PageNoteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class PdfReaderViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val pdfRepository: PDFRepository,
    private val bookmarkRepository: BookmarkRepository,
    private val highlightRepository: HighlightRepository,
    private val pageNoteRepository: PageNoteRepository
) : ViewModel() {

    private val encodedUri: String = checkNotNull(savedStateHandle["encodedUri"])
    private val uri: String = Uri.decode(encodedUri)

    private val _state = MutableStateFlow(PdfReaderState(uri = uri))
    val state = _state.asStateFlow()

    private val _events = Channel<PdfReaderEvent>()
    val events = _events.receiveAsFlow()

    init {
        loadBookData()
    }

    private fun loadBookData() {
        viewModelScope.launch {
            try {
                val book = pdfRepository.getBookByUri(uri)
                if (book != null) {
                    val bookmarks = bookmarkRepository.getAllBookmarksForBook(uri)
                    val bookmarkedPages = bookmarks.map { it.page }.toSet()
                    val highlightsMap = highlightRepository.getHighlightsGroupedByPage(uri)
                    val note = pageNoteRepository.getNoteForPage(uri, book.lastReadPage)

                    _state.update { 
                        it.copy(
                            isLoading = false,
                            initialPage = book.lastReadPage,
                            currentPage = book.lastReadPage,
                            filePath = book.filePath,
                            bookmarkedPages = bookmarkedPages,
                            isCurrentPageBookmarked = bookmarkedPages.contains(book.lastReadPage),
                            highlights = highlightsMap,
                            currentPageNote = note,
                            error = null
                        ) 
                    }
                } else {
                    _state.update { it.copy(isLoading = false, error = "Libro no encontrado en la biblioteca") }
                    _events.send(PdfReaderEvent.ShowSnackbar(R.string.reader_error_file))
                }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message) }
                _events.send(PdfReaderEvent.ShowSnackbar(R.string.reader_error_file))
            }
        }
    }

    fun onAction(action: PdfReaderAction) {
        when (action) {
            is PdfReaderAction.OnPageChanged -> {
                viewModelScope.launch {
                    pdfRepository.updateLastReadPage(uri, action.pageIndex)
                    val isBookmarked = bookmarkRepository.isPageBookmarked(uri, action.pageIndex)
                    val note = pageNoteRepository.getNoteForPage(uri, action.pageIndex)
                    _state.update {
                        it.copy(
                            currentPage = action.pageIndex,
                            isCurrentPageBookmarked = isBookmarked,
                            currentPageNote = note
                        )
                    }
                }
            }
            PdfReaderAction.OnBackClick -> {
                viewModelScope.launch {
                    _events.send(PdfReaderEvent.NavigateBack)
                }
            }
            PdfReaderAction.ToggleFab -> {
                _state.update { it.copy(isFabExpanded = !it.isFabExpanded) }
            }
            PdfReaderAction.CloseFab -> {
                _state.update { it.copy(isFabExpanded = false) }
            }
            PdfReaderAction.ToggleThumbnailDrawer -> {
                _state.update {
                    it.copy(
                        isThumbnailDrawerOpen = !it.isThumbnailDrawerOpen,
                        isFabExpanded = false
                    )
                }
            }
            PdfReaderAction.CloseThumbnailDrawer -> {
                _state.update { it.copy(isThumbnailDrawerOpen = false) }
            }
            is PdfReaderAction.UpdatePageCount -> {
                _state.update { it.copy(pageCount = action.count) }
            }
            PdfReaderAction.ToggleBookmark -> {
                val currentPage = _state.value.currentPage
                viewModelScope.launch {
                    val newBookmarkedStatus = bookmarkRepository.toggleBookmark(uri, currentPage)
                    _state.update { state ->
                        val newBookmarkedPages = if (newBookmarkedStatus) {
                            state.bookmarkedPages + currentPage
                        } else {
                            state.bookmarkedPages - currentPage
                        }
                        state.copy(
                            isCurrentPageBookmarked = newBookmarkedStatus,
                            bookmarkedPages = newBookmarkedPages
                        )
                    }
                }
            }
            is PdfReaderAction.SavePageNote -> {
                val page = _state.value.currentPage
                viewModelScope.launch {
                    pageNoteRepository.saveNote(uri, page, action.text)
                    val note = pageNoteRepository.getNoteForPage(uri, page)
                    _state.update { it.copy(currentPageNote = note) }
                }
            }
            PdfReaderAction.DeletePageNote -> {
                val page = _state.value.currentPage
                viewModelScope.launch {
                    pageNoteRepository.deleteNote(uri, page)
                    _state.update { it.copy(currentPageNote = null) }
                }
            }
            is PdfReaderAction.SaveHighlight -> {
                if (action.normalizedRects.isEmpty()) return
                val groupId = UUID.randomUUID().toString()
                viewModelScope.launch {
                    highlightRepository.saveHighlights(
                        bookUri = uri,
                        page = action.page,
                        snippet = action.snippet,
                        color = action.color,
                        groupId = groupId,
                        normalizedRects = action.normalizedRects
                    )
                    val highlightsMap = highlightRepository.getHighlightsGroupedByPage(uri)
                    _state.update { it.copy(highlights = highlightsMap) }
                }
            }
        }
    }
}
