package mycorda.app.tasks.logging

import mycorda.app.registry.Registry
import java.io.PrintStream
import java.util.*
import java.util.function.Consumer

/**
 * A common logging message and supporting classes plus access to stdout and stderr. The basis concept
 * is that any Task write and consumes log messages in a standard way, but underneath different logging
 * frameworks can be plugged in. Although not yet included, the plan is to integrate in distributed
 * tracing concepts for use with tools like Zipkin (https://zipkin.io/).
 *
 * As with most services, we wire up using the Registry.
 *  TODO - an example
 *
 * For completeness we also include a "logical" stdout and stderr which should be used in place of
 * the regular println()
 *
 */

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
     */
    val executionId: UUID,

    val level: LogLevel,

    val body: String,
    val timestamp: Long = System.currentTimeMillis(),
    val taskId: UUID? = null
)


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


interface StdoutHolder {
    fun out() : PrintStream
}

interface StderrHolder {
    fun err() : PrintStream
}

class DefaultStdoutHolder : StdoutHolder {
    override fun out(): PrintStream = System.out
}

class DefaultStderrHolder : StderrHolder {
    override fun err(): PrintStream = System.err
}



/**
 * The standard Logging Context
 */
interface LoggingContext {
    /**
     * Abstract generating a log message
     */
    fun log(msg: LogMessage): LoggingContext

    /**
     * Abstract writing to the console
     */
    fun stdout(): PrintStream

    /**
     * Abstract writing to the error stream
     */
    fun stderr(): PrintStream
}

// is this useful
interface LoggingContextBuilder<T> {
    fun withStdout(out: PrintStream) : T
    fun withStderr(out: PrintStream) : T
    fun buildLoggingContext() : T
}

class DefaultLoggingContextBuilder<T : Any> : LoggingContextBuilder<T> {
    override fun withStdout(out: PrintStream): T {
        TODO("Not yet implemented")
    }

    override fun withStderr(out: PrintStream): T {
        TODO("Not yet implemented")
    }

    override fun buildLoggingContext(): T {
        TODO("Not yet implemented")
    }

}

class DefaultLoggingContext(registry: Registry = Registry()) : LoggingContext {
    private val sink = registry.geteOrElse(LogMessageSink::class.java, ConsoleLogMessageSink(registry))
    private val level = registry.geteOrElse(LogLevel::class.java, LogLevel.INFO)
    private val out = registry.geteOrElse(StdoutHolder::class.java, DefaultStdoutHolder())
    private val err = registry.geteOrElse(StderrHolder::class.java, DefaultStderrHolder())

    override fun log(msg: LogMessage): LoggingContext {
        if (msg.level >= level) {
            sink.accept(msg)
        }
        return this
    }

    override fun stdout(): PrintStream = out.out()

    override fun stderr(): PrintStream = err.err()
}

/**
 * Stores in memory. Mainly for unit testing. All configuration is via
 * the registry. If not provided, the following defaults are used:
 *
 *  - level is LogLevel.INFO
 *  - format is LogFormat.Simple
 *  - formatter is DefaultStringLogFormatter
 */
class InMemoryLogMessageSink(
    registry: Registry = Registry()
) : LogMessageSink {
    private val format = registry.geteOrElse(LogFormat::class.java, LogFormat.Simple)
    private val level = registry.geteOrElse(LogLevel::class.java, LogLevel.INFO)
    private val formatter = registry.geteOrElse(StringLogFormatter::class.java, DefaultStringLogFormatter())
    private val messages = ArrayList<LogMessage>()
    override fun accept(msg: LogMessage) {
        if (msg.level >= level) {
            messages.add(msg)
        }
    }

    fun clear() {
        println("clearing out the log message buffer")
        messages.clear()
    }

    fun messages(): List<LogMessage> {
        return messages
    }

    /**
     * Dump out all messages in the format provided
     */
    override fun toString(): String {
        return messages.joinToString(separator = "\n") { formatter.toString(it, format) }
    }
}
