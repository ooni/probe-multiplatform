package org.ooni.probe.background

import androidx.work.WorkInfo

open class EarlyStopWorkerException(reason: Int?) :
    Exception("Early stop due to ${reasonToString(reason)}") {
    companion object {
        private fun reasonToString(reason: Int?) =
            when (reason) {
                WorkInfo.STOP_REASON_FOREGROUND_SERVICE_TIMEOUT ->
                    "STOP_REASON_FOREGROUND_SERVICE_TIMEOUT"

                WorkInfo.STOP_REASON_UNKNOWN ->
                    "STOP_REASON_UNKNOWN"

                WorkInfo.STOP_REASON_CANCELLED_BY_APP ->
                    "STOP_REASON_CANCELLED_BY_APP"

                WorkInfo.STOP_REASON_PREEMPT ->
                    "STOP_REASON_PREEMPT"

                WorkInfo.STOP_REASON_TIMEOUT ->
                    "STOP_REASON_TIMEOUT"

                WorkInfo.STOP_REASON_DEVICE_STATE ->
                    "STOP_REASON_DEVICE_STATE"

                WorkInfo.STOP_REASON_CONSTRAINT_BATTERY_NOT_LOW ->
                    "STOP_REASON_CONSTRAINT_BATTERY_NOT_LOW"

                WorkInfo.STOP_REASON_CONSTRAINT_CHARGING ->
                    "STOP_REASON_CONSTRAINT_CHARGING"

                WorkInfo.STOP_REASON_CONSTRAINT_CONNECTIVITY ->
                    "STOP_REASON_CONSTRAINT_CONNECTIVITY"

                WorkInfo.STOP_REASON_CONSTRAINT_DEVICE_IDLE ->
                    "STOP_REASON_CONSTRAINT_DEVICE_IDLE"

                WorkInfo.STOP_REASON_CONSTRAINT_STORAGE_NOT_LOW ->
                    "STOP_REASON_CONSTRAINT_STORAGE_NOT_LOW"

                WorkInfo.STOP_REASON_QUOTA ->
                    "STOP_REASON_QUOTA"

                WorkInfo.STOP_REASON_BACKGROUND_RESTRICTION ->
                    "STOP_REASON_BACKGROUND_RESTRICTION"

                WorkInfo.STOP_REASON_APP_STANDBY ->
                    "STOP_REASON_APP_STANDBY"

                WorkInfo.STOP_REASON_USER ->
                    "STOP_REASON_USER"

                WorkInfo.STOP_REASON_SYSTEM_PROCESSING ->
                    "STOP_REASON_SYSTEM_PROCESSING"

                WorkInfo.STOP_REASON_ESTIMATED_APP_LAUNCH_TIME_CHANGED ->
                    "STOP_REASON_ESTIMATED_APP_LAUNCH_TIME_CHANGED"

                else ->
                    reason.toString()
            }
    }
}
