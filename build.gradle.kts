import groovy.json.JsonOutput
import org.gradle.internal.extensions.stdlib.capitalized


plugins {
    alias(libs.plugins.kotlin.jvm)
}

allprojects {
    group = "org.im.polyloader"
    version = "1.0.0"
}

kotlin {
    jvmToolchain(libs.versions.java.get().toInt())
}

val build = layout.buildDirectory.get()
val modules = build.dir("modules")
val buildName = rootProject.name.capitalized()

// Cache known tasks
evaluationDependsOn(":polyloader-desktop")
evaluationDependsOn(":polyloader-core")
val polyloaderDesktopJar = project(":polyloader-desktop").tasks.named("shadowJar")
val polyloaderCoreJar = project(":polyloader-core").tasks.jar

// Copies :jar results from subprojects to /modules build directory
val prepareBuild = tasks.register("prepareBuild") {
    dependsOn(polyloaderDesktopJar, polyloaderCoreJar)
    group = "build"
    doLast {
        copy {
            from(polyloaderDesktopJar)
            from(polyloaderCoreJar)
            into(modules)
        }
    }
}

// Creates and runs a local build of this project. Used only for testing and debugging
tasks.register<JavaExec>("localBuildAndRun") {
    dependsOn(prepareBuild)
    group = "build"
    val targetDir = file(build).resolve(buildName)
    workingDir = targetDir
    mainClass.set("org.im.polyloader.Polyloader")
    doFirst {
        if (targetDir.exists())
            targetDir.deleteRecursively()
        val executablesDir = targetDir.resolve("app")
        executablesDir.mkdirs()
        copy {
            from(modules)
            into(executablesDir)
        }
        val mindustryConfigProperty = properties["game-config"] ?:
            error("game-config gradle property must be set for local build to run")
        val configFile = targetDir.resolve("$buildName.json")
        val config = mapOf("mindustryConfigFile" to mindustryConfigProperty.toString())
        val configString = JsonOutput.prettyPrint(JsonOutput.toJson(config))
        configFile.writeText(configString)
    }
    classpath = files(targetDir.resolve("app/${project(":polyloader-desktop").name}.jar"))
}