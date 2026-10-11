package org.im.polyloader.utils

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import org.im.polyloader.JsonConfig
import java.io.File

val json = Json { ignoreUnknownKeys = true }

/**
 * Finds first .json file that looks like a **serialized** data class of type <[DataType]> inside a given path
 */
inline fun <reified DataType> findClosestJson(file: File): JsonConfig<DataType>? {
    // find all .json files in 'where' path
    val files = file.listFiles()?.filter{ it.extension == "json" } ?: return null
    // check each until anything looks like a correct config
    for (file in files) {
        val data = tryParseJson<DataType>(file) ?: continue
        return JsonConfig(file.normalize(), data as DataType)
    }
    return null
}

// Marked experimental to shut the compiler (decodeFromStream was used)
@OptIn(ExperimentalSerializationApi::class)
inline fun <reified DataType> tryParseJson(file: File) : DataType? {
    try {
        return json.decodeFromStream<DataType>(file.inputStream())
    } catch (_: Exception) { }
    return null
}