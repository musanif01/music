package com.pulsemusic.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pulsemusic.data.model.Track
import com.pulsemusic.ui.components.TrackItem

@Composable
fun QueueScreen(
    queueTracks: List<Track>,
    favoriteIds: List<String>,
    currentTrackId: String?,
    onPlay: (Track) -> Unit,
    onAddToQueue: (Track) -> Unit,
    onRemoveFromQueue: (Track) -> Unit,
    onMoveUp: (Track) -> Unit,
    onMoveDown: (Track) -> Unit,
    onClearQueue: () -> Unit,
    onToggleFavorite: (Track) -> Unit,
    onAddToPlaylist: (Track) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Queue",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            if (queueTracks.isNotEmpty()) {
                TextButton(onClick = onClearQueue) {
                    Text("Clear")
                }
            }
        }

        if (queueTracks.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.QueueMusic,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Queue is empty",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Add tracks from search, library, or playlists.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (currentTrackId != null) {
                    val currentTrack = queueTracks.find { it.id == currentTrackId }
                    if (currentTrack != null) {
                        item {
                            Text(
                                text = "Now playing",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                        item {
                            TrackItem(
                                track = currentTrack,
                                isFavorite = favoriteIds.contains(currentTrack.id),
                                onPlay = { onPlay(currentTrack) },
                                onAddToQueue = { },
                                onToggleFavorite = { onToggleFavorite(currentTrack) },
                                onAddToPlaylist = { onAddToPlaylist(currentTrack) },
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    }
                }

                val upcomingTracks = queueTracks.filter { it.id != currentTrackId }
                if (upcomingTracks.isNotEmpty()) {
                    item {
                        Text(
                            text = "Next in queue",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                    items(upcomingTracks, key = { it.id }) { track ->
                        TrackItem(
                            track = track,
                            isFavorite = favoriteIds.contains(track.id),
                            onPlay = { onPlay(track) },
                            onAddToQueue = { },
                            onToggleFavorite = { onToggleFavorite(track) },
                            onAddToPlaylist = { onAddToPlaylist(track) },
                            showMoveUp = true,
                            showMoveDown = true,
                            showRemove = true,
                            onMoveUp = { onMoveUp(track) },
                            onMoveDown = { onMoveDown(track) },
                            onRemove = { onRemoveFromQueue(track) },
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }
}
