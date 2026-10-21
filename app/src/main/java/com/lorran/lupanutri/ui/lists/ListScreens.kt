@file:OptIn(ExperimentalMaterial3Api::class)

package com.lorran.lupanutri.ui.lists

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lorran.lupanutri.LupaApp
import com.lorran.lupanutri.data.Profile
import com.lorran.lupanutri.data.ProductRepository
import com.lorran.lupanutri.data.ProfileStore
import com.lorran.lupanutri.data.Saved
import com.lorran.lupanutri.ui.components.CenterMessage
import com.lorran.lupanutri.ui.components.LoadingBox
import com.lorran.lupanutri.ui.components.ProductRow
import com.lorran.lupanutri.ui.theme.Space
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ListsViewModel(private val repo: ProductRepository, profileStore: ProfileStore) : ViewModel() {
    val history: StateFlow<List<Saved>?> = repo.history.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    val favorites: StateFlow<List<Saved>?> = repo.favorites.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    val profile: StateFlow<Profile> = profileStore.profile.stateIn(viewModelScope, SharingStarted.Eagerly, Profile())

    fun removeFromHistory(code: String) = viewModelScope.launch { repo.removeFromHistory(code) }
    fun clearHistory() = viewModelScope.launch { repo.clearHistory() }
    fun unfavorite(s: Saved) = viewModelScope.launch { repo.setFavorite(s.product, false) }
}

@Composable
fun SavedScreen(app: LupaApp, onOpen: (String) -> Unit) {
    val vm: ListsViewModel = viewModel { ListsViewModel(app.repository, app.profileStore) }
    val history by vm.history.collectAsStateWithLifecycle()
    val favorites by vm.favorites.collectAsStateWithLifecycle()
    val profile by vm.profile.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var confirm by remember { mutableStateOf(false) }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Salvos") },
                actions = {
                    if (tab == 0 && !history.isNullOrEmpty()) {
                        IconButton(onClick = { confirm = true }) { Icon(Icons.Outlined.DeleteSweep, "Limpar histórico") }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            PrimaryTabRow(selectedTabIndex = tab, containerColor = MaterialTheme.colorScheme.background) {
                Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Histórico") })
                Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Favoritos") })
            }
            if (tab == 0) {
                SavedList(
                    list = history,
                    goal = profile,
                    empty = { CenterMessage(Icons.Outlined.History, "Nada por aqui ainda", "Os produtos que você escanear ou abrir aparecem aqui, mesmo sem internet.") },
                    onOpen = onOpen,
                    onRemove = { vm.removeFromHistory(it.product.code) },
                    removeLabel = "Tirar do histórico",
                )
            } else {
                SavedList(
                    list = favorites,
                    goal = profile,
                    empty = { CenterMessage(Icons.Outlined.FavoriteBorder, "Sem favoritos", "Toque no coração na tela do produto para guardar os que você compra sempre.") },
                    onOpen = onOpen,
                    onRemove = { vm.unfavorite(it) },
                    removeLabel = "Tirar dos favoritos",
                )
            }
        }
    }

    if (confirm) {
        AlertDialog(
            onDismissRequest = { confirm = false },
            title = { Text("Limpar histórico?") },
            text = { Text("Os favoritos, a lista de compras e os produtos cadastrados por você continuam salvos.") },
            confirmButton = { TextButton(onClick = { confirm = false; vm.clearHistory() }) { Text("Limpar") } },
            dismissButton = { TextButton(onClick = { confirm = false }) { Text("Cancelar") } },
        )
    }
}

@Composable
private fun SavedList(
    list: List<Saved>?,
    goal: Profile,
    empty: @Composable () -> Unit,
    onOpen: (String) -> Unit,
    onRemove: (Saved) -> Unit,
    removeLabel: String,
) {
    Box(Modifier.fillMaxSize()) {
        when {
            list == null -> LoadingBox()
            list.isEmpty() -> empty()
            else -> LazyColumn(
                contentPadding = PaddingValues(Space.lg),
                verticalArrangement = Arrangement.spacedBy(Space.sm),
            ) {
                items(list, key = { it.product.code }) { s ->
                    ProductRow(
                        s.product, goal.goal,
                        onClick = { onOpen(s.product.code) },
                        trailing = {
                            IconButton(onClick = { onRemove(s) }) { Icon(Icons.Outlined.Close, removeLabel) }
                        },
                    )
                }
            }
        }
    }
}
