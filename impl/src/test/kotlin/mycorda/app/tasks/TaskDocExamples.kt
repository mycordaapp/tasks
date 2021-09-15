package mycorda.app.tasks

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import mycorda.app.registry.Registry
import mycorda.app.tasks.demo.CalcSquareTask
import mycorda.app.tasks.executionContext.DefaultExecutionProducerContext
import mycorda.app.tasks.executionContext.DefaultExecutionContextFactory
import mycorda.app.tasks.logging.InMemoryLogMessageSink
import mycorda.app.tasks.processManager.ProcessManager
import org.junit.Test

/**
Code to match examples in 'tasks.md' file
 */
class TaskDocExamples {

    @Test
    fun `should call task directly`() {
        val task = CalcSquareTask()
        val ctx = DefaultExecutionProducerContext()
        val result = task.exec(ctx, 10)
        assertThat(result, equalTo(100))
    }


    @Test
    fun `should call task via the TaskFactory`() {
        // register a real task
        val liveFactory = TaskFactory2()
        liveFactory.register(ListDirectoryTaskImpl::class, ListDirectoryTask::class)

        // create by class
        val taskByClass = liveFactory.createInstance(ListDirectoryTask::class)
        val ctx = DefaultExecutionProducerContext()
        assert(taskByClass.exec(ctx,".").contains("build.gradle"))

        // create by name
        val taskByName = liveFactory.createInstance("mycorda.app.tasks.ListDirectoryTask") as BlockingTask<String,List<String>>
        assert(taskByName.exec(ctx,".").contains("build.gradle"))

        // register and create a fake task
        val fakeFactory = TaskFactory2()
        fakeFactory.register(ListDirectoryTaskFake::class, ListDirectoryTask::class)
        val fakeTask= fakeFactory.createInstance(ListDirectoryTask::class)
        assert(fakeTask.exec(ctx,".").contains("fake.txt"))
    }

    @Test
    fun `should call task via a task client`() {
        val registry = Registry()
        val msgSink = InMemoryLogMessageSink()
        registry.store(msgSink)
        registry.store(ProcessManager())
        registry.store(FixedThreadPoolExecutor())
        registry.store(DefaultExecutionContextFactory(registry))
        //val es = FileEventStore()
        //registry.store(es)

        val factory = TaskFactory2()
        factory.register(ListDirectoryTaskImpl::class, ListDirectoryTask::class)
        registry.store(factory)


        val ctx = DefaultExecutionProducerContext()


    }

}