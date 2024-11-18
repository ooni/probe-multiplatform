package org.ooni.probe.data.models

sealed class IntentAction {
    data class Mail(val subject: String, val body: String, val chooserTitle: String? = null) :
        IntentAction()

    data class OpenUrl(val url: String) : IntentAction()

    data class Share(val text: String) : IntentAction()
}
