# Changelog

All notable changes to Echo Music will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [4.3.0] - 2026-05-14

**A Fresh New Experience**

This update introduces a complete visual refresh alongside major improvements to local media handling, playback experience, customization, and overall app optimization.

### New & Redesigned
- Brand-new refreshed app icon
- Redesigned carousel interface
- New bottom navigation bar
- Material You based UI enhancements added throughout the app
- UI improvements inspired by and ported from ArchiveTune
- Dynamic theming support added
- User preference based themes added

### Playback & Audio Improvements
- Listen Together issues fixed (MetroList)
- Fixed blurred album art and music artwork issues
- Improved lyrics animations
- Improved Set Ringtone functionality

### Local Media Enhancements
- Search bar added for local media
- Users can now exclude specific files from local media scanning
- Improved Local Download (Beta) support
- Added support for third-party apps

### Settings Changes
- Spotify Import has been moved to the Backup & Restore section in Settings

### Performance & Optimization
- Significant app size reduction:
  - Reduced from 120MB to 21MB
- General performance improvements and optimizations.

---

## [4.2.2] - 2026-04-16

# Changelog – Fix & Improvements

- Fixed UI by adding a cross (close) button in the Important Notice section for better user control.
- Added a shuffle button to the Old Music page UI to enhance playback experience.

---
## [4.2.1] - 2026-04-16

### Features
- Added `CANVAS_BEARER_TOKEN` and ArchiveTuneCanvas authentication support  
- Introduced granular YouTube Music sync options  
- Added lyrics provider selection field  
- Improved casting functionality  
- Added optional Cloudflare DNS-over-HTTPS (DoH) resolver  
- Enabled Cloudflare DNS by default  

### UI / UX Improvements
- Revamped overall settings UI  
- Restyled Equalizer dialog  
- Refactored Equalizer UI and appearance  
- Improved BottomSheet interaction with Enter/DPAD key support for expansion  

### Fixes
- Fixed widget local album art issue (PR #207) / @harshal20m

---
## [4.2.0] - 2026-04-08

### New Features
- Added PoToken extraction, stream client, and Lyrics v2  
- Introduced Equalizer UI with persistent canvas cache  
- Added network troubleshoot settings screen  
- Implemented host/join UI with participant styling (Listen Together)  
- Added lyrics romanization and preload support  
- Introduced new player design toggle (`UseNewPlayerDesign`)  
- Added old music page UI toggle (`UseOldMusicPageUi`)  

### UI & UX Improvements
- Updated settings UI  
- Improved Cast UI and queue handling  
- Refined floating toolbar and settings gradients  
- Applied `surfaceContainerHigh` styling and added card borders  
- Improved Equalizer dialog UI  
- Added back navigation to Settings and Listen Together  
- Hid navigation bar and mini-player in Settings and Listen Together screens  
- Replaced shuffle toggle with a share button  
- Selecting a song in queue, playlist, or album is now clearly marked  

### Enhancements
- Improved Cast queue handling  
- Updated internal components:
  - `Queue.kt`  
  - `Items.kt`  

### Changes
- Disabled SponsorBlock by default  
- Removed legacy appearance and player preferences  
- Refactored lyrics animations and removed V2 styles  
- Applied fixed blur for old music page UI

---
## [4.1.1] - 2026-04-05

### Improvements
- Improved playback synchronization for a smoother listening experience
- Enhanced service binding reliability

### New Features
- Added Old Navbar style option
- Introduced Legacy Player toggle for compatibility  
  - Available in Settings → Appearance → Misc (bottom section)

### Updates
- Updated AppearanceSettings.kt for improved configuration handling

### Bug Fixes
- Fixed issues in Listen Together
- General bug fixes and stability improvements

---
## [4.1.0] - 2026-04-05

### Highlights
- Introduced Listen Together for synchronized playback using Protobuf  
- Added Audio Output Selector for seamless device switching  
- Integrated Spatial Audio, Pro EQ, and Download Preferences  
- Optimized user interface for tablet devices and improved Ambient Mode  

### User Interface Improvements
- Implemented dynamic theming with palette-based gradients  
- Refined layouts for album, playlist, and player screens  
- Enhanced lyrics seeking and auto-scroll behavior  

### Core Enhancements
- Improved queue management and shuffle behavior  
- Enhanced Spotify import reliability, including paging and rate-limit handling  
- Increased playback and background service stability  

### Fixes
- Addressed multiple bugs and improved overall performance  
- General stability improvements across player and service components

---
## [4.0.2] - 2026-03-07

This update focuses on small improvements, UI refinements, and playback adjustments.

**Changes**
- Refined UI spacing for menus
- Improvements to Settings and Updater interface
- Adjusted ExoPlayer buffering for better playback stability
- Renamed the lyrics button for better clarity
- Fixed icon issues

**Other**
- Minor code improvements and internal changes
- General stability improvements

---
## [4.0.1] - 2026-03-06

## Added
- Option to **force stop the player when the task is cleared**
- **Inline lyrics view** in the player
- Changed Crossfade icon
- **Crossfade toggle** in Music Page
- Ability to **hide lyrics on tap**

## Improvements
- Refined **player UI layout**
- Improved **UI spacing and alignment**
- Better **scroll-to-top behavior**
- Improved **back navigation handling**

## Changes
- **LyricsPlus** is now enabled by default
- **SponsorBlock** is now disabled by default
- Adjusted **SponsorBlock slider position**
- Switched **adaptive launcher icons** to drawable resources

## Fixes
- Fixed **crossfade starting incorrectly** (now starts from a minimum of 3 seconds)
- Fixed navigation issue when moving from **Local Media to the Library page**

## Other
- General stability improvements and minor UI tweaks

---
## [4.0.0] - 2026-03-05

This release introduces a redesigned interface, new media capabilities, improved lyrics support, and deeper integrations.

## New Features
- Completely redesigned UI for a cleaner and faster experience
- Import from Spotify to easily bring your playlists and tracks
- Podcast support
- Local media support for playing music stored on your device
- Auto data migration to seamlessly move existing app data to the new version
- Android Dynamic Island support for enhanced playback notifications
- Multiple lyrics animations
- Word-by-word lyrics support
- New lyrics provider: Lyrics+ for improved accuracy and coverage
- AI lyrics translation provider with built-in Google Translate
- Discord integration
- Last.fm integration for scrobbling
- Music sharing support via Odesli (Song.link) for cross-platform sharing
- Set song as ringtone option
- Canvas animations while playing music
- Crossfade between tracks
- Vertical Ambient Mode support
- Music haptics for tactile feedback
- TTS song announcements

## Smart Playback
- Pause on mute
- Resume on Bluetooth connect
- Keep screen on while playing music

## Customization
- UI density scale to adjust interface spacing
- High refresh rate support for smoother UI and animations
- Hide player thumbnail
- Crop album art option
- Hide video songs
- Hide YouTube Shorts

## Improvements
- Improved lyrics quality and synchronization
- Better performance across the app
- Stability improvements and bug fixes
- Many under-the-hood optimizations and refinements

---
## [3.3.6] - 2026-01-26

## Fixed
- Fixed an issue where the app crashed immediately after opening

---
## [3.3.5] - 2026-01-23

- Fixed lyrics translations.
- Resolved an issue where updating the app caused deletion of history, stats, playlists, and downloaded songs.
- Fixed multiple minor bugs and improved overall stability.

---
## [3.3.4] - 2026-01-23

### Improvements
- Removed the **Glass UI** for a cleaner and more consistent interface.
- Improved **music playback polish** for a smoother listening experience.
- Reduced app launch time for faster opening and better responsiveness.

### Known Issues
- Cache, Uploaded & Local Media system is under rework and may not function as expected.

### General
- Overall UI and performance polished across the app for improved stability and user experience.

---
## [3.3.3] - 2026-01-20

## What’s new
- Glass UI added to the navbar, miniplayer, and menu
- Music page redesigned with improved lyrics and queue
- Local media now supports MP3
- Uploaded songs can now be played
- Login to Desktop option added on desktop login
- Chromecast issues fixed

## Fixes and improvements
- Synced lyrics issue fixed, including translated lyrics
- OpenRouter issues fixed
- Performance and stability improvements

---
## [3.3.2] - 2026-01-04

## What’s New
- **Echo Wrapped** added  
- **SponsorBlock** added  
- **Automatic language mismatch detection for lyrics**, with on-the-fly translation  
- **Double-tap on the music cover to like a song**  
- Improved lyrics sharing experience  
- Glow and bounce effects added to lyrics  
- Lyrics accuracy improved  
- **Play All** button added to Quick Picks  
- Advanced playlist download support added  

## Fixes & Improvements
- Chromecast issues fixed  
- Lyrics sync problems resolved  
- Permission denied message removed  
- Overall performance and stability improved  

## Privacy Updates
- New **Permissions** section added in Settings  
- View all permissions used by the app  
- Clear explanations for why each permission is required  


> ### Support the Project
> You can support this project here:  
> https://support.iad1tya.cyou/
>

---
## [3.3.1] - 2025-12-22

## What’s New
• **AI Lyrics Translation** – Translate song lyrics using AI  
• **Custom Provider Support** – Use a custom provider by setting your own URL  
• **Advanced Download Options** – Download music to your preferred storage location  
• **Custom Playlist Covers** – Change and personalize playlist cover images  

## Fixes & Improvements
• **Slim Navigation Bar Fixed** – Resolved display issues  
• **DLNA & Wi-Fi Casting Patched** – Improved casting reliability  
• **Find Widget Bug Fixed** – Restored proper functionality  
• **Minor Bug Fixes** – Improved overall stability  
• **Performance Enhancements** – Faster and smoother app experience

---
## [3.3.0] - 2025-12-19

## Changelog – Echo Music

### What’s New
- **Echo Find**: Instantly find the song playing in your surroundings.
- **New Widgets Introduced**: Added the **Echo Find widget** for quicker access.
- **Player Widget Optimized**: Improved performance and smoother interactions.
- **Animated Music Accent Background**: A clean, modern look with subtle animations.
- **Queue Control Enhanced**: You can now change the position of songs directly in the queue.
- **Overall App Optimizations**: Performance improvements, smoother experience, and stability fixes.

### Thank You
Thank you for your continued support.  
Echo Music will continue to receive updates, improvements, and new features in future releases.

**Discord Restored**
Discord Server has been restored. You can now join the community from here: https://discord.com/invite/EcfV3AxH5c

---
## [3.2.2] - 2025-12-14

---
#48 Stop music on task clear - fixed

---
## [3.2.1] - 2025-12-09

- **Widget** is now resizable.
- Added **Previous** and **Next** song options.
- Fixed minor bugs related to **Wi-Fi** and **DLNA casting**.

---
## [3.2.0] - 2025-12-06

# Change Log

- **Fixed:** An unknown error that impacted overall stability.  
- **New:** **Ambient Mode** for a minimal, clean desk-style landscape experience.  
- **New:** **AI-powered lyrics translation** feature.  
- **Improved:** Faster lyrics loading.  
- **New:** Long-press any lyric line to share it as an image in your story.  
- **Restored:** Radio button for music selection has been added back.  
- **Other:** Minor bug fixes and performance improvements.

---
## [3.1.4] - 2025-12-02

- **Bottom navbar** is now always visible on all screens.
- **Playlist** option has been added back to the Library section.
- **Settings** has been moved inside the **Account** menu.
- Fixed the **Unknown Error** bug.
- Fixed the **Search on TV** navigation bug.

---
## [3.1.3] - 2025-11-22

- Fixed a database migration issue.  
  The schema was at version 25, but only a **1 → 2** migration was defined.  
  Any install starting from another version had no valid upgrade path, causing the app to crash during migration.  
  All required migration steps are now in place so upgrades work correctly across versions.

**Thanks to:** pixelated_buttons and justkev_3611

---
## [3.1.2] - 2025-11-22

- Bug has been fixed that caused the app to crash.

> ## Note
> Echo Music now includes Google Cast and DLNA support at the request of owenconnor98 (https://discord.com/channels/1421059997982265448/1422161045119828041/1441590684271251466).
> 
> These features require Nearby Devices and Location permissions to discover devices on the network.
> 
> Echo Music does not collect or store any information from these permissions; they are used only when you choose to use Google Cast or DLNA.

---
## [3.1.1] - 2025-11-22

- **Video Quality:** Added support for upto **1080p playback**.
- **DLNA:** Introduced **DLNA streaming support** (suggestion by *owenconnor98*).
- **Lyrics:** Optimised lyrics layout and performance on **tablets**.

---
## [3.1] - 2025-11-21

- **“Switch to Video” Restored**  
  The switch-to-video button is back, letting you move to the song’s video with a single tap.

- **Nearby Devices Permission Update**  
  Added required **Wi-Fi** and **Location** permissions to improve nearby device discovery.

- **Google Cast Improvements**  
  Internal refinements to casting functionality. Thanks to **owenconnor98** for the contribution.

- **Music Page Visual Update**  
  Removed the black background behind the artwork for a cleaner, more consistent look.

- **Redesigned Audio Output Window**  
  The audio output selector has been refreshed for better clarity.

- **Multi-Account Support**  
  You can now add and switch between multiple accounts easily. Suggested by **job_done**.

- **Playback Stability Fix**  
  Fixed an issue where music stopped playing due to URL expiration.

- **Updated Mini Player**  
  The mini player now features a modern pill-shaped design.

---

## Acknowledgements

Special thanks to all supporters, including **Emagik** and **AdamPoy**, for their continuous supporting.

---
## [3.0.0] - 2025-11-11

- **Minor UI adjustments** for a cleaner and more consistent experience.
- **New feature**: Inside Settings → Player & Audio, a new option “**Tap album art for lyrics**” has been added. When enabled, you can view lyrics by simply tapping the album art on the music page. (_Suggested by_ @dins2k2)
- Added support for **Google Cast SDK**: The app can now detect and connect to Wi-Fi devices that support Google Cast, allowing you to stream music directly.
- **Enhanced in-app updater**: Updating the app is now easier than ever — no need to open a browser. You can download and install updates directly within the app.

---