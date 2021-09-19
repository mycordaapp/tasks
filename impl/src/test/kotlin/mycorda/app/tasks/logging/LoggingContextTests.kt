package mycorda.app.tasks.logging

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import mycorda.app.registry.Registry
import org.junit.Test
import java.io.PrintStream
import java.util.*

class LoggingContextTests {
    private val executionId = UUID.randomUUID()
    private val debug = LogMessage.debug("Debug Message", executionId)
    private val info = LogMessage.info("Info Message", executionId)
    private val warn = LogMessage.warn("Warning Message", executionId)
    private val error = LogMessage.error("Error Message", executionId)

    @Test
    fun `should capture print stream and stored as stdout`() {
        val logConsumerContext = InMemoryLoggingConsumerContext()
        val captured = CapturedOutputStream(logConsumerContext)
        val ps = PrintStream(captured)

        ps.println("Hello World")
        ps.print("Goodbye, ")
        ps.println("cruel World")
        println(logConsumerContext.stdout())
        assertThat(logConsumerContext.stdout(), equalTo("Hello World\nGoodbye, cruel World\n"))
        assertThat(logConsumerContext.stderr(), equalTo(""))
    }

    @Test
    fun `should capture print stream and stored as stderr`() {
        val logConsumerContext = InMemoryLoggingConsumerContext()
        val captured = CapturedOutputStream(logConsumerContext, false)
        val ps = PrintStream(captured)

        ps.println("Opps, it went wrong!")
        assertThat(logConsumerContext.stderr(), equalTo("Opps, it went wrong!\n"))
        assertThat(logConsumerContext.stdout(), equalTo(""))
    }

    @Test
    fun `should connect InMemoryLoggingConsumerContext to InMemoryLoggingProducerContext`() {
        // simulate both side in memory
        val logConsumerContext = InMemoryLoggingConsumerContext()
        val logProducerContext = InMemoryLoggingProducerContext(logConsumerContext)

        // test stdout
        logProducerContext.stdout().println("Hello World")
        assertThat(logConsumerContext.stdout(), equalTo("Hello World\n"))

        // test stderr
        logProducerContext.stderr().println("Opps!")
        assertThat(logConsumerContext.stderr(), equalTo("Opps!\n"))

        // test log messages
        logProducerContext.logger().accept(info)
        logProducerContext.log(warn)
        assertThat(logConsumerContext.messages(), equalTo(listOf(info, warn)))
    }

    @Test
    fun `create logging context with default wiring`() {
        // note, these go stdout and allow INFO and above
        val loggingContext = InjectableLoggingProducerContext()
        loggingContext.log(debug)
        loggingContext.log(info)
        loggingContext.log(warn)
        loggingContext.log(error)
    }

    @Test
    fun `create logging context with explicit  wiring`() {
        // 1. setup the registry
        val registry = Registry()
        registry.store(LogFormat.Simple)
            .store(DefaultStringLogFormatter())
            .store(InMemoryLogMessageSink(registry))

        // 2. log some messages
        val loggingContext = InjectableLoggingProducerContext(registry)
        loggingContext.log(debug)
        loggingContext.log(info)
        loggingContext.log(warn)
        loggingContext.log(error)

        // 3. validate captured messages
        val inMemory = registry.get(InMemoryLogMessageSink::class.java)
        assertThat(inMemory.messages().size, equalTo(3))
        assertThat(inMemory.messages(), equalTo(listOf(info, warn, error)))
        assertThat(
            inMemory.toString(), equalTo(
                "INFO Info Message\n" +
                        "WARN Warning Message\n" +
                        "ERROR Error Message"
            )
        )
    }
}