package mycorda.app.tasks

import mycorda.app.registry.Registry
import mycorda.app.tasks.demo.BaseTask
import mycorda.app.tasks.executionContext.DefaultExecutionContextModifier
import mycorda.app.tasks.executionContext.ExecutionContext
import mycorda.app.tasks.logging.LogMessage
import java.io.File
import java.net.URL
import java.util.*



interface ExampleTask : BlockingTask<Int, Int>

class ExampleTaskImpl : ExampleTask, BaseBlockingTask<Int, Int>() {
    override fun exec(executionContext: ExecutionContext, params: Int): Int {
        return params + params
    }
}

class ExampleTaskFake : ExampleTask, BaseBlockingTask<Int, Int>() {
    override fun exec(executionContext: ExecutionContext, params: Int): Int {
        val out = executionContext.stdout()
        out.println("MyTask:")
        out.println("   params: ${params}")
        return 99
    }
}

interface ListDirectoryTask : BlockingTask<String, List<String>>

class ListDirectoryTaskImpl : ListDirectoryTask, BaseBlockingTask<String, List<String>>() {
    override fun exec(executionContext: ExecutionContext, params: String): List<String> {
        return File(params).listFiles().map { it.name }
    }
}

class ListDirectoryTaskFake : ListDirectoryTask, BaseBlockingTask<String, List<String>>() {
    override fun exec(executionContext: ExecutionContext, params: String): List<String> {
        val out = executionContext.stdout()
        out.println("ListDirectoryTask:")
        out.println("   params: $params")
        executionContext.log(LogMessage.info("listing directory '$params'"))
        return listOf("fake.txt")
    }
}


class RegistryTask(private val registry: Registry) : BaseTask(), BlockingTask<Unit?, String> {

    override fun exec(ctx: ExecutionContext, params: Unit?): String {
        return registry.get(String::class.java)
    }
}


data class Params(val p1: String, val p2: Int)
class ParamsTask() : BaseTask(), UnitBlockingTask<Params> {
    override fun exec(ctx: ExecutionContext, params: Params) {
        ctx.log(LogMessage.info("called with params $params"))
    }
}

enum class Colour { Red, Green, Blue }
class EnumTask() : BaseTask(), UnitBlockingTask<Colour> {
    override fun exec(ctx: ExecutionContext, params: Colour) {
        ctx.log(LogMessage.info("called with params $params"))
    }
}

data class ParamsWithDefault(val p1: String, val p2: Int = 99, val p3: String = "foo")
class ParamsWithDefaultTask() : BaseTask(), BlockingTask<ParamsWithDefault, ParamsWithDefault> {
    override fun exec(ctx: ExecutionContext, params: ParamsWithDefault): ParamsWithDefault {
        ctx.log(LogMessage.info("called with params $params"))
        return params
    }
}

class MapTask() : BaseTask(), UnitBlockingTask<Map<String, Any>> {
    override fun exec(ctx: ExecutionContext, params: Map<String, Any>) {
        ctx.log(LogMessage.info("called with params $params"))
    }
}

class NoParamTask() : BaseTask(), UnitBlockingTask<Nothing?> {
    override fun exec(executionContext: ExecutionContext, params: Nothing?) {
        executionContext.log(LogMessage.info("called with params $params"))
    }
}

class NotRequiredParamTask() : BaseTask(), UnitBlockingTask<NotRequired> {
    override fun exec(ctx: ExecutionContext, params: NotRequired) {
        ctx.log(LogMessage.info("called with params $params"))
    }
}

class FileTask : BaseTask(), BlockingTask<File, Int> {
    override fun exec(ctx: ExecutionContext, params: File): Int {
        val ctx = updatedCtx(ctx)
        ctx.log(LogMessage.info("Loading file $params"))
        return params.readBytes().size
    }
}


class URLTask : BaseTask(), BlockingTask<URL, String> {
    override fun exec(ctx: ExecutionContext, params: URL): String {
        val ctx = updatedCtx(ctx)
        ctx.log(LogMessage.info("Loading url $params"))
        return params.toExternalForm()
    }
}


data class ParamsWithFile(val file: File, val files: List<File>)
class ParamsWithFileTask() : BaseTask(), BlockingTask<ParamsWithFile, ParamsWithFile> {
    override fun exec(ctx: ExecutionContext, params: ParamsWithFile): ParamsWithFile {
        ctx.log(LogMessage.info("called with params $params"))
        return params
    }
}


// testing of sealed classes
sealed class DatabaseConfig

data class PostgresConfig(val postgres: String) : DatabaseConfig()
data class OracleConfig(val oracle: String) : DatabaseConfig()

class DatabaseTask() : BaseTask(), BlockingTask<DatabaseConfig, DatabaseConfig> {
    override fun exec(ctx: ExecutionContext, params: DatabaseConfig): DatabaseConfig {
        ctx.log(LogMessage.info("called with params $params"))
        return params
    }
}

// emulates a task that reads some status information. after a period
// of time that status will change, e.g. a system might go from "starting" to "running" status
class StatusChangeTask<I, O>(private val before: O, private val after: O, private val delay: Long = 1000) :
    BaseBlockingTask<I, O>() {

    private val startTime = System.currentTimeMillis()
    override fun exec(ctx: ExecutionContext, params: I): O {
        return if (System.currentTimeMillis() < (startTime + delay)) before else after
    }

}


fun unitfunction(): Unit {
    return
}