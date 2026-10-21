@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.lorran.lupanutri.ui.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lorran.lupanutri.LupaApp
import com.lorran.lupanutri.data.Profile
import com.lorran.lupanutri.data.ProfileStore
import com.lorran.lupanutri.domain.Goal
import com.lorran.lupanutri.domain.Restriction
import com.lorran.lupanutri.ui.theme.Space
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ProfileViewModel(private val store: ProfileStore) : ViewModel() {
    val profile: StateFlow<Profile> = store.profile.stateIn(viewModelScope, SharingStarted.Eagerly, Profile())
    fun setGoal(g: Goal) = viewModelScope.launch { store.setGoal(g) }
    fun toggle(r: Restriction) = viewModelScope.launch { store.toggle(r) }
    fun setOff(user: String, pass: String) = viewModelScope.launch { store.setOffAccount(user, pass) }
}

@Composable
fun ProfileScreen(app: LupaApp) {
    val vm: ProfileViewModel = viewModel { ProfileViewModel(app.profileStore) }
    val profile by vm.profile.collectAsStateWithLifecycle()

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Meu perfil") },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Space.lg),
        ) {
            Text("Objetivo", style = MaterialTheme.typography.titleMedium)
            Text(
                "Muda o peso de cada nutriente na nota.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(Space.sm))
            Goal.entries.forEach { g ->
                val selected = profile.goal == g
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                    border = if (selected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = Space.xs)
                        .selectable(selected = selected, role = Role.RadioButton, onClick = { vm.setGoal(g) }),
                ) {
                    Row(Modifier.padding(Space.md), verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = selected, onClick = null)
                        Spacer(Modifier.padding(horizontal = Space.xs))
                        Column {
                            Text(g.label, style = MaterialTheme.typography.titleSmall)
                            Text(g.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            Spacer(Modifier.height(Space.xl))
            Text("Restrições e preferências", style = MaterialTheme.typography.titleMedium)
            Text(
                "Avisamos na tela do produto quando ele não combina com o que você marcou.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(Space.sm))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
                Restriction.entries.forEach { r ->
                    val on = r in profile.restrictions
                    FilterChip(
                        selected = on,
                        onClick = { vm.toggle(r) },
                        label = { Text(r.label) },
                        leadingIcon = if (on) {
                            { Icon(Icons.Outlined.Check, null) }
                        } else null,
                    )
                }
            }

            Spacer(Modifier.height(Space.xl))
            OffAccount(profile, onSave = { u, p -> vm.setOff(u, p) })

            Spacer(Modifier.height(Space.xl))
            Text("Sobre a nota", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(Space.sm))
            Text(
                "A nota vai de 0 a 100 e é calculada no próprio aparelho a partir da tabela nutricional (por 100 g ou 100 ml): " +
                    "perde pontos com calorias, açúcares, gordura saturada, sódio, gordura trans, ultraprocessamento, aditivos e adoçantes, " +
                    "e ganha com fibras, proteína e alimentos in natura. Os alertas de lupa seguem os limites da rotulagem brasileira.\n\n" +
                    "Faixas: 75–100 Ótimo · 55–74 Bom · 30–54 Ruim · 0–29 Evite.\n\n" +
                    "Quando a ficha do produto não traz todos os dados, a nota aparece como estimada; use \"Completar dados\" para corrigir.\n\n" +
                    "É uma ferramenta informativa e não substitui a orientação de um nutricionista ou médico. " +
                    "Os dados dos produtos vêm do Open Food Facts (licença ODbL), uma base aberta mantida por voluntários — " +
                    "em caso de alergia, confira sempre a embalagem.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(Space.xxl))
        }
    }
}


@Composable
private fun OffAccount(profile: Profile, onSave: (String, String) -> Unit) {
    var user by remember(profile.offUser) { mutableStateOf(profile.offUser) }
    var pass by remember(profile.offPassword) { mutableStateOf(profile.offPassword) }
    val changed = user != profile.offUser || pass != profile.offPassword
    Text("Conta do Open Food Facts (opcional)", style = MaterialTheme.typography.titleMedium)
    Text(
        "Com ela, os produtos que você cadastrar ou corrigir vão também para a base aberta e ficam certos para todo mundo. " +
            "Crie grátis em world.openfoodfacts.org. A senha fica guardada só neste celular.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(Space.sm))
    OutlinedTextField(
        value = user, onValueChange = { user = it.trim() }, label = { Text("Usuário") },
        singleLine = true, modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.height(Space.sm))
    OutlinedTextField(
        value = pass, onValueChange = { pass = it }, label = { Text("Senha") },
        singleLine = true, modifier = Modifier.fillMaxWidth(),
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
    )
    Spacer(Modifier.height(Space.sm))
    Row(verticalAlignment = Alignment.CenterVertically) {
        Button(onClick = { onSave(user, pass) }, enabled = changed) { Text("Salvar conta") }
        Spacer(Modifier.width(Space.md))
        if (!changed && profile.hasOffAccount) {
            Text("Conta salva", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        }
    }
}
