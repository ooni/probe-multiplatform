package org.ooni.probe.config

import android.app.Activity
import android.app.AlertDialog
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallState
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import ooniprobe.composeapp.generated.resources.Dashboard_Update_Ready
import ooniprobe.composeapp.generated.resources.Dashboard_Update_Restart
import ooniprobe.composeapp.generated.resources.Res
import org.jetbrains.compose.resources.getString

// We want to do a Flexible update: the download happens in the background
// while the user continues using the app, and we show a warning when it's ready
// to restart
class AndroidUpdateMonitoring : UpdateMonitoring {
    private var checkedIfUpdateIsAvailable = false
    private var userStartedUpdate = false

    override fun onResume(activity: Activity) {
        val appUpdateManager = AppUpdateManagerFactory.create(activity)
        val installListener = object : InstallStateUpdatedListener {
            override fun onStateUpdate(state: InstallState) {
                if (state.installStatus() == InstallStatus.DOWNLOADED) {
                    appUpdateManager.showInstallDownloadedUpdateMessage(activity)
                    appUpdateManager.unregisterListener(this)
                }
            }
        }

        if (!checkedIfUpdateIsAvailable) {
            // We only check once at app start
            val appUpdateInfoTask = appUpdateManager.appUpdateInfo
            appUpdateInfoTask.addOnSuccessListener { info ->
                if (
                    info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE &&
                    info.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)
                ) {
                    // Register listener to know when the app finishes downloading
                    appUpdateManager.registerListener(installListener)
                    appUpdateManager.startUpdateFlowForResult(
                        info,
                        activity,
                        AppUpdateOptions
                            .newBuilder(AppUpdateType.FLEXIBLE)
                            .build(),
                        ACTIVITY_CODE,
                    )
                    userStartedUpdate = true
                }
            }
            checkedIfUpdateIsAvailable = true
        } else if (userStartedUpdate) {
            // If the update download finished when the app was in the background,
            // we still need the user to complete it
            appUpdateManager
                .appUpdateInfo
                .addOnSuccessListener { appUpdateInfo ->
                    if (appUpdateInfo.installStatus() == InstallStatus.DOWNLOADED) {
                        appUpdateManager.showInstallDownloadedUpdateMessage(activity)
                    }
                }
        }
    }

    // Update is downloaded but not installed, we need to ask the user to complete the update
    private fun AppUpdateManager.showInstallDownloadedUpdateMessage(activity: Activity) {
        CoroutineScope(Dispatchers.Main).launch {
            AlertDialog.Builder(activity)
                .setMessage(getString(Res.string.Dashboard_Update_Ready))
                .setPositiveButton(
                    getString(Res.string.Dashboard_Update_Restart),
                ) { _, _ -> completeUpdate() }
                .show()
        }
    }

    companion object {
        private const val ACTIVITY_CODE = 1
    }
}
