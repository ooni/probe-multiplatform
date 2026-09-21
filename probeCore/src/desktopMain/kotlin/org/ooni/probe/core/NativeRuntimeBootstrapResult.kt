package org.ooni.probe.core

/** Populated by [configureBundledNativeLibraries] with the names of the libraries it applied. */
data class NativeRuntimeBootstrapResult(
    val appliedLibraries: List<String>,
)
