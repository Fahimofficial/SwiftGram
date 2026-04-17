# SwiftGram Project Structure

This document outlines the planned project structure and key files for the SwiftGram Android client.

## Top-Level Project Structure

```
SwiftGram/
├── .github/                       # GitHub Actions workflows
│   └── workflows/
│       └── android_ci.yml         # CI/CD pipeline for builds, tests, and releases
├── app/                           # Android application module
├── build.gradle.kts               # Top-level Gradle build file
├── data/                          # Data layer module
├── domain/                        # Domain layer module
├── core/                          # Core utilities and common components module
├── gradle/                        # Gradle wrapper files
├── gradlew
├── gradlew.bat
├── local.properties               # Local development properties (ignored by Git)
├── settings.gradle.kts            # Gradle settings file
└── README.md                      # Project documentation
```

## Module Structures

### `app` Module

This module contains the Android application entry point, UI components, and navigation logic.

```
app/
├── src/
│   ├── main/
│   │   ├── AndroidManifest.xml
│   │   ├── java/com/swiftgram/app/
│   │   │   ├── SwiftGramApplication.kt    # Application class
│   │   │   ├── MainActivity.kt            # Main activity for Jetpack Compose UI
│   │   │   ├── navigation/                # Navigation graph and routes
│   │   │   │   └── AppNavHost.kt
│   │   │   ├── ui/
│   │   │   │   ├── screens/               # UI screens (ChatList, Chat, Settings)
│   │   │   │   │   ├── ChatListScreen.kt
│   │   │   │   │   ├── ChatScreen.kt
│   │   │   │   │   └── SettingsScreen.kt
│   │   │   │   ├── components/            # Reusable UI components
│   │   │   │   ├── theme/                 # Material You theming
│   │   │   │   │   ├── Color.kt
│   │   │   │   │   ├── Theme.kt
│   │   │   │   │   └── Type.kt
│   │   │   │   └── viewmodels/            # UI-specific ViewModels
│   │   │   │       ├── ChatListViewModel.kt
│   │   │   │       ├── ChatViewModel.kt
│   │   │   │       └── SettingsViewModel.kt
│   │   │   └── di/                        # Dependency Injection setup (e.g., Hilt)
│   │   │       └── AppModule.kt
│   │   └── res/                           # Android resources (layouts, drawables, values)
│   └── test/                              # Unit tests for app module
├── build.gradle.kts                       # Gradle build file for app module
└── proguard-rules.pro                     # ProGuard rules
```

### `domain` Module

This module contains the business logic, use cases, and entity models, independent of any specific framework.

```
domain/
├── src/
│   ├── main/
│   │   └── java/com/swiftgram/domain/
│   │       ├── model/                     # Data models/entities
│   │       │   ├── Chat.kt
│   │       │   ├── Message.kt
│   │       │   └── User.kt
│   │       ├── repository/                # Interfaces for data repositories
│   │       │   ├── ChatRepository.kt
│   │       │   ├── MessageRepository.kt
│   │       │   └── UserRepository.kt
│   │       └── usecase/                   # Business logic use cases
│   │           ├── chat/
│   │           │   ├── GetChatsUseCase.kt
│   │           │   └── SendMessageUseCase.kt
│   │           └── user/
│   │               └── GetCurrentUserUseCase.kt
│   └── test/                              # Unit tests for domain module
└── build.gradle.kts                       # Gradle build file for domain module
```

### `data` Module

This module provides implementations for the repository interfaces defined in the `domain` module, handling data sources (network, database, local storage).

```
data/
├── src/
│   ├── main/
│   │   └── java/com/swiftgram/data/
│   │       ├── local/                     # Local data sources (Room, SharedPreferences)
│   │       │   ├── dao/
│   │       │   ├── entity/
│   │       │   └── SwiftGramDatabase.kt
│   │       ├── remote/                    # Remote data sources (Telegram API, Firebase)
│   │       │   ├── api/
│   │       │   │   └── TelegramApiService.kt # Wrapper for TDLib or similar
│   │       │   └── dto/
│   │       ├── repository/                # Implementations of domain repositories
│   │       │   ├── ChatRepositoryImpl.kt
│   │       │   ├── MessageRepositoryImpl.kt
│   │       │   └── UserRepositoryImpl.kt
│   │       └── mapper/                    # Data mappers between DTOs/Entities and Domain Models
│   │           ├── ChatMapper.kt
│   │           └── MessageMapper.kt
│   └── test/                              # Unit tests for data module
└── build.gradle.kts                       # Gradle build file for data module
```

### `core` Module

This module contains common utilities, extensions, base classes, and shared resources that can be used across other modules.

```
core/
├── src/
│   ├── main/
│   │   └── java/com/swiftgram/core/
│   │       ├── common/                    # Common utilities and constants
│   │       ├── di/                        # Core DI modules
│   │       ├── extensions/                # Kotlin extension functions
│   │       └── utils/                     # General utility classes
│   └── test/                              # Unit tests for core module
└── build.gradle.kts                       # Gradle build file for core module
```

## Key Kotlin Files (Examples)

### `app/src/main/java/com/swiftgram/app/MainActivity.kt`

```kotlin
package com.swiftgram.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.swiftgram.app.navigation.AppNavHost
import com.swiftgram.app.ui.theme.SwiftGramTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SwiftGramTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    AppNavHost(navController = navController)
                }
            }
        }
    }
}
```

### `app/src/main/java/com/swiftgram/app/ui/viewmodels/ChatListViewModel.kt`

```kotlin
package com.swiftgram.app.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.swiftgram.domain.model.Chat
import com.swiftgram.domain.usecase.chat.GetChatsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatListViewModel @Inject constructor(
    private val getChatsUseCase: GetChatsUseCase
) : ViewModel() {

    private val _chats = MutableStateFlow<List<Chat>>(emptyList())
    val chats: StateFlow<List<Chat>> = _chats

    init {
        loadChats()
    }

    private fun loadChats() {
        viewModelScope.launch {
            getChatsUseCase().collect { chats ->
                _chats.value = chats
            }
        }
    }

    // Other chat list related logic
}
```

### `data/src/main/java/com/swiftgram/data/repository/ChatRepositoryImpl.kt`

```kotlin
package com.swiftgram.data.repository

import com.swiftgram.data.local.dao.ChatDao
import com.swiftgram.data.mapper.ChatMapper
import com.swiftgram.data.remote.api.TelegramApiService
import com.swiftgram.domain.model.Chat
import com.swiftgram.domain.repository.ChatRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ChatRepositoryImpl @Inject constructor(
    private val telegramApiService: TelegramApiService,
    private val chatDao: ChatDao,
    private val chatMapper: ChatMapper
) : ChatRepository {

    override fun getChats(): Flow<List<Chat>> {
        // Example: Fetch from remote, save to local, then expose local as single source of truth
        return chatDao.getAllChats().map { entities ->
            entities.map { chatMapper.mapToDomain(it) }
        }
    }

    override suspend fun getChatById(chatId: Long): Chat? {
        val chatEntity = chatDao.getChatById(chatId)
        return chatEntity?.let { chatMapper.mapToDomain(it) }
    }

    override suspend fun sendMessage(chatId: Long, text: String) {
        telegramApiService.sendMessage(chatId, text)
        // Update local cache if necessary
    }

    // Other chat related repository implementations
}
```

## GitHub Actions Workflow (`.github/workflows/android_ci.yml`)

```yaml
name: Android CI

on:
  push:
    branches:
      - main
    tags:
      - 'v*.*.*' # Trigger on version tags like v1.0.0
  pull_request:
    branches:
      - main

jobs:
  build:
    runs-on: ubuntu-latest

    steps:
    - name: Checkout code
      uses: actions/checkout@v4

    - name: Set up JDK 17
      uses: actions/setup-java@v4
      with:
        distribution: 'temurin'
        java-version: '17'

    - name: Grant execute permission for gradlew
      run: chmod +x gradlew

    - name: Build debug APK
      run: ./gradlew assembleDebug

    - name: Upload debug APK artifact
      uses: actions/upload-artifact@v4
      with:
        name: app-debug
        path: app/build/outputs/apk/debug/app-debug.apk

    - name: Run Lint checks
      run: ./gradlew lintDebug

    - name: Run unit tests
      run: ./gradlew testDebugUnitTest

    - name: Build release APK (if tag push)
      if: startsWith(github.ref, 'refs/tags/v')
      run: ./gradlew assembleRelease

    - name: Sign release APK (if tag push)
      if: startsWith(github.ref, 'refs/tags/v')
      env:
        KEYSTORE_BASE64: ${{ secrets.KEYSTORE_BASE64 }}
        KEYSTORE_PASSWORD: ${{ secrets.KEYSTORE_PASSWORD }}
        KEY_ALIAS: ${{ secrets.KEY_ALIAS }}
        KEY_PASSWORD: ${{ secrets.KEY_PASSWORD }}
      run: |
        # Use printf (not echo) so backslashes in the base64 blob aren't
        # interpreted, and pipe to base64 -d rather than placing the secret
        # on the command line. Quote every secret expansion.
        printf '%s' "$KEYSTORE_BASE64" | base64 -d > app/release.jks
        # Use apksigner (APK Signature Scheme v2/v3) with SHA-256, not
        # jarsigner/SHA-1. Play Store has required v2+ for new apps since
        # August 2021, and SHA-1 is deprecated.
        zipalign -v 4 \
          app/build/outputs/apk/release/app-release-unsigned.apk \
          app/build/outputs/apk/release/app-release-aligned.apk
        "$ANDROID_SDK_ROOT/build-tools/34.0.0/apksigner" sign \
          --ks app/release.jks \
          --ks-key-alias "$KEY_ALIAS" \
          --ks-pass "pass:$KEYSTORE_PASSWORD" \
          --key-pass "pass:$KEY_PASSWORD" \
          --out app/build/outputs/apk/release/app-release-signed.apk \
          app/build/outputs/apk/release/app-release-aligned.apk
        # Scrub the keystore from the runner's disk even though the VM is
        # ephemeral — this is belt-and-braces.
        shred -u app/release.jks || rm -f app/release.jks

    - name: Upload release APK artifact (if tag push)
      if: startsWith(github.ref, 'refs/tags/v')
      uses: actions/upload-artifact@v4
      with:
        name: app-release
        path: app/build/outputs/apk/release/app-release-signed.apk
```

## `README.md` (Initial Draft)

```markdown
# SwiftGram: A Modern Telegram Client for Android

![SwiftGram Logo Placeholder](https://via.placeholder.com/400x200?text=SwiftGram+Logo)

SwiftGram is an open-source, high-performance Telegram client for Android, meticulously designed with a focus on speed, minimalism, advanced customization, and robust privacy features. Built entirely with Kotlin and Jetpack Compose, SwiftGram aims to provide an ultra-smooth and delightful messaging experience while maintaining full compatibility with the official Telegram API.

## ✨ Features

### Core Functionality
- **Secure Telegram Login**: Seamless integration with the official Telegram API for secure authentication.
- **Rich Chat Messaging**: Send and receive text, media (photos, videos, documents), and voice messages.
- **Real-time Notifications**: Instant push notifications powered by Firebase for an always-connected experience.
- **Efficient Message Management**: Advanced message search, filters, chat folders, and message pinning.
- **Offline-first Sync**: Robust offline caching and synchronization to ensure your chats are always accessible.

### Design & Customization
- **Ultra-smooth UI**: Fluid animations and transitions for a highly responsive user interface.
- **Dynamic Theming**: Supports Dark Mode and Material You dynamic theming for a personalized look.
- **Advanced Customization**: Fine-tune fonts, chat bubble styles, and layout density to match your preferences.
- **Minimalist Mode**: A distraction-free messaging experience for focused conversations.

### Privacy & Security
- **App Lock**: Secure your chats with an app-level lock.
- **Hidden Chats**: Keep sensitive conversations private with hidden chat features.
- **Data Protection**: Adherence to best practices for user data protection and secure API key handling.

### Unique Differentiators
- **Gesture-based Navigation**: Intuitive and quick navigation through the app using gestures.
- **Smart Message Summarization (AI)**: (Optional) An intelligent feature to quickly grasp the essence of long conversations.

## 🚀 Getting Started

### Prerequisites
- Android Studio Flamingo | 2022.2.1 or newer
- JDK 17
- An active Telegram API ID and Hash (obtainable from [my.telegram.org](https://my.telegram.org/apps))
- A Firebase project for push notifications (optional, but recommended)

### Setup Instructions
1. **Clone the repository**:
   ```bash
   git clone https://github.com/your-username/SwiftGram.git
   cd SwiftGram
   ```

2. **Configure Telegram API Credentials**:
   Create a `local.properties` file in the root of the project (`SwiftGram/local.properties`) and add your Telegram API credentials:
   ```properties
   telegram.api.id=YOUR_TELEGRAM_API_ID
   telegram.api.hash=YOUR_TELEGRAM_API_HASH
   ```
   *Note: This file is `.gitignore`d to prevent sensitive information from being committed to version control.*

3. **Configure Firebase (Optional)**:
   If you plan to use Firebase for push notifications, follow the official Firebase documentation to add your `google-services.json` file to the `app/` module.

4. **Open in Android Studio**:
   Open the `SwiftGram` project in Android Studio. Let Gradle sync complete.

### Build Instructions

#### Debug Build
To build a debug APK, run the following Gradle command from the project root:
```bash
./gradlew assembleDebug
```
The debug APK will be located at `app/build/outputs/apk/debug/app-debug.apk`.

#### Release Build
For a release build, you will need to set up signing configurations. This typically involves creating a keystore and configuring it in your `app/build.gradle.kts`.

**Note**: The GitHub Actions workflow automatically generates signed release APKs on version tag pushes. Refer to the CI/CD section for more details.

```bash
./gradlew assembleRelease
```
The unsigned release APK will be at `app/build/outputs/apk/release/app-release-unsigned.apk`.

## ⚙️ CI/CD with GitHub Actions

SwiftGram utilizes GitHub Actions for automated continuous integration and continuous deployment. The workflow is defined in `.github/workflows/android_ci.yml` and includes:

- **Automatic Debug APK Build**: On every push to `main` and pull requests, a debug APK is built and uploaded as an artifact.
- **Lint and Unit Tests**: Automated code quality checks and unit test execution on every push and pull request.
- **Release APK Generation**: When a version tag (e.g., `v1.0.0`) is pushed, a signed release APK is generated and uploaded as an artifact.

### Environment Variables for Release Signing
For release APK signing in GitHub Actions, the following secrets must be configured in your GitHub repository settings:
- `KEYSTORE_BASE64`: Base64 encoded content of your `.jks` keystore file.
- `KEYSTORE_PASSWORD`: Password for your keystore.
- `KEY_ALIAS`: Alias of the key within the keystore.
- `KEY_PASSWORD`: Password for the key alias.

## 🤝 Contributing

We welcome contributions to SwiftGram! Please see `CONTRIBUTING.md` (to be created) for guidelines on how to contribute.

## 📄 License

This project is licensed under the MIT License - see the `LICENSE` file for details.

## 📞 Contact

For any inquiries or support, please open an issue on GitHub.
```
