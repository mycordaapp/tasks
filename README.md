# The "Tasks" framework

[![Circle CI](https://circleci.com/gh/mycordaapp/tasks.svg?style=shield)](https://circleci.com/gh/mycordaapp/tasks)
[![Licence Status](https://img.shields.io/github/license/mycordaapp/tasks)](https://github.com/mycordaapp/tasks/blob/master/licence.txt)

## What it does

The "Tasks" framework simply provides a common way of encapsulating calls into the plethora of tools and APIs that we
typically need to build out the backends to deploy and manage complex applications. The original use case is
encapsulating the many dev ops tools needed to deploy and manage the Corda DLT and its related components.

This framework is most certainly **NOT** intended as replacement for existing devops tooling. In these cases each task
should ideally be a simple wrapper over the underlying toolset. The key insight is that all tasks expose a similar API
and can be thought of as Lego style building block.

This library also implements a standard framework (the LoggingContext) to manage [logging](docs/logging.md)
. Its likely this will be broken out into its own library in the future.

There are also higher level services that build on these tasks, for example the
task [Http Remoting](https://github.com/mycordaapp/tasks-http#readme)
toolkit lets clients call tasks on remote servers using http(s) standards for data transfer and security.

The design and modularity is heavily influenced by [HTT4kk](https://www.http4k.org/guide/concepts/rationale/). In
particular

* Minimal Dependencies (see below)
* Very modular
* Avoid "magic": No annotations, very minimal use of reflections
* Fast startup/ shutdown

## Dependencies

As with everything in [myCorda dot App](https://mycorda.app), this library has minimal dependencies.

* Kotlin 1.4
* Java 11
* The object [Registry](https://github.com/mycordaapp/registry#readme)
* The [Commons](https://github.com/mycordaapp/commons#readme) module
* The [Really Simple Serialisation(rss)](https://github.com/mycordaapp/really-simple-serialisation#readme) module
    - [Jackson](https://github.com/FasterXML/jackson) for JSON serialisation

## Next Steps

* more on building and using Tasks is [here](./docs/tasks.md)
* details of the Logging Context are [here](./docs/logging.md))

