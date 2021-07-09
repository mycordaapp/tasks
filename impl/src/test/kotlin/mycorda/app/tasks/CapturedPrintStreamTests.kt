package mycorda.app.tasks

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import junit.framework.Assert.fail
import org.junit.Test
import java.io.File
import java.lang.RuntimeException
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.concurrent.thread


class CapturedPrintStreamTests {

    @Test
    fun `it should capture messages`() {
        val captured = InMemoryCapturedPrintStream()
        captured.printStream().println("hello")
        captured.printStream().println("world")

        assertThat(captured.captured(), equalTo("hello\nworld\n"))
    }

    @Test
    fun `it should support concurrent writers`() {
        val capturedPrintStream = InMemoryCapturedPrintStream()
        val threadCount = 1000
        val iterations = 1000
        val messages = ConcurrentLinkedQueue<String>()

        (1..threadCount).forEach { t ->
            thread {
                (1..iterations).forEach {
                    try {
                        val message = "thread:$t iter:$it"
                        capturedPrintStream.printStream().println(message)
                        messages.add(message)
                        Thread.sleep(Random().nextInt(5).toLong())   // random delay within thread
                    } catch (ex: Exception) {
                        fail("opps, thread exception - $ex")
                    }
                }
            }
            Thread.sleep(Random().nextInt(5).toLong()) // random delay in between starting threads
        }

        // wait long enough for all thread to complete.
        Thread.sleep(((iterations * 5) + (threadCount * 5)).toLong())

        val captured = capturedPrintStream.captured()

        assertThat(captured.removeSuffix("\n").lines().count(), equalTo(threadCount * iterations))

        val x = captured.removeSuffix("\n").lines().sorted().joinToString(separator = "\n")
        val y = messages.toList().sorted().joinToString(separator = "\n")

        if (x != y) {
            File("captured.txt").writeText(x)
            File("message.txt").writeText(y)
            fail("they don't match :( - look in `captured.txt` and `message.txt`")
        }
    }

    // TODO - need to write a better test
//    @Test
//    fun `it should support concurrent reader`() {
//        val capturedPrintStream = InMemoryCapturedPrintStream()
//        val readerThreads = 10
//        val iterations = 1000
//        val messages = ConcurrentLinkedQueue<String>()
//
//        // writer thread
//        thread {
//            (1..iterations).forEach {
//                try {
//                    val message = it.toString().padStart(7, '0')
//                    capturedPrintStream.printStream().println(message)
//                    messages.add(message)
//                    Thread.sleep(Random().nextInt(5).toLong())   // random delay within thread
//                } catch (ex: Exception) {
//                    fail("opps, writer exception - $ex")
//                }
//            }
//        }
//
//
//        (1..readerThreads).forEach { theadNum ->
//            thread {
//                var last = 0
//                var foundEnd = false
//                val captured = capturedPrintStream.captured()
//                captured.removeSuffix("\n").lines().forEach {
//                    //if (!foundEnd && it.isEmpty())
//                    val line = it.toInt()
//                    if (line != last+1){
//                        println(captured)
//                        throw RuntimeException("fail in $theadNum, expected line ${last+1}, found $line}")
//                    }
//                    else {
//                        last = line
//                    }
//                }
//                Thread.sleep(Random().nextInt(5).toLong())   // random delay within thread
//            }
//            Thread.sleep(Random().nextInt(5).toLong()) // random delay in between reader threads
//        }
//
//
//        Thread.sleep(((iterations * 5) + (readerThreads * 5)).toLong())
//
//    }
}
