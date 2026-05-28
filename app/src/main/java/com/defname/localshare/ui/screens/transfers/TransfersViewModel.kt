package com.defname.localshare.ui.screens.transfers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.defname.localshare.data.UploadRepository
import com.defname.localshare.domain.manager.FeatureFlagsProvider
import com.defname.localshare.domain.manager.UploadManager
import com.defname.localshare.domain.model.DraftUploadBatch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class TransfersViewModel(
    private val uploadManager: UploadManager,
    private val uploadRepository: UploadRepository,
    private val featureFlagsProvider: FeatureFlagsProvider
) : ViewModel() {
    private val _state = MutableStateFlow(TransfersState())
    val state = _state.asStateFlow()

    init {
        observeFeatureFlas()
        observeUploadRequests()
        observeActiveBatches()
    }

    private fun observeFeatureFlas() {
        viewModelScope.launch {
            featureFlagsProvider.uploadFeature.collect { active ->
                _state.update {
                    it.copy(uploadFeatureActive = active)
                }
            }
        }
    }

    private fun observeUploadRequests() {
        viewModelScope.launch {
            uploadManager.openUploadRequests.collect { list ->
                _state.update {
                    it.copy(draftBatches = list)
                }
            }
        }
    }

    private fun observeActiveBatches() {
        viewModelScope.launch {
            uploadRepository.batches.collect { map ->
                _state.update {
                    it.copy(batches = map.values.toList())
                }
            }
        }
    }

    fun onGrantUpload(uploadBatch: DraftUploadBatch) {
        viewModelScope.launch {
            uploadManager.onRequestAccepted(uploadBatch.clientIp, uploadBatch.draftId)
        }
    }

    fun onDenyUpload(uploadBatch: DraftUploadBatch) {
        viewModelScope.launch {
            uploadManager.onRequestDenied(uploadBatch.clientIp, uploadBatch.draftId)
        }
    }
}