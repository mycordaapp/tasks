package mycorda.app.tasks.executionContext

import mycorda.app.registry.Registry
import mycorda.app.tasks.Task
import mycorda.app.tasks.logging.*
import mycorda.app.tasks.processManager.ProcessManager
import java.io.PrintStream
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


/**
 * A standard context to pass around. It provides access to services and
 * information that are deferred until execution time, as they may
 * change on each run
 */
interface ExecutionContext : LoggingProducerContext, ExecutionContextModifier {

    /**
     *  One single place for running and checking the status of processes.
     *
     *  @see ProcessManager
     */
    fun processManager(): ProcessManager


    /*
      The Java ExecutorService to be used.
     */
    fun executorService(): ExecutorService

    /*
      This should link back to the original invocation, so that all activity 
      linked to it can be tracked against a single ID. So typically 
      an API controller would set on up if not present and then pass it on. 
      
      The intention is that this id can be used for distributed logging and tracing, 
      so that if calls cross process boundaries this ID is retained, i.e. a new 
      executionId is only generated for the original invocation, after that it is retained 
      and passed between services, though the current implementation (Dec 2019) 
      doesn't really enforce this.

      In ZipKin terminology this is called the traceId
    */
    fun executionId(): UUID

    fun taskId(): UUID?

    /**
     * Provisioning state
     */
    fun provisioningState(): ProvisioningState

    /**
     * Instance qualifier - if multiple services are deployed to a server,
     * this gives that task the additional information needed to disambiguate names and
     * directories, e.g. if we have both alice and bob on the same server, then obviously
     * they need separate install directories. This does not help with other parts of the problem,
     * such as picking different port numbers. By default it is null, as the usual
     * server based deploy is a single service per VM / container.
     */
    fun instanceQualifier(): String?

    /**
     * A short cut for logging that pulls in all the information
     * on the ExecutionContext
     */
    fun log(body: String, level: LogLevel = LogLevel.INFO) {
        val msg = LogMessage(
            executionId = this.executionId(),
            taskId = this.taskId(),
            body = body,
            level = level
        )
        log(msg)
    }
}

/**
 * A standard way to modify an existing ExecutionContext
 */
interface ExecutionContextModifier {

    fun withTaskId(taskId: UUID): ExecutionContext

    fun withTaskId(task: Task): ExecutionContext = withTaskId(task.taskId())


    /**
     * Make it easy to add objects to the scope
     */
    fun withScope(scopedObject: Any): ExecutionContext

    fun withProvisioningState(provisioningState: ProvisioningState): ExecutionContext

    fun withInstanceQualifier(instanceQualifier: String?): ExecutionContext
}

/**
 * The
 */
class DefaultExecutionContextModifier(original: ExecutionContext) : ExecutionContextModifier {
    private var working = original
    override fun withTaskId(taskId: UUID): ExecutionContext {
        working = SimpleExecutionContext(
            loggingProducerContext = working,
            executionId = working.executionId(),
            taskId = taskId,
            executor = working.executorService(),
            pm = working.processManager(),
            //scoped = working.scoped(),
            provisioningState = working.provisioningState(),
            instanceQualifier = working.instanceQualifier()
        )
        return working
    }


    override fun withScope(scopedObject: Any): ExecutionContext {
        working = SimpleExecutionContext(
            executionId = working.executionId(),
            taskId = working.taskId(),
            executor = working.executorService(),
            pm = working.processManager(),
            //scoped = working.scoped().clone().store(scopedObject),
            provisioningState = working.provisioningState(),
            instanceQualifier = working.instanceQualifier()
        )
        return working
    }

    override fun withProvisioningState(provisioningState: ProvisioningState): ExecutionContext {
        working = SimpleExecutionContext(
            executionId = working.executionId(),
            taskId = working.taskId(),
            executor = working.executorService(),
            pm = working.processManager(),
            //scoped = working.scoped(),
            provisioningState = provisioningState,
            instanceQualifier = working.instanceQualifier()

        )
        return working
    }

    override fun withInstanceQualifier(instanceQualifier: String?): ExecutionContext {
        working = SimpleExecutionContext(
            executionId = working.executionId(),
            taskId = working.taskId(),
            executor = working.executorService(),
            pm = working.processManager(),
            //scoped = working.scoped(),
            provisioningState = working.provisioningState(),
            instanceQualifier = instanceQualifier
        )
        return working
    }

}

/**
 * A simple service, only suitable for basic unit test
 */
class SimpleExecutionContext(
    private val loggingProducerContext: LoggingProducerContext = ConsoleLoggingProducerContext(),
    private val executionId: UUID = UUID.randomUUID(),
    private val taskId: UUID? = null,
    private val instanceQualifier: String? = null,
    private val executor: ExecutorService = Executors.newFixedThreadPool(10),
    private val pm: ProcessManager = ProcessManager(),
    private val provisioningState: ProvisioningState = DefaultProvisioningState()
) : ExecutionContext, ExecutionContextModifier {

    override fun provisioningState(): ProvisioningState {
        return provisioningState
    }

    override fun stdout(): PrintStream = loggingProducerContext.stdout()

    override fun stderr(): PrintStream = loggingProducerContext.stderr()

    override fun logger(): LogMessageSink = loggingProducerContext.logger()


    override fun processManager(): ProcessManager {
        return pm
    }

    override fun executorService(): ExecutorService {
        return executor
    }

    override fun executionId(): UUID {
        return executionId
    }

    override fun taskId(): UUID? {
        return taskId
    }


    override fun instanceQualifier(): String? {
        return instanceQualifier
    }

    override fun withTaskId(taskId: UUID): ExecutionContext {
        return DefaultExecutionContextModifier(this).withTaskId(taskId)
    }

    override fun withScope(scopedObject: Any): ExecutionContext {
        return DefaultExecutionContextModifier(this).withScope(scopedObject)
    }

    override fun withProvisioningState(withProvisioningState: ProvisioningState): ExecutionContext {
        return DefaultExecutionContextModifier(this).withProvisioningState(withProvisioningState)
    }

    override fun withInstanceQualifier(instanceQualifier: String?): ExecutionContext {
        return DefaultExecutionContextModifier(this).withInstanceQualifier(instanceQualifier)
    }


}


interface ExecutionContextFactory {

    /**
     * Inject in the key context specific information here.
     * Other values are overridden with the .withXXX methods
     * on the built execution context.
     */
    fun get(
        executionId: UUID = UUID.randomUUID(),
        taskId: UUID? = null,
        stepId: UUID? = null,
        scoped: Registry = Registry(),
        logMessageSink: LogMessageSink? = null

    ): ExecutionContext
}

/**
 * Wrap standard printStream print stream. Primarily for use in the Registry, which
 * isn't designed to store generic classes like PrintStream
 */
data class StdOut(val printStream: PrintStream = System.out)