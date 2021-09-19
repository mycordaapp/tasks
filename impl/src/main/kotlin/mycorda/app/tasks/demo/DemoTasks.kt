package mycorda.app.tasks.demo


import mycorda.app.registry.Registry
import mycorda.app.tasks.*
import mycorda.app.tasks.executionContext.DefaultExecutionContextModifier
import mycorda.app.tasks.executionContext.ExecutionContext
import mycorda.app.tasks.logging.LogMessage
import java.io.File
import java.util.*
import java.util.concurrent.Future

/*
 Some prebuilt demo tasks for tests and example
 */

@Deprecated(message = "use one the Base variants defined in Tasks.kt")
abstract class BaseTask : Task {
    private val id = UUID.randomUUID()
    override fun taskID(): UUID {
        return id
    }

    /**
     * Update the ExecutionContext with the TaskId.
     */
    fun updatedCtx(ctx: ExecutionContext): ExecutionContext = DefaultExecutionContextModifier(ctx).withTaskId(taskID())
}


class CalcSquareTask : BaseBlockingTask<Int, Int>(), TaskDocument<Int, Int> {

    override fun exec(ctx: ExecutionContext, params: Int): Int {
        val ctx = ctxWithTaskID(ctx)
        ctx.log(LogMessage.info("Calculating square of $params"))

        return params.times(params)
    }

    override fun description(): String {
        return "An example Task that calculates the square of a number"
    }

    override fun examples(): List<TaskExample<Int, Int>> {
        val input = DefaultTaskExampleData<Int>(2)
        val output = DefaultTaskExampleData<Int>(4)
        return listOf(
            DefaultTaskExample<Int, Int>(
                "two sqaured",
                input, output
            )
        )
    }
}

class CalcSquareAsyncTask(registry: Registry, private val delayMs: Long = 1000) : BaseAsyncTask<Int, Int>() {

    private val executors = registry.get(ExecutorFactory::class.java).executorService()

    override fun exec(ctx: ExecutionContext, num: Int): Future<Int> {
        val ctx = updatedCtx(ctx)
        ctx.log(LogMessage.info("Calculating square of $num"))
        return executors.submit<Int> {
            Thread.sleep(delayMs)
            num * num
        }
    }
}

class CalcSquareAsync2Task(registry: Registry) : BaseAsyncTask<Int, Int>() {

    private val executors = registry.get(ExecutorFactory::class.java).executorService()

    override fun exec(ctx: ExecutionContext, params: Int): Future<Int> {
        val ctx = DefaultExecutionContextModifier(ctx).withTaskId(taskID())
        ctx.log(LogMessage.info("Calculating square of $params"))
        return executors.submit<Int> {
            Thread.sleep(10)
            params * params
        }
    }
}


class ExceptionGeneratingAsyncTask(registry: Registry) : BaseAsyncTask<String, String>() {

    private val executors = registry.get(ExecutorFactory::class.java).executorService()

    override fun exec(ctx: ExecutionContext, params: String): Future<String> {
        val ctx = DefaultExecutionContextModifier(ctx).withTaskId(taskID())
        ctx.log(LogMessage.info("Message is '$params'"))
        return executors.submit<String> {
            if (params.contains("exception", ignoreCase = true)) throw RuntimeException(params)
            Thread.sleep(10)
            params
        }
    }
}

class FileTask : BaseBlockingTask<File, Int>() {
    override fun exec(ctx: ExecutionContext, file: File): Int {
        val ctx = DefaultExecutionContextModifier(ctx).withTaskId(taskID())
        ctx.log(LogMessage.info("Loading file $file"))
        return file.readBytes().size
    }
}

class UnitTask : BaseUnitBlockingTask<String>() {
    override fun exec(ctx: ExecutionContext, params: String) {
        val ctx = DefaultExecutionContextModifier(ctx).withTaskId(taskID())
        ctx.log(LogMessage.info("Params are: $params"))
    }
}

class PrintStreamTask : BaseUnitBlockingTask<String>() {
    override fun exec(ctx: ExecutionContext, params: String) {
        ctx.stdout().println(params)
    }
}




