package com.fmaestre98.pdfviewer.repository

import com.fmaestre98.pdfviewer.models.Book
import com.fmaestre98.pdfviewer.room.BookDao
import com.fmaestre98.pdfviewer.room.toBook
import com.fmaestre98.pdfviewer.room.toBookEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class RoomPdfRepository @Inject constructor(
    private val bookDao: BookDao,
    @ApplicationContext private val context: Context
) : PDFRepository {

    private val storageDir: File by lazy {
        File(context.filesDir, "pdfs").apply {
            if (!exists()) {
                mkdirs()
            }
        }
    }

    override fun observeBooks(): Flow<List<Book>> =
        bookDao.observeAllBooks().map { entities -> entities.map { it.toBook() } }

    override suspend fun getAllBooks(): List<Book> =
        bookDao.getAllBooks().map { it.toBook() }

    override suspend fun addBook(sourceUri: Uri, displayName: String, sizeBytes: Long) {
        val fileName = UUID.randomUUID().toString() + ".pdf"
        val internalFile = File(storageDir, fileName)

        // Copy from source URI to internal storage
        context.contentResolver.openInputStream(sourceUri)?.use { input ->
            FileOutputStream(internalFile).use { output ->
                input.copyTo(output)
            }
        }

        val fileUriString = Uri.fromFile(internalFile).toString()

        val newBook = Book(
            uri = fileUriString,
            displayName = displayName,
            filePath = internalFile.absolutePath,
            sizeBytes = internalFile.length()
        )
        bookDao.insertBook(newBook.toBookEntity())
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
        val book = bookDao.getBookByUri(bookUri)
        if (book != null && book.filePath != null) {
            val file = File(book.filePath)
            if (file.exists()) {
                file.delete()
            }
        }
        bookDao.deleteBookByUri(bookUri)
    }
}
