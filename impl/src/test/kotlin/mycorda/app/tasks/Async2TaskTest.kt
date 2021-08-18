package mycorda.app.tasks

import mycorda.app.tasks.executionContext.ExecutionContext
import org.junit.Test
import java.util.*

class Async2TaskTest {

    @Test
    fun `simple task returns success`() {
        val f = DefaultAsyncResultChannelSinkFactory()
        val sinkLocator = AsyncResultChannelSinkLocator("LOCAL")
        val channelId = UniqueId.random()


        val x = Async2TaskClientImpl()




        x.execTask(
            taskClazz = "mycorda.app.tasks.SimpleAsync2Task",
            channelLocator = sinkLocator,
            channelId = channelId,
            input = 9
        )


    }

    /**
     *
     */
    class SimpleAsync2Task : Async2Task<Int, Int> {
        private val taskId = UUID.randomUUID()
        override fun exec(
            executionContext: ExecutionContext,
            channelLocator: AsyncResultChannelSinkLocator,
            channelId: UniqueId,
            input: Int
        ) {
            if (input != 42) {
                val x = Int::class.java as Class<Any>
                val result = AsyncResultChannelMessage(
                    channelId,
                    Success<Any>(input.times(input)),
                    x
                )
            }
        }

        override fun taskID(): UUID {
            return taskId
        }

    }
}