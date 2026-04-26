package com.fmaestre98.pdfviewer.room

import kotlinx.coroutines.flow.Flow

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.fmaestre98.pdfviewer.room.entity.PageNoteEntity

@Dao
interface PageNoteDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateNote(note: PageNoteEntity): Long

    @Query("SELECT * FROM ${PageNoteEntity.TABLE_NAME} WHERE ${PageNoteEntity.FIELD_BOOK_URI} = :bookUri AND ${PageNoteEntity.FIELD_PAGE} = :page LIMIT 1")
    suspend fun getNoteForPage(bookUri: String, page: Int): PageNoteEntity?

    @Query("DELETE FROM ${PageNoteEntity.TABLE_NAME} WHERE ${PageNoteEntity.FIELD_BOOK_URI} = :bookUri AND ${PageNoteEntity.FIELD_PAGE} = :page")
    suspend fun deleteNote(bookUri: String, page: Int)

    @Query("SELECT * FROM ${PageNoteEntity.TABLE_NAME} WHERE ${PageNoteEntity.FIELD_BOOK_URI} = :bookUri")
    suspend fun getAllNotesForBook(bookUri: String): List<PageNoteEntity>

    @Query("SELECT * FROM ${PageNoteEntity.TABLE_NAME} ORDER BY ${PageNoteEntity.FIELD_UPDATED_AT} DESC")
    fun getAllNotesFlow(): Flow<List<PageNoteEntity>>
}
