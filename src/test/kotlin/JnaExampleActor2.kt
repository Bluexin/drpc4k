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

import be.bluexin.drpc4k.jna.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import kotlin.random.Random

private val logger = KotlinLogging.logger { }

@ExperimentalCoroutinesApi
fun main(args: Array<String>) = runBlocking {
    if (args.isEmpty()) {
        logger.error("Missing Client ID")
        return@runBlocking
    }

    val rpc = rpcActor2()

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

    /*
    These seem to work fine without using suspending calls on send, should be thanks to unlimited mailbox
     */
    rpc.connect(args[0])
    rpc.updatePresence(presence)
    rpc.setOnReady {
        logger.info("Logged in as ${it.username}#${it.discriminator}")
    }
    rpc.setOnDisconnected { errorCode, message ->
        logger.warn("Disconnected: #$errorCode (${message.takeIf { message.isNotEmpty() }
                ?: "No message provided"})")
    }
    rpc.setOnErrored { errorCode, message ->
        logger.error("Error: #$errorCode (${message.takeIf { message.isNotEmpty() } ?: "No message provided"})")
    }
    logger.info("Is connected? ${rpc.isConnected().await()}")
    delay(500)
    delay(10000)
    logger.info("Is connected? ${rpc.isConnected().await()}")

    rpc.stop()
    delay(500)
}
