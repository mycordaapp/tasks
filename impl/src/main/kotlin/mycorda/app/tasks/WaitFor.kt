package mycorda.app.tasks

class WaitFor<T> () {

    fun wait ( func : () -> T ) :T  {
        return func.invoke()
    }

}