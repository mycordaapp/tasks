
package mycorda.app.tasks

import java.io.File
import java.util.*

/**
 * A repo for recording tasks run.
 */
interface TaskRepo {
    fun store(message: TaskLogMessage)
    fun all(): List<TaskLogMessage>
}

class SimpleTaskRepo(private val directory: String, private val dataFile: String = "tasklog.txt") : TaskRepo {
    init {
        println("creating $directory/$dataFile")
        File(directory).mkdirs()
        File("$directory/$dataFile").createNewFile()
    }

    override fun all(): List<TaskLogMessage> {
        val file = File("$directory/$dataFile")

        val decoder = Base64.getDecoder()

        val result = ArrayList<TaskLogMessage>();
        file.forEachLine {
            val parts = it.split(":")
            val ts = parts[0].toLong()
            val executionId = UUID.fromString(parts[1])
            val taskId = UUID.fromString(parts[2])
            val messageType = parts[3]
            val message = String(decoder.decode(parts[4]))
            result.add(TaskLogMessage(executionId, messageType, message, taskId, ts))
        }

        return result
    }

    override fun store(message: TaskLogMessage) {
        val file = File("$directory/$dataFile")

        val base64 = Base64.getEncoder().encodeToString(message.message.toByteArray())
        val encoded = "${message.timestamp}:${message.executionId}:${message.taskId}:${message.type}:$base64\n"
        file.appendText(encoded)
    }
}