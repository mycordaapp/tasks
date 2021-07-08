package mycorda.app.tasks.executionContext

import mycorda.app.registry.Registry
import mycorda.app.tasks.ExecutorFactory
import mycorda.app.tasks.logging.LogLevel
import mycorda.app.tasks.logging.LogMessage
import mycorda.app.tasks.logging.LogMessageSink
import mycorda.app.tasks.processManager.ProcessManager


import java.io.PrintStream
import java.util.*
import java.util.concurrent.ExecutorService


/**
 * A properly wired up context
 */
class DefaultExecutionContextFactory(registry: Registry) : ExecutionContextFactory {
    private val messageSink = registry.get(LogMessageSink::class.java)
    private val pm = registry.get(ProcessManager::class.java)
    private val executor = registry.get(ExecutorFactory::class.java).executorService()
    private val stdout = registry.geteOrElse(StdOut::class.java, StdOut())
    private val provisioningState = DefaultProvisioningState()

    override fun get(executionId: UUID,
                     taskId: UUID?,
                     stepId: UUID?,
                     scoped: Registry,
                     logMessageSink: LogMessageSink?): ExecutionContext {
        return Ctx(executionId, taskId, stepId, logMessageSink ?: messageSink, pm, executor, scoped,
                stdout.printStream, provisioningState,  null)
    }

    class Ctx(private val executionId: UUID,
              private val taskId: UUID?,
              private val stepId: UUID?,
              private val sink: LogMessageSink,
              private val processManager: ProcessManager,
              private val executorService: ExecutorService,
              private var scoped: Registry,
              private var stdout: PrintStream,
              private val provisioningState: ProvisioningState,
              private val instanceQualifier: String?) : ExecutionContext {



        override fun stdout(): PrintStream {
            return stdout
        }

        override fun withStdout(stdout: StdOut): ExecutionContext {
            return Ctx(executionId = this.executionId, taskId = this.taskId, stepId = stepId,
                    sink = this.sink, processManager = this.processManager, executorService = this.executorService,
                    scoped = this.scoped, stdout = stdout.printStream, provisioningState = this.provisioningState,
                     instanceQualifier = this.instanceQualifier)
        }

        override fun scoped(): Registry {
            return scoped
        }

        override fun withScope(scopedObject: Any): ExecutionContext {
            scoped = scoped.clone().store(scopedObject)
            return this
        }


        override fun log(logLevel: LogLevel, msg: String) {
            val logMessage = LogMessage(executionId = executionId,
                    level = logLevel,
                    body = msg,
                    taskId = taskId,
                    timestamp = System.currentTimeMillis())

            sink.accept(logMessage)
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

        override fun withTaskId(taskId: UUID): ExecutionContext {
            return Ctx(executionId = this.executionId, taskId = taskId, stepId = this.stepId,
                    sink = this.sink, processManager = this.processManager, executorService = this.executorService,
                    scoped = this.scoped, stdout = this.stdout, provisioningState = this.provisioningState,
                    instanceQualifier = this.instanceQualifier)

        }


        override fun provisioningState(): ProvisioningState {
            return provisioningState
        }

        override fun withProvisioningState(state: ProvisioningState): ExecutionContext {
            return Ctx(executionId = this.executionId, taskId = this.taskId, stepId = stepId,
                    sink = this.sink, processManager = this.processManager, executorService = this.executorService,
                    scoped = this.scoped, stdout = this.stdout, provisioningState = state,
                    instanceQualifier = this.instanceQualifier)
        }



        override fun withInstanceQualifier(qualifier: String?): ExecutionContext {
            return Ctx(executionId = this.executionId, taskId = this.taskId, stepId = stepId,
                    sink = this.sink, processManager = this.processManager, executorService = this.executorService,
                    scoped = this.scoped, stdout = this.stdout, provisioningState = this.provisioningState,
                    instanceQualifier = qualifier)
        }
    }
}