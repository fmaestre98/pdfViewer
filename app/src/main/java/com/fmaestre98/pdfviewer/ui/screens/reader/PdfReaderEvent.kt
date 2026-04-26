package com.fmaestre98.pdfviewer.ui.screens.reader

sealed interface PdfReaderEvent {
    data object NavigateBack : PdfReaderEvent
    data class ShowSnackbar(val messageResId: Int) : PdfReaderEvent
}
