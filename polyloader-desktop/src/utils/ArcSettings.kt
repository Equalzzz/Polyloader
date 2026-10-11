package org.im.polyloader.utils

import org.im.polyloader.Polyloader
import java.io.BufferedInputStream
import java.io.DataInputStream
import java.io.File

// Ruthlessly ripped from Arc core
class ArcSettings {
    companion object {
        const val TYPE_BOOL: Byte = 0
        const val TYPE_INT: Byte = 1
        const val TYPE_LONG: Byte = 2
        const val TYPE_FLOAT: Byte = 3
        const val TYPE_STRING: Byte = 4
        const val TYPE_BIN: Byte = 5
    }

    private val values : MutableMap<String, Any?> = HashMap()

    // We assume that settings file is ideal, because game won't start otherwise
    fun load() {
        DataInputStream(BufferedInputStream(getSettingsFile().inputStream())).use { stream ->
            val count = stream.readInt()
            repeat(count) {
                val key = stream.readUTF()
                val type = stream.readByte()

                when (type) {
                    TYPE_BOOL -> values[key] = stream.readBoolean()
                    TYPE_INT -> values[key] = stream.readInt()
                    TYPE_LONG -> values[key] = stream.readLong()
                    TYPE_FLOAT -> values[key] = stream.readFloat()
                    TYPE_STRING -> values[key] = stream.readUTF()
                    TYPE_BIN -> {
                        val length = stream.readInt()
                        val bytes = stream.readNBytes(length)
                        values[key] = bytes
                    }
                }
            }
        }
    }

    fun get(key : String) : Any? = values[key]

    fun getSettingsFile() : File =
        Polyloader.mindustryDataDirectory.resolve("settings.bin")
}