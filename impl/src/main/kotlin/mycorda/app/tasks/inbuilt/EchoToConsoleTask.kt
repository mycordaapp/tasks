package  mycorda.app.tasks.inbuilt

import mycorda.app.tasks.BlockingTask
import mycorda.app.tasks.executionContext.ExecutionContext
import mycorda.app.tasks.executionContext.SimpleExecutionContext
import java.util.*


/**
 * Just echo the message supplied. For testing and debug
 */
class EchoToConsoleTask : BlockingTask<String, Unit> {
    private val taskId = UUID.randomUUID()
    override fun taskId(): UUID {
        return taskId
    }

    override fun exec(ctx: ExecutionContext, input: String): Unit {
        ctx.stdout().println(input)
    }
}



// basic test harness
fun main() {

    val ctx = SimpleExecutionContext()
    EchoToConsoleTask().exec(ctx,"Hello World")

    //ctx.logger()

}