/*
package be.bluexin.drpc4k.kinterface

import java.util.*

*/
/**
 * Part of drpc4k by Bluexin, released under GNU GPLv3.
 *
 * @author Bluexin
 *//*



data class DiscordEventHandlers( // TODO: replace defaults with no-op
        var ready: () -> Unit = {
            println("Discord ready!")
        },
        var disconnected: (errorCode: Int, message: String) -> Unit = { errorCode, message ->
            println("Discord disconnected: $errorCode ($message)")
        },
        var errored: (errorCode: Int, message: String) -> Unit = { errorCode, message ->
            println("Discord errored: $errorCode ($message)")
        },
        var joinGame: (joinSecret: String) -> Unit = { joinSecret ->
            println("Discord join game: $joinSecret")
        },
        var spectateGame: (spectateSecret: String) -> Unit = { joinSecret ->
            println("Discord spectate game: $joinSecret")
        },
        var joinRequest: (request: Any */
/*DiscordJoinRequest*//*
) -> Unit = { joinSecret ->
            println("Discord join request: $joinSecret")
        }
)

const val MaxRpcFrameSize: Int = 64 * 1024 // size_t

object RpcConnection {
    val connection: Any? = null             // BaseConnection?
    var state: State = State.Disconnected
    var onConnect: () -> Unit = {}
    var onDisconnect: (errorCode: Int, message: String) -> Unit = { _, _ -> }
    lateinit var appId: String              // char[64]
    var lastErrorCode: Int = 0
    var lastErrorMessage: String = ""       // char[256]
    val sendFrame: MessageFrame = MessageFrame(OpCode.Close, 0, charArrayOf())

    enum class ErrorCode {  // int
        Success,            // 1
        PipeClosed,         // 2
        ReadCorrupt         // 3
    }

    enum class OpCode { // uint32_t
        Handshake,      // 0
        Frame,          // 1
        Close,          // 2
        Ping,           // 3
        Pong,           // 4
    }

    data class MessageFrameHeader(
            val opcode: OpCode,
            val length: Int     // uint32_t
    )

    data class MessageFrame(
            // MessageFrameHeader
            var opcode: OpCode,
            var length: Int,        // uint32_t
            // MessageFrame
            var message: CharArray  // char message[MaxRpcFrameSize - sizeof(MessageFrameHeader)];
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as MessageFrame
            return opcode == other.opcode && length == other.length && Arrays.equals(message, other.message)
        }

        override fun hashCode(): Int {
            var result = opcode.hashCode()
            result = 31 * result + length
            result = 31 * result + Arrays.hashCode(message)
            return result
        }
    }

    enum class State {  // uint32_t
        Disconnected,
        SentHandshake,
        AwaitingResponse,
        Connected
    }

    val isOpen: Boolean = state == State.Connected

    fun open() {
        when (state) {
            State.Connected -> return
            State.Disconnected ->
                // if (!connection.open()) return
                TODO()
            State.SentHandshake -> {
                val message: Any */
/*JsonDocument*//*
 = this
                if (read(message)) {
                    */
/*auto cmd = GetStrMember(&message, "cmd");
                    auto evt = GetStrMember(&message, "evt");
                    if (cmd && evt && !strcmp(cmd, "DISPATCH") && !strcmp(evt, "READY")) {
                        state = State::Connected;
                        if (onConnect) {
                            onConnect();
                        }
                    }*//*

                }

                TODO()
            }
            State.AwaitingResponse -> {
                sendFrame.opcode = OpCode.Handshake
                sendFrame.length = 0 // (uint32_t)JsonWriteHandshakeObj(sendFrame.message, sizeof(sendFrame.message), RpcVersion, appId);
                */
/*
                if (connection->Write(&sendFrame, sizeof(MessageFrameHeader) + sendFrame.length)) {
                    state = State::SentHandshake;
                }
                else {
                    Close();
                }
                 *//*

                TODO()
            }
        }
    }

    fun close() {
        if (state == State.Connected || state == State.SentHandshake) {
            onDisconnect(lastErrorCode, lastErrorMessage)
        }
        // connection.close()
        state = State.Disconnected
        TODO()
    }

    fun write(data: Any? */
/*const void* *//*
, length: Int): Boolean {
        sendFrame.opcode = OpCode.Frame
        sendFrame.message = charArrayOf(*/
/*data*//*
)
        sendFrame.length = length
        */
/*
        if (!connection->Write(&sendFrame, sizeof(MessageFrameHeader) + length)) {
            Close();
            return false;
        }
         *//*

        TODO()
        return true
    }

    fun read(message: Any */
/*JsonDocument*//*
): Boolean {
        TODO()

        */
/*
        bool RpcConnection::Read(JsonDocument& message)
        {
            if (state != State::Connected && state != State::SentHandshake) {
                return false;
            }
            MessageFrame readFrame;
            for (;;) {
                bool didRead = connection->Read(&readFrame, sizeof(MessageFrameHeader));
                if (!didRead) {
                    if (!connection->isOpen) {
                        lastErrorCode = (int)ErrorCode::PipeClosed;
                        StringCopy(lastErrorMessage, "Pipe closed");
                        Close();
                    }
                    return false;
                }

                if (readFrame.length > 0) {
                    didRead = connection->Read(readFrame.message, readFrame.length);
                    if (!didRead) {
                        lastErrorCode = (int)ErrorCode::ReadCorrupt;
                        StringCopy(lastErrorMessage, "Partial data in frame");
                        Close();
                        return false;
                    }
                    readFrame.message[readFrame.length] = 0;
                }

                switch (readFrame.opcode) {
                case Opcode::Close: {
                    message.ParseInsitu(readFrame.message);
                    lastErrorCode = GetIntMember(&message, "code");
                    StringCopy(lastErrorMessage, GetStrMember(&message, "message", ""));
                    Close();
                    return false;
                }
                case Opcode::Frame:
                    message.ParseInsitu(readFrame.message);
                    return true;
                case Opcode::Ping:
                    readFrame.opcode = Opcode::Pong;
                    if (!connection->Write(&readFrame, sizeof(MessageFrameHeader) + readFrame.length)) {
                        Close();
                    }
                    break;
                case Opcode::Pong:
                    break;
                case Opcode::Handshake:
                default:
                    // something bad happened
                    lastErrorCode = (int)ErrorCode::ReadCorrupt;
                    StringCopy(lastErrorMessage, "Bad ipc frame");
                    Close();
                    return false;
                }
            }
        }
     *//*

    }

    fun create(applicationId: String): RpcConnection {
        TODO()
        // connection = BaseConnection.create()
        // connection.applicationId = applicationId
        return this
    }

    fun destroy() {
        close()
        // connection.destroy()
        state = State.Disconnected
        TODO()
    }
}

object BaseConnection {
    fun t() {

    }
}

*/
/*
struct BaseConnection {
    static BaseConnection* Create();
    static void Destroy(BaseConnection*&);
    bool isOpen{false};
    bool Open();
    bool Close();
    bool Write(const void* data, size_t length);
    bool Read(void* data, size_t length);
};
 *//*



*/
