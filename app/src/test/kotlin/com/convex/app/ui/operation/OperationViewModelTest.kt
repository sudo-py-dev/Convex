package com.convex.app.ui.operation

import androidx.lifecycle.SavedStateHandle
import com.convex.app.data.ffmpeg.FfmpegRepository
import com.convex.app.data.prefs.AppPreferences
import com.convex.app.domain.model.ExecutionState
import com.convex.app.domain.model.MediaInfo
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class OperationViewModelTest {

    private val ffmpeg = mockk<FfmpegRepository>(relaxed = true)
    private val prefs = mockk<AppPreferences>(relaxed = true)
    private val context = mockk<android.content.Context>(relaxed = true)
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        every { prefs.technicalMode } returns flowOf(false)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initialization loads operation from definitions`() = runTest {
        val savedStateHandle = SavedStateHandle(mapOf("categoryId" to "video", "operationId" to "video_convert"))
        val viewModel = OperationViewModel(ffmpeg, prefs, context, savedStateHandle)
        
        advanceUntilIdle()
        
        val state = viewModel.uiState.value
        assertNotNull(state.operation)
        assertEquals("video_convert", state.operation?.id)
        // Check if defaults are loaded
        assertEquals("libx264", state.paramValues["vcodec"])
    }

    @Test
    fun `updateParam updates state and rebuilds command`() = runTest {
        val savedStateHandle = SavedStateHandle(mapOf("categoryId" to "video", "operationId" to "video_convert"))
        val viewModel = OperationViewModel(ffmpeg, prefs, context, savedStateHandle)
        
        advanceUntilIdle()
        
        viewModel.updateParam("vcodec", "libx265")
        
        val state = viewModel.uiState.value
        assertEquals("libx265", state.paramValues["vcodec"])
        assert(state.generatedCommand.contains("libx265"))
    }

    @Test
    fun `resolveFileUri suggests output name for input file`() = runTest {
        val savedStateHandle = SavedStateHandle(mapOf("categoryId" to "video", "operationId" to "video_convert"))
        val viewModel = OperationViewModel(ffmpeg, prefs, context, savedStateHandle)
        
        advanceUntilIdle()
        
        viewModel.resolveFileUri("input", mockk(), "my_video.mp4", "/path/to/my_video.mp4")
        
        val state = viewModel.uiState.value
        assertEquals("/path/to/my_video.mp4", state.paramValues["input"])
        assertEquals("my_video_convex.mp4", state.paramValues["output"])
    }

    @Test
    fun `startExecution shows error if required files missing`() = runTest {
        val savedStateHandle = SavedStateHandle(mapOf("categoryId" to "video", "operationId" to "video_convert"))
        val viewModel = OperationViewModel(ffmpeg, prefs, context, savedStateHandle)
        
        advanceUntilIdle()
        
        viewModel.startExecution()
        
        val state = viewModel.uiState.value
        assertNotNull(state.validationError)
        assertEquals(ExecutionState.Idle, state.executionState)
    }

    @Test
    fun `startExecution calls ffmpeg execute when valid`() = runTest {
        val savedStateHandle = SavedStateHandle(mapOf("categoryId" to "video", "operationId" to "video_convert"))
        val viewModel = OperationViewModel(ffmpeg, prefs, context, savedStateHandle)
        
        advanceUntilIdle()
        
        viewModel.updateParam("input", "/path/to/input.mp4")
        
        every { ffmpeg.execute(any(), any()) } returns emptyFlow()
        
        viewModel.startExecution()
        
        advanceUntilIdle()
        
        verify { ffmpeg.execute(any(), any()) }
    }

    @Test
    fun `auto-probe happens when input file is updated`() = runTest {
        val savedStateHandle = SavedStateHandle(mapOf("categoryId" to "video", "operationId" to "video_convert"))
        val viewModel = OperationViewModel(ffmpeg, prefs, context, savedStateHandle)
        
        val mockMediaInfo = MediaInfo("/path/to/input.mp4", "10.5", 1024L, null, null, null, null, null, null)
        coEvery { ffmpeg.probe(any()) } returns mockMediaInfo
        
        viewModel.updateParam("input", "/path/to/input.mp4")
        
        advanceUntilIdle()
        
        assertEquals(mockMediaInfo, viewModel.uiState.value.mediaInfo)
    }
}
