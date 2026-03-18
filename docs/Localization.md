# Localization

This document explains how localization works in the project and how to add or remove a supported
language.

## Architecture

Language support is defined once in the build system and flows through to all platforms:

```
Organization.kt (source of truth)
    └─ AppConfig.supportedLanguages        — full locale list (e.g. "pt-rBR")
         ├─ AppConfig.supportedLanguageCodes — derived language codes ("pt"), always includes "en"
         ├─ Android res string               — "supported_languages" for runtime use
         │    └─ AndroidApplication.kt       — feeds LocaleManager on API 34+
         └─ iOS Info.plist CFBundleLocalizations — **manual**, must match supportedLanguages
```

### Key files

| File | Role |
|------|------|
| `buildSrc/src/main/kotlin/Organization.kt` | Defines `supportedLanguages` per variant (ooni / dw) |
| `buildSrc/src/main/kotlin/AppConfig.kt` | Derives `supportedLanguageCodes` (strips region qualifiers, adds "en") |
| `composeApp/src/ooniMain/.../OrganizationConfig.kt` | OONI variant |
| `composeApp/src/dwMain/.../OrganizationConfig.kt` | DW variant |
| `composeApp/src/commonMain/.../OrganizationConfig.kt` | Interface declaring `supportedLanguageCodes: Set<String>` |
| `composeApp/src/commonMain/.../LocalizationString.kt` | Runtime locale resolution — falls back to English if unsupported |
| `composeApp/src/androidMain/.../AndroidApplication.kt` | Feeds `supported_languages` to `LocaleManager` on Android 14+ for per-app language picker |
| `composeApp/src/iosMain/.../SetupDependencies.kt` | Overrides `AppleLanguages` on iOS if device language is unsupported |
| `iosApp/iosApp/Info.plist` | `CFBundleLocalizations` for the OONI Probe iOS target — **must be updated manually** |
| `iosApp/iosApp/NewsMediaScan-Info.plist` | `CFBundleLocalizations` for the News Media Scan iOS target — **must be updated manually** |

## String resource files

Translations live in `composeApp/src/commonMain/composeResources/values-XX/` where `XX` is a locale
qualifier following the Android convention:

- Simple language: `values-fr`
- Language + region: `values-pt-rBR` (region prefixed with `r`)

Each directory contains:

- `strings-common.xml` — strings shared across all variants
- `strings-organization.xml` — variant-specific strings (OONI Probe vs News Media Scan)

The base English strings are in `composeApp/src/commonMain/composeResources/values/`.

> **Note:** The `composeResources` directory may contain translation directories for languages not
> listed in `supportedLanguages`. Only languages declared in `Organization.kt` are included in
> builds and available at runtime.

## Adding a new language

### 1. Add string resource directory

Create `composeApp/src/commonMain/composeResources/values-XX/` with the locale qualifier for the
new language. Copy the base `strings-common.xml` and `strings-organization.xml` from `values/` and
translate them.

For a language with a region variant (e.g. Brazilian Portuguese), use the format `values-pt-rBR`.

### 2. Register the language in Organization.kt

Edit `buildSrc/src/main/kotlin/Organization.kt` and add the locale qualifier to the
`supportedLanguages` list for the appropriate variant(s):

```kotlin
// Example: adding Japanese to the OONI variant
supportedLanguages = listOf(
    "ar",
    // ...existing languages...
    "ja",       // ← add here, keep alphabetical order
    // ...
)
```

If the language includes a region qualifier, add it as-is (e.g. `"pt-rBR"`).

### 3. Update iOS Info.plist and `OrganizationConfig`

Add the language code to the `CFBundleLocalizations` and `OrganizationConfig#supportedLanguages` array in the appropriate plist file(s). iOS
uses hyphenated codes **without** the `r` prefix (e.g. `pt-BR`, not `pt-rBR`):

- **OONI Probe:** `composeApp/src/ooniMain/.../config/OrganizationConfig.kt`, `iosApp/iosApp/Info.plist`
- **News Media Scan:** `composeApp/src/dwMain/.../config/OrganizationConfig.kt`, `iosApp/iosApp/NewsMediaScan-Info.plist`

```xml
<key>CFBundleLocalizations</key>
<array>
    <!-- ...existing languages... -->
    <string>ja</string>  <!-- ← add here -->
</array>
```

> **This step is manual.** Unlike the Android side, `CFBundleLocalizations` is not generated from
> `Organization.kt`.

### 4. Verify

- Run the app and switch the device language to the new locale
- Confirm strings resolve correctly
- Confirm `LocalizationString.getCurrent()` picks up the new language for API-sourced strings

## Removing a language

### 1. Remove from Organization.kt

Remove the locale qualifier from the `supportedLanguages` list in
`buildSrc/src/main/kotlin/Organization.kt`.

### 2. Remove from iOS Info.plist  and `OrganizationConfig`

Remove the corresponding `<string>` entry from `CFBundleLocalizations` in the appropriate plist
file(s) (`composeApp/src/ooniMain/.../config/OrganizationConfig.kt`,`Info.plist` and/or `composeApp/src/dwMain/.../config/OrganizationConfig.kt`, `NewsMediaScan-Info.plist`).

### 3. Rebuild

The generated code and Android resource filters will update automatically.

### 4. Optionally remove resource directory

The `composeResources/values-XX/` directory can be left in place (it will be excluded from builds)
or deleted if the translations are no longer needed.

## How locale resolution works at runtime

### Compose Resources (UI strings)

Compose Resources automatically resolves XML string files based on the device locale and the
available `values-XX` directories, filtered by Android `localeFilters` on Android.

### API-sourced strings (LocalizationString)

`LocalizationString.getCurrent()` in `LocalizationString.kt`:

1. Reads the OS language code via `Locale.current.language`
2. Checks if it is in `OrganizationConfig.supportedLanguageCodes`
3. Falls back to `"en"` if unsupported
4. Looks up the localized value by language + region, then language only

### Android per-app language picker (API 34+)

`AndroidApplication.onCreate()` configures `LocaleManager.overrideLocaleConfig` using the
`supported_languages` resource string (which is generated from `AppConfig.supportedLanguages`).
This tells Android which languages appear in the system's per-app language picker
(Settings > Apps > OONI Probe > Language). No manual update is needed here — the resource string
is generated at build time.

### iOS locale override

`SetupDependencies.ensureSupportedLocale()` runs at iOS app launch. If the device's primary
language is not in `supportedLanguageCodes`, it overrides `AppleLanguages` in `NSUserDefaults` to
the first supported language from the user's preference list (or English as a last resort). This
ensures Compose Resources resolve to a supported language.
