package org.ooni.probe.data.models

import org.ooni.engine.models.TaskOrigin
import org.ooni.engine.models.TestType
import org.ooni.probe.di.Dependencies
import kotlin.test.Test
import kotlin.test.assertEquals

class RunSpecificationTest {
    private val json = Dependencies.buildJson()

    @Test
    fun serializationFull() {
        val spec = RunSpecification.Full(
            tests = listOf(
                RunSpecification.Test(
                    descriptorId = Descriptor.Id("1234"),
                    netTests = listOf(
                        NetTest(
                            test = TestType.WebConnectivity,
                            inputs = listOf("https://example.org"),
                        ),
                    ),
                ),
            ),
            taskOrigin = TaskOrigin.OoniRun,
        )
        val specJson = json.encodeToString<RunSpecification>(spec)
        val output = json.decodeFromString<RunSpecification>(specJson)
        assertEquals(spec, output)
    }

    @Test
    fun serializationRerun() {
        val spec = RunSpecification.Rerun(ResultModel.Id(1234L))
        val specJson = json.encodeToString<RunSpecification>(spec)
        val output = json.decodeFromString<RunSpecification>(specJson)
        assertEquals(spec, output)
    }

    @Test
    fun serializationOnlyMissingUploads() {
        val spec = RunSpecification.OnlyUploadMissingResults
        val specJson = json.encodeToString<RunSpecification>(spec)
        val output = json.decodeFromString<RunSpecification>(specJson)
        assertEquals(spec, output)
    }
}
