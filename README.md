<div align="center">

<a href="https://github.com/workwithshabd/niaromi">
    <img src="./.github/assets/logo.png" alt="Niaromi logo" title="Niaromi" width="96"/>
</a>

# Niaromi

### A refined, editorial manga & eBook reading experience for Android.
*Designed with physical acrylic aesthetics, Didone typography, unified background search, and automated Google Drive sync.*

[![Android](https://img.shields.io/badge/Android-8.0%2B-3DDC84?style=flat&logo=android&logoColor=white)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0-7F52FF?style=flat&logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4?style=flat&logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![License: Apache-2.0](https://img.shields.io/badge/License-Apache%202.0-0877d2?style=flat)](/LICENSE)

---

</div>

## Overview

**Niaromi** reimagines reading on Android by blending a curated editorial design with modern reading capabilities. Departing from conventional utility grids, Niaromi introduces a warm ivory canvas, physical acrylic floating bookshelves, tactile book stacks, high-contrast serif typography, and seamless cloud synchronization.

Whether you're following ongoing manga series, webtoons, comic archives, or reading classic literature via EPUB eBooks, Niaromi offers a distraction-free, fluid reading experience.

---

## Key Features

### 🎨 Editorial Library & Floating Acrylic Shelves
- **Tactile Shelf System**: Floating horizontal bookshelves with frosted acrylic transparency, subtle highlights, soft drop shadows, and metallic screw accents.
- **Physical Book Stacks**: Manga and book covers arranged in miniature physical stacks with realistic depth and layering.
- **Curated Typography**: Elegant Didone/Bodoni editorial serif headings paired with clean, compact sans-serif metadata.
- **Warm Ivory Palette**: Minimalist `#FFFDF4` background that lets cover artwork remain the primary visual element.
- **Floating Pill Navigation**: Minimal, distraction-free bottom navigation bar featuring Home, Explore, and Settings.

### 🧭 Cross-Extension Explore Feed
- **Curated Discoveries**: Discover new releases across all installed extensions in one unified feed.
- **Strict Deduplication**: Intelligent title normalization eliminates duplicate listings across sources.
- **Preferred Extension Priority**: Titles from preferred (pinned) extensions are prioritized and highlighted with a dedicated shelf.
- **Quick Source Filtering**: Instant filter chips to view releases by individual extensions or all combined.

### 🔍 Unified Behind-the-Scenes Search
- **Instant Library Search**: Queries your local library instantly as you type.
- **One-Tap "Add Title"**: If a title isn't in your collection, tap **Add Title** to search across all installed extensions in parallel behind the scenes.
- **Intelligent Results**: Automatically displays the best match with the latest released chapter and direct library addition.
- **Source Exploration**: One-tap access to inspect all extensions and source results whenever desired.

### ☁️ Automated Cloud Sync & Seamless Restoration
- **Google Drive Integration**: Back up your entire library, read progress, bookmarks, custom categories, history, and preferences directly to Google Drive.
- **Automatic Restore on Reinstall**: Clean installations automatically restore your library and reading status upon Google Sign-In—no manual backup file selection required.
- **Zero-Touch Storage Provisioning**: Automatically detects or provisions your storage directory (`Documents/Niaromi`) on clean install without prompting for directory pickers.

### 📖 Full eBook & EPUB Reader Support
- **Native EPUB Support**: Open and read `.epub` eBooks with a dedicated reader engine.
- **Editorial Typography**: Customized book layouts featuring drop caps, responsive margins, balanced line spacing, and justified text.
- **Flexible Reading Modes**: Left-to-Right, Right-to-Left, Vertical, Webtoon, and Continuous Paged modes.
- **Broad Format Support**: Read local directories, `.cbz`, `.cbr`, `.zip`, `.tar.gz`, and `.epub` archives.

### 🔄 One-Tap Extension Migration
- **Batch Migration**: Move all titles from one extension to another with a single click.
- **Smart Result Matching**: Automatically pairs each title with its optimal counterpart on the target extension.
- **Performant & Lag-Free**: Background migration worker handles large libraries smoothly.

### 🛡️ Clear Diagnostics & Friendly Error Handling
- **Informative Error Feedback**: Clear, actionable error messages for common reading issues:
  - *"Too slow internet or connection timed out. Please check your network and try again."*
  - *"Chapter unavailable or moved on source. Try re-adding title or selecting another source."*
  - *"Source protected by Cloudflare or captcha. Open in WebView to solve, or re-add from another extension."*
  - *"Source extension is not installed. Please reinstall the extension or migrate this title."*

---

## Architecture & Tech Stack

Niaromi is built following modern Android architectural standards:

- **Language**: 100% Kotlin with Coroutines & Kotlin StateFlow.
- **UI Framework**: Modern declarative UI with **Jetpack Compose** and Material 3 design tokens.
- **Modularization**: Clean architecture partitioned into Gradle modules (`app`, `domain`, `data`, `core`, `presentation-core`, `source-api`, `source-local`).
- **Database**: SQLDelight SQLite engine for type-safe persistence.
- **Networking & Media**: OkHttp, Coil image pipeline, custom EPUB parser and static layout page renderer.
- **Cloud Backend**: Google Identity Services & Google Drive REST API v3.

---

## Getting Started

### Prerequisites
- Android 8.0 (Oreo / API Level 26) or higher.
- Phone or tablet device / emulator.

### Installation
1. Download the latest release APK from the [Releases](https://github.com/workwithshabd/niaromi/releases) page.
2. Install the APK on your device.
3. On first launch, Niaromi automatically initializes your storage folder in `Documents/Niaromi`.
4. Sign in with Google under **Settings > Cloud Sync** to sync your library across devices.

---

## Building from Source

To compile Niaromi locally:

```bash
# Clone the repository
git clone https://github.com/workwithshabd/niaromi.git
cd niaromi

# Build debug APK using JDK 17 or 21
./gradlew assembleStandardDebug
```

The compiled APK will be generated at:
`app/build/outputs/apk/standard/debug/`

---

## Connect & Community

Developed by **Shabd** ([@workwithshabd](https://github.com/workwithshabd)).

- **X (Twitter)**: [@workwithshabd](https://x.com/workwithshabd)
- **Discord**: [@workwithshabd](https://discord.com/users/workwithshabd)
- **GitHub**: [@workwithshabd](https://github.com/workwithshabd)

---

## Disclaimer

Niaromi is a reading client and content organizer. The developer does not host, distribute, or provide any manga, comic, or eBook content. All content accessed through extensions is retrieved directly from third-party sources.

---

## License

```
Copyright © 2015 Javier Tomás
Copyright © 2024 Mihon Open Source Project
Copyright © 2026 Niaromi Project (Shabd)

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
