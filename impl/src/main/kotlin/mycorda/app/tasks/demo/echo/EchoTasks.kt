package mycorda.app.tasks.demo.echo

import mycorda.app.helpers.random
import mycorda.app.registry.Registry
import mycorda.app.tasks.*
import mycorda.app.tasks.executionContext.ExecutionContext
import java.math.BigDecimal
import java.util.*

data class DemoModel(
    val aString: String = String.random(80),
    val anInt: Int = Random().nextInt(),
    val aLong: Long = Random().nextLong(),
    val aDouble: Double = Random().nextDouble(),
    val aFloat: Float = Random().nextFloat(),
    val aBoolean: Boolean = Random().nextBoolean(),
    val nested: DemoModel? = null
)

/**
 * Tasks that simply echo the result back. Good for basic testing
 * of communication channels and serialisation
 */

class EchoIntTask : BaseBlockingTask<Int, Int>() {
    override fun exec(ctx: ExecutionContext, input: Int): Int {
        return input
    }
}

class EchoLongTask : BaseBlockingTask<Long, Long>() {
    override fun exec(ctx: ExecutionContext, input: Long): Long {
        return input
    }
}

class EchoDoubleTask : BaseBlockingTask<Double, Double>() {
    override fun exec(ctx: ExecutionContext, input: Double): Double {
        return input
    }
}

class EchoFloatTask : BaseBlockingTask<Float, Float>() {
    override fun exec(ctx: ExecutionContext, input: Float): Float {
        return input
    }
}

class EchoBooleanTask : BaseBlockingTask<Boolean, Boolean>() {
    override fun exec(ctx: ExecutionContext, input: Boolean): Boolean {
        return input
    }
}

class EchoStringTask : BaseBlockingTask<String, String>() {
    override fun exec(ctx: ExecutionContext, input: String): String {
        return input
    }
}

class EchoBigDecimalTask : BaseBlockingTask<BigDecimal, BigDecimal>() {
    override fun exec(ctx: ExecutionContext, input: BigDecimal): BigDecimal {
        return input
    }
}

class EchoUUIDTask : BaseBlockingTask<UUID, UUID>() {
    override fun exec(ctx: ExecutionContext, input: UUID): UUID {
        return input
    }
}

class EchoStringListTask : BaseBlockingTask<StringList, StringList>() {
    override fun exec(ctx: ExecutionContext, input: StringList): StringList {
        return input
    }
}

class EchoDemoModelTask : BaseBlockingTask<DemoModel, DemoModel>() {
    override fun exec(ctx: ExecutionContext, input: DemoModel): DemoModel {
        return input
    }
}

abstract class BaseEchoAsyncTask<I, O>(registry: Registry) : BaseAsyncTask<I, O>() {
    protected val resultChannelFactory = registry.get(AsyncResultChannelSinkFactory::class.java)

    protected fun submitResultWithDelay(
        ctx: ExecutionContext,
        channelLocator: AsyncResultChannelSinkLocator,
        result: AsyncResultChannelMessage<I>
    ) {
        ctx.executorService().submit<Unit> {
            // 1. Get the results channel
            val resultChannel = resultChannelFactory.create(channelLocator)

            // 2. Simulate a delay
            Thread.sleep(AsyncTask.platformTick())

            // 3. Write the result and also echo to logging channels
            resultChannel.accept(result)
        }
    }
}

class EchoIntAsyncTask(registry: Registry) : BaseEchoAsyncTask<Int, Int>(registry) {
    override fun exec(
        ctx: ExecutionContext,
        channelLocator: AsyncResultChannelSinkLocator,
        channelId: UniqueId,
        input: Int
    ) {
        val result = AsyncResultChannelMessage(channelId, Success(input), Int::class.java)
        submitResultWithDelay(ctx, channelLocator, result)
    }
}

class EchoStringAsyncTask(registry: Registry) : BaseEchoAsyncTask<String, String>(registry) {
    override fun exec(
        ctx: ExecutionContext,
        channelLocator: AsyncResultChannelSinkLocator,
        channelId: UniqueId,
        input: String
    ) {
        val result = AsyncResultChannelMessage(channelId, Success(input), String::class.java)
        submitResultWithDelay(ctx, channelLocator, result)
    }
}

