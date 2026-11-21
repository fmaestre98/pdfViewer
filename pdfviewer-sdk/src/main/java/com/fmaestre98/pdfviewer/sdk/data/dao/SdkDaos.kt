package com.fmaestre98.pdfviewer.sdk.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.fmaestre98.pdfviewer.sdk.data.entity.SdkBookmarkEntity
import com.fmaestre98.pdfviewer.sdk.data.entity.SdkHighlightEntity
import com.fmaestre98.pdfviewer.sdk.data.entity.SdkPageNoteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SdkBookmarkDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: SdkBookmarkEntity): Long

    @Query("DELETE FROM ${SdkBookmarkEntity.TABLE_NAME} WHERE ${SdkBookmarkEntity.FIELD_BOOK_URI} = :bookUri AND ${SdkBookmarkEntity.FIELD_PAGE} = :page")
    suspend fun deleteBookmarkByPage(bookUri: String, page: Int)

    @Query("SELECT COUNT(*) FROM ${SdkBookmarkEntity.TABLE_NAME} WHERE ${SdkBookmarkEntity.FIELD_BOOK_URI} = :bookUri AND ${SdkBookmarkEntity.FIELD_PAGE} = :page")
    suspend fun isPageBookmarked(bookUri: String, page: Int): Boolean

    @Query("SELECT * FROM ${SdkBookmarkEntity.TABLE_NAME} WHERE ${SdkBookmarkEntity.FIELD_BOOK_URI} = :bookUri")
    suspend fun getAllBookmarksForBook(bookUri: String): List<SdkBookmarkEntity>

    @Query("SELECT * FROM ${SdkBookmarkEntity.TABLE_NAME} ORDER BY ${SdkBookmarkEntity.FIELD_CREATED_AT} DESC")
    fun getAllBookmarksFlow(): Flow<List<SdkBookmarkEntity>>
}

@Dao
interface SdkHighlightDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHighlights(highlights: List<SdkHighlightEntity>)

    @Query("DELETE FROM ${SdkHighlightEntity.TABLE_NAME} WHERE ${SdkHighlightEntity.FIELD_GROUP_ID} = :groupId")
    suspend fun deleteHighlightsByGroup(groupId: String)

    @Query("SELECT * FROM ${SdkHighlightEntity.TABLE_NAME} WHERE ${SdkHighlightEntity.FIELD_BOOK_URI} = :bookUri ORDER BY ${SdkHighlightEntity.FIELD_PAGE}, ${SdkHighlightEntity.FIELD_ID}")
    suspend fun getAllHighlightsForBook(bookUri: String): List<SdkHighlightEntity>

    @Query("SELECT * FROM ${SdkHighlightEntity.TABLE_NAME} ORDER BY ${SdkHighlightEntity.FIELD_UPDATED_AT} DESC")
    fun getAllHighlightsFlow(): Flow<List<SdkHighlightEntity>>
}

@Dao
interface SdkPageNoteDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateNote(note: SdkPageNoteEntity): Long

    @Query("SELECT * FROM ${SdkPageNoteEntity.TABLE_NAME} WHERE ${SdkPageNoteEntity.FIELD_BOOK_URI} = :bookUri AND ${SdkPageNoteEntity.FIELD_PAGE} = :page LIMIT 1")
    suspend fun getNoteForPage(bookUri: String, page: Int): SdkPageNoteEntity?

    @Query("DELETE FROM ${SdkPageNoteEntity.TABLE_NAME} WHERE ${SdkPageNoteEntity.FIELD_BOOK_URI} = :bookUri AND ${SdkPageNoteEntity.FIELD_PAGE} = :page")
    suspend fun deleteNote(bookUri: String, page: Int)

    @Query("SELECT * FROM ${SdkPageNoteEntity.TABLE_NAME} WHERE ${SdkPageNoteEntity.FIELD_BOOK_URI} = :bookUri")
    suspend fun getAllNotesForBook(bookUri: String): List<SdkPageNoteEntity>

    @Query("SELECT * FROM ${SdkPageNoteEntity.TABLE_NAME} ORDER BY ${SdkPageNoteEntity.FIELD_UPDATED_AT} DESC")
    fun getAllNotesFlow(): Flow<List<SdkPageNoteEntity>>
}
