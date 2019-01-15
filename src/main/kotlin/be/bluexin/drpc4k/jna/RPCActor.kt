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

package be.bluexin.drpc4k.jna

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.actor
import mu.KotlinLogging
import kotlin.coroutines.CoroutineContext

/**
 * Start a new Discord RPC Actor in the current [this] [CoroutineScope].
 *
 * The newly created actor will receive messages of type [RPCInputMessage] and send [RPCOutputMessage] to the
 * specified [output].
 *
 * A typical usage looks like this :
 * ```
 * val rpcOutput = Channel<RPCOutputMessage>(capacity = Channel.UNLIMITED)
 * val rpcInput = rpcActor(rpcOutput)
 * // Connect to the client via RPC
 * rpcInput.send(RPCInputMessage.Connect(myClientKey))
 * // Update rich presence
 * rpcInput.send(RPCInputMessage.UpdatePresence(myPresence))
 * // Set up receiving of updates from the RPC actor
 * launch {
 *     for (msg in rpcOutput) with(msg) {
 *         when (this) {
 *             is RPCOutputMessage.Ready -> with(user) { logger.info("Logged in as $username#$discriminator") }
 *             is RPCOutputMessage.Disconnected -> logger.warn("Disconnected: #$errorCode $message")
 *             is RPCOutputMessage.Errored -> logger.error("Error: #$errorCode $message")
 *         }
 *     }
 * }
 * ...
 * // Disconnect from the client
 * rpcInput.close()
 * ```
 *
 * Note that because we use a [Channel.UNLIMITED] capacity channel, it is safe to use non-suspending [Channel.offer]
 * instead of the suspending [Channel.send].
 *
 * @param output Channel the RPC Actor will be sending [RPCOutputMessage] update messages to.
 * @param context additional to [CoroutineScope.coroutineContext] context of the coroutine.
 * @see CoroutineScope.actor for more technical information.
 */
@ObsoleteCoroutinesApi
@ExperimentalCoroutinesApi
fun CoroutineScope.rpcActor(output: SendChannel<RPCOutputMessage>, context: CoroutineContext = this.coroutineContext):
        SendChannel<RPCInputMessage> = actor(context = context, capacity = Channel.UNLIMITED, start = CoroutineStart.LAZY) {
    RPCActor(this, channel, output).start()
}

/**
 * Superclass for messages sent to the RPC actor.
 */
@Suppress("MemberVisibilityCanBePrivate")
sealed class RPCInputMessage {
    /**
     * Make the RPC actor connect to the client.
     *
     * @param clientId your app's client ID.
     * @param autoRegister whether Discord should register your app for automatic launch (untested! Probably broken because Java).
     * @param steamId your app's Steam ID, if any.
     * @param refreshRate the rate in milliseconds at which this handler will run callbacks and send info to discord.
     */
    class Connect(
            val clientId: String,
            val autoRegister: Boolean = false,
            val steamId: String? = null,
            val refreshRate: Long = 500L
    ) : RPCInputMessage()

    /**
     * Update the user's Rich Presence.
     * The presence will be cached if used before the app has connected, and automatically sent once ready.
     *
     * @see DiscordRichPresence for all available options.
     */
    class UpdatePresence(val presence: DiscordRichPresence) : RPCInputMessage()
}

/**
 * Superclass for messages sent by the RPC actor.
 */
@Suppress("MemberVisibilityCanBePrivate")
sealed class RPCOutputMessage {
    /**
     * Sent when the RPC actor has logged in, and is ready to be accessed.
     */
    class Ready(val user: DiscordUser) : RPCOutputMessage()

    /**
     * Sent when the RPC actor has been disconnected.
     *
     * @param errorCode the error code causing disconnection.
     * @param message the message for disconnection.
     */
    class Disconnected(val errorCode: Int, val message: String) : RPCOutputMessage()

    /**
     * Sent when the RPC actor has detected an error.
     *
     * @param errorCode the error code causing the error.
     * @param message the message for the error.
     */
    class Errored(val errorCode: Int, val message: String) : RPCOutputMessage()

    /**
     * Sent when the someone accepted a game invitation.
     *
     * @param joinSecret the game invitation secret.
     */
    class JoinGame(val joinSecret: String) : RPCOutputMessage()

    /**
     * Sent when the someone accepted a game spectating invitation.
     *
     * @param spectateSecret the game spectating secret.
     */
    class Spectate(val spectateSecret: String) : RPCOutputMessage()

    /**
     * Sent when the someone requested to join the game.
     *
     * @param user the requester.
     */
    class JoinRequest(val user: DiscordUser) : RPCOutputMessage()
}

/**
 * RPC Actor implementation.
 *
 * @param scope the scope for this actor to act in.
 * @param input the actor's input channel.
 * @param output the actor's output channel.
 */
@ExperimentalCoroutinesApi
private class RPCActor(
        private val scope: CoroutineScope,
        private val input: ReceiveChannel<RPCInputMessage>,
        private val output: SendChannel<RPCOutputMessage>) {

    private val logger = KotlinLogging.logger { }

    private var connected = false
    private var initialized = false
    private lateinit var user: DiscordUser
    private var queuedPresence: DiscordRichPresence? = null

    /**
     * Start the actor.
     */
    suspend fun start() {
        for (m in input) onReceive(m)
    }

    private suspend fun onReceive(msg: RPCInputMessage) {
        when (msg) {
            is RPCInputMessage.Connect -> with(msg) { connect(clientId, autoRegister, steamId, refreshRate) }
            is RPCInputMessage.UpdatePresence -> if (initialized) DiscordRpc.Discord_UpdatePresence(msg.presence) else queuedPresence = msg.presence
        }
    }

    /**
     * Connect the actor to the RPC Client.
     *
     * @see DiscordRpc.Discord_Initialize
     */
    private suspend fun connect(clientId: String, autoRegister: Boolean = false, steamId: String? = null, refreshRate: Long = 500L) {
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
            scope.coroutineContext.cancelChildren()
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
            scope.launch {
                output.send(RPCOutputMessage.Ready(it))
            }
        }
        onDisconnected { errorCode, message ->
            logger.warn("Disconnected: #$errorCode (${message.takeIf { message.isNotEmpty() }
                    ?: "No message provided"})")
            connected = false
            scope.launch {
                output.send(RPCOutputMessage.Disconnected(errorCode, message))
            }
        }
        onErrored { errorCode, message ->
            logger.error("Error: #$errorCode (${message.takeIf { message.isNotEmpty() } ?: "No message provided"})")
            connected = false
            scope.launch {
                output.send(RPCOutputMessage.Errored(errorCode, message))
            }
        }
        onJoinGame {
            scope.launch {
                output.send(RPCOutputMessage.JoinGame(it))
            }
        }
        onSpectateGame {
            scope.launch {
                output.send(RPCOutputMessage.Spectate(it))
            }
        }
        onJoinRequest {
            scope.launch {
                output.send(RPCOutputMessage.JoinRequest(it))
            }
        }
    }
}
