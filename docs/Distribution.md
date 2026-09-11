# Distribution

This document details:

1. **App Store Compromises:** What was restricted or removed from the desktop app to meet Mac App Store requirements, and why each restriction exists.
2. **Build Variants:** How the project's distribution channels (desktop), product flavors (Android), and organization variants (`ooni` / `dw`) differ from one another.

For instructions on the release and publishing process, see [Release.md](Release.md).

## 1. Compromises made for App Store distribution

The desktop app is built for three distribution channels, modeled by the `Distribution` enum and selected via `-PdesktopDistribution=<direct|mac-appstore|ms-store>`:
- `Direct`: Self-updating DMG, EXE, AppImage, Deb.
- `MacAppStore`: Sandboxed `.pkg`.
- `MicrosoftStore`: `.exe` for the Windows Store.

The table below documents functional trade-offs specific to `MacAppStore`, where Apple's review and sandboxing rules are stricter than Microsoft Store's.

| Compromise                                                      | What changed                                                                                                                                                                                                                                                                                                                                          | Why                                                                                                                                                                                                                                                                                                                                                                                                           |
|-----------------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| App Sandbox forced on                                           | `com.apple.security.app-sandbox = true`                                                                                                                                                                                                                                                                                                               | Hard Apple requirement for Mac App Store submission.                                                                                                                                                                                                                                                                                                                                                          |
| Self-update removed                                             | Sparkle is not bundled; no in-app "check for updates".                                                                                                                                                                                                                                                                                                | Updates go through the store.                                                                                                                                                                                                                                                                                                                                                                                 |
| Embedded browser dropped                                        | JavaFX `WebView` is not bundled; in-app links open the system browser instead.                                                                                                                                                                                                                                                                        | App Store Connect rejects JavaFX's `libjfxwebkit.dylib` for using non-public Apple APIs:<br><br><em>"The app uses or references the following non-public or deprecated APIs:<br><br>Contents/app/resources/javafx/darwin-aarch64/libjfxwebkit.dylib<br><br>Symbols:<br>• _cache_simulate_memory_warning_event<br><br>The use of non-public or deprecated APIs is not permitted on the App Store..."</em>      |
| Donations hidden                                                | The Donate settings item is not shown.                                                                                                                                                                                                                                                                                                                | Avoids Apple's in-app-purchase/external-payment review requirements for a browser-based donation flow.                                                                                                                                                                                                                                                                                                        |
| Extra re-signing pipeline                                       | Bundled native libs (JNA, sqlite-jdbc, gojni, JavaFX, passport `.dylib`/`.jnilib`) are re-signed individually, then the outer `.app` is re-signed with the App Store entitlements.                                                                                                                                                                    | `jpackage`'s `codesign --deep` misses nested dylibs, and replacing them invalidates the bundle's resource hash tree — App Store Connect would otherwise report `app-sandbox` missing from the main binary even though the plist has it.                                                                                                                                                                       |
| Quarantine xattr stripped                                       | `com.apple.quarantine` removed from the embedded provisioning profile.                                                                                                                                                                                                                                                                                | Otherwise App Store Connect / TestFlight rejects the upload with error 91109.                                                                                                                                                                                                                                                                                                                                 |
| Store-bundle verification added                                 | A `verifyStoreBundle` Gradle task scans the produced `.pkg`/`.exe` and fails the build if it finds Sparkle/WinSparkle/updatebridge or (Mac App Store only) JavaFX file markers.                                                                                                                                                                       | Guards against accidentally shipping self-update or JavaFX artifacts inside a store build.                                                                                                                                                                                                                                                                                                                    |
| ProGuard disabled for desktop                                   | `buildTypes.release.proguard { isEnabled.set(false) }`                                                                                                                                                                                                                                                                                                | ProGuard mangles okio, strips reflectively-referenced classes (`org.sqlite.Function`), and renames JNI method names so `libsqlitejdbc.dylib`'s `_open_utf8` symbol no longer matches — this was first observed at first launch of the Mac App Store build. No desktop keep rules exist yet, and the size win is marginal next to the ~250 MB bundled JRE.                                                     |
| Silent background-hide replaced with a quit-confirmation prompt | Closing the window (or choosing "Quit" from the tray) on a Mac App Store build no longer silently hides the window and drops the Dock icon. Instead it re-shows the window and shows the existing Quit/Hide/Cancel dialog; only picking "Hide" there drops to tray-only. Direct and Microsoft Store builds keep the old silent hide-to-tray behavior. | App Review rejection under Guideline 2.4.5(iii) - Performance:<br><br><em>"Issue Description: The app spawns processes that continue running after the user has quit the app.<br><br>The spawned process is 1806 /Applications/OONI Probe.app/Contents/MacOS/OONI Probe.<br><br>Next Steps: Revise the app to not spawn processes that continue to run without consent after the user has quit the app."</em> |

Mobile platforms are unaffected: iOS apps are always sandboxed and do not self-update, and Android builds (Google Play/Huawei) have no equivalent restrictions.

## 2. Distribution channels and build variants

### Desktop (`Distribution` enum, `-PdesktopDistribution=`)

|                                   | `direct`                                                | `mac-appstore`                                                                              | `ms-store`                               |
|-----------------------------------|---------------------------------------------------------|---------------------------------------------------------------------------------------------|------------------------------------------|
| Package format                    | DMG / EXE / Deb / AppImage                              | `.pkg`                                                                                      | `.exe`                                   |
| Sandboxed                         | No                                                      | Yes                                                                                         | No (store review, not OS sandbox)        |
| Self-update                       | Sparkle (macOS) / WinSparkle (Windows)                  | None (store-managed)                                                                        | None (store-managed)                     |
| Embedded browser (JavaFX WebView) | Yes                                                     | No — opens system browser                                                                   | Yes                                      |
| Donations                         | Yes                                                     | No                                                                                          | Yes                                      |

Linux only ships as `.deb`/AppImage under `direct` — there is no Linux app store channel.

### Android (product flavors, dimension `license`)

Selected by Gradle task name, since the `com.android.kotlin.multiplatform.library` DSL does not support product flavors:

|                                                     | `full`                         | `fdroid`                                            | `xperimental`                         |
|-----------------------------------------------------|--------------------------------|-----------------------------------------------------|---------------------------------------|
| Distributed via                                     | Google Play, Huawei AppGallery | F-Droid                                             | Internal / experimental builds        |
| Play Services (in-app update prompt, in-app review) | Yes                            | No (no-ops)                                         | —                                     |
| Crash reporting (Sentry)                            | Yes                            | No — compiled out (`optionalFeatures = emptySet()`) | Excluded from Sentry uploads          |
| `oonimkall` engine dependency                       | Published artifact             | Published artifact                                  | Prebuilt `libs/android-oonimkall.aar` |
| APK splitting                                       | No                             | Per-ABI split APKs with per-ABI versionCode offsets | No                                    |

Play Services and Sentry are removed entirely (rather than merely disabled at runtime) in the `fdroid` flavor to comply with F-Droid inclusion policies prohibiting proprietary or tracking dependencies.

### iOS

One app binary exists per organization (see section 3); there is no distribution-channel dimension beyond that, as the Apple App Store is the only iOS channel.

### Store listings per flavor

The full, authoritative list of every store listing — Google Play, F-Droid, Huawei AppGallery, Apple App Store for both organizations, plus Mac App Store / Microsoft Store for desktop — is maintained in [Release.md](Release.md#distributing).

## 3. OONI Probe vs News Media Scan (DW) organization differences

The `ooni`/`dw` organization split is selected via `-Porganization=<ooni|dw>` (consumed by Gradle build scripts). Each organization has its own Kotlin source set (`ooniMain` / `dwMain`) providing an `OrganizationConfig` implementation and default preferences.

*Note on default preferences:* **OONI Probe** pre-enables `MAX_RUNTIME_ENABLED=true`, `MAX_RUNTIME=90`, and all `WebConnectivityCategory` settings, while the **DW** flavor returns an empty list (no default preferences pre-set).

Branding resources for the active organization are copied into `commonMain` at build time by the `copyBrandingToCommonResources` task (reversed by `cleanCopiedCommonResourcesToFlavor` on `clean`).

Developer-facing `-Porganization=` commands are documented in [README.md](../README.md); publishing workflows for each organization are documented in [Release.md](Release.md).
