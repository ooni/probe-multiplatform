package org.ooni.probe.cli

/**
 * Minimal JSON writer for machine-readable command output.
 *
 * `:cliApp` intentionally does not put kotlinx-serialization on its runtime classpath (probeCore
 * exposes it as `implementation`), so structured output is assembled here. Values passed to
 * [obj]/[arr] must already be JSON fragments produced by [str]/[bool]/[num]/[rawOrNull].
 */
internal object CliJson {
    private const val FORM_FEED = ''

    fun str(value: String?): String =
        if (value == null) {
            "null"
        } else {
            buildString {
                append('"')
                value.forEach { character ->
                    when (character) {
                        '"' -> append("\\\"")
                        '\\' -> append("\\\\")
                        '\b' -> append("\\b")
                        FORM_FEED -> append("\\f")
                        '\n' -> append("\\n")
                        '\r' -> append("\\r")
                        '\t' -> append("\\t")
                        else ->
                            if (character.code < 0x20) {
                                append("\\u")
                                append(character.code.toString(16).padStart(4, '0'))
                            } else {
                                append(character)
                            }
                    }
                }
                append('"')
            }
        }

    fun bool(value: Boolean): String = value.toString()

    fun num(value: Long): String = value.toString()

    fun numOrNull(value: Long?): String = value?.toString() ?: "null"

    /** Embeds an already-serialized JSON fragment verbatim, or `null`. */
    fun rawOrNull(json: String?): String = if (json.isNullOrBlank()) "null" else json

    fun obj(vararg fields: Pair<String, String>): String = fields.joinToString(",", "{", "}") { (key, value) -> "${str(key)}:$value" }

    fun arr(elements: List<String>): String = elements.joinToString(",", "[", "]")
}
