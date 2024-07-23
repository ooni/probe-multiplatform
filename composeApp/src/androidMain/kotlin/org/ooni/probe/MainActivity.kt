package org.ooni.probe

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import org.ooni.engine.AndroidOoniEngine
import org.ooni.probe.di.Dependencies

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val engine = AndroidOoniEngine()
        val dependencies = Dependencies(engine)

        setContent {
            App(dependencies)
        }
    }
}
