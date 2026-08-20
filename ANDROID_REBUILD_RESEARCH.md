# Android Rebuild Research: Offline Music App Without Ads

## Target product shape

I could not find a reliable public listing for the exact title `Tunecho music-Play offline APK for Android`. The public search results I checked did not surface a matching Google Play, APKMirror, APKPure, APKCombo, or package-name record under that exact spelling. Treat this as a feature-rebuild brief, not a clone of a verified app package.

The local project in this repo, `Pulse Music`, already models the compliant feature set:

- Search/discovery for YouTube music metadata.
- Playback of YouTube items only through visible official embeds/links.
- True offline playback only for user-owned or licensed audio files imported from the device.
- Playlists, favorites, recents, queue, shuffle, import/export, and offline library storage.
- No ad SDKs.

## Ad-free MVP scope

1. Let users search YouTube music/videos.
2. Save chosen YouTube video IDs into app-owned playlists.
3. Let users reorder tracks in playlists and in the active queue.
4. Play the ordered list using a visible YouTube player.
5. Add local owned files for true offline/native playback.
6. Optionally add Spotify/Apple Music login later for subscription users.

## Non-negotiable legal/API boundary

Do not build a YouTube downloader, background audio extractor, ad blocker, or offline YouTube cache. YouTube's API policies prohibit API clients from downloading, importing, backing up, caching, storing copies of YouTube audiovisual content, or making it available for offline playback without written approval.

Allowed:

- Store YouTube metadata such as video id, title, channel/artist label, thumbnail URL, and watch URL within the policy's data rules.
- Open YouTube Music or embed a visible YouTube player.
- Store and play user-owned local audio files offline.
- Remove ads from our own rebuilt app by not integrating ad SDKs.

Not allowed:

- Reverse engineer undocumented YouTube endpoints.
- Extract direct audio streams from YouTube.
- Hide/replace YouTube playback UI to simulate a native background music service.
- Patch someone else's APK to remove ads.

## Android architecture

Recommended stack:

- Kotlin.
- Jetpack Compose for UI.
- Media3 ExoPlayer for local/owned audio playback.
- Media3 `MediaSessionService` or `MediaLibraryService` for background playback, lock screen controls, notification controls, Bluetooth/headset controls, and Android Auto/Wear clients.
- Room for tracks, playlists, queue, favorites, recents, play counts, and cached metadata.
- DataStore for small settings such as region, safe search, sort mode, theme, and onboarding flags.
- MediaStore plus Android Photo Picker / Storage Access Framework for user-selected audio files.
- Retrofit/OkHttp or Ktor for YouTube Data API requests if online discovery is included.
- Coil for album art and thumbnails.
- WorkManager only for non-urgent background maintenance, such as rescanning local library metadata.

## Core data model

`Track`

- `id`: local UUID.
- `source`: `LOCAL`, `YOUTUBE_LINK`, or future licensed-provider source.
- `title`.
- `artist`.
- `album`.
- `durationMs`.
- `artUri`.
- `contentUri`: Android `content://` URI for local audio.
- `youtubeVideoId`: only for YouTube metadata/link records.
- `createdAt`, `lastPlayedAt`, `playCount`.

`Playlist`

- `id`, `name`, `createdAt`, `updatedAt`, `sortOrder`.

`PlaylistTrack`

- `playlistId`, `trackId`, `position`, `addedAt`.

`PlaybackQueue`

- `queueId`, `trackId`, `position`, `sourceContext`, `createdAt`.

`Favorite`

- `trackId`, `createdAt`.

## APIs and platform permissions

Local audio:

- Query device music through `MediaStore.Audio.Media`.
- Open user-selected files through `ContentResolver.openFileDescriptor()` or pass their `content://` URI to Media3.
- Request `READ_MEDIA_AUDIO` on Android 13+ only if scanning the broader media library is needed.
- Prefer the system picker or Storage Access Framework for user-selected imports because it avoids broad library permission.

Playback:

- Use Media3 `ExoPlayer` as the `Player`.
- Set `AudioAttributes.USAGE_MEDIA` and let ExoPlayer manage audio focus.
- Host the player inside `MediaSessionService`.
- Expose playback through `MediaController` in Compose.

Online discovery:

- YouTube Data API `search.list`.
- Suggested query parameters:
  - `part=snippet`
  - `q=<user query>`
  - `type=video`
  - `videoCategoryId=10`
  - `videoEmbeddable=true`
  - `regionCode=<setting>`
  - `safeSearch=<setting>`
  - `maxResults=10-25`
- Playback should open YouTube Music or a visible YouTube embed/WebView, not native extracted streams.

## Algorithms

Search normalization:

- Trim whitespace, lowercase for local matching, remove repeated spaces.
- For local library search, use Room FTS if the library grows beyond a few hundred tracks.
- Rank exact title prefix matches first, then title substring, artist substring, album substring, then recency/play count boost.

Deduplication:

- Local files: stable key from `MediaStore._ID` or persisted URI plus size/duration fallback.
- YouTube links: `yt:<videoId>`.
- Imported files: persisted URI or app-copied file id.

Queue:

- Queue is an ordered list of track IDs.
- `playNext`: next position if available, otherwise repeat behavior decides stop/restart.
- `playPrevious`: if current position is over 3 seconds, seek to start; otherwise previous item.
- Removing a track must remove it from queue and playlists atomically.

Shuffle:

- Use Fisher-Yates over the current playable track list.
- Keep a shuffle seed/session so the order is stable until the user reshuffles.

Recent plays:

- Move played track to top.
- Cap list to 40-100 records.
- Update `lastPlayedAt` and `playCount` on playback start or after a threshold such as 30 seconds.

Offline import:

- Let users pick files.
- Persist URI permission when using SAF.
- Read metadata with `MediaMetadataRetriever`.
- Store structured metadata in Room.
- Play directly from URI with Media3.

Artwork:

- Prefer embedded album art for local files.
- Cache generated thumbnails or extracted album art in app cache.
- For YouTube, use thumbnail URLs only as metadata; obey YouTube display and refresh rules.

## Minimal Android implementation milestones

1. Native shell: Compose navigation with Home, Library, Playlists, Offline, Settings.
2. Local playback: Media3 player service, notification, audio focus, queue, lock screen controls.
3. Local import/library: picker, persisted URI permissions, metadata extraction, Room persistence.
4. Playlists/favorites/recents: Room relations and stable queue behavior.
5. YouTube discovery: official Data API search and "open in YouTube Music" or visible embed.
6. Polish: search ranking, shuffle seed, export/import playlists, empty states, responsive layouts.
7. Release: no ad SDK; package as AAB/APK; add privacy policy for media permissions and API use.

## Sources

- Android Media3 ExoPlayer: https://developer.android.com/media/media3/exoplayer
- Android Media3 background playback: https://developer.android.com/media/media3/session/background-playback
- Android audio focus: https://developer.android.com/media/optimize/audio-focus
- Android MediaStore/shared media: https://developer.android.com/training/data-storage/shared/media
- Android Room: https://developer.android.com/training/data-storage/room
- Android DataStore: https://developer.android.com/topic/libraries/architecture/datastore
- Jetpack Compose: https://developer.android.com/compose
- YouTube Data API `search.list`: https://developers.google.com/youtube/v3/docs/search/list
- YouTube IFrame Player API: https://developers.google.com/youtube/iframe_api_reference
- YouTube API Services Developer Policies: https://developers.google.com/youtube/terms/developer-policies
