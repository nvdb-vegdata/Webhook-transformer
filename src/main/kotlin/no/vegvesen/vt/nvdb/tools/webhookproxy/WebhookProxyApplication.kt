package no.vegvesen.vt.nvdb.tools.webhookproxy

import com.google.gson.JsonParser
import org.apache.http.HttpStatus
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.ContentType
import org.apache.http.entity.StringEntity
import org.apache.http.impl.client.HttpClients
import org.jetbrains.ktor.application.*
import org.jetbrains.ktor.host.embeddedServer
import org.jetbrains.ktor.netty.Netty
import org.jetbrains.ktor.pipeline.PipelineContext
import org.jetbrains.ktor.routing.Routing
import org.jetbrains.ktor.routing.post
import org.slf4j.Logger
import org.slf4j.LoggerFactory

val logger: Logger = LoggerFactory.getLogger("WebhookProxy")

fun Application.module() {
    val splunkWebhookUrl: String = System.getenv("SPLUNK_WEBHOOK_URL")
            ?: throw IllegalArgumentException("Environment variable SPLUNK_WEBHOOK_URL is not defined")
    val elastalertWebhookUrl: String = System.getenv("ELASTALERT_WEBHOOK_URL")
            ?: throw IllegalArgumentException("Environment variable SPLUNK_WEBHOOK_URL is not defined")

    install(Routing) {
        post("/splunk") {
            val splunkMessage = call.request.receive(String::class)
            logger.info("Recieved ${splunkMessage}")
            val content = transformSplunkMessage(splunkMessage)

            sendMessage(splunkWebhookUrl, content, this)
        }
        post("/elastalert") {
            val elastalertMessage = call.request.receive(String::class)
            logger.info("Recieved ${elastalertMessage}")
            val content = transformElastalertMessage(elastalertMessage)

            sendMessage(elastalertWebhookUrl, content, this)
        }
    }

}

private suspend fun sendMessage(webhookUrl: String, content: String, pipelineContext: PipelineContext<ApplicationCall>) {
    val post = HttpPost(webhookUrl)
    val payload = "payload={\"text\": \"${content}\"}"
    post.entity = StringEntity(payload, ContentType.APPLICATION_FORM_URLENCODED)
    HttpClients.createDefault().use {
        it.execute(post).use {
            val responseBody = it.entity.content.bufferedReader().use { it.readText() }
            if (it.statusLine.statusCode != HttpStatus.SC_OK) {
                logger.error("Response status ${it.statusLine.statusCode}: ${responseBody}")
            }
        }
    }
    pipelineContext.call.respond("OK")
}

fun transformSplunkMessage(splunkMessage: String): String {
    try {
        val parser = JsonParser()
        val element = parser.parse(splunkMessage)
        if(element.isJsonObject){
            val obj = element.asJsonObject
            val result = obj.getAsJsonObject("result")
            val host = result.get("host").asString
            val origin = result.get("ORIGIN").asString
            val message = result.get("MESSAGE").asString.replace("\"", "\\\"")
            val source = result.get("source").asString
            return "## Error on ${host}!\n" +
                    "${origin}: ${message}\n" +
                    "Source: ${source}"
        }
    } catch(e: Exception) {
        logger.error("Kunne ikke tolke melding ${splunkMessage}", e)
    }
    return "Kunne ikke tolke ${splunkMessage}"
}

fun transformElastalertMessage(elastalertMessage: String): String {
    try {
        val parser = JsonParser()
        val element = parser.parse(elastalertMessage)
        if(element.isJsonObject){
            val obj = element.asJsonObject
            val matches = obj.getAsJsonArray("matches")
            val result = matches.get(0).asJsonObject
            val host = result.get("HOSTNAME").asString
            val origin = result.get("logger_name").asString
            val message = result.get("message").asString.replace("\"", "\\\"")
            return "## Error on ${host}!\n" +
                    "${origin}: ${message}"
        }
    } catch(e: Exception) {
        logger.error("Kunne ikke tolke melding ${elastalertMessage}", e)
    }
    return "Kunne ikke tolke ${elastalertMessage}"
}

fun main(args: Array<String>) {
    embeddedServer(Netty, 8080, module = Application::module).start()
}
