package com.fmaestre98.pdfviewer.ui.screens.annotations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fmaestre98.pdfviewer.repository.PDFRepository
import com.fmaestre98.pdfviewer.room.BookmarkDao
import com.fmaestre98.pdfviewer.room.HighlightDao
import com.fmaestre98.pdfviewer.room.PageNoteDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AnnotationsViewModel @Inject constructor(
    pdfRepository: PDFRepository,
    bookmarkDao: BookmarkDao,
    highlightDao: HighlightDao,
    pageNoteDao: PageNoteDao
) : ViewModel() {

    private val _events = Channel<AnnotationsEvent>()
    val events = _events.receiveAsFlow()

    val state: StateFlow<AnnotationsState> = combine(
        pdfRepository.observeBooks(),
        bookmarkDao.getAllBookmarksFlow(),
        highlightDao.getAllHighlightsFlow(),
        pageNoteDao.getAllNotesFlow()
    ) { books, bookmarks, highlights, notes ->
        val booksMap = books.associateBy { it.uri }

        // Group highlights by bookUri & groupId (or single item)
        val highlightItems = highlights
            .groupBy { "${it.bookUri}_${it.groupId}" }
            .values
            .mapNotNull { group ->
                val first = group.firstOrNull() ?: return@mapNotNull null
                val combinedSnippet = group.map { it.snippet }.joinToString(" ")
                AnnotationItem.Highlight(
                    id = "h_${first.groupId}",
                    page = first.page,
                    timestamp = first.updatedAt,
                    snippet = combinedSnippet,
                    color = first.color
                ) to first.bookUri
            }

        val noteItems = notes.map { note ->
            AnnotationItem.PageNote(
                id = "n_${note.id}",
                page = note.page,
                timestamp = note.updatedAt,
                noteText = note.noteText,
                color = note.color
            ) to note.bookUri
        }

        val bookmarkItems = bookmarks.map { bookmark ->
            AnnotationItem.Bookmark(
                id = "b_${bookmark.id}",
                page = bookmark.page,
                timestamp = bookmark.createdAt
            ) to bookmark.bookUri
        }

        // Group all annotation items by bookUri
        val allAnnotationsByBook = (highlightItems + noteItems + bookmarkItems)
            .groupBy({ it.second }, { it.first })

        val bookGroups = allAnnotationsByBook.mapNotNull { (bookUri, items) ->
            val book = booksMap[bookUri] ?: return@mapNotNull null
            val sortedItems = items.sortedByDescending { it.timestamp }
            val latestTimestamp = sortedItems.firstOrNull()?.timestamp ?: 0L
            BookAnnotationsGroup(
                book = book,
                latestModifiedAt = latestTimestamp,
                items = sortedItems
            )
        }.sortedByDescending { it.latestModifiedAt }

        AnnotationsState(
            isLoading = false,
            bookGroups = bookGroups
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AnnotationsState(isLoading = true)
    )

    fun onAction(action: AnnotationsAction) {
        when (action) {
            is AnnotationsAction.OnAnnotationClick -> {
                viewModelScope.launch {
                    _events.send(AnnotationsEvent.NavigateToReader(action.bookUri, action.page))
                }
            }
            AnnotationsAction.OnBackClick -> {
                viewModelScope.launch {
                    _events.send(AnnotationsEvent.NavigateBack)
                }
            }
        }
    }
}
