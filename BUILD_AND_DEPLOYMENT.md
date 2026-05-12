# SwiftGram Build and Deployment Guide

This document provides comprehensive instructions for building, testing, and deploying SwiftGram using GitHub Actions CI/CD pipelines.

## Overview

SwiftGram uses automated GitHub Actions workflows to build Android APKs and App Bundles. The system supports two build types: debug builds for continuous integration and signed release builds for production deployment.

### Build Workflows

| Workflow | Trigger | Output | Purpose |
|----------|---------|--------|---------|
| **build-debug-apk.yml** | Push to main/develop | Debug APK | Continuous integration testing |
| **build-release-apk.yml** | Version tag (v*) | Signed APK + AAB | Production release |

## Prerequisites

Before deploying SwiftGram, ensure you have the following in place:

**Local Development:**
- Android Studio or command-line tools
- JDK 17 or later
- Gradle 8.0+
- Git and GitHub CLI

**GitHub Repository:**
- Repository created and cloned locally
- Write access to repository settings
- Ability to create and manage secrets

**Telegram API:**
- API ID and Hash from [my.telegram.org/apps](https://my.telegram.org/apps)
- These credentials must be stored as GitHub secrets

**Android Signing:**
- Keystore file for release signing
- Keystore password and key alias
- Key password for the signing key

## Step 1: Configure GitHub Secrets

GitHub Actions requires several secrets to build and sign APKs. Navigate to your repository settings and add the following secrets under Settings → Secrets and variables → Actions.

### Required Secrets

The `TELEGRAM_API_ID` and `TELEGRAM_API_HASH` are obtained from Telegram's developer portal. Log into [my.telegram.org](https://my.telegram.org), navigate to the API Development Tools section, and create or select your application to retrieve these values.

The `KEYSTORE_BASE64` is your Android keystore file encoded in base64 format. To generate this, first create a keystore file using the keytool command, then encode it:

```bash
keytool -genkey -v -keystore swiftgram-release.keystore \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -alias swiftgram_key \
  -storepass YOUR_KEYSTORE_PASSWORD \
  -keypass YOUR_KEY_PASSWORD

base64 -i swiftgram-release.keystore | pbcopy
```

Then add these secrets to GitHub:

| Secret | Value |
|--------|-------|
| `TELEGRAM_API_ID` | Your Telegram API ID (numeric) |
| `TELEGRAM_API_HASH` | Your Telegram API Hash (alphanumeric string) |
| `KEYSTORE_BASE64` | Base64-encoded keystore file content |
| `KEYSTORE_PASSWORD` | Password for the keystore file |
| `KEY_ALIAS` | Alias of the key in the keystore (e.g., swiftgram_key) |
| `KEY_PASSWORD` | Password for the signing key |

### Optional Secrets

For enhanced notifications, you can add a Slack webhook URL to receive build notifications:

| Secret | Value |
|--------|-------|
| `SLACK_WEBHOOK_URL` | Slack incoming webhook URL for notifications |

## Step 2: Verify Local Build

Before relying on CI/CD, verify that the project builds successfully locally:

```bash
cd SwiftGram
./gradlew clean build
```

This ensures all dependencies are resolved and the project compiles without errors. If the local build fails, the GitHub Actions workflow will also fail.

## Step 3: Trigger Debug Builds

Debug builds are automatically triggered whenever code is pushed to the `main` or `develop` branches. These builds verify that the code compiles and passes lint checks without requiring signing keys.

**To trigger a debug build:**

```bash
git add .
git commit -m "feat: Add new feature"
git push origin main
```

**To monitor the build:**

1. Navigate to the Actions tab in your GitHub repository
2. Select the "Build Debug APK" workflow
3. Click on the latest run to view logs
4. Once complete, download artifacts from the Artifacts section

Debug APKs are useful for testing but cannot be installed on production devices. They are retained for 7 days and then automatically deleted.

## Step 4: Create Release Builds

Release builds are triggered by creating a version tag. These builds produce signed APKs and Android App Bundles (AAB) ready for distribution.

**To create a release:**

First, update the version in `app/build.gradle.kts`:

```kotlin
android {
    defaultConfig {
        versionCode = 2
        versionName = "1.1.0"
    }
}
```

Then commit and tag:

```bash
git add app/build.gradle.kts
git commit -m "Bump version to 1.1.0"
git tag v1.1.0
git push origin main
git push origin v1.1.0
```

GitHub Actions will automatically:
1. Build the signed release APK
2. Build the Android App Bundle (AAB)
3. Create a GitHub release
4. Upload both files to the release
5. Send a Slack notification (if configured)

**To monitor the release build:**

1. Go to Actions tab
2. Select "Build Release APK" workflow
3. View the run logs
4. Once complete, navigate to Releases tab to download files

## Step 5: Download Build Artifacts

### Debug Artifacts

Debug builds produce several artifacts available for 7 days:

- **app-debug.apk** – The debug APK for testing
- **lint-report** – HTML lint analysis report
- **test-results** – Unit test results and coverage

To download:
1. Go to Actions → Build Debug APK → Latest run
2. Scroll to Artifacts section
3. Click download button

### Release Artifacts

Release builds produce production-ready artifacts:

- **SwiftGram-v1.0.0-release.apk** – Signed APK for direct installation
- **SwiftGram-v1.0.0-release.aab** – App Bundle for Google Play Store

To download:
1. Go to Releases tab
2. Find your version (e.g., v1.0.0)
3. Download APK or AAB
4. View release notes

## Step 6: Install and Test APKs

### Install Debug APK

Debug APKs can be installed on connected Android devices or emulators:

```bash
adb install app-debug.apk
```

### Install Release APK

Release APKs are signed and can be distributed directly:

```bash
adb install SwiftGram-v1.0.0-release.apk
```

### Test on Google Play

The Android App Bundle (AAB) is the recommended format for Google Play Store:

1. Go to Google Play Console
2. Create a new app or select existing
3. Navigate to Release → Production
4. Upload the AAB file
5. Review and publish

## Troubleshooting Build Issues

### Build Fails: "Cannot find Telegram credentials"

This error occurs when GitHub secrets are not configured. Verify that `TELEGRAM_API_ID` and `TELEGRAM_API_HASH` are set in repository secrets. The workflow cannot proceed without these values.

### Build Fails: "Invalid keystore"

This error indicates the keystore file is corrupted or the base64 encoding is incorrect. To fix:

1. Regenerate the keystore file
2. Re-encode to base64: `base64 -i keystore.jks | pbcopy`
3. Update the `KEYSTORE_BASE64` secret with the new value
4. Retry the build

### Build Fails: "Signing failed"

This error occurs when keystore passwords are incorrect. Verify that `KEYSTORE_PASSWORD`, `KEY_ALIAS`, and `KEY_PASSWORD` match the values used when creating the keystore. All three must be exact matches.

### APK Not Uploaded to Release

This error indicates the GitHub token lacks permissions or the release creation failed. Verify that:

1. The repository has write access enabled
2. The tag format matches `v*` (e.g., v1.0.0)
3. No existing release with the same version exists

### Tests Fail in CI but Pass Locally

This typically occurs when tests depend on local files or external services. To fix:

1. Review test logs in workflow artifacts
2. Mock external API calls
3. Use relative paths instead of absolute paths
4. Ensure tests are isolated and don't depend on execution order

## Best Practices

**Version Management:** Always use semantic versioning (MAJOR.MINOR.PATCH) for releases. This ensures consistency and helps users understand the scope of changes.

**Changelog Updates:** Update `CHANGELOG.md` before creating a release. Document all changes, new features, bug fixes, and security updates. This helps users understand what's new in each version.

**Local Testing:** Always test locally with `./gradlew build` before pushing code. This catches issues early and reduces CI/CD failures.

**Keystore Security:** Never commit the keystore file to git. Store it securely offline and only use the base64-encoded version in GitHub secrets. Rotate keystore passwords periodically.

**Monitoring:** Regularly check GitHub Actions logs for warnings and errors. Address any issues promptly to maintain build reliability.

## Accessing Build Logs

GitHub Actions provides detailed logs for every build step. To access logs:

1. Navigate to Actions tab in your repository
2. Select the workflow (Build Debug APK or Build Release APK)
3. Click on the specific run
4. Expand each step to view detailed output
5. Search for errors or warnings

Logs are retained for 90 days and help diagnose build failures.

## Next Steps

After setting up CI/CD, consider these enhancements:

1. **Add Code Coverage** – Integrate Codecov or similar tools to track test coverage
2. **Performance Testing** – Add benchmarks to monitor app performance
3. **Security Scanning** – Integrate OWASP or similar security analysis tools
4. **Automated Screenshots** – Generate screenshots for Google Play listing
5. **Beta Testing** – Set up Google Play beta channel for testing releases

## Support and Resources

For additional help, refer to these resources:

- [GitHub Actions Documentation](https://docs.github.com/en/actions)
- [Android Gradle Plugin Guide](https://developer.android.com/studio/build)
- [Android App Signing Documentation](https://developer.android.com/studio/publish/app-signing)
- [Telegram Bot API Documentation](https://core.telegram.org/bots/api)

## Summary

SwiftGram's CI/CD pipeline automates the entire build and release process. By following this guide, you can:

1. Automatically build and test code on every push
2. Generate signed release APKs with a single tag
3. Distribute to GitHub releases and Google Play Store
4. Monitor builds and troubleshoot issues efficiently
5. Maintain consistent versioning and release notes

The automated workflows eliminate manual build steps, reduce human error, and ensure consistent, reliable releases.
