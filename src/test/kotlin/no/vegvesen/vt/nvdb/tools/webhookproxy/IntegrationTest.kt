package no.vegvesen.vt.nvdb.tools.webhookproxy

import org.apache.http.HttpStatus
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.ContentType
import org.apache.http.entity.StringEntity
import org.apache.http.impl.client.HttpClients
import org.hamcrest.CoreMatchers
import org.jetbrains.ktor.application.Application
import org.jetbrains.ktor.application.call
import org.jetbrains.ktor.application.install
import org.jetbrains.ktor.host.ApplicationHost
import org.jetbrains.ktor.host.embeddedServer
import org.jetbrains.ktor.locations.Locations
import org.jetbrains.ktor.locations.location
import org.jetbrains.ktor.locations.post
import org.jetbrains.ktor.netty.Netty
import org.jetbrains.ktor.routing.routing
import org.junit.AfterClass
import org.junit.Assert.assertThat
import org.junit.BeforeClass
import org.junit.Test
import java.util.concurrent.TimeUnit

class IntegrationTest {

    companion object {
        var webhookServer: ApplicationHost? = null
        var webhookProxyServer: ApplicationHost? = null
        val messages = HashMap<String, String>().toMutableMap()

        @BeforeClass @JvmStatic fun setup() {
            System.setProperty("SPLUNK_WEBHOOK_URL", "http://localhost:8999/webhook/default")
            System.setProperty("ELASTALERT_WEBHOOK_URL", "http://localhost:8999/webhook/default")
            System.setProperty("APIV2_WEBHOOK_URL", "http://localhost:8999/webhook/apiv2")
            System.setProperty("VEGKART_WEBHOOK_URL", "http://localhost:8999/webhook/vegkart")
            System.setProperty("NVDBIND_WEBHOOK_URL", "http://localhost:8999/webhook/nvdbind")
            System.setProperty("APISKRIV_WEBHOOK_URL", "http://localhost:8999/webhook/apiskriv")
            System.setProperty("DATAFANGST_WEBHOOK_URL", "http://localhost:8999/webhook/datafangst")
            System.setProperty("K2LES_WEBHOOK_URL", "http://localhost:8999/webhook/k2les")
            System.setProperty("K2LES_WEBHOOK_CHANNEL", "k2les")

            @location("/webhook/{id}")
            data class Webhook(val id: String)

            webhookServer = embeddedServer(Netty, 8999) {
                install(Locations)
                routing {
                    post<Webhook> {
                        messages.put(it.id, call.request.receive(String::class))
                        this.call.respond("OK")
                    }
                }
            }.start()
            webhookProxyServer = embeddedServer(Netty, 18080, module = Application::module).start()
        }

        @AfterClass @JvmStatic fun tearDown() {
            webhookServer!!.stop(1000, 1000, TimeUnit.MILLISECONDS)
            webhookProxyServer!!.stop(1000, 1000, TimeUnit.MILLISECONDS)
        }
    }

    @Test fun splunkAPIV2Bullshit(){
        val url = "http://localhost:18080/splunk/apiv2"
        val (incomming, expected) = loadPayloads("apiv2bullshit.json")
        val message = postAndGetWebhookPayload(url, incomming, "apiv2")

        assertThat(message, CoreMatchers.`is`(expected))
    }

    @Test fun datafangstInvaliduuid(){
        val url = "http://localhost:18080/elastalert/datafangst"
        val (incomming, expected) = loadPayloads("datafangstInvaliduuid.json")

        val message = postAndGetWebhookPayload(url, incomming, "datafangst")

        assertThat(message, CoreMatchers.`is`(expected))
    }

    @Test fun solrInvalidNumber() {
        val url = "http://localhost:18080/splunk/apiv2"
        val (incomming, expected) = loadPayloads("apiv2SolrInvalidNumber.json")

        val message = postAndGetWebhookPayload(url, incomming, "apiv2")

        assertThat(message, CoreMatchers.`is`(expected))
    }

    @Test fun apiSkriv() {
        val url = "http://localhost:18080/splunk/apiskriv"
        val (incomming, expected) = loadPayloads("apiskriv.json")

        val message = postAndGetWebhookPayload(url, incomming, "apiskriv")

        assertThat(message, CoreMatchers.`is`(expected))
    }

    @Test fun k2channel() {
        val url = "http://localhost:18080/elastalert/k2les"
        val (incomming, expected) = loadPayloads("k2les.json")

        val message = postAndGetWebhookPayload(url, incomming, "k2les")

        assertThat(message, CoreMatchers.`is`(expected))
    }

    private fun postAndGetWebhookPayload(url: String, payload: String, app: String): String? {
        val post = HttpPost(url)
        post.entity = StringEntity(payload, ContentType.APPLICATION_FORM_URLENCODED)
        HttpClients.createDefault().use {
            it.execute(post).use {
                val responseBody = isToString(it.entity.content)
                if (it.statusLine.statusCode != HttpStatus.SC_OK) {
                    logger.error("Response status ${it.statusLine.statusCode}: ${responseBody}")
                }
            }
        }
        val message = messages[app]
        return message
    }
}
