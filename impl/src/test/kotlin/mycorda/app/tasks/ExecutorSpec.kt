package mycorda.app.tasks



import mycorda.app.registry.Registry
import mycorda.app.tasks.demo.CalcSquareTask
import mycorda.app.tasks.logging.ConsoleLogMessageSink
import mycorda.app.tasks.logging.InMemoryLogMessageSink
import mycorda.app.tasks.logging.LogFormat
import mycorda.app.tasks.processManager.ProcessManager
import org.junit.platform.runner.JUnitPlatform
import org.junit.runner.RunWith
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

@RunWith(JUnitPlatform::class)
object ExecutorSpec : Spek({

    val messageSink = InMemoryLogMessageSink(LogFormat.Test)

    beforeEachTest {
        messageSink.clear()
    }

    describe("Running a simple Task") {

        val registry = Registry().store(SingleThreadedExecutor())
                .store(ProcessManager())
                .store(ConsoleLogMessageSink())

        val executor = DefaultTaskExecutor<Int, Int>(registry)

        val task = CalcSquareTask()
        val result = executor.exec(task, params = 9)


    }
})