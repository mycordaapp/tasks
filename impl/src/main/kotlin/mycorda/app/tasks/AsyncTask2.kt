package mycorda.app.tasks

import mycorda.app.helpers.random
import mycorda.app.tasks.executionContext.SimpleExecutionContext
import mycorda.app.tasks.executionContext.ExecutionContext
import java.util.*


/**
 * A generic encapsulation of a unique identifier that
 * can be generated in a range of formats like UUIDs or hashing
 * functions.
 *
 * Max length is 64, which allows for easy encoding of 256 bit hashes
 */
open class UniqueId(val id: String = UUID.randomUUID().toString()) {

    init {
        // set some basic rules length rules
        assert(id.length >= 6)
        assert(id.length <= 256 / 4)
    }

    companion object {
        /**
         * From a random UUID
         */
        fun randomUUID(): UniqueId {
            return UniqueId(UUID.randomUUID().toString())
        }

        /**
         * From a provided String. Min length is 6, max is 64
         */
        fun fromString(id: String): UniqueId {
            return UniqueId(id)
        }

        /**
         * From a provided UUID
         */
        fun fromUUID(id: UUID): UniqueId {
            return UniqueId(id.toString())
        }

        /**
         * Build a random string in a 'booking reference' style,
         * e.g. `BZ13FG`
         */
        fun random(length: Int = 6): UniqueId {
            return UniqueId(String.random(length))
        }
    }
}

/**
 * The three basic result types for onSuccess, onFail
 * and onTimeout
 */
sealed class AsyncResult2<T>
class Success<T>(val result: T) : AsyncResult2<T>() {
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

class Fail<T>(val message: String) : AsyncResult2<T>()
class Timeout<T>(val message: String) : AsyncResult2<T>()






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


interface Async2Task<I, O> : Task {
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






