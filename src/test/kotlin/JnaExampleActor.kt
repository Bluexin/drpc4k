/*
 * Copyright (c) 2019 Arnaud 'Bluexin' Solé
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
import be.bluexin.drpc4k.jna.RPCInputMessage
import be.bluexin.drpc4k.jna.RPCOutputMessage
import be.bluexin.drpc4k.jna.rpcActor
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
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

    val rpcOutput = Channel<RPCOutputMessage>(capacity = Channel.UNLIMITED)
    val rpcInput = rpcActor(rpcOutput)

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

    launch {
        for (msg in rpcOutput) with(msg) {
            when (this) {
                is RPCOutputMessage.Ready -> with(user) { logger.info("Logged in as $username#$discriminator") }
                is RPCOutputMessage.Disconnected -> logger.warn("Disconnected: #$errorCode (${message.takeIf { message.isNotEmpty() }
                        ?: "No message provided"})")
                is RPCOutputMessage.Errored -> logger.error("Error: #$errorCode (${message.takeIf { message.isNotEmpty() }
                        ?: "No message provided"})")
            }
        }
    }

    rpcInput.send(RPCInputMessage.Connect(args[0]))
    rpcInput.send(RPCInputMessage.UpdatePresence(presence))

    delay(10000)

    rpcInput.close()
    delay(500)
}
