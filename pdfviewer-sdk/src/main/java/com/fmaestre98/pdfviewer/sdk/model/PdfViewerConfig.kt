package com.fmaestre98.pdfviewer.sdk.model

import androidx.compose.ui.graphics.Color
import com.fmaestre98.pdfviewer.sdk.core.model.PdfViewerOrientation

/**
 * Selection / highlight colors used by the SDK overlays.
 */
data class PdfViewerColors(
    val selectionHighlight: Color = Color(0x40007AFF),
    val selectionHandle: Color = Color(0xFF007AFF)
)

/**
 * Configuration options for PDF viewer behaviour.
 */
data class PdfViewerConfig(
    val enableAnnotations: Boolean = true,
    val enableBookmarks: Boolean = true,
    val enableSearch: Boolean = true,
    val enableThumbnailDrawer: Boolean = true,
    val initialOrientation: PdfViewerOrientation = PdfViewerOrientation.Horizontal,
    val initialPage: Int? = null,
    val colors: PdfViewerColors = PdfViewerColors()
)
