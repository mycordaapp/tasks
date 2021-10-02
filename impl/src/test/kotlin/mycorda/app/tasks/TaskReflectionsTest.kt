package mycorda.app.tasks

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import mycorda.app.tasks.demo.CalcSquareTask
import org.junit.jupiter.api.Test

class TaskReflectionsTest {

    @Test
    fun `Should determine the input param type`() {
        val reflections = TaskReflections(CalcSquareTask::class)
        assertThat(reflections.paramClass().simpleName, equalTo("Int"))
    }

    @Test
    fun `Should determine the return param type`() {
        val reflections = TaskReflections(CalcSquareTask::class)
        assertThat(reflections.resultClass().simpleName, equalTo("Int"))
    }

}