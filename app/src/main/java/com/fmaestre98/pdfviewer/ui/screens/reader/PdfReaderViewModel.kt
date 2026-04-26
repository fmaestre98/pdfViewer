package com.fmaestre98.pdfviewer.ui.screens.reader

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fmaestre98.pdfviewer.R
import com.fmaestre98.pdfviewer.repository.PDFRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PdfReaderViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val pdfRepository: PDFRepository
) : ViewModel() {

    private val encodedUri: String = checkNotNull(savedStateHandle["encodedUri"])
    private val uri: String = Uri.decode(encodedUri)

    private val _state = MutableStateFlow(PdfReaderState(uri = uri))
    val state = _state.asStateFlow()

    private val _events = Channel<PdfReaderEvent>()
    val events = _events.receiveAsFlow()

    init {
        loadBookData()
    }

    private fun loadBookData() {
        viewModelScope.launch {
            try {
                val book = pdfRepository.getBookByUri(uri)
                if (book != null) {
                    _state.update { 
                        it.copy(
                            isLoading = false,
                            initialPage = book.lastReadPage,
                            filePath = book.filePath,
                            error = null
                        ) 
                    }
                } else {
                    _state.update { it.copy(isLoading = false, error = "Libro no encontrado en la biblioteca") }
                    _events.send(PdfReaderEvent.ShowSnackbar(R.string.reader_error_file))
                }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message) }
                _events.send(PdfReaderEvent.ShowSnackbar(R.string.reader_error_file))
            }
        }
    }

    fun onAction(action: PdfReaderAction) {
        when (action) {
            is PdfReaderAction.OnPageChanged -> {
                viewModelScope.launch {
                    pdfRepository.updateLastReadPage(uri, action.pageIndex)
                }
            }
            PdfReaderAction.OnBackClick -> {
                viewModelScope.launch {
                    _events.send(PdfReaderEvent.NavigateBack)
                }
            }
        }
    }
}
