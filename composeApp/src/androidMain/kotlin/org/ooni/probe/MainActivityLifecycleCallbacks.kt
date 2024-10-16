package org.ooni.probe

import android.app.Activity
import android.app.Application.ActivityLifecycleCallbacks
import android.os.Bundle

class MainActivityLifecycleCallbacks : ActivityLifecycleCallbacks {
    var activity: MainActivity? = null
        private set

    override fun onActivityCreated(
        activity: Activity,
        savedInstanceState: Bundle?,
    ) {
        this.activity = activity as? MainActivity
    }

    override fun onActivityStarted(activity: Activity) {}

    override fun onActivityResumed(activity: Activity) {}

    override fun onActivityPaused(activity: Activity) {}

    override fun onActivityStopped(activity: Activity) {}

    override fun onActivitySaveInstanceState(
        activity: Activity,
        outState: Bundle,
    ) {}

    override fun onActivityDestroyed(activity: Activity) {
        this.activity = null
    }
}
