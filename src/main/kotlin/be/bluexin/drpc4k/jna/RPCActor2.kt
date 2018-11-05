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
import kotlinx.coroutines.channels.actor
import mu.KotlinLogging

@ExperimentalCoroutinesApi
fun CoroutineScope.rpcActor2() = RPCActor2()(this)

@ExperimentalCoroutinesApi
class RPCActor2 {
    private val logger = KotlinLogging.logger { }

    private var onReady: ReadyCallback = {}
    private var onDisconnected: DisconnectedCallback = { _, _ -> }
    private var onErrored: ErroredCallback = { _, _ -> }
    private var onJoinGame: JoinGameCallback = { }
    private var onSpectateGame: SpectateCallback = { }
    private var onJoinRequest: JoinRequestCallback = { }

    private var connected = false
    private var initialized = false
    private lateinit var user: DiscordUser
    private var queuedPresence: DiscordRichPresence? = null

    private lateinit var channel: Channel<RPCMessage>

    operator fun invoke(scope: CoroutineScope): RPCActor2 {
        scope.actor<RPCMessage>(context = Dispatchers.Unconfined, capacity = Channel.UNLIMITED) {
            this@RPCActor2.channel = this@actor.channel
            for (msg in this@RPCActor2.channel) this@RPCActor2.onReceive(msg)
        }
        return this
    }

    private suspend fun onReceive(msg: RPCMessage) = msg.body(this)

    suspend fun actS(body: suspend RPCActor2.() -> Unit) = channel.send(RPCMessage(body))

    fun act(body: suspend RPCActor2.() -> Unit) = channel.offer(RPCMessage(body))

    fun updatePresence(presence: DiscordRichPresence) = this.act {
        if (initialized) DiscordRpc.Discord_UpdatePresence(presence) else queuedPresence = presence
    }

    suspend fun updatePresenceS(presence: DiscordRichPresence) = this.actS {
        if (initialized) DiscordRpc.Discord_UpdatePresence(presence) else queuedPresence = presence
    }

    fun setOnReady(callback: ReadyCallback) = this.act {
        onReady = callback
    }

    suspend fun setOnReadyS(callback: ReadyCallback) = this.actS {
        onReady = callback
    }

    fun setOnDisconnected(callback: DisconnectedCallback) = this.act {
        onDisconnected = callback
    }

    suspend fun setOnDisconnectedS(callback: DisconnectedCallback) = this.actS {
        onDisconnected = callback
    }

    fun setOnErrored(callback: ErroredCallback) = this.act {
        onErrored = callback
    }

    suspend fun setOnErroredS(callback: ErroredCallback) = this.actS {
        onErrored = callback
    }

    fun setOnJoinGame(callback: JoinGameCallback) = this.act {
        onJoinGame = callback
    }

    suspend fun setOnJoinGameS(callback: JoinGameCallback) = this.actS {
        onJoinGame = callback
    }

    fun setOnSpectate(callback: SpectateCallback) = this.act {
        onSpectateGame = callback
    }

    suspend fun setOnSpectateS(callback: SpectateCallback) = this.actS {
        onSpectateGame = callback
    }

    fun setOnJoinRequest(callback: JoinRequestCallback) = this.act {
        onJoinRequest = callback
    }

    suspend fun setOnJoinRequestS(callback: JoinRequestCallback) = this.actS {
        onJoinRequest = callback
    }

    fun isConnected(): Deferred<Boolean> = CompletableDeferred<Boolean>().also {
        this.act { it.complete(this.connected) }
    }

    suspend fun isConnectedS(): Deferred<Boolean> = CompletableDeferred<Boolean>().also {
        this.actS { it.complete(this.connected) }
    }

    fun isInitialized(): Deferred<Boolean> = CompletableDeferred<Boolean>().also {
        this.act { it.complete(this.initialized) }
    }

    suspend fun isInitializedS(): Deferred<Boolean> = CompletableDeferred<Boolean>().also {
        this.actS { it.complete(this.initialized) }
    }

    fun connect(
            clientId: String,
            autoRegister: Boolean = false,
            steamId: String? = null,
            refreshRate: Long = 500L
    ) = this.act {
        this.start(clientId, autoRegister, steamId, refreshRate)
    }

    suspend fun connectS(
            clientId: String,
            autoRegister: Boolean = false,
            steamId: String? = null,
            refreshRate: Long = 500L
    ) = this.actS {
        this.start(clientId, autoRegister, steamId, refreshRate)
    }

    fun stop(cause: Throwable? = null) = this.channel.close(cause)

    private suspend fun start(clientId: String, autoRegister: Boolean = false, steamId: String? = null, refreshRate: Long = 500L) {
        try {
            DiscordRpc.Discord_Initialize(clientId, handlers, autoRegister, steamId)
            initialized = true
            if (queuedPresence != null) {
                DiscordRpc.Discord_UpdatePresence(queuedPresence!!)
                queuedPresence = null
            }
            while (!channel.isClosedForReceive) {
                var m = channel.poll()
                while (m != null) {
                    onReceive(m)
                    m = channel.poll()
                }
                DiscordRpc.Discord_RunCallbacks()
                delay(refreshRate)
            }
        } catch (e: CancellationException) {
        } catch (e: Throwable) {
            onErrored(-1, "Unknown error caused by: ${e.message}")
        } finally {
            onDisconnected(0, "Discord RPC Thread closed.")
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
            this@RPCActor2.onReady(it)
        }
        onDisconnected { errorCode, message ->
            logger.warn("Disconnected: #$errorCode (${message.takeIf { message.isNotEmpty() }
                    ?: "No message provided"})")
            connected = false
            this@RPCActor2.onDisconnected(errorCode, message)
        }
        onErrored { errorCode, message ->
            logger.error("Error: #$errorCode (${message.takeIf { message.isNotEmpty() } ?: "No message provided"})")
            connected = false
            this@RPCActor2.onErrored(errorCode, message)
        }
        onJoinGame { joinSecret -> this@RPCActor2.onJoinGame(joinSecret) }
        onSpectateGame { spectateSecret -> this@RPCActor2.onSpectateGame(spectateSecret) }
        onJoinRequest { request -> this@RPCActor2.onJoinRequest(request) }
    }

    private class RPCMessage(val body: suspend RPCActor2.() -> Unit)
}
