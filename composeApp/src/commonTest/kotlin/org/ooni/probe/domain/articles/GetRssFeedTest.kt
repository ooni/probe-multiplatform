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
                assertEquals("https://ooni.org/post/2025-gg-omg-village/images/omg-banner.png", imageUrl)
            }
        }

    companion object {
        private val RSS_FEED = """
            <?xml version="1.0" encoding="utf-8" standalone="yes"?>
            <rss version="2.0" xmlns:atom="http://www.w3.org/2005/Atom" xmlns:media="http://search.yahoo.com/mrss/">
                <channel>
                    <title>Blog posts on OONI: Open Observatory of Network Interference</title>
                    <link>https://ooni.org/blog/</link>
                    <description>Recent content in Blog posts on OONI: Open Observatory of Network Interference</description>
                    <generator>Hugo</generator>
                    <language>en</language>
                    <atom:link href="https://ooni.org/blog/index.xml" rel="self" type="application/rss+xml"/>
                    <item>
                        <title>Join us at the OMG Village at the Global Gathering 2025!</title>
                        <link>https://ooni.org/post/2025-gg-omg-village/</link>
                        <media:content url="https://ooni.org/post/2025-gg-omg-village/images/omg-banner.png" medium="image"/>
                        <pubDate>Mon, 01 Sep 2025 00:00:00 +0000</pubDate>
                        <guid>https://ooni.org/post/2025-gg-omg-village/</guid>
                        <description>
                            &lt;div>
                             &lt;a href="https://ooni.org/post/2025-gg-omg-village/images/omg-banner.png">
                             &lt;img
                             src="https://ooni.org/post/2025-gg-omg-village/images/omg-banner_hu_1e6c0cc0ca63d2d9.png"


                             srcset="https://ooni.org/post/2025-gg-omg-village/images/omg-banner_hu_79aea09410c8ceb7.png 2x"


                             title="OMG Village announcement"

                             alt="OMG Village announcement"

                             />
                             &lt;/a>

                            &lt;/div>
                        </description>
                    </item>
                </channel>
            </rss>
        """.trimIndent()
    }
}
