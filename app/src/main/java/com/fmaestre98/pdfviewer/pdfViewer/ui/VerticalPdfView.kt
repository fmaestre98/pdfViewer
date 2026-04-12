package com.fmaestre98.pdfviewer.pdfViewer.ui

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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

    // Navigation function - usar animateScrollToItem para LazyColumn
    val navigateToPage: (Int) -> Unit = { targetPage ->
        scope.launch {
            listState.animateScrollToItem(targetPage)
        }
    }

    // Restore page position when data is ready or navigation index changes
    LaunchedEffect(optimalPageSizes.value, navigationState.currentPageIndex, loaderState.pageCount) {
        val targetPage = navigationState.currentPageIndex.coerceIn(0, (loaderState.pageCount - 1).coerceAtLeast(0))
        if (targetPage >= 0 && targetPage < loaderState.pageCount) {
            if (listState.firstVisibleItemIndex != targetPage || optimalPageSizes.value != null) {
                // If it's the initial load (pager at 0, target not 0), use scrollToItem for immediate jump
                if (listState.firstVisibleItemIndex == 0 && targetPage != 0) {
                    listState.scrollToItem(targetPage)
                } else {
                    listState.animateScrollToItem(targetPage)
                }
            }
        }
    }

    // Expose navigation state to parent
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
                
                // 1. DES-ZOOM: Convertir coordenada de pantalla (Zoomed) a coordenada de LazyColumn (Unzoomed)
                // El LazyColumn tiene un graphicsLayer con scale y translationX.
                // Necesitamos revertir esa transformación para saber dónde se tocó en la "superficie" de la lista.
                val zoom = interactiveState.zoomLevel
                val panX = interactiveState.offsetX

                // Centro de la pantalla para el pivote del zoom
                val centerX = containerSize.value.width / 2f
                val centerY = containerSize.value.height / 2f

                // Fórmula inversa del zoom centrado:
                // screenX = (localX - centerX) * zoom + centerX + panX
                // localX = (screenX - panX - centerX) / zoom + centerX
                val unzoomedX = (x - panX - centerX) / zoom + centerX
                val unzoomedY = (y - centerY) / zoom + centerY

                // 2. HIT TEST: Encontrar qué item de la LazyColumn está bajo unzoomedY
                val visibleItems = listState.layoutInfo.visibleItemsInfo

                // Buscamos el item cuyo rango vertical contenga el toque
                val hitItem = visibleItems.find { item ->
                    val itemTop = item.offset
                    val itemBottom = item.offset + item.size
                    unzoomedY >= itemTop && unzoomedY <= itemBottom
                } ?: return // No se tocó ningún item visible

                val targetPageIndex = hitItem.index
                val optimalSize = optimalPageSizes.value?.getOrNull(targetPageIndex) ?: return
                val (pageWidth, pageHeight) = optimalSize

                // 3. CALCULAR COORDENADAS RELATIVAS AL ITEM
                // Restamos el offset del item para obtener Y relativo a la imagen de la página
                val localItemY = unzoomedY - hitItem.offset

                // 4. CONVERTIR A PDF
                val (pageX, pageY) = PdfCoordinateHelpers.convertToPageCoordinatesVertical(
                    tapX = unzoomedX,
                    tapY = localItemY, // Usamos la Y local
                    pageWidth = pageWidth,
                    pageHeight = pageHeight,
                    viewWidth = containerSize.value.width
                )

                // 5. OBTENER MODELO CORRECTO
                // IMPORTANTE: Cargar el modelo de la página TOCADA, no de la "actual"
                // Si el modelo ya está cargado en interactiveState y es el mismo índice, lo usamos.
                // Si no, intentamos obtenerlo del renderer (síncrono si está en caché o rápido)
                val hitPageModel = interactiveState.getPageModel(targetPageIndex)
                // 6. BUSCAR PALABRA
                val word = hitPageModel?.let { PdfCoordinateHelpers.findWordInModel(it, pageX, pageY) }
                println("Vertical LongPress Page $targetPageIndex -> Screen($x, $y) Unzoomed($unzoomedX, $unzoomedY) PDF($pageX, $pageY) Word: ${word?.text}")

                if (word != null) {
                    // Replace previous selection with new one (per user preference)
                    interactiveState.activateTextSelection(word, targetPageIndex)
                    onWordSelected?.invoke(word)
                }
            }

            override fun onSelectionHandleDragged(isStart: Boolean, x: Float, y: Float, viewWidth: Int, viewHeight: Int) {
                val pageModel = interactiveState.getPageModel(interactiveState.selectionPageIndex) ?: return
                interactiveState.isDraggingHandle = true

                val optimalSize = optimalPageSizes.value?.getOrNull(interactiveState.selectionPageIndex) ?: return
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

                // Find the new char
                val optimizedModel = interactiveState.getOptimizedPageModel(interactiveState.selectionPageIndex)
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
                            interactiveState.selectionPageIndex == pageIndex
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
                val selectionPage = interactiveState.selectionPageIndex
                // Find if the selection page is currently visible in the LazyColumn
                val visibleItem = listState.layoutInfo.visibleItemsInfo.find { it.index == selectionPage }

                if (visibleItem != null) {
                    val startChar = interactiveState.selectionStartChar
                    val endChar = interactiveState.selectionEndChar
                    val optimalSize = optimalPageSizes.value?.getOrNull(selectionPage)

                    if (startChar != null && endChar != null && optimalSize != null) {
                        val (pageW, pageH) = optimalSize
                        // The item's offset relative to the LazyColumn viewport start
                        val itemOffset = visibleItem.offset

                        // Convert to screen coordinates using the helper for Vertical view
                        val startScreen = PdfCoordinateHelpers.convertToScreenCoordinatesVertical(
                            pdfPoint = Offset(startChar.rect.top, startChar.rect.bottom),
                            pageWidth = pageW,
                            pageHeight = pageH,
                            itemOffset = itemOffset,
                            screenWidth = containerSize.value.width,
                            screenHeight = containerSize.value.height,
                            zoomLevel = interactiveState.zoomLevel,
                            offsetX = interactiveState.offsetX
                        )
                        val endScreen = PdfCoordinateHelpers.convertToScreenCoordinatesVertical(
                            pdfPoint = Offset(endChar.rect.top, endChar.rect.bottom),
                            pageWidth = pageW,
                            pageHeight = pageH,
                            itemOffset = itemOffset,
                            screenWidth = containerSize.value.width,
                            screenHeight = containerSize.value.height,
                            zoomLevel = interactiveState.zoomLevel,
                            offsetX = interactiveState.offsetX
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