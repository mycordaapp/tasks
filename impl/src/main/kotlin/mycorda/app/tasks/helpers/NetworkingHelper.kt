package mycorda.app.tasks.helpers

import mycorda.app.tasks.SocketAddress
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketTimeoutException

object NetworkingHelper {
    fun isSocketAlive(address: SocketAddress, timeoutMs: Int = 1000): Boolean {
        var isAlive = false
        val socketAddress = InetSocketAddress(address.address, address.port)
        val socket = Socket()
        try {
            socket.connect(socketAddress, timeoutMs)
            isAlive = true
        } catch (steIgnored: SocketTimeoutException) {
        } catch (ioeIgnored: IOException) {
        }

        return isAlive
    }
}