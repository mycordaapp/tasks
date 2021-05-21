package mycorda.app.tasks.demo


import mycorda.app.registry.Registry
import mycorda.app.tasks.*
import mycorda.app.tasks.executionContext.ExecutionContext
import mycorda.app.tasks.executionContext.LogLevel
import java.io.File
import java.util.*
import java.util.concurrent.Future

/*
 Some prebuilt demo tasks for tests and example
 */

abstract class BaseTask : Task {
    private val id = UUID.randomUUID()
    override fun taskID(): UUID {
        return id
    }
}

class CalcSquareTask : BaseTask(), BlockingTask<Int, Int>, TaskDocument<Int, Int> {

    override fun exec(executionContext: ExecutionContext, params: Int): Int {
        val ctx = executionContext.withTaskId(taskID())
        ctx.log(logLevel = LogLevel.INFO, msg = "Calculating square of $params")
        return params.times(params)
    }

    override fun description(): String {
        return "An example Task that calculates the square of a number"
    }

    override fun examples(): List<TaskExample<Int, Int>> {
        val input = DefaultTaskExampleData<Int>(2)
        val output = DefaultTaskExampleData<Int>(4)
        return listOf(DefaultTaskExample<Int, Int>("two sqaured",
                input, output))
    }
}

class CalcSquareAsyncTask(registry: Registry, private val delayMs: Long = 1000) : BaseTask(), AsyncTask<Int, Int> {

    private val executors = registry.get(ExecutorFactory::class.java).executorService()

    override fun exec(executionContext: ExecutionContext, num: Int): Future<Int> {
        val ctx = executionContext.withTaskId(taskID())
        ctx.log(logLevel = LogLevel.INFO, msg = "Calculating square of $num")
        return executors.submit<Int> {
            Thread.sleep(delayMs)
            num * num
        }
    }
}

class CalcSquareAsync2Task(registry: Registry) : BaseTask(), AsyncTask<Int, Int> {

    private val executors = registry.get(ExecutorFactory::class.java).executorService()

    override fun exec(executionContext: ExecutionContext, params: Int): Future<Int> {
        val ctx = executionContext.withTaskId(taskID())
        ctx.log(logLevel = LogLevel.INFO, msg = "Calculating square of $params")
        return executors.submit<Int> {
            Thread.sleep(10)
            params * params
        }
    }
}


class ExceptionGeneratingAsyncTask(registry: Registry) : BaseTask(), AsyncTask<String, String> {

    private val executors = registry.get(ExecutorFactory::class.java).executorService()

    override fun exec(executionContext: ExecutionContext, params: String): Future<String> {
        val ctx = executionContext.withTaskId(taskID())
        ctx.log(logLevel = LogLevel.INFO, msg = "Message is '$params'")
        return executors.submit<String> {
            if (params.contains("exception", ignoreCase = true)) throw RuntimeException(params)
            Thread.sleep(10)
            params
        }
    }
}

class FileTask : BaseTask(), BlockingTask<File, Int> {
    override fun exec(executionContext: ExecutionContext, file: File): Int {
        val ctx = executionContext.withTaskId(taskID())
        ctx.log(logLevel = LogLevel.INFO, msg = "Loading file $file")
        return file.readBytes().size
    }
}

class UnitTask : BaseUnitBlockingTask<String>() {
    override fun exec(executionContext: ExecutionContext, params: String) {
        val ctx = executionContext.withTaskId(taskID())
        ctx.log(logLevel = LogLevel.INFO, msg = "Params are: $params")
    }
}

class PrintStreamTask : BaseUnitBlockingTask<String>() {
    override fun exec(executionContext: ExecutionContext, params: String) {
        executionContext.stdout().println(params)
    }
}




