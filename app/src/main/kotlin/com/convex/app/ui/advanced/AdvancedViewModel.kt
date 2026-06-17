package com.convex.app.ui.advanced

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.convex.app.data.ffmpeg.FfmpegRepository
import com.convex.app.data.prefs.AppPreferences
import com.convex.app.domain.model.ExecutionState
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

data class AdvancedUiState(
    val cliInput: String = "",
    val executionState: ExecutionState = ExecutionState.Idle,
    val selectedPreset: String = "",
    val generatedCommand: String = "ffmpeg",
    val technicalMode: Boolean = false,
)

val CLI_PRESETS =
    listOf(
        "List codecs" to "-codecs",
        "List formats" to "-formats",
        "List filters" to "-filters",
        "Video info" to "-i input.mp4 -hide_banner",
        "Fast H264 encode" to "-i input.mp4 -c:v libx264 -preset fast -crf 23 output.mp4",
        "Extract audio (AAC)" to "-i input.mp4 -vn -c:a copy output.aac",
        "Convert to GIF" to "-i input.mp4 -vf fps=10,scale=480:-1:flags=lanczos -loop 0 output.gif",
        "Concat two files" to "-f concat -safe 0 -i list.txt -c copy output.mp4",
        "Loudness normalization" to "-i input.mp3 -af loudnorm=I=-23:TP=-1.5:LRA=11 output.mp3",
        "4K to 1080p" to "-i input.mp4 -vf scale=1920:1080 -c:v libx264 -crf 20 -c:a copy output.mp4",
    )

@HiltViewModel
class AdvancedViewModel
    @Inject
    constructor(
        private val ffmpeg: FfmpegRepository,
        private val prefs: AppPreferences,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(AdvancedUiState())
        val uiState: StateFlow<AdvancedUiState> = _uiState.asStateFlow()

        private var execJob: Job? = null

        init {
            prefs.technicalMode
                .onEach { techMode ->
                    _uiState.update { it.copy(technicalMode = techMode) }
                }
                .launchIn(viewModelScope)
        }

        fun updateCliInput(input: String) {
            _uiState.update { it.copy(cliInput = input, generatedCommand = "ffmpeg $input") }
        }

        fun applyPreset(presetArgs: String) {
            _uiState.update { it.copy(cliInput = presetArgs, generatedCommand = "ffmpeg $presetArgs") }
        }

        fun runCommand() {
            val raw = _uiState.value.cliInput.trim()
            if (raw.isBlank()) return

            // Simple shell-like split respecting quoted strings
            val args = parseArgs(raw)
            execJob?.cancel()
            execJob =
                viewModelScope.launch {
                    ffmpeg.execute(args)
                        .onEach { state ->
                            _uiState.update { it.copy(executionState = state) }
                            if (state is ExecutionState.Completed || state is ExecutionState.Failed) {
                                saveSession(args, state)
                            }
                        }
                        .launchIn(this)
                }
        }

        fun cancelExecution() {
            execJob?.cancel()
            _uiState.update { it.copy(executionState = ExecutionState.Cancelled) }
        }

        fun resetExecution() {
            execJob?.cancel()
            _uiState.update { it.copy(executionState = ExecutionState.Idle) }
        }

        private fun parseArgs(input: String): List<String> {
            val result = mutableListOf<String>()
            val current = StringBuilder()
            var inQuotes = false
            for (c in input) {
                when {
                    c == '"' -> inQuotes = !inQuotes
                    c == ' ' && !inQuotes -> {
                        if (current.isNotEmpty()) {
                            result.add(current.toString())
                            current.clear()
                        }
                    }
                    else -> current.append(c)
                }
            }
            if (current.isNotEmpty()) result.add(current.toString())
            return result
        }

        private suspend fun saveSession(
            args: List<String>,
            state: ExecutionState,
        ) {
            val status =
                when (state) {
                    is ExecutionState.Completed -> SessionStatus.SUCCESS
                    is ExecutionState.Cancelled -> SessionStatus.CANCELLED
                    else -> SessionStatus.ERROR
                }
            prefs.addSession(
                SessionRecord(
                    id = UUID.randomUUID().toString(),
                    operationLabel = "CLI",
                    command = "ffmpeg " + args.joinToString(" "),
                    outputPath = (state as? ExecutionState.Completed)?.outputPath ?: "",
                    status = status,
                    timestampUtc = System.currentTimeMillis(),
                    durationMs = (state as? ExecutionState.Completed)?.durationMs ?: 0L,
                ),
            )
        }
    }
