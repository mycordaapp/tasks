package mycorda.app.tasks.executionContext

import mycorda.app.tasks.TaskLogMessage
import mycorda.app.tasks.TaskRepo
import java.text.SimpleDateFormat
import java.util.*
import java.util.function.Consumer

/**
 * A common log message and supporting classes
 */

enum class LogLevel { INFO, WARN, ERROR }

enum class LogFormat {
    Full, /* All fields in Java style */
    Test   /* Timestamp excluded and IDs masked, good for use in test cases*/
}

/**
 * The common LogMessage
 */
data class LogMessage(val executionId: UUID,
                      val level: LogLevel,
                      val message: String,
                      val timestamp: Long = System.currentTimeMillis(),
                      val taskId: UUID? = null,
                      val stepId: UUID? = null) {
    fun shortFormat() : String {
        val sb = StringBuilder()
        sb.append(SimpleDateFormat("HH:mm:ss.SSS").format(Date(timestamp)))
        sb.append(String.format("%1$5s",level.name))
        sb.append(" - ")
        sb.append(message)
        return sb.toString()
    }
}

/**
 * We always write to a sink
 */
interface LogMessageSink : Consumer<LogMessage>

/**
 * The very basic default sink that simply writes to the console
 */
class ConsoleLogMessageSink : LogMessageSink {
    override fun accept(msg: LogMessage) {
        println(msg)
    }
}

/**
 * Linked to a task repo
 */
class TaskRepoLogMessageSink(private val repo: TaskRepo) : LogMessageSink {
    override fun accept(msg: LogMessage) {
        val taskLogMessage  = TaskLogMessage(executionId = msg.executionId,
                type = msg.level.name,
                message = msg.message,
                taskId = msg.taskId ?: UUID(0,0),
                timestamp = msg.timestamp)

        repo.store(taskLogMessage)
    }

}

/**
 * Stores in memory to a StringBuilder. Mainly for unit testing
 */
class InMemoryLogMessageSink(private val format: LogFormat = LogFormat.Full,
                             private val buffer: StringBuilder = StringBuilder()) : LogMessageSink {
    private val messages = ArrayList<LogMessage>()
    override fun accept(m: LogMessage) {
        messages.add(m)
        if (buffer.isNotEmpty()) buffer.append("\n")

        if (format == LogFormat.Test) {
            buffer.append("level=${m.level}, message=${m.message}")
            if (m.taskId != null) {
                buffer.append(", taskId=xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxx")
            }
            if (m.stepId != null) {
                buffer.append(", stepId==xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxx")
            }
        }
        if (format == LogFormat.Full) {
            buffer.append("level=${m.level}, message=${m.message}, executionId=${m.executionId}")
            if (m.taskId != null) {
                buffer.append(", taskId=${m.taskId}")
            }
            if (m.stepId != null) {
                buffer.append(", stepId=${m.stepId}")
            }
            buffer.append(", timestamp=${m.timestamp}")
        }
    }

    fun clear() {
        println("need to cler out thr buffrr")
        //buffer.clear()
        messages.clear()
    }

    fun messages(): List<LogMessage> {
        return messages
    }

    override fun toString(): String {
        return buffer.toString()
    }


}
