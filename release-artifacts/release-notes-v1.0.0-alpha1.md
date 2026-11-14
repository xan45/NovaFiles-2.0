# NovaFiles 1.0.0-alpha1 — First Public Alpha

Welcome to the first official public Alpha release of **NovaFiles** (`1.0.0-alpha1`), a next-generation Android file manager, media browser, and storage suite built with modern Kotlin, Jetpack Compose, Material 3, Clean Architecture, and AndroidX Media3.

This Alpha release establishes a clean foundation with a redesigned unified file browser, desktop-grade tabbed multi-pane operations, full-screen storage category navigation, local media gallery, cinematic video player, Media3 music studio, and a tri-mode theming engine.

---

## What's Included in Alpha 1.0

### 📁 Unified File Management
- **Single Vertical Browsing**: Folders and files displayed in a unified scrollable list with folders prioritized at top.
- **List & Grid View Modes**: Switch between detailed list view and compact visual grid view.
- **Context Menus (BUG-004 Fix)**: Individual file and folder three-dot menus provide instant access to Open, Open with, Cut, Copy, Paste into, Rename, Delete, Share, Properties & Checksum, Add to Favorites, Compress (ZIP), and Extract without triggering selection mode.
- **Multi-Selection & Batch Actions**: Long-press any item to activate selection mode with a floating bottom action dock (Copy, Move, Delete, Share, Compress).
- **Floating Clipboard Dock**: Live indicator for queued files with item count, quick Paste, and Clear actions.
- **Fast Breadcrumbs & In-Browser Search**: Tap any segment to navigate parent folders; real-time filename search.

### 📑 Tabbed Multi-Pane Manager (FEATURE-003)
- **Full-Screen Tabbed Interface**: Multiple independently navigable tabs replacing traditional split-pane limits.
- **Independent Tab State**: Each tab remembers its own directory history, scroll position, and selection state.
- **Cross-Tab Transfers**: Cut or copy items in one tab, switch tabs, and paste directly into another folder.
- **Tab Controls**: Add tab (`+`), close tab (`x`), swipe horizontally between tabs, and tap tab headers.

### 📊 Full-Screen Storage Analyzer (FEATURE-005)
- **Visual Storage Breakdown**: Animated circular capacity gauge and multi-segment category allocation bar.
- **Full-Screen Category Explorer**: Tap Audio, Videos, Documents, Images, or APKs to explore matching files in a dedicated full-screen browser with sorting, search, and context operations.
- **Storage Junk Cleaner**: Scans for application cache, system logs, empty folders, obsolete APK packages, and full-stream SHA-256 verified duplicate files.

### 🖼️ Local Media Gallery (FEATURE-002)
- **Three Primary Views**: All Images (chronological grid with date grouping), Folders view, and custom persistent Albums.
- **Album Management**: Create, rename, delete custom photo albums persisted in local storage.
- **Smart Hidden Folder Detection**: Excludes system thumbnail and cache folders (`.thumbnails`, `.cache`) with an optional toggle to show hidden media.
- **EXIF Metadata Sheet**: View camera model, lens, aperture, shutter speed, ISO, focal length, and GPS coordinates.

### 🎬 Video Library & Media3 Player (FEATURE-010)
- **Three-Section Library**: All Videos with chronological date grouping and duration badges, Folders view, and custom persistent Playlists.
- **Cinematic ExoPlayer Player**: Edge swipe gestures for brightness (left edge) and volume (right edge), continuous timeline scrubbing, variable speed playback (0.5x–2.0x), audio/subtitle track pickers, and stream technical details.

### 🎵 Audio & Music Studio
- **AndroidX Media3 Service**: Background audio playback with persistent media notifications and lock-screen controls.
- **Home-Screen Widgets**: Interactive Glance widgets connected to live MediaSession state.

### 🎨 Tri-Mode Theming & Palette Engine (Phase 5C)
- **White (Clean Light)**: High-contrast pure white canvas (`#FFFFFF`) with dark typography.
- **Black (AMOLED Pure Black)**: True pitch-black background (`#000000`) for OLED power savings.
- **Custom (Palette Customizer)**: Choose surface and card colors, pick from preset swatches, enter custom HEX codes, and benefit from automatic luminance-based text contrast.
- **6 Accent Colors**: Indigo, Emerald, Coral, Cyan, Amber, and Pink.

### 🔒 Application Security & System Utilities
- **App PIN Lock**: Optional security PIN protection on app launch.
- **Wi-Fi FTP Server**: Embedded RFC 959 FTP server with passive mode and sandboxed path validation.
- **Code & Text Editor**: In-app monospace text and code editor with background saving.
- **Package Migration (Phase 6A)**: Standardized application ID to `com.xan.novafiles`.

---

## Installation & Sideloading

1. Download **`NovaFiles-1.0.0-alpha1.apk`** from the release assets below.
2. Open the downloaded file on your Android device.
3. If prompted, allow your browser or file manager to **Install unknown apps**.
4. Grant the **All files access** (`MANAGE_EXTERNAL_STORAGE`) permission when prompted to enable file browsing.

---

## System Compatibility

- **Application ID**: `com.xan.novafiles`
- **Version Name**: `1.0.0-alpha1`
- **Version Code**: `1`
- **Minimum OS**: Android 8.0 Oreo (API 26)
- **Target OS**: Android 15 (API 35)
- **Signing**: Signed with official production release keystore.

---

## ⚠️ Alpha Testing Disclaimer

This is an early Alpha release intended for testing and feedback. While core file operations are equipped with transactional checks and copy-then-delete protections, unexpected edge-case issues may occur. **Please back up critical files before performing large batch file moves or deletions.**

---

## Bug Reports & Feedback

Found a bug or have a suggestion?
- Report bugs via [GitHub Issues](https://github.com/xan45/NovaFiles-2.0/issues).
- Include your device model, Android version, and steps to reproduce.

---

## Release Checksums

| Asset | Type | Size | SHA-256 Checksum |
| :--- | :--- | :--- | :--- |
| `NovaFiles-1.0.0-alpha1.apk` | Release APK (Signed) | 5.17 MB (5,424,672 bytes) | `647E180650D6A322C25D3181276D9DCA66DC9E22BBDA09688999C2D13BC2D267` |
