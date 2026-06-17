package com.convex.app.ui.operation

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Headphones
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.convex.app.R
import com.convex.app.domain.model.ExecutionState
import com.convex.app.domain.model.MediaInfo
import com.convex.app.domain.model.OperationParam
import com.convex.app.ui.operation.components.executionOverlay
import com.convex.app.ui.operation.components.paramField

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun operationScreen(
    onBack: () -> Unit,
    viewModel: OperationViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val op =
        uiState.operation ?: run {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return
        }

    val isExecuting = uiState.executionState !is ExecutionState.Idle

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(stringResource(op.labelRes), style = MaterialTheme.typography.titleLarge)
                        Text(stringResource(op.descRes), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (isExecuting) viewModel.cancelExecution() else onBack()
                    }) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = null)
                    }
                },
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                    ),
            )
        },
        bottomBar = {
            AnimatedVisibility(visible = !isExecuting) {
                Surface(
                    tonalElevation = 8.dp,
                    shadowElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.extraLarge.copy(bottomStart = androidx.compose.foundation.shape.CornerSize(0.dp), bottomEnd = androidx.compose.foundation.shape.CornerSize(0.dp))
                ) {
                    Column(Modifier.padding(horizontal = 24.dp, vertical = 20.dp)) {
                        uiState.validationError?.let { err ->
                            Text(
                                text = err,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(bottom = 12.dp),
                            )
                        }
                        if (uiState.technicalMode && uiState.generatedCommand.isNotBlank()) {
                            commandPreviewChip(uiState.generatedCommand)
                            Spacer(Modifier.height(12.dp))
                        }
                        Button(
                            onClick = { viewModel.startExecution() },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            shape = MaterialTheme.shapes.extraLarge,
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                        ) {
                            Icon(Icons.Outlined.PlayArrow, contentDescription = null, modifier = Modifier.size(24.dp))
                            Spacer(Modifier.width(12.dp))
                            Text(
                                stringResource(R.string.btn_start).uppercase(),
                                style = MaterialTheme.typography.titleMedium,
                                letterSpacing = androidx.compose.ui.unit.TextUnit(1.5f, androidx.compose.ui.unit.TextUnitType.Sp)
                            )
                        }
                    }
                }
            }
        },
    ) { padding ->

        AnimatedVisibility(
            visible = isExecuting,
            enter = fadeIn() + slideInVertically(),
            exit = fadeOut() + slideOutVertically(),
        ) {
            executionOverlay(
                state = uiState.executionState,
                onCancel = { viewModel.cancelExecution() },
                onReset = { viewModel.resetExecution() },
                technicalMode = uiState.technicalMode,
                modifier = Modifier.padding(padding),
            )
        }

        AnimatedVisibility(visible = !isExecuting) {
            LazyColumn(
                contentPadding =
                    PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = padding.calculateTopPadding() + 8.dp,
                        bottom = padding.calculateBottomPadding() + 8.dp,
                    ),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                uiState.mediaInfo?.let { info ->
                    item(key = "media_info") {
                        mediaInfoCard(info)
                    }
                }

                item(key = "file_selections") {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        op.params.filterIsInstance<OperationParam.FilePicker>().forEach { param ->
                            fileSelectionCard(
                                param = param,
                                value = uiState.paramValues[param.id] ?: "",
                                onFileSelected = { uri, displayName, realPath ->
                                    viewModel.resolveFileUri(param.id, uri, displayName, realPath)
                                },
                                context = context
                            )
                        }
                    }
                }

                items(op.params.filter { it !is OperationParam.FilePicker }, key = { it.id }) { param ->
                    paramField(
                        param = param,
                        value = uiState.paramValues[param.id] ?: "",
                        onValueChange = { viewModel.updateParam(param.id, it) },
                        onFileSelected = { _, _, _ -> }, // Not used here
                        context = context,
                    )
                }
            }
        }
    }
}

@Composable
private fun fileSelectionCard(
    param: OperationParam.FilePicker,
    value: String,
    onFileSelected: (Uri, String, String) -> Unit,
    context: android.content.Context
) {
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            val displayName = context.getDisplayName(uri)
            onFileSelected(it, displayName, it.toString())
        }
    }

    val isSelected = value.isNotBlank()
    val displayName = if (isSelected) {
        if (value.startsWith("content://")) {
            runCatching { context.getDisplayName(Uri.parse(value)) }.getOrDefault(value.substringAfterLast("/"))
        } else {
            value.substringAfterLast("/")
        }
    } else {
        stringResource(R.string.no_file_selected)
    }
    
    Card(
        onClick = { launcher.launch(param.mimeTypes.toTypedArray()) },
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f) 
                             else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ),
        border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                shape = MaterialTheme.shapes.large,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f),
                modifier = Modifier.size(56.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (param.mimeTypes.any { it.startsWith("video") }) Icons.Outlined.Videocam 
                                      else if (param.mimeTypes.any { it.startsWith("audio") }) Icons.Outlined.Headphones 
                                      else Icons.Outlined.Image,
                        contentDescription = null,
                        tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    stringResource(param.labelRes).uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = androidx.compose.ui.unit.TextUnit(1.2f, androidx.compose.ui.unit.TextUnitType.Sp)
                )
                Text(
                    displayName,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }
            
            if (!isSelected) {
                Icon(
                    Icons.Outlined.FolderOpen,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
        }
    }
}

// Utility extension for getting display name (copied from ParamFields or should be shared)
private fun android.content.Context.getDisplayName(uri: Uri): String {
    return contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
        if (nameIndex != -1 && cursor.moveToFirst()) {
            cursor.getString(nameIndex)
        } else null
    } ?: uri.lastPathSegment ?: "file"
}

@Composable
private fun commandPreviewChip(command: String) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = command,
            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            maxLines = 2,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun mediaInfoCard(info: MediaInfo) {
    Card(
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
            ),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                stringResource(R.string.media_info_title),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f))
            if (info.width != null && info.height != null) {
                infoRow(stringResource(R.string.media_info_resolution), "${info.width}×${info.height}")
            }
            info.videoCodec?.let { infoRow(stringResource(R.string.media_info_codec), it) }
            info.fps?.let { infoRow(stringResource(R.string.media_info_fps), it) }
            val durationSeconds = info.duration.toDoubleOrNull() ?: 0.0
            val h = (durationSeconds / 3600).toInt()
            val m = ((durationSeconds % 3600) / 60).toInt()
            val s = (durationSeconds % 60).toInt()
            val durationText = if (h > 0) "%02d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
            infoRow(stringResource(R.string.media_info_duration), durationText)
            infoRow(
                stringResource(R.string.media_info_size),
                "%.1f MB".format(info.sizeBytes / 1_048_576.0),
            )
        }
    }
}

@Composable
private fun infoRow(
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
        )
        Text(
            value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }
}
