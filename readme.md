# drpc4k

drpc4k (discord-rpc for Kotlin) is a project aiming to provide [Kotlin](https://kotlinlang.org) bindings for [discord-rpc](https://github.com/discordapp/discord-rpc)

## Current State and JNA

Only Rich Presence is available at the moment.
Currently, the only working method of connecting is trough a JNA wrapper.
I plan on making a pure Kotlin one too though.

## Rich Presence

To use Discord Rich Presence, the easiest way is to use the wrapper class [be.bluexin.drpc4k.jna.RPCHandler](src/main/kotlin/be/bluexin/drpc4k/jna/RPCHandler.kt).
An example usage can be found at [be.bluexin.drpc4k.jna.Main](src/main/kotlin/be/bluexin/drpc4k/jna/Main.kt).
