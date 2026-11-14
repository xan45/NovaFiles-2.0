# NovaFiles v2.0.0-alpha.1

## What's New

### 🗂️ Complete File Manager Redesign (FEATURE-001)
- **Unified file listing**: Folders and files displayed together in one vertical scroll (folders first)
- **List & Grid views**: List view as default with grid view toggle
- **Context menus**: Full context menu via three-dot icon — Open, Open With, Cut, Copy, Paste, Rename, Delete, Share, Properties, Favorites, Compress, Extract
- **Multi-selection**: Long-press to enter selection mode with bulk Copy, Cut, Delete, Share, Compress
- **Clipboard dock**: Floating paste bar with item count when clipboard has content
- **In-browser settings**: Sort by, sort direction, layout toggle, and show hidden files — all accessible from the file browser toolbar
- **Operation progress**: Real-time progress dialog with cancellation support
- **FileProvider sharing**: Secure content URI sharing via Android FileProvider

### 📑 Tabbed Multi-Pane Manager (FEATURE-003)
- **Full-screen tabbed interface**: Multi-tab browsing area replacing vertical split pane
- **Independent navigation**: Each tab maintains independent directory history, scroll position, and selection state
- **Tab management**: Add, close, tap to switch, and swipe horizontally between tabs
- **Cross-tab file transfers**: Cut/copy items in one tab, switch tabs, and paste seamlessly

### 📊 Full-Screen Category Browser (FEATURE-005)
- **Full-screen storage explorer**: Upgraded from bottom sheet to full-screen view for Audio, Videos, Documents, Images, and APKs
- **Integrated file operations**: Reuses shared file manager components with List/Grid toggle, sorting, multi-selection, and context menus
- **Live analyzer synchronization**: Storage totals and category metrics auto-refresh upon file deletion or modification

### 🖼️ Comprehensive Media Gallery (FEATURE-002)
- **Three primary views**: All Images, Folders, and Albums with date grouping headers
- **Albums system**: Create, rename, delete custom albums; add/remove photos; persistent JSON storage
- **Hidden folders & exclusions**: Intelligent hidden folder detection (.thumbnails, .cache) with user toggle
- **EXIF metadata sheet**: Full technical details view (Camera model, lens, aperture, shutter speed, ISO, focal length, location coordinates)

### 🎬 Complete Video Library Redesign (FEATURE-010)
- **Three primary sections**: All Videos, Folders, and Playlists with smooth tab transitions
- **All Videos section**: Chronological date grouping (Today, Yesterday, Month), duration badges, quality badges (4K UHD, 1080p FHD, 720p HD, SD), and configurable sorting/filtering
- **Dedicated folders view**: Folder cards showing video count, total folder duration, and folder size, with deep folder drill-down and breadcrumb navigation
- **Persistent playlists**: Create, rename, delete custom playlists; add/remove videos; sequential queue playback
- **Dedicated video settings**: Bottom sheet for Browsing Layout (List vs. Grid), Thumbnail Density, Grid Spacing, Duration Overlay toggle, Date Grouping toggle, Playback Position remembering, Default Subtitles, Default Playback Speed, and Orientation Preferences
- **Advanced player enhancements**: ExoPlayer/Media3 integration with subtitle and audio track pickers, playback speed dialog (0.5x to 2.0x), continuous scrubber, orientation lock toggle, queue navigation (Previous/Next), and Technical Details metadata sheet (Codec, Bitrate, Resolution, FPS, Container)

### 🐛 Bug Fixes
- **BUG-001 — Activity Recreation**: State holders converted to lifecycle-managed AndroidX ViewModels; configuration changes no longer restart the app or lose browsing state
- **BUG-002 — Music Library Empty Screen**: Fixed MediaStore audio query integration, permission handling, and tab data population (Tracks, Albums, Artists, Folders)
- **BUG-003 — Back Navigation**: Replaced single-screen navigation with a proper navigation stack; system back gesture navigates in-app instead of minimizing; predictive back gesture support
- **BUG-004 — Context Menu vs Selection**: Three-dot menu now correctly opens the item's context menu instead of triggering multi-selection mode

### 🏗️ Architecture
- Navigation stack with `activeScreenStack` and proper `BackHandler` implementations
- `SavedStateHandle` / `rememberSaveable` for UI state across configuration changes
- Repository pattern with persistent JSON preferences across Files, Gallery, and Video modules
- Secure content URI sharing via Android FileProvider

## Checksums

| File | Type | Size | SHA-256 |
|---|---|---|---|
| `app-release.apk` | Release (Signed) | 5.90 MB (6,185,551 bytes) | `C787D5C8CEC50279ED884BA525B20E8C24DEA475FCAF20994116D50766EFD060` |

## Installation
Download and install `app-release.apk` on Android 8.0 (API 26) or higher. Grant All Files Access permission when prompted.

