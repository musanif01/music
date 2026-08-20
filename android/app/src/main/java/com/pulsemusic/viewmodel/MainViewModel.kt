package com.pulsemusic.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.session.MediaController
import com.pulsemusic.bridge.SearchResult
import com.pulsemusic.bridge.YTMusicBridge
import com.pulsemusic.data.model.*
import com.pulsemusic.data.repository.MusicRepository
import com.pulsemusic.ui.navigation.Screen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

enum class LibraryFilter { ALL, FAVORITES, RECENT }
enum class RepeatMode { OFF, ONE, ALL }

class MainViewModel(
    application: Application,
    private val repository: MusicRepository,
    private val ytMusic: YTMusicBridge
) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("pulse_settings", Context.MODE_PRIVATE)

    private var mediaController: MediaController? = null
    private var searchJob: Job? = null
    private var progressJob: Job? = null

    private val _currentScreen = MutableStateFlow<Screen>(Screen.Home)
    val currentScreen: StateFlow<Screen> = _currentScreen

    private val _showFullScreenPlayer = MutableStateFlow(false)
    val showFullScreenPlayer: StateFlow<Boolean> = _showFullScreenPlayer

    private val _showYouTubePlayer = MutableStateFlow(false)
    val showYouTubePlayer: StateFlow<Boolean> = _showYouTubePlayer

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _typedSearchResults = MutableStateFlow(TypedSearchResults())
    val typedSearchResults: StateFlow<TypedSearchResults> = _typedSearchResults

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching

    private val _searchSuggestions = MutableStateFlow<List<String>>(emptyList())
    val searchSuggestions: StateFlow<List<String>> = _searchSuggestions

    private val _playingTrackId = MutableStateFlow<String?>(null)
    val playingTrackId: StateFlow<String?> = _playingTrackId

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying

    private val _currentYouTubeVideoId = MutableStateFlow<String?>(null)
    val currentYouTubeVideoId: StateFlow<String?> = _currentYouTubeVideoId

    private val _progress = MutableStateFlow(0f)
    val progress: StateFlow<Float> = _progress

    private val _isShuffle = MutableStateFlow(false)
    val isShuffle: StateFlow<Boolean> = _isShuffle

    private val _repeatMode = MutableStateFlow(RepeatMode.OFF)
    val repeatMode: StateFlow<RepeatMode> = _repeatMode

    private val _libraryFilter = MutableStateFlow(LibraryFilter.ALL)
    val libraryFilter: StateFlow<LibraryFilter> = _libraryFilter

    private val _selectedPlaylistId = MutableStateFlow<String?>(null)
    val selectedPlaylistId: StateFlow<String?> = _selectedPlaylistId

    private val _selectedArtistId = MutableStateFlow<String?>(null)
    val selectedArtistId: StateFlow<String?> = _selectedArtistId

    private val _selectedAlbumId = MutableStateFlow<String?>(null)
    val selectedAlbumId: StateFlow<String?> = _selectedAlbumId

    private val _artistDetail = MutableStateFlow<ArtistDetail?>(null)
    val artistDetail: StateFlow<ArtistDetail?> = _artistDetail

    private val _albumDetail = MutableStateFlow<AlbumDetail?>(null)
    val albumDetail: StateFlow<AlbumDetail?> = _albumDetail

    private val _isLoadingDetail = MutableStateFlow(false)
    val isLoadingDetail: StateFlow<Boolean> = _isLoadingDetail

    private val _region = MutableStateFlow(prefs.getString("region", "US") ?: "US")
    val region: StateFlow<String> = _region

    private val _safeSearch = MutableStateFlow(prefs.getString("safe_search", "moderate") ?: "moderate")
    val safeSearch: StateFlow<String> = _safeSearch

    private val _isAuthenticated = MutableStateFlow(false)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated

    private val _showPlaylistDialog = MutableStateFlow(false)
    val showPlaylistDialog: StateFlow<Boolean> = _showPlaylistDialog

    private val _dialogTrackId = MutableStateFlow<String?>(null)
    val dialogTrackId: StateFlow<String?> = _dialogTrackId

    private val _favoriteIds = MutableStateFlow<List<String>>(emptyList())
    val favoriteIds: StateFlow<List<String>> = _favoriteIds

    val allTracks: StateFlow<List<Track>> = repository.getAllTracks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val playlists: StateFlow<List<Playlist>> = repository.getAllPlaylists()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val queueTracks: StateFlow<List<Track>> = repository.getQueue()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentTracks: StateFlow<List<Track>> = repository.getRecentTracks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val playlistTracks: StateFlow<List<Track>> = _selectedPlaylistId.flatMapLatest { id ->
        if (id != null) repository.getPlaylistTracks(id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val libraryTracks: StateFlow<List<Track>> = combine(
        allTracks, _libraryFilter, _favoriteIds
    ) { tracks, filter, favIds ->
        when (filter) {
            LibraryFilter.ALL -> tracks
            LibraryFilter.FAVORITES -> tracks.filter { favIds.contains(it.id) }
            LibraryFilter.RECENT -> tracks // Will be overridden by recentTracks
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val localTracks: StateFlow<List<Track>> = allTracks.map { tracks ->
        tracks.filter { it.source == "local" }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentTrack: StateFlow<Track?> = combine(allTracks, _playingTrackId) { tracks, id ->
        id?.let { trackId -> tracks.find { it.id == trackId } }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    init {
        viewModelScope.launch {
            _favoriteIds.value = repository.getFavoriteIds()
        }
    }

    fun setMediaController(controller: MediaController) {
        mediaController = controller
        controller.addListener(object : androidx.media3.common.Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _isPlaying.value = isPlaying
                if (isPlaying) startProgressTracking() else stopProgressTracking()
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    androidx.media3.common.Player.STATE_ENDED -> onTrackEnded()
                }
            }
        })
    }

    fun navigateTo(screen: Screen) {
        _currentScreen.value = screen
    }

    fun showFullScreenPlayer() {
        _showFullScreenPlayer.value = true
    }

    fun hideFullScreenPlayer() {
        _showFullScreenPlayer.value = false
    }

    fun showYouTubePlayer() {
        _showYouTubePlayer.value = true
    }

    fun hideYouTubePlayer() {
        _showYouTubePlayer.value = false
        _currentYouTubeVideoId.value = null
    }

    fun search(query: String) {
        if (query.isBlank()) return
        _isSearching.value = true
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            try {
                val results = ytMusic.search(query)
                val songs = results.filter { it.resultType == "song" || it.resultType == "video" }
                    .map { ytMusic.searchResultToTrack(it) }
                val albums = results.filter { it.resultType == "album" }
                val artists = results.filter { it.resultType == "artist" }
                repository.upsertTracks(songs)
                _typedSearchResults.value = TypedSearchResults(
                    songs = songs, albums = albums, artists = artists
                )
            } catch (e: Exception) {
                android.util.Log.e("MainViewModel", "Search failed", e)
            } finally {
                _isSearching.value = false
            }
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun playTrack(track: Track) {
        _playingTrackId.value = track.id
        _isPlaying.value = true
        viewModelScope.launch {
            repository.recordPlay(track.id)
        }
        if (track.source == "local" && track.fileUri != null) {
            mediaController?.let { controller ->
                val mediaItem = MediaItem.Builder()
                    .setMediaId(track.id)
                    .setUri(track.fileUri)
                    .build()
                controller.setMediaItem(mediaItem)
                controller.prepare()
                controller.play()
            }
        } else if (track.source == "youtube" && track.youtubeId != null) {
            _currentYouTubeVideoId.value = track.youtubeId
            _showYouTubePlayer.value = true
        }
    }

    fun playTrackFromSearch(track: Track) {
        _playingTrackId.value = track.id
        _isPlaying.value = true
        viewModelScope.launch {
            repository.recordPlay(track.id)
            repository.addToQueue(track.id)
        }
        if (track.source == "local" && track.fileUri != null) {
            mediaController?.let { controller ->
                val mediaItem = MediaItem.Builder()
                    .setMediaId(track.id)
                    .setUri(track.fileUri)
                    .build()
                controller.setMediaItem(mediaItem)
                controller.prepare()
                controller.play()
            }
        } else if (track.source == "youtube" && track.youtubeId != null) {
            _currentYouTubeVideoId.value = track.youtubeId
            _showYouTubePlayer.value = true
        }
    }

    fun togglePlayPause() {
        mediaController?.let { controller ->
            if (controller.isPlaying) controller.pause() else controller.play()
        }
    }

    fun playNextTrack() {
        val currentId = _playingTrackId.value ?: return
        val tracks = queueTracks.value
        val idx = tracks.indexOfFirst { it.id == currentId }
        val next = if (idx >= 0 && idx < tracks.lastIndex) tracks[idx + 1] else tracks.firstOrNull()
        if (next != null) playTrack(next)
    }

    fun playPreviousTrack() {
        val currentId = _playingTrackId.value ?: return
        val tracks = queueTracks.value
        val idx = tracks.indexOfFirst { it.id == currentId }
        val prev = if (idx > 0) tracks[idx - 1] else tracks.lastOrNull()
        if (prev != null) playTrack(prev)
    }

    fun toggleShuffle() {
        _isShuffle.value = !_isShuffle.value
        if (_isShuffle.value) {
            viewModelScope.launch {
                val tracks = allTracks.value.shuffled()
                repository.clearQueue()
                tracks.forEach { repository.addToQueue(it.id) }
                if (tracks.isNotEmpty()) {
                    _playingTrackId.value = tracks.first().id
                    playTrack(tracks.first())
                }
            }
        }
    }

    fun cycleRepeat() {
        _repeatMode.value = when (_repeatMode.value) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }
    }

    fun toggleFavorite(trackId: String) {
        viewModelScope.launch {
            repository.toggleFavorite(trackId)
            _favoriteIds.value = repository.getFavoriteIds()
        }
    }

    fun addToQueue(trackId: String) {
        viewModelScope.launch { repository.addToQueue(trackId) }
    }

    fun removeFromQueue(trackId: String) {
        viewModelScope.launch { repository.removeFromQueue(trackId) }
    }

    fun moveQueueTrack(trackId: String, direction: Int) {
        viewModelScope.launch { repository.moveQueueTrack(trackId, direction) }
    }

    fun clearQueue() {
        viewModelScope.launch { repository.clearQueue() }
    }

    fun createPlaylist(name: String) {
        viewModelScope.launch { repository.createPlaylist(name) }
    }

    fun selectPlaylist(playlistId: String?) {
        _selectedPlaylistId.value = playlistId
    }

    fun addTrackToPlaylist(playlistId: String, trackId: String) {
        viewModelScope.launch {
            repository.addTrackToPlaylist(playlistId, trackId)
            _showPlaylistDialog.value = false
            _dialogTrackId.value = null
        }
    }

    fun removeTrackFromPlaylist(playlistId: String, trackId: String) {
        viewModelScope.launch { repository.removeTrackFromPlaylist(playlistId, trackId) }
    }

    fun movePlaylistTrack(playlistId: String, trackId: String, direction: Int) {
        viewModelScope.launch { repository.movePlaylistTrack(playlistId, trackId, direction) }
    }

    fun deletePlaylist(playlistId: String) {
        viewModelScope.launch {
            repository.deletePlaylist(playlistId)
            if (_selectedPlaylistId.value == playlistId) {
                _selectedPlaylistId.value = null
            }
        }
    }

    fun showAddToPlaylistDialog(trackId: String) {
        _dialogTrackId.value = trackId
        _showPlaylistDialog.value = true
    }

    fun hidePlaylistDialog() {
        _showPlaylistDialog.value = false
        _dialogTrackId.value = null
    }

    fun playPlaylist(playlistId: String) {
        viewModelScope.launch {
            val tracks = playlistTracks.value
            if (tracks.isNotEmpty()) {
                repository.clearQueue()
                tracks.forEach { repository.addToQueue(it.id) }
                _playingTrackId.value = tracks.first().id
                playTrack(tracks.first())
            }
        }
    }

    fun loadArtistDetail(artist: SearchResult) {
        _selectedArtistId.value = artist.browseId
        _isLoadingDetail.value = true
        viewModelScope.launch {
            try {
                val json = ytMusic.getArtist(artist.browseId ?: "")
                if (json != null) {
                    _artistDetail.value = ytMusic.parseArtistDetail(json)
                }
            } catch (e: Exception) {
                android.util.Log.e("MainViewModel", "Get artist failed", e)
            } finally {
                _isLoadingDetail.value = false
            }
        }
    }

    fun loadAlbumDetail(album: SearchResult) {
        _selectedAlbumId.value = album.browseId
        _isLoadingDetail.value = true
        viewModelScope.launch {
            try {
                val json = ytMusic.getAlbum(album.browseId ?: "")
                if (json != null) {
                    _albumDetail.value = ytMusic.parseAlbumDetail(json)
                }
            } catch (e: Exception) {
                android.util.Log.e("MainViewModel", "Get album failed", e)
            } finally {
                _isLoadingDetail.value = false
            }
        }
    }

    fun clearArtistDetail() {
        _selectedArtistId.value = null
        _artistDetail.value = null
    }

    fun clearAlbumDetail() {
        _selectedAlbumId.value = null
        _albumDetail.value = null
    }

    fun setLibraryFilter(filter: LibraryFilter) {
        _libraryFilter.value = filter
    }

    fun updateRegion(region: String) {
        _region.value = region
    }

    fun updateSafeSearch(safeSearch: String) {
        _safeSearch.value = safeSearch
    }

    fun saveSettings() {
        prefs.edit()
            .putString("region", _region.value)
            .putString("safe_search", _safeSearch.value)
            .apply()
    }

    fun importAudioFiles(uris: List<Uri>) {
        viewModelScope.launch {
            for (uri in uris) {
                val track = Track(
                    id = "local-${UUID.randomUUID()}",
                    source = "local",
                    title = uri.lastPathSegment?.substringAfterLast('/')?.substringBeforeLast('.')
                        ?: "Unknown",
                    artist = "Device file",
                    fileUri = uri.toString(),
                    thumbnail = ""
                )
                repository.upsertTrack(track)
            }
        }
    }

    fun deleteTrack(trackId: String) {
        viewModelScope.launch { repository.deleteTrackById(trackId) }
    }

    fun clearRecent() {
        viewModelScope.launch { repository.clearRecent() }
    }

    private fun onTrackEnded() {
        when (_repeatMode.value) {
            RepeatMode.ONE -> {
                currentTrack.value?.let { playTrack(it) }
            }
            RepeatMode.ALL -> {
                playNextTrack()
            }
            RepeatMode.OFF -> {
                val currentId = _playingTrackId.value ?: return
                val tracks = queueTracks.value
                val idx = tracks.indexOfFirst { it.id == currentId }
                if (idx < tracks.lastIndex) {
                    playNextTrack()
                } else {
                    _isPlaying.value = false
                }
            }
        }
    }

    private fun startProgressTracking() {
        stopProgressTracking()
        progressJob = viewModelScope.launch {
            while (true) {
                mediaController?.let { controller ->
                    val duration = controller.duration.coerceAtLeast(1L)
                    val position = controller.currentPosition
                    _progress.value = (position.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
                }
                delay(500)
            }
        }
    }

    private fun stopProgressTracking() {
        progressJob?.cancel()
        progressJob = null
    }

    override fun onCleared() {
        super.onCleared()
        stopProgressTracking()
    }
}
