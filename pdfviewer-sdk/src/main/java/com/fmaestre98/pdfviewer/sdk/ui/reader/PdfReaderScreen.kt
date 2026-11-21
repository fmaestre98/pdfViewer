package com.fmaestre98.pdfviewer.sdk.ui.reader

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.fmaestre98.pdfviewer.sdk.R
import com.fmaestre98.pdfviewer.sdk.core.model.HighlightData
import com.fmaestre98.pdfviewer.sdk.core.model.PdfViewerOrientation
import com.fmaestre98.pdfviewer.sdk.core.rendering.PageThumbnailDrawer
import com.fmaestre98.pdfviewer.sdk.core.rendering.PdfPageRenderer
import com.fmaestre98.pdfviewer.sdk.core.ui.ComposePdfViewer
import com.fmaestre98.pdfviewer.sdk.core.viewmodel.PdfViewerViewModel
import com.fmaestre98.pdfviewer.sdk.model.PdfViewerColors
import com.fmaestre98.pdfviewer.sdk.storage.NormalizedRect
import com.fmaestre98.pdfviewer.sdk.ui.LocalPdfViewerColors
import com.fmaestre98.pdfviewer.sdk.ui.reader.components.PageNoteBottomSheet
import com.fmaestre98.pdfviewer.sdk.ui.reader.components.PdfSearchOverlay
import java.io.File

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
internal fun PdfReaderScreen(
    state: PdfReaderState,
    snackbarHostState: SnackbarHostState,
    pdfViewerViewModel: PdfViewerViewModel,
    onAction: (PdfReaderAction) -> Unit,
    selectionHandleColor: Color = PdfViewerColors().selectionHandle,
    selectionHighlightColor: Color = PdfViewerColors().selectionHighlight
) {
    CompositionLocalProvider(
        LocalPdfViewerColors provides PdfViewerColors(
            selectionHighlight = selectionHighlightColor,
            selectionHandle = selectionHandleColor
        )
    ) {
        PdfReaderScreenContent(
            state = state,
            snackbarHostState = snackbarHostState,
            pdfViewerViewModel = pdfViewerViewModel,
            onAction = onAction
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
private fun PdfReaderScreenContent(
    state: PdfReaderState,
    snackbarHostState: SnackbarHostState,
    pdfViewerViewModel: PdfViewerViewModel,
    onAction: (PdfReaderAction) -> Unit
) {
    val interactiveState = pdfViewerViewModel.interactiveState

    var pageRenderer: PdfPageRenderer? by remember { mutableStateOf(null) }
    var navigateToPage: ((Int) -> Unit)? by remember { mutableStateOf(null) }
    var showNoteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(state.highlights) {
        val mappedHighlights = state.highlights.mapValues { (_, list) ->
            list.map { repoHighlight ->
                HighlightData(
                    groupId = repoHighlight.groupId,
                    color = repoHighlight.color,
                    startX = repoHighlight.startX,
                    startY = repoHighlight.startY,
                    endX = repoHighlight.endX,
                    endY = repoHighlight.endY,
                    snippet = repoHighlight.snippet
                )
            }
        }
        interactiveState.setHighlights(mappedHighlights)
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (state.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (state.error != null) {
                Text(
                    text = state.error,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                val file = remember(state.filePath) {
                    state.filePath?.let { File(it) }?.takeIf { it.exists() }
                }

                if (file != null) {
                    val resolvedInitialPage = remember(file) { state.currentPage }
                    val isPageTextLoaded =
                        interactiveState.getPageModel(pdfViewerViewModel.navigationState.currentPageIndex) != null

                    PageThumbnailDrawer(
                        isOpen = state.isThumbnailDrawerOpen,
                        onDismiss = { onAction(PdfReaderAction.CloseThumbnailDrawer) },
                        pageRenderer = pageRenderer,
                        pageCount = state.pageCount,
                        currentPage = state.currentPage,
                        bookmarkedPages = state.bookmarkedPages,
                        onPageSelected = { targetPage ->
                            onAction(PdfReaderAction.OnPageChanged(targetPage))
                            navigateToPage?.invoke(targetPage)
                            onAction(PdfReaderAction.CloseThumbnailDrawer)
                        }
                    ) {
                        ComposePdfViewer(
                            pdfFile = file,
                            modifier = Modifier
                                .fillMaxSize()
                                .semantics {
                                    if (isPageTextLoaded) {
                                        val model =
                                            interactiveState.getPageModel(pdfViewerViewModel.navigationState.currentPageIndex)
                                        if (model != null) {
                                            val textContent =
                                                model.coordinates.joinToString("\n") { it.text }
                                            text = AnnotatedString(textContent)
                                        }
                                    }
                                },
                            initialPage = resolvedInitialPage,
                            orientation = state.orientation,
                            onNavigationStateChange = { currentPage, navigate ->
                                onAction(PdfReaderAction.OnPageChanged(currentPage))
                                navigateToPage = navigate
                            },
                            onRendererReady = { renderer, count ->
                                pageRenderer = renderer
                                onAction(PdfReaderAction.UpdatePageCount(count))
                            },
                            enableTextExtraction = true,
                            viewModel = pdfViewerViewModel,
                            onHighlightRequested = { color, page, snippet ->
                                val startChar = interactiveState.selectionStartChar
                                val endChar = interactiveState.selectionEndChar
                                val pageIndex = interactiveState.selectionPageIndex
                                val optimizedModel =
                                    interactiveState.getOptimizedPageModel(pageIndex)
                                val pageModel = interactiveState.getPageModel(pageIndex)

                                if (startChar != null && endChar != null && optimizedModel != null && pageModel != null) {
                                    val selectedChars =
                                        optimizedModel.getCharactersBetween(startChar, endChar)
                                    val pw = pageModel.targetWidth.toFloat()
                                    val ph = pageModel.targetHeight.toFloat()

                                    if (selectedChars.isNotEmpty() && pw > 0 && ph > 0) {
                                        val normalizedRects = mutableListOf<NormalizedRect>()
                                        var lineLeft = selectedChars[0].rect.left
                                        var lineTop = selectedChars[0].rect.top
                                        var lineRight = selectedChars[0].rect.right
                                        var lineBottom = selectedChars[0].rect.bottom

                                        for (i in 1 until selectedChars.size) {
                                            val charRect = selectedChars[i].rect
                                            val charCenterY = (charRect.top + charRect.bottom) / 2f
                                            val lineCenterY = (lineTop + lineBottom) / 2f

                                            if (kotlin.math.abs(charCenterY - lineCenterY) < 5f) {
                                                lineLeft = minOf(lineLeft, charRect.left)
                                                lineRight = maxOf(lineRight, charRect.right)
                                                lineTop = minOf(lineTop, charRect.top)
                                                lineBottom = maxOf(lineBottom, charRect.bottom)
                                            } else {
                                                normalizedRects.add(
                                                    NormalizedRect(
                                                        startX = lineLeft / pw,
                                                        startY = lineTop / ph,
                                                        endX = lineRight / pw,
                                                        endY = lineBottom / ph
                                                    )
                                                )
                                                lineLeft = charRect.left
                                                lineTop = charRect.top
                                                lineRight = charRect.right
                                                lineBottom = charRect.bottom
                                            }
                                        }
                                        normalizedRects.add(
                                            NormalizedRect(
                                                startX = lineLeft / pw,
                                                startY = lineTop / ph,
                                                endX = lineRight / pw,
                                                endY = lineBottom / ph
                                            )
                                        )

                                        onAction(
                                            PdfReaderAction.SaveHighlight(
                                                color = color,
                                                page = page,
                                                snippet = snippet,
                                                normalizedRects = normalizedRects
                                            )
                                        )
                                    }
                                }
                            },
                            onError = { }
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 16.dp, end = 16.dp),
                        contentAlignment = Alignment.TopEnd
                    ) {
                        Row {
                            if (state.enableSearch) {
                                IconButton(
                                    onClick = { onAction(PdfReaderAction.ToggleSearch) },
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = stringResource(R.string.reader_search),
                                        tint = Color.Gray
                                    )
                                }
                            }
                            if (state.enableBookmarks) {
                                IconButton(
                                    onClick = { onAction(PdfReaderAction.ToggleBookmark) },
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(
                                            id = if (state.isCurrentPageBookmarked) {
                                                R.drawable.ic_bookmarked
                                            } else {
                                                R.drawable.ic_not_bookmarked
                                            }
                                        ),
                                        contentDescription = stringResource(
                                            if (state.isCurrentPageBookmarked) {
                                                R.string.reader_remove_bookmark
                                            } else {
                                                R.string.reader_add_bookmark
                                            }
                                        ),
                                        tint = Color(0xFFFFA000)
                                    )
                                }
                            }
                        }
                    }

                    if (state.currentPageNote != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(top = 16.dp, start = 16.dp),
                            contentAlignment = Alignment.TopStart
                        ) {
                            IconButton(
                                onClick = { showNoteDialog = true },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = stringResource(R.string.reader_view_footnote),
                                    tint = Color(0xFFFFA000),
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = 36.dp, start = 16.dp, end = 16.dp, top = 16.dp),
                        contentAlignment = Alignment.BottomEnd
                    ) {
                        Column(
                            horizontalAlignment = Alignment.End,
                            modifier = Modifier.padding(bottom = 16.dp)
                        ) {
                            AnimatedVisibility(
                                visible = state.isFabExpanded,
                                enter = fadeIn() + expandVertically(),
                                exit = fadeOut() + shrinkVertically()
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.End,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                ) {
                                    if (state.enableThumbnailDrawer) {
                                        FloatingActionButton(
                                            onClick = { onAction(PdfReaderAction.ToggleThumbnailDrawer) },
                                            modifier = Modifier.size(56.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.List,
                                                contentDescription = stringResource(R.string.reader_index)
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                    }

                                    FloatingActionButton(
                                        onClick = { onAction(PdfReaderAction.ToggleOrientation) },
                                        modifier = Modifier.size(56.dp)
                                    ) {
                                        Icon(
                                            painter = painterResource(
                                                id = if (state.orientation == PdfViewerOrientation.Horizontal) {
                                                    R.drawable.ic_swap_vertical
                                                } else {
                                                    R.drawable.ic_swap_horizontal
                                                }
                                            ),
                                            contentDescription = stringResource(
                                                if (state.orientation == PdfViewerOrientation.Horizontal) {
                                                    R.string.reader_orientation_vertical
                                                } else {
                                                    R.string.reader_orientation_horizontal
                                                }
                                            )
                                        )
                                    }

                                    if (state.enableAnnotations) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        FloatingActionButton(
                                            onClick = {
                                                showNoteDialog = true
                                                onAction(PdfReaderAction.CloseFab)
                                            },
                                            modifier = Modifier.size(56.dp),
                                            containerColor = if (state.currentPageNote != null) {
                                                Color(0xFFFFF9C4)
                                            } else {
                                                MaterialTheme.colorScheme.primaryContainer
                                            }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = stringResource(R.string.reader_page_note),
                                                tint = if (state.currentPageNote != null) {
                                                    Color.Black
                                                } else {
                                                    MaterialTheme.colorScheme.onPrimaryContainer
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                            FloatingActionButton(
                                shape = FloatingActionButtonDefaults.smallShape,
                                onClick = { onAction(PdfReaderAction.ToggleFab) }
                            ) {
                                Icon(
                                    imageVector = if (state.isFabExpanded) Icons.Default.Close else Icons.Default.Add,
                                    contentDescription = stringResource(
                                        if (state.isFabExpanded) {
                                            R.string.reader_close_menu
                                        } else {
                                            R.string.reader_open_menu
                                        }
                                    )
                                )
                            }
                        }
                    }
                } else {
                    Text(
                        text = stringResource(R.string.reader_error_file),
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }

            if (state.enableSearch) {
                Box(modifier = Modifier.align(Alignment.TopCenter)) {
                    PdfSearchOverlay(
                        isSearchOpen = state.isSearchOpen,
                        searchQuery = state.searchQuery,
                        searchResults = state.searchResults,
                        isSearching = state.isSearching,
                        onQueryChange = { onAction(PdfReaderAction.UpdateSearchQuery(it)) },
                        onClose = { onAction(PdfReaderAction.CloseSearch) },
                        onResultClick = { page ->
                            interactiveState.pendingSearchQuery = state.searchQuery
                            interactiveState.pendingSearchPage = page
                            navigateToPage?.invoke(page)
                            onAction(PdfReaderAction.SelectSearchResult(page))
                        }
                    )
                }
            }
        }

        if (showNoteDialog && state.enableAnnotations) {
            PageNoteBottomSheet(
                existingNote = state.currentPageNote?.noteText,
                onDismissRequest = { showNoteDialog = false },
                onSave = { text ->
                    onAction(PdfReaderAction.SavePageNote(text))
                    showNoteDialog = false
                },
                onDelete = {
                    onAction(PdfReaderAction.DeletePageNote)
                    showNoteDialog = false
                }
            )
        }
    }
}
