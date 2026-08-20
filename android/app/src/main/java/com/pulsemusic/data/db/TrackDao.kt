package com.pulsemusic.data.db

import androidx.room.*
import com.pulsemusic.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TrackDao {
    @Query("SELECT * FROM tracks ORDER BY lastPlayedAt DESC")
    fun getAllTracks(): Flow<List<Track>>

    @Query("SELECT * FROM tracks WHERE id = :id")
    suspend fun getTrack(id: String): Track?

    @Query("SELECT * FROM tracks WHERE youtubeId = :youtubeId LIMIT 1")
    suspend fun getTrackByYoutubeId(youtubeId: String): Track?

    @Upsert
    suspend fun upsertTrack(track: Track)

    @Upsert
    suspend fun upsertTracks(tracks: List<Track>)

    @Delete
    suspend fun deleteTrack(track: Track)

    @Query("DELETE FROM tracks WHERE id = :id")
    suspend fun deleteTrackById(id: String)

    @Query("UPDATE tracks SET lastPlayedAt = :playedAt, playCount = playCount + 1 WHERE id = :id")
    suspend fun recordPlay(id: String, playedAt: Long = System.currentTimeMillis())

    @Query("SELECT * FROM tracks WHERE title LIKE '%' || :query || '%' OR artist LIKE '%' || :query || '%'")
    fun searchTracks(query: String): Flow<List<Track>>
}

@Dao
interface PlaylistDao {
    @Query("SELECT * FROM playlists ORDER BY createdAt DESC")
    fun getAllPlaylists(): Flow<List<Playlist>>

    @Query("SELECT * FROM playlists WHERE id = :id")
    suspend fun getPlaylist(id: String): Playlist?

    @Insert
    suspend fun createPlaylist(playlist: Playlist)

    @Delete
    suspend fun deletePlaylist(playlist: Playlist)

    @Query("DELETE FROM playlists WHERE id = :id")
    suspend fun deletePlaylistById(id: String)

    @Query("SELECT t.* FROM tracks t INNER JOIN playlist_tracks pt ON t.id = pt.trackId WHERE pt.playlistId = :playlistId ORDER BY pt.position")
    fun getPlaylistTracks(playlistId: String): Flow<List<Track>>

    @Query("SELECT * FROM playlist_tracks WHERE playlistId = :playlistId ORDER BY position")
    suspend fun getPlaylistTrackEntries(playlistId: String): List<PlaylistTrack>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addTrackToPlaylist(entry: PlaylistTrack)

    @Query("DELETE FROM playlist_tracks WHERE playlistId = :playlistId AND trackId = :trackId")
    suspend fun removeTrackFromPlaylist(playlistId: String, trackId: String)

    @Query("DELETE FROM playlist_tracks WHERE playlistId = :playlistId")
    suspend fun clearPlaylist(playlistId: String)

    @Query("UPDATE playlist_tracks SET position = :position WHERE playlistId = :playlistId AND trackId = :trackId")
    suspend fun updateTrackPosition(playlistId: String, trackId: String, position: Int)
}

@Dao
interface FavoriteDao {
    @Query("SELECT t.* FROM tracks t INNER JOIN favorites f ON t.id = f.trackId ORDER BY f.createdAt DESC")
    fun getFavoriteTracks(): Flow<List<Track>>

    @Query("SELECT trackId FROM favorites")
    suspend fun getFavoriteIds(): List<String>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addFavorite(favorite: Favorite)

    @Query("DELETE FROM favorites WHERE trackId = :trackId")
    suspend fun removeFavorite(trackId: String)

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE trackId = :trackId)")
    suspend fun isFavorite(trackId: String): Boolean
}

@Dao
interface RecentDao {
    @Query("SELECT t.* FROM tracks t INNER JOIN recent_plays r ON t.id = r.trackId ORDER BY r.playedAt DESC LIMIT 40")
    fun getRecentTracks(): Flow<List<Track>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addRecent(recent: RecentPlay)

    @Query("DELETE FROM recent_plays")
    suspend fun clearRecent()
}

@Dao
interface QueueDao {
    @Query("SELECT t.* FROM tracks t INNER JOIN queue_items q ON t.id = q.trackId ORDER BY q.position")
    fun getQueue(): Flow<List<Track>>

    @Query("SELECT * FROM queue_items ORDER BY position")
    suspend fun getQueueItems(): List<QueueItem>

    @Query("SELECT COUNT(*) FROM queue_items")
    suspend fun getQueueCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addToQueue(item: QueueItem)

    @Query("DELETE FROM queue_items")
    suspend fun clearQueue()

    @Query("DELETE FROM queue_items WHERE trackId = :trackId")
    suspend fun removeFromQueue(trackId: String)

    @Query("UPDATE queue_items SET position = :position WHERE trackId = :trackId")
    suspend fun updateQueuePosition(trackId: String, position: Int)

    @Query("SELECT MAX(position) FROM queue_items")
    suspend fun getMaxPosition(): Int?
}
