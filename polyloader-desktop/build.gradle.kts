plugins {
    kotlin("jvm")
    id("common-convention-plugin")
    alias(libs.plugins.shadow)
    alias(libs.plugins.serialization)
}

dependencies {
    implementation(libs.serialization)
    api(libs.mixin)
}

sourceSets.main {
    kotlin.srcDir("src")
}

tasks.shadowJar {
    archiveClassifier.set("")
    archiveBaseName.set(project.name)
    archiveVersion.set("")
    manifest {
        attributes["Main-Class"] = "org.im.polyloader.Polyloader"
    }
}

tasks.jar {
    dependsOn(tasks.shadowJar)
}

// Custom task for copying resulting shadow jar in some location FOR DEBUGGING ONLY
val copyJar = tasks.register<Copy>("copyJar") {
    dependsOn(tasks.shadowJar)
    from(tasks.shadowJar)
    val dir = properties["polyloader-directory"] as String + "/jre"
    println("Copying ${project.name} to $dir")
    into(dir)
}

tasks.register<Exec>("buildAndRun") {
    dependsOn("copyJar")
    val dir = properties["polyloader-directory"] as String + "/jre"
    val cmd = "${dir}/bin/java.exe -jar ../${project.name}.jar"
    workingDir = file("${dir}/bin")
    commandLine(cmd.split(" "))
}