package com.fmaestre98.pdfviewer.repository

import com.fmaestre98.pdfviewer.models.Book
import com.fmaestre98.pdfviewer.room.BookDao
import com.fmaestre98.pdfviewer.room.toBook
import com.fmaestre98.pdfviewer.room.toBookEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class RoomPdfRepository @Inject constructor(
    private val bookDao: BookDao
) : PDFRepository {

    override fun observeBooks(): Flow<List<Book>> =
        bookDao.observeAllBooks().map { entities -> entities.map { it.toBook() } }

    override suspend fun getAllBooks(): List<Book> =
        bookDao.getAllBooks().map { it.toBook() }

    override suspend fun addBook(book: Book) {
        bookDao.insertBook(book.toBookEntity())
    }

    override suspend fun updateBook(book: Book) {
        bookDao.updateBook(book.toBookEntity())
    }

    override suspend fun getBookByUri(bookUri: String): Book? =
        bookDao.getBookByUri(bookUri)?.toBook()

    override suspend fun updateLastReadPage(bookUri: String, page: Int) {
        bookDao.updateLastReadPage(bookUri, page)
    }

    override suspend fun getLastReadPage(bookUri: String): Int =
        bookDao.getBookByUri(bookUri)?.lastReadPage ?: 0

    override suspend fun deleteBook(bookUri: String) {
        bookDao.deleteBookByUri(bookUri)
    }
}
