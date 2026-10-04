package org.im.polyloader.utils

import java.io.File

// Ruthlessly ripped from Arc core
object ArcOS {
    // TODO:
    // Android support
    private val fs = File.separator
    val osName = prop("os.name")
    val osArch = prop("os.arch")
    val userHome = prop("user.home")
    val isWindows = osName.lowercase().contains("win")
    val isMac = osName.lowercase().contains("mac")
    val isLinux = osName.lowercase().contains("linux") || osName.lowercase().contains("bsd")

    fun prop(name: String): String {
        return System.getProperty(name) ?: ""
    }
    fun env(name: String): String {
        return System.getenv(name) ?: ""
    }

    fun getAppDataDirectoryString(appName: String): String {
        if (isWindows)
            return "${env("AppData")}\\$appName"
        else if (isMac)
            return "$userHome/Library/Application Support/${appName}/"
        else if (isLinux) {
            if (System.getenv("XDG_DATA_HOME") != null) {
                var dir = env("XDG_DATA_HOME")
                if (!dir.endsWith("/"))
                    dir += "/"
                return "$dir$appName/"
            }
        }
        return ""
    }
}