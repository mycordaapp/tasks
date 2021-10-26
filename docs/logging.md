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

## The LoggingContext

The logging context simply defines as a number of standard interfaces for creating `LoggingContextProducer`
consuming `LoggingContextConsumer` and querying `LoggingContextQuery`. The goals are quite simple:

* provide a standard framework for getting logs and optionally console output back the clients (helps solve problem 1)
* provide a basic level of standardisation on logging. This helps solve
    - "hand off" of log / error message between service (problem 2)
    - manageable log aggregation (problem 2 and 3)

At the back of my mind is also the idea of writing a lightweight log aggregator using Kafka and a lightweight key/value.
Producers all write to a Kafka topic. A consumer reads from the topic and writes to the key/value store. The queries run
against the key/value store. If this is worth writing as opposed to something off the shelf is TBD in my mind 










