package org.im.polyloader

import kotlinx.serialization.*
import kotlinx.serialization.json.*
import java.io.File
import java.io.IOException
import java.nio.file.Paths
import java.util.Locale
import java.util.logging.FileHandler
import java.util.logging.Level
import java.util.logging.Logger
import java.util.logging.SimpleFormatter
import java.util.logging.StreamHandler

@Serializable
data class LoaderConfigData(val gameConfigPath: String)
@Serializable
data class GameConfig(val classPath : List<String>, val mainClass : String, val vmArgs : List<String>)

object Polyloader {
    const val DEFAULT_CONFIG_NAME = "Polyloader"
    const val DEFAULT_LOG_NAME = "last_log"
    const val LOGGER_FORMAT = $$"[%1$tH:%1$tM:%1$tS] %4$s: %5$s%6$s%n" // [hh:mm:ss] LOG_LEVEL: msg STACKTRACE
    val LOCALE: Locale = Locale.ENGLISH
    val LOGGER: Logger = Logger.getLogger(this::class.java.name)
    // We are typically in '../Polyloader/jre/bin/' path
    // Lets get just '../Polyloader/' directory
    val polyloaderDirectory : File = Paths.get("").toAbsolutePath().parent.parent.toFile()
    init {
        // Applies format and locale changes here + creates log.txt in launcher path
        initLogger()
    }

        @JvmStatic
        fun main(args: Array<String>) {
            // Finds a valid config with all the info about where mindustry is
            val config = getLoaderConfig(polyloaderDirectory)
            val dataString = config.readText()
            validateLoaderConfig(dataString)
        }

        // Finds or creates semi-valid launcher config
        fun getLoaderConfig(path: File): File {
            // find all .json files in polyloader root
            val jsons = path.listFiles()?.filter { it.extension == "json" }
                ?: error("could not find any json files") // it never throws an error for some reason
            // check each until anything looks like a correct polyloader config
            for (file in jsons) {
                val json = Json { ignoreUnknownKeys = true }
                try {
                    json.decodeFromString<LoaderConfigData>(file.readText())
                    return file
                } catch (_: SerializationException) { }
            }
            // if it fails to find one, then it creates it out of thin air
            LOGGER.log(Level.WARNING, "Could not find valid config file. Creating a new one...")
            return createBlankLoaderConfig(path)
        }

        fun createBlankLoaderConfig(path: File): File {
            val fileName = "$DEFAULT_CONFIG_NAME.json"
            val dataString = Json.encodeToString(LoaderConfigData.serializer(), LoaderConfigData(" "))
            val file = File(path.toPath().toAbsolutePath().toString() + File.separator + fileName)
            file.writeText(dataString)
            return file
        }

        // Checks whether all data inside of config is valid
        // otherwise tries to fix it by writing actual data
        // or just throws an error
        @OptIn(ExperimentalSerializationApi::class)
        fun validateLoaderConfig(dataString: String) {
            val json = Json { ignoreUnknownKeys = true }
            val data = json.decodeFromString<LoaderConfigData>(dataString)
            val gameConfig = File(data.gameConfigPath)
            if (gameConfig.exists() && gameConfig.isFile && gameConfig.extension == "json") {
                try {
                    val config = json.decodeFromStream<GameConfig>(gameConfig.inputStream())
                    // TODO:
                    // Find vital files using this config
                }
                catch (_: SerializationException) { }
                // TODO:
                // 1. As a fallback, in case there are no useful data in 'loader config'
                // try to find 'mindustry json' in PARENTS, SIBLINGS and CHILDREN folders
                // 2. Find vital files
                // 3. Write that info to 'loader config'
            }
        }

        private fun initLogger() {
            Locale.setDefault(LOCALE)
            System.setProperty("java.util.logging.SimpleFormatter.format", LOGGER_FORMAT)
            LOGGER.useParentHandlers = false
            val formatter = SimpleFormatter()
            val outHandler = StreamHandler(System.out, formatter)
            LOGGER.addHandler(outHandler)
            try {
                val lastLog = File(polyloaderDirectory, "$DEFAULT_LOG_NAME.txt")
                val fileHandler = FileHandler(lastLog.absolutePath, false)
                fileHandler.formatter = formatter
                LOGGER.addHandler(fileHandler)
            }
            catch (e: IOException) {
                LOGGER.log(Level.SEVERE, "Could not create log file. All logging is performed in IDE", e)
            }
        }
    }
}
