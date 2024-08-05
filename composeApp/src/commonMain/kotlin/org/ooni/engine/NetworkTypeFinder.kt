package org.ooni.engine

import org.ooni.engine.models.NetworkType

fun interface NetworkTypeFinder {
    operator fun invoke(): NetworkType
}
