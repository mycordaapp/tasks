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
        executionContext: ExecutionContext = SimpleExecutionContext(),

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
}

/**
 * Running a task remotely - as it on another
 * server we pass the tasknum
 */
interface Async2TaskClient {
    fun <I> execTask(
        taskClazz: String,
        //channelLocator: AsyncResultChannelSinkLocator,
        channelId: UniqueId,
        input: I
        // handler: AsyncResultHandler<O>
    )
}

class Async2TaskClientImpl(channelLocator: AsyncResultChannelSinkLocator ) : Async2TaskClient {
    override fun <I> execTask(
        taskClazz: String,
        //channelLocator: AsyncResultChannelSinkLocator,
        channelId: UniqueId,
        input: I
    ) {

    }

}






