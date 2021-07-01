package mycorda.app.tasks.executionContext

import mycorda.app.registry.Registry
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
interface ExecutionContext {
    /**
     * A standard way to manage log output.
     */
    fun log(logLevel: LogLevel = LogLevel.INFO, msg: String)

    /**
     * Write directly to the console if needed
     */
    fun stdout(): PrintStream

    fun withStdout(stdout: StdOut) : ExecutionContext

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
    fun withTaskId(taskId: UUID): ExecutionContext

    /**
     * Any additional data that is scoped to a specific request and therefore setup when building
     * the ExecutionContext, for example a User object holding the current user.
     */
    fun scoped(): Registry

    /**
     * Make it easy to add objects to the scope
     */
    fun withScope(scopedObject: Any): ExecutionContext

    /**
     * Provisioning state
     */
    fun provisioningState(): ProvisioningState

    fun withProvisioningState(state: ProvisioningState): ExecutionContext

    /**
     * Distribution Service
     */
    //fun distributionService(): DistributionService

    //fun withDistributionService(service: DistributionService): ExecutionContext

    /**
     * Instance qualifier - if multiple services are deployed to a server,
     * this gives that task the additional information needed to disambiguate names and
     * directories, e.g. if we have both alice and bob on the same server, then obviously
     * they need separate install directories. This does not help with other parts of the problem,
     * such as picking different port numbers. By default it is null, as the usual
     * server based deploy is a single service per VM / container.
     */
     fun instanceQualifier() : String ?

     fun withInstanceQualifier(qualifier : String? ): ExecutionContext

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
        //private val distributionService: DistributionService = SharedFileDistributionService(".testing/${provisioningState.tag()}"),
        private val stdout: PrintStream = System.out
) : ExecutionContext {

//    override fun distributionService(): DistributionService {
//        return distributionService
//    }

    override fun provisioningState(): ProvisioningState {
        return provisioningState
    }

    override fun log(logLevel: LogLevel, msg: String) {
        println("$logLevel - $msg")
    }

    override fun stdout(): PrintStream {
        return stdout
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


    override fun withTaskId(taskId: UUID): ExecutionContext {
        return DefaultExecutionContext(executionId = this.executionId,
                taskId = taskId,
                stepId = this.stepId,
                executor = this.executor,
                pm = this.pm,
                scoped = this.scoped,
                provisioningState = this.provisioningState,
                //distributionService = this.distributionService,
                instanceQualifier = this.instanceQualifier,
                stdout = this.stdout)
    }

    override fun withScope(scopedObject: Any): ExecutionContext {
        return DefaultExecutionContext(executionId = this.executionId,
                taskId = taskId,
                stepId = this.stepId,
                executor = this.executor,
                pm = this.pm,
                scoped = this.scoped.clone().store(scopedObject),
                provisioningState = this.provisioningState,
                //distributionService = this.distributionService,
                instanceQualifier = this.instanceQualifier,
                stdout = this.stdout)

    }

    override fun withProvisioningState(state: ProvisioningState): ExecutionContext {
        return DefaultExecutionContext(executionId = this.executionId,
                taskId = this.taskId,
                stepId = stepId,
                executor = this.executor,
                pm = this.pm,
                scoped = this.scoped,
                provisioningState = state,
                //distributionService = this.distributionService,
                instanceQualifier = this.instanceQualifier,
                stdout = this.stdout)

    }

//    override fun withDistributionService(service: DistributionService): ExecutionContext {
//        return DefaultExecutionContext(executionId = this.executionId,
//                taskId = this.taskId,
//                stepId = stepId,
//                executor = this.executor,
//                pm = this.pm,
//                scoped = this.scoped,
//                provisioningState = this.provisioningState,
//                distributionService = service,
//                instanceQualifier = this.instanceQualifier,
//                stdout = this.stdout)
//
//    }

    override fun withInstanceQualifier(qualifier: String?): ExecutionContext {
        return DefaultExecutionContext(executionId = this.executionId,
                taskId = this.taskId,
                stepId = stepId,
                executor = this.executor,
                pm = this.pm,
                scoped = this.scoped,
                provisioningState = this.provisioningState,
                //distributionService = this.distributionService,
                instanceQualifier = qualifier,
                stdout = this.stdout)
    }

    override fun withStdout(stdout: StdOut): ExecutionContext {
        return DefaultExecutionContext(executionId = this.executionId,
                taskId = this.taskId,
                stepId = stepId,
                executor = this.executor,
                pm = this.pm,
                scoped = this.scoped,
                provisioningState = this.provisioningState,
                //distributionService = this.distributionService,
                instanceQualifier = this.instanceQualifier,
                stdout = stdout.printStream
        )
    }
}


interface ExecutionContextFactory {

    /**
     * Inject in the key context specific information here.
     * Other values are overridden with the .withXXX methods
     * on the built execution context.
     */
    fun get(executionId: UUID = UUID.randomUUID(),
            taskId: UUID? = null,
            stepId: UUID? = null,
            scoped: Registry = Registry(),
            logMessageSink : LogMessageSink? = null

    ): ExecutionContext
}

/**
 * Wrap standard printStream print stream. Primarily for use in the Registry, which
 * isn't designed to store generic classes like PrintStream
 */
data class StdOut(val printStream: PrintStream = System.out)