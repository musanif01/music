package com.pulsemusic.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.pulsemusic.data.db.AppDatabase
import com.pulsemusic.data.model.Track
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MusicRepositoryTest {

    private lateinit var db: AppDatabase
    private lateinit var repository: MusicRepository

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = MusicRepository(db)
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun `upsertTrack and getTrack works`() = runTest {
        val track = Track(
            id = "test-1",
            source = "local",
            title = "Test Track",
            artist = "Test Artist",
            thumbnail = ""
        )
        repository.upsertTrack(track)
        
        val retrieved = repository.getTrack("test-1")
        assertNotNull(retrieved)
        assertEquals("Test Track", retrieved?.title)
    }

    @Test
    fun `toggleFavorite adds and removes favorite`() = runTest {
        val trackId = "test-1"
        
        // Initially not favorite
        assertTrue(repository.getFavoriteIds().isEmpty())
        
        // Toggle on
        repository.toggleFavorite(trackId)
        val favIds = repository.getFavoriteIds()
        assertEquals(1, favIds.size)
        assertEquals(trackId, favIds[0])
        
        // Toggle off
        repository.toggleFavorite(trackId)
        assertTrue(repository.getFavoriteIds().isEmpty())
    }

    @Test
    fun `addToQueue and getQueue works`() = runTest {
        val track = Track(id = "test-1", source = "local", title = "T1", artist = "A1")
        repository.upsertTrack(track)
        
        repository.addToQueue("test-1")
        
        val queue = repository.getQueue().first()
        assertEquals(1, queue.size)
        assertEquals("test-1", queue[0].id)
    }

    @Test
    fun `moveQueueTrack changes positions`() = runTest {
        val t1 = Track(id = "t1", source = "local", title = "T1", artist = "A1")
        val t2 = Track(id = "t2", source = "local", title = "T2", artist = "A2")
        repository.upsertTracks(listOf(t1, t2))
        
        repository.addToQueue("t1")
        repository.addToQueue("t2")
        
        // Initial order: t1, t2
        var queue = repository.getQueue().first()
        assertEquals("t1", queue[0].id)
        assertEquals("t2", queue[1].id)
        
        // Move t1 down
        repository.moveQueueTrack("t1", 1)
        queue = repository.getQueue().first()
        assertEquals("t2", queue[0].id)
        assertEquals("t1", queue[1].id)
    }

    @Test
    fun `createPlaylist and addTrackToPlaylist works`() = runTest {
        val track = Track(id = "t1", source = "local", title = "T1", artist = "A1")
        repository.upsertTrack(track)
        
        val playlist = repository.createPlaylist("My Playlist")
        repository.addTrackToPlaylist(playlist.id, "t1")
        
        val playlistTracks = repository.getPlaylistTracks(playlist.id).first()
        assertEquals(1, playlistTracks.size)
        assertEquals("t1", playlistTracks[0].id)
    }
}
