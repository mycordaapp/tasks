package mycorda.app.tasks

import SecurityPrinciple
import mycorda.app.tasks.logging.LogLevel
import mycorda.app.tasks.logging.LogMessage
import kotlin.reflect.KClass


fun List<LogMessage>.hasMessage(level: LogLevel, body: String): Boolean {
    return this.any { (it.level == level) && it.body == body }
}

fun List<LogMessage>.doesNotHaveMessage(level: LogLevel, body: String): Boolean {
    return this.none { (it.level == level) && it.body == body }
}

fun Set<SecurityPrinciple>.hasType(type : KClass<SecurityPrinciple>) : Boolean = true