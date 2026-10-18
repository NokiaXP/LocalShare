// SPDX-FileCopyrightText: 2026 2026 defname
//
// SPDX-License-Identifier: GPL-3.0-or-later

package com.defname.localshare.ui.screens.transfers.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.defname.localshare.R
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.defname.localshare.domain.model.DraftUploadBatch
import com.defname.localshare.domain.model.UploadBatch
import com.defname.localshare.domain.model.UploadFile
import com.defname.localshare.domain.model.UploadFileState
import com.defname.localshare.util.sizeAsString

@Composable
fun DraftBatchCard(
    batch: DraftUploadBatch,
    onGrant: () -> Unit,
    onDeny: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .clickable { expanded = !expanded }
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.CloudUpload,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.upload_request_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(R.string.batch_from_client, batch.clientIp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = if (batch.files.size == 1) stringResource(R.string.batch_files_one, batch.files.size) else stringResource(R.string.batch_files_many, batch.files.size),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.batch_total_size, batch.files.sumOf { it.size }.sizeAsString()),
                    style = MaterialTheme.typography.bodyMedium
                )
                Row {
                    FilledTonalIconButton(
                        onClick = onDeny,
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        )
                    ) {
                        Icon(Icons.Default.Close, contentDescription = stringResource(R.string.action_reject))
                    }
                    Spacer(Modifier.width(8.dp))
                    FilledIconButton(
                        onClick = onGrant,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Icon(Icons.Default.Check, contentDescription = stringResource(R.string.action_grant))
                    }
                }
            }

            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))
                    Spacer(Modifier.height(8.dp))
                    batch.files.forEach { file ->
                        FileListRow(file.name, file.size)
                    }
                }
            }
        }
    }
}

@Composable
fun ActiveBatchCard(
    batch: UploadBatch,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .clickable { expanded = !expanded }
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                val isFinished = batch.files.all { it.state is UploadFileState.Finished }
                Icon(
                    imageVector = if (isFinished) Icons.Default.DoneAll else Icons.Default.Sync,
                    contentDescription = null,
                    tint = if (isFinished) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.batch_transfer_id, batch.batchId.take(8)),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(R.string.batch_source, batch.clientIp),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                
                val progress = if (batch.files.isEmpty()) 0f else {
                    batch.files.sumOf { 
                        when (val s = it.state) {
                            is UploadFileState.Finished -> it.size
                            is UploadFileState.Uploading -> s.progress
                            else -> 0
                        }
                    }.toFloat() / (batch.files.sumOf { it.size }.toFloat())
                }
                
                Box(contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        progress = progress,
                        modifier = Modifier.size(32.dp),
                        strokeWidth = 3.dp,
                        color = MaterialTheme.colorScheme.secondary,
                        trackColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                    Text(
                        text = "${(progress * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 8.sp
                    )
                }
            }

            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    HorizontalDivider()
                    Spacer(Modifier.height(8.dp))
                    batch.files.forEach { file ->
                        FileProgressRow(file)
                    }
                }
            }
        }
    }
}

@Composable
private fun FileListRow(name: String, size: Long) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1f),
            maxLines = 1
        )
        Text(
            text = size.sizeAsString(),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun FileProgressRow(file: UploadFile) {
    Column(modifier = Modifier.padding(vertical = 6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = getStatusIcon(file.state),
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = getStatusColor(file.state)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = file.name,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1
                )
            }
            Text(
                text = file.size.sizeAsString(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        if (file.state is UploadFileState.Uploading) {
            LinearProgressIndicator(
            progress = { file.state.progress.toFloat() / file.size.toFloat() },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp)
                .height(2.dp),
            color = MaterialTheme.colorScheme.secondary,
            trackColor = ProgressIndicatorDefaults.linearTrackColor,
            strokeCap = ProgressIndicatorDefaults.LinearStrokeCap,
            )
        }
    }
}

@Composable
private fun getStatusIcon(state: UploadFileState): ImageVector = when (state) {
    is UploadFileState.Pending -> Icons.Default.Schedule
    is UploadFileState.Uploading -> Icons.Default.CloudUpload
    is UploadFileState.Finished -> Icons.Default.CheckCircle
    is UploadFileState.Error -> Icons.Default.Error
    is UploadFileState.Canceled -> Icons.Default.Block
}

@Composable
private fun getStatusColor(state: UploadFileState) = when (state) {
    is UploadFileState.Pending -> MaterialTheme.colorScheme.onSurfaceVariant
    is UploadFileState.Uploading -> MaterialTheme.colorScheme.secondary
    is UploadFileState.Finished -> MaterialTheme.colorScheme.tertiary
    is UploadFileState.Error -> MaterialTheme.colorScheme.error
    is UploadFileState.Canceled -> MaterialTheme.colorScheme.outline
}
