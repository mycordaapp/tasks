package mycorda.app.tasks.logging

import mycorda.app.registry.Registry
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
    Test   /* Timestamp excluded and IDs masked, good for use in test cases*/
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
) {
//    fun shortFormat(): String {
//        val sb = StringBuilder()
//        sb.append(SimpleDateFormat("HH:mm:ss.SSS").format(Date(timestamp)))
//        sb.append(String.format("%1$5s", level.name))
//        sb.append(" - ")
//        sb.append(body)
//        return sb.toString()
//    }
}


/**
 * We always write to a sink. There will be
 */
interface LogMessageSink : Consumer<LogMessage>

/**
 * The very basic default sink that simply writes to the console
 */
class ConsoleLogMessageSink(registry: Registry = Registry(), private val format: LogFormat = LogFormat.Full) :
    LogMessageSink {
    private val formatter = registry.geteOrElse(StringLogFormatter::class.java, DefaultStringLogFormatter())
    override fun accept(msg: LogMessage) {
        println(formatter.toString(msg, format))
    }
}

/**
 * Stores in memory to a StringBuilder. Mainly for unit testing
 */
class InMemoryLogMessageSink(
    private val format: LogFormat = LogFormat.Full,
    private val buffer: StringBuilder = StringBuilder()
) : LogMessageSink {
    private val messages = ArrayList<LogMessage>()
    override fun accept(m: LogMessage) {
        messages.add(m)
        if (buffer.isNotEmpty()) buffer.append("\n")

        if (format == LogFormat.Test) {
            buffer.append("level=${m.level}, message=${m.body}")
            if (m.taskId != null) {
                buffer.append(", taskId=xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxx")
            }

        }
        if (format == LogFormat.Full) {
            buffer.append("level=${m.level}, message=${m.body}, executionId=${m.executionId}")
            if (m.taskId != null) {
                buffer.append(", taskId=${m.taskId}")
            }
            buffer.append(", timestamp=${m.timestamp}")
        }
    }

    fun clear() {
        println("clearing out the log message buffer")
        messages.clear()
    }

    fun messages(): List<LogMessage> {
        return messages
    }

    override fun toString(): String {
        return buffer.toString()
    }
}
