package org.ooni.probe

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import org.ooni.engine.AndroidOonimkallBridge
import org.ooni.probe.di.Dependencies

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val bridge = AndroidOonimkallBridge()
        val dependencies = Dependencies(bridge, filesDir.absolutePath)

        setContent {
            App(dependencies)
        }
    }
}
