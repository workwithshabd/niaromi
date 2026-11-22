<div align="center">

# Niaromi

**An Android reader for manga, manhwa, comics, and EPUB books.**

[![Download the latest APK](https://img.shields.io/badge/Download-Latest%20APK-725AC1?style=for-the-badge)](https://github.com/workwithshabd/niaromi/releases/latest)
[![Android 8.0+](https://img.shields.io/badge/Android-8.0%2B-27845B?style=for-the-badge)](#get-niaromi)
[![Apache License 2.0](https://img.shields.io/badge/License-Apache%202.0-266DB2?style=for-the-badge)](LICENSE)

[Get Niaromi](#get-niaromi) · [Features](#features) · [Report a problem](#report-a-problem) · [Build from source](#build-from-source)

</div>

## A different approach to Mihon

Niaromi is a variant of [Mihon](https://github.com/mihonapp/mihon) with a library-first approach. Home puts new chapters alongside your genres and custom collections. Explore brings extension search and discovery into one place, and the interface uses Android’s Material 3 design throughout the main screens. Niaromi also supports importing EPUB books alongside your manga and comics.

## Features

| Area | What you can do |
| --- | --- |
| **Home and collections** | Browse recent chapters from Home, sort titles by last read, and organize your library with genres and custom collections. In **View all**, choose compact, comfortable, or cover-only grids and set the number of titles per row. |
| **Explore and extensions** | Discover titles based on your reading history, search across installed extensions, and prioritize the extensions you prefer. Pull down to refresh the feed. |
| **Reading** | Resume from your reading progress, browse chapters in latest-first order, download chapters for offline reading, and import local EPUB books. |
| **Appearance** | Choose Light, Dark, or System mode and set an accent. Pure black dark mode is an optional setting. |
| **Backups** | Create and restore local backups or use optional Google Drive backups. |
| **Privacy** | Current releases ship without Firebase Analytics or Crashlytics. You can review and share a crash log manually. |

## Get Niaromi

Niaromi requires **Android 8.0 or later**.

1. Open the [latest GitHub release](https://github.com/workwithshabd/niaromi/releases/latest).
2. Download **Universal** if you’re unsure which build to choose. Use **ARM 64-bit** or **ARM 32-bit** if you know your device’s architecture.
3. Open the APK and follow Android’s installation prompts.

For future updates, open **Niaromi → Settings → Check for updates**. In-place upgrades require a build signed with the same certificate as your current installation.

Niaromi is a reading client. It does not host or distribute manga, comics, or books. Extensions retrieve content from third-party sources.

## Report a problem

Open a [GitHub issue](https://github.com/workwithshabd/niaromi/issues) with your Niaromi version, phone model, Android version, and the steps that reproduce the problem. If the app crashes, use **Share crash log** on the crash screen. Review the log before posting it publicly.

You can also tag [@workwithshabd on X](https://x.com/workwithshabd) or mention **workwithshabd** on Discord.

## Build from source

Open the repository in Android Studio. CI uses **JDK 21**. Install the Android SDK components requested by Gradle, then run:

```sh
./gradlew :app:assembleDebug
./gradlew check
```

Debug APKs are written to `app/build/outputs/apk/debug/`.

## Credits and license

Niaromi builds on the work of the [Mihon project](https://github.com/mihonapp/mihon) and its upstream contributors. Niaromi and Mihon are separate projects.

Niaromi is distributed under the [Apache License 2.0](LICENSE). Upstream copyright and attribution notices apply to the files they accompany.
