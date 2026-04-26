package com.fmaestre98.pdfviewer.repository

import kotlinx.coroutines.flow.Flow
import com.fmaestre98.pdfviewer.room.HighlightDao
import com.fmaestre98.pdfviewer.room.entity.HighlightEntity
import javax.inject.Inject

/**
 * Rendering model for a single highlight rect.
 * Uses normalized coordinates (0..1) relative to the page dimensions.
 */
data class HighlightData(
    val groupId: String,
    val color: String,
    val startX: Float,
    val startY: Float,
    val endX: Float,
    val endY: Float,
    val snippet: String = ""
)

/** A single normalized bounding rect for a highlight selection. */
data class NormalizedRect(
    val startX: Float,
    val startY: Float,
    val endX: Float,
    val endY: Float
)

interface HighlightRepository {
    suspend fun saveHighlights(
        bookUri: String,
        page: Int,
        snippet: String,
        color: String,
        groupId: String,
        normalizedRects: List<NormalizedRect>
    )
    suspend fun deleteHighlight(groupId: String)
    suspend fun getAllHighlightsForBook(bookUri: String): List<HighlightEntity>
    fun getAllHighlights(): Flow<List<HighlightEntity>>
    suspend fun getHighlightsGroupedByPage(bookUri: String): Map<Int, List<HighlightData>>
}

class HighlightRepositoryImpl @Inject constructor(
    private val highlightDao: HighlightDao
) : HighlightRepository {

    override suspend fun saveHighlights(
        bookUri: String,
        page: Int,
        snippet: String,
        color: String,
        groupId: String,
        normalizedRects: List<NormalizedRect>
    ) {
        val entities = normalizedRects.map { rect ->
            HighlightEntity(
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

    override suspend fun getAllHighlightsForBook(bookUri: String): List<HighlightEntity> =
        highlightDao.getAllHighlightsForBook(bookUri)

    override fun getAllHighlights(): Flow<List<HighlightEntity>> =
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
