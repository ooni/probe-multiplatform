package org.ooni.probe.ui.shared

import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.pow

fun Long.formatDataUsage(): String {
    if (this <= 0) return "0"
    val units = arrayOf("B", "kB", "MB", "GB", "TB", "PB", "EB")
    val digitGroups = (log10(this.toDouble()) / log10(1024.0)).toInt()
    return (this / 1024.0.pow(digitGroups.toDouble())).format() + " " + units[digitGroups]
}

private fun Double.format(decimalChars: Int = 2): String {
    val absoluteValue = abs(this).toInt()
    val decimalValue = abs((this - absoluteValue) * 10.0.pow(decimalChars)).toInt()
    return if (decimalValue == 0) absoluteValue.toString() else "$absoluteValue.$decimalValue"
}
