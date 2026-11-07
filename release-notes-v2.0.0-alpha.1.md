# NovaFiles 2.0 Alpha 1 (v2.0.0-alpha.1) 🚀

We are thrilled to announce the first public Alpha release of **NovaFiles 2.0** — a ground-up architectural rewrite of the NovaFiles file manager for Android, engineered using modern Kotlin, Jetpack Compose, Clean Architecture, and AndroidX Media3.

NovaFiles 2.0 has been redesigned from a blank canvas to deliver uncompromising performance, security, and a signature **Dark Navy** visual aesthetic (`#0B1226`, `#111B3A`, `#1A2547`, `#2A3A6A`, `#5B6EF5`).

---

### 🌟 Key Highlights & Capabilities

#### 🏗️ Ground-Up Multi-Module Clean Architecture
- **17 Cohesive Gradle Modules**: Modular separation of concerns across core utilities, domain models, design system, storage engines, media services, security ciphers, and feature UI.
- **100% Jetpack Compose**: Modern declarative UI with single-source-of-truth state management and fluid motion transitions.
- **R8 Whole-Program Optimization**: Release build optimized from ~25.1 MB debug footprint down to **4.9 MB (~80% size reduction)**.

#### 🎨 18 Dynamic Color & Theme Variations
- **Dark Navy (Default)**: Deep midnight blue surfaces with slate elevation and cobalt borders.
- **AMOLED Pure Black**: True `#000000` dark mode for battery savings on OLED displays.
- **Clean Light**: High-contrast slate canvas for bright daylight environments.
- **6 Accent Palettes**: Indigo, Violet, Cyan, Green, Orange, and Pink.

#### 🗂️ Desktop-Class Dual-Pane File Management
- **Side-by-Side Dual-Pane Browser**: Independent directory navigation, synchronized folder views, and rapid single-tap cross-pane Copy/Move.
- **Transactional File Engine**: Atomic file operations with automatic rollback safety on failures and collision handling (`(1)`, `(2)`).

#### 🎵 Android Native Media Studio & Home-Screen Widgets
- **AndroidX Media3 Integration**: Complete `MediaSessionService` implementation with background audio playback, lock-screen controls, and system media notifications.
- **Interactive App Widgets**: Jetpack Glance powered Compact and Expanded home-screen music widgets reflecting live MediaSession state.
- **Cinematic Video Player**: Hardware-accelerated ExoPlayer with intuitive edge gestures for volume (right edge), brightness (left edge), and horizontal seek.
- **Photo Gallery**: Fullscreen image pager with smooth pinch-to-zoom, pan, and EXIF inspection.

#### 🧹 Deep Storage Optimization Suite
- **Storage Analyzer**: Visual gauge metrics, multi-segment category allocation, and large-file scanner (>50 MB).
- **Storage Cleaner**: Deep-clean scanning for application cache, crash logs, empty folders, obsolete APK installers, and MD5-verified duplicates.

#### 🔒 Cryptographic Secure Vault & Wi-Fi FTP
- **Hardware-Backed AES-256-GCM Vault**: End-to-end file encryption with PBKDF2 key derivation (65,536 iterations), Biometric Prompt unlock, and `NOVAV1` header authentication.
- **RFC 959 Wi-Fi FTP Server**: Embedded local wireless file transfer server supporting active and passive (`PASV`, `EPSV`) transfers with canonical path-traversal sandboxing.

---

### 📱 Physical Device Verification
NovaFiles 2.0 has been deployed and verified on physical hardware running **Android 17 / SDK 37 (Samsung Galaxy SM-S942B)**:
- Storage Cleaner live internal storage scan: identified **2.1 GB of reclaimable junk** and **35 obsolete APK packages**.
- Real-time dynamic theme switcher verified across all 18 theme variations.
- Media3 notification and lock-screen playback verified.

---

### ⚠️ Installation & Upgrade Warning
> **IMPORTANT**: NovaFiles 2.0 is a complete rewrite built with a new database architecture and release signing key.
> Upgrades over legacy v1.x builds (v1.0.0-beta1 .. v1.0.0-rc.2-build3) are **not supported in-place** and will fail with `INSTALL_FAILED_UPDATE_INCOMPATIBLE`.
> **You must completely uninstall any previous version of NovaFiles before installing NovaFiles 2.0.**

---

### 📦 Release Assets & Checksums

| Package | Filename | Size | SHA-256 Checksum |
|---|---|---|---|
| **Signed Release APK** | `NovaFiles-v2.0.0-alpha.1.apk` | 4.91 MB (4,910,489 bytes) | `914A232198EB800D2676F5986B74C7A343F90CE29CA631895D545F92960C4D46` |
| **Signed Release AAB** | `NovaFiles-v2.0.0-alpha.1.aab` | 9.05 MB (9,050,635 bytes) | `6817928E4EF01F812C2F69B32DE98FB53B755F8022E447FA6F5CC877A20C3BB0` |

---

### 👨‍💻 Developer & Contact Attribution
- **Developer**: Abhinav Kishore (Xan45)
- **Built Using**: Google Antigravity
- **Contact & Support**: [xandronhq@gmail.com](mailto:xandronhq@gmail.com)
