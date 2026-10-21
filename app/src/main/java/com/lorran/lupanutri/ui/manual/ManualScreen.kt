@file:OptIn(ExperimentalMaterial3Api::class)

package com.lorran.lupanutri.ui.manual

import android.content.Context
import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.Checkbox
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import java.io.File
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import com.lorran.lupanutri.data.OcrReader
import com.lorran.lupanutri.data.model.Nutrients
import com.lorran.lupanutri.data.model.Product
import com.lorran.lupanutri.domain.LabelField
import com.lorran.lupanutri.domain.LabelParser
import com.lorran.lupanutri.domain.Scorer
import com.lorran.lupanutri.ui.components.LoadingBox
import com.lorran.lupanutri.ui.theme.Space
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
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

class ManualViewModel(private val app: LupaApp, private val code: String) : ViewModel() {
    private val repo = app.repository
    val form = ManualForm()
    val ready = MutableStateFlow(false)
    var base: Product? = null
        private set
    var reading by mutableStateOf(false)
        private set
    var ocrMessage by mutableStateOf<String?>(null)
        private set
    var hasOffAccount by mutableStateOf(false)
        private set
    var sendToOff by mutableStateOf(true)

    init {
        viewModelScope.launch {
            base = repo.cachedRaw(code)
            base?.let { form.fill(it) }
            hasOffAccount = app.profileStore.profile.first().hasOffAccount
            ready.value = true
        }
    }

    /** Lê a foto da embalagem e preenche o que encontrar (tabela e/ou ingredientes). */
    fun readPhoto(context: Context, uri: Uri) {
        reading = true
        ocrMessage = null
        viewModelScope.launch {
            ocrMessage = try {
                val lines = OcrReader.read(context, uri)
                val label = LabelParser.parse(lines)
                val ingredients = LabelParser.ingredients(lines)
                val f = form
                for ((field, v) in label.values) {
                    val txt = Scorer.fmt(v)
                    when (field) {
                        LabelField.KCAL -> f.kcal = txt
                        LabelField.CARBS -> f.carbs = txt
                        LabelField.SUGARS -> f.sugars = txt
                        LabelField.ADDED_SUGARS -> f.addedSugars = txt
                        LabelField.PROTEIN -> f.protein = txt
                        LabelField.FAT -> f.fat = txt
                        LabelField.SAT_FAT -> f.satFat = txt
                        LabelField.TRANS_FAT -> f.transFat = txt
                        LabelField.FIBER -> f.fiber = txt
                        LabelField.SODIUM -> f.sodium = txt
                    }
                }
                if (label.isLiquid) f.liquid = true
                if (ingredients != null) f.ingredients = ingredients
                val n = label.values.size
                when {
                    n == 0 && ingredients == null ->
                        "Não consegui ler a tabela nem os ingredientes. Tente de novo com o rótulo reto, de perto e com boa luz."
                    else -> buildString {
                        if (n > 0) append("Li $n valor${if (n > 1) "es" else ""} da tabela")
                        if (label.convertedFromPortion) append(" (estavam por porção; converti para 100 g)")
                        if (ingredients != null) append(if (n > 0) " e a lista de ingredientes" else "Li a lista de ingredientes")
                        append(". ")
                        if (label.maybePerPortion) append("Não achei a coluna de 100 g: confira se os números não estão por porção. ")
                        append("Confira tudo com a embalagem antes de salvar.")
                    }
                }
            } catch (e: Exception) {
                "Não deu para ler a foto. Na primeira vez o celular precisa de internet para baixar o leitor de texto; tente de novo em instantes."
            }
            reading = false
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
        val upload = hasOffAccount && sendToOff
        viewModelScope.launch {
            repo.save(p, viewed = true)
            if (upload) {
                val ctx = app.applicationContext
                app.appScope.launch {
                    val profile = app.profileStore.profile.first()
                    val err = app.api.upload(p, profile.offUser, profile.offPassword)
                    Toast.makeText(
                        ctx,
                        if (err == null) "Enviado ao Open Food Facts. Obrigado!" else "Não foi enviado ao Open Food Facts: $err",
                        Toast.LENGTH_LONG,
                    ).show()
                }
            }
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
fun ManualScreen(app: LupaApp, code: String, onBack: () -> Unit, onSaved: () -> Unit, onDeleted: () -> Unit) {
    val vm: ManualViewModel = viewModel(key = "manual-$code") { ManualViewModel(app, code) }
    val ready by vm.ready.collectAsStateWithLifecycle()
    val f = vm.form
    val isEdit = vm.base?.isManual == true
    val context = LocalContext.current
    var photoUri by rememberSaveable { mutableStateOf<String?>(null) }
    val takePicture = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        if (ok) photoUri?.let { vm.readPhoto(context, Uri.parse(it)) }
    }
    fun launchCamera() {
        val dir = File(context.cacheDir, "fotos").apply { mkdirs() }
        dir.listFiles()?.forEach { it.delete() }
        val file = File(dir, "rotulo-${System.currentTimeMillis()}.jpg")
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fotos", file)
        photoUri = uri.toString()
        runCatching { takePicture.launch(uri) }
    }
    val cameraPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) launchCamera()
    }
    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) vm.readPhoto(context, uri)
    }

    Scaffold(
        contentWindowInsets = WindowInsets.navigationBars,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(if (isEdit) "Editar produto" else "Cadastrar produto") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Voltar") } },
                actions = {
                    if (isEdit) IconButton(onClick = { vm.delete(onDeleted) }) { Icon(Icons.Outlined.Delete, "Apagar cadastro") }
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
                "Código $code. Use os valores por 100 g (ou 100 ml) da embalagem.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.secondaryContainer, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(Space.md), verticalArrangement = Arrangement.spacedBy(Space.sm)) {
                    Text("Preencher pela foto", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSecondaryContainer)
                    Text(
                        "Fotografe a tabela nutricional — e, se quiser, a lista de ingredientes. A leitura é feita no próprio celular.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
                        Button(
                            enabled = !vm.reading,
                            onClick = {
                                val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
                                if (granted) launchCamera() else cameraPermission.launch(Manifest.permission.CAMERA)
                            },
                        ) {
                            Icon(Icons.Outlined.PhotoCamera, null)
                            Spacer(Modifier.width(Space.sm))
                            Text("Tirar foto")
                        }
                        OutlinedButton(
                            enabled = !vm.reading,
                            onClick = { pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                        ) {
                            Icon(Icons.Outlined.PhotoLibrary, null)
                            Spacer(Modifier.width(Space.sm))
                            Text("Da galeria")
                        }
                    }
                    if (vm.reading) LinearProgressIndicator(Modifier.fillMaxWidth())
                    vm.ocrMessage?.let {
                        Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSecondaryContainer)
                    }
                }
            }
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
            if (vm.hasOffAccount) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = vm.sendToOff, onCheckedChange = { vm.sendToOff = it })
                    Text(
                        "Enviar também para o Open Food Facts (a ficha fica certa para todo mundo)",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                    )
                }
            } else {
                Text(
                    "O cadastro fica salvo neste aparelho. Para enviar também ao Open Food Facts, entre com sua conta em Perfil.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
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
