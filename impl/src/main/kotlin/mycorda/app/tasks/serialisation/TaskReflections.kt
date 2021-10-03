package mycorda.app.tasks.serialisation;

import mycorda.app.tasks.NotRequired
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf


/**
 * Examine a Task via reflections to extract meta data to drive other
 * layers such as JSON mappings
 */
class TaskReflections {

    companion object {
        fun isScalar(clazz: KClass<*>): Boolean {
            return (clazz == Int::class)
                    || (clazz == Long::class)
                    || (clazz == Double::class)
                    || (clazz == String::class)
                    || (clazz == Float::class)
                    || (clazz == Boolean::class)
                    || (clazz == String::class)
                    || (clazz == UUID::class)

        }

        fun isEnum(type: KClass<out Any>) = type.isSubclassOf(Enum::class)


        fun isUnit(clazz: KClass<*>): Boolean {
            return (clazz == Unit::class)
                    || (clazz == Nothing::class)
        }

        fun isNotRequired(clazz: KClass<*>): Boolean {
            return (clazz == NotRequired::class)
        }

    }

}