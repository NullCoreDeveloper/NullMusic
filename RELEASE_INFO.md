# Echo Music v5.1.91

- Fixed specific playback error codes: 3001, 2008, 1004 by retrying the network or AudioTrack securely.
- Fixed playback going soundless by locking the stream quality for the currently playing song to prevent mid-stream container changes.
- Fixed audio fallback toast notifications only showing once per app session.
- Fixed ExoPlayer crashing when stream container format falls back to a different format than the cache.
- Removed volume level percentage indicator from the Phone Speaker row in the Audio Device selector.
- Fixed playback error 2008 (io read position out of range) when skipping to a segment in downloaded songs.
- Fixed the app incorrectly requesting an internet connection to play fully downloaded songs due to a cache metadata mismatch.
- Fixed generic playback IO error 2000 (io unspecified) by properly recovering the audio stream and cache.
- Added deep link support to directly open `share.echomusic.fun` links from other apps (like WhatsApp).
- Added a new "Service Uptime" checker in the Content settings to monitor the real-time status of YouTube Music, JioSaavn, and Qobuz.
