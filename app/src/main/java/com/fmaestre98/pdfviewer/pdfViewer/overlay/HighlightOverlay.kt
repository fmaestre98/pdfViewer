package com.fmaestre98.pdfviewer.pdfViewer.overlay

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import com.fmaestre98.pdfviewer.repository.HighlightData
import kotlin.math.min

/**
 * Overlay that renders persistent highlights on a PDF page.
 * Uses normalized coordinates (0..1) that are projected to screen space,
 * making highlights orientation-independent.
 */
@Composable
fun HighlightOverlay(
    highlights: List<HighlightData>,
    pageWidth: Int,
    pageHeight: Int,
    modifier: Modifier = Modifier
) {
    if (highlights.isEmpty()) return

    Canvas(modifier = modifier.fillMaxSize()) {
        val viewWidth = size.width
        val viewHeight = size.height

        // Same projection logic used by TextSelectionOverlay
        val scaleX = viewWidth / pageWidth
        val scaleY = viewHeight / pageHeight
        val baseScale = min(scaleX, scaleY)
        val renderedWidth = pageWidth * baseScale
        val renderedHeight = pageHeight * baseScale
        val centeringOffsetX = (viewWidth - renderedWidth) / 2f
        val centeringOffsetY = (viewHeight - renderedHeight) / 2f

        highlights.forEach { highlight ->
            // Convert normalized coords (0..1) to PDF coords, then to screen coords
            val pdfLeft = highlight.startX * pageWidth
            val pdfTop = highlight.startY * pageHeight
            val pdfRight = highlight.endX * pageWidth
            val pdfBottom = highlight.endY * pageHeight

            val screenLeft = pdfLeft * baseScale + centeringOffsetX
            val screenTop = pdfTop * baseScale + centeringOffsetY
            val screenRight = pdfRight * baseScale + centeringOffsetX
            val screenBottom = pdfBottom * baseScale + centeringOffsetY

            val color = parseHighlightColor(highlight.color)

            drawRect(
                color = color,
                topLeft = Rect(screenLeft, screenTop, screenRight, screenBottom).topLeft,
                size = Rect(screenLeft, screenTop, screenRight, screenBottom).size
            )
        }
    }
}

/**
 * Parses a color string to a Compose Color with transparency for highlight rendering.
 */
fun parseHighlightColor(colorString: String): Color {
    return try {
        val baseColor = Color(android.graphics.Color.parseColor(colorString))
        baseColor.copy(alpha = 0.35f)
    } catch (e: Exception) {
        Color(0x59FFEB3B) // Default: yellow with transparency
    }
}
