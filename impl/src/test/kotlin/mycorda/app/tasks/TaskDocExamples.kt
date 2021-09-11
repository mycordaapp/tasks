package mycorda.app.tasks

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import mycorda.app.tasks.demo.CalcSquareTask
import mycorda.app.tasks.executionContext.DefaultExecutionContext
import org.junit.Test

/**
Code to match examples in 'tasks.md' file
 */
class TaskDocExamples {

    @Test
    fun `should call task directly`() {
        val task = CalcSquareTask()
        val ctx = DefaultExecutionContext()
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
        val ctx = DefaultExecutionContext()
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

}