package mycorda.app.tasks

import mycorda.app.tasks.executionContext.SimpleExecutionContext
import mycorda.app.tasks.executionContext.ExecutionContext


/**
 * The three basic result types for onSuccess, onFail
 * and onTimeout
 */
sealed class AsyncResult<T>
class Success<T>(val result: T) : AsyncResult<T>() {
    override fun equals(other: Any?): Boolean {
        return if (other is Success<*>) {
            this.result == other.result
        } else {
            false
        }
    }

    override fun hashCode(): Int {
        return result!!.hashCode()
    }
}

class Fail<T>(val message: String) : AsyncResult<T>()
class Timeout<T>(val message: String) : AsyncResult<T>()


/**
 * The server (generator) side of ResultChannel
 */
interface AsyncResultChannelSource {
    fun create(): AsyncResultChannelMessage<Any>
}


/**
 * The usual way of handling an AsyncResult in client code
 */
interface AsyncResultHandler<T> {
    fun onSuccess(result: T)
    fun onFail(message: String)
    fun onTimeout(message: String)
}

/**
 * register a handler for the result
 */
interface RegisterAsyncResultHandler<T> {
    fun register(channelId: UniqueId, handler: AsyncResultHandler<T>)
}


interface AsyncTask<I, O> : Task {
    /**
     * Execute the task.
     */
    fun exec(
        ctx: ExecutionContext = SimpleExecutionContext(),

        /**
         * Where to send the result back to? Should be stored
         * with the original request.
         */
        channelLocator: AsyncResultChannelSinkLocator,

        /**
         * The unique channelId
         */
        channelId: UniqueId,

        /**
         * The actual input
         */
        input: I
    )

    companion object {
        // use for timings in threads, esp test cases.  Keep to
        // the minimum for underlying system clock on the OS
        // for now just defaulting to 5 ms
        fun platformTick(): Long = 5
        fun sleepForTicks(ticks: Int) = Thread.sleep(platformTick() * ticks)
    }
}








