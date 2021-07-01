package mycorda.app.tasks

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import mycorda.app.tasks.demo.CalcSquareTask


import org.junit.platform.runner.JUnitPlatform
import org.junit.runner.RunWith
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

@RunWith(JUnitPlatform::class)
object TaskReflectionsSpec : Spek({
    describe("Should determine the input param type") {
        val reflections  = TaskReflections(CalcSquareTask::class)
        assertThat(reflections.paramClass().simpleName,equalTo("Int"))
    }

    describe("Should determine the return param type") {
        val reflections  = TaskReflections(CalcSquareTask::class)
        assertThat(reflections.resultClass().simpleName,equalTo("Int"))
    }



})