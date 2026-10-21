package com.lorran.lupanutri.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.ShoppingBasket
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.lorran.lupanutri.data.model.Product
import com.lorran.lupanutri.domain.Band
import com.lorran.lupanutri.domain.Goal
import com.lorran.lupanutri.domain.Scorer
import com.lorran.lupanutri.ui.theme.Space
import com.lorran.lupanutri.ui.theme.bandColors

/** Anel com a nota no centro. */
@Composable
fun ScoreRing(score: Int?, band: Band?, size: Dp, stroke: Dp = 8.dp) {
    val c = bandColors(band)
    val track = MaterialTheme.colorScheme.outlineVariant
    Box(Modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val st = Stroke(width = stroke.toPx(), cap = StrokeCap.Round)
            val inset = stroke.toPx() / 2
            val arcSize = androidx.compose.ui.geometry.Size(this.size.width - inset * 2, this.size.height - inset * 2)
            val topLeft = androidx.compose.ui.geometry.Offset(inset, inset)
            drawArc(track, -90f, 360f, false, topLeft, arcSize, style = st)
            if (score != null) drawArc(c.main, -90f, 360f * score / 100f, false, topLeft, arcSize, style = st)
        }
        Text(
            text = score?.toString() ?: "?",
            fontWeight = FontWeight.Black,
            fontSize = (size.value * 0.34f).sp,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
fun BandChip(band: Band?, estimated: Boolean = false) {
    val c = bandColors(band)
    Text(
        text = (band?.label ?: "Sem nota") + if (band != null && estimated) " · estimada" else "",
        style = MaterialTheme.typography.labelLarge,
        color = c.onContainer,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(c.container)
            .padding(horizontal = Space.md, vertical = Space.xs),
    )
}

@Composable
fun ProductThumb(url: String?, size: Dp) {
    Box(
        Modifier
            .size(size)
            .clip(MaterialTheme.shapes.small)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Outlined.ShoppingBasket, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        if (url != null) {
            AsyncImage(
                model = url,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

/** Linha de produto usada no histórico, favoritos, busca e alternativas. */
@Composable
fun ProductRow(
    product: Product,
    goal: Goal,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailing: @Composable (() -> Unit)? = null,
) {
    val result = Scorer.score(product, goal)
    Row(
        modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(Space.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ProductThumb(product.imageUrl, 56.dp)
        Spacer(Modifier.width(Space.md))
        Column(Modifier.weight(1f)) {
            Text(product.name, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
            val sub = listOfNotNull(product.brand, product.quantity).joinToString(" · ")
            if (sub.isNotEmpty()) {
                Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.height(Space.xs))
            BandChip(result.band, result.estimated)
        }
        Spacer(Modifier.width(Space.sm))
        ScoreRing(result.value, result.band, 48.dp, stroke = 5.dp)
        trailing?.invoke()
    }
}

@Composable
fun CenterMessage(
    icon: ImageVector,
    title: String,
    body: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier.fillMaxSize().padding(Space.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            Modifier.size(72.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(36.dp))
        }
        Spacer(Modifier.height(Space.lg))
        Text(title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        Spacer(Modifier.height(Space.sm))
        Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(Space.lg))
            Button(onClick = onAction) { Text(actionLabel) }
        }
    }
}

@Composable
fun LoadingBox(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
}

@Composable
fun OfflineMessage(onRetry: () -> Unit) = CenterMessage(
    icon = Icons.Outlined.CloudOff,
    title = "Sem conexão",
    body = "Não foi possível consultar a base de produtos. Confira a internet e tente de novo.",
    actionLabel = "Tentar de novo",
    onAction = onRetry,
)
