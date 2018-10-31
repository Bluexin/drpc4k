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

package be.bluexin.drpc4k.jna

import be.bluexin.drpc4k.jna.RPCHandler.onDisconnected
import be.bluexin.drpc4k.jna.RPCHandler.onReady
import kotlinx.coroutines.*
import mu.KotlinLogging
import java.util.concurrent.atomic.AtomicBoolean

// TODO: Move all @Volatile & java Atomic usages to kotlinx.atomicfu

/**
 * Handles the Discord Rich Presence Connection.
 * Replace the callbacks ([onReady], [onDisconnected], ...) with your own.
 * Please note that all callbacks will be run on the RPC Thread!
 *
 * @author Bluexin
 */
@Suppress("MemberVisibilityCanPrivate", "unused")
object RPCHandler {
    private val logger = KotlinLogging.logger {}

    /**
     * Called when Discord Rich Presence is ready.
     */
    @Volatile
    var onReady: (user: DiscordUser) -> Unit = {}

    /**
     * Called when Discord Rich Presence gets disconnected.
     */
    @Volatile
    var onDisconnected: (errorCode: Int, message: String) -> Unit = { _, _ -> }

    /**
     * Called when Discord Rich Presence gets an error.
     */
    @Volatile
    var onErrored: (errorCode: Int, message: String) -> Unit = { _, _ -> }

    /**
     * Called when the client clicks to join.
     */
    @Volatile
    var onJoinGame: (joinSecret: String) -> Unit = { _ -> }

    /**
     * Called when the client clicks to spectate.
     */
    @Volatile
    var onSpectateGame: (spectateSecret: String) -> Unit = { _ -> }

    /**
     * Called when Discord Rich Presence receives a join request.
     */
    @Volatile
    var onJoinRequest: (request: DiscordUser) -> Unit = { _ -> }

    /**
     * Tries to connect the Discord Rich Presence Connection asynchronously.
     * If already connected (or connecting), the connection will be reset.
     *
     * @param[clientId] your app's public ID, client_id, application_id, whatever you wanna call it
     * @param[autoRegister] whether Discord should register your app for automatic launch (untested! Probably broken because Java)
     * @param[steamId] your app's Steam ID, if any
     * @param[refreshRate] the rate at which this handler will run callbacks and send info to discord
     */
    suspend fun connect(clientId: String, autoRegister: Boolean = false, steamId: String? = null, refreshRate: Long = 500L) {
        if (connected.get() || runner != null) {
            logger.info("Disconnecting")
            disconnect()
            finishPending()
        }

        runner = coroutineScope {
            launch {
                try {
                    DiscordRpc.Discord_Initialize(clientId, handlers, autoRegister, steamId)
                    while (isActive) {
                        DiscordRpc.Discord_RunCallbacks()
                        delay(refreshRate)
                    }
                } catch (e: CancellationException) {
                    onDisconnected(0, "Discord RPC Thread closed.")
                } catch (e: Throwable) {
                    onErrored(-1, "Unknown error caused by: ${e.message}")
                } finally {
                    connected.set(false)
                    try {
                        DiscordRpc.Discord_Shutdown()
                    } catch (e: Throwable) {
                    }
                }
            }
        }
    }

    /**
     * Send a new Rich Presence to Discord.
     *
     * @throws [IllegalStateException] when not connected
     */
    suspend fun updatePresence(presence: DiscordRichPresence) {
        if (!connected.get() || runner == null) throw IllegalStateException("Not connected!")

        coroutineScope {
            launch(runner!!) {
                DiscordRpc.Discord_UpdatePresence(presence)
            }
        }
    }

    /**
     * Disconnect the Rich Presence Connection.
     * Ideally, this call would be followed by a call to [finishPending] before the end of the application's lifecycle.
     *
     * @throws [IllegalStateException] when not connected
     */
    fun disconnect() {
        if (!connected.get() && runner == null) throw IllegalStateException("Not connected!")
        connected.set(false)
        runner?.cancel()
    }

    /**
     * Ensures everything finishes in a clean state.
     * This is a blocking call, and *may* not finish immediately.
     *
     * @throws [IllegalStateException] when still connected
     */
    fun finishPending() = runBlocking {
        if (connected.get()) throw IllegalStateException("Still connected!")
        runner?.join()
        runner = null
        DiscordRpc.Discord_Shutdown()
    }

    /**
     * Run [block] immediately if connected, otherwise run it upon connection.
     */
    suspend fun ifConnectedOrLater(block: suspend (DiscordUser) -> Unit) {
        coroutineScope {
            if (connected.get()) launch { block(user) }
            else onReady = {
                launch { block(it) }
            }
        }
    }

    /**
     * @return Whether this handler is currently connected.
     */
    @Volatile
    var connected = AtomicBoolean(false)
        private set

    @Volatile
    lateinit var user: DiscordUser
        private set

    private val handlers = DiscordEventHandlers {
        onReady {
            user = it
            connected.set(true)
            this@RPCHandler.onReady(it)
        }
        onDisconnected { errorCode, message ->
            logger.warn("Disconnexted: #$errorCode (${message.takeIf { message.isNotEmpty() }
                    ?: "No message provided"})")
            connected.set(false)
            runner?.cancel()
            this@RPCHandler.onDisconnected(errorCode, message)
        }
        onErrored { errorCode, message ->
            logger.error("Error: #$errorCode (${message.takeIf { message.isNotEmpty() } ?: "No message provided"})")
            connected.set(true)
            runner?.cancel()
            this@RPCHandler.onErrored(errorCode, message)
        }
        onJoinGame { joinSecret -> this@RPCHandler.onJoinGame(joinSecret) }
        onSpectateGame { spectateSecret -> this@RPCHandler.onSpectateGame(spectateSecret) }
        onJoinRequest { request -> this@RPCHandler.onJoinRequest(request) }
    }

    @Volatile
    private var runner: Job? = null
}
