package mycorda.app.tasks

import mycorda.app.helpers.random
import mycorda.app.tasks.executionContext.ExecutionContext
import java.io.File


/**
 * One class for all location information. The implementing class should ensure that the
 * requested directories exist.
 */
interface Locations {

    /**
     * The home directory for a service
     */
    fun serviceHomeDirectory(product: String, service: String, instance: String? = null): String

    /**
     * The home directory for a service, picking up the
     * instance qualifier from the execution context
     */
    fun serviceHomeDirectory(ctx: ExecutionContext, product: String, service: String): String {
        return serviceHomeDirectory(product, service, ctx.instanceQualifier()?.toLowerCase())
    }


    /**
     * Where any cached data is stored.
     */
    fun cacheDirectory(): String

    /**
     * Where any persistent data is stored
     */
    fun dataDirectory(): String

    /**
     * The place for any temporary files. Applications are responsible for ensuring the uniqueness of any
     * files stored here
     */
    fun tempDirectory(): String

}


// for use in test cases
class TestLocations(private val suffix: String = String.random(),
                    private val useGlobalCache: Boolean = true) : Locations {
    init {
        File(cacheDirectory()).mkdirs()
        File(dataDirectory()).mkdirs()
        File(tempDirectory()).mkdirs()
    }

    override fun cacheDirectory(): String {
        return if (useGlobalCache) {
            System.getProperty("user.home") + "/.corda-agent/cache"
        } else {
            ".testing/$suffix/cache"
        }
    }

    override fun dataDirectory(): String {
        return ".testing/$suffix/data"
    }

    override fun tempDirectory(): String {
        return ".testing/$suffix/tmp"
    }

    override fun serviceHomeDirectory(product: String, service: String, instance: String?): String {
        val location = if (instance == null || instance.isEmpty()) {
            ".testing/$suffix/$product/$service"
        } else {
            ".testing/$suffix/$product/$service-$instance"
        }
        File(location).mkdirs()
        return location
    }

    fun suffix(): String {
        return suffix
    }

    fun homeDirectory(): String {
        return File(".testing/$suffix").absolutePath
    }
}


// for use locally
class LocalLocations(private val root: String = System.getProperty("user.home") + "/.corda-agent") : Locations {

    init {
        File(cacheDirectory()).mkdirs()
        File(dataDirectory()).mkdirs()
        File(dataDirectory()).mkdirs()
    }

    override fun cacheDirectory(): String {
        return "$root/downloads"
    }

    override fun dataDirectory(): String {
        return "$root/data"
    }

    override fun tempDirectory(): String {
        return "$root/tmp"
    }

    override fun serviceHomeDirectory(product: String, service: String, instance: String?): String {
        val location = if (instance == null || instance.isEmpty()) {
            "$root/$product/$service"
        } else {
            "$root/$product/$service-$instance"
        }
        File(location).mkdirs()
        return location
    }

    fun homeDirectory(): String {
        return root
    }
}

// on a (unix) server
class UnixServerLocations(private val root: String = "/opt/corda") : Locations {

    init {
        File(cacheDirectory()).mkdirs()
        File(dataDirectory()).mkdirs()
        File(dataDirectory()).mkdirs()
    }

    override fun cacheDirectory(): String {
        return "$root/cache"
    }

    override fun dataDirectory(): String {
        return "$root/data"
    }

    override fun tempDirectory(): String {
        return "$root/tmp"
    }

    override fun serviceHomeDirectory(product: String, service: String, instance: String?): String {
        val location = if (instance == null || instance.isEmpty()) {
            "$root/$product/$service"
        } else {
            "$root/$product/$service-$instance"
        }
        File(location).mkdirs()
        return location
    }
}
