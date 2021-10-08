package mycorda.app.tasks.logging

import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream
import java.nio.charset.StandardCharsets
import java.nio.file.Paths

/**
 * Simply tie a PrintStream to some backing storage so we can read in back later.
 */
interface CapturedPrintStream {
    /**
     * The stream to output to
     */
    fun printStream(): PrintStream

    /**
     * Return the entire captured stream as a single string.
     */
    fun captured(): String

    // TODO - a "tail -f"  like streaming api
}


/**
 * The simple case, just capture to an in memory byte array
 */
open class InMemoryCapturedPrintStream : CapturedPrintStream {
    private val baos = ByteArrayOutputStream()
    private val utf8 = StandardCharsets.UTF_8.name()
    private val ps = PrintStream(baos, true, utf8)

    override fun printStream(): PrintStream {
        return ps
    }

    override fun captured(): String {
        synchronized(this) {
            return baos.toString(utf8)
        }
    }
}

/* Typesafe to use as an stdout stream */
class InMemoryStdoutStream : InMemoryCapturedPrintStream() , StdoutHolder {
    override fun out(): PrintStream {
        return printStream()
    }
}

/* Typesafe to use as an stdee stream */
class InMemoryStderrStream : InMemoryCapturedPrintStream() , StderrHolder {
    override fun err(): PrintStream {
        return printStream()
    }
}


/**
 * Use a normal file for storage
 */
class FileCapturedPrintStream(fileName: String) : CapturedPrintStream {
    private val storedStream: File
    private val ps: PrintStream

    init {
        Paths.get(fileName).parent.toFile().mkdir()
        storedStream = File(fileName)
        storedStream.createNewFile()
        ps = PrintStream(storedStream)
    }

    override fun printStream(): PrintStream {
        return ps
    }

    override fun captured(): String {
        return storedStream.readText(StandardCharsets.UTF_8)
    }
}
