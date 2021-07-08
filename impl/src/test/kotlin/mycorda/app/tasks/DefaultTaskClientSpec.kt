package mycorda.app.tasks

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.greaterThanOrEqualTo
import com.natpryce.hamkrest.lessThan
import mycorda.app.registry.Registry
import mycorda.app.tasks.demo.CalcSquareAsync2Task
import mycorda.app.tasks.demo.CalcSquareTask
import mycorda.app.tasks.demo.PrintStreamTask
import mycorda.app.tasks.demo.UnitTask
import mycorda.app.tasks.executionContext.DefaultExecutionContextFactory
import mycorda.app.tasks.logging.InMemoryLogMessageSink
import mycorda.app.tasks.processManager.ProcessManager
import org.junit.platform.runner.JUnitPlatform
import org.junit.runner.RunWith
import org.spekframework.spek2.Spek
import org.spekframework.spek2.meta.Ignore
import org.spekframework.spek2.style.specification.describe
import java.util.*


@Ignore
@RunWith(JUnitPlatform::class)
object DefaultTaskClientSpec : Spek({
    val registry = Registry()
    val msgSink = InMemoryLogMessageSink()
    registry.store(msgSink)
    registry.store(ProcessManager())
    registry.store(FixedThreadPoolExecutor())
    registry.store(DefaultExecutionContextFactory(registry))
    //val es = FileEventStore()
    //registry.store(es)

    val factory = TaskFactory(registry)
    factory.register(CalcSquareTask::class)
    factory.register(CalcSquareAsync2Task::class)
    factory.register(UnitTask::class)
    factory.register(PrintStreamTask::class)
    registry.store(factory)

    val executionContextFactory = DefaultExecutionContextFactory(registry)


    describe("Test the Default Client ") {

        beforeEachTest {
            //es.truncate()
        }

        // TODO  - these are only simple happy path tests at the moment

        it("should return task result") {
            val client = DefaultTaskClient(registry)
            val ctx = executionContextFactory.get()
            val result = client.exec(ctx, CalcSquareTask::class, 9)

            assertThat(result, equalTo(81))
        }

        it("should call unit task") {
            val client = DefaultTaskClient(registry)
            val ctx = executionContextFactory.get()
            client.execUnit(ctx, UnitTask::class, "ignore me")

        }

        it("should return async task result") {
            val client = DefaultTaskClient(registry)
            val ctx = executionContextFactory.get()
            val result = client.execAsync(ctx, CalcSquareAsync2Task::class, 9, Int::class)

            assertThat(result, equalTo(81))
        }

        it("should return captured stdout") {
            val client = DefaultTaskClient(registry)
            val ctx = executionContextFactory.get()
            client.exec(ctx, PrintStreamTask::class, "Mary had a little lamb")

            assertThat(client.capturedStdOut(), equalTo("Mary had a little lamb\n"))
        }

        it("should identify concurrent async tasks using correlation id ") {
            val client = DefaultTaskClient(registry)
            val ctx = executionContextFactory.get()
            val correlationId1 = UUID.randomUUID()
            val correlationId2 = UUID.randomUUID()

            client.execAsyncWithCorrelationId(ctx, correlationId1, CalcSquareAsync2Task::class, 9, Int::class)
            client.execAsyncWithCorrelationId(ctx, correlationId2, CalcSquareAsync2Task::class, 10, Int::class)
            assert(!client.isDone(correlationId1))
            assert(!client.isDone(correlationId2))

            // let them complete
            Thread.sleep(50)

            assert(client.isDone(correlationId1))
            assert(client.isDone(correlationId2))
            assertThat(client.result(correlationId1), equalTo(81))
            assertThat(client.result(correlationId2), equalTo(100))
        }


        it("should ignore subsequent attempts to use the same correlationId") {
            val client = DefaultTaskClient(registry)
            val ctx = executionContextFactory.get()
            val correlationId = UUID.randomUUID()

            client.execAsyncWithCorrelationId(ctx, correlationId, CalcSquareAsync2Task::class, 9, Int::class)
            client.execAsyncWithCorrelationId(ctx, correlationId, CalcSquareAsync2Task::class, 99, Int::class)

            // let it complete, then we should see the result from the first invocation
            Thread.sleep(50)
            assert(client.isDone(correlationId))
            assertThat(client.result(correlationId), equalTo(81))
        }

        it("should return the running time") {
            val client = DefaultTaskClient(registry)
            val ctx = executionContextFactory.get()
            val correlationId = UUID.randomUUID()

            client.execAsyncWithCorrelationId(ctx, correlationId, CalcSquareAsync2Task::class, 9, Int::class)

            // let it complete
            Thread.sleep(50)
            assert(client.isDone(correlationId))

            // the current implementation only has crude logic. We know that the
            // time taken will be at least the delay until we check the status
            val timeTaken = client.runningTimeMs(correlationId)
            assertThat(timeTaken, greaterThanOrEqualTo(50L))
            assertThat(timeTaken, lessThan(100L))

            // timer should have stopped, if we ask again
            Thread.sleep(10)
            assertThat(client.runningTimeMs(correlationId), equalTo(timeTaken))
        }


    }
})


