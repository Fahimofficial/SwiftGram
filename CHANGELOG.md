# Changelog

All notable changes to SwiftGram will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- Initial project structure with Clean Architecture
- TDLib integration for Telegram API
- Authentication flow (phone, code, password, registration)
- Chat list and messaging UI
- User profiles and settings
- Material 3 theming with dark mode support
- Redux state management
- GitHub Actions CI/CD workflows

### Changed
- Improved UI/UX with Jetpack Compose
- Enhanced error handling and logging

### Fixed
- Fixed navigation issues
- Improved performance

### Security
- Encrypted storage for sensitive data
- Secure API communication
- Input validation and sanitization

## [1.0.0] - 2026-05-13

### Added
- Initial release of SwiftGram
- Complete authentication system
- Chat messaging functionality
- User profile management
- Settings and preferences
- Dark mode support
- Offline message caching
- Push notifications (Firebase)
- Message search and filters
- Chat folders and pinning
- Privacy features (app lock, hidden chats)

### Features
- **Ultra-smooth UI** – Optimized animations and transitions
- **Advanced customization** – Fonts, bubbles, layout density options
- **Minimalist mode** – Distraction-free messaging
- **Smart message summarization** – AI-powered message summaries (optional)
- **Gesture-based navigation** – Intuitive swipe gestures

### Performance
- Optimized for large chats (10,000+ messages)
- Efficient memory management
- Fast message loading and rendering
- Smooth scrolling performance

### Security
- End-to-end encryption support
- Secure token storage
- Encrypted local database
- Privacy-focused design

## Version Format

Versions follow [Semantic Versioning](https://semver.org/):
- **MAJOR** – Breaking changes
- **MINOR** – New features (backward compatible)
- **PATCH** – Bug fixes

## How to Release

1. Update version in `app/build.gradle.kts`:
   ```kotlin
   versionCode = 2
   versionName = "1.1.0"
   ```

2. Update this CHANGELOG.md with changes

3. Commit changes:
   ```bash
   git add .
   git commit -m "Release v1.1.0"
   ```

4. Create and push tag:
   ```bash
   git tag v1.1.0
   git push origin main
   git push origin v1.1.0
   ```

5. GitHub Actions will automatically:
   - Build release APK
   - Create GitHub release
   - Upload APK and AAB

## Links

- [GitHub Repository](https://github.com/Fahimofficial/SwiftGram)
- [Issue Tracker](https://github.com/Fahimofficial/SwiftGram/issues)
- [Discussions](https://github.com/Fahimofficial/SwiftGram/discussions)
