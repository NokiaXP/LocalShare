package com.defname.localshare.ui.screens.transfers

import com.defname.localshare.domain.model.DraftUploadBatch
import com.defname.localshare.domain.model.UploadBatch

data class TransfersState(
    val uploadFeatureActive: Boolean = false,
    val draftBatches: List<DraftUploadBatch> = emptyList(),
    val batches: List<UploadBatch> = emptyList()
)