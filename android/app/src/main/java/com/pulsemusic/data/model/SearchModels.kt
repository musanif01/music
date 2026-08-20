package com.pulsemusic.data.model

import com.pulsemusic.bridge.SearchResult

data class TypedSearchResults(
    val songs: List<Track> = emptyList(),
    val albums: List<SearchResult> = emptyList(),
    val artists: List<SearchResult> = emptyList()
)

data class ArtistDetail(
    val name: String,
    val thumbnail: String? = null,
    val description: String? = null,
    val songs: List<Track> = emptyList()
)

data class AlbumDetail(
    val title: String,
    val artist: String? = null,
    val thumbnail: String? = null,
    val year: String? = null,
    val songs: List<Track> = emptyList()
)
