package com.fmaestre98.pdfviewer.pdfViewer.ui

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import com.fmaestre98.pdfviewer.pdfViewer.model.PdfWord
import com.fmaestre98.pdfviewer.pdfViewer.model.PdfViewerConstants
import com.fmaestre98.pdfviewer.pdfViewer.viewmodel.PdfViewerViewModel
import com.fmaestre98.pdfviewer.pdfViewer.viewmodel.PdfLoaderState
import com.fmaestre98.pdfviewer.pdfViewer.viewmodel.PdfNavigationState
import com.fmaestre98.pdfviewer.pdfViewer.viewmodel.PdfInteractiveState
import com.fmaestre98.pdfviewer.pdfViewer.viewmodel.PdfViewerState
import com.fmaestre98.pdfviewer.pdfViewer.rendering.PdfPageRenderer
import com.fmaestre98.pdfviewer.pdfViewer.gestures.PdfInteractionListener
import com.fmaestre98.pdfviewer.pdfViewer.gestures.GestureOrchestrator
import com.fmaestre98.pdfviewer.pdfViewer.gestures.GestureHandler
import com.fmaestre98.pdfviewer.pdfViewer.gestures.ZoomGestureHelpers
import com.fmaestre98.pdfviewer.pdfViewer.gestures.detectTransformGesturesWithLifecycle
import com.fmaestre98.pdfviewer.pdfViewer.utils.PdfCoordinateHelpers
import com.fmaestre98.pdfviewer.pdfViewer.overlay.PdfDebugOverlay
import com.fmaestre98.pdfviewer.pdfViewer.overlay.ReadingOverlay
import com.fmaestre98.pdfviewer.pdfViewer.overlay.TextSelectionOverlay
import com.fmaestre98.pdfviewer.pdfViewer.overlay.HighlightOverlay
import com.fmaestre98.pdfviewer.pdfViewer.overlay.TextSelectionMenu
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlin.math.min
import kotlin.math.max

/**
 * Horizontal PDF viewer using HorizontalPager for page-by-page navigation.
 * Supports zoom and pan gestures with double-tap zoom animation.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun HorizontalPdfView(
    viewModel: PdfViewerViewModel,
    loaderState: PdfLoaderState,
    navigationState: PdfNavigationState,
    interactiveState: PdfInteractiveState,
    pageRenderer: PdfPageRenderer?,
    modifier: Modifier = Modifier,
    screenWidth: Float,
    screenHeight: Float,
    optimalPageSizes: MutableState<List<Pair<Int, Int>>?>,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null,
    drawableResName: String? = null,
    assetPath: String? = null,
    onNavigationStateChange: ((Int, (Int) -> Unit) -> Unit)? = null,
    onWordSelected: ((PdfWord) -> Unit)? = null,
    onSelectionCleared: (() -> Unit)? = null,
    onHighlightRequested: ((color: String, page: Int, snippet: String) -> Unit)? = null,
) {
    val pagerState = rememberPagerState(
        initialPage = navigationState.currentPageIndex.coerceIn(0, (loaderState.pageCount - 1).coerceAtLeast(0)),
        pageCount = { loaderState.pageCount }
    )
    val scope = rememberCoroutineScope()

    // Navigation function
    val navigateToPage: (Int) -> Unit = { targetPage ->
        scope.launch {
            pagerState.animateScrollToPage(targetPage)
        }
    }

    // Restore page position when data is ready or navigation index changes
    LaunchedEffect(optimalPageSizes.value, navigationState.currentPageIndex, loaderState.pageCount) {
        val targetPage = navigationState.currentPageIndex
        if (targetPage >= 0 && targetPage < loaderState.pageCount) {
            if (targetPage != pagerState.currentPage) {
                // If it's the initial load (pager at 0, target not 0), use scrollToPage for immediate jump
                if (pagerState.currentPage == 0 && targetPage != 0) {
                    pagerState.scrollToPage(targetPage)
                } else {
                    pagerState.animateScrollToPage(targetPage)
                }
            }
        }
    }

    // Expose navigation state to parent (current page and navigation function)
    LaunchedEffect(pagerState.currentPage, onNavigationStateChange) {
        onNavigationStateChange?.invoke(pagerState.currentPage, navigateToPage)
    }

    // Reset zoom when user changes page and load text model
    LaunchedEffect(pagerState.currentPage, optimalPageSizes.value) {
        val optimalSize = optimalPageSizes.value?.getOrNull(pagerState.currentPage)
        viewModel.setCurrentPage(pagerState.currentPage, optimalSize)
    }

    // Track actual container size (HorizontalPager dimensions)
    val containerSize = remember { mutableStateOf(Size(screenWidth, screenHeight)) }

    // Create gesture orchestrator
    val gestureOrchestrator = remember { GestureOrchestrator() }

    // Create interaction listener that implements business logic
    val interactionListener = remember(
        pagerState.currentPage,
        optimalPageSizes.value,
        containerSize.value,
        interactiveState,
        pageRenderer,
        gestureOrchestrator
    ) {
        object : PdfInteractionListener {
            override fun onPageTapped(x: Float, y: Float) {
                // Dismiss text selection on tap, unless tap is on a handle or part of zoom/drag
                if (interactiveState.isTextSelectionActive && gestureOrchestrator.shouldDismissSelectionOnTap()) {
                    interactiveState.deactivateTextSelection()
                    onSelectionCleared?.invoke()
                }
            }

            override suspend fun onPageLongPressed(x: Float, y: Float) {
                // Check if long press should be processed (orchestrator decides)
                if (!gestureOrchestrator.shouldProcessLongPress(interactiveState)) {
                    return
                }
                
                val optimalSize =
                    optimalPageSizes.value?.getOrNull(pagerState.currentPage) ?: return
                val (pageWidth, pageHeight) = optimalSize

                // Convert screen coordinates to PDF coordinates
                val (pageX, pageY) = PdfCoordinateHelpers.convertToPageCoordinates(
                    tapOffset = Offset(x, y),
                    pageWidth = pageWidth,
                    pageHeight = pageHeight,
                    screenWidth = containerSize.value.width,
                    screenHeight = containerSize.value.height,
                    zoomLevel = interactiveState.zoomLevel,
                    offsetX = interactiveState.offsetX,
                    offsetY = interactiveState.offsetY
                )

                // Get page model
                val pageModel = interactiveState.getPageModel(navigationState.currentPageIndex)

                // Find word at point
                val word = pageModel?.let { PdfCoordinateHelpers.findWordInModel(it, pageX, pageY) }
                println("Long press at ($x, $y) found word: ${word?.text}")
                if (word != null) {
                    // Replace previous selection with new one (per user preference)
                    interactiveState.activateTextSelection(word, navigationState.currentPageIndex)
                    onWordSelected?.invoke(word)
                }
            }


            override fun onSelectionHandleDragged(isStart: Boolean, x: Float, y: Float, viewWidth: Int, viewHeight: Int) {
                val pageModel =  interactiveState.getPageModel(navigationState.currentPageIndex) ?: return
                interactiveState.isDraggingHandle = true // Ocultar menú

                val optimalSize = optimalPageSizes.value?.getOrNull(navigationState.currentPageIndex) ?: return
                val pageWidth = optimalSize.first.toFloat()
                val pageHeight = optimalSize.second.toFloat()

                // Calculate scale using passed view dimensions
                val curViewWidth = viewWidth.toFloat()
                val curViewHeight = viewHeight.toFloat()
                val scaleX = curViewWidth / pageWidth
                val scaleY = curViewHeight / pageHeight
                val baseScale = minOf(scaleX, scaleY)
                val centeringOffsetX = (curViewWidth - pageWidth * baseScale) / 2f
                val centeringOffsetY = (curViewHeight - pageHeight * baseScale) / 2f

                // Inverse projection
                val pdfX = (x - centeringOffsetX) / baseScale
                val pdfY = (y - centeringOffsetY) / baseScale

                // 2. Buscar el nuevo char
                val optimizedModel = interactiveState.getOptimizedPageModel(navigationState.currentPageIndex)
                val newChar = if (optimizedModel != null) {
                    PdfCoordinateHelpers.findCharInModel(optimizedModel, pdfX, pdfY)
                } else {
                     PdfCoordinateHelpers.findCharInModel(pageModel, pdfX, pdfY)
                }
                
                if (newChar != null) {
                    if (isStart) interactiveState.updateSelectionStart(newChar)
                    else interactiveState.updateSelectionEnd(newChar)
                }
            }

            override fun onTransformGesture(pan: Offset, zoom: Float) {
                // Check if transform should be processed (orchestrator decides based on priorities)
                if (gestureOrchestrator.shouldProcessTransform(interactiveState, zoom, pan)) {
                    ZoomGestureHelpers.handleTransformGesture(
                        viewerState = object : PdfViewerState() {
                            override var zoomLevel
                                get() = interactiveState.zoomLevel
                                set(value) {
                                    interactiveState.zoomLevel = value
                                }
                            override var offsetX
                                get() = interactiveState.offsetX
                                set(value) {
                                    interactiveState.offsetX = value
                                }
                            override var offsetY
                                get() = interactiveState.offsetY
                                set(value) {
                                    interactiveState.offsetY = value
                                }
                            override var isTextSelectionActive = false
                                get() = interactiveState.isTextSelectionActive
                        },
                        pan = pan,
                        zoom = zoom,
                        screenWidth = containerSize.value.width,
                        screenHeight = containerSize.value.height
                    )
                }
            }
            
            override fun onTransformStarted() {
                gestureOrchestrator.onTransformStarted(1.0f)
            }
            
            override fun onTransformEnded() {
                gestureOrchestrator.onTransformEnded()
            }

            override fun onDoubleTap(x: Float, y: Float) {
                // Double tap zoom works even with text selection active
                // Orchestrator allows it if not actively dragging handles
                if (!gestureOrchestrator.isHandleDragActive()) {
                    ZoomGestureHelpers.handleDoubleTapZoom(
                        viewerState = object : PdfViewerState() {
                            override var zoomLevel
                                get() = interactiveState.zoomLevel
                                set(value) {
                                    interactiveState.zoomLevel = value
                                }
                            override var offsetX
                                get() = interactiveState.offsetX
                                set(value) {
                                    interactiveState.offsetX = value
                                }
                            override var offsetY
                                get() = interactiveState.offsetY
                                set(value) {
                                    interactiveState.offsetY = value
                                }
                            override var isTextSelectionActive = false
                                get() = interactiveState.isTextSelectionActive
                        },
                        tapOffset = Offset(x, y),
                        screenWidth = containerSize.value.width,
                        screenHeight = containerSize.value.height,
                        scope = scope
                    )
                }
            }
            
            override fun onHandleDragStarted() {
                gestureOrchestrator.onHandleDragStarted()
            }
            
            override fun onHandleDragEnded() {
                gestureOrchestrator.onHandleDragEnded()
            }
        }
    }

    // Create gesture handler
    val clipboardManager = LocalClipboardManager.current
    var menuSize by remember { mutableStateOf(IntSize.Zero) }
    val density = androidx.compose.ui.platform.LocalDensity.current

    val gestureHandler = remember { GestureHandler(interactionListener) }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            HorizontalPager(
                state = pagerState,
                // Disable swipe when zoomed in (text selection no longer blocks swipe)
                userScrollEnabled = interactiveState.zoomLevel == PdfViewerConstants.ZOOM_LEVEL_1,
                modifier = Modifier
                    .fillMaxSize()
                    .onSizeChanged { size ->
                        containerSize.value = Size(size.width.toFloat(), size.height.toFloat())
                    }
                    .pointerInput(Unit) {
                        detectTransformGesturesWithLifecycle(
                            onGestureStart = {
                                gestureHandler.notifyTransformStarted()
                            },
                            onGesture = { _, pan, zoom, _ ->
                                gestureHandler.handleTransformGesture(pan, zoom)
                            },
                            onGestureEnd = {
                                gestureHandler.notifyTransformEnded()
                            }
                        )
                    }
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = { tapOffset ->
                                gestureHandler.handleTap(tapOffset)
                            },
                            onDoubleTap = { tapOffset ->
                                gestureHandler.handleDoubleTap(tapOffset)
                            },
                            onLongPress = { tapOffset ->
                                scope.launch {
                                    gestureHandler.handleLongPress(tapOffset)
                                }
                            }
                        )
                    }
                    .graphicsLayer {
                        scaleX = interactiveState.zoomLevel
                        scaleY = interactiveState.zoomLevel
                        translationX = interactiveState.offsetX
                        translationY = interactiveState.offsetY
                    }
            ) { pageIndex ->
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    val optimalSize = optimalPageSizes.value?.getOrNull(pageIndex)
                    PdfPageItem(
                        pageIndex = pageIndex,
                        pageRenderer = pageRenderer,
                        optimalSize = optimalSize,
                        modifier = Modifier,
                        sharedTransitionScope = sharedTransitionScope,
                        animatedVisibilityScope = animatedVisibilityScope,
                        drawableResName = drawableResName,
                        assetPath = assetPath,
                    )

                    // Persistent highlights overlay
                    if (optimalSize != null) {
                        val pageHighlights = interactiveState.getHighlightsForPage(pageIndex)
                        if (pageHighlights.isNotEmpty()) {
                            HighlightOverlay(
                                highlights = pageHighlights,
                                pageWidth = optimalSize.first,
                                pageHeight = optimalSize.second,
                                modifier = Modifier.matchParentSize()
                            )
                        }
                    }

                    if (optimalSize != null && navigationState.currentPageIndex == pageIndex && interactiveState.isTextSelectionActive) {
                        TextSelectionOverlay(
                            interactiveState = interactiveState,
                            gestureHandler = gestureHandler,
                            pageWidth = optimalSize.first,
                            pageHeight = optimalSize.second,
                            scaleFactor = interactiveState.zoomLevel,
                            modifier = Modifier.matchParentSize(),
                            onHighlightRequested = { color ->
                                onHighlightRequested?.invoke(
                                    color,
                                    pageIndex,
                                    interactiveState.getSelectedText()
                                )
                            },
                            pageRenderer = pageRenderer,
                            pageIndex = pageIndex,
                            optimalPageSize = optimalSize,
                        )
                    }

                    // Overlay de palabra actual en modo karaoke
                    if (optimalSize != null && navigationState.currentPageIndex == pageIndex && interactiveState.isKaraokeMode) {
                        ReadingOverlay(
                            currentReadingWord = interactiveState.currentReadingWord,
                            pageWidth = optimalSize.first,
                            pageHeight = optimalSize.second
                        )
                    }


                    // Debug overlay for word bounding boxes
                    if (optimalSize != null && navigationState.currentPageIndex == pageIndex) {
                        PdfDebugOverlay(
                            pageModel =  interactiveState.getPageModel(navigationState.currentPageIndex),
                            pageWidth = optimalSize.first,
                            pageHeight = optimalSize.second,
                        )
                    }
                }
            }

            // Floating Text Selection Menu
            if (interactiveState.isTextSelectionActive && !interactiveState.isDraggingHandle && !pagerState.isScrollInProgress) {
                val selectionPage = interactiveState.selectionPageIndex
                if (selectionPage == pagerState.currentPage) {
                    val startChar = interactiveState.selectionStartChar
                    val endChar = interactiveState.selectionEndChar
                    val optimalSize = optimalPageSizes.value?.getOrNull(selectionPage)

                    if (startChar != null && endChar != null && optimalSize != null) {
                        val (pageW, pageH) = optimalSize

                        // Convert to screen coordinates
                        val startScreen = PdfCoordinateHelpers.convertToScreenCoordinates(
                            pdfPoint = Offset(startChar.rect.top, startChar.rect.bottom),
                            pageWidth = pageW,
                            pageHeight = pageH,
                            screenWidth = containerSize.value.width,
                            screenHeight = containerSize.value.height,
                            zoomLevel = interactiveState.zoomLevel,
                            offsetX = interactiveState.offsetX,
                            offsetY = interactiveState.offsetY
                        )
                        val endScreen = PdfCoordinateHelpers.convertToScreenCoordinates(
                            pdfPoint = Offset(endChar.rect.top, endChar.rect.bottom),
                            pageWidth = pageW,
                            pageHeight = pageH,
                            screenWidth = containerSize.value.width,
                            screenHeight = containerSize.value.height,
                            zoomLevel = interactiveState.zoomLevel,
                            offsetX = interactiveState.offsetX,
                            offsetY = interactiveState.offsetY
                        )

                        val selectionTop = min(startScreen.y, endScreen.y)
                        val selectionBottom = max(startScreen.y, endScreen.y)
                        val selectionCenterX = (startScreen.x + endScreen.x) / 2f
                        
                        val screenW = containerSize.value.width
                        val screenH = containerSize.value.height
                        
                        // Initial position (centered above selection)
                        var menuX = selectionCenterX - (menuSize.width / 2f)
                        var menuY = selectionTop - (menuSize.height + 60f) // 60f margin equivalent

                        val padding = with(density) { 16.dp.toPx() }

                        // Clamp X
                        if (menuSize.width > 0) {
                             menuX = menuX.coerceIn(padding, screenW - menuSize.width - padding)
                        }

                        // Clamp Y
                        if (menuSize.height > 0) {
                            if (menuY < padding) {
                                menuY = selectionBottom + 20f // Show below if no space on top
                            }
                            menuY = menuY.coerceIn(padding, screenH - menuSize.height - padding)
                        }

                        TextSelectionMenu(
                            modifier = Modifier
                                .offset { IntOffset(menuX.toInt(), menuY.toInt()) }
                                .onSizeChanged { menuSize = it },
                            onCopy = {
                                val text = interactiveState.getSelectedText()
                                clipboardManager.setText(AnnotatedString(text))
                                interactiveState.deactivateTextSelection()
                                onSelectionCleared?.invoke()
                            },
                            onHighlightRequested = { color ->
                                onHighlightRequested?.invoke(color, selectionPage, interactiveState.getSelectedText())
                                interactiveState.deactivateTextSelection()
                                onSelectionCleared?.invoke()
                            },
                            onDismiss = {
                                // Handled by tap on page
                            }
                        )
                    }
                }
            }
        }
        SwipeIndicator(pagerState, loaderState.pageCount)
    }
}


/**
 * Displays swipe indicator text based on current page position.
 */
@Composable
private fun SwipeIndicator(pagerState: PagerState, pageCount: Int) {
    val indicatorText = when {
        pagerState.currentPage == 0 && pageCount > 1 -> "Desliza para la siguiente página 👉"
        pagerState.currentPage > 0 && pagerState.currentPage < pageCount - 1 -> "👈 Desliza 👉"
        pagerState.currentPage == pageCount - 1 && pageCount > 1 -> "👈 Desliza para volver"
        else -> ""
    }

    if (indicatorText.isNotEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    bottom = PdfViewerConstants.SWIPE_INDICATOR_BOTTOM_PADDING_DP.dp,
                    top = PdfViewerConstants.SWIPE_INDICATOR_TOP_PADDING_DP.dp
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = indicatorText,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
