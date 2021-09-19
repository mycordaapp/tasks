package  mycorda.app.tasks.inbuilt

import mycorda.app.tasks.BlockingTask
import mycorda.app.tasks.NotRequired
import mycorda.app.tasks.executionContext.ExecutionContext
import mycorda.app.tasks.logging.LogMessage
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.URL
import java.util.*

/**
 * Finds the local private IP address using this most reliable method available, starting
 * with the known cloud APIs
 */

interface DeterminePublishedIpAddressTask : BlockingTask<NotRequired, String>

class DeterminePublishedIpAddressTaskImpl : DeterminePublishedIpAddressTask {
    private val taskId = UUID.randomUUID()
    override fun taskId(): UUID {
        return taskId
    }

    override fun exec(ctx: ExecutionContext, params: NotRequired): String {
        if (isForced()) {
            return forced()
        }

        // Am i on a PC or Mac
        if (isLaptop()) {
            return "localhost"
        }

        // AWS - as reported by meta data
        val aws = doRequest("http://169.254.169.254/latest/meta-data/public-ipv4")
        if (aws.success) {
            ctx.log(LogMessage.info("Found public ip address of ${aws.result} using AWS endpoint"))
            return aws.result
        }

        // Use checkip
        val checkIp = doRequest("http://checkip.amazonaws.com")
        if (checkIp.success) {
            ctx.log(LogMessage.info("Found public ip address of ${checkIp.result} using 'checkip.amazonaws.com'"))
            return checkIp.result
        }

        // Ask the JVM and assume tge local address is the public address
        val localhost = InetAddress.getLocalHost()
        val localIP = localhost.hostAddress.trim()
        ctx.log(LogMessage.info("Assuming local address of $localIP from JVM"))
        return localIP
    }


    // using plain old java code to minimise 3rd party deps
    private fun doRequest(targetURL: String): RequestResult {
        try {
            val url = URL(targetURL)
            val connection = url.openConnection() as HttpURLConnection
            connection.readTimeout = 1000
            connection.connectTimeout = 1000
            connection.requestMethod = "GET"
            connection.useCaches = false
            connection.doOutput = false

            val input = connection.inputStream
            val rd = BufferedReader(InputStreamReader(input))

            val response = StringBuilder() // or StringBuffer if Java version 5+
            rd.lines().forEach {
                if (response.isNotEmpty()) response.append("\n")
                response.append(it)
            }

            rd.close()
            return RequestResult(response.toString().trim(), true)
        } catch (ex: Exception) {
            return RequestResult("", false)
        }
    }

    private fun doCommand(cmd: List<String>): RequestResult {
        val pb = ProcessBuilder()
        pb.command(cmd)
        val process = pb.start()
        val sb = StringBuilder()

        val reader = BufferedReader(InputStreamReader(process.inputStream))

        reader.lines().forEach {
            if (sb.isNotEmpty()) sb.append("\n")
            sb.append(it)
        }

        val exitCode = process.waitFor()
        return RequestResult(sb.toString().trim(), exitCode == 0)
    }

    private fun isLaptop(): Boolean {

        val osName = System.getProperties().getProperty("os.name").toLowerCase()
        if (osName.contains("mac")) return true
        if (osName.contains("win")) return true

        // todo - add tests for desktop builds of linux

        return false
    }

    private fun isForced(): Boolean {
        return System.getenv().containsKey("MYCORDAAPP_FORCE_PUBLISHED_IP")
    }

    private fun forced(): String {
        return System.getenv("MYCORDAAPP_FORCE_PUBLISHED_IP")
    }


    data class RequestResult(val result: String, val success: Boolean)
}


fun main(args: Array<String>) {
    //System.getenv().put("CORDA_FORCE_PUBLISHED_IP","123.123.123.123")
    val result = DeterminePublishedIpAddressTaskImpl().exec(params = NotRequired.instance())
    println(result)
}

