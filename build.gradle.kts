plugins {
    `java`
}

group = "de.opalium"
version = "0.2.0"
description = "LuckySky-Opalium core controls for Opalium Haven"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
    withSourcesJar()
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.1-R0.1-SNAPSHOT")
    // TODO: später LuckPerms, Multiverse etc. als compileOnly hinzufügen
}

tasks.processResources {
    filesMatching("plugin.yml") {
        expand(
            "version" to project.version,
            "description" to project.description
        )
    }
}

tasks.jar {
    archiveBaseName.set("LuckySky-Opalium")
}
