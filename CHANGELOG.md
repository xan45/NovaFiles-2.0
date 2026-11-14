# Changelog

All notable changes to NovaFiles will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.0-alpha1] — Initial Public Alpha Release

### Added
- **Unified File Manager**: Single vertically scrollable list displaying folders followed by files, with List View and Grid View options, breadcrumb path navigation, search, and real-time operation progress indicators.
- **Context Menu System (BUG-004)**: Contextual action sheet for individual files and folders providing Open, Open with, Cut, Copy, Paste into, Rename, Delete, Share, Properties & Checksum, Add to Favorites, Compress (ZIP), and Extract actions without triggering multi-selection mode.
- **Multi-Selection & Batch Operations**: Floating selection dock supporting batch Copy, Cut/Move, Delete, Share, and Compress operations.
- **Tabbed Multi-Pane Manager (FEATURE-003)**: Full-screen tabbed file explorer supporting multiple independent tabs, horizontal swipe navigation, add/close tab controls, separate folder history and scroll positions per tab, and cross-tab copy/paste transfers.
- **Full-Screen Storage Category Explorer (FEATURE-005)**: Full-screen browsing interface for Audio, Videos, Documents, Images, and APKs with integrated search, sorting, context operations, and live storage statistics synchronization.
- **Local Media Gallery (FEATURE-002)**: Dedicated media viewer featuring All Images (chronological grid with date grouping), Folders view, custom persistent Albums, hidden folder exclusion detection, and detailed EXIF metadata inspection.
- **Video Library & Player (FEATURE-010)**: Comprehensive video hub with All Videos, Folders, and custom persistent Playlists, paired with an AndroidX Media3/ExoPlayer video player featuring edge gesture controls for volume and brightness, horizontal scrubbing, speed adjustment (0.5x–2.0x), audio/subtitle track selection, and technical metadata display.
- **Audio & Music Studio**: AndroidX Media3 `MediaSessionService` player with persistent playback notifications, dynamic album art, lock-screen controls, background playback, and interactive Glance home-screen widgets.
- **Storage Analyzer & Cleaner**: Circular capacity breakdown gauge, multi-segment category distribution visualization, large file inspection, and storage junk cleaner (app cache, temporary logs, obsolete APK installers, and SHA-256 duplicate detection).
- **Wi-Fi FTP Server**: Embedded RFC 959 compliant FTP server supporting passive data transfers, sandboxed directory validation, and persistent foreground notification with one-tap shutdown.
- **Code & Text Editor**: Integrated monospace viewer and editor for plain text, markdown, JSON, XML, logs, and code files with background persistence.
- **Tri-Mode Theming & Palette Engine (Phase 5C)**: Pure White Light Theme, AMOLED Pure Black (`#000000`), and configurable Custom Theme with live color swatch selection, custom hex code inputs, luminance-based text contrast calculation, and instant default reset.
- **Application Security**: Optional PIN lock screen protecting application entry.
- **Application Package Migration (Phase 6A)**: Migrated package identifier and namespace to `com.xan.novafiles`.
