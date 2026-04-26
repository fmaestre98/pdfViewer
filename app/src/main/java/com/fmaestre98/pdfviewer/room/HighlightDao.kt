package com.fmaestre98.pdfviewer.room

import kotlinx.coroutines.flow.Flow

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.fmaestre98.pdfviewer.room.entity.HighlightEntity

@Dao
interface HighlightDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHighlight(highlight: HighlightEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHighlights(highlights: List<HighlightEntity>)

    @Query("DELETE FROM ${HighlightEntity.TABLE_NAME} WHERE ${HighlightEntity.FIELD_GROUP_ID} = :groupId")
    suspend fun deleteHighlightsByGroup(groupId: String)

    @Query("SELECT * FROM ${HighlightEntity.TABLE_NAME} WHERE ${HighlightEntity.FIELD_BOOK_URI} = :bookUri AND ${HighlightEntity.FIELD_PAGE} = :page")
    suspend fun getHighlightsForPage(bookUri: String, page: Int): List<HighlightEntity>

    @Query("SELECT * FROM ${HighlightEntity.TABLE_NAME} WHERE ${HighlightEntity.FIELD_BOOK_URI} = :bookUri ORDER BY ${HighlightEntity.FIELD_PAGE}, ${HighlightEntity.FIELD_ID}")
    suspend fun getAllHighlightsForBook(bookUri: String): List<HighlightEntity>

    @Query("SELECT * FROM ${HighlightEntity.TABLE_NAME} ORDER BY ${HighlightEntity.FIELD_UPDATED_AT} DESC")
    fun getAllHighlightsFlow(): Flow<List<HighlightEntity>>
}
