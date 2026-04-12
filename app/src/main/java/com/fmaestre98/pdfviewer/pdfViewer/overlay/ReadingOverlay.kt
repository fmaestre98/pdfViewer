package com.fmaestre98.pdfviewer.pdfViewer.overlay


import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.fmaestre98.pdfviewer.pdfViewer.model.PdfWord

@Composable
fun ReadingOverlay(
    currentReadingWord: PdfWord?,
    pageWidth: Int,
    pageHeight: Int,
    modifier: Modifier = Modifier
) {
    currentReadingWord?.let { word ->
        val density = LocalDensity.current

        Box(modifier = with(density) {
            modifier
                .width(pageWidth.toDp())
                .height(pageHeight.toDp())
        }) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                // Dibujar un subrayado animado
                val underlineY = word.rect.bottom + 2.dp.toPx() // 2dp debajo de la palabra

                drawRect(
                    color = Color.Yellow.copy(alpha = 0.3f),
                    topLeft = Offset(word.rect.left, word.rect.top),
                    size = Size(word.rect.width(), word.rect.height())
                )

                // Subrayado animado (podría usar Animatable para animar el ancho)
                drawRect(
                    color = Color.Red.copy(alpha = 0.7f),
                    topLeft = Offset(word.rect.left, underlineY),
                    size = Size(word.rect.width(), 4.dp.toPx())
                )
            }
        }
    }
}