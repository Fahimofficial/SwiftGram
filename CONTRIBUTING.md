# Contributing to SwiftGram

Thank you for your interest in contributing to SwiftGram! This document provides guidelines and instructions for contributing to the project.

## Code of Conduct

We are committed to providing a welcoming and inclusive environment for all contributors. Please be respectful, constructive, and professional in all interactions.

## Getting Started

### Prerequisites

Before you begin, ensure you have the following installed:

- **Android Studio** (latest stable version)
- **JDK 17** or later
- **Git** for version control
- **Gradle** (included with Android Studio)

### Setting Up Your Development Environment

1. **Fork the Repository** – Click the "Fork" button on GitHub to create your own copy of the repository.

2. **Clone Your Fork** – Clone the forked repository to your local machine:
   ```bash
   git clone https://github.com/YOUR_USERNAME/SwiftGram.git
   cd SwiftGram
   ```

3. **Add Upstream Remote** – Add the original repository as an upstream remote:
   ```bash
   git remote add upstream https://github.com/Fahimofficial/SwiftGram.git
   ```

4. **Create a Feature Branch** – Create a new branch for your feature or bugfix:
   ```bash
   git checkout -b feature/your-feature-name
   ```

5. **Open in Android Studio** – Open the project in Android Studio and let it sync Gradle files.

## Development Workflow

### Branching Strategy

We follow a simplified Git Flow branching model:

- **main** – Production-ready code. Merges only via pull requests after review.
- **develop** – Development branch. Integration point for features.
- **feature/*** – Feature branches. Created from `develop`, merged back via pull request.
- **bugfix/*** – Bugfix branches. Created from `develop` for non-critical fixes.
- **hotfix/*** – Hotfix branches. Created from `main` for critical production fixes.

### Commit Messages

Write clear, descriptive commit messages following this format:

```
type(scope): subject

body (optional)

footer (optional)
```

**Types:**
- `feat` – A new feature
- `fix` – A bug fix
- `docs` – Documentation changes
- `style` – Code style changes (formatting, missing semicolons, etc.)
- `refactor` – Code refactoring without feature changes
- `perf` – Performance improvements
- `test` – Adding or updating tests
- `chore` – Build process, dependencies, or tooling changes

**Examples:**
```
feat(auth): implement phone number verification

Add SMS and Telegram code verification for authentication flow.
Includes input validation and error handling.

Closes #123
```

```
fix(chat): resolve message ordering issue

Messages were being displayed in incorrect order due to timestamp
comparison bug. Fixed by using proper Long comparison.

Fixes #456
```

## Code Style & Standards

### Kotlin Style Guide

We follow the official [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html). Key points:

- Use 4 spaces for indentation (no tabs)
- Keep lines under 120 characters when possible
- Use meaningful variable and function names
- Add KDoc comments for public APIs

### Android Best Practices

- Follow [Android Architecture Components](https://developer.android.com/topic/architecture) patterns
- Use Jetpack libraries (Compose, ViewModel, LiveData, Room, etc.)
- Implement proper error handling and logging
- Write testable code with dependency injection (Hilt)
- Use coroutines for asynchronous operations

### Code Organization

```
app/
├── ui/
│   ├── screens/
│   ├── components/
│   ├── theme/
│   └── viewmodels/
├── navigation/
├── di/
└── MainActivity.kt

data/
├── repository/
├── local/
├── remote/
└── di/

domain/
├── model/
├── repository/
├── usecase/
└── di/

core/
├── utils/
├── di/
└── common/
```

## Testing

### Unit Tests

Write unit tests for business logic in the `domain` and `data` layers:

```bash
./gradlew testDebugUnitTest
```

### Instrumented Tests

Write UI tests for Compose screens:

```bash
./gradlew connectedAndroidTest
```

### Test Coverage

Aim for at least 70% code coverage for critical paths. Use Android Studio's built-in coverage tools to measure.

## Pull Request Process

### Before Submitting

1. **Sync with Upstream** – Ensure your branch is up-to-date:
   ```bash
   git fetch upstream
   git rebase upstream/develop
   ```

2. **Run Local Checks** – Execute lint and tests:
   ```bash
   ./gradlew lint testDebugUnitTest
   ```

3. **Build Successfully** – Ensure the project builds without errors:
   ```bash
   ./gradlew build
   ```

### Submitting a Pull Request

1. **Push Your Branch** – Push your feature branch to your fork:
   ```bash
   git push origin feature/your-feature-name
   ```

2. **Create Pull Request** – Go to GitHub and create a pull request against the `develop` branch (or `main` for hotfixes).

3. **Fill PR Template** – Provide a clear description:
   - What problem does this solve?
   - How does it solve the problem?
   - Any breaking changes?
   - Screenshots or videos (if applicable)

4. **Link Issues** – Reference related issues using `Closes #123` or `Fixes #456`.

### PR Review Checklist

Your PR will be reviewed for:

- **Code Quality** – Follows style guide and best practices
- **Functionality** – Correctly implements the intended feature
- **Testing** – Includes appropriate tests
- **Documentation** – Updates README or docs if needed
- **Performance** – No unnecessary performance regressions
- **Security** – No security vulnerabilities introduced

## Documentation

### README Updates

If your changes affect how users interact with the app, update the README.md with:

- New features or capabilities
- Changed API or configuration
- Setup instructions (if applicable)

### Code Comments

Add comments for complex logic:

```kotlin
// Calculate message offset for pagination
// Offset = (page - 1) * pageSize
val offset = (page - 1) * PAGE_SIZE
```

### KDoc for Public APIs

```kotlin
/**
 * Sends a message to the specified chat.
 *
 * @param chatId The ID of the chat
 * @param text The message text
 * @return The sent message or null if failed
 * @throws IllegalArgumentException if chatId is invalid
 */
suspend fun sendMessage(chatId: Long, text: String): Message?
```

## Issue Reporting

### Bug Reports

When reporting a bug, include:

- **Description** – Clear description of the issue
- **Steps to Reproduce** – Exact steps to reproduce the bug
- **Expected Behavior** – What should happen
- **Actual Behavior** – What actually happens
- **Device Info** – Android version, device model, app version
- **Logs** – Relevant error logs or stack traces
- **Screenshots** – Visual evidence if applicable

### Feature Requests

For feature requests, provide:

- **Description** – Clear description of the feature
- **Use Case** – Why is this feature needed?
- **Proposed Solution** – How should it work?
- **Alternatives** – Any alternative approaches

## Development Tips

### Useful Gradle Commands

```bash
# Build the project
./gradlew build

# Run linting
./gradlew lint

# Run unit tests
./gradlew testDebugUnitTest

# Run instrumented tests
./gradlew connectedAndroidTest

# Build and install debug APK
./gradlew installDebug

# Clean build
./gradlew clean build

# Generate dependency tree
./gradlew dependencies
```

### Debugging

- Use Android Studio's debugger to step through code
- Enable verbose logging: `adb logcat *:V`
- Use Logcat filters to find relevant logs
- Check the `.manus-logs/` directory for dev server logs

### Performance Profiling

- Use Android Profiler for CPU, memory, and network analysis
- Use Compose Layout Inspector to debug UI hierarchy
- Monitor frame rate with `adb shell dumpsys gfxinfo`

## Release Process

### Version Numbering

We follow [Semantic Versioning](https://semver.org/):

- **MAJOR** – Breaking changes
- **MINOR** – New features (backward compatible)
- **PATCH** – Bug fixes

Example: `1.2.3`

### Creating a Release

1. Update version in `build.gradle.kts`
2. Update `CHANGELOG.md` with release notes
3. Create a git tag: `git tag v1.2.3`
4. Push tag: `git push origin v1.2.3`
5. GitHub Actions will automatically build and create a release

## Community & Support

- **GitHub Issues** – Report bugs and request features
- **GitHub Discussions** – Ask questions and discuss ideas
- **Pull Requests** – Submit code contributions

## License

By contributing to SwiftGram, you agree that your contributions will be licensed under the same license as the project (typically MIT or GPL).

## Questions?

If you have questions or need clarification, feel free to:

- Open a GitHub Discussion
- Comment on a relevant Issue
- Contact the maintainers

Thank you for contributing to SwiftGram! 🚀
