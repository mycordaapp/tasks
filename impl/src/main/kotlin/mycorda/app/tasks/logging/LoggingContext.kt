package mycorda.app.tasks.logging

import mycorda.app.registry.Registry
import mycorda.app.tasks.client.ClientContext
import java.io.OutputStream
import java.io.PrintStream
import java.util.*
import java.util.function.Consumer
import kotlin.collections.ArrayList

/**
 * A common logging message and supporting classes plus access to stdout and stderr. The basis concept
 * is that any Task write and consumes log messages in a standard way, but underneath different logging
 * frameworks can be plugged in. Although not yet included, the plan is to integrate in distributed
 * tracing concepts for use with tools like Zipkin (https://zipkin.io/).
 *
 * On the producer side there is [mycorda.app.tasks.logging.LoggingProducerContext]
 * On the consumer (client) side there is [LoggingConsumerContext]
 *
 * The design allows for the producer and consumer to be running in separate processes
 * with communication over commons protocols including https and Kafka.
 *
 * For local development and unit tests there is `InMemoryLoggingProducerContext` and `InMemoryLoggingConsumerContext`
 *
 */


/**
 * The Logging Context for generation of output.
 * This is passed to the task's exec() method in the ExecutionContext
 */
interface LoggingProducerContext {
    /**
     * Abstract generating a log message
     */
    fun logger(): LogMessageSink

    /**
     * Abstract writing to the console
     */
    fun stdout(): PrintStream

    /**
     * Abstract writing to the error stream
     */
    fun stderr(): PrintStream

    /**
     * Shortcut for writing a log message. The message must be fully
     * constructed manually in order to correctly record within the distributed
     * logging framework
     */
    fun log(msg: LogMessage) {
        logger().accept(msg)
    }

    fun log(clientContext: ClientContext, msg: LogMessage) {
        logger().accept(msg)
    }

//    /**
//     * Shortcut for writing to the stdout console
//     */
//    fun println(line: String) {
//        stdout().println(line)
//    }
//
//    /**
//     * Shortcut for writing to the stderr console
//     */
//    fun printErrLn(line: String) {
//        stderr().println(line)
//    }

}

/**
 * The consumer of a logging context. This is on the client
 * side. There is a design assumption that the 'channel' linking the
 * the LoggingProducerContext to the LoggingConsumerContext will be as
 * timely as is reasonably possible.
 */
interface LoggingConsumerContext {
    fun acceptLog(msg: LogMessage)
    fun acceptStdout(output: String)
    fun acceptStderr(error: String)
}

interface LoggingContext {
    fun consumer(): LoggingConsumerContext
    fun producer(): LoggingProducerContext

    companion object {
        fun inMemory(): InMemoryLoggingContext {
            return InMemoryLoggingContext()
        }

        fun console(): ConsoleLoggingContext {
            return ConsoleLoggingContext()
        }

        class InMemoryLoggingContext : LoggingContext {
            private val consumer = InMemoryLoggingConsumerContext()
            private val producer = InMemoryLoggingProducerContext(consumer)
            override fun consumer(): LoggingConsumerContext = consumer

            override fun producer(): LoggingProducerContext = producer

            fun stdout(): String = consumer.stdout()

            fun stderr(): String = consumer.stderr()

            fun messages(): List<LogMessage> = consumer.messages()
        }

        class NoOpConsumer : LoggingConsumerContext {
            override fun acceptLog(msg: LogMessage) {}

            override fun acceptStdout(output: String) {}


            override fun acceptStderr(error: String) {}


        }

        class ConsoleLoggingContext : LoggingContext {
            private val consumer = NoOpConsumer()
            private val producer = ConsoleLoggingProducerContext()
            override fun consumer(): LoggingConsumerContext = consumer
            override fun producer(): LoggingProducerContext = producer
        }
    }
}


enum class LogLevel { DEBUG, INFO, WARN, ERROR }

/**
 * There are a fixed set of format supported. If something custom
 * is needed, the downstream system is responsible
 */
enum class LogFormat {
    Full, /* All fields in Java style */
    Test,   /* Timestamp excluded and IDs masked, good for use in test cases*/
    Simple, /* A short format. Useful for test cases */
}

/**
 * Format the log as string
 */
interface StringLogFormatter {
    fun toString(msg: LogMessage, format: LogFormat): String
}

/**
 * A standard String format that is human readable
 */
class DefaultStringLogFormatter : StringLogFormatter {
    override fun toString(msg: LogMessage, format: LogFormat): String {
        return when (format) {
            LogFormat.Simple -> {
                val buffer = StringBuilder()
                buffer.append(msg.level)
                buffer.append(" ")
                buffer.append(msg.body)
                return buffer.toString()
            }
            LogFormat.Test -> {
                val buffer = StringBuilder()
                buffer.append("level=${msg.level}, message=${msg.body}")
                if (msg.taskId != null) {
                    buffer.append(", taskId=xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxx")
                }
                buffer.toString()
            }
            LogFormat.Full -> {
                val buffer = StringBuilder()
                buffer.append("level=${msg.level}, message=${msg.body}, executionId=${msg.executionId}")
                if (msg.taskId != null) {
                    buffer.append(", taskId=${msg.taskId}")
                }
                buffer.append(", timestamp=${msg.timestamp}")
                buffer.toString()
            }
        }
    }
}

/**
 * The common attributes in a single log message
 */
data class LogMessage(
    /*
     * Every log message is linked to an executionId. The key principle
     * is that for a set of related task they are linked by a single executionId,
     * for example if a higher level service needed to run TaskA, TaskB and TaskC in
     * order, it would link them with the same executionId
     *
     * This is analagous to the concept of the 'traceId' in Zipkin
     *
     * not sure this is a good name :(
     */
    val executionId: UUID,

    val level: LogLevel,

    val body: String,
    val timestamp: Long = System.currentTimeMillis(),
    val taskId: UUID? = null    // is this useful ? do
) {
    companion object {
        fun debug(body: String, executionId: UUID = UUID.randomUUID()): LogMessage = LogMessage(
            executionId = executionId,
            body = body,
            level = LogLevel.DEBUG
        )

        fun info(body: String, executionId: UUID = UUID.randomUUID()): LogMessage = LogMessage(
            executionId = executionId,
            body = body,
            level = LogLevel.INFO
        )

        fun warn(body: String, executionId: UUID = UUID.randomUUID()): LogMessage = LogMessage(
            executionId = executionId,
            body = body,
            level = LogLevel.WARN
        )

        fun error(body: String, executionId: UUID = UUID.randomUUID()): LogMessage = LogMessage(
            executionId = executionId,
            body = body,
            level = LogLevel.ERROR
        )
    }
}


/**
 * We always write to a sink.
 */
interface LogMessageSink : Consumer<LogMessage>

/**
 * The very basic default sink that simply writes to the console. All configuration is
 * via the registry. If settings are missing, the following defaults are used:
 *
 *  - format is LogFormat.Simple
 *  - formatter is DefaultStringLogFormatter
 */
class ConsoleLogMessageSink(registry: Registry = Registry()) :
    LogMessageSink {
    private val formatter = registry.geteOrElse(StringLogFormatter::class.java, DefaultStringLogFormatter())
    private val format = registry.geteOrElse(LogFormat::class.java, LogFormat.Simple)
    override fun accept(msg: LogMessage) {
        println(formatter.toString(msg, format))
    }
}

class ConsoleLoggingConsumerContext() :
    LoggingConsumerContext {
    override fun acceptLog(msg: LogMessage) {
        println(msg)
    }

    override fun acceptStdout(output: String) {
        print(output)
    }

    override fun acceptStderr(error: String) {
        System.err.print(error)
    }
}

/**
 * A standard interface for readers of the logging
 */
interface LoggingReaderContext {
    fun stdout(): String
    fun stderr(): String
    fun messages(): List<LogMessage>
}

class InMemoryLoggingConsumerContext : LoggingConsumerContext, LoggingReaderContext {
    private val stdout = StringBuilder()
    private val stderr = StringBuilder()
    private val logMessages = ArrayList<LogMessage>()

    override fun acceptLog(msg: LogMessage) {
        logMessages.add(msg)
    }

    override fun acceptStdout(output: String) {
        stdout.append(output).append("\n")
    }

    override fun acceptStderr(error: String) {
        stderr.append(error).append("\n")
    }

    override fun stdout(): String = stdout.toString()

    override fun stderr(): String = stderr.toString()

    override fun messages(): List<LogMessage> = ArrayList(logMessages)
}

class InMemoryLoggingProducerContext(private val consumer: LoggingConsumerContext) : LoggingProducerContext {
    private val stdout: PrintStream = PrintStream(CapturedOutputStream(consumer, true))
    private val stderr: PrintStream = PrintStream(CapturedOutputStream(consumer, false))

    override fun logger(): LogMessageSink =
        object : LogMessageSink {
            override fun accept(m: LogMessage) {
                consumer.acceptLog(m)
            }
        }

    override fun stdout(): PrintStream = stdout

    override fun stderr(): PrintStream = stderr
}


/**
 * Simple class for capturing an input stream and passing onto a
 * LoggingConsumerContext.
 */
class CapturedOutputStream(
    private val loggingConsumerContext: LoggingConsumerContext,
    private val isStdout: Boolean = true
) : OutputStream() {
    private var data = StringBuffer()

    override fun write(p0: Int) {
        if (p0 == 10) { // newline
            if (isStdout) {
                loggingConsumerContext.acceptStdout(data.toString())
            } else {
                loggingConsumerContext.acceptStderr(data.toString())
            }
            data = StringBuffer()
        } else {
            data.append(p0.toChar())
        }
    }
}


/**
 * Everything is directed to stdout & stderr
 */
class ConsoleLoggingProducerContext : LoggingProducerContext {
    private val sink = ConsoleLogMessageSink()

    override fun logger(): LogMessageSink = sink

    override fun stdout(): PrintStream = System.out

    override fun stderr(): PrintStream = System.err
}

