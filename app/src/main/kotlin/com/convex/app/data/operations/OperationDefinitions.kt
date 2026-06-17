package com.convex.app.data.operations

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.CallMerge
import androidx.compose.material.icons.automirrored.outlined.CallSplit
import androidx.compose.material.icons.automirrored.outlined.MergeType
import androidx.compose.material.icons.automirrored.outlined.RotateRight
import androidx.compose.material.icons.automirrored.outlined.VolumeOff
import androidx.compose.material.icons.outlined.AspectRatio
import androidx.compose.material.icons.outlined.AudioFile
import androidx.compose.material.icons.outlined.ClosedCaption
import androidx.compose.material.icons.outlined.Compress
import androidx.compose.material.icons.outlined.ContentCut
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.Gif
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.Headphones
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.MoveToInbox
import androidx.compose.material.icons.outlined.Photo
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Slideshow
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Subtitles
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material.icons.outlined.VideocamOff
import androidx.compose.material.icons.outlined.WaterDrop
import com.convex.app.R
import com.convex.app.data.ffmpeg.FfmpegRepository
import com.convex.app.domain.model.Category
import com.convex.app.domain.model.Operation
import com.convex.app.domain.model.OperationParam

/**
 * All categories and operations defined statically.
 * commandBuilder lambdas produce validated FFmpeg argument lists.
 */
object OperationDefinitions {
    private val videoFormats =
        listOf(
            "mp4" to "mp4",
            "mkv" to "mkv",
            "avi" to "avi",
            "mov" to "mov",
            "webm" to "webm",
            "flv" to "flv",
            "ts" to "ts",
            "m4v" to "m4v",
        )
    private val audioFormats =
        listOf(
            "mp3" to "mp3",
            "aac" to "aac",
            "flac" to "flac",
            "wav" to "wav",
            "ogg" to "ogg",
            "opus" to "opus",
            "m4a" to "m4a",
            "wma" to "wma",
        )
    private val resolutions =
        listOf(
            "3840x2160 (4K)" to "3840:2160",
            "2560x1440 (2K)" to "2560:1440",
            "1920x1080 (FHD)" to "1920:1080",
            "1280x720 (HD)" to "1280:720",
            "854x480 (SD)" to "854:480",
            "640x360" to "640:360",
            "426x240" to "426:240",
        )
    private val watermarkPositions =
        listOf(
            "Top Left" to "10:10",
            "Top Right" to "main_w-overlay_w-10:10",
            "Bottom Left" to "10:main_h-overlay_h-10",
            "Bottom Right" to "main_w-overlay_w-10:main_h-overlay_h-10",
            "Center" to "(main_w-overlay_w)/2:(main_h-overlay_h)/2",
        )
    private val rotations =
        listOf(
            "90°" to "transpose=1",
            "180°" to "transpose=2,transpose=2",
            "270°" to "transpose=2",
            "Flip Horizontal" to "hflip",
            "Flip Vertical" to "vflip",
        )
    private val videoCodecs =
        listOf(
            "H.264 (libx264)" to "libx264",
            "H.265 (libx265)" to "libx265",
            "VP9" to "libvpx-vp9",
            "Copy (no re-encode)" to "copy",
        )
    private val audioCodecs =
        listOf(
            "AAC" to "aac",
            "MP3 (libmp3lame)" to "libmp3lame",
            "FLAC" to "flac",
            "Opus" to "libopus",
            "Vorbis" to "libvorbis",
            "Copy (no re-encode)" to "copy",
        )

    val VIDEO =
        Category(
            id = "video",
            labelRes = R.string.category_video,
            icon = Icons.Outlined.Videocam,
            operations =
                listOf(
                    Operation(
                        id = "video_convert",
                        labelRes = R.string.op_video_convert,
                        descRes = R.string.op_video_convert_desc,
                        icon = Icons.Outlined.SwapHoriz,
                        params =
                            listOf(
                                OperationParam.FilePicker(
                                    "input",
                                    R.string.param_input_file,
                                    mimeTypes = listOf("video/*"),
                                ),
                                OperationParam.DropdownParam(
                                    "vcodec",
                                    R.string.param_codec,
                                    options = videoCodecs,
                                    defaultIndex = 0,
                                ),
                                OperationParam.DropdownParam(
                                    "acodec",
                                    R.string.param_codec,
                                    options = audioCodecs,
                                    defaultIndex = 0,
                                ),
                                OperationParam.DropdownParam(
                                    "format",
                                    R.string.param_format,
                                    options = videoFormats,
                                    defaultIndex = 0,
                                ),
                                OperationParam.TextParam(
                                    "output",
                                    R.string.param_output_file,
                                    required = false,
                                    defaultValue = "output.mp4",
                                ),
                            ),
                        commandBuilder = { v ->
                            val input = v["input"] ?: error("input required")
                            val format = v["format"] ?: "mp4"
                            val output = v["output"]?.ifBlank { null } ?: "output.$format"
                            listOf(
                                "-i",
                                input,
                                "-c:v",
                                v["vcodec"] ?: "libx264",
                                "-c:a",
                                v["acodec"] ?: "aac",
                                "-y",
                                output,
                            )
                        },
                    ),
                    Operation(
                        id = "video_resize",
                        labelRes = R.string.op_video_resize,
                        descRes = R.string.op_video_resize_desc,
                        icon = Icons.Outlined.AspectRatio,
                        params =
                            listOf(
                                OperationParam.FilePicker(
                                    "input",
                                    R.string.param_input_file,
                                    mimeTypes = listOf("video/*"),
                                ),
                                OperationParam.DropdownParam(
                                    "resolution",
                                    R.string.param_resolution,
                                    options = resolutions,
                                    defaultIndex = 2,
                                ),
                                OperationParam.TextParam(
                                    "output",
                                    R.string.param_output_file,
                                    required = false,
                                    defaultValue = "output_resized.mp4",
                                ),
                            ),
                        commandBuilder = { v ->
                            val res = v["resolution"] ?: "1920:1080"
                            listOf(
                                "-i",
                                v["input"] ?: error("input required"),
                                "-vf",
                                "scale=$res",
                                "-c:a",
                                "copy",
                                "-y",
                                v["output"]?.ifBlank { null } ?: "output_resized.mp4",
                            )
                        },
                    ),
                    Operation(
                        id = "video_trim",
                        labelRes = R.string.op_video_trim,
                        descRes = R.string.op_video_trim_desc,
                        icon = Icons.Outlined.ContentCut,
                        params =
                            listOf(
                                OperationParam.FilePicker(
                                    "input",
                                    R.string.param_input_file,
                                    mimeTypes = listOf("video/*"),
                                ),
                                OperationParam.TextParam(
                                    "start",
                                    R.string.param_start_time,
                                    defaultValue = "00:00:00",
                                ),
                                OperationParam.TextParam(
                                    "duration",
                                    R.string.param_duration,
                                    defaultValue = "00:01:00",
                                ),
                                OperationParam.TextParam(
                                    "output",
                                    R.string.param_output_file,
                                    required = false,
                                    defaultValue = "output_trim.mp4",
                                ),
                            ),
                        commandBuilder = { v ->
                            listOf(
                                "-ss", v["start"] ?: "00:00:00",
                                "-i", v["input"] ?: error("input required"),
                                "-t", v["duration"] ?: "00:01:00",
                                "-c", "copy",
                                "-y", v["output"]?.ifBlank { null } ?: "output_trim.mp4",
                            )
                        },
                    ),
                    Operation(
                        id = "video_compress",
                        labelRes = R.string.op_video_compress,
                        descRes = R.string.op_video_compress_desc,
                        icon = Icons.Outlined.Compress,
                        params =
                            listOf(
                                OperationParam.FilePicker(
                                    "input",
                                    R.string.param_input_file,
                                    mimeTypes = listOf("video/*"),
                                ),
                                OperationParam.SliderParam(
                                    "crf",
                                    R.string.param_crf,
                                    min = 18f,
                                    max = 51f,
                                    defaultValue = 28f,
                                    valueFormat = "%.0f",
                                ),
                                OperationParam.TextParam(
                                    "output",
                                    R.string.param_output_file,
                                    required = false,
                                    defaultValue = "output_compressed.mp4",
                                ),
                            ),
                        commandBuilder = { v ->
                            val crf = v["crf"]?.toFloatOrNull()?.toInt() ?: 28
                            listOf(
                                "-i", v["input"] ?: error("input required"),
                                "-c:v", "libx264", "-crf", "$crf",
                                "-c:a", "copy",
                                "-y", v["output"]?.ifBlank { null } ?: "output_compressed.mp4",
                            )
                        },
                    ),
                    Operation(
                        id = "video_watermark",
                        labelRes = R.string.op_video_watermark,
                        descRes = R.string.op_video_watermark_desc,
                        icon = Icons.Outlined.WaterDrop,
                        params =
                            listOf(
                                OperationParam.FilePicker(
                                    "input",
                                    R.string.param_input_file,
                                    mimeTypes = listOf("video/*"),
                                ),
                                OperationParam.FilePicker(
                                    "watermark",
                                    R.string.param_watermark_file,
                                    mimeTypes = listOf("image/*"),
                                ),
                                OperationParam.DropdownParam(
                                    "position",
                                    R.string.param_watermark_position,
                                    options = watermarkPositions,
                                    defaultIndex = 0,
                                ),
                                OperationParam.TextParam(
                                    "output",
                                    R.string.param_output_file,
                                    required = false,
                                    defaultValue = "output_watermark.mp4",
                                ),
                            ),
                        commandBuilder = { v ->
                            val pos = v["position"] ?: "10:10"
                            listOf(
                                "-i", v["input"] ?: error("input required"),
                                "-i", v["watermark"] ?: error("watermark required"),
                                "-filter_complex", "overlay=$pos",
                                "-c:a", "copy",
                                "-y", v["output"]?.ifBlank { null } ?: "output_watermark.mp4",
                            )
                        },
                    ),
                    Operation(
                        id = "video_extract_frames",
                        labelRes = R.string.op_video_extract_frames,
                        descRes = R.string.op_video_extract_frames_desc,
                        icon = Icons.Outlined.PhotoLibrary,
                        params =
                            listOf(
                                OperationParam.FilePicker(
                                    "input",
                                    R.string.param_input_file,
                                    mimeTypes = listOf("video/*"),
                                ),
                                OperationParam.SliderParam(
                                    "fps",
                                    R.string.param_fps,
                                    min = 1f,
                                    max = 30f,
                                    defaultValue = 1f,
                                    valueFormat = "%.0f",
                                ),
                                OperationParam.TextParam(
                                    "output",
                                    R.string.param_output_file,
                                    required = false,
                                    defaultValue = "frame_%04d.png",
                                ),
                            ),
                        commandBuilder = { v ->
                            val fps = v["fps"]?.toFloatOrNull()?.toInt() ?: 1
                            listOf(
                                "-i",
                                v["input"] ?: error("input required"),
                                "-vf",
                                "fps=$fps",
                                v["output"]?.ifBlank { null } ?: "frame_%04d.png",
                            )
                        },
                    ),
                    Operation(
                        id = "video_speed",
                        labelRes = R.string.op_video_speed,
                        descRes = R.string.op_video_speed_desc,
                        icon = Icons.Outlined.Speed,
                        params =
                            listOf(
                                OperationParam.FilePicker(
                                    "input",
                                    R.string.param_input_file,
                                    mimeTypes = listOf("video/*"),
                                ),
                                OperationParam.SliderParam(
                                    "speed",
                                    R.string.param_speed,
                                    min = 0.25f,
                                    max = 4f,
                                    defaultValue = 2f,
                                    valueFormat = "%.2f",
                                ),
                                OperationParam.TextParam(
                                    "output",
                                    R.string.param_output_file,
                                    required = false,
                                    defaultValue = "output_speed.mp4",
                                ),
                            ),
                        commandBuilder = { v ->
                            val speed = v["speed"]?.toFloatOrNull() ?: 2f
                            val pts = 1f / speed
                            
                            // atempo filter only supports 0.5 to 2.0. Chain them for higher/lower speeds.
                            var audioFilter = ""
                            var remainingSpeed = speed
                            while (remainingSpeed > 2.0) {
                                audioFilter += "atempo=2.0,"
                                remainingSpeed /= 2.0f
                            }
                            while (remainingSpeed < 0.5) {
                                audioFilter += "atempo=0.5,"
                                remainingSpeed /= 0.5f
                            }
                            audioFilter += "atempo=$remainingSpeed"

                            listOf(
                                "-i", v["input"] ?: error("input required"),
                                "-filter_complex",
                                "[0:v]setpts=$pts*PTS[v];[0:a]$audioFilter[a]",
                                "-map", "[v]", "-map", "[a]",
                                "-y", v["output"]?.ifBlank { null } ?: "output_speed.mp4",
                            )
                        },
                    ),
                    Operation(
                        id = "video_rotate",
                        labelRes = R.string.op_video_rotate,
                        descRes = R.string.op_video_rotate_desc,
                        icon = Icons.AutoMirrored.Outlined.RotateRight,
                        params =
                            listOf(
                                OperationParam.FilePicker(
                                    "input",
                                    R.string.param_input_file,
                                    mimeTypes = listOf("video/*"),
                                ),
                                OperationParam.DropdownParam(
                                    "rotation",
                                    R.string.param_rotation,
                                    options = rotations,
                                    defaultIndex = 0,
                                ),
                                OperationParam.TextParam(
                                    "output",
                                    R.string.param_output_file,
                                    required = false,
                                    defaultValue = "output_rotated.mp4",
                                ),
                            ),
                        commandBuilder = { v ->
                            val rot = v["rotation"] ?: "transpose=1"
                            listOf(
                                "-i",
                                v["input"] ?: error("input required"),
                                "-vf",
                                rot,
                                "-c:a",
                                "copy",
                                "-y",
                                v["output"]?.ifBlank { null } ?: "output_rotated.mp4",
                            )
                        },
                    ),
                ),
        )

    val AUDIO =
        Category(
            id = "audio",
            labelRes = R.string.category_audio,
            icon = Icons.Outlined.Headphones,
            operations =
                listOf(
                    Operation(
                        id = "audio_convert",
                        labelRes = R.string.op_audio_convert,
                        descRes = R.string.op_audio_convert_desc,
                        icon = Icons.Outlined.SwapHoriz,
                        params =
                            listOf(
                                OperationParam.FilePicker(
                                    "input",
                                    R.string.param_input_file,
                                    mimeTypes = listOf("audio/*", "video/*"),
                                ),
                                OperationParam.DropdownParam(
                                    "codec",
                                    R.string.param_codec,
                                    options = audioCodecs,
                                    defaultIndex = 0,
                                ),
                                OperationParam.DropdownParam(
                                    "format",
                                    R.string.param_format,
                                    options = audioFormats,
                                    defaultIndex = 0,
                                ),
                                OperationParam.TextParam(
                                    "output",
                                    R.string.param_output_file,
                                    required = false,
                                    defaultValue = "output.mp3",
                                ),
                            ),
                        commandBuilder = { v ->
                            val fmt = v["format"] ?: "mp3"
                            listOf(
                                "-i",
                                v["input"] ?: error("input required"),
                                "-c:a",
                                v["codec"] ?: "libmp3lame",
                                "-vn",
                                "-y",
                                v["output"]?.ifBlank { null } ?: "output.$fmt",
                            )
                        },
                    ),
                    Operation(
                        id = "audio_extract",
                        labelRes = R.string.op_audio_extract,
                        descRes = R.string.op_audio_extract_desc,
                        icon = Icons.Outlined.AudioFile,
                        params =
                            listOf(
                                OperationParam.FilePicker(
                                    "input",
                                    R.string.param_input_file,
                                    mimeTypes = listOf("video/*"),
                                ),
                                OperationParam.DropdownParam(
                                    "format",
                                    R.string.param_format,
                                    options = audioFormats,
                                    defaultIndex = 1,
                                ),
                                OperationParam.TextParam(
                                    "output",
                                    R.string.param_output_file,
                                    required = false,
                                    defaultValue = "output.aac",
                                ),
                            ),
                        commandBuilder = { v ->
                            val fmt = v["format"] ?: "aac"
                            listOf(
                                "-i",
                                v["input"] ?: error("input required"),
                                "-vn",
                                "-c:a",
                                "copy",
                                "-y",
                                v["output"]?.ifBlank { null } ?: "output.$fmt",
                            )
                        },
                    ),
                    Operation(
                        id = "audio_bitrate",
                        labelRes = R.string.op_audio_bitrate,
                        descRes = R.string.op_audio_bitrate_desc,
                        icon = Icons.Outlined.Tune,
                        params =
                            listOf(
                                OperationParam.FilePicker(
                                    "input",
                                    R.string.param_input_file,
                                    mimeTypes = listOf("audio/*"),
                                ),
                                OperationParam.SliderParam(
                                    "bitrate",
                                    R.string.param_bitrate,
                                    min = 64f,
                                    max = 320f,
                                    steps = 7,
                                    defaultValue = 128f,
                                    valueFormat = "%.0f",
                                ),
                                OperationParam.TextParam(
                                    "output",
                                    R.string.param_output_file,
                                    required = false,
                                    defaultValue = "output_bitrate.mp3",
                                ),
                            ),
                        commandBuilder = { v ->
                            val br = v["bitrate"]?.toFloatOrNull()?.toInt() ?: 128
                            listOf(
                                "-i",
                                v["input"] ?: error("input required"),
                                "-b:a",
                                "${br}k",
                                "-y",
                                v["output"]?.ifBlank { null } ?: "output_bitrate.mp3",
                            )
                        },
                    ),
                    Operation(
                        id = "audio_merge",
                        labelRes = R.string.op_audio_merge,
                        descRes = R.string.op_audio_merge_desc,
                        icon = Icons.AutoMirrored.Outlined.MergeType,
                        params =
                            listOf(
                                OperationParam.FilePicker(
                                    "input1",
                                    R.string.param_input_file,
                                    mimeTypes = listOf("audio/*"),
                                ),
                                OperationParam.FilePicker(
                                    "input2",
                                    R.string.param_second_input,
                                    mimeTypes = listOf("audio/*"),
                                ),
                                OperationParam.TextParam(
                                    "output",
                                    R.string.param_output_file,
                                    required = false,
                                    defaultValue = "output_merged.mp3",
                                ),
                            ),
                        commandBuilder = { v ->
                            listOf(
                                "-i",
                                v["input1"] ?: error("input1 required"),
                                "-i",
                                v["input2"] ?: error("input2 required"),
                                "-filter_complex",
                                "amix=inputs=2:duration=longest",
                                "-y",
                                v["output"]?.ifBlank { null } ?: "output_merged.mp3",
                            )
                        },
                    ),
                    Operation(
                        id = "audio_trim",
                        labelRes = R.string.op_audio_trim,
                        descRes = R.string.op_audio_trim_desc,
                        icon = Icons.Outlined.ContentCut,
                        params =
                            listOf(
                                OperationParam.FilePicker(
                                    "input",
                                    R.string.param_input_file,
                                    mimeTypes = listOf("audio/*"),
                                ),
                                OperationParam.TextParam(
                                    "start",
                                    R.string.param_start_time,
                                    defaultValue = "00:00:00",
                                ),
                                OperationParam.TextParam(
                                    "duration",
                                    R.string.param_duration,
                                    defaultValue = "00:01:00",
                                ),
                                OperationParam.TextParam(
                                    "output",
                                    R.string.param_output_file,
                                    required = false,
                                    defaultValue = "output_trim.mp3",
                                ),
                            ),
                        commandBuilder = { v ->
                            listOf(
                                "-ss", v["start"] ?: "00:00:00",
                                "-i", v["input"] ?: error("input required"),
                                "-t", v["duration"] ?: "00:01:00",
                                "-c", "copy",
                                "-y", v["output"]?.ifBlank { null } ?: "output_trim.mp3",
                            )
                        },
                    ),
                    Operation(
                        id = "audio_normalize",
                        labelRes = R.string.op_audio_normalize,
                        descRes = R.string.op_audio_normalize_desc,
                        icon = Icons.Outlined.GraphicEq,
                        params =
                            listOf(
                                OperationParam.FilePicker(
                                    "input",
                                    R.string.param_input_file,
                                    mimeTypes = listOf("audio/*"),
                                ),
                                OperationParam.TextParam(
                                    "output",
                                    R.string.param_output_file,
                                    required = false,
                                    defaultValue = "output_normalized.mp3",
                                ),
                            ),
                        commandBuilder = { v ->
                            listOf(
                                "-i",
                                v["input"] ?: error("input required"),
                                "-af",
                                "loudnorm=I=-23:TP=-1.5:LRA=11",
                                "-y",
                                v["output"]?.ifBlank { null } ?: "output_normalized.mp3",
                            )
                        },
                    ),
                ),
        )

    val IMAGE =
        Category(
            id = "image",
            labelRes = R.string.category_image,
            icon = Icons.Outlined.Image,
            operations =
                listOf(
                    Operation(
                        id = "image_thumbnail",
                        labelRes = R.string.op_image_thumbnail,
                        descRes = R.string.op_image_thumbnail_desc,
                        icon = Icons.Outlined.Photo,
                        params =
                            listOf(
                                OperationParam.FilePicker(
                                    "input",
                                    R.string.param_input_file,
                                    mimeTypes = listOf("video/*"),
                                ),
                                OperationParam.TextParam(
                                    "time",
                                    R.string.param_thumbnail_time,
                                    defaultValue = "00:00:01",
                                ),
                                OperationParam.TextParam(
                                    "output",
                                    R.string.param_output_file,
                                    required = false,
                                    defaultValue = "thumbnail.jpg",
                                ),
                            ),
                        commandBuilder = { v ->
                            listOf(
                                "-ss",
                                v["time"] ?: "00:00:01",
                                "-i",
                                v["input"] ?: error("input required"),
                                "-frames:v",
                                "1",
                                "-y",
                                v["output"]?.ifBlank { null } ?: "thumbnail.jpg",
                            )
                        },
                    ),
                    Operation(
                        id = "image_slideshow",
                        labelRes = R.string.op_image_slideshow,
                        descRes = R.string.op_image_slideshow_desc,
                        icon = Icons.Outlined.Slideshow,
                        params =
                            listOf(
                                OperationParam.FilePicker(
                                    "input",
                                    R.string.param_input_file,
                                    mimeTypes = listOf("image/*"),
                                ),
                                OperationParam.SliderParam(
                                    "fps",
                                    R.string.param_fps,
                                    min = 1f,
                                    max = 30f,
                                    defaultValue = 2f,
                                    valueFormat = "%.0f",
                                ),
                                OperationParam.TextParam(
                                    "output",
                                    R.string.param_output_file,
                                    required = false,
                                    defaultValue = "slideshow.mp4",
                                ),
                            ),
                        commandBuilder = { v ->
                            val fps = v["fps"]?.toFloatOrNull()?.toInt() ?: 2
                            // Assumes user provides pattern like /path/to/img%04d.jpg
                            listOf(
                                "-framerate", "$fps",
                                "-i", v["input"] ?: error("input required"),
                                "-c:v", "libx264", "-pix_fmt", "yuv420p",
                                "-y", v["output"]?.ifBlank { null } ?: "slideshow.mp4",
                            )
                        },
                    ),
                    Operation(
                        id = "image_gif",
                        labelRes = R.string.op_image_gif,
                        descRes = R.string.op_image_gif_desc,
                        icon = Icons.Outlined.Gif,
                        params =
                            listOf(
                                OperationParam.FilePicker(
                                    "input",
                                    R.string.param_input_file,
                                    mimeTypes = listOf("video/*"),
                                ),
                                OperationParam.TextParam(
                                    "start",
                                    R.string.param_start_time,
                                    defaultValue = "00:00:00",
                                ),
                                OperationParam.TextParam(
                                    "duration",
                                    R.string.param_duration,
                                    defaultValue = "00:00:05",
                                ),
                                OperationParam.SliderParam(
                                    "fps",
                                    R.string.param_fps,
                                    min = 5f,
                                    max = 24f,
                                    defaultValue = 10f,
                                    valueFormat = "%.0f",
                                ),
                                OperationParam.TextParam(
                                    "output",
                                    R.string.param_output_file,
                                    required = false,
                                    defaultValue = "output.gif",
                                ),
                            ),
                        commandBuilder = { v ->
                            val fps = v["fps"]?.toFloatOrNull()?.toInt() ?: 10
                            listOf(
                                "-ss", v["start"] ?: "00:00:00",
                                "-i", v["input"] ?: error("input required"),
                                "-t", v["duration"] ?: "00:00:05",
                                "-vf", "fps=$fps,scale=480:-1:flags=lanczos,split[s0][s1];[s0]palettegen[p];[s1][p]paletteuse",
                                "-loop", "0",
                                "-y", v["output"]?.ifBlank { null } ?: "output.gif",
                            )
                        },
                    ),
                ),
        )

    val MUX =
        Category(
            id = "mux",
            labelRes = R.string.category_mux,
            icon = Icons.AutoMirrored.Outlined.CallSplit,
            operations =
                listOf(
                    Operation(
                        id = "mux_remux",
                        labelRes = R.string.op_mux_remux,
                        descRes = R.string.op_mux_remux_desc,
                        icon = Icons.Outlined.MoveToInbox,
                        params =
                            listOf(
                                OperationParam.FilePicker(
                                    "input",
                                    R.string.param_input_file,
                                    mimeTypes = listOf("video/*", "audio/*"),
                                ),
                                OperationParam.DropdownParam(
                                    "format",
                                    R.string.param_format,
                                    options = videoFormats,
                                    defaultIndex = 0,
                                ),
                                OperationParam.TextParam(
                                    "output",
                                    R.string.param_output_file,
                                    required = false,
                                    defaultValue = "output.mp4",
                                ),
                            ),
                        commandBuilder = { v ->
                            val fmt = v["format"] ?: "mp4"
                            listOf(
                                "-i",
                                v["input"] ?: error("input required"),
                                "-c",
                                "copy",
                                "-y",
                                v["output"]?.ifBlank { null } ?: "output.$fmt",
                            )
                        },
                    ),
                    Operation(
                        id = "mux_strip_audio",
                        labelRes = R.string.op_mux_strip_audio,
                        descRes = R.string.op_mux_strip_audio_desc,
                        icon = Icons.AutoMirrored.Outlined.VolumeOff,
                        params =
                            listOf(
                                OperationParam.FilePicker(
                                    "input",
                                    R.string.param_input_file,
                                    mimeTypes = listOf("video/*"),
                                ),
                                OperationParam.TextParam(
                                    "output",
                                    R.string.param_output_file,
                                    required = false,
                                    defaultValue = "output_noaudio.mp4",
                                ),
                            ),
                        commandBuilder = { v ->
                            listOf(
                                "-i",
                                v["input"] ?: error("input required"),
                                "-an",
                                "-c:v",
                                "copy",
                                "-y",
                                v["output"]?.ifBlank { null } ?: "output_noaudio.mp4",
                            )
                        },
                    ),
                    Operation(
                        id = "mux_strip_video",
                        labelRes = R.string.op_mux_strip_video,
                        descRes = R.string.op_mux_strip_video_desc,
                        icon = Icons.Outlined.VideocamOff,
                        params =
                            listOf(
                                OperationParam.FilePicker(
                                    "input",
                                    R.string.param_input_file,
                                    mimeTypes = listOf("video/*"),
                                ),
                                OperationParam.DropdownParam(
                                    "format",
                                    R.string.param_format,
                                    options = audioFormats,
                                    defaultIndex = 1,
                                ),
                                OperationParam.TextParam(
                                    "output",
                                    R.string.param_output_file,
                                    required = false,
                                    defaultValue = "output_novideo.aac",
                                ),
                            ),
                        commandBuilder = { v ->
                            val fmt = v["format"] ?: "aac"
                            listOf(
                                "-i",
                                v["input"] ?: error("input required"),
                                "-vn",
                                "-c:a",
                                "copy",
                                "-y",
                                v["output"]?.ifBlank { null } ?: "output_novideo.$fmt",
                            )
                        },
                    ),
                    Operation(
                        id = "mux_split",
                        labelRes = R.string.op_mux_split,
                        descRes = R.string.op_mux_split_desc,
                        icon = Icons.AutoMirrored.Outlined.CallSplit,
                        params =
                            listOf(
                                OperationParam.FilePicker(
                                    "input",
                                    R.string.param_input_file,
                                    mimeTypes = listOf("video/*", "audio/*"),
                                ),
                                OperationParam.SliderParam(
                                    "segment",
                                    R.string.param_segment_duration,
                                    min = 10f,
                                    max = 600f,
                                    defaultValue = 60f,
                                    valueFormat = "%.0f",
                                ),
                                OperationParam.TextParam(
                                    "output",
                                    R.string.param_output_file,
                                    required = false,
                                    defaultValue = "segment_%03d.mp4",
                                ),
                            ),
                        commandBuilder = { v ->
                            val seg = v["segment"]?.toFloatOrNull()?.toInt() ?: 60
                            listOf(
                                "-i", v["input"] ?: error("input required"),
                                "-c", "copy",
                                "-map", "0",
                                "-segment_time", "$seg",
                                "-f", "segment",
                                "-reset_timestamps", "1",
                                v["output"]?.ifBlank { null } ?: "segment_%03d.mp4",
                            )
                        },
                    ),
                    Operation(
                        id = "mux_concat",
                        labelRes = R.string.op_mux_concat,
                        descRes = R.string.op_mux_concat_desc,
                        icon = Icons.AutoMirrored.Outlined.CallMerge,
                        params =
                            listOf(
                                OperationParam.FilePicker(
                                    "input1",
                                    R.string.param_input_file,
                                    mimeTypes = listOf("video/*", "audio/*"),
                                ),
                                OperationParam.FilePicker(
                                    "input2",
                                    R.string.param_second_input,
                                    mimeTypes = listOf("video/*", "audio/*"),
                                ),
                                OperationParam.TextParam(
                                    "output",
                                    R.string.param_output_file,
                                    required = false,
                                    defaultValue = "output_concat.mp4",
                                ),
                            ),
                        commandBuilder = { v ->
                            val i1 = v["input1"] ?: error("input1 required")
                            val i2 = v["input2"] ?: error("input2 required")
                            listOf(
                                "-i", i1, "-i", i2,
                                "-filter_complex",
                                "[0:v][0:a][1:v][1:a]concat=n=2:v=1:a=1[outv][outa]",
                                "-map", "[outv]", "-map", "[outa]",
                                "-y", v["output"]?.ifBlank { null } ?: "output_concat.mp4",
                            )
                        },
                    ),
                ),
        )

    val SUBTITLES =
        Category(
            id = "subtitles",
            labelRes = R.string.category_subtitles,
            icon = Icons.Outlined.Subtitles,
            operations =
                listOf(
                    Operation(
                        id = "sub_burn",
                        labelRes = R.string.op_sub_burn,
                        descRes = R.string.op_sub_burn_desc,
                        icon = Icons.Outlined.ClosedCaption,
                        params =
                            listOf(
                                OperationParam.FilePicker(
                                    "input",
                                    R.string.param_input_file,
                                    mimeTypes = listOf("video/*"),
                                ),
                                OperationParam.FilePicker(
                                    "subtitle",
                                    R.string.param_subtitle_file,
                                    mimeTypes = listOf("*/*"),
                                ),
                                OperationParam.TextParam(
                                    "output",
                                    R.string.param_output_file,
                                    required = false,
                                    defaultValue = "output_subtitled.mp4",
                                ),
                            ),
                        commandBuilder = { v ->
                            val sub = v["subtitle"] ?: error("subtitle required")
                            listOf(
                                "-i",
                                v["input"] ?: error("input required"),
                                "-vf",
                                "subtitles=${FfmpegRepository.escapeFilterArg(sub)}",
                                "-y",
                                v["output"]?.ifBlank { null } ?: "output_subtitled.mp4",
                            )
                        },
                    ),
                    Operation(
                        id = "sub_extract",
                        labelRes = R.string.op_sub_extract,
                        descRes = R.string.op_sub_extract_desc,
                        icon = Icons.Outlined.FileDownload,
                        params =
                            listOf(
                                OperationParam.FilePicker(
                                    "input",
                                    R.string.param_input_file,
                                    mimeTypes = listOf("video/*"),
                                ),
                                OperationParam.TextParam(
                                    "output",
                                    R.string.param_output_file,
                                    required = false,
                                    defaultValue = "output.srt",
                                ),
                            ),
                        commandBuilder = { v ->
                            listOf(
                                "-i",
                                v["input"] ?: error("input required"),
                                "-map",
                                "0:s:0",
                                "-y",
                                v["output"]?.ifBlank { null } ?: "output.srt",
                            )
                        },
                    ),
                ),
        )

    val ALL: List<Category> = listOf(VIDEO, AUDIO, IMAGE, MUX, SUBTITLES)
}
