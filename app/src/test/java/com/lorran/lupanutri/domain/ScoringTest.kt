package com.lorran.lupanutri.domain

import com.lorran.lupanutri.data.model.Nutrients
import com.lorran.lupanutri.data.model.Product
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ScoringTest {

    private fun p(
        n: Nutrients, nova: Int? = null, additives: List<String> = emptyList(),
        ingredients: String? = null, liquid: Boolean = false,
    ) = Product(
        code = "1", name = "x", brand = null, imageUrl = null, quantity = null,
        ingredientsText = ingredients, nutrients = n, additives = additives, nova = nova,
        isLiquidOverride = liquid,
    )

    private val soda = p(Nutrients(kcal = 42.0, sugars = 10.6, satFat = 0.0, sodiumMg = 10.0), nova = 4,
        additives = listOf("en:e150d", "en:e338"), liquid = true)
    private val cookie = p(Nutrients(kcal = 480.0, sugars = 35.0, satFat = 10.0, sodiumMg = 250.0, fiber = 2.0, protein = 5.0),
        nova = 4, additives = listOf("en:e322", "en:e500", "en:e503"))
    private val oats = p(Nutrients(kcal = 390.0, sugars = 1.0, satFat = 1.5, sodiumMg = 5.0, fiber = 9.0, protein = 14.0), nova = 1,
        ingredients = "Aveia em flocos")
    private val water = p(Nutrients(kcal = 0.0, sugars = 0.0, satFat = 0.0, sodiumMg = 2.0), nova = 1, liquid = true)

    @Test fun ordem_faz_sentido() {
        val g = Goal.EQUILIBRIO
        val s = { x: Product -> Scorer.score(x, g).value!! }
        assertTrue(s(water) >= 95)
        assertTrue(s(oats) >= 75)
        assertTrue(s(soda) < 55)
        assertTrue(s(cookie) < 30)
    }

    @Test fun lupa_refrigerante() {
        assertEquals(listOf(Lupa.ACUCAR), Scorer.lupa(soda))
    }

    @Test fun sem_dados_sem_nota() {
        assertNull(Scorer.score(p(Nutrients(kcal = 100.0)), Goal.EQUILIBRIO).value)
    }

    @Test fun massa_valoriza_proteina() {
        val whey = p(Nutrients(kcal = 400.0, sugars = 5.0, satFat = 2.0, sodiumMg = 200.0, protein = 75.0), nova = 4,
            additives = listOf("en:e955", "en:e322"))
        assertTrue(Scorer.score(whey, Goal.MASSA).value!! > Scorer.score(whey, Goal.EQUILIBRIO).value!!)
    }

    @Test fun restricoes_texto() {
        val semGluten = p(Nutrients(), ingredients = "Milho, sal. NÃO CONTÉM GLÚTEN. Pode conter leite.")
        assertEquals(Match.NAO_CONTEM, Restrictions.check(semGluten, Restriction.GLUTEN))
        assertEquals(Match.PODE_CONTER, Restrictions.check(semGluten, Restriction.LEITE))
        val biscoito = p(Nutrients(), ingredients = "Farinha de trigo enriquecida com ferro, açúcar, gordura vegetal, corante caramelo")
        assertEquals(Match.CONTEM, Restrictions.check(biscoito, Restriction.GLUTEN))
        assertEquals(Match.CONTEM, Restrictions.check(biscoito, Restriction.ACUCAR))
        assertEquals(Match.CONTEM, Restrictions.check(biscoito, Restriction.CORANTE))
        val melancia = p(Nutrients(), ingredients = "Suco de melancia")
        assertEquals(Match.NAO_CONTEM, Restrictions.check(melancia, Restriction.ACUCAR))
        assertEquals(Match.DESCONHECIDO, Restrictions.check(p(Nutrients()), Restriction.SOJA))
    }
}
