package com.convex.app.data.ffmpeg

import android.content.Context
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.FFmpegKitConfig
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

class SecurityTest {

    private val context = mockk<Context>(relaxed = true)
    private lateinit var repository: FfmpegRepository
    private val cacheDir = File("/tmp/convex_cache")

    @Before
    fun setUp() {
        mockkStatic(FFmpegKit::class)
        mockkStatic(FFmpegKitConfig::class)
        
        if (!cacheDir.exists()) cacheDir.mkdirs()
        every { context.cacheDir } returns cacheDir
        
        repository = FfmpegRepository(context)
    }

    @After
    fun tearDown() {
        unmockkAll()
        cacheDir.deleteRecursively()
    }

    @Test
    fun `mapArguments removes null bytes`() = runBlocking {
        val argsSlot = slot<Array<String>>()
        every { 
            FFmpegKit.executeWithArgumentsAsync(capture(argsSlot), any(), any(), any()) 
        } returns mockk(relaxed = true)

        val args = listOf("input\u0000.mp4", "-vf", "subtitles=file\u0000.srt", "output\u0000.mp4")
        repository.execute(args).first()

        val captured = argsSlot.captured
        assertEquals("input.mp4", captured[0])
        assertEquals("subtitles=file.srt", captured[2])
        assertEquals("output.mp4", captured[3])
    }

    @Test
    fun `mapArguments prevents path traversal in cache`() = runBlocking {
        val argsSlot = slot<Array<String>>()
        every { 
            FFmpegKit.executeWithArgumentsAsync(capture(argsSlot), any(), any(), any()) 
        } returns mockk(relaxed = true)

        // Attempting to go outside cache dir
        val traversalArg = "../../etc/passwd.mp4"
        val args = listOf("-i", traversalArg, "output.mp4")
        
        repository.execute(args).first()

        val captured = argsSlot.captured
        // It should NOT have resolved to /etc/passwd.mp4
        // Since it's invalid for cache, it remains as is
        assertEquals(traversalArg, captured[1])
    }

    @Test
    fun `escapeFilterArg correctly escapes single quotes`() {
        val input = "my file's name.srt"
        val escaped = FfmpegRepository.escapeFilterArg(input)
        assertEquals("'my file'\\''s name.srt'", escaped)
    }
}
