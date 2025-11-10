import org.gradle.api.GradleException
import org.gradle.language.jvm.tasks.ProcessResources

plugins {
    `java`
}

group = "de.opalium"
version = "0.1.0-SNAPSHOT"
description = "Paper plugin for LuckySky (start/stop/reset, safe platform, wipes, wither, GUI)"

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
    testImplementation("io.papermc.paper:paper-api:1.21.1-R0.1-SNAPSHOT")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.2")
    testImplementation("org.mockito:mockito-core:5.12.0")
    testImplementation("org.mockito:mockito-inline:5.2.0")
}

tasks.register("validateResources") {
    doLast {
        val resourcesDir = project.layout.projectDirectory.dir("src/main/resources").asFile
        if (!resourcesDir.exists()) {
            return@doLast
        }

        val forbiddenEntries = resourcesDir.walkTopDown()
            .filter { file ->
                if (file == resourcesDir) {
                    false
                } else {
                    val relativePath = file.relativeTo(resourcesDir).invariantSeparatorsPath
                    relativePath == "src" || relativePath.startsWith("src/")
                }
            }
            .toList()

        if (forbiddenEntries.isNotEmpty()) {
            val details = forbiddenEntries.joinToString(separator = ", ") { entry ->
                entry.relativeTo(resourcesDir).invariantSeparatorsPath
            }
            throw GradleException("Forbidden resources detected under src/main/resources: $details")
        }
    }
}

tasks.named<ProcessResources>("processResources") {
    dependsOn("validateResources")

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

tasks.test {
    useJUnitPlatform()
}
