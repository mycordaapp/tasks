package mycorda.app.tasks

import mycorda.app.types.NotRequired
import java.io.File
import java.net.URL
import java.util.*
import java.util.concurrent.Future
import kotlin.reflect.KClass
import kotlin.reflect.full.functions
import kotlin.reflect.full.isSubclassOf

/**
 * Examine a Task via reflections to extract meta data to drive other
 * layers such as JSON mappings
 */
class TaskReflections(val t: KClass<out Task>) {

    companion object {
        fun isScalar(clazz: KClass<*>): Boolean {
            return (clazz == Int::class)
                    || (clazz == Long::class)
                    || (clazz == Double::class)
                    || (clazz == String::class)
                    || (clazz == Float::class)
                    || (clazz == Boolean::class)
                    || (clazz == String::class)
                    || (clazz == File::class)
                    || (clazz == UUID::class)
                    || (clazz == URL::class)
        }

        fun isEnum(type: KClass<out Any>) = type.isSubclassOf(Enum::class)


        fun isUnit(clazz: KClass<*>): Boolean {
            return (clazz == Unit::class)
                    || (clazz == Nothing::class)
        }

        fun isNotRequired(clazz: KClass<*>): Boolean {
            return (clazz == NotRequired::class)
        }

        fun isFuture(clazz: KClass<*>): Boolean {
            return (clazz == Future::class)

        }
    }

    fun paramClass(): KClass<out Any> {
        val execMethod = t.functions.single { it.name == "exec" }
        val type = execMethod.parameters[2].type
        @Suppress("UNCHECKED_CAST")
        return type.classifier as KClass<Any>
    }

    fun resultClass(): KClass<out Any> {
        val execMethod = t.functions.single { it.name == "exec" }
        @Suppress("UNCHECKED_CAST")
        return execMethod.returnType.classifier as KClass<Any>
    }

    fun isParamOptional(): Boolean {
        val execMethod = t.functions.single { it.name == "exec" }
        return (execMethod.parameters[2].type.isMarkedNullable)
    }

//    fun isScalar(clazz: KClass<*>): Boolean {
//        return TaskReflections.isScalar(clazz)
//    }
//
//    fun isUnit(clazz: KClass<*>): Boolean {
//        return TaskReflections.isUnit(clazz)
//    }
//
//    fun isFuture(clazz: KClass<*>): Boolean {
//        return TaskReflections.isFuture(clazz)
//    }


}