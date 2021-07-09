package mycorda.app.tasks.executionContext

import mycorda.app.helpers.random
import mycorda.app.registry.Registry
import mycorda.app.tasks.*
import mycorda.app.tasks.logging.*
import mycorda.app.tasks.processManager.ProcessManager
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream
import java.util.*
import java.io.IOException
import java.lang.StringBuilder


/**
 * Wraps common operations in a more builder like style, and will
 * auto populate the registry if necessary, so there
 * is less need to understand the internal DI processes.
 *
 * This is intended to support test cases. Runtime code is recommended
 * to populate the registry manually.
 *
 */
class TestContextManager() {
    private lateinit var msgSink: LogMessageSink
    private lateinit var processManager: ProcessManager
    private lateinit var executor: ExecutorFactory
    private lateinit var bos: ByteArrayOutputStream
    private lateinit var stdout: StdOut
    private lateinit var tag: String
    private lateinit var locations: Locations
    private var registered = false
    private lateinit var registry: Registry
    private lateinit var trackingOption: RegistryTrackingOption


    /**
     * Initialise with an internal registry. Simple and self contained for basic testing.
     */
    fun initialise(): TestContextManager {
        if (!registered) {
            msgSink = InMemoryLogMessageSink()
            processManager = ProcessManager()
            executor = FixedThreadPoolExecutor()
            bos = ByteArrayOutputStream()
            stdout = StdOut(PrintStream(bos))
            tag = String.random(length = 6)
            locations = TestLocations(tag)

            registry = Registry()
            registry.store(msgSink)
            registry.store(processManager)
            registry.store(executor)
            registry.store(stdout)
            registry.store(locations)
            registry.store(DefaultExecutionContextFactory(registry))
            registered = true
            return this
        } else {
            throw RuntimeException("Can only call initialise() once!")
        }
    }

    /**
     * Initialise with an external registry. In this case services in the external
     * registry are used if available UNLESS the forceOverride flag is set to true.
     */
    fun initialiseWithRegistry(registry: Registry, tracking: RegistryTrackingOption): TestContextManager {
        if (!registered) {
            trackingOption = tracking
            msgSink = setObject(registry, tracking, LogMessageSink::class.java) { InMemoryLogMessageSink() }
            processManager = setObject(registry, tracking, ProcessManager::class.java) { ProcessManager() }
            executor = setObject(registry, tracking, ExecutorFactory::class.java) { FixedThreadPoolExecutor() }
            bos = ByteArrayOutputStream()
            stdout = setObject(registry, tracking, StdOut::class.java) { StdOut(PrintStream(bos)) }
            tag = String.random(length = 6)
            locations = setObject(registry, tracking, Locations::class.java) { TestLocations(tag) }
            registry.store(DefaultExecutionContextFactory(registry))
            this.registry = registry
            registered = true
        }
        return this
    }

    fun initialiseWithRegistry(registry: Registry, forceOverride: Boolean = false): TestContextManager {
        val trackingOption =
            if (forceOverride) RegistryTrackingOption.alwaysUpdate else RegistryTrackingOption.addIfMissing
        return initialiseWithRegistry(registry, trackingOption)
    }


    private fun <T> setObject(
        registry: Registry,
        tracking: RegistryTrackingOption,
        clazz: Class<T>,
        intBlock: () -> T
    ): T {
        return when (tracking) {
            RegistryTrackingOption.addIfMissing -> {
                if (registry.contains(clazz)) {
                    return registry.get(clazz)
                } else {
                    val obj: T = intBlock()
                    registry.store(obj as Any)
                    obj
                }
            }
            RegistryTrackingOption.alwaysUpdate -> {
                val obj: T = intBlock()
                registry.store(obj as Any)
                obj
            }
            RegistryTrackingOption.dontUpdate -> {
                val obj: T = intBlock()
                obj
            }
        }
    }


    fun withConsoleMessageSink(): TestContextManager {
        msgSink = setObject(registry, trackingOption, LogMessageSink::class.java) { ConsoleLogMessageSink() }
        return this
    }

    fun withInMemoryLogMessageSink(): TestContextManager {
        msgSink = setObject(registry, trackingOption, LogMessageSink::class.java) { InMemoryLogMessageSink() }
        return this
    }

    fun withLogMessageSink(newMsgSink: LogMessageSink): TestContextManager {
        msgSink = setObject(registry, trackingOption, LogMessageSink::class.java) { newMsgSink }
        return this
    }

    fun withLocations(newLocations: Locations): TestContextManager {
        locations = setObject(registry, trackingOption, Locations::class.java) { newLocations }
        return this
    }


    /**
     * Create an ExecutionContext directly
     */
    fun createExecutionContext(): ExecutionContext {
        val raw = registry.get(ExecutionContextFactory::class.java).get()
        val provisioningState = raw.provisioningState().withTag(tag)
        return raw.withProvisioningState(provisioningState)
    }

    fun executionContextFactory(): ExecutionContextFactory {
        return registry.get(ExecutionContextFactory::class.java)
    }

    fun registry(): Registry {
        return registry
    }


    fun printCapturedLogMessages() {
        print(capturedLogMessages())
    }

    fun capturedLogMessages(): String {
        val sb = StringBuilder()
        if (msgSink is InMemoryLogMessageSink) {
            val formatter = DefaultStringLogFormatter()
            (msgSink as InMemoryLogMessageSink).messages()
                .forEach { sb.append(formatter.toString(it, LogFormat.Full)).append("\n") }
        }
        return sb.toString()
    }


    @Deprecated(message = "Use printCapturedLogMessages")
    fun printCapturedMessages() {
        return printCapturedLogMessages()
    }

    fun printCaptureStdOut() {
        print(captureStdOut())
    }

    fun captureStdOut(): String {
        return String(bos.toByteArray())
    }

    fun printCapturedProcessOutput(processId: UUID) {
        val output = processManager.lookupOutput(processId)
        if (output != null) {
            print(output.stdout.toString())
        }
    }

    @Deprecated(message = "Use printCapturedProcessOutput")
    fun printCapturedOutput(processId: UUID) {
        return printCapturedProcessOutput(processId)
    }


    fun processManager(): ProcessManager {
        return processManager
    }

    fun locations(): Locations {
        return this.locations
    }


    fun logMessageSink(): LogMessageSink {
        return msgSink
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
     * Force processes to close down
     */
    fun shutdownProcesses(): TestContextManager {
        processManager.killAll()
        val remaining = executor.executorService().shutdownNow()
        if (remaining.isNotEmpty()) {
            remaining.forEach {
                println("Warning, failed to shutdown ${it}")
            }
        }
        return this
    }

    fun cleanupData(): TestContextManager {
        if (locations() is TestLocations) {
            val dir = File(testLocations().homeDirectory())

            if (dir.exists()) {
                val deleteCmd = "rm -r $dir"
                val runtime = Runtime.getRuntime()
                try {
                    runtime.exec(deleteCmd)
                } catch (e: IOException) {
                }

            }
        }
        return this
    }

    /**
     * What to do the main registry
     */
    enum class RegistryTrackingOption {
        dontUpdate,     // full isolated - the injected repositry is never update
        addIfMissing,   // add the entry if it wasn't there, if not don't touch
        alwaysUpdate    // always update the main registry
    }

}