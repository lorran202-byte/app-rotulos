package com.lorran.lupanutri.data

import com.lorran.lupanutri.data.model.Product
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/** Cliente da API pública do Open Food Facts (sem chave). */
class OffApi {

    /** Retorna null se o produto não existe na base. Lança IOException sem internet. */
    suspend fun product(code: String): Product? = withContext(Dispatchers.IO) {
        val url = "https://world.openfoodfacts.org/api/v2/product/${enc(code)}.json?fields=${OffCodec.FIELDS}&lc=pt"
        val (status, body) = get(url)
        if (status == 404) return@withContext null
        if (status !in 200..299) throw IOException("HTTP $status")
        val root = JSONObject(body)
        if (root.optInt("status", 0) != 1) return@withContext null
        val p = root.optJSONObject("product") ?: return@withContext null
        OffCodec.parse(p, fallbackCode = code)
    }

    /** Busca por nome ou marca entre produtos vendidos no Brasil. */
    suspend fun search(query: String): List<Product> = withContext(Dispatchers.IO) {
        val url = "https://br.openfoodfacts.org/cgi/search.pl?search_terms=${enc(query)}" +
            "&search_simple=1&action=process&json=1&page_size=30&fields=${OffCodec.FIELDS}"
        parseList(url)
    }

    /** Produtos da mesma categoria vendidos no Brasil (para sugerir alternativas). */
    suspend fun byCategory(categoryTag: String): List<Product> = withContext(Dispatchers.IO) {
        val url = "https://world.openfoodfacts.org/api/v2/search?categories_tags=${enc(categoryTag)}" +
            "&countries_tags=en:brazil&page_size=60&sort_by=popularity_key&fields=${OffCodec.FIELDS}"
        parseList(url)
    }

    private fun parseList(url: String): List<Product> {
        val (status, body) = get(url)
        if (status !in 200..299) throw IOException("HTTP $status")
        val arr = JSONObject(body).optJSONArray("products") ?: return emptyList()
        return (0 until arr.length()).mapNotNull { i ->
            arr.optJSONObject(i)?.let { runCatching { OffCodec.parse(it) }.getOrNull() }
        }.filter { it.code.isNotBlank() }
    }

    private fun get(url: String): Pair<Int, String> {
        val conn = URL(url).openConnection() as HttpURLConnection
        try {
            conn.connectTimeout = 12_000
            conn.readTimeout = 20_000
            conn.setRequestProperty("User-Agent", "LupaNutri/1.0 (Android)")
            conn.setRequestProperty("Accept", "application/json")
            val status = conn.responseCode
            val stream = if (status in 200..299) conn.inputStream else conn.errorStream
            val body = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            return status to body
        } finally {
            conn.disconnect()
        }
    }

    private fun enc(s: String) = URLEncoder.encode(s, "UTF-8")
}
