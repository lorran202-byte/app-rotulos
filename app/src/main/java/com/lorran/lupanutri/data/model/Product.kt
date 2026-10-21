package com.lorran.lupanutri.data.model

/** Valores por 100 g (ou 100 ml em bebidas). Null = não informado. */
data class Nutrients(
    val kcal: Double? = null,
    val carbs: Double? = null,
    val sugars: Double? = null,
    val addedSugars: Double? = null,
    val protein: Double? = null,
    val fat: Double? = null,
    val satFat: Double? = null,
    val transFat: Double? = null,
    val fiber: Double? = null,
    val sodiumMg: Double? = null,
) {
    /** Tem o mínimo para calcular uma nota. */
    val isUsable: Boolean
        get() = kcal != null && (sugars != null || carbs != null) && satFat != null && sodiumMg != null
}

data class Product(
    val code: String,
    val name: String,
    val brand: String?,
    val imageUrl: String?,
    val quantity: String?,
    val ingredientsText: String?,
    val nutrients: Nutrients,
    /** Ex.: "en:e330" */
    val additives: List<String> = emptyList(),
    /** Ex.: "en:gluten" */
    val allergens: List<String> = emptyList(),
    val traces: List<String> = emptyList(),
    /** Ex.: "en:non-vegan" */
    val analysis: List<String> = emptyList(),
    val nova: Int? = null,
    /** Do mais geral ao mais específico. Ex.: "en:beverages" */
    val categories: List<String> = emptyList(),
    val isLiquidOverride: Boolean? = null,
    val isManual: Boolean = false,
) {
    val isLiquid: Boolean
        get() {
            isLiquidOverride?.let { return it }
            if (categories.any { it in LIQUID_CATEGORIES }) return true
            val q = quantity?.lowercase() ?: return false
            return Regex("""\d\s*(ml|l|litros?)\b""").containsMatchIn(q)
        }

    val unitLabel: String get() = if (isLiquid) "100 ml" else "100 g"

    companion object {
        private val LIQUID_CATEGORIES = setOf(
            "en:beverages", "en:waters", "en:milks", "en:sodas", "en:juices",
            "en:fruit-juices", "en:plant-based-beverages", "en:dairy-drinks",
        )
    }
}
