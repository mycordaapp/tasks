package mycorda.app.tasks.executionContext

import mycorda.app.helpers.random
import mycorda.app.registry.Registry
import mycorda.app.tasks.FixedThreadPoolExecutor
import mycorda.app.tasks.Locations
import mycorda.app.tasks.TestLocations
import mycorda.app.tasks.logging.*
import mycorda.app.tasks.processManager.ProcessManager
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.util.*


/*
 Some useful helpers to simplify code
 */


/**
 * Wraps common operations in a more builder like style, so there
 * is less need to understand the internal DI processes.
 *
 * This is intended to support test cases.
 */
@Deprecated(message = "use TestContextManager instead")
class ContextManager(val registry: Registry) {
    var msgSink: LogMessageSink = InMemoryLogMessageSink()
    var processManager = ProcessManager()
    var executor = FixedThreadPoolExecutor()
    val bos = ByteArrayOutputStream()
    var stdout = StdOut(PrintStream(bos))
    val tag = String.random(length = 6)
    private var locations: Locations = TestLocations(tag)
    private var registered = false

    fun register(): ContextManager {
        if (!registered) {
            registry.store(msgSink)
            registry.store(processManager)
            registry.store(executor)
            registry.store(stdout)
            registry.store(locations)
            registry.store(DefaultExecutionContextFactory(registry))
            registered = true
            return this
        } else {
            throw RuntimeException("Can only call register() once!")
        }
    }


    fun withConsoleMessageSink(): ContextManager {
        msgSink = ConsoleLogMessageSink()
        return this
    }

    fun withLocations(locations: Locations): ContextManager {
        this.locations = locations
        return this
    }

    /**
     * Create an ExecutionContext directly
     */
    fun createExecutionContext(): ExecutionContext {
        val raw = registry.get(ExecutionContextFactory::class.java)
            .get()

        val provisioningState = raw.provisioningState().withTag(tag)
        return DefaultExecutionContextModifier(raw).withProvisioningState(provisioningState)
    }


    fun printCapturedMessages() {
        if (msgSink is InMemoryLogMessageSink) {
            val formatter = DefaultStringLogFormatter()
            (msgSink as InMemoryLogMessageSink).messages().forEach { println(formatter.toString(it, LogFormat.Full)) }
        }
    }

    fun printCaptureStdOut() {
        print(String(bos.toByteArray()))
    }

    fun printCapturedOutput(processId: UUID) {
        val output = processManager.lookupOutput(processId)
        if (output != null) {
            print(output.stdout.toString())
        }
    }

    fun processManager(): ProcessManager {
        return processManager
    }

    fun locations(): Locations {
        return this.locations
    }

    fun testLocations(): TestLocations {
        if (locations is TestLocations) {
            return this.locations as TestLocations
        }
        throw RuntimeException("${locations::class.simpleName} is not an instance of TestLocations")
    }


    fun tag(): String {
        return tag
    }


    /**
     * Force everything to close
     */
    fun shutdownProcesses() {
        processManager.killAll()
        executor.executorService().shutdownNow()
    }

}


