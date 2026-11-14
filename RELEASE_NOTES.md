# NovaFiles v2.0.0-alpha.1

## What's New

### 🎬 Complete Video Library Redesign (FEATURE-010)
- **Three Primary Sections**: All Videos, Folders, and Playlists with smooth tab transitions.
- **All Videos Section**: Chronological date grouping (Today, Yesterday, Month headers), duration badges, quality badges (4K UHD, 1080p FHD, 720p HD, SD), and configurable sorting/filtering.
- **Dedicated Folders View**: Folder cards showing video count, total folder duration, and folder size, with deep folder drill-down and breadcrumb navigation.
- **Persistent Playlists**: Create, rename, delete custom playlists; add/remove videos; sequential queue playback.
- **Dedicated Video Settings**: Bottom sheet for Browsing Layout (List vs. Grid), Thumbnail Density, Grid Spacing, Duration Overlay toggle, Date Grouping toggle, Playback Position remembering, Default Subtitles, Default Playback Speed, and Orientation Preferences.
- **Advanced Player Enhancements**: ExoPlayer/Media3 integration with subtitle and audio track pickers, playback speed dialog (0.5x to 2.0x), continuous scrubber, orientation lock toggle, queue navigation (Previous/Next), and Technical Details metadata sheet (Codec, Bitrate, Resolution, FPS, Container).

- **Unified file listing**: Folders and files displayed together in one vertical scroll (folders first)
- **List & Grid views**: List view as default with grid view toggle
- **Context menus**: Full context menu via three-dot icon — Open, Open With, Cut, Copy, Paste, Rename, Delete, Share, Properties, Favorites, Compress, Extract
- **Multi-selection**: Long-press to enter selection mode with bulk Copy, Cut, Delete, Share, Compress
- **Clipboard dock**: Floating paste bar with item count when clipboard has content
- **In-browser settings**: Sort by, sort direction, layout toggle, and show hidden files — all accessible from the file browser toolbar
- **Operation progress**: Real-time progress dialog with cancellation support
- **FileProvider sharing**: Secure content URI sharing via Android FileProvider

### 🐛 Bug Fixes
- **BUG-001 — Activity Recreation**: State holders converted to lifecycle-managed AndroidX ViewModels; configuration changes no longer restart the app or lose browsing state
- **BUG-002 — Music Library Empty Screen**: Fixed MediaStore audio query integration, permission handling, and tab data population (Tracks, Albums, Artists, Folders)
- **BUG-003 — Back Navigation**: Replaced single-screen navigation with a proper navigation stack; system back gesture navigates in-app instead of minimizing; predictive back gesture support
- **BUG-004 — Context Menu vs Selection**: Three-dot menu now correctly opens the item's context menu instead of triggering multi-selection mode

### 🏗️ Architecture
- Navigation stack with `activeScreenStack` and proper `BackHandler` implementations
- `SavedStateHandle` / `rememberSaveable` for UI state across configuration changes
- `FilesPreferencesRepository` for persistent file browser settings
- `FileActionHelper` for safe share and open-with intents
- Settings screen cleaned up — moved file-browser-specific settings into the file manager

## Checksums

| File | SHA-256 |
|------|---------|
| `app-debug.apk` | `A640DFD1CEF02C6D3D0386548E213A3A205A08D6DC74E232AC0801F4065CB35F` |

## APK Size
- **27.31 MB** (28,637,369 bytes)

## Known Limitations
- File operations use `java.io.File` API (requires `MANAGE_EXTERNAL_STORAGE`); SAF integration planned for future release
- Operation cancellation may leave partially copied files
- No undo for delete operations
- Clipboard state is in-memory (lost on process death)
- This is a debug build; release/signed APK coming in a future alpha

## Full Changelog
https://github.com/xan45/NovaFiles-2.0/compare/v2.0.0-alpha.0...v2.0.0-alpha.1
