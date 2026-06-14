# Echo Music v5.1.91

- Fixed specific playback error codes: 3001, 2008, 1004 by retrying the network or AudioTrack securely.
- Fixed playback going soundless by locking the stream quality for the currently playing song to prevent mid-stream container changes.
- Fixed audio fallback toast notifications only showing once per app session.
- Fixed ExoPlayer crashing when stream container format falls back to a different format than the cache.
