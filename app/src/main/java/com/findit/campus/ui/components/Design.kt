package com.findit.campus.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.findit.campus.R

@Composable
fun Brand(modifier: Modifier = Modifier, subtitle: String = "Campus lost & found") {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Image(painterResource(R.drawable.cutout_2_d511eb0ee341), null, Modifier.size(38.dp))
        Column {
            Text("FindIt", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
fun Illustration(@DrawableRes resource: Int, modifier: Modifier = Modifier) {
    Image(painterResource(resource), null, modifier, contentScale = ContentScale.Fit)
}

@Composable
fun IllustratedHeading(title: String, body: String, @DrawableRes art: Int, modifier: Modifier = Modifier,
    action: (@Composable ColumnScope.() -> Unit)? = null) {
    FeaturePanel(modifier) {
        BoxWithConstraints {
            val narrow = maxWidth < 310.dp
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Column(Modifier.weight(if (narrow) 2f else 1.7f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(title, style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier.semantics { heading() })
                    Text(body, style = MaterialTheme.typography.bodyMedium,
                        color = LocalContentColor.current.copy(alpha = .78f))
                }
                Illustration(art, Modifier.weight(1f).height(if (narrow) 100.dp else 140.dp))
            }
        }
        action?.invoke(this)
    }
}

@Composable
fun FormCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Surface(modifier.fillMaxWidth().animateContentSize(), shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = .65f))) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(16.dp), content = content)
    }
}

@Composable
fun Notice(text: String, modifier: Modifier = Modifier, error: Boolean = false) {
    Surface(modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium,
        color = if (error) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer) {
        Row(Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            val foreground = if (error) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer
            Icon(if (error) Icons.Outlined.ErrorOutline else Icons.Outlined.Info, null, Modifier.size(20.dp), tint = foreground)
            Text(text, style = MaterialTheme.typography.bodyMedium, color = foreground, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
fun TrustNote(text: String, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(Icons.Outlined.VerifiedUser, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
        Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun Avatar(name: String, modifier: Modifier = Modifier) {
    Surface(modifier.sizeIn(minWidth = 42.dp, minHeight = 42.dp), shape = CircleShape, color = MaterialTheme.colorScheme.tertiaryContainer) {
        Box(contentAlignment = Alignment.Center) {
            Text(name.split(' ').filter { it.isNotBlank() }.take(2).joinToString("") { it.take(1).uppercase() }.ifBlank { "F" },
                style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onTertiaryContainer)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PageTopBar(title: String, onBack: () -> Unit, actions: @Composable RowScope.() -> Unit = {}) {
    TopAppBar(colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background), title = { Text(title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back") } }, actions = actions)
}
