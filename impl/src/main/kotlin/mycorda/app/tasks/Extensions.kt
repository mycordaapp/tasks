package mycorda.app.tasks

import mycorda.app.tasks.logging.LogLevel
import mycorda.app.tasks.logging.LogMessage

fun List<LogMessage>.hasMessage(level: LogLevel, body: String): Boolean {
    return this.any { (it.level == level) && it.body == body }
}

fun List<LogMessage>.doesNotHaveMessage(level: LogLevel, body: String): Boolean {
    return this.none { (it.level == level) && it.body == body }
}

