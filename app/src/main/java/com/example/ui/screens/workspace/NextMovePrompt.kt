package com.example.ui.screens.workspace

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.DayMove
import com.example.ui.components.LocalReducedMotion
import com.example.ui.components.WorkspaceButton
import com.example.ui.components.WorkspaceTextButton
import com.example.ui.theme.LocalLifeOsColors
import kotlinx.coroutines.delay

/** An in-app invitation. Dismissal keeps the saved draft and never commits work. */
@Composable
fun NextMovePrompt(move: DayMove, onExpand: () -> Unit, onDismiss: () -> Unit) {
    val colors = LocalLifeOsColors.current
    val reduced = LocalReducedMotion.current
    var promptExpanded by remember { mutableStateOf(false) }
    var closing by remember { mutableStateOf(false) }
    var expandAfterClose by remember { mutableStateOf(false) }
    val expandCallback by rememberUpdatedState(onExpand)
    val dismissCallback by rememberUpdatedState(onDismiss)
    fun close(expand: Boolean) {
        if (!closing) { expandAfterClose = expand; closing = true; promptExpanded = false }
    }
    LaunchedEffect(Unit) {
        if (!reduced) delay(180)
        if (!closing) promptExpanded = true
    }
    LaunchedEffect(closing) {
        if (closing) {
            if (!reduced) delay(280)
            if (expandAfterClose) expandCallback() else dismissCallback()
        }
    }
    val corner by animateDpAsState(if (promptExpanded) 28.dp else 34.dp,
        if (reduced) snap() else tween(260), label = "move_prompt_corner")
    Dialog(onDismissRequest = { close(false) }, properties = DialogProperties(
        usePlatformDefaultWidth = false, dismissOnClickOutside = false)) {
        Box(Modifier.fillMaxWidth().padding(horizontal = 28.dp), contentAlignment = Alignment.Center) {
            Surface(shape = RoundedCornerShape(corner), color = colors.surfaceGlass,
                border = BorderStroke(1.dp, colors.borderGlass), tonalElevation = 0.dp) {
                AnimatedContent(targetState = promptExpanded, transitionSpec = {
                    (fadeIn(tween(if (reduced) 0 else 180)) togetherWith fadeOut(tween(if (reduced) 0 else 110)))
                        .using(SizeTransform(clip = true) { _, _ -> tween(if (reduced) 0 else 260) })
                }, label = "move_prompt_expand") { expanded ->
                    if (!expanded) {
                        Box(Modifier.size(68.dp), contentAlignment = Alignment.Center) {
                            Icon(Icons.Outlined.AutoAwesome, null, Modifier.size(25.dp), tint = colors.primaryAccent)
                        }
                    } else {
                        Column(Modifier.widthIn(max = 340.dp).fillMaxWidth().heightIn(max = 420.dp)
                            .verticalScroll(rememberScrollState()).padding(22.dp).testTag("next_move_prompt"),
                            verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Icon(Icons.Outlined.AutoAwesome, null, Modifier.size(22.dp), tint = colors.primaryAccent)
                            Text(if (move.sourceType == "PREVIEW") "Ready to try it?" else "Anything to add?",
                                color = colors.textPrimary, fontSize = 22.sp, lineHeight = 27.sp, fontWeight = FontWeight.Medium)
                            Text(if (move.title.isBlank()) "Give your next move a little room when you're ready."
                                else "Your draft is ready: ${move.title}", color = colors.textSecondary, fontSize = 13.sp, lineHeight = 19.sp, maxLines = 3)
                            WorkspaceButton(onClick = { close(true) }, modifier = Modifier.fillMaxWidth().testTag("next_move_expand")) { Text("Expand") }
                            WorkspaceTextButton(onClick = { close(false) }, modifier = Modifier.fillMaxWidth().testTag("next_move_dismiss")) { Text("Dismiss · keep draft") }
                        }
                    }
                }
            }
        }
    }
}
