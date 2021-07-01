package mycorda.app.tasks

data class SocketAddress(val address: String, val port: Int) {
    constructor(sockerAddress: String) : this(sockerAddress.split(":")[0],
            sockerAddress.split(":")[1].toInt())

}