package mycorda.app.tasks


//import kotlin.random.Random

//
//@RunWith(JUnitPlatform::class)
//object CapturedPrintStreamSpec : Spek({
//
//
//    describe("Test the InMemory implement") {
//
//        it("should support concurrent writers") {
//            val capturedPrintStream = InMemoryCapturedPrintStream()
//            val threadCount = 100
//            val iterations = 100
//
//            (1..threadCount).forEach {t ->
//                thread {
//                    (1..iterations).forEach {
//                        capturedPrintStream.printStream().println("thread:$t iter:$it")
//                        Thread.sleep(Random().nextLong(10))   // random delay in thread
//                    }
//                }
//                Thread.sleep(Random.nextLong(10)) // random delay in between thread
//            }
//
//            // wait long enough for all to complete
//            Thread.sleep((iterations * 10 * 2).toLong())
//
//            val captured = capturedPrintStream.captured()
////            println(">")
////            print(captured)
////            println(">")
//
//            // a very simple test for mixed up lines. Note, take of one for trailing new line on the last entry
//            assertThat(captured.lines().count()-1, equalTo(threadCount * iterations))
//        }
//    }
//
//
//    describe("Test the File implementation") {
//
//        it("should support concurrent writers") {
//            val suffix = String.random(6)
//
//            val capturedPrintStream = FileCapturedPrintStream(".testing/$suffix/captured.txt")
//            val threadCount = 100
//            val iterations = 100
//
//            (1..threadCount).forEach {t ->
//                thread {
//                    (1..iterations).forEach {
//                        capturedPrintStream.printStream().println("thread:$t iter:$it")
//                        Thread.sleep(Random.nextLong(10))   // random delay in thread
//                    }
//                }
//                Thread.sleep(Random.nextLong(10)) // random delay in between thread
//            }
//
//            // wait long enough for all to complete
//            Thread.sleep((iterations * 10 * 2).toLong())
//
//            val captured = capturedPrintStream.captured()
////            println(">")
////            print(captured)
////            println(">")
//
//            // a very simple test for mixed up lines. Note, take of one for trailing new line on the last entry
//            assertThat(captured.lines().count()-1, equalTo(threadCount * iterations))
//        }
//    }
//
//
//
//})