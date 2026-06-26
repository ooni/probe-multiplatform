package org.ooni.probe.shared

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.Snapshot
import java.awt.AWTEvent
import java.awt.Toolkit
import java.awt.event.AWTEventListener
import java.awt.event.InputEvent
import java.awt.event.KeyEvent
import java.awt.event.MouseEvent

@Composable
fun rememberAltPressedState(): State<Boolean> {
    val state = remember { mutableStateOf(false) }
    DisposableEffect(Unit) {
        val toolkit = Toolkit.getDefaultToolkit()
        val listener = AWTEventListener { event ->
            val pressed = when (event) {
                is KeyEvent -> {
                    if (event.keyCode != KeyEvent.VK_ALT &&
                        event.keyCode != KeyEvent.VK_ALT_GRAPH
                    ) {
                        return@AWTEventListener
                    }
                    event.id == KeyEvent.KEY_PRESSED
                }
                // Mouse events carry the live modifier mask, so any subsequent
                // mouse interaction recovers from key-up events that were swallowed
                // by a native popup menu (e.g. macOS NSMenu).
                is MouseEvent -> (event.modifiersEx and InputEvent.ALT_DOWN_MASK) != 0
                else -> return@AWTEventListener
            }
            if (state.value != pressed) {
                Snapshot.withMutableSnapshot { state.value = pressed }
            }
        }
        toolkit.addAWTEventListener(
            listener,
            AWTEvent.KEY_EVENT_MASK or AWTEvent.MOUSE_EVENT_MASK,
        )
        onDispose { toolkit.removeAWTEventListener(listener) }
    }
    return state
}
