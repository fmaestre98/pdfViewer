package com.fmaestre98.pdfviewer.pdfViewer.overlay

import android.graphics.RectF
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import com.fmaestre98.pdfviewer.pdfViewer.model.OptimizedPageModel
import com.fmaestre98.pdfviewer.pdfViewer.model.PageModel
import com.fmaestre98.pdfviewer.pdfViewer.model.PdfChar
import kotlin.math.min
import kotlin.math.abs

/**
 * Helper class for efficient selection rendering.
 * Reduces draw calls by grouping adjacent characters into single rectangles.
 */
class SelectionPainter {
    private val selectionPaint = Paint().apply {
        color = Color(0x662196F3) // Light Blue with transparency
        style = PaintingStyle.Fill
    }
    
    fun drawSelection(
        canvas: Canvas,
        startChar: PdfChar,
        endChar: PdfChar,
        optimizedPageModel: OptimizedPageModel,
        pageWidth: Int,
        pageHeight: Int,
        overlaySize: Size
    ) {
        val selectedChars = optimizedPageModel.getCharactersBetween(startChar, endChar)
        if (selectedChars.isEmpty()) return

        // Cache projection calculation values
        val viewWidth = overlaySize.width
        val viewHeight = overlaySize.height
        val scaleX = viewWidth / pageWidth
        val scaleY = viewHeight / pageHeight
        val baseScale = min(scaleX, scaleY)
        val renderedWidth = pageWidth * baseScale
        val renderedHeight = pageHeight * baseScale
        val centeringOffsetX = (viewWidth - renderedWidth) / 2f
        val centeringOffsetY = (viewHeight - renderedHeight) / 2f

        // Helper to project rect
        fun projectRect(rect: RectF): Rect {
            val left = rect.left * baseScale + centeringOffsetX
            val top = rect.top * baseScale + centeringOffsetY
            val right = rect.right * baseScale + centeringOffsetX
            val bottom = rect.bottom * baseScale + centeringOffsetY
            return Rect(left, top, right, bottom)
        }

        // Line grouping logic
        var currentLineTop = -1f
        var currentLineBottom = -1f
        var currentLineLeft = Float.MAX_VALUE
        var currentLineRight = Float.MIN_VALUE
        
        // Tolerance for considering characters on same line (in screen pixels)
        val lineTolerance = 5f 

        // Draw batching
        selectedChars.forEach { char ->
            val screenRect = projectRect(char.rect)
            
            // If new line or not initialized
            if (currentLineTop == -1f) {
                 currentLineTop = screenRect.top
                 currentLineBottom = screenRect.bottom
                 currentLineLeft = screenRect.left
                 currentLineRight = screenRect.right
            } else {
                // Check if on same line
                val verticalCenter = (screenRect.top + screenRect.bottom) / 2
                val currentVerticalCenter = (currentLineTop + currentLineBottom) / 2
                
                if (abs(verticalCenter - currentVerticalCenter) < lineTolerance) {
                    // Extend current line
                    currentLineLeft = minOf(currentLineLeft, screenRect.left)
                    currentLineRight = maxOf(currentLineRight, screenRect.right)
                    // Expand vertical bounds to encompass max height
                    currentLineTop = minOf(currentLineTop, screenRect.top)
                    currentLineBottom = maxOf(currentLineBottom, screenRect.bottom)
                } else {
                    // Draw previous line
                    canvas.drawRect(
                        Rect(currentLineLeft, currentLineTop, currentLineRight, currentLineBottom),
                        selectionPaint
                    )
                    
                    // Start new line
                    currentLineTop = screenRect.top
                    currentLineBottom = screenRect.bottom
                    currentLineLeft = screenRect.left
                    currentLineRight = screenRect.right
                }
            }
        }
        
        // Draw last line
        if (currentLineTop != -1f) {
            canvas.drawRect(
                Rect(currentLineLeft, currentLineTop, currentLineRight, currentLineBottom),
                selectionPaint
            )
        }
    }
}
