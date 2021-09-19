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
    override fun taskId(): UUID {
        return id
    }

    /**
     * Update the ExecutionContext with the TaskId.
     */
    fun updatedCtx(ctx: ExecutionContext): ExecutionContext = DefaultExecutionContextModifier(ctx).withTaskId(taskId())
}


class CalcSquareTask : BaseBlockingTask<Int, Int>(), TaskDocument<Int, Int> {

    override fun exec(ctx: ExecutionContext, params: Int): Int {
        // this is normally the first line - it ensures the task is stored in the context
        val ctx = ctx.withTaskId(this)
        ctx.log("Calculating square of $params")
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
                "two squared",
                input, output
            )
        )
    }
}


class ExceptionGeneratingAsyncTask(registry: Registry) : BaseAsyncTask<String, String>() {

    private val executors = registry.get(ExecutorFactory::class.java).executorService()

    fun exec(ctx: ExecutionContext, params: String): Future<String> {
        val ctx = DefaultExecutionContextModifier(ctx).withTaskId(taskId())
        ctx.log(LogMessage.info("Message is '$params'"))
        return executors.submit<String> {
            if (params.contains("exception", ignoreCase = true)) throw RuntimeException(params)
            Thread.sleep(10)
            params
        }
    }

    override fun exec(
        ctx: ExecutionContext,
        channelLocator: AsyncResultChannelSinkLocator,
        channelId: UniqueId,
        input: String
    ) {
        TODO("Not yet implemented")
    }
}

class FileTask : BaseBlockingTask<File, Int>() {
    override fun exec(ctx: ExecutionContext, file: File): Int {
        val ctx = DefaultExecutionContextModifier(ctx).withTaskId(taskId())
        ctx.log(LogMessage.info("Loading file $file"))
        return file.readBytes().size
    }
}

class UnitTask : BaseUnitBlockingTask<String>() {
    override fun exec(ctx: ExecutionContext, input: String) {
        val ctx = DefaultExecutionContextModifier(ctx).withTaskId(taskId())
        ctx.log(LogMessage.info("Params are: $input"))
    }
}

class PrintStreamTask : BaseUnitBlockingTask<String>() {
    override fun exec(ctx: ExecutionContext, input: String) {
        ctx.stdout().println(input)
    }
}

class CalcSquareAsyncTask(registry: Registry) : AsyncTask<Int, Int> {
    private val resultChannelFactory = registry.get(AsyncResultChannelSinkFactory::class.java)
    private val taskId = UUID.randomUUID()
    override fun exec(
        ctx: ExecutionContext,
        channelLocator: AsyncResultChannelSinkLocator,
        channelId: UniqueId,
        input: Int
    ) {
        // 1. Find my channel
        val resultChannel = resultChannelFactory.create(channelLocator)

        ctx.executorService().submit<Unit> {

            // 2. Generate a result
            val result = AsyncResultChannelMessage(channelId, Success(input * input), Int::class.java)

            Thread.sleep(AsyncTask.platformTick())

            // 3. Write the result
            resultChannel.accept(result)
        }

//        //ctx.executorService().submit
//
//
//        // In real code wait for the long running process, i.e. start a thread, wait on
//        // an event
//
//        // 2. Generate a result
//        val result = AsyncResultChannelMessage(channelId, Success(input * input), Int::class.java)
//
//        // 3. Write the result
//        resultChannel.accept(result)
    }

    override fun taskId(): UUID = taskId
}




