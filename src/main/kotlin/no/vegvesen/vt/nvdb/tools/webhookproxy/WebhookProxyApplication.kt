package no.vegvesen.vt.nvdb.tools.webhookproxy

import com.google.gson.JsonObject
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

private suspend fun sendMessage(webhookUrl: String, content: JsonObject, pipelineContext: PipelineContext<ApplicationCall>) {
    val post = HttpPost(webhookUrl)
    val payload = "payload={\"attachments\": [${content}], \"username\": \"PROD ERROR\"}"
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

fun transformSplunkMessage(splunkMessage: String): JsonObject {
    val o = JsonObject()
    o.addProperty("color", "danger")

    try {
        val parser = JsonParser()
        val element = parser.parse(splunkMessage)
        if(element.isJsonObject){
            val obj = element.asJsonObject
            val result = obj.getAsJsonObject("result")
            val host = result.get("host").asString
            val origin = if(result.has("ORIGIN")) { result.get("ORIGIN").asString} else {""}
            val message = if(result.has("MESSAGE")) result.get("MESSAGE").asString.replace("\"", "\\\"").replace(";", "") else result.get("_raw").asString
            val source = result.get("source").asString

            val summary = "## Error on ${host}!\n" +
                    "${origin}: ${message}\n" +
                    "Source: ${source}"
            o.addProperty("fallback", summary.substring(0, Math.min(2999, summary.length)))
            o.addProperty("title", "Error on ${host}!")
            val text = "${origin}: ${message}"
            o.addProperty("text", text.substring(0, Math.min(2999, text.length)))
        }
    } catch(e: Exception) {
        logger.error("Kunne ikke tolke melding ${splunkMessage}", e)
        o.addProperty("title", "Error!")
        o.addProperty("text", "Kunne ikke tolke ${splunkMessage}")
    }
    return o
}

fun transformElastalertMessage(elastalertMessage: String): JsonObject {
    val o = JsonObject()
    o.addProperty("color", "danger")

    try {
        val parser = JsonParser()
        val element = parser.parse(elastalertMessage)
        if(element.isJsonObject){
            val obj = element.asJsonObject
            val matches = obj.getAsJsonArray("matches")
            val result = matches.get(0).asJsonObject
            val host = result.get("HOSTNAME").asString
            val origin = result.get("logger_name").asString
            val message = result.get("message").asString.replace("\"", "\\\"").replace(";", "")

            val text = "${origin}: ${message}"
            val summary = "## Error on ${host}!\n$text"
            o.addProperty("fallback", summary.substring(0, Math.min(2999, summary.length)))
            o.addProperty("title", "Error on ${host}!")
            o.addProperty("text", text.substring(0, Math.min(2999, text.length)))
        }
    } catch(e: Exception) {
        logger.error("Kunne ikke tolke melding ${elastalertMessage}", e)
        o.addProperty("title", "Error!")
        o.addProperty("text", "Kunne ikke tolke ${elastalertMessage}")
    }
    return o
}

fun main(args: Array<String>) {
    embeddedServer(Netty, 8080, module = Application::module).start()
}
