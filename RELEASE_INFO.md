Echo Music v5.2.5

What's New
- Liquid Glass Settings Improvement: Tagged as (Beta), fixed the unscrollable bottom strip on the settings screen, mapped the "Unknown" background style correctly to "Liquid Glass", and added adaptive text colors so the miniplayer and bottom navbar remain legible across Light and Dark themes.
- Automix Fixes: Tagged Automix as (Beta) and offloaded the heavy beat-analysis math to a broader thread pool (IO). This prevents the UI and audio playback from freezing or stuttering on devices with limited cores while songs transition smoothly without the need to pause.
- Echo Extractor: A new extractor seamlessly integrated into Echo Music.
- Auto-Fetch Mechanism: Echo Extractor automatically fetches updates randomly once every 24 hours on app startup.
- Refined UI: Removed unnecessary extractor toggle cards and redundant text to ensure the settings page matches Echo Music's sleek aesthetics.
- AutoMix (Beta): Added DJ-style, beat-matched crossfades that analyze BPM and beat grid to tempo-match and align transitions, with automatic fallback to a regular crossfade when analysis is unclear (#770 by @barathsuresh).
- Lossless Funding Tracker: Added a sleek "Lossless Music Funding Tracker" to the Player Settings to transparently display server costs and goals for Qobuz integration.
- Playlist Migration Crash Fix: Fixed a crash on app open caused by the database migration trying to re-add an already-existing `isAutoSync` column (#755 by @jester-sys).
- Localization Update: Updated Spanish translations to 100% coverage, including new strings for playlist export, listening history, Spotify import, and Echo Brain (#760 by @weblate).

## Under-the-Hood Fixes
- Clock Skew Safety: Hardened background fetching against system clock drift to prevent the app from getting stuck.
- Concurrency & Atomicity: Player JS cache writes are now fully atomic. Improved token minting concurrency by using RequestSlots, ensuring requests are properly isolated during rapid signature decodes.
- Memory Integrity: Improved generation state logic for PoToken mints so that failures do not leak lingering session data.