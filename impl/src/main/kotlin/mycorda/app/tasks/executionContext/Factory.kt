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
 */
class DefaultExecutionContextFactory(registry: Registry) : ExecutionContextFactory {
    private val messageSink = registry.get(LogMessageSink::class.java)
    private val pm = registry.get(ProcessManager::class.java)
    private val executor = registry.get(ExecutorFactory::class.java).executorService()
    private val stdout = registry.geteOrElse(StdOut::class.java, StdOut())
    private val provisioningState = DefaultProvisioningState()
    private val loggingContext = registry.geteOrElse(LoggingContext::class.java, DefaultLoggingContext(registry))

    override fun get(executionId: UUID,
                     taskId: UUID?,
                     stepId: UUID?,
                     scoped: Registry,
                     logMessageSink: LogMessageSink?): ExecutionContext {
        return Ctx(executionId, taskId, stepId, logMessageSink ?: messageSink, pm, executor, scoped,
                stdout.printStream, provisioningState,  null, loggingContext)
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
              private val instanceQualifier: String?,
              private val loggingContext : LoggingContext) : ExecutionContext {



        override fun stdout(): PrintStream {
            return stdout
        }

        override fun stderr(): PrintStream {
            return stdout
        }


        override fun scoped(): Registry {
            return scoped
        }



        override fun log(logLevel: LogLevel, msg: String) {
            val logMessage = LogMessage(executionId = executionId,
                    level = logLevel,
                    body = msg,
                    taskId = taskId,
                    timestamp = System.currentTimeMillis())

            sink.accept(logMessage)
        }

        override fun log(msg: LogMessage): LoggingContext {
            loggingContext.log(msg)
            return this
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


        override fun provisioningState(): ProvisioningState {
            return provisioningState
        }

    }
}