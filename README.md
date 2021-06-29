# The "Tasks" framework

## What it does

The "Tasks" framework simply provides a common way of encapsulating calls into the plethora of tools and APIs that we
typically need to build out the backends needed to deploy and manage applications. The original use case is
encapsulating the many dev ops tools needed to deploy and manage the Corda DLT and its related components.

This framework is most certainly **NOT** intended as replacement for existing devops tooling. Each task should ideally
be a simple wrapper over the underlying toolset. The key insight is that all tasks expose a similar API and can be
thought of a Lego style building block.

There are also higher level services that build on these tasks, for example the task [Remoting](http://todo.com)
framework lets clients call tasks on remote servers using industry accepted standards for data transfer and security.

## Dependencies

As with everything in [myCorda dot App](https://mycorda.app), this library has minimal dependencies.

* Kotlin 1.2
* Java 1.8
* The object [Registry](https://github.com/mycordaapp/registry#readme)

## Next Steps 

