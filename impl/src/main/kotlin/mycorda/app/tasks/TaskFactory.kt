package  mycorda.app.tasks


import mycorda.app.registry.Registry
import kotlin.reflect.KClass


class TaskFactory(private val registry: Registry) {
    private val lookup = HashMap<String, KClass<out Task>>()

    /**
     * Register the class, taking the class simple name as the registered name
     */
    fun register(t: KClass<out Task>) {

            register(t.simpleName!!
                    .removeSuffix("Impl")
                    .removeSuffix("Fake")
                    .removeSuffix("Task"), t)

    }


    /**
     * Register the class with a custom name
     */
    fun register(name: String, t: KClass<out Task>) {
        if (lookup.containsKey(name)) throw RuntimeException("$name is already registered")
        lookup[name] = t
    }

    fun list(): List<String> {
        return lookup.keys.sorted()
    }


    fun createInstance(name: String): Task {
        val clazz = lookup[name]!!

        // try with Registry
        clazz.constructors.forEach {
            if (it.parameters.size == 1) {
                val paramClazz = it.parameters[0].type.classifier as KClass<Any>
                if (paramClazz == Registry::class) {
                    return it.call(registry)
                }
            }
        }

        // try wih no params constructor
        clazz.constructors.forEach {
            if (it.parameters.isEmpty()) {
                return it.call()
            }
        }
        throw RuntimeException("Couldn't find a suitable constructor")
    }

}