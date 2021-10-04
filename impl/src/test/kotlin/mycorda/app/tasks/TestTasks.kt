package mycorda.app.tasks.test

import mycorda.app.registry.Registry
import mycorda.app.tasks.*
import mycorda.app.tasks.executionContext.ExecutionContext
import mycorda.app.tasks.logging.LogMessage
import java.io.File
import java.net.URL

interface ListDirectoryTask : BlockingTask<String, StringList>

class ListDirectoryTaskImpl : ListDirectoryTask, BaseBlockingTask<String, StringList>() {
    override fun exec(ctx: ExecutionContext, input: String): StringList {
        val data = File(input).listFiles().map { it.name }
        return StringList(data)
    }
}

class ListDirectoryTaskFake : ListDirectoryTask, BaseBlockingTask<String, StringList>() {
    override fun exec(ctx: ExecutionContext, input: String): StringList {
        val out = ctx.stdout()
        out.println("ListDirectoryTask:")
        out.println("   params: $input")
        ctx.log(LogMessage.info("listing directory '$input'"))
        return StringList(listOf("fake.txt"))
    }
}


class RegistryTask(private val registry: Registry) : BaseBlockingTask<Unit?, String>() {
    override fun exec(ctx: ExecutionContext, input: Unit?): String {
        return registry.get(String::class.java)
    }
}


data class Params(val p1: String, val p2: Int)
class ParamsTask() : BaseBlockingTask<Params, Unit>(), UnitBlockingTask<Params> {
    override fun exec(ctx: ExecutionContext, input: Params) {
        ctx.log(LogMessage.info("called with params $input"))
    }
}

enum class Colour { Red, Green, Blue }
class EnumTask() : BaseBlockingTask<Colour, Unit>(), UnitBlockingTask<Colour> {
    override fun exec(ctx: ExecutionContext, input: Colour) {
        ctx.log(LogMessage.info("called with params $input"))
    }
}

data class ParamsWithDefault(val p1: String, val p2: Int = 99, val p3: String = "foo")
class ParamsWithDefaultTask() : BaseBlockingTask<ParamsWithDefault, ParamsWithDefault>() {
    override fun exec(ctx: ExecutionContext, input: ParamsWithDefault): ParamsWithDefault {
        ctx.log(LogMessage.info("called with params $input"))
        return input
    }
}

class MapTask() : BaseBlockingTask<Map<String, Any>, Unit>(), UnitBlockingTask<Map<String, Any>> {
    override fun exec(ctx: ExecutionContext, input: Map<String, Any>) {
        ctx.log(LogMessage.info("called with params $input"))
    }
}

class NoParamTask() : BaseBlockingTask<Nothing?, Unit>(), UnitBlockingTask<Nothing?> {
    override fun exec(ctx: ExecutionContext, input: Nothing?) {
        ctx.log(LogMessage.info("called with params $input"))
    }
}

class NotRequiredParamTask() : BaseBlockingTask<NotRequired, Unit>(), UnitBlockingTask<NotRequired> {
    override fun exec(ctx: ExecutionContext, input: NotRequired) {
        ctx.log(LogMessage.info("called with params $input"))
    }
}

class FileTask : BaseBlockingTask<File, Int>() {
    override fun exec(ctx: ExecutionContext, input: File): Int {
        val modifiedCtx = ctxWithTaskID(ctx)
        modifiedCtx.log(LogMessage.info("Loading file $input"))
        return input.readBytes().size
    }
}


class URLTask : BaseBlockingTask<URL, String>() {
    override fun exec(ctx: ExecutionContext, input: URL): String {
        val modifiedCtx = ctxWithTaskID(ctx)
        modifiedCtx.log(LogMessage.info("Loading url $input"))
        return input.toExternalForm()
    }
}


data class ParamsWithFile(val file: File, val files: List<File>)
class ParamsWithFileTask() : BaseBlockingTask<ParamsWithFile, ParamsWithFile>() {
    override fun exec(ctx: ExecutionContext, input: ParamsWithFile): ParamsWithFile {
        ctx.log(LogMessage.info("called with params $input"))
        return input
    }
}


// testing of sealed classes
sealed class DatabaseConfig

data class PostgresConfig(val postgres: String) : DatabaseConfig()
data class OracleConfig(val oracle: String) : DatabaseConfig()

class DatabaseTask() : BaseBlockingTask<DatabaseConfig, DatabaseConfig>() {
    override fun exec(ctx: ExecutionContext, input: DatabaseConfig): DatabaseConfig {
        ctx.log(LogMessage.info("called with params $input"))
        return input
    }
}

// emulates a task that reads some status information. after a period
// of time that status will change, e.g. a system might go from "starting" to "running" status
class StatusChangeTask<I, O>(private val before: O, private val after: O, private val delay: Long = 1000) :
    BaseBlockingTask<I, O>() {

    private val startTime = System.currentTimeMillis()
    override fun exec(ctx: ExecutionContext, input: I): O {
        return if (System.currentTimeMillis() < (startTime + delay)) before else after
    }

}

