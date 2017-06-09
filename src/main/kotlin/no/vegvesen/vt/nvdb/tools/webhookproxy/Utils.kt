package no.vegvesen.vt.nvdb.tools.webhookproxy

import java.io.InputStream

fun isToString(stream: InputStream): String {
    return stream.bufferedReader().use { it.readText() }
}
