package org.ooni.probe.data.models

sealed class DeepLink {
    data class AddDescriptor(val id: String) : DeepLink()
}
