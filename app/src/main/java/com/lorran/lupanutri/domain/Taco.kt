package com.lorran.lupanutri.domain

import com.lorran.lupanutri.data.model.Product
import java.text.Normalizer

/**
 * Tabela Brasileira de Composição de Alimentos (TACO, 4ª ed., NEPA/UNICAMP).
 * Usada só para completar dados que faltam em alimentos simples (poucos ingredientes).
 */
data class TacoFood(
    val id: Int, val category: Int, val name: String,
    val kcal: Double?, val carbs: Double?, val protein: Double?, val fat: Double?,
    val sat: Double?, val fiber: Double?, val sodium: Double?,
)

class Taco(private val foods: List<TacoFood>) {

    private val indexed = foods.map { it to tokens(it.name) }

    /** Melhor alimento da TACO para o nome do produto, ou null se não houver um bom. */
    fun match(productName: String, brand: String? = null): TacoFood? {
        val brandTokens = brand?.let { tokens(it).toSet() }.orEmpty()
        var name = norm(productName)
        for ((from, to) in ALIASES) name = name.replace(from, to)
        val p = tokens(name).filter { it !in brandTokens }
        if (p.isEmpty()) return null
        val cooked = p.any { it.startsWith("cozid") || it.startsWith("frit") || it.startsWith("assad") || it.startsWith("grelhad") }
        var best: TacoFood? = null
        var bestScore = 0.0
        for ((f, t) in indexed) {
            if (t.isEmpty()) continue
            // o alimento principal da TACO (primeira palavra) precisa aparecer no nome do produto
            if (p.none { same(it, t[0]) }) continue
            var score = 2.0
            val matchedOthers = t.drop(1).count { tt -> p.any { same(it, tt) } }
            score += matchedOthers * 1.0
            val unmatched = t.drop(1).count { tt -> p.none { same(it, tt) } && tt !in NEUTRAL }
            score -= unmatched * 0.3
            val raw = t.any { it == "cru" || it == "crua" }
            if (raw && !cooked) score += 0.6
            if (!raw && !cooked && t.any { it.startsWith("cozid") || it.startsWith("frit") || it.startsWith("assad") }) score -= 1.0
            if ("integral" in t && p.none { it == "integral" }) score -= 1.0
            if (score > bestScore + 1e-9) { bestScore = score; best = f }
        }
        return best?.takeIf { bestScore >= 2.0 }
    }

    /**
     * Completa só os nutrientes que faltam. Só vale para alimentos simples
     * (não ultraprocessados e com até 3 ingredientes), onde a TACO é uma boa aproximação.
     */
    fun fill(p: Product): Product {
        if (p.isManual || p.nutrients.isUsable) return p
        if (p.nova == 3 || p.nova == 4) return p
        val ingredientCount = p.ingredientsText?.split(',', ';')?.count { it.isNotBlank() } ?: 0
        if (ingredientCount > 3) return p
        val f = match(p.name, p.brand) ?: return p
        val n = p.nutrients
        val noSugarCategory = f.category in setOf(1, 4, 5, 6, 9, 14) &&
            listOf("doce", "acucar", "mel", "chocolate", "biscoito", "bolo", "pao", "achocolatado").none { it in norm(f.name) }
        val filled = n.copy(
            kcal = n.kcal ?: f.kcal,
            carbs = n.carbs ?: f.carbs,
            protein = n.protein ?: f.protein,
            fat = n.fat ?: f.fat,
            satFat = n.satFat ?: f.sat ?: if (f.fat != null && f.fat < 0.5) 0.0 else null,
            fiber = n.fiber ?: f.fiber,
            sodiumMg = n.sodiumMg ?: f.sodium,
            sugars = n.sugars ?: if (noSugarCategory && n.addedSugars == null) 0.0 else null,
        )
        if (filled == n) return p
        return p.copy(nutrients = filled, tacoName = f.name)
    }

    companion object {
        private val STOP = setOf("de", "da", "do", "das", "dos", "com", "sem", "em", "e", "a", "o", "para", "tipo", "pct", "pacote", "kg", "g", "ml", "un")
        /** Nomes de mercado que a TACO chama de outro jeito. */
        private val ALIASES = listOf(
            "flocao de milho" to "milho fuba", "flocos de milho para cuscuz" to "milho fuba",
            "cuscuz" to "milho fuba", "flocao" to "flocos", "ovos" to "ovo galinha inteiro",
            "parboilizado" to "tipo 1",
        )
        private val NEUTRAL = setOf("cru", "crua", "1", "2", "natural")
        private val SYNONYMS = mapOf(
            "flocao" to "flocos", "floco" to "flocos", "flocada" to "flocos",
            "fuba" to "fuba", "file" to "file", "peito" to "peito",
            "feijao" to "feijao", "macarrao" to "macarrao", "massa" to "macarrao",
        )

        fun norm(s: String): String =
            Normalizer.normalize(s.lowercase(), Normalizer.Form.NFD).replace(Regex("""\p{Mn}+"""), "")

        fun tokens(s: String): List<String> =
            norm(s).split(Regex("""[^a-z0-9]+"""))
                .filter { it.length >= 2 && it !in STOP && it.any { c -> c.isLetter() } }
                .map { SYNONYMS[it] ?: it }

        private fun same(a: String, b: String): Boolean {
            if (a == b || a == b + "s" || b == a + "s") return true
            // plural/singular e variações curtas (flocos/floco, verde/verdes)
            val (s, l) = if (a.length <= b.length) a to b else b to a
            return s.length >= 4 && l.startsWith(s) && l.length - s.length <= 2
        }

        /** Lê o arquivo taco.tsv (cabeçalho + uma linha por alimento). */
        fun parse(lines: Sequence<String>): Taco {
            val foods = lines.drop(1).mapNotNull { line ->
                val c = line.split('\t')
                if (c.size < 10) return@mapNotNull null
                fun d(i: Int) = c[i].toDoubleOrNull()
                TacoFood(c[0].toInt(), c[1].toInt(), c[2], d(3), d(4), d(5), d(6), d(7), d(8), d(9))
            }.toList()
            return Taco(foods)
        }
    }
}
