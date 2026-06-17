package com.convex.app.ui.operation

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.convex.app.data.ffmpeg.FfmpegRepository
import com.convex.app.data.operations.OperationDefinitions
import com.convex.app.data.prefs.AppPreferences
import com.convex.app.domain.model.ExecutionState
import com.convex.app.domain.model.MediaInfo
import com.convex.app.domain.model.Operation
import com.convex.app.domain.model.OperationParam
import com.convex.app.domain.model.SessionRecord
import com.convex.app.domain.model.SessionStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class OperationUiState(
    val operation: Operation? = null,
    val paramValues: Map<String, String> = emptyMap(),
    val mediaInfo: MediaInfo? = null,
    val executionState: ExecutionState = ExecutionState.Idle,
    val validationError: String? = null,
    val generatedCommand: String = "",
    val technicalMode: Boolean = false,
)

@HiltViewModel
class OperationViewModel
    @Inject
    constructor(
        private val ffmpeg: FfmpegRepository,
        private val prefs: AppPreferences,
        @dagger.hilt.android.qualifiers.ApplicationContext private val context: android.content.Context,
        savedStateHandle: SavedStateHandle,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(OperationUiState())
        val uiState: StateFlow<OperationUiState> = _uiState.asStateFlow()

        private val categoryId: String = savedStateHandle.get<String>("categoryId") ?: ""
        private val operationId: String = savedStateHandle.get<String>("operationId") ?: ""
        private var execJob: Job? = null

        init {
            prefs.technicalMode
                .onEach { techMode ->
                    _uiState.update { it.copy(technicalMode = techMode) }
                }
                .launchIn(viewModelScope)

            val op =
                OperationDefinitions.ALL
                    .firstOrNull { it.id == categoryId }
                    ?.operations?.firstOrNull { it.id == operationId }

            if (op != null) {
                val defaults =
                    op.params.associate { param ->
                        param.id to
                            when (param) {
                                is OperationParam.SliderParam -> param.defaultValue.toString()
                                is OperationParam.DropdownParam ->
                                    param.options.getOrNull(param.defaultIndex)?.second ?: ""
                                is OperationParam.TextParam -> param.defaultValue
                                is OperationParam.SwitchParam -> param.defaultValue.toString()
                                is OperationParam.FilePicker -> ""
                            }
                    }
                _uiState.update { it.copy(operation = op, paramValues = defaults) }
                rebuildCommand(defaults, op)
            }
        }

        fun updateParam(
            id: String,
            value: String,
        ) {
            val current = _uiState.value.paramValues.toMutableMap()
            current[id] = value
            _uiState.update { it.copy(paramValues = current, validationError = null) }
            rebuildCommand(current, _uiState.value.operation)

            // Auto-probe if input file updated
            if ((id == "input" || id == "input1") && value.isNotBlank()) {
                probeFile(value)
            }
        }

        fun resolveFileUri(
            paramId: String,
            uri: Uri,
            displayName: String,
            realPath: String,
        ) {
            updateParam(paramId, realPath)
            if (paramId == "input" || paramId == "input1") {
                suggestOutputName(displayName)
            }
        }

        fun startExecution() {
            val op = _uiState.value.operation ?: return
            val values = _uiState.value.paramValues

            val missingRequired =
                op.params
                    .filterIsInstance<OperationParam.FilePicker>()
                    .filter { it.required && values[it.id].isNullOrBlank() }

            if (missingRequired.isNotEmpty()) {
                _uiState.update { it.copy(validationError = "Required files not selected") }
                return
            }

            val args =
                runCatching { op.commandBuilder(values) }.getOrElse { e ->
                    _uiState.update { it.copy(validationError = e.message) }
                    return
                }

            val durationMs = _uiState.value.mediaInfo?.duration?.toDoubleOrNull()?.let { (it * 1000).toLong() }

            execJob?.cancel()
            execJob =
                ffmpeg.execute(args, durationMs)
                    .onEach { state ->
                        _uiState.update { it.copy(executionState = state) }
                        if (state is ExecutionState.Completed || state is ExecutionState.Failed) {
                            saveSession(op, args, state)
                        }
                    }
                    .launchIn(viewModelScope)
        }

        fun cancelExecution() {
            execJob?.cancel()
            _uiState.update { it.copy(executionState = ExecutionState.Cancelled) }
        }

        fun resetExecution() {
            execJob?.cancel()
            _uiState.update { it.copy(executionState = ExecutionState.Idle) }
        }

        private fun suggestOutputName(inputDisplayName: String) {
            val op = _uiState.value.operation ?: return
            val outputParam = op.params.find { it.id == "output" } as? OperationParam.TextParam ?: return
            
            val currentValues = _uiState.value.paramValues.toMutableMap()
            val currentOutput = currentValues["output"] ?: ""
            
            // Only suggest if current output is empty, matches the default, or is just an extension
            if (currentOutput.isBlank() || 
                currentOutput == outputParam.defaultValue || 
                currentOutput.startsWith("output.")) {
                
                val inputName = inputDisplayName.substringBeforeLast('.')
                val extension = outputParam.defaultValue.substringAfterLast('.', "mp4")
                val suggestion = "${inputName}_convex.$extension"
                
                currentValues["output"] = suggestion
                _uiState.update { it.copy(paramValues = currentValues) }
                rebuildCommand(currentValues, op)
            }
        }

        private fun probeFile(path: String) =
            viewModelScope.launch {
                val info = ffmpeg.probe(path)
                _uiState.update { it.copy(mediaInfo = info) }
            }

        private fun rebuildCommand(
            values: Map<String, String>,
            op: Operation?,
        ) {
            if (op == null) return
            val cmd =
                runCatching {
                    val args = op.commandBuilder(values)
                    "ffmpeg " + args.joinToString(" ") { if (it.contains(' ')) "\"$it\"" else it }
                }.getOrDefault("")
            _uiState.update { it.copy(generatedCommand = cmd) }
        }

        private suspend fun saveSession(
            op: Operation,
            args: List<String>,
            state: ExecutionState,
        ) {
            val status =
                when (state) {
                    is ExecutionState.Completed -> SessionStatus.SUCCESS
                    is ExecutionState.Cancelled -> SessionStatus.CANCELLED
                    else -> SessionStatus.ERROR
                }
            val durationMs =
                when (state) {
                    is ExecutionState.Completed -> state.durationMs
                    else -> 0L
                }
            prefs.addSession(
                SessionRecord(
                    id = UUID.randomUUID().toString(),
                    categoryId = categoryId,
                    operationId = operationId,
                    operationLabel = context.getString(op.labelRes),
                    command = "ffmpeg " + args.joinToString(" "),
                    outputPath = (state as? ExecutionState.Completed)?.outputPath ?: "",
                    status = status,
                    timestampUtc = System.currentTimeMillis(),
                    durationMs = durationMs,
                ),
            )
        }
    }
