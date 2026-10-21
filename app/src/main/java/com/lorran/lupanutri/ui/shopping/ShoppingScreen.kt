@file:OptIn(ExperimentalMaterial3Api::class)

package com.lorran.lupanutri.ui.shopping

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lorran.lupanutri.LupaApp
import com.lorran.lupanutri.data.ListEntry
import com.lorran.lupanutri.data.Profile
import com.lorran.lupanutri.data.ProfileStore
import com.lorran.lupanutri.data.ShoppingRepository
import com.lorran.lupanutri.domain.Goal
import com.lorran.lupanutri.domain.Scorer
import com.lorran.lupanutri.ui.components.CenterMessage
import com.lorran.lupanutri.ui.components.LoadingBox
import com.lorran.lupanutri.ui.components.ScoreRing
import com.lorran.lupanutri.ui.theme.Space
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ShoppingViewModel(private val repo: ShoppingRepository, profileStore: ProfileStore) : ViewModel() {
    val entries: StateFlow<List<ListEntry>?> = repo.entries.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    val profile: StateFlow<Profile> = profileStore.profile.stateIn(viewModelScope, SharingStarted.Eagerly, Profile())

    fun add(text: String) = viewModelScope.launch { repo.addText(text) }
    fun toggle(e: ListEntry) = viewModelScope.launch { repo.setChecked(e.item.id, !e.item.checked) }
    fun delete(e: ListEntry) = viewModelScope.launch { repo.delete(e.item.id) }
    fun clearChecked() = viewModelScope.launch { repo.clearChecked() }
    suspend fun addFavorites(): Int = repo.addFavorites()
}

@Composable
fun ShoppingScreen(app: LupaApp, onOpen: (String) -> Unit) {
    val vm: ShoppingViewModel = viewModel { ShoppingViewModel(app.shopping, app.profileStore) }
    val entries by vm.entries.collectAsStateWithLifecycle()
    val profile by vm.profile.collectAsStateWithLifecycle()
    var input by rememberSaveable { mutableStateOf("") }
    var menu by remember { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    fun submit() {
        if (input.isNotBlank()) { vm.add(input); input = "" }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text("Lista de compras") },
                actions = {
                    val open = entries.orEmpty().filter { !it.item.checked }
                    if (open.isNotEmpty()) {
                        IconButton(onClick = {
                            val text = "Lista de compras\n" + open.joinToString("\n") { "• " + it.item.text }
                            val send = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, text)
                            }
                            context.startActivity(Intent.createChooser(send, "Compartilhar lista"))
                        }) { Icon(Icons.Outlined.Share, "Compartilhar lista") }
                    }
                    Box {
                        IconButton(onClick = { menu = true }) { Icon(Icons.Outlined.MoreVert, "Mais opções") }
                        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                            DropdownMenuItem(
                                text = { Text("Adicionar meus favoritos") },
                                onClick = {
                                    menu = false
                                    scope.launch {
                                        val n = vm.addFavorites()
                                        snackbar.showSnackbar(
                                            when (n) {
                                                0 -> "Nenhum favorito novo para adicionar"
                                                1 -> "1 favorito adicionado"
                                                else -> "$n favoritos adicionados"
                                            }
                                        )
                                    }
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("Apagar itens comprados") },
                                enabled = entries.orEmpty().any { it.item.checked },
                                onClick = { menu = false; vm.clearChecked() },
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = Space.lg),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it.take(80) },
                    placeholder = { Text("Adicionar item (ex.: banana, ovos)") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    shape = MaterialTheme.shapes.medium,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { submit() }),
                )
                Spacer(Modifier.width(Space.sm))
                FilledIconButton(onClick = { submit() }, enabled = input.isNotBlank()) {
                    Icon(Icons.Outlined.Add, "Adicionar")
                }
            }
            Box(Modifier.fillMaxSize()) {
                val list = entries
                when {
                    list == null -> LoadingBox()
                    list.isEmpty() -> CenterMessage(
                        icon = Icons.Outlined.ShoppingCart,
                        title = "Lista vazia",
                        body = "Escreva um item acima ou toque no carrinho na tela de um produto. Os favoritos entram de uma vez pelo menu ⋮.",
                    )
                    else -> {
                        val open = list.filter { !it.item.checked }
                        val done = list.filter { it.item.checked }
                        LazyColumn(
                            contentPadding = PaddingValues(Space.lg),
                            verticalArrangement = Arrangement.spacedBy(Space.sm),
                        ) {
                            items(open, key = { it.item.id }) { e ->
                                EntryRow(e, profile.goal, onToggle = { vm.toggle(e) }, onDelete = { vm.delete(e) }, onOpen = onOpen)
                            }
                            if (done.isNotEmpty()) {
                                item(key = "header-done") {
                                    Text(
                                        "No carrinho (${done.size})",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(top = Space.md),
                                    )
                                }
                                items(done, key = { it.item.id }) { e ->
                                    EntryRow(e, profile.goal, onToggle = { vm.toggle(e) }, onDelete = { vm.delete(e) }, onOpen = onOpen)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EntryRow(e: ListEntry, goal: Goal, onToggle: () -> Unit, onDelete: () -> Unit, onOpen: (String) -> Unit) {
    val checked = e.item.checked
    val product = e.product
    Row(
        Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surface)
            .clickable { if (product != null) onOpen(product.code) else onToggle() }
            .padding(start = Space.xs, end = Space.xs, top = Space.xs, bottom = Space.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = checked, onCheckedChange = { onToggle() })
        Column(Modifier.weight(1f)) {
            Text(
                e.item.text,
                style = MaterialTheme.typography.bodyLarge,
                textDecoration = if (checked) TextDecoration.LineThrough else null,
                color = if (checked) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (product != null) {
                val r = Scorer.score(product, goal)
                Text(
                    "Nota ${r.value ?: "?"} · ${r.band?.label ?: "sem nota"} · toque para ver",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (product != null) {
            val r = Scorer.score(product, goal)
            ScoreRing(r.value, r.band, 36.dp, stroke = 4.dp)
        }
        IconButton(onClick = onDelete) { Icon(Icons.Outlined.Close, "Tirar da lista") }
    }
}
