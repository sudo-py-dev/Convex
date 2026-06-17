package com.convex.app.ui.operation

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.Button
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
                    Text(stringResource(op.labelRes), style = MaterialTheme.typography.titleLarge)
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
                    tonalElevation = 3.dp,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                        uiState.validationError?.let { err ->
                            Text(
                                text = err,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(bottom = 8.dp),
                            )
                        }
                        if (uiState.technicalMode && uiState.generatedCommand.isNotBlank()) {
                            commandPreviewChip(uiState.generatedCommand)
                            Spacer(Modifier.height(8.dp))
                        }
                        Button(
                            onClick = { viewModel.startExecution() },
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.large,
                        ) {
                            Icon(Icons.Outlined.PlayArrow, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                stringResource(R.string.btn_start),
                                style = MaterialTheme.typography.labelLarge,
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

                items(op.params, key = { it.id }) { param ->
                    paramField(
                        param = param,
                        value = uiState.paramValues[param.id] ?: "",
                        onValueChange = { viewModel.updateParam(param.id, it) },
                        onFileSelected = { uri, displayName, realPath ->
                            viewModel.resolveFileUri(param.id, uri, displayName, realPath)
                        },
                        context = context,
                    )
                }
            }
        }
    }
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
            infoRow(stringResource(R.string.media_info_duration), info.duration)
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
