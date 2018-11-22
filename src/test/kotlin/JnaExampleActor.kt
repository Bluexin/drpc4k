/*
 * Copyright (c) 2018 Arnaud 'Bluexin' Solé
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
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import kotlin.random.Random

private val logger = KotlinLogging.logger { }

@ObsoleteCoroutinesApi
@ExperimentalCoroutinesApi
fun main(args: Array<String>) = runBlocking {
    if (args.isEmpty()) {
        logger.error("Missing Client ID")
        return@runBlocking
    }

    val rpc = rpcActor()

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

    rpc.send(Connect(args[0]))
    rpc.send(UpdatePresence(presence))
    rpc.send(SetOnReady {
        logger.info("Logged in as ${it.username}#${it.discriminator}")
    })
    rpc.send(SetOnDisconnected { errorCode, message ->
        logger.warn("Disconnected: #$errorCode (${message.takeIf { message.isNotEmpty() }
                ?: "No message provided"})")
    })
    rpc.send(SetOnErrored { errorCode, message ->
        logger.error("Error: #$errorCode (${message.takeIf { message.isNotEmpty() } ?: "No message provided"})")
    })
    delay(500)
    delay(10000)

    rpc.close()
    delay(500)
}