package com.fmaestre98.pdfviewer.pdfViewer.rendering

import android.graphics.Bitmap
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import com.fmaestre98.pdfviewer.R

/**
 * Drawer that displays thumbnail previews of all PDF pages with page numbers.
 * Allows quick navigation by clicking on a thumbnail.
 *
 * @param isOpen Whether the drawer is currently open
 * @param onDismiss Callback when drawer should be closed
 * @param pageRenderer The renderer for PDF pages
 * @param pageCount Total number of pages
 * @param currentPage Zero-based index of the currently displayed page
 * @param bookmarkedPages Set of page indices that are bookmarked
 * @param onPageSelected Callback when a page thumbnail is clicked
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PageThumbnailDrawer(
    isOpen: Boolean,
    onDismiss: () -> Unit,
    pageRenderer: PdfPageRenderer?,
    pageCount: Int,
    currentPage: Int,
    bookmarkedPages: Set<Int>,
    onPageSelected: (Int) -> Unit,
    content: @Composable () -> Unit,
) {
    val drawerState = rememberDrawerState(initialValue = androidx.compose.material3.DrawerValue.Closed)

    LaunchedEffect(isOpen) {
        if (isOpen) {
            drawerState.open()
        } else {
            drawerState.close()
        }
    }

    LaunchedEffect(drawerState.currentValue) {
        if (drawerState.currentValue == androidx.compose.material3.DrawerValue.Closed && isOpen) {
            onDismiss()
        }
    }

    val density = LocalDensity.current
    val thumbnailWidth = with(density) { 200.dp.toPx().toInt() }
    val thumbnailHeight = with(density) { 300.dp.toPx().toInt() }
    val listState = rememberLazyListState()

    // Scroll to current page when drawer opens
    LaunchedEffect(drawerState.currentValue, currentPage) {
        if (drawerState.currentValue == androidx.compose.material3.DrawerValue.Open && currentPage >= 0 && currentPage < pageCount) {
            listState.animateScrollToItem(currentPage)
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.width(300.dp),
                drawerContainerColor = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(16.dp)
                ) {
                    // Header con título y contador de bookmarks
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                    ) {
                        Text(
                            text = "📚 Índice de Páginas",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.align(Alignment.CenterStart)
                        )

                        // Contador de bookmarks con diseño atractivo
                        if (bookmarkedPages.isNotEmpty()) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.CenterEnd)
                                    .background(
                                        color = Color(0xFFFFD700).copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(20.dp)
                                    )
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_bookmarked),
                                        contentDescription = "Bookmarks",
                                        tint = Color(0xFFFFA000),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "${bookmarkedPages.size}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFFF6B00)
                                    )
                                }
                            }
                        }
                    }

                    if (pageCount > 0) {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            itemsIndexed((0 until pageCount).toList()) { index, pageIndex ->
                                ThumbnailItem(
                                    pageIndex = pageIndex,
                                    pageRenderer = pageRenderer,
                                    thumbnailWidth = thumbnailWidth,
                                    thumbnailHeight = thumbnailHeight,
                                    isCurrentPage = pageIndex == currentPage,
                                    isBookmarked = bookmarkedPages.contains(pageIndex),
                                    onClick = {
                                        onPageSelected(pageIndex)
                                        onDismiss()
                                    }
                                )
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Cargando páginas...",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }
    ) {
        content()
    }
}

/**
 * Individual thumbnail item in the drawer with enhanced visual effects for bookmarks.
 */
@Composable
private fun ThumbnailItem(
    pageIndex: Int,
    pageRenderer: PdfPageRenderer?,
    thumbnailWidth: Int,
    thumbnailHeight: Int,
    isCurrentPage: Boolean,
    isBookmarked: Boolean,
    onClick: () -> Unit,
) {
    var thumbnail by remember(pageIndex) { mutableStateOf<Bitmap?>(null) }
    var isLoading by remember(pageIndex) { mutableStateOf(true) }
    val rotation = remember { Animatable(0f) }
    val scale = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()

    // Efecto de animación sutil para páginas bookmarked
    if (isBookmarked) {
        LaunchedEffect(isBookmarked) {
            scope.launch {
                rotation.animateTo(
                    targetValue = 5f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(durationMillis = 3000, easing = androidx.compose.animation.core.FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    )
                )
            }
        }
    } else {
        LaunchedEffect(isBookmarked) {
            rotation.snapTo(0f)
        }
    }

    // Load thumbnail asynchronously
    LaunchedEffect(pageIndex, pageRenderer) {
        if (pageRenderer != null) {
            isLoading = true
            // Render at low resolution for thumbnails
            val bitmap = pageRenderer.renderPage(
                pageIndex = pageIndex,
                scaleFactor = 0.45f, // Low resolution for thumbnails
                maxWidth = thumbnailWidth,
                maxHeight = thumbnailHeight
            )?.bitmap
            thumbnail = bitmap
            isLoading = false
        }
    }

    // Determinar el color de acento basado en si está bookmarked
    val accentColor = if (isBookmarked) {
        MaterialTheme.colorScheme.tertiary
    } else if (isCurrentPage) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clickable(onClick = onClick)
            .graphicsLayer {
                rotationZ = if (isBookmarked) rotation.value else 0f
                scaleX = scale.value
                scaleY = scale.value
            }
            .drawBehind {
                if (isBookmarked) {
                    // Efecto de brillo sutil en el fondo
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color.Yellow.copy(alpha = 0.1f),
                                Color.Transparent
                            ),
                            center = Offset(size.width * 0.8f, size.height * 0.2f),
                            radius = size.minDimension * 0.5f
                        ),
                        blendMode = BlendMode.Overlay
                    )
                }
            },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrentPage) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isCurrentPage) 8.dp else if (isBookmarked) 6.dp else 2.dp
        ),
        border = if (isCurrentPage || isBookmarked) {
            androidx.compose.foundation.BorderStroke(
                width = if (isBookmarked) 3.dp else 2.dp,
                color = accentColor
            )
        } else {
            null
        }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            accentColor.copy(alpha = 0.05f),
                            Color.Transparent
                        )
                    )
                )
        ) {
            when {
                isLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        Color.LightGray.copy(alpha = 0.3f),
                                        Color.LightGray.copy(alpha = 0.6f)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        // Indicador de carga con animación
                        LoadingSparkle()
                    }
                }
                thumbnail != null -> {
                    Image(
                        bitmap = thumbnail!!.asImageBitmap(),
                        contentDescription = "Página ${pageIndex + 1}",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )

                    // Superponer un degradado para mejor legibilidad del texto
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.3f)
                                    ),
                                    startY = 0f,
                                    endY = Float.POSITIVE_INFINITY
                                )
                            )
                    )
                }
                else -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.LightGray.copy(alpha = 0.5f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "📄",
                            fontSize = 32.sp,
                            color = Color.Gray.copy(alpha = 0.5f)
                        )
                    }
                }
            }

            // Indicador de página actual
            if (isCurrentPage) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primary,
                            shape = CircleShape
                        )
                        .padding(4.dp)
                ) {
                    Text(
                        text = "📍",
                        fontSize = 12.sp,
                        modifier = Modifier.padding(2.dp)
                    )
                }
            }

            // Indicador de bookmark con diseño especial
            if (isBookmarked) {
                BookmarkIndicator(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                )
            }

            // Número de página con diseño mejorado
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 12.dp, bottom = 8.dp)
            ) {
                Text(
                    text = "${pageIndex + 1}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isBookmarked) Color.White else Color.White,
                    modifier = Modifier
                        .shadow(2.dp, shape = CircleShape)
                        .background(
                            color = if (isBookmarked) Color(0xFFFF6B00) else Color.Black.copy(alpha = 0.7f),
                            shape = CircleShape
                        )
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    fontSize = 14.sp
                )
            }

            // Etiqueta de bookmark (solo si está bookmarked)
            if (isBookmarked) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 8.dp, bottom = 8.dp)
                ) {
                    Text(
                        text = "⭐ Guardada",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFFD700),
                        modifier = Modifier
                            .background(
                                color = Color(0xFF4A1E00).copy(alpha = 0.8f),
                                shape = RoundedCornerShape(10.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}

/**
 * Indicador de bookmark con diseño especial animado
 */
@Composable
private fun BookmarkIndicator(modifier: Modifier = Modifier) {
    val pulseAnim = remember { Animatable(1f) }
    val rotationAnim = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        // Animación de pulso
        scope.launch {
            pulseAnim.animateTo(
                targetValue = 1.2f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 1500),
                    repeatMode = RepeatMode.Reverse
                )
            )
        }

        // Ligera rotación oscilante
        scope.launch {
            rotationAnim.animateTo(
                targetValue = 15f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 2000, easing = androidx.compose.animation.core.FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                )
            )
        }
    }

    Box(
        modifier = modifier
            .size(36.dp)
            .graphicsLayer {
                scaleX = pulseAnim.value
                scaleY = pulseAnim.value
                rotationZ = rotationAnim.value
            }
            .shadow(4.dp, shape = CircleShape)
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFFFD700),
                        Color(0xFFFF6B00)
                    )
                ),
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_bookmarked),
            contentDescription = "Bookmarked",
            tint = Color.White,
            modifier = Modifier.size(20.dp)
        )

        // Efecto de brillo interno
        Box(
            modifier = Modifier
                .size(16.dp)
                .align(Alignment.TopStart)
                .background(
                    color = Color.White.copy(alpha = 0.3f),
                    shape = CircleShape
                )
        )
    }
}

/**
 * Efecto de sparkle para el estado de carga
 */
@Composable
private fun LoadingSparkle() {
    val sparkles = remember { List(5) { Animatable(0f) } }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        sparkles.forEachIndexed { index, animatable ->
            scope.launch {
                animatable.animateTo(
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(durationMillis = 1000),
                        repeatMode = RepeatMode.Reverse
                    )
                )
            }
        }
    }

    Box(
        modifier = Modifier.size(40.dp),
        contentAlignment = Alignment.Center
    ) {
        sparkles.forEachIndexed { index, animatable ->
            val angle = (index * 72).toFloat() // 5 sparkles, 72 grados entre cada uno
            val distance = 15

            Box(
                modifier = Modifier
                    .offset(
                        x = (distance * kotlin.math.cos(Math.toRadians(angle.toDouble()))).toFloat().dp,
                        y = (distance * kotlin.math.sin(Math.toRadians(angle.toDouble()))).toFloat().dp
                    )
                    .size(6.dp)
                    .graphicsLayer {
                        alpha = animatable.value
                        scaleX = animatable.value
                        scaleY = animatable.value
                    }
                    .background(
                        color = Color(0xFFFFD700).copy(alpha = animatable.value),
                        shape = CircleShape
                    )
            )
        }

        Text(
            text = "✨",
            fontSize = 20.sp,
            modifier = Modifier.scale(1.5f)
        )
    }
}