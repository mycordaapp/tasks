package mycorda.app.tasks

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import mycorda.app.registry.Registry
import mycorda.app.tasks.client.SimpleClientContext
import mycorda.app.tasks.client.SimpleTaskClient
import mycorda.app.tasks.demo.CalcSquareAsyncTask
import mycorda.app.tasks.demo.CalcSquareTask
import mycorda.app.tasks.demo.echo.EchoIntTask
import mycorda.app.tasks.demo.echo.EchoStringTask
import mycorda.app.tasks.executionContext.SimpleExecutionContext
import mycorda.app.tasks.logging.DefaultLoggingChannelFactory
import mycorda.app.tasks.logging.LoggingChannelLocator
import mycorda.app.tasks.logging.LogLevel
import mycorda.app.tasks.test.ListDirectoryTask
import mycorda.app.tasks.test.ListDirectoryTaskFake
import mycorda.app.tasks.test.ListDirectoryTaskImpl
import mycorda.app.types.StringList
import org.junit.jupiter.api.Test

/**
Code to match examples in 'tasks.md' file
 */
class TaskDocExamples {

    @Test
    fun `should call task directly`() {
        val task = CalcSquareTask()
        val ctx = SimpleExecutionContext()
        val result = task.exec(ctx, 10)
        assertThat(result, equalTo(100))
    }

    @Test
    fun `should call task via the TaskFactory`() {
        // register a real task
        val liveFactory = TaskFactory()
        liveFactory.register(ListDirectoryTaskImpl::class, ListDirectoryTask::class)

        // create by class
        val taskByClass = liveFactory.createInstance(ListDirectoryTask::class)
        val ctx = SimpleExecutionContext()
        assert(taskByClass.exec(ctx, ".").contains("build.gradle"))

        // create by name
        @Suppress("UNCHECKED_CAST")
        val taskByName =
            liveFactory.createInstance("mycorda.app.tasks.test.ListDirectoryTask") as BlockingTask<String, List<String>>
        assert(taskByName.exec(ctx, ".").contains("build.gradle"))

        // register and create a fake task
        val fakeFactory = TaskFactory()
        fakeFactory.register(ListDirectoryTaskFake::class, ListDirectoryTask::class)
        val fakeTask = fakeFactory.createInstance(ListDirectoryTask::class)
        assert(fakeTask.exec(ctx, ".").contains("fake.txt"))
    }

    @Test
    fun `should call task via a task client`() {

        // 1. register a real task in the TaskFactory (server side)
        val taskFactory = TaskFactory()
        taskFactory.register(ListDirectoryTaskFake::class, ListDirectoryTask::class)
        val registry = Registry().store(taskFactory)

        // 2. register LogChannelFactory
        val logChannelFactory = DefaultLoggingChannelFactory()
        registry.store(logChannelFactory)

        // 3. get a task client (client side)
        val taskClient = SimpleTaskClient(registry)

        // 4. call the client
        val clientContext = SimpleClientContext()
        val result = taskClient.execBlocking(
            clientContext,
            "mycorda.app.tasks.test.ListDirectoryTask", ".", StringList::class
        )

        // 5. assert results
        assert(result.contains("fake.txt"))

        // 6. assert logging output
        val logQuery = logChannelFactory.channelQuery(LoggingChannelLocator.local())
        assertThat(
            logQuery.stdout(),
            equalTo(
                "ListDirectoryTask:\n" +
                        "   params: .\n"
            )
        )
        assert(
            logQuery.messages()
                .hasMessage(LogLevel.INFO, "listing directory '.'")
        )
    }

    @Test
    fun `should register groups of tasks using TaskRegistrations`() {
        // 1. setup some groups using SimpleTaskRegistrations - this emulates
        //    the TaskRegistrations exposed by one or more Jar files
        class CalculatorTasks : SimpleTaskRegistrations(
            listOf(TaskRegistration(CalcSquareAsyncTask::class), TaskRegistration(CalcSquareTask::class))
        )

        val calculatorTasksClazzName = CalculatorTasks::class.java.name

        class EchoTasks : SimpleTaskRegistrations(
            listOf(TaskRegistration(EchoStringTask::class), TaskRegistration(EchoIntTask::class))
        )


        // 2. register the groups
        val taskFactory = TaskFactory()
        // by clazzName - this emulates the scenario in which registrations are controlled by external configs
        taskFactory.register(TaskRegistrations.fromClazzName(calculatorTasksClazzName))
        // by instantiating an instance - this emulate the scenario of hard coded registrations
        taskFactory.register(EchoTasks())

        // 3. check the tasks can be created
        val ctx = SimpleExecutionContext()
        assertThat(taskFactory.createInstance(CalcSquareTask::class).exec(ctx, 10), equalTo(100))
        assertThat(taskFactory.createInstance(EchoStringTask::class).exec(ctx, "foo"), equalTo("foo"))
    }

}