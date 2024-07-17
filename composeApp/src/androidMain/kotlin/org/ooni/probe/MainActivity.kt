package org.ooni.probe

import App
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import di.Dependencies
import platform.GoOONIProbeClientBridge

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val client = GoOONIProbeClient(this)
        val dependencies = Dependencies(object : GoOONIProbeClientBridge {
            override fun apiCall(funcName: String): String {
                return client.call(funcName)
            }

            override fun apiCallWithArgs(funcName: String, args: String): String {
                return client.callWithArgs(funcName, args)
            }
        })

        setContent {
            App(dependencies)
        }
    }
}
