import mycorda.app.tasks.Task
import java.util.*

class Wibble {
    fun foo() {
        println ("foo")
    }
}

class MyTask : Task {
    override fun taskID(): UUID {
        TODO("Not yet implemented")
    }
}