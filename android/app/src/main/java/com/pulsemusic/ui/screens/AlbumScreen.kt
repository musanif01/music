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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.pulsemusic.data.model.AlbumDetail
import com.pulsemusic.data.model.Track
import com.pulsemusic.ui.components.TrackItem
import com.pulsemusic.ui.theme.artColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumScreen(
    albumDetail: AlbumDetail,
    favoriteIds: List<String>,
    onBack: () -> Unit,
    onPlay: (Track) -> Unit,
    onAddToQueue: (Track) -> Unit,
    onToggleFavorite: (Track) -> Unit,
    onAddToPlaylist: (Track) -> Unit
) {
    val dominantColor = artColors[Math.abs(albumDetail.title.hashCode()) % artColors.size]

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            dominantColor.copy(alpha = 0.8f),
                            MaterialTheme.colorScheme.background
                        )
                    )
                )
        )

        Column(modifier = Modifier.fillMaxSize()) {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Box(
                            modifier = Modifier
                                .size(160.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (!albumDetail.thumbnail.isNullOrEmpty()) {
                                AsyncImage(
                                    model = albumDetail.thumbnail,
                                    contentDescription = albumDetail.title,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Surface(
                                    modifier = Modifier.fillMaxSize(),
                                    color = dominantColor
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Default.Album,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(64.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = albumDetail.title,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            if (albumDetail.artist != null) {
                                Text(
                                    text = albumDetail.artist,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                                )
                            }
                            if (albumDetail.year != null) {
                                Text(
                                    text = albumDetail.year,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                                )
                            }
                            Text(
                                text = "${albumDetail.songs.size} songs",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                            )
                        }
                    }
                }

                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { }) {
                            Icon(
                                Icons.Default.FavoriteBorder,
                                contentDescription = "Add to favorites",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        IconButton(onClick = { }) {
                            Icon(
                                Icons.Default.MoreVert,
                                contentDescription = "More options",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        if (albumDetail.songs.isNotEmpty()) {
                            Button(
                                onClick = {
                                    albumDetail.songs.forEach { onAddToQueue(it) }
                                    albumDetail.songs.firstOrNull()?.let { onPlay(it) }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Icon(
                                    Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Play")
                            }
                        }
                    }
                }

                items(albumDetail.songs, key = { it.id }) { track ->
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

                if (albumDetail.songs.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "No songs found in this album",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
