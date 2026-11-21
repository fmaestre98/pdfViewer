package com.fmaestre98.pdfviewer.sdk.model

enum class PdfViewerOrientation {
    Vertical,
    Horizontal
}

/**
 * Opciones de configuración para el comportamiento del visor de PDF.
 */
data class PdfViewerConfig(
    val enableAnnotations: Boolean = true,
    val enableBookmarks: Boolean = true,
    val enableSearch: Boolean = true,
    val enableThumbnailDrawer: Boolean = true,
    val initialOrientation: PdfViewerOrientation = PdfViewerOrientation.Vertical,
    val initialPage: Int = 0
)
