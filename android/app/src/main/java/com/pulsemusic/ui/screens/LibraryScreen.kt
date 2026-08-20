package com.pulsemusic.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.pulsemusic.bridge.SearchResult
import com.pulsemusic.data.model.Playlist
import com.pulsemusic.data.model.Track
import com.pulsemusic.ui.components.TrackItem
import com.pulsemusic.ui.theme.artColors

enum class LibraryTab(val label: String, val icon: ImageVector) {
    PLAYLISTS("Playlists", Icons.Default.QueueMusic),
    ARTISTS("Artists", Icons.Default.Person),
    ALBUMS("Albums", Icons.Default.Album),
    SONGS("Songs", Icons.Default.MusicNote)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    tracks: List<Track>,
    playlists: List<Playlist>,
    favoriteIds: List<String>,
    recentTracks: List<Track>,
    artists: List<SearchResult>,
    albums: List<SearchResult>,
    onPlay: (Track) -> Unit,
    onAddToQueue: (Track) -> Unit,
    onToggleFavorite: (Track) -> Unit,
    onAddToPlaylist: (Track) -> Unit,
    onSelectPlaylist: (Playlist) -> Unit,
    onPlayPlaylist: (Playlist) -> Unit,
    onArtistClick: (SearchResult) -> Unit = {},
    onAlbumClick: (SearchResult) -> Unit = {}
) {
    var selectedTab by remember { mutableStateOf(LibraryTab.PLAYLISTS) }

    Column(modifier = Modifier.fillMaxSize()) {
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Your Library",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )

            IconButton(onClick = { }) {
                Icon(
                    Icons.Default.Search,
                    contentDescription = "Search in library"
                )
            }

            IconButton(onClick = { }) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Create playlist"
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(LibraryTab.entries) { tab ->
                FilterChip(
                    selected = selectedTab == tab,
                    onClick = { selectedTab = tab },
                    label = { Text(tab.label) },
                    leadingIcon = if (selectedTab == tab) {
                        { Icon(tab.icon, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    } else null
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        when (selectedTab) {
            LibraryTab.PLAYLISTS -> {
                if (playlists.isEmpty()) {
                    EmptyState("No playlists yet. Create one to get started.")
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 80.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(playlists, key = { it.id }) { playlist ->
                            PlaylistListItem(
                                playlist = playlist,
                                onClick = { onSelectPlaylist(playlist) },
                                onPlay = { onPlayPlaylist(playlist) }
                            )
                        }
                    }
                }
            }

            LibraryTab.ARTISTS -> {
                if (artists.isEmpty()) {
                    EmptyState("Artists you follow will appear here.")
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 80.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(artists, key = { it.id ?: it.title ?: "" }) { artist ->
                            ArtistListItem(
                                artist = artist,
                                onClick = { onArtistClick(artist) }
                            )
                        }
                    }
                }
            }

            LibraryTab.ALBUMS -> {
                if (albums.isEmpty()) {
                    EmptyState("Albums you save will appear here.")
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 80.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(albums, key = { it.id ?: it.title ?: "" }) { album ->
                            AlbumListItem(
                                album = album,
                                onClick = { onAlbumClick(album) }
                            )
                        }
                    }
                }
            }

            LibraryTab.SONGS -> {
                if (tracks.isEmpty()) {
                    EmptyState("Songs you like will appear here.")
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 80.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(tracks, key = { it.id }) { track ->
                            TrackItem(
                                track = track,
                                isFavorite = favoriteIds.contains(track.id),
                                onPlay = { onPlay(track) },
                                onAddToQueue = { onAddToQueue(track) },
                                onToggleFavorite = { onToggleFavorite(track) },
                                onAddToPlaylist = { onAddToPlaylist(track) },
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyState(message: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun PlaylistListItem(
    playlist: Playlist,
    onClick: () -> Unit,
    onPlay: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(4.dp)),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = artColors[Math.abs(playlist.name.hashCode()) % artColors.size]
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.QueueMusic,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = playlist.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "Playlist",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        IconButton(onClick = onPlay) {
            Icon(
                Icons.Default.PlayArrow,
                contentDescription = "Play",
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun ArtistListItem(
    artist: SearchResult,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (!artist.thumbnail.isNullOrEmpty()) {
                AsyncImage(
                    model = artist.thumbnail,
                    contentDescription = artist.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = artColors[Math.abs((artist.title ?: "").hashCode()) % artColors.size]
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = artist.title ?: "Unknown",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "Artist",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AlbumListItem(
    album: SearchResult,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(4.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (!album.thumbnail.isNullOrEmpty()) {
                AsyncImage(
                    model = album.thumbnail,
                    contentDescription = album.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = artColors[Math.abs((album.title ?: "").hashCode()) % artColors.size]
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Album,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = album.title ?: "Unknown",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = album.artist ?: "Album",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
