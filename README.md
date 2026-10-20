# StreamFetch — Mobile Video Downloader & Browser

<p align="center">
  <b>Modern, high-performance Android mobile application to browse, detect, and download videos directly to your smartphone.</b>
</p>

<p align="center">
  <a href="#-downloading-the-latest-apk"><img src="https://img.shields.io/badge/Release-APK_Download-blue?style=for-the-badge&logo=android" alt="Download APK" /></a>
  <a href="https://github.com"><img src="https://img.shields.io/badge/Platform-Android%208.0%2B-green?style=for-the-badge" alt="Platform Android 8+" /></a>
  <a href="https://github.com"><img src="https://img.shields.io/badge/Kotlin-2.2-purple?style=for-the-badge&logo=kotlin" alt="Kotlin" /></a>
  <a href="https://github.com"><img src="https://img.shields.io/badge/UI-Jetpack_Compose_M3-teal?style=for-the-badge" alt="Jetpack Compose" /></a>
</p>

---

## 🚀 Downloading the Latest APK

You can download the ready-to-install Android `.apk` package in two ways:

### Option 1: GitHub Releases (Recommended)
1. Navigate to the **[Releases](../../releases)** section of this GitHub repository.
2. Select the latest release version (e.g., `v1.0.0`).
3. Under **Assets**, click on **`StreamFetch-release.apk`** (or `StreamFetch-debug.apk`) to download directly to your smartphone.
4. On your Android device, open the downloaded APK and tap **Install** (allow installation from unknown sources if prompted).

### Option 2: AI Studio Settings
- Inside Google AI Studio, click the **Settings / Menu** in the top navigation bar.
- Choose **Export APK / AAB** to immediately generate and download the compiled APK file.

---

## ✨ Key Features

- **Smart Video Sniffer**:
  - Automatically identifies downloadable video streams (`.mp4`, `.webm`, `.m3u8`, `.mov`) as you browse or play videos.
  - Floating animated **Download Video** indicator appears as soon as media is found.
- **Universal Link Downloader ("Fast Link")**:
  - Direct URL input with one-tap clipboard paste button.
  - Inspects stream formats, headers, and sizes before downloading.
  - Preloaded royalty-free media presets (Blender Peach, Elephants Dream, Tears of Steel) for instant test downloads.
- **Multi-Resolution Quality Picker**:
  - Download in **1080p Full HD**, **720p HD**, **480p Standard**, **360p Data Saver**, or **Original Stream**.
  - Displays approximate bitrate and estimated file sizes (~MB).
- **Download Manager**:
  - Background downloads with automatic resume using HTTP `Range: bytes=` headers.
  - Real-time speedometer (MB/s / KB/s), progress percentage, and dynamic Estimated Time of Arrival (ETA).
  - Controls to Pause, Resume, Cancel, and Retry tasks.
- **Saved Video Library & Offline Player**:
  - Built-in hardware-accelerated video player with seek slider, play/pause, 10s scrub, and speed multiplier (0.5x – 2.0x).
  - **"Open With"** support to launch videos directly into system media apps (VLC, MX Player, Google Photos) via Android `FileProvider`.
  - Rename, share, view file details, and manage local storage.
- **Modern Material Design 3**:
  - Sleek cyber slate & electric cyan theme with full edge-to-edge system insets.

---

## 🛠️ Automated GitHub Release Workflow

This repository includes a GitHub Actions workflow (`.github/workflows/release.yml`) that automatically compiles the Android APK and attaches it to GitHub Releases.

### How to Trigger an APK Release:

#### Method A: Push a Git Tag
Push any tag starting with `v` (e.g., `v1.0.0`):
```bash
git tag v1.0.0
git push origin v1.0.0
```
GitHub Actions will automatically build the APK, create the GitHub Release, and attach `StreamFetch-v1.0.0-release.apk` and `StreamFetch-v1.0.0-debug.apk` with SHA-256 checksums.

#### Method B: Manual Workflow Dispatch
1. Go to the **Actions** tab on your GitHub repository.
2. Select **Build and Release Android APK** in the left sidebar.
3. Click **Run workflow**, enter the version tag (e.g. `v1.0.0`), and click **Run workflow**.

---

## 🏗️ Building from Source

To build locally using Gradle:

```bash
# Clone the repository
git clone https://github.com/<your-username>/<repo-name>.git
cd <repo-name>

# Build debug APK
gradle assembleDebug

# The APK will be located at:
# app/build/outputs/apk/debug/app-debug.apk
```

---

## ⚖️ Legal & Disclaimer

StreamFetch is designed for downloading public domain, open-source, Creative Commons media, and videos that users have authorization or rights to save. It strictly complies with platform terms and does not circumvent DRM protection.
