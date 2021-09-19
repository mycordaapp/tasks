package mycorda.app.tasks

import com.natpryce.hamkrest.*
import com.natpryce.hamkrest.assertion.assertThat
import junit.framework.Assert.fail
import mycorda.app.registry.Registry
import mycorda.app.tasks.demo.CalcSquareAsyncTask
import mycorda.app.tasks.executionContext.SimpleExecutionContext
import mycorda.app.tasks.executionContext.ExecutionContext
import org.junit.Test
import java.util.*

class TaskFactoryTest {
    private val executionContext = SimpleExecutionContext()
    private val notRequired = NotRequired.instance()

    @Test
    fun `should store by class name`() {
        val factory = TaskFactory()
        factory.register(MultiplyTask::class)

        val t = factory.createInstance(MultiplyTask::class.qualifiedName!!)
        assertThat(t::class.qualifiedName, equalTo(MultiplyTask::class.qualifiedName))
    }

    @Test
    fun `should construct using registry version of the constructor`() {
        // build CalculateTask with an Adder in the registry
        val factory1 = TaskFactory(Registry().store(Adder()))
        factory1.register(CalculateTask::class)
        val sumCalculator = factory1.createInstance(CalculateTask::class.qualifiedName!!)
        assertThat(sumCalculator::class.qualifiedName, equalTo(CalculateTask::class.qualifiedName!!))
        assertThat((sumCalculator as CalculateTask).exec(SimpleExecutionContext(), 10), equalTo(20))

        // build CalculateTask with an Multiplier in the registry
        val factory2 = TaskFactory(Registry().store(Multiplier()))
        factory2.register(CalculateTask::class)
        val multiplyCalculator = factory2.createInstance(CalculateTask::class.qualifiedName!!)
        assertThat(
            (multiplyCalculator as CalculateTask).exec(SimpleExecutionContext(), 10),
            equalTo(100)
        )

        // build CalculateTask with nothing in the registry
        val factory3 = TaskFactory(Registry())
        factory3.register(CalculateTask::class)
        assertThat(
            { factory3.createInstance(CalculateTask::class.qualifiedName!!) },
            throws<TaskException>(
                has(
                    Exception::message,
                    present(equalTo("Problem instantiating `mycorda.app.tasks.CalculateTask`. Original error: `Class interface mycorda.app.tasks.Calculator in not in the registry`"))
                )
            )
        )
    }

    @Test
    fun `should lookup task by interface name if provided`() {
        val factory = TaskFactory()
        factory.register(HelloWorldTask::class, SimpleTask::class)

        // can lookup by the interface
        val t = factory.createInstance(SimpleTask::class.qualifiedName!!)
        if (t is SimpleTask) {
            assertThat(t.exec(executionContext, notRequired), equalTo("Hello World"))
        } else {
            fail("Didn't expect an instance of ${t::class}")
        }

        // cannot lookup by implementing clazzname, as the alias was used
        assertThat({ factory.createInstance(HelloWorldTask::class.qualifiedName!!) }, throws<TaskException>())
    }

    @Test
    fun `should fail to create task if no suitable constructor`() {
        val factory = TaskFactory()
        factory.register(TaskWithoutAGoodConstructor::class)

        assertThat(
            { factory.createInstance(TaskWithoutAGoodConstructor::class.qualifiedName!!) }, throws<TaskException>(
                has(
                    Exception::message,
                    present(equalTo("Couldn't find a suitable constructor for task: `mycorda.app.tasks.TaskWithoutAGoodConstructor`"))
                )
            )
        )
    }

    @Test
    fun `cannot register the same task twice`() {
        val factory = TaskFactory()
        factory.register(MultiplyTask::class)
        assertThat(
            { factory.register(MultiplyTask::class) }, throws<TaskException>(
                has(
                    Exception::message,
                    present(equalTo("`mycorda.app.tasks.MultiplyTask` is already registered"))
                )
            )
        )

        factory.register(HelloWorldTask::class, SimpleTask::class)
        assertThat(
            { factory.register(GoodbyeWorldTask::class, SimpleTask::class) }, throws<TaskException>(
                has(
                    Exception::message,
                    present(equalTo("`mycorda.app.tasks.SimpleTask` is already registered"))
                )
            )
        )
    }

    @Test
    fun `should lookup BlockingTask by class`() {
        val factory = TaskFactory()
        factory.register(MultiplyTask::class)
        factory.register(HelloWorldTask::class)

        // can lookup by class rather than string
        val multiplier = factory.createInstance(MultiplyTask::class)
        assert(multiplier is MultiplyTask)
        assertThat(multiplier.exec(executionContext, 10), equalTo(100))

        val helloWorld = factory.createInstance(HelloWorldTask::class)
        assert(helloWorld is HelloWorldTask)
        assertThat(helloWorld.exec(executionContext, notRequired), equalTo("Hello World"))
    }

    @Test
    fun `should lookup AsyncTask by class`() {
        // the DefaultAsyncResultChannelSinkFactory will support the "LOCAL" channel
        val sinkFactory = DefaultAsyncResultChannelSinkFactory()
        val registry = Registry().store(sinkFactory)
        val factory = TaskFactory(registry)
        factory.register(CalcSquareAsyncTask::class)

        val channelId = UniqueId.randomUUID()
        val locator = AsyncResultChannelSinkLocator.LOCAL
        val simpleTask = factory.createInstance(CalcSquareAsyncTask::class)

        // the SimpleAsyncTask returns immediately, so we don't have to wait
        simpleTask.exec(
            ctx = executionContext,
            channelLocator = locator,
            channelId = channelId,
            input = 10
        )

        val query = sinkFactory.channelQuery(locator)
        AsyncTask.sleepForTicks(2)
        assert(query.hasResult(channelId))
        assertThat(query.result<Int>(channelId) as Success<Int>, equalTo(Success(100)))
    }

    @Test
    // TODO - this test case doesn't really belong here, but currently the task client design \
    //       is still unstable
    fun `should work with TaskClient `() {

        // the DefaultAsyncResultChannelSinkFactory will support the "LOCAL" channel
        val sinkFactory = DefaultAsyncResultChannelSinkFactory()
        val registry = Registry().store(sinkFactory)
        val factory = TaskFactory(registry)
        factory.register(CalcSquareAsyncTask::class)

        val channelId = UniqueId.randomUUID()
        val locator = AsyncResultChannelSinkLocator("LOCAL")

        //val client = Async2TaskClientImpl()

//        client.execTask(taskClazz = CalcSquareAsyncTask::class.qualifiedName!!,
//            channelLocator = locator
//
//        )
        val simpleTask = factory.createInstance(CalcSquareAsyncTask::class)

        // the SimpleAsyncTask returns immediately, so we don't have to wait
        simpleTask.exec(
            ctx = executionContext,
            channelLocator = locator,
            channelId = channelId,
            input = 10
        )

        val query = sinkFactory.channelQuery(locator)
        AsyncTask.sleepForTicks(2)
        assert(query.hasResult(channelId))
        assertThat(query.result<Int>(channelId) as Success<Int>, equalTo(Success(100)))
    }
}

class MultiplyTask : BlockingTask<Int, Int> {
    private val taskId = UUID.randomUUID()
    override fun taskId(): UUID = taskId
    override fun exec(ctx: ExecutionContext, params: Int) = params * params
}

interface Calculator {
    fun calc(x: Int): Int
}

class Multiplier : Calculator {
    override fun calc(x: Int): Int = x * x
}

class Adder : Calculator {
    override fun calc(x: Int): Int = x + x
}

class CalculateTask(registry: Registry) : BlockingTask<Int, Int> {
    private val calculator = registry.get(Calculator::class.java)
    private val taskId = UUID.randomUUID()
    override fun taskId(): UUID = taskId
    override fun exec(ctx: ExecutionContext, params: Int): Int = calculator.calc(params)
}

interface SimpleTask : BlockingTask<NotRequired, String>

class HelloWorldTask() : SimpleTask {
    private val taskId = UUID.randomUUID()
    override fun exec(ctx: ExecutionContext, input: NotRequired): String = "Hello World"
    override fun taskId(): UUID = taskId
}

class GoodbyeWorldTask() : SimpleTask {
    private val taskId = UUID.randomUUID()
    override fun exec(ctx: ExecutionContext, input: NotRequired): String = "Goodbye, cruel World"
    override fun taskId(): UUID = taskId
}

// Tasks can either have a default constructor, or a constructor that takes a registry
class TaskWithoutAGoodConstructor(notAllowedConstructor: String) : Task {
    private val taskId = UUID.randomUUID()
    override fun taskId(): UUID = taskId
}
