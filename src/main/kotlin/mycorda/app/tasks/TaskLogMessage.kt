package mycorda.app.tasks

import java.util.*

/**
 * Simple raw class to hold basic logging information
 */
data class TaskLogMessage(
    val executionId: UUID,
    val type: String, // INFO,WARN,ERROR
    val message: String,
    val taskId: UUID,
    val timestamp: Long = System.currentTimeMillis()
)