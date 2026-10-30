package com.example.ui.screens.workspace

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MoveDuration
import com.example.ui.components.*
import com.example.ui.theme.LocalLifeOsColors
import kotlin.math.roundToInt

/** The editable numeral is the primary duration control; no preset choices. */
@Composable
fun MoveDurationControl(text: String, enabled: Boolean, onChange: (String) -> Unit) {
    val colors = LocalLifeOsColors.current
    val reduced = LocalReducedMotion.current
    val focus = LocalFocusManager.current
    val minutes = MoveDuration.parse(text)
    val shape = RoundedCornerShape(30.dp)
    var value by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue(text)) }
    var editing by remember { mutableStateOf(false) }
    LaunchedEffect(text) {
        if (value.text != text) value = TextFieldValue(text, TextRange(text.length))
    }
    val light by animateFloatAsState(if (editing) 0.25f else 0.12f,
        if (reduced) snap() else tween(180), label = "duration_light")
    Column(Modifier.fillMaxWidth().clip(shape)
        .background(Brush.radialGradient(listOf(colors.primaryAccent.copy(alpha = light), colors.surfaceGlass)))
        .border(1.dp, Brush.verticalGradient(listOf(colors.primaryAccent.copy(alpha = if (editing) 0.65f else 0.32f), colors.borderGlass)), shape)
        .padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("YOUR TIME", Modifier.weight(1f), color = colors.textSecondary, fontSize = 10.sp, letterSpacing = 1.5.sp)
            Icon(Icons.Outlined.Timelapse, null, Modifier.size(19.dp), tint = colors.primaryAccent)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            BasicTextField(value, { next ->
                value = next.copy(text = next.text.take(12))
                onChange(value.text)
            }, modifier = Modifier.widthIn(min = 84.dp, max = 210.dp).weight(1f, fill = false)
                .onFocusChanged {
                    if (it.isFocused && !editing) value = value.copy(selection = TextRange(0, value.text.length))
                    editing = it.isFocused
                }.semantics {
                    contentDescription = "Duration in minutes"
                    if (minutes == null) this.error("Enter 1–180 whole minutes")
                }.testTag("next_move_minutes"),
                enabled = enabled, singleLine = true,
                textStyle = MaterialTheme.typography.displayLarge.copy(color = colors.textPrimary,
                    fontSize = 64.sp, lineHeight = 76.sp, fontWeight = FontWeight.Light, letterSpacing = (-2).sp, textAlign = TextAlign.Center),
                cursorBrush = SolidColor(colors.primaryAccent),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { focus.clearFocus() }),
                decorationBox = { inner -> Box(contentAlignment = Alignment.Center) {
                    if (value.text.isEmpty()) Text("—", color = colors.textTertiary, fontSize = 58.sp)
                    inner()
                } })
            Text("min", Modifier.padding(start = 7.dp, top = 20.dp), color = colors.textSecondary, fontSize = 15.sp)
        }
        Text(if (editing) "Enter your exact minutes" else "Tap the number to set your own time", color = colors.textSecondary,
            style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
        Slider(value = (minutes ?: MoveDuration.MIN).toFloat(), onValueChange = { onChange(it.roundToInt().toString()) },
            valueRange = MoveDuration.MIN.toFloat()..MoveDuration.MAX.toFloat(), steps = MoveDuration.MAX - MoveDuration.MIN - 1,
            enabled = enabled, modifier = Modifier.fillMaxWidth().semantics {
                contentDescription = "Adjust duration"; stateDescription = minutes?.let { "$it minutes" } ?: "Enter a valid duration"
            }.testTag("next_move_duration_slider"),
            colors = SliderDefaults.colors(thumbColor = colors.primaryAccent, activeTrackColor = colors.primaryAccent,
                inactiveTrackColor = colors.primaryAccent.copy(alpha = 0.12f), activeTickColor = Color.Transparent, inactiveTickColor = Color.Transparent))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            WorkspaceIconButton(enabled = enabled && minutes != null && minutes > MoveDuration.MIN,
                onClick = { minutes?.let { onChange((it - 1).toString()) } }) {
                Icon(Icons.Outlined.Remove, "One minute less", Modifier.size(18.dp))
            }
            Text(if (minutes == null) "Enter 1–180 whole minutes" else "1–180 minutes · adjust by one",
                Modifier.weight(1f), color = if (minutes == null) MaterialTheme.colorScheme.error else colors.textSecondary,
                style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
            WorkspaceIconButton(enabled = enabled && minutes != null && minutes < MoveDuration.MAX,
                onClick = { minutes?.let { onChange((it + 1).toString()) } }) {
                Icon(Icons.Outlined.Add, "One minute more", Modifier.size(18.dp))
            }
        }
    }
}

/** Inset native editor with a quiet label, rather than a stack of outlined form boxes. */
@Composable
fun MoveField(value: String, onChange: (String) -> Unit, label: String, placeholder: String,
    enabled: Boolean, modifier: Modifier = Modifier, prominent: Boolean = false, maxLines: Int = 4,
    minLines: Int = 1, singleLine: Boolean = false, keyboardOptions: KeyboardOptions = KeyboardOptions.Default) {
    val colors = LocalLifeOsColors.current
    val source = remember { MutableInteractionSource() }
    val focused by source.collectIsFocusedAsState()
    val shape = RoundedCornerShape(23.dp)
    BasicTextField(value, onChange, enabled = enabled, interactionSource = source, maxLines = maxLines, minLines = minLines,
        singleLine = singleLine, keyboardOptions = keyboardOptions,
        modifier = modifier.fillMaxWidth().clip(shape).background(colors.surfaceGlass)
            .border(1.dp, if (focused) colors.primaryAccent.copy(alpha = 0.65f) else colors.borderGlass, shape)
            .semantics { contentDescription = label },
        cursorBrush = SolidColor(colors.primaryAccent),
        textStyle = MaterialTheme.typography.bodyLarge.copy(color = colors.textPrimary,
            fontSize = if (prominent) 20.sp else 14.sp, lineHeight = if (prominent) 28.sp else 21.sp,
            fontWeight = if (prominent) FontWeight.Medium else FontWeight.Normal, letterSpacing = 0.sp),
        decorationBox = { inner -> Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Text(label, color = colors.textSecondary, style = MaterialTheme.typography.labelMedium)
            Box(Modifier.fillMaxWidth()) {
                if (value.isEmpty()) Text(placeholder, color = colors.textTertiary,
                    fontSize = if (prominent) 20.sp else 14.sp, lineHeight = if (prominent) 28.sp else 21.sp)
                inner()
            }
        } })
}

@Composable
fun MovePlacementReceipt(minutes: Int?, preview: Boolean, window: Long?, now: Long, linkedBlock: Boolean) {
    val colors = LocalLifeOsColors.current
    val ready = window != null && window <= now + 1000L
    Column(Modifier.fillMaxWidth().padding(horizontal = 2.dp, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(11.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            Icon(if (preview) Icons.Outlined.PlayCircleOutline else Icons.Outlined.Schedule, null, Modifier.size(18.dp), tint = colors.primaryAccent)
            Text(when { minutes == null -> "Choose your minutes"; preview -> "A tryout, just for you"; ready -> "Room right now";
                window != null -> "Your next free window"; else -> "A smaller window is needed" }, color = colors.textPrimary,
                style = MaterialTheme.typography.labelLarge)
        }
        if (minutes != null && !preview && window != null) {
            Row(Modifier.fillMaxWidth().background(colors.primaryAccent.copy(alpha = 0.07f), RoundedCornerShape(18.dp)).padding(16.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text("START", color = colors.textSecondary, fontSize = 9.sp, letterSpacing = 1.sp)
                    Text(clock(window), color = colors.textPrimary, style = MaterialTheme.typography.labelLarge)
                }
                Text("$minutes min", color = colors.primaryAccent, style = MaterialTheme.typography.labelMedium)
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text("END", color = colors.textSecondary, fontSize = 9.sp, letterSpacing = 1.sp)
                    Text(clock(window + minutes * 60_000L), color = colors.textPrimary, style = MaterialTheme.typography.labelLarge)
                }
            }
        }
        Text(when { minutes == null -> "Your window updates when the duration is valid.";
            preview -> "$minutes ${if (minutes == 1) "minute" else "minutes"} to try the loop. No task, plan block, history or alert is created.";
            window == null -> "Shorten the duration, or use Plan for tomorrow. Your saved commitments keep their place.";
            linkedBlock -> "Replaces this block's window. Its source and material stay linked.";
            else -> "Fits around your saved classes and commitments. Nothing is reserved until you save." },
            color = colors.textSecondary, style = MaterialTheme.typography.bodySmall)
    }
}
