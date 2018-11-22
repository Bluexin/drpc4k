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

package be.bluexin.drpc4k.jna

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.actor
import mu.KotlinLogging
import kotlin.coroutines.CoroutineContext

@ObsoleteCoroutinesApi
@ExperimentalCoroutinesApi
fun CoroutineScope.rpcActor3(output: SendChannel<RPCOutputMessage>, context: CoroutineContext = Dispatchers.Unconfined):
        SendChannel<RPCInputMessage> = actor(context = context, capacity = Channel.UNLIMITED) {
    RPCActor3(channel, output)()
}

sealed class RPCInputMessage {
    class Connect(
            val clientId: String,
            val autoRegister: Boolean = false,
            val steamId: String? = null,
            val refreshRate: Long = 500L
    ) : RPCInputMessage()

    class UpdatePresence(val presence: DiscordRichPresence) : RPCInputMessage()
}

sealed class RPCOutputMessage {
    class Ready(val user: DiscordUser) : RPCOutputMessage()
    class Disconnected(val errorCode: Int, val message: String) : RPCOutputMessage()
    class Errored(val errorCode: Int, val message: String) : RPCOutputMessage()
    class JoinGame(val joinSecret: String) : RPCOutputMessage()
    class Spectate(val spectateSecret: String) : RPCOutputMessage()
    class JoinRequest(val user: DiscordUser) : RPCOutputMessage()
}

@ExperimentalCoroutinesApi
private class RPCActor3(
        private val input: ReceiveChannel<RPCInputMessage>,
        private val output: SendChannel<RPCOutputMessage>) {

    private val logger = KotlinLogging.logger { }

    private var connected = false
    private var initialized = false
    private lateinit var user: DiscordUser
    private var queuedPresence: DiscordRichPresence? = null

    suspend operator fun invoke() {
        for (m in input) onReceive(m)
    }

    private suspend fun onReceive(msg: RPCInputMessage) {
        when (msg) {
            is RPCInputMessage.Connect -> with(msg) { start(clientId, autoRegister, steamId, refreshRate) }
            is RPCInputMessage.UpdatePresence -> if (initialized) DiscordRpc.Discord_UpdatePresence(msg.presence) else queuedPresence = msg.presence
        }
    }

    private suspend fun start(clientId: String, autoRegister: Boolean = false, steamId: String? = null, refreshRate: Long = 500L) {
        try {
            DiscordRpc.Discord_Initialize(clientId, handlers, autoRegister, steamId)
            initialized = true
            if (queuedPresence != null) {
                DiscordRpc.Discord_UpdatePresence(queuedPresence!!)
                queuedPresence = null
            }
            while (!input.isClosedForReceive) {
                var m = input.poll()
                while (m != null) {
                    onReceive(m)
                    m = input.poll()
                }
                DiscordRpc.Discord_RunCallbacks()
                delay(refreshRate)
            }
        } catch (e: CancellationException) {
        } catch (e: Throwable) {
            output.send(RPCOutputMessage.Errored(-1, "Unknown error caused by: ${e.message}"))
        } finally {
            output.send(RPCOutputMessage.Disconnected(0, "Discord RPC Thread closed."))
            output.close()
            connected = false
            try {
                DiscordRpc.Discord_Shutdown()
            } catch (e: Throwable) {
            }
        }
    }

    private val handlers = DiscordEventHandlers {
        onReady {
            user = it
            connected = true
            GlobalScope.launch {
                output.send(RPCOutputMessage.Ready(it))
            }
        }
        onDisconnected { errorCode, message ->
            logger.warn("Disconnected: #$errorCode (${message.takeIf { message.isNotEmpty() }
                    ?: "No message provided"})")
            connected = false
            GlobalScope.launch {
                output.send(RPCOutputMessage.Disconnected(errorCode, message))
            }
        }
        onErrored { errorCode, message ->
            logger.error("Error: #$errorCode (${message.takeIf { message.isNotEmpty() } ?: "No message provided"})")
            connected = false
            GlobalScope.launch {
                output.send(RPCOutputMessage.Errored(errorCode, message))
            }
        }
        onJoinGame {
            GlobalScope.launch {
                output.send(RPCOutputMessage.JoinGame(it))
            }
        }
        onSpectateGame {
            GlobalScope.launch {
                output.send(RPCOutputMessage.Spectate(it))
            }
        }
        onJoinRequest {
            GlobalScope.launch {
                output.send(RPCOutputMessage.JoinRequest(it))
            }
        }
    }
}