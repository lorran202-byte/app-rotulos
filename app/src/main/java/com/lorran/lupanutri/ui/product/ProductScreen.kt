@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.lorran.lupanutri.ui.product

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.RemoveCircleOutline
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.AddShoppingCart
import androidx.compose.material.icons.outlined.RemoveShoppingCart
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lorran.lupanutri.LupaApp
import com.lorran.lupanutri.data.model.Product
import com.lorran.lupanutri.domain.Additives
import com.lorran.lupanutri.domain.Goal
import com.lorran.lupanutri.domain.Lupa
import com.lorran.lupanutri.domain.Match
import com.lorran.lupanutri.domain.RestrictionHit
import com.lorran.lupanutri.domain.Restrictions
import com.lorran.lupanutri.domain.ScoreResult
import com.lorran.lupanutri.domain.Scorer
import com.lorran.lupanutri.ui.components.CenterMessage
import com.lorran.lupanutri.ui.components.LoadingBox
import com.lorran.lupanutri.ui.components.OfflineMessage
import com.lorran.lupanutri.ui.components.ProductRow
import com.lorran.lupanutri.ui.components.ProductThumb
import com.lorran.lupanutri.ui.components.ScoreRing
import com.lorran.lupanutri.ui.theme.Space
import com.lorran.lupanutri.ui.theme.bandColors
import com.lorran.lupanutri.domain.Band

@Composable
fun ProductScreen(
    app: LupaApp,
    code: String,
    onBack: () -> Unit,
    onOpen: (String) -> Unit,
    onRegister: () -> Unit,
    onEdit: () -> Unit,
) {
    val vm: ProductViewModel = viewModel(key = "product-$code") {
        ProductViewModel(app.repository, app.profileStore, app.shopping, code)
    }
    val ui by vm.ui.collectAsStateWithLifecycle()
    val profile by vm.profile.collectAsStateWithLifecycle()
    val fav by vm.isFavorite.collectAsStateWithLifecycle()
    val inList by vm.inList.collectAsStateWithLifecycle()
    val alts by vm.alts.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val loaded = ui as? ProductUi.Loaded

    Scaffold(
        contentWindowInsets = WindowInsets.navigationBars,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(if (loaded != null) "Produto" else "Consulta") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Voltar") }
                },
                actions = {
                    if (loaded != null) {
                        if (loaded.product.isManual) {
                            IconButton(onClick = onEdit) { Icon(Icons.Outlined.Edit, "Editar cadastro") }
                        }
                        IconButton(onClick = {
                            val r = Scorer.score(loaded.product, profile.goal)
                            val text = buildString {
                                append(loaded.product.name)
                                loaded.product.brand?.let { append(" ($it)") }
                                append(": nota ")
                                append(r.value?.toString() ?: "?")
                                append("/100")
                                r.band?.let { append(" – ${it.label}") }
                                if (r.lupa.isNotEmpty()) append(". ${r.lupa.joinToString { it.label.lowercase() }}")
                                append(". Consultado no app Lupa Nutri.")
                            }
                            val send = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, text)
                            }
                            context.startActivity(Intent.createChooser(send, "Compartilhar"))
                        }) { Icon(Icons.Outlined.Share, "Compartilhar") }
                        IconButton(onClick = vm::toggleList) {
                            Icon(
                                if (inList) Icons.Outlined.RemoveShoppingCart else Icons.Outlined.AddShoppingCart,
                                contentDescription = if (inList) "Tirar da lista de compras" else "Pôr na lista de compras",
                                tint = if (inList) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            )
                        }
                        IconButton(onClick = vm::toggleFavorite) {
                            Icon(
                                if (fav) Icons.Outlined.Favorite else Icons.Outlined.FavoriteBorder,
                                contentDescription = if (fav) "Tirar dos favoritos" else "Salvar nos favoritos",
                                tint = if (fav) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when (val s = ui) {
                ProductUi.Loading -> LoadingBox()
                ProductUi.Offline -> OfflineMessage(onRetry = vm::load)
                ProductUi.NotFound -> CenterMessage(
                    icon = Icons.Outlined.SearchOff,
                    title = "Produto não encontrado",
                    body = "O código $code ainda não está na base aberta de produtos. Cadastre com os dados da embalagem e a nota aparece na hora.",
                    actionLabel = "Cadastrar produto",
                    onAction = onRegister,
                )
                is ProductUi.Loaded -> ProductContent(
                    product = s.product,
                    fromCache = s.fromCache,
                    goal = profile.goal,
                    hits = Restrictions.check(s.product, profile.restrictions),
                    alts = alts,
                    onOpen = onOpen,
                    onComplete = onRegister,
                )
            }
        }
    }
}

@Composable
private fun ProductContent(
    product: Product,
    fromCache: Boolean,
    goal: Goal,
    hits: List<RestrictionHit>,
    alts: AltUi,
    onOpen: (String) -> Unit,
    onComplete: () -> Unit,
) {
    val result = Scorer.score(product, goal)
    LazyColumn(
        contentPadding = PaddingValues(horizontal = Space.lg, vertical = Space.sm),
        verticalArrangement = Arrangement.spacedBy(Space.md),
    ) {
        item { Header(product, fromCache) }
        item { ScoreCard(result, goal, onComplete = onComplete, canComplete = !product.nutrients.isUsable) }
        if (hits.isNotEmpty()) item { RestrictionCard(hits) }
        if (result.lupa.isNotEmpty()) item { LupaRow(result.lupa) }
        if (result.positives.isNotEmpty() || result.negatives.isNotEmpty()) {
            item { ProsCons(result) }
        }
        item { NutritionTable(product) }
        if (!product.ingredientsText.isNullOrBlank()) {
            item {
                Section("Ingredientes") {
                    Text(product.ingredientsText, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        if (product.additives.isNotEmpty()) item { AdditivesCard(product.additives) }
        if (alts != AltUi.Hidden) {
            item {
                Text("Alternativas melhores", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = Space.sm))
            }
            when (alts) {
                AltUi.Loading -> item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(Space.sm))
                        Text("Procurando na mesma categoria…", style = MaterialTheme.typography.bodyMedium)
                    }
                }
                AltUi.Failed -> item { Muted("Não foi possível buscar alternativas agora.") }
                is AltUi.Loaded -> if (alts.items.isEmpty()) {
                    item { Muted("Não encontramos opção com nota bem maior nesta categoria.") }
                } else {
                    items(alts.items, key = { "alt-" + it.code }) { p -> ProductRow(p, goal, onClick = { onOpen(p.code) }) }
                }
                AltUi.Hidden -> Unit
            }
        }
        item {
            Text(
                "Nota calculada pelo app a partir do rótulo, apenas informativa — não substitui orientação de nutricionista. " +
                    "Dados de produtos: Open Food Facts (licença ODbL), preenchidos por voluntários; confira sempre a embalagem.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = Space.lg),
            )
        }
    }
}

@Composable
private fun Header(p: Product, fromCache: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        ProductThumb(p.imageUrl, 88.dp)
        Spacer(Modifier.width(Space.lg))
        Column(Modifier.weight(1f)) {
            Text(p.name, style = MaterialTheme.typography.titleLarge)
            val sub = listOfNotNull(p.brand, p.quantity).joinToString(" · ")
            if (sub.isNotEmpty()) Text(sub, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Código ${p.code}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (p.isManual) Text("Cadastrado por você", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            else if (fromCache) Text("Dados salvos no aparelho (sem conexão)", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.tertiary)
        }
    }
}

@Composable
private fun ScoreCard(r: ScoreResult, goal: Goal, onComplete: () -> Unit, canComplete: Boolean) {
    val c = bandColors(r.band)
    Surface(shape = MaterialTheme.shapes.large, color = c.container) {
        Row(Modifier.fillMaxWidth().padding(Space.lg), verticalAlignment = Alignment.CenterVertically) {
            ScoreRing(r.value, r.band, 112.dp, stroke = 10.dp)
            Spacer(Modifier.width(Space.lg))
            Column(Modifier.weight(1f)) {
                if (r.value != null) {
                    Text(r.band?.label ?: "", style = MaterialTheme.typography.headlineSmall, color = c.onContainer)
                    Text("Nota de 0 a 100", style = MaterialTheme.typography.bodySmall, color = c.onContainer)
                    Spacer(Modifier.height(Space.sm))
                    Text("Objetivo: ${goal.label}", style = MaterialTheme.typography.labelLarge, color = c.onContainer)
                } else {
                    Text("Sem nota", style = MaterialTheme.typography.titleLarge, color = c.onContainer)
                    Text(
                        "Faltam dados da tabela nutricional (calorias, açúcares, gordura saturada e sódio).",
                        style = MaterialTheme.typography.bodySmall, color = c.onContainer,
                    )
                    if (canComplete) {
                        Spacer(Modifier.height(Space.sm))
                        OutlinedButton(onClick = onComplete) { Text("Completar dados") }
                    }
                }
            }
        }
    }
}

@Composable
private fun RestrictionCard(hits: List<RestrictionHit>) {
    val bad = hits.filter { it.match == Match.CONTEM }
    val maybe = hits.filter { it.match == Match.PODE_CONTER }
    val unknown = hits.filter { it.match == Match.DESCONHECIDO }
    val ok = hits.filter { it.match == Match.NAO_CONTEM }
    Section("Suas restrições") {
        bad.forEach { HitLine(Icons.Outlined.ErrorOutline, MaterialTheme.colorScheme.error, "Contém ${it.restriction.warning}") }
        maybe.forEach { HitLine(Icons.Outlined.WarningAmber, bandColors(Band.RUIM).main, "Pode conter ${it.restriction.warning}") }
        unknown.forEach { HitLine(Icons.Outlined.HelpOutline, MaterialTheme.colorScheme.onSurfaceVariant, "Não deu para verificar: ${it.restriction.label.lowercase()}") }
        if (ok.isNotEmpty()) {
            Spacer(Modifier.height(Space.xs))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Space.sm), verticalArrangement = Arrangement.spacedBy(Space.sm)) {
                ok.forEach {
                    val g = bandColors(Band.OTIMO)
                    Row(
                        Modifier.clip(RoundedCornerShape(50)).background(g.container).padding(horizontal = Space.md, vertical = Space.xs),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Outlined.CheckCircle, null, tint = g.onContainer, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(Space.xs))
                        Text(it.restriction.label, style = MaterialTheme.typography.labelMedium, color = g.onContainer)
                    }
                }
            }
        }
        Spacer(Modifier.height(Space.sm))
        Muted("Verificação automática pela lista de ingredientes. Em caso de alergia, confira sempre a embalagem.")
    }
}

@Composable
private fun HitLine(icon: ImageVector, tint: Color, text: String) {
    Row(Modifier.padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(Space.sm))
        Text(text, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun LupaRow(lupa: List<Lupa>) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(Space.sm), verticalArrangement = Arrangement.spacedBy(Space.sm)) {
        lupa.forEach { l ->
            Row(
                Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF111111))
                    .padding(horizontal = Space.md, vertical = Space.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Outlined.Search, null, tint = Color.White, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(Space.xs))
                Text(l.label.uppercase(), color = Color.White, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
private fun ProsCons(r: ScoreResult) {
    Section("Pontos do produto") {
        r.negatives.forEach { HitLine(Icons.Outlined.RemoveCircleOutline, bandColors(Band.EVITE).main, it) }
        r.positives.forEach { HitLine(Icons.Outlined.AddCircleOutline, bandColors(Band.OTIMO).main, it) }
    }
}

@Composable
private fun NutritionTable(p: Product) {
    val n = p.nutrients
    val rows = listOf(
        "Valor energético" to n.kcal?.let { "${Scorer.fmt(it)} kcal" },
        "Carboidratos" to n.carbs?.let { "${Scorer.fmt(it)} g" },
        "Açúcares totais" to n.sugars?.let { "${Scorer.fmt(it)} g" },
        "Açúcares adicionados" to n.addedSugars?.let { "${Scorer.fmt(it)} g" },
        "Proteínas" to n.protein?.let { "${Scorer.fmt(it)} g" },
        "Gorduras totais" to n.fat?.let { "${Scorer.fmt(it)} g" },
        "Gorduras saturadas" to n.satFat?.let { "${Scorer.fmt(it)} g" },
        "Gorduras trans" to n.transFat?.let { "${Scorer.fmt(it)} g" },
        "Fibras" to n.fiber?.let { "${Scorer.fmt(it)} g" },
        "Sódio" to n.sodiumMg?.let { "${Scorer.fmt(it)} mg" },
    )
    Section("Tabela nutricional (por ${p.unitLabel})") {
        rows.forEachIndexed { i, (label, value) ->
            if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Row(Modifier.fillMaxWidth().padding(vertical = Space.sm)) {
                Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                Text(
                    value ?: "—",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (value == null) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

@Composable
private fun AdditivesCard(tags: List<String>) {
    Section("Aditivos (${tags.size})") {
        tags.forEach { t ->
            val attention = Additives.isAttention(t)
            Row(Modifier.padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(8.dp).clip(RoundedCornerShape(50))
                        .background(if (attention) bandColors(Band.RUIM).main else MaterialTheme.colorScheme.outline),
                )
                Spacer(Modifier.width(Space.sm))
                Text(Additives.label(t), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                if (attention) Text("atenção", style = MaterialTheme.typography.labelMedium, color = bandColors(Band.RUIM).main)
            }
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(Space.lg)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(Space.sm))
            content()
        }
    }
}

@Composable
private fun Muted(text: String) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}
