# drpc4k

drpc4k (discord-rpc for Kotlin) is a project aiming to provide [Kotlin](https://kotlinlang.org) bindings for [discord-rpc](https://github.com/discordapp/discord-rpc)

## Current State of this project

Library in early development, lacking extensive testing.
Only [Rich Presence](https://discordapp.com/developers/docs/topics/rich-presence) is available at the moment.
Currently, the only working method of connecting is trough a JNA wrapper.
I plan on making a pure Kotlin one too though.

## Using this lib

Either build from sources and add the lib the way you'd do for any other lib, or using Maven :

Add the url `http://maven.bluexin.be/repository/snapshots/` to your repositories (or `/releases/` for release version once one comes out).

Ex, using gradle :
```
repositories {
    /* other repos, like jcenter() or mavenCentral() */
    maven {
        url = "http://maven.bluexin.be/repository/releases/" // For releases
        url = "http://maven.bluexin.be/repository/snapshots/" // For snapshots
    }
}
```

Then add the actual maven dependency :
```
dependencies {
    /* project dependencies */
    compile "be.bluexin:drpc4k:<version>-SNAPSHOT" // Replace <version> with appropriate version number, remove SNAPSHOT once releases come out.
}
```
(current latest version number is 0.3)

## Rich Presence

To use Discord Rich Presence, the easiest way is to use the wrapper class [be.bluexin.drpc4k.jna.RPCHandler](src/main/kotlin/be/bluexin/drpc4k/jna/RPCHandler.kt).
It will handle managing callbacks, updating, ... in a lightweight Kotlin Coroutine.
An example usage can be found at [be.bluexin.drpc4k.jna.Main](src/test/kotlin/JnaExample.kt).
