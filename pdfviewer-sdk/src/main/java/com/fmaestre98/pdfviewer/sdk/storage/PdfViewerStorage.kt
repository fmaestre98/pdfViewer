package com.fmaestre98.pdfviewer.sdk.storage

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * Persistence contract for bookmarks, highlights, page notes and reading progress.
 *
 * Host apps can implement this against their own database. The SDK ships with
 * [RoomPdfViewerStorage] as a default for apps that do not provide one.
 */
interface PdfViewerStorage {
    suspend fun getBookmarkedPages(documentId: String): Set<Int>
    suspend fun isPageBookmarked(documentId: String, page: Int): Boolean
    suspend fun toggleBookmark(documentId: String, page: Int): Boolean

    suspend fun getHighlightsByPage(documentId: String): Map<Int, List<PdfHighlight>>
    suspend fun saveHighlight(
        documentId: String,
        page: Int,
        snippet: String,
        color: String,
        groupId: String,
        rects: List<NormalizedRect>
    )
    suspend fun deleteHighlight(groupId: String)

    suspend fun getPageNote(documentId: String, page: Int): PdfPageNote?
    suspend fun savePageNote(documentId: String, page: Int, text: String, color: String? = null)
    suspend fun deletePageNote(documentId: String, page: Int)

    suspend fun getLastReadPage(documentId: String): Int?
    suspend fun saveLastReadPage(documentId: String, page: Int)

    /**
     * Observes all annotations for a document. Default implementation emits once
     * by querying suspend APIs; Room-backed storage overrides with live Flows.
     */
    fun observeAnnotations(documentId: String): Flow<PdfDocumentAnnotations> = flowOf(
        PdfDocumentAnnotations()
    )
}
