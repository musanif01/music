package com.pulsemusic.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.pulsemusic.bridge.SearchResult
import com.pulsemusic.data.model.Track
import com.pulsemusic.data.model.TypedSearchResults
import com.pulsemusic.ui.components.SearchBar
import com.pulsemusic.ui.components.TrackItem
import com.pulsemusic.ui.theme.artColors

data class BrowseCategory(
    val id: String,
    val title: String,
    val color: Color
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    searchResults: TypedSearchResults,
    searchSuggestions: List<String>,
    isSearching: Boolean,
    searchError: String? = null,
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: (String) -> Unit,
    onPlay: (Track) -> Unit,
    onAddToQueue: (Track) -> Unit,
    onToggleFavorite: (Track) -> Unit,
    onAddToPlaylist: (Track) -> Unit,
    onArtistClick: (SearchResult) -> Unit = {},
    onAlbumClick: (SearchResult) -> Unit = {},
    onCategoryClick: (String) -> Unit = {}
) {
    val hasResults = searchResults.songs.isNotEmpty() || searchResults.albums.isNotEmpty() || searchResults.artists.isNotEmpty()
    val hasQuery = query.isNotBlank()

    val categories = remember {
        listOf(
            BrowseCategory("podcasts", "Podcasts", Color(0xFFE13300)),
            BrowseCategory("live", "Live Events", Color(0xFF8400E7)),
            BrowseCategory("made_for_you", "Made For You", Color(0xFF1E3264)),
            BrowseCategory("new_releases", "New Releases", Color(0xFFE8115B)),
            BrowseCategory("pop", "Pop", Color(0xFF148A08)),
            BrowseCategory("hip_hop", "Hip-Hop", Color(0xFFBA5D07)),
            BrowseCategory("rock", "Rock", Color(0xFFE91429)),
            BrowseCategory("latin", "Latin", Color(0xFFE1118B)),
            BrowseCategory("mood", "Mood", Color(0xFF477D95)),
            BrowseCategory("charts", "Charts", Color(0xFF8D67AB)),
            BrowseCategory("discover", "Discover", Color(0xFF1E3264)),
            BrowseCategory("concerts", "Concerts", Color(0xFF509BF5)),
            BrowseCategory("indie", "Indie", Color(0xFF608108)),
            BrowseCategory("workout", "Workout", Color(0xFF777777)),
            BrowseCategory("chill", "Chill", Color(0xFF503750)),
            BrowseCategory("sleep", "Sleep", Color(0xFF1E3264)),
            BrowseCategory("party", "Party", Color(0xFFE13300)),
            BrowseCategory("focus", "Focus", Color(0xFF509BF5)),
            BrowseCategory("jazz", "Jazz", Color(0xFF477D95)),
            BrowseCategory("classical", "Classical", Color(0xFF7D4B32)),
            BrowseCategory("country", "Country", Color(0xFFBA5D07)),
            BrowseCategory("r_and_b", "R&B", Color(0xDC148A08)),
            BrowseCategory("k_pop", "K-Pop", Color(0xFF148A08)),
            BrowseCategory("ambient", "Ambient", Color(0xFF503750))
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))

            SearchBar(
                query = query,
                onQueryChange = onQueryChange,
                onSearch = { onSearch(query) }
            )

            Spacer(modifier = Modifier.height(12.dp))
        }

        if (hasQuery && hasResults) {
            if (searchResults.songs.isNotEmpty()) {
                item {
                    Text(
                        text = "Songs",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
                items(searchResults.songs.take(5), key = { it.id }) { track ->
                    TrackItem(
                        track = track,
                        isFavorite = false,
                        onPlay = { onPlay(track) },
                        onAddToQueue = { onAddToQueue(track) },
                        onToggleFavorite = { onToggleFavorite(track) },
                        onAddToPlaylist = { onAddToPlaylist(track) },
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }

            if (searchResults.artists.isNotEmpty()) {
                item {
                    Text(
                        text = "Artists",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
                items(searchResults.artists.take(5), key = { it.id ?: it.title ?: "" }) { artist ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onArtistClick(artist) }
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(24.dp)),
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

                        Text(
                            text = artist.title ?: "Unknown",
                            style = MaterialTheme.typography.bodyLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            if (searchResults.albums.isNotEmpty()) {
                item {
                    Text(
                        text = "Albums",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
                items(searchResults.albums.take(5), key = { it.id ?: it.title ?: "" }) { album ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onAlbumClick(album) }
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
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
            }
        } else if (hasQuery && isSearching) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        } else if (hasQuery && searchError != null && !isSearching) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Search error: $searchError",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else if (hasQuery && !hasResults && !isSearching) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No results found for \"$query\"",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            item {
                Text(
                    text = "Browse all",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(categories.take(8)) { category ->
                        CategoryCard(category = category, onClick = { onCategoryClick(category.title) })
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }

            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(categories.drop(8).take(8)) { category ->
                        CategoryCard(category = category, onClick = { onCategoryClick(category.title) })
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }

            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(categories.drop(16)) { category ->
                        CategoryCard(category = category, onClick = { onCategoryClick(category.title) })
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
private fun CategoryCard(
    category: BrowseCategory,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .size(160.dp, 100.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
        color = category.color
    ) {
        Box(
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                text = category.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}
