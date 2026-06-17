package com.convex.app.data.ffmpeg

import android.content.Context
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.FFmpegKitConfig
import com.arthenica.ffmpegkit.FFprobeKit
import com.arthenica.ffmpegkit.ReturnCode
import com.convex.app.domain.model.ExecutionState
import com.convex.app.domain.model.MediaInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FfmpegRepository
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) {
        private fun mapArguments(args: List<String>): List<String> {
            val lastIndex = args.lastIndex
            return args.mapIndexed { index, arg ->
                // 1. Sanitize: remove null bytes which can cause truncation in native code
                var processedArg = arg.replace("\u0000", "")
                val sanitizedArg = processedArg
                
                // 2. Handle content:// URIs, including those embedded in filter strings
                // Greedy match until whitespace or quotes. Most SAF URIs are complex and can contain many characters.
                val contentUriRegex = Regex("content://[^'\"\\s]+[\"']?")
                processedArg = contentUriRegex.replace(processedArg) { match ->
                    val fullMatch = match.value
                    // If matched string ends with a quote that was used to wrap it, don't include it in the URI parsing
                    var uriStr = fullMatch
                    var suffix = ""
                    if (fullMatch.endsWith("'") || fullMatch.endsWith("\"")) {
                        uriStr = fullMatch.substring(0, fullMatch.length - 1)
                        suffix = fullMatch.last().toString()
                    }
                    
                    val replacement = runCatching {
                        val uri = android.net.Uri.parse(uriStr)
                        // If it's the last argument and not part of a filter, treat as write
                        if (index == lastIndex && !arg.contains("=")) {
                            FFmpegKitConfig.getSafParameterForWrite(context, uri)
                        } else {
                            FFmpegKitConfig.getSafParameterForRead(context, uri)
                        }
                    }.getOrDefault(uriStr)

                    // If the URI was wrapped in single quotes (likely in a filter),
                    // we must escape any single quotes in the replacement path.
                    val result = if (match.range.first > 0 && arg[match.range.first - 1] == '\'' && suffix == "'") {
                        replacement.replace("'", "'\\''")
                    } else {
                        replacement
                    }
                    result + suffix
                }

                // 3. Handle file:// URIs
                if (processedArg.contains("file://")) {
                    val fileUriRegex = Regex("file://[^'\"\\s,;]+")
                    processedArg = fileUriRegex.replace(processedArg) { match ->
                        android.net.Uri.parse(match.value).path ?: match.value
                    }
                }

                // 4. Handle relative paths in cache
                // Use sanitizedArg to check if it was originally a relative path candidate
                if (processedArg == sanitizedArg && !processedArg.startsWith("/") && !processedArg.startsWith("-") && 
                    !processedArg.contains("=") && processedArg.contains(".") && 
                    processedArg.toDoubleOrNull() == null && !processedArg.contains("://")) {
                    runCatching {
                        val cacheDir = context.cacheDir.canonicalFile
                        val targetFile = java.io.File(cacheDir, processedArg).canonicalFile
                        if (targetFile.path.startsWith(cacheDir.path + java.io.File.separator)) {
                            targetFile.parentFile?.mkdirs()
                            processedArg = targetFile.absolutePath
                        }
                    }
                }

                processedArg
            }
        }

        /**
         * Executes an FFmpeg command and emits [ExecutionState] updates.
         * The flow completes when FFmpeg finishes or is cancelled.
         * @param args FFmpeg arguments
         * @param durationMs Optional total duration of the media in milliseconds for progress calculation.
         */
        fun execute(
            args: List<String>,
            durationMs: Long? = null,
        ): Flow<ExecutionState> =
            callbackFlow {
                val mappedArgs = mapArguments(args)
                val startMs = System.currentTimeMillis()
                val logLines = mutableListOf<String>()
                var currentProgress = -1f

                val session =
                    FFmpegKit.executeWithArgumentsAsync(
                        mappedArgs.toTypedArray(),
                        // completeCallback
                        { completedSession ->
                            val elapsed = System.currentTimeMillis() - startMs
                            when {
                                ReturnCode.isSuccess(completedSession.returnCode) -> {
                                    trySend(
                                        ExecutionState.Completed(
                                            outputPath = mappedArgs.lastOrNull() ?: "",
                                            durationMs = elapsed,
                                        ),
                                    )
                                }
                                ReturnCode.isCancel(completedSession.returnCode) -> {
                                    trySend(ExecutionState.Cancelled)
                                }
                                else -> {
                                    val errorLine = logLines.findLast { it.contains("error", ignoreCase = true) } 
                                        ?: logLines.lastOrNull() 
                                        ?: "Unknown error"
                                    trySend(
                                        ExecutionState.Failed(
                                            returnCode = completedSession.returnCode?.value ?: -1,
                                            message = errorLine,
                                        ),
                                    )
                                }
                            }
                            close()
                        },
                        // logCallback
                        { log ->
                            val line = log.message?.trimEnd() ?: return@executeWithArgumentsAsync
                            logLines.add(line)
                            if (logLines.size > 200) logLines.removeAt(0)
                            trySend(
                                ExecutionState.Running(
                                    progress = currentProgress,
                                    speed = "",
                                    elapsed = formatElapsed(System.currentTimeMillis() - startMs),
                                    outputSize = "",
                                    recentLog = logLines.toList().takeLast(50),
                                ),
                            )
                        },
                        // statisticsCallback
                        { stats ->
                            val elapsed = System.currentTimeMillis() - startMs
                            val speedStr = "%.2fx".format(stats.speed)
                            val sizeStr = formatSize(stats.size)
                            
                            if (durationMs != null && durationMs > 0) {
                                currentProgress = (stats.time.toFloat() / durationMs).coerceIn(0f, 1f)
                            }

                            trySend(
                                ExecutionState.Running(
                                    progress = currentProgress,
                                    speed = speedStr,
                                    elapsed = formatElapsed(elapsed),
                                    outputSize = sizeStr,
                                    recentLog = logLines.toList().takeLast(50),
                                ),
                            )
                        },
                    )
                val sessionId = session.sessionId

                awaitClose { FFmpegKit.cancel(sessionId) }
            }

        /**
         * Probes a media file and returns parsed [MediaInfo].
         * Returns null if ffprobe fails or file is invalid.
         */
        suspend fun probe(filePath: String): MediaInfo? =
            runCatching {
                val sanitizedPath = filePath.replace("\u0000", "")
                val mappedPath =
                    if (sanitizedPath.startsWith("content://")) {
                        FFmpegKitConfig.getSafParameterForRead(context, android.net.Uri.parse(sanitizedPath))
                    } else if (sanitizedPath.startsWith("file://")) {
                        android.net.Uri.parse(sanitizedPath).path ?: sanitizedPath
                    } else {
                        if (!sanitizedPath.startsWith("/") && sanitizedPath.contains(".") && sanitizedPath.toDoubleOrNull() == null) {
                            val cacheDir = context.cacheDir.canonicalFile
                            val targetFile = java.io.File(cacheDir, sanitizedPath).canonicalFile
                            if (targetFile.path.startsWith(cacheDir.path + java.io.File.separator)) {
                                targetFile.absolutePath
                            } else {
                                sanitizedPath
                            }
                        } else {
                            sanitizedPath
                        }
                    }
                val session = FFprobeKit.getMediaInformationAsync(mappedPath, null)
                val info = session.mediaInformation ?: return null
                val videoStream = info.streams?.firstOrNull { it.type == "video" }
                val audioStream = info.streams?.firstOrNull { it.type == "audio" }

                MediaInfo(
                    path = filePath,
                    duration = info.duration ?: "0",
                    sizeBytes = info.size?.toLongOrNull() ?: 0L,
                    videoCodec = videoStream?.codec,
                    width = videoStream?.width?.toInt(),
                    height = videoStream?.height?.toInt(),
                    fps = videoStream?.averageFrameRate,
                    audioBitrate = audioStream?.bitrate,
                    audioCodec = audioStream?.codec,
                )
            }.getOrNull()

        private fun formatElapsed(ms: Long): String {
            val s = ms / 1000
            return "%02d:%02d:%02d".format(s / 3600, (s % 3600) / 60, s % 60)
        }

        private fun formatSize(bytes: Long): String =
            when {
                bytes >= 1_073_741_824 -> "%.1f GB".format(bytes / 1_073_741_824.0)
                bytes >= 1_048_576 -> "%.1f MB".format(bytes / 1_048_576.0)
                bytes >= 1024 -> "%.1f KB".format(bytes / 1024.0)
                else -> "$bytes B"
            }

        companion object {
            /**
             * Escapes a string for use as an argument within an FFmpeg filter (e.g. subtitles=filename).
             * FFmpeg filter escaping requires wrapping in single quotes and escaping inner single quotes
             * using the sequence: '\'' (close quote, escaped backslash, escaped quote, open quote).
             */
            fun escapeFilterArg(arg: String): String {
                return "'" + arg.replace("'", "'\\''") + "'"
            }
        }
    }
