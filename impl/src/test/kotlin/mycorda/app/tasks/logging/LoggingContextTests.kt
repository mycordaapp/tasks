package mycorda.app.tasks.logging

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import mycorda.app.registry.Registry
import org.junit.Test
import java.util.*

class LoggingContextTests {
    val executionId = UUID.randomUUID()
    val debug = LogMessage(executionId = executionId, level = LogLevel.DEBUG, body = "Debug Message")
    val info = LogMessage(executionId = executionId, level = LogLevel.INFO, body = "Info Message")
    val warn = LogMessage(executionId = executionId, level = LogLevel.WARN, body = "Warn Message")
    val error = LogMessage(executionId = executionId, level = LogLevel.ERROR, body = "Error Message")

    @Test
    fun `create logging context with default wiring`() {
        // note, these go stdout and allow INFO and above
        val loggingContext = DefaultLoggingContext()
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
        val loggingContext = DefaultLoggingContext(registry)
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
                        "WARN Warn Message\n" +
                        "ERROR Error Message"
            )
        )
    }
}