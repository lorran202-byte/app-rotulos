package com.lorran.lupanutri.domain

import java.text.Normalizer

/** Uma linha de texto lida pela câmera, com a posição na foto. */
data class OcrLine(val text: String, val left: Int, val top: Int, val right: Int, val bottom: Int) {
    val centerY get() = (top + bottom) / 2
    val height get() = (bottom - top).coerceAtLeast(1)
}

enum class LabelField { KCAL, CARBS, SUGARS, ADDED_SUGARS, PROTEIN, FAT, SAT_FAT, TRANS_FAT, FIBER, SODIUM }

data class ParsedLabel(
    val values: Map<LabelField, Double>,
    /** Os números lidos estavam por porção e foram convertidos para 100 g/ml. */
    val convertedFromPortion: Boolean,
    /** Não achamos coluna de 100 g nem o tamanho da porção: pode estar por porção. */
    val maybePerPortion: Boolean,
    val isLiquid: Boolean,
)

/** Lê a tabela nutricional (modelo brasileiro novo e antigo) a partir do texto da foto. */
object LabelParser {

    private fun norm(s: String) =
        Normalizer.normalize(s.lowercase(), Normalizer.Form.NFD).replace(Regex("""\p{Mn}+"""), "")

    /** Ordem importa: os mais específicos primeiro. */
    private val ORDER = listOf(
        LabelField.KCAL, LabelField.ADDED_SUGARS, LabelField.SUGARS, LabelField.CARBS, LabelField.PROTEIN,
        LabelField.SAT_FAT, LabelField.TRANS_FAT, LabelField.FAT, LabelField.FIBER, LabelField.SODIUM,
    )

    private fun matches(field: LabelField, t: String): Boolean = when (field) {
        LabelField.KCAL -> "valor energ" in t || "energia" in t
        LabelField.ADDED_SUGARS -> "acuca" in t && "adicion" in t
        LabelField.SUGARS -> ("acucares" in t || "acucar" in t) && "adicion" !in t
        LabelField.CARBS -> "carboidrat" in t || "carbo-hidrat" in t
        LabelField.PROTEIN -> "protei" in t
        LabelField.SAT_FAT -> "saturad" in t && "insatur" !in t && "poli" !in t && "mono" !in t
        LabelField.TRANS_FAT -> "trans" in t && "gordura" in t
        LabelField.FAT -> ("gordura" in t || "lipid" in t) && "saturad" !in t && "trans" !in t
        LabelField.FIBER -> "fibra" in t
        LabelField.SODIUM -> "sodio" in t
    }

    private val NUM = Regex("""(?<![\d.,])(\d{1,4}(?:[.,]\d{1,2})?)\s*(%|kcal|kj|mg|mcg|µg|g|ml)?""")

    private data class Num(val value: Double, val unit: String?)

    private fun numbers(text: String): List<Num> {
        // "O" isolado no lugar de zero é erro comum de leitura
        val fixed = Regex("""(?<=\s|^)[oO](?=\s*(g|mg|%|$))""").replace(text, "0")
        return NUM.findAll(fixed).mapNotNull { m ->
            val v = m.groupValues[1].replace(',', '.').toDoubleOrNull() ?: return@mapNotNull null
            Num(v, m.groupValues[2].lowercase().ifEmpty { null })
        }.filter { it.unit != "%" }.toList()
    }

    fun parse(lines: List<OcrLine>): ParsedLabel {
        val all = norm(lines.joinToString("\n") { it.text })
        val has100 = Regex("""100\s*(g|ml)\b""").containsMatchIn(all)
        val portion = Regex("""porcao\s*(?:de|:)?\s*(\d{1,4}(?:[.,]\d)?)\s*(g|ml)""").find(all)
            ?.groupValues?.get(1)?.replace(',', '.')?.toDoubleOrNull()
            ?.takeIf { it in 1.0..2000.0 }
        val liquid = Regex("""100\s*ml\b""").containsMatchIn(all) ||
            Regex("""porcao\s*(?:de|:)?\s*\d+[.,]?\d*\s*ml""").containsMatchIn(all)

        val used = mutableSetOf<Int>()
        val raw = mutableMapOf<LabelField, Double>()
        val normLines = lines.map { norm(it.text) }

        for (field in ORDER) {
            val idx = normLines.indices.firstOrNull { it !in used && matches(field, normLines[it]) } ?: continue
            used += idx
            val line = lines[idx]
            val t = normLines[idx]
            val keyEnd = keywordEnd(field, t)
            val inLine = numbers(t.substring(keyEnd))
            // Números das outras caixas de texto na mesma "linha" da tabela, à direita do rótulo
            val tol = line.height * 0.6
            val right = lines.indices
                .filter { it != idx && lines[it].left >= line.left + line.height && kotlin.math.abs(lines[it].centerY - line.centerY) <= tol }
                .sortedBy { lines[it].left }
                .filter { j -> ORDER.none { matches(it, normLines[j]) } }
                .flatMap { numbers(normLines[it]) }
            val nums = inLine + right
            val pick = if (field == LabelField.KCAL) {
                nums.firstOrNull { it.unit == "kcal" } ?: nums.firstOrNull { it.unit != "kj" }
            } else nums.firstOrNull()
            pick?.let { raw[field] = it.value }
        }

        val factor = if (!has100 && portion != null) 100.0 / portion else 1.0
        val values = raw.mapValues { (_, v) -> round1(v * factor) }
        return ParsedLabel(
            values = values,
            convertedFromPortion = factor != 1.0,
            maybePerPortion = !has100 && portion == null && values.isNotEmpty(),
            isLiquid = liquid,
        )
    }

    private fun keywordEnd(field: LabelField, t: String): Int {
        val keys = when (field) {
            LabelField.KCAL -> listOf("valor energetico", "valor energ", "energia")
            LabelField.ADDED_SUGARS -> listOf("adicionados", "adicionado", "adicion")
            LabelField.SUGARS -> listOf("totais", "acucares", "acucar")
            LabelField.CARBS -> listOf("carboidratos", "carboidrato", "carbo-hidratos", "carboidrat")
            LabelField.PROTEIN -> listOf("proteinas", "proteina", "protei")
            LabelField.SAT_FAT -> listOf("saturadas", "saturada", "saturad")
            LabelField.TRANS_FAT -> listOf("trans")
            LabelField.FAT -> listOf("totais", "total", "gorduras", "gordura", "lipidios", "lipid")
            LabelField.FIBER -> listOf("alimentares", "alimentar", "fibras", "fibra")
            LabelField.SODIUM -> listOf("sodio")
        }
        for (k in keys) {
            val i = t.indexOf(k)
            if (i >= 0) return i + k.length
        }
        return 0
    }

    /** Extrai a lista de ingredientes do texto da foto. */
    fun ingredients(lines: List<OcrLine>): String? {
        val text = lines.sortedWith(compareBy({ it.top }, { it.left })).joinToString(" ") { it.text }
            .replace(Regex("""\s+"""), " ")
        val n = norm(text)
        val start = n.indexOf("ingredientes")
        if (start < 0) return null
        var from = start + "ingredientes".length
        while (from < text.length && (text[from] == ':' || text[from] == ' ')) from++
        val stops = listOf("informacao nutricional", "valor energetico", "conservar", "validade", "fabricado", "lote", "sac ")
        val end = stops.map { n.indexOf(it, from) }.filter { it > from }.minOrNull() ?: text.length
        return text.substring(from, end).trim().trimEnd('.', ',').take(1500).ifBlank { null }
    }

    private fun round1(v: Double) = Math.round(v * 10.0) / 10.0
}
