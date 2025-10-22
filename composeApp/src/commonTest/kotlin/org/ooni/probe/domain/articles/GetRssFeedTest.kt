package org.ooni.probe.domain.articles

import kotlinx.coroutines.test.runTest
import org.ooni.engine.models.Success
import org.ooni.probe.data.models.ArticleModel
import kotlin.test.Test
import kotlin.test.assertEquals

class GetRssFeedTest {
    @Test
    fun invoke() =
        runTest {
            val subject = GetRSSFeed(
                httpDo = { _, _, _ -> Success(RSS_FEED) },
                url = "https://example.org",
                source = ArticleModel.Source.Blog,
            )

            val articles = subject().get()!!
            assertEquals(1, articles.size)
            with(articles.first()) {
                assertEquals("https://ooni.org/post/2025-gg-omg-village/", url.value)
                assertEquals("Join us at the OMG Village at the Global Gathering 2025!", title)
                assertEquals(2025, time.year)
            }
        }

    companion object {
        private const val RSS_FEED =
            "<?xml version=\"1.0\" encoding=\"utf-8\" standalone=\"yes\"?><rss version=\"2.0\" xmlns:atom=\"http://www.w3.org/2005/Atom\"><channel><title>Blog posts on OONI: Open Observatory of Network Interference</title><link>https://ooni.org/blog/</link><description>Recent content in Blog posts on OONI: Open Observatory of Network Interference</description><generator>Hugo</generator><language>en</language><atom:link href=\"https://ooni.org/blog/index.xml\" rel=\"self\" type=\"application/rss+xml\"/><item><title>Join us at the OMG Village at the Global Gathering 2025!</title><link>https://ooni.org/post/2025-gg-omg-village/</link><pubDate>Mon, 01 Sep 2025 00:00:00 +0000</pubDate><guid>https://ooni.org/post/2025-gg-omg-village/</guid><description>&lt;p>Are you attending the upcoming &lt;a href=\"https://wiki.digitalrights.community/index.php?title=Global_Gathering_2025\">Global Gathering&lt;/a> event in Estoril, Portugal? Are you interested in investigating internet shutdowns and censorship, and curious to learn more about the tools and open datasets that support this work?&lt;/p></description></item></channel></rss>"
    }
}
