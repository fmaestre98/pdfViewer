package com.fmaestre98.pdfviewer.ui.screens.reader

import android.net.Uri
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fmaestre98.pdfviewer.R
import com.fmaestre98.pdfviewer.pdfViewer.model.PdfViewerOrientation
import com.fmaestre98.pdfviewer.pdfViewer.ui.ComposePdfViewer
import com.fmaestre98.pdfviewer.ui.util.ObserveAsEvents
import kotlinx.coroutines.launch
import java.io.File
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.fmaestre98.pdfviewer.pdfViewer.rendering.PageThumbnailDrawer
import com.fmaestre98.pdfviewer.pdfViewer.rendering.PdfPageRenderer
import com.fmaestre98.pdfviewer.pdfViewer.viewmodel.PdfViewerViewModel
import com.fmaestre98.pdfviewer.repository.NormalizedRect
import com.fmaestre98.pdfviewer.ui.screens.reader.components.PageNoteBottomSheet

@Composable
fun PdfReaderRoot(
    onNavigateBack: () -> Unit,
    viewModel: PdfReaderViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            is PdfReaderEvent.NavigateBack -> onNavigateBack()
            is PdfReaderEvent.ShowSnackbar -> {
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(message = context.getString(event.messageResId))
                }
            }
        }
    }

    PdfReaderScreen(
        state = state,
        snackbarHostState = snackbarHostState,
        onAction = viewModel::onAction
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun PdfReaderScreen(
    state: PdfReaderState,
    snackbarHostState: SnackbarHostState,
    onAction: (PdfReaderAction) -> Unit
) {
    val pdfViewerViewModel: PdfViewerViewModel = hiltViewModel()
    val interactiveState = pdfViewerViewModel.interactiveState

    var pageRenderer: PdfPageRenderer? by remember { mutableStateOf(null) }
    var navigateToPage: ((Int) -> Unit)? by remember { mutableStateOf(null) }
    var showNoteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(state.highlights) {
        val mappedHighlights = state.highlights.mapValues { (_, list) ->
            list.map { repoHighlight ->
                com.fmaestre98.pdfviewer.pdfViewer.model.HighlightData(
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
                val context = LocalContext.current

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
                            orientation = PdfViewerOrientation.Horizontal,
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

                    // Botón de bookmark en la parte superior derecha
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 16.dp, end = 16.dp),
                        contentAlignment = Alignment.TopEnd
                    ) {
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
                                contentDescription = if (state.isCurrentPageBookmarked) "Quitar bookmark" else "Añadir bookmark",
                                tint = Color(0xFFFFA000)
                            )
                        }
                    }

                    // Persistent Page Note Icon (Top Left)
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
                                    contentDescription = "Ver nota de página",
                                    tint = Color(0xFFFFA000),
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                    }

                    // Floating Action Toolbar - Visible si hay archivo
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
                                    FloatingActionButton(
                                        onClick = { onAction(PdfReaderAction.ToggleThumbnailDrawer) },
                                        modifier = Modifier.size(56.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.List,
                                            contentDescription = "Índice de páginas"
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    FloatingActionButton(
                                        onClick = {
                                            showNoteDialog = true
                                            onAction(PdfReaderAction.CloseFab)
                                        },
                                        modifier = Modifier.size(56.dp),
                                        containerColor = if (state.currentPageNote != null) Color(
                                            0xFFFFF9C4
                                        ) else MaterialTheme.colorScheme.primaryContainer
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Nota de página",
                                            tint = if (state.currentPageNote != null) Color.Black else MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                }
                            }
                            FloatingActionButton(
                                shape = FloatingActionButtonDefaults.smallShape,
                                onClick = { onAction(PdfReaderAction.ToggleFab) }
                            ) {
                                Icon(
                                    imageVector = if (state.isFabExpanded) Icons.Default.Close else Icons.Default.Add,
                                    contentDescription = if (state.isFabExpanded) "Cerrar menú" else "Abrir menú"
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
        }

        if (showNoteDialog) {
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