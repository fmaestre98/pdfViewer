package com.fmaestre98.pdfviewer.pdfViewer.ui

import android.util.Size
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import com.fmaestre98.pdfviewer.pdfViewer.model.PageModel
import com.fmaestre98.pdfviewer.pdfViewer.model.PdfViewerOrientation
import com.fmaestre98.pdfviewer.pdfViewer.viewmodel.PdfViewerViewModel
import com.fmaestre98.pdfviewer.pdfViewer.viewmodel.PdfLoaderState
import com.fmaestre98.pdfviewer.pdfViewer.viewmodel.PdfNavigationState
import com.fmaestre98.pdfviewer.pdfViewer.viewmodel.PdfInteractiveState
import com.fmaestre98.pdfviewer.pdfViewer.rendering.PdfPageRenderer
import com.fmaestre98.pdfviewer.pdfViewer.utils.FitPolicy
import com.fmaestre98.pdfviewer.pdfViewer.utils.PageSizeCalculator
import java.io.File

/**
 * Main Compose PDF viewer component.
 * Uses LazyColumn for efficient rendering with smooth scrolling.
 *
 * @param pdfFile The PDF file to display
 * @param modifier Modifier for the component
 * @param onError Callback for error handling
 * @param onNavigationStateChange Callback to expose navigation state (current page and navigation function)
 * @param onRendererReady Callback when page renderer is ready (for thumbnail drawer)
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun ComposePdfViewer(
    pdfFile: File,
    modifier: Modifier = Modifier,
    initialPage: Int = 0,
    onError: ((Throwable) -> Unit)? = null,
    orientation: PdfViewerOrientation = PdfViewerOrientation.Vertical,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null,
    drawableResName: String? = null,
    assetPath: String? = null,
    onNavigationStateChange: ((Int, (Int) -> Unit) -> Unit)? = null,
    onRendererReady: ((PdfPageRenderer?, Int) -> Unit)? = null,
    enableTextExtraction: Boolean = false,
    onTextExtracted: ((Int, PageModel?) -> Unit)? = null,
    onHighlightRequested: ((color: String, page: Int, snippet: String) -> Unit)? = null,
    viewModel: PdfViewerViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val density = LocalDensity.current

    // Observe ViewModel state (mutableStateOf is automatically observed by Compose)
    val loaderState = viewModel.loaderState
    val navigationState = viewModel.navigationState
    val interactiveState = viewModel.interactiveState

    // Set initial page when first loaded
    LaunchedEffect(initialPage) {
        viewModel.setCurrentPage(initialPage)
    }

    // Load PDF when file or text extraction setting changes
    LaunchedEffect(pdfFile, enableTextExtraction) {
        try {
            viewModel.loadPdf(pdfFile, context, enableTextExtraction)
        } catch (e: Exception) {
            onError?.invoke(e)
        }
    }

    // Notify when renderer is ready
    LaunchedEffect(viewModel.getPageRenderer(), loaderState.pageCount) {
        onRendererReady?.invoke(viewModel.getPageRenderer(), loaderState.pageCount)
    }

    when {
        loaderState.isLoading -> {
            Box(
                modifier = modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }

        loaderState.error != null -> {
            Box(
                modifier = modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Error loading PDF")
                    Text(
                        text = loaderState.error?.message ?: "Unknown error",
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }

        loaderState.pageCount > 0 -> {
            PdfViewerContent(
                viewModel = viewModel,
                loaderState = loaderState,
                navigationState = navigationState,
                interactiveState = interactiveState,
                modifier = modifier,
                density = density,
                orientation = orientation,
                sharedTransitionScope = sharedTransitionScope,
                animatedVisibilityScope = animatedVisibilityScope,
                drawableResName = drawableResName,
                assetPath = assetPath,
                onNavigationStateChange = onNavigationStateChange,
                onHighlightRequested = onHighlightRequested,
            )
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun PdfViewerContent(
    viewModel: PdfViewerViewModel,
    loaderState: PdfLoaderState,
    navigationState: PdfNavigationState,
    interactiveState: PdfInteractiveState,
    modifier: Modifier,
    density: Density,
    orientation: PdfViewerOrientation,
    sharedTransitionScope: SharedTransitionScope?,
    animatedVisibilityScope: AnimatedVisibilityScope?,
    drawableResName: String?,
    assetPath: String?,
    onNavigationStateChange: ((Int, (Int) -> Unit) -> Unit)? = null,
    onHighlightRequested: ((color: String, page: Int, snippet: String) -> Unit)? = null,
) {
    val configuration = LocalConfiguration.current
    val screenWidth = with(density) { configuration.screenWidthDp.dp.toPx() }
    val screenHeight = with(density) { configuration.screenHeightDp.dp.toPx() }

    val pageRenderer = viewModel.getPageRenderer()

    // Store optimal page sizes calculated using PageSizeCalculator
    val optimalPageSizes = remember { mutableStateOf<List<Pair<Int, Int>>?>(null) }

    // Calculate optimal page sizes when renderer is available
    LaunchedEffect(pageRenderer, loaderState.pageCount, screenWidth, screenHeight, orientation) {
        optimalPageSizes.value = calculateOptimalPageSizes(
            pageRenderer = pageRenderer,
            pageCount = loaderState.pageCount,
            screenWidth = screenWidth,
            screenHeight = screenHeight,
            orientation = orientation
        )
    }
    if (orientation == PdfViewerOrientation.Vertical) {
        VerticalPdfView(
            viewModel = viewModel,
            loaderState = loaderState,
            navigationState = navigationState,
            interactiveState = interactiveState,
            optimalPageSizes = optimalPageSizes,
            modifier = modifier,
            screenWidth = screenWidth,
            screenHeight = screenHeight,
            density = density,
            configuration = configuration,
            pageRenderer = pageRenderer,
            sharedTransitionScope = sharedTransitionScope,
            animatedVisibilityScope = animatedVisibilityScope,
            drawableResName = drawableResName,
            assetPath = assetPath,
            onNavigationStateChange = onNavigationStateChange,
            onHighlightRequested = onHighlightRequested,
        )
    } else {
        HorizontalPdfView(
            viewModel = viewModel,
            loaderState = loaderState,
            navigationState = navigationState,
            interactiveState = interactiveState,
            pageRenderer = pageRenderer,
            modifier = modifier,
            screenWidth = screenWidth,
            screenHeight = screenHeight,
            optimalPageSizes = optimalPageSizes,
            sharedTransitionScope = sharedTransitionScope,
            animatedVisibilityScope = animatedVisibilityScope,
            drawableResName = drawableResName,
            assetPath = assetPath,
            onNavigationStateChange = onNavigationStateChange,
            onHighlightRequested = onHighlightRequested,
        )
    }

}

/**
 * Calculates optimal page sizes for all pages in the PDF.
 * Uses PageSizeCalculator to ensure pages fit the screen properly.
 * 
 * @param orientation The orientation of the PDF viewer (Vertical for portrait, Horizontal for landscape)
 *                   - Vertical (portrait): Uses WIDTH fit policy to fit page width to screen
 *                   - Horizontal (landscape): Uses BOTH fit policy to fit both width and height
 */
private suspend fun calculateOptimalPageSizes(
    pageRenderer: PdfPageRenderer?,
    pageCount: Int,
    screenWidth: Float,
    screenHeight: Float,
    orientation: PdfViewerOrientation
): List<Pair<Int, Int>>? {
    if (pageRenderer == null || pageCount <= 0) {
        return null
    }

    return coroutineScope {
        // Get original page sizes for all pages
        val originalSizes = (0 until pageCount).map { pageIndex ->
            async { pageRenderer.getPageSize(pageIndex) }
        }.awaitAll()

        if (originalSizes.any { it == null }) {
            return@coroutineScope null
        }

        val sizes = originalSizes.mapNotNull { it }

        // Find pages with maximum width and height for calculator setup
        val maxWidthPage = sizes.maxByOrNull { it.first } ?: sizes.first()
        val maxHeightPage = sizes.maxByOrNull { it.second } ?: sizes.first()

        // Create Size objects for PageSizeCalculator
        val originalMaxWidthPageSize = Size(maxWidthPage.first, maxWidthPage.second)
        val originalMaxHeightPageSize = Size(maxHeightPage.first, maxHeightPage.second)
        val viewSize = Size(screenWidth.toInt(), screenHeight.toInt())

        // Use different fit policy based on orientation:
        // - Vertical (portrait): WIDTH policy to fit page width to screen (maintains aspect ratio)
        // - Horizontal (landscape): BOTH policy to fit both width and height (prevents pages from being too large)
        val fitPolicy = when (orientation) {
            PdfViewerOrientation.Vertical -> FitPolicy.WIDTH
            PdfViewerOrientation.Horizontal -> FitPolicy.BOTH
        }

        // Create calculator with appropriate fit policy
        val calculator = PageSizeCalculator(
            fitPolicy,
            originalMaxWidthPageSize,
            originalMaxHeightPageSize,
            viewSize,
            true
        )

        // Calculate optimal size for each page
        sizes.map { (width, height) ->
            val originalSize = Size(width, height)
            val optimalSize = calculator.calculate(originalSize)
            Pair(optimalSize.width.toInt(), optimalSize.height.toInt())
        }
    }
}
