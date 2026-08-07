package org.ooni.probe.shared

import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.roundToInt

fun Long.formatDataUsage(): String {
    if (this <= 0) return "0"
    val units = arrayOf("B", "kB", "MB", "GB", "TB", "PB", "EB")
    val digitGroups = (log10(this.toDouble()) / log10(1024.0)).toInt()
    return (this / 1024.0.pow(digitGroups.toDouble())).format() + " " + units[digitGroups]
}

fun Double.format(decimalChars: Int = 2): String {
    val order = 10.0.pow(decimalChars).toInt()
    val roundedValue = (this * order).roundToInt()
    val absoluteValue = roundedValue / order
    val decimalValue = roundedValue % order
    if (decimalValue == 0) return absoluteValue.toString()
    val zeroPadding = (decimalChars - log10(decimalValue.toDouble()).toInt()).coerceAtLeast(0)
    val decimalString = decimalValue.toString().padStart(zeroPadding, '0')
    return "$absoluteValue.$decimalString"
}

fun Number.largeNumberShort(): String {
    val number = toLong()
    if (number <= 0) return "0"
    val units = arrayOf("", "K", "M")
    val digitGroups = (log10(toDouble()) / log10(1000.0)).toInt()
    return (number / 1000.0.pow(digitGroups.toDouble())).withFractionalDigits() + units[digitGroups]
}

fun Double.withFractionalDigits(): String = if (this < 10) format(2) else format(1)
