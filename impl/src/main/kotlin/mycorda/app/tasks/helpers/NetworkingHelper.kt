package mycorda.app.tasks.helpers

import mycorda.app.helpers.SocketTester
import mycorda.app.tasks.SocketAddress


object NetworkingHelper {
    fun isSocketAlive(address: SocketAddress, timeoutMs: Int = 1000): Boolean {
        return SocketTester.isLive(address.address, address.port, timeoutMs)
    }
}