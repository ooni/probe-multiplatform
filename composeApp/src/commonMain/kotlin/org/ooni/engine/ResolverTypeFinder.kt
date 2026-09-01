package org.ooni.engine

import org.ooni.engine.models.ResolverType

fun interface ResolverTypeFinder {
    operator fun invoke(): ResolverType
}
