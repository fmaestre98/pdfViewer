package com.fmaestre98.pdfviewer.pdfViewer.overlay

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

import com.fmaestre98.pdfviewer.pdfViewer.gestures.GestureHandler
import com.fmaestre98.pdfviewer.pdfViewer.viewmodel.PdfInteractiveState
import com.fmaestre98.pdfviewer.pdfViewer.model.PdfChar
import com.fmaestre98.pdfviewer.pdfViewer.rendering.PdfPageRenderer
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
    val pageModel = interactiveState.getPageModel(interactiveState.selectionPageIndex) ?: return

    // Get optimized model for efficient lookups and drawing
    val optimizedModel = interactiveState.getOptimizedPageModel(interactiveState.selectionPageIndex)

    val density = LocalDensity.current

    // Touch target / visual sizes
    val touchTargetDp = 60.dp
    val visualHandleSize = 24.dp

    // Overlay real size in px
    var overlaySize by remember { mutableStateOf(IntSize(1, 1)) }

    // Painter instance
    val selectionPainter = remember { SelectionPainter() }

    // ── Magnifier bitmap ────────────────────────────────────────────────────
    // We load the already-cached bitmap from the renderer when a handle drag starts.
    var magnifierBitmap by remember { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(pageRenderer, pageIndex, optimalPageSize) {
        if (pageRenderer != null && optimalPageSize != null) {
            val (w, h) = optimalPageSize
            // renderPage hit the internal cache on subsequent calls, so this is fast.
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

    // Helper to compare order between chars
    fun compareChars(a: PdfChar, b: PdfChar): Int {
        val lineCmp = a.lineId.compareTo(b.lineId)
        if (lineCmp != 0) return lineCmp
        val wordCmp = a.wordId.compareTo(b.wordId)
        if (wordCmp != 0) return wordCmp
        return a.id.compareTo(b.id)
    }

    // ── Handle visual positions (anchored to character bounds) ──────────────
    var snappedStart by remember { mutableStateOf(Offset.Zero) }
    var snappedEnd   by remember { mutableStateOf(Offset.Zero) }

    // Virtual drag positions (accumulates deltas)
    var dragPositionStart by remember { mutableStateOf(Offset.Zero) }
    var dragPositionEnd   by remember { mutableStateOf(Offset.Zero) }

    // Which handle is currently dragged
    var draggingHandle by remember { mutableStateOf(HandleType.NONE) }

    // Sync visual positions when chars or overlay size changes
    LaunchedEffect(startCharCurrent, endCharCurrent, overlaySize) {
        val w = overlaySize.width.toFloat()
        val h = overlaySize.height.toFloat()
        if (w <= 0f || h <= 0f) return@LaunchedEffect

        val startRect = pdfRectToScreen(startCharCurrent.rect, w, h)
        snappedStart = startRect.bottomLeft

        val endRect = pdfRectToScreen(endCharCurrent.rect, w, h)
        snappedEnd = endRect.bottomRight

        if (draggingHandle != HandleType.START) dragPositionStart = snappedStart
        if (draggingHandle != HandleType.END)   dragPositionEnd   = snappedEnd
    }

    Box(modifier = modifier) {
        // ── Highlight canvas ────────────────────────────────────────────────
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            if (overlaySize.width != w.toInt() || overlaySize.height != h.toInt()) {
                overlaySize = IntSize(w.toInt(), h.toInt())
            }

            if (optimizedModel != null) {
                val start = interactiveState.selectionStartChar
                val end   = interactiveState.selectionEndChar
                if (start != null && end != null) {
                    val (minChar, maxChar) =
                        if (compareChars(start, end) <= 0) start to end else end to start
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
            } else {
                // Fallback (should be rare)
                val allChars = mutableListOf<PdfChar>()
                pageModel.coordinates.forEach { line ->
                    line.words.forEach { word ->
                        word.characters.forEach { ch -> allChars.add(ch) }
                    }
                }
                val sChar = interactiveState.selectionStartChar
                val eChar = interactiveState.selectionEndChar
                if (sChar != null && eChar != null) {
                    val startIndex = allChars.indexOf(sChar)
                    val endIndex   = allChars.indexOf(eChar)
                    if (startIndex >= 0 && endIndex >= 0) {
                        val low  = min(startIndex, endIndex)
                        val high = max(startIndex, endIndex)
                        for (i in low..high) {
                            val ch   = allChars[i]
                            val rect = pdfRectToScreen(ch.rect, w, h)
                            drawRect(
                                color   = PdfViewerTheme.selectionHighlight,
                                topLeft = rect.topLeft,
                                size    = rect.size
                            )
                        }
                    }
                }
            }
        }

        // ── Selection Handles ───────────────────────────────────────────────
        if (snappedStart != Offset.Zero) {
            SelectionHandleTeardrop(
                isStart = true,
                position = snappedStart,
                touchSize = touchTargetDp,
                visualSize = visualHandleSize,
                onDragStart = {
                    interactiveState.isDraggingHandle = true
                    draggingHandle = HandleType.START
                    gestureHandler.notifyHandleDragStarted()
                    dragPositionStart = snappedStart
                },
                onDragDelta = { delta ->
                    val scaledDelta = delta / scaleFactor
                    dragPositionStart += scaledDelta
                    gestureHandler.updateSelectionHandle(
                        isStart    = true,
                        x          = dragPositionStart.x,
                        y          = dragPositionStart.y,
                        viewWidth  = overlaySize.width,
                        viewHeight = overlaySize.height
                    )
                },
                onDragEnd = {
                    interactiveState.isDraggingHandle = false
                    draggingHandle = HandleType.NONE
                    gestureHandler.notifyHandleDragEnded()
                }
            )
        }

        if (snappedEnd != Offset.Zero) {
            SelectionHandleTeardrop(
                isStart = false,
                position = snappedEnd,
                touchSize = touchTargetDp,
                visualSize = visualHandleSize,
                onDragStart = {
                    interactiveState.isDraggingHandle = true
                    draggingHandle = HandleType.END
                    gestureHandler.notifyHandleDragStarted()
                    dragPositionEnd = snappedEnd
                },
                onDragDelta = { delta ->
                    val scaledDelta = delta / scaleFactor
                    dragPositionEnd += scaledDelta
                    gestureHandler.updateSelectionHandle(
                        isStart    = false,
                        x          = dragPositionEnd.x,
                        y          = dragPositionEnd.y,
                        viewWidth  = overlaySize.width,
                        viewHeight = overlaySize.height
                    )
                },
                onDragEnd = {
                    interactiveState.isDraggingHandle = false
                    draggingHandle = HandleType.NONE
                    gestureHandler.notifyHandleDragEnded()
                }
            )
        }

        // ── Magnifier ───────────────────────────────────────────────────────
        // The magnifier follows the snapped handle position (the actual selected character)
        // rather than the raw finger position. This way, when the finger moves past the
        // last available character and the handle stops, the magnifier stays with the handle.
        val magnifierVisible = draggingHandle != HandleType.NONE
        val magnifierAnchor = when (draggingHandle) {
            HandleType.START -> snappedStart
            HandleType.END   -> snappedEnd
            HandleType.NONE  -> {
                // Hold last known position during the collapse animation
                if (snappedStart != Offset.Zero) snappedStart else snappedEnd
            }
        }

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
    onDragStart: () -> Unit,
    onDragDelta: (Offset) -> Unit,
    onDragEnd: () -> Unit
) {
    val density = LocalDensity.current
    val touchRadiusPx = with(density) { (touchSize / 2).toPx() }
    val visualSizePx  = with(density) { visualSize.toPx() }
    val handleColor   = PdfViewerTheme.selectionHandle

    Box(
        modifier = Modifier
            .offset {
                IntOffset(
                    x = (position.x - touchRadiusPx).toInt(),
                    y = (position.y - touchRadiusPx).toInt()
                )
            }
            .size(touchSize)
            // Unified gesture: claims the touch immediately then tracks drag deltas.
            .pointerInput(isStart) {
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
            },
        contentAlignment = Alignment.Center
    ) {
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
