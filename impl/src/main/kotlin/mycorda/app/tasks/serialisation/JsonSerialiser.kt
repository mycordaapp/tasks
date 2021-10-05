package mycorda.app.tasks.serialisation

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import mycorda.app.tasks.NotRequired
import java.lang.Exception
import java.util.*
import kotlin.reflect.KClass


/**
 * Represents what can be passed. Only one of the 5 options
 * can be filled in
 */
data class SerialisationPacket(
    val scalar: Any? = null,
    val data: Any? = null,
    val list: Any? = null,
    val map: Map<String, Any?>? = null,
    val exception: Exception? = null
) {
    init {
        // can have at most one option
        assert(listOf().filterNotNull().size <= 1)
    }

    private fun listOf(): List<Any?> = listOf(scalar, data, list, map, exception)
    fun isEmpty() = listOf().filterNotNull().isEmpty()
}

class JsonSerialiser {
    private val mapper: ObjectMapper = ObjectMapper()

    init {
        val module = KotlinModule()
        mapper.registerModule(module)
    }

    fun serialiseBlockingTaskRequest(model: BlockingTaskRequest): String {
        return mapper.writeValueAsString(model)
    }

    fun deserialiseBlockingTaskRequest(json: String): BlockingTaskRequest {
        return mapper.readValue(json, BlockingTaskRequest::class.java)
    }


    fun deserialiseData(data: String, clazz: KClass<out Any>): Any? {
        if (TaskReflections.isUnit(clazz)) {
            if (data.isNotBlank()) throw RuntimeException("doDeserialize found data '$data' when Unit / Nothing is expected")
            return Unit
        }

        if (TaskReflections.isNotRequired(clazz)) {
            if (data.isNotBlank()) throw RuntimeException("doDeserialize found data '$data' when NotRequired is expected")
            return NotRequired.instance()
        }

        return if (TaskReflections.isScalar(clazz)) {
            when (clazz.simpleName) {
                "Int" -> data.toInt()
                "Long" -> data.toLong()
                "BigDecimal" -> data.toBigDecimal()
                "Boolean" -> data.toBoolean()
                "Double" -> data.toDouble()
                "Float" -> data.toFloat()
                "String" -> data
                "UUID" -> UUID.fromString(data)
                else -> throw RuntimeException("Don't know about scalar ${clazz.simpleName}")
            }
        } else {
            mapper.readValue(data, clazz.java)
        }
    }

    fun serialiseData(data: Any, prettyPrint: Boolean = false): String {
        val clazz = data::class

        if (data is Unit) return ""
        if (data is Nothing) return ""
        if (data is NotRequired) return ""

        return if (TaskReflections.isScalar(clazz)) {
            when (clazz.simpleName) {
                "Int" -> data.toString()
                "Long" -> data.toString()
                "BigDecimal" -> data.toString()
                "Boolean" -> data.toString()
                "Double" -> data.toString()
                "Float" -> data.toString()
                "String" -> data.toString()
                "UUID" -> data.toString()
                else -> throw RuntimeException("Don't know about scalar ${clazz.simpleName}")
            }
        } else {
            if (prettyPrint) {
                mapper.writerWithDefaultPrettyPrinter().writeValueAsString(data)
            } else {
                mapper.writeValueAsString(data)
            }
        }
    }


    fun serialiseData2(data: Any, prettyPrint: Boolean = false): String {
        val clazz = data::class

        if (data is Unit) return ""
        if (data is Nothing) return ""
        if (data is NotRequired) return ""

        return if (TaskReflections.isScalar(clazz)) {
            when (clazz.simpleName) {
                "Int" -> data.toString()
                "Long" -> data.toString()
                "BigDecimal" -> data.toString()
                "Boolean" -> data.toString()
                "Double" -> data.toString()
                "Float" -> data.toString()
                "String" -> data.toString()
                "UUID" -> data.toString()
                else -> throw RuntimeException("Don't know about scalar ${clazz.simpleName}")
            }
        } else {
            if (prettyPrint) {
                mapper.writerWithDefaultPrettyPrinter().writeValueAsString(data)
            } else {
                mapper.writeValueAsString(data)
            }
        }
    }

    fun mapDataToSerialisationPacket(data: Any): SerialisationPacket {
        val clazz = data::class

        if (data is Unit) return SerialisationPacket()
        if (data is Nothing) return SerialisationPacket()
        if (data is NotRequired) return SerialisationPacket()

        if (TaskReflections.isRawList(clazz)) throw RuntimeException("Raw List classes are not allowed. Must use a subclass")
        if (TaskReflections.isScalar(clazz)) return SerialisationPacket(scalar = data)
        if (TaskReflections.isDataClass(clazz)) return SerialisationPacket(data = data)
        if (TaskReflections.isListSubclass(clazz)) return SerialisationPacket(list = data)
        if (TaskReflections.isMapOfStrings(data)) return SerialisationPacket(map = data as Map<String, Any?>)
        if (TaskReflections.isException(clazz)) return SerialisationPacket(exception = data as Exception)

        throw RuntimeException("Don't know how to serialise class ${data::class.qualifiedName}")
    }


}


