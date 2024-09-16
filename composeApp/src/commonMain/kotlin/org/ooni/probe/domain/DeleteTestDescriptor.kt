package org.ooni.probe.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import okio.Path
import org.ooni.probe.data.models.InstalledTestDescriptorModel
import org.ooni.probe.data.models.MeasurementModel
import org.ooni.probe.data.models.toDescriptor
import org.ooni.probe.data.repositories.PreferenceRepository

class DeleteTestDescriptor(
    private val preferencesRepository: PreferenceRepository,
    private val deleteByRunId: suspend (InstalledTestDescriptorModel.Id) -> Unit,
    private val deleteMeasurementByResultRunId: suspend (InstalledTestDescriptorModel.Id) -> Unit,
    private val deleteResultByRunId: suspend (InstalledTestDescriptorModel.Id) -> Unit,
    private val selectMeasurementsByResultRunId: suspend (InstalledTestDescriptorModel.Id) -> Flow<List<MeasurementModel>>,
    private val deleteFile: suspend (Path) -> Unit,
) {
    suspend operator fun invoke(testDescriptor: InstalledTestDescriptorModel) {
        preferencesRepository.removeDescriptorPreferences(
            descriptor = testDescriptor.toDescriptor(),
        )
        selectMeasurementsByResultRunId(testDescriptor.id).first().let {
            it.forEach { measurement ->
                deleteFile(measurement.logFilePath)
                measurement.reportFilePath?.let { reportFilePath ->
                    deleteFile(reportFilePath)
                }
            }
        }
        deleteMeasurementByResultRunId(testDescriptor.id)
        deleteResultByRunId(testDescriptor.id)
        deleteByRunId(testDescriptor.id)
    }
}
