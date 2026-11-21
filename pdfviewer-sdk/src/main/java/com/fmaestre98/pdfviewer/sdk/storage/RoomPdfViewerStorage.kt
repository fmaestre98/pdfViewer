package com.fmaestre98.pdfviewer.sdk.storage

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.fmaestre98.pdfviewer.sdk.data.db.PdfSdkDatabase
import com.fmaestre98.pdfviewer.sdk.data.entity.SdkBookmarkEntity
import com.fmaestre98.pdfviewer.sdk.data.entity.SdkHighlightEntity
import com.fmaestre98.pdfviewer.sdk.data.entity.SdkPageNoteEntity
import com.fmaestre98.pdfviewer.sdk.data.entity.SdkReadingProgressEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/**
 * Default Room-backed [PdfViewerStorage] used when the host app does not provide its own.
 */
class RoomPdfViewerStorage(
    context: Context
) : PdfViewerStorage {

    private val db = PdfSdkDatabase.getInstance(context.applicationContext)
    private val bookmarkDao = db.bookmarkDao()
    private val highlightDao = db.highlightDao()
    private val pageNoteDao = db.pageNoteDao()
    private val readingProgressDao = db.readingProgressDao()

    override suspend fun getBookmarkedPages(documentId: String): Set<Int> =
        bookmarkDao.getAllBookmarksForBook(documentId).map { it.page }.toSet()

    override suspend fun isPageBookmarked(documentId: String, page: Int): Boolean =
        bookmarkDao.isPageBookmarked(documentId, page)

    override suspend fun toggleBookmark(documentId: String, page: Int): Boolean {
        return if (bookmarkDao.isPageBookmarked(documentId, page)) {
            bookmarkDao.deleteBookmarkByPage(documentId, page)
            false
        } else {
            bookmarkDao.insertBookmark(
                SdkBookmarkEntity(
                    bookUri = documentId,
                    page = page,
                    createdAt = System.currentTimeMillis()
                )
            )
            true
        }
    }

    override suspend fun getHighlightsByPage(documentId: String): Map<Int, List<PdfHighlight>> =
        highlightDao.getAllHighlightsForBook(documentId)
            .groupBy { it.page }
            .mapValues { (_, entities) -> entities.map { it.toPdfHighlight() } }

    override suspend fun saveHighlight(
        documentId: String,
        page: Int,
        snippet: String,
        color: String,
        groupId: String,
        rects: List<NormalizedRect>
    ) {
        val now = System.currentTimeMillis()
        val entities = rects.map { rect ->
            SdkHighlightEntity(
                bookUri = documentId,
                page = page,
                snippet = snippet,
                color = color,
                startX = rect.startX,
                startY = rect.startY,
                endX = rect.endX,
                endY = rect.endY,
                groupId = groupId,
                updatedAt = now
            )
        }
        highlightDao.insertHighlights(entities)
    }

    override suspend fun deleteHighlight(groupId: String) {
        highlightDao.deleteHighlightsByGroup(groupId)
    }

    override suspend fun getPageNote(documentId: String, page: Int): PdfPageNote? =
        pageNoteDao.getNoteForPage(documentId, page)?.toPdfPageNote()

    override suspend fun savePageNote(documentId: String, page: Int, text: String, color: String?) {
        pageNoteDao.insertOrUpdateNote(
            SdkPageNoteEntity(
                bookUri = documentId,
                page = page,
                noteText = text,
                color = color,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    override suspend fun deletePageNote(documentId: String, page: Int) {
        pageNoteDao.deleteNote(documentId, page)
    }

    override suspend fun getLastReadPage(documentId: String): Int? =
        readingProgressDao.get(documentId)?.lastReadPage

    override suspend fun saveLastReadPage(documentId: String, page: Int) {
        readingProgressDao.upsert(
            SdkReadingProgressEntity(
                documentId = documentId,
                lastReadPage = page,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    override fun observeAnnotations(documentId: String): Flow<PdfDocumentAnnotations> {
        return combine(
            bookmarkDao.getBookmarksForBookFlow(documentId),
            highlightDao.getHighlightsForBookFlow(documentId),
            pageNoteDao.getNotesForBookFlow(documentId)
        ) { bookmarks, highlights, notes ->
            PdfDocumentAnnotations(
                bookmarks = bookmarks.map { PdfBookmark(page = it.page, createdAt = it.createdAt) },
                highlights = highlights.map { it.toPdfHighlight() },
                notes = notes.map { it.toPdfPageNote() }
            )
        }
    }

    private fun SdkHighlightEntity.toPdfHighlight() = PdfHighlight(
        groupId = groupId,
        color = color,
        startX = startX,
        startY = startY,
        endX = endX,
        endY = endY,
        snippet = snippet,
        page = page,
        updatedAt = updatedAt
    )

    private fun SdkPageNoteEntity.toPdfPageNote() = PdfPageNote(
        page = page,
        noteText = noteText,
        color = color,
        updatedAt = updatedAt
    )
}

@Composable
fun rememberDefaultPdfViewerStorage(): PdfViewerStorage {
    val context = LocalContext.current
    return remember(context.applicationContext) {
        RoomPdfViewerStorage(context.applicationContext)
    }
}
