---
name: Mobile QA
about: QA for mobile releases
title: "<system>: QA for release <version>"
labels: priority/medium
assignees: aanorbel

---

## Preparing the master branch for release

- [ ] (android) `gradle/libs.versions.toml`: use the latest probe-engine
- [ ] (android) `composeApp/build.gradle.kts`: bump version number and code
- [ ] (android) `composeApp/build.gradle.kts`: possibly upgrade the SDKs
- [ ] (ios) `iosApp/Podfile`: update to the last probe-engine and run `pod install`
- [ ] update the translation strings
- [ ] merge any other feature PR we may need

## Tests and results

- [ ] while a nettest is running
    - [ ] put the app in the background and ensure you can come back with an decreased ETA
    - [ ] check that the title changes as we change the nettests we're running
- [ ] after the test has run
    - [ ] ensure the number of run tests has been updated
    - [ ] check that the data usage numbers have increased
    - [ ] try to adjust the filter and validate that only the filtered results appear
    - [ ] tap the result row
        - [ ] check that the last run test is highlighted
        - [ ] check that the date & time of the test are correct and displayed in the timezone of the phone
        - [ ] check that the country & network are resolved properly
    - [ ] tap the measurement row
        - [ ] check that the country & network are resolved properly
        - [ ] check that it’s possible to view the log
        - [ ] check that it’s possible to view data
            - [ ] check that the report_id is not `""`, unless publish result is disabled (see settings test section)
            - [ ] check that the test_start_time in the measurement is using GMT
    - [ ] disable automatic measurement upload
        - [ ] check that the upload all (or individual upload) toast is shown
        - [ ] tap on upload on an individual test and check it works
        - [ ] tap on upload all and check it works
- [ ] run the websites tests
    - [ ] tap on Choose Websites
        - [ ] run the test with a specified set of 2 websites: https://ooni.torproject.org/, https://expired.badssl.com/
    - [ ] tap on settings and re-run the test changing
        - [ ] the test duration to a very short runtime
        - [ ] enabling only one category to test and checking on that is tested
        - [ ] enabling zero categories should not crash the app
    - [ ] tap the result row
        - [ ] check that the summary shows some number blocked some accessible
        - [ ] check that the blocked sites are displayed first
        - [ ] check that the icons are displayed next to each test
        - [ ] check that the data usage shows a sane number, ex 6.3MB up, 5.8MB down
        - [ ] check that the total runtime shows a sane number, ex. 257s
    - [ ] tap the measurement row
        - [ ] check that everything is displayed properly in this screen
        - [ ] check that the runtime is sane ex 2.71s
        - [ ] repeat this for on measurement which is OK and one which is blocked (if present)
- [ ] run the IM tests
    - [ ] tap on settings and re-run the test changing
        - [ ] disable telegram, signal and facebook messenger; check that only whatsapp is run
    - [ ] tap on the result row
        - [ ] check that the total counts on top match
        - [ ] check that the data usage shows a sane number, ex 36KB up, 18KB down
        - [ ] check that the total runtime shows a sane number, ex. 2.16s
- [ ] run the performance test
     - [ ] tap on settings and re-run the test changing
         - [ ] disable all tests but dash; check that only dash is run
     - [ ] check that you see upload, download and video speed in the summary
     - [ ] tap the result row
         - [ ] check that the summary view has video, upload, download and ping
         - [ ] check that the data usage shows a sane number, ex 22MB up, 104MB down
         - [ ] check that the total runtime shows a sane number, ex. 54s
     - [ ] tap the measurement row
         - [ ] check that everything is OK

## Settings

- [ ] notifications
    - [ ] (ios) disable notifications in the system settings and then tab to re-enable them and make sure it asks you to enable notifications from the system settings
    - [ ] (android) disable notifications for OONI Probe from android settings and make sure that installing the app asks for the dynamic permission of enabling notifications
- [ ] advanced
    - [ ] enable debug logs and run a test; check that the logs are at debug level verbosity
    - [ ] ensure logs can be shared.

## About

- [ ] make sure all links are working as intended

## OONI Run v2

We should also test the following links using OONI Run v2.
- [ ] Install a sample Run v2 link and ensure that all the tests are shown in the installed page. https://run.ooni.org/v2/10026
- [ ] Ensure the installed descriptor is shown on the dashboard.
- [ ] Ensure you can run the installed descriptor and the results are shown in the results page.
- [ ] Ensure the results json contains an annotation with the key `ooni_run_link_id` and value being the run ID.
- [ ] Filter the test results to only display results for the Run v2 link.
- [ ] Install an expired Run v2 link and ensure that it doesn't show up in the run page.
