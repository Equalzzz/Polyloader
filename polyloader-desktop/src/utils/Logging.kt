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
fun Logger.debug(msg: String?, silent: Boolean = false) {
    if (!silent)
        return log(ExtendedLevel.DEBUG, "$msg\n\tat ${Thread.currentThread().stackTrace[3].let { "${it.className}.${it.methodName}(${it.fileName}:${it.lineNumber})" }}")
    log(ExtendedLevel.DEBUG, msg)
}

// Adds error(...) function to display errors (I know severe exists, it's not the same vibe)
fun Logger.error(msg: String, e: Exception? = null) {
    if (e != null)
        return log(ExtendedLevel.ERROR, msg, e)
    log(ExtendedLevel.ERROR, msg)
}
fun Logger.error(e: Exception) {
    log(ExtendedLevel.ERROR, e.stackTraceToString())
}

fun debug(msg: Any?, silent: Boolean = false) {
    println("\u001b[32m[D] [Polyloader] $msg\u001b[0m")
    if (!silent)
        println("\u001b[37m\tat ${getSource()}\u001b[0m")
}
fun info(msg: Any?) {
    println("[I] [Polyloader] $msg")
}
fun warn(msg: Any?, silent: Boolean = true) {
    println("\u001b[33m[W] [Polyloader] $msg\u001b[0m")
    if (!silent)
        println("\u001b[33m\tat ${getSource()}\u001b[0m")
}
fun logError(msg: Any?, e: Exception? = null) {
    println("\u001b[31m[E] [Polyloader] $msg\u001b[0m")
    e?.printStackTrace()
}
fun logError(e: Exception) {
    e.printStackTrace()
}
private fun getSource() : String =
    Thread.currentThread().stackTrace[4].let { "${it.className}.${it.methodName}(${it.fileName}:${it.lineNumber})" }