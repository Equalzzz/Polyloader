import java.net.URI

plugins {
    `java-library`
}

val libs = versionCatalogs.find("libs").get()
val version = libs.findVersion("mindustry").get().toString()
println(version)
repositories {
    mavenCentral()
    //Downloads the dependencies JAR file from Mindustry releases; does not use any real repository. Surprisingly, this is the most reliable option.
    ivy {
        url = URI("https://github.com/")
        patternLayout {
            artifact(
                (when (version) {
                    "latest" -> "/[organisation]/[module]/releases/[revision]/download/dependencies.jar" //latest stable release
                    "be" -> "/[organisation]/[module]/releases/download/master/[revision].jar" //latest commit (BE)
                    else -> "/[organisation]/[module]/releases/download/[revision]/dependencies.jar" //specific release
                })
            )
        }
        metadataSources { artifact() }

        content {
            //BE artifact version is always "latest"
            if (version == "be")
                includeVersion("Anuken", "MindustryBuilds", "latest")
            else
                includeVersion("Anuken", "Mindustry", version)
        }
    }
}

dependencies {
    compileOnly(if (version == "be") "Anuken:MindustryBuilds:latest" else "Anuken:Mindustry:$version")
}