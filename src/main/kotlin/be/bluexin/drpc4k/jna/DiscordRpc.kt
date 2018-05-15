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

@file:Suppress("MemberVisibilityCanPrivate", "FunctionName", "PropertyName", "unused")

package be.bluexin.drpc4k.jna

import com.sun.jna.Callback
import com.sun.jna.CallbackReference
import com.sun.jna.Native
import com.sun.jna.Pointer

/**
 * Maps directly to discord-rpc.h
 * Use this only if you know what you're doing.
 * The handler class [RPCHandler] will be more useful for most people.
 *
 * @author Bluexin
 */
object DiscordRpc {

    /**
     * Initialize the connection to Discord Client.
     *
     */
    external fun Discord_Initialize(
            applicationId: String,
            handlers: DiscordEventHandlers?,
            autoRegister: Boolean,
            optionalSteamId: String?)

    /**
     * Close the connection to Discord Client.
     */
    external fun Discord_Shutdown()

    /**
     * Sync stats to/from Discord Client.
     */
    external fun Discord_RunCallbacks()

    //    external fun Discord_UpdateConnection()

    /**
     * Update Rich Presence.
     */
    external fun Discord_UpdatePresence(presence: DiscordRichPresence)

    /**
     * Reply to a party request.
     */
    fun Discord_Respond(userid: String, reply: DISCORD_REPLY) = Discord_Respond(userid, reply.ordinal)

    @PublishedApi
    internal external fun Discord_Respond(userid: String, reply: Int /*DISCORD_REPLY_*/)

    init {
        Native.register(javaClass, "discord-rpc")
    }

    enum class DISCORD_REPLY {
        DISCORD_REPLY_NO,
        DISCORD_REPLY_YES,
        DISCORD_REPLY_IGNORE

        /*
        #define DISCORD_REPLY_NO 0
        #define DISCORD_REPLY_YES 1
        #define DISCORD_REPLY_IGNORE 2
         */
    }
}

/**
 * Presence update structure.
 *
 * (typedef struct DiscordRichPresence)
 */
class DiscordRichPresence() : Structure() {

    constructor(initializer: DiscordRichPresence.() -> Unit) : this() {
        executeBatch(initializer)
    }

    /**
     * the user's current party status
     * Maximum 128 characters long.
     */
    var state: String
        get() = _state
        set(value) {
            ensureLength(value, 128, "state")
            _state = value
            if (!batching) write()
        }

    /**
     * what the player is currently doing
     * Maximum 128 characters long.
     */
    var details: String
        get() = _details
        set(value) {
            ensureLength(value, 128, "details")
            _details = value
            if (!batching) write()
        }

    /**
     * Helper method to set the game's duration.
     */
    fun setDuration(seconds: Long) {
        startTimeStamp = System.currentTimeMillis() / 1000L
        endTimeStamp = startTimeStamp + seconds
    }

    /**
     * unix timestamp for the start of the game
     */
    var startTimeStamp: Long
        get() = _startTimeStamp
        set(value) {
            _startTimeStamp = value
            if (!batching) write()
        }

    /**
     * unix timestamp for when the game will end
     */
    var endTimeStamp: Long
        get() = _endTimeStamp
        set(value) {
            _endTimeStamp = value
            if (!batching) write()
        }

    /**
     * name of the uploaded image for the large profile artwork
     * Maximum 32 characters long.
     */
    var largeImageKey: String
        get() = _largeImageKey
        set(value) {
            ensureLength(value, 32, "largeImageKey")
            _largeImageKey = value
            if (!batching) write()
        }

    /**
     * tooltip for the largeImageKey
     * Maximum 128 characters long.
     */
    var largeImageText: String
        get() = _largeImageText
        set(value) {
            ensureLength(value, 128, "largeImageText")
            _largeImageText = value
            if (!batching) write()
        }

    /**
     * 	name of the uploaded image for the small profile artwork
     * Maximum 32 characters long.
     */
    var smallImageKey: String
        get() = _smallImageKey
        set(value) {
            ensureLength(value, 32, "smallImageKey")
            _smallImageKey = value
            if (!batching) write()
        }

    /**
     * tootltip for the smallImageKey
     * Maximum 128 characters long.
     */
    var smallImageText: String
        get() = _smallImageText
        set(value) {
            ensureLength(value, 128, "smallImageText")
            _smallImageText = value
            if (!batching) write()
        }

    /**
     * 	id of the player's party, lobby, or group
     * Maximum 128 characters long.
     */
    var partyId: String
        get() = _partyId
        set(value) {
            ensureLength(value, 128, "partyId")
            _partyId = value
            if (!batching) write()
        }

    /**
     * current size of the player's party, lobby, or group
     */
    var partySize: Int
        get() = _partySize
        set(value) {
            _partySize = value
            if (!batching) write()
        }

    /**
     * maximum size of the player's party, lobby, or group
     */
    var partyMax: Int
        get() = _partyMax
        set(value) {
            _partyMax = value
            if (!batching) write()
        }

    /**
     * unique hashed string for Spectate and Join
     * Maximum 128 characters long.
     */
    var matchSecret: String
        get() = _matchSecret
        set(value) {
            ensureLength(value, 128, "matchSecret")
            _matchSecret = value
            if (!batching) write()
        }

    /**
     * 	unique hased string for chat invitations and Ask to Join
     * Maximum 128 characters long.
     */
    var joinSecret: String
        get() = _joinSecret
        set(value) {
            ensureLength(value, 128, "joinSecret")
            _joinSecret = value
            if (!batching) write()
        }

    /**
     * unique hased string for Spectate button
     * Maximum 128 characters long.
     */
    var spectateSecret: String
        get() = _spectateSecret
        set(value) {
            ensureLength(value, 128, "spectateSecret")
            _spectateSecret = value
            if (!batching) write()
        }

    /**
     * helps track when games have ended
     */
    var instance: Byte
        get() = _instance
        set(value) {
            _instance = value
            if (!batching) write()
        }

    internal lateinit var _state: String          /* max 128 bytes */
    internal lateinit var _details: String        /* max 128 bytes */
    @JvmField
    internal var _startTimeStamp: Long = 0
    @JvmField
    internal var _endTimeStamp: Long = 0
    internal lateinit var _largeImageKey: String  /* max 32 bytes */
    internal lateinit var _largeImageText: String /* max 128 bytes */
    internal lateinit var _smallImageKey: String  /* max 32 bytes */
    internal lateinit var _smallImageText: String /* max 128 bytes */
    internal lateinit var _partyId: String        /* max 128 bytes */
    @JvmField
    internal var _partySize: Int = 0
    @JvmField
    internal var _partyMax: Int = 0
    internal lateinit var _matchSecret: String    /* max 128 bytes */
    internal lateinit var _joinSecret: String     /* max 128 bytes */
    internal lateinit var _spectateSecret: String /* max 128 bytes */
    @JvmField
    internal var _instance: Byte = 0

    override fun getFieldOrder() = FIELD_ORDER

    private companion object {
        val FIELD_ORDER = listOf(
                "_state",
                "_details",
                "_startTimeStamp",
                "_endTimeStamp",
                "_largeImageKey",
                "_largeImageText",
                "_smallImageKey",
                "_smallImageText",
                "_partyId",
                "_partySize",
                "_partyMax",
                "_matchSecret",
                "_joinSecret",
                "_spectateSecret",
                "_instance"
        )
    }

    internal fun ensureLength(value: String, max: Int, field: String) {
        if (value.length > max) throw IllegalArgumentException("$field must not be longer than $max characters. Provided ${value.length} chars.")
    }
}

/**
 * Event handlers structure.
 *
 * (typedef struct DiscordEventHandlers)
 */
class DiscordEventHandlers() : Structure() {

    constructor(initializer: DiscordEventHandlers.() -> Unit) : this() {
        executeBatch(initializer)
    }

    fun onReady(write: Boolean = true, body: (user: DiscordUser) -> Unit) {
        _ready = CallbackReference.getFunctionPointer(object : Callback {
            fun invoke(user: DiscordUser) = body(user)
        })
        if (write && !batching) write()
    }

    fun onDisconnected(write: Boolean = true, body: (errorCode: Int, message: String) -> Unit) {
        _disconnected = CallbackReference.getFunctionPointer(object : Callback {
            fun invoke(errorCode: Int, message: String) = body(errorCode, message)
        })
        if (write && !batching) write()
    }

    fun onErrored(write: Boolean = true, body: (errorCode: Int, message: String) -> Unit) {
        _errored = CallbackReference.getFunctionPointer(object : Callback {
            fun invoke(errorCode: Int, message: String) = body(errorCode, message)
        })
        if (write && !batching) write()
    }

    fun onJoinGame(write: Boolean = true, body: (joinSecret: String) -> Unit) {
        _joinGame = CallbackReference.getFunctionPointer(object : Callback {
            fun invoke(joinSecret: String) = body(joinSecret)
        })
        if (write && !batching) write()
    }

    fun onSpectateGame(write: Boolean = true, body: (spectateSecret: String) -> Unit) {
        _spectateGame = CallbackReference.getFunctionPointer(object : Callback {
            fun invoke(spectateSecret: String) = body(spectateSecret)
        })
        if (write && !batching) write()
    }

    fun onJoinRequest(write: Boolean = true, body: (request: DiscordUser) -> Unit) {
        _joinRequest = CallbackReference.getFunctionPointer(object : Callback {
            fun invoke(request: DiscordUser) {
                body(request)
            }
        })
        if (write && !batching) write()
    }

    /*
    Error codes :
    1000 = User logout
        happens when user logs out
    4000 = Invalid Client ID
        happens when Client ID is wrong, or when user is not logged into Discord
    5005 = secrets must be unique
        happens when you use the same secret for invite & spectate
     */

    internal lateinit var _ready: Pointer
    internal lateinit var _disconnected: Pointer
    internal lateinit var _errored: Pointer
    internal lateinit var _joinGame: Pointer
    internal lateinit var _spectateGame: Pointer
    internal lateinit var _joinRequest: Pointer

    override fun getFieldOrder() = FIELD_ORDER

    private companion object {
        val FIELD_ORDER = listOf(
                "_ready",
                "_disconnected",
                "_errored",
                "_joinGame",
                "_spectateGame",
                "_joinRequest"
        )
    }
}

/**
 * Discord user structure, used in Join request and Ready event.
 * The Ask to Join request persists for 30 seconds after the request is received.
 *
 * (typedef struct User)
 */
class DiscordUser : Structure() {

    /**
     * the userId of the player asking to join
     *
     * snowflake (64bit int), turned into a ascii decimal string, at most 20 chars +1 null
     * terminator = 21
     * (char[32])
     */
    lateinit var userId: String
        internal set

    /**
     * the username of the player asking to join
     *
     * 32 unicode glyphs is max name size => 4 bytes per glyph in the worst case, +1 for null
     * terminator = 129
     * (char[344])
     */
    lateinit var username: String
        internal set

    /**
     *
     *
     * 4 decimal digits + 1 null terminator = 5
     * char discriminator[8];
     */
    lateinit var discriminator: String
        internal set

    /**
     * the avatar hash of the player asking to join—see [image formatting](https://discordapp.com/developers/docs/reference#image-formatting) for how to retrieve the image
     * can be an empty string if the user has not uploaded an avatar to Discord
     *
     * optional 'a_' + md5 hex digest (32 bytes) + null terminator = 35
     * (char[128])
     */
    lateinit var avatar: String
        internal set

    override fun getFieldOrder() = FIELD_ORDER

    private companion object {
        val FIELD_ORDER = listOf(
                "userId",
                "username",
                "discriminator",
                "avatar"
        )
    }
}

abstract class Structure : com.sun.jna.Structure() {
    protected var batching = false

    @PublishedApi
    internal var `access$batching`: Boolean
        get() = batching
        set(value) {
            batching = value
        }
}

inline fun <reified T : Structure> T.executeBatch(body: T.() -> Unit) {
    `access$batching` = true
    this.body()
    `access$batching` = false
    write()
}
