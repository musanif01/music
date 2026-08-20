package com.pulsemusic.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.pulsemusic.data.model.Track
import com.pulsemusic.ui.theme.artColors

@Composable
fun TrackItem(
    track: Track,
    isFavorite: Boolean = false,
    onPlay: () -> Unit = {},
    onAddToQueue: () -> Unit = {},
    onToggleFavorite: () -> Unit = {},
    onAddToPlaylist: () -> Unit = {},
    onClick: () -> Unit = {},
    showMoveUp: Boolean = false,
    showMoveDown: Boolean = false,
    showRemove: Boolean = false,
    onMoveUp: () -> Unit = {},
    onMoveDown: () -> Unit = {},
    onRemove: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(4.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (!track.thumbnail.isNullOrEmpty()) {
                AsyncImage(
                    model = track.thumbnail,
                    contentDescription = track.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = artColors[Math.abs(track.title.hashCode()) % artColors.size]
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = track.title.first().uppercase(),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = track.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (track.source == "youtube") {
                    Icon(
                        Icons.Default.YouTube,
                        contentDescription = "YouTube",
                        modifier = Modifier.size(12.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }
                Text(
                    text = track.artist,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (track.album != null) {
                    Text(
                        text = " \u00B7 ${track.album}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Row {
            if (showMoveUp) {
                IconButton(onClick = onMoveUp, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Move up", modifier = Modifier.size(18.dp))
                }
            }
            if (showMoveDown) {
                IconButton(onClick = onMoveDown, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Move down", modifier = Modifier.size(18.dp))
                }
            }
            if (showRemove) {
                IconButton(onClick = onRemove, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Remove", modifier = Modifier.size(18.dp))
                }
            }

            IconButton(onClick = onToggleFavorite, modifier = Modifier.size(32.dp)) {
                Icon(
                    if (isFavorite) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = "Favorite",
                    modifier = Modifier.size(18.dp),
                    tint = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(onClick = onAddToQueue, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Add, contentDescription = "Add to queue", modifier = Modifier.size(18.dp))
            }

            IconButton(onClick = onPlay, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.PlayArrow, contentDescription = "Play", modifier = Modifier.size(18.dp))
            }
        }
    }
}
