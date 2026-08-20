# Music App PWA

A mobile-first music player prototype that supports:

- YouTube/YouTube Music discovery through the official YouTube Data API.
- YouTube playback through visible embedded YouTube players.
- Local/offline playback for user-owned audio files imported from the device.
- Playlists, favorites, queue, recent plays, playlist reordering, queue reordering, and playlist export.
- PWA shell caching for app files.
- No ad SDKs.

## MVP scope

For a clean ad-free Android rebuild, keep the product focused on:

1. Let users search YouTube music/videos.
2. Save chosen YouTube video IDs into app-owned playlists.
3. Let users reorder playlists and the active queue.
4. Play the ordered list using a visible YouTube player.
5. Add local owned files for true offline/native playback.
6. Optionally add Spotify/Apple Music login later for subscription users.

## Important YouTube boundary

This app does **not** download, cache, extract, or provide offline playback for YouTube audiovisual content. YouTube tracks are saved as metadata and links only, then played through official embeds. Offline playback and file download features are limited to user-owned or properly licensed local audio files imported by the user.

Official references:

- YouTube Data API `search.list`: https://developers.google.com/youtube/v3/docs/search/list
- YouTube IFrame Player API: https://developers.google.com/youtube/iframe_api_reference
- YouTube API Services Developer Policies: https://developers.google.com/youtube/terms/developer-policies

## Run locally

```bash
python3 -m http.server 4173
```

Then open:

```text
http://127.0.0.1:4173
```

## YouTube search setup

Open the Settings tab in the app and paste a browser-restricted YouTube Data API key. The app uses `search.list` with music-focused filters:

- `type=video`
- `videoCategoryId=10`
- `videoEmbeddable=true`

Without an API key, search still opens YouTube Music in a new tab and shows sample tracks so the app can be tested.

## Features

- YouTube search via official Data API (with API key)
- YouTube playback through visible embedded player
- Local file import for offline playback (IndexedDB)
- Playlists with create, rename, delete, reorder, and export
- Favorites and recent plays tracking
- Queue management with shuffle
- Dark mode support
- Search history persistence
- Loading states for API calls
- PWA shell caching with offline fallback

## Files

- `index.html` - PWA document and app views.
- `styles.css` - responsive mobile app styling.
- `app.js` - state, storage, playback, playlists, search, and downloads.
- `sw.js` - service worker with offline fallback.
- `manifest.webmanifest` - install metadata.
- `tests.js` - unit tests for core functions.
- `test.html` - test runner page.
