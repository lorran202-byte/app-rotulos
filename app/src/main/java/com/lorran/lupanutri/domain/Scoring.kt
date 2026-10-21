package com.lorran.lupanutri.domain

import com.lorran.lupanutri.data.model.Product
import java.util.Locale
import kotlin.math.roundToInt

enum class Goal(val label: String, val description: String) {
    EQUILIBRIO("Equilíbrio", "Alimentação saudável no geral"),
    EMAGRECER("Emagrecimento", "Pesa mais calorias e açúcar, valoriza fibras"),
    MASSA("Ganho de massa", "Valoriza proteína e pesa menos as calorias"),
    PRESSAO("Controle do sódio", "Pesa bem mais o sal (sódio)"),
    ACUCAR("Controle do açúcar", "Pesa bem mais os açúcares"),
    INFANTIL("Infantil", "Mais rigoroso com aditivos e adoçantes"),
}

enum class Band(val label: String) { OTIMO("Ótimo"), BOM("Bom"), RUIM("Ruim"), EVITE("Evite") }

enum class Lupa(val label: String) {
    ACUCAR("Alto em açúcar"), GORDURA("Alto em gordura saturada"), SODIO("Alto em sódio")
}

data class ScoreResult(
    val value: Int?,
    val band: Band?,
    val positives: List<String>,
    val negatives: List<String>,
    val lupa: List<Lupa>,
    /** Nota calculada com dados faltando (o que falta fica em [missing]). */
    val estimated: Boolean = false,
    val missing: List<String> = emptyList(),
)

private class Weights(
    val kcal: Double = 1.0, val sugar: Double = 1.0, val sat: Double = 1.0, val sodium: Double = 1.0,
    val protein: Double = 1.0, val fiber: Double = 1.0, val additives: Double = 1.0, val sweet: Double = 1.0,
)

object Scorer {

    fun bandOf(score: Int): Band = when {
        score >= 75 -> Band.OTIMO
        score >= 55 -> Band.BOM
        score >= 30 -> Band.RUIM
        else -> Band.EVITE
    }

    private fun weights(goal: Goal) = when (goal) {
        Goal.EQUILIBRIO -> Weights()
        Goal.EMAGRECER -> Weights(kcal = 1.5, sugar = 1.3, sat = 1.1, fiber = 1.3)
        Goal.MASSA -> Weights(kcal = 0.4, protein = 2.0)
        Goal.PRESSAO -> Weights(sat = 1.1, sodium = 1.8)
        Goal.ACUCAR -> Weights(sugar = 1.8, fiber = 1.2, sweet = 0.6)
        Goal.INFANTIL -> Weights(sugar = 1.3, sodium = 1.2, additives = 1.6, sweet = 2.0)
    }

    /** Alertas da lupa frontal (limites da RDC 429/2020). */
    fun lupa(p: Product): List<Lupa> {
        val n = p.nutrients
        val liq = p.isLiquid
        val out = mutableListOf<Lupa>()
        val sugar = n.addedSugars ?: n.sugars
        if (sugar != null && sugar >= (if (liq) 7.5 else 15.0)) out += Lupa.ACUCAR
        if (n.satFat != null && n.satFat >= (if (liq) 3.0 else 6.0)) out += Lupa.GORDURA
        if (n.sodiumMg != null && n.sodiumMg >= (if (liq) 300.0 else 600.0)) out += Lupa.SODIO
        return out
    }

    fun score(p: Product, goal: Goal): ScoreResult {
        val n = p.nutrients
        val liq = p.isLiquid
        val w = weights(goal)
        val lupa = lupa(p)
        val pos = mutableListOf<String>()
        val neg = mutableListOf<String>()

        val additives = p.additives
        val attention = additives.count { Additives.isAttention(it) }
        val sweet = additives.any { Additives.isSweetener(it) }

        // ---- textos (valem mesmo sem nota) ----
        n.kcal?.let {
            if (it >= (if (liq) 70.0 else 400.0)) neg += "Muito calórico (${fmt(it)} kcal)"
            else if (it <= (if (liq) 20.0 else 80.0)) pos += "Poucas calorias (${fmt(it)} kcal)"
        }
        val sugar = n.sugars ?: n.addedSugars
        sugar?.let {
            when {
                it >= (if (liq) 7.5 else 15.0) -> neg += "Muito açúcar (${fmt(it)} g)"
                it >= (if (liq) 2.5 else 5.0) -> neg += "Açúcar moderado (${fmt(it)} g)"
                else -> pos += "Pouco açúcar (${fmt(it)} g)"
            }
        }
        n.satFat?.let {
            when {
                it >= (if (liq) 3.0 else 6.0) -> neg += "Muita gordura saturada (${fmt(it)} g)"
                it <= (if (liq) 0.75 else 1.5) -> pos += "Pouca gordura saturada"
            }
        }
        n.sodiumMg?.let {
            when {
                it >= (if (liq) 300.0 else 600.0) -> neg += "Muito sódio (${fmt(it)} mg)"
                it >= (if (liq) 150.0 else 300.0) -> neg += "Sódio moderado (${fmt(it)} mg)"
                it <= 120.0 -> pos += "Pouco sódio (${fmt(it)} mg)"
            }
        }
        if ((n.transFat ?: 0.0) > 0.1) neg += "Contém gordura trans"
        n.fiber?.let {
            if (it >= 6.0) pos += "Rico em fibras (${fmt(it)} g)" else if (it >= 3.0) pos += "Fonte de fibras (${fmt(it)} g)"
        }
        n.protein?.let {
            if (it >= 12.0) pos += "Rico em proteína (${fmt(it)} g)" else if (it >= 6.0) pos += "Fonte de proteína (${fmt(it)} g)"
        }
        when (p.nova) {
            4 -> neg += "Ultraprocessado"
            1 -> pos += "In natura ou minimamente processado"
        }
        if (sweet) neg += "Contém adoçante"
        if (additives.isNotEmpty()) {
            val extra = if (attention > 0) " ($attention de atenção)" else ""
            neg += "${additives.size} aditivo${if (additives.size > 1) "s" else ""}$extra"
        } else if (!p.ingredientsText.isNullOrBlank() && p.nova != 4) {
            pos += "Sem aditivos na lista"
        }

        val missing = n.missingCore
        if (!n.isUsable && !n.canEstimate) return ScoreResult(null, null, pos, neg, lupa, missing = missing)
        val estimated = !n.isUsable

        // ---- nota ----
        var s = 100.0
        val kcal = n.kcal ?: 0.0
        s -= ((kcal - (if (liq) 20.0 else 120.0)) / (if (liq) 8.0 else 20.0)).coerceIn(0.0, 12.0) * w.kcal
        // Sem açúcares informados: em bebidas os carboidratos são quase todos açúcar; em sólidos não dá para supor.
        val sug = n.sugars ?: n.addedSugars ?: (if (liq) n.carbs else null) ?: 0.0
        // Sem gordura saturada mas com gordura total: estima ~1/3 da gordura total.
        val sat = n.satFat ?: n.fat?.let { it / 3.0 } ?: 0.0
        s -= (sug * (if (liq) 3.0 else 0.9)).coerceIn(0.0, if (liq) 35.0 else 25.0) * w.sugar
        s -= (sat * 2.5).coerceIn(0.0, 16.0) * w.sat
        s -= (((n.sodiumMg ?: 0.0) - 80.0) / 35.0).coerceIn(0.0, 25.0) * w.sodium
        if ((n.transFat ?: 0.0) > 0.1) s -= 8.0
        s -= lupa.size * 10.0

        when (p.nova) { 4 -> s -= 18.0; 3 -> s -= 4.0; 1 -> s += 4.0 }
        if (sweet) s -= 6.0 * w.sweet
        val addPen = additives.sumOf { tag ->
            val k = Additives.base(Additives.key(tag))
            when {
                k in setOf("e249", "e250", "e251", "e252") -> 6.0
                Additives.isAttention(tag) -> 4.0
                else -> 2.0
            }
        }
        s -= addPen.coerceAtMost(16.0) * w.additives

        s += ((n.fiber ?: 0.0) * 1.6).coerceAtMost(8.0) * w.fiber
        s += (((n.protein ?: 0.0) * 0.5).coerceAtMost(8.0) * w.protein).coerceAtMost(16.0)

        val value = s.roundToInt().coerceIn(0, 100)
        return ScoreResult(value, bandOf(value), pos, neg, lupa, estimated = estimated, missing = missing)
    }

    fun fmt(v: Double): String =
        if (v >= 100 || v == v.roundToInt().toDouble()) v.roundToInt().toString()
        else String.format(Locale("pt", "BR"), "%.1f", v)
}
