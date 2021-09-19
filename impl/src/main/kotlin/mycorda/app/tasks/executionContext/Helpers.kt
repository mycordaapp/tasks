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
