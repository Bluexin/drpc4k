package be.bluexin.drpc4k.kinterface

/**
 * Huge WIP, not working yet.
 *
 * @author Bluexin
 */
object DiscordRpc {
    var pid: Long = 0L
    lateinit var handlers: DiscordEventHandlers
    var connection: Any? /*RpcConnection?*/ = null

    fun initializeDiscord(applicationId: String, handlers: DiscordEventHandlers?, autoRegister: Boolean, steamId: String? = null) {
        if (autoRegister) {
            if (steamId == null) {
                // Register non-Steam app
            } else {
                // Register Steam app
            }
        }

        pid = ProcessHandle.current().pid()

        DiscordRpc.handlers = handlers?: DiscordEventHandlers()

        if (connection != null) return


    }
}
