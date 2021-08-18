package mycorda.app.tasks

import com.natpryce.hamkrest.*
import com.natpryce.hamkrest.assertion.assertThat
import junit.framework.Assert.fail
import mycorda.app.registry.Registry
import mycorda.app.tasks.executionContext.DefaultExecutionContext
import mycorda.app.tasks.executionContext.ExecutionContext
import org.junit.Test
import java.util.*

class TaskFactory2Test {

    private val executionContext = DefaultExecutionContext()
    private val notRequired = NotRequired.instance()

    @Test
    fun `should store by class name`() {
        val factory = TaskFactory2()
        factory.register(MultiplyTask::class)

        val t = factory.createInstance(MultiplyTask::class.qualifiedName!!)
        assertThat(t::class.qualifiedName, equalTo(MultiplyTask::class.qualifiedName))
    }

    @Test
    fun `should construct using registry version of the constructor`() {
        // build CalculateTask with an Adder in the registry
        val factory1 = TaskFactory2(Registry().store(Adder()))
        factory1.register(CalculateTask::class)
        val sumCalculator = factory1.createInstance(CalculateTask::class.qualifiedName!!)
        assertThat(sumCalculator::class.qualifiedName, equalTo(CalculateTask::class.qualifiedName!!))
        assertThat((sumCalculator as CalculateTask).exec(DefaultExecutionContext(), 10), equalTo(20))

        // build CalculateTask with an Multiplier in the registry
        val factory2 = TaskFactory2(Registry().store(Multiplier()))
        factory2.register(CalculateTask::class)
        val multiplyCalculator = factory2.createInstance(CalculateTask::class.qualifiedName!!)
        assertThat((multiplyCalculator as CalculateTask).exec(DefaultExecutionContext(), 10), equalTo(100))

        // build CalculateTask with nothing in the registry
        val factory3 = TaskFactory2(Registry())
        factory3.register(CalculateTask::class)
        assertThat(
            { factory3.createInstance(CalculateTask::class.qualifiedName!!) },
            throws<TaskException>(
                has(
                    Exception::message,
                    present(equalTo("Problem instantiating `mycorda.app.tasks.CalculateTask`. Original error: `null`"))
                )
            )
        )
    }

    @Test
    fun `should lookup task by interface name if provided`() {
        val factory = TaskFactory2()
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
        val factory = TaskFactory2()
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
        val factory = TaskFactory2()
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

}

class MultiplyTask : BlockingTask<Int, Int> {
    private val taskId = UUID.randomUUID()
    override fun taskID(): UUID {
        return taskId
    }

    override fun exec(ctx: ExecutionContext, params: Int): Int {
        return params * params
    }
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
    val calculator = registry.get(Calculator::class.java)
    private val taskId = UUID.randomUUID()
    override fun taskID(): UUID {
        return taskId
    }

    override fun exec(ctx: ExecutionContext, params: Int): Int {
        return calculator.calc(params)
    }
}

interface SimpleTask : BlockingTask<NotRequired, String>

class HelloWorldTask() : SimpleTask {
    private val taskId = UUID.randomUUID()
    override fun exec(ctx: ExecutionContext, params: NotRequired): String = "Hello World"
    override fun taskID(): UUID = taskId
}

class GoodbyeWorldTask() : SimpleTask {
    private val taskId = UUID.randomUUID()
    override fun exec(ctx: ExecutionContext, params: NotRequired): String = "Goodbye, cruel World"
    override fun taskID(): UUID = taskId
}

// Tasks can either have a default constructor, or a constructor that takes a registry
class TaskWithoutAGoodConstructor(notAllowed: String) : Task {
    private val taskId = UUID.randomUUID()
    override fun taskID(): UUID = taskId
}