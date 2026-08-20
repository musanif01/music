package com.pulsemusic

import android.Manifest
import android.content.ComponentName
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.media3.common.util.NotificationUtil
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.pulsemusic.bridge.SearchResult
import com.pulsemusic.bridge.YTMusicBridge
import com.pulsemusic.data.model.*
import com.pulsemusic.data.repository.MusicRepository
import com.pulsemusic.service.MusicService
import com.pulsemusic.ui.navigation.Screen
import com.pulsemusic.ui.screens.*
import com.pulsemusic.ui.components.FullScreenPlayer
import com.pulsemusic.ui.theme.PulseMusicTheme
import kotlinx.coroutines.*
import java.util.UUID

class MainActivity : ComponentActivity() {

    private lateinit var repository: MusicRepository
    private lateinit var ytMusic: YTMusicBridge
    private lateinit var mediaControllerFuture: ListenableFuture<MediaController>
    private var mediaController: MediaController? = null

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var searchJob: Job? = null

    private val requestNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* granted or not */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val app = application as PulseMusicApp
        repository = app.repository
        ytMusic = YTMusicBridge(this)

        val sessionToken = SessionToken(this, ComponentName(this, MusicService::class.java))
        mediaControllerFuture = MediaController.Builder(this, sessionToken).buildAsync()
        mediaControllerFuture.addListener({
            mediaController = mediaControllerFuture.get()
        }, MoreExecutors.directExecutor())

        requestNotificationPermissionIfNeeded()

        setContent {
            PulseMusicTheme {
                PulseMusicMain(
                    repository = repository,
                    ytMusic = ytMusic,
                    mediaController = mediaController,
                    onLaunchService = { startMusicService() }
                )
            }
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
        scope.cancel()
        if (::mediaControllerFuture.isInitialized) {
            MediaController.releaseFuture(mediaControllerFuture)
        }
        super.onDestroy()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PulseMusicMain(
    repository: MusicRepository,
    ytMusic: YTMusicBridge,
    mediaController: MediaController?,
    onLaunchService: () -> Unit
) {
    val scope = rememberCoroutineScope()

    var currentScreen by remember { mutableStateOf<Screen>(Screen.Home) }

    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<SearchResult>>(emptyList()) }
    var typedSearchResults by remember { mutableStateOf<TypedSearchResults>(TypedSearchResults()) }
    var isSearching by remember { mutableStateOf(false) }
    var searchSuggestions by remember { mutableStateOf<List<String>>(emptyList()) }

    val allTracks by repository.getAllTracks().collectAsState(initial = emptyList())
    val favoriteIdsState = remember { mutableStateOf<List<String>>(emptyList()) }
    val playlists by repository.getAllPlaylists().collectAsState(initial = emptyList())
    val queueTracks by repository.getQueue().collectAsState(initial = emptyList())
    var selectedPlaylistId by remember { mutableStateOf<String?>(null) }
    val playlistTracks = selectedPlaylistId?.let { id ->
        repository.getPlaylistTracks(id).collectAsState(initial = emptyList()).value
    } ?: emptyList()

    var libraryFilter by remember { mutableStateOf(LibraryFilter.ALL) }
    var region by remember { mutableStateOf("US") }
    var safeSearch by remember { mutableStateOf("moderate") }
    var isAuthenticated by remember { mutableStateOf(false) }

    var showPlaylistDialog by remember { mutableStateOf(false) }
    var dialogTrackId by remember { mutableStateOf<String?>(null) }
    var playingTrackId by remember { mutableStateOf<String?>(null) }
    var isPlaying by remember { mutableStateOf(false) }

    var showFullScreenPlayer by remember { mutableStateOf(false) }
    var showYouTubePlayer by remember { mutableStateOf(false) }
    var currentYouTubeVideoId by remember { mutableStateOf<String?>(null) }

    var selectedArtistId by remember { mutableStateOf<String?>(null) }
    var selectedAlbumId by remember { mutableStateOf<String?>(null) }
    var artistDetail by remember { mutableStateOf<ArtistDetail?>(null) }
    var albumDetail by remember { mutableStateOf<AlbumDetail?>(null) }
    var isLoadingDetail by remember { mutableStateOf(false) }

    val libraryTracks = when (libraryFilter) {
        LibraryFilter.ALL -> allTracks
        LibraryFilter.FAVORITES -> allTracks.filter { favoriteIdsState.value.contains(it.id) }
        LibraryFilter.RECENT -> repository.getRecentTracks()
            .collectAsState(initial = emptyList()).value
    }

    val localTracks = allTracks.filter { it.source == "local" }

    val recentTracks = repository.getRecentTracks()
        .collectAsState(initial = emptyList()).value

    LaunchedEffect(Unit) {
        favoriteIdsState.value = repository.getFavoriteIds()
    }

    fun playTrackFromQueue(track: Track) {
        playingTrackId = track.id
        if (track.source == "local" && track.fileUri != null) {
            onLaunchService()
            mediaController?.let {
                val mediaItem = androidx.media3.common.MediaItem.Builder()
                    .setMediaId(track.id)
                    .setUri(track.fileUri)
                    .build()
                it.setMediaItem(mediaItem)
                it.prepare()
                it.play()
            }
        } else if (track.source == "youtube" && track.youtubeId != null) {
            currentYouTubeVideoId = track.youtubeId
            showYouTubePlayer = true
        }
    }

    fun playNextTrack() {
        val currentId = playingTrackId ?: return
        val idx = queueTracks.indexOfFirst { it.id == currentId }
        val next = if (idx >= 0 && idx < queueTracks.lastIndex) queueTracks[idx + 1] else queueTracks.firstOrNull()
        if (next != null) playTrackFromQueue(next)
    }

    fun playPreviousTrack() {
        val currentId = playingTrackId ?: return
        val idx = queueTracks.indexOfFirst { it.id == currentId }
        val prev = if (idx > 0) queueTracks[idx - 1] else queueTracks.lastOrNull()
        if (prev != null) playTrackFromQueue(prev)
    }

    val currentTrack = playingTrackId?.let { id -> allTracks.find { it.id == id } }

    Scaffold(
        bottomBar = {
            Surface(shadowElevation = 8.dp) {
                Column {
                    if (currentTrack != null && !showFullScreenPlayer) {
                        com.pulsemusic.ui.components.PlayerBar(
                            track = currentTrack,
                            isPlaying = isPlaying,
                            onPlayPause = {
                                if (currentTrack.source == "youtube") {
                                    showYouTubePlayer = !showYouTubePlayer
                                } else {
                                    mediaController?.let {
                                        if (it.isPlaying) it.pause() else it.play()
                                    }
                                }
                            },
                            onNext = { playNextTrack() },
                            onPrevious = { playPreviousTrack() },
                            onClick = { showFullScreenPlayer = true }
                        )
                    }

                    NavigationBar {
                        Screen.bottomNavItems.forEach { screen ->
                            NavigationBarItem(
                                selected = currentScreen == screen,
                                onClick = { currentScreen = screen },
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
                        progress = 0f,
                        onPlayPause = {
                            if (currentTrack.source == "youtube") {
                                showYouTubePlayer = !showYouTubePlayer
                            } else {
                                mediaController?.let {
                                    if (it.isPlaying) it.pause() else it.play()
                                }
                            }
                        },
                        onNext = { playNextTrack() },
                        onPrevious = { playPreviousTrack() },
                        onShuffle = { },
                        onRepeat = { },
                        onFavorite = {
                            scope.launch {
                                repository.toggleFavorite(currentTrack.id)
                                favoriteIdsState.value = repository.getFavoriteIds()
                            }
                        },
                        isFavorite = favoriteIdsState.value.contains(currentTrack.id),
                        onClose = { showFullScreenPlayer = false },
                        onQueueClick = {
                            showFullScreenPlayer = false
                            currentScreen = Screen.Queue
                        }
                    )
                }

                showYouTubePlayer && currentYouTubeVideoId != null -> {
                    com.pulsemusic.ui.components.YouTubePlayerView(
                        videoId = currentYouTubeVideoId!!,
                        onClose = {
                            showYouTubePlayer = false
                            currentYouTubeVideoId = null
                        }
                    )
                }

                selectedArtistId != null && artistDetail != null -> {
                    ArtistScreen(
                        artistDetail = artistDetail!!,
                        favoriteIds = favoriteIdsState.value,
                        onBack = {
                            selectedArtistId = null
                            artistDetail = null
                        },
                        onPlay = { track ->
                            playingTrackId = track.id
                            scope.launch { repository.recordPlay(track.id) }
                            playTrackFromQueue(track)
                        },
                        onAddToQueue = { track ->
                            scope.launch { repository.addToQueue(track.id) }
                        },
                        onToggleFavorite = { track ->
                            scope.launch {
                                repository.toggleFavorite(track.id)
                                favoriteIdsState.value = repository.getFavoriteIds()
                            }
                        },
                        onAddToPlaylist = { track ->
                            dialogTrackId = track.id
                            showPlaylistDialog = true
                        }
                    )
                }

                selectedAlbumId != null && albumDetail != null -> {
                    AlbumScreen(
                        albumDetail = albumDetail!!,
                        favoriteIds = favoriteIdsState.value,
                        onBack = {
                            selectedAlbumId = null
                            albumDetail = null
                        },
                        onPlay = { track ->
                            playingTrackId = track.id
                            scope.launch { repository.recordPlay(track.id) }
                            playTrackFromQueue(track)
                        },
                        onAddToQueue = { track ->
                            scope.launch { repository.addToQueue(track.id) }
                        },
                        onToggleFavorite = { track ->
                            scope.launch {
                                repository.toggleFavorite(track.id)
                                favoriteIdsState.value = repository.getFavoriteIds()
                            }
                        },
                        onAddToPlaylist = { track ->
                            dialogTrackId = track.id
                            showPlaylistDialog = true
                        }
                    )
                }

                else -> when (currentScreen) {
                    Screen.Home -> HomeScreen(
                        searchResults = typedSearchResults,
                        favoriteIds = favoriteIdsState.value,
                        recentTracks = recentTracks,
                        allTracks = allTracks,
                        onPlay = { track ->
                            playingTrackId = track.id
                            onLaunchService()
                            scope.launch {
                                repository.recordPlay(track.id)
                                repository.addToQueue(track.id)
                                if (track.source == "local" && track.fileUri != null) {
                                    mediaController?.let {
                                        val mediaItem = androidx.media3.common.MediaItem.Builder()
                                            .setMediaId(track.id)
                                            .setUri(track.fileUri)
                                            .build()
                                        it.setMediaItem(mediaItem)
                                        it.prepare()
                                        it.play()
                                    }
                                } else if (track.source == "youtube" && track.youtubeId != null) {
                                    currentYouTubeVideoId = track.youtubeId
                                    showYouTubePlayer = true
                                }
                            }
                        },
                        onAddToQueue = { track ->
                            scope.launch { repository.addToQueue(track.id) }
                        },
                        onToggleFavorite = { track ->
                            scope.launch {
                                repository.toggleFavorite(track.id)
                                favoriteIdsState.value = repository.getFavoriteIds()
                            }
                        },
                        onAddToPlaylist = { track ->
                            dialogTrackId = track.id
                            showPlaylistDialog = true
                        },
                        onPlayShuffle = {
                            if (allTracks.isNotEmpty()) {
                                val shuffled = allTracks.shuffled()
                                scope.launch {
                                    repository.clearQueue()
                                    shuffled.forEach { repository.addToQueue(it.id) }
                                    if (shuffled.isNotEmpty()) {
                                        playingTrackId = shuffled.first().id
                                        playTrackFromQueue(shuffled.first())
                                    }
                                }
                            }
                        },
                        onArtistClick = { artist ->
                            selectedArtistId = artist.browseId
                            isLoadingDetail = true
                            scope.launch {
                                try {
                                    val json = ytMusic.getArtist(artist.browseId ?: "")
                                    if (json != null) {
                                        artistDetail = ytMusic.parseArtistDetail(json)
                                    }
                                } catch (e: Exception) {
                                    android.util.Log.e("MainActivity", "Get artist failed", e)
                                } finally {
                                    isLoadingDetail = false
                                }
                            }
                        },
                        onAlbumClick = { album ->
                            selectedAlbumId = album.browseId
                            isLoadingDetail = true
                            scope.launch {
                                try {
                                    val json = ytMusic.getAlbum(album.browseId ?: "")
                                    if (json != null) {
                                        albumDetail = ytMusic.parseAlbumDetail(json)
                                    }
                                } catch (e: Exception) {
                                    android.util.Log.e("MainActivity", "Get album failed", e)
                                } finally {
                                    isLoadingDetail = false
                                }
                            }
                        }
                    )

                    Screen.Search -> SearchScreen(
                        searchResults = typedSearchResults,
                        searchSuggestions = searchSuggestions,
                        isSearching = isSearching,
                        query = searchQuery,
                        onQueryChange = { searchQuery = it },
                        onSearch = { query ->
                            if (query.isBlank()) return@SearchScreen
                            isSearching = true
                            searchJob?.cancel()
                            searchJob = scope.launch {
                                try {
                                    val results = ytMusic.search(query)
                                    val songs = results.filter { it.resultType == "song" || it.resultType == "video" }
                                        .map { ytMusic.searchResultToTrack(it) }
                                    val albums = results.filter { it.resultType == "album" }
                                    val artists = results.filter { it.resultType == "artist" }
                                    repository.upsertTracks(songs)
                                    typedSearchResults = TypedSearchResults(
                                        songs = songs,
                                        albums = albums,
                                        artists = artists
                                    )
                                } catch (e: Exception) {
                                    android.util.Log.e("MainActivity", "Search failed", e)
                                } finally {
                                    isSearching = false
                                }
                            }
                        },
                        onPlay = { track ->
                            playingTrackId = track.id
                            onLaunchService()
                            scope.launch {
                                repository.recordPlay(track.id)
                                repository.addToQueue(track.id)
                                if (track.source == "local" && track.fileUri != null) {
                                    mediaController?.let {
                                        val mediaItem = androidx.media3.common.MediaItem.Builder()
                                            .setMediaId(track.id)
                                            .setUri(track.fileUri)
                                            .build()
                                        it.setMediaItem(mediaItem)
                                        it.prepare()
                                        it.play()
                                    }
                                } else if (track.source == "youtube" && track.youtubeId != null) {
                                    currentYouTubeVideoId = track.youtubeId
                                    showYouTubePlayer = true
                                }
                            }
                        },
                        onAddToQueue = { track ->
                            scope.launch { repository.addToQueue(track.id) }
                        },
                        onToggleFavorite = { track ->
                            scope.launch {
                                repository.toggleFavorite(track.id)
                                favoriteIdsState.value = repository.getFavoriteIds()
                            }
                        },
                        onAddToPlaylist = { track ->
                            dialogTrackId = track.id
                            showPlaylistDialog = true
                        },
                        onArtistClick = { artist ->
                            selectedArtistId = artist.browseId
                            isLoadingDetail = true
                            scope.launch {
                                try {
                                    val json = ytMusic.getArtist(artist.browseId ?: "")
                                    if (json != null) {
                                        artistDetail = ytMusic.parseArtistDetail(json)
                                    }
                                } catch (e: Exception) {
                                    android.util.Log.e("MainActivity", "Get artist failed", e)
                                } finally {
                                    isLoadingDetail = false
                                }
                            }
                        },
                        onAlbumClick = { album ->
                            selectedAlbumId = album.browseId
                            isLoadingDetail = true
                            scope.launch {
                                try {
                                    val json = ytMusic.getAlbum(album.browseId ?: "")
                                    if (json != null) {
                                        albumDetail = ytMusic.parseAlbumDetail(json)
                                    }
                                } catch (e: Exception) {
                                    android.util.Log.e("MainActivity", "Get album failed", e)
                                } finally {
                                    isLoadingDetail = false
                                }
                            }
                        },
                        onCategoryClick = { category ->
                            searchQuery = category
                            isSearching = true
                            searchJob?.cancel()
                            searchJob = scope.launch {
                                try {
                                    val results = ytMusic.search(category)
                                    val songs = results.filter { it.resultType == "song" || it.resultType == "video" }
                                        .map { ytMusic.searchResultToTrack(it) }
                                    val albums = results.filter { it.resultType == "album" }
                                    val artists = results.filter { it.resultType == "artist" }
                                    repository.upsertTracks(songs)
                                    typedSearchResults = TypedSearchResults(
                                        songs = songs,
                                        albums = albums,
                                        artists = artists
                                    )
                                } catch (e: Exception) {
                                    android.util.Log.e("MainActivity", "Search failed", e)
                                } finally {
                                    isSearching = false
                                }
                            }
                        }
                    )

                    Screen.Library -> LibraryScreen(
                        tracks = libraryTracks,
                        playlists = playlists,
                        favoriteIds = favoriteIdsState.value,
                        recentTracks = recentTracks,
                        artists = typedSearchResults.artists,
                        albums = typedSearchResults.albums,
                        onPlay = { track ->
                            playingTrackId = track.id
                            scope.launch {
                                repository.recordPlay(track.id)
                                repository.addToQueue(track.id)
                            }
                            playTrackFromQueue(track)
                        },
                        onAddToQueue = { track ->
                            scope.launch { repository.addToQueue(track.id) }
                        },
                        onToggleFavorite = { track ->
                            scope.launch {
                                repository.toggleFavorite(track.id)
                                favoriteIdsState.value = repository.getFavoriteIds()
                            }
                        },
                        onAddToPlaylist = { track ->
                            dialogTrackId = track.id
                            showPlaylistDialog = true
                        },
                        onSelectPlaylist = { selectedPlaylistId = it.id },
                        onPlayPlaylist = { playlist ->
                            if (playlistTracks.isNotEmpty()) {
                                scope.launch {
                                    repository.clearQueue()
                                    playlistTracks.forEach { repository.addToQueue(it.id) }
                                    playingTrackId = playlistTracks.first().id
                                    playTrackFromQueue(playlistTracks.first())
                                }
                            }
                        },
                        onArtistClick = { artist ->
                            selectedArtistId = artist.browseId
                            isLoadingDetail = true
                            scope.launch {
                                try {
                                    val json = ytMusic.getArtist(artist.browseId ?: "")
                                    if (json != null) {
                                        artistDetail = ytMusic.parseArtistDetail(json)
                                    }
                                } catch (e: Exception) {
                                    android.util.Log.e("MainActivity", "Get artist failed", e)
                                } finally {
                                    isLoadingDetail = false
                                }
                            }
                        },
                        onAlbumClick = { album ->
                            selectedAlbumId = album.browseId
                            isLoadingDetail = true
                            scope.launch {
                                try {
                                    val json = ytMusic.getAlbum(album.browseId ?: "")
                                    if (json != null) {
                                        albumDetail = ytMusic.parseAlbumDetail(json)
                                    }
                                } catch (e: Exception) {
                                    android.util.Log.e("MainActivity", "Get album failed", e)
                                } finally {
                                    isLoadingDetail = false
                                }
                            }
                        }
                    )

                    Screen.Queue -> QueueScreen(
                        queueTracks = queueTracks,
                        favoriteIds = favoriteIdsState.value,
                        currentTrackId = playingTrackId,
                        onPlay = { track ->
                            playingTrackId = track.id
                            scope.launch { repository.recordPlay(track.id) }
                            playTrackFromQueue(track)
                        },
                        onAddToQueue = {},
                        onRemoveFromQueue = { track ->
                            scope.launch { repository.removeFromQueue(track.id) }
                        },
                        onMoveUp = { track ->
                            scope.launch { repository.moveQueueTrack(track.id, -1) }
                        },
                        onMoveDown = { track ->
                            scope.launch { repository.moveQueueTrack(track.id, 1) }
                        },
                        onClearQueue = { scope.launch { repository.clearQueue() } },
                        onToggleFavorite = { track ->
                            scope.launch {
                                repository.toggleFavorite(track.id)
                                favoriteIdsState.value = repository.getFavoriteIds()
                            }
                        },
                        onAddToPlaylist = { track ->
                            dialogTrackId = track.id
                            showPlaylistDialog = true
                        }
                    )

                    Screen.Settings -> SettingsScreen(
                        region = region,
                        safeSearch = safeSearch,
                        onRegionChange = { region = it },
                        onSafeSearchChange = { safeSearch = it },
                        onSave = { /* persist settings */ },
                        isAuthenticated = isAuthenticated,
                        onInitOAuth = { /* TODO: OAuth flow via WebView */ }
                    )
                }
            }
        }
    }

    if (showPlaylistDialog && dialogTrackId != null) {
        AlertDialog(
            onDismissRequest = {
                showPlaylistDialog = false
                dialogTrackId = null
            },
            title = { Text("Add to playlist") },
            text = {
                Column {
                    playlists.forEach { playlist ->
                        TextButton(
                            onClick = {
                                scope.launch {
                                    repository.addTrackToPlaylist(playlist.id, dialogTrackId!!)
                                    showPlaylistDialog = false
                                    dialogTrackId = null
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(playlist.name)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showPlaylistDialog = false
                    dialogTrackId = null
                }) {
                    Text("Done")
                }
            }
        )
    }
}

private suspend fun importAudioFiles(uris: List<Uri>, repository: MusicRepository) {
    for (uri in uris) {
        val track = Track(
            id = "local-${UUID.randomUUID()}",
            source = "local",
            title = uri.lastPathSegment?.substringAfterLast('/')?.substringBeforeLast('.') ?: "Unknown",
            artist = "Device file",
            fileUri = uri.toString(),
            thumbnail = ""
        )
        repository.upsertTrack(track)
    }
}
