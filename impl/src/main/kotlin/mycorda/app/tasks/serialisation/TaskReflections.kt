package mycorda.app.tasks.serialisation;

import mycorda.app.tasks.NotRequired
import java.lang.Exception
import java.math.BigDecimal
import java.util.*
import kotlin.collections.ArrayList
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
                    || (clazz == BigDecimal::class)

        }

        fun isEnum(type: KClass<out Any>) = type.isSubclassOf(Enum::class)


        fun isUnit(clazz: KClass<*>): Boolean {
            return (clazz == Unit::class)
                    || (clazz == Nothing::class)
        }

        fun isNotRequired(clazz: KClass<*>): Boolean {
            return (clazz == NotRequired::class)
        }

        fun isDataClass(clazz: KClass<*>): Boolean {
            return (clazz.isData)
        }

        fun isException(clazz: KClass<*>): Boolean {
            return (clazz.isSubclassOf(Exception::class))
        }

        fun isMap(clazz: KClass<*>): Boolean {
            return (clazz.isSubclassOf(Map::class))
        }

        fun isListSubclass(clazz: KClass<*>): Boolean {
            return (clazz.isSubclassOf(List::class))
        }

        fun isMapOfStrings(any: Any): Boolean {
            return if (any is Map<*, *>) {
                any.filterKeys { !(it is String) }.isEmpty()
            } else {
                false
            }
        }

        fun isRawList(clazz: KClass<out Any>): Boolean {
            return (clazz == ArrayList::class) || (clazz == LinkedList::class)
        }

    }

}