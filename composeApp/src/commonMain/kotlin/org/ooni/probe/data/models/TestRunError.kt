package org.ooni.probe.data.models

sealed interface TestRunError {
    data object DownloadUrlsFailed : TestRunError

    data object NoInternet : TestRunError
}
