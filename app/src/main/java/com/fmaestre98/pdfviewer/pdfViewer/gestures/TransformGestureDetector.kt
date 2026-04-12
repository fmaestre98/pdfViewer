package com.fmaestre98.pdfviewer.pdfViewer.gestures

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateRotation
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.positionChanged
import kotlin.math.PI
import kotlin.math.abs

/**
 * Custom transform gesture detector with explicit lifecycle callbacks.
 * This extends the standard detectTransformGestures to provide:
 * - onGestureStart: Called when the gesture begins (2+ fingers touch)
 * - onGesture: Called during the gesture (same as standard detector)
 * - onGestureEnd: Called when the gesture ends (back to 0-1 fingers)
 * 
 * This is critical for the GestureOrchestrator to properly reset its state
 * and avoid blocking other gestures after a transform completes.
 */
suspend fun PointerInputScope.detectTransformGesturesWithLifecycle(
    panZoomLock: Boolean = false,
    onGestureStart: () -> Unit = {},
    onGesture: (centroid: Offset, pan: Offset, zoom: Float, rotation: Float) -> Unit,
    onGestureEnd: () -> Unit = {}
) {
    awaitEachGesture {
        var rotation = 0f
        var zoom = 1f
        var pan = Offset.Zero
        var pastTouchSlop = false
        val touchSlop = viewConfiguration.touchSlop
        var lockedToPanZoom = false
        
        // Track if gesture has started (to call onGestureStart only once)
        var gestureStarted = false

        // Wait for first finger down
        awaitFirstDown(requireUnconsumed = false)
        
        do {
            val event = awaitPointerEvent()
            val canceled = event.changes.any { it.isConsumed }
            
            if (!canceled) {
                val zoomChange = event.calculateZoom()
                val rotationChange = event.calculateRotation()
                val panChange = event.calculatePan()

                if (!pastTouchSlop) {
                    zoom *= zoomChange
                    rotation += rotationChange
                    pan += panChange

                    val centroidSize = event.calculateCentroid(useCurrent = false).getDistance()
                    val zoomMotion = abs(1 - zoom) * centroidSize
                    val rotationMotion = abs(rotation * PI.toFloat() * centroidSize / 180f)
                    val panMotion = pan.getDistance()

                    if (zoomMotion > touchSlop ||
                        rotationMotion > touchSlop ||
                        panMotion > touchSlop
                    ) {
                        pastTouchSlop = true
                        lockedToPanZoom = panZoomLock && rotationMotion < touchSlop
                    }
                }

                if (pastTouchSlop) {
                    // Notify start only once when we have 2+ pointers and passed slop
                    if (!gestureStarted && event.changes.size >= 2) {
                        gestureStarted = true
                        onGestureStart()
                    }
                    
                    val centroid = event.calculateCentroid(useCurrent = false)
                    val effectiveRotation = if (lockedToPanZoom) 0f else rotationChange
                    if (effectiveRotation != 0f ||
                        zoomChange != 1f ||
                        panChange != Offset.Zero
                    ) {
                        onGesture(centroid, panChange, zoomChange, effectiveRotation)
                    }
                    event.changes.forEach {
                        if (it.positionChanged()) {
                            it.consume()
                        }
                    }
                }
            }
        } while (!canceled && event.changes.any { it.pressed })
        
        // Notify end when all fingers are released
        if (gestureStarted) {
            onGestureEnd()
        }
    }
}
