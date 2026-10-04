plugins {
    kotlin("jvm")
    id("common-convention-plugin")
    alias(libs.plugins.shadow) // for shadowJar task
    alias(libs.plugins.serialization) // for automatic 'serializer()' code gen
}

dependencies {
    implementation(libs.serialization) // json manipulation

    // Mixin dependencies
    api(libs.mixin) // actually don't know if it should be api or implementation... todo find out
    implementation(libs.log4j.api)
    runtimeOnly(libs.log4j.core)
    implementation(libs.guava.jre)
}

// this make folder trees shorter
sourceSets.main {
    // In /src/ lies the code - you can read that
    kotlin.srcDir("src")
    // In /res/ lies service declaration overloads, so ServiceLoader could find them
    resources.srcDir("res")
}

// this thing bundles every dependency inside a single jar file, so called 'shadowJar'
tasks.shadowJar {
    archiveClassifier.set("")
    archiveBaseName.set(project.name)
    archiveVersion.set("")
    manifest {
        attributes["Main-Class"] = "org.im.polyloader.Polyloader"
    }
}

// I don't know if it's necessary
// TODO: figure out what this does
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

// Custom task for quickly testing if desktop project even runs
tasks.register<Exec>("buildAndRun") {
    dependsOn(copyJar)
    val dir = properties["polyloader-directory"] as String + "/jre"
    val cmd = "${dir}/bin/java -jar ../${project.name}.jar"
    workingDir = file("${dir}/bin")
    commandLine(cmd.split(" "))
}