import org.gradle.api.tasks.bundling.Jar
import kotlin.concurrent.thread
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.io.InputStream
import org.jetbrains.kotlin.gradle.dsl.Coroutines

plugins {
    `java-library`
    `maven-publish`
    kotlin("jvm") version "1.2.61"
//    id("net.saliman.properties") version "1.4.6"
}

fun gitBranch() =
        if (System.getenv("TRAVIS_BRANCH") != null) System.getenv("TRAVIS_BRANCH")!!
        else {
            val r = "git rev-parse --abbrev-ref HEAD".execute(rootDir.absolutePath)
            r.lineSequence().last()
        }

val branch = gitBranch()
logger.info("On branch $branch")

group = "be.bluexin"
version = "${if (branch == "master") "" else "$branch-"}${prop("version_number")}.${prop("build_number")}"

repositories {
    jcenter()
}

configurations {
    arrayOf(
            "shade",
            "shadeInPlace"
    ).forEach {
        create(it) {
            get("compileOnly").extendsFrom(this)
            get("testCompileOnly").extendsFrom(this)
        }
    }
}

dependencies {
    api(kotlin("stdlib-jdk8"))
    api("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    api("org.jetbrains.kotlinx:kotlinx-coroutines-core:0.19.3")
    api("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:0.19.3")
    api("org.jetbrains.kotlinx:kotlinx-coroutines-nio:0.19.3")

    implementation("org.slf4j:slf4j-api:1.7.25")
    implementation("io.github.microutils:kotlin-logging:1.6.10")
    implementation("org.slf4j:slf4j-simple:1.7.25")

    "shade"("net.java.dev.jna:jna:4.5.0")
    "shadeInPlace"(files("libs"))
    "shadeInPlace"(files("libsExt") {
        builtBy("expandDiscordRPC")
    })
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        jvmTarget = "1.8"
        languageVersion = "1.2"
        apiVersion = "1.2"
    }
}

kotlin {
    experimental.coroutines = Coroutines.ENABLE
}

val sourceJar by tasks.registering(Jar::class) {
    from(sourceSets["main"].allSource)
    from(sourceSets["test"].allSource)
    classifier = "sources"
}

tasks.withType<Jar> {
    for (dep in configurations["shade"]) {
        from(project.zipTree(dep)) {
            exclude("META-INF", "META-INF/**")
        }
    }
    for (dep in configurations["shadeInPlace"]) {
        from(dep)
    }
}

publishing {
    publications {
        register("drpc4k", MavenPublication::class.java) {
            from(components["java"])
            artifact(sourceJar.get())
        }
    }

    repositories {
        val remote = System.getenv("REPO_PWD") != null
        maven {
            val remoteURL = "https://maven.bluexin.be/repository/" + (if ((version as String).contains("SNAPSHOT")) "snapshots" else "releases")
            val localURL = "file://$buildDir/repo"
            url = uri(if (remote) remoteURL else localURL)
            if (remote) {
                credentials(PasswordCredentials::class.java) {
                    username = "CI"
                    password = System.getenv("REPO_PWD")
                }
            }
        }
    }
}

fun downloadOne(url: String, filename: String) {
    file("tmp").mkdir()
    val connection = uri(url).toURL().openConnection()
    connection.inputStream.use {
        file("tmp/$filename").outputStream().use { output ->
            var r = it.read()
            while (r >= 0) {
                output.write(r)
                r = it.read()
            }
        }
    }
}

val dlDiscordRPC by tasks.registering {
    logger.info("Downloading discord-rpc v${prop("discord_rpc_version")}...")
    arrayOf("win", "linux", "osx").forEach {
        downloadOne("https://github.com/discordapp/discord-rpc/releases/download/v${prop("discord_rpc_version")}/discord-rpc-$it.zip", "discord-rpc-$it.zip")
    }
}

val expandDiscordRPC by tasks.registering(Copy::class) {
    dependsOn += dlDiscordRPC

    logger.info("Expanding discord-rpc...")
    val outputDir = file("libsExt")
    outputDir.delete()
    logger.warn("Output: $outputDir")

    val extensions = arrayOf("dll", "so", "dylib")
    val map = mapOf(
            "win64-dynamic" to "win32-x86-64",
            "win32-dynamic" to "win32-x86",
            "linux-dynamic" to "linux-x86-64",
            "osx-dynamic" to "darwin"
    )

    val tmp = file("tmp")

    from(fileTree(tmp).map { logger.info("Zip: $it"); zipTree(it) })
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

fun String.execute(wd: String? = null, ignoreExitCode: Boolean = false): String =
        split(" ").execute(wd, ignoreExitCode)

fun List<String>.execute(wd: String? = null, ignoreExitCode: Boolean = false): String {
    val process = ProcessBuilder(this)
            .also { pb -> wd?.let { pb.directory(File(it)) } }
            .start()
    var result = ""
    val errReader = thread { process.errorStream.bufferedReader().forEachLine { logger.info(it) } }
    val outReader = thread {
        process.inputStream.bufferedReader().forEachLine { line ->
            logger.info(line)
            result += line
        }
    }
    process.waitFor()
    outReader.join()
    errReader.join()
    if (process.exitValue() != 0 && !ignoreExitCode) error("Non-zero exit status for `$this`")
    return result
}

fun hasProp(name: String): Boolean = extra.has(name)

fun prop(name: String): String =
        extra.properties[name] as? String
                ?: error("Property `$name` is not defined in gradle.properties")