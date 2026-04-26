package com.fmaestre98.pdfviewer.ui.screens.reader

data class PdfReaderState(
    val uri: String = "",
    val filePath: String? = null,
    val isLoading: Boolean = true,
    val initialPage: Int = 0,
    val error: String? = null,
    val isFabExpanded: Boolean = false,
    val isThumbnailDrawerOpen: Boolean = false,
    val currentPage: Int = 0,
    val pageCount: Int = 0,
    val isCurrentPageBookmarked: Boolean = false,
    val bookmarkedPages: Set<Int> = emptySet(),
    val highlights: Map<Int, List<com.fmaestre98.pdfviewer.repository.HighlightData>> = emptyMap(),
    val currentPageNote: com.fmaestre98.pdfviewer.room.entity.PageNoteEntity? = null
)
