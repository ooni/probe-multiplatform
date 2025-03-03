# Distributing

This app is built as 2 different *flavors*:

- **OONI Probe:** the general official probe used for collecting mobile data
- **News Media Scan:** a probe specific for news sites, created in partnership with Deutsche Welle

Each *flavor* is available for Android and iOS devices, through different platforms:

- [OONI Probe Android on Google Play](https://play.google.com/store/apps/details?id=org.openobservatory.ooniprobe)
- [OONI Probe Android on F-droid](https://f-droid.org/en/packages/org.openobservatory.ooniprobe/)
- [OONI Probe Android on Huawei AppGallery](https://appgallery.huawei.com/app/C105911849)
- [OONI Probe iOS on Apple App Store](https://apps.apple.com/us/app/ooni-probe/id1199566366)
- [News Media Scan Android on Google Play](https://play.google.com/store/apps/details?id=com.dw.ooniprobe)
- [News Media Scan iOS on Apple App Store](https://apps.apple.com/us/app/news-media-scan/id6738992797)

## Continuous Deployment

Pull-requests merged into the `main` branch trigger a new OONI Probe Android and News Media Scan
Android builds that are automatically published to Firebase.

## Release Process

Here are the steps required to release a new app version across all platforms.

### 1. Prepare Release

### 1.1 Create release branch

Create a new release branch named `releases/NEW_VERSION` where `NEW_VERSION` in the new version to
be released.

#### 1.2 Update the version

Update the `versionCode` and `versionName` at `composeApp/build.gradle.kts`. The `versionCode`
should be increased in increments of 10 to allow for different versionCodes for each split APK for
F-droid.

#### 1.3 Release notes

Update the release notes for each flavor and platform:
- OONI Probe Android: `metadata/ooni/android/en-US/changelogs/default.txt`
- OONI Probe iOS: `metadata/ooni/ios/en-US/release_notes.txt`
- News Media Scan Android: `metadata/dw/android/en-US/changelogs/default.txt`
- News Media Scan iOS: `metadata/dw/ios/en-US/release_notes.txt`

#### 1.4 Create the Pull Request

Push the branch to Github and create a Pull Request for it against `main`. Make sure CI validations
complete successfully.

#### 1.5 Android alphas

Distribute Closed Testing (alpha) builds of the OONI Probe Android and News Media Scan Android apps
on Google Play, so the OONI team and partners can test them.

Go to [Publish Android on Google Play](https://github.com/ooni/probe-multiplatform/actions/workflows/publish_android_on_google_play.yml),
press *Run Workflow*, pick `alpha` as the track, select both apps and press *Run Workflow*. Confirm
both actions run successfully.

### 2. Publishing

Once the alpha versions have been approved for release, we can start publishing.

#### 2.1 Tag & Create Release

2.1.1 Merge the release branch PR into `main`.

2.1.2 Tag the latest commit on `main`:

```
git tag -s v[x.y.z] -f -m "[x.x.x] release"
git push origin --tags
```

2.1.3 Create a new [Github release](https://github.com/ooni/probe-multiplatform/releases) with the
release notes.

The new Github release should create new release on Sentry to help with error reporting, and publish
an internal Slack message warning of the new incoming release.

#### 2.2 Publish iOS Apps

Go to [Publish iOS Apps](https://github.com/ooni/probe-multiplatform/actions/workflows/publish_ios.yml),
press *Run Workflow*, pick both apps and press *Run Workflow*. Confirm both actions run
successfully. The updates will be reviewed by Apple, so we need to keep an eye if they pass.

#### 2.2 Publish OONI Probe Android on F-Droid

By pushing a new tag on Github, F-Droid bots will check if our app `versionCode` was updated. Since
it was, they will trigger a new build and release it automatically. It should take around 3 days for
the new release to be available [here](https://f-droid.org/en/packages/org.openobservatory.ooniprobe/)
but sometimes it can take more time.

#### 2.3 Publish OONI Probe Android on Huawei AppGallery

Go to [Publish OONI Probe on Huawei AppGallery](https://github.com/ooni/probe-multiplatform/actions/workflows/publish_android_on_huawei.yml),
press *Run Workflow* and press *Run Workflow*. Confirm the action ran successfully.

#### 2.4 Promote News Media Scan Android

Go to [Promote Android on Google Play](https://github.com/ooni/probe-multiplatform/actions/workflows/promote_android_on_google_play.yml),
press *Run Workflow*, pick the Organization `dw`, the Current Track `alpha`, the Promote Track
`production` and press *Run Workflow*. Confirm the action ran successfully.

#### 2.5 Promote OONI Probe Android

Since this is our app with the biggest amount of users, we take more steps to release it. First we
promote from `alpha` to `beta`, and then from `beta` to `production` with a `0.2` (20%) rollout value.

Both steps are done at [Promote Android on Google Play](https://github.com/ooni/probe-multiplatform/actions/workflows/promote_android_on_google_play.yml)
like on the previous step, but with the Organization as `ooni`.

## Monitoring

We use Sentry to monitor for crashes and handled errors. We have specific views for:
* [All Android releases](https://ooni.sentry.io/issues/?project=4508325642764288&viewId=148098)
* [OONI Probe iOS](https://ooni.sentry.io/issues/?project=4508325650235392&viewId=80423)
* [News Media Scan iOS](https://ooni.sentry.io/issues/?project=4508325650235392&viewId=148094)

We also monitor the number of uploaded measurements to OONI Explorer through our internal
[Grafana dashboard](https://grafana.ooni.org/d/f996246b-e529-420b-b5de-290d5b4e6dd7/ooni-probe-release?orgId=1&var-cnt_value=cnt&var-software_version=1.0&var-test_name=web_connectivity&var-software_name=ooniprobe-android).

## Store Listings

### Capture Android screenshots

There is a fastlane command to capture new screenshots of the app
(*organization* can be ooni or dw):

```
bundle exec fastlane android capture_screens organization:ooni
```

Only the screenshots 1-5 are committed to git, since those are the ones we submit to Google Play.

### Update Google Play listings

To update the screenshots or the metadata (title, short and full description) of the OONI Probe
and News Media Scan Android apps, go to
[Update Google Play information](https://github.com/ooni/probe-multiplatform/actions/workflows/update_google_play.yml).

### Update Apple App Store listings

To update the screenshots or the metadata (title, short and full description) of the OONI Probe
and News Media Scan iOS apps, go to
[Update Apple App Store information](https://github.com/ooni/probe-multiplatform/actions/workflows/update_apple_app_store.yml).

### Update F-Droid listing

On release, F-Droid should pick up the OONI Probe screenshots and metadata automtically from the
repository.

### Updating Huawei AppGallery listing

The Huawei AppGallery listing must be updated manually.
