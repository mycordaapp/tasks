package mycorda.app.tasks

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import mycorda.app.registry.Registry
import mycorda.app.tasks.demo.CalcSquareTask
import mycorda.app.tasks.executionContext.InMemoryLogMessageSink
import mycorda.app.tasks.executionContext.LogFormat

import org.junit.platform.runner.JUnitPlatform
import org.junit.runner.RunWith
import org.spekframework.spek2.Spek
import org.spekframework.spek2.meta.Ignore
import org.spekframework.spek2.style.specification.describe

@Ignore
@RunWith(JUnitPlatform::class)
object TaskFactorySpec : Spek({

    val messageSink = InMemoryLogMessageSink(LogFormat.Test)

    beforeEachTest {
        messageSink.clear()
    }

    describe("Creating a simple task") {
        val factory = TaskFactory(Registry())
        factory.register(CalcSquareTask::class)
        val task = factory.createInstance("CalcSquare")

        assertThat(task::class.qualifiedName, equalTo("net.corda.ccl.commons.tasks.demo.CalcSquareTask") )
        val result = (task as BlockingTask<Int,Int>).exec(params = 9)

        assertThat(result, equalTo(81) )
    }

    describe("Creating a task that needs the registry") {
        val factory = TaskFactory(Registry().store("foo"))
        factory.register(RegistryTask::class)
        val task = factory.createInstance("Registry")

        assertThat(task::class.qualifiedName, equalTo("net.corda.ccl.tasks.RegistryTask") )
        val result = (task as BlockingTask<Unit?,String>).exec(params = null)

        assertThat(result, equalTo("foo") )
    }

    describe("Creating a Fake task") {
        val factory = TaskFactory(Registry())
        factory.register(ExampleTaskFake::class)
        val task = factory.createInstance("Example") as ExampleTask

        // fake version returns a fixed result
        assertThat(task.exec(params = 10), equalTo(99) )
        assertThat(task.exec(params = 100), equalTo(99) )
    }
})