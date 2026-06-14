# Echo Music v5.1.91 — Maintenance Update

> This is a maintenance release focused on playback stability and reliability improvements. The only new addition is the Service Uptime checker.

---

## Playback Fixes

- Resolved multiple playback errors (1004, 2008, 3001) by improving network retry logic and securing AudioTrack recovery.
- Fixed silent playback caused by mid-stream container changes — stream quality is now locked for the duration of the currently playing track.
- Fixed an ExoPlayer crash that occurred when a stream's container format fell back to a format different from what was cached.
- Fixed seek errors (error 2008 — IO read position out of range) when skipping to a segment within a downloaded song.
- Fixed IO error 2000 (unspecified IO error) by properly recovering the audio stream and cache state.

## Offline & Cache Fixes

- Fixed downloaded songs incorrectly requiring an internet connection due to a cache metadata mismatch — fully downloaded tracks now play offline as expected.
- Fixed audio fallback toast notifications appearing only once per app session instead of triggering on each relevant event.

## UI Fix

- Removed the volume percentage indicator from the Phone Speaker row in the Audio Device selector.

---

## New Feature

- **Service Uptime Checker** — Available under Content Settings, this tool allows users to monitor the real-time status of YouTube Music, JioSaavn, and Qobuz.

---

*No other new features or behavioral changes are included in this release.*