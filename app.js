"use strict";

const STORE_KEY = "pulse-music-state-v1";
const DB_NAME = "pulse-music-files";
const DB_VERSION = 1;
const ART_KEYS = ["teal", "gold", "plum", "coral"];

const sampleTracks = [
  {
    id: "yt-sample-1",
    source: "youtube",
    youtubeId: "jfKfPfyJRdk",
    title: "lofi hip hop radio",
    artist: "Lofi Girl",
    duration: "Live",
    thumbnail: "https://i.ytimg.com/vi/jfKfPfyJRdk/hqdefault_live.jpg",
    url: "https://music.youtube.com/watch?v=jfKfPfyJRdk",
    art: "plum"
  },
  {
    id: "yt-sample-2",
    source: "youtube",
    youtubeId: "5qap5aO4i9A",
    title: "Chillhop stream",
    artist: "Chillhop Music",
    duration: "Live",
    thumbnail: "https://i.ytimg.com/vi/5qap5aO4i9A/hqdefault_live.jpg",
    url: "https://music.youtube.com/watch?v=5qap5aO4i9A",
    art: "gold"
  },
  {
    id: "yt-sample-3",
    source: "youtube",
    youtubeId: "DWcJFNfaw9c",
    title: "Electronic focus mix",
    artist: "YouTube Music",
    duration: "Stream",
    thumbnail: "",
    url: "https://music.youtube.com/search?q=electronic+focus+mix",
    art: "coral"
  }
];

const initialState = {
  settings: {
    apiKey: "",
    region: "US",
    safeSearch: "moderate",
    darkMode: false
  },
  tracks: sampleTracks,
  localTrackIds: [],
  favorites: [],
  recent: [],
  queue: [],
  playlists: [
    {
      id: "playlist-drive",
      name: "Drive",
      trackIds: ["yt-sample-1", "yt-sample-3"],
      createdAt: Date.now()
    },
    {
      id: "playlist-focus",
      name: "Focus",
      trackIds: ["yt-sample-2"],
      createdAt: Date.now()
    }
  ],
  selectedPlaylistId: "playlist-drive",
  currentTrackId: "",
  libraryFilter: "all",
  searchHistory: []
};

const els = {};
let state = loadState();
let dbPromise;
let currentObjectUrl = "";
let pendingDialogTrackId = "";
let progressTimer = 0;
let deferredInstallPrompt = null;

document.addEventListener("DOMContentLoaded", () => {
  bindElements();
  bindEvents();
  hydrateSettings();
  renderAll();
  registerServiceWorker();
});

function bindElements() {
  const ids = [
    "viewTitle",
    "installButton",
    "searchForm",
    "searchInput",
    "searchButton",
    "openYtMusicButton",
    "nowCover",
    "nowTitle",
    "nowArtist",
    "progressBar",
    "shuffleButton",
    "searchStatus",
    "searchResults",
    "libraryList",
    "clearRecentButton",
    "playlistForm",
    "playlistName",
    "playlistList",
    "selectedPlaylistTitle",
    "playPlaylistButton",
    "exportPlaylistButton",
    "playlistTracks",
    "fileInput",
    "downloadList",
    "queueList",
    "clearQueueButton",
    "apiKeyInput",
    "regionInput",
    "safeSearchInput",
    "saveSettingsButton",
    "dockCover",
    "dockTitle",
    "dockArtist",
    "prevButton",
    "playPauseButton",
    "nextButton",
    "embedPanel",
    "youtubeFrame",
    "closeEmbedButton",
    "audioElement",
    "playlistDialog",
    "dialogTrackTitle",
    "dialogPlaylistOptions",
    "renamePlaylistDialog",
    "renamePlaylistTitle",
    "renamePlaylistInput",
    "confirmRenameButton",
    "renamePlaylistButton",
    "deletePlaylistButton",
    "darkModeToggle",
    "toast"
  ];

  ids.forEach((id) => {
    els[id] = document.getElementById(id);
  });
}

function bindEvents() {
  document.querySelectorAll("[data-tab-target]").forEach((button) => {
    button.addEventListener("click", () => switchView(button.dataset.tabTarget));
  });

  document.querySelectorAll("[data-library-filter]").forEach((button) => {
    button.addEventListener("click", () => {
      state.libraryFilter = button.dataset.libraryFilter;
      persistState();
      renderLibrary();
    });
  });

  els.searchForm.addEventListener("submit", handleSearch);
  els.openYtMusicButton.addEventListener("click", openCurrentSearchInYouTubeMusic);
  els.shuffleButton.addEventListener("click", shuffleLibrary);
  els.clearRecentButton.addEventListener("click", clearRecent);
  els.playlistForm.addEventListener("submit", createPlaylist);
  els.playPlaylistButton.addEventListener("click", playSelectedPlaylist);
  els.exportPlaylistButton.addEventListener("click", exportSelectedPlaylist);
  els.renamePlaylistButton.addEventListener("click", openRenamePlaylistDialog);
  els.deletePlaylistButton.addEventListener("click", deleteSelectedPlaylist);
  els.confirmRenameButton.addEventListener("click", confirmRenamePlaylist);
  document.querySelector("#renamePlaylistDialog .text-button.ghost").addEventListener("click", () => {
    els.renamePlaylistDialog.close();
  });
  els.fileInput.addEventListener("change", importLocalFiles);
  els.clearQueueButton.addEventListener("click", clearQueue);
  els.saveSettingsButton.addEventListener("click", saveSettings);
  els.darkModeToggle.addEventListener("change", toggleDarkMode);
  els.prevButton.addEventListener("click", playPrevious);
  els.playPauseButton.addEventListener("click", togglePlayback);
  els.nextButton.addEventListener("click", playNext);
  document.querySelector(".hero-player").addEventListener("click", (event) => {
    if (event.target.closest("button")) return;
    const track = findTrack(state.currentTrackId);
    if (track?.source === "youtube") els.embedPanel.hidden = false;
  });
  document.querySelector(".player-dock").addEventListener("click", () => switchView("home"));
  els.closeEmbedButton.addEventListener("click", () => {
    els.embedPanel.hidden = true;
  });
  els.audioElement.addEventListener("ended", playNext);
  els.audioElement.addEventListener("play", () => {
    els.playPauseButton.textContent = "❚❚";
    startProgressTimer();
  });
  els.audioElement.addEventListener("pause", () => {
    els.playPauseButton.textContent = "▶";
    stopProgressTimer();
  });
  els.audioElement.addEventListener("timeupdate", updateProgress);

  window.addEventListener("keydown", (event) => {
    if (event.target.tagName === "INPUT" || event.target.tagName === "TEXTAREA" || event.target.tagName === "SELECT") return;
    if (event.code === "Space") {
      event.preventDefault();
      togglePlayback();
    }
    if (event.code === "ArrowRight") { event.preventDefault(); playNext(); }
    if (event.code === "ArrowLeft") { event.preventDefault(); playPrevious(); }
  });

  window.addEventListener("beforeinstallprompt", (event) => {
    event.preventDefault();
    deferredInstallPrompt = event;
    els.installButton.hidden = false;
  });

  els.installButton.addEventListener("click", async () => {
    if (!deferredInstallPrompt) return;
    deferredInstallPrompt.prompt();
    await deferredInstallPrompt.userChoice;
    deferredInstallPrompt = null;
    els.installButton.hidden = true;
  });
}

function loadState() {
  try {
    const raw = localStorage.getItem(STORE_KEY);
    if (!raw) return cloneInitialState();
    const parsed = JSON.parse(raw);
    return mergeState(parsed);
  } catch {
    return cloneInitialState();
  }
}

function mergeState(saved) {
  const merged = cloneInitialState();
  merged.settings = { ...merged.settings, ...(saved.settings || {}) };
  merged.tracks = mergeTracks(merged.tracks, saved.tracks || []);
  merged.localTrackIds = Array.isArray(saved.localTrackIds) ? saved.localTrackIds : [];
  merged.favorites = Array.isArray(saved.favorites) ? saved.favorites : [];
  merged.recent = Array.isArray(saved.recent) ? saved.recent : [];
  merged.queue = Array.isArray(saved.queue) ? saved.queue : [];
  merged.playlists = Array.isArray(saved.playlists) && saved.playlists.length ? saved.playlists : merged.playlists;
  merged.selectedPlaylistId = saved.selectedPlaylistId || merged.selectedPlaylistId;
  merged.currentTrackId = saved.currentTrackId || "";
  merged.libraryFilter = saved.libraryFilter || "all";
  merged.searchHistory = Array.isArray(saved.searchHistory) ? saved.searchHistory : [];
  return merged;
}

function mergeTracks(base, saved) {
  const map = new Map();
  base.concat(saved).forEach((track) => {
    if (track && track.id) map.set(track.id, track);
  });
  return Array.from(map.values());
}

function cloneInitialState() {
  if (typeof structuredClone === "function") return structuredClone(initialState);
  return JSON.parse(JSON.stringify(initialState));
}

function persistState() {
  localStorage.setItem(STORE_KEY, JSON.stringify(state));
}

function hydrateSettings() {
  els.apiKeyInput.value = state.settings.apiKey;
  els.regionInput.value = state.settings.region;
  els.safeSearchInput.value = state.settings.safeSearch;
  els.darkModeToggle.checked = state.settings.darkMode;
  applyTheme();
}

function applyTheme() {
  document.documentElement.setAttribute("data-theme", state.settings.darkMode ? "dark" : "light");
  const themeColor = state.settings.darkMode ? "#0f1214" : "#f7f8fb";
  document.querySelector('meta[name="theme-color"]').setAttribute("content", themeColor);
}

function toggleDarkMode() {
  state.settings.darkMode = els.darkModeToggle.checked;
  persistState();
  applyTheme();
}

function saveSettings() {
  state.settings.apiKey = els.apiKeyInput.value.trim();
  state.settings.region = normalizeRegion(els.regionInput.value);
  state.settings.safeSearch = els.safeSearchInput.value;
  els.regionInput.value = state.settings.region;
  persistState();
  renderSearchStatus();
  showToast("Settings saved");
}

function normalizeRegion(value) {
  const region = String(value || "US").trim().toUpperCase().replace(/[^A-Z]/g, "").slice(0, 2);
  return region.length === 2 ? region : "US";
}

function switchView(name) {
  document.querySelectorAll("[data-view]").forEach((view) => {
    view.hidden = view.dataset.view !== name;
  });
  document.querySelectorAll(".nav-item").forEach((item) => {
    item.classList.toggle("active", item.dataset.tabTarget === name);
  });
  els.viewTitle.textContent = titleForView(name);
  renderAll();
}

function titleForView(name) {
  return {
    home: "Home",
    library: "Library",
    playlists: "Playlists",
    downloads: "Offline",
    queue: "Queue",
    settings: "Settings"
  }[name] || "Home";
}

async function handleSearch(event) {
  event.preventDefault();
  const query = els.searchInput.value.trim();
  if (!query) {
    showToast("Enter a song, artist, or album");
    return;
  }

  if (!state.settings.apiKey) {
    renderSearchResults(sampleTracks);
    showToast("Add an API key for live YouTube search");
    return;
  }

  els.searchStatus.textContent = "Searching";
  els.searchButton.disabled = true;
  els.searchButton.textContent = "↻";
  try {
    const results = await searchYouTube(query);
    upsertTracks(results);
    renderSearchResults(results);
    els.searchStatus.textContent = `${results.length} results`;
    addToSearchHistory(query);
    showToast("Search updated");
  } catch (error) {
    els.searchStatus.textContent = "Search failed";
    showToast(error.message || "YouTube search failed");
  } finally {
    els.searchButton.disabled = false;
    els.searchButton.textContent = "⌕";
  }
}

async function searchYouTube(query) {
  const params = new URLSearchParams({
    part: "snippet",
    q: query,
    type: "video",
    videoCategoryId: "10",
    videoEmbeddable: "true",
    maxResults: "12",
    safeSearch: state.settings.safeSearch,
    regionCode: state.settings.region,
    key: state.settings.apiKey
  });

  const response = await fetch(`https://www.googleapis.com/youtube/v3/search?${params.toString()}`);
  const payload = await response.json();
  if (!response.ok) {
    const reason = payload?.error?.message || "YouTube API request failed";
    throw new Error(reason);
  }

  return (payload.items || [])
    .filter((item) => item.id?.videoId)
    .map((item, index) => {
      const videoId = item.id.videoId;
      const snippet = item.snippet || {};
      return {
        id: `yt-${videoId}`,
        source: "youtube",
        youtubeId: videoId,
        title: decodeHtml(snippet.title || "Untitled"),
        artist: decodeHtml(snippet.channelTitle || "YouTube"),
        duration: "Stream",
        thumbnail: snippet.thumbnails?.medium?.url || snippet.thumbnails?.default?.url || "",
        url: `https://music.youtube.com/watch?v=${videoId}`,
        art: ART_KEYS[index % ART_KEYS.length]
      };
    });
}

function decodeHtml(value) {
  const textarea = document.createElement("textarea");
  textarea.innerHTML = value;
  return textarea.value;
}

function upsertTracks(tracks) {
  const map = new Map(state.tracks.map((track) => [track.id, track]));
  tracks.forEach((track) => map.set(track.id, { ...map.get(track.id), ...track }));
  state.tracks = Array.from(map.values());
  persistState();
}

function openCurrentSearchInYouTubeMusic() {
  const query = els.searchInput.value.trim() || "music";
  window.open(`https://music.youtube.com/search?q=${encodeURIComponent(query)}`, "_blank", "noopener,noreferrer");
}

function addToSearchHistory(query) {
  if (!query) return;
  state.searchHistory = [query, ...state.searchHistory.filter((q) => q !== query)].slice(0, 10);
  persistState();
  renderSearchHistory();
}

function renderSearchHistory() {
  const container = document.getElementById("searchHistory");
  if (!container) return;
  if (!state.searchHistory.length) {
    container.hidden = true;
    return;
  }
  container.hidden = false;
  container.innerHTML = state.searchHistory.map((query) => `
    <button class="history-item" type="button" data-query="${escapeHtml(query)}">${escapeHtml(query)}</button>
  `).join("");
  container.querySelectorAll(".history-item").forEach((button) => {
    button.addEventListener("click", () => {
      els.searchInput.value = button.dataset.query;
      els.searchForm.dispatchEvent(new Event("submit"));
    });
  });
}

function escapeHtml(value) {
  const div = document.createElement("div");
  div.textContent = value;
  return div.innerHTML;
}

function renderAll() {
  renderSearchStatus();
  renderSearchResults(getSearchSeed());
  renderSearchHistory();
  renderNowPlaying();
  renderLibrary();
  renderPlaylists();
  renderDownloads();
  renderQueue();
}

function renderSearchStatus() {
  els.searchStatus.textContent = state.settings.apiKey ? "API connected" : "Sample mode";
}

function getSearchSeed() {
  const query = els.searchInput.value.trim().toLowerCase();
  if (!query) return sampleTracks.map((track) => findTrack(track.id) || track);
  return state.tracks.filter((track) => {
    return `${track.title} ${track.artist}`.toLowerCase().includes(query);
  });
}

function renderSearchResults(tracks) {
  const list = tracks.length ? tracks : sampleTracks;
  els.searchResults.replaceChildren(...list.map((track) => createTrackItem(track, "search")));
}

function renderLibrary() {
  document.querySelectorAll("[data-library-filter]").forEach((button) => {
    button.classList.toggle("active", button.dataset.libraryFilter === state.libraryFilter);
  });

  const tracks = libraryTracks();
  if (!tracks.length) {
    renderEmpty(els.libraryList, "Saved tracks, favorites, and recent plays will appear here.");
    return;
  }
  els.libraryList.replaceChildren(...tracks.map((track) => createTrackItem(track, "library")));
}

function libraryTracks() {
  if (state.libraryFilter === "favorites") {
    return state.favorites.map(findTrack).filter(Boolean);
  }
  if (state.libraryFilter === "recent") {
    return state.recent.map(findTrack).filter(Boolean);
  }
  const ids = new Set([...state.favorites, ...state.recent, ...state.localTrackIds]);
  state.playlists.forEach((playlist) => playlist.trackIds.forEach((id) => ids.add(id)));
  return Array.from(ids).map(findTrack).filter(Boolean);
}

function renderPlaylists() {
  ensureSelectedPlaylist();
  els.playlistList.replaceChildren(...state.playlists.map(createPlaylistButton));
  const selected = getSelectedPlaylist();
  els.selectedPlaylistTitle.textContent = selected ? selected.name : "Select a playlist";
  if (!selected || !selected.trackIds.length) {
    renderEmpty(els.playlistTracks, "Add tracks from search, library, or offline files.");
    return;
  }
  els.playlistTracks.replaceChildren(
    ...selected.trackIds.map(findTrack).filter(Boolean).map((track) => createTrackItem(track, "playlist"))
  );
}

function renderDownloads() {
  const tracks = state.localTrackIds.map(findTrack).filter(Boolean);
  if (!tracks.length) {
    renderEmpty(els.downloadList, "No offline files yet. Import audio that you own or are licensed to use.");
    return;
  }
  els.downloadList.replaceChildren(...tracks.map((track) => createTrackItem(track, "downloads")));
}

function renderQueue() {
  const tracks = state.queue.map(findTrack).filter(Boolean);
  if (!tracks.length) {
    renderEmpty(els.queueList, "Queue tracks from search, library, playlists, or offline files.");
    return;
  }
  els.queueList.replaceChildren(...tracks.map((track) => createTrackItem(track, "queue")));
}

function renderEmpty(container, message) {
  const node = document.createElement("div");
  node.className = "empty-state";
  node.textContent = message;
  container.replaceChildren(node);
}

function createTrackItem(track, context) {
  const item = document.createElement("article");
  item.className = "track-item";

  const art = createCover(track, "cover-art-small");
  const meta = document.createElement("div");
  meta.className = "track-meta";
  meta.innerHTML = `
    <span class="track-title"></span>
    <span class="track-subtitle"></span>
  `;
  meta.querySelector(".track-title").textContent = track.title;
  meta.querySelector(".track-subtitle").textContent = `${track.artist} · ${track.duration || sourceLabel(track)}`;

  const actions = document.createElement("div");
  actions.className = "track-actions";
  actions.append(
    actionButton("▶", "Play", () => playTrackFromContext(track.id, context)),
    actionButton("＋", "Add to queue", () => addToQueue(track.id)),
    actionButton(isFavorite(track.id) ? "♥" : "♡", "Favorite", () => toggleFavorite(track.id)),
    actionButton("▦", "Add to playlist", () => openPlaylistDialog(track.id))
  );

  if (context === "downloads") {
    actions.append(
      actionButton("⇩", "Download file", () => downloadLocalTrack(track.id)),
      actionButton("×", "Delete offline file", () => deleteLocalTrack(track.id))
    );
  } else if (track.source === "youtube") {
    actions.append(actionButton("↗", "Open in YouTube Music", () => openTrackLink(track)));
  }

  if (context === "playlist") {
    actions.append(
      actionButton("↑", "Move up", () => moveTrackInSelectedPlaylist(track.id, -1)),
      actionButton("↓", "Move down", () => moveTrackInSelectedPlaylist(track.id, 1)),
      actionButton("×", "Remove from playlist", () => removeFromSelectedPlaylist(track.id))
    );
  }
  if (context === "queue") {
    actions.append(
      actionButton("↑", "Move up", () => moveQueueTrack(track.id, -1)),
      actionButton("↓", "Move down", () => moveQueueTrack(track.id, 1)),
      actionButton("×", "Remove from queue", () => removeFromQueue(track.id))
    );
  }

  item.append(art, meta, actions);
  return item;
}

function createCover(track, sizeClass) {
  const cover = document.createElement("div");
  cover.className = `cover-art ${sizeClass}`;
  cover.dataset.art = track.art || ART_KEYS[Math.abs(hashCode(track.id)) % ART_KEYS.length];
  if (track.thumbnail) {
    const img = document.createElement("img");
    img.src = track.thumbnail;
    img.alt = "";
    img.loading = "lazy";
    img.referrerPolicy = "no-referrer";
    cover.append(img);
  }
  return cover;
}

function actionButton(symbol, label, handler) {
  const button = document.createElement("button");
  button.className = "icon-button";
  button.type = "button";
  button.textContent = symbol;
  button.title = label;
  button.setAttribute("aria-label", label);
  button.addEventListener("click", handler);
  return button;
}

function sourceLabel(track) {
  return track.source === "local" ? "Offline" : "YouTube";
}

function hashCode(value) {
  return String(value).split("").reduce((acc, char) => ((acc << 5) - acc + char.charCodeAt(0)) | 0, 0);
}

function playTrackFromContext(trackId, context) {
  if (context === "playlist") {
    const playlist = getSelectedPlaylist();
    if (playlist?.trackIds.includes(trackId)) {
      state.queue = rotateToTrack(playlist.trackIds.filter((id) => findTrack(id)), trackId);
      persistState();
      renderQueue();
      playTrack(trackId, { keepQueue: true });
      return;
    }
  }

  playTrack(trackId, { keepQueue: context === "queue" });
}

async function playTrack(trackId, options = {}) {
  const track = findTrack(trackId);
  if (!track) return;

  state.currentTrackId = trackId;
  addRecent(trackId);
  if (!options.keepQueue && !state.queue.includes(trackId)) state.queue.unshift(trackId);
  persistState();
  renderAll();

  if (track.source === "local") {
    await playLocalTrack(track);
  } else {
    playYouTubeTrack(track);
  }
}

async function playLocalTrack(track) {
  els.youtubeFrame.src = "";
  els.embedPanel.hidden = true;
  revokeCurrentObjectUrl();
  try {
    const record = await getLocalFile(track.fileId);
    if (!record?.blob) {
      showToast("Local file is missing");
      return;
    }
    currentObjectUrl = URL.createObjectURL(record.blob);
    els.audioElement.src = currentObjectUrl;
    try {
      await els.audioElement.play();
    } catch {
      showToast("Tap play to start audio");
    }
  } catch (error) {
    console.error("Failed to load local file:", error);
    showToast("Failed to load local file");
  }
}

function playYouTubeTrack(track) {
  els.audioElement.pause();
  els.audioElement.removeAttribute("src");
  revokeCurrentObjectUrl();
  const params = new URLSearchParams({
    autoplay: "1",
    enablejsapi: "1",
    origin: window.location.origin,
    rel: "0"
  });
  const upcomingIds = youtubeIdsFromQueue(track.id).slice(1);
  if (upcomingIds.length) params.set("playlist", upcomingIds.join(","));
  els.youtubeFrame.src = `https://www.youtube.com/embed/${encodeURIComponent(track.youtubeId)}?${params.toString()}`;
  els.embedPanel.hidden = false;
  els.playPauseButton.textContent = "▶";
  els.progressBar.style.width = "0%";
  showToast("Use the visible YouTube player controls");
}

function youtubeIdsFromQueue(currentTrackId) {
  const currentIndex = state.queue.indexOf(currentTrackId);
  const orderedIds = currentIndex >= 0 ? state.queue.slice(currentIndex) : [currentTrackId];
  return orderedIds
    .map(findTrack)
    .filter((item) => item?.source === "youtube" && item.youtubeId)
    .map((item) => item.youtubeId);
}

function togglePlayback() {
  const track = findTrack(state.currentTrackId);
  if (!track) {
    const first = state.queue[0] || state.tracks[0]?.id;
    if (first) playTrack(first);
    return;
  }
  if (track.source === "youtube") {
    els.embedPanel.hidden = false;
    showToast("YouTube playback uses the embedded player controls");
    return;
  }
  if (els.audioElement.paused) {
    els.audioElement.play().catch(() => showToast("Unable to start audio"));
  } else {
    els.audioElement.pause();
  }
}

function playNext() {
  const currentIndex = state.queue.indexOf(state.currentTrackId);
  const nextId = state.queue[currentIndex + 1] || state.queue[0];
  if (nextId) playTrack(nextId);
}

function playPrevious() {
  const currentIndex = state.queue.indexOf(state.currentTrackId);
  const previousId = state.queue[currentIndex - 1] || state.queue[state.queue.length - 1];
  if (previousId) playTrack(previousId);
}

function renderNowPlaying() {
  const track = findTrack(state.currentTrackId);
  els.nowCover.replaceWith(createCover(track || { id: "empty", art: "teal" }, "cover-art-large"));
  els.nowCover = document.querySelector(".hero-player .cover-art-large");
  els.dockCover.replaceWith(createCover(track || { id: "dock", art: "teal" }, "cover-art-small"));
  els.dockCover = document.querySelector(".player-dock .cover-art-small");
  els.nowTitle.textContent = track?.title || "Nothing playing";
  els.nowArtist.textContent = track ? `${track.artist} · ${sourceLabel(track)}` : "Pick a track to start.";
  els.dockTitle.textContent = track?.title || "No track selected";
  els.dockArtist.textContent = track ? `${track.artist} · ${sourceLabel(track)}` : "Queue is empty";
}

function addToQueue(trackId) {
  if (!findTrack(trackId)) return;
  state.queue = state.queue.filter((id) => id !== trackId).concat(trackId);
  persistState();
  renderQueue();
  showToast("Added to queue");
}

async function playSelectedPlaylist() {
  const playlist = getSelectedPlaylist();
  if (!playlist) {
    showToast("Select a playlist first");
    return;
  }
  const playableIds = playlist.trackIds.filter((id) => findTrack(id));
  if (!playableIds.length) {
    showToast("Add tracks to this playlist first");
    return;
  }
  state.queue = [...playableIds];
  persistState();
  renderAll();
  await playTrack(playableIds[0], { keepQueue: true });
}

function removeFromQueue(trackId) {
  state.queue = state.queue.filter((id) => id !== trackId);
  persistState();
  renderQueue();
}

function moveQueueTrack(trackId, direction) {
  const moved = moveId(state.queue, trackId, direction);
  if (!moved) return;
  state.queue = moved;
  persistState();
  renderQueue();
}

function clearQueue() {
  state.queue = [];
  persistState();
  renderQueue();
  showToast("Queue cleared");
}

function shuffleLibrary() {
  const tracks = libraryTracks();
  const ids = tracks.map((track) => track.id);
  if (!ids.length) {
    showToast("Add music before shuffling");
    return;
  }
  state.queue = shuffle(ids);
  persistState();
  renderQueue();
  playTrack(state.queue[0]);
}

function shuffle(items) {
  const copy = [...items];
  for (let i = copy.length - 1; i > 0; i -= 1) {
    const j = Math.floor(Math.random() * (i + 1));
    [copy[i], copy[j]] = [copy[j], copy[i]];
  }
  return copy;
}

function rotateToTrack(trackIds, trackId) {
  const index = trackIds.indexOf(trackId);
  if (index < 0) return trackIds;
  return trackIds.slice(index).concat(trackIds.slice(0, index));
}

function toggleFavorite(trackId) {
  if (!findTrack(trackId)) return;
  if (isFavorite(trackId)) {
    state.favorites = state.favorites.filter((id) => id !== trackId);
  } else {
    state.favorites.unshift(trackId);
  }
  persistState();
  renderAll();
}

function isFavorite(trackId) {
  return state.favorites.includes(trackId);
}

function addRecent(trackId) {
  state.recent = [trackId, ...state.recent.filter((id) => id !== trackId)].slice(0, 40);
}

function clearRecent() {
  state.recent = [];
  persistState();
  renderLibrary();
  showToast("Recent plays cleared");
}

function createPlaylist(event) {
  event.preventDefault();
  const name = els.playlistName.value.trim();
  if (!name) {
    showToast("Name the playlist");
    return;
  }
  const playlist = {
    id: newId("playlist"),
    name,
    trackIds: [],
    createdAt: Date.now()
  };
  state.playlists.unshift(playlist);
  state.selectedPlaylistId = playlist.id;
  els.playlistName.value = "";
  persistState();
  renderPlaylists();
  showToast("Playlist created");
}

function createPlaylistButton(playlist) {
  const button = document.createElement("button");
  button.className = "playlist-button";
  button.type = "button";
  button.classList.toggle("active", playlist.id === state.selectedPlaylistId);
  button.innerHTML = "<strong></strong><span></span>";
  button.querySelector("strong").textContent = playlist.name;
  button.querySelector("span").textContent = `${playlist.trackIds.length} tracks`;
  button.addEventListener("click", () => {
    state.selectedPlaylistId = playlist.id;
    persistState();
    renderPlaylists();
  });
  return button;
}

function ensureSelectedPlaylist() {
  if (!state.playlists.some((playlist) => playlist.id === state.selectedPlaylistId)) {
    state.selectedPlaylistId = state.playlists[0]?.id || "";
  }
}

function getSelectedPlaylist() {
  return state.playlists.find((playlist) => playlist.id === state.selectedPlaylistId);
}

function openPlaylistDialog(trackId) {
  pendingDialogTrackId = trackId;
  const track = findTrack(trackId);
  els.dialogTrackTitle.textContent = track?.title || "Track";
  els.dialogPlaylistOptions.replaceChildren(...state.playlists.map((playlist) => {
    const button = document.createElement("button");
    button.type = "button";
    const contains = playlist.trackIds.includes(trackId);
    button.innerHTML = "<span></span><strong></strong>";
    button.querySelector("span").textContent = playlist.name;
    button.querySelector("strong").textContent = contains ? "Added" : "Add";
    button.disabled = contains;
    button.addEventListener("click", () => addTrackToPlaylist(playlist.id, pendingDialogTrackId));
    return button;
  }));
  if (els.playlistDialog.open) return;
  if (typeof els.playlistDialog.showModal === "function") {
    els.playlistDialog.showModal();
  } else {
    showToast("Playlist dialog is not supported in this browser");
  }
}

function addTrackToPlaylist(playlistId, trackId) {
  const playlist = state.playlists.find((item) => item.id === playlistId);
  if (!playlist || !findTrack(trackId)) return;
  if (!playlist.trackIds.includes(trackId)) playlist.trackIds.push(trackId);
  state.selectedPlaylistId = playlistId;
  persistState();
  renderAll();
  openPlaylistDialog(trackId);
  showToast("Added to playlist");
}

function removeFromSelectedPlaylist(trackId) {
  const playlist = getSelectedPlaylist();
  if (!playlist) return;
  playlist.trackIds = playlist.trackIds.filter((id) => id !== trackId);
  persistState();
  renderPlaylists();
}

function moveTrackInSelectedPlaylist(trackId, direction) {
  const playlist = getSelectedPlaylist();
  if (!playlist) return;
  const moved = moveId(playlist.trackIds, trackId, direction);
  if (!moved) return;
  playlist.trackIds = moved;
  persistState();
  renderPlaylists();
}

function openRenamePlaylistDialog() {
  const playlist = getSelectedPlaylist();
  if (!playlist) {
    showToast("Select a playlist first");
    return;
  }
  els.renamePlaylistTitle.textContent = playlist.name;
  els.renamePlaylistInput.value = playlist.name;
  if (typeof els.renamePlaylistDialog.showModal === "function") {
    els.renamePlaylistDialog.showModal();
  } else {
    showToast("Rename dialog is not supported in this browser");
  }
}

function confirmRenamePlaylist() {
  const playlist = getSelectedPlaylist();
  if (!playlist) return;
  const newName = els.renamePlaylistInput.value.trim();
  if (!newName) {
    showToast("Enter a playlist name");
    return;
  }
  playlist.name = newName;
  persistState();
  renderPlaylists();
  els.renamePlaylistDialog.close();
  showToast("Playlist renamed");
}

function deleteSelectedPlaylist() {
  const playlist = getSelectedPlaylist();
  if (!playlist) {
    showToast("Select a playlist first");
    return;
  }
  if (!confirm(`Delete "${playlist.name}"?`)) return;
  state.playlists = state.playlists.filter((p) => p.id !== playlist.id);
  state.selectedPlaylistId = state.playlists[0]?.id || "";
  persistState();
  renderPlaylists();
  showToast("Playlist deleted");
}

function moveId(ids, id, direction) {
  const index = ids.indexOf(id);
  const targetIndex = index + direction;
  if (index < 0 || targetIndex < 0 || targetIndex >= ids.length) return null;
  const moved = [...ids];
  [moved[index], moved[targetIndex]] = [moved[targetIndex], moved[index]];
  return moved;
}

function exportSelectedPlaylist() {
  const playlist = getSelectedPlaylist();
  if (!playlist) {
    showToast("Select a playlist first");
    return;
  }
  const payload = {
    name: playlist.name,
    exportedAt: new Date().toISOString(),
    tracks: playlist.trackIds.map(findTrack).filter(Boolean).map((track) => ({
      title: track.title,
      artist: track.artist,
      source: track.source,
      url: track.url || "",
      youtubeId: track.youtubeId || "",
      localFileName: track.fileName || ""
    }))
  };
  downloadBlob(`${slugify(playlist.name)}.playlist.json`, new Blob([JSON.stringify(payload, null, 2)], { type: "application/json" }));
}

async function importLocalFiles(event) {
  const files = Array.from(event.target.files || []);
  if (!files.length) return;
  const imported = [];
  const errors = [];

  for (const file of files) {
    if (!file.type.startsWith("audio/")) continue;
    const fileId = newId("file");
    const track = {
      id: newId("local"),
      source: "local",
      fileId,
      fileName: file.name,
      title: file.name.replace(/\.[^.]+$/, ""),
      artist: "Device file",
      duration: formatSize(file.size),
      thumbnail: "",
      url: "",
      art: ART_KEYS[Math.abs(hashCode(file.name)) % ART_KEYS.length]
    };
    try {
      await putLocalFile({ id: fileId, blob: file, name: file.name, type: file.type, size: file.size, createdAt: Date.now() });
      imported.push(track);
    } catch (error) {
      console.error("Failed to import file:", file.name, error);
      errors.push(file.name);
    }
  }

  if (imported.length) {
    upsertTracks(imported);
    state.localTrackIds.unshift(...imported.map((track) => track.id));
    state.localTrackIds = Array.from(new Set(state.localTrackIds));
    persistState();
    renderAll();
    showToast(`${imported.length} audio file${imported.length === 1 ? "" : "s"} imported`);
  }

  if (errors.length) {
    showToast(`Failed to import ${errors.length} file${errors.length === 1 ? "" : "s"}`);
  }

  event.target.value = "";
}

function formatSize(bytes) {
  if (!bytes) return "Offline";
  const mb = bytes / (1024 * 1024);
  return `${mb.toFixed(mb >= 10 ? 0 : 1)} MB`;
}

async function downloadLocalTrack(trackId) {
  const track = findTrack(trackId);
  if (!track || track.source !== "local") {
    showToast("YouTube tracks cannot be downloaded here");
    return;
  }
  try {
    const record = await getLocalFile(track.fileId);
    if (!record?.blob) {
      showToast("Local file is missing");
      return;
    }
    downloadBlob(record.name || `${track.title}.audio`, record.blob);
  } catch (error) {
    console.error("Failed to download file:", error);
    showToast("Failed to download file");
  }
}

async function deleteLocalTrack(trackId) {
  const track = findTrack(trackId);
  if (!track || track.source !== "local") return;
  try {
    await deleteLocalFile(track.fileId);
    state.localTrackIds = state.localTrackIds.filter((id) => id !== trackId);
    state.favorites = state.favorites.filter((id) => id !== trackId);
    state.recent = state.recent.filter((id) => id !== trackId);
    state.queue = state.queue.filter((id) => id !== trackId);
    state.playlists.forEach((playlist) => {
      playlist.trackIds = playlist.trackIds.filter((id) => id !== trackId);
    });
    state.tracks = state.tracks.filter((item) => item.id !== trackId);
    if (state.currentTrackId === trackId) {
      els.audioElement.pause();
      els.audioElement.removeAttribute("src");
      revokeCurrentObjectUrl();
      state.currentTrackId = "";
    }
    persistState();
    renderAll();
    showToast("Offline file removed");
  } catch (error) {
    console.error("Failed to delete file:", error);
    showToast("Failed to delete file");
  }
}

function downloadBlob(fileName, blob) {
  const url = URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.href = url;
  link.download = fileName;
  document.body.append(link);
  link.click();
  link.remove();
  setTimeout(() => URL.revokeObjectURL(url), 1000);
}

function openTrackLink(track) {
  if (!track.url) {
    showToast("No link for this track");
    return;
  }
  window.open(track.url, "_blank", "noopener,noreferrer");
}

function findTrack(trackId) {
  return state.tracks.find((track) => track.id === trackId);
}

function startProgressTimer() {
  if (progressTimer) return;
  progressTimer = window.setInterval(updateProgress, 1000);
}

function stopProgressTimer() {
  window.clearInterval(progressTimer);
  progressTimer = 0;
}

function updateProgress() {
  const audio = els.audioElement;
  if (!Number.isFinite(audio.duration) || audio.duration <= 0) {
    els.progressBar.style.width = "0%";
    return;
  }
  const percent = Math.min(100, (audio.currentTime / audio.duration) * 100);
  els.progressBar.style.width = `${percent}%`;
}

function revokeCurrentObjectUrl() {
  if (!currentObjectUrl) return;
  URL.revokeObjectURL(currentObjectUrl);
  currentObjectUrl = "";
}

function slugify(value) {
  const slug = String(value).toLowerCase().replace(/[^a-z0-9]+/g, "-").replace(/(^-|-$)/g, "");
  return slug || "playlist";
}

function newId(prefix) {
  const random = globalThis.crypto?.randomUUID?.() || `${Date.now()}-${Math.random().toString(16).slice(2)}`;
  return `${prefix}-${random}`;
}

function showToast(message) {
  els.toast.textContent = message;
  els.toast.classList.add("show");
  window.clearTimeout(showToast.timer);
  showToast.timer = window.setTimeout(() => els.toast.classList.remove("show"), 2400);
}

function openDb() {
  if (dbPromise) return dbPromise;
  dbPromise = new Promise((resolve, reject) => {
    const request = indexedDB.open(DB_NAME, DB_VERSION);
    request.onupgradeneeded = () => {
      const db = request.result;
      if (!db.objectStoreNames.contains("files")) {
        db.createObjectStore("files", { keyPath: "id" });
      }
    };
    request.onsuccess = () => resolve(request.result);
    request.onerror = () => reject(request.error);
  });
  return dbPromise;
}

async function putLocalFile(record) {
  const db = await openDb();
  return new Promise((resolve, reject) => {
    const tx = db.transaction("files", "readwrite");
    tx.objectStore("files").put(record);
    tx.oncomplete = () => resolve();
    tx.onerror = () => reject(tx.error);
  });
}

async function getLocalFile(id) {
  const db = await openDb();
  return new Promise((resolve, reject) => {
    const tx = db.transaction("files", "readonly");
    const request = tx.objectStore("files").get(id);
    request.onsuccess = () => resolve(request.result);
    request.onerror = () => reject(request.error);
  });
}

async function deleteLocalFile(id) {
  const db = await openDb();
  return new Promise((resolve, reject) => {
    const tx = db.transaction("files", "readwrite");
    tx.objectStore("files").delete(id);
    tx.oncomplete = () => resolve();
    tx.onerror = () => reject(tx.error);
  });
}

function registerServiceWorker() {
  if (!("serviceWorker" in navigator)) return;
  navigator.serviceWorker.register("sw.js").catch(() => {
    /* The app works without a service worker in local test environments. */
  });
}
