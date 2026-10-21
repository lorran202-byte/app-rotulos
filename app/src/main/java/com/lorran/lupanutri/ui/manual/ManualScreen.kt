@file:OptIn(ExperimentalMaterial3Api::class)

package com.lorran.lupanutri.ui.manual

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lorran.lupanutri.LupaApp
import com.lorran.lupanutri.data.ProductRepository
import com.lorran.lupanutri.data.model.Nutrients
import com.lorran.lupanutri.data.model.Product
import com.lorran.lupanutri.domain.Scorer
import com.lorran.lupanutri.ui.components.LoadingBox
import com.lorran.lupanutri.ui.theme.Space
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class ManualForm {
    var name by mutableStateOf("")
    var brand by mutableStateOf("")
    var quantity by mutableStateOf("")
    var liquid by mutableStateOf(false)
    var kcal by mutableStateOf("")
    var carbs by mutableStateOf("")
    var sugars by mutableStateOf("")
    var addedSugars by mutableStateOf("")
    var protein by mutableStateOf("")
    var fat by mutableStateOf("")
    var satFat by mutableStateOf("")
    var transFat by mutableStateOf("")
    var fiber by mutableStateOf("")
    var sodium by mutableStateOf("")
    var ingredients by mutableStateOf("")
    var tried by mutableStateOf(false)

    fun fill(p: Product) {
        name = if (p.name == "Produto sem nome") "" else p.name
        brand = p.brand.orEmpty()
        quantity = p.quantity.orEmpty()
        liquid = p.isLiquid
        val n = p.nutrients
        kcal = n.kcal.str(); carbs = n.carbs.str(); sugars = n.sugars.str(); addedSugars = n.addedSugars.str()
        protein = n.protein.str(); fat = n.fat.str(); satFat = n.satFat.str(); transFat = n.transFat.str()
        fiber = n.fiber.str(); sodium = n.sodiumMg.str()
        ingredients = p.ingredientsText.orEmpty()
    }

    private fun Double?.str() = this?.let { Scorer.fmt(it) }.orEmpty()

    val valid: Boolean
        get() = name.isNotBlank() && kcal.num() != null && sugars.num() != null && satFat.num() != null && sodium.num() != null
}

fun String.num(): Double? = trim().replace(',', '.').toDoubleOrNull()?.takeIf { it >= 0 }

class ManualViewModel(private val repo: ProductRepository, private val code: String) : ViewModel() {
    val form = ManualForm()
    val ready = MutableStateFlow(false)
    var base: Product? = null
        private set

    init {
        viewModelScope.launch {
            base = repo.cached(code)
            base?.let { form.fill(it) }
            ready.value = true
        }
    }

    fun save(onDone: () -> Unit) {
        form.tried = true
        if (!form.valid) return
        val f = form
        val ingredients = f.ingredients.trim().ifBlank { null }
        val detected = ingredients?.let { txt ->
            Regex("""\b(?:ins|e)\s?-?(\d{3,4}[a-z]?)\b""", RegexOption.IGNORE_CASE).findAll(txt)
                .map { "en:e" + it.groupValues[1].lowercase() }.distinct().toList()
        }.orEmpty()
        val old = base
        val p = Product(
            code = code,
            name = f.name.trim(),
            brand = f.brand.trim().ifBlank { null },
            imageUrl = old?.imageUrl,
            quantity = f.quantity.trim().ifBlank { null },
            ingredientsText = ingredients,
            nutrients = Nutrients(
                kcal = f.kcal.num(), carbs = f.carbs.num(), sugars = f.sugars.num(), addedSugars = f.addedSugars.num(),
                protein = f.protein.num(), fat = f.fat.num(), satFat = f.satFat.num(), transFat = f.transFat.num(),
                fiber = f.fiber.num(), sodiumMg = f.sodium.num(),
            ),
            additives = (old?.additives.orEmpty() + detected).distinct(),
            allergens = old?.allergens.orEmpty(),
            traces = old?.traces.orEmpty(),
            analysis = old?.analysis.orEmpty(),
            nova = old?.nova,
            categories = old?.categories.orEmpty(),
            isLiquidOverride = f.liquid,
            isManual = true,
        )
        viewModelScope.launch {
            repo.save(p, viewed = true)
            onDone()
        }
    }

    fun delete(onDone: () -> Unit) {
        viewModelScope.launch {
            repo.deleteManual(code)
            onDone()
        }
    }
}

@Composable
fun ManualScreen(app: LupaApp, code: String, onBack: () -> Unit, onSaved: () -> Unit) {
    val vm: ManualViewModel = viewModel(key = "manual-$code") { ManualViewModel(app.repository, code) }
    val ready by vm.ready.collectAsStateWithLifecycle()
    val f = vm.form
    val isEdit = vm.base?.isManual == true

    Scaffold(
        contentWindowInsets = WindowInsets.navigationBars,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(if (isEdit) "Editar produto" else "Cadastrar produto") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Voltar") } },
                actions = {
                    if (isEdit) IconButton(onClick = { vm.delete(onBack) }) { Icon(Icons.Outlined.Delete, "Apagar cadastro") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        if (!ready) {
            LoadingBox(Modifier.padding(padding))
            return@Scaffold
        }
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Space.lg),
            verticalArrangement = Arrangement.spacedBy(Space.sm),
        ) {
            Text(
                "Código $code. Copie da embalagem os valores por 100 g (ou 100 ml). O cadastro fica salvo só neste aparelho.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TextIn("Nome do produto *", f.name, f.tried && f.name.isBlank(), cap = true) { f.name = it }
            TextIn("Marca", f.brand, cap = true) { f.brand = it }
            TextIn("Quantidade (ex.: 200 g, 1 L)", f.quantity) { f.quantity = it }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("É bebida (valores por 100 ml)", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                Switch(checked = f.liquid, onCheckedChange = { f.liquid = it })
            }

            Spacer(Modifier.height(Space.sm))
            Text("Tabela nutricional", style = MaterialTheme.typography.titleMedium)
            Text(
                "Muitas tabelas mostram por porção: divida pelo tamanho da porção e multiplique por 100. Os campos com * são obrigatórios.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            NumRow("Calorias (kcal) *", f.kcal, f.tried && f.kcal.num() == null, { f.kcal = it }, "Carboidratos (g)", f.carbs, false, { f.carbs = it })
            NumRow("Açúcares totais (g) *", f.sugars, f.tried && f.sugars.num() == null, { f.sugars = it }, "Açúcares adic. (g)", f.addedSugars, false, { f.addedSugars = it })
            NumRow("Proteínas (g)", f.protein, false, { f.protein = it }, "Gorduras totais (g)", f.fat, false, { f.fat = it })
            NumRow("Gord. saturadas (g) *", f.satFat, f.tried && f.satFat.num() == null, { f.satFat = it }, "Gord. trans (g)", f.transFat, false, { f.transFat = it })
            NumRow("Fibras (g)", f.fiber, false, { f.fiber = it }, "Sódio (mg) *", f.sodium, f.tried && f.sodium.num() == null, { f.sodium = it })

            Spacer(Modifier.height(Space.sm))
            OutlinedTextField(
                value = f.ingredients,
                onValueChange = { f.ingredients = it },
                label = { Text("Ingredientes (copie da embalagem)") },
                minLines = 3,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            )
            if (f.tried && !f.valid) {
                Text("Preencha os campos com *.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }
            Button(onClick = { vm.save(onSaved) }, modifier = Modifier.fillMaxWidth().padding(vertical = Space.lg)) {
                Text("Salvar e ver a nota")
            }
        }
    }
}

@Composable
private fun TextIn(label: String, value: String, error: Boolean = false, cap: Boolean = false, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = true,
        isError = error,
        modifier = Modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(capitalization = if (cap) KeyboardCapitalization.Words else KeyboardCapitalization.None),
    )
}

@Composable
private fun NumRow(
    l1: String, v1: String, e1: Boolean, c1: (String) -> Unit,
    l2: String, v2: String, e2: Boolean, c2: (String) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
        NumIn(l1, v1, e1, c1, Modifier.weight(1f))
        NumIn(l2, v2, e2, c2, Modifier.weight(1f))
    }
}

@Composable
private fun NumIn(label: String, value: String, error: Boolean, onChange: (String) -> Unit, modifier: Modifier) {
    OutlinedTextField(
        value = value,
        onValueChange = { v -> onChange(v.filter { it.isDigit() || it == ',' || it == '.' }.take(8)) },
        label = { Text(label, maxLines = 1) },
        singleLine = true,
        isError = error,
        modifier = modifier,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
    )
}
