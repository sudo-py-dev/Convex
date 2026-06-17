package com.convex.app.data.ffmpeg

import android.content.Context
import android.net.Uri
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.FFmpegKitConfig
import com.arthenica.ffmpegkit.FFmpegSession
import com.arthenica.ffmpegkit.ReturnCode
import com.convex.app.domain.model.ExecutionState
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

class FfmpegRepositoryTest {

    private val context = mockk<Context>(relaxed = true)
    private lateinit var repository: FfmpegRepository

    @Before
    fun setup() {
        mockkStatic(FFmpegKit::class)
        mockkStatic(FFmpegKitConfig::class)
        mockkStatic(Uri::class)
        repository = FfmpegRepository(context)
        
        // Mock Uri.parse
        every { Uri.parse(any()) } answers {
            val uri = mockk<Uri>()
            every { uri.toString() } returns firstArg()
            every { uri.path } returns firstArg<String>().substringAfter("file://")
            uri
        }
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `execute emits Completed when FFmpeg succeeds`() = runBlocking {
        val session = mockk<FFmpegSession>()
        val completeCallbackSlot = slot<(FFmpegSession) -> Unit>()
        
        every { session.sessionId } returns 123L
        every { session.returnCode } returns ReturnCode(0)
        
        every {
            FFmpegKit.executeWithArgumentsAsync(any(), capture(completeCallbackSlot), any(), any())
        } returns session

        val flow = repository.execute(listOf("-i", "input.mp4", "output.mp4"))
        
        // In a real scenario, the callback is called by FFmpegKit
        // We simulate it here
        val job = launch {
            delay(10)
            completeCallbackSlot.captured(session)
        }

        val results = flow.toList()
        assertTrue(results.any { it is ExecutionState.Completed })
        val completed = results.last() as ExecutionState.Completed
        assertEquals("output.mp4", completed.outputPath)
        job.join()
    }

    @Test
    fun `execute emits Failed when FFmpeg returns error code`() = runBlocking {
        val session = mockk<FFmpegSession>()
        val completeCallbackSlot = slot<(FFmpegSession) -> Unit>()
        
        every { session.sessionId } returns 123L
        every { session.returnCode } returns ReturnCode(1)
        
        every {
            FFmpegKit.executeWithArgumentsAsync(any(), capture(completeCallbackSlot), any(), any())
        } returns session

        val flow = repository.execute(listOf("-i", "input.mp4", "output.mp4"))
        
        val job = launch {
            delay(10)
            completeCallbackSlot.captured(session)
        }

        val results = flow.toList()
        assertTrue(results.any { it is ExecutionState.Failed })
        val failed = results.last() as ExecutionState.Failed
        assertEquals(1, failed.returnCode)
        job.join()
    }

    @Test
    fun `mapArguments handles content URIs correctly`() {
        // We can't directly test private mapArguments, but we can test it via execute
        // by checking what is passed to FFmpegKit.executeWithArgumentsAsync
        
        val argsSlot = slot<Array<String>>()
        every {
            FFmpegKit.executeWithArgumentsAsync(capture(argsSlot), any(), any(), any())
        } returns mockk(relaxed = true)

        every { FFmpegKitConfig.getSafParameterForRead(any(), any()) } returns "/saf/read/path"
        every { FFmpegKitConfig.getSafParameterForWrite(any(), any()) } returns "/saf/write/path"

        runBlocking {
            repository.execute(listOf("-i", "content://media/1", "content://media/2")).first()
        }

        val capturedArgs = argsSlot.captured
        assertEquals("/saf/read/path", capturedArgs[1])
        assertEquals("/saf/write/path", capturedArgs[2])
    }

    @Test
    fun `mapArguments handles relative cache paths`() {
        val argsSlot = slot<Array<String>>()
        val cacheDir = File("/tmp/cache")
        every { context.cacheDir } returns cacheDir
        
        every {
            FFmpegKit.executeWithArgumentsAsync(capture(argsSlot), any(), any(), any())
        } returns mockk(relaxed = true)

        runBlocking {
            repository.execute(listOf("-i", "input.mp4", "temp.mp4")).first()
        }

        val capturedArgs = argsSlot.captured
        assertTrue(capturedArgs[2].startsWith("/tmp/cache/"))
        assertTrue(capturedArgs[2].endsWith("temp.mp4"))
    }
}
