import de.undercouch.gradle.tasks.download.Download
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `java-library`
    `maven-publish`
    kotlin("jvm")
    id("de.undercouch.download")
}

val branch = prop("branch")
    ?: System.getenv("TRAVIS_BRANCH")
    ?: System.getenv("GIT_BRANCH")
    ?: Runtime.getRuntime().exec(
        "git rev-parse --abbrev-ref HEAD", null, layout.buildDirectory.asFile.get()
    ).inputStream.reader().readLines().last()

logger.info("On branch $branch")

group = "be.bluexin"
version = "$branch-".takeUnless { branch == "master" }.orEmpty() + prop("version_number") + "." + prop("build_number")
description = "Bringing Discord-RPC to Kotlin"

repositories {
    mavenCentral()
}

val shade by configurations.creating
val shadeInPlace by configurations.creating

configurations.implementation.configure {
    extendsFrom(shade, shadeInPlace)
}
configurations.testImplementation.configure {
    extendsFrom(shade, shadeInPlace)
}

dependencies {
    api(kotlin("stdlib-jdk8"))
    api("org.jetbrains.kotlinx", "kotlinx-coroutines-core", prop("coroutinesVersion"))
    api("org.jetbrains.kotlinx", "kotlinx-coroutines-jdk8", prop("coroutinesVersion"))

    implementation("org.slf4j", "slf4j-api", prop("slf4jVersion"))
    implementation("io.github.microutils", "kotlin-logging-jvm", prop("kotlinLoggingVersion"))
    testRuntimeOnly("org.slf4j", "slf4j-simple", prop("slf4jVersion"))

    shade("net.java.dev.jna", "jna", prop("jnaVersion"))
    shadeInPlace(files("libs"))
    shadeInPlace(files("libsExt") {
        builtBy("expandDiscordRPC")
    })
}

tasks {
    withType<KotlinCompile>().configureEach {
        kotlinOptions.jvmTarget = "1.8"
    }
    withType<JavaCompile>().configureEach {
        targetCompatibility = "1.8"
    }

    named<Jar>("jar") {
        for (dep in shade) {
            from(zipTree(dep)) {
                exclude("META-INF", "META-INF/**")
            }
        }
        for (dep in shadeInPlace) {
            from(dep)
        }
    }

    // DISCORDRPC

    val discordVersion = prop("discord_rpc_version")
    val discordDownloadFolder = "${layout.buildDirectory.asFile.get()}/download/discord/$discordVersion"

    val downloadTasks = arrayOf("win", "linux", "osx").map {
        register<Download>("download-$it") {
            src("https://github.com/discordapp/discord-rpc/releases/download/v$discordVersion/discord-rpc-$it.zip")
            dest("$discordDownloadFolder/discord-rpc-$it.zip")
            overwrite(false)
        }
    }

    val dlDiscordRPC by registering {
        logger.info("Downloading discord-rpc version $discordVersion...")
        downloadTasks.forEach {
            outputs.files(it.get().outputFiles)
            dependsOn(it)
        }
    }

    val expandDiscordRPC by creating(Copy::class) {
        inputs.files(dlDiscordRPC.get().outputs.files)

        logger.info("Expanding discord-rpc...")
        val outputDir = file("libsExt")
        outputDir.delete()

        val extensions = arrayOf("dll", "so", "dylib")
        val map = mapOf(
            "win64-dynamic" to "win32-x86-64",
            "win32-dynamic" to "win32-x86",
            "linux-dynamic" to "linux-x86-64",
            "osx-dynamic" to "darwin"
        )

        from(inputs.files.map { logger.info("Zip: $it"); zipTree(it) })
        eachFile {
            var accepted = false
            val split = name.split('.')
            if (split.any() && split.last() in extensions) for (entry in map) {
                logger.info("3 $relativePath")
                if (relativePath.pathString.contains(entry.key)) {
                    logger.info("4 $relativePath")
                    relativePath = RelativePath(true, entry.value, name)
                    logger.info("5 $relativePath")
                    accepted = true
                    break
                }
            }
            if (!accepted) exclude()
        }
        into(outputDir)
        includeEmptyDirs = false
    }

}

// PUBLISHING

val sourcesJar by tasks.creating(Jar::class) {
    from(sourceSets["main"].allSource)
    from(sourceSets["test"].allSource)
    archiveClassifier.set("sources")
}

publishing {
    publications.register("publication", MavenPublication::class) {
        from(components["java"])
        artifact(sourcesJar)
    }

    repositories {
        val mavenPassword = if (hasProp("local")) null else prop("mavenPassword")
        maven {
            val remoteURL =
                "https://maven.bluexin.be/repository/" + (if ((version as String).contains("SNAPSHOT")) "snapshots" else "releases")
            val localURL = "file://${layout.buildDirectory.asFile.get()}/repo"
            url = uri(if (mavenPassword != null) remoteURL else localURL)
            if (mavenPassword != null) {
                credentials(PasswordCredentials::class.java) {
                    username = prop("mavenUser")
                    password = mavenPassword
                }
            }
        }
    }
}
val publication by publishing.publications

// UTILS

fun hasProp(name: String): Boolean = extra.has(name)
fun prop(name: String): String? = extra.properties[name] as? String
