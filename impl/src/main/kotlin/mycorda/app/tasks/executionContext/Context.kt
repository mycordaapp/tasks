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
interface ExecutionContext : LoggingContext {

    /**
     * A standard way to manage log output.
     */
    @Deprecated("use log() or logXxxx() instead")
    fun log(logLevel: LogLevel = LogLevel.INFO, msg: String) {
        val message = LogMessage(level = logLevel, executionId = executionId(), body = msg)
        log(message)
    }


    /**
     * Create a fully populated info message
     */
    fun logInfo(t: Task, msg: String) {
        val message = LogMessage(
            executionId = executionId(),
            level = LogLevel.INFO,
            taskId = t.taskID(),
            body = msg
        )
        log(message)
    }

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
     * Any additional data that is scoped to a specific request and therefore setup when building
     * the ExecutionContext, for example a User object holding the current user.
     */
    fun scoped(): Registry


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
}

/**
 * A standard way to modify an existing ExecutionContext
 */
interface ExecutionContextModifier {

    fun withTaskId(taskId: UUID): ExecutionContext

    fun withStdout(stdout: StdOut): ExecutionContext

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
        working = DefaultExecutionContext(
            executionId = working.executionId(),
            taskId = taskId,
            executor = working.executorService(),
            pm = working.processManager(),
            scoped = working.scoped(),
            provisioningState = working.provisioningState(),
            instanceQualifier = working.instanceQualifier(),
            stdout = working.stdout(),
            stderr = working.stderr()
        )
        return working
    }


    override fun withStdout(stdout: StdOut): ExecutionContext {
        working = DefaultExecutionContext(
            executionId = working.executionId(),
            taskId = working.taskId(),
            executor = working.executorService(),
            pm = working.processManager(),
            scoped = working.scoped(),
            provisioningState = working.provisioningState(),
            instanceQualifier = working.instanceQualifier(),
            stdout = stdout.printStream,
            stderr = working.stderr()
        )
        return working
    }

    override fun withScope(scopedObject: Any): ExecutionContext {
        working = DefaultExecutionContext(
            executionId = working.executionId(),
            taskId = working.taskId(),
            executor = working.executorService(),
            pm = working.processManager(),
            scoped = working.scoped().clone().store(scopedObject),
            provisioningState = working.provisioningState(),
            instanceQualifier = working.instanceQualifier(),
            stdout = working.stdout(),
            stderr = working.stderr()
        )
        return working
    }

    override fun withProvisioningState(provisioningState: ProvisioningState): ExecutionContext {
        working = DefaultExecutionContext(
            executionId = working.executionId(),
            taskId = working.taskId(),
            executor = working.executorService(),
            pm = working.processManager(),
            scoped = working.scoped(),
            provisioningState = provisioningState,
            instanceQualifier = working.instanceQualifier(),
            stdout = working.stdout(),
            stderr = working.stderr()
        )
        return working
    }

    override fun withInstanceQualifier(instanceQualifier: String?): ExecutionContext {
        working = DefaultExecutionContext(
            executionId = working.executionId(),
            taskId = working.taskId(),
            executor = working.executorService(),
            pm = working.processManager(),
            scoped = working.scoped(),
            provisioningState = working.provisioningState(),
            instanceQualifier = instanceQualifier,
            stdout = working.stdout(),
            stderr = working.stderr()
        )
        return working
    }

}

/**
 * A simple service, only suitable for basic unit test
 */
class DefaultExecutionContext(
    private val executionId: UUID = UUID.randomUUID(),
    private val taskId: UUID? = null,
    private val stepId: UUID? = null,
    private val instanceQualifier: String? = null,
    private val executor: ExecutorService = Executors.newFixedThreadPool(10),
    private val pm: ProcessManager = ProcessManager(),
    private val scoped: Registry = Registry(),
    private val provisioningState: ProvisioningState = DefaultProvisioningState(),
    private val stdout: PrintStream = System.out,
    private val stderr: PrintStream = System.err,
    private val loggingContext: LoggingContext = DefaultLoggingContext(scoped)
) : ExecutionContext {

//    override fun distributionService(): DistributionService {
//        return distributionService
//    }

    override fun provisioningState(): ProvisioningState {
        return provisioningState
    }


    override fun log(msg: LogMessage): LoggingContext {
        return loggingContext.log(msg)
    }

    override fun stdout(): PrintStream {
        return stdout
    }

    override fun stderr(): PrintStream {
        return stderr
    }

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

    override fun scoped(): Registry {
        return scoped
    }

    override fun instanceQualifier(): String? {
        return instanceQualifier
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