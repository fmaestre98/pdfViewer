package com.fmaestre98.pdfviewer.room

import com.fmaestre98.pdfviewer.room.entity.BookEntity
import com.fmaestre98.pdfviewer.room.entity.HighlightEntity
import com.fmaestre98.pdfviewer.room.entity.PageNoteEntity

/** SQL string constants used by [BookDao] and the annotation DAOs. */
object BookQueries {

    // ── Books ────────────────────────────────────────────────────────────────
    const val GET_ALL_BOOKS =
        "SELECT * FROM ${BookEntity.TABLE_NAME} ORDER BY ${BookEntity.FIELD_ADDED_AT} DESC"

    const val GET_BOOK_BY_URI =
        "SELECT * FROM ${BookEntity.TABLE_NAME} WHERE ${BookEntity.FIELD_URI} = :uri LIMIT 1"

    const val UPDATE_LAST_READ_PAGE =
        "UPDATE ${BookEntity.TABLE_NAME} SET ${BookEntity.FIELD_LAST_READ_PAGE} = :page WHERE ${BookEntity.FIELD_URI} = :bookUri"

    const val DELETE_BOOK_BY_URI =
        "DELETE FROM ${BookEntity.TABLE_NAME} WHERE ${BookEntity.FIELD_URI} = :uri"
}

/** Legacy query constants kept for the annotation DAOs (BookmarkDao, HighlightDao, PageNoteDao). */
object Queries {

    // Highlights
    const val GET_HIGHLIGHTS_FOR_PAGE =
        "SELECT * FROM ${HighlightEntity.TABLE_NAME} WHERE ${HighlightEntity.FIELD_BOOK_URI} = :bookUri AND ${HighlightEntity.FIELD_PAGE} = :page"

    const val GET_ALL_HIGHLIGHTS_FOR_BOOK =
        "SELECT * FROM ${HighlightEntity.TABLE_NAME} WHERE ${HighlightEntity.FIELD_BOOK_URI} = :bookUri ORDER BY ${HighlightEntity.FIELD_PAGE}, ${HighlightEntity.FIELD_ID}"

    const val DELETE_HIGHLIGHTS_BY_GROUP =
        "DELETE FROM ${HighlightEntity.TABLE_NAME} WHERE ${HighlightEntity.FIELD_GROUP_ID} = :groupId"

    // Page Notes
    const val GET_PAGE_NOTE =
        "SELECT * FROM ${PageNoteEntity.TABLE_NAME} WHERE ${PageNoteEntity.FIELD_BOOK_URI} = :bookUri AND ${PageNoteEntity.FIELD_PAGE} = :page LIMIT 1"

    const val DELETE_PAGE_NOTE =
        "DELETE FROM ${PageNoteEntity.TABLE_NAME} WHERE ${PageNoteEntity.FIELD_BOOK_URI} = :bookUri AND ${PageNoteEntity.FIELD_PAGE} = :page"
}
