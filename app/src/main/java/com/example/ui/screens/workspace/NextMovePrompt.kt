package com.example.ui.screens.workspace

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DayMove
import com.example.ui.components.LocalOrbScene
import com.example.ui.components.LocalReducedMotion
import com.example.ui.components.WorkspaceTextButton
import com.example.ui.components.WorkspaceTonalButton
import com.example.ui.theme.LocalLifeOsColors
import kotlinx.coroutines.delay

/** Non-modal in-app invitation; the user's choice, never a timer, closes the banner. */
@Composable
fun NextMovePrompt(move: DayMove, onExpand: () -> Unit, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    val colors = LocalLifeOsColors.current
    val reduced = LocalReducedMotion.current || (LocalOrbScene.current?.still == true)
    var expanded by remember { mutableStateOf(false) }
    var closing by remember { mutableStateOf(false) }
    var expandAfterClose by remember { mutableStateOf(false) }
    val arrival = remember { Animatable(if (reduced) 1f else 0f) }
    val expandCallback by rememberUpdatedState(onExpand)
    val dismissCallback by rememberUpdatedState(onDismiss)
    fun close(openEditor: Boolean) {
        if (!closing) { expandAfterClose = openEditor; closing = true; expanded = false }
    }
    BackHandler(enabled = !closing) { close(false) }
    LaunchedEffect(Unit) {
        if (reduced) arrival.snapTo(1f) else {
            withFrameNanos { }
            arrival.animateTo(1f, tween(180))
            delay(100)
        }
        if (!closing) expanded = true
    }
    LaunchedEffect(closing) {
        if (closing) {
            if (!reduced) {
                delay(250)
                arrival.animateTo(0f, tween(160))
            }
            if (expandAfterClose) expandCallback() else dismissCallback()
        }
    }
    val corner by animateDpAsState(if (expanded) 24.dp else 28.dp,
        if (reduced) snap() else tween(240), label = "move_banner_corner")
    Box(modifier.fillMaxWidth().statusBarsPadding().padding(start = 16.dp, end = 16.dp, top = 8.dp),
        contentAlignment = Alignment.TopCenter) {
        Surface(modifier = Modifier.graphicsLayer {
            alpha = arrival.value
            translationY = (1f - arrival.value) * -18.dp.toPx()
        }, shape = RoundedCornerShape(corner), color = colors.surfaceGlass.copy(alpha = 0.98f),
            border = BorderStroke(1.dp, colors.borderGlass), tonalElevation = 0.dp, shadowElevation = 5.dp) {
            AnimatedContent(targetState = expanded, transitionSpec = {
                (fadeIn(tween(if (reduced) 0 else 150)) togetherWith fadeOut(tween(if (reduced) 0 else 100)))
                    .using(SizeTransform(clip = true) { _, _ -> tween(if (reduced) 0 else 240) })
            }, label = "move_banner_expand") { full ->
                if (!full) {
                    Box(Modifier.size(56.dp), contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.AutoAwesome, null, Modifier.size(22.dp), tint = colors.primaryAccent)
                    }
                } else {
                    BoxWithConstraints(Modifier.widthIn(max = 440.dp).fillMaxWidth().testTag("next_move_prompt")) {
                        val compact = maxWidth < 345.dp || LocalDensity.current.fontScale > 1.2f
                        if (compact) {
                            Column(Modifier.fillMaxWidth().padding(start = 16.dp, end = 10.dp, top = 10.dp, bottom = 6.dp)) {
                                MoveBannerTitle(move, Modifier.fillMaxWidth())
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                                    MoveBannerActions({ close(false) }, { close(true) })
                                }
                            }
                        } else {
                            Row(Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(start = 16.dp, end = 8.dp),
                                verticalAlignment = Alignment.CenterVertically) {
                                MoveBannerTitle(move, Modifier.weight(1f))
                                MoveBannerActions({ close(false) }, { close(true) })
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MoveBannerTitle(move: DayMove, modifier: Modifier = Modifier) {
    val colors = LocalLifeOsColors.current
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Icon(Icons.Outlined.AutoAwesome, null, Modifier.size(20.dp), tint = colors.primaryAccent)
        Column(Modifier.weight(1f)) {
            Text(if (move.sourceType == "PREVIEW") "Try a next move?" else "Anything to add?",
                color = colors.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (move.title.isNotBlank()) Text(move.title, color = colors.textSecondary, fontSize = 11.sp,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun MoveBannerActions(onDismiss: () -> Unit, onExpand: () -> Unit) {
    WorkspaceTextButton(onClick = onDismiss, modifier = Modifier.testTag("next_move_dismiss"),
        contentPadding = PaddingValues(horizontal = 7.dp, vertical = 8.dp)) {
        Text("Dismiss", fontSize = 12.sp, maxLines = 1, softWrap = false)
    }
    Spacer(Modifier.width(2.dp))
    WorkspaceTonalButton(onClick = onExpand, modifier = Modifier.testTag("next_move_expand"),
        shape = RoundedCornerShape(16.dp), contentPadding = PaddingValues(horizontal = 11.dp, vertical = 8.dp)) {
        Text("Expand", fontSize = 12.sp, fontWeight = FontWeight.Medium, maxLines = 1, softWrap = false)
    }
}
