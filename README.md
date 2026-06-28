<div align="center">
  <img src="assets/Echo-new.png" alt="NullMusic Logo" width="140"/>

  <h1>NullMusic</h1>

  <p><strong>A modern Android music app with ad-free streaming, synced lyrics, offline playback, and an intuitive user experience.</strong></p>

  [![GitHub Release](https://img.shields.io/github/v/release/NullCoreDeveloper/NullMusic?style=for-the-badge&color=6f42c1)](https://github.com/NullCoreDeveloper/NullMusic/releases)
  [![GitHub Stars](https://img.shields.io/github/stars/NullCoreDeveloper/NullMusic?style=for-the-badge&color=e3b341)](https://github.com/NullCoreDeveloper/NullMusic/stargazers)
  [![License](https://img.shields.io/github/license/NullCoreDeveloper/NullMusic?style=for-the-badge&color=28a745)](LICENSE)
  [![Telegram](https://img.shields.io/badge/Telegram-Join-2CA5E0?style=for-the-badge&logo=telegram&logoColor=white)](https://t.me/NullCoreDeveloper)
</div>

---

## Overview

NullMusic delivers a seamless, premium listening experience by leveraging YouTube Music's vast library — without the ads. It adds powerful extras including offline downloads, real-time synchronized lyrics, Yandex Music playlist import, and environment-aware music recognition.

---

## Table of Contents

- [Overview](#overview)
- [Screenshots](#screenshots)
- [Features](#features)
- [Installation & Setup](#installation--setup)
- [Community](#community)
- [Special Thanks](#special-thanks)

---

## Screenshots

<div align="center">
  <table style="margin: 0 auto; border-collapse: collapse;">
    <tr>
      <td align="center" style="padding: 10px; border: none;">
        <strong>Home Screen</strong><br><br>
        <img src="Screenshots/sc_1.png" alt="Home Screen" width="200" style="border-radius: 8px; box-shadow: 0 4px 8px rgba(0,0,0,0.2);"/>
      </td>
      <td align="center" style="padding: 10px; border: none;">
        <strong>Music Player</strong><br><br>
        <img src="Screenshots/sc_2.png" alt="Music Player" width="200" style="border-radius: 8px; box-shadow: 0 4px 8px rgba(0,0,0,0.2);"/>
      </td>
      <td align="center" style="padding: 10px; border: none;">
        <strong>Synchronized Lyrics</strong><br><br>
        <img src="Screenshots/sc_3.png" alt="Synchronized Lyrics" width="200" style="border-radius: 8px; box-shadow: 0 4px 8px rgba(0,0,0,0.2);"/>
      </td>
    </tr>
    <tr>
      <td align="center" style="padding: 10px; border: none;">
        <strong>Search & Explore</strong><br><br>
        <img src="Screenshots/sc_4.png" alt="Search & Explore" width="200" style="border-radius: 8px; box-shadow: 0 4px 8px rgba(0,0,0,0.2);"/>
      </td>
      <td align="center" style="padding: 10px; border: none;">
        <strong>Music Library</strong><br><br>
        <img src="Screenshots/sc_5.png" alt="Music Library" width="200" style="border-radius: 8px; box-shadow: 0 4px 8px rgba(0,0,0,0.2);"/>
      </td>
      <td align="center" style="padding: 10px; border: none;">
        <strong>Echo Find (Recognition)</strong><br><br>
        <img src="Screenshots/sc_6.png" alt="Echo Find" width="200" style="border-radius: 8px; box-shadow: 0 4px 8px rgba(0,0,0,0.2);"/>
      </td>
    </tr>
  </table>
</div>

---

## Features

- **Redesigned UI** — Cleaner, faster, and more intuitive interface from the ground up.
- **Import Playlists** — Bring your playlists and tracks over from Spotify and Yandex Music.
- **Ad-Free** — Stream without any interruptions.
- **Seamless Playback** — Switch effortlessly between audio-only and video modes.
- **Background Playback** — Listen while using other apps or with the screen off.
- **Offline Mode** — Download tracks, albums, and playlists via a dedicated download manager.
- **Echo Find** — Identify songs playing around you using advanced audio recognition.
- **Echo Brain** — An intelligent, on-device engine that analyzes your listening momentum and auto-injects perfectly aligned tracks into your queue.
- **Multiple Lyric Animations** — Choose from various lyric display styles with word-by-word synchronization.

---

## Installation & Setup

### Android Installation

Download the latest pre-compiled APK from the [Releases Page](https://github.com/NullCoreDeveloper/NullMusic/releases/latest).

### Building from Source

1. **Clone the Repository**
   ```bash
   git clone https://github.com/NullCoreDeveloper/NullMusic.git
   cd NullMusic
   ```

2. **Configure Android SDK**
   Create a `local.properties` file:
   ```bash
   echo "sdk.dir=/path/to/your/android/sdk" > local.properties
   ```

3. **Build the Application**
   NullMusic has two build variants: **FOSS** (without Google Play Services / Cast) and **GMS** (with Cast support).
   
   To build the GMS Universal Debug variant:
   ```bash
   ./gradlew assembleUniversalGmsDebug
   ```

---

## Community

Join our Telegram community for updates and discussions:

<div align="center">
  <a href="https://t.me/NullCoreDeveloper">
    <img src="assets/telegram.png" alt="Telegram Logo" width="130"/>
  </a>
</div>

---

## Special Thanks

NullMusic stands on the shoulders of several excellent open-source projects. Sincere thanks to:

| Project | Description |
| :--- | :--- |
| [Metrolist](https://github.com/MetrolistGroup/Metrolist) & [Vivi Music](https://github.com/vivizzz007/vivi-music) | Foundational inspiration and architecture reference |
| [ArchiveTune](https://github.com/koiverse/ArchiveTune) | Material You UI inspiration |
| [Better Lyrics](https://better-lyrics.boidu.dev/) | Lyrics enhancement and synchronization |
| [SimpMusic](https://github.com/maxrave-dev/SimpMusic) | Lyrics implementation reference |
| [Music Recognizer](https://github.com/aleksey-saenko/MusicRecognizer) | Audio recognition (Echo Find) |
| [Flow](https://github.com/a-edev/Flow) | AI queue generation engine (Echo Brain) |

---

<div align="center">
  Licensed under <a href="LICENSE">GPL-3.0</a>
</div>
