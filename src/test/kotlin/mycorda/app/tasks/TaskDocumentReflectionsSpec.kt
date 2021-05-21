package mycorda.app.tasks

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import mycorda.app.tasks.TaskDocumentReflections
import mycorda.app.tasks.demo.CalcSquareTask

import org.junit.platform.runner.JUnitPlatform
import org.junit.runner.RunWith
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

@RunWith(JUnitPlatform::class)
object TaskDocumentReflectionsSpec : Spek({
    describe("Should read description if available") {
        val t = CalcSquareTask()
        val reflections = TaskDocumentReflections(t)
        assertThat(reflections.description(), equalTo("An example Task that calculates the square of a number"))
    }


    describe("Should read description if available") {
        val t = CalcSquareTask()
        val reflections = TaskDocumentReflections(t)
        assertThat(reflections.description(), equalTo("An example Task that calculates the square of a number"))
    }


})