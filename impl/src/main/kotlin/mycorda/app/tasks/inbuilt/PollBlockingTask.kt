package  mycorda.app.tasks.inbuilt


import mycorda.app.tasks.BlockingTask
import mycorda.app.tasks.executionContext.ExecutionContext
import java.util.*
import kotlin.concurrent.thread

/**
 * A simple polling task that keeps trying the underlying
 * task until it returns a condition that matches the successMapper,
 * or it timeouts or the underlying task raises an exception
 */
class PollBlockingTask<I, O>(
    private val task: BlockingTask<I, O>,
    private val successMapper: (O) -> Boolean,
    private val maxWaitMs: Long = 60000,
    private val intervalMs: Long = 1000
) : BlockingTask<I, O> {

    private val taskId = UUID.randomUUID()
    override fun taskId(): UUID {
        return taskId
    }

    override fun exec(ctx: ExecutionContext, params: I): O {
        val startTime = System.currentTimeMillis()
        var result = task.exec(ctx, params)

        val t = thread(start = false) {
            while (!successMapper(result) && System.currentTimeMillis() < (startTime + maxWaitMs)) {
                Thread.sleep(intervalMs)
                result = task.exec(ctx, params)
            }
        }
        t.run()
        if (!successMapper(result)) {
            throw RuntimeException("Timeout waiting for task to complete")
        }

        return result
    }

}