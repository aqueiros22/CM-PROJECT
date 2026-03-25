package com.example.fieldsense.data

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AttachmentViewModel(private val repository: AttachmentRepository) : ViewModel() {

    private val attachmentsCache = mutableMapOf<Int, StateFlow<List<Attachment>>>()

    fun getAttachmentsForVisit(visitId: Int): StateFlow<List<Attachment>> {
        return attachmentsCache.getOrPut(visitId) {
            repository.getAttachmentsForVisit(visitId)
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
        }
    }

    fun insertAttachment(visitId: Int, fileName: String, fileUri: Uri, type: String) {
        viewModelScope.launch {
            repository.insertAttachment(visitId, fileName, fileUri, type)
        }
    }

    fun deleteAttachment(attachment: Attachment) {
        viewModelScope.launch {
            repository.deleteAttachment(attachment)
        }
    }

    fun syncPendingAttachments() {
        viewModelScope.launch {
            repository.syncPendingAttachments()
        }
    }

    fun onNetworkRestored() {
        viewModelScope.launch {
            repository.syncPendingAttachments()
        }
    }
}

class AttachmentViewModelFactory(
    private val repository: AttachmentRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return AttachmentViewModel(repository) as T
    }
}