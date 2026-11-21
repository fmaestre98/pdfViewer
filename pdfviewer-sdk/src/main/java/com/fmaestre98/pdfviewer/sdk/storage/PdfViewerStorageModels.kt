package com.fmaestre98.pdfviewer.sdk.storage

/**
 * A single normalized bounding rect for a highlight selection (0..1 relative to page size).
 */
data class NormalizedRect(
    val startX: Float,
    val startY: Float,
    val endX: Float,
    val endY: Float
)

/**
 * Rendering / persistence model for a highlight rect.
 */
data class PdfHighlight(
    val groupId: String,
    val color: String,
    val startX: Float,
    val startY: Float,
    val endX: Float,
    val endY: Float,
    val snippet: String = "",
    val page: Int = 0,
    val updatedAt: Long = 0L
)

/**
 * Note attached to a specific page of a document.
 */
data class PdfPageNote(
    val page: Int,
    val noteText: String,
    val color: String? = null,
    val updatedAt: Long = 0L
)

/**
 * Bookmark on a specific page.
 */
data class PdfBookmark(
    val page: Int,
    val createdAt: Long = 0L
)

/**
 * Aggregated annotations for a document, useful for observation / list UIs.
 */
data class PdfDocumentAnnotations(
    val bookmarks: List<PdfBookmark> = emptyList(),
    val highlights: List<PdfHighlight> = emptyList(),
    val notes: List<PdfPageNote> = emptyList()
)
