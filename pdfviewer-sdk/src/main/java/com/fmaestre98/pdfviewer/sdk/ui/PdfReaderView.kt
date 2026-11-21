package com.fmaestre98.pdfviewer.sdk.ui

import android.content.Context
import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fmaestre98.pdfviewer.sdk.core.viewmodel.PdfViewerViewModel
import com.fmaestre98.pdfviewer.sdk.data.db.PdfSdkDatabase
import com.fmaestre98.pdfviewer.sdk.data.repository.SdkBookmarkRepositoryImpl
import com.fmaestre98.pdfviewer.sdk.data.repository.SdkHighlightRepositoryImpl
import com.fmaestre98.pdfviewer.sdk.data.repository.SdkPageNoteRepositoryImpl
import com.fmaestre98.pdfviewer.sdk.model.PdfSource
import com.fmaestre98.pdfviewer.sdk.model.PdfViewerConfig
import com.fmaestre98.pdfviewer.sdk.ui.reader.PdfReaderEvent
import com.fmaestre98.pdfviewer.sdk.ui.reader.PdfReaderScreen
import com.fmaestre98.pdfviewer.sdk.ui.reader.PdfReaderViewModel
import com.fmaestre98.pdfviewer.sdk.ui.util.ObserveAsEvents
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

@Composable
fun PdfReaderView(
    source: PdfSource,
    modifier: Modifier = Modifier,
    config: PdfViewerConfig = PdfViewerConfig(),
    onClose: () -> Unit = {},
    onShare: ((Uri) -> Unit)? = null
) {
    val context = LocalContext.current
    var resolvedFilePath by remember(source) { mutableStateOf<String?>(null) }
    var isResolving by remember(source) { mutableStateOf(true) }

    LaunchedEffect(source) {
        isResolving = true
        withContext(Dispatchers.IO) {
            val path = when (source) {
                is PdfSource.FromFile -> source.file.absolutePath
                is PdfSource.FromUri -> resolveUriToFile(context, source.uri)?.absolutePath
                is PdfSource.FromAsset -> copyAssetToFile(context, source.assetPath)?.absolutePath
            }
            resolvedFilePath = path
            isResolving = false
        }
    }

    if (isResolving) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val filePath = resolvedFilePath
    if (filePath == null) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No se pudo cargar el archivo PDF")
        }
        return
    }

    val db = remember(context) { PdfSdkDatabase.getInstance(context) }
    val bookmarkRepo = remember(db) { SdkBookmarkRepositoryImpl(db.bookmarkDao()) }
    val highlightRepo = remember(db) { SdkHighlightRepositoryImpl(db.highlightDao()) }
    val pageNoteRepo = remember(db) { SdkPageNoteRepositoryImpl(db.pageNoteDao()) }

    val documentId = remember(source) { source.getDocumentId() }

    val factory = remember(documentId, filePath, config, bookmarkRepo, highlightRepo, pageNoteRepo) {
        PdfReaderViewModel.Factory(
            documentId = documentId,
            filePath = filePath,
            initialPage = config.initialPage,
            initialOrientation = config.initialOrientation,
            bookmarkRepository = bookmarkRepo,
            highlightRepository = highlightRepo,
            pageNoteRepository = pageNoteRepo
        )
    }

    val readerViewModel: PdfReaderViewModel = viewModel(factory = factory, key = documentId)
    val pdfViewerViewModel: PdfViewerViewModel = viewModel(key = "pdf_viewer_$documentId")

    val state by readerViewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    ObserveAsEvents(readerViewModel.events) { event ->
        when (event) {
            is PdfReaderEvent.NavigateBack -> onClose()
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
        pdfViewerViewModel = pdfViewerViewModel,
        onAction = readerViewModel::onAction,
        onClose = onClose
    )
}

private fun resolveUriToFile(context: Context, uri: Uri): File? {
    return try {
        if (uri.scheme == "file") {
            uri.path?.let { File(it) }
        } else {
            val contentResolver = context.contentResolver
            val tempFile = File(context.cacheDir, "temp_pdf_${System.currentTimeMillis()}.pdf")
            contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output)
                }
            }
            tempFile
        }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

private fun copyAssetToFile(context: Context, assetPath: String): File? {
    return try {
        val tempFile = File(context.cacheDir, "asset_pdf_${assetPath.replace("/", "_")}")
        context.assets.open(assetPath).use { input ->
            FileOutputStream(tempFile).use { output ->
                input.copyTo(output)
            }
        }
        tempFile
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}
