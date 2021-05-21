package net.corda.ccl.commons.tasks

class WaitFor<T> () {

    fun wait ( func : () -> T ) :T  {

        return func.invoke()
    }

}