package com.fmaestre98.pdfviewer.sdk.ui.reader

import com.fmaestre98.pdfviewer.sdk.core.model.PdfViewerOrientation
import com.fmaestre98.pdfviewer.sdk.core.text.SearchResult
import com.fmaestre98.pdfviewer.sdk.storage.PdfHighlight
import com.fmaestre98.pdfviewer.sdk.storage.PdfPageNote

internal data class PdfReaderState(
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
    val highlights: Map<Int, List<PdfHighlight>> = emptyMap(),
    val currentPageNote: PdfPageNote? = null,
    val isSearchOpen: Boolean = false,
    val searchQuery: String = "",
    val searchResults: List<SearchResult> = emptyList(),
    val isSearching: Boolean = false,
    val orientation: PdfViewerOrientation = PdfViewerOrientation.Horizontal,
    val enableAnnotations: Boolean = true,
    val enableBookmarks: Boolean = true,
    val enableSearch: Boolean = true,
    val enableThumbnailDrawer: Boolean = true
)
