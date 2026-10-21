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

    /**
     * Envia os dados da embalagem para o Open Food Facts com a conta do usuário.
     * Retorna null se deu certo, ou a mensagem de erro.
     */
    suspend fun upload(p: Product, user: String, password: String): String? = withContext(Dispatchers.IO) {
        val f = linkedMapOf(
            "code" to p.code, "user_id" to user, "password" to password, "lc" to "pt",
            "product_name_pt" to p.name,
        )
        p.brand?.let { f["brands"] = it }
        p.quantity?.let { f["quantity"] = it }
        p.ingredientsText?.let { f["ingredients_text_pt"] = it }
        val n = p.nutrients
        f["nutrition_data_per"] = "100g"
        fun put(key: String, v: Double?, unit: String) {
            if (v != null) { f["nutriment_$key"] = v.toString(); f["nutriment_${key}_unit"] = unit }
        }
        put("energy-kcal", n.kcal, "kcal")
        put("carbohydrates", n.carbs, "g")
        put("sugars", n.sugars, "g")
        put("added-sugars", n.addedSugars, "g")
        put("proteins", n.protein, "g")
        put("fat", n.fat, "g")
        put("saturated-fat", n.satFat, "g")
        put("trans-fat", n.transFat, "g")
        put("fiber", n.fiber, "g")
        put("sodium", n.sodiumMg, "mg")
        val body = f.entries.joinToString("&") { "${enc(it.key)}=${enc(it.value)}" }
        try {
            val conn = URL("https://world.openfoodfacts.org/cgi/product_jqm2.pl").openConnection() as HttpURLConnection
            try {
                conn.requestMethod = "POST"
                conn.doOutput = true
                conn.connectTimeout = 15_000
                conn.readTimeout = 30_000
                conn.setRequestProperty("User-Agent", "LupaNutri/1.3 (Android)")
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
                val status = conn.responseCode
                val text = (if (status in 200..299) conn.inputStream else conn.errorStream)
                    ?.bufferedReader()?.use { it.readText() }.orEmpty()
                if (status == 401 || status == 403) return@withContext "usuário ou senha do Open Food Facts incorretos"
                if (status !in 200..299) return@withContext "o servidor respondeu com erro ($status)"
                val json = runCatching { JSONObject(text) }.getOrNull()
                if (json != null && json.optInt("status", 0) == 1) null
                else json?.optString("status_verbose")?.takeIf { it.isNotBlank() } ?: "resposta inesperada do servidor"
            } finally {
                conn.disconnect()
            }
        } catch (e: IOException) {
            "sem conexão"
        }
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
