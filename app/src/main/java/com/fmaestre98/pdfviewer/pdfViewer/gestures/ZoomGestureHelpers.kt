package com.fmaestre98.pdfviewer.pdfViewer.gestures

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.util.lerp
import kotlinx.coroutines.launch
import com.fmaestre98.pdfviewer.pdfViewer.model.PdfViewerConstants
import com.fmaestre98.pdfviewer.pdfViewer.viewmodel.PdfViewerState

/**
 * Helper functions for zoom gesture calculations and animations.
 */
internal object ZoomGestureHelpers {

    /**
     * Calculates the next zoom level in the cycle: 1x -> 2x -> 3x -> 1x
     */
    fun nextZoomLevel(current: Float): Float {
        return when {
            current < PdfViewerConstants.ZOOM_LEVEL_2 -> PdfViewerConstants.ZOOM_LEVEL_2
            current < PdfViewerConstants.ZOOM_LEVEL_3 -> PdfViewerConstants.ZOOM_LEVEL_3
            else -> PdfViewerConstants.ZOOM_LEVEL_1
        }
    }

    /**
     * Calculates the maximum offset for a given zoom level and screen dimension.
     */
    fun calculateMaxOffset(screenDimension: Float, zoomLevel: Float): Float {
        return (screenDimension * zoomLevel - screenDimension) / 2f
    }

    /**
     * Calculates target offset for double-tap zoom animation.
     * Keeps the tapped point under the finger during zoom.
     */
    fun calculateTargetOffset(
        startOffset: Float,
        tapCoordinate: Float,
        centerCoordinate: Float,
        startZoom: Float,
        targetZoom: Float,
        maxOffset: Float
    ): Float {
        if (targetZoom == PdfViewerConstants.ZOOM_LEVEL_1) {
            return 0f
        }

        val zoomFactor = targetZoom / startZoom
        val moveDistance = (tapCoordinate - centerCoordinate) * (zoomFactor - 1)
        val rawTarget = startOffset - moveDistance

        return rawTarget.coerceIn(-maxOffset, maxOffset)
    }

    /**
     * Applies zoom constraints to a zoom value.
     */
    fun constrainZoom(zoom: Float): Float {
        return zoom.coerceIn(PdfViewerConstants.MIN_ZOOM, PdfViewerConstants.MAX_ZOOM)
    }

    /**
     * Interpolates offset during zoom animation.
     */
    fun interpolateOffset(
        startOffset: Float,
        targetOffset: Float,
        startZoom: Float,
        currentZoom: Float,
        targetZoom: Float
    ): Float {
        val fraction = (currentZoom - startZoom) / (targetZoom - startZoom)
        return lerp(startOffset, targetOffset, fraction)
    }

    /**
     * Handles transform gestures (pinch zoom and pan) for PDF view.
     * The GestureOrchestrator decides whether this should be called based on gesture priorities.
     */
    fun handleTransformGesture(
        viewerState: PdfViewerState,
        pan: Offset,
        zoom: Float,
        screenWidth: Float,
        screenHeight: Float
    ) {
        val newZoom = constrainZoom(viewerState.zoomLevel * zoom)
        viewerState.zoomLevel = newZoom

        if (newZoom > PdfViewerConstants.ZOOM_LEVEL_1) {
            val maxOffsetX = calculateMaxOffset(screenWidth, newZoom)
            val maxOffsetY = calculateMaxOffset(screenHeight, newZoom)

            val newX = viewerState.offsetX + pan.x
            val newY = viewerState.offsetY + pan.y

            viewerState.offsetX = newX.coerceIn(-maxOffsetX, maxOffsetX)
            viewerState.offsetY = newY.coerceIn(-maxOffsetY, maxOffsetY)
        } else {
            viewerState.offsetX = 0f
            viewerState.offsetY = 0f
        }
    }

    /**
     * Handles double-tap zoom animation for PDF view.
     * Animates zoom with pivot point under the tapped location.
     * The GestureOrchestrator decides whether this should be called based on gesture priorities.
     */
    fun handleDoubleTapZoom(
        viewerState: PdfViewerState,
        tapOffset: Offset,
        screenWidth: Float,
        screenHeight: Float,
        scope: kotlinx.coroutines.CoroutineScope
    ) {
        val currentZoom = viewerState.zoomLevel
        val targetZoom = nextZoomLevel(currentZoom)
        val centerX = screenWidth / 2f
        val centerY = screenHeight / 2f

        scope.launch {
            val animatable = Animatable(currentZoom)
            val startZoom = currentZoom
            val startOffsetX = viewerState.offsetX
            val startOffsetY = viewerState.offsetY

            val maxOffsetX = calculateMaxOffset(screenWidth, targetZoom)
            val maxOffsetY = calculateMaxOffset(screenHeight, targetZoom)

            val targetOffsetX = calculateTargetOffset(
                startOffset = startOffsetX,
                tapCoordinate = tapOffset.x,
                centerCoordinate = centerX,
                startZoom = startZoom,
                targetZoom = targetZoom,
                maxOffset = maxOffsetX
            )

            val targetOffsetY = calculateTargetOffset(
                startOffset = startOffsetY,
                tapCoordinate = tapOffset.y,
                centerCoordinate = centerY,
                startZoom = startZoom,
                targetZoom = targetZoom,
                maxOffset = maxOffsetY
            )

            // Animate zoom
            animatable.animateTo(
                targetValue = targetZoom,
                animationSpec = tween(durationMillis = PdfViewerConstants.ZOOM_ANIMATION_DURATION_MS)
            ) {
                val newZoom = this.value
                viewerState.zoomLevel = newZoom

                // Interpolate offsets smoothly
                viewerState.offsetX = interpolateOffset(
                    startOffset = startOffsetX,
                    targetOffset = if (targetZoom == PdfViewerConstants.ZOOM_LEVEL_1) 0f else targetOffsetX,
                    startZoom = startZoom,
                    currentZoom = newZoom,
                    targetZoom = targetZoom
                )

                viewerState.offsetY = interpolateOffset(
                    startOffset = startOffsetY,
                    targetOffset = if (targetZoom == PdfViewerConstants.ZOOM_LEVEL_1) 0f else targetOffsetY,
                    startZoom = startZoom,
                    currentZoom = newZoom,
                    targetZoom = targetZoom
                )
            }
        }
    }
}