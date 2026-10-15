import groovy.json.JsonOutput
import org.gradle.internal.extensions.stdlib.capitalized


plugins {
    alias(libs.plugins.kotlin.jvm)
    //alias(libs.plugins.launch4j)
    //alias(libs.plugins.download)
}

allprojects {
    group = "org.im.polyloader"
    version = "1.0-SNAPSHOT"
}

kotlin {
    jvmToolchain(libs.versions.java.get().toInt())
}

val build = layout.buildDirectory.get()
val buildName = rootProject.name.capitalized()
tasks.register("localBuild") {
    dependsOn(":polyloader-desktop:jar", ":polyloader-core:jar")
    group = "build"
    doLast {
        val targetDir = file(build).resolve("${rootProject.name.capitalized()}/jre")
        if (targetDir.exists())
            targetDir.deleteRecursively()
        targetDir.mkdirs()
        copy {
            from(project(":polyloader-desktop").tasks.named("shadowJar"))
            from(project(":polyloader-core").tasks.jar)
            into(targetDir)
        }
    }
}

tasks.register<JavaExec>("LocalBuildAndRun") {
    dependsOn("localBuild")
    group = "build"
    val targetDir = file(build).resolve(buildName)
    workingDir = targetDir.resolve("jre")
    mainClass.set("org.im.polyloader.Polyloader")
    doFirst {
        val mindustryConfigProperty = properties["game-config"] ?:
        error("game-config gradle property must be set for local build to run")
        val configFile = targetDir.resolve("$buildName.json")
        val config = mapOf("mindustryConfigFile" to mindustryConfigProperty.toString())
        val configString = JsonOutput.prettyPrint(JsonOutput.toJson(config))
        configFile.writeText(configString)
    }
    classpath = files(targetDir.resolve("jre/${project(":polyloader-desktop").name}.jar"))
}