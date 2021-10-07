package mycorda.app.tasks.client

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.isEmpty
import com.natpryce.hamkrest.isEmptyString
import com.natpryce.hamkrest.throws
import mycorda.app.registry.Registry
import mycorda.app.tasks.*
import mycorda.app.tasks.demo.CalcSquareTask
import mycorda.app.tasks.demo.ExceptionGeneratingBlockingTask
import mycorda.app.tasks.demo.echo.*
import mycorda.app.tasks.logging.LoggingReaderContext
import mycorda.app.tasks.test.ListDirectoryTask
import mycorda.app.tasks.test.ListDirectoryTaskFake
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SimpleTaskClientTests : BaseTaskClientTest() {

    private val registry = Registry()

    init {
        val factory = TaskFactory()
        factory.register(CalcSquareTask::class)
        factory.register(EchoIntTask::class)
        factory.register(EchoLongTask::class)
        factory.register(EchoDoubleTask::class)
        factory.register(EchoFloatTask::class)
        factory.register(EchoBigDecimalTask::class)
        factory.register(EchoBooleanTask::class)
        factory.register(EchoStringTask::class)
        factory.register(EchoUUIDTask::class)
        factory.register(EchoEnumTask::class)
        factory.register(EchoDemoModelTask::class)
        factory.register(EchoToStdOutTask::class)
        factory.register(EchoToStdErrTask::class)
        factory.register(EchoToLogTask::class)
        factory.register(ListDirectoryTaskFake::class, ListDirectoryTask::class)
        factory.register(ExceptionGeneratingBlockingTask::class)
        registry.store(factory)
    }

    @Test
    fun `should call task and return output`() {
        val clientContext = SimpleClientContext()
        val result = SimpleTaskClient(registry).execBlocking(
            clientContext,
            "mycorda.app.tasks.demo.echo.EchoStringTask",
            "Hello, world",
            String::class
        )

        assertThat(result, equalTo("Hello, world"))
        assertNoOutput(clientContext)
    }

    @Test
    fun `should pass on task exception`() {
        val clientContext = SimpleClientContext()
        assertThat({
            SimpleTaskClient(registry).execBlocking(
                clientContext,
                "mycorda.app.tasks.demo.ExceptionGeneratingBlockingTask",
                "opps",
                String::class
            )
        }, throws<RuntimeException>())

        assertPartialLogMessage(clientContext, "opps")
    }

    @Test
    fun `should return stdout to client`() {
        val clientContext = SimpleClientContext()
        SimpleTaskClient(registry).execBlocking(
            clientContext,
            "mycorda.app.tasks.demo.echo.EchoToStdOutTask",
            "Hello, world\n",
            Unit::class
        )

        val readerContext: LoggingReaderContext = clientContext.inMemoryLoggingContext()
        assertThat(readerContext.stdout(), equalTo("Hello, world\n"))
        assertThat(readerContext.stderr(), isEmptyString)
        assertThat(readerContext.messages(), isEmpty)
    }

    @Test
    fun `should return stderr to client`() {
        val clientContext = SimpleClientContext()
        SimpleTaskClient(registry).execBlocking(
            clientContext,
            "mycorda.app.tasks.demo.echo.EchoToStdErrTask",
            "Goodbye, cruel world\n",
            Unit::class
        )

        val readerContext: LoggingReaderContext = clientContext.inMemoryLoggingContext()
        assertThat(readerContext.stdout(), isEmptyString)
        assertThat(readerContext.stderr(), equalTo("Goodbye, cruel world\n"))
        assertThat(readerContext.messages(), isEmpty)
    }

    @Test
    fun `should pass on exception to client`() {
        val clientContext = SimpleClientContext()

        try {
            SimpleTaskClient(registry).execBlocking(
                clientContext,
                "mycorda.app.tasks.demo.ExceptionGeneratingBlockingTask",
                "Opps",
                String::class
            )
            fail("should have thrown a RuntimeException")
        } catch (re: RuntimeException) {
            re.printStackTrace()
            assertThat(re.message, equalTo("Opps"))
        }


    }

}