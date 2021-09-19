package mycorda.app.tasks


import mycorda.app.tasks.executionContext.SimpleExecutionContext
import mycorda.app.tasks.executionContext.DefaultExecutionContextModifier
import mycorda.app.tasks.executionContext.ExecutionContext
import java.util.UUID
import java.util.concurrent.Future

/**
 * A common marker interface for a Task
 */
interface Task {
    /**
     * A unique ID created for each run of the Task
     */
    fun taskId(): UUID
}


/**
 * A blocking task, i.e. one we can assume will either complete within a reasonable time or just
 * fail with an exception
 */
interface BlockingTask<in I, out O> : Task {
    /**
     * Execute the task.
     */
    fun exec(ctx: ExecutionContext = SimpleExecutionContext(), params: I): O
}

interface UnitBlockingTask<I> : BlockingTask<I, Unit> {
    override fun exec(ctx: ExecutionContext, params: I)
}

interface AsyncTask<I, O> : Task {
    /**
     * Execute the task.
     */
    fun exec(executionContext: ExecutionContext = SimpleExecutionContext(), params: I): Future<O>
}


abstract class BaseBlockingTask<I, O> : BlockingTask<I, O> {
    private val taskID = UUID.randomUUID()
    override fun taskId(): UUID {
        return taskID
    }

    /**
     * Update the ExecutionContext with the TaskId.
     */
    protected fun ctxWithTaskID(ctx: ExecutionContext): ExecutionContext =
        DefaultExecutionContextModifier(ctx).withTaskId(taskId())
}

abstract class BaseUnitBlockingTask<I> : UnitBlockingTask<I> {
    private val taskID = UUID.randomUUID()
    override fun taskId(): UUID {
        return taskID
    }

    /**
     * Update the ExecutionContext with the TaskId.
     */
    protected fun ctxWithTaskID(ctx: ExecutionContext): ExecutionContext =
        DefaultExecutionContextModifier(ctx).withTaskId(taskId())
}

abstract class BaseAsyncTask<I, O> : AsyncTask<I, O> {
    private val taskID = UUID.randomUUID()
    override fun taskId(): UUID {
        return taskID
    }

    /**
     * Update the ExecutionContext with the TaskId.
     */
    protected fun updatedCtx(ctx: ExecutionContext): ExecutionContext =
        DefaultExecutionContextModifier(ctx).withTaskId(taskId())
}


/**
 * A standard result for use in Async tasks.
 */
data class AsyncResult(
    val success: Boolean,
    val message: String,
    val exception: Exception? = null,
    val processId: UUID? = null
)

class TaskException(override val message: String, override val cause : Throwable? = null) : RuntimeException(message, cause)


