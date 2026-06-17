package com.convex.app.domain.model

/** Controls which theme the app renders. */
enum class ThemeMode { SYSTEM, LIGHT, DARK }

/** Supported app languages. */
enum class AppLanguage(val tag: String) {
    SYSTEM(""),
    ENGLISH("en"),
    HEBREW("iw"),
}

/** Represents a running / finished FFmpeg session state. */
sealed interface ExecutionState {
    data object Idle : ExecutionState

    data class Running(
        // 0..1, or negative if unknown
        val progress: Float,
        val speed: String,
        val elapsed: String,
        val outputSize: String,
        val recentLog: List<String>,
    ) : ExecutionState

    data class Completed(val outputPath: String, val durationMs: Long) : ExecutionState

    data class Failed(val returnCode: Int, val message: String) : ExecutionState

    data object Cancelled : ExecutionState
}

/** A recorded history entry stored in DataStore. */
data class SessionRecord(
    val id: String,
    val categoryId: String = "",
    val operationId: String = "",
    val operationLabel: String,
    val command: String,
    val outputPath: String,
    val status: SessionStatus,
    val timestampUtc: Long,
    val durationMs: Long,
)

enum class SessionStatus { SUCCESS, ERROR, CANCELLED }

/** Parsed media stream information from ffprobe. */
data class MediaInfo(
    val path: String,
    val duration: String,
    val sizeBytes: Long,
    val videoCodec: String?,
    val width: Int?,
    val height: Int?,
    val fps: String?,
    val audioBitrate: String?,
    val audioCodec: String?,
)
