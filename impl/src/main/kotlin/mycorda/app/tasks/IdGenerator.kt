package mycorda.app.tasks

import mycorda.app.helpers.random


/**
 * Emulate the rules used by the backend when naming resources such as ids. This is
 * often useful in test Fakes
 */
interface IdGenerator {
    fun create(): String
}

class RandomIdGenerator : IdGenerator {
    override fun create(): String {
        return String.random(length = 6)
    }
}

class SequentialIdGenerator : IdGenerator {
    private var id = 1;
    override fun create(): String {
        return String.format("%04d", id++)
    }
}
