pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        google()
    }
}
dependencyResolutionManagement {
    @Suppress("UnstableApiUsage")
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
        maven("https://spongepowered.org")

    }
}

plugins {
    // Auto jdk installation if I choose another java version for some reason
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0" apply false
}

rootProject.name = "polyloader"

include("polyloader-desktop")