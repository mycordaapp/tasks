package  mycorda.app.tasks.inbuilt

import mycorda.app.tasks.BlockingTask
import mycorda.app.tasks.executionContext.ExecutionContext
import mycorda.app.tasks.executionContext.TestContextManager
import java.util.*


/**
 * Just echo the message supplied. For testing and debug
 */
class EchoToConsoleTask : BlockingTask<String, Unit> {
    private val taskId = UUID.randomUUID()
    override fun taskID(): UUID {
        return taskId
    }

    override fun exec(ctx: ExecutionContext, params: String): Unit {
        ctx.stdout().println(params)
    }
}



// basic test harness
fun main(args: Array<String>) {

    val manager = TestContextManager().initialise()
    val ctx = manager.createExecutionContext()

    EchoToConsoleTask().exec(ctx,"Hello World")

    manager.printCaptureStdOut()

//
//    val data = "wibble"
//
//    val enc = data.encrypt()
//    println(enc)
//    val dec = enc.decrypt()
//    println(dec)
}