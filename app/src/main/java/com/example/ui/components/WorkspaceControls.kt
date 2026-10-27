package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.ui.theme.LocalLifeOsColors

/** Shared geometry and native interactions. Wrappers never change callbacks or data state. */
@Composable
private fun Modifier.controlPress(source: MutableInteractionSource, shape: Shape): Modifier {
    val pressed by source.collectIsPressedAsState()
    val focused by source.collectIsFocusedAsState()
    val reduced = LocalReducedMotion.current
    val colors = LocalLifeOsColors.current
    val scale by animateFloatAsState(if (pressed) 0.978f else 1f,
        if (reduced) snap() else spring(dampingRatio = 0.78f, stiffness = 900f), label = "control_press")
    return this.graphicsLayer { scaleX = scale; scaleY = scale }
        .then(if (focused) Modifier.border(2.dp, colors.textPrimary, shape) else Modifier)
}

@Composable
fun WorkspaceButton(
    onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true,
    shape: Shape = RoundedCornerShape(18.dp),
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    elevation: ButtonElevation? = null,
    border: BorderStroke? = BorderStroke(1.dp, Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.26f), Color.Transparent))),
    contentPadding: PaddingValues = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
    interactionSource: MutableInteractionSource? = null, content: @Composable RowScope.() -> Unit
) {
    val source = interactionSource ?: remember { MutableInteractionSource() }
    Button(onClick, modifier.heightIn(min = 48.dp).controlPress(source, shape), enabled, shape,
        colors, elevation, border, contentPadding, source, content)
}

@Composable
fun WorkspaceTonalButton(
    onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true,
    shape: Shape = RoundedCornerShape(16.dp),
    colors: ButtonColors = ButtonDefaults.filledTonalButtonColors(
        containerColor = LocalLifeOsColors.current.surfaceGlassHighlight,
        contentColor = LocalLifeOsColors.current.textPrimary),
    elevation: ButtonElevation? = null, border: BorderStroke? = null,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
    interactionSource: MutableInteractionSource? = null, content: @Composable RowScope.() -> Unit
) {
    val source = interactionSource ?: remember { MutableInteractionSource() }
    FilledTonalButton(onClick, modifier.heightIn(min = 48.dp).controlPress(source, shape), enabled,
        shape, colors, elevation, border, contentPadding, source, content)
}

@Composable
fun WorkspaceOutlinedButton(
    onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true,
    shape: Shape = RoundedCornerShape(16.dp),
    colors: ButtonColors = ButtonDefaults.outlinedButtonColors(contentColor = LocalLifeOsColors.current.textPrimary),
    elevation: ButtonElevation? = null,
    border: BorderStroke? = BorderStroke(1.dp, LocalLifeOsColors.current.borderGlass),
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
    interactionSource: MutableInteractionSource? = null, content: @Composable RowScope.() -> Unit
) {
    val source = interactionSource ?: remember { MutableInteractionSource() }
    OutlinedButton(onClick, modifier.heightIn(min = 48.dp).controlPress(source, shape), enabled,
        shape, colors, elevation, border, contentPadding, source, content)
}

@Composable
fun WorkspaceTextButton(
    onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true,
    shape: Shape = RoundedCornerShape(12.dp),
    colors: ButtonColors = ButtonDefaults.textButtonColors(contentColor = LocalLifeOsColors.current.textSecondary),
    elevation: ButtonElevation? = null, border: BorderStroke? = null,
    contentPadding: PaddingValues = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
    interactionSource: MutableInteractionSource? = null, content: @Composable RowScope.() -> Unit
) {
    val source = interactionSource ?: remember { MutableInteractionSource() }
    TextButton(onClick, modifier.heightIn(min = 48.dp).controlPress(source, shape), enabled,
        shape, colors, elevation, border, contentPadding, source, content)
}

@Composable
fun WorkspaceIconButton(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true,
    content: @Composable () -> Unit) {
    val colors = LocalLifeOsColors.current
    val shape = RoundedCornerShape(15.dp)
    val source = remember { MutableInteractionSource() }
    IconButton(onClick, modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp).controlPress(source, shape)
        .clip(shape).background(colors.surfaceGlass).border(1.dp, colors.borderGlass, shape),
        enabled = enabled, colors = IconButtonDefaults.iconButtonColors(contentColor = colors.textSecondary),
        interactionSource = source, content = content)
}

/** Disclosure is a stateful, bounded control rather than a floating text link. */
@Composable
fun WorkspaceDisclosure(label: String, expanded: Boolean, onClick: () -> Unit,
    modifier: Modifier = Modifier, enabled: Boolean = true) {
    val colors = LocalLifeOsColors.current
    val reduced = LocalReducedMotion.current
    val rotation by animateFloatAsState(if (expanded) 180f else 0f,
        if (reduced) snap() else spring(1f, 650f), label = "disclosure_rotation")
    WorkspaceOutlinedButton(onClick, modifier.semantics { stateDescription = if (expanded) "Expanded" else "Collapsed" },
        enabled = enabled, shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.outlinedButtonColors(containerColor = colors.surfaceGlass, contentColor = colors.textPrimary),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)) {
        Text(label, Modifier.weight(1f, fill = false), style = MaterialTheme.typography.labelMedium)
        Spacer(Modifier.width(7.dp))
        Icon(Icons.Outlined.ExpandMore, null, Modifier.size(17.dp).graphicsLayer { rotationZ = rotation })
    }
}

@Composable
private fun ControlSurface(modifier: Modifier, embedded: Boolean = false, onClick: () -> Unit,
    content: @Composable () -> Unit) {
    val colors = LocalLifeOsColors.current
    val shape = RoundedCornerShape(22.dp)
    val source = remember { MutableInteractionSource() }
    Box(modifier.controlPress(source, shape).clip(shape)
        .background(if (embedded) Color.Transparent else colors.surfaceGlass)
        .then(if (embedded) Modifier else Modifier.border(1.dp, colors.borderGlass, shape))
        .clickable(interactionSource = source, indication = androidx.compose.foundation.LocalIndication.current,
            role = Role.Button, onClick = onClick)) { content() }
}

@Composable
fun WorkspaceActionRow(title: String, subtitle: String, icon: ImageVector, modifier: Modifier = Modifier,
    embedded: Boolean = false, onClick: () -> Unit) {
    val colors = LocalLifeOsColors.current
    ControlSurface(modifier.fillMaxWidth(), embedded, onClick) {
        Row(Modifier.fillMaxWidth().heightIn(min = 72.dp).padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(38.dp).background(colors.primaryAccent.copy(alpha = 0.09f), RoundedCornerShape(13.dp)),
                contentAlignment = Alignment.Center) { Icon(icon, null, Modifier.size(20.dp), tint = colors.primaryAccent) }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, color = colors.textPrimary, style = MaterialTheme.typography.labelLarge)
                Text(subtitle, color = colors.textSecondary, style = MaterialTheme.typography.bodySmall)
            }
            Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null, Modifier.size(18.dp), tint = colors.textSecondary)
        }
    }
}

@Composable
fun WorkspaceShortcut(title: String, icon: ImageVector, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val colors = LocalLifeOsColors.current
    ControlSurface(modifier, embedded = true, onClick = onClick) {
        Column(Modifier.fillMaxWidth().heightIn(min = 80.dp).padding(horizontal = 8.dp, vertical = 15.dp),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Icon(icon, null, Modifier.size(21.dp), tint = colors.textSecondary)
            Text(title, color = colors.textPrimary, style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center)
        }
    }
}
