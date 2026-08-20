"use strict";

const tests = [];
let passed = 0;
let failed = 0;

function test(name, fn) {
  tests.push({ name, fn });
}

function assert(condition, message) {
  if (!condition) throw new Error(message || "Assertion failed");
}

function assertEquals(actual, expected, message) {
  if (actual !== expected) {
    throw new Error(message || `Expected ${expected}, got ${actual}`);
  }
}

function assertArrayEquals(actual, expected, message) {
  if (JSON.stringify(actual) !== JSON.stringify(expected)) {
    throw new Error(message || `Expected ${JSON.stringify(expected)}, got ${JSON.stringify(actual)}`);
  }
}

function runTests() {
  const results = document.getElementById("testResults");
  results.innerHTML = "";
  
  for (const { name, fn } of tests) {
    try {
      fn();
      passed++;
      results.innerHTML += `<div class="test-pass">✓ ${name}</div>`;
    } catch (error) {
      failed++;
      results.innerHTML += `<div class="test-fail">✗ ${name}: ${error.message}</div>`;
    }
  }
  
  results.innerHTML = `<div class="test-summary">${passed} passed, ${failed} failed</div>` + results.innerHTML;
}

test("hashCode produces consistent results", () => {
  const hash1 = hashCode("test");
  const hash2 = hashCode("test");
  assertEquals(hash1, hash2, "Same input should produce same hash");
});

test("hashCode produces different results for different inputs", () => {
  const hash1 = hashCode("test1");
  const hash2 = hashCode("test2");
  assert(hash1 !== hash2, "Different inputs should produce different hashes");
});

test("slugify creates valid slugs", () => {
  assertEquals(slugify("My Playlist"), "my-playlist");
  assertEquals(slugify("  spaces  "), "spaces");
  assertEquals(slugify("Special!@#Chars"), "specialchars");
  assertEquals(slugify(""), "playlist");
});

test("normalizeRegion formats region codes", () => {
  assertEquals(normalizeRegion("us"), "US");
  assertEquals(normalizeRegion("  gb  "), "GB");
  assertEquals(normalizeRegion("a"), "US");
  assertEquals(normalizeRegion(""), "US");
});

test("formatSize formats bytes correctly", () => {
  assertEquals(formatSize(0), "Offline");
  assertEquals(formatSize(1024 * 1024), "1.0 MB");
  assertEquals(formatSize(10 * 1024 * 1024), "10 MB");
  assertEquals(formatSize(1024 * 1024 * 1024), "1024 MB");
});

test("moveId moves items correctly", () => {
  const ids = ["a", "b", "c", "d"];
  assertArrayEquals(moveId(ids, "b", -1), ["b", "a", "c", "d"]);
  assertArrayEquals(moveId(ids, "b", 1), ["a", "c", "b", "d"]);
  assertEquals(moveId(ids, "a", -1), null);
  assertEquals(moveId(ids, "d", 1), null);
  assertEquals(moveId(ids, "x", 1), null);
});

test("shuffle maintains array length", () => {
  const items = [1, 2, 3, 4, 5];
  const shuffled = shuffle(items);
  assertEquals(shuffled.length, items.length);
  assertArrayEquals(items.sort(), shuffled.sort());
});

test("rotateToTrack rotates correctly", () => {
  const ids = ["a", "b", "c", "d"];
  assertArrayEquals(rotateToTrack(ids, "c"), ["c", "d", "a", "b"]);
  assertArrayEquals(rotateToTrack(ids, "a"), ["a", "b", "c", "d"]);
  assertArrayEquals(rotateToTrack(ids, "x"), ["a", "b", "c", "d"]);
});

test("decodeHtml decodes HTML entities", () => {
  assertEquals(decodeHtml("&amp;"), "&");
  assertEquals(decodeHtml("&lt;"), "<");
  assertEquals(decodeHtml("test"), "test");
});

test("newId generates unique IDs", () => {
  const id1 = newId("test");
  const id2 = newId("test");
  assert(id1 !== id2, "IDs should be unique");
  assert(id1.startsWith("test-"), "ID should have prefix");
});

test("ART_KEYS has correct values", () => {
  assertArrayEquals(ART_KEYS, ["teal", "gold", "plum", "coral"]);
});

test("sampleTracks has correct structure", () => {
  assert(sampleTracks.length > 0, "Should have sample tracks");
  sampleTracks.forEach((track) => {
    assert(track.id, "Track should have id");
    assert(track.title, "Track should have title");
    assert(track.artist, "Track should have artist");
    assert(track.source, "Track should have source");
  });
});

test("initialState has required fields", () => {
  assert(initialState.settings, "Should have settings");
  assert(Array.isArray(initialState.tracks), "Should have tracks array");
  assert(Array.isArray(initialState.favorites), "Should have favorites array");
  assert(Array.isArray(initialState.recent), "Should have recent array");
  assert(Array.isArray(initialState.queue), "Should have queue array");
  assert(Array.isArray(initialState.playlists), "Should have playlists array");
  assert(Array.isArray(initialState.searchHistory), "Should have searchHistory array");
});

if (typeof document !== "undefined") {
  document.addEventListener("DOMContentLoaded", runTests);
}

if (typeof module !== "undefined" && module.exports) {
  module.exports = { test, assert, assertEquals, assertArrayEquals, runTests, tests, passed, failed };
}