package com.fmaestre98.pdfviewer.room

import com.fmaestre98.pdfviewer.models.Book
import com.fmaestre98.pdfviewer.room.entity.BookEntity

// ── BookEntity ↔ Book ────────────────────────────────────────────────────────

fun BookEntity.toBook(): Book = Book(
    id = id,
    uri = uri,
    displayName = displayName,
    filePath = filePath,
    sizeBytes = sizeBytes,
    totalPages = totalPages,
    lastReadPage = lastReadPage,
    addedAt = addedAt,
)

fun Book.toBookEntity(): BookEntity = BookEntity(
    id = id,
    uri = uri,
    displayName = displayName,
    filePath = filePath,
    sizeBytes = sizeBytes,
    totalPages = totalPages,
    lastReadPage = lastReadPage,
    addedAt = addedAt,
)
