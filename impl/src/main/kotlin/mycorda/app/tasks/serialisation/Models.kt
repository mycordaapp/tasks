package mycorda.app.tasks.serialisation


/**
 * The data passed between client and server for a blocking
 */
data class BlockingTaskRequest(
    val task: String,
    val inputSerialized: String,
    val inputClazz: String,
    val outputClazz: String
)
