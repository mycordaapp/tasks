package mycorda.app.tasks.client

import mycorda.app.registry.Registry
import mycorda.app.rss.JsonSerialiser
import mycorda.app.tasks.*
import mycorda.app.tasks.executionContext.SimpleExecutionContext
import mycorda.app.tasks.logging.*
import mycorda.app.types.UniqueId
import kotlin.reflect.KClass

/**
 * Marker interface for any type of security (authentication & authorisation) protocol
 */
interface SecurityPrinciple

/**
 * Pass a JWT token that can be checked
 */
class JwtSecurityPrinciple(val jwtToken: String) : SecurityPrinciple

/**
 * Authenticated with just a username and set of roles. We trust an external system
 */
class UserAndRoles(val userName: String, val roles: Set<String>) : SecurityPrinciple

/**
 * For testing, or environments where security is unimportant
 */
class NotAuthenticatedSecurityPrinciple(val userName: String = "unknown") : SecurityPrinciple

/**
 * The information that any client must provide
 */
interface ClientContext {
    /**
     * One of the security principles
     */
    fun securityPrinciples(): Set<SecurityPrinciple>

    /**
     * How to be sent back log messages
     */
    fun logChannelLocator(): LoggingChannelLocator

    /**
     * Web Request style custom headers. Should be used with care (is this a good idea?)
     */
    fun customHeaders(): Map<String, String>
}

interface TaskClient {
    fun <I : Any, O : Any> execBlocking(
        ctx: ClientContext,
        taskName: String,
        input: I,
        outputClazz: KClass<O>  // need access to the output clazz for serialization
    ): O

    fun <I : Any, O : Any> execAsync(
        ctx: ClientContext,
        taskName: String,
        channelLocator: AsyncResultChannelSinkLocator,
        channelId: UniqueId,
        input: I,
        outputClazz: KClass<O>  // need access to the output clazz for serialization
    )
}

/**
 * Enough for unit test and to communicate with tasks running locally
 */
class SimpleClientContext(private val loggingChannelLocator: LoggingChannelLocator = LoggingChannelLocator.inMemory()) :
    ClientContext {
    private val principle = NotAuthenticatedSecurityPrinciple()
    override fun securityPrinciples(): Set<SecurityPrinciple> = setOf(principle)
    override fun logChannelLocator(): LoggingChannelLocator = loggingChannelLocator
    override fun customHeaders(): Map<String, String> = emptyMap()

}

/**
 * Enough for unit tests and tasks running locally
 */
class SimpleTaskClient(registry: Registry) : TaskClient {
    private val taskFactory = registry.get(TaskFactory::class.java)
    private val serialiser = registry.getOrElse(JsonSerialiser::class.java, JsonSerialiser())
    private val logChannelLocatorFactory =
        registry.getOrElse(LoggingChannelFactory::class.java, DefaultLoggingChannelFactory(registry))

    override fun <I : Any, O : Any> execBlocking(
        ctx: ClientContext,
        taskName: String,
        input: I,
        outputClazz: KClass<O>
    ): O {
        @Suppress("UNCHECKED_CAST")
        val task = taskFactory.createInstance(taskName) as BlockingTask<I, O>

        // hook in logging producer / consumer pair
        val loggingConsumerContext = logChannelLocatorFactory.consumer(ctx.logChannelLocator())
        val producerContext = LoggingProducerToConsumer(loggingConsumerContext)
        val executionContext = SimpleExecutionContext(producerContext)

        try {
            // note, force serialisation / de-serialisation locally to catch any problems early
            val result = task.exec(executionContext, roundTripInput(input))
            return (roundTripOutput(result))
        } catch (e: Exception) {
            val message = LogMessage(
                executionId = executionContext.executionId(),
                level = LogLevel.WARN,
                body = "Task generated exception of: ${e.message}",
                taskId = task.taskId()
            )
            loggingConsumerContext.acceptLog(message)
            throw e
        }
    }

    private fun <I : Any> roundTripInput(input: I): I {
        @Suppress("UNCHECKED_CAST")
        return serialiser.deserialiseData(serialiser.serialiseData(input)).any() as I
    }

    private fun <O : Any> roundTripOutput(output: O): O {
        @Suppress("UNCHECKED_CAST")
        return serialiser.deserialiseData(serialiser.serialiseData(output)).any() as O
    }

    override fun <I : Any, O : Any> execAsync(
        ctx: ClientContext,
        taskName: String,
        channelLocator: AsyncResultChannelSinkLocator,
        channelId: UniqueId,
        input: I,
        outputClazz: KClass<O>
    ) {
        @Suppress("UNCHECKED_CAST")
        val task = taskFactory.createInstance(taskName) as AsyncTask<I, O>

        // hook in logging producer / consumer pair
        val loggingConsumerContext = logChannelLocatorFactory.consumer(ctx.logChannelLocator())
        val producerContext = LoggingProducerToConsumer(loggingConsumerContext)
        val executionContext = SimpleExecutionContext(producerContext)

        task.exec(executionContext, channelLocator, channelId, input)
    }
}