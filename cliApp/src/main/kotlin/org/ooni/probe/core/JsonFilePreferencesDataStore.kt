package org.ooni.probe.core

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.double
import kotlinx.serialization.json.float
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import java.io.File

/**
 * A [DataStore] of [Preferences] backed by a plain JSON file instead of DataStore's protobuf-lite
 * file serializer, which is problematic with GraalVM native-image.
 */
internal class JsonFilePreferencesDataStore(
    private val file: File,
) : DataStore<Preferences> {
    private val mutex = Mutex()
    private var cached: Preferences? = null

    override val data: Flow<Preferences> = flow { emit(mutex.withLock { loadLocked() }) }

    override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences =
        mutex.withLock {
            val updated = transform(loadLocked()).toPreferences()
            file.parentFile?.mkdirs()
            file.writeText(encode(updated).toString())
            cached = updated
            updated
        }

    private fun loadLocked(): Preferences {
        cached?.let { return it }
        val loaded = if (file.exists() && file.length() > 0) {
            decode(Json.parseToJsonElement(file.readText()).jsonObject)
        } else {
            emptyPreferences()
        }
        cached = loaded
        return loaded
    }

    private fun encode(preferences: Preferences): JsonObject =
        JsonObject(preferences.asMap().entries.associate { (key, value) -> key.name to encodeValue(value) })

    private fun encodeValue(value: Any): JsonObject =
        when (value) {
            is Boolean -> typed("boolean", JsonPrimitive(value))
            is Int -> typed("int", JsonPrimitive(value))
            is Long -> typed("long", JsonPrimitive(value))
            is Float -> typed("float", JsonPrimitive(value))
            is Double -> typed("double", JsonPrimitive(value))
            is String -> typed("string", JsonPrimitive(value))
            is Set<*> -> typed("stringSet", JsonArray(value.map { JsonPrimitive(it.toString()) }))
            else -> error("Unsupported preference value type: ${value::class}")
        }

    private fun typed(type: String, value: JsonElement) = JsonObject(mapOf("type" to JsonPrimitive(type), "value" to value))

    private fun decode(root: JsonObject): Preferences {
        val prefs: MutablePreferences = mutablePreferencesOf()
        root.forEach { (name, element) ->
            val entry = element.jsonObject
            val value = entry["value"] ?: return@forEach
            when (entry["type"]?.jsonPrimitive?.contentOrNull) {
                "boolean" -> prefs[booleanPreferencesKey(name)] = value.jsonPrimitive.boolean
                "int" -> prefs[intPreferencesKey(name)] = value.jsonPrimitive.int
                "long" -> prefs[longPreferencesKey(name)] = value.jsonPrimitive.long
                "float" -> prefs[floatPreferencesKey(name)] = value.jsonPrimitive.float
                "double" -> prefs[doublePreferencesKey(name)] = value.jsonPrimitive.double
                "string" -> prefs[stringPreferencesKey(name)] = value.jsonPrimitive.content
                "stringSet" -> prefs[stringSetPreferencesKey(name)] = value.jsonArray.map { it.jsonPrimitive.content }.toSet()
            }
        }
        return prefs.toPreferences()
    }
}
