package mycorda.app.tasks.demo


import mycorda.app.registry.Registry
import mycorda.app.tasks.*
import mycorda.app.tasks.executionContext.ExecutionContext
import mycorda.app.tasks.logging.LogMessage
import java.io.File
import java.util.*

/*
 Some prebuilt demo tasks for tests and example
 */


class CalcSquareTask : BaseBlockingTask<Int, Int>(), TaskDocument<Int, Int> {

    override fun exec(ctx: ExecutionContext, input: Int): Int {
        // this is normally the first line - it ensures the task is stored in the context
        val ctxWithTask = ctx.withTaskId(this)
        ctxWithTask.log("Calculating square of $input")
        return input.times(input)
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

//    fun exec(ctx: ExecutionContext, input: String): Future<String> {
//        val ctx = DefaultExecutionContextModifier(ctx).withTaskId(taskId())
//        ctx.log(LogMessage.info("Message is '$input'"))
//        return executors.submit<String> {
//            if (input.contains("exception", ignoreCase = true)) throw RuntimeException(input)
//            AsyncTask.sleepForTicks(1)
//            input
//        }
//    }

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
    override fun exec(ctx: ExecutionContext, input: File): Int {
        val ctx2 = ctx.withTaskId(taskId())
        ctx2.log(LogMessage.info("Loading file $input"))
        return input.readBytes().size
    }
}

class UnitTask : BaseUnitBlockingTask<String>() {
    override fun exec(ctx: ExecutionContext, input: String) {
        val ctx2 = ctx.withTaskId(taskId())
        ctx2.log(LogMessage.info("Params are: $input"))
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
        val ctxWithTask = ctx.withTaskId(taskId())

        ctxWithTask.log("Starting calculation")

        // 1. Find the channel
        val resultChannel = resultChannelFactory.create(channelLocator)

        ctxWithTask.executorService().submit<Unit> {
            // 2. Generate a result
            val result = AsyncResultChannelMessage(channelId, Success(input * input), Int::class.java)

            // 3. Simulate a delay
            Thread.sleep(AsyncTask.platformTick())

            // 4. Write the result and also echo to logging channels
            ctxWithTask.log("Completed calculation")
            ctxWithTask.stdout().println(result)
            resultChannel.accept(result)
        }
    }

    override fun taskId(): UUID = taskId
}




