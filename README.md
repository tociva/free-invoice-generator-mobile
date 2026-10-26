<div align="center">
  <img src="app/src/main/res/mipmap-xxxhdpi/ic_launcher.webp" alt="Daybook.Cloud invoice generator icon" width="96" />

  # Free Invoice Generator

  Create invoices in your brand’s style from a large library of ready-made templates. Completely free. No credit card. No signup.

  Free Invoice Generator is made by Tociva Private Limited (tociva.com) as part of daybook.cloud.

  Private, offline-first Android and iOS apps for creating, managing, exporting, and sharing professional invoices.

  [![Android](https://img.shields.io/badge/Android-API%2028%2B-3DDC84?logo=android&logoColor=white)](https://developer.android.com/)
  [![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org/)
  [![Compose Multiplatform](https://img.shields.io/badge/UI-Compose%20Multiplatform-4285F4?logo=jetpackcompose&logoColor=white)](https://www.jetbrains.com/compose-multiplatform/)
  [![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE.md)
</div>

## Overview

Free Invoice Generator helps freelancers and small businesses build polished invoices directly on a mobile device. Invoice and business data is stored locally, and PDF generation runs on-device without requiring an account or internet connection.

Both Android and iOS run the same Compose Multiplatform app from the `shared` module. Platform-specific code is limited to PDF rendering, file sharing, notifications, logo picking, and database file paths.

| Platform | Implementation |
| --- | --- |
| Android | Thin `app` shell that hosts `SharedApp` via `MainActivity` |
| iOS | Thin SwiftUI host (`ComposeRootView`) that links the `Shared` framework and calls `MainViewController()` |

## Features

- Guided **Simple** and **Advanced** invoice creation workflows
- Business branding with organization details and a custom logo
- Customer details, line items, discounts, notes, terms, and due dates
- Taxable and non-taxable invoices, including **CGST/SGST** and **IGST** modes
- Bank account and UPI payment details
- 18 supported currencies and configurable decimal precision
- Automatic invoice numbering with a customizable prefix
- 154 bundled invoice templates across professional, modern, creative, retail, healthcare, and other styles
- On-device A4 PDF generation, download, and sharing
- Local invoice history with draft, sent, paid, and overdue states
- Light, dark, and system theme options
- Room-backed persistence and database migration coverage

## Tech stack

| Area | Technology |
| --- | --- |
| Languages | Kotlin (shared) and Swift (iOS host only) |
| UI | Compose Multiplatform / Material 3 |
| Architecture | Shared UI, domain, and data layers with repositories and ViewModels |
| Dependency injection | Manual composition root in `shared` (`AppContainer`) |
| Local database | Room KMP with `BundledSQLiteDriver` |
| Preferences | Platform key-value store (`SharedPreferences` / `UserDefaults`) |
| Navigation | JetBrains Navigation Compose (KMP) |
| PDF rendering | Shared HTML templates; Android WebView print APIs; iOS `WKWebView` PDF |
| Networking | Ktor (remote template catalog) |
| Build | Gradle Kotlin DSL with a version catalog; Xcode for iOS packaging |
| Testing | JUnit 4, AndroidX Test, Espresso, Compose UI Test, and Room Test |

## Requirements

- Android Studio with support for Android Gradle Plugin 9.3.3
- JDK 17 or newer to launch Gradle (the configured daemon toolchain uses JDK 25 and can be provisioned automatically)
- Android SDK 37
- An emulator or physical device running Android 9 (API 28) or newer
- macOS with Xcode 26 or newer for App Store uploads
- An iOS 17+ simulator or device

The repository includes the Gradle wrapper, so a separate Gradle installation is not required.

## Getting started

1. Clone the repository:

   ```bash
   git clone git@github.com:tociva/free-invoice-generator-mobile.git
   cd free-invoice-generator-mobile
   ```

2. Open the project in Android Studio, install SDK Platform 37 and the requested build tools from **SDK Manager**, then allow Gradle sync to finish.

3. For Android, select an Android 9+ device or emulator and run the `app` configuration.

4. For iOS, open `ios/FreeInvoiceGenerator.xcodeproj`, select the `FreeInvoiceGenerator` scheme, and choose an iOS 17+ simulator. Xcode runs a build phase that compiles the `Shared` framework via Gradle.

## Building the apps

The platform builds are intentionally separate. Both scripts place generated artifacts under the repository's existing build directories.

### Android

Build the Android debug APK from macOS, Linux, or Windows with a Bash environment:

```bash
bash scripts/build-android.sh
```

The debug APK is generated at `app/build/outputs/apk/debug/app-debug.apk`.

To build directly with Gradle instead:

```bash
bash gradlew assembleDebug
```

### iOS

Build the unsigned iOS Simulator app on macOS with Xcode installed:

```bash
bash scripts/build-ios.sh
```

The app bundle is generated at `build/ios/Build/Products/Debug-iphonesimulator/Free Invoice Generator.app`.

To invoke Xcode directly instead:

```bash
bash gradlew :shared:linkDebugFrameworkIosSimulatorArm64

xcodebuild \
  -project ios/FreeInvoiceGenerator.xcodeproj \
  -scheme FreeInvoiceGenerator \
  -configuration Debug \
  -sdk iphonesimulator \
  -destination 'generic/platform=iOS Simulator' \
  -derivedDataPath build/ios \
  FRAMEWORK_SEARCH_PATHS="$PWD/shared/build/bin/iosSimulatorArm64/debugFramework" \
  OTHER_LDFLAGS="-framework Shared" \
  CODE_SIGNING_ALLOWED=NO \
  build
```

Device and App Store builds are covered in [Publishing](#publishing).

## Publishing

Store builds use the identifiers and versions already set in the project.

| | Android | iOS |
| --- | --- | --- |
| Application ID | `com.tociva.freeinvoicegenerator` | `com.tociva.freeinvoicegenerator` |
| Public version | `versionName` in `app/build.gradle.kts` (`1.0`) | `MARKETING_VERSION` in the Xcode project (`1.0`) |
| Build number | `versionCode` in `app/build.gradle.kts` (`1`) | `CURRENT_PROJECT_VERSION` in the Xcode project (`1`) |

### Versioning

Version updates are **manual**. The build, archive, emulator, and upload commands do not change any version values.

- For the first store upload, the current `1.0 (1)` values are valid if build `1` has not already been uploaded.
- Before every later upload, increase the build number on that platform. Google Play and App Store Connect reject build numbers that were already uploaded.
- Change the public version only when preparing a new customer-facing release, such as `1.0` to `1.1`.
- Keep Android and iOS public versions aligned when they represent the same product release.
- Prefer monotonically increasing build numbers across releases on both platforms to keep release history easy to follow.

For Android, edit `app/build.gradle.kts`:

```kotlin
defaultConfig {
    versionCode = 2
    versionName = "1.0"
}
```

For iOS, open the `FreeInvoiceGenerator` target in Xcode and update **General → Identity → Version** and **Build**. This updates `MARKETING_VERSION` and `CURRENT_PROJECT_VERSION` for the target's build configurations.

An example release sequence:

| Upload | Public version | Build number |
| --- | --- | --- |
| First production candidate | `1.0` | `1` |
| Corrected `1.0` candidate | `1.0` | `2` |
| Next feature release | `1.1` | `3` |

Commit version changes before generating the store artifacts so the repository records exactly which source produced each upload.

### Google Play

Play Console accepts an Android App Bundle (`.aab`) signed with your upload key. Google Play App Signing then re-signs the app for users. A signed release APK is the same build, packaged so you can install it locally.

Create the upload keystore once, outside the repository, and keep the file and passwords. Losing this keystore blocks later updates if the app is not enrolled in Play App Signing.

```bash
keytool -genkeypair -v \
  -keystore "$HOME/keystores/free-invoice-generator-upload.keystore" \
  -alias upload \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000
```

Add the keystore path and passwords to `keystore.properties` in the repository root. That file is gitignored. Do not commit it or the `.keystore` file.

```properties
storeFile=/absolute/path/to/free-invoice-generator-upload.keystore
storePassword=your-store-password
keyAlias=upload
keyPassword=your-key-password
```

`app/build.gradle.kts` reads that file and signs the release build with it. PKCS12 keystores use the same value for `storePassword` and `keyPassword`.

Build both artifacts:

```bash
bash gradlew bundleRelease assembleRelease
```

| Artifact | Path |
| --- | --- |
| Play Console upload | `app/build/outputs/bundle/release/app-release.aab` |
| Signed release APK | `app/build/outputs/apk/release/app-release.apk` |

In Play Console, create the app with package name `com.tociva.freeinvoicegenerator`, enroll in Play App Signing, and upload `app-release.aab` to a testing track before production.

### App Store

The simulator `.app` produced by `bash scripts/build-ios.sh` cannot be uploaded to the App Store. Transporter requires a signed `.ipa` built for physical iOS devices with the App Store distribution method.

Before starting, make sure you have:

- An active Apple Developer Program membership
- Xcode 26 or newer and the [Transporter app](https://apps.apple.com/app/transporter/id1450874784) installed on macOS
- The appropriate Account Holder, Admin, App Manager, or Developer access in App Store Connect
- A unique build number in `CURRENT_PROJECT_VERSION`; increase it before every upload

#### 1. Create the App Store Connect record

Before uploading the first build, open [App Store Connect](https://appstoreconnect.apple.com/), go to **Apps**, click **+**, and select **New App**. Use:

| Field | Value |
| --- | --- |
| Platform | iOS |
| Name | Free Invoice Generator |
| Bundle ID | `com.tociva.freeinvoicegenerator` |
| SKU | A unique internal value, such as `free-invoice-generator-ios` |

The bundle ID must exactly match the Xcode target. Apple documents the complete process in [Add a new app](https://developer.apple.com/help/app-store-connect/create-an-app-record/add-a-new-app/).

#### 2. Configure signing

Open `ios/FreeInvoiceGenerator.xcodeproj`, select the `FreeInvoiceGenerator` target, and open **Signing & Capabilities**. Confirm that:

- **Automatically manage signing** is enabled.
- The correct Apple Developer team is selected.
- The bundle identifier is `com.tociva.freeinvoicegenerator`.
- A valid Apple Distribution certificate and App Store provisioning profile are available.

The project currently has a development team configured. If building for a different team, replace `YOUR_TEAM_ID` below with that team's 10-character ID. `-allowProvisioningUpdates` allows Xcode to create or download required signing assets.

#### 3. Create a signed archive

From Xcode, select **Any iOS Device (arm64)** and choose **Product → Archive**. Alternatively, archive from Terminal:

```bash
xcodebuild \
  -project ios/FreeInvoiceGenerator.xcodeproj \
  -scheme FreeInvoiceGenerator \
  -configuration Release \
  -destination 'generic/platform=iOS' \
  -archivePath build/ios/FreeInvoiceGenerator.xcarchive \
  -allowProvisioningUpdates \
  DEVELOPMENT_TEAM=YOUR_TEAM_ID \
  CODE_SIGN_STYLE=Automatic \
  MARKETING_VERSION=1.0.1 \
  CURRENT_PROJECT_VERSION=3 \
  archive
```

The archive is written to `build/ios/FreeInvoiceGenerator.xcarchive`.

#### 4. Export the App Store IPA

Create `ios/ExportOptions.plist` using the following content. Replace `YOUR_TEAM_ID` with the same Apple Developer team ID. This file is gitignored and must not be committed.

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
    <key>method</key>
    <string>app-store-connect</string>
    <key>teamID</key>
    <string>YOUR_TEAM_ID</string>
    <key>uploadSymbols</key>
    <true/>
    <key>signingStyle</key>
    <string>automatic</string>
</dict>
</plist>
```

Export the signed `.ipa`:

```bash
xcodebuild \
  -exportArchive \
  -archivePath build/ios/FreeInvoiceGenerator.xcarchive \
  -exportPath build/ios/export \
  -exportOptionsPlist ios/ExportOptions.plist
```

The signed IPA is written under `build/ios/export/`. Confirm it exists before opening Transporter:

```bash
find build/ios/export -maxdepth 1 -name '*.ipa' -print
```

#### 5. Upload with Transporter

1. Open **Transporter** and sign in with the Apple ID associated with the App Store Connect account.
2. Select the correct provider if the account belongs to more than one App Store Connect organization.
3. Drag the exported `.ipa` from `build/ios/export/` into the Transporter window, or click **+** and select it.
4. Click **Deliver**. Transporter validates the package before uploading it.
5. Wait until the delivery moves to **Delivered**. If delivery fails, open **Issues** or **View Delivery Log**, fix every error, increase the build number when required, archive/export again, and retry.

Apple's [Transporter guide](https://support.apple.com/en-in/guide/transporter-app/apda158c5f7c/mac) covers delivery status, warnings, and logs. Warnings may not prevent delivery, but errors do.

#### 6. Finish in App Store Connect

After delivery, Apple processes the build before it appears in App Store Connect. Processing is asynchronous, and Apple sends an email when it completes.

1. Open the app in App Store Connect and check **TestFlight** for the processed build.
2. Resolve any **Missing Compliance** questions, including export-compliance information.
3. Test the build with internal TestFlight testers before release.
4. Open the iOS app version under **Distribution**, complete the required metadata, privacy details, screenshots, age rating, pricing, and availability.
5. In the **Build** section, click **+**, select the processed build, and save.
6. Complete the App Review information and click **Add for Review**, then submit the version.

See Apple's current [upload-build guidance](https://developer.apple.com/help/app-store-connect/manage-builds/upload-builds/) and [build-selection steps](https://developer.apple.com/help/app-store-connect/manage-builds/choose-a-build-to-submit/). The bundle ID, marketing version, and build number in the uploaded binary determine which App Store Connect record and version receive the build.

## Opening the apps in emulators

Each launcher builds first, then installs and opens the app. Gradle and Xcode skip recompilation when nothing relevant has changed since the last build.

### Android emulator

Open the Android app on a running emulator, or start the first configured Android Virtual Device automatically:

```bash
bash scripts/run-android.sh
```

To start a specific AVD, pass its name:

```bash
bash scripts/run-android.sh Pixel_9_API_35
```

List the available AVD names with `emulator -list-avds`. You can also set the `ANDROID_AVD` environment variable instead of passing a name.

### iOS Simulator

Open the iOS app on a booted iPhone Simulator, or start the first available iPhone Simulator automatically:

```bash
bash scripts/run-ios.sh
```

To start a specific simulator, pass its device name:

```bash
bash scripts/run-ios.sh "iPhone 16 Pro"
```

List available devices with `xcrun simctl list devices available`. You can also set the `IOS_SIMULATOR` environment variable instead of passing a name.

## Testing

Run the local unit tests:

```bash
bash gradlew testDebugUnitTest
```

Run the instrumented tests on a connected device or emulator:

```bash
bash gradlew connectedDebugAndroidTest
```

The test suite covers invoice calculations, money formatting, Room persistence and migrations, settings persistence, and PDF export.

## Project structure

```text
shared/src/
├── commonMain/kotlin/.../
│   ├── data/                  # Room, preferences, mappers, repositories
│   ├── di/                    # AppContainer composition root
│   ├── domain/                # Models, repository contracts, calculations
│   ├── pdf/                   # Shared HTML template rendering
│   ├── platform/              # expect/actual platform APIs
│   ├── ui/                    # Compose screens, navigation, theme, ViewModels
│   └── shared/SharedApp.kt    # Shared app entry point
├── androidMain/               # Android platform implementations
└── iosMain/                   # iOS platform implementations + MainViewController

app/src/main/java/.../
├── MainActivity.kt            # Android entry → SharedApp
├── DaybookApplication.kt      # Platform init + PDF bridge registration
├── AndroidPdfBridgeSetup.kt   # Wires HtmlPdfPrinter into shared module
└── pdf/                       # Android WebView PDF printer

ios/
├── FreeInvoiceGenerator/      # Swift Compose host (ComposeRootView)
└── FreeInvoiceGenerator.xcodeproj/

scripts/
├── build-android.sh           # Android debug APK build
├── build-ios.sh               # Unsigned iOS Simulator build
├── run-android.sh             # Build if needed, then open the APK in an Android emulator
└── run-ios.sh                 # Build if needed, then open the app in iOS Simulator
```

## Privacy and permissions

Neither app requires internet access for its core workflow. Invoice data is stored in a local Room database on both platforms. Generated PDFs are saved or shared through each platform's native file APIs. On supported Android versions, notification permission is used to report completed downloads.

## Contributing

Contributions are welcome. To propose a change:

1. Fork the repository and create a focused feature branch.
2. Make the change and add or update tests where appropriate.
3. Run the relevant unit and instrumented tests.
4. Open a pull request describing the motivation and implementation.

Please keep changes consistent with the shared Compose architecture and existing conventions.

## License

This project is available under the [MIT License](LICENSE.md).
