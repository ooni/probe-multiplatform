# Distributing the application

Below are the primary platforms where your app can be published or updated:

- **[Apple App Store](https://appstoreconnect.apple.com/login)**
  The official marketplace for iOS apps, requiring an Apple Developer account. Apps are uploaded via App Store Connect using tools like Xcode or Transporter.
- **[Google Play Store](https://play.google.com/apps/publish/)**
  The marketplace for Android apps, accessible through the Google Play Console. Android apps are uploaded as `.aab`.
- **[F-Droid](https://f-droid.org/)**
  A free and open-source platform for Android apps. Developers submit their app source code for review to be built and published by F-Droid. Apps are distributed as `.apk` files.
- **[Huawei AppGallery](https://developer.huawei.com/consumer/en/)**
  Huawei's proprietary app distribution platform, requiring a Huawei Developer account for uploading and managing Android apps. Android apps are uploaded as `.aab`.

---

## Prerequisites

Before proceeding with app store updates, ensure the following steps are completed:

- **Merge Required Pull Requests (PRs)**
    - Review and merge all PRs related to the current app version. Verify that all changes have been tested and approved.

- **Complete Necessary Testing**
    - Run all required tests, to ensure the app is stable and ready for release.

- **Version Tagging**
    - Update the app's version number following the [Semantic Versioning](https://semver.org/) convention.
    - Tag the repository with the new version to mark the release point in the version control history.
    - ```
      git tag -s v[x.y.z] -f -m "OONI Probe|NMS [x.x.x] release" # nms-iOS-x.y.z
      git push origin v[x.y.z]
      ```

- **Build the App**
    - Generate platform-specific builds:
        - **iOS:** Use Xcode to build the `.ipa` file.
        - **Android:** Use Gradle to build the `.aab` and `.apk` file.
    - Sign F-Droid build. `gpg -b --armor OONIProbe-Android-[x.y.z]-fdroid.apk`
    - For iOS, upload Sentry Debug Symbols to ensure proper error tracking and debugging.

- **Prepare the App for Distribution**
    - Review platform-specific guidelines.

- **Upload the App**
    - Submit the built app to each platform:
        - Apple App Store via App Store Connect.
        - Google Play Store via Play Console.
        - F-Droid by submitting source code and metadata.
        - Huawei AppGallery via Huawei Developer Console.
