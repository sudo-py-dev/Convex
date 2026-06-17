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
    private val cacheDir = File("build/test_cache").absoluteFile

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
    fun `mapArguments removes null bytes and resolves cache`() = runBlocking {
        val argsSlot = slot<Array<String>>()
        every { 
            FFmpegKit.executeWithArgumentsAsync(capture(argsSlot), any<com.arthenica.ffmpegkit.FFmpegSessionCompleteCallback>(), any<com.arthenica.ffmpegkit.LogCallback>(), any<com.arthenica.ffmpegkit.StatisticsCallback>()) 
        } returns mockk(relaxed = true)

        val args = listOf("input\u0000.mp4", "output\u0000.mp4")
        repository.execute(args).first()

        val captured = argsSlot.captured
        
        // After sanitization and cache resolution
        assertTrue("Captured[0] is ${captured[0]}", captured[0].contains("input.mp4"))
        assertTrue("Captured[1] is ${captured[1]}", captured[1].contains("output.mp4"))
    }

    @Test
    fun `mapArguments handles SAF URIs correctly`() = runBlocking {
        val argsSlot = slot<Array<String>>()
        every { 
            FFmpegKit.executeWithArgumentsAsync(capture(argsSlot), any<com.arthenica.ffmpegkit.FFmpegSessionCompleteCallback>(), any<com.arthenica.ffmpegkit.LogCallback>(), any<com.arthenica.ffmpegkit.StatisticsCallback>()) 
        } returns mockk(relaxed = true)
        
        every { FFmpegKitConfig.getSafParameterForRead(any(), any()) } returns "saf:0"
        every { FFmpegKitConfig.getSafParameterForWrite(any(), any()) } returns "saf:1"

        val args = listOf("-i", "content://media/external/video/media/1", "content://document/2")
        repository.execute(args).first()

        val captured = argsSlot.captured
        assertEquals("saf:0", captured[1])
        assertEquals("saf:1", captured[2])
    }

    @Test
    fun `mapArguments prevents path traversal`() = runBlocking {
        val argsSlot = slot<Array<String>>()
        every { 
            FFmpegKit.executeWithArgumentsAsync(capture(argsSlot), any<com.arthenica.ffmpegkit.FFmpegSessionCompleteCallback>(), any<com.arthenica.ffmpegkit.LogCallback>(), any<com.arthenica.ffmpegkit.StatisticsCallback>()) 
        } returns mockk(relaxed = true)

        val traversalArg = "../../etc/passwd.mp4"
        val args = listOf("-i", traversalArg)
        
        repository.execute(args).first()

        val captured = argsSlot.captured
        assertEquals(traversalArg, captured[1])
    }

    @Test
    fun `escapeFilterArg correctly escapes single quotes`() {
        val input = "my file's name.srt"
        val escaped = FfmpegRepository.escapeFilterArg(input)
        assertEquals("'my file'\\''s name.srt'", escaped)
    }
}
