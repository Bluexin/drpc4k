package be.bluexin.drpc4k.jna

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.actor
import mu.KotlinLogging

sealed class RPCMessage
class UpdatePresence(val presence: DiscordRichPresence) : RPCMessage()
class SetOnReady(val callback: ReadyCallback) : RPCMessage()
class SetOnDisconnected(val callback: DisconnectedCallback) : RPCMessage()
class SetOnErrored(val callback: ErroredCallback) : RPCMessage()
class SetOnJoinGame(val callback: JoinGameCallback) : RPCMessage()
class SetOnSpectate(val callback: SpectateCallback) : RPCMessage()
class SetOnJoinRequest(val callback: JoinRequestCallback) : RPCMessage()
class IsConnected(val response: CompletableDeferred<Boolean>) : RPCMessage()
class IsInitialized(val response: CompletableDeferred<Boolean>) : RPCMessage()
class Connect(
        val clientId: String,
        val autoRegister: Boolean = false,
        val steamId: String? = null,
        val refreshRate: Long = 500L
) : RPCMessage()

typealias ReadyCallback = (user: DiscordUser) -> Unit
typealias DisconnectedCallback = (errorCode: Int, message: String) -> Unit
typealias ErroredCallback = (errorCode: Int, message: String) -> Unit
typealias JoinGameCallback = (joinSecret: String) -> Unit
typealias SpectateCallback = (spectateSecret: String) -> Unit
typealias JoinRequestCallback = (user: DiscordUser) -> Unit

@ExperimentalCoroutinesApi
fun CoroutineScope.rpcActor() = actor<RPCMessage>(context = Dispatchers.Unconfined, capacity = Channel.UNLIMITED) {
    with(RPCActor(channel)) { for (msg in channel) onReceive(msg) }
}

@ExperimentalCoroutinesApi
private class RPCActor(
        private val channel: Channel<RPCMessage>
) {
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
            onDisconnected(0, "Discord RPC Thread closed.")
        } catch (e: Throwable) {
            onErrored(-1, "Unknown error caused by: ${e.message}")
        } finally {
            connected = false
            try {
                DiscordRpc.Discord_Shutdown()
            } catch (e: Throwable) {
            }
        }
    }

    suspend fun onReceive(msg: RPCMessage) {
        when (msg) {
            is UpdatePresence -> if (initialized) DiscordRpc.Discord_UpdatePresence(msg.presence) else queuedPresence = msg.presence
            is SetOnReady -> onReady = msg.callback
            is SetOnDisconnected -> onDisconnected = msg.callback
            is SetOnErrored -> onErrored = msg.callback
            is SetOnJoinGame -> onJoinGame = msg.callback
            is SetOnSpectate -> onSpectateGame = msg.callback
            is SetOnJoinRequest -> onJoinRequest = msg.callback
            is Connect -> with(msg) { start(clientId, autoRegister, steamId, refreshRate) }
            is IsConnected -> msg.response.complete(this.connected)
            is IsInitialized -> msg.response.complete(this.initialized)
        }
    }

    private val handlers = DiscordEventHandlers {
        onReady {
            user = it
            connected = true
            this@RPCActor.onReady(it)
        }
        onDisconnected { errorCode, message ->
            logger.warn("Disconnected: #$errorCode (${message.takeIf { message.isNotEmpty() }
                    ?: "No message provided"})")
            connected = false
            this@RPCActor.onDisconnected(errorCode, message)
        }
        onErrored { errorCode, message ->
            logger.error("Error: #$errorCode (${message.takeIf { message.isNotEmpty() } ?: "No message provided"})")
            connected = false
            this@RPCActor.onErrored(errorCode, message)
        }
        onJoinGame { joinSecret -> this@RPCActor.onJoinGame(joinSecret) }
        onSpectateGame { spectateSecret -> this@RPCActor.onSpectateGame(spectateSecret) }
        onJoinRequest { request -> this@RPCActor.onJoinRequest(request) }
    }
}
