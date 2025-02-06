package org.ooni.probe.ui.shared

import android.app.Activity
import co.touchlab.kermit.Logger
import com.google.android.play.core.review.ReviewManagerFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

fun showAppReview(
    activity: Activity,
    onShown: suspend () -> Unit,
) {
    val manager = ReviewManagerFactory.create(activity)
    val request = manager.requestReviewFlow()
    request.addOnCompleteListener { task ->
        if (task.isSuccessful) {
            val flow = manager.launchReviewFlow(activity, task.result)
            flow.addOnCompleteListener {
                Logger.i("App Review flow requested")
                CoroutineScope(Dispatchers.Default).launch {
                    onShown()
                }
            }
        } else {
            Logger.w("Could not launch review flow", task.exception)
        }
    }
}
