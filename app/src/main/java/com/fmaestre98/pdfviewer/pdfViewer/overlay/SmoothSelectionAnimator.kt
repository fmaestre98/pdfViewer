package com.fmaestre98.pdfviewer.pdfViewer.overlay

import android.animation.ValueAnimator
import android.view.animation.AccelerateDecelerateInterpolator
import com.fmaestre98.pdfviewer.pdfViewer.model.PdfChar

/**
 * Helper class to manage smooth transitions between selection states.
 * Can be used to interpolate highlight positions or handle automatic selection changes.
 */
class SmoothSelectionAnimator {
    private val animator = ValueAnimator()
    private var currentStartChar: PdfChar? = null
    private var targetStartChar: PdfChar? = null
    private var currentEndChar: PdfChar? = null
    private var targetEndChar: PdfChar? = null
    
    init {
        animator.interpolator = AccelerateDecelerateInterpolator()
    }
    
    fun animateTo(
        newStartChar: PdfChar?,
        newEndChar: PdfChar?,
        duration: Long = 100L,
        onUpdate: (startChar: PdfChar?, endChar: PdfChar?, fraction: Float) -> Unit
    ) {
        animator.cancel()
        
        currentStartChar = targetStartChar ?: newStartChar
        currentEndChar = targetEndChar ?: newEndChar
        targetStartChar = newStartChar
        targetEndChar = newEndChar
        
        // Si no hay cambio, actualizar inmediatamente
        if (currentStartChar == targetStartChar && currentEndChar == targetEndChar) {
            onUpdate(targetStartChar, targetEndChar, 1f)
            return
        }
        
        animator.setFloatValues(0f, 1f)
        animator.duration = duration
        animator.removeAllUpdateListeners()
        animator.addUpdateListener { animation ->
            val fraction = animation.animatedFraction
            onUpdate(targetStartChar, targetEndChar, fraction)
        }
        animator.start()
    }
    
    fun cancel() {
        animator.cancel()
    }
}
