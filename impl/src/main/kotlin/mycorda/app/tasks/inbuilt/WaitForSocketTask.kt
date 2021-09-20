package  mycorda.app.tasks.inbuilt


import mycorda.app.registry.Registry
import mycorda.app.tasks.AsyncTask
import mycorda.app.tasks.AsyncResultChannelSinkLocator
import mycorda.app.tasks.SocketAddress
import mycorda.app.tasks.UniqueId
import mycorda.app.tasks.executionContext.ExecutionContext
import mycorda.app.tasks.executionContext.SimpleExecutionContext
import java.util.*

/**
 * Checks for an open socket on a given address
 *
 */

interface WaitForSocketTask : AsyncTask<SocketAddress, Long> {}

class WaitForSocketTaskImpl : WaitForSocketTask {
    private val taskId = UUID.randomUUID()
    override fun exec(
        ctx: ExecutionContext,
        channelLocator: AsyncResultChannelSinkLocator,
        channelId: UniqueId,
        input: SocketAddress
    ) {
        TODO("Not yet implemented")
    }

    override fun taskId(): UUID {
        return taskId
    }

//    override fun exec(ctx: ExecutionContext, params: SocketAddress): Future<Long> {
//        val start = System.currentTimeMillis()
//        return ctx.executorService().submit<Long> {
//            while (!NetworkingHelper.isSocketAlive(params)) {
//                Thread.sleep(1000)
//            }
//            System.currentTimeMillis() - start
//        }
//    }
}


class WaitForSocketTaskFake : WaitForSocketTask {
    private val taskId = UUID.randomUUID()
    override fun exec(
        ctx: ExecutionContext,
        channelLocator: AsyncResultChannelSinkLocator,
        channelId: UniqueId,
        input: SocketAddress
    ) {
        TODO("Not yet implemented")
    }

    override fun taskId(): UUID {
        return taskId
    }


}


// basic test harness
fun main(args: Array<String>) {

    val ctx = SimpleExecutionContext()
    //val address = SocketAddress("localhost", 12345)

    // Can use netcat to create a test server:
    // $ nc -l -p 12345
    // or on a Mac:
    // $ nc -p 12345

    try {
        println("Running WaitAgentReadyCmdImpl")
        //val result = WaitForSocketTaskImpl().exec(ctx, address)
        //println("Success!, that took $result milliseconds")
    } catch (ex: RuntimeException) {
        ex.printStackTrace()
    }

    ctx.executorService().shutdownNow()
}