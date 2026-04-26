package com.fmaestre98.pdfviewer.room.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = BookmarkEntity.TABLE_NAME,
    foreignKeys = [
        ForeignKey(
            entity = BookEntity::class,
            parentColumns = [BookEntity.FIELD_URI],
            childColumns = [BookmarkEntity.FIELD_BOOK_URI],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [androidx.room.Index(value = [BookmarkEntity.FIELD_BOOK_URI])]
)
data class BookmarkEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = FIELD_ID)
    val id: Long = 0,

    /** URI of the parent [BookEntity]. */
    @ColumnInfo(name = FIELD_BOOK_URI)
    val bookUri: String,

    @ColumnInfo(name = FIELD_PAGE)
    val page: Int,

    @ColumnInfo(name = FIELD_CREATED_AT)
    val createdAt: Long = System.currentTimeMillis(),
) {
    companion object {
        const val TABLE_NAME = "pdfBookmarks"

        const val FIELD_ID = "id"
        const val FIELD_BOOK_URI = "book_uri"
        const val FIELD_PAGE = "page"
        const val FIELD_CREATED_AT = "created_at"
    }
}

@Entity(
    tableName = HighlightEntity.TABLE_NAME,
    foreignKeys = [
        ForeignKey(
            entity = BookEntity::class,
            parentColumns = [BookEntity.FIELD_URI],
            childColumns = [HighlightEntity.FIELD_BOOK_URI],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = [HighlightEntity.FIELD_BOOK_URI]),
        Index(value = [HighlightEntity.FIELD_GROUP_ID])
    ]
)
data class HighlightEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = FIELD_ID)
    val id: Long = 0,

    /** URI of the parent [BookEntity]. */
    @ColumnInfo(name = FIELD_BOOK_URI)
    val bookUri: String,

    @ColumnInfo(name = FIELD_PAGE)
    val page: Int,

    @ColumnInfo(name = FIELD_SNIPPET)
    val snippet: String,

    @ColumnInfo(name = FIELD_COLOR)
    val color: String,

    /** Normalized start X coordinate (0..1 relative to page width) */
    @ColumnInfo(name = FIELD_START_X)
    val startX: Float,

    /** Normalized start Y coordinate (0..1 relative to page height) */
    @ColumnInfo(name = FIELD_START_Y)
    val startY: Float,

    /** Normalized end X coordinate (0..1 relative to page width) */
    @ColumnInfo(name = FIELD_END_X)
    val endX: Float,

    /** Normalized end Y coordinate (0..1 relative to page height) */
    @ColumnInfo(name = FIELD_END_Y)
    val endY: Float,

    /** Groups multi-line highlights together (UUID string) */
    @ColumnInfo(name = FIELD_GROUP_ID)
    val groupId: String,

    @ColumnInfo(name = FIELD_UPDATED_AT)
    val updatedAt: Long = System.currentTimeMillis(),
) {
    companion object {
        const val TABLE_NAME = "pdf_highlights"

        const val FIELD_ID = "id"
        const val FIELD_BOOK_URI = "book_uri"
        const val FIELD_PAGE = "page"
        const val FIELD_SNIPPET = "snippet"
        const val FIELD_COLOR = "color"
        const val FIELD_START_X = "start_x"
        const val FIELD_START_Y = "start_y"
        const val FIELD_END_X = "end_x"
        const val FIELD_END_Y = "end_y"
        const val FIELD_GROUP_ID = "group_id"
        const val FIELD_UPDATED_AT = "updated_at"
    }
}

@Entity(
    tableName = PageNoteEntity.TABLE_NAME,
    foreignKeys = [
        ForeignKey(
            entity = BookEntity::class,
            parentColumns = [BookEntity.FIELD_URI],
            childColumns = [PageNoteEntity.FIELD_BOOK_URI],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        androidx.room.Index(value = [PageNoteEntity.FIELD_BOOK_URI, PageNoteEntity.FIELD_PAGE], unique = true)
    ]
)
data class PageNoteEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = FIELD_ID)
    val id: Long = 0,

    /** URI of the parent [BookEntity]. */
    @ColumnInfo(name = FIELD_BOOK_URI)
    val bookUri: String,

    @ColumnInfo(name = FIELD_PAGE)
    val page: Int,

    @ColumnInfo(name = FIELD_NOTE_TEXT)
    val noteText: String,

    /** Optional background color for the note chip. */
    @ColumnInfo(name = FIELD_COLOR)
    val color: String? = null,

    @ColumnInfo(name = FIELD_UPDATED_AT)
    val updatedAt: Long = System.currentTimeMillis()
) {
    companion object {
        const val TABLE_NAME = "page_notes"

        const val FIELD_ID = "id"
        const val FIELD_BOOK_URI = "book_uri"
        const val FIELD_PAGE = "page"
        const val FIELD_NOTE_TEXT = "note_text"
        const val FIELD_COLOR = "color"
        const val FIELD_UPDATED_AT = "updated_at"
    }
}