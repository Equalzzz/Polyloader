plugins {
    alias(libs.plugins.kotlin.jvm)
}

allprojects {
    group = "org.im.polyloader"
    version = "1.0-SNAPSHOT"
}

kotlin {
    jvmToolchain(libs.versions.java.get().toInt())
}

tasks.register("Run Game") {
    dependsOn(tasks.build)
    doLast {
        println("TEST!!!!!!")
    }
}