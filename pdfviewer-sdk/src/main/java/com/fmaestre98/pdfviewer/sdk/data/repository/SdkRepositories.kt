package com.fmaestre98.pdfviewer.sdk.data.repository

import com.fmaestre98.pdfviewer.sdk.data.dao.SdkBookmarkDao
import com.fmaestre98.pdfviewer.sdk.data.dao.SdkHighlightDao
import com.fmaestre98.pdfviewer.sdk.data.dao.SdkPageNoteDao
import com.fmaestre98.pdfviewer.sdk.data.entity.SdkBookmarkEntity
import com.fmaestre98.pdfviewer.sdk.data.entity.SdkHighlightEntity
import com.fmaestre98.pdfviewer.sdk.data.entity.SdkPageNoteEntity
import kotlinx.coroutines.flow.Flow

data class HighlightData(
    val groupId: String,
    val color: String,
    val startX: Float,
    val startY: Float,
    val endX: Float,
    val endY: Float,
    val snippet: String = ""
)

data class NormalizedRect(
    val startX: Float,
    val startY: Float,
    val endX: Float,
    val endY: Float
)

interface SdkBookmarkRepository {
    suspend fun addBookmark(bookUri: String, page: Int)
    suspend fun removeBookmark(bookUri: String, page: Int)
    suspend fun toggleBookmark(bookUri: String, page: Int): Boolean
    suspend fun isPageBookmarked(bookUri: String, page: Int): Boolean
    suspend fun getAllBookmarksForBook(bookUri: String): List<SdkBookmarkEntity>
    fun getAllBookmarks(): Flow<List<SdkBookmarkEntity>>
}

class SdkBookmarkRepositoryImpl(
    private val bookmarkDao: SdkBookmarkDao
) : SdkBookmarkRepository {

    override suspend fun addBookmark(bookUri: String, page: Int) {
        bookmarkDao.insertBookmark(
            SdkBookmarkEntity(
                bookUri = bookUri,
                page = page,
                createdAt = System.currentTimeMillis()
            )
        )
    }

    override suspend fun removeBookmark(bookUri: String, page: Int) {
        bookmarkDao.deleteBookmarkByPage(bookUri, page)
    }

    override suspend fun toggleBookmark(bookUri: String, page: Int): Boolean {
        return if (bookmarkDao.isPageBookmarked(bookUri, page)) {
            bookmarkDao.deleteBookmarkByPage(bookUri, page)
            false
        } else {
            addBookmark(bookUri, page)
            true
        }
    }

    override suspend fun isPageBookmarked(bookUri: String, page: Int): Boolean =
        bookmarkDao.isPageBookmarked(bookUri, page)

    override suspend fun getAllBookmarksForBook(bookUri: String): List<SdkBookmarkEntity> =
        bookmarkDao.getAllBookmarksForBook(bookUri)

    override fun getAllBookmarks(): Flow<List<SdkBookmarkEntity>> =
        bookmarkDao.getAllBookmarksFlow()
}

interface SdkHighlightRepository {
    suspend fun saveHighlights(
        bookUri: String,
        page: Int,
        snippet: String,
        color: String,
        groupId: String,
        normalizedRects: List<NormalizedRect>
    )
    suspend fun deleteHighlight(groupId: String)
    suspend fun getAllHighlightsForBook(bookUri: String): List<SdkHighlightEntity>
    fun getAllHighlights(): Flow<List<SdkHighlightEntity>>
    suspend fun getHighlightsGroupedByPage(bookUri: String): Map<Int, List<HighlightData>>
}

class SdkHighlightRepositoryImpl(
    private val highlightDao: SdkHighlightDao
) : SdkHighlightRepository {

    override suspend fun saveHighlights(
        bookUri: String,
        page: Int,
        snippet: String,
        color: String,
        groupId: String,
        normalizedRects: List<NormalizedRect>
    ) {
        val entities = normalizedRects.map { rect ->
            SdkHighlightEntity(
                bookUri = bookUri,
                page = page,
                snippet = snippet,
                color = color,
                startX = rect.startX,
                startY = rect.startY,
                endX = rect.endX,
                endY = rect.endY,
                groupId = groupId,
                updatedAt = System.currentTimeMillis()
            )
        }
        highlightDao.insertHighlights(entities)
    }

    override suspend fun deleteHighlight(groupId: String) {
        highlightDao.deleteHighlightsByGroup(groupId)
    }

    override suspend fun getAllHighlightsForBook(bookUri: String): List<SdkHighlightEntity> =
        highlightDao.getAllHighlightsForBook(bookUri)

    override fun getAllHighlights(): Flow<List<SdkHighlightEntity>> =
        highlightDao.getAllHighlightsFlow()

    override suspend fun getHighlightsGroupedByPage(bookUri: String): Map<Int, List<HighlightData>> =
        highlightDao.getAllHighlightsForBook(bookUri)
            .groupBy { it.page }
            .mapValues { (_, entities) ->
                entities.map { entity ->
                    HighlightData(
                        groupId = entity.groupId,
                        color = entity.color,
                        startX = entity.startX,
                        startY = entity.startY,
                        endX = entity.endX,
                        endY = entity.endY,
                        snippet = entity.snippet
                    )
                }
            }
}

interface SdkPageNoteRepository {
    suspend fun saveNote(bookUri: String, page: Int, text: String, color: String? = null)
    suspend fun deleteNote(bookUri: String, page: Int)
    suspend fun getNoteForPage(bookUri: String, page: Int): SdkPageNoteEntity?
    suspend fun getAllNotesForBook(bookUri: String): List<SdkPageNoteEntity>
    fun getAllNotes(): Flow<List<SdkPageNoteEntity>>
}

class SdkPageNoteRepositoryImpl(
    private val pageNoteDao: SdkPageNoteDao
) : SdkPageNoteRepository {

    override suspend fun saveNote(bookUri: String, page: Int, text: String, color: String?) {
        pageNoteDao.insertOrUpdateNote(
            SdkPageNoteEntity(
                bookUri = bookUri,
                page = page,
                noteText = text,
                color = color,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    override suspend fun deleteNote(bookUri: String, page: Int) {
        pageNoteDao.deleteNote(bookUri, page)
    }

    override suspend fun getNoteForPage(bookUri: String, page: Int): SdkPageNoteEntity? =
        pageNoteDao.getNoteForPage(bookUri, page)

    override suspend fun getAllNotesForBook(bookUri: String): List<SdkPageNoteEntity> =
        pageNoteDao.getAllNotesForBook(bookUri)

    override fun getAllNotes(): Flow<List<SdkPageNoteEntity>> =
        pageNoteDao.getAllNotesFlow()
}
