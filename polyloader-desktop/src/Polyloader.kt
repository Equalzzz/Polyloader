package org.im.polyloader

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.im.polyloader.mixin.PolyloaderClassLoader
import org.im.polyloader.mixin.PolyloaderMixinService
import org.im.polyloader.mixin.PolyloaderMixinTransformer
import org.im.polyloader.utils.*
import org.spongepowered.asm.launch.MixinBootstrap
import org.spongepowered.asm.mixin.MixinEnvironment
import org.spongepowered.asm.mixin.Mixins
import org.spongepowered.asm.service.MixinService
import java.io.File
import java.io.IOException
import java.nio.file.Paths
import java.util.*
import java.util.logging.*
import java.util.zip.ZipInputStream


@Serializable
data class PolyloaderConfigData(val mindustryConfigFile: String = "__BLANK__")
@Serializable
data class GameConfigData(val classPath : List<String>, val mainClass : String, val vmArgs : List<String>)
@Serializable
data class ModConfigData(val displayName: String, val name: String, val author: String, val main: String, val description: String, val version: String)

data class JsonConfig<DataType>(val file: File, val data: DataType)

object Polyloader {
    const val JSON_MOD_CONFIG_NAME = "mod.json"
    const val HJSON_MOD_CONFIG_NAME = "mod.hjson"
    val MOD_CONFIG_NAMES = listOf(JSON_MOD_CONFIG_NAME, HJSON_MOD_CONFIG_NAME)
    const val DEFAULT_CONFIG_NAME = "Polyloader"
    const val DEFAULT_LOG_NAME = "last_log"
    //const val LOGGER_FORMAT = $$"[%1$tH:%1$tM:%1$tS] %4$s: %5$s%6$s%n" // [hh:mm:ss] 'LOG_LEVEL': 'msg' 'stacktrace'
    const val DEFAULT_LOGGER_FORMAT = $$"[%4$.1s] [Polyloader] %5$s%6$s%n" // [first letter of 'LOG_LEVEL'] [Polyloader] 'msg' 'stacktrace'

    val log: Logger = Logger.getLogger(this::class.java.name)
    // We are typically in '../Polyloader/jre/bin/' path
    // Lets get just '../Polyloader/' directory
    val polyloaderDirectory : File = Paths.get("").toAbsolutePath().parent.parent.toFile()
    val mindustryDataDirectory : File = File(ArcOS.getAppDataDirectoryString("Mindustry"))

    init {
        // Applies format and locale changes here + creates log.txt in launcher path
        initLogger()
    }

    lateinit var polyloaderConfig: JsonConfig<PolyloaderConfigData>
        private set
    lateinit var gameConfig: JsonConfig<GameConfigData>
        private set
    lateinit var mindustryExecutable: File
        private set
    lateinit var classLoader : PolyloaderClassLoader
        private set
    val settings = ArcSettings()

    @JvmStatic
    fun main(args: Array<String>) {
        log.info("Initializing Polyloader")
        // Finds a valid config with all the info about where mindustry is
        polyloaderConfig = getLoaderConfig(polyloaderDirectory)

        // Finds a valid config with all data on how to launch mindustry
        gameConfig = getGameConfig(polyloaderConfig)

        // Tries to cache paths and actualize data in polyloader config
        simplifyLoaderConfigData(polyloaderConfig, gameConfig)

        // Gets mindustry desktop.jar file
        mindustryExecutable = File(gameConfig.file.absolutePath).parentFile.resolve(gameConfig.data.classPath.first())
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

        loadSettings()

        classLoader = PolyloaderClassLoader(arrayOf(mindustryExecutable.toURI().toURL()), Polyloader::class.java.classLoader)

        MixinBootstrap.init()
        MixinEnvironment.getDefaultEnvironment().setSide(MixinEnvironment.Side.CLIENT).activeTransformer
        classLoader.addTransformer(PolyloaderMixinTransformer())

        Mixins.addConfiguration("polyloader-test.mixins.json")

        (MixinService.getService() as PolyloaderMixinService).onGameStart()

        val mindustryMainClass = classLoader.loadClass("mindustry.desktop.DesktopLauncher")
        val mindustryMainMethod = mindustryMainClass.getDeclaredMethod("main", Array<String>::class.java)
        mindustryMainMethod.invoke(null, emptyArray<String>())


        //mindustryDataDirectory.resolve("mods").listFiles()?.forEach {
        //    loadModModifiers(it)
        //}

        classLoader.close()
    }

    fun loadSettings() {
        try {
            settings.load()
            log.info("Successfully loaded Mindustry settings.bin")
        }
        catch (e: IOException) {
            log.error("Unable to load Mindustry settings. Check if game is able to run normally.")
            error(e)
        }
    }

    data class UnzippedFile(val name: String, val content: String)

    fun loadModModifiers(file: File) {
        // for jars and zips
        if (file.isFile) {
            // Unzips into a list of files
            val files = ZipInputStream(file.inputStream()).use { zis ->
                generateSequence { zis.nextEntry }
                    .filterNot { it.isDirectory }
                    .map { UnzippedFile(it.name, zis.readAllBytes().toString(Charsets.UTF_8)) }
                    .toList()
            }
            // Finds mod.[h]json in mod root
            val modConfig = files.first { MOD_CONFIG_NAMES.any { sampleName -> it.name.endsWith(sampleName) } }

            var mixinConfigFile : File? = null
            val modFileToName : MutableMap<UnzippedFile, String> = HashMap()

            // Parse hjson OR json and get:
            // 1. Mod name
            // 2. Mixin configuration
            if (modConfig.name.endsWith(HJSON_MOD_CONFIG_NAME)) {
                try {
                    val modConfigData = SimpleHJsonParser(modConfig.content)
                    val modName = modConfigData.get("name") as String
                    modFileToName[modConfig] = modName
                    mixinConfigFile = File(modConfigData.get("mixin") as String)
                    log.info("Found mixin config file from mod $modName")
                }
                catch (e : Exception) {
                    log.error("Couldn't parse file or find mixin config", e)
                }
                //log.debug(JsonValue.readHjson(modConfig.content).toString())
            }
            // Try parse json
            else if (modConfig.name.endsWith(JSON_MOD_CONFIG_NAME)) {
                //json.parseToJsonElement(modConfig.content).jsonObject.
            }
        }
    }

    // That doesn't really do what I wanted yet
    private fun openGame() {
        val cmd = ArrayList<String>(8)
        cmd.add(System.getProperty("java.home") + File.separator + "bin" + File.separator + "java" + (if (ArcOS.isWindows) ".exe" else ""))
        if (ArcOS.isMac)
            cmd.add("-XstartOnFirstThread") // still don't know what it does
        gameConfig.data.vmArgs.forEach { cmd.add(it.trim()) }
        cmd.add("-jar")
        cmd.add(mindustryExecutable.absolutePath)
        log.debug("Assembled a command:\n${cmd.joinToString(" ")}")
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
            log.log(Level.WARNING, "Could not find valid config file. Creating a new one...")
            return createBlankLoaderConfig(path)
        }
        // if everything is okay, then return the file
        log.info("Found ${polyloaderConfig.file.name} launcher config file")
        return polyloaderConfig
    }

    private fun createBlankLoaderConfig(path: File): JsonConfig<PolyloaderConfigData> {
        val fileName = "$DEFAULT_CONFIG_NAME.json"
        val blankData = PolyloaderConfigData()
        val dataString = Json.encodeToString(PolyloaderConfigData.serializer(), blankData)
        val file = File(path.toPath().toAbsolutePath().toString() + File.separator + fileName)
        file.writeText(dataString)
        log.info("Created new $fileName launcher config file")
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
            log.warning("Specified path '${gameConfigPath.absolutePath}' for Mindustry config file is incorrect\n Searching in adjacent directories...")
            gameConfig = findClosestJson<GameConfigData>(polyloaderDirectory)
            if (gameConfig == null) {
                // 2. Try to find it inside parent
                gameConfig = findClosestJson<GameConfigData>(polyloaderDirectory.parentFile)
                if (gameConfig == null) {
                    // 3. Try to find it inside siblings, but only inside 'mIndUsTrY' folders
                    polyloaderDirectory.parentFile.listFiles()?.filter { it.isDirectory && it.name.lowercase().contains("mindustry") } ?. forEach {
                        gameConfig = findClosestJson<GameConfigData>(it)
                        if (gameConfig != null) {
                            log.info("Found ${gameConfig.file.name} Mindustry config file")
                            return gameConfig
                        }
                    }
                    // 4. Try to find it inside siblings, but we try to find any mindustry.json file in
                    polyloaderDirectory.parentFile.listFiles()?.filter { it -> it.isDirectory && it?.listFiles()!!.any { it.name.lowercase().contains("mindustry") }} ?. forEach {
                        gameConfig = findClosestJson<GameConfigData>(it)
                        if (gameConfig != null) {
                            log.info("Found ${gameConfig.file.name} Mindustry config file")
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
                    log.log(Level.SEVERE, msg)
                    error(msg)
                    // TODO:
                    // Display a dialog box, informing on how to fix the issue
                }
            }
        }
        log.info("Found ${gameConfig.file.name} Mindustry config file")
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
        Locale.setDefault(Locale.getDefault())
        System.setProperty("java.util.logging.SimpleFormatter.format", DEFAULT_LOGGER_FORMAT)
        log.useParentHandlers = false
        val formatter = SimpleFormatter()
        val outHandler = StreamHandler(System.out, formatter)
        log.addHandler(outHandler)
        try {
            val lastLog = File(polyloaderDirectory, "$DEFAULT_LOG_NAME.txt")
            val fileHandler = FileHandler(lastLog.absolutePath, false)
            fileHandler.formatter = formatter
            log.addHandler(fileHandler)
        }
        catch (e: IOException) {
            log.log(Level.SEVERE, "Could not create log file. All logging is performed in IDE", e)
        }
    }
}