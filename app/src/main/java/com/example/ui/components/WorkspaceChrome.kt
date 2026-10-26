package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppThemeMode
import com.example.ui.theme.LocalLifeOsColors

@Composable
fun WorkspaceIntro(eyebrow: String, title: String, description: String, modifier: Modifier = Modifier) {
    val colors = LocalLifeOsColors.current
    Column(modifier.fillMaxWidth().padding(top = 8.dp, bottom = 6.dp)) {
        Text(eyebrow, color = colors.primaryAccent, fontSize = 10.sp, letterSpacing = 1.4.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(12.dp))
        Text(title, color = colors.textPrimary, fontSize = 31.sp, lineHeight = 37.sp,
            letterSpacing = (-0.8).sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(10.dp))
        Text(description, color = colors.textSecondary, fontSize = 13.sp, lineHeight = 20.sp)
    }
}

@Composable
fun WorkspaceLabel(text: String, modifier: Modifier = Modifier) {
    Text(text, modifier = modifier, color = LocalLifeOsColors.current.textTertiary,
        fontSize = 10.sp, letterSpacing = 1.sp, fontWeight = FontWeight.Medium)
}

@Composable
fun WorkspaceTopBar(
    title: String,
    subtitle: String?,
    currentThemeMode: AppThemeMode,
    onThemeModeChanged: (AppThemeMode) -> Unit,
    onOpenSettings: () -> Unit,
    isSettingsOpen: Boolean,
    settingsProgress: Float,
    modifier: Modifier = Modifier
) {
    val colors = LocalLifeOsColors.current
    var showAppearance by remember { mutableStateOf(false) }
    Row(
        modifier.fillMaxWidth().background(colors.background).statusBarsPadding()
            .heightIn(min = 76.dp).padding(start = 24.dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f).graphicsLayer { alpha = (1f - settingsProgress * 2f).coerceIn(0f, 1f) }) {
            WorkspaceLabel(subtitle ?: "YOUR WORKSPACE")
            Spacer(Modifier.height(4.dp))
            Text(title, color = colors.textPrimary, fontSize = 22.sp, fontWeight = FontWeight.Medium,
                maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        IconButton(onClick = { showAppearance = true }, enabled = !isSettingsOpen,
            modifier = Modifier.testTag("theme_selector_pill")) {
            Icon(Icons.Outlined.Palette, "Change appearance", tint = colors.textSecondary, modifier = Modifier.size(21.dp))
        }
        IconButton(onClick = onOpenSettings, modifier = Modifier.testTag("top_app_bar_settings_btn")) {
            Icon(if (isSettingsOpen) Icons.Outlined.Close else Icons.Outlined.Tune,
                if (isSettingsOpen) "Close settings" else "Open settings",
                tint = colors.primaryAccent, modifier = Modifier.size(21.dp))
        }
    }
    if (showAppearance) {
        AlertDialog(
            onDismissRequest = { showAppearance = false },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(26.dp),
            title = { Text("Make yourself at home.", fontWeight = FontWeight.Medium) },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Choose a palette for your workspace. Today keeps its first-look dark canvas.",
                        color = colors.textSecondary, fontSize = 13.sp, lineHeight = 20.sp)
                    Spacer(Modifier.height(8.dp))
                    AppThemeMode.entries.forEach { mode ->
                        val selected = mode == currentThemeMode
                        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                            .background(if (selected) colors.primaryAccent.copy(alpha = 0.12f) else colors.background)
                            .clickable { onThemeModeChanged(mode); showAppearance = false }
                            .heightIn(min = 52.dp).padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(8.dp).clip(CircleShape).background(colors.primaryAccent))
                            Spacer(Modifier.width(12.dp))
                            Text(mode.title, color = colors.textPrimary, fontSize = 13.sp, modifier = Modifier.weight(1f))
                            if (selected) Icon(Icons.Outlined.Check, "Selected", tint = colors.primaryAccent, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showAppearance = false }) { Text("Done") } }
        )
    }
}
