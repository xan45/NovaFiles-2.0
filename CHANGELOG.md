# Changelog

All notable changes to NovaFiles 2.0 will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [v2.0.0-alpha.2] - 2026-11-07

### Fixed
- **Duplicate Scanner Collision & Data Loss**: Replaced 8 KB truncated MD5 checksum with full-stream SHA-256 calculation processed in 64 KB streaming buffers, preventing data loss from false duplicate collisions.
- **Transactional Move Safety**: Hardened `FileOperationsEngine.moveFiles` with copy-then-delete verification; source files are now only removed after confirming destination existence and size, with automatic rollback if copy fails.
- **Recursive Move Prevention**: Added canonical path parent checks preventing moving or copying directories into themselves or any of their descendants.
- **Biometric Hardware Authentication**: Migrated `MainActivity` to `androidx.fragment.app.FragmentActivity`, resolving biometric authentication failures across Android 10+.
- **All-Files-Access Permission Flow**: Added a dedicated storage permission onboarding screen with one-tap CTA directing users to system storage settings when `MANAGE_EXTERNAL_STORAGE` is not yet granted.
- **Foreground Service Lifecycle**: Replaced premature `startForegroundService` call in `MusicPlaybackManager` with on-demand service binding, eliminating `ForegroundServiceDidNotStartInTimeException` crashes.
- **Dead File Browser Controls**: Fully connected the (+) FAB to `NovaInputDialog` folder creation, connected selection dock actions (Copy, Move, Rename, Delete), and added floating clipboard paste bar.
- **Live Dashboard Storage Metrics**: Connected real `FileSystemDataSource` storage statistics, live directories, and live recent files to `NovaHomeDashboard`, replacing static placeholders.
- **Theme Persistence**: Connected `SettingsPreferencesRepository` in `MainActivity` ensuring selected theme mode and accent color persist across app restarts.
- **Data Integrity & Simulation Removal**: Removed 19.4 MB synthetic junk file generator (`sample_junk_cache`) and removed hardcoded mock deleted items from storage analysis.
- **Backup Data Protection**: Set `android:allowBackup="false"` in `AndroidManifest.xml` to prevent extraction of PBKDF2 vault credentials via ADB backup.
- **Dependency Acknowledgements**: Corrected About screen credits to accurately list Compose ("Declarative UI") instead of Room.

### Changed
- Configured automated release signing via `local.properties` and environment variables with official `novafiles-release.jks` keystore.
- Upgraded release build configuration to `versionCode = 10`, `versionName = "2.0.0-alpha2"`.

## [v2.0.0-alpha.1] - 2026-11-07

### Added
- **Multi-Module Clean Architecture**: Rewrote the entire application from the ground up into 17 decoupled Gradle modules (`:app`, `:core:common`, `:core:model`, `:core:designsystem`, `:core:ui`, `:core:navigation`, `:core:storage`, `:core:media`, `:core:security`, `:data:files`, `:feature:files`, `:feature:dualpane`, `:feature:music`, `:feature:video`, `:feature:gallery`, `:feature:vault`, `:feature:analyzer`, `:feature:cleaner`, `:feature:ftp`, `:feature:settings`).
- **Signature Dark Navy Design Language**: Implemented modern Dark Navy base aesthetic (`#0B1226`, `#111B3A`, `#1A2547`, `#2A3A6A`, `#5B6EF5`) with 18 dynamic theme variations across Dark Navy, AMOLED Pure Black, and Clean Light modes with 6 accent palettes (Indigo, Violet, Cyan, Green, Orange, Pink).
- **Dual-Pane Desktop-Grade File Browser**: Independent side-by-side pane directory navigation, synchronized folder viewing, drag-and-drop file transfers, and single-tap cross-pane copy/move.
- **Transactional File Operations Engine**: Atomic copy, move, rename, batch delete, and archive operations with rollback safety and automated filename collision resolution (`(1)`, `(2)`).
- **Storage Analyzer**: Visual storage capacity breakdown with animated circular usage gauge, multi-segment category distribution bar (Photos, Videos, Audio, Documents, APKs, System/Archives), large files scanner (>50 MB), and trash bin monitor.
- **Storage Cleaner**: Deep storage scanning for application cache, system logs, empty directories, obsolete APK packages, and MD5-hash verified duplicate files.
- **AndroidX Media3 Music Player**: Complete `MediaSessionService` implementation with persistent playback notification, dynamic album art, lock-screen controls, background playback, and repeat/shuffle queues.
- **Interactive Home-Screen App Widgets**: Jetpack Glance powered Compact and Expanded music player widgets connected directly to real MediaSession state with playback controls.
- **Cinematic Gesture Video Player**: Hardware-accelerated ExoPlayer implementation featuring vertical swipe brightness control (left edge), volume control (right edge), horizontal swipe timeline scrubbing, double-tap seek, and auto-hiding frosted glass playback controls.
- **High-Performance Photo Gallery**: Responsive multi-column grid, full-screen swipeable image pager with smooth pinch-to-zoom, pan, rotation, and EXIF metadata viewer.
- **Hardware-Backed AES-256 Secure Vault**: End-to-end file encryption using AES-256-GCM with unique 12-byte initialization vectors (IV), PBKDF2-HMAC-SHA256 password derivation (65,536 iterations), Biometric Prompt (fingerprint/face) unlock, duress PIN wipe triggers, and `NOVAV1` header authentication.
- **Embedded Wi-Fi FTP Server**: RFC 959 compliant wireless file transfer server supporting active and passive (`PASV`, `EPSV`) data modes, custom ports, anonymous or authenticated logins, canonical path-traversal sandboxing, and persistent notification status.
- **Built-in Code & Text Editor**: Monospace code viewing and editing engine supporting `.txt`, `.md`, `.json`, `.xml`, `.log`, `.gradle`, `.kt`, and `.html` with non-blocking coroutine background saving.
- **R8 ProGuard & Shrinking Configuration**: Production R8 optimization reducing APK binary footprint from ~25.1 MB debug down to **4.9 MB release** (~80% size reduction) with 100% vital lint pass.
- **Full Module Unit Test Suite**: Comprehensive unit tests covering domain models, ViewModels, encryption ciphers, path normalizers, and repository streams.

### Changed
- Migrated legacy single-module Java/Kotlin architecture to modern Kotlin 2.0+ and Jetpack Compose BOM 2024.10+.
- Replaced custom media notification with official AndroidX Media3 `MediaNotificationProvider` for lock screen and Bluetooth AVRCP support.
- Modernized all storage access to adhere to Android 14/15/16 Storage Access Framework (SAF) and scoped MediaStore guidelines.

### Security
- Added secure PBKDF2 key derivation and AES-GCM tag verification preventing unauthorized decryption or payload manipulation.
- Implemented canonical sandbox checks in Wi-Fi FTP server preventing directory traversal above the configured root.
- Vault files are encrypted in-memory and wiped from temporary memory buffers immediately upon operation completion.
