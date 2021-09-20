package mycorda.app.tasks

import org.junit.platform.runner.JUnitPlatform
import org.junit.runner.RunWith
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

@RunWith(JUnitPlatform::class)
object WaitForSpec : Spek({

    describe("A simple threaded WaitFor primitive") {

        it("should wait for function to complete ") {
            val waitFor = WaitFor<Unit>()

            val func = { Thread.sleep(100)}

            waitFor.wait(func)
            println("it worked!")
        }
    }
})
