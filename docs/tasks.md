
The first principle is that the basic building block is a `Task`. Tasks come in a few flavours, as an example
the interface for a blocking task is below:

```kotlin
interface Task {
    //a unique ID created for each run of the Task
    fun taskID(): UUID
}

interface BlockingTask<in I, out O> : Task {
    fun exec(executionContext: ExecutionContext = DefaultExecutionContext(), params: I): O
}
```

`ExecutionContext` is explained in more detail later. All you need to know here is that it
injects information that is only know at execution time, such as a connection to a console,
and also provides access to key common services.

Tasks really have two key characteristics:
* do one thing well
* provide stable and unambiguous inputs and outputs to their clients

A simple example is

```kotlin
class CalcSquareTask : BlockingTask<Int, Int> {
    private val id = UUID.randomUUID()
    override fun taskID(): UUID {
        return id
    }

    override fun exec(executionContext: ExecutionContext, num: Int): Int {
        val ctx = executionContext.withTaskId(taskID())
        ctx.log(logLevel = LogLevel.INFO, msg = "Calculating square of $num")
        return num.times(num)
    }
}
```


### #2 - Combining Tasks

The second principle is that is easy to call and combine tasks, even if the implementation is
running on another server. To support this there is prebuilt support for common problems, including:
* creation of fully wired ExecutionContext
* serialisation of input and outputs
* 'executors' to encapsulate  common patterns like logging, exceptions handling and retries
* building and securing servers running tasks

### #3 - Provisioners and Templates

The third principle is of Provisioners and Templates. Typically users will want to
perform higher actions, for example deploy a Corda Node to AWS with their apps pre-installed,
without having to understand all the Tasks needed to accomplish that goal. Provisioners
simply take a predefined template (currently YAML or JSON) and use these rules to drive calling
the correct Tasks with required params. Written like this, it sounds simple. But in practice
there must be a fairly consistent set of rules that are followed when implementing a provisoner.

Provisioners can be constructed from prebuilt stages that define  
common steps. As an example, think of deploying a corda node that connects to Testnet to either
Azure or AWS. Obviously the steps that provision the VM on the cloud are different, but once past
this stage the steps to configure the Corda node (setting X500 name, overriding default settings,
installing corDapps, joining Testnet, setting up unix systemctl to run Corda, staring Corda, ... )
are the same.

### #4 - Testing is built in

The final principle is testability. Mock/Fake implementation of Tasks must be provided for use in the
test suites.

*todo - expand this principle*

## Implementation

### Creating new Tasks

See [Create Instance](../aws-tasks/src/main/kotlin/net/corda/ccl/aws/task/CreateInstanceTask.kt) for
an example.

All tasks share a common [Execution Context](../commons/src/main/kotlin/net/corda/ccl/commons/executionContext/Context.kt).

Wiring everything up for an execution context can be simplified with the  `ContextManager` class - here is
an [example](../corda-tasks/src/main/kotlin/net/corda/ccl/tasks/cenm/identitymanager/ReceivePKITask.kt)


The input and output are always a single value. This may be a simple scalar, but in more
complicated cases a class will be required. Sometimes there is already
a suitable domain class to use, but if not the standard pattern is to
create additional Kotlin data classes with the suffix `Params` and `Result`
in the same source file as the Task. For example:

```kotlin
data class ComplexCalculationParams(val p1: Int, val p2: Int, val p3: Int)
class ComplexCalculationTask :  BlockingTask<ComplexCalculationParams, Int> {
    // implementation goes here 
}
```

Note that as custom serializers are not supported, be careful when reusing existing domain model - they may
rely upon external serializers. 
 