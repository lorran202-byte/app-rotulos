package com.lorran.lupanutri.data

import com.lorran.lupanutri.data.db.ProductDao
import com.lorran.lupanutri.data.db.ProductEntity
import com.lorran.lupanutri.data.model.Product
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONObject

sealed interface Fetch {
    data class Found(val product: Product, val fromCache: Boolean) : Fetch
    data object NotFound : Fetch
    data object Offline : Fetch
}

data class Saved(val product: Product, val isFavorite: Boolean, val viewedAt: Long?)

class ProductRepository(private val dao: ProductDao, private val api: OffApi) {

    private fun ProductEntity.toProduct(): Product? =
        runCatching { OffCodec.parse(JSONObject(json), fallbackCode = code) }.getOrNull()

    private fun ProductEntity.toSaved(): Saved? = toProduct()?.let { Saved(it, isFavorite, lastViewedAt) }

    val history: Flow<List<Saved>> = dao.observeHistory().map { l -> l.mapNotNull { it.toSaved() } }
    val favorites: Flow<List<Saved>> = dao.observeFavorites().map { l -> l.mapNotNull { it.toSaved() } }

    fun isFavorite(code: String): Flow<Boolean> = dao.observe(code).map { it?.isFavorite == true }

    suspend fun cached(code: String): Product? = dao.get(code)?.toProduct()

    /** Busca na rede; se não der, usa o que já está salvo no aparelho. */
    suspend fun fetch(code: String): Fetch {
        val local = dao.get(code)
        val localProduct = local?.toProduct()
        if (localProduct?.isManual == true) {
            markViewed(localProduct)
            return Fetch.Found(localProduct, fromCache = true)
        }
        return try {
            val remote = api.product(code)
            when {
                remote != null -> { markViewed(remote); Fetch.Found(remote, fromCache = false) }
                localProduct != null -> { markViewed(localProduct); Fetch.Found(localProduct, fromCache = true) }
                else -> Fetch.NotFound
            }
        } catch (e: Exception) {
            if (localProduct != null) { markViewed(localProduct); Fetch.Found(localProduct, fromCache = true) }
            else Fetch.Offline
        }
    }

    private suspend fun markViewed(p: Product) = save(p, viewed = true)

    suspend fun save(p: Product, viewed: Boolean) {
        val old = dao.get(p.code)
        dao.upsert(
            ProductEntity(
                code = p.code,
                json = OffCodec.toJson(p).toString(),
                name = p.name,
                brand = p.brand,
                imageUrl = p.imageUrl,
                isManual = p.isManual,
                isFavorite = old?.isFavorite ?: false,
                lastViewedAt = if (viewed) System.currentTimeMillis() else old?.lastViewedAt,
                favoritedAt = old?.favoritedAt,
            )
        )
    }

    suspend fun setFavorite(p: Product, fav: Boolean) {
        if (dao.get(p.code) == null) save(p, viewed = false)
        dao.setFavorite(p.code, fav, if (fav) System.currentTimeMillis() else null)
    }

    suspend fun removeFromHistory(code: String) = dao.removeFromHistory(code)
    suspend fun clearHistory() = dao.clearHistory()
    suspend fun deleteManual(code: String) = dao.delete(code)

    suspend fun search(query: String): Pair<List<Product>, Boolean> {
        val local = dao.searchLocal(query).mapNotNull { it.toProduct() }
        return try {
            val remote = api.search(query)
            val seen = local.map { it.code }.toSet()
            (local + remote.filter { it.code !in seen }) to true
        } catch (e: Exception) {
            local to false
        }
    }

    suspend fun byCategory(tag: String): List<Product> = api.byCategory(tag)
}
