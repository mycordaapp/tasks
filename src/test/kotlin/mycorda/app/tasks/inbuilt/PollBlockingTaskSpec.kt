package mycorda.app.tasks.inbuilt

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import mycorda.app.tasks.NotRequired
import mycorda.app.tasks.executionContext.DefaultExecutionContext

import org.junit.platform.runner.JUnitPlatform
import org.junit.runner.RunWith
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
//
//
//@RunWith(JUnitPlatform::class)
//object PollBlockingTaskSpec : Spek({
//
//    describe("the PollBlockingTask ") {
//
//        it("wait for the operation to return") {
//            val statusTask = StatusChangeTask<NotRequired, String>(before = "waiting", after = "ready", delay = 50)
//
//            val poller = PollBlockingTask(task = statusTask,
//                    successMapper = { it == "ready" },
//                    intervalMs = 10,
//                    maxWaitMs = 100)
//            val result = poller.exec(DefaultExecutionContext(), NotRequired.instance())
//
//            assertThat(result, equalTo("ready"))
//        }
//
//    }
//})