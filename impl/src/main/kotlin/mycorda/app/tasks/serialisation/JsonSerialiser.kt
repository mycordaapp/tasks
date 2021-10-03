package mycorda.app.tasks.serialisation

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import mycorda.app.tasks.NotRequired
import java.util.*
import kotlin.reflect.KClass

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
}


