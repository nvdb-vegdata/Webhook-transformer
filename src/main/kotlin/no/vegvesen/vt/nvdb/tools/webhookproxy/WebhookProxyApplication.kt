package no.vegvesen.vt.nvdb.tools.webhookproxy

import com.google.gson.JsonParser
import org.apache.http.HttpStatus
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.StringEntity
import org.apache.http.impl.client.HttpClients
import org.jetbrains.ktor.application.Application
import org.jetbrains.ktor.application.call
import org.jetbrains.ktor.application.install
import org.jetbrains.ktor.application.log
import org.jetbrains.ktor.features.DefaultHeaders
import org.jetbrains.ktor.host.embeddedServer
import org.jetbrains.ktor.logging.CallLogging
import org.jetbrains.ktor.netty.Netty
import org.jetbrains.ktor.routing.Routing
import org.jetbrains.ktor.routing.post

fun Application.module() {

    install(CallLogging)
    install(DefaultHeaders)
    install(Routing) {
        post("/splunk") {
            val splunkMessage = call.request.receive(String::class)
            log.info("1 Recieved $splunkMessage")
            val httpClient = HttpClients.createDefault()

            val post = HttpPost("https://mattermost.kantega.no/hooks/y57uqiwe3bdfxfb13nehrntjcc")
            val content = transformSplunkMessage(splunkMessage)
            val payload = "payload={\"text\": \"${content}\"}"
            post.entity = StringEntity(payload, org.apache.http.entity.ContentType.APPLICATION_FORM_URLENCODED)
            httpClient.use {
                it.execute(post).use {
                    val responseBody = it.entity.content.bufferedReader().use { it.readText() }
                    if(it.statusLine.statusCode != HttpStatus.SC_OK) {
                        log.error("Response status ${it.statusLine.statusCode}: ${responseBody}")
                    }
                }
            }
            call.respond("OK")
        }
    }
}

fun transformSplunkMessage(splunkMessage: String): String {
    val parser = JsonParser()
    val element = parser.parse(splunkMessage)
    if(element.isJsonObject){
        val obj = element.asJsonObject
        val result = obj.getAsJsonObject("result")
        val host = result.get("host").asString
        val origin = result.get("ORIGIN").asString
        val message = result.get("MESSAGE").asString
        val source = result.get("source").asString
        return "## Error on ${host}!\n" +
                "${origin}: ${message}\n" +
                "Source: ${source}"
    }
    return "Kunne ikke tolke ${splunkMessage}"
}

fun main(args: Array<String>) {
    embeddedServer(Netty, 8080, reloadPackages = listOf("webhookproxy"), module = Application::module).start()
}
