import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    id("common-convention-plugin")
    alias(libs.plugins.shadow)
}

val sdkRoot: String? = System.getenv("ANDROID_HOME") ?: System.getenv("ANDROID_SDK_ROOT")
val isWindows = System.getProperty("os.name").lowercase().contains("windows")
val modArtifactName = project.name

dependencies {
    compileOnly(libs.mixin)
    compileOnly(if(libs.versions.mindustry.get() == "be") "Anuken:MindustryBuilds:latest" else "Anuken:Mindustry:${libs.versions.mindustry}")
}

sourceSets.main {
    kotlin.srcDirs("src")
}

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions{
        jvmTarget.set(JvmTarget.JVM_1_8)
    }
}

tasks.withType<JavaCompile>().configureEach {
    sourceCompatibility = "1.8"
    targetCompatibility = "1.8"
    options.release.set(8)
}

val jarAndroid = tasks.register("jarAndroid") {
    dependsOn("jar")

    doLast{
        if (sdkRoot.isNullOrEmpty() || !File(sdkRoot).exists()) {
            throw GradleException("No valid Android SDK found. Ensure that ANDROID_HOME is set to your Android SDK directory.")
        }

        val platformRoot = File("$sdkRoot/platforms/").listFiles()
            ?.sorted()
            ?.reversed()
            ?.find{ f -> File(f, "android.jar").exists() }
            ?: throw GradleException("No android.jar found. Ensure that you have an Android platform installed.")

        // collect dependencies needed for desugaring
        val dependencies = (configurations.compileClasspath.get().toList() +
                configurations.runtimeClasspath.get().toList() +
                listOf(File(platformRoot, "android.jar")))
            .joinToString(" "){ "--classpath ${it.path}" }

        // dex and desugar files - this requires d8 in your PATH
        val d8 = if (isWindows) "d8.bat" else "d8"

        val process = ProcessBuilder(
            "$d8 $dependencies --min-api 21 --output ${modArtifactName}Android.jar ${modArtifactName}Desktop.jar".split(" "))
            .directory(File("${layout.buildDirectory.get().asFile}/libs"))
            .redirectOutput(ProcessBuilder.Redirect.INHERIT)
            .redirectError(ProcessBuilder.Redirect.INHERIT)
            .start()
        process.waitFor()
    }
}

tasks.jar {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    archiveFileName.set("${modArtifactName}Desktop.jar")

    from(configurations.runtimeClasspath.map{ config -> config.map{ if (it.isDirectory) it else zipTree(it) } })

    from(rootDir) {
        include("mod.hjson")
    }

    from("assets/") {
        include("**")
    }
}

val deploy = tasks.register("deploy", Jar::class) {
    dependsOn(jarAndroid)
    dependsOn(tasks.jar)
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    archiveFileName.set("$modArtifactName.jar")

    from({
        listOf(
            zipTree("${layout.buildDirectory.get().asFile}/libs/${modArtifactName}Desktop.jar"),
            zipTree("${layout.buildDirectory.get().asFile}/libs/${modArtifactName}Android.jar")
        )
    })

    doLast {
        delete(
            "${layout.buildDirectory.get().asFile}/libs/${modArtifactName}Desktop.jar",
            "${layout.buildDirectory.get().asFile}/libs/${modArtifactName}Android.jar"
        )
    }
}