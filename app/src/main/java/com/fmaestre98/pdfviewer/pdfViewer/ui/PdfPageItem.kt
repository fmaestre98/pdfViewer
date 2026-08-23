package com.fmaestre98.pdfviewer.pdfViewer.ui

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.fmaestre98.pdfviewer.pdfViewer.rendering.PdfPageRenderer


/**
 * Composable that renders a single PDF page.
 * Handles asynchronous rendering with loading state and optimal size calculation.
 *
 * @param pageIndex Zero-based index of the page to render
 * @param pageRenderer The renderer for PDF pages
 * @param optimalSize Pre-calculated optimal size for this page (width, height)
 * @param modifier Modifier for the component
 * @param sharedTransitionScope Shared transition scope for hero animation
 * @param animatedVisibilityScope Animated visibility scope for hero animation
 * @param drawableResName Drawable resource name for first page cover image
 * @param assetPath Asset path for shared element key
 */
private val DarkReaderColorMatrix = androidx.compose.ui.graphics.ColorMatrix(
    floatArrayOf(
        -1f,  0f,  0f, 0f, 255f,
         0f, -1f,  0f, 0f, 255f,
         0f,  0f, -1f, 0f, 255f,
         0f,  0f,  0f, 1f,   0f
    )
)

/**
 * Composable that renders a single PDF page.
 * Handles asynchronous rendering with loading state and optimal size calculation.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun PdfPageItem(
    pageIndex: Int,
    pageRenderer: PdfPageRenderer?,
    optimalSize: Pair<Int, Int>?,
    modifier: Modifier = Modifier,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null,
    drawableResName: String? = null,
    assetPath: String? = null,
    isDarkReaderMode: Boolean = false,
    searchHighlights: List<android.graphics.RectF> = emptyList(),
) {
    val context = LocalContext.current
    
    // Get drawable resource ID for first page
    val drawableId = remember(drawableResName) {
        if (pageIndex == 0 && drawableResName != null) {
            context.resources.getIdentifier(
                drawableResName,
                "drawable",
                context.packageName
            )
        } else {
            0
        }
    }
    
    val hasDrawable = drawableId != 0 && pageIndex == 0

    var bitmap by remember(pageIndex, optimalSize) { 
        mutableStateOf<Bitmap?>(null) 
    }
    var isLoading by remember { 
        mutableStateOf(!hasDrawable) 
    }

    LaunchedEffect(pageIndex, pageRenderer, optimalSize) {
        if (pageRenderer != null && optimalSize != null) {
            val renderedBitmap = renderPageWithOptimalSize(
                pageRenderer = pageRenderer,
                pageIndex = pageIndex,
                optimalSize = optimalSize
            )
            if (renderedBitmap != null) {
                bitmap = renderedBitmap
            }
        }
        isLoading = false
    }

    val density = LocalDensity.current
    val bgColor = if (isDarkReaderMode) Color(0xFF121212) else Color.White
    val colorFilter = if (isDarkReaderMode) androidx.compose.ui.graphics.ColorFilter.colorMatrix(DarkReaderColorMatrix) else null

    val itemModifier = if (optimalSize != null) {
        with(density) {
            modifier
                .width(optimalSize.first.toDp())
                .height(optimalSize.second.toDp())
                .background(bgColor)
        }
    } else {
        modifier
            .fillMaxSize()
            .background(bgColor)
    }
    Box(
        modifier = itemModifier,
        contentAlignment = Alignment.Center
    ) {
        when {
            isLoading && !hasDrawable && bitmap == null -> {
                CircularProgressIndicator()
            }
            hasDrawable && bitmap == null -> {
                val baseModifier = if (optimalSize != null) {
                    with(density) {
                        Modifier
                            .width(optimalSize.first.toDp())
                            .height(optimalSize.second.toDp())
                    }
                } else {
                    Modifier.fillMaxWidth()
                }
                
                val imageModifier = if (sharedTransitionScope != null && animatedVisibilityScope != null && assetPath != null) {
                    with(sharedTransitionScope) {
                        baseModifier.sharedElement(
                            rememberSharedContentState(key = "cover-$assetPath"),
                            animatedVisibilityScope = animatedVisibilityScope
                        )
                    }
                } else {
                    baseModifier
                }
                
                Image(
                    painter = painterResource(id = drawableId),
                    contentDescription = "Page ${pageIndex + 1}",
                    modifier = imageModifier,
                    colorFilter = colorFilter,
                    contentScale = if (optimalSize != null) ContentScale.Fit else ContentScale.FillWidth,
                )
            }
            bitmap != null -> {
                val baseModifier = if (optimalSize != null) {
                    with(density) {
                        Modifier
                            .width(optimalSize.first.toDp())
                            .height(optimalSize.second.toDp())
                    }
                } else {
                    Modifier.fillMaxWidth()
                }
                
                val imageModifier = if (pageIndex == 0 && sharedTransitionScope != null && animatedVisibilityScope != null && assetPath != null) {
                    with(sharedTransitionScope) {
                        baseModifier.sharedElement(
                            rememberSharedContentState(key = "cover-$assetPath"),
                            animatedVisibilityScope = animatedVisibilityScope
                        )
                    }
                } else {
                    baseModifier
                }
                
                Image(
                    bitmap = bitmap!!.asImageBitmap(),
                    contentDescription = "Page ${pageIndex + 1}",
                    modifier = imageModifier,
                    colorFilter = colorFilter,
                    contentScale = if (optimalSize != null) ContentScale.Fit else ContentScale.FillWidth,
                )
            }
        }

        // In-page search highlight overlays
        if (searchHighlights.isNotEmpty() && optimalSize != null) {
            androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                val (pageW, pageH) = optimalSize
                val scaleX = size.width / pageW.toFloat()
                val scaleY = size.height / pageH.toFloat()

                searchHighlights.forEach { rect ->
                    drawRect(
                        color = Color(0x99FF9800), // Bright semi-transparent orange highlight
                        topLeft = androidx.compose.ui.geometry.Offset(rect.left * scaleX, rect.top * scaleY),
                        size = androidx.compose.ui.geometry.Size(
                            width = rect.width() * scaleX,
                            height = rect.height() * scaleY
                        )
                    )
                }
            }
        }
    }
}

/**
 * Renders a PDF page with optimal size calculation.
 * Returns null if rendering cannot be performed.
 */
private suspend fun renderPageWithOptimalSize(
    pageRenderer: PdfPageRenderer?,
    pageIndex: Int,
    optimalSize: Pair<Int, Int>?
): Bitmap? {
    if (pageRenderer == null || optimalSize == null) {
        return null
    }

    val (optimalWidth, optimalHeight) = optimalSize

    // Calculate scale factor to achieve optimal size
    val originalSize = pageRenderer.getPageSize(pageIndex)
    return if (originalSize != null) {
        val (origWidth, origHeight) = originalSize
        val scaleFactor = minOf(
            optimalWidth.toFloat() / origWidth,
            optimalHeight.toFloat() / origHeight
        )

        pageRenderer.renderPage(
            pageIndex = pageIndex,
            scaleFactor = scaleFactor,
            maxWidth = optimalWidth,
            maxHeight = optimalHeight,
        )?.bitmap
    } else {
        // Fallback to simple rendering if page size cannot be determined
        pageRenderer.renderPage(
            pageIndex = pageIndex,
            scaleFactor = 1f,
            maxWidth = optimalWidth,
        )?.bitmap
    }
}