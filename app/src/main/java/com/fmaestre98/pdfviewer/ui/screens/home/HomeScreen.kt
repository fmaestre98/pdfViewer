package com.fmaestre98.pdfviewer.ui.screens.home

import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.Image
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fmaestre98.pdfviewer.R
import com.fmaestre98.pdfviewer.models.Book
import com.fmaestre98.pdfviewer.ui.util.ObserveAsEvents
import com.fmaestre98.pdfviewer.pdfViewer.rendering.PdfRendererManager
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

import androidx.compose.ui.res.painterResource


@Composable
fun HomeRoot(
    onNavigateToReader: (String) -> Unit,
    onNavigateToAnnotations: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            is HomeEvent.NavigateToReader -> onNavigateToReader(event.uri)
            is HomeEvent.ShowSnackbar -> {
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(message = context.getString(event.messageResId))
                }
            }
        }
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri: Uri? ->
            uri?.let {
                // Read metadata
                var displayName = "Unknown PDF"
                var sizeBytes = -1L
                context.contentResolver.query(it, null, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (nameIndex != -1) {
                            displayName = cursor.getString(nameIndex)
                        }
                        val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                        if (sizeIndex != -1) {
                            sizeBytes = cursor.getLong(sizeIndex)
                        }
                    }
                }
                viewModel.onAction(HomeAction.OnPdfSelected(it, displayName, sizeBytes))
            }
        }
    )

    HomeScreen(
        state = state,
        snackbarHostState = snackbarHostState,
        onAction = { action ->
            if (action is HomeAction.OnPdfSelected) {
                 // Intercepted above, but here we trigger the picker
                 launcher.launch(arrayOf("application/pdf"))
            } else {
                viewModel.onAction(action)
            }
        },
        onPickPdfClick = {
             launcher.launch(arrayOf("application/pdf"))
        },
        onNavigateToAnnotations = onNavigateToAnnotations
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    state: HomeState,
    snackbarHostState: SnackbarHostState,
    onAction: (HomeAction) -> Unit,
    onPickPdfClick: () -> Unit,
    onNavigateToAnnotations: () -> Unit
) {
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.home_title)) },
                actions = {
                    IconButton(onClick = onNavigateToAnnotations) {
                        Icon(
                            painter = painterResource(R.drawable.ic_bookmarked),
                            contentDescription = stringResource(R.string.annotations_title),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onPickPdfClick) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.home_add_pdf))
            }
        },
        containerColor = Color(0xFFF5E6D3) // Warm wallpaper color
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (state.isLoading && state.books.isEmpty()) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (state.books.isEmpty()) {
                EmptyState(modifier = Modifier.align(Alignment.Center))
            } else {
                val booksInRows = state.books.chunked(3)
                
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 88.dp, top = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(
                        items = booksInRows,
                        key = { it.first().uri }
                    ) { rowBooks ->
                        ShelfRow(
                            books = rowBooks,
                            onClick = { book -> onAction(HomeAction.OnBookClick(book.uri)) },
                            onDelete = { book -> onAction(HomeAction.OnDeleteBook(book.uri)) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(R.string.home_empty_title),
            style = MaterialTheme.typography.titleLarge
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.home_empty_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun ShelfRow(
    books: List<Book>,
    onClick: (Book) -> Unit,
    onDelete: (Book) -> Unit
) {
    val woodColor = Color(0xFF8B6F47)
    val woodColorLight = Color(0xFFA0826D)
    val woodColorDark = Color(0xFF6B5235)

    Box(modifier = Modifier.fillMaxWidth().height(170.dp)) {
        // Shelf background
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(24.dp)
                .shadow(4.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(woodColorLight, woodColor, woodColorDark)
                    )
                )
        )
        
        // Books
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp, start = 16.dp, end = 16.dp)
                .align(Alignment.BottomCenter),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.Bottom
        ) {
            books.forEach { book ->
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.BottomCenter) {
                    BookShelfItem(
                        book = book,
                        onClick = { onClick(book) },
                        onDelete = { onDelete(book) }
                    )
                }
            }
            // Fill empty spaces if there are less than 3 books so they stay aligned to grid
            repeat(3 - books.size) {
                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun BookShelfItem(
    book: Book,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val rotation = remember(book.uri) { (-4..4).random().toFloat() }
    val bookColor = remember(book.uri) {
        listOf(
            Color(0xFFE57373), Color(0xFF81C784), Color(0xFF64B5F6),
            Color(0xFFFFB74D), Color(0xFF9575CD), Color(0xFF4DB6AC),
            Color(0xFFD4E157), Color(0xFF7986CB)
        ).random()
    }
    
    val context = LocalContext.current
    var thumbnail by remember(book.uri) { mutableStateOf<android.graphics.Bitmap?>(null) }

    LaunchedEffect(book.uri) {
        withContext(Dispatchers.IO) {
            try {
                val fd = context.contentResolver.openFileDescriptor(Uri.parse(book.uri), "r")
                if (fd != null) {
                    val manager = PdfRendererManager.create(fd)
                    if (manager.pageCount > 0) {
                        thumbnail = manager.renderPage(
                            pageIndex = 0,
                            scaleFactor = 0.45f,
                            maxWidth = 300,
                            maxHeight = 450
                        )
                    }
                    manager.close() // this also closes fd
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    Box(
        modifier = modifier
            .width(90.dp)
            .height(130.dp)
            .graphicsLayer { rotationZ = rotation }
            .shadow(6.dp, RoundedCornerShape(topEnd = 12.dp, bottomEnd = 12.dp, topStart = 4.dp, bottomStart = 4.dp))
            .background(bookColor, RoundedCornerShape(topEnd = 12.dp, bottomEnd = 12.dp, topStart = 4.dp, bottomStart = 4.dp))
            .clickable { onClick() }
    ) {
        if (thumbnail != null) {
            Image(
                bitmap = thumbnail!!.asImageBitmap(),
                contentDescription = "Cover for ${book.displayName}",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(topEnd = 12.dp, bottomEnd = 12.dp, topStart = 4.dp, bottomStart = 4.dp))
            )
            
            // Subtle gradient to ensure text readability over the image
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.5f),
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.7f)
                            )
                        ),
                        shape = RoundedCornerShape(topEnd = 12.dp, bottomEnd = 12.dp, topStart = 4.dp, bottomStart = 4.dp)
                    )
            )
        }
        
        // Spine
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(8.dp)
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(Color.Black.copy(alpha = 0.3f), Color.Transparent)
                    ),
                    shape = RoundedCornerShape(topStart = 4.dp, bottomStart = 4.dp)
                )
                .align(Alignment.CenterStart)
        )
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = book.displayName,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 4.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = stringResource(R.string.home_delete),
                        tint = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.size(16.dp)
                    )
                }
                
                if (book.totalPages > 0) {
                    val percentage = (book.lastReadPage * 100) / book.totalPages
                    Text(
                        text = "$percentage%",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Spacer(modifier = Modifier.width(4.dp))
                }
            }
        }
    }
}
