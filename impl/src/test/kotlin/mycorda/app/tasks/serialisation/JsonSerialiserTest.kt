package mycorda.app.tasks.serialisation

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import mycorda.app.helpers.random
import mycorda.app.tasks.StringList
import mycorda.app.tasks.demo.echo.DemoModel
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.Test
import java.lang.Exception
import java.lang.RuntimeException
import java.math.BigDecimal
import java.util.*
import kotlin.collections.ArrayList

class JsonSerialiserTest {
    private val serialiser = JsonSerialiser()
    private val random = Random()

    @Test
    fun `should round-trip data`() {
        val examples = listOf(
            random.nextInt(),
            random.nextLong(),
            random.nextDouble(),
            random.nextFloat(),
            random.nextBoolean(),
            BigDecimal.valueOf(random.nextDouble()),
            String.random(10),
            UUID.randomUUID(),
            DemoModel()
            //StringList(listOf("Mary", "had", "a", "little", "lamb"))
            //RuntimeException("This went wrong")
        )

        examples.forEach {
            try {
                assertThat(
                    it,
                    equalTo(roundTrip(it))
                ) { "Failed to round-trip $it of class ${it::class.qualifiedName}" }
            } catch (ex: Exception) {
                fail("Exception ${ex.message} for round-trip $it of class ${it::class.qualifiedName}")
            }
        }
    }

    @Test
    fun `should map to SerialisationPacket`() {
        val examples = listOf(
            random.nextInt(),
            random.nextLong(),
            random.nextDouble(),
            random.nextFloat(),
            random.nextBoolean(),
            BigDecimal.valueOf(random.nextDouble()),
            String.random(10),
            UUID.randomUUID(),
            DemoModel(),
            StringList(listOf("Mary", "had", "a", "little", "lamb")),
            mapOf<String, Any>("name" to "Paul"),
            RuntimeException("This went wrong")
        )

        examples.forEach {
            try {
                serialiser.mapDataToSerialisationPacket(it)
            } catch (ex: Exception) {
                fail("Exception ${ex.message} for mapDataToSerialisationPacket $it of class ${it::class.qualifiedName}")
            }
        }


    }

    @Test
    fun `should not map to SerialisationPacket for unsupported types`() {
        val examples = listOf(
            Pair(ArrayList<String>(), "Raw List classes are not allowed. Must use a subclass"),
            Pair(Date(), "Don't know how to serialise class java.util.Date")
        )

        examples.forEach {
            try {
                serialiser.mapDataToSerialisationPacket(it.first)
                fail("should have thrown an execption")
            } catch (ex: Exception) {
                assertThat(ex.message, equalTo(it.second))
            }
        }


    }


    //fun `should not serialize unsupported


    private fun roundTrip(data: Any): Any {
        @Suppress("UNCHECKED_CAST")
        return serialiser.deserialiseData(serialiser.serialiseData(data), data::class) as Any
    }
}