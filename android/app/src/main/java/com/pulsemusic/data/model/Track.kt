package com.pulsemusic.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tracks")
data class Track(
    @PrimaryKey val id: String,
    val source: String,
    val youtubeId: String? = null,
    val title: String,
    val artist: String,
    val album: String? = null,
    val duration: String? = null,
    val durationMs: Long? = null,
    val thumbnail: String? = null,
    val url: String? = null,
    val fileUri: String? = null,
    val fileName: String? = null,
    val art: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val lastPlayedAt: Long? = null,
    val playCount: Int = 0
)

@Entity(tableName = "playlists")
data class Playlist(
    @PrimaryKey val id: String,
    val name: String,
    val description: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "playlist_tracks", primaryKeys = ["playlistId", "trackId"])
data class PlaylistTrack(
    val playlistId: String,
    val trackId: String,
    val position: Int,
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "favorites", primaryKeys = ["trackId"])
data class Favorite(
    val trackId: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "recent_plays", primaryKeys = ["trackId"])
data class RecentPlay(
    val trackId: String,
    val playedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "queue_items")
data class QueueItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val trackId: String,
    val position: Int,
    val context: String? = null
)
