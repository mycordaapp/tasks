package mycorda.app.tasks

import mycorda.app.tasks.executionContext.ExecutionContext
import java.util.*
import java.util.concurrent.Future
import kotlin.reflect.KClass

/**
 * A way of running a task by clazz name only
 */
interface TaskClient {

    fun defaultTimeoutInSeconds(): Int

    /**
     * Run a blocking task
     * TODO - can we find a way not to have to pass resultClazz. Erasure seem to f*** it all up
     */
    fun <I, O> exec(ctx: ExecutionContext,
                    taskClazz: KClass<out BlockingTask<I, O>>,
                    input: I,
                    timeout: Int = defaultTimeoutInSeconds()): O

    fun <I> execUnit(ctx: ExecutionContext,
                     taskClazz: KClass<out UnitBlockingTask<I>>,
                     input: I,
                     timeout: Int = defaultTimeoutInSeconds())

    /**
     * Run a Async Task in a blocking style, i.e. wait a given time and then
     * fail if there is no result.
     * TODO - can we find a way not to have to pass resultClazz. Erasure seem to f*** it all up
     */
    fun <I, O> execAsync(ctx: ExecutionContext,
                         taskClazz: KClass<out AsyncTask<I, O>>,
                         input: I,
                         resultClazz: KClass<out Any>,
                         timeout: Int = defaultTimeoutInSeconds()): O

    /**
     * Run a Async Task returning a Future
     * TODO - can we find a way not to have to pass resultClazz. Erasure seem to f*** it all up
     */
    fun <I, O> execAsyncAsFuture(ctx: ExecutionContext,
                                 taskClazz: KClass<out AsyncTask<I, O>>,
                                 input: I,
                                 resultClazz: KClass<out Any>): Future<O>


    /**
     * Run a Async Task a then check for the result using a correlation Id
     */
    fun <I, O> execAsyncWithCorrelationId(ctx: ExecutionContext,
                                          correlationId: UUID,
                                          taskClazz: KClass<out AsyncTask<I, O>>,
                                          input: I,
                                          resultClazz: KClass<out Any>,
                                          timeout: Int = defaultTimeoutInSeconds())

    /**
     * Checks to see if the task with correlation has completed. Follows the same
     * semantics as isDone on java.util.concurrent.Future
     */
    fun isDone(correlationId: UUID): Boolean

    /**
     * Returns the result if available , or one
     *
     * @throws CancellationException if the computation was cancelled
     * @throws ExecutionException if the was a problem with the Task
     * @throws IllegalStateException if there is no result available (still running) or correlationId is unknown
     */
    fun <O> result(correlationId: UUID): O

    /**
     * The running time until the task completed (or failed with an exception). Intended for
     * reporting purposes only.
     */
    fun runningTimeMs(correlationId: UUID) : Long


    /**
     * Not fully sure if this is the best place, but basically we expect a TaskClient to have setup
     * a CapturedPrintStream, so that any console output from the Tasks can returned
     */
    fun capturedStdOut(): String

    /**
     * Get the Id of the agent linked to this TaskClient.
     */
    fun agentId() : UUID
}


/**
 * The simplified new version of the task client for a blocking Task
 */
interface BlockingTaskClient {
    fun <I,O> execTask(
        taskClazz: String,
        //channelLocator: AsyncResultChannelSinkLocator,
        input: I
        // handler: AsyncResultHandler<O>
    ) : O
}

class LocalTaskClint