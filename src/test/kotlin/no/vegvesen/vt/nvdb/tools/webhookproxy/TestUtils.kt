package no.vegvesen.vt.nvdb.tools.webhookproxy

fun loadPayloads(file: String): Pair<String, String> {
    return Pair(isToString(IntegrationTest::class.java.getResourceAsStream("/data/${file}")).trim(),
                isToString(IntegrationTest::class.java.getResourceAsStream("/data/fasit/${file}")).trim())
}
