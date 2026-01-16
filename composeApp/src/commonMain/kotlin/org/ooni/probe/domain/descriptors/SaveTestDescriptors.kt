package org.ooni.probe.domain.descriptors

import org.ooni.engine.models.TestType
import org.ooni.engine.models.WebConnectivityCategory
import org.ooni.probe.data.models.Descriptor
import org.ooni.probe.data.models.UrlModel

class SaveTestDescriptors(
    private val createOrIgnoreDescriptors: suspend (List<Descriptor>) -> Unit,
    private val createOrUpdateDescriptors: suspend (List<Descriptor>) -> Unit,
    private val storeUrlsByUrl: suspend (List<UrlModel>) -> List<UrlModel>,
) {
    suspend operator fun invoke(
        models: List<Descriptor>,
        mode: Mode,
    ) {
        val webConnectivityUrls = models
            .flatMap { it.netTests.orEmpty() }
            .filter { it.test == TestType.WebConnectivity }
            .flatMap { it.inputs.orEmpty() }
            .map { url ->
                UrlModel(
                    url = url,
                    category = WebConnectivityCategory.MISC,
                    countryCode = "XX",
                )
            }

        if (webConnectivityUrls.isNotEmpty()) {
            storeUrlsByUrl(webConnectivityUrls)
        }

        when (mode) {
            Mode.CreateOrIgnore -> createOrIgnoreDescriptors(models)
            Mode.CreateOrUpdate -> createOrUpdateDescriptors(models)
        }
    }

    enum class Mode {
        CreateOrIgnore,
        CreateOrUpdate,
    }
}
