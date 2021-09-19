package mycorda.app.tasks

import mycorda.app.helpers.random
import java.util.*


/**
 * A generic encapsulation of a unique identifier that
 * can be generated in a range of formats like UUIDs or hashing
 * functions.
 *
 * Max length is 64, which allows for easy encoding of 256 bit hashes
 */
open class UniqueId(val id: String = UUID.randomUUID().toString()) {

    init {
        // set some basic rules length rules
        assert(id.length >= 6)
        assert(id.length <= 256 / 4)
    }

    companion object {
        /**
         * From a random UUID
         */
        fun randomUUID(): UniqueId {
            return UniqueId(UUID.randomUUID().toString())
        }

        /**
         * From a provided String. Min length is 6, max is 64
         */
        fun fromString(id: String): UniqueId {
            return UniqueId(id)
        }

        /**
         * From a provided UUID
         */
        fun fromUUID(id: UUID): UniqueId {
            return UniqueId(id.toString())
        }

        /**
         * Build a random string in a 'booking reference' style,
         * e.g. `BZ13FG`
         */
        fun random(length: Int = 6): UniqueId {
            return UniqueId(String.random(length))
        }
    }
}