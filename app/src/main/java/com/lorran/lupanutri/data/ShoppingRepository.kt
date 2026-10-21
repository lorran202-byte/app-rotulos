package com.lorran.lupanutri.data

import com.lorran.lupanutri.data.db.ShoppingDao
import com.lorran.lupanutri.data.db.ShoppingItem
import com.lorran.lupanutri.data.model.Product
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/** Item da lista com o produto salvo (quando veio do scanner), para mostrar a nota. */
data class ListEntry(val item: ShoppingItem, val product: Product?)

class ShoppingRepository(private val dao: ShoppingDao, private val products: ProductRepository) {

    val entries: Flow<List<ListEntry>> = dao.observeAll().map { list ->
        list.map { ListEntry(it, it.code?.let { c -> products.cached(c) }) }
    }

    fun isInList(code: String): Flow<Boolean> = dao.observeOpen(code).map { it > 0 }

    suspend fun addText(text: String) {
        val t = text.trim()
        if (t.isEmpty()) return
        dao.insert(ShoppingItem(text = t, code = null, createdAt = System.currentTimeMillis()))
    }

    /** Adiciona o produto; se já estiver na lista (não comprado), não duplica. */
    suspend fun addProduct(p: Product) {
        if (dao.countOpen(p.code) > 0) return
        products.save(p, viewed = false)
        val label = listOfNotNull(p.name, p.brand).joinToString(" – ")
        dao.insert(ShoppingItem(text = label, code = p.code, createdAt = System.currentTimeMillis()))
    }

    suspend fun removeProduct(code: String) = dao.deleteOpenByCode(code)

    /** Coloca na lista todos os favoritos que ainda não estão nela. Retorna quantos entraram. */
    suspend fun addFavorites(): Int {
        var n = 0
        products.favorites.first().forEach { s ->
            if (dao.countOpen(s.product.code) == 0) { addProduct(s.product); n++ }
        }
        return n
    }

    suspend fun setChecked(id: Long, checked: Boolean) = dao.setChecked(id, checked)
    suspend fun delete(id: Long) = dao.delete(id)
    suspend fun clearChecked() = dao.clearChecked()
}
