package com.fmaestre98.pdfviewer.pdfViewer.overlay

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.math.min
import kotlin.math.roundToInt

private val MAGNIFIER_DIAMETER_DP = 110.dp

/** How many dp above the dragged handle the center of the bubble sits. */
private val MAGNIFIER_LIFT_DP = 90.dp

/**
 * Fraction of the rendered page content width captured in the lens.
 * Smaller value = higher zoom. 0.18 ≈ ~5x zoom on a typical page.
 */
private const val MAGNIFIER_SOURCE_FRACTION = 0.15f

private val BORDER_WIDTH_DP = 2.5.dp
private val BORDER_COLOR = Color(0xFF007AFF)
private val SHADOW_COLOR  = Color(0x33000000)

/**
 * A circular magnifier bubble that shows the page content magnified around
 * the currently dragged selection handle.
 *
 * Coordinate mapping:
 *  The overlay can be larger than the rendered page content (e.g. a landscape page centered
 *  inside a full-screen overlay). [pdfRectToScreen] bakes in centering offsets when producing
 *  handle positions. This composable reverses those offsets before indexing into the bitmap so
 *  the content shown in the lens is exactly what is under the handle tip.
 *
 * @param handlePosition  Drag position in overlay pixel space (with centering offsets included).
 * @param overlaySize     Overlay pixel dimensions (from `onSizeChanged`).
 * @param bitmap          Rendered page bitmap (its pixel dimensions equal the rendered content
 *                        size, which may be smaller than [overlaySize]).
 * @param pageWidth       Logical page width used in [pdfRectToScreen] (optimalSize.first).
 * @param pageHeight      Logical page height used in [pdfRectToScreen] (optimalSize.second).
 */
@Composable
fun SelectionMagnifier(
    isVisible: Boolean,
    handlePosition: Offset,
    overlaySize: IntSize,
    bitmap: ImageBitmap?,
    pageWidth: Int,
    pageHeight: Int,
    /**
     * The zoom level of the containing graphicsLayer.
     * Layout dimensions are divided by this value so the bubble always appears
     * at a constant physical size on screen regardless of the current zoom.
     */
    scaleFactor: Float = 1f
) {
    val density = LocalDensity.current

    val scale by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness    = Spring.StiffnessMedium
        ),
        label = "magnifier_scale"
    )

    if (scale < 0.01f) return

    // Divide all layout dimensions by a softened zoom factor so its visual size
    // physically grows slightly, meaning its relative reduction compared to the heavily
    // zoomed text is less extreme, exactly as requested.
    val rawZoom = scaleFactor.coerceAtLeast(0.01f)
    val zoom = if (rawZoom > 1f) 1f + (rawZoom - 1f) * 0.35f else rawZoom

    val diameterPx = with(density) { MAGNIFIER_DIAMETER_DP.toPx() } / zoom
    val liftPx     = with(density) { MAGNIFIER_LIFT_DP.toPx()     } / zoom
    val radiusPx   = diameterPx / 2f

    // For the source crop: show slightly less bitmap area when zoomed so the
    // text inside the lens looks a bit larger (user's request).
    // At zoom = 1 → MAGNIFIER_SOURCE_FRACTION; at higher zoom → smaller fraction.
    val sourceFraction = (MAGNIFIER_SOURCE_FRACTION / zoom).coerceIn(0.05f, MAGNIFIER_SOURCE_FRACTION)

    // Layout size in dp (un-scaled so it matches the physical dp the user sees)
    val layoutDiameterDp = MAGNIFIER_DIAMETER_DP / zoom

    // Bubble center in overlay pixel space (above the handle tip)
    val bubbleCenterX = handlePosition.x
    val bubbleCenterY = handlePosition.y - liftPx

    val offsetX = (bubbleCenterX - radiusPx).roundToInt()
    val offsetY = (bubbleCenterY - radiusPx).roundToInt()

    Canvas(
        modifier = Modifier
            .offset { IntOffset(offsetX, offsetY) }
            .size(layoutDiameterDp)  // constant physical size regardless of zoom
    ) {
        val cx = size.width  / 2f
        val cy = size.height / 2f
        val r  = min(size.width, size.height) / 2f * scale   // r already includes scale

        // ── Clip to the animated circle ───────────────────────────────────
        val circlePath = Path().apply {
            addOval(Rect(cx - r, cy - r, cx + r, cy + r))
        }

        clipPath(circlePath) {
            drawCircle(color = Color.White, radius = r, center = Offset(cx, cy))

            if (bitmap != null && overlaySize.width > 0 && overlaySize.height > 0
                && pageWidth > 0 && pageHeight > 0
            ) {
                // ── Undo the centering offset that pdfRectToScreen applied ─
                // The overlay may be larger than the rendered page content.
                // The bitmap starts at (0,0) without any offset.
                val overlayW    = overlaySize.width.toFloat()
                val overlayH    = overlaySize.height.toFloat()
                val baseScale   = min(overlayW / pageWidth, overlayH / pageHeight)
                val renderedW   = pageWidth  * baseScale   // actual content px inside overlay
                val renderedH   = pageHeight * baseScale
                val centerOffX  = (overlayW - renderedW) / 2f
                val centerOffY  = (overlayH - renderedH) / 2f

                // Handle position in "content space" (0..renderedW × 0..renderedH)
                val contentX = (handlePosition.x - centerOffX).coerceIn(0f, renderedW)
                val contentY = (handlePosition.y - centerOffY).coerceIn(0f, renderedH)

                // Map content-space position to bitmap pixel coords
                val bitmapScaleX = bitmap.width.toFloat()  / renderedW
                val bitmapScaleY = bitmap.height.toFloat() / renderedH
                val bmpX = contentX * bitmapScaleX
                val bmpY = contentY * bitmapScaleY

                // ── Source crop (in bitmap pixels) ────────────────────────
                // sourceFraction shrinks with zoom → text inside lens looks larger.
                val srcDiameter = bitmap.width * sourceFraction
                val halfSrc = srcDiameter / 2f
                val srcLeft = (bmpX - halfSrc).coerceIn(0f, (bitmap.width  - srcDiameter).coerceAtLeast(0f))
                val srcTop  = (bmpY - halfSrc).coerceIn(0f, (bitmap.height - srcDiameter).coerceAtLeast(0f))
                val srcW    = srcDiameter.roundToInt().coerceAtLeast(1)
                val srcH    = srcDiameter.roundToInt().coerceAtLeast(1)

                // ── Destination: fill the full circle diameter ─────────────
                // r already includes `scale`, so (r * 2) is the animated circle diameter.
                val dstDim  = (r * 2f).roundToInt().coerceAtLeast(1)
                val dstLeft = (cx - dstDim / 2f).roundToInt()
                val dstTop  = (cy - dstDim / 2f).roundToInt()

                drawImage(
                    image     = bitmap,
                    srcOffset = IntOffset(srcLeft.roundToInt(), srcTop.roundToInt()),
                    srcSize   = IntSize(srcW, srcH),
                    dstOffset = IntOffset(dstLeft, dstTop),
                    dstSize   = IntSize(dstDim, dstDim)
                )
            } else {
                // Placeholder while bitmap is loading
                drawCircle(color = Color(0xFFD6E8FF), radius = r, center = Offset(cx, cy))
            }
        }

        // ── Outer shadow ───────────────────────────────────────────────────
        drawCircle(
            color  = SHADOW_COLOR.copy(alpha = SHADOW_COLOR.alpha * scale),
            radius = r + with(density) { 4.dp.toPx() },
            center = Offset(cx, cy),
            style  = Stroke(width = with(density) { 4.dp.toPx() })
        )

        // ── Border ring ────────────────────────────────────────────────────
        drawCircle(
            color  = BORDER_COLOR.copy(alpha = scale),
            radius = r - with(density) { BORDER_WIDTH_DP.toPx() } / 2f,
            center = Offset(cx, cy),
            style  = Stroke(width = with(density) { BORDER_WIDTH_DP.toPx() })
        )

        // ── Crosshair at center ────────────────────────────────────────────
        val crossLen    = r * 0.18f * scale
        val crossColor  = BORDER_COLOR.copy(alpha = 0.5f * scale)
        val crossStroke = with(density) { 1.5.dp.toPx() }
        drawLine(crossColor, Offset(cx - crossLen, cy), Offset(cx + crossLen, cy), crossStroke)
        drawLine(crossColor, Offset(cx, cy - crossLen), Offset(cx, cy + crossLen), crossStroke)

        // ── Teardrop stem pointing to the handle ───────────────────────────
        if (scale > 0.3f) {
            val stemAlpha = ((scale - 0.3f) / 0.7f).coerceIn(0f, 1f)
            val stemW     = r * 0.2f
            val stemTip   = r + with(density) { 7.dp.toPx() }
            val stemPath  = Path().apply {
                moveTo(cx - stemW, cy + r - with(density) { 1.dp.toPx() })
                lineTo(cx + stemW, cy + r - with(density) { 1.dp.toPx() })
                lineTo(cx,         cy + stemTip)
                close()
            }
            drawPath(stemPath, color = BORDER_COLOR.copy(alpha = stemAlpha))
        }
    }
}
