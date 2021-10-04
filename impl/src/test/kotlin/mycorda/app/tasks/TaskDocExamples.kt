package mycorda.app.tasks

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import mycorda.app.registry.Registry
import mycorda.app.tasks.client.SimpleClientContext
import mycorda.app.tasks.client.SimpleTaskClient
import mycorda.app.tasks.demo.CalcSquareTask
import mycorda.app.tasks.executionContext.SimpleExecutionContext
import mycorda.app.tasks.logging.LogLevel
import org.junit.Test

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
            liveFactory.createInstance("mycorda.app.tasks.ListDirectoryTask") as BlockingTask<String, List<String>>
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

        // 2. get a task client (client side)
        val taskClient = SimpleTaskClient(registry)

        // 3. call the client
        val clientContext = SimpleClientContext()
        val result = taskClient.execBlocking(
            clientContext,
            "mycorda.app.tasks.ListDirectoryTask", ".", StringList::class
        )

        // 4. assert results
        assert(result.contains("fake.txt"))

        // 5. assert logging output
        assertThat(
            clientContext.inMemoryLoggingContext().stdout(),
            equalTo(
                "ListDirectoryTask:\n" +
                        "   params: .\n"
            )
        )
        assert(
            clientContext.inMemoryLoggingContext().messages()
                .hasMessage(LogLevel.INFO, "listing directory '.'")
        )
    }

}