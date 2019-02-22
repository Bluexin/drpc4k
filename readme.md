# drpc4k [![Build Status](https://travis-ci.org/Bluexin/drpc4k.svg?branch=master)](https://travis-ci.org/Bluexin/drpc4k) [ ![Bintray](https://api.bintray.com/packages/bluexin/bluexin/drpc4k/images/download.svg) ](https://bintray.com/bluexin/bluexin/drpc4k/_latestVersion)

drpc4k (discord-rpc for Kotlin) is a project that aims to provide [Kotlin](https://kotlinlang.org) bindings for [discord-rpc](https://github.com/discordapp/discord-rpc)

## Current state of this project

Library is in early development, lacking extensive testing.
Only [Rich Presence](https://discordapp.com/developers/docs/topics/rich-presence) is available at the moment.  
Currently, the only working method of connecting is trough a JNA wrapper.
I plan on making a pure Kotlin one too though.

## Using this library

Make sure you enable the `jcenter` repository for releases.
For snapshots, please add [https://oss.jfrog.org/simple/libs-snapshot](https://oss.jfrog.org/simple/libs-snapshot).

When using gradle (groovy or kts):
```groovy
repositories {
    jcenter()
    /* for snapshots */
	maven("https://oss.jfrog.org/simple/libs-snapshot")
}
```

Then add the dependency:
```groovy
dependencies {
    /* project dependencies */
    compile("be.bluexin:drpc4k:0.9")
}
```
Maven :
```xml
<dependency>
  <groupId>be.bluexin</groupId>
  <artifactId>drpc4k</artifactId>
  <version>0.9</version>
  <type>pom</type>
</dependency>
```
A list of versions can be found [on bintray](https://bintray.com/bluexin/bluexin/drpc4k).
Snapshot versions can be found on [OJO](https://oss.jfrog.org/artifactory/webapp/#/artifacts/browse/tree/General/oss-snapshot-local/be/bluexin/drpc4k).

You can also directly download it from [Bintray](https://bintray.com/bluexin/bluexin/drpc4k/_latestVersion).

## Rich Presence

To use Discord Rich Presence, the easiest way is to use the Actor wrapper [be.bluexin.drpc4k.jna.RPCActor](src/main/kotlin/be/bluexin/drpc4k/jna/RPCActor.kt).
It will handle everything using lightweight Kotlin Coroutines.
An example usage can be found at [src/test/kotlin/JnaExampleActor.kt](src/test/kotlin/JnaExampleActor.kt).
