package com.convex.app.data.operations

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OperationDefinitionsTest {

    @Test
    fun `video_convert command builder produces correct arguments`() {
        val op = OperationDefinitions.VIDEO.operations.first { it.id == "video_convert" }
        val values = mapOf(
            "input" to "in.mp4",
            "vcodec" to "libx264",
            "acodec" to "aac",
            "format" to "mp4",
            "output" to "out.mp4"
        )
        val args = op.commandBuilder(values)
        
        assertEquals(listOf("-i", "in.mp4", "-c:v", "libx264", "-c:a", "aac", "-y", "out.mp4"), args)
    }

    @Test
    fun `video_trim command builder produces correct arguments`() {
        val op = OperationDefinitions.VIDEO.operations.first { it.id == "video_trim" }
        val values = mapOf(
            "input" to "in.mp4",
            "start" to "00:00:10",
            "duration" to "00:00:05",
            "output" to "trimmed.mp4"
        )
        val args = op.commandBuilder(values)
        
        assertEquals(listOf("-ss", "00:00:10", "-i", "in.mp4", "-t", "00:00:05", "-c", "copy", "-y", "trimmed.mp4"), args)
    }

    @Test
    fun `audio_merge command builder handles multiple inputs`() {
        val op = OperationDefinitions.AUDIO.operations.first { it.id == "audio_merge" }
        val values = mapOf(
            "input1" to "a.mp3",
            "input2" to "b.mp3",
            "output" to "merged.mp3"
        )
        val args = op.commandBuilder(values)
        
        assertTrue(args.contains("-filter_complex"))
        assertTrue(args.contains("amix=inputs=2:duration=longest"))
    }

    @Test
    fun `image_gif command builder uses palettegen for high quality`() {
        val op = OperationDefinitions.IMAGE.operations.first { it.id == "image_gif" }
        val values = mapOf(
            "input" to "video.mp4",
            "start" to "00:00:00",
            "duration" to "00:00:05",
            "fps" to "10",
            "output" to "out.gif"
        )
        val args = op.commandBuilder(values)
        
        val filter = args[args.indexOf("-vf") + 1]
        assertTrue(filter.contains("palettegen"))
        assertTrue(filter.contains("paletteuse"))
    }
}
