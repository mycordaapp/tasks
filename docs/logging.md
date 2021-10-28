# Logging

## The Problem

In most cases a Task will be executed by a [Client](../impl/src/main/kotlin/mycorda/app/tasks/client/TaskClient.kt)
running in another process, possibly even in another data centre. In this model the problem of getting logs and console
output available to the operators of the client is not trivial. Let's walk through the common scenarios

### 1. Logs and console output stay on the server

This is what most applications do by default. Ideally they capture all errors and report them back as sensible messages.
But in practice this is almost impossible to do reliably. Weakness are:

* not every error can be anticipated
* useful diagnostics like log messages stay on the server
* problems are only reported once the process has completed. There is no "status" reporting back to the client while the
  server process is running. This is generally acceptable for quick process run via a blocking client but much less so
  for lengthy processes run via a non-blocking client (which is common in the deployment domain)

The result is that consumers of the service will almost certainly require access to the server log for fault diagnosis.
This is at best slightly annoying (say having to run a docker log command) to incredibly tedious (requesting access to a
customer's logs)

### 2. Service Aggregation

Many modern application have some style of `micro service` architecture. In this pattern an originating request will
often pass between services .... (need some more words)

### 3. Logs and console out are passed to a log aggregator

This is a fairly common pattern. Logs and also often captured output are passed onto a central log aggregator where they
can queried and examined. It helps solves the problems above, but still has weaknesses:

* log aggregators are typically quite expensive to run locally, and hence they are less useful in day to day dev.
* defining queries to locate specific log entries can sometimes be awkward
* moving logs to another service is sometimes considered a security risk

### 4. Logs are process by a distributed logging framework

Simplistically, this is log aggregation on steroids. Logs include specific fields that allow the framework to easily
reassemble views on specific activities, for instance the "end to end" view as a request is passed through a number of
services for processing. Downsides include

* agreeing and implementing a suitable framework
* log aggregators are typically quite expensive to run locally, and hence they are less useful in day to day dev.
* moving logs to another service is sometimes considered a security risk

### 5. App Altering concerns

Ideally all the key lifecycle events are driven via a clear API and managed by a deployment / management layer. I say
ideally, as this usually a lot of effort. It also has the downside of a being in the fuzzy list of non-functional
requirements which are typically pushed towards the end of development even though they can be useful very useful during
dev and test.

_hmm - i need to expand this still_

### 6. 12 Factor App concerns

Concern 11 of the [Twelve-Factor App](https://12factor.net/) says:

_"Treat logs as event streams"_

In practice this mostly means a single event stream available via something like the `docker logs` command. The
implication here is that at some point in an architecture, it is quite likely that all output (console, logs and
structured monitoring if available) are collapsed into a single stream

## The `Logging Context`

The logging context is simply a number of standard interfaces for creating `LoggingContextProducer`
consuming `LoggingContextConsumer` and querying `LoggingContextQuery`. The goals are quite simple:

* provide a standard framework for getting logs and optionally console output back the clients (helps solve problem 1)
* provide a basic level of standardisation on logging in terms of both APIs and formats. This helps solve
    - "hand off" of log / error message between service (problem 2)
    - manageable log aggregation (problem 2 and 3)
    - mixed up data in the log stream (problems 5 and 6)

At the back of my mind is also the idea of writing a lightweight log aggregator using Kafka and a lightweight key/value.
Producers all write to a Kafka topic. A consumer reads from the topic and writes to the key/value store. The queries run
against the key/value store. If this is worth writing as opposed to something off the shelf is TBD in my mind

## Design of the 'Logging Context'

The current implementation is quite minimal and is all
within [one file](../impl/src/main/kotlin/mycorda/app/tasks/logging/LoggingContext.kt).

### LogMessage

This defines a common logging entry. The current definition is missing a number of key fields. The structure should
match well with that expected by distributed logging tools

### LoggingProducerContext

This is passed to application code to generate (produce) output. The base interface is below(the full one includes a
number of default helper methods). It is very simple, and deliberately behaves like normal Kotlin/Java code with a
PrintStream

```kotlin
interface LoggingProducerContext {
    /**
     * Abstract generating a log message
     */
    fun logger(): LogMessageConsumer

    /**
     * Abstract writing to the console
     */
    fun stdout(): PrintStream

    /**
     * Abstract writing to the error stream
     */
    fun stderr(): PrintStream
}
```

### LoggingConsumerContext

This sits the other side of a producer. The design is quite obvious. For simplicity all console output is just a string
at this API level.

```kotlin
interface LoggingConsumerContext {
    fun acceptLog(msg: LogMessage)
    fun acceptStdout(output: String)
    fun acceptStderr(error: String)
}
```

### LoggingReaderContext

This is the absolute bare-bones reader side. It is expected that some simple query patterns will be also be added

```kotlin
interface LoggingReaderContext {
    fun stdout(): String
    fun stderr(): String
    fun messages(): List<LogMessage>
}
```

### Simple Logging Scenarios

In many simple uses case esp local development and unit test all services are running locally. For these cases the
current design provides:

#### ConsoleLoggingProducerContext

Everything straight to the console

#### InMemoryLogging

Captures all output to memory, readable via LoggingReaderContext. See below

```kotlin
val inMemoryLogging = InMemoryLogging()
val logProducer = LoggingProducerToConsumer(inMemoryLogging)

// write something
logProducer.stdout().prinln("Hello World")

// read it back
println(inMemoryLogging.stdout())
```










