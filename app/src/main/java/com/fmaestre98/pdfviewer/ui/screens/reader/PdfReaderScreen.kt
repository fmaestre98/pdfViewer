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
import com.fmaestre98.pdfviewer.pdfViewer.ui.ComposePdfViewer
import com.fmaestre98.pdfviewer.ui.util.ObserveAsEvents
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

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
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                navigationIcon = {
                    IconButton(onClick = { onAction(PdfReaderAction.OnBackClick) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.reader_back))
                    }
                }
            )
        }
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
                    ComposePdfViewer(
                        pdfFile = file,
                        initialPage = state.initialPage,
                        modifier = Modifier.fillMaxSize(),
                        onNavigationStateChange = { currentPage, _ ->
                            onAction(PdfReaderAction.OnPageChanged(currentPage))
                        },
                        onError = {
                            // Optionally handle error
                        }
                    )
                } else {
                    Text(
                        text = stringResource(R.string.reader_error_file),
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
        }
    }
}
