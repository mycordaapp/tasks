package mycorda.app.tasks.inbuilt

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