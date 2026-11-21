package com.fmaestre98.pdfviewer.sdk.ui.annotations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fmaestre98.pdfviewer.sdk.storage.PdfViewerStorage
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AnnotationsViewModel(
    private val documentId: String,
    private val storage: PdfViewerStorage
) : ViewModel() {

    private val _events = Channel<AnnotationsEvent>()
    val events = _events.receiveAsFlow()

    val state: StateFlow<AnnotationsState> = storage.observeAnnotations(documentId)
        .map { annotations ->
            val highlightItems = annotations.highlights
                .groupBy { it.groupId }
                .values
                .mapNotNull { group ->
                    val first = group.firstOrNull() ?: return@mapNotNull null
                    val combinedSnippet = group.map { it.snippet }.joinToString(" ")
                    AnnotationItem.Highlight(
                        id = "h_${first.groupId}",
                        page = first.page,
                        timestamp = first.updatedAt,
                        snippet = combinedSnippet,
                        color = first.color
                    )
                }

            val noteItems = annotations.notes.map { note ->
                AnnotationItem.PageNote(
                    id = "n_${note.page}_${note.updatedAt}",
                    page = note.page,
                    timestamp = note.updatedAt,
                    noteText = note.noteText,
                    color = note.color
                )
            }

            val bookmarkItems = annotations.bookmarks.map { bookmark ->
                AnnotationItem.Bookmark(
                    id = "b_${bookmark.page}_${bookmark.createdAt}",
                    page = bookmark.page,
                    timestamp = bookmark.createdAt
                )
            }

            AnnotationsState(
                isLoading = false,
                items = (highlightItems + noteItems + bookmarkItems)
                    .sortedByDescending { it.timestamp }
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AnnotationsState(isLoading = true)
        )

    fun onAction(action: AnnotationsAction) {
        when (action) {
            is AnnotationsAction.OnAnnotationClick -> {
                viewModelScope.launch {
                    _events.send(AnnotationsEvent.NavigateToReader(documentId, action.page))
                }
            }
            AnnotationsAction.OnBackClick -> {
                viewModelScope.launch {
                    _events.send(AnnotationsEvent.NavigateBack)
                }
            }
        }
    }

    class Factory(
        private val documentId: String,
        private val storage: PdfViewerStorage
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AnnotationsViewModel(
                documentId = documentId,
                storage = storage
            ) as T
        }
    }
}

data class AnnotationsState(
    val isLoading: Boolean = true,
    val items: List<AnnotationItem> = emptyList()
)

sealed interface AnnotationsAction {
    data class OnAnnotationClick(val page: Int) : AnnotationsAction
    data object OnBackClick : AnnotationsAction
}

sealed interface AnnotationsEvent {
    data class NavigateToReader(val documentId: String, val page: Int) : AnnotationsEvent
    data object NavigateBack : AnnotationsEvent
}
