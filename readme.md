# drpc4k

drpc4k (discord-rpc for Kotlin) is a project that aims to provide [Kotlin](https://kotlinlang.org) bindings for [discord-rpc](https://github.com/discordapp/discord-rpc)

## Current state of this project

Library is in early development, lacking extensive testing.
Only [Rich Presence](https://discordapp.com/developers/docs/topics/rich-presence) is available at the moment.  
Currently, the only working method of connecting is trough a JNA wrapper.
I plan on making a pure Kotlin one too though.

## Using this library

Usage of Jitpack.io is preferred over the previous use of JCenter (Which is now read-only).
To use snapshots, replace the version tag with the commit short hash.

When using gradle (groovy or kts):
```groovy
repositories {
    maven("https://jitpack.io")
}
```

Then add the dependency:
```groovy
dependencies {
    /* project dependencies */
    compile("com.github.Bluexin:drpc4k:v0.9")
}
```

Maven :
```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>
```

And the dependency:
```xml
<dependency>
    <groupId>com.github.defvs</groupId>
    <artifactId>drpc4k</artifactId>
    <version>Tag</version>
</dependency>
```
A list of versions can be found [on Jitpack]([https://bintray.com/bluexin/bluexin/drpc4k](https://jitpack.io/#Bluexin/drpc4k)).

You can also directly download it from [Bintray](https://bintray.com/bluexin/bluexin/drpc4k/_latestVersion).

## Rich Presence

To use Discord Rich Presence, the easiest way is to use the Actor wrapper [be.bluexin.drpc4k.jna.RPCActor](src/main/kotlin/be/bluexin/drpc4k/jna/RPCActor.kt).
It will handle everything using lightweight Kotlin Coroutines.
An example usage can be found at [src/test/kotlin/JnaExampleActor.kt](src/test/kotlin/JnaExampleActor.kt).
