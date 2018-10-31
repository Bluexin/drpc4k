/*
 * Copyright (c) 2017 Arnaud 'Bluexin' Solé
 *
 * This file is part of drpc4k.
 *
 * drpc4k is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * drpc4k is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with drpc4k.  If not, see <http://www.gnu.org/licenses/>.
 */

import be.bluexin.drpc4k.jna.DiscordRichPresence
import be.bluexin.drpc4k.jna.RPCHandler
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import kotlin.random.Random

private val logger = KotlinLogging.logger { }

fun main(args: Array<String>) = runBlocking {
    if (args.isEmpty()) {
        logger.error("Missing Client ID")
        return@runBlocking
    }

    // Setting up error/disconnection callbacks
    RPCHandler.onErrored = { errorCode, message -> logger.error("$errorCode = $message") }
    RPCHandler.onDisconnected = { errorCode, message -> logger.warn("${if (errorCode != 0) "$errorCode = " else ""}$message") }

    // Connect using the client ID
    RPCHandler.connect(args[0])
    logger.info("Connecting")

    // Let's build our awesome presence
    val presence = DiscordRichPresence {
        details = "Raid: Kill Migas"
        state = "Recruiting"
        partyId = "Awesome Party ID"
        partySize = Random.nextInt(20) + 1
        partyMax = 24
        setDuration(1200L)
        smallImageKey = "ia_sakura_water"
        largeImageKey = "ia_sakura_water"
        smallImageText = "OwO smol"
        largeImageText = "OwO big"
        joinSecret = "anawesomesecret"
        spectateSecret = "anawesomesecret2"
    }

    RPCHandler.ifConnectedOrLater {
        // This will be called immediately if we are connected, or as soon as we connect
        logger.info("Logged in as ${it.username}#${it.discriminator}")
        delay(2000)
        RPCHandler.updatePresence(presence)
    }

    // "playing the game" ;p
    logger.info("Starting to sleep...")
    delay(120000)

    logger.info("Done, disconnecting")
    if (RPCHandler.connected.get()) RPCHandler.disconnect()

    // Making sure everything is done.
    RPCHandler.finishPending()
}
