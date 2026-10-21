package com.lorran.lupanutri.data

import com.lorran.lupanutri.data.db.ProductDao
import com.lorran.lupanutri.data.db.ProductEntity
import com.lorran.lupanutri.data.model.Product
import com.lorran.lupanutri.domain.Taco
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONObject

sealed interface Fetch {
    data class Found(val product: Product, val fromCache: Boolean) : Fetch
    data object NotFound : Fetch
    data object Offline : Fetch
}

data class Saved(val product: Product, val isFavorite: Boolean, val viewedAt: Long?)

class ProductRepository(
    private val dao: ProductDao,
    private val api: OffApi,
    private val taco: Lazy<Taco?>,
) {

    /** Completa dados faltando com a TACO (só em memória; o que fica salvo é o dado original). */
    private fun Product.filled(): Product = taco.value?.let { t -> runCatching { t.fill(this) }.getOrNull() } ?: this

    private fun ProductEntity.toRaw(): Product? =
        runCatching { OffCodec.parse(JSONObject(json), fallbackCode = code) }.getOrNull()

    private fun ProductEntity.toProduct(): Product? = toRaw()?.filled()

    private fun ProductEntity.toSaved(): Saved? = toProduct()?.let { Saved(it, isFavorite, lastViewedAt) }

    val history: Flow<List<Saved>> = dao.observeHistory().map { l -> l.mapNotNull { it.toSaved() } }
    val favorites: Flow<List<Saved>> = dao.observeFavorites().map { l -> l.mapNotNull { it.toSaved() } }

    fun isFavorite(code: String): Flow<Boolean> = dao.observe(code).map { it?.isFavorite == true }

    suspend fun cached(code: String): Product? = dao.get(code)?.toProduct()

    /** Como está salvo, sem completar com a TACO (para o formulário de cadastro). */
    suspend fun cachedRaw(code: String): Product? = dao.get(code)?.toRaw()

    /** Busca na rede; se não der, usa o que já está salvo no aparelho. */
    suspend fun fetch(code: String): Fetch {
        val local = dao.get(code)
        val localRaw = local?.toRaw()
        if (localRaw?.isManual == true) {
            markViewed(localRaw)
            return Fetch.Found(localRaw, fromCache = true)
        }
        return try {
            val remote = api.product(code)
            when {
                remote != null -> { markViewed(remote); Fetch.Found(remote.filled(), fromCache = false) }
                localRaw != null -> { markViewed(localRaw); Fetch.Found(localRaw.filled(), fromCache = true) }
                else -> Fetch.NotFound
            }
        } catch (e: Exception) {
            if (localRaw != null) { markViewed(localRaw); Fetch.Found(localRaw.filled(), fromCache = true) }
            else Fetch.Offline
        }
    }

    private suspend fun markViewed(p: Product) = save(p, viewed = true)

    suspend fun save(p: Product, viewed: Boolean) {
        val old = dao.get(p.code)
        dao.upsert(
            ProductEntity(
                code = p.code,
                // Valores completados pela TACO nunca viram dado salvo: mantém o original.
                json = if (p.tacoName != null && old != null) old.json else OffCodec.toJson(p).toString(),
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
            val remote = api.search(query).map { it.filled() }
            val seen = local.map { it.code }.toSet()
            (local + remote.filter { it.code !in seen }) to true
        } catch (e: Exception) {
            local to false
        }
    }

    suspend fun byCategory(tag: String): List<Product> = api.byCategory(tag)
}
