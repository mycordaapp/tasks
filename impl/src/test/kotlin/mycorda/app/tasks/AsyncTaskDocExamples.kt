package mycorda.app.tasks

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import mycorda.app.tasks.demo.CalcSquareTask
import mycorda.app.tasks.executionContext.SimpleExecutionContext
import org.junit.Test

/**
Code to match examples in 'tasks.md' file
 */
class AsyncTaskDocExamples {

    @Test
    fun `should call task directly`() {
        val task = CalcSquareTask()
        val ctx = SimpleExecutionContext()
        val result = task.exec(ctx, 10)
        assertThat(result, equalTo(100))
    }
}