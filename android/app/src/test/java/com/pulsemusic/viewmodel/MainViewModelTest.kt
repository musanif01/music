package com.pulsemusic.viewmodel

import android.app.Application
import com.pulsemusic.bridge.YTMusicBridge
import com.pulsemusic.data.model.Track
import com.pulsemusic.data.repository.MusicRepository
import com.pulsemusic.ui.navigation.Screen
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {

    private val application = mockk<Application>(relaxed = true)
    private val repository = mockk<MusicRepository>(relaxed = true)
    private val ytMusic = mockk<YTMusicBridge>(relaxed = true)
    
    private lateinit var viewModel: MainViewModel
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        
        // Mock default repository flows
        every { repository.getAllTracks() } returns flowOf(emptyList())
        every { repository.getAllPlaylists() } returns flowOf(emptyList())
        every { repository.getQueue() } returns flowOf(emptyList())
        every { repository.getRecentTracks() } returns flowOf(emptyList())
        coEvery { repository.getFavoriteIds() } returns emptyList()

        viewModel = MainViewModel(application, repository, ytMusic)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `navigateTo updates currentScreen`() {
        viewModel.navigateTo(Screen.Library)
        assertEquals(Screen.Library, viewModel.currentScreen.value)
    }

    @Test
    fun `playTrack records play in repository`() = runTest {
        val track = Track(id = "t1", source = "local", title = "T1", artist = "A1")
        viewModel.playTrack(track)
        
        coVerify { repository.recordPlay("t1") }
    }

    @Test
    fun `toggleFavorite calls repository`() = runTest {
        viewModel.toggleFavorite("t1")
        coVerify { repository.toggleFavorite("t1") }
    }

    @Test
    fun `search updates isSearching state`() = runTest {
        coEvery { ytMusic.search(any()) } coAnswers {
            assertEquals(true, viewModel.isSearching.value)
            emptyList()
        }
        
        viewModel.search("query")
        assertEquals(false, viewModel.isSearching.value)
    }
}
