package mycorda.app.tasks

import mycorda.app.tasks.logging.LogLevel
import mycorda.app.tasks.logging.LogMessage


fun List<LogMessage>.hasMessage(level: LogLevel, body: String): Boolean {
    return this.filter { (it.level == level) && it.body == body }.size == 1
}