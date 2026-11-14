# Contributing to NovaFiles

Thank you for your interest in contributing to NovaFiles! We welcome contributions from the community to make NovaFiles a better, faster, and more versatile file manager.

## Code of Conduct

Please treat everyone with respect, courtesy, and professionalism. Collaborative, constructive discussions make this project thrive.

## Getting Started

### Prerequisites
- **Android Studio**: Android Studio Koala (2024.1) or newer recommended.
- **JDK**: Java Development Kit 17 (Eclipse Temurin 17 or OpenJDK 17).
- **Android SDK**: API Level 36 SDK platform and build tools.
- **Git**: For version control.

### Building the Project
1. **Fork and Clone**:
   ```bash
   git clone https://github.com/<your-username>/NovaFiles-2.0.git
   cd NovaFiles-2.0
   ```
2. **Open in Android Studio**:
   - Open Android Studio and select `Open` -> choose the cloned directory.
   - Wait for Gradle sync to complete.
3. **Run Unit Tests**:
   ```bash
   ./gradlew test
   ```
4. **Build Debug APK**:
   ```bash
   ./gradlew assembleDebug
   ```

## Development Guidelines

### Architecture & Style
- **Architecture**: Adhere strictly to Clean Architecture and MVVM patterns. UI logic lives in Jetpack Compose, state holders are AndroidX ViewModels, and data access is mediated through Repositories.
- **Kotlin**: Follow official Kotlin coding conventions. Use expressive, null-safe idioms, coroutines for asynchronous work, and StateFlow for observable UI state.
- **Design System**: Use `NovaTheme` design tokens (`NovaTheme.colors`, `NovaTheme.typography`, `NovaTheme.shapes`, `NovaTheme.elevation`) rather than hardcoded Material colors or sizes.
- **Testing**: Add unit tests for new business logic, view models, and utility classes in the respective module's `src/test/java` directory.

### Submitting Pull Requests
1. Create a descriptive feature branch:
   ```bash
   git checkout -b feature/my-new-feature
   ```
2. Commit changes with clear, semantic commit messages (e.g., `feat(files): add sorting by file extension`).
3. Ensure all tests pass and lint checks succeed:
   ```bash
   ./gradlew test
   ./gradlew lintDebug
   ```
4. Push your branch to your fork and submit a Pull Request against the `main` branch.
5. Provide a clear PR summary outlining what was changed, why, and how it was tested.
