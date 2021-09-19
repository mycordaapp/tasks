package mycorda.app.tasks

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import mycorda.app.registry.Registry
import mycorda.app.tasks.demo.CalcSquareAsyncTask
import mycorda.app.tasks.executionContext.SimpleExecutionContext
import org.junit.Test

/**
Code to match examples in 'tasks.md' file
 */
class AsyncTaskDocExamples {

    @Test
    fun `should call task directly`() {
        // 1. Setup a result sink,
        val resultSinkFactory = DefaultAsyncResultChannelSinkFactory()
        val reg = Registry().store(resultSinkFactory)

        // 2. setup a channel for the results
        val locator = AsyncResultChannelSinkLocator.LOCAL
        val channelId = UniqueId.random()

        // 3. setup the task
        val task = CalcSquareAsyncTask(reg)
        val ctx = SimpleExecutionContext()

        // 4. call the task. The result will come back on the results channel
        task.exec(ctx, locator, channelId, 10)

        // 5. run a query over the result channel. In this
        //    CalcSquareAsyncTask is a demo and has returned a result immediately
        val query = resultSinkFactory.channelQuery(locator)

        // 6. assert expected results
        // not yet read
        assertThat(query.hasResult(channelId), equalTo(false))
        // wait long enough
        Thread.sleep(AsyncTask.platformTick() * 2)
        // now ready
        assert(query.hasResult(channelId))
        val result = query.result<Int>(channelId)
        assertThat(result, equalTo(Success<Int>(100) as AsyncResult<Int>))
    }
}