package com.pulsemusic.service

import android.app.PendingIntent
import android.content.Intent
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.pulsemusic.MainActivity
import com.pulsemusic.data.db.AppDatabase
import com.pulsemusic.data.model.Track
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first

class MusicService : MediaSessionService() {

    private lateinit var player: ExoPlayer
    private lateinit var mediaSession: MediaSession
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()

        player = ExoPlayer.Builder(this)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .build()

        player.playWhenReady = true

        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(
                PendingIntent.getActivity(
                    this,
                    0,
                    Intent(this, MainActivity::class.java),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession {
        return mediaSession
    }

    fun playLocalTrack(track: Track) {
        val uri = track.fileUri ?: return
        val mediaItem = MediaItem.Builder()
            .setMediaId(track.id)
            .setUri(uri)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(track.title)
                    .setArtist(track.artist)
                    .apply { track.thumbnail?.let { setArtworkUri(android.net.Uri.parse(it)) } }
                    .build()
            )
            .build()

        player.setMediaItem(mediaItem)
        player.prepare()
        player.play()
    }

    fun playQueue(tracks: List<Track>) {
        val mediaItems = tracks.map { track ->
            val uri = track.fileUri ?: return@map null
            MediaItem.Builder()
                .setMediaId(track.id)
                .setUri(uri)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(track.title)
                        .setArtist(track.artist)
                        .apply { track.thumbnail?.let { setArtworkUri(android.net.Uri.parse(it)) } }
                        .build()
                )
                .build()
        }.filterNotNull()

        if (mediaItems.isEmpty()) return

        player.setMediaItems(mediaItems)
        player.prepare()
        player.play()
    }

    fun setQueue(tracks: List<Track>) {
        val mediaItems = tracks.mapNotNull { track ->
            val uri = track.fileUri ?: return@mapNotNull null
            MediaItem.Builder()
                .setMediaId(track.id)
                .setUri(uri)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(track.title)
                        .setArtist(track.artist)
                        .apply { track.thumbnail?.let { setArtworkUri(android.net.Uri.parse(it)) } }
                        .build()
                )
                .build()
        }
        player.setMediaItems(mediaItems)
    }

    fun addToQueue(tracks: List<Track>) {
        val mediaItems = tracks.mapNotNull { track ->
            val uri = track.fileUri ?: return@mapNotNull null
            MediaItem.Builder()
                .setMediaId(track.id)
                .setUri(uri)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(track.title)
                        .setArtist(track.artist)
                        .apply { track.thumbnail?.let { setArtworkUri(android.net.Uri.parse(it)) } }
                        .build()
                )
                .build()
        }
        player.addMediaItems(mediaItems)
    }

    val currentPlayer: ExoPlayer
        get() = player

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession.player
        if (!player.playWhenReady || player.mediaItemCount == 0) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        mediaSession.run {
            player.release()
            release()
        }
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        const val ACTION_PLAY = "com.pulsemusic.action.PLAY"
        const val ACTION_PAUSE = "com.pulsemusic.action.PAUSE"
        const val ACTION_NEXT = "com.pulsemusic.action.NEXT"
        const val ACTION_PREV = "com.pulsemusic.action.PREV"
        const val EXTRA_TRACK_ID = "track_id"
    }
}
