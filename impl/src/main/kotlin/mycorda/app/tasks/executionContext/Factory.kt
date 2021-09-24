package mycorda.app.tasks.executionContext

import mycorda.app.registry.Registry
import mycorda.app.tasks.ExecutorFactory
import mycorda.app.tasks.logging.*
import mycorda.app.tasks.processManager.ProcessManager
import java.io.PrintStream
import java.util.*
import java.util.concurrent.ExecutorService

/**
 * A properly wired up context
 * TODO - now that ExecutionContex has been simplified, is this useful?
 */
class DefaultExecutionContextFactory(registry: Registry) : ExecutionContextFactory {
    private val pm = registry.get(ProcessManager::class.java)
    private val executor = registry.get(ExecutorFactory::class.java).executorService()
    private val stdout = registry.geteOrElse(StdOut::class.java, StdOut())
    private val provisioningState = DefaultProvisioningState()
    private val loggingContext =
        registry.geteOrElse(LoggingProducerContext::class.java, InjectableLoggingProducerContext(registry))

    override fun get(
        executionId: UUID,
        taskId: UUID?,
        scoped: Registry,
        logMessageSink: LogMessageSink?
    ): ExecutionContext {
        return Ctx(
            executionId, taskId, pm, executor,
            stdout.printStream, provisioningState, null, loggingContext
        )
    }

    class Ctx(
        private val executionId: UUID,
        private val taskId: UUID?,
        private val processManager: ProcessManager,
        private val executorService: ExecutorService,
        private var stdout: PrintStream,
        private val provisioningState: ProvisioningState,
        private val instanceQualifier: String?,
        private val loggingProducerContext: LoggingProducerContext
    ) : ExecutionContext {


        override fun stdout(): PrintStream {
            return stdout
        }

        override fun stderr(): PrintStream {
            return stdout
        }

        override fun withTaskId(taskId: UUID): ExecutionContext {
            return DefaultExecutionContextModifier(this).withTaskId(taskId)
        }

        override fun withProvisioningState(provisioningState: ProvisioningState): ExecutionContext {
            return DefaultExecutionContextModifier(this).withProvisioningState(provisioningState)

        }

        override fun withInstanceQualifier(instanceQualifier: String?): ExecutionContext {
            return DefaultExecutionContextModifier(this).withInstanceQualifier(instanceQualifier)
        }

        override fun processManager(): ProcessManager {
            return processManager
        }

        override fun executorService(): ExecutorService {
            return executorService
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

        override fun logger(): LogMessageSink {
            return loggingProducerContext.logger()
        }

        override fun provisioningState(): ProvisioningState {
            return provisioningState
        }
    }
}