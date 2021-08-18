package  mycorda.app.tasks


import mycorda.app.registry.Registry
import java.lang.Exception
import kotlin.reflect.KClass


class TaskFactory2(private val registry: Registry = Registry()) {
    private val lookup = HashMap<String, KClass<out Task>>()

    /**
     * Register the class, taking the class  name as the registered name
     * - task = the implementingClass
     * - asTask = the interface (if different)
     */
    fun register(
        task: KClass<out Task>,
        asTask: KClass<out Task> = task
    ) {
        val name = asTask.qualifiedName!!
        if (lookup.containsKey(name)) throw TaskException("`$name` is already registered")
        lookup[name] = task
    }


    fun list(): List<String> {
        return lookup.keys.sorted()
    }


    /**
     * Create an instance of a Task by fully qualified name. This
     * is the "core" factory method, but in most cases one of the
     * more type safe variants will result in cleaner code.
     *
     */
    fun createInstance(qualifiedName: String): Task {
        if (!lookup.containsKey(qualifiedName)) {
            throw TaskException("Task: `$qualifiedName` is not registered")
        }
        val clazz = lookup[qualifiedName]!!

        // try with Registry
        clazz.constructors.forEach {
            if (it.parameters.size == 1) {
                val paramClazz = it.parameters[0].type.classifier as KClass<Any>
                if (paramClazz == Registry::class) {
                    try {
                        return it.call(registry)
                    } catch (ex: Exception) {
                        throw TaskException("Problem instantiating `$qualifiedName`. Original error: `${ex.message}`")
                    }
                }
            }
        }

        // try wih no params constructor
        clazz.constructors.forEach {
            if (it.parameters.isEmpty()) {
                try {
                    return it.call()
                } catch (ex: Exception) {
                    throw TaskException("Problem instantiating $qualifiedName. Original error ${ex.message}")
                }
            }
        }
        throw TaskException("Couldn't find a suitable constructor for task: `$qualifiedName`")
    }

}