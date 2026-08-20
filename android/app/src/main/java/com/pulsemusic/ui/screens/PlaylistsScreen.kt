package com.pulsemusic.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pulsemusic.data.model.Playlist
import com.pulsemusic.data.model.Track
import com.pulsemusic.ui.components.TrackItem

@Composable
fun PlaylistsScreen(
    playlists: List<Playlist>,
    selectedPlaylist: Playlist?,
    playlistTracks: List<Track>,
    favoriteIds: List<String>,
    onCreatePlaylist: (String) -> Unit,
    onSelectPlaylist: (Playlist) -> Unit,
    onDeletePlaylist: (Playlist) -> Unit,
    onPlayPlaylist: () -> Unit,
    onExport: () -> Unit,
    onPlay: (Track) -> Unit,
    onAddToQueue: (Track) -> Unit,
    onToggleFavorite: (Track) -> Unit,
    onRemoveFromPlaylist: (Track) -> Unit,
    onMoveUp: (Track) -> Unit,
    onMoveDown: (Track) -> Unit
) {
    var newPlaylistName by remember { mutableStateOf("") }
    var showCreateDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Playlists",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )

            IconButton(onClick = { showCreateDialog = true }) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Create playlist"
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (selectedPlaylist != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        selectedPlaylist.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "${playlistTracks.size} tracks",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (playlistTracks.isNotEmpty()) {
                    IconButton(onClick = onPlayPlaylist) {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = "Play",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }

        if (playlistTracks.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Select a playlist or create a new one.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(playlistTracks, key = { it.id }) { track ->
                    TrackItem(
                        track = track,
                        isFavorite = favoriteIds.contains(track.id),
                        onPlay = { onPlay(track) },
                        onAddToQueue = { onAddToQueue(track) },
                        onToggleFavorite = { onToggleFavorite(track) },
                        onAddToPlaylist = {},
                        showMoveUp = true,
                        showMoveDown = true,
                        showRemove = true,
                        onMoveUp = { onMoveUp(track) },
                        onMoveDown = { onMoveDown(track) },
                        onRemove = { onRemoveFromPlaylist(track) },
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }
        }
    }

    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("Create playlist") },
            text = {
                OutlinedTextField(
                    value = newPlaylistName,
                    onValueChange = { newPlaylistName = it },
                    placeholder = { Text("Playlist name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newPlaylistName.isNotBlank()) {
                            onCreatePlaylist(newPlaylistName.trim())
                            newPlaylistName = ""
                            showCreateDialog = false
                        }
                    }
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showCreateDialog = false
                    newPlaylistName = ""
                }) {
                    Text("Cancel")
                }
            }
        )
    }
}
