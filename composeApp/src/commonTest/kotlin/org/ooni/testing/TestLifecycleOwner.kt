package org.ooni.testing

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry

class TestLifecycleOwner(
    val state: Lifecycle.State,
) : LifecycleOwner {
    private val registry = LifecycleRegistry(this).apply {
        currentState = state
    }
    override val lifecycle: Lifecycle = registry
}
