package com.fmaestre98.pdfviewer.sdk.ui.reader

sealed interface PdfReaderEvent {
    data object NavigateBack : PdfReaderEvent
    data class ShowSnackbar(val messageResId: Int) : PdfReaderEvent
}
