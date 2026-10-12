# Testing desktop update feed failover

The desktop app retries failed update checks with exponential backoff and then fails over to the
next appcast feed in `UpdateConfig.URLS` (GitHub → OONI mirror). This document describes how to
exercise that flow manually.

## How it behaves

`DesktopUpdateController` is the only class that talks to the `UpdateManager`. It runs single feed
checks (point the updater at a feed, check, wait for the outcome reported by the native callbacks)
for `UpdateFeedFailover`, which decides which feed to check, when to retry and when to move on.

A check counts as a feed failure only for feed/download errors (`CheckOutcome.fromError`);
signature, installation and configuration errors are not retried. "No update found" and the user
cancelling an install are not errors. A check that never reports back counts as a feed failure
after 2 minutes.

Silent checks (app launch, window brought to the foreground):

1. Retry the same feed up to 3 times, with delays of 1, 5 and 15 minutes.
2. Switch to the next feed (wrapping around the list) and check again after a short pause
   (`DEFAULT_SWITCH_DELAY`; a check started right after an abort is swallowed by the native
   updater).
3. If every feed fails, reset to the primary feed and wait for the next scheduled check
   (Sparkle/WinSparkle's own timer, every `UpdateConfig.CHECK_INTERVAL_HOURS` hours) or the next
   foreground-triggered check. Foreground checks are skipped while a check, or its retry, is in
   progress.
4. On a successful check, persist the working feed index
   (`SettingsKey.LAST_WORKING_UPDATE_FEED_INDEX`) if it changed, so the next launch starts with that
   host.

User-initiated checks (tray menu "Check for updates") replace any check in progress and skip the
backoff:

- **macOS:** every feed is checked once, with Sparkle's UI. The bridge's `OONIUserDriver` holds
  back Sparkle's error alert meanwhile, so a failing feed shows no error. The first feed that works
  shows Sparkle's usual "up to date" or update dialog. If every feed fails, the last error is shown
  once with Sparkle's standard error alert.
- **Windows:** WinSparkle cannot hold back its error dialog, so only the current feed is checked
  with UI and WinSparkle shows its error if it fails. The remaining feeds are then checked silently,
  with retries, so later checks use the one that works.

## Unit tests

The retry/failover decisions are covered by `UpdateFeedFailoverTest` (virtual time, no native code):

```bash
./gradlew :desktopApp:test --tests '*UpdateFeedFailoverTest'
```

## Prerequisites

- macOS host for the Sparkle path (Windows requires a Windows machine or CI for the WinSparkle
  path).
- The packaged app, run with `./gradlew :desktopApp:runDistributable`. Plain
  `./gradlew :desktopApp:run` does **not** work: `libupdatebridge.dylib` fails to load because
  `Sparkle.framework` is only staged next to it during packaging.

## Test procedure (macOS)

1. **Point the primary feed at a dead host.** In
   `desktopApp/src/main/kotlin/org/ooni/probe/config/UpdateConfig.kt` temporarily set:

   ```kotlin
   private const val GITHUB_MAC_URL = "https://127.0.0.1:9/feed-mac.rss"
   ```

   (Nothing listens on port 9, so the fetch fails instantly with a connection error.)

2. **Speed up the retry schedule.** In
   `desktopApp/src/main/kotlin/org/ooni/probe/update/UpdateFeedFailover.kt` temporarily set:

   ```kotlin
   val DEFAULT_RETRY_DELAYS = listOf(5.seconds, 5.seconds, 5.seconds)
   ```

   so retries are 5 seconds apart instead of minutes.

   To exercise a successful failover without the mirror being deployed, also temporarily point
   `MIRROR_MAC_URL` at the real GitHub feed URL.

3. **Run the packaged app**, capturing output:

   ```bash
   ./gradlew :desktopApp:runDistributable --console=plain > /tmp/ooni-update-test.log 2>&1
   ```

4. **Watch the log** for the cascade:

   ```bash
   grep -Ei "retrying|aborted|failed|Switched" /tmp/ooni-update-test.log
   ```

   Expected sequence (timestamps abbreviated):

   ```
   Update aborted with error (2001): An error occurred in retrieving update information...
   Retrying update check 1/3 in 5s   # with the temp change
   ... (retry 2, retry 3, each aborting with error 2001) ...
   Update feed https://127.0.0.1:9/feed-mac.rss failed
   Switched appcast feed URL to: https://distribution.ooni.org/feed-mac.rss
   ```

   Then, depending on the mirror's state:

   - Mirror not yet deployed (DNS/HTTP error): another 3 retries, then
     `Update check failed (FeedFailure); waiting for the next scheduled check` and the feed resets to the
     primary. No further `Retrying update check` lines should appear.
   - Mirror serving a feed: `Finished loading appcast with N items` followed by either
     `No update found` or an update prompt, and no `Retrying update check` afterwards. Afterwards the feed index is persisted; quit and
     relaunch the app and confirm startup logs show
     `Appcast feeds: ... (starting with https://distribution.ooni.org/feed-mac.rss)`.

5. **Check from the tray menu.** With the dead primary and a working mirror, choose
   "Check for updates": no error alert appears, the log shows `Deferring error alert` for the
   primary, then Sparkle shows the "up to date" (or update) dialog from the mirror. With both feeds
   dead (`MIRROR_MAC_URL` also pointed at `https://127.0.0.1:9/...`), a single Sparkle error alert
   appears, only after both feeds were tried.

## Windows notes

The procedure is the same on a Windows host, with `GITHUB_WINDOWS_URL` as the dead primary. The
log lines come from `WinSparkleHelper` instead of `SparkleHelper`; check/download failures surface
through `win_sparkle_set_error_callback` (available since WinSparkle 0.5; absence is logged
as a warning at DLL load and disables error reporting only). A check from the tray menu shows
WinSparkle's error dialog for the dead primary, then the mirror is checked silently.
