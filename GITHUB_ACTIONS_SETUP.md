# GitHub Actions Setup Guide for SwiftGram APK Builds

This guide explains how to configure GitHub Actions to automatically build and sign APKs for SwiftGram.

## Overview

Two workflows are configured:

1. **build-debug-apk.yml** – Builds debug APKs on every push to `main` or `develop`
2. **build-release-apk.yml** – Builds signed release APKs when you create a version tag (e.g., `v1.0.0`)

## Prerequisites

Before setting up GitHub Actions, you need:

- A GitHub repository (already created)
- Android keystore file for signing releases
- Telegram API credentials
- GitHub secrets configured

## Step 1: Generate Android Keystore

If you don't have a keystore, generate one:

```bash
keytool -genkey -v -keystore swiftgram-release.keystore \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -alias swiftgram_key \
  -storepass YOUR_KEYSTORE_PASSWORD \
  -keypass YOUR_KEY_PASSWORD
```

**Important:** Keep this keystore file safe! You'll need it for all future releases.

## Step 2: Encode Keystore to Base64

GitHub Actions needs the keystore as a base64-encoded string:

```bash
base64 -i swiftgram-release.keystore -o keystore.base64
cat keystore.base64
```

Copy the entire base64 string (it will be very long).

## Step 3: Configure GitHub Secrets

Go to your GitHub repository → Settings → Secrets and variables → Actions

Add the following secrets:

### Required Secrets

| Secret Name | Value | Example |
|------------|-------|---------|
| `TELEGRAM_API_ID` | Your Telegram API ID | `123456` |
| `TELEGRAM_API_HASH` | Your Telegram API Hash | `abcdef1234567890...` |
| `KEYSTORE_BASE64` | Base64-encoded keystore | (Very long string from Step 2) |
| `KEYSTORE_PASSWORD` | Keystore password | `your_keystore_password` |
| `KEY_ALIAS` | Key alias from keystore | `swiftgram_key` |
| `KEY_PASSWORD` | Key password | `your_key_password` |

### Optional Secrets

| Secret Name | Value | Purpose |
|------------|-------|---------|
| `SLACK_WEBHOOK_URL` | Slack webhook URL | Send build notifications to Slack |

## Step 4: Get Telegram Credentials

1. Go to [my.telegram.org/apps](https://my.telegram.org/apps)
2. Log in with your Telegram account
3. Create a new application
4. Copy your **API ID** and **API Hash**
5. Add them to GitHub secrets

## Step 5: Verify Gradle Configuration

Ensure your `app/build.gradle.kts` reads from `local.properties`:

```kotlin
android {
    compileSdk = 34
    
    defaultConfig {
        applicationId = "com.swiftgram.app"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"
        
        // Read from local.properties
        val localProperties = Properties()
        val localPropertiesFile = rootProject.file("local.properties")
        if (localPropertiesFile.exists()) {
            localProperties.load(localPropertiesFile.inputStream())
        }
        
        buildConfigField("int", "TELEGRAM_API_ID", 
            localProperties.getProperty("telegram.api.id", "0"))
        buildConfigField("String", "TELEGRAM_API_HASH", 
            "\"${localProperties.getProperty("telegram.api.hash", "")}\"")
    }
}
```

## Workflow: Building Debug APKs

**Trigger:** Every push to `main` or `develop` branch

**What happens:**
1. Checks out the code
2. Sets up Java 17 and Android SDK
3. Creates `local.properties` with Telegram credentials
4. Builds debug APK
5. Runs lint checks
6. Runs unit tests
7. Uploads artifacts (APK, lint report, test results)
8. Comments on PR with build status

**Access artifacts:**
- Go to Actions tab
- Click on the workflow run
- Download from "Artifacts" section

## Workflow: Building Release APKs

**Trigger:** When you create a version tag (e.g., `git tag v1.0.0 && git push origin v1.0.0`)

**What happens:**
1. Checks out the code
2. Sets up Java 17 and Android SDK
3. Creates keystore from base64 secret
4. Builds signed release APK
5. Builds Android App Bundle (AAB) for Google Play
6. Creates GitHub release
7. Uploads APK and AAB to release
8. Sends Slack notification (if configured)

**Create a release:**

```bash
# Update version in build.gradle.kts
# Commit changes
git add .
git commit -m "Release v1.0.0"

# Create and push tag
git tag v1.0.0
git push origin main
git push origin v1.0.0
```

The workflow will automatically:
- Build the APK and AAB
- Create a GitHub release
- Upload both files
- You can download from the release page

## Troubleshooting

### Build fails with "Cannot find Telegram credentials"

**Solution:** Ensure `TELEGRAM_API_ID` and `TELEGRAM_API_HASH` are set in GitHub secrets.

### Signing fails: "Invalid keystore"

**Solution:** 
1. Re-encode keystore: `base64 -i keystore.jks | pbcopy`
2. Update `KEYSTORE_BASE64` secret with new value
3. Verify passwords match in secrets

### APK not uploaded to release

**Solution:**
1. Check workflow logs for errors
2. Ensure `GITHUB_TOKEN` has write permissions (usually automatic)
3. Verify tag format matches `v*` pattern

### Tests fail in CI but pass locally

**Solution:**
1. Check test logs in workflow artifacts
2. Ensure tests don't depend on local files
3. Mock external dependencies (API calls, etc.)

## Monitoring Builds

### View Workflow Runs

1. Go to GitHub repository
2. Click "Actions" tab
3. Select workflow from list
4. View run details and logs

### Download Artifacts

1. Go to workflow run
2. Scroll to "Artifacts" section
3. Download APK, lint reports, or test results

### View Release

1. Go to "Releases" tab
2. Find your release (e.g., v1.0.0)
3. Download APK or AAB
4. View release notes

## Best Practices

1. **Always test locally first** – Run `./gradlew build` before pushing
2. **Use semantic versioning** – Tags like `v1.0.0`, `v1.1.0`, etc.
3. **Update CHANGELOG** – Document changes before releasing
4. **Keep keystore safe** – Never commit keystore to git
5. **Rotate secrets regularly** – Update passwords periodically
6. **Monitor build logs** – Check for warnings and errors

## Next Steps

1. Generate your Android keystore
2. Add all required secrets to GitHub
3. Push a commit to `main` to trigger debug build
4. Verify debug APK builds successfully
5. Create a version tag to trigger release build
6. Download APK from GitHub release

## Support

For issues:
1. Check workflow logs in GitHub Actions
2. Review error messages carefully
3. Ensure all secrets are configured
4. Verify local build works: `./gradlew build`

## References

- [GitHub Actions Documentation](https://docs.github.com/en/actions)
- [Android Gradle Plugin Guide](https://developer.android.com/studio/build)
- [Android App Signing](https://developer.android.com/studio/publish/app-signing)
- [Telegram Bot API](https://core.telegram.org/bots/api)
