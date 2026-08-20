"use strict";

const CACHE_NAME = "pulse-music-shell-v2";
const SHELL_FILES = [
  "./",
  "index.html",
  "styles.css",
  "app.js",
  "manifest.webmanifest",
  "assets/icon.svg"
];

self.addEventListener("install", (event) => {
  event.waitUntil(
    caches.open(CACHE_NAME).then((cache) => cache.addAll(SHELL_FILES))
  );
  self.skipWaiting();
});

self.addEventListener("activate", (event) => {
  event.waitUntil(
    caches.keys().then((keys) => Promise.all(
      keys.filter((key) => key !== CACHE_NAME).map((key) => caches.delete(key))
    ))
  );
  self.clients.claim();
});

self.addEventListener("fetch", (event) => {
  const url = new URL(event.request.url);
  
  if (url.origin !== self.location.origin) return;
  
  event.respondWith(
    caches.match(event.request).then((cached) => {
      if (cached) return cached;
      
      return fetch(event.request).then((response) => {
        if (response.ok) {
          const responseClone = response.clone();
          caches.open(CACHE_NAME).then((cache) => {
            cache.put(event.request, responseClone);
          });
        }
        return response;
      }).catch(() => {
        if (event.request.mode === "navigate") {
          return caches.match("index.html");
        }
        return new Response("Offline", { status: 503, statusText: "Service Unavailable" });
      });
    })
  );
});
