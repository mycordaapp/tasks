import mycorda.app.registry.Registry
import mycorda.app.tasks.BlockingTask
import mycorda.app.tasks.TaskFactory2
import mycorda.app.tasks.executionContext.SimpleExecutionContext
import mycorda.app.tasks.logging.InMemoryLoggingConsumerContext
import mycorda.app.tasks.logging.InMemoryLoggingProducerContext
import mycorda.app.tasks.logging.LoggingConsumerContext

sealed class SecurityPrinciple

/**
 * Pass a JWT token that can be checked
 */
class JwtSecurityPrinciple(val jwtToken: String) : SecurityPrinciple() {}

/**
 * Authenicated with just a username and set of roles. We trust an external system
 * to
 */
class UserAndRoles(val userName: String, val roles: Set<String>) : SecurityPrinciple() {}

/**
 * For testing, or environments where security is unimportant
 */
class NotAuthenticatedSecurityPrinciple(val userName: String = "unknown") : SecurityPrinciple()

/**
 * The information that any client must provide
 */
interface ClientContext {
    /**
     * One of the security principles
     */
    fun securityPrinciple(): SecurityPrinciple

    /**
     * Be able to consume the logging output
     */
    fun loggingConsumer(): LoggingConsumerContext

    /**
     * Web Request style custom headers. Should be used with care
     */
    fun customHeaders(): Map<String, String>
}

interface TaskClient2 {
    fun <I, O> execBlocking(
        ctx: ClientContext,
        taskName: String,
        input: I
    ): O
}

/**
 * Enough for unit test and to communicate with tasks running locally
 */
class SimpleClientContext : ClientContext {
    private val principle = NotAuthenticatedSecurityPrinciple()
    private val logging = InMemoryLoggingConsumerContext()

    override fun securityPrinciple(): SecurityPrinciple = principle

    override fun loggingConsumer(): LoggingConsumerContext = logging

    override fun customHeaders(): Map<String, String> = emptyMap()

    // shortcut to
    fun notAuthenticatedSecurityPrinciple(): NotAuthenticatedSecurityPrinciple = principle
    fun inMemoryLoggingConsumerContext(): InMemoryLoggingConsumerContext = logging

}


/**
 * Enough for unit tests and tasks running locally
 */
class SimpleTaskClient(private val registry: Registry) : TaskClient2 {
    private val taskFactory = registry.get(TaskFactory2::class.java)
    override fun <I, O> execBlocking(ctx: ClientContext, taskName: String, input: I): O {
        val task = taskFactory.createInstance(taskName) as BlockingTask<I, O>

        // hook in logging producer / consumer pair
        val loggingProducerContext = InMemoryLoggingProducerContext(ctx.loggingConsumer())
        val executionContext = SimpleExecutionContext().withLoggingProducerContext(loggingProducerContext)

        return task.exec(executionContext, input)
    }

}