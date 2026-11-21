package com.fmaestre98.pdfviewer.sdk.ui.annotations

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
