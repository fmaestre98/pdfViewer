package com.fmaestre98.pdfviewer.ui.screens.reader

sealed interface PdfReaderAction {
    data class OnPageChanged(val pageIndex: Int) : PdfReaderAction
    data object OnBackClick : PdfReaderAction
    data object ToggleFab : PdfReaderAction
    data object CloseFab : PdfReaderAction
    data object ToggleThumbnailDrawer : PdfReaderAction
    data object CloseThumbnailDrawer : PdfReaderAction
    data class UpdatePageCount(val count: Int) : PdfReaderAction
    data object ToggleBookmark : PdfReaderAction
    data class SavePageNote(val text: String) : PdfReaderAction
    data object DeletePageNote : PdfReaderAction
    data class SaveHighlight(
        val color: String,
        val page: Int,
        val snippet: String,
        val normalizedRects: List<com.fmaestre98.pdfviewer.repository.NormalizedRect>
    ) : PdfReaderAction
}
