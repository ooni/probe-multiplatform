package org.ooni.engine

import co.touchlab.kermit.Logger

/** Swift-facing bridge to Kermit [Logger]. */
object IosEngineLogger {
    fun debug(message: String) = Logger.d(message)

    fun info(message: String) = Logger.i(message)

    fun warn(message: String) = Logger.w(message)

    fun error(message: String) = Logger.e(message)
}
