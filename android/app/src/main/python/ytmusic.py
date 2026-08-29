from ytmusicapi import YTMusic
from ytmusicapi.setup import setup_oauth


class PulseYTMusic:
    def __init__(self, auth_file=None):
        if auth_file:
            self.yt = YTMusic(auth_file)
        else:
            self.yt = YTMusic()

    def search(self, query, search_filter=None):
        import json
        try:
            kwargs = {}
            if search_filter:
                kwargs["filter"] = search_filter
            results = self.yt.search(query, **kwargs)
            return json.dumps(self._normalize_search(results), default=str, ensure_ascii=False)
        except Exception as e:
            return json.dumps({"error": str(e)})

    def _normalize_search(self, results):
        normalized = []
        if not isinstance(results, list):
            return normalized
        for r in results:
            if not isinstance(r, dict):
                continue
            rtype = r.get("resultType") or r.get("type")
            if rtype not in ("song", "video", "album", "artist"):
                continue
            video_id = r.get("videoId")
            browse_id = r.get("browseId")
            artists = r.get("artists") or []
            if isinstance(artists, list) and artists:
                artist = ", ".join(a.get("name") for a in artists if isinstance(a, dict) and a.get("name"))
            else:
                artist = r.get("artist")
            album_name = None
            album_field = r.get("album")
            if isinstance(album_field, dict):
                album_name = album_field.get("name")
            elif isinstance(album_field, str):
                album_name = album_field
            thumb = self._first_thumb(r.get("thumbnails"))
            dur = r.get("duration")
            dur_secs = r.get("duration_seconds")
            if dur_secs:
                duration_ms = int(dur_secs) * 1000
            elif dur and ":" in dur:
                parts = dur.split(":")
                try:
                    parts = [int(p) for p in parts]
                    if len(parts) == 2:
                        duration_ms = parts[0] * 60000 + parts[1] * 1000
                    elif len(parts) == 3:
                        duration_ms = parts[0] * 3600000 + parts[1] * 60000 + parts[2] * 1000
                    else:
                        duration_ms = None
                except Exception:
                    duration_ms = None
            else:
                duration_ms = None
            title = r.get("title") or r.get("name")
            normalized.append({
                "id": ("yt-" + video_id) if video_id else None,
                "youtubeId": video_id,
                "browseId": browse_id,
                "title": title,
                "artist": artist,
                "album": album_name,
                "duration": dur,
                "durationMs": duration_ms,
                "thumbnail": thumb,
                "url": ("https://music.youtube.com/watch?v=" + video_id) if video_id else None,
                "resultType": rtype,
                "type": rtype,
            })
        return normalized

    def _first_thumb(self, thumbs):
        if isinstance(thumbs, list) and thumbs:
            last = thumbs[-1]
            if isinstance(last, dict):
                return last.get("url")
        return None


    def search_suggestions(self, query):
        import json
        try:
            results = self.yt.get_search_suggestions(query)
            return json.dumps(results, default=str, ensure_ascii=False)
        except Exception as e:
            return json.dumps({"error": str(e)})

    def get_song(self, video_id):
        import json
        try:
            result = self.yt.get_song(video_id)
            return json.dumps(result, default=str, ensure_ascii=False)
        except Exception as e:
            return json.dumps({"error": str(e)})

    def get_stream_url(self, video_id):
        import json
        video_id = str(video_id)
        try:
            url = "https://www.youtube.com/watch?v=" + video_id
            from pytubefix import YouTube
            for kwargs in ({"client": "ANDROID"}, {"client": "WEB"}, {}):
                try:
                    yt = YouTube(url, **kwargs)
                    stream = yt.streams.filter(only_audio=True).first()
                    if stream is None:
                        stream = yt.streams.first()
                    if stream is not None and stream.url:
                        return json.dumps({
                            "videoId": video_id,
                            "streamUrl": stream.url,
                            "mime": getattr(stream, 'mime_type', None),
                            "abr": getattr(stream, 'abr', None),
                        })
                except Exception as e:
                    continue
        except Exception:
            pass

        try:
            stream_url, mime = self._inner_tube_stream(video_id)
            if stream_url:
                return json.dumps({
                    "videoId": video_id,
                    "streamUrl": stream_url,
                    "mime": mime,
                })
        except Exception:
            pass

        return json.dumps({"error": "could not extract stream url"})

    def _inner_tube_stream(self, video_id):
        import requests
        endpoint = "https://www.youtube.com/youtubei/v1/player"
        headers = {
            "Content-Type": "application/json",
            "User-Agent": "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36",
        }
        clients = [
            ("ANDROID", "19.09.37"),
            ("WEB", "2.20240801.01.00"),
            ("IOS", "19.09.3"),
        ]
        for name, ver in clients:
            payload = {
                "videoId": video_id,
                "contentCheckOk": True,
                "racyCheckOk": True,
                "context": {
                    "client": {"clientName": name, "clientVersion": ver}
                },
            }
            try:
                r = requests.post(endpoint, json=payload, headers=headers, timeout=30)
                data = r.json()
                sd = data.get("streamingData") or {}
                formats = (sd.get("adaptiveFormats") or []) + (sd.get("formats") or [])
                audio = [f for f in formats if (f.get("mimeType") or "").startswith("audio/")]
                if not audio:
                    continue
                best = sorted(audio, key=lambda f: f.get("bitrate") or 0, reverse=True)[0]
                su = best.get("url")
                if not su:
                    continue
                return su, best.get("mimeType")
            except Exception:
                continue
        return None, None


    def get_lyrics(self, video_id):
        import json
        try:
            result = self.yt.get_lyrics(video_id)
            return json.dumps(result, default=str, ensure_ascii=False)
        except Exception as e:
            return json.dumps({"error": str(e)})

    def get_watch_playlist(self, video_id, limit=25):
        import json
        try:
            result = self.yt.get_watch_playlist(video_id, limit=limit)
            return json.dumps(result, default=str, ensure_ascii=False)
        except Exception as e:
            return json.dumps({"error": str(e)})

    def get_album(self, album_browse_id):
        import json
        try:
            result = self.yt.get_album(album_browse_id)
            return json.dumps(result, default=str, ensure_ascii=False)
        except Exception as e:
            return json.dumps({"error": str(e)})

    def get_artist(self, artist_id):
        import json
        try:
            result = self.yt.get_artist(artist_id)
            return json.dumps(result, default=str, ensure_ascii=False)
        except Exception as e:
            return json.dumps({"error": str(e)})

    def get_artist_albums(self, artist_id, album_type=None):
        import json
        try:
            kwargs = {}
            if album_type:
                kwargs["album_type"] = album_type
            result = self.yt.get_artist_albums(artist_id, **kwargs)
            return json.dumps(result, default=str, ensure_ascii=False)
        except Exception as e:
            return json.dumps({"error": str(e)})

    def get_home(self):
        import json
        try:
            result = self.yt.get_home()
            return json.dumps(result, default=str, ensure_ascii=False)
        except Exception as e:
            return json.dumps({"error": str(e)})

    def get_charts(self, country=None):
        import json
        try:
            kwargs = {}
            if country:
                kwargs["country"] = country
            result = self.yt.get_charts(**kwargs)
            return json.dumps(result, default=str, ensure_ascii=False)
        except Exception as e:
            return json.dumps({"error": str(e)})

    def get_mood_categories(self):
        import json
        try:
            result = self.yt.get_mood_categories()
            return json.dumps(result, default=str, ensure_ascii=False)
        except Exception as e:
            return json.dumps({"error": str(e)})

    def get_mood_playlists(self, category):
        import json
        try:
            result = self.yt.get_mood_playlists(category)
            return json.dumps(result, default=str, ensure_ascii=False)
        except Exception as e:
            return json.dumps({"error": str(e)})

    def get_playlist(self, playlist_id, limit=None):
        import json
        try:
            kwargs = {}
            if limit:
                kwargs["limit"] = limit
            result = self.yt.get_playlist(playlist_id, **kwargs)
            return json.dumps(result, default=str, ensure_ascii=False)
        except Exception as e:
            return json.dumps({"error": str(e)})

    # === Authenticated methods ===

    def init_oauth(self, client_id, client_secret):
        import json
        try:
            setup_oauth(
                client_id=client_id,
                client_secret=client_secret,
                filepath="oauth.json"
            )
            self.yt = YTMusic("oauth.json")
            return json.dumps({"success": True})
        except Exception as e:
            return json.dumps({"error": str(e)})

    def get_library_playlists(self, limit=25):
        import json
        try:
            result = self.yt.get_library_playlists(limit=limit)
            return json.dumps(result, default=str, ensure_ascii=False)
        except Exception as e:
            return json.dumps({"error": str(e)})

    def get_library_songs(self, limit=25):
        import json
        try:
            result = self.yt.get_library_songs(limit=limit)
            return json.dumps(result, default=str, ensure_ascii=False)
        except Exception as e:
            return json.dumps({"error": str(e)})

    def get_history(self):
        import json
        try:
            result = self.yt.get_history()
            return json.dumps(result, default=str, ensure_ascii=False)
        except Exception as e:
            return json.dumps({"error": str(e)})

    def rate_song(self, video_id, rating="LIKE"):
        import json
        try:
            result = self.yt.rate_song(video_id, rating)
            return json.dumps({"success": result})
        except Exception as e:
            return json.dumps({"error": str(e)})

    def create_playlist(self, title, description="", privacy_status="PRIVATE"):
        import json
        try:
            result = self.yt.create_playlist(title, description, privacy_status)
            return json.dumps({"playlist_id": result})
        except Exception as e:
            return json.dumps({"error": str(e)})

    def add_playlist_items(self, playlist_id, video_ids):
        import json
        try:
            if isinstance(video_ids, str):
                video_ids = json.loads(video_ids)
            result = self.yt.add_playlist_items(playlist_id, video_ids)
            return json.dumps({"success": result})
        except Exception as e:
            return json.dumps({"error": str(e)})

    def remove_playlist_items(self, playlist_id, video_ids):
        import json
        try:
            if isinstance(video_ids, str):
                video_ids = json.loads(video_ids)
            result = self.yt.remove_playlist_items(playlist_id, video_ids)
            return json.dumps({"success": result})
        except Exception as e:
            return json.dumps({"error": str(e)})

    def delete_playlist(self, playlist_id):
        import json
        try:
            result = self.yt.delete_playlist(playlist_id)
            return json.dumps({"success": result})
        except Exception as e:
            return json.dumps({"error": str(e)})

    def get_account_info(self):
        import json
        try:
            result = self.yt.get_account_info()
            return json.dumps(result, default=str, ensure_ascii=False)
        except Exception as e:
            return json.dumps({"error": str(e)})
