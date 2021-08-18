package mycorda.app.tasks

import mycorda.app.helpers.random
import mycorda.app.tasks.executionContext.DefaultExecutionContext
import mycorda.app.tasks.executionContext.ExecutionContext
import java.util.*
import java.util.function.Consumer

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
class Success<T>(val result: T) : AsyncResult2<T>()
class Fail<T>(val message: String) : AsyncResult2<T>()
class Timeout<T>(val message: String) : AsyncResult2<T>()


/**
 * The logical channel for passing results between services.
 * Note that writing to channel is ALWAYS idempotent, i.e. we NEVER
 * generate different results for the same channelId
 */
data class AsyncResultChannelMessage(
    val channelId: UniqueId,
    val result: AsyncResult2<Any>,
    val resultClazz: Class<Any>    // for serializer and helping with type safety. Is it s good idea?
)

/**
 * The client (consumer) side of ResultChannel.
 * Note at this
 */
interface AsyncResultChannelSink : Consumer<AsyncResultChannelMessage> {
    override fun accept(result: AsyncResultChannelMessage)
}

/**
 * The server (generator) side of ResultChannel
 */
interface AsyncResultChannelSource {
    fun create(): AsyncResultChannelMessage
}

/**
 * As the AsyncTask can be long running and may live between restarts,
 * then there needs to a way for the server side to know how to
 * recreate the AsyncResultChannelSource that was specified
 * when the original request was made.
 *
 * There are some assumptions and conventions here:
 *   - the first part of the string specifies the type of channel,
 *     for example "LOCAL:", "REST:", "AWSSMS:"
 *   - the second part of the string encodes all other connection information
 *   - the server side will store the locator linked to the originating
 *     request
 *   - an implementation of AsyncResultChannelSourceFactory will
 *     know how to create a suitable concrete implementations of AsyncResultChannelSource
 *     from the Locator
 *
 */
data class AsyncResultChannelSinkLocator(val locator: String)

/**
 * Given a AsyncResultChannelSourceLocator, return the actual channel
 */
interface AsyncResultChannelSinkFactory {
    fun create(locator: AsyncResultChannelSinkLocator): AsyncResultChannelSink
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
        executionContext: ExecutionContext = DefaultExecutionContext(),

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
        channelLocator: AsyncResultChannelSinkLocator,
        channelId: UniqueId,
        input: I
       // handler: AsyncResultHandler<O>
    )
}

class Async2TaskClientImpl : Async2TaskClient {
    override fun <I> execTask(
        taskClazz: String,
        channelLocator: AsyncResultChannelSinkLocator,
        channelId: UniqueId,
        input: I
    ) {

    }

}


class InMemoryAsyncResultChannel() : AsyncResultChannelSink {
    private val channel = LinkedList<AsyncResultChannelMessage>()

    override fun accept(result: AsyncResultChannelMessage) {
        channel.add(result)
    }

}

class DefaultAsyncResultChannelSinkFactory() : AsyncResultChannelSinkFactory {
    private val inMemoryAsyncResultChannel = InMemoryAsyncResultChannel()
    override fun create(locator: AsyncResultChannelSinkLocator): AsyncResultChannelSink {
        if (locator.locator == "LOCAL") {
            return inMemoryAsyncResultChannel
        } else {
            throw RuntimeException("Dont know about $locator")
        }
    }

}

