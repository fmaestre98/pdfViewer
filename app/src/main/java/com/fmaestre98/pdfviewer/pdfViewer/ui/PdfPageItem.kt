package com.fmaestre98.pdfviewer.pdfViewer.ui

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.Image
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
    // If we have drawable for first page, we can show it immediately (no loading state)
    var isLoading by remember { 
        mutableStateOf(!hasDrawable) 
    }

    // Render page with optimal size
    // If we have the drawable for page 0, we still want to load the high-res PDF version
    LaunchedEffect(pageIndex, pageRenderer, optimalSize) {
        // Load high-res PDF version (will replace drawable if it exists)
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
    val itemModifier = if (optimalSize != null) {
        with(density) {
            modifier
                .width(optimalSize.first.toDp())
                .height(optimalSize.second.toDp())
        }
    } else {
        modifier
            .fillMaxSize()
    }
    Box(
        modifier = itemModifier,
        contentAlignment = Alignment.Center
    ) {
        when {
            // Show loading indicator only if we don't have drawable and no bitmap yet
            isLoading && !hasDrawable && bitmap == null -> {
                CircularProgressIndicator()
            }
            // Show drawable for first page if available and PDF not loaded yet
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
                    contentScale = if (optimalSize != null) ContentScale.Fit else ContentScale.FillWidth,
                )
            }
            // Show rendered PDF bitmap (preferred, replaces drawable when loaded)
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
                    contentScale = if (optimalSize != null) ContentScale.Fit else ContentScale.FillWidth,
                )
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