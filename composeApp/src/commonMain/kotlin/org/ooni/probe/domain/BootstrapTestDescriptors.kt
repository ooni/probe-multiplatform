package org.ooni.probe.domain

import org.ooni.engine.models.TestType
import org.ooni.engine.models.WebConnectivityCategory
import org.ooni.probe.data.models.InstalledTestDescriptorModel
import org.ooni.probe.data.models.UrlModel

class BootstrapTestDescriptors(
    private val getBootstrapTestDescriptors: suspend () -> List<InstalledTestDescriptorModel>,
    private val createOrIgnoreTestDescriptors: suspend (List<InstalledTestDescriptorModel>) -> Unit,
    private val storeUrlsByUrl: suspend (List<UrlModel>) -> List<UrlModel>,
) {
    suspend operator fun invoke() {
        val descriptors = getBootstrapTestDescriptors()
        descriptors.flatMap { descriptor ->
            descriptor.netTests?.filter { webConnectivityTest -> webConnectivityTest.test == TestType.WebConnectivity }
                ?.mapNotNull { webConnectivityTest ->
                    webConnectivityTest.inputs?.map { url ->
                        UrlModel(
                            url = url,
                            category = WebConnectivityCategory.MISC,
                            countryCode = "XX",
                        )
                    }
                } ?: emptyList()
        }.let { urlsToStore ->
            storeUrlsByUrl(urlsToStore.flatten())
        }

        createOrIgnoreTestDescriptors(descriptors)
    }
}
