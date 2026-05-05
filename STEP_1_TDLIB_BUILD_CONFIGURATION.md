# Step 1: TDLib & Build Configuration

This document provides complete implementation details for integrating Telegram's TDLib into SwiftGram with secure API credential injection and JNI native library setup.

## Overview

**Objective:** Configure the Gradle build system to:
1. Inject Telegram API credentials (`telegram.api.id` and `telegram.api.hash`) from `local.properties` into `BuildConfig`
2. Properly integrate TDLib's native `.so` files and Java wrapper classes
3. Set up JNI (Java Native Interface) bindings for seamless Kotlin-to-C++ communication

**Architecture Decision:**
- **API Credentials:** Stored in `local.properties` (git-ignored), injected at compile-time into `BuildConfig`
- **TDLib Native Libraries:** Placed in `app/src/main/jniLibs/{abi}/` (standard Android convention)
- **TDLib Java Wrapper:** Can be either:
  - Pre-compiled JAR/AAR in `core` or `data` module
  - Source code in `core` module compiled as part of the build
- **Module Placement:** TDLib wrapper classes belong in the `data` module (data layer responsibility)

---

## Part 1: Local Properties & BuildConfig Injection

### 1.1 Create `local.properties` Template

Create a file at the project root: `/home/ubuntu/SwiftGram/local.properties`

```properties
# Telegram API Credentials (from https://my.telegram.org/apps)
# DO NOT commit this file to version control
telegram.api.id=YOUR_TELEGRAM_API_ID_HERE
telegram.api.hash=YOUR_TELEGRAM_API_HASH_HERE

# Example (replace with your actual credentials):
# telegram.api.id=123456
# telegram.api.hash=abcdef1234567890abcdef1234567890
```

**Important:** This file is already in `.gitignore`, so it will not be committed.

### 1.2 Update `app/build.gradle.kts` to Inject Credentials

Modify `/home/ubuntu/SwiftGram/app/build.gradle.kts` to read from `local.properties` and inject into `BuildConfig`:

```kotlin
import java.util.Properties
import java.io.File

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.dagger.hilt.android")
    id("kotlin-kapt")
}

// Load local.properties
val localPropertiesFile = rootProject.file("local.properties")
val localProperties = Properties()
if (localPropertiesFile.exists()) {
    localProperties.load(localPropertiesFile.inputStream())
}

// Extract Telegram API credentials with fallback defaults
val telegramApiId = localProperties.getProperty("telegram.api.id", "0").toIntOrNull() ?: 0
val telegramApiHash = localProperties.getProperty("telegram.api.hash", "")

android {
    namespace = "com.swiftgram.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.swiftgram.app"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        // Inject Telegram credentials into BuildConfig
        buildConfigField("int", "TELEGRAM_API_ID", telegramApiId.toString())
        buildConfigField("String", "TELEGRAM_API_HASH", "\"$telegramApiHash\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        debug {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true  // Ensure BuildConfig is generated
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.4"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(project(":domain"))
    implementation(project(":data"))
    implementation(project(":core"))

    // AndroidX Core & Lifecycle
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")
    implementation("androidx.activity:activity-compose:1.8.1")

    // Jetpack Compose
    implementation(platform("androidx.compose:compose-bom:2023.10.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.navigation:navigation-compose:2.7.5")

    // Hilt
    implementation("com.google.dagger:hilt-android:2.48")
    kapt("com.google.dagger:hilt-android-compiler:2.48")
    implementation("androidx.hilt:hilt-navigation-compose:1.1.0")

    // Coil (Image Loading)
    implementation("io.coil-kt:coil-compose:2.5.0")

    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2023.10.01"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
```

**Key Changes:**
- Import `Properties` and `File` from Java utilities
- Load `local.properties` at build time
- Use `buildConfigField()` to inject `TELEGRAM_API_ID` (Int) and `TELEGRAM_API_HASH` (String) into `BuildConfig`
- Add `buildConfig = true` to `buildFeatures` to ensure `BuildConfig` class is generated

### 1.3 Access Credentials in Code

In any Kotlin file, access the injected credentials:

```kotlin
import com.swiftgram.app.BuildConfig

// Access the injected credentials
val apiId = BuildConfig.TELEGRAM_API_ID
val apiHash = BuildConfig.TELEGRAM_API_HASH

Log.d("TDLib", "API ID: $apiId, API Hash: ${apiHash.take(4)}***")
```

---

## Part 2: TDLib Native Libraries & JNI Setup

### 2.1 Folder Structure for JNI Libraries

Android expects native `.so` files in a specific directory structure. Create the following folders in the `app` module:

```
app/src/main/jniLibs/
├── arm64-v8a/
│   ├── libtdjson.so
│   └── libc++_shared.so
├── armeabi-v7a/
│   ├── libtdjson.so
│   └── libc++_shared.so
├── x86/
│   ├── libtdjson.so
│   └── libc++_shared.so
└── x86_64/
    ├── libtdjson.so
    └── libc++_shared.so
```

**Create the directories:**

```bash
mkdir -p app/src/main/jniLibs/{arm64-v8a,armeabi-v7a,x86,x86_64}
```

### 2.2 Obtaining TDLib Pre-compiled Binaries

**Option A: Download Pre-built TDLib (Recommended for Quick Start)**

1. Visit the [TDLib releases page](https://github.com/tdlib/td/releases)
2. Download the Android SDK package (e.g., `TDLib-Android-SDK-1.8.0.zip`)
3. Extract the `.so` files for each architecture and place them in `app/src/main/jniLibs/{abi}/`

**Option B: Build TDLib from Source**

If you need a custom build or the latest version:

```bash
# Clone TDLib repository
git clone https://github.com/tdlib/td.git
cd td

# Follow the official TDLib build instructions for Android
# https://github.com/tdlib/td/blob/master/example/android/README.md
```

### 2.3 Update `app/build.gradle.kts` for JNI

Add the following configuration to ensure the `.so` files are properly packaged:

```kotlin
android {
    // ... existing configuration ...

    packagingOptions {
        pickFirst("lib/arm64-v8a/libc++_shared.so")
        pickFirst("lib/armeabi-v7a/libc++_shared.so")
        pickFirst("lib/x86/libc++_shared.so")
        pickFirst("lib/x86_64/libc++_shared.so")
    }

    // Ensure JNI libraries are included in the APK
    sourceSets {
        getByName("main") {
            jniLibs.srcDirs("src/main/jniLibs")
        }
    }
}
```

---

## Part 3: TDLib Java Wrapper Integration

### 3.1 Option A: Using Pre-compiled TDLib Java Wrapper JAR

If you have a pre-compiled `tdlib.jar` or `tdlib.aar`:

**Update `data/build.gradle.kts`:**

```kotlin
android {
    // ... existing configuration ...
}

dependencies {
    implementation(project(":domain"))
    implementation(project(":core"))

    // TDLib Java Wrapper (if available as JAR or AAR)
    // Option 1: Local JAR file
    implementation(files("libs/tdlib.jar"))

    // Option 2: Local AAR file
    // implementation(files("libs/tdlib.aar"))

    // Option 3: From Maven Central (if available)
    // implementation("org.drinkless.tdlib:tdlib:1.8.0")

    // ... rest of dependencies ...
}
```

**Create the libs directory:**

```bash
mkdir -p data/libs
# Place your tdlib.jar or tdlib.aar in this directory
```

### 3.2 Option B: TDLib Java Wrapper Source Code

If you have the TDLib Java wrapper source code (typically `TdApi.java` and `Client.java`):

1. **Create the directory structure:**

```bash
mkdir -p data/src/main/java/org/drinkless/tdlib
```

2. **Place the Java wrapper files:**

```
data/src/main/java/org/drinkless/tdlib/
├── TdApi.java          # Contains all TDLib data classes
└── Client.java         # Main client interface
```

3. **Update `data/build.gradle.kts`** (no additional changes needed; Gradle will compile these automatically)

### 3.3 Verify JNI Library Loading

Create a utility class to verify that TDLib native libraries are properly loaded:

**File: `core/src/main/java/com/swiftgram/core/utils/TDLibLoader.kt`**

```kotlin
package com.swiftgram.core.utils

import android.content.Context
import com.swiftgram.core.utils.Logger

/**
 * Utility for loading TDLib native libraries.
 * Handles the loading of libtdjson.so and manages JNI initialization.
 */
object TDLibLoader {
    private const val TAG = "TDLibLoader"
    private var isLoaded = false

    /**
     * Load TDLib native libraries.
     * Must be called once during app initialization.
     */
    fun loadLibraries(context: Context) {
        if (isLoaded) {
            Logger.d(TAG, "TDLib libraries already loaded")
            return
        }

        try {
            // Load the C++ runtime library first
            System.loadLibrary("c++_shared")
            Logger.d(TAG, "Loaded libc++_shared.so")

            // Load the main TDLib library
            System.loadLibrary("tdjson")
            Logger.d(TAG, "Loaded libtdjson.so")

            isLoaded = true
            Logger.i(TAG, "TDLib native libraries loaded successfully")
        } catch (e: UnsatisfiedLinkError) {
            Logger.e(TAG, "Failed to load TDLib native libraries", e)
            throw RuntimeException("Failed to load TDLib: ${e.message}", e)
        } catch (e: Exception) {
            Logger.e(TAG, "Unexpected error loading TDLib", e)
            throw RuntimeException("Unexpected error: ${e.message}", e)
        }
    }

    /**
     * Check if TDLib libraries are loaded.
     */
    fun isLibrariesLoaded(): Boolean = isLoaded
}
```

---

## Part 4: Update `settings.gradle.kts` for Local Artifacts (Optional)

If you're using local TDLib artifacts (JAR/AAR files), update `settings.gradle.kts` to include a local repository:

```kotlin
rootProject.name = "SwiftGram"
include(":app")
include(":domain")
include(":data")
include(":core")

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        
        // Add local repository for TDLib artifacts (if using local files)
        flatDir {
            dirs("data/libs")
        }
    }
}
```

---

## Part 5: Update `data/build.gradle.kts` for TDLib

Replace the entire `data/build.gradle.kts` with the following:

```kotlin
plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("com.google.dagger.hilt.android")
    id("kotlin-kapt")
}

android {
    namespace = "com.swiftgram.data"
    compileSdk = 34

    defaultConfig {
        minSdk = 26
        targetSdk = 34

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation(project(":domain"))
    implementation(project(":core"))

    // Hilt
    implementation("com.google.dagger:hilt-android:2.48")
    kapt("com.google.dagger:hilt-android-compiler:2.48")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")

    // Room Database (for local caching)
    implementation("androidx.room:room-runtime:2.6.0")
    implementation("androidx.room:room-ktx:2.6.0")
    kapt("androidx.room:room-compiler:2.6.0")

    // TDLib Java Wrapper
    // Option 1: Local JAR (if you have pre-compiled tdlib.jar)
    // implementation(files("libs/tdlib.jar"))

    // Option 2: From Maven Central (if available)
    // implementation("org.drinkless.tdlib:tdlib:1.8.0")

    // Option 3: Source code in data/src/main/java/org/drinkless/tdlib/
    // (No additional dependency needed; will be compiled as part of this module)

    // Logging
    implementation("com.jakewharton.timber:timber:5.0.1")

    // Security
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}
```

---

## Part 6: Folder Structure Summary

After completing Step 1, your project structure should look like:

```
SwiftGram/
├── local.properties                    # (git-ignored) Telegram API credentials
├── settings.gradle.kts                 # Updated with flatDir for local artifacts
├── build.gradle.kts
├── app/
│   ├── build.gradle.kts                # Updated with BuildConfig injection & JNI setup
│   └── src/main/
│       ├── jniLibs/                    # TDLib native libraries
│       │   ├── arm64-v8a/
│       │   │   ├── libtdjson.so
│       │   │   └── libc++_shared.so
│       │   ├── armeabi-v7a/
│       │   ├── x86/
│       │   └── x86_64/
│       └── AndroidManifest.xml         # (to be created)
├── data/
│   ├── build.gradle.kts                # Updated with TDLib dependencies
│   ├── libs/                           # (optional) Local TDLib JAR/AAR
│   │   └── tdlib.jar
│   └── src/main/java/
│       ├── org/drinkless/tdlib/        # (optional) TDLib Java wrapper source
│       │   ├── TdApi.java
│       │   └── Client.java
│       └── com/swiftgram/data/
│           └── remote/
│               └── telegram/
│                   └── TelegramClient.kt  # (to be created in Step 2)
├── core/
│   ├── build.gradle.kts
│   └── src/main/java/com/swiftgram/core/
│       └── utils/
│           ├── TDLibLoader.kt          # JNI library loader
│           ├── Logger.kt
│           ├── SecureStorage.kt
│           └── InputValidator.kt
└── domain/
    └── build.gradle.kts
```

---

## Part 7: Build & Test

### 7.1 Build the Project

```bash
cd /home/ubuntu/SwiftGram

# Clean and build
./gradlew clean build

# Or just assemble debug APK
./gradlew assembleDebug
```

### 7.2 Verify BuildConfig Injection

Check that `BuildConfig` contains your Telegram credentials:

```kotlin
// In any Activity or Fragment
Log.d("BuildConfig", "API ID: ${BuildConfig.TELEGRAM_API_ID}")
Log.d("BuildConfig", "API Hash: ${BuildConfig.TELEGRAM_API_HASH.take(4)}***")
```

### 7.3 Verify JNI Libraries

In your app's initialization (e.g., `MainActivity.onCreate()`):

```kotlin
import com.swiftgram.core.utils.TDLibLoader

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Load TDLib native libraries
        try {
            TDLibLoader.loadLibraries(this)
            Log.i("MainActivity", "TDLib loaded successfully")
        } catch (e: Exception) {
            Log.e("MainActivity", "Failed to load TDLib", e)
        }
        
        // ... rest of onCreate
    }
}
```

---

## Troubleshooting

### Issue: `UnsatisfiedLinkError: libtdjson.so not found`

**Solution:**
1. Verify `.so` files are in `app/src/main/jniLibs/{abi}/`
2. Ensure the ABI matches your device (use `adb shell getprop ro.product.cpu.abi` to check)
3. Rebuild and reinstall the APK: `./gradlew clean assembleDebug`

### Issue: `BuildConfig.TELEGRAM_API_ID is 0`

**Solution:**
1. Verify `local.properties` exists and contains valid credentials
2. Check that `telegram.api.id` is a valid integer
3. Rebuild: `./gradlew clean build`

### Issue: `libc++_shared.so conflicts`

**Solution:**
The `packagingOptions` in `build.gradle.kts` handles this. If conflicts persist:
1. Use `pickFirst()` as shown in Part 2.3
2. Ensure only one version of `libc++_shared.so` is included

---

## Next Steps

Once Step 1 is complete:
- Verify `BuildConfig` injection works
- Confirm TDLib native libraries load without errors
- Proceed to **Step 2: TDLib Client Wrapper** to implement the Telegram client initialization and async event listener

