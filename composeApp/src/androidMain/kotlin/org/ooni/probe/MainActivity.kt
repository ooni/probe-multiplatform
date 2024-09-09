package org.ooni.probe

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.NavHostController
import co.touchlab.kermit.Logger

class MainActivity : ComponentActivity() {
    private lateinit var navController: NavHostController
    private val app get() = applicationContext as AndroidApplication

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            navController = App(app.dependencies)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (Intent.ACTION_VIEW == intent.action) {
            manageIntent(intent)
        }
    }

    private fun manageIntent(intent: Intent) {
        intent.data?.let { uri: Uri ->
            try {
                when (uri.host) {
                    "runv2" -> {
                        navController.navigate("add-descriptor/${uri.lastPathSegment}")
                    }

                    "run.test.ooni.org" -> {
                        navController.navigate("add-descriptor/${uri.lastPathSegment}")
                    }
                }
            } catch (e: Exception) {
                Logger.e(e) { "Failed to open run v2 link" }
            }
        }
    }
}
