# NovaFiles 2.0

> **Next-Generation Android File Manager, Media Studio & Storage Optimization Suite**  
> Built with modern Kotlin, Jetpack Compose, Clean Architecture, and AndroidX Media3.

[![Kotlin](https://img.shields.io/badge/Kotlin-2.0+-blue.svg?logo=kotlin)](https://kotlinlang.org)
[![Android Gradle Plugin](https://img.shields.io/badge/AGP-8.7+-green.svg?logo=android)](https://developer.android.com/studio/releases/gradle-plugin)
[![Jetpack Compose](https://img.shields.io/badge/Compose-BOM%202024.10-purple.svg?logo=jetpackcompose)](https://developer.android.com/jetpack/compose)
[![Architecture](https://img.shields.io/badge/Architecture-Clean%20%2B%20MVVM-orange.svg)]()
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)

---

## 🌟 Overview

**NovaFiles 2.0** is a clean-slate, ground-up rewrite engineered from the approved architectural blueprint. It replaces legacy file management systems with a decoupled, reactive, multi-module architecture adhering strictly to Google's recommended Modern Android Architecture standards.

Designed around a signature **Dark Navy** aesthetic (`#0B1226`, `#111B3A`, `#1A2547`, `#2A3A6A`, `#5B6EF5`), NovaFiles 2.0 pairs high-performance storage indexing with a desktop-grade feature set.

---

## 🎨 Visual Design Language & Themes

NovaFiles 2.0 implements an 18-variation design engine supporting dynamic real-time theme swapping:

* **Theme Modes:**
  * **Dark Navy (Default):** Deep midnight blue canvas (`#0B1226`), elevated slate surfaces (`#111B3A`, `#1A2547`), subtle borders (`#2A3A6A`).
  * **AMOLED Pure Black:** Pure `#000000` base for maximum power efficiency on OLED panels.
  * **Clean Light:** High-contrast neutral slate (`#F8FAFC`) with crisp card surfaces (`#FFFFFF`).
* **Accent Colors:** Indigo (`#5B6EF5`), Violet (`#8A6CFF`), Cyan (`#2DD4BF`), Green (`#4ADE80`), Orange (`#FF9F43`), Pink (`#F472B6`).
* **Category Color Matrix:** Photos (Purple `#8A6CFF`), Videos (Coral Red `#FF6B6B`), Documents (Blue `#3B82F6`), Audio (Warm Yellow `#FFD166`), APKs (Emerald `#10B981`), Archives (Magenta `#EC4899`).

---

## 🏗️ Architecture & Modular Structure

The codebase is organized into **17 cohesive, decoupled Gradle modules**:

```text
NovaFiles-2.0/
├── app/                  # Application bootstrap, navigation graph, app widgets
├── core/
│   ├── common/           # Result types, formatters, coroutine dispatchers
│   ├── model/            # Domain models (FileItem, StorageStats, FileType, SortConfig)
│   ├── designsystem/     # Color tokens, typography, shapes, NovaTheme (18 styles)
│   ├── ui/               # Reusable UI components (NovaCard, NovaButton, StorageBar, etc.)
│   ├── navigation/       # Type-safe Serializable navigation destinations
│   ├── storage/          # SAF, MediaStore engine, transactional file operations
│   ├── media/            # AndroidX Media3 player service & MediaSession engine
│   └── security/         # AES-256-GCM cipher, PBKDF2 key derivation, lockout manager
├── data/
│   └── files/            # Reactive FileRepository, indexing & search streams
└── feature/
    ├── files/            # Home dashboard, file browser, text/code editor
    ├── dualpane/         # Dual-pane side-by-side file manager & drag-transfer
    ├── music/            # Media3 music player, playlist queue & lyric view
    ├── video/            # Cinematic gesture video player (volume, brightness, seek)
    ├── gallery/          # High-performance photo gallery & pinch-zoom viewer
    ├── vault/            # Hardware-backed AES-256 encrypted secure storage
    ├── analyzer/         # Storage breakdown, category distribution & large files
    ├── cleaner/          # Junk cleaner (app cache, logs, obsolete APKs, duplicates)
    ├── ftp/              # RFC 959 Wi-Fi FTP server with foreground service
    └── settings/         # Theme customizer, security locks & app preferences
```

---

## ⚡ Key Features

### 1. Dual-Pane Desktop-Grade File Management
* Independent, side-by-side directory navigation.
* Fast folder swapping, synchronized navigation, and single-tap cross-pane copy/move.
* Transactional file operations engine with automatic rollback on failure.

### 2. Media Studio
* **Music Player:** Fully integrated with AndroidX Media3 `MediaSessionService`. Persistent notification with dynamic album art, lock-screen playback controls, and background playback.
* **App Widgets:** Compact and Expanded interactive Android home-screen widgets reflecting real MediaSession state.
* **Video Player:** Hardware-accelerated ExoPlayer with intuitive swipe gestures for brightness (left edge), volume (right edge), and scrubbing (horizontal drag).
* **Photo Gallery:** Grid/detail browsing with smooth pinch-to-zoom and rotation.

### 3. Storage Optimization & Analytics
* **Storage Analyzer:** Circular usage gauge, multi-segment category allocation bar, large files scanner (>50MB), and trash bin monitor matching the visual reference.
* **Storage Cleaner:** Automated scans for system/app cache, temporary crash logs, obsolete APK installers, empty directories, and MD5-verified duplicate files.

### 4. Hardware-Backed AES-256 Secure Vault
* AES-256-GCM authenticated encryption with unique 12-byte IVs.
* PBKDF2-HMAC-SHA256 password derivation (65,536 iterations).
* `NOVAV1` file encapsulation header for foolproof integrity checks.
* Duress PIN support and Biometric (Fingerprint/Face) hardware authentication.

### 5. Wi-Fi FTP Server
* Embedded RFC 959 compliant FTP server supporting passive (`PASV`, `EPSV`) data transfers.
* Canonical sandbox validation to prevent path-traversal attacks.
* Persistent foreground notification (`novafiles_ftp_channel`) with one-tap shutdown.

### 6. Built-in Code & Text Editor
* Monospace viewer and editor supporting `.txt`, `.md`, `.json`, `.xml`, `.log`, `.gradle`, `.kt`, `.html`.
* Non-blocking background coroutine IO persistence.

---

## 🧪 Testing & Quality Assurance

Unit test suites cover domain logic, view models, formatters, and security ciphers across all 17 modules:

* `:core:common` — Human-readable byte formatting, dates, path normalizers.
* `:core:model` — File classification by MIME and extension.
* `:core:designsystem` — Verification of all 18 theme and accent variations.
* `:core:navigation` — Navigation route serializability and argument decoding.
* `:core:security` — Round-trip AES-256-GCM encryption, decryption, invalid key rejection, tampering detection.
* `:core:storage` — Transactional file operations, collision resolution.
* `:data:files` — Reactive FileRepository streams and sorting.
* `:feature:files` — FilesViewModel navigation, multi-selection, and search queries.
* `:feature:dualpane` — DualPaneViewModel concurrent navigation, swapping, and sync.
* `:feature:music` — Playback state machine, repeat modes, queue shuffling.
* `:feature:video` — Aspect ratios, time formatting, player state transitions.
* `:feature:gallery` — Image media grouping, date formatting.
* `:feature:vault` — Master password hashing, lockout logic, duress PIN triggers.
* `:feature:analyzer` — Storage calculations, percentage distributions.
* `:feature:cleaner` — Junk item categorization, total reclaimable space logic.
* `:feature:ftp` — Server configuration defaults, anonymous access policies.
* `:feature:settings` — Persistent settings preferences and default mappings.

To execute the test suite:
```bash
./gradlew test
```

---

## 📦 Building & Release

### Debug Build
```bash
./gradlew assembleDebug
```

### Production Release Build (R8 / Minified & Shrunk)
NovaFiles 2.0 uses R8 whole-program optimization and resource shrinking:
* **Debug APK Size:** ~25.1 MB
* **Release APK Size:** **~4.9 MB (~80% reduction)**
* **Zero Lint Vital Errors**

```bash
./gradlew assembleRelease
```

The resulting signed APK is placed at:
`app/build/outputs/apk/release/app-release.apk`

---

## 📄 License
Licensed under the Apache License, Version 2.0.
