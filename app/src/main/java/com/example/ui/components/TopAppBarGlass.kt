package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppThemeMode
import com.example.ui.theme.LocalLifeOsColors
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun TopAppBarGlass(
    title: String,
    subtitle: String? = null,
    currentThemeMode: AppThemeMode,
    onThemeModeChanged: (AppThemeMode) -> Unit,
    onOpenSettings: (() -> Unit)? = null,
    isSettingsOpen: Boolean = false,
    settingsProgress: Float = 0f,
    modifier: Modifier = Modifier
) {
    val lifeColors = LocalLifeOsColors.current
    val context = LocalContext.current
    var showThemeDialog by remember { mutableStateOf(false) }

    val dateStr = remember {
        val sdf = SimpleDateFormat("EEEE, MMM d", Locale.getDefault())
        sdf.format(Date()).uppercase()
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Header Info
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp)
                    .graphicsLayer {
                        alpha = (1f - settingsProgress * 2.5f).coerceIn(0f, 1f)
                    }
            ) {
                Text(
                    text = subtitle ?: dateStr,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.1.sp,
                    color = lifeColors.primaryAccent,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = title,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = lifeColors.textPrimary,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Glass Theme Capsule
                Row(
                    modifier = Modifier
                        .graphicsLayer {
                            alpha = (1f - settingsProgress * 2.5f).coerceIn(0f, 1f)
                        }
                        .clip(RoundedCornerShape(18.dp))
                        .background(lifeColors.surfaceGlass)
                        .border(
                            width = 1.dp,
                            color = lifeColors.borderGlass,
                            shape = RoundedCornerShape(18.dp)
                        )
                        .clickable(
                            enabled = settingsProgress < 0.2f,
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            playAppleHaptic(context)
                            showThemeDialog = true
                        }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                        .testTag("theme_selector_pill"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val themeIcon = when (currentThemeMode) {
                        AppThemeMode.AUTO_TIME -> Icons.Default.AutoMode
                        AppThemeMode.LIGHT_DAY -> Icons.Default.LightMode
                        AppThemeMode.SUNSET_AFTERNOON -> Icons.Default.WbTwilight
                        AppThemeMode.NIGHT_GRAPHITE -> Icons.Default.DarkMode
                    }

                    Icon(
                        imageVector = themeIcon,
                        contentDescription = "Theme mode",
                        modifier = Modifier.size(15.dp),
                        tint = lifeColors.primaryAccent
                    )

                    Text(
                        text = when (currentThemeMode) {
                            AppThemeMode.AUTO_TIME -> "Auto"
                            AppThemeMode.LIGHT_DAY -> "Day"
                            AppThemeMode.SUNSET_AFTERNOON -> "Sunset"
                            AppThemeMode.NIGHT_GRAPHITE -> "Night"
                        },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = lifeColors.textPrimary,
                        maxLines = 1
                    )
                }

                if (onOpenSettings != null) {
                    val iconRotation = settingsProgress * 90f
                    val gearAlpha = ((0.5f - settingsProgress) * 2f).coerceIn(0f, 1f)
                    val closeAlpha = ((settingsProgress - 0.5f) * 2f).coerceIn(0f, 1f)
                    val containerBg = lerp(
                        lifeColors.surfaceGlass,
                        lifeColors.primaryAccent.copy(alpha = 0.22f),
                        settingsProgress
                    )
                    val containerBorder = lerp(
                        lifeColors.borderGlass,
                        lifeColors.primaryAccent.copy(alpha = 0.65f),
                        settingsProgress
                    )

                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .graphicsLayer {
                                rotationZ = iconRotation
                            }
                            .clip(CircleShape)
                            .background(containerBg)
                            .border(1.dp, containerBorder, CircleShape)
                            .clickable {
                                playAppleHaptic(context)
                                onOpenSettings()
                            }
                            .testTag("top_app_bar_settings_btn"),
                        contentAlignment = Alignment.Center
                    ) {
                        if (gearAlpha > 0f) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = if (isSettingsOpen) null else "Settings",
                                tint = lerp(
                                    lifeColors.textPrimary,
                                    lifeColors.primaryAccent,
                                    settingsProgress
                                ),
                                modifier = Modifier
                                    .size(19.dp)
                                    .graphicsLayer {
                                        alpha = gearAlpha
                                        scaleX = 1f - settingsProgress * 0.3f
                                        scaleY = 1f - settingsProgress * 0.3f
                                    }
                            )
                        }

                        if (closeAlpha > 0f) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = if (isSettingsOpen) "Close Settings" else null,
                                tint = lifeColors.primaryAccent,
                                modifier = Modifier
                                    .size(19.dp)
                                    .graphicsLayer {
                                        alpha = closeAlpha
                                        scaleX = 0.7f + settingsProgress * 0.3f
                                        scaleY = 0.7f + settingsProgress * 0.3f
                                    }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            containerColor = if (lifeColors.isDark) Color(0xFF1B1E28) else Color.White,
            shape = RoundedCornerShape(28.dp),
            title = {
                Text(
                    text = "Appearance & Dynamics",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = lifeColors.textPrimary
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Auto mode transitions smoothly between Day, Golden Sunset, and Night Graphite based on real-time clock.",
                        fontSize = 13.sp,
                        color = lifeColors.textSecondary
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    AppThemeMode.values().forEach { mode ->
                        val isSelected = (mode == currentThemeMode)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    if (isSelected) lifeColors.primaryAccent.copy(alpha = 0.18f)
                                    else Color.Transparent
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) lifeColors.primaryAccent else Color.Transparent,
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .clickable {
                                    playAppleHaptic(context)
                                    onThemeModeChanged(mode)
                                    showThemeDialog = false
                                }
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                val icon = when (mode) {
                                    AppThemeMode.AUTO_TIME -> Icons.Default.AutoMode
                                    AppThemeMode.LIGHT_DAY -> Icons.Default.LightMode
                                    AppThemeMode.SUNSET_AFTERNOON -> Icons.Default.WbTwilight
                                    AppThemeMode.NIGHT_GRAPHITE -> Icons.Default.DarkMode
                                }
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = if (isSelected) lifeColors.primaryAccent else lifeColors.textSecondary
                                )
                                Text(
                                    text = mode.title,
                                    fontSize = 14.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = lifeColors.textPrimary
                                )
                            }
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    modifier = Modifier.size(18.dp),
                                    tint = lifeColors.primaryAccent
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                WorkspaceTextButton(onClick = { showThemeDialog = false }) {
                    Text("Close", color = lifeColors.primaryAccent, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}
