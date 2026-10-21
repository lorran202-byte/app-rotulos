package com.lorran.lupanutri.ui.product

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lorran.lupanutri.data.Fetch
import com.lorran.lupanutri.data.Profile
import com.lorran.lupanutri.data.ProductRepository
import com.lorran.lupanutri.data.ProfileStore
import com.lorran.lupanutri.data.ShoppingRepository
import com.lorran.lupanutri.data.model.Product
import com.lorran.lupanutri.domain.Scorer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface ProductUi {
    data object Loading : ProductUi
    data object NotFound : ProductUi
    data object Offline : ProductUi
    data class Loaded(val product: Product, val fromCache: Boolean) : ProductUi
}

sealed interface AltUi {
    data object Hidden : AltUi
    data object Loading : AltUi
    data class Loaded(val items: List<Product>) : AltUi
    data object Failed : AltUi
}

class ProductViewModel(
    private val repo: ProductRepository,
    profileStore: ProfileStore,
    private val shopping: ShoppingRepository,
    private val code: String,
) : ViewModel() {

    private val _ui = MutableStateFlow<ProductUi>(ProductUi.Loading)
    val ui: StateFlow<ProductUi> = _ui.asStateFlow()

    private val _alts = MutableStateFlow<AltUi>(AltUi.Hidden)
    val alts: StateFlow<AltUi> = _alts.asStateFlow()

    val profile: StateFlow<Profile> = profileStore.profile.stateIn(viewModelScope, SharingStarted.Eagerly, Profile())
    val isFavorite: StateFlow<Boolean> = repo.isFavorite(code).stateIn(viewModelScope, SharingStarted.Eagerly, false)
    val inList: StateFlow<Boolean> = shopping.isInList(code).stateIn(viewModelScope, SharingStarted.Eagerly, false)

    fun toggleList() {
        val p = (ui.value as? ProductUi.Loaded)?.product ?: return
        viewModelScope.launch { if (inList.value) shopping.removeProduct(p.code) else shopping.addProduct(p) }
    }

    init { load() }

    fun load() {
        _ui.value = ProductUi.Loading
        viewModelScope.launch {
            _ui.value = when (val r = repo.fetch(code)) {
                is Fetch.Found -> ProductUi.Loaded(r.product, r.fromCache).also { loadAlternatives(r.product) }
                Fetch.NotFound -> ProductUi.NotFound
                Fetch.Offline -> ProductUi.Offline
            }
        }
    }

    private fun loadAlternatives(p: Product) {
        if (p.isManual || p.categories.isEmpty() || !p.nutrients.isUsable) {
            _alts.value = AltUi.Hidden
            return
        }
        _alts.value = AltUi.Loading
        viewModelScope.launch {
            val goal = profile.first().goal
            val mine = Scorer.score(p, goal).value ?: 0
            val tags = p.categories.takeLast(2).reversed()
            var found: List<Product> = emptyList()
            var failed = false
            for (tag in tags) {
                try {
                    found = repo.byCategory(tag)
                        .filter { it.code != p.code && it.nutrients.isUsable }
                        .map { it to (Scorer.score(it, goal).value ?: 0) }
                        .filter { it.second >= mine + 5 }
                        .sortedByDescending { it.second }
                        .distinctBy { it.first.name.lowercase() }
                        .take(8)
                        .map { it.first }
                    failed = false
                    if (found.size >= 3) break
                } catch (e: Exception) {
                    failed = true
                }
            }
            _alts.value = if (failed && found.isEmpty()) AltUi.Failed else AltUi.Loaded(found)
        }
    }

    fun toggleFavorite() {
        val p = (ui.value as? ProductUi.Loaded)?.product ?: return
        viewModelScope.launch { repo.setFavorite(p, !isFavorite.value) }
    }
}
