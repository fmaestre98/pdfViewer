package com.fmaestre98.pdfviewer

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.OpenableColumns
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.fmaestre98.pdfviewer.repository.PDFRepository
import com.fmaestre98.pdfviewer.ui.navigation.AppNavigation
import com.fmaestre98.pdfviewer.ui.screens.annotations.AnnotationsRoot
import com.fmaestre98.pdfviewer.ui.screens.home.HomeRoot
import com.fmaestre98.pdfviewer.ui.screens.reader.PdfReaderRoot
import com.fmaestre98.pdfviewer.ui.theme.PdfViewerTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var pdfRepository: PDFRepository

    private val intentFlow = MutableStateFlow<Intent?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        intentFlow.value = intent

        setContent {
            PdfViewerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()

                    LaunchedEffect(Unit) {
                        intentFlow.collect { incomingIntent ->
                            handlePdfIntent(incomingIntent) { bookUri ->
                                navController.navigate(AppNavigation.createReaderRoute(bookUri))
                            }
                        }
                    }

                    NavHost(
                        navController = navController,
                        startDestination = AppNavigation.ROUTE_HOME
                    ) {
                        composable(route = AppNavigation.ROUTE_HOME) {
                            HomeRoot(
                                onNavigateToReader = { uri ->
                                    navController.navigate(AppNavigation.createReaderRoute(uri))
                                },
                                onNavigateToAnnotations = {
                                    navController.navigate(AppNavigation.ROUTE_ANNOTATIONS)
                                }
                            )
                        }
                        composable(route = AppNavigation.ROUTE_ANNOTATIONS) {
                            AnnotationsRoot(
                                onNavigateBack = {
                                    navController.popBackStack()
                                },
                                onNavigateToReader = { uri, page ->
                                    navController.navigate(AppNavigation.createReaderRoute(uri, page))
                                }
                            )
                        }
                        composable(
                            route = AppNavigation.ROUTE_READER,
                            arguments = listOf(
                                navArgument("encodedUri") { type = NavType.StringType },
                                navArgument("initialPage") {
                                    type = NavType.StringType
                                    nullable = true
                                    defaultValue = null
                                }
                            )
                        ) {
                            PdfReaderRoot(
                                onNavigateBack = {
                                    navController.popBackStack()
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intentFlow.value = intent
    }

    private fun handlePdfIntent(intent: Intent?, onPdfReady: (String) -> Unit) {
        if (intent == null) return
        val action = intent.action
        val uri: Uri? = when (action) {
            Intent.ACTION_VIEW -> intent.data
            Intent.ACTION_SEND -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(Intent.EXTRA_STREAM)
                }
            }
            else -> null
        }

        if (uri != null) {
            // Consume intent to prevent re-processing on rotation / recomposition
            intent.data = null
            intent.removeExtra(Intent.EXTRA_STREAM)
            intentFlow.value = null

            lifecycleScope.launch {
                try {
                    val (displayName, sizeBytes) = getUriMetadata(uri)
                    val book = pdfRepository.addBook(uri, displayName, sizeBytes)
                    onPdfReady(book.uri)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun getUriMetadata(uri: Uri): Pair<String, Long> {
        var displayName = "Opened PDF"
        var sizeBytes = -1L
        try {
            if (uri.scheme == "content") {
                contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (nameIndex != -1) {
                            val name = cursor.getString(nameIndex)
                            if (!name.isNullOrBlank()) displayName = name
                        }
                        val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                        if (sizeIndex != -1) {
                            sizeBytes = cursor.getLong(sizeIndex)
                        }
                    }
                }
            } else if (uri.scheme == "file") {
                val file = uri.path?.let { File(it) }
                if (file != null && file.exists()) {
                    displayName = file.name
                    sizeBytes = file.length()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        if (!displayName.endsWith(".pdf", ignoreCase = true)) {
            val lastSegment = uri.lastPathSegment
            if (!lastSegment.isNullOrBlank()) {
                displayName = if (lastSegment.endsWith(".pdf", ignoreCase = true)) lastSegment else "$lastSegment.pdf"
            }
        }
        return Pair(displayName, sizeBytes)
    }
}