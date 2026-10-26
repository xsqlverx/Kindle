package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.LocalLifeOsColors
import kotlinx.coroutines.launch

enum class LifeOsTab(
    val title: String,
    val filledIcon: ImageVector,
    val outlinedIcon: ImageVector
) {
    TODAY("Today", Icons.Filled.WbSunny, Icons.Outlined.WbSunny),
    GYM("Fitness", Icons.Filled.FitnessCenter, Icons.Outlined.FitnessCenter),
    ACADEMICS("College", Icons.Filled.School, Icons.Outlined.School),
    TASKS("Tasks", Icons.Filled.DashboardCustomize, Icons.Outlined.DashboardCustomize),
    RESET("60s Reset", Icons.Filled.Bedtime, Icons.Outlined.Bedtime)
}

@Composable
fun LiquidGlassBottomBar(
    currentTab: LifeOsTab,
    onTabSelected: (LifeOsTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val lifeColors = LocalLifeOsColors.current
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val tabs = LifeOsTab.values()
    val selectedIndex = tabs.indexOf(currentTab)

    // Animated target position for liquid pill slide
    val targetFraction = selectedIndex.toFloat() / (tabs.size - 1)
    val animatedFraction by animateFloatAsState(
        targetValue = targetFraction,
        animationSpec = spring(
            dampingRatio = 0.68f,
            stiffness = Spring.StiffnessLow
        ),
        label = "liquidBarPill"
    )

    // Fluid liquid stretch physics: stretches along the X axis during motion, rebounds slightly on Y
    val isMoving = (kotlin.math.abs(animatedFraction - targetFraction) > 0.02f)
    val pillScaleX by animateFloatAsState(
        targetValue = if (isMoving) 1.22f else 1.0f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 380f),
        label = "pillStretchX"
    )
    val pillScaleY by animateFloatAsState(
        targetValue = if (isMoving) 0.88f else 1.0f,
        animationSpec = spring(dampingRatio = 0.60f, stiffness = 420f),
        label = "pillStretchY"
    )

    // Interactive liquid tap ripple wave
    val rippleAnim = remember { Animatable(0f) }
    var rippleCenterFraction by remember { mutableStateOf(targetFraction) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            // Soft gradient veil behind bottom bar so scrolling elements gracefully fade
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.Transparent,
                        lifeColors.background.copy(alpha = 0.65f),
                        lifeColors.background.copy(alpha = 0.96f)
                    ),
                    startY = 0f,
                    endY = 140f
                )
            )
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        // Ambient caustic floor glow underneath the transparent glass bar
        Box(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .height(54.dp)
                .drawBehind {
                    val activeCenterX = (animatedFraction * (tabs.size - 1) + 0.5f) * (size.width / tabs.size)
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                lifeColors.primaryAccent.copy(alpha = if (lifeColors.isDark) 0.32f else 0.22f),
                                lifeColors.secondaryAccent.copy(alpha = if (lifeColors.isDark) 0.14f else 0.08f),
                                Color.Transparent
                            ),
                            center = Offset(activeCenterX, size.height * 0.5f),
                            radius = size.width * 0.35f
                        )
                    )
                }
        )

        // Floating Island Liquid Glass Body
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp)
                .shadow(
                    elevation = 16.dp,
                    shape = RoundedCornerShape(34.dp),
                    ambientColor = if (lifeColors.isDark) Color(0x66000000) else Color(0x33000000),
                    spotColor = lifeColors.primaryAccent.copy(alpha = if (lifeColors.isDark) 0.35f else 0.25f)
                )
                .clip(RoundedCornerShape(34.dp))
                // Truly transparent frosted glass substrate
                .background(
                    if (lifeColors.isDark) {
                        Brush.verticalGradient(
                            listOf(
                                Color(0x38181D2E), // Transparent smoky blue-graphite top
                                Color(0x1F0E121E), // Ultra-translucent middle allowing scroll content to show
                                Color(0x2E141826)  // Gentle tint at bottom
                            )
                        )
                    } else {
                        Brush.verticalGradient(
                            listOf(
                                Color(0x85FFFFFF), // Translucent crystal white top
                                Color(0x40DDE6F4), // Translucent frosted middle
                                Color(0x65EDF2FA)  // Soft milky glass bottom
                            )
                        )
                    }
                )
                // Razor-sharp optical refraction border with specular highlight
                .border(
                    width = 1.2.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = if (lifeColors.isDark) 0.65f else 0.85f), // Razor white Fresnel rim
                            lifeColors.primaryAccent.copy(alpha = 0.45f), // Chromatic refraction accent
                            Color.White.copy(alpha = 0.15f),
                            Color.Transparent,
                            lifeColors.secondaryAccent.copy(alpha = 0.30f),
                            Color.White.copy(alpha = if (lifeColors.isDark) 0.35f else 0.55f)
                        ),
                        start = Offset(0f, 0f),
                        end = Offset(650f, 120f)
                    ),
                    shape = RoundedCornerShape(34.dp)
                )
                // Draw internal liquid optics, specular horizon, and fluid droplet indicator
                .drawBehind {
                    val tabWidth = size.width / tabs.size
                    val pillWidth = (tabWidth * 0.78f) * pillScaleX
                    val pillHeight = (size.height * 0.74f) * pillScaleY
                    val currentCenter = (animatedFraction * (tabs.size - 1) + 0.5f) * tabWidth
                    val left = currentCenter - (pillWidth / 2f)
                    val top = (size.height - pillHeight) / 2f

                    // 1. Curved Lens Meniscus Highlight across upper half of the glass capsule
                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = if (lifeColors.isDark) 0.22f else 0.45f),
                                Color.White.copy(alpha = if (lifeColors.isDark) 0.05f else 0.12f),
                                Color.Transparent
                            ),
                            startY = 0f,
                            endY = size.height * 0.48f
                        ),
                        topLeft = Offset(2.dp.toPx(), 1.5.dp.toPx()),
                        size = Size(size.width - 4.dp.toPx(), size.height * 0.46f),
                        cornerRadius = CornerRadius(30.dp.toPx())
                    )

                    // 2. Liquid Tap Ripple Wave expanding through the glass
                    if (rippleAnim.value > 0f && rippleAnim.value < 1f) {
                        val waveCenter = (rippleCenterFraction * (tabs.size - 1) + 0.5f) * tabWidth
                        val maxRadius = tabWidth * 1.6f
                        val currentRadius = rippleAnim.value * maxRadius
                        val waveAlpha = (1f - rippleAnim.value) * (if (lifeColors.isDark) 0.35f else 0.25f)
                        drawCircle(
                            color = lifeColors.primaryAccent.copy(alpha = waveAlpha),
                            radius = currentRadius,
                            center = Offset(waveCenter, size.height / 2f),
                            style = Stroke(width = (2.5f * (1f - rippleAnim.value)).dp.toPx())
                        )
                    }

                    // 3. Liquid Droplet Refraction Glow (Caustic spotlight inside the glass)
                    drawRoundRect(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                lifeColors.primaryAccent.copy(alpha = if (lifeColors.isDark) 0.42f else 0.32f),
                                lifeColors.secondaryAccent.copy(alpha = if (lifeColors.isDark) 0.18f else 0.12f),
                                Color.Transparent
                            ),
                            center = Offset(currentCenter, size.height / 2f),
                            radius = pillWidth * 1.15f
                        ),
                        topLeft = Offset(left - 8.dp.toPx(), top - 6.dp.toPx()),
                        size = Size(pillWidth + 16.dp.toPx(), pillHeight + 12.dp.toPx()),
                        cornerRadius = CornerRadius(28.dp.toPx())
                    )

                    // 4. Liquid Mercury / Acrylic Droplet Capsule
                    drawRoundRect(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                lifeColors.primaryAccent.copy(alpha = if (lifeColors.isDark) 0.36f else 0.26f),
                                lifeColors.secondaryAccent.copy(alpha = if (lifeColors.isDark) 0.20f else 0.14f),
                                Color.White.copy(alpha = if (lifeColors.isDark) 0.08f else 0.20f)
                            ),
                            start = Offset(left, top),
                            end = Offset(left + pillWidth, top + pillHeight)
                        ),
                        topLeft = Offset(left, top),
                        size = Size(pillWidth, pillHeight),
                        cornerRadius = CornerRadius(22.dp.toPx())
                    )

                    // 5. Droplet Specular Reflection Arc (Top gloss reflection on liquid droplet)
                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = if (lifeColors.isDark) 0.45f else 0.65f),
                                Color.White.copy(alpha = 0.05f),
                                Color.Transparent
                            ),
                            startY = top,
                            endY = top + (pillHeight * 0.55f)
                        ),
                        topLeft = Offset(left + 2.dp.toPx(), top + 1.dp.toPx()),
                        size = Size(pillWidth - 4.dp.toPx(), pillHeight * 0.52f),
                        cornerRadius = CornerRadius(18.dp.toPx())
                    )

                    // 6. Droplet Specular Rim Stroke
                    drawRoundRect(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color.White.copy(alpha = if (lifeColors.isDark) 0.60f else 0.75f),
                                lifeColors.primaryAccent.copy(alpha = 0.40f),
                                Color.White.copy(alpha = 0.12f)
                            ),
                            start = Offset(left, top),
                            end = Offset(left + pillWidth, top + pillHeight)
                        ),
                        topLeft = Offset(left, top),
                        size = Size(pillWidth, pillHeight),
                        cornerRadius = CornerRadius(22.dp.toPx()),
                        style = Stroke(width = 1.3.dp.toPx())
                    )
                }
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                tabs.forEachIndexed { index, tab ->
                    val isSelected = (currentTab == tab)
                    val iconScale by animateFloatAsState(
                        targetValue = if (isSelected) 1.15f else 0.94f,
                        animationSpec = spring(dampingRatio = 0.62f, stiffness = 450f),
                        label = "tabIconScale_${tab.name}"
                    )
                    val iconOffsetY by animateFloatAsState(
                        targetValue = if (isSelected) (-1.5f) else 0f,
                        animationSpec = spring(dampingRatio = 0.65f, stiffness = 400f),
                        label = "tabOffsetY_${tab.name}"
                    )

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                if (!isSelected) {
                                    playAppleHaptic(context, strong = false)
                                    rippleCenterFraction = index.toFloat() / (tabs.size - 1)
                                    coroutineScope.launch {
                                        rippleAnim.snapTo(0f)
                                        rippleAnim.animateTo(
                                            targetValue = 1f,
                                            animationSpec = tween(400, easing = FastOutSlowInEasing)
                                        )
                                    }
                                    onTabSelected(tab)
                                }
                            }
                            .testTag("nav_tab_${tab.name.lowercase()}"),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = if (isSelected) tab.filledIcon else tab.outlinedIcon,
                            contentDescription = tab.title,
                            modifier = Modifier
                                .offset(y = iconOffsetY.dp)
                                .size(22.dp)
                                .scale(iconScale),
                            tint = if (isSelected) {
                                lifeColors.primaryAccent
                            } else {
                                if (lifeColors.isDark) {
                                    Color.White.copy(alpha = 0.62f)
                                } else {
                                    Color(0xBB1E2433)
                                }
                            }
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = tab.title,
                            fontSize = 10.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                            color = if (isSelected) {
                                lifeColors.primaryAccent
                            } else {
                                if (lifeColors.isDark) {
                                    Color.White.copy(alpha = 0.55f)
                                } else {
                                    Color(0x992B3144)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

