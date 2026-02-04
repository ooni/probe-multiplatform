package org.ooni.probe.shared

import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.pow

fun Long.formatDataUsage(): String {
    if (this <= 0) return "0"
    val units = arrayOf("B", "kB", "MB", "GB", "TB", "PB", "EB")
    val digitGroups = (log10(this.toDouble()) / log10(1024.0)).toInt()
    return (this / 1024.0.pow(digitGroups.toDouble())).format() + " " + units[digitGroups]
}

fun Double.format(decimalChars: Int = 2): String {
    val absoluteValue = abs(this).toInt()
    val decimalValue = abs((this - absoluteValue) * 10.0.pow(decimalChars)).toInt()
    return if (decimalValue == 0) absoluteValue.toString() else "$absoluteValue.$decimalValue"
}

fun Number.largeNumberShort(): String {
    val number = toLong()
    if (number <= 0) return "0"
    val units = arrayOf("", "K", "M")
    val digitGroups = (log10(toDouble()) / log10(1000.0)).toInt()
    return (number / 1000.0.pow(digitGroups.toDouble())).withFractionalDigits() + units[digitGroups]
}

fun Double.withFractionalDigits(): String = if (this < 10) format(2) else format(1)
