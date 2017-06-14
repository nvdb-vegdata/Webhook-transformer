package no.vegvesen.vt.nvdb.tools.webhookproxy

import org.apache.http.HttpStatus
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.ContentType
import org.apache.http.entity.StringEntity
import org.apache.http.impl.client.HttpClients

fun main(args: Array<String>) {
    val post = HttpPost("https://mattermost.kantega.no/hooks/9humkpkwz3gh8fism451ajtzua")
    val (s, mattermost) = loadPayloads("apiv2SolrInvalidNumber.json")
    val payload = "payload=${mattermost}"
    post.entity = StringEntity(payload, ContentType.APPLICATION_FORM_URLENCODED)
    HttpClients.createDefault().use {
        it.execute(post).use {
            val responseBody = isToString(it.entity.content)
            if (it.statusLine.statusCode != HttpStatus.SC_OK) {
                logger.error("Response status ${it.statusLine.statusCode}: ${responseBody}")
            }
        }
    }
}
