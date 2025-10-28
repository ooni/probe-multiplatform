# API Usage

## Base URLs

* **API** (for the engine session config and explicit API calls)

    * Release: https://api.ooni.org
    * Debug: https://api.dev.ooni.io

* **Run** (for deeplinks and displaying descriptor revisions)

    * Release: https://run.ooni.org
    * Debug: https://run.test.ooni.org

* **Explorer**

    * https://explorer.ooni.org

## Engine Calls

The app calls this Oonimkall engine methods, and some will call the API underneath:

* `Oonimkall.startTask`
* `Oonimkall.newSession`
* `Session.newContextWithTimeout`
* `Session.submit`
* `Session.checkIn`

## Explicit Calls

There are HTTP requests made explicitly by the app using the Oonimkall engine call `Session.httpDo`.

* `/health`: Test if a proxy is working.
* `/api/v2/oonirun/links/{descriptor_id}`: Download the latest version of a descriptor for
installation or update.
* `/api/v1/incidents/search`: Get all findings for the OONI News.
* `https://ooni.org/blog/index.xml`: Get RSS feed of the OONI blog posts for the OONI News.
* `https://ooni.org/reports/index.xml`: Get RSS feed of the OONI reports for the OONI News.

## Websites displayed inside WebView

* `{EXPLORER_URL}/m/{measurement_id}`: Show measurements results.
* `{EXPLORER_URL}/measurement/{report_id}?input={input}`: Show measurements results when
measurement ID is not available.
* `{EXPLORER_URL}/findings/{finding_id}`: Show finding page for the OONI News.
* Blog posts and report URLs provided by their RSS feeds.

## Links opened in an external device browser

* About

    * https://ooni.org/
    * https://ooni.org/blog/
    * https://ooni.org/reports/
    * https://ooni.org/about/data-policy/

* Onboarding

    * https://ooni.org/about/risks/

* Donations

    * https://ooni.org/donate

* Descriptor revisions

    * `{RUN_URL}/revisions/{descriptor_id}`
    * `{RUN_URL}/revisions/{descriptor_id}?revision={revision}`

* All websites that should be displayed inside a WebView, if for some reason a WebView is not
supported on the current device.

## Deeplinks supported

* `{RUN_URL}/v2/*`
* `ooni://runv2/*`
