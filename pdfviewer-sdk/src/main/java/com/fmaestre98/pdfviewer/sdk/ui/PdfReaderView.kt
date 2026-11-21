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
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fmaestre98.pdfviewer.sdk.R
import com.fmaestre98.pdfviewer.sdk.core.viewmodel.PdfViewerViewModel
import com.fmaestre98.pdfviewer.sdk.model.PdfSource
import com.fmaestre98.pdfviewer.sdk.model.PdfViewerConfig
import com.fmaestre98.pdfviewer.sdk.storage.PdfViewerStorage
import com.fmaestre98.pdfviewer.sdk.storage.rememberDefaultPdfViewerStorage
import com.fmaestre98.pdfviewer.sdk.ui.reader.PdfReaderEvent
import com.fmaestre98.pdfviewer.sdk.ui.reader.PdfReaderScreen
import com.fmaestre98.pdfviewer.sdk.ui.reader.PdfReaderViewModel
import com.fmaestre98.pdfviewer.sdk.ui.util.ObserveAsEvents
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest

@Composable
fun PdfReaderView(
    source: PdfSource,
    modifier: Modifier = Modifier,
    documentId: String = source.getDocumentId(),
    config: PdfViewerConfig = PdfViewerConfig(),
    storage: PdfViewerStorage = rememberDefaultPdfViewerStorage(),
    onClose: () -> Unit = {}
) {
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        ensurePdfBoxInitialized(context)
    }

    var resolvedFilePath by remember(source) { mutableStateOf<String?>(null) }
    var isResolving by remember(source) { mutableStateOf(true) }

    LaunchedEffect(source, documentId) {
        isResolving = true
        withContext(Dispatchers.IO) {
            val path = when (source) {
                is PdfSource.FromFile -> source.file.absolutePath
                is PdfSource.FromUri -> resolveUriToFile(context, source.uri, documentId)?.absolutePath
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
            Text(stringResource(R.string.reader_error_file))
        }
        return
    }

    val factory = remember(documentId, filePath, config, storage) {
        PdfReaderViewModel.Factory(
            documentId = documentId,
            filePath = filePath,
            config = config,
            storage = storage
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
        selectionHandleColor = config.colors.selectionHandle,
        selectionHighlightColor = config.colors.selectionHighlight,
        onAction = readerViewModel::onAction
    )
}

private val pdfBoxInitLock = Any()

@Volatile
private var pdfBoxInitialized = false

private fun ensurePdfBoxInitialized(context: Context) {
    if (!pdfBoxInitialized) {
        synchronized(pdfBoxInitLock) {
            if (!pdfBoxInitialized) {
                PDFBoxResourceLoader.init(context.applicationContext)
                pdfBoxInitialized = true
            }
        }
    }
}

private fun resolveUriToFile(context: Context, uri: Uri, documentId: String): File? {
    return try {
        if (uri.scheme == "file") {
            uri.path?.let { File(it) }
        } else {
            val cacheName = "pdf_${stableHash(documentId)}.pdf"
            val cacheFile = File(context.cacheDir, cacheName)
            if (!cacheFile.exists() || cacheFile.length() == 0L) {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(cacheFile).use { output ->
                        input.copyTo(output)
                    }
                } ?: return null
            }
            cacheFile
        }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

private fun copyAssetToFile(context: Context, assetPath: String): File? {
    return try {
        val cacheName = "asset_pdf_${stableHash(assetPath)}.pdf"
        val tempFile = File(context.cacheDir, cacheName)
        if (!tempFile.exists() || tempFile.length() == 0L) {
            context.assets.open(assetPath).use { input ->
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output)
                }
            }
        }
        tempFile
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

private fun stableHash(value: String): String {
    val digest = MessageDigest.getInstance("SHA-256").digest(value.toByteArray())
    return digest.take(16).joinToString("") { "%02x".format(it) }
}
