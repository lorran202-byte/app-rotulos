package com.lorran.lupanutri.domain

import com.lorran.lupanutri.data.model.Nutrients
import com.lorran.lupanutri.data.model.Product
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class LabelAndTacoTest {

    private fun row(label: String, values: String, y: Int) = listOf(
        OcrLine(label, 10, y, 300, y + 30),
        OcrLine(values, 400, y + 2, 900, y + 30),
    )

    @Test fun tabela_nova_100g() {
        val lines = listOf(OcrLine("INFORMAÇÃO NUTRICIONAL", 10, 0, 600, 30), OcrLine("Porções por embalagem: 10 Porção: 30 g", 10, 40, 600, 70),
            OcrLine("100 g 30 g %VD*", 400, 80, 900, 110)) +
            row("Valor energético (kcal)", "350 105 5", 120) +
            row("Carboidratos (g)", "78 23 8", 160) +
            row("Açúcares totais (g)", "0,5 0,2", 200) +
            row("Açúcares adicionados (g)", "0 0 0", 240) +
            row("Proteínas (g)", "7,2 2,2 4", 280) +
            row("Gorduras totais (g)", "0,8 0,2 0", 320) +
            row("Gorduras saturadas (g)", "0,2 0 0", 360) +
            row("Gorduras trans (g)", "O O O", 400) +
            row("Fibras alimentares (g)", "1,6 0,5 2", 440) +
            row("Sódio (mg)", "3 1 0", 480)
        val r = LabelParser.parse(lines)
        assertEquals(350.0, r.values[LabelField.KCAL])
        assertEquals(78.0, r.values[LabelField.CARBS])
        assertEquals(0.5, r.values[LabelField.SUGARS])
        assertEquals(0.0, r.values[LabelField.ADDED_SUGARS])
        assertEquals(7.2, r.values[LabelField.PROTEIN])
        assertEquals(0.8, r.values[LabelField.FAT])
        assertEquals(0.2, r.values[LabelField.SAT_FAT])
        assertEquals(0.0, r.values[LabelField.TRANS_FAT])
        assertEquals(1.6, r.values[LabelField.FIBER])
        assertEquals(3.0, r.values[LabelField.SODIUM])
        assertTrue(!r.convertedFromPortion && !r.maybePerPortion)
    }

    @Test fun tabela_antiga_por_porcao_converte() {
        val lines = listOf(OcrLine("Porção de 50 g (1/2 xícara)", 10, 0, 600, 30)) +
            row("Valor energético 168 kcal = 706 kJ", "8%", 40) +
            listOf(OcrLine("Carboidratos 39 g 13%", 10, 80, 600, 110)) +
            row("Sódio", "120 mg 5%", 120)
        val r = LabelParser.parse(lines)
        assertEquals(336.0, r.values[LabelField.KCAL])
        assertEquals(78.0, r.values[LabelField.CARBS])
        assertEquals(240.0, r.values[LabelField.SODIUM])
        assertTrue(r.convertedFromPortion)
    }

    @Test fun ingredientes() {
        val lines = listOf(OcrLine("INGREDIENTES: Milho flocado, sal.", 0, 0, 500, 20),
            OcrLine("NÃO CONTÉM GLÚTEN.", 0, 25, 500, 45), OcrLine("INFORMAÇÃO NUTRICIONAL", 0, 60, 500, 80))
        assertEquals("Milho flocado, sal. NÃO CONTÉM GLÚTEN", LabelParser.ingredients(lines))
    }

    private val taco: Taco by lazy {
        val f = listOf(File("src/main/assets/taco.tsv"), File("app/src/main/assets/taco.tsv"), File(System.getProperty("taco.path") ?: "x")).first { it.exists() }
        Taco.parse(f.readLines().asSequence())
    }

    @Test fun taco_casamentos() {
        assertTrue(taco.match("Flocão de arroz", "Nutrivita")!!.name.startsWith("Arroz, tipo 1, cru"))
        assertEquals("Aveia, flocos, crua", taco.match("Aveia em flocos")!!.name)
        assertTrue(taco.match("Feijão carioca tipo 1", "Camil")!!.name.startsWith("Feijão, carioca, cru"))
        assertNull(taco.match("Whey protein isolado baunilha"))
    }

    @Test fun taco_completa_so_o_que_falta() {
        val p = Product("1", "Flocão de arroz", "Nutrivita", null, null, null,
            Nutrients(kcal = 168.0, carbs = 39.0, protein = 3.0, fiber = 1.0))
        val f = taco.fill(p)
        assertEquals(168.0, f.nutrients.kcal)
        assertTrue(f.nutrients.sodiumMg != null && f.nutrients.satFat != null && f.nutrients.sugars == 0.0)
        assertTrue(f.tacoName != null)
        assertTrue(!Scorer.score(f, Goal.EQUILIBRIO).estimated)
        val ultra = p.copy(nova = 4)
        assertNull(taco.fill(ultra).tacoName)
    }
}
