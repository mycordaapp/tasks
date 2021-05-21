package mycorda.app.tasks

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import junit.framework.Assert.fail
import mycorda.app.registry.Registry
import mycorda.app.tasks.demo.CalcSquareAsyncTask
import mycorda.app.tasks.demo.CalcSquareTask
import mycorda.app.tasks.demo.ExceptionGeneratingAsyncTask
import mycorda.app.tasks.executionContext.DefaultExecutionContextFactory
import mycorda.app.tasks.executionContext.InMemoryLogMessageSink
import mycorda.app.tasks.executionContext.LogFormat
import mycorda.app.tasks.processManager.ProcessManager
import org.junit.platform.runner.JUnitPlatform
import org.junit.runner.RunWith
import org.spekframework.spek2.Spek
import org.spekframework.spek2.meta.Ignore
import org.spekframework.spek2.style.specification.describe
import java.util.*
import java.util.concurrent.Future

@Ignore
@RunWith(JUnitPlatform::class)
object TasksSpec : Spek({

    val messageSink = InMemoryLogMessageSink(LogFormat.Test)

    beforeEachTest {
        messageSink.clear()
    }

    describe("Running a simple Task") {

        val registry = Registry().store(SingleThreadedExecutor())
                .store(ProcessManager())
                .store(messageSink)

        it("should execute CalcSquareTask") {
            val task = CalcSquareTask() as BlockingTask<Int, Int>
            val result = task.exec(params = 9)
            assertThat(result, equalTo(81))
        }

        it("should run CalcSquareTask via executor") {
            // doExec
            val executionId = UUID.randomUUID()
            val ctx = DefaultExecutionContextFactory(registry).get(executionId = executionId, logMessageSink = messageSink)
            val executor = SimpleTaskExecutor<Int, Int>(ctx)
            val task = CalcSquareTask()
            val result = executor.exec(task, params = 9)

            // verify
            assertThat(result, equalTo(81))
            assertThat(messageSink.toString(), equalTo("level=INFO, message=Started CalcSquareTask, taskId=xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxx\n" +
                    "level=INFO, message=Calculating square of 9, taskId=xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxx\n" +
                    "level=INFO, message=Completed CalcSquareTask, taskId=xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxx"))
            assertThat(messageSink.messages().count { it.executionId == executionId }, equalTo(3))
            assertThat(messageSink.messages().count { it.taskId == task.taskID() }, equalTo(3))
        }
    }

    describe("Running an Async Task") {
        val registry = Registry().store(SingleThreadedExecutor())
                .store(ProcessManager())
                .store(messageSink)

        it("executing CalcSquareAsyncTask directly") {
            val task = CalcSquareAsyncTask(registry) as AsyncTask<Int, Int>
            val result = task.exec(params = 9).get()
            assertThat(result, equalTo(81))
            assert(messageSink.messages().isEmpty())
        }

        it("executing CalcSquareAsyncTask via executor") {
            val executionId = UUID.randomUUID()
            val ctx = DefaultExecutionContextFactory(registry).get(executionId = executionId, logMessageSink = messageSink)
            val executor = SimpleTaskExecutor<Int, Int>(ctx)
            val task = CalcSquareAsyncTask(registry)
            val result = executor.exec(task, params = 9) as Future<Int>

            // verify
            assertThat(result.get(), equalTo(81))
            assertThat(messageSink.toString(), equalTo("level=INFO, message=Started CalcSquareAsyncTask, taskId=xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxx\n" +
                    "level=INFO, message=Calculating square of 9, taskId=xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxx\n" +
                    "level=INFO, message=Running Future for CalcSquareAsyncTask, taskId=xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxx\n" +
                    "level=INFO, message=Completed Future for CalcSquareAsyncTask, taskId=xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxx"))
            assertThat(messageSink.messages().count { it.executionId == executionId }, equalTo(4))
            assertThat(messageSink.messages().count { it.taskId == task.taskID() }, equalTo(4))
        }


        it("executing ExceptionGeneratingAsyncTask via executor") {
            val executionId = UUID.randomUUID()
            val ctx = DefaultExecutionContextFactory(registry).get(executionId = executionId, logMessageSink = messageSink)
            val executor = SimpleTaskExecutor<String, String>(ctx)
            val task = ExceptionGeneratingAsyncTask(registry)
            val result = executor.exec(task, params = "Here is the exception") as Future<Int>

            try {
                result.get()
                fail("There should have been an Exception")
            } catch (ignored: Exception) {
            }

            // verify
            val expected = """
                level=INFO, message=Started ExceptionGeneratingAsyncTask, taskId=xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxx
                level=INFO, message=Message is 'Here is the exception', taskId=xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxx
                level=INFO, message=Running Future for ExceptionGeneratingAsyncTask, taskId=xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxx
                level=INFO, message=ExecutionException: java.lang.RuntimeException: Here is the exception, taskId=xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxx
                level=ERROR, message=Future Failed for ExceptionGeneratingAsyncTask, taskId=xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxx""".trimIndent()
            assertThat(messageSink.toString(), equalTo(expected))
            assertThat(messageSink.messages().count { it.executionId == executionId }, equalTo(5))
            assertThat(messageSink.messages().count { it.taskId == task.taskID() }, equalTo(5))

        }
    }

})