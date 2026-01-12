package org.ooni.testing.factories

import org.ooni.engine.models.NetworkType
import org.ooni.engine.models.TaskOrigin
import org.ooni.engine.models.TestType
import org.ooni.probe.config.OrganizationConfig
import org.ooni.probe.data.models.MeasurementModel
import org.ooni.probe.data.models.NetworkModel
import org.ooni.probe.di.Dependencies
import kotlin.concurrent.Volatile

class DatabaseHelper private constructor(
    private val dependency: Dependencies,
) {
    companion object {
        @Volatile
        private var instance: DatabaseHelper? = null

        fun initialize(dependency: Dependencies): DatabaseHelper {
            if (instance == null) {
                instance = DatabaseHelper(dependency)
            }
            return instance!!
        }

        val shared: DatabaseHelper
            get() = instance ?: throw IllegalStateException("DatabaseHelper is not initialized")

        suspend fun clear() {
            val clearStorage by lazy { shared.dependency.clearStorage }
            clearStorage.invoke()
        }

        suspend fun setup() {
            shared.dependency.resultRepository.deleteAll()

            val networkId = shared.dependency.networkRepository.createIfNew(
                NetworkModel(
                    networkName = "Vodafone Italia",
                    asn = "AS12345",
                    countryCode = "IT",
                    networkType = NetworkType.Wifi,
                ),
            )

            if (OrganizationConfig.baseSoftwareName.contains("ooni")) {
                setupOoni(networkId)
            } else {
                setupDw(networkId)
            }
        }

        private suspend fun setupDw(networkId: NetworkModel.Id) {
            val trustedId = shared.dependency.resultRepository.createOrUpdate(
                ResultModelFactory.build(
                    id = null,
                    networkId = networkId,
                    descriptorKey = Descriptor.Key(
                        id = Descriptor.Id("10004"),
                        revision = 2,
                    ),
                    isViewed = true,
                    isDone = true,
                    dataUsageUp = 1257,
                    dataUsageDown = 26589,
                    taskOrigin = TaskOrigin.AutoRun,
                ),
            )
            val selectedId = shared.dependency.resultRepository.createOrUpdate(
                ResultModelFactory.build(
                    id = null,
                    networkId = networkId,
                    descriptorKey = Descriptor.Key(
                        id = Descriptor.Id("10005"),
                        revision = 4,
                    ),
                    isViewed = true,
                    isDone = true,
                    dataUsageUp = 1257,
                    dataUsageDown = 26589,
                    taskOrigin = TaskOrigin.AutoRun,
                ),
            )
            val globalId = shared.dependency.resultRepository.createOrUpdate(
                ResultModelFactory.build(
                    id = null,
                    networkId = networkId,
                    descriptorKey = Descriptor.Key(
                        id = Descriptor.Id("10006"),
                        revision = 5,
                    ),
                    isViewed = true,
                    isDone = true,
                    dataUsageUp = 1267,
                    dataUsageDown = 37189,
                    taskOrigin = TaskOrigin.AutoRun,
                ),
            )

            shared.dependency.measurementRepository.createOrUpdate(
                MeasurementModelFactory.build(
                    resultId = trustedId,
                    test = TestType.WebConnectivity,
                    urlId = shared.dependency.urlRepository.createOrUpdate(
                        UrlModelFactory.build(url = "https://www.dw.com"),
                    ),
                    reportId = MeasurementModel.ReportId("20250205T153106Z_webconnectivity_DE_3209_n1_iB2GPLBoLLpSlEYf"),
                    isDone = true,
                    isUploaded = true,
                ),
            )
            listOf(
                "https://www.francemediasmonde.com/",
                "https://www.mc-doualiya.com/",
                "https://www.bbc.com/",
                "http://www.lemonde.fr/",
                "https://www.rferl.org/",
                "http://www.rfi.fr/",
                "http://www.voanews.com/",
                "https://ici.radio-canada.ca/rci/en",
                "https://www.rfa.org/english/",
                "https://www.france24.com/en/",
                "https://www3.nhk.or.jp/nhkworld/",
                "https://www.abc.net.au/news",
                "https://www.swissinfo.ch/eng/",
                "https://www.srgssr.ch/en/home/",
            ).forEach { url ->
                shared.dependency.measurementRepository.createOrUpdate(
                    MeasurementModelFactory.build(
                        resultId = trustedId,
                        test = TestType.WebConnectivity,
                        urlId = shared.dependency.urlRepository.createOrUpdate(
                            UrlModelFactory.build(url = url),
                        ),
                        reportId = MeasurementModel.ReportId("12345"),
                        isDone = true,
                        isUploaded = true,
                    ),
                )
            }

            repeat(91) {
                shared.dependency.measurementRepository.createOrUpdate(
                    MeasurementModelFactory.build(
                        resultId = selectedId,
                        test = TestType.WebConnectivity,
                        urlId = shared.dependency.urlRepository.createOrUpdate(
                            UrlModelFactory.build(url = "https://example.org"),
                        ),
                        reportId = MeasurementModel.ReportId("12345"),
                        isDone = true,
                        isUploaded = true,
                    ),
                )
            }

            repeat(142) {
                shared.dependency.measurementRepository.createOrUpdate(
                    MeasurementModelFactory.build(
                        resultId = globalId,
                        test = TestType.WebConnectivity,
                        urlId = shared.dependency.urlRepository.createOrUpdate(
                            UrlModelFactory.build(url = "https://example.org"),
                        ),
                        reportId = MeasurementModel.ReportId("12345"),
                        isDone = true,
                        isUploaded = true,
                    ),
                )
            }
        }

        private suspend fun setupOoni(networkId: NetworkModel.Id) {
            val websitesResultId = shared.dependency.resultRepository.createOrUpdate(
                ResultModelFactory.build(
                    id = null,
                    networkId = networkId,
                    descriptorName = "websites",
                    isViewed = true,
                    isDone = true,
                    dataUsageUp = 257,
                    dataUsageDown = 12345,
                    taskOrigin = TaskOrigin.AutoRun,
                ),
            )
            shared.dependency.measurementRepository.createOrUpdate(
                MeasurementModelFactory.build(
                    resultId = websitesResultId,
                    test = TestType.WebConnectivity,
                    urlId = shared.dependency.urlRepository.createOrUpdate(
                        UrlModelFactory.build(url = "https://z-lib.org/"),
                    ),
                    reportId = MeasurementModel.ReportId("20250210T113750Z_webconnectivity_IT_12874_n1_qx1LFyoqM4orUsor"),
                    isDone = true,
                    isUploaded = true,
                    isAnomaly = true,
                ),
            )
            listOf(
                "https://ooni.org",
                "https://twitter.com",
                "https://facebook.com",
                "https://peta.org",
                "https://www.ran.org",
                "https://leap.se",
                "https://ilga.org",
                "https://gpgtools.org",
                "https://cdt.org",
                "https://www.viber.com",
                "https://anonymouse.org",
                "https://mail.proton.me",
                "https://kick.com",
                "https://ipfs.io",
                "https://imgur.com",
                "https://icq.com",
                "https://duckduckgo.com",
                "https://discord.com",
                "https://cloudflare-ipfs.com",
                "https://app.element.io",
                "https://github.com",
            ).forEach { url ->
                shared.dependency.measurementRepository.createOrUpdate(
                    MeasurementModelFactory.build(
                        resultId = websitesResultId,
                        urlId = shared.dependency.urlRepository.createOrUpdate(
                            UrlModelFactory.build(url = url),
                        ),
                        isDone = true,
                        isUploaded = true,
                        isAnomaly = false,
                        reportId = MeasurementModel.ReportId("1234"),
                    ),
                )
            }
            listOf(
                "http://mp3cool.pro",
                "https://ytx.mx",
                "https://sci-hub.se",
                "https://vibe3.com",
                "https://cb01.in",
                "http://ulub.pl",
            ).forEach { url ->
                shared.dependency.measurementRepository.createOrUpdate(
                    MeasurementModelFactory.build(
                        resultId = websitesResultId,
                        test = TestType.WebConnectivity,
                        urlId = shared.dependency.urlRepository.createOrUpdate(
                            UrlModelFactory.build(url = url),
                        ),
                        isDone = true,
                        isUploaded = true,
                        isAnomaly = true,
                        reportId = MeasurementModel.ReportId("1234"),
                    ),
                )
            }

            val imResultId = shared.dependency.resultRepository.createOrUpdate(
                ResultModelFactory.build(
                    id = null,
                    networkId = networkId,
                    descriptorName = "instant_messaging",
                    isViewed = true,
                    isDone = true,
                    dataUsageUp = 257,
                    dataUsageDown = 12345,
                    taskOrigin = TaskOrigin.AutoRun,
                ),
            )
            listOf(
                TestType.Whatsapp,
                TestType.Telegram,
                TestType.FacebookMessenger,
                TestType.Signal,
            ).forEach { testType ->
                shared.dependency.measurementRepository.createOrUpdate(
                    MeasurementModelFactory.build(
                        resultId = imResultId,
                        test = testType,
                        isDone = true,
                        isUploaded = true,
                        reportId = MeasurementModel.ReportId("1234"),
                    ),
                )
            }

            val circumventionResultId = shared.dependency.resultRepository.createOrUpdate(
                ResultModelFactory.build(
                    id = null,
                    networkId = networkId,
                    descriptorName = "circumvention",
                    isViewed = true,
                    isDone = true,
                    dataUsageUp = 257,
                    dataUsageDown = 12345,
                    taskOrigin = TaskOrigin.AutoRun,
                ),
            )
            listOf(
                TestType.Tor,
                TestType.Psiphon,
                TestType.HttpHeaderFieldManipulation,
                TestType.HttpInvalidRequestLine,
            ).forEach { testType ->
                shared.dependency.measurementRepository.createOrUpdate(
                    MeasurementModelFactory.build(
                        resultId = circumventionResultId,
                        test = testType,
                        isDone = true,
                        isUploaded = true,
                        reportId = MeasurementModel.ReportId("1234"),
                    ),
                )
            }

            val performanceResultId = shared.dependency.resultRepository.createOrUpdate(
                ResultModelFactory.build(
                    id = null,
                    networkId = networkId,
                    descriptorName = "performance",
                    isViewed = true,
                    isDone = true,
                    dataUsageUp = 257,
                    dataUsageDown = 12345,
                    taskOrigin = TaskOrigin.AutoRun,
                ),
            )
            shared.dependency.measurementRepository.createOrUpdate(
                MeasurementModelFactory.build(
                    resultId = performanceResultId,
                    test = TestType.Ndt,
                    isDone = true,
                    isUploaded = true,
                    testKeys = """{"summary":{"upload":6058.420633995402,"download":554105.6493846333,"ping":28}}""",
                    reportId = MeasurementModel.ReportId("1234"),
                ),
            )
            shared.dependency.measurementRepository.createOrUpdate(
                MeasurementModelFactory.build(
                    resultId = performanceResultId,
                    test = TestType.Dash,
                    reportId = MeasurementModel.ReportId("20250210T143842Z_dash_IT_1267_n1_1hoAk1rFwFsAoyXH"),
                    isDone = true,
                    isUploaded = true,
                    testKeys = """{"simple":{"median_bitrate":230936,"upload":1000,"download":2000}}""",
                ),
            )
        }
    }
}
