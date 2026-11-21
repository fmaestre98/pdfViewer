package com.fmaestre98.pdfviewer.sdk.core.overlay

import android.graphics.RectF
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp

import com.fmaestre98.pdfviewer.sdk.core.gestures.GestureHandler
import com.fmaestre98.pdfviewer.sdk.core.viewmodel.PdfInteractiveState
import com.fmaestre98.pdfviewer.sdk.core.model.PdfChar
import com.fmaestre98.pdfviewer.sdk.core.rendering.PdfPageRenderer
import com.fmaestre98.pdfviewer.ui.theme.PdfViewerTheme
import kotlin.math.max
import kotlin.math.min

private enum class HandleType { NONE, START, END }

/**
 * Overlay that draws selection highlights and provides draggable handles.
 * Coordinates are in overlay-pixel space (matches rendered bitmap dimensions).
 *
 * @param pageRenderer  Used to retrieve the page bitmap for the magnifier.
 * @param pageIndex     Index of the page currently showing the selection.
 * @param optimalPageSize  Pre-calculated (width, height) for the page in pixels.
 */
@Composable
fun TextSelectionOverlay(
    interactiveState: PdfInteractiveState,
    gestureHandler: GestureHandler,
    pageWidth: Int,
    pageHeight: Int,
    scaleFactor: Float = 1f,
    modifier: Modifier = Modifier,
    onHighlightRequested: ((color: String) -> Unit)? = null,
    // Magnifier support
    pageRenderer: PdfPageRenderer? = null,
    pageIndex: Int = 0,
    optimalPageSize: Pair<Int, Int>? = null,
) {
    val startCharCurrent = interactiveState.selectionStartChar
    val endCharCurrent = interactiveState.selectionEndChar
    if (startCharCurrent == null || endCharCurrent == null) return
    if (!interactiveState.isSelectionActiveOnPage(pageIndex)) return

    val pageModel = interactiveState.getPageModel(pageIndex) ?: return
    val optimizedModel = interactiveState.getOptimizedPageModel(pageIndex)

    val density = LocalDensity.current

    // Touch target / visual sizes
    val touchTargetDp = 60.dp
    val visualHandleSize = 24.dp

    // Overlay real size in px
    var overlaySize by remember { mutableStateOf(IntSize(1, 1)) }

    // Painter instance
    val selectionPainter = remember { SelectionPainter() }

    val startPage = interactiveState.selectionStartPageIndex
    val endPage = interactiveState.selectionEndPageIndex
    val showStartHandle = (startPage == pageIndex)
    val showEndHandle = (endPage == pageIndex)

    // ── Magnifier bitmap ────────────────────────────────────────────────────
    var magnifierBitmap by remember { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(pageRenderer, pageIndex, optimalPageSize) {
        if (pageRenderer != null && optimalPageSize != null) {
            val (w, h) = optimalPageSize
            val originalSize = pageRenderer.getPageSize(pageIndex)
            if (originalSize != null) {
                val (origW, origH) = originalSize
                val sf = minOf(w.toFloat() / origW, h.toFloat() / origH)
                val rendered = pageRenderer.renderPage(
                    pageIndex = pageIndex,
                    scaleFactor = sf,
                    maxWidth = w,
                    maxHeight = h
                )
                magnifierBitmap = rendered?.bitmap?.asImageBitmap()
            }
        }
    }

    // ── Coordinate conversion ───────────────────────────────────────────────
    fun pdfRectToScreen(rect: RectF, overlayW: Float, overlayH: Float): Rect {
        val scaleX = overlayW / pageWidth
        val scaleY = overlayH / pageHeight
        val baseScale = min(scaleX, scaleY)
        val renderedWidth = pageWidth * baseScale
        val renderedHeight = pageHeight * baseScale
        val centeringOffsetX = (overlayW - renderedWidth) / 2f
        val centeringOffsetY = (overlayH - renderedHeight) / 2f

        return Rect(
            left   = rect.left   * baseScale + centeringOffsetX,
            top    = rect.top    * baseScale + centeringOffsetY,
            right  = rect.right  * baseScale + centeringOffsetX,
            bottom = rect.bottom * baseScale + centeringOffsetY
        )
    }

    fun compareChars(a: PdfChar, b: PdfChar): Int {
        val lineCmp = a.lineId.compareTo(b.lineId)
        if (lineCmp != 0) return lineCmp
        val wordCmp = a.wordId.compareTo(b.wordId)
        if (wordCmp != 0) return wordCmp
        return a.id.compareTo(b.id)
    }

    // Determine characters for this page's highlight
    val pageFirstChar = optimizedModel?.firstChar ?: pageModel.coordinates.firstOrNull()?.words?.firstOrNull()?.characters?.firstOrNull()
    val pageLastChar = optimizedModel?.lastChar ?: pageModel.coordinates.lastOrNull()?.words?.lastOrNull()?.characters?.lastOrNull()

    val (drawStartChar, drawEndChar) = remember(startCharCurrent, endCharCurrent, startPage, endPage, pageIndex, pageFirstChar, pageLastChar) {
        if (startPage == endPage) {
            startCharCurrent to endCharCurrent
        } else if (startPage < endPage) {
            when (pageIndex) {
                startPage -> startCharCurrent to pageLastChar
                endPage -> pageFirstChar to endCharCurrent
                else -> pageFirstChar to pageLastChar
            }
        } else {
            when (pageIndex) {
                startPage -> pageFirstChar to startCharCurrent
                endPage -> endCharCurrent to pageLastChar
                else -> pageFirstChar to pageLastChar
            }
        }
    }

    // ── Handle visual positions (anchored to character bounds) ──────────────
    var snappedStart by remember { mutableStateOf(Offset.Zero) }
    var snappedEnd   by remember { mutableStateOf(Offset.Zero) }

    var dragPositionStart by remember { mutableStateOf(Offset.Zero) }
    var dragPositionEnd   by remember { mutableStateOf(Offset.Zero) }

    var draggingHandle by remember { mutableStateOf(HandleType.NONE) }

    LaunchedEffect(startCharCurrent, endCharCurrent, overlaySize, showStartHandle, showEndHandle) {
        val w = overlaySize.width.toFloat()
        val h = overlaySize.height.toFloat()
        if (w <= 0f || h <= 0f) return@LaunchedEffect

        if (showStartHandle && startCharCurrent != null) {
            val startRect = pdfRectToScreen(startCharCurrent.rect, w, h)
            snappedStart = startRect.bottomLeft
            if (draggingHandle != HandleType.START) dragPositionStart = snappedStart
        } else {
            snappedStart = Offset.Zero
        }

        if (showEndHandle && endCharCurrent != null) {
            val endRect = pdfRectToScreen(endCharCurrent.rect, w, h)
            snappedEnd = endRect.bottomRight
            if (draggingHandle != HandleType.END) dragPositionEnd = snappedEnd
        } else {
            snappedEnd = Offset.Zero
        }
    }

    Box(modifier = modifier) {
        // ── Highlight canvas ────────────────────────────────────────────────
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            if (overlaySize.width != w.toInt() || overlaySize.height != h.toInt()) {
                overlaySize = IntSize(w.toInt(), h.toInt())
            }

            if (optimizedModel != null && drawStartChar != null && drawEndChar != null) {
                val (minChar, maxChar) =
                    if (compareChars(drawStartChar, drawEndChar) <= 0) drawStartChar to drawEndChar else drawEndChar to drawStartChar
                selectionPainter.drawSelection(
                    canvas             = drawContext.canvas,
                    startChar          = minChar,
                    endChar            = maxChar,
                    optimizedPageModel = optimizedModel,
                    pageWidth          = pageWidth,
                    pageHeight         = pageHeight,
                    overlaySize        = size
                )
            }
        }

        // ── Selection Handles ───────────────────────────────────────────────
        val isDragging = interactiveState.isDraggingHandle
        val draggingIsStart = interactiveState.draggingHandleIsStart

        // START HANDLE:
        // - Gesture handler: this page overlay if draggingHandle == HandleType.START
        // - Selection start page: this page overlay if showStartHandle == true
        val isStartGestureHandler = (draggingHandle == HandleType.START)
        val shouldRenderStart = showStartHandle || isStartGestureHandler

        if (shouldRenderStart) {
            val handlePos = if (isStartGestureHandler && !showStartHandle) dragPositionStart else snappedStart
            if (handlePos != Offset.Zero) {
                SelectionHandleTeardrop(
                    isStart = true,
                    position = handlePos,
                    touchSize = touchTargetDp / scaleFactor,
                    visualSize = visualHandleSize / scaleFactor,
                    isVisible = showStartHandle,
                    isTouchEnabled = isStartGestureHandler || (!isDragging && showStartHandle),
                    onDragStart = {
                        interactiveState.isDraggingHandle = true
                        interactiveState.draggingHandleIsStart = true
                        draggingHandle = HandleType.START
                        gestureHandler.notifyHandleDragStarted()
                        dragPositionStart = snappedStart
                    },
                    onDragDelta = { delta ->
                        dragPositionStart += delta
                        gestureHandler.updateSelectionHandle(
                            isStart    = true,
                            x          = dragPositionStart.x,
                            y          = dragPositionStart.y,
                            viewWidth  = overlaySize.width,
                            viewHeight = overlaySize.height,
                            sourcePage = pageIndex
                        )
                    },
                    onDragEnd = {
                        interactiveState.isDraggingHandle = false
                        interactiveState.draggingHandleIsStart = null
                        draggingHandle = HandleType.NONE
                        gestureHandler.notifyHandleDragEnded()
                    }
                )
            }
        }

        // END HANDLE:
        // - Gesture handler: this page overlay if draggingHandle == HandleType.END
        // - Selection end page: this page overlay if showEndHandle == true
        val isEndGestureHandler = (draggingHandle == HandleType.END)
        val shouldRenderEnd = showEndHandle || isEndGestureHandler

        if (shouldRenderEnd) {
            val handlePos = if (isEndGestureHandler && !showEndHandle) dragPositionEnd else snappedEnd
            if (handlePos != Offset.Zero) {
                SelectionHandleTeardrop(
                    isStart = false,
                    position = handlePos,
                    touchSize = touchTargetDp / scaleFactor,
                    visualSize = visualHandleSize / scaleFactor,
                    isVisible = showEndHandle,
                    isTouchEnabled = isEndGestureHandler || (!isDragging && showEndHandle),
                    onDragStart = {
                        interactiveState.isDraggingHandle = true
                        interactiveState.draggingHandleIsStart = false
                        draggingHandle = HandleType.END
                        gestureHandler.notifyHandleDragStarted()
                        dragPositionEnd = snappedEnd
                    },
                    onDragDelta = { delta ->
                        dragPositionEnd += delta
                        gestureHandler.updateSelectionHandle(
                            isStart    = false,
                            x          = dragPositionEnd.x,
                            y          = dragPositionEnd.y,
                            viewWidth  = overlaySize.width,
                            viewHeight = overlaySize.height,
                            sourcePage = pageIndex
                        )
                    },
                    onDragEnd = {
                        interactiveState.isDraggingHandle = false
                        interactiveState.draggingHandleIsStart = null
                        draggingHandle = HandleType.NONE
                        gestureHandler.notifyHandleDragEnded()
                    }
                )
            }
        }

        // ── Magnifier ───────────────────────────────────────────────────────
        // Magnifier shows on the active handle's current page (showStartHandle / showEndHandle)
        val isMagnifierForThisPage = if (isDragging) {
            if (draggingIsStart == true) showStartHandle
            else showEndHandle
        } else false

        val magnifierVisible = isMagnifierForThisPage && scaleFactor <= 1f
        val magnifierAnchor = if (draggingIsStart == true) snappedStart else snappedEnd

        SelectionMagnifier(
            isVisible      = magnifierVisible,
            handlePosition = magnifierAnchor,
            overlaySize    = overlaySize,
            bitmap         = magnifierBitmap,
            pageWidth      = pageWidth,
            pageHeight     = pageHeight,
            scaleFactor    = scaleFactor,
        )
    }
}

/**
 * Improved handle (teardrop shape) with unified gesture priority.
 */
@Composable
private fun SelectionHandleTeardrop(
    isStart: Boolean,
    position: Offset,
    touchSize: androidx.compose.ui.unit.Dp,
    visualSize: androidx.compose.ui.unit.Dp,
    isVisible: Boolean = true,
    isTouchEnabled: Boolean = true,
    onDragStart: () -> Unit = {},
    onDragDelta: (Offset) -> Unit = {},
    onDragEnd: () -> Unit = {}
) {
    val density = LocalDensity.current
    val touchRadiusPx = with(density) { (touchSize / 2).toPx() }
    val visualSizePx  = with(density) { visualSize.toPx() }
    val handleColor   = PdfViewerTheme.selectionHandle

    var boxModifier = Modifier
        .offset {
            IntOffset(
                x = (position.x - touchRadiusPx).toInt(),
                y = (position.y - touchRadiusPx).toInt()
            )
        }
        .size(touchSize)

    if (isTouchEnabled) {
        boxModifier = boxModifier.pointerInput(Unit) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                down.consume()   // Claim touch to block parent gestures
                onDragStart()

                try {
                    while (true) {
                        val event = awaitPointerEvent()
                        if (event.changes.all { !it.pressed }) break
                        event.changes.forEach { change ->
                            if (change.positionChanged()) {
                                onDragDelta(change.positionChange())
                                change.consume()
                            }
                        }
                    }
                } finally {
                    onDragEnd()
                }
            }
        }
    }

    Box(
        modifier = boxModifier,
        contentAlignment = Alignment.Center
    ) {
        if (isVisible) {
            Canvas(modifier = Modifier.size(visualSize)) {
                val cx = size.width  / 2
                val cy = size.height / 2
                val radius = visualSizePx / 2f

                if (isStart) {
                    // Left handle: circle bottom-left + triangle
                    drawCircle(
                        color  = handleColor,
                        radius = radius,
                        center = Offset(cx - radius, cy + radius)
                    )
                    val tri = Path().apply {
                        moveTo(cx, cy)
                        lineTo(cx - radius, cy)
                        lineTo(cx, cy + radius)
                        close()
                    }
                    drawPath(tri, color = handleColor)
                } else {
                    // Right handle: circle bottom-right + triangle
                    drawCircle(
                        color  = handleColor,
                        radius = radius,
                        center = Offset(cx + radius, cy + radius)
                    )
                    val tri = Path().apply {
                        moveTo(cx, cy)
                        lineTo(cx + radius, cy)
                        lineTo(cx, cy + radius)
                        close()
                    }
                    drawPath(tri, color = handleColor)
                }
            }
        }
    }
}
