package org.ooni.probe.domain

import androidx.compose.ui.graphics.Color
import ooniprobe.composeapp.generated.resources.Dashboard_Circumvention_Card_Description
import ooniprobe.composeapp.generated.resources.Dashboard_Circumvention_Overview_Paragraph
import ooniprobe.composeapp.generated.resources.Dashboard_Experimental_Card_Description
import ooniprobe.composeapp.generated.resources.Dashboard_Experimental_Overview_Paragraph
import ooniprobe.composeapp.generated.resources.Dashboard_InstantMessaging_Card_Description
import ooniprobe.composeapp.generated.resources.Dashboard_InstantMessaging_Overview_Paragraph
import ooniprobe.composeapp.generated.resources.Dashboard_Performance_Card_Description
import ooniprobe.composeapp.generated.resources.Dashboard_Performance_Overview_Paragraph
import ooniprobe.composeapp.generated.resources.Dashboard_Websites_Card_Description
import ooniprobe.composeapp.generated.resources.Dashboard_Websites_Overview_Paragraph
import ooniprobe.composeapp.generated.resources.Res
import ooniprobe.composeapp.generated.resources.TestResults_NotAvailable
import ooniprobe.composeapp.generated.resources.Test_Circumvention_Fullname
import ooniprobe.composeapp.generated.resources.Test_Experimental_Fullname
import ooniprobe.composeapp.generated.resources.Test_InstantMessaging_Fullname
import ooniprobe.composeapp.generated.resources.Test_Performance_Fullname
import ooniprobe.composeapp.generated.resources.Test_Websites_Fullname
import ooniprobe.composeapp.generated.resources.performance_datausage
import ooniprobe.composeapp.generated.resources.small_datausage
import ooniprobe.composeapp.generated.resources.test_circumvention
import ooniprobe.composeapp.generated.resources.test_experimental
import ooniprobe.composeapp.generated.resources.test_instant_messaging
import ooniprobe.composeapp.generated.resources.test_performance
import ooniprobe.composeapp.generated.resources.test_websites
import ooniprobe.composeapp.generated.resources.websites_datausage
import org.ooni.probe.data.models.DefaultTestDescriptor
import org.ooni.probe.data.models.NetTest

class GetDefaultTestDescriptors {
    operator fun invoke(): List<DefaultTestDescriptor> =
        listOf(
            WEBSITES,
            INSTANT_MESSAGING,
            CIRCUMVENTION,
            PERFORMANCE,
            EXPERIMENTAL,
        )

    companion object {
        private val WEBSITES =
            DefaultTestDescriptor(
                label = "websites",
                title = Res.string.Test_Websites_Fullname,
                shortDescription = Res.string.Dashboard_Websites_Card_Description,
                description = Res.string.Dashboard_Websites_Overview_Paragraph,
                icon = Res.drawable.test_websites,
                color = Color(0xFF4c6ef5),
                animation = "anim/websites.json",
                dataUsage = Res.string.websites_datausage,
                netTests =
                    listOf(
                        NetTest(name = "web_connectivity"),
                    ),
            )

        private val INSTANT_MESSAGING =
            DefaultTestDescriptor(
                label = "instant_messaging",
                title = Res.string.Test_InstantMessaging_Fullname,
                shortDescription = Res.string.Dashboard_InstantMessaging_Card_Description,
                description = Res.string.Dashboard_InstantMessaging_Overview_Paragraph,
                icon = Res.drawable.test_instant_messaging,
                color = Color(0xFF15aabf),
                animation = "anim/instant_messaging.json",
                dataUsage = Res.string.small_datausage,
                netTests =
                    listOf(
                        NetTest(name = "whatsapp"),
                        NetTest(name = "telegram"),
                        NetTest(name = "facebook_messenger"),
                        NetTest(name = "signal"),
                    ),
            )

        private val CIRCUMVENTION =
            DefaultTestDescriptor(
                label = "circumvention",
                title = Res.string.Test_Circumvention_Fullname,
                shortDescription = Res.string.Dashboard_Circumvention_Card_Description,
                description = Res.string.Dashboard_Circumvention_Overview_Paragraph,
                icon = Res.drawable.test_circumvention,
                color = Color(0xFFe64980),
                animation = "anim/circumvention.json",
                dataUsage = Res.string.small_datausage,
                netTests =
                    listOf(
                        NetTest(name = "psiphon"),
                        NetTest(name = "tor"),
                    ),
            )

        private val PERFORMANCE =
            DefaultTestDescriptor(
                label = "performance",
                title = Res.string.Test_Performance_Fullname,
                shortDescription = Res.string.Dashboard_Performance_Card_Description,
                description = Res.string.Dashboard_Performance_Overview_Paragraph,
                icon = Res.drawable.test_performance,
                color = Color(0xFFbe4bdb),
                animation = "anim/performance.json",
                dataUsage = Res.string.performance_datausage,
                netTests =
                    listOf(
                        NetTest(name = "ndt"),
                        NetTest(name = "dash"),
                        NetTest(name = "http_header_field_manipulation"),
                        NetTest(name = "http_invalid_request_line"),
                    ),
            )

        private val EXPERIMENTAL =
            DefaultTestDescriptor(
                label = "experimental",
                title = Res.string.Test_Experimental_Fullname,
                shortDescription = Res.string.Dashboard_Experimental_Card_Description,
                description = Res.string.Dashboard_Experimental_Overview_Paragraph,
                icon = Res.drawable.test_experimental,
                color = Color(0xFF495057),
                animation = "anim/experimental.json",
                dataUsage = Res.string.TestResults_NotAvailable,
                netTests =
                    listOf(
                        NetTest(name = "stunreachability"),
                        NetTest(name = "dnscheck"),
                        NetTest(name = "riseupvpn"),
                        NetTest(name = "echcheck"),
                    ),
                longRunningTests =
                    listOf(
                        NetTest(name = "torsf"),
                        NetTest(name = "vanilla_tor"),
                    ),
            )
    }
}
