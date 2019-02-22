import com.jfrog.bintray.gradle.BintrayExtension
import de.undercouch.gradle.tasks.download.Download
import groovy.lang.GroovyObject
import org.jetbrains.kotlin.gradle.dsl.Coroutines
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jfrog.gradle.plugin.artifactory.dsl.PublisherConfig
import kotlin.concurrent.thread

plugins {
    `java-library`
    `maven-publish`
    kotlin("jvm")
    id("de.undercouch.download")
    id("com.jfrog.bintray")
    id("com.jfrog.artifactory")
}

val branch = prop("branch") ?: System.getenv("TRAVIS_BRANCH")
?: Runtime.getRuntime().exec("git rev-parse --abbrev-ref HEAD", null, buildDir).inputStream.reader().readLines().last()
logger.info("On branch $branch")

group = "be.bluexin"
version = "$branch-".takeUnless { branch == "master" }.orEmpty() + prop("version_number") + "." + prop("build_number")
description = "Bringing Discord-RPC to Kotlin"

repositories {
    jcenter()
}

val shade by configurations.creating
val shadeInPlace by configurations.creating

configurations.compileOnly.extendsFrom(shade, shadeInPlace)
configurations.testCompileOnly.extendsFrom(shade, shadeInPlace)

dependencies {
    api(kotlin("stdlib-jdk8"))
    api("org.jetbrains.kotlinx", "kotlinx-coroutines-core")
    api("org.jetbrains.kotlinx", "kotlinx-coroutines-jdk8", prop("coroutinesVersion"))

    implementation("org.slf4j", "slf4j-api")
    implementation("io.github.microutils", "kotlin-logging", prop("kotlinLoggingVersion"))
    testRuntime("org.slf4j", "slf4j-simple", prop("slf4jVersion"))

    shade("net.java.dev.jna", "jna", prop("jnaVersion"))
    shadeInPlace(files("libs"))
    shadeInPlace(files("libsExt") {
        builtBy("expandDiscordRPC")
    })
}

tasks {
    withType<KotlinCompile> {
        kotlinOptions.jvmTarget = "1.8"
    }

    getByName("jar", Jar::class).apply {
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
    val discordDownloadFolder = "$buildDir/download/discord/$discordVersion"

    val dlDiscordRPC by creating {
        logger.info("Downloading discord-rpc version $discordVersion...")
        arrayOf("win", "linux", "osx").forEach {
            val dlCurrent by tasks.register<Download>("download-$it") {
                src("https://github.com/discordapp/discord-rpc/releases/download/v$discordVersion/discord-rpc-$it.zip")
                dest("$discordDownloadFolder/discord-rpc-$it.zip")
                overwrite(false)
            }
            outputs.files(dlCurrent.outputFiles)
            dependsOn(dlCurrent)
        }
    }

    val expandDiscordRPC by creating(Copy::class) {
        inputs.files(dlDiscordRPC.outputs.files)

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
    classifier = "sources"
}

publishing {
    publications.register("publication", MavenPublication::class) {
        from(components["java"])
        artifact(sourcesJar)
    }

    repositories {
        val mavenPassword = if (hasProp("local")) null else prop("mavenPassword")
        maven {
            val remoteURL = "https://maven.bluexin.be/repository/" + (if ((version as String).contains("SNAPSHOT")) "snapshots" else "releases")
            val localURL = "file://$buildDir/repo"
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

bintray {
    user = prop("bintrayUser")
    key = prop("bintrayApiKey")
    publish = true
    override = true
    setPublications(publication.name)
    pkg(delegateClosureOf<BintrayExtension.PackageConfig> {
        repo = "bluexin"
        name = project.name
        userOrg = "bluexin"
        websiteUrl = "https://github.com/Bluexin/drpc4k"
        githubRepo = "Bluexin/drpc4k"
        vcsUrl = "https://github.com/Bluexin/drpc4k"
        issueTrackerUrl = "https://github.com/Bluexin/drpc4k/issues"
        desc = project.description
        setLabels("kotlin", "discord")
        setLicenses("GPL-3.0")
    })
}

artifactory {
    setContextUrl("https://oss.jfrog.org")
    publish(delegateClosureOf<PublisherConfig> {
        repository(delegateClosureOf<GroovyObject> {
            val targetRepoKey = if (project.version.toString().endsWith("-SNAPSHOT")) "oss-snapshot-local" else "oss-release-local"
            setProperty("repoKey", targetRepoKey)
            setProperty("username", prop("bintrayUser"))
            setProperty("password", prop("bintrayApiKey"))
            setProperty("maven", true)
        })
        defaults(delegateClosureOf<GroovyObject> {
            invokeMethod("publications", publication.name)
        })
    })
}

// UTILS

fun hasProp(name: String): Boolean = extra.has(name)
fun prop(name: String): String? = extra.properties[name] as? String
