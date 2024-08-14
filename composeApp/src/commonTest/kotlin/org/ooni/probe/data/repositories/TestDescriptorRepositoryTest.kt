package org.ooni.probe.data.repositories

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.ooni.probe.data.models.InstalledTestDescriptorModel
import org.ooni.probe.data.models.NetTest
import org.ooni.probe.di.Dependencies
import org.ooni.testing.createTestDatabaseDriver
import org.ooni.testing.factories.DescriptorFactory
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class TestDescriptorRepositoryTest {
    private lateinit var subject: TestDescriptorRepository

    @BeforeTest
    fun before() {
        subject =
            TestDescriptorRepository(
                database = Dependencies.buildDatabase(::createTestDatabaseDriver),
                json = Dependencies.buildJson(),
                backgroundDispatcher = Dispatchers.Default,
            )
    }

    @Test
    fun createAndGet() =
        runTest {
            val model =
                DescriptorFactory.buildInstalledModel(
                    netTests =
                        listOf(
                            NetTest("web_connectivity", inputs = listOf("https://ooni.org")),
                        ),
                    nameIntl = mapOf("PT" to "Teste"),
                )
            subject.createOrIgnore(listOf(model))

            val result = subject.list().first().first()
            assertEquals(model, result)
        }

    @Test
    fun createDuplicatedIsIgnored() =
        runTest {
            val model =
                DescriptorFactory.buildInstalledModel(
                    id = InstalledTestDescriptorModel.Id(123L),
                )
            subject.createOrIgnore(listOf(model, model))

            val result = subject.list().first()
            assertEquals(1, result.size)
        }
}
