package com.fmaestre98.pdfviewer.pdfViewer.ui

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.fmaestre98.pdfviewer.pdfViewer.model.PdfWord
import com.fmaestre98.pdfviewer.pdfViewer.model.PdfViewerConstants
import com.fmaestre98.pdfviewer.pdfViewer.viewmodel.PdfViewerViewModel
import com.fmaestre98.pdfviewer.pdfViewer.viewmodel.PdfLoaderState
import com.fmaestre98.pdfviewer.pdfViewer.viewmodel.PdfNavigationState
import com.fmaestre98.pdfviewer.pdfViewer.viewmodel.PdfInteractiveState
import com.fmaestre98.pdfviewer.pdfViewer.rendering.PdfPageRenderer
import com.fmaestre98.pdfviewer.pdfViewer.gestures.PdfInteractionListener
import com.fmaestre98.pdfviewer.pdfViewer.gestures.GestureOrchestrator
import com.fmaestre98.pdfviewer.pdfViewer.gestures.GestureHandler
import com.fmaestre98.pdfviewer.pdfViewer.gestures.ZoomGestureHelpers
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
 * Vertical PDF viewer using LazyColumn for infinite scroll.
 * Supports zoom and horizontal pan, with vertical scrolling when zoomed.
 * Uses PdfInteractionListener pattern for better scalability.
 *
 * @param optimalPageSizes Pre-calculated optimal sizes for each page
 * @param modifier Modifier for the component
 * @param screenWidth Screen width in pixels
 * @param screenHeight Screen height in pixels
 * @param density Density for unit conversions
 * @param configuration Configuration for screen dimensions
 * @param pageRenderer The renderer for PDF pages
 * @param sharedTransitionScope Shared transition scope for hero animation
 * @param animatedVisibilityScope Animated visibility scope for hero animation
 * @param drawableResName Drawable resource name for first page cover image
 * @param assetPath Asset path for shared element key
 * @param onWordSelected Callback when a word is selected via long press
 * @param onSelectionCleared Callback when text selection is cleared
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun VerticalPdfView(
    viewModel: PdfViewerViewModel,
    loaderState: PdfLoaderState,
    navigationState: PdfNavigationState,
    interactiveState: PdfInteractiveState,
    optimalPageSizes: MutableState<List<Pair<Int, Int>>?>,
    modifier: Modifier,
    screenWidth: Float,
    screenHeight: Float,
    density: Density,
    configuration: Configuration,
    pageRenderer: PdfPageRenderer?,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null,
    drawableResName: String? = null,
    assetPath: String? = null,
    onNavigationStateChange: ((Int, (Int) -> Unit) -> Unit)?,
    onWordSelected: ((PdfWord) -> Unit)? = null,
    onSelectionCleared: (() -> Unit)? = null,
    onHighlightRequested: ((color: String, page: Int, snippet: String) -> Unit)? = null,
) {

    val pagerState = rememberPagerState(pageCount = { loaderState.pageCount })
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    // Tracks a page navigation requested programmatically (e.g. thumbnail drawer).
    // null means no pending navigation — the user is scrolling freely.
    val pendingNavigationTarget = remember { mutableStateOf<Int?>(null) }

    // Navigation function exposed to parent — sets the pending target and scrolls.
    // Near pages (<= 2 away) use animateScrollToItem; distant jumps use scrollToItem to prevent unwanted intermediate scroll animations.
    val navigateToPage: (Int) -> Unit = { targetPage ->
        scope.launch {
            pendingNavigationTarget.value = targetPage
            val currentVisible = listState.firstVisibleItemIndex
            val distance = kotlin.math.abs(targetPage - currentVisible)
            if (distance > 2 || (currentVisible == 0 && targetPage != 0)) {
                listState.scrollToItem(targetPage)
            } else {
                listState.animateScrollToItem(targetPage)
            }
            // Consume the pending target after the scroll completes
            pendingNavigationTarget.value = null
        }
    }

    // Handle initial page restore when optimalPageSizes first becomes available.
    // This only runs once when sizes are ready (or when the page count changes on re-load).
    LaunchedEffect(optimalPageSizes.value, loaderState.pageCount) {
        if (optimalPageSizes.value == null) return@LaunchedEffect
        val targetPage = navigationState.currentPageIndex.coerceIn(0, (loaderState.pageCount - 1).coerceAtLeast(0))
        if (targetPage > 0 && listState.firstVisibleItemIndex == 0) {
            listState.scrollToItem(targetPage)
        }
    }

    // Expose navigation state to parent — reports the current visible page and the
    // navigate function so the parent (thumbnail drawer, search) can request scrolls.
    LaunchedEffect(listState.firstVisibleItemIndex, onNavigationStateChange) {
        val currentPage = listState.firstVisibleItemIndex
        onNavigationStateChange?.invoke(currentPage, navigateToPage)
    }



    // Load page models for all visible pages with debounce to avoid processing during fast scrolling
    LaunchedEffect(listState.firstVisibleItemIndex, listState.layoutInfo.visibleItemsInfo.size, optimalPageSizes.value) {
        // Debounce: wait before processing. If keys change during this delay, 
        // this coroutine will be cancelled automatically and a new one will start
        delay(PdfViewerConstants.PAGE_MODEL_LOAD_DEBOUNCE_MS)
        
        val visibleItems = listState.layoutInfo.visibleItemsInfo
        if (visibleItems.isEmpty() || optimalPageSizes.value == null) return@LaunchedEffect

        val firstVisiblePage = visibleItems.first().index
        
        // For the first visible page, call setCurrentPage (full logic)
        val firstOptimalSize = optimalPageSizes.value?.getOrNull(firstVisiblePage)
        viewModel.setCurrentPage(firstVisiblePage, firstOptimalSize)

        // For other visible pages, only load the text model
        visibleItems.forEach { item ->
            val pageIndex = item.index
            if (pageIndex != firstVisiblePage) {
                val optimalSize = optimalPageSizes.value?.getOrNull(pageIndex)
                optimalSize?.let { (width, height) ->
                    viewModel.loadPageTextModel(pageIndex, width, height)
                }
            }
        }
    }

    // Track actual container size (LazyColumn dimensions)
    val containerSize = remember { mutableStateOf(Size(screenWidth, screenHeight)) }

    // Create gesture orchestrator
    val gestureOrchestrator = remember { GestureOrchestrator() }

    var lastDragScreenY by remember { mutableFloatStateOf(-1f) }
    var lastDragIsStart by remember { mutableStateOf(false) }
    var lastDragX by remember { mutableFloatStateOf(0f) }
    var lastDragY by remember { mutableFloatStateOf(0f) }

    // Create interaction listener that implements business logic for vertical view
    val interactionListener = remember(
        pagerState.currentPage,
        optimalPageSizes.value,
        containerSize.value,
        interactiveState,
        pageRenderer,
        listState,
        scope,
        gestureOrchestrator
    ) {
        object : PdfInteractionListener {
            override fun onPageTapped(x: Float, y: Float) {
                // Dismiss text selection on tap, unless tap is on a handle or part of zoom/drag/scroll
                if (interactiveState.isTextSelectionActive &&
                    !interactiveState.isDraggingHandle &&
                    !listState.isScrollInProgress &&
                    gestureOrchestrator.shouldDismissSelectionOnTap(interactiveState)
                ) {
                    interactiveState.deactivateTextSelection()
                    onSelectionCleared?.invoke()
                }
            }

            override suspend fun onPageLongPressed(x: Float, y: Float) {
                // Check if long press should be processed (orchestrator decides)
                if (!gestureOrchestrator.shouldProcessLongPress(interactiveState)) {
                    return
                }
                
                val zoom = interactiveState.zoomLevel
                val panX = interactiveState.offsetX

                val centerX = containerSize.value.width / 2f
                val centerY = containerSize.value.height / 2f

                val unzoomedX = (x - panX - centerX) / zoom + centerX
                val unzoomedY = (y - centerY) / zoom + centerY

                val visibleItems = listState.layoutInfo.visibleItemsInfo

                val hitItem = visibleItems.find { item ->
                    val itemTop = item.offset
                    val itemBottom = item.offset + item.size
                    unzoomedY >= itemTop && unzoomedY <= itemBottom
                } ?: return // No se tocó ningún item visible

                val targetPageIndex = hitItem.index
                val optimalSize = optimalPageSizes.value?.getOrNull(targetPageIndex) ?: return
                val (pageWidth, pageHeight) = optimalSize

                val localItemY = unzoomedY - hitItem.offset

                val (pageX, pageY) = PdfCoordinateHelpers.convertToPageCoordinatesVertical(
                    tapX = unzoomedX,
                    tapY = localItemY,
                    pageWidth = pageWidth,
                    pageHeight = pageHeight,
                    viewWidth = containerSize.value.width
                )

                val hitPageModel = interactiveState.getPageModel(targetPageIndex)
                val word = hitPageModel?.let { PdfCoordinateHelpers.findWordInModel(it, pageX, pageY) }

                if (word != null) {
                    interactiveState.activateTextSelection(word, targetPageIndex)
                    onWordSelected?.invoke(word)
                }
            }

            override fun onSelectionHandleDragged(isStart: Boolean, x: Float, y: Float, viewWidth: Int, viewHeight: Int) {
                val sourcePage = if (isStart) interactiveState.selectionStartPageIndex else interactiveState.selectionEndPageIndex
                if (sourcePage < 0) return

                interactiveState.isDraggingHandle = true
                val visibleItems = listState.layoutInfo.visibleItemsInfo
                val sourceItem = visibleItems.find { it.index == sourcePage }

                // Calculate unzoomed Y position in LazyColumn content
                val layoutY = if (sourceItem != null) {
                    sourceItem.offset + y
                } else {
                    val firstVis = visibleItems.firstOrNull() ?: return
                    firstVis.offset + y
                }

                val firstVisOffset = visibleItems.firstOrNull()?.offset ?: 0
                lastDragIsStart = isStart
                lastDragX = x
                lastDragY = y
                lastDragScreenY = (layoutY - firstVisOffset)

                // Find target item in visible items matching layoutY
                var targetItem = visibleItems.find { item ->
                    layoutY >= item.offset && layoutY <= item.offset + item.size
                }

                if (targetItem == null && visibleItems.isNotEmpty()) {
                    if (layoutY < visibleItems.first().offset) {
                        targetItem = visibleItems.first()
                    } else if (layoutY > visibleItems.last().offset + visibleItems.last().size) {
                        targetItem = visibleItems.last()
                    }
                }

                if (targetItem == null) return
                val targetPageIndex = targetItem.index
                val targetOptimalSize = optimalPageSizes.value?.getOrNull(targetPageIndex) ?: return
                val targetPageWidth = targetOptimalSize.first.toFloat()
                val targetPageHeight = targetOptimalSize.second.toFloat()

                val targetLocalY = (layoutY - targetItem.offset).coerceIn(0f, targetPageHeight)
                val targetLocalX = x.coerceIn(0f, targetPageWidth)

                val targetOptimizedModel = interactiveState.getOptimizedPageModel(targetPageIndex)
                val targetPageModel = interactiveState.getPageModel(targetPageIndex)

                val newChar = if (targetOptimizedModel != null) {
                    PdfCoordinateHelpers.findCharInModel(targetOptimizedModel, targetLocalX, targetLocalY)
                } else if (targetPageModel != null) {
                    PdfCoordinateHelpers.findCharInModel(targetPageModel, targetLocalX, targetLocalY)
                } else null

                if (newChar != null) {
                    if (isStart) {
                        interactiveState.updateSelectionStart(newChar, targetPageIndex)
                    } else {
                        interactiveState.updateSelectionEnd(newChar, targetPageIndex)
                    }
                }
            }

            override fun onTransformGesture(pan: Offset, zoom: Float) {
                // Check if transform should be processed (orchestrator decides based on priorities)
                if (gestureOrchestrator.shouldProcessTransform(interactiveState, zoom, pan)) {
                    // Handle zoom and horizontal pan using ZoomGestureHelpers
                    val newZoom = ZoomGestureHelpers.constrainZoom(interactiveState.zoomLevel * zoom)
                    interactiveState.zoomLevel = newZoom

                    // Handle horizontal pan (offset X)
                    val maxOffsetX = ZoomGestureHelpers.calculateMaxOffset(screenWidth, newZoom)
                    if (newZoom > PdfViewerConstants.ZOOM_LEVEL_1) {
                        val newOffsetX = interactiveState.offsetX + pan.x * PdfViewerConstants.PAN_SENSITIVITY_FACTOR
                        interactiveState.offsetX = newOffsetX.coerceIn(-maxOffsetX, maxOffsetX)
                    } else {
                        interactiveState.offsetX = 0f
                    }

                    // Convert vertical pan to scroll when zoomed
                    if (newZoom > PdfViewerConstants.ZOOM_LEVEL_1) {
                        // Invert pan.y because dragging down should scroll up
                        val scrollDelta = -pan.y * PdfViewerConstants.PAN_SENSITIVITY_FACTOR
                        scope.launch {
                            // dispatchRawDelta is instant and has no inertia animation,
                            // ideal for following finger exactly
                            listState.dispatchRawDelta(scrollDelta)
                        }
                    }
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
                    handleVerticalDoubleTapZoom(
                        interactiveState = interactiveState,
                        tapOffset = Offset(x, y),
                        screenWidth = screenWidth,
                        screenHeight = screenHeight,
                        listState = listState,
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

    // Automatic Edge Autoscroll when dragging handles near top or bottom screen boundaries
    LaunchedEffect(interactiveState.isDraggingHandle, lastDragScreenY) {
        if (!interactiveState.isDraggingHandle) return@LaunchedEffect
        val edgeThresholdPx = with(density) { 80.dp.toPx() }
        val maxScrollStepPx = with(density) { 12.dp.toPx() }

        val screenH = containerSize.value.height
        if (screenH > 0 && lastDragScreenY >= 0) {
            var scrollDelta = 0f
            if (lastDragScreenY < edgeThresholdPx) {
                val factor = (1f - (lastDragScreenY / edgeThresholdPx)).coerceIn(0f, 1f)
                scrollDelta = -maxScrollStepPx * factor
            } else if (lastDragScreenY > screenH - edgeThresholdPx) {
                val factor = ((lastDragScreenY - (screenH - edgeThresholdPx)) / edgeThresholdPx).coerceIn(0f, 1f)
                scrollDelta = maxScrollStepPx * factor
            }

            if (scrollDelta != 0f) {
                listState.scrollBy(scrollDelta)
                interactionListener.onSelectionHandleDragged(
                    isStart = lastDragIsStart,
                    x = lastDragX,
                    y = lastDragY,
                    viewWidth = 0,
                    viewHeight = 0
                )
            }
        }
    }

    // Create gesture handler
    val gestureHandler = remember { GestureHandler(interactionListener) }

    val clipboardManager = LocalClipboardManager.current
    var menuSize by remember { mutableStateOf(IntSize.Zero) }

    if (optimalPageSizes.value == null) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    } else {
        Box(
            modifier = modifier.fillMaxSize()
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .onSizeChanged { size ->
                        containerSize.value = Size(size.width.toFloat(), size.height.toFloat())
                    }
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            gestureHandler.handleTransformGesture(pan, zoom)
                        }
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
                        // Keep translationY at 0 - vertical scrolling is handled by LazyColumn
                        translationY = 0f
                    },
                contentPadding = PaddingValues(horizontal = PdfViewerConstants.VERTICAL_PAGE_HORIZONTAL_PADDING_DP.dp),
                verticalArrangement = Arrangement.spacedBy(PdfViewerConstants.VERTICAL_PAGE_SPACING_DP.dp)
            ) {
                items(
                    count = loaderState.pageCount,
                    key = { it }
                ) { pageIndex ->
                    Box(
                        modifier = Modifier.fillMaxWidth(),
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

                        // Text Selection Overlay (Handles only)
                        if (optimalSize != null &&
                            interactiveState.isTextSelectionActive &&
                            interactiveState.isSelectionActiveOnPage(pageIndex)
                        ) {
                            TextSelectionOverlay(
                                modifier = Modifier.matchParentSize(),
                                interactiveState = interactiveState,
                                gestureHandler = gestureHandler,
                                pageWidth = optimalSize.first,
                                pageHeight = optimalSize.second,
                                scaleFactor = interactiveState.zoomLevel,
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
                                pageModel = interactiveState.getPageModel(pageIndex),
                                pageWidth = optimalSize.first,
                                pageHeight = optimalSize.second,
                            )
                        }
                    }
                }
            }

            // Floating Text Selection Menu
            if (interactiveState.isTextSelectionActive && !interactiveState.isDraggingHandle && !listState.isScrollInProgress) {
                val startPage = interactiveState.selectionStartPageIndex
                val visibleItem = listState.layoutInfo.visibleItemsInfo.find { it.index == startPage } ?: listState.layoutInfo.visibleItemsInfo.firstOrNull()

                if (visibleItem != null) {
                    val menuPage = visibleItem.index
                    val startChar = interactiveState.selectionStartChar
                    val endChar = interactiveState.selectionEndChar
                    val optimalSize = optimalPageSizes.value?.getOrNull(menuPage)

                    if (startChar != null && endChar != null && optimalSize != null) {
                        val (pageW, pageH) = optimalSize
                        val itemOffset = visibleItem.offset

                        val targetCharForMenu = if (interactiveState.selectionStartPageIndex == menuPage) startChar else endChar
                        val charScreenPos = PdfCoordinateHelpers.convertToScreenCoordinatesVertical(
                            pdfPoint = Offset(targetCharForMenu.rect.left, targetCharForMenu.rect.top),
                            pageWidth = pageW,
                            pageHeight = pageH,
                            itemOffset = itemOffset,
                            screenWidth = containerSize.value.width,
                            screenHeight = containerSize.value.height,
                            zoomLevel = interactiveState.zoomLevel,
                            offsetX = interactiveState.offsetX
                        )

                        val screenW = containerSize.value.width
                        val screenH = containerSize.value.height

                        var menuX = charScreenPos.x - (menuSize.width / 2f)
                        var menuY = charScreenPos.y - (menuSize.height + 60f)

                        val padding = with(density) { 16.dp.toPx() }
                        if (menuSize.width > 0) {
                            menuX = menuX.coerceIn(padding, screenW - menuSize.width - padding)
                        }
                        if (menuSize.height > 0) {
                            if (menuY < padding) {
                                menuY = charScreenPos.y + 40f
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
                                onHighlightRequested?.invoke(color, menuPage, interactiveState.getSelectedText())
                                interactiveState.deactivateTextSelection()
                                onSelectionCleared?.invoke()
                            },
                            onDismiss = { /* Handled by tap on page */ }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Handles double-tap zoom animation for vertical PDF view.
 * Animates zoom with pivot point under the tapped location.
 * Applies incremental vertical scroll during animation to keep content centered.
 * This is a specialized function for LazyColumn that handles vertical scrolling differently than HorizontalPager.
 */
private fun handleVerticalDoubleTapZoom(
    interactiveState: PdfInteractiveState,
    tapOffset: Offset,
    screenWidth: Float,
    screenHeight: Float,
    listState: androidx.compose.foundation.lazy.LazyListState,
    scope: kotlinx.coroutines.CoroutineScope
) {
    val currentZoom = interactiveState.zoomLevel
    val targetZoom = ZoomGestureHelpers.nextZoomLevel(currentZoom)
    val centerX = screenWidth / 2f
    val centerY = screenHeight / 2f

    scope.launch {
        val animatable = Animatable(currentZoom)
        val startZoom = currentZoom
        val startOffsetX = interactiveState.offsetX

        // Calculate final horizontal offset
        val maxOffsetX = ZoomGestureHelpers.calculateMaxOffset(screenWidth, targetZoom)
        val finalOffsetX = if (targetZoom == PdfViewerConstants.ZOOM_LEVEL_1) {
            0f
        } else {
            ZoomGestureHelpers.calculateTargetOffset(
                startOffset = startOffsetX,
                tapCoordinate = tapOffset.x,
                centerCoordinate = centerX,
                startZoom = startZoom,
                targetZoom = targetZoom,
                maxOffset = maxOffsetX
            )
        }

        var previousZoom = startZoom

        animatable.animateTo(
            targetValue = targetZoom,
            animationSpec = tween(durationMillis = PdfViewerConstants.ZOOM_ANIMATION_DURATION_MS)
        ) {
            val currentAnimatedZoom = this.value

            // Update zoom
            interactiveState.zoomLevel = currentAnimatedZoom

            // Interpolate horizontal offset
            val fraction = (currentAnimatedZoom - startZoom) / (targetZoom - startZoom)
            interactiveState.offsetX = ZoomGestureHelpers.interpolateOffset(
                startOffset = startOffsetX,
                targetOffset = finalOffsetX,
                startZoom = startZoom,
                currentZoom = currentAnimatedZoom,
                targetZoom = targetZoom
            )

            // Apply incremental vertical scroll
            // LazyColumn doesn't easily support exact pixel scrollTo during animation,
            // so we use dispatchRawDelta with the difference from the last frame
            if (targetZoom != PdfViewerConstants.ZOOM_LEVEL_1) {
                val zoomStep = currentAnimatedZoom / previousZoom
                val deltaY = (tapOffset.y - centerY) * (zoomStep - 1)
                listState.dispatchRawDelta(deltaY)
            }

            previousZoom = currentAnimatedZoom
        }
    }
}