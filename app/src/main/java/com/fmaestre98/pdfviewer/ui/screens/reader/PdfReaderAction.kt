package com.fmaestre98.pdfviewer.ui.screens.reader

sealed interface PdfReaderAction {
    data class OnPageChanged(val pageIndex: Int) : PdfReaderAction
    data object OnBackClick : PdfReaderAction
}
