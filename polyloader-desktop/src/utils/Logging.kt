package org.im.polyloader.utils

import java.util.logging.Logger
import java.util.logging.Level

// Adds some levels I wanted to have
class ExtendedLevel : Level {
    companion object {
        val DEBUG = ExtendedLevel("DEBUG", 850, "sun.util.logging.resources.logging")
        val ERROR = ExtendedLevel("ERROR", 1100, "sun.util.logging.resources.logging")
    }
    constructor(name: String, value: Int, resourceBundle: String) : super(name, value, resourceBundle)
}

// Adds debug(...) function to display debug messages
fun Logger.debug(msg: String) {
    log(ExtendedLevel.DEBUG, msg)
}

fun Logger.error(msg: String, e: Exception? = null) {
    if (e != null)
        return log(ExtendedLevel.ERROR, msg, e)
    log(ExtendedLevel.ERROR, msg)
}
fun Logger.error(e: Exception) {
    log(ExtendedLevel.ERROR, e.stackTraceToString())
}