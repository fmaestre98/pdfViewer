package com.fmaestre98.pdfviewer.repository

import kotlinx.coroutines.flow.Flow
import com.fmaestre98.pdfviewer.room.PageNoteDao
import com.fmaestre98.pdfviewer.room.entity.PageNoteEntity
import javax.inject.Inject

interface PageNoteRepository {
    suspend fun saveNote(bookUri: String, page: Int, text: String, color: String? = null)
    suspend fun deleteNote(bookUri: String, page: Int)
    suspend fun getNoteForPage(bookUri: String, page: Int): PageNoteEntity?
    suspend fun getAllNotesForBook(bookUri: String): List<PageNoteEntity>
    fun getAllNotes(): Flow<List<PageNoteEntity>>
}

class PageNoteRepositoryImpl @Inject constructor(
    private val pageNoteDao: PageNoteDao
) : PageNoteRepository {

    override suspend fun saveNote(bookUri: String, page: Int, text: String, color: String?) {
        pageNoteDao.insertOrUpdateNote(
            PageNoteEntity(
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

    override suspend fun getNoteForPage(bookUri: String, page: Int): PageNoteEntity? =
        pageNoteDao.getNoteForPage(bookUri, page)

    override suspend fun getAllNotesForBook(bookUri: String): List<PageNoteEntity> =
        pageNoteDao.getAllNotesForBook(bookUri)

    override fun getAllNotes(): Flow<List<PageNoteEntity>> =
        pageNoteDao.getAllNotesFlow()
}
