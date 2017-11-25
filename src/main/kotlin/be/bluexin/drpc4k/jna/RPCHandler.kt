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
import kotlinx.coroutines.experimental.*
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Handles the Discord Rich Presence Connection.
 * Replace the callbacks ([onReady], [onDisconnected], ...) with your own.
 * Please note that all callbacks will be run on the RPC Thread. Take extra care !
 *
 * @author Bluexin
 */
@Suppress("MemberVisibilityCanPrivate", "unused")
object RPCHandler {

    /**
     * Called when Discord Rich Presence is ready.
     */
    @Volatile
    var onReady: () -> Unit = {}

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
    var onJoinRequest: (request: DiscordJoinRequest) -> Unit = { _ -> }

    /**
     * Tries to connect the Discord Rich Presence Connection asynchronously.
     *
     * @param[clientId] your app's public ID, client_id, application_id, whatever you wanna call it
     * @param[autoRegister] whether Discord should register your app for automatic launch
     * @param[steamId] your app's Steam ID, if any
     * @param[refreshRate] the rate at which this handler will run callbacks and send info to discord
     * @throws [IllegalStateException] when already connected
     */
    fun connect(clientId: String, autoRegister: Boolean = false, steamId: String? = null, refreshRate: Long = 500L) {
        if (connected.get()) throw IllegalStateException("Already connected!")
        runner = launch(CommonPool) {
            DiscordRpc.Discord_Initialize(clientId, handlers, autoRegister, steamId)

            try {
                while (isActive) {
                    DiscordRpc.Discord_RunCallbacks()
                    delay(refreshRate)
                }
            } finally {
                connected.set(false)
                DiscordRpc.Discord_Shutdown()
            }
        }
    }

    /**
     * Send a new Rich Presence to Discord.
     *
     * @throws [IllegalStateException] when not connected
     */
    fun updatePresence(presence: DiscordRichPresence) {
        if (!connected.get() || runner == null) throw IllegalStateException("Not connected!")

        launch(runner!!) {
            DiscordRpc.Discord_UpdatePresence(presence)
        }
    }

    /**
     * Disconnect the Rich Presence Connection.
     * Ideally, this call would be followed by a call to [finishPending] before the end of the application's lifecycle.
     *
     * @throws [IllegalStateException] when not connected
     */
    fun disconnect() {
        if (!connected.get() || runner == null) throw IllegalStateException("Not connected!")
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
    }

    /**
     * Run [block] immediately if connected, otherwise run it upon connection.
     */
    inline fun ifConnectedOrLater(crossinline block: () -> Unit) {
        if (connected.get()) block()
        else onReady = {
            block()
        }
    }

    /**
     * @return Whether this handler is currently connected.
     */
    @Volatile
    var connected = AtomicBoolean(false)
        private set

    private val handlers = DiscordEventHandlers {
        onReady {
            println("Let's Rock !")
            connected.set(true)
            this@RPCHandler.onReady()
        }
        onDisconnected { errorCode, message ->
            println("dc :x #$errorCode  (${message.takeIf { message.isNotEmpty() }?: "No message provided"})")
            connected.set(true)
            runner?.cancel()
            this@RPCHandler.onDisconnected(errorCode, message)
        }
        onErrored { errorCode, message ->
            println("Something somewhere went terribly wrong. #$errorCode (${message.takeIf { message.isNotEmpty() }?: "No message provided"})")
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
