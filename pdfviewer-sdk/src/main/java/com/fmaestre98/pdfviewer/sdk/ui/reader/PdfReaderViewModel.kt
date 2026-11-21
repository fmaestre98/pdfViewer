package com.fmaestre98.pdfviewer.sdk.ui.reader

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fmaestre98.pdfviewer.sdk.R
import com.fmaestre98.pdfviewer.sdk.core.model.PdfViewerOrientation
import com.fmaestre98.pdfviewer.sdk.core.text.PdfSearchManager
import com.fmaestre98.pdfviewer.sdk.model.PdfViewerConfig
import com.fmaestre98.pdfviewer.sdk.storage.PdfViewerStorage
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

internal class PdfReaderViewModel(
    private val documentId: String,
    private val filePath: String?,
    private val config: PdfViewerConfig,
    private val storage: PdfViewerStorage
) : ViewModel() {

    private val _state = MutableStateFlow(
        PdfReaderState(
            uri = documentId,
            filePath = filePath,
            orientation = config.initialOrientation,
            enableAnnotations = config.enableAnnotations,
            enableBookmarks = config.enableBookmarks,
            enableSearch = config.enableSearch,
            enableThumbnailDrawer = config.enableThumbnailDrawer
        )
    )
    val state = _state.asStateFlow()

    private val _events = Channel<PdfReaderEvent>()
    val events = _events.receiveAsFlow()

    private val searchManager = PdfSearchManager()
    private var searchJob: Job? = null

    init {
        loadDocumentData()
    }

    private fun loadDocumentData() {
        viewModelScope.launch {
            try {
                val lastReadPage = storage.getLastReadPage(documentId) ?: 0
                val targetPage = config.initialPage ?: lastReadPage
                val bookmarkedPages = storage.getBookmarkedPages(documentId)
                val highlightsMap = storage.getHighlightsByPage(documentId)
                val note = storage.getPageNote(documentId, targetPage)

                _state.update {
                    it.copy(
                        isLoading = false,
                        initialPage = targetPage,
                        currentPage = targetPage,
                        bookmarkedPages = bookmarkedPages,
                        isCurrentPageBookmarked = bookmarkedPages.contains(targetPage),
                        highlights = highlightsMap,
                        currentPageNote = note,
                        error = null
                    )
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
                    storage.saveLastReadPage(documentId, action.pageIndex)
                    val isBookmarked = storage.isPageBookmarked(documentId, action.pageIndex)
                    val note = storage.getPageNote(documentId, action.pageIndex)
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
                    val newBookmarkedStatus = storage.toggleBookmark(documentId, currentPage)
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
                    storage.savePageNote(documentId, page, action.text)
                    val note = storage.getPageNote(documentId, page)
                    _state.update { it.copy(currentPageNote = note) }
                }
            }
            PdfReaderAction.DeletePageNote -> {
                val page = _state.value.currentPage
                viewModelScope.launch {
                    storage.deletePageNote(documentId, page)
                    _state.update { it.copy(currentPageNote = null) }
                }
            }
            is PdfReaderAction.SaveHighlight -> {
                if (action.normalizedRects.isEmpty()) return
                val groupId = UUID.randomUUID().toString()
                viewModelScope.launch {
                    storage.saveHighlight(
                        documentId = documentId,
                        page = action.page,
                        snippet = action.snippet,
                        color = action.color,
                        groupId = groupId,
                        rects = action.normalizedRects
                    )
                    val highlightsMap = storage.getHighlightsByPage(documentId)
                    _state.update { it.copy(highlights = highlightsMap) }
                }
            }
            PdfReaderAction.ToggleSearch -> {
                _state.update {
                    it.copy(
                        isSearchOpen = !it.isSearchOpen,
                        isFabExpanded = false,
                        isThumbnailDrawerOpen = false
                    )
                }
            }
            PdfReaderAction.CloseSearch -> {
                _state.update {
                    it.copy(
                        isSearchOpen = false,
                        searchQuery = "",
                        searchResults = emptyList(),
                        isSearching = false
                    )
                }
            }
            is PdfReaderAction.UpdateSearchQuery -> {
                _state.update {
                    it.copy(
                        searchQuery = action.query,
                        isSearching = action.query.isNotBlank(),
                        searchResults = emptyList()
                    )
                }
                searchJob?.cancel()
                if (action.query.isNotBlank()) {
                    searchJob = viewModelScope.launch {
                        delay(500)
                        val path = _state.value.filePath
                        if (path != null) {
                            searchManager.search(File(path), action.query)
                                .catch { e ->
                                    e.printStackTrace()
                                    _state.update { it.copy(isSearching = false) }
                                }
                                .collect { results ->
                                    if (results.isNotEmpty()) {
                                        _state.update {
                                            it.copy(searchResults = it.searchResults + results)
                                        }
                                    }
                                }
                            _state.update { it.copy(isSearching = false) }
                        } else {
                            _state.update { it.copy(isSearching = false) }
                        }
                    }
                }
            }
            is PdfReaderAction.SelectSearchResult -> {
                _state.update { it.copy(isSearchOpen = false) }
            }
            PdfReaderAction.ToggleOrientation -> {
                _state.update { state ->
                    val newOrientation = if (state.orientation == PdfViewerOrientation.Horizontal) {
                        PdfViewerOrientation.Vertical
                    } else {
                        PdfViewerOrientation.Horizontal
                    }
                    state.copy(
                        orientation = newOrientation,
                        isFabExpanded = false
                    )
                }
            }
        }
    }

    class Factory(
        private val documentId: String,
        private val filePath: String?,
        private val config: PdfViewerConfig,
        private val storage: PdfViewerStorage
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return PdfReaderViewModel(
                documentId = documentId,
                filePath = filePath,
                config = config,
                storage = storage
            ) as T
        }
    }
}
