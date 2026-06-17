package com.arthenica.ffmpegkit

import android.content.Context
import android.net.Uri
import com.convex.app.data.ffmpeg.FfmpegRepository
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
import org.junit.AfterClass
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import java.io.File

class FfmpegRepositoryTest {

    private val context = mockk<Context>(relaxed = true)
    private lateinit var repository: FfmpegRepository
    private val argsSlot = slot<Array<String>>()

    companion object {
        @JvmStatic
        @BeforeClass
        fun setUpClass() {
            mockkStatic(FFmpegKit::class)
            mockkStatic(FFmpegKitConfig::class)
            mockkStatic(Uri::class)
            mockkStatic(android.util.Log::class)
            mockkStatic(NativeLoader::class)
            
            every { android.util.Log.d(any<String>(), any<String>()) } returns 0
            every { android.util.Log.i(any<String>(), any<String>()) } returns 0
            every { android.util.Log.w(any<String>(), any<String>()) } returns 0
            every { android.util.Log.e(any<String>(), any<String>()) } returns 0
            every { FFmpegKit.cancel(any<Long>()) } returns Unit
            every { NativeLoader.loadFFmpegKitAbiDetect() } returns Unit
            every { NativeLoader.loadFFmpeg() } returns true
        }

        @JvmStatic
        @AfterClass
        fun tearDownClass() {
            unmockkAll()
        }
    }

    @Before
    fun setup() {
        repository = FfmpegRepository(context)
        
        // Mock Uri.parse
        every { Uri.parse(any()) } answers {
            val uri = mockk<Uri>()
            every { uri.toString() } returns firstArg()
            every { uri.path } returns firstArg<String>().substringAfter("file://")
            every { uri.equals(any<Any>()) } answers { firstArg<Any>() === uri }
            uri
        }

        // Default stub for executeWithArgumentsAsync to prevent hanging and capture argsSlot
        every {
            FFmpegKit.executeWithArgumentsAsync(capture(argsSlot), any(), any(), any())
        } answers {
            val session = mockk<FFmpegSession>(relaxed = true)
            every { session.sessionId } returns 123L
            every { session.returnCode } returns ReturnCode(0)
            
            val callback = secondArg<FFmpegSessionCompleteCallback>()
            callback.apply(session)
            session
        }
    }

    @Test
    fun `execute emits Completed when FFmpeg succeeds`() = runBlocking {
        val session = mockk<FFmpegSession>()
        val completeCallbackSlot = slot<FFmpegSessionCompleteCallback>()
        
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
            completeCallbackSlot.captured.apply(session)
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
        val completeCallbackSlot = slot<FFmpegSessionCompleteCallback>()
        
        every { session.sessionId } returns 123L
        every { session.returnCode } returns ReturnCode(1)
        
        every {
            FFmpegKit.executeWithArgumentsAsync(any(), capture(completeCallbackSlot), any(), any())
        } returns session

        val flow = repository.execute(listOf("-i", "input.mp4", "output.mp4"))
        
        val job = launch {
            delay(10)
            completeCallbackSlot.captured.apply(session)
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
        val cacheDir = File("/tmp/cache")
        every { context.cacheDir } returns cacheDir
        
        runBlocking {
            repository.execute(listOf("-i", "input.mp4", "temp.mp4")).first()
        }

        val capturedArgs = argsSlot.captured
        assertTrue(capturedArgs[2].startsWith("/tmp/cache/"))
        assertTrue(capturedArgs[2].endsWith("temp.mp4"))
    }
}
