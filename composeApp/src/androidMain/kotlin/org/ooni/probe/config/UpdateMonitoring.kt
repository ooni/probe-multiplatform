package org.ooni.probe.config

import android.app.Activity

interface UpdateMonitoring {
    fun onResume(activity: Activity)
}
