package com.lorran.lupanutri.data

import com.lorran.lupanutri.data.model.Nutrients
import com.lorran.lupanutri.data.model.Product
import org.json.JSONArray
import org.json.JSONObject

/**
 * Converte entre o JSON da base aberta (Open Food Facts) e o modelo do app.
 * O mesmo formato é usado para guardar produtos no aparelho (inclusive os cadastrados à mão).
 */
object OffCodec {

    const val FIELDS = "code,product_name,product_name_pt,generic_name_pt,brands,image_front_url,image_url," +
        "quantity,ingredients_text_pt,ingredients_text,nutriments,additives_tags,allergens_tags,traces_tags," +
        "ingredients_analysis_tags,nova_group,categories_tags"

    fun parse(o: JSONObject, fallbackCode: String? = null): Product {
        val n = o.optJSONObject("nutriments") ?: JSONObject()
        val sodiumG = n.num("sodium_100g")
        val saltG = n.num("salt_100g")
        val sodiumMg = when {
            sodiumG != null -> sodiumG * 1000.0
            saltG != null -> saltG * 400.0
            else -> null
        }
        val nutrients = Nutrients(
            kcal = n.num("energy-kcal_100g") ?: n.num("energy_100g")?.let { it / 4.184 },
            carbs = n.num("carbohydrates_100g"),
            sugars = n.num("sugars_100g"),
            addedSugars = n.num("added-sugars_100g"),
            protein = n.num("proteins_100g"),
            fat = n.num("fat_100g"),
            satFat = n.num("saturated-fat_100g"),
            transFat = n.num("trans-fat_100g"),
            fiber = n.num("fiber_100g"),
            sodiumMg = sodiumMg,
        )
        val name = listOf("product_name_pt", "product_name", "generic_name_pt")
            .firstNotNullOfOrNull { o.str(it) } ?: "Produto sem nome"
        val nova = o.optInt("nova_group", 0).takeIf { it in 1..4 }
        return Product(
            code = o.str("code") ?: fallbackCode.orEmpty(),
            name = name,
            brand = o.str("brands")?.split(",")?.firstOrNull()?.trim()?.takeIf { it.isNotEmpty() },
            imageUrl = o.str("image_front_url") ?: o.str("image_url"),
            quantity = o.str("quantity"),
            ingredientsText = o.str("ingredients_text_pt") ?: o.str("ingredients_text"),
            nutrients = nutrients,
            additives = o.strList("additives_tags"),
            allergens = o.strList("allergens_tags"),
            traces = o.strList("traces_tags"),
            analysis = o.strList("ingredients_analysis_tags"),
            nova = nova,
            categories = o.strList("categories_tags"),
            isLiquidOverride = if (o.has("lupa_liquid")) o.optBoolean("lupa_liquid") else null,
            isManual = o.optBoolean("lupa_manual", false),
        )
    }

    fun toJson(p: Product): JSONObject {
        val n = JSONObject()
        fun put(key: String, v: Double?) { if (v != null) n.put(key, v) }
        put("energy-kcal_100g", p.nutrients.kcal)
        put("carbohydrates_100g", p.nutrients.carbs)
        put("sugars_100g", p.nutrients.sugars)
        put("added-sugars_100g", p.nutrients.addedSugars)
        put("proteins_100g", p.nutrients.protein)
        put("fat_100g", p.nutrients.fat)
        put("saturated-fat_100g", p.nutrients.satFat)
        put("trans-fat_100g", p.nutrients.transFat)
        put("fiber_100g", p.nutrients.fiber)
        put("sodium_100g", p.nutrients.sodiumMg?.let { it / 1000.0 })

        val o = JSONObject()
        o.put("code", p.code)
        o.put("product_name", p.name)
        p.brand?.let { o.put("brands", it) }
        p.imageUrl?.let { o.put("image_front_url", it) }
        p.quantity?.let { o.put("quantity", it) }
        p.ingredientsText?.let { o.put("ingredients_text", it) }
        o.put("nutriments", n)
        o.put("additives_tags", JSONArray(p.additives))
        o.put("allergens_tags", JSONArray(p.allergens))
        o.put("traces_tags", JSONArray(p.traces))
        o.put("ingredients_analysis_tags", JSONArray(p.analysis))
        p.nova?.let { o.put("nova_group", it) }
        o.put("categories_tags", JSONArray(p.categories))
        p.isLiquidOverride?.let { o.put("lupa_liquid", it) }
        if (p.isManual) o.put("lupa_manual", true)
        return o
    }

    private fun JSONObject.num(key: String): Double? {
        if (!has(key) || isNull(key)) return null
        val d = optDouble(key)
        return if (d.isNaN() || d < 0) null else d
    }

    private fun JSONObject.str(key: String): String? =
        if (!has(key) || isNull(key)) null else optString(key).trim().takeIf { it.isNotEmpty() }

    private fun JSONObject.strList(key: String): List<String> {
        val arr = optJSONArray(key) ?: return emptyList()
        return (0 until arr.length()).mapNotNull { arr.optString(it).takeIf { s -> s.isNotBlank() } }
    }
}
