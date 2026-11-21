package com.fmaestre98.pdfviewer.sdk

import com.fmaestre98.pdfviewer.repository.BookmarkRepository
import com.fmaestre98.pdfviewer.repository.HighlightRepository
import com.fmaestre98.pdfviewer.repository.PDFRepository
import com.fmaestre98.pdfviewer.repository.PageNoteRepository
import com.fmaestre98.pdfviewer.sdk.storage.NormalizedRect
import com.fmaestre98.pdfviewer.sdk.storage.PdfBookmark
import com.fmaestre98.pdfviewer.sdk.storage.PdfDocumentAnnotations
import com.fmaestre98.pdfviewer.sdk.storage.PdfHighlight
import com.fmaestre98.pdfviewer.sdk.storage.PdfPageNote
import com.fmaestre98.pdfviewer.sdk.storage.PdfViewerStorage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Bridges the host app Room database to the SDK [PdfViewerStorage] contract.
 */
@Singleton
class AppPdfViewerStorage @Inject constructor(
    private val bookmarkRepository: BookmarkRepository,
    private val highlightRepository: HighlightRepository,
    private val pageNoteRepository: PageNoteRepository,
    private val pdfRepository: PDFRepository
) : PdfViewerStorage {

    override suspend fun getBookmarkedPages(documentId: String): Set<Int> =
        bookmarkRepository.getAllBookmarksForBook(documentId).map { it.page }.toSet()

    override suspend fun isPageBookmarked(documentId: String, page: Int): Boolean =
        bookmarkRepository.isPageBookmarked(documentId, page)

    override suspend fun toggleBookmark(documentId: String, page: Int): Boolean =
        bookmarkRepository.toggleBookmark(documentId, page)

    override suspend fun getHighlightsByPage(documentId: String): Map<Int, List<PdfHighlight>> =
        highlightRepository.getHighlightsGroupedByPage(documentId).mapValues { (_, list) ->
            list.map {
                PdfHighlight(
                    groupId = it.groupId,
                    color = it.color,
                    startX = it.startX,
                    startY = it.startY,
                    endX = it.endX,
                    endY = it.endY,
                    snippet = it.snippet
                )
            }
        }

    override suspend fun saveHighlight(
        documentId: String,
        page: Int,
        snippet: String,
        color: String,
        groupId: String,
        rects: List<NormalizedRect>
    ) {
        highlightRepository.saveHighlights(
            bookUri = documentId,
            page = page,
            snippet = snippet,
            color = color,
            groupId = groupId,
            normalizedRects = rects
        )
    }

    override suspend fun deleteHighlight(groupId: String) {
        highlightRepository.deleteHighlight(groupId)
    }

    override suspend fun getPageNote(documentId: String, page: Int): PdfPageNote? =
        pageNoteRepository.getNoteForPage(documentId, page)?.let {
            PdfPageNote(
                page = it.page,
                noteText = it.noteText,
                color = it.color,
                updatedAt = it.updatedAt
            )
        }

    override suspend fun savePageNote(documentId: String, page: Int, text: String, color: String?) {
        pageNoteRepository.saveNote(documentId, page, text, color)
    }

    override suspend fun deletePageNote(documentId: String, page: Int) {
        pageNoteRepository.deleteNote(documentId, page)
    }

    override suspend fun getLastReadPage(documentId: String): Int? {
        val page = pdfRepository.getLastReadPage(documentId)
        return page
    }

    override suspend fun saveLastReadPage(documentId: String, page: Int) {
        pdfRepository.updateLastReadPage(documentId, page)
    }

    override fun observeAnnotations(documentId: String): Flow<PdfDocumentAnnotations> {
        return combine(
            bookmarkRepository.getBookmarksForBook(documentId),
            highlightRepository.getAllHighlights(),
            pageNoteRepository.getAllNotes()
        ) { bookmarks, highlights, notes ->
            PdfDocumentAnnotations(
                bookmarks = bookmarks.map {
                    PdfBookmark(page = it.page, createdAt = it.createdAt)
                },
                highlights = highlights
                    .filter { it.bookUri == documentId }
                    .map {
                        PdfHighlight(
                            groupId = it.groupId,
                            color = it.color,
                            startX = it.startX,
                            startY = it.startY,
                            endX = it.endX,
                            endY = it.endY,
                            snippet = it.snippet,
                            page = it.page,
                            updatedAt = it.updatedAt
                        )
                    },
                notes = notes
                    .filter { it.bookUri == documentId }
                    .map {
                        PdfPageNote(
                            page = it.page,
                            noteText = it.noteText,
                            color = it.color,
                            updatedAt = it.updatedAt
                        )
                    }
            )
        }
    }
}
