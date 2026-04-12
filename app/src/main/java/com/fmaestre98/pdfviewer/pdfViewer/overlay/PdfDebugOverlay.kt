package com.fmaestre98.pdfviewer.pdfViewer.overlay

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.fmaestre98.pdfviewer.pdfViewer.model.PageModel

/**
 * Debug overlay that visualizes word bounding boxes in debug builds.
 * Only renders when BuildConfig.DEBUG is true.
 */
@Composable
fun PdfDebugOverlay(
    pageModel: PageModel?,
    pageWidth: Int,
    pageHeight: Int,
) {
    // Only show in debug builds
    val isDebug = try {
        Class.forName("com.fmaestre98.pdfviewer.BuildConfig")
            .getField("DEBUG")
            .getBoolean(null)
    } catch (e: Exception) {
        false // Default to false if BuildConfig is not available
    }
    val density = LocalDensity.current

    Box(modifier = with(density) {
        Modifier
            .width(pageWidth.toDp())
            .height(pageHeight.toDp())
    }) {
        if (isDebug && pageModel != null) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                // Draw all word bounding boxes in red (transparent)
                pageModel.coordinates.flatMap { it.words }.forEach { word ->
                    drawRect(
                        color = Color.Red.copy(alpha = 0.2f),
                        topLeft = Offset(word.rect.left, word.rect.top),
                        size = Size(word.rect.width(), word.rect.height()),
                        style = Stroke(width = 1.dp.toPx())
                    )
                }
            }
        }
    }
}

