package com.fmaestre98.pdfviewer.pdfViewer.overlay

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp

@Composable
fun TextSelectionMenu(
    modifier: Modifier = Modifier,
    onCopy: () -> Unit,
    onHighlightRequested: (String) -> Unit,
    onDismiss: () -> Unit
) {
    // Local state for showing color picker vs main menu
    var showColorPicker by remember { mutableStateOf(false) }

    // Highlight colors
    val highlightColors = remember {
        listOf(
            "#FFEB3B" to Color(0xFFFFEB3B), // Yellow
            "#66BB6A" to Color(0xFF66BB6A), // Green
            "#42A5F5" to Color(0xFF42A5F5), // Blue
            "#EF5350" to Color(0xFFEF5350), // Pink/Red
            "#FFA726" to Color(0xFFFFA726), // Orange
            "#AB47BC" to Color(0xFFAB47BC), // Purple
        )
    }

    if (showColorPicker) {
        // Color Picker Modal
        Surface(
            modifier = modifier,
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 4.dp,
            shadowElevation = 4.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                highlightColors.forEach { (colorHex, color) ->
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .size(20.dp)
                            .background(color = color, shape = CircleShape)
                            .border(
                                width = 1.dp,
                                color = Color.Gray.copy(alpha = 0.4f),
                                shape = CircleShape
                            )
                            .pointerInput(colorHex) {
                                detectTapGestures {
                                    onHighlightRequested(colorHex)
                                    showColorPicker = false
                                }
                            }
                    )
                }
                // Close button (X) — returns to main menu
                IconButton(
                    onClick = { showColorPicker = false },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cerrar",
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    } else {
        // Main floating menu (Copy / Highlight)
        Surface(
            modifier = modifier,
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 4.dp,
            shadowElevation = 4.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ActionItem(icon = Icons.Default.Menu, text = "Copiar") {
                    onCopy()
                }
                Box(modifier = Modifier.width(16.dp))
                ActionItem(icon = Icons.Default.Create, text = "Resaltar") {
                    showColorPicker = true
                }
            }
        }
    }
}

@Composable
private fun ActionItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .pointerInput(Unit) {
                detectTapGestures { onClick() }
            }
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = text, modifier = Modifier.size(20.dp))
        Box(modifier = Modifier.width(4.dp))
        Text(text = text, style = MaterialTheme.typography.labelMedium)
    }
}
