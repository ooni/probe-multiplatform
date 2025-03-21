fastlane documentation
----

# Installation

Make sure you have the latest version of the Xcode command line tools installed:

```sh
xcode-select --install
```

For _fastlane_ installation instructions, see [Installing _fastlane_](https://docs.fastlane.tools/#installing-fastlane)

# Available Actions

## Android

### android publish_app_gallery

```sh
[bundle exec] fastlane android publish_app_gallery
```

Publish the app to Huawei AppGallery

#### Example:

```
fastlane android publish_app_gallery client_id:xxx client_secret:xxx app_id:xxx apk_path:xxx
```

#### Options

 * **`client_id`**: The client ID for Huawei AppGallery Connect

 * **`client_secret`**: The client secret for Huawei AppGallery Connect

 * **`app_id`**: The app ID for the application

 * **`apk_path`**: The path to the APK/AAB file

#### Required environment variables

 * **`ANDROID_KEYSTORE_FILE`**: path the Android Keystore file

 * **`ANDROID_KEYSTORE_PASSWORD`**: Android Keystore password

 * **`ANDROID_KEY_PASSWORD`**: Android Keystore Key password

 * **`ANDROID_KEY_ALIAS`**: Android Keystore Key alias



### android update_app_gallery

```sh
[bundle exec] fastlane android update_app_gallery
```

Update Huawei AppGallery store listing information

#### Example:

```
fastlane android update_app_gallery client_id:xxx client_secret:xxx app_id:xxx 
```

#### Options

 * **`client_id`**: The client ID for Huawei AppGallery Connect

 * **`client_secret`**: The client secret for Huawei AppGallery Connect

 * **`app_id`**: The app ID for the application



### android publish

```sh
[bundle exec] fastlane android publish
```

Publish a new version of the app on Google Play

#### Example:

```
fastlane android publish track:alpha version_code:100 organization:ooni json_key:key.json
```

#### Options

 * **`track`**: internal, alpha, beta, production

 * **`organization`**: ooni, dw

 * **`version_code`**: new version code

 * **`json_key`**: path to Google Play service account JSON file

#### Required environment variables

 * **`ANDROID_KEYSTORE_FILE`**: path the Android Keystore file

 * **`ANDROID_KEYSTORE_PASSWORD`**: Android Keystore password

 * **`ANDROID_KEY_PASSWORD`**: Android Keystore Key password

 * **`ANDROID_KEY_ALIAS`**: Android Keystore Key alias



### android bundle

```sh
[bundle exec] fastlane android bundle
```

Create AAB file

#### Example:

```
fastlane android bundle organization:ooni
```

#### Options

 * **`organization`**: ooni, dw

#### Required environment variables

 * **`ANDROID_KEYSTORE_FILE`**: path the Android Keystore file

 * **`ANDROID_KEYSTORE_PASSWORD`**: Android Keystore password

 * **`ANDROID_KEY_PASSWORD`**: Android Keystore Key password

 * **`ANDROID_KEY_ALIAS`**: Android Keystore Key alias



### android promote

```sh
[bundle exec] fastlane android promote
```

Promote Google Play release

#### Example:

```
fastlane android promote organization:ooni track:alpha promote_track:beta rollout:0.5 json_key:key.json
```

#### Options

 * **`organization`**: ooni, dw

 * **`current_track`**: internal, alpha, beta, production

 * **`promote_track`**: alpha, beta, production (optional to just update rollout)

 * **`rollout`**: set or update rollout [0 to 1] (optional, defaults to 1)

 * **`json_key`**: path to Google Play service account JSON file



### android capture_screens

```sh
[bundle exec] fastlane android capture_screens
```

Capture screenshots for Google Play

#### Example:

```
fastlane android capture_screens organization:ooni locales:en,it
```

#### Options

 * **`organization`**: ooni, dw

 * **`locales`**: comma-separated list of locales (optional, defaults to full list based on the organization)



### android update_google_play

```sh
[bundle exec] fastlane android update_google_play
```

Update Google Play store listing information

#### Example:

```
fastlane android update_google_play organization:ooni screenshots:true metadata:true json_key:key.json
```

#### Options

 * **`organization`**: ooni, dw

 * **`screenshots`**: true or false (default false)

 * **`metadata`**: true or false (default false)

 * **`json_key`**: path to Google Play service account JSON file



----


## iOS

### ios build

```sh
[bundle exec] fastlane ios build
```

Build iOS app

#### Example:

```
fastlane ios build organization:ooni
```

#### Options

 * **`organization`**: ooni, dw



### ios publish

```sh
[bundle exec] fastlane ios publish
```

Publish iOS app

#### Example:

```
fastlane ios publish organization:ooni
```

#### Options

 * **`organization`**: ooni, dw



### ios update_app_store

```sh
[bundle exec] fastlane ios update_app_store
```

Update Apple App Store information

#### Example:

```
fastlane android update_app_store organization:ooni screenshots:true metadata:true
```

#### Options

 * **`organization`**: ooni, dw

 * **`screenshots`**: true or false (default false)

 * **`metadata`**: true or false (default false)



### ios capture_screens

```sh
[bundle exec] fastlane ios capture_screens
```

Capture screenshots for Apple App Store

#### Example:

```
fastlane ios capture_screens organization:ooni locales:en,it
```

#### Options

 * **`organization`**: ooni, dw

 * **`locales`**: comma-separated list of locales (optional, defaults to full list based on the organization)



### ios sentry_upload_debug_symbols

```sh
[bundle exec] fastlane ios sentry_upload_debug_symbols
```

Upload debug symbols to Sentry

#### Example:

```
fastlane sentry_upload_debug_symbols auth_token:... org_slug:ooni project_slug:probe-multiplatform-ios path:.
```

#### Options

 * **`auth_token`**: Sentry auth token

 * **`org_slug`**: Sentry organization slug

 * **`project_slug`**: Sentry project slug



----

This README.md is auto-generated and will be re-generated every time [_fastlane_](https://fastlane.tools) is run.

More information about _fastlane_ can be found on [fastlane.tools](https://fastlane.tools).

The documentation of _fastlane_ can be found on [docs.fastlane.tools](https://docs.fastlane.tools).
