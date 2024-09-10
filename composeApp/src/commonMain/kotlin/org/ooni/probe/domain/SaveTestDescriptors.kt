package org.ooni.probe.domain

import org.ooni.engine.models.TestType
import org.ooni.engine.models.WebConnectivityCategory
import org.ooni.probe.data.models.InstalledTestDescriptorModel
import org.ooni.probe.data.models.NetTest
import org.ooni.probe.data.models.UrlModel
import org.ooni.probe.data.models.toDescriptor
import org.ooni.probe.data.repositories.PreferenceRepository

class SaveTestDescriptors(
    private val preferencesRepository: PreferenceRepository,
    private val createOrIgnoreTestDescriptors: suspend (List<InstalledTestDescriptorModel>) -> Unit,
    private val storeUrlsByUrl: suspend (List<UrlModel>) -> List<UrlModel>,
) {
    suspend operator fun invoke(models: List<Pair<InstalledTestDescriptorModel, List<NetTest>>>) {
        models.forEach { (model, tests) ->

            val webConnectivityUrls = tests.filter { test -> test.test == TestType.WebConnectivity }
                .flatMap { test ->
                    test.inputs?.map { url ->
                        UrlModel(
                            url = url,
                            category = WebConnectivityCategory.MISC,
                            countryCode = "XX",
                        )
                    } ?: emptyList()
                }

            storeUrlsByUrl(webConnectivityUrls)

            preferencesRepository.setAreNetTestsEnabled(
                list = tests.map { test ->
                    model.toDescriptor() to test
                },
                isAutoRun = true,
                isEnabled = true,
            )
        }
        createOrIgnoreTestDescriptors(models.map { it.first })
    }
}
