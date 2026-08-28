package com.pulsemusic.bridge

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import com.pulsemusic.data.model.AlbumDetail
import com.pulsemusic.data.model.ArtistDetail
import com.pulsemusic.data.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

class YTMusicBridge(private val context: Context) {

    private val gson = Gson()
    private var pyClass: Any? = null
    private var initError: String? = null

    init {
        try {
            val python = com.chaquo.python.Python.getInstance()
            val module = python.getModule("ytmusic")
            pyClass = module.callAttr("PulseYTMusic")
        } catch (e: Throwable) {
            initError = "Python module init failed: ${e.message}"
            Log.e("YTMusicBridge", "Failed to initialize Python module", e)
        }
    }

    suspend fun search(query: String, filter: String? = null): List<SearchResult> = withContext(Dispatchers.IO) {
        if (pyClass == null) {
            throw IllegalStateException(initError ?: "Python not initialized")
        }

        try {
            val py = pyClass as com.chaquo.python.PyObject
            val result = if (filter != null) {
                py.callAttr("search", query, filter)
            } else {
                py.callAttr("search", query)
            }
            val json = result.toString()
            if (json.contains("\"error\"")) {
                val err = gson.fromJson(json, JsonObject::class.java)
                throw IllegalStateException("ytmusicapi: ${err?.get("error")?.asString ?: "unknown"}")
            }
            val parsed = parseSearchResults(json)
            if (parsed.isEmpty()) {
                val head = if (json == null || json.isBlank()) "empty" else json.take(120) + "…"
                throw IllegalStateException("Search returned no results (raw: $head)")
            }
            parsed
        } catch (e: Exception) {
            Log.e("YTMusicBridge", "Search failed", e)
            throw e
        }
    }

    suspend fun getSearchSuggestions(query: String): List<String> = withContext(Dispatchers.IO) {
        if (pyClass == null) return@withContext emptyList()
        try {
            val py = pyClass as com.chaquo.python.PyObject
            val result = py.callAttr("search_suggestions", query)
            val arr = gson.fromJson(result.toString(), Array::class.java)
            arr?.map { it.toString() }?.filter { it.startsWith(query) } ?: emptyList()
        } catch (e: Exception) {
            Log.e("YTMusicBridge", "Suggestions failed", e)
            emptyList()
        }
    }

    suspend fun getLyrics(videoId: String): String? = withContext(Dispatchers.IO) {
        if (pyClass == null) return@withContext null
        try {
            val py = pyClass as com.chaquo.python.PyObject
            val result = py.callAttr("get_lyrics", videoId)
            val obj = gson.fromJson(result.toString(), JsonObject::class.java)
            obj?.get("lyrics")?.asString
        } catch (e: Exception) {
            Log.e("YTMusicBridge", "Get lyrics failed", e)
            null
        }
    }

    suspend fun getWatchPlaylist(videoId: String, limit: Int = 25): List<WatchPlaylistItem> = withContext(Dispatchers.IO) {
        if (pyClass == null) return@withContext emptyList()
        try {
            val py = pyClass as com.chaquo.python.PyObject
            val result = py.callAttr("get_watch_playlist", videoId, limit)
            val json = result.toString()
            parseWatchPlaylist(json)
        } catch (e: Exception) {
            Log.e("YTMusicBridge", "Get watch playlist failed", e)
            emptyList()
        }
    }

    suspend fun getAlbum(albumBrowseId: String): JsonObject? = withContext(Dispatchers.IO) {
        if (pyClass == null) return@withContext null
        try {
            val py = pyClass as com.chaquo.python.PyObject
            val result = py.callAttr("get_album", albumBrowseId)
            gson.fromJson(result.toString(), JsonObject::class.java)
        } catch (e: Exception) {
            Log.e("YTMusicBridge", "Get album failed", e)
            null
        }
    }

    suspend fun getArtist(artistId: String): JsonObject? = withContext(Dispatchers.IO) {
        if (pyClass == null) return@withContext null
        try {
            val py = pyClass as com.chaquo.python.PyObject
            val result = py.callAttr("get_artist", artistId)
            gson.fromJson(result.toString(), JsonObject::class.java)
        } catch (e: Exception) {
            Log.e("YTMusicBridge", "Get artist failed", e)
            null
        }
    }

    suspend fun getCharts(country: String? = null): JsonObject? = withContext(Dispatchers.IO) {
        if (pyClass == null) return@withContext null
        try {
            val py = pyClass as com.chaquo.python.PyObject
            val result = if (country != null) {
                py.callAttr("get_charts", country)
            } else {
                py.callAttr("get_charts")
            }
            gson.fromJson(result.toString(), JsonObject::class.java)
        } catch (e: Exception) {
            Log.e("YTMusicBridge", "Get charts failed", e)
            null
        }
    }

    suspend fun getHome(): JsonObject? = withContext(Dispatchers.IO) {
        if (pyClass == null) return@withContext null
        try {
            val py = pyClass as com.chaquo.python.PyObject
            val result = py.callAttr("get_home")
            gson.fromJson(result.toString(), JsonObject::class.java)
        } catch (e: Exception) {
            Log.e("YTMusicBridge", "Get home failed", e)
            null
        }
    }

    suspend fun getMoodCategories(): List<String> = withContext(Dispatchers.IO) {
        if (pyClass == null) return@withContext emptyList()
        try {
            val py = pyClass as com.chaquo.python.PyObject
            val result = py.callAttr("get_mood_categories")
            val arr = gson.fromJson(result.toString(), Array::class.java)
            arr?.map { it.toString() } ?: emptyList()
        } catch (e: Exception) {
            Log.e("YTMusicBridge", "Get mood categories failed", e)
            emptyList()
        }
    }

    fun searchResultToTrack(result: SearchResult): Track {
        return Track(
            id = result.id ?: "yt-${UUID.randomUUID()}",
            source = "youtube",
            youtubeId = result.youtubeId,
            title = result.title ?: "Unknown",
            artist = result.artist ?: "YouTube",
            album = result.album,
            duration = result.duration,
            durationMs = result.durationMs,
            thumbnail = result.thumbnail,
            url = result.url
        )
    }

    private fun parseSearchResults(json: String): List<SearchResult> {
        val type = object : com.google.gson.reflect.TypeToken<List<SearchResult>>() {}.type
        return try {
            gson.fromJson<List<SearchResult>>(json, type) ?: emptyList()
        } catch (e: Exception) {
            Log.e("YTMusicBridge", "Parse search results failed", e)
            emptyList()
        }
    }


    fun parseArtistDetail(json: JsonObject): ArtistDetail {
        val header = json.getAsJsonObject("header")
        val musicCarouselShelfRenderer = json.getAsJsonArray("sections")?.firstOrNull()?.asJsonObject
            ?.getAsJsonObject("musicShelfRenderer")

        val name = header?.getAsJsonObject("musicResponsiveHeaderRenderer")
            ?.getAsJsonArray("title")?.get(0)?.asJsonObject?.get("text")?.asString
            ?: "Unknown Artist"

        val thumbnail = header?.getAsJsonObject("musicResponsiveHeaderRenderer")
            ?.getAsJsonArray("thumbnail")?.get(0)?.asJsonObject
            ?.getAsJsonArray("thumbnails")?.lastOrNull()?.asJsonObject?.get("url")?.asString

        val description = header?.getAsJsonObject("musicResponsiveHeaderRenderer")
            ?.getAsJsonArray("description")?.get(0)?.asJsonObject?.get("text")?.asString

        val songs = mutableListOf<Track>()
        val contents = musicCarouselShelfRenderer?.getAsJsonArray("contents")
        if (contents != null) {
            for (item in contents) {
                val obj = item.asJsonObject
                val flexItem = obj.getAsJsonObject("musicResponsiveListItemRenderer") ?: continue
                val videoId = flexItem.getAsJsonArray("playlistItemData")
                    ?.get(0)?.asJsonObject?.get("videoId")?.asString
                val titleRuns = flexItem.getAsJsonArray("flexColumns")
                    ?.get(0)?.asJsonObject?.getAsJsonObject("musicResponsiveListItemFlexColumnRenderer")
                    ?.getAsJsonObject("text")?.getAsJsonArray("runs")
                val title = titleRuns?.joinToString("") { it.asJsonObject.get("text").asString } ?: "Unknown"

                val artistRuns = flexItem.getAsJsonArray("flexColumns")
                    ?.get(1)?.asJsonObject?.getAsJsonObject("musicResponsiveListItemFlexColumnRenderer")
                    ?.getAsJsonObject("text")?.getAsJsonArray("runs")
                val artist = artistRuns?.joinToString("") { it.asJsonObject.get("text").asString } ?: name

                val thumbUrl = flexItem.getAsJsonArray("thumbnail")?.get(0)?.asJsonObject
                    ?.getAsJsonArray("thumbnails")?.lastOrNull()?.asJsonObject?.get("url")?.asString

                if (videoId != null) {
                    songs.add(Track(
                        id = "yt-$videoId",
                        source = "youtube",
                        youtubeId = videoId,
                        title = title,
                        artist = artist,
                        thumbnail = thumbUrl
                    ))
                }
            }
        }

        return ArtistDetail(
            name = name,
            thumbnail = thumbnail,
            description = description,
            songs = songs
        )
    }

    fun parseAlbumDetail(json: JsonObject): AlbumDetail {
        val header = json.getAsJsonObject("header")

        val title = header?.getAsJsonObject("musicResponsiveHeaderRenderer")
            ?.getAsJsonArray("title")?.get(0)?.asJsonObject?.get("text")?.asString
            ?: "Unknown Album"

        val artist = header?.getAsJsonObject("musicResponsiveHeaderRenderer")
            ?.getAsJsonArray("subtitle")?.get(0)?.asJsonObject?.get("text")?.asString

        val thumbnail = header?.getAsJsonObject("musicResponsiveHeaderRenderer")
            ?.getAsJsonArray("thumbnail")?.get(0)?.asJsonObject
            ?.getAsJsonArray("thumbnails")?.lastOrNull()?.asJsonObject?.get("url")?.asString

        val year = header?.getAsJsonObject("musicResponsiveHeaderRenderer")
            ?.getAsJsonArray("subtitle")?.get(2)?.asJsonObject?.get("text")?.asString

        val songs = mutableListOf<Track>()
        val contents = json.getAsJsonArray("contents")?.firstOrNull()?.asJsonObject
            ?.getAsJsonObject("musicShelfRenderer")?.getAsJsonArray("contents")
        if (contents != null) {
            for (item in contents) {
                val obj = item.asJsonObject
                val flexItem = obj.getAsJsonObject("musicResponsiveListItemRenderer") ?: continue
                val playlistItemData = flexItem.getAsJsonObject("playlistItemData") ?: continue
                val videoId = playlistItemData.get("videoId")?.asString
                val titleRuns = flexItem.getAsJsonArray("flexColumns")
                    ?.get(0)?.asJsonObject?.getAsJsonObject("musicResponsiveListItemFlexColumnRenderer")
                    ?.getAsJsonObject("text")?.getAsJsonArray("runs")
                val trackTitle = titleRuns?.joinToString("") { it.asJsonObject.get("text").asString } ?: "Unknown"

                val artistRuns = flexItem.getAsJsonArray("flexColumns")
                    ?.get(1)?.asJsonObject?.getAsJsonObject("musicResponsiveListItemFlexColumnRenderer")
                    ?.getAsJsonObject("text")?.getAsJsonArray("runs")
                val trackArtist = artistRuns?.joinToString("") { it.asJsonObject.get("text").asString } ?: artist ?: "Unknown"

                val thumbUrl = flexItem.getAsJsonArray("thumbnail")?.get(0)?.asJsonObject
                    ?.getAsJsonArray("thumbnails")?.lastOrNull()?.asJsonObject?.get("url")?.asString

                if (videoId != null) {
                    songs.add(Track(
                        id = "yt-$videoId",
                        source = "youtube",
                        youtubeId = videoId,
                        title = trackTitle,
                        artist = trackArtist,
                        album = title,
                        thumbnail = thumbUrl
                    ))
                }
            }
        }

        return AlbumDetail(
            title = title,
            artist = artist,
            thumbnail = thumbnail,
            year = year,
            songs = songs
        )
    }

    private fun parseWatchPlaylist(json: String): List<WatchPlaylistItem> {
        try {
            val obj = gson.fromJson(json, JsonObject::class.java)
            val tracks = obj?.getAsJsonArray("tracks") ?: return emptyList()
            return tracks.map { element ->
                val t = element.asJsonObject
                WatchPlaylistItem(
                    videoId = t.get("videoId")?.asString,
                    title = t.get("title")?.asString,
                    artist = t.get("artist")?.asString,
                    thumbnail = t.get("thumbnail")?.asString,
                    duration = t.get("length")?.asString,
                    durationMs = t.get("durationMs")?.asString?.toLongOrNull()
                )
            }
        } catch (e: Exception) {
            Log.e("YTMusicBridge", "Parse watch playlist failed", e)
            return emptyList()
        }
    }
}

data class SearchResult(
    val id: String?,
    val youtubeId: String? = null,
    val browseId: String? = null,
    val title: String?,
    val artist: String? = null,
    val album: String? = null,
    val duration: String? = null,
    val durationMs: Long? = null,
    val thumbnail: String? = null,
    val url: String? = null,
    val type: String? = "song",
    val resultType: String = "song"
)

data class WatchPlaylistItem(
    val videoId: String?,
    val title: String?,
    val artist: String?,
    val thumbnail: String?,
    val duration: String?,
    val durationMs: Long?
)
