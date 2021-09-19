package mycorda.app.tasks

import SecurityPrinciple
import mycorda.app.tasks.logging.LogLevel
import mycorda.app.tasks.logging.LogMessage
import kotlin.reflect.KClass


fun List<LogMessage>.hasMessage(level: LogLevel, body: String): Boolean {
    return this.filter { (it.level == level) && it.body == body }.size == 1
}

fun Set<SecurityPrinciple>.hasType(type : KClass<SecurityPrinciple>) : Boolean = true