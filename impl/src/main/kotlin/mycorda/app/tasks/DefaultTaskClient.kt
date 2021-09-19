package mycorda.app.tasks

import mycorda.app.registry.Registry
import mycorda.app.tasks.executionContext.ExecutionContext
import java.util.*
import java.util.concurrent.Future
import kotlin.reflect.KClass

/**
 * An implementation of TaskClient for local use only. By default all stdout is sent
 * to its own embedded in memory print stream.
 */
class DefaultTaskClient(registry: Registry) : TaskClient {

    private val lookup = HashMap<UUID, Pair<Future<Any>, Timer>>()
    private val taskFactory = registry.get(TaskFactory::class.java)
    private val capturedPrintStream = InMemoryCapturedPrintStream()
    //private val es = registry.geteOrElse(EventStore::class.java, FileEventStore())

    companion object {
        val AGENTID = UUID.fromString("F1E06FB7-05EE-4C32-B359-054143410063")
    }

    private fun ctxWithCapturedPrintStream(original: ExecutionContext): ExecutionContext {
        println("TODO ctxWithCapturedPrintStream() is not needed ")
        return original
    }

    override fun defaultTimeoutInSeconds(): Int {
        return 60
    }

    override fun <I, O> exec(
        ctx: ExecutionContext,
        taskClazz: KClass<out BlockingTask<I, O>>,
        input: I,
        timeout: Int
    ): O {
        // todo - what about the timeout ?
        val className = taskClazz.simpleName!!.removeSuffix("Task")

        val t = taskFactory.createInstance(className)
        val executor = SimpleTaskExecutor<I, O>(ctxWithCapturedPrintStream(ctx))
        return executor.exec(t, input)
    }

    override fun <I> execUnit(
        ctx: ExecutionContext,
        taskClazz: KClass<out UnitBlockingTask<I>>,
        input: I,
        timeout: Int
    ) {
        // todo - what about the timeout ?
        val className = taskClazz.simpleName!!.removeSuffix("Task")
        val t = taskFactory.createInstance(className)
        val executor = SimpleTaskExecutor<I, Unit>(ctxWithCapturedPrintStream(ctx))
        return executor.exec(t, input)
    }

    override fun <I, O> execAsync(
        ctx: ExecutionContext,
        taskClazz: KClass<out AsyncTask<I, O>>,
        input: I,
        resultClazz: KClass<out Any>,
        timeout: Int
    ): O {
        TODO("Not yet implemented")
    }

    override fun <I, O> execAsyncAsFuture(
        ctx: ExecutionContext,
        taskClazz: KClass<out AsyncTask<I, O>>,
        input: I,
        resultClazz: KClass<out Any>
    ): Future<O> {
        TODO("Not yet implemented")
    }

//    override fun <I, O> execAsync(ctx: ExecutionContext, taskClazz: KClass<out AsyncTask<I, O>>, input: I, resultClazz: KClass<out Any>, timeout: Int): O {
//        return execAsyncAsFuture(ctx, taskClazz, input, resultClazz).get(timeout.toLong(), TimeUnit.SECONDS)
//    }


//    private fun <I, O> execAsyncAsFuture(ctx: ExecutionContext, taskClazz: KClass<out AsyncTask<I, O>>, input: I, resultClazz: KClass<out Any>): Future<O> {
//        val className = taskClazz.simpleName!!.removeSuffix("Task")
//        val t = taskFactory.createInstance(className)
//        val executor = SimpleTaskExecutor<I, Future<O>>(ctx.withStdout(StdOut(capturedPrintStream.printStream())))
//        return executor.exec(t, input)
//    }

    override fun <I, O> execAsyncWithCorrelationId(
        ctx: ExecutionContext,
        correlationId: UUID,
        taskClazz: KClass<out AsyncTask<I, O>>,
        input: I,
        resultClazz: KClass<out Any>,
        timeout: Int
    ) {
        TODO("Not yet implemented")
    }

//    override fun <I, O> execAsyncWithCorrelationId(ctx: ExecutionContext, correlationId: UUID, taskClazz: KClass<out AsyncTask<I, O>>, input: I, resultClazz: KClass<out Any>, timeout: Int) {
//        if (!lookup.containsKey(correlationId)) {
//            //val agentContext = AgentContext(es, correlationId, AGENTID)
//            val future = execAsyncAsFuture(ctx, taskClazz, input, resultClazz, agentContext)
//            val className = taskClazz.simpleName!!.removeSuffix("Task")
//            //es.storeEvent(TaskManagerEventFactory.TASK_STARTED(agentId = AGENTID, correlationId = correlationId, taskName = className))
//            lookup[correlationId] = Pair(future as Future<Any>, Timer())
//        }
//    }

    override fun isDone(correlationId: UUID): Boolean {
        if (!lookup.containsKey(correlationId)) throw NoSuchElementException()
        return lookup[correlationId]!!.first.isDone
    }

    override fun <O> result(correlationId: UUID): O {
        if (!isDone(correlationId)) throw IllegalStateException("The Task is still running")
        if (!lookup.containsKey(correlationId)) throw NoSuchElementException()
        return lookup[correlationId]!!.first.get() as O
    }

    override fun runningTimeMs(correlationId: UUID): Long {
        if (!lookup.containsKey(correlationId)) throw IllegalStateException()
        val timer = lookup[correlationId]!!.second
        if (isDone(correlationId)) {
            timer.stop()
        }
        return timer.runningTime()
    }

    override fun capturedStdOut(): String {
        return capturedPrintStream.captured()
    }

    override fun agentId(): UUID {
        // a fixed UUID, as there can only ever be one local TaskClient
        return AGENTID
    }

    class Timer(private val startTime: Long = System.currentTimeMillis()) {
        private var stopTime: Long = -1

        fun runningTime(): Long {
            return if (stopTime == -1L) {
                System.currentTimeMillis() - startTime
            } else {
                stopTime - startTime
            }
        }

        fun stop(): Timer {
            if (stopTime == -1L) {
                stopTime = System.currentTimeMillis()
            }
            return this
        }
    }

}