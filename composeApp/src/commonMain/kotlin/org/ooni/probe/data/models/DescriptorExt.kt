package org.ooni.probe.data.models

import org.ooni.probe.config.OrganizationConfig

val Descriptor.runLink: String
    get() = "${OrganizationConfig.ooniRunDashboardUrl}/v2/${id.value}"

fun Descriptor.title(): String = nameIntl?.getCurrent() ?: name
