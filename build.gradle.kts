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

tasks.register("DesktopBuildAndRun") {
    dependsOn(tasks.build)
    doLast {
        println("TEST!!!!!!")
    }
}
