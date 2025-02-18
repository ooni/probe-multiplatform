package org.ooni.probe.data.repositories

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.ooni.engine.models.TestType
import org.ooni.probe.data.models.InstalledTestDescriptorModel
import org.ooni.probe.data.models.NetTest
import org.ooni.probe.di.Dependencies
import org.ooni.probe.shared.now
import org.ooni.probe.shared.toLocalDateTime
import org.ooni.testing.createTestDatabaseDriver
import org.ooni.testing.factories.DescriptorFactory
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.days

class TestDescriptorRepositoryTest {
    private lateinit var subject: TestDescriptorRepository

    @BeforeTest
    fun before() {
        subject = TestDescriptorRepository(
            database = Dependencies.buildDatabase(::createTestDatabaseDriver),
            json = Dependencies.buildJson(),
            backgroundContext = Dispatchers.Default,
        )
    }

    @Test
    fun createAndGet() =
        runTest {
            val model = DescriptorFactory.buildInstalledModel(
                netTests = listOf(
                    NetTest(TestType.WebConnectivity, inputs = listOf("https://ooni.org")),
                ),
                nameIntl = mapOf("PT" to "Teste"),
            )
            subject.createOrIgnore(listOf(model))

            val result = subject.listAll().first().first()
            assertEquals(model, result)
        }

    @Test
    fun createDuplicatedIsIgnored() =
        runTest {
            val model = DescriptorFactory.buildInstalledModel(
                id = InstalledTestDescriptorModel.Id("ABC"),
                revision = 1,
            )
            subject.createOrIgnore(listOf(model, model))

            val result = subject.listAll().first()
            assertEquals(1, result.size)
        }

    @Test
    fun createOrUpdateClearsOldNetTests() =
        runTest {
            val oldModel = DescriptorFactory.buildInstalledModel(
                id = InstalledTestDescriptorModel.Id("ABC"),
                revision = 1,
                netTests = listOf(NetTest(TestType.FacebookMessenger)),
            )
            val newModel = oldModel.copy(revision = 2)
            subject.createOrUpdate(listOf(oldModel))
            subject.createOrUpdate(listOf(newModel))

            val result = subject.listAll().first()
            assertEquals(2, result.size)
            assertNull(result.first { it.revision == 1L }.netTests)
            assertEquals(newModel.netTests, result.first { it.revision == 2L }.netTests)
        }

    @Test
    fun listAllAndLatest() =
        runTest {
            val modelA1 = DescriptorFactory.buildInstalledModel(
                id = InstalledTestDescriptorModel.Id("A"),
                revision = 1,
                dateUpdated = now().minus(1.days).toLocalDateTime(),
            )
            val modelA2 = DescriptorFactory.buildInstalledModel(
                id = InstalledTestDescriptorModel.Id("A"),
                revision = 2,
                dateUpdated = now().toLocalDateTime(),
            )
            val modelB1 = DescriptorFactory.buildInstalledModel(
                id = InstalledTestDescriptorModel.Id("B"),
                revision = 1,
                dateUpdated = now().minus(1.days).toLocalDateTime(),
            )
            val modelB2 = DescriptorFactory.buildInstalledModel(
                id = InstalledTestDescriptorModel.Id("B"),
                revision = 2,
                dateUpdated = now().toLocalDateTime(),
            )
            subject.createOrIgnore(listOf(modelA1, modelA2, modelB1, modelB2))

            val all = subject.listAll().first()
            assertEquals(4, all.size)

            val latest = subject.listLatest().first()
            assertEquals(2, latest.size)
            assertContains(latest, modelA2)
            assertContains(latest, modelB2)
        }

    @Test
    fun listLatestWithSameDateUpdated() =
        runTest {
            val model1 = DescriptorFactory.buildInstalledModel(
                id = InstalledTestDescriptorModel.Id("A"),
                revision = 0,
                dateUpdated = now().toLocalDateTime(),
            )
            val model2 = model1.copy(revision = 1)
            subject.createOrIgnore(listOf(model1, model2))

            val latest = subject.listLatest().first()
            assertEquals(1, latest.size)
            assertContains(latest, model2)
        }

    @Test
    fun listLatestByRunIds() =
        runTest {
            val modelA1 = DescriptorFactory.buildInstalledModel(
                id = InstalledTestDescriptorModel.Id("A"),
                revision = 1,
                dateUpdated = now().minus(1.days).toLocalDateTime(),
            )
            val modelA2 = DescriptorFactory.buildInstalledModel(
                id = InstalledTestDescriptorModel.Id("A"),
                revision = 2,
                dateUpdated = now().toLocalDateTime(),
            )
            val modelB1 = DescriptorFactory.buildInstalledModel(
                id = InstalledTestDescriptorModel.Id("B"),
                revision = 1,
                dateUpdated = now().toLocalDateTime(),
            )
            subject.createOrIgnore(listOf(modelA1, modelA2, modelB1))

            val latest = subject.listLatestByRunIds(listOf(modelA1.id)).first()
            assertEquals(1, latest.size)
            assertContains(latest, modelA2)
        }

    private fun now() = Instant.fromEpochMilliseconds(Clock.System.now().toEpochMilliseconds())
}
