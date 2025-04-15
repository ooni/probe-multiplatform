package org.ooni.probe.data.models

sealed interface MeasurementsFilter {
    data object All : MeasurementsFilter

    data class Result(val resultId: ResultModel.Id) : MeasurementsFilter

    data class Measurement(val measurementId: MeasurementModel.Id) : MeasurementsFilter
}
