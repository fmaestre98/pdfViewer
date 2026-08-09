package com.fmaestre98.pdfviewer.ui.screens.annotations

import com.fmaestre98.pdfviewer.models.Book
import com.fmaestre98.pdfviewer.room.entity.BookmarkEntity
import com.fmaestre98.pdfviewer.room.entity.HighlightEntity
import com.fmaestre98.pdfviewer.room.entity.PageNoteEntity

sealed interface AnnotationItem {
    val id: String
    val page: Int
    val timestamp: Long

    data class Highlight(
        override val id: String,
        override val page: Int,
        override val timestamp: Long,
        val snippet: String,
        val color: String
    ) : AnnotationItem

    data class PageNote(
        override val id: String,
        override val page: Int,
        override val timestamp: Long,
        val noteText: String,
        val color: String?
    ) : AnnotationItem

    data class Bookmark(
        override val id: String,
        override val page: Int,
        override val timestamp: Long
    ) : AnnotationItem
}

data class BookAnnotationsGroup(
    val book: Book,
    val latestModifiedAt: Long,
    val items: List<AnnotationItem>
)

data class AnnotationsState(
    val isLoading: Boolean = true,
    val bookGroups: List<BookAnnotationsGroup> = emptyList()
)

sealed interface AnnotationsAction {
    data class OnAnnotationClick(val bookUri: String, val page: Int) : AnnotationsAction
    object OnBackClick : AnnotationsAction
}

sealed interface AnnotationsEvent {
    data class NavigateToReader(val bookUri: String, val page: Int) : AnnotationsEvent
    object NavigateBack : AnnotationsEvent
}
