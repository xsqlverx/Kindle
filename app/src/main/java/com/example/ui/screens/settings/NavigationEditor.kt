package com.example.ui.screens.settings

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Destination
import com.example.data.reminders.PlanReminders
import com.example.ui.components.*
import com.example.ui.theme.LocalLifeOsColors
import com.example.viewmodel.WorkspaceViewModel

@Composable
fun NavigationEditor(model: WorkspaceViewModel) {
    val colors = LocalLifeOsColors.current
    val slots by model.slots.collectAsStateWithLifecycle()
    val reduced by model.reduceMotion.collectAsStateWithLifecycle()
    val reminders by model.reminders.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val requestNotifications = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        model.setReminders(granted)
    }
    var slot by rememberSaveable { mutableIntStateOf(0) }
    var picker by rememberSaveable { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Your four shortcuts", color = colors.textPrimary, fontSize = 25.sp, fontWeight = FontWeight.Medium)
        Text("Tap a slot in the live preview. Every destination can go anywhere. The centre always opens all spaces.", color = colors.textSecondary, fontSize = 13.sp, lineHeight = 20.sp)
        NavbarPreview(slots, slot, { from, to -> model.assign(to, slots[from]); slot = to }) { slot = it }
        Text("Hold and drag to swap slots, or use the arrow buttons below.", color = colors.textSecondary, fontSize = 11.sp)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Slot ${slot + 1} · ${slots[slot].title}", color = colors.textPrimary, fontWeight = FontWeight.SemiBold)
                Text("Changes save immediately", color = colors.textTertiary, fontSize = 11.sp)
            }
            IconButton(enabled = slot > 0, onClick = { model.move(slot, -1); slot-- }) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Move selected slot left")
            }
            IconButton(enabled = slot < 3, onClick = { model.move(slot, 1); slot++ }) {
                Icon(Icons.AutoMirrored.Outlined.ArrowForward, "Move selected slot right")
            }
        }
        WorkspaceButton(onClick = { picker = true }, modifier = Modifier.fillMaxWidth()) { Text("Choose destination for slot ${slot + 1}") }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Reduce motion", color = colors.textPrimary)
                Text("Instant launcher and page transitions", color = colors.textSecondary, fontSize = 12.sp)
            }
            Switch(checked = reduced, onCheckedChange = model::setReduceMotion)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Plan reminders", color = colors.textPrimary)
                Text("Opt in to local alerts with Start, Delay and Replan. Android may delay delivery; confirmed commitments and active work are quiet.", color = colors.textSecondary, fontSize = 12.sp, lineHeight = 18.sp)
            }
            Switch(checked = reminders, onCheckedChange = { enabled ->
                if (!enabled) model.setReminders(false)
                else if (Build.VERSION.SDK_INT >= 33 && !PlanReminders.permitted(context)) requestNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
                else model.setReminders(true)
            })
        }
        if (reminders && !PlanReminders.permitted(context)) Text("Notifications are blocked in Android settings. Plan remains available in the app.", color = colors.textSecondary, fontSize = 12.sp)
    }
    if (picker) AlertDialog(onDismissRequest = { picker = false }, title = { Text("Choose any destination") },
        text = {
            androidx.compose.foundation.lazy.LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                items(Destination.entries.size) { index ->
                    val destination = Destination.entries[index]
                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                        .background(colors.primaryAccent.copy(alpha = if (destination == slots[slot]) 0.18f else 0.03f))
                        .clickable { model.assign(slot, destination); picker = false }.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(destination.icon(), null, tint = colors.primaryAccent)
                        Column(Modifier.padding(start = 12.dp)) {
                            Text(destination.title, color = colors.textPrimary)
                            val position = slots.indexOf(destination)
                            Text(if (position >= 0) "In slot ${position + 1} · choosing swaps slots" else destination.purpose,
                                color = colors.textSecondary, fontSize = 11.sp)
                        }
                    }
                }
            }
        }, confirmButton = { WorkspaceTextButton(onClick = { picker = false }) { Text("Done") } })
}
