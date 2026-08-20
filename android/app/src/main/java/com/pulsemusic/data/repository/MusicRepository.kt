package com.pulsemusic.data.repository

import com.pulsemusic.data.db.AppDatabase
import com.pulsemusic.data.model.*
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class MusicRepository(private val db: AppDatabase) {

    private val trackDao = db.trackDao()
    private val playlistDao = db.playlistDao()
    private val favoriteDao = db.favoriteDao()
    private val recentDao = db.recentDao()
    private val queueDao = db.queueDao()

    fun getAllTracks(): Flow<List<Track>> = trackDao.getAllTracks()
    fun getFavoriteTracks(): Flow<List<Track>> = favoriteDao.getFavoriteTracks()
    fun getRecentTracks(): Flow<List<Track>> = recentDao.getRecentTracks()
    fun getQueue(): Flow<List<Track>> = queueDao.getQueue()
    fun getAllPlaylists(): Flow<List<Playlist>> = playlistDao.getAllPlaylists()
    fun getPlaylistTracks(playlistId: String): Flow<List<Track>> = playlistDao.getPlaylistTracks(playlistId)

    suspend fun getTrack(id: String): Track? = trackDao.getTrack(id)
    suspend fun getTrackByYoutubeId(youtubeId: String): Track? = trackDao.getTrackByYoutubeId(youtubeId)
    suspend fun upsertTrack(track: Track) = trackDao.upsertTrack(track)
    suspend fun upsertTracks(tracks: List<Track>) = trackDao.upsertTracks(tracks)
    suspend fun deleteTrackById(id: String) = trackDao.deleteTrackById(id)

    suspend fun isFavorite(trackId: String): Boolean = favoriteDao.isFavorite(trackId)
    suspend fun getFavoriteIds(): List<String> = favoriteDao.getFavoriteIds()

    suspend fun toggleFavorite(trackId: String) {
        if (favoriteDao.isFavorite(trackId)) {
            favoriteDao.removeFavorite(trackId)
        } else {
            favoriteDao.addFavorite(Favorite(trackId = trackId))
        }
    }

    suspend fun recordPlay(trackId: String) {
        trackDao.recordPlay(trackId)
        recentDao.addRecent(RecentPlay(trackId = trackId))
    }

    suspend fun clearRecent() = recentDao.clearRecent()

    suspend fun addToQueue(trackId: String) {
        val count = queueDao.getQueueCount()
        queueDao.addToQueue(QueueItem(trackId = trackId, position = count))
    }

    suspend fun removeFromQueue(trackId: String) = queueDao.removeFromQueue(trackId)
    suspend fun clearQueue() = queueDao.clearQueue()
    suspend fun getQueueItems(): List<QueueItem> = queueDao.getQueueItems()

    suspend fun moveQueueTrack(trackId: String, direction: Int) {
        val items = queueDao.getQueueItems()
        val index = items.indexOfFirst { it.trackId == trackId }
        if (index < 0) return
        val targetIndex = index + direction
        if (targetIndex < 0 || targetIndex >= items.size) return
        val itemAtTarget = items[targetIndex]
        queueDao.updateQueuePosition(trackId, itemAtTarget.position)
        queueDao.updateQueuePosition(itemAtTarget.trackId, items[index].position)
    }

    suspend fun movePlaylistTrack(playlistId: String, trackId: String, direction: Int) {
        val entries = playlistDao.getPlaylistTrackEntries(playlistId)
        val index = entries.indexOfFirst { it.trackId == trackId }
        if (index < 0) return
        val targetIndex = index + direction
        if (targetIndex < 0 || targetIndex >= entries.size) return
        val entryAtTarget = entries[targetIndex]
        playlistDao.updateTrackPosition(playlistId, trackId, entryAtTarget.position)
        playlistDao.updateTrackPosition(playlistId, entryAtTarget.trackId, entries[index].position)
    }

    suspend fun createPlaylist(name: String): Playlist {
        val playlist = Playlist(id = newId("playlist"), name = name)
        playlistDao.createPlaylist(playlist)
        return playlist
    }

    suspend fun deletePlaylist(id: String) {
        playlistDao.clearPlaylist(id)
        playlistDao.deletePlaylistById(id)
    }

    suspend fun addTrackToPlaylist(playlistId: String, trackId: String) {
        val existing = playlistDao.getPlaylistTrackEntries(playlistId)
        val position = existing.size
        playlistDao.addTrackToPlaylist(
            PlaylistTrack(playlistId = playlistId, trackId = trackId, position = position)
        )
    }

    suspend fun removeTrackFromPlaylist(playlistId: String, trackId: String) {
        playlistDao.removeTrackFromPlaylist(playlistId, trackId)
    }

    private fun newId(prefix: String): String {
        return "$prefix-${UUID.randomUUID()}"
    }
}
