package org.im.polyloader

import kotlinx.serialization.*
import kotlinx.serialization.json.*
import java.io.File
import java.io.IOException
import java.nio.file.Paths
import java.util.*
import java.util.Locale
import java.util.logging.*


@Serializable
data class PolyloaderConfigData(val mindustryConfigFile: String = "__BLANK__")
@Serializable
data class GameConfigData(val classPath : List<String>, val mainClass : String, val vmArgs : List<String>)

data class JsonConfig<DataType>(val file: File, val data: DataType)

val json = Json { ignoreUnknownKeys = true }

object Polyloader {
    const val DEFAULT_CONFIG_NAME = "Polyloader"
    const val DEFAULT_LOG_NAME = "last_log"
    const val LOGGER_FORMAT = $$"[%1$tH:%1$tM:%1$tS] %4$s: %5$s%6$s%n" // [hh:mm:ss] LOG_LEVEL: msg STACKTRACE
    val LOCALE: Locale = Locale.ENGLISH
    val LOGGER: Logger = Logger.getLogger(this::class.java.name)
    // We are typically in '../Polyloader/jre/bin/' path
    // Lets get just '../Polyloader/' directory
    val polyloaderDirectory : File = Paths.get("").toAbsolutePath().parent.parent.toFile()
    val IS_WINDOWS : Boolean = System.getProperty("os.name").lowercase(LOCALE).contains("win")
    val IS_MAC : Boolean = System.getProperty("os.name").lowercase(LOCALE).contains("mac")

    init {
        // Applies format and locale changes here + creates log.txt in launcher path
        initLogger()
    }

    lateinit var polyloaderConfig: JsonConfig<PolyloaderConfigData>
        private set
    lateinit var gameConfig: JsonConfig<GameConfigData>
        private set

    @JvmStatic
    fun main(args: Array<String>) {

        LOGGER.info("Initializing Polyloader")

        // Finds a valid config with all the info about where mindustry is
        polyloaderConfig = getLoaderConfig(polyloaderDirectory)

        // Finds a valid config with all data on how to launch mindustry
        gameConfig = getGameConfig(polyloaderConfig)

        // tries to cache paths and actualize data in polyloader config
        simplifyLoaderConfigData(polyloaderConfig, gameConfig)

        // TODO:
        // 0. Find mindustry setting and apply them before opening (like ui scale, it requires restart for some reason)
        // 1. Find mods folder
        // 2. Get all mod mixins configs
        // 3. Apply mixins
        // 3.5? Apply access wideners
        // 4. Open the game
        //
        // minor things:
        // Add icon and a name to the java process
        // Make a way to tell apart modified and unmodified versions (apart from adding THE CORE)

        // That doesn't really do what I wanted
        val cmd = ArrayList<String>(8)
        cmd.add(System.getProperty("java.home") + File.separator + "bin" + File.separator + "java" + (if (IS_WINDOWS) ".exe" else ""))
        if (IS_MAC)
            cmd.add("-XstartOnFirstThread") // still don't know what it does
        gameConfig.data.vmArgs.forEach { cmd.add(it.trim()) }
        cmd.add("-jar")
        cmd.add(File(gameConfig.file.absolutePath).parentFile.resolve(gameConfig.data.classPath.first()).absolutePath)
        LOGGER.info("Assembled a command:\n${cmd.joinToString(" ")}")
        ProcessBuilder(cmd)
            .redirectOutput(ProcessBuilder.Redirect.INHERIT)
            .redirectError(ProcessBuilder.Redirect.INHERIT)
            .start()


    }

    // Finds or creates semi-valid launcher config
    private fun getLoaderConfig(path: File): JsonConfig<PolyloaderConfigData> {
        val polyloaderConfig = findClosestJson<PolyloaderConfigData>(path)
        // if it fails to find one, then it creates it out of thin air
        if (polyloaderConfig == null) {
            LOGGER.log(Level.WARNING, "Could not find valid config file. Creating a new one...")
            return createBlankLoaderConfig(path)
        }
        // if everything is okay, then return the file
        LOGGER.info("Found ${polyloaderConfig.file.name} launcher config file")
        return polyloaderConfig
    }

    private fun createBlankLoaderConfig(path: File): JsonConfig<PolyloaderConfigData> {
        val fileName = "$DEFAULT_CONFIG_NAME.json"
        val blankData = PolyloaderConfigData()
        val dataString = Json.encodeToString(PolyloaderConfigData.serializer(), blankData)
        val file = File(path.toPath().toAbsolutePath().toString() + File.separator + fileName)
        file.writeText(dataString)
        LOGGER.info("Created new $fileName launcher config file")
        return JsonConfig(file, blankData)
    }

    // Gets game config file using polyloader config file, or tries to find it somewhere near
    // or just throws an error if everything fails, while having no idea, where game files might be
    private fun getGameConfig(loaderConfig: JsonConfig<PolyloaderConfigData>) : JsonConfig<GameConfigData> {
        // Regularly we have a path to loader config like /Polyloader/Polyloader.json
        // But to get the directory instead (to resolve it against given path), we use .parentFile
        // normalization converts nasty 'C:/SOME_PATH/./Polyloader/../PATH_II' to nice 'C:/SOME_PATH/PATH_II'
        val gameConfigPath = loaderConfig.file.parentFile.resolve(loaderConfig.data.mindustryConfigFile).normalize()
        var gameConfig : JsonConfig<GameConfigData>? = null
        if (gameConfigPath.exists()) {
            // if a directory specified, then try to find it there
            if (gameConfigPath.isDirectory)
                gameConfig = findClosestJson<GameConfigData>(gameConfigPath)
            // if a file path specified, then try to parse it directly
            else if (gameConfigPath.isFile) {
                val data = tryParseJson<GameConfigData>(gameConfigPath)
                if (data != null)
                    gameConfig = JsonConfig(gameConfigPath, data)
            }
        }
        if (gameConfig == null) {
            // FALLBACK:
            // in case path is incorrect
            // 1. Try to find it inside polyloader folder
            LOGGER.warning("Specified path '${gameConfigPath.absolutePath}' for Mindustry config file is incorrect\n Searching in adjacent directories...")
            gameConfig = findClosestJson<GameConfigData>(polyloaderDirectory)
            if (gameConfig == null) {
                // 2. Try to find it inside parent
                gameConfig = findClosestJson<GameConfigData>(polyloaderDirectory.parentFile)
                if (gameConfig == null) {
                    // 3. Try to find it inside siblings, but only inside 'mIndUsTrY' folders
                    polyloaderDirectory.parentFile.listFiles()?.filter { it.isDirectory && it.name.lowercase().contains("mindustry") } ?. forEach {
                        gameConfig = findClosestJson<GameConfigData>(it)
                        if (gameConfig != null) {
                            LOGGER.info("Found ${gameConfig.file.name} Mindustry config file")
                            return gameConfig
                        }
                    }
                    // 4. Try to find it inside siblings, but we try to find any mindustry.json file in
                    polyloaderDirectory.parentFile.listFiles()?.filter { it -> it.isDirectory && it?.listFiles()!!.any { it.name.lowercase().contains("mindustry") }} ?. forEach {
                        gameConfig = findClosestJson<GameConfigData>(it)
                        if (gameConfig != null) {
                            LOGGER.info("Found ${gameConfig.file.name} Mindustry config file")
                            return gameConfig
                        }
                    }
                    // 4. Cry (throw an error)
                    val msg =
                        """
                        Could not find game config file anywhere. Can't find the game without it
                        HOW TO FIX:
                            Relocate ${polyloaderDirectory.name} directory closer to Mindustry folder
                        OR
                            0. Find and remember path to Mindustry config file (generally called Mindustry.json, and sits inside /Mindustry/ folder, along with game's .exe file)
                            1. Go to ${polyloaderDirectory.absolutePath}
                            2. Open ${polyloaderConfig.file.name}
                            3. Write a correct path to game's config file in a "${polyloaderConfig.data::mindustryConfigFile.name}" property
                            4. Relaunch Polyloader
                        Searched in:
                        ${gameConfigPath.absolutePath}
                        ${polyloaderDirectory.absolutePath}
                        ${polyloaderDirectory.parentFile.absolutePath}
                        ${polyloaderDirectory.parentFile.listFiles()?.filter { it.isDirectory }?.joinToString {"\n"} ?: ""}
                        """
                    LOGGER.log(Level.SEVERE, msg)
                    error(msg)
                    // TODO:
                    // Display a dialog box, informing on how to fix the issue
                }
            }
        }
        LOGGER.info("Found ${gameConfig.file.name} Mindustry config file")
        return gameConfig
    }

    private fun simplifyLoaderConfigData(loaderConfig: JsonConfig<PolyloaderConfigData>, gameConfig: JsonConfig<GameConfigData>) {
        val previousData = loaderConfig.data
        val currentData = PolyloaderConfigData(gameConfig.file.absolutePath)
        if (currentData != previousData && loaderConfig.file.canWrite()) {
            val json = Json {
                ignoreUnknownKeys = true
                prettyPrint = true
            }
            loaderConfig.file.writeText(json.encodeToString(PolyloaderConfigData.serializer(), currentData))
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