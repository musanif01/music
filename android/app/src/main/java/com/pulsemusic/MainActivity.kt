package com.pulsemusic

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ComponentName
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.pulsemusic.bridge.YTMusicBridge
import com.pulsemusic.data.model.*
import com.pulsemusic.service.MusicService
import com.pulsemusic.ui.navigation.Screen
import com.pulsemusic.ui.screens.*
import com.pulsemusic.ui.components.FullScreenPlayer
import com.pulsemusic.ui.components.PlayerBar
import com.pulsemusic.ui.components.YouTubePlayerView
import com.pulsemusic.ui.theme.PulseMusicTheme
import com.pulsemusic.viewmodel.MainViewModel
import com.pulsemusic.viewmodel.RepeatMode

class MainActivity : ComponentActivity() {

    private lateinit var mediaControllerFuture: ListenableFuture<MediaController>
    private var mediaController: MediaController? = null

    private val requestNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* granted or not */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        createNotificationChannel()
        requestNotificationPermissionIfNeeded()

        val sessionToken = SessionToken(this, ComponentName(this, MusicService::class.java))
        mediaControllerFuture = MediaController.Builder(this, sessionToken).buildAsync()
        mediaControllerFuture.addListener({
            mediaController = mediaControllerFuture.get()
        }, MoreExecutors.directExecutor())

        val app = application as PulseMusicApp
        val ytMusic = YTMusicBridge(this)

        setContent {
            PulseMusicTheme {
                val vm: MainViewModel = viewModel {
                    MainViewModel(application, app.repository, ytMusic)
                }

                LaunchedEffect(mediaController) {
                    mediaController?.let { vm.setMediaController(it) }
                }

                LaunchedEffect(Unit) {
                    startMusicService()
                }

                PulseMusicMain(vm = vm)
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "pulse_music_playback",
                "Music Playback",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows currently playing track"
            }
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(channel)
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun startMusicService() {
        val intent = android.content.Intent(this, MusicService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    override fun onDestroy() {
        if (::mediaControllerFuture.isInitialized) {
            MediaController.releaseFuture(mediaControllerFuture)
        }
        super.onDestroy()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PulseMusicMain(vm: MainViewModel) {
    val currentScreen by vm.currentScreen.collectAsState()
    val showFullScreenPlayer by vm.showFullScreenPlayer.collectAsState()
    val showYouTubePlayer by vm.showYouTubePlayer.collectAsState()
    val searchQuery by vm.searchQuery.collectAsState()
    val typedSearchResults by vm.typedSearchResults.collectAsState()
    val isSearching by vm.isSearching.collectAsState()
    val searchSuggestions by vm.searchSuggestions.collectAsState()
    val playingTrackId by vm.playingTrackId.collectAsState()
    val isPlaying by vm.isPlaying.collectAsState()
    val currentYouTubeVideoId by vm.currentYouTubeVideoId.collectAsState()
    val progress by vm.progress.collectAsState()
    val isShuffle by vm.isShuffle.collectAsState()
    val repeatMode by vm.repeatMode.collectAsState()
    val libraryFilter by vm.libraryFilter.collectAsState()
    val selectedPlaylistId by vm.selectedPlaylistId.collectAsState()
    val selectedArtistId by vm.selectedArtistId.collectAsState()
    val selectedAlbumId by vm.selectedAlbumId.collectAsState()
    val artistDetail by vm.artistDetail.collectAsState()
    val albumDetail by vm.albumDetail.collectAsState()
    val isLoadingDetail by vm.isLoadingDetail.collectAsState()
    val region by vm.region.collectAsState()
    val safeSearch by vm.safeSearch.collectAsState()
    val isAuthenticated by vm.isAuthenticated.collectAsState()
    val showPlaylistDialog by vm.showPlaylistDialog.collectAsState()
    val dialogTrackId by vm.dialogTrackId.collectAsState()
    val favoriteIds by vm.favoriteIds.collectAsState()
    val allTracks by vm.allTracks.collectAsState()
    val playlists by vm.playlists.collectAsState()
    val queueTracks by vm.queueTracks.collectAsState()
    val recentTracks by vm.recentTracks.collectAsState()
    val playlistTracks by vm.playlistTracks.collectAsState()
    val libraryTracks by vm.libraryTracks.collectAsState()
    val localTracks by vm.localTracks.collectAsState()
    val currentTrack by vm.currentTrack.collectAsState()

    val selectedPlaylist = playlists.find { it.id == selectedPlaylistId }

    Scaffold(
        bottomBar = {
            Surface(shadowElevation = 8.dp) {
                Column {
                    if (currentTrack != null && !showFullScreenPlayer) {
                        PlayerBar(
                            track = currentTrack,
                            isPlaying = isPlaying,
                            isFavorite = currentTrack?.let { favoriteIds.contains(it.id) } ?: false,
                            onPlayPause = { vm.togglePlayPause() },
                            onNext = { vm.playNextTrack() },
                            onPrevious = { vm.playPreviousTrack() },
                            onFavorite = {
                                currentTrack?.let { vm.toggleFavorite(it.id) }
                            },
                            onClick = { vm.showFullScreenPlayer() }
                        )
                    }

                    NavigationBar {
                        Screen.bottomNavItems.forEach { screen ->
                            NavigationBarItem(
                                selected = currentScreen == screen,
                                onClick = { vm.navigateTo(screen) },
                                icon = {
                                    Icon(
                                        if (currentScreen == screen) screen.selectedIcon else screen.unselectedIcon,
                                        contentDescription = screen.label
                                    )
                                },
                                label = { Text(screen.label) }
                            )
                        }
                    }
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when {
                showFullScreenPlayer && currentTrack != null -> {
                    FullScreenPlayer(
                        track = currentTrack,
                        isPlaying = isPlaying,
                        progress = progress,
                        onPlayPause = { vm.togglePlayPause() },
                        onNext = { vm.playNextTrack() },
                        onPrevious = { vm.playPreviousTrack() },
                        onShuffle = { vm.toggleShuffle() },
                        onRepeat = { vm.cycleRepeat() },
                        onFavorite = { currentTrack?.let { vm.toggleFavorite(it.id) } },
                        isFavorite = currentTrack?.let { favoriteIds.contains(it.id) } ?: false,
                        isShuffle = isShuffle,
                        repeatMode = repeatMode,
                        onClose = { vm.hideFullScreenPlayer() },
                        onQueueClick = {
                            vm.hideFullScreenPlayer()
                            vm.navigateTo(Screen.Queue)
                        }
                    )
                }

                showYouTubePlayer && currentYouTubeVideoId != null -> {
                    YouTubePlayerView(
                        videoId = currentYouTubeVideoId!!,
                        onClose = { vm.hideYouTubePlayer() }
                    )
                }

                selectedArtistId != null && artistDetail != null -> {
                    ArtistScreen(
                        artistDetail = artistDetail!!,
                        favoriteIds = favoriteIds,
                        onBack = { vm.clearArtistDetail() },
                        onPlay = { track -> vm.playTrackFromSearch(track) },
                        onAddToQueue = { track -> vm.addToQueue(track.id) },
                        onToggleFavorite = { track -> vm.toggleFavorite(track.id) },
                        onAddToPlaylist = { track -> vm.showAddToPlaylistDialog(track.id) }
                    )
                }

                selectedAlbumId != null && albumDetail != null -> {
                    AlbumScreen(
                        albumDetail = albumDetail!!,
                        favoriteIds = favoriteIds,
                        onBack = { vm.clearAlbumDetail() },
                        onPlay = { track -> vm.playTrackFromSearch(track) },
                        onAddToQueue = { track -> vm.addToQueue(track.id) },
                        onToggleFavorite = { track -> vm.toggleFavorite(track.id) },
                        onAddToPlaylist = { track -> vm.showAddToPlaylistDialog(track.id) }
                    )
                }

                else -> when (currentScreen) {
                    Screen.Home -> HomeScreen(
                        searchResults = typedSearchResults,
                        favoriteIds = favoriteIds,
                        recentTracks = recentTracks,
                        allTracks = allTracks,
                        onPlay = { track -> vm.playTrackFromSearch(track) },
                        onAddToQueue = { track -> vm.addToQueue(track.id) },
                        onToggleFavorite = { track -> vm.toggleFavorite(track.id) },
                        onAddToPlaylist = { track -> vm.showAddToPlaylistDialog(track.id) },
                        onPlayShuffle = { vm.toggleShuffle() },
                        onArtistClick = { artist -> vm.loadArtistDetail(artist) },
                        onAlbumClick = { album -> vm.loadAlbumDetail(album) },
                        onSettingsClick = { vm.navigateTo(Screen.Settings) }
                    )

                    Screen.Search -> SearchScreen(
                        searchResults = typedSearchResults,
                        searchSuggestions = searchSuggestions,
                        isSearching = isSearching,
                        query = searchQuery,
                        onQueryChange = { vm.updateSearchQuery(it) },
                        onSearch = { vm.search(it) },
                        onPlay = { track -> vm.playTrackFromSearch(track) },
                        onAddToQueue = { track -> vm.addToQueue(track.id) },
                        onToggleFavorite = { track -> vm.toggleFavorite(track.id) },
                        onAddToPlaylist = { track -> vm.showAddToPlaylistDialog(track.id) },
                        onArtistClick = { artist -> vm.loadArtistDetail(artist) },
                        onAlbumClick = { album -> vm.loadAlbumDetail(album) },
                        onCategoryClick = { category ->
                            vm.updateSearchQuery(category)
                            vm.search(category)
                        }
                    )

                    Screen.Library -> LibraryScreen(
                        tracks = libraryTracks,
                        playlists = playlists,
                        favoriteIds = favoriteIds,
                        recentTracks = recentTracks,
                        artists = typedSearchResults.artists,
                        albums = typedSearchResults.albums,
                        onPlay = { track -> vm.playTrackFromSearch(track) },
                        onAddToQueue = { track -> vm.addToQueue(track.id) },
                        onToggleFavorite = { track -> vm.toggleFavorite(track.id) },
                        onAddToPlaylist = { track -> vm.showAddToPlaylistDialog(track.id) },
                        onSelectPlaylist = { vm.selectPlaylist(it.id) },
                        onPlayPlaylist = { vm.playPlaylist(it.id) },
                        onArtistClick = { artist -> vm.loadArtistDetail(artist) },
                        onAlbumClick = { album -> vm.loadAlbumDetail(album) }
                    )

                    Screen.Offline -> OfflineScreen(
                        localTracks = localTracks,
                        favoriteIds = favoriteIds,
                        onPlay = { track -> vm.playTrack(track) },
                        onAddToQueue = { track -> vm.addToQueue(track.id) },
                        onToggleFavorite = { track -> vm.toggleFavorite(track.id) },
                        onImportFiles = { uris -> vm.importAudioFiles(uris) },
                        onDeleteTrack = { track -> vm.deleteTrack(track.id) }
                    )

                    Screen.Queue -> QueueScreen(
                        queueTracks = queueTracks,
                        favoriteIds = favoriteIds,
                        currentTrackId = playingTrackId,
                        onPlay = { track -> vm.playTrack(track) },
                        onAddToQueue = { track -> vm.addToQueue(track.id) },
                        onRemoveFromQueue = { track -> vm.removeFromQueue(track.id) },
                        onMoveUp = { track -> vm.moveQueueTrack(track.id, -1) },
                        onMoveDown = { track -> vm.moveQueueTrack(track.id, 1) },
                        onClearQueue = { vm.clearQueue() },
                        onToggleFavorite = { track -> vm.toggleFavorite(track.id) },
                        onAddToPlaylist = { track -> vm.showAddToPlaylistDialog(track.id) }
                    )

                    Screen.Settings -> SettingsScreen(
                        region = region,
                        safeSearch = safeSearch,
                        onRegionChange = { vm.updateRegion(it) },
                        onSafeSearchChange = { vm.updateSafeSearch(it) },
                        onSave = { vm.saveSettings() },
                        isAuthenticated = isAuthenticated,
                        onInitOAuth = { /* TODO: OAuth flow via WebView */ },
                        onBack = { vm.navigateTo(Screen.Home) }
                    )
                }
            }
        }
    }

    if (showPlaylistDialog && dialogTrackId != null) {
        AlertDialog(
            onDismissRequest = { vm.hidePlaylistDialog() },
            title = { Text("Add to playlist") },
            text = {
                Column {
                    playlists.forEach { playlist ->
                        TextButton(
                            onClick = { vm.addTrackToPlaylist(playlist.id, dialogTrackId!!) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(playlist.name)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { vm.hidePlaylistDialog() }) {
                    Text("Done")
                }
            }
        )
    }
}
