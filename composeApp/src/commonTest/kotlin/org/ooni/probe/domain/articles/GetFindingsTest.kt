package org.ooni.probe.domain.articles

import kotlinx.coroutines.test.runTest
import org.ooni.engine.models.Success
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GetFindingsTest {
    @Test
    fun invoke() =
        runTest {
            val subject = GetFindings(
                httpDo = { _, _, _ -> Success(API_RESPONSE) },
            )

            val articles = subject().get()!!
            assertEquals(2, articles.size)
            with(articles.first()) {
                assertTrue(url.value.endsWith("8025203600"))
                assertEquals("Indonesia blocked access to the Internet Archive", title)
                assertEquals("This report shares OONI data on the blocking of the Internet Archive in Indonesia in May 2025.", description)
                assertEquals(2025, time.year)
            }
        }

    companion object {
        private const val API_RESPONSE =
            "{\"incidents\":[{\"id\":\"8025203600\",\"email_address\":\"\",\"title\":\"Indonesia blocked access to the Internet Archive\",\"short_description\":\"This report shares OONI data on the blocking of the Internet Archive in Indonesia in May 2025.\",\"slug\":\"2025-indonesia-blocked-access-to-the-internet-archive\",\"start_time\":\"2025-05-26T00:00:00.000000Z\",\"create_time\":\"2025-06-13T07:35:49.000000Z\",\"update_time\":\"2025-06-13T07:35:49.000000Z\",\"end_time\":\"2025-05-29T00:00:00.000000Z\",\"reported_by\":\"Elizaveta Yachmeneva, Maria Xynou\",\"creator_account_id\":\"\",\"published\":true,\"event_type\":\"incident\",\"ASNs\":[23693,63859,24203,17451,136119,7713,18004,23951,139447],\"CCs\":[\"ID\"],\"themes\":[],\"tags\":[\"censorship\",\"archive.org\"],\"test_names\":[\"web_connectivity\"],\"domains\":[\"archive.org\"],\"links\":[],\"mine\":false},{\"id\":\"178720534001\",\"email_address\":\"\",\"title\":\"Malaysia blocked MalaysiaNow and website of former MP\",\"short_description\":\"This report shares OONI data on the blocking of news media outlet MalaysiaNow and of a website which belongs to a former Malaysian Member of Parliament (Wee Choo Keong). \",\"slug\":null,\"start_time\":\"2023-06-28T00:00:00.000000Z\",\"create_time\":\"2023-12-19T09:07:46.000000Z\",\"update_time\":\"2025-06-02T11:50:22.000000Z\",\"end_time\":\"2024-09-07T00:00:00.000000Z\",\"reported_by\":\"Maria Xynou\",\"creator_account_id\":\"\",\"published\":true,\"event_type\":\"incident\",\"ASNs\":[10030,4788,4818,9534,38466,45960,38322,4818],\"CCs\":[\"MY\"],\"themes\":[\"news_media\"],\"tags\":[\"censorship\",\"MalaysiaNow\",\"Wee Choo Keong\"],\"test_names\":[\"web_connectivity\"],\"domains\":[\"www.malaysianow.com\",\"weechookeong.com\"],\"links\":[],\"mine\":false}]}"
    }
}
