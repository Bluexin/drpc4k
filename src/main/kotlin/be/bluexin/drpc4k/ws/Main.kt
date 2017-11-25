package be.bluexin.drpc4k.ws

import org.java_websocket.WebSocket
import org.java_websocket.client.WebSocketClient
import org.java_websocket.drafts.Draft
import org.java_websocket.drafts.Draft_6455
import org.java_websocket.handshake.ClientHandshake
import org.java_websocket.handshake.HandshakedataImpl1
import org.java_websocket.handshake.ServerHandshake
import java.net.URI
import java.nio.ByteBuffer


/**
 * Part of drpc4k by Bluexin, released under GNU GPLv3.
 *
 * @author Bluexin
 */
fun main(args: Array<String>) {
    if (args.isEmpty()) {
        println("Missing Client ID")
        return
    }

    val encoding = "json"
    val client_id = args[0]
    val version = 1

    var ws: EmptyClient? = null
    for (port in 6463..6472) {
        ws = EmptyClient(URI("ws://localhost.:$port/?v=$version&client_id=$client_id&encoding=$encoding"),
                headers = mapOf("Origin" to "https://localhost"))

        if (ws.connectBlocking()) {
            println("Opened discord rpc on port $port.")
            break
        }
    }

    Thread.sleep(1000)
    ws?.closeBlocking()
}

class EmptyClient(serverURI: URI,
                  protocolDraft: Draft = Draft_6455(),
                  headers: Map<String, String>? = null,
                  connectionTimeout: Int = 3000) :
        WebSocketClient(serverURI, protocolDraft, headers, connectionTimeout) {

    override fun onOpen(handshakedata: ServerHandshake) {
        println("opened")
    }

    override fun onClose(code: Int, reason: String, remote: Boolean) {
        println("closed : $code = $reason")
    }

    override fun onMessage(message: String) {
        println("message: " + message)
    }

    override fun onMessage(message: ByteBuffer) {
        println("received ByteBuffer")
    }

    override fun onError(ex: Exception) {
        System.err.println("an error occurred:")
        ex.printStackTrace()
    }

    override fun onWebsocketHandshakeSentAsClient(conn: WebSocket?, request: ClientHandshake?) {
        println("Sending handshake. Origin : ${(request as? HandshakedataImpl1)?.getFieldValue("Origin")}")
        super.onWebsocketHandshakeSentAsClient(conn, request)
    }
}
