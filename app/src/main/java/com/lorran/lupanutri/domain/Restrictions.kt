package com.lorran.lupanutri.domain

import com.lorran.lupanutri.data.model.Product
import java.text.Normalizer

enum class Match { NAO_CONTEM, PODE_CONTER, CONTEM, DESCONHECIDO }

enum class Restriction(val label: String, val warning: String) {
    GLUTEN("Sem glúten", "glúten"),
    LEITE("Sem leite/lactose", "leite ou lactose"),
    OVO("Sem ovo", "ovo"),
    SOJA("Sem soja", "soja"),
    AMENDOIM("Sem amendoim", "amendoim"),
    CASTANHAS("Sem castanhas", "castanhas"),
    FRUTOS_MAR("Sem peixe/frutos do mar", "peixe ou frutos do mar"),
    VEGANO("Vegano", "ingrediente de origem animal"),
    VEGETARIANO("Vegetariano", "carne ou peixe"),
    ACUCAR("Sem açúcar adicionado", "açúcar adicionado"),
    ADOCANTE("Sem adoçante", "adoçante"),
    CORANTE("Sem corante", "corante"),
    TRANS("Sem gordura trans", "gordura trans"),
    ADITIVOS("Sem aditivos", "aditivos"),
}

data class RestrictionHit(val restriction: Restriction, val match: Match)

object Restrictions {

    private val KEYWORDS = mapOf(
        Restriction.GLUTEN to listOf("trigo", "gluten", "cevada", "centeio", "malte", "aveia"),
        Restriction.LEITE to listOf("leite", "lactose", "soro de leite", "caseina", "caseinato", "manteiga", "creme de leite", "queijo", "lacteo"),
        Restriction.OVO to listOf("ovo", "ovos", "albumina", "clara de ovo"),
        Restriction.SOJA to listOf("soja"),
        Restriction.AMENDOIM to listOf("amendoim"),
        Restriction.CASTANHAS to listOf("castanha", "amendoa", "avela", "nozes", "noz", "macadamia", "pistache", "caju"),
        Restriction.FRUTOS_MAR to listOf("peixe", "camarao", "atum", "sardinha", "bacalhau", "crustaceo", "molusco", "lula", "salmao"),
        Restriction.VEGETARIANO to listOf("carne", "frango", "bovina", "suina", "bacon", "presunto", "gelatina", "peixe", "atum", "sardinha", "camarao", "toucinho", "banha"),
        Restriction.ADOCANTE to listOf("sucralose", "aspartame", "acessulfame", "ciclamato", "sacarina", "estevia", "stevia", "eritritol", "xilitol", "sorbitol", "maltitol"),
        Restriction.ACUCAR to listOf("acucar", "xarope de glicose", "xarope de milho", "xarope de frutose", "maltodextrina", "dextrose", "glicose", "frutose", "acucar invertido", "mel", "melado", "acucar mascavo"),
        Restriction.CORANTE to listOf("corante"),
        Restriction.TRANS to listOf("hidrogenad"),
    )

    private val ALLERGEN_TAGS = mapOf(
        Restriction.GLUTEN to setOf("en:gluten"),
        Restriction.LEITE to setOf("en:milk"),
        Restriction.OVO to setOf("en:eggs"),
        Restriction.SOJA to setOf("en:soybeans"),
        Restriction.AMENDOIM to setOf("en:peanuts"),
        Restriction.CASTANHAS to setOf("en:nuts"),
        Restriction.FRUTOS_MAR to setOf("en:fish", "en:crustaceans", "en:molluscs"),
    )

    fun check(p: Product, selected: Set<Restriction>): List<RestrictionHit> =
        selected.map { RestrictionHit(it, check(p, it)) }

    fun check(p: Product, r: Restriction): Match {
        val raw = p.ingredientsText.orEmpty()
        val hasText = raw.isNotBlank()
        val text = normalize(raw)
            .replace("nao contem gluten", " ")
            .replace("sem gluten", " ")
            .replace("zero lactose", " ")
            .replace("sem lactose", " ")
            .replace("nao contem lactose", " ")
            .replace("carne de soja", "proteina de soja")
            .replace("noz-moscada", "especiaria")
            .replace("noz moscada", "especiaria")
        val idx = text.indexOf("pode conter")
        val main = if (idx >= 0) text.substring(0, idx) else text
        val traces = if (idx >= 0) text.substring(idx) else ""

        fun textHas(t: String, words: List<String>) = words.any { w ->
            val end = if (w.length <= 4) "\\b" else ""
            Regex("\\b" + Regex.escape(w) + end).containsMatchIn(t)
        }

        return when (r) {
            Restriction.GLUTEN, Restriction.LEITE, Restriction.OVO, Restriction.SOJA,
            Restriction.AMENDOIM, Restriction.CASTANHAS, Restriction.FRUTOS_MAR -> {
                val tags = ALLERGEN_TAGS.getValue(r)
                val words = KEYWORDS.getValue(r)
                when {
                    p.allergens.any { it in tags } || textHas(main, words) -> Match.CONTEM
                    p.traces.any { it in tags } || textHas(traces, words) -> Match.PODE_CONTER
                    hasText || p.allergens.isNotEmpty() -> Match.NAO_CONTEM
                    else -> Match.DESCONHECIDO
                }
            }
            Restriction.VEGANO -> when {
                "en:non-vegan" in p.analysis -> Match.CONTEM
                p.allergens.any { it in setOf("en:milk", "en:eggs", "en:fish", "en:crustaceans", "en:molluscs") } -> Match.CONTEM
                hasText && (textHas(main, KEYWORDS.getValue(Restriction.LEITE)) ||
                    textHas(main, KEYWORDS.getValue(Restriction.OVO)) ||
                    textHas(main, KEYWORDS.getValue(Restriction.VEGETARIANO))) -> Match.CONTEM
                "en:maybe-vegan" in p.analysis -> Match.PODE_CONTER
                "en:vegan" in p.analysis || hasText -> Match.NAO_CONTEM
                else -> Match.DESCONHECIDO
            }
            Restriction.VEGETARIANO -> when {
                "en:non-vegetarian" in p.analysis -> Match.CONTEM
                p.allergens.any { it in setOf("en:fish", "en:crustaceans", "en:molluscs") } -> Match.CONTEM
                hasText && textHas(main, KEYWORDS.getValue(Restriction.VEGETARIANO)) -> Match.CONTEM
                "en:maybe-vegetarian" in p.analysis -> Match.PODE_CONTER
                "en:vegetarian" in p.analysis || hasText -> Match.NAO_CONTEM
                else -> Match.DESCONHECIDO
            }
            Restriction.ADOCANTE -> when {
                p.additives.any { Additives.isSweetener(it) } || textHas(main, KEYWORDS.getValue(r)) -> Match.CONTEM
                hasText -> Match.NAO_CONTEM
                else -> Match.DESCONHECIDO
            }
            Restriction.ACUCAR -> when {
                (p.nutrients.addedSugars ?: 0.0) > 0.5 || textHas(main, KEYWORDS.getValue(r)) -> Match.CONTEM
                hasText || p.nutrients.addedSugars != null -> Match.NAO_CONTEM
                else -> Match.DESCONHECIDO
            }
            Restriction.CORANTE -> when {
                p.additives.any { Additives.isColorant(it) } || textHas(main, KEYWORDS.getValue(r)) -> Match.CONTEM
                hasText -> Match.NAO_CONTEM
                else -> Match.DESCONHECIDO
            }
            Restriction.TRANS -> when {
                (p.nutrients.transFat ?: 0.0) > 0.1 || textHas(main, KEYWORDS.getValue(r)) -> Match.CONTEM
                hasText || p.nutrients.transFat != null -> Match.NAO_CONTEM
                else -> Match.DESCONHECIDO
            }
            Restriction.ADITIVOS -> when {
                p.additives.isNotEmpty() -> Match.CONTEM
                hasText -> Match.NAO_CONTEM
                else -> Match.DESCONHECIDO
            }
        }
    }

    fun normalize(s: String): String =
        Normalizer.normalize(s.lowercase(), Normalizer.Form.NFD).replace(Regex("""\p{Mn}+"""), "")
}
