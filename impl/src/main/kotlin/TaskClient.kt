import mycorda.app.registry.Registry
import mycorda.app.tasks.*
import mycorda.app.tasks.executionContext.SimpleExecutionContext
import mycorda.app.tasks.logging.InMemoryLoggingConsumerContext
import mycorda.app.tasks.logging.InMemoryLoggingProducerContext
import mycorda.app.tasks.logging.LoggingConsumerContext

/**
 * Marker interface for any type of security (authentication & authorisation) protocol
 *
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
     * Be able to consume the logging output
     */
    fun loggingConsumer(): LoggingConsumerContext

    /**
     * Web Request style custom headers. Should be used with care (is this a good idea?)
     */
    fun customHeaders(): Map<String, String>
}

interface TaskClient {
    fun <I, O> execBlocking(
        ctx: ClientContext,
        taskName: String,
        input: I
    ): O

    fun <I, O> execAsync(
        ctx: ClientContext,
        taskName: String,
        channelLocator: AsyncResultChannelSinkLocator,
        channelId: UniqueId,
        input: I
    )
}

/**
 * Enough for unit test and to communicate with tasks running locally
 */
class SimpleClientContext : ClientContext {
    private val principle = NotAuthenticatedSecurityPrinciple()
    private val logging = InMemoryLoggingConsumerContext()

    override fun securityPrinciples(): Set<SecurityPrinciple> = setOf(principle)

    override fun loggingConsumer(): LoggingConsumerContext = logging

    override fun customHeaders(): Map<String, String> = emptyMap()

    // shortcut to
    fun notAuthenticatedSecurityPrinciple(): NotAuthenticatedSecurityPrinciple = principle
    fun inMemoryLoggingContext(): InMemoryLoggingConsumerContext = logging
}


/**
 * Enough for unit tests and tasks running locally
 */
class SimpleTaskClient(private val registry: Registry) : TaskClient {
    private val taskFactory = registry.get(TaskFactory::class.java)
    override fun <I, O> execBlocking(ctx: ClientContext, taskName: String, input: I): O {
        val task = taskFactory.createInstance(taskName) as BlockingTask<I, O>

        // hook in logging producer / consumer pair
        val loggingProducerContext = InMemoryLoggingProducerContext(ctx.loggingConsumer())
        val executionContext = SimpleExecutionContext(loggingProducerContext)

        return task.exec(executionContext, input)
    }

    override fun <I, O> execAsync(
        ctx: ClientContext,
        taskName: String,
        channelLocator: AsyncResultChannelSinkLocator,
        channelId: UniqueId,
        input: I
    ) {
        val task = taskFactory.createInstance(taskName) as AsyncTask<I, O>

        // hook in logging producer / consumer pair
        val loggingProducerContext = InMemoryLoggingProducerContext(ctx.loggingConsumer())
        val executionContext = SimpleExecutionContext(loggingProducerContext)

        task.exec(executionContext, channelLocator, channelId, input)

    }

}