@file:OptIn(ExperimentalMaterial3Api::class)

package com.lorran.lupanutri.ui.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ManageSearch
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lorran.lupanutri.LupaApp
import com.lorran.lupanutri.data.Profile
import com.lorran.lupanutri.data.ProductRepository
import com.lorran.lupanutri.data.ProfileStore
import com.lorran.lupanutri.data.model.Product
import com.lorran.lupanutri.ui.components.CenterMessage
import com.lorran.lupanutri.ui.components.LoadingBox
import com.lorran.lupanutri.ui.theme.Space
import com.lorran.lupanutri.ui.components.ProductRow
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface SearchUi {
    data object Idle : SearchUi
    data object Loading : SearchUi
    data class Results(val items: List<Product>, val online: Boolean) : SearchUi
}

class SearchViewModel(private val repo: ProductRepository, profileStore: ProfileStore) : ViewModel() {
    val query = MutableStateFlow("")
    val ui = MutableStateFlow<SearchUi>(SearchUi.Idle)
    val profile: StateFlow<Profile> = profileStore.profile.stateIn(viewModelScope, SharingStarted.Eagerly, Profile())
    private var job: Job? = null

    fun search() {
        val q = query.value.trim()
        if (q.length < 2) return
        job?.cancel()
        ui.value = SearchUi.Loading
        job = viewModelScope.launch {
            val (items, online) = repo.search(q)
            ui.value = SearchUi.Results(items, online)
        }
    }

    fun clear() {
        job?.cancel()
        query.value = ""
        ui.value = SearchUi.Idle
    }
}

@Composable
fun SearchScreen(app: LupaApp, onOpen: (String) -> Unit) {
    val vm: SearchViewModel = viewModel { SearchViewModel(app.repository, app.profileStore) }
    val query by vm.query.collectAsStateWithLifecycle()
    val ui by vm.ui.collectAsStateWithLifecycle()
    val profile by vm.profile.collectAsStateWithLifecycle()
    val keyboard = LocalSoftwareKeyboardController.current

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Buscar produto") },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            OutlinedTextField(
                value = query,
                onValueChange = { vm.query.value = it },
                modifier = Modifier.fillMaxWidth().padding(horizontal = Space.lg),
                placeholder = { Text("Nome ou marca (ex.: granola, iogurte natural)") },
                leadingIcon = { Icon(Icons.Outlined.Search, null) },
                trailingIcon = {
                    if (query.isNotEmpty()) IconButton(onClick = vm::clear) { Icon(Icons.Outlined.Close, "Limpar") }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { keyboard?.hide(); vm.search() }),
                shape = MaterialTheme.shapes.medium,
            )
            Box(Modifier.fillMaxSize()) {
                when (val s = ui) {
                    SearchUi.Idle -> CenterMessage(
                        icon = Icons.Outlined.ManageSearch,
                        title = "Procure sem escanear",
                        body = "Digite o nome ou a marca para ver a nota de produtos vendidos no Brasil.",
                    )
                    SearchUi.Loading -> LoadingBox()
                    is SearchUi.Results -> if (s.items.isEmpty()) {
                        CenterMessage(
                            icon = Icons.Outlined.SearchOff,
                            title = if (s.online) "Nada encontrado" else "Sem conexão",
                            body = if (s.online) "Tente outro nome ou escaneie o código de barras."
                            else "Sem internet, a busca só olha os produtos já salvos no aparelho.",
                            actionLabel = if (s.online) null else "Tentar de novo",
                            onAction = if (s.online) null else vm::search,
                        )
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(Space.lg),
                            verticalArrangement = Arrangement.spacedBy(Space.sm),
                        ) {
                            if (!s.online) item {
                                Text("Sem internet: mostrando só produtos salvos.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.tertiary)
                            }
                            items(s.items, key = { it.code }) { p ->
                                ProductRow(p, profile.goal, onClick = { onOpen(p.code) })
                            }
                        }
                    }
                }
            }
        }
    }
}
