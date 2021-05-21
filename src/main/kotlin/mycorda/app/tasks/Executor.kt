package mycorda.app.tasks


import mycorda.app.registry.Registry
import mycorda.app.tasks.executionContext.DefaultExecutionContextFactory
import mycorda.app.tasks.executionContext.ExecutionContext
import mycorda.app.tasks.executionContext.LogLevel
import mycorda.app.tasks.executionContext.ProvisioningState
import java.lang.RuntimeException
import java.util.*
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit

interface TaskExecutor<I, O> {
    fun exec(t: Task, params: I): O
}

/**
 * Executes a single task with logging, passing on any exceptions thrown.
 *
 * @TODO - Is this useful still? TaskClient really does provides the same
 *         basic behaviour and can also deals with remoting and serializations
 *
 * A fresh ExecutionContext is created using the DefaultExecutionContextFactory. The registry
 * must be populated with the common services needed by the DefaultExecutionContextFactory to build
 * a fully wired up ExecutionContext, below is an example of the minimal wiring code
 *
 *  val registry = Registry().store(SingleThreadedExecutor())
 *     .store(ProcessManager())
 *     .store(ConsoleLogMessageSink())
 *
 *  val executor = DefaultTaskExecutor<Int, Int>(registry)
 *  val task = CalcSquareTask()
 *  val result = executor.exec(task, params = 9)
 *
 */
@Deprecated("use a TaskClient to start the tasks, or SimpleTaskExecutor inside a task")
class DefaultTaskExecutor<I, O>(
    private val registry: Registry,
    private val executionId: UUID = UUID.randomUUID(),
    private val scoped: Registry = Registry(),
    private val provisioningState: ProvisioningState? = null
) : TaskExecutor<I, O>, BaseTaskExecutor<I, O>() {
    override fun exec(t: Task, params: I): O {

        // wireup a new ExecutionContext
        var executionContext = buildExecutionContext(t)

        if (provisioningState != null) {
            executionContext = executionContext.withProvisioningState(provisioningState)
        }
        return doExec(executionContext, t, params)
    }

    private fun buildExecutionContext(task: Task): ExecutionContext {
        // todo - should be using the execution context factory in the registry if available, else ...
        return DefaultExecutionContextFactory(registry).get(
            executionId = executionId,
            taskId = task.taskID(), scoped = scoped
        )
    }
}

/**
 * Executes a single task with logging, passing on any exceptions thrown.
 *
 * This implementation uses the provided ExecutionContext, and the responsibility
 * on creating a correctly wired and valid ExecutionContext is now with the caller.
 *
 */
class SimpleTaskExecutor<I, O>(private val executionContext: ExecutionContext) : TaskExecutor<I, O>,
    BaseTaskExecutor<I, O>() {
    override fun exec(t: Task, params: I): O {
        return doExec(executionContext.withTaskId(t.taskID()), t, params)
    }

//    fun exec(t: Task, params: I, agentContext: AgentContext?): O {
//        return  doExec(executionContext.withTaskId(t.taskID()), t, params, agentContext)
//    }
}

// todo - this is really a candidate for a cleanup - inject in additional
// agent specific context so that the events can be generated correctly
//data class AgentContext(val es: EventStore, val correlationId: UUID, val agentId: UUID)

abstract class BaseTaskExecutor<I, O> {
    fun doExec(
        executionContext: ExecutionContext,
        t: Task,
        params: I
    ): O {

        executionContext.log(LogLevel.INFO, "Started ${t::class.java.simpleName}")

        try {
            if (t is BlockingTask<*, *>) {
                val tt = t as BlockingTask<I, O>
                val result = tt.exec(executionContext, params)
                executionContext.log(LogLevel.INFO, "Completed ${t::class.java.simpleName}")
                return result
            }

            if (t is AsyncTask<*, *>) {
                val tt = t as AsyncTask<I, O>

                val result = tt.exec(executionContext, params)
                executionContext.log(LogLevel.INFO, "Running Future for ${tt::class.java.simpleName}")
                val wrapped = WrappedFuture(result as Future<Any>, executionContext, tt::class.java)
                return wrapped as O
            }

            throw RuntimeException("Don't known what to do with ${t::class.qualifiedName}")

        } catch (ex: Exception) {
            executionContext.log(LogLevel.INFO, "${ex::class.java.simpleName}: ${ex.message}")
            executionContext.log(LogLevel.ERROR, "Failed ${t::class.java.name}")
            throw ex
        }
    }


    /**
     * Wrap the Future so we can generate events, logging
     * Certainly not a production quality piece of code, and using Futures
     * at all in this part of the code is questionable, but there was a lot of
     * logic originally built around Futures
     */
    class WrappedFuture<T>(
        private val f: Future<T>,
        private val ctx: ExecutionContext,
        private val clazz: Class<*>
    ) : Future<T> {
        private var resultRead = false
        private var eventGenerated = false
        override fun isDone(): Boolean {
            if (!f.isDone) return false
            if (!resultRead) {
                // make sure we trigger an event by forcing a read
                try {
                    get()
                } catch (ignored: Exception) {
                }
            }
            return true
        }

        override fun get(): T {
            try {
                val result = f.get()
                ctx.log(LogLevel.INFO, "Completed Future for ${clazz.simpleName}")
                //generateCompletedEvent()

                return result
            } catch (ex: Exception) {
                ctx.log(LogLevel.INFO, "${ex::class.java.simpleName}: ${ex.message}")
                ctx.log(LogLevel.ERROR, "Future Failed for ${clazz.simpleName}")
                //generateFailedEvent(ex)
                throw ex
            } finally {
                resultRead = true
            }
        }

//        private fun generateCompletedEvent() {
//            if (agentContext != null && !eventGenerated) {
//                eventGenerated = true
//                agentContext.es.storeEvent(TaskManagerEventFactory.TASK_COMPLETED(agentId = agentContext.agentId, correlationId = agentContext.correlationId))
//            }
//        }

//        private fun generateFailedEvent(ex: Exception) {
//            if (agentContext != null && !eventGenerated) {
//                eventGenerated = true
//                agentContext.es.storeEvent(TaskManagerEventFactory.TASK_FAILED(agentId = agentContext.agentId,
//                        correlationId = agentContext.correlationId,
//                        message = ex.message!!))
//            }
//        }

        override fun get(timeout: Long, unit: TimeUnit): T {
            try {
                val result = f.get(timeout, unit)
                ctx.log(LogLevel.INFO, "Completed Future for ${clazz.simpleName}")
                //generateCompletedEvent()
                return result
            } catch (ex: Exception) {
                ctx.log(LogLevel.INFO, "${ex::class.java.simpleName}: ${ex.message}")
                ctx.log(LogLevel.ERROR, "Future Failed for ${clazz.simpleName}")
                //generateFailedEvent(ex)
                throw ex
            } finally {
                resultRead = true
            }
        }

        override fun cancel(mayInterruptIfRunning: Boolean): Boolean {
            ctx.log(LogLevel.INFO, "Cancelling Future for ${clazz.simpleName}")
            return f.cancel(mayInterruptIfRunning)
        }

        override fun isCancelled(): Boolean {
            return f.isCancelled
        }

    }
}

