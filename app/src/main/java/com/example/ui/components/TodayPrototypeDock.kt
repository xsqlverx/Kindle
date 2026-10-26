package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.LocalLifeOsColors

/** All five destinations remain visible; additional tools unfold within Today. */
@Composable
fun TodayPrototypeDock(currentTab: LifeOsTab, onTabSelected: (LifeOsTab) -> Unit, modifier: Modifier = Modifier) {
    val colors = LocalLifeOsColors.current
    Box(modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(Color.Transparent, colors.background)))
        .navigationBarsPadding().padding(horizontal = 16.dp, vertical = 10.dp)) {
        Row(Modifier.fillMaxWidth().selectableGroup().clip(RoundedCornerShape(25.dp))
            .background(Brush.verticalGradient(if (colors.isDark) listOf(Color(0xFF292831), Color(0xFF1B1B22))
                else listOf(Color.White, colors.background)))
            .border(1.dp, colors.borderGlass, RoundedCornerShape(25.dp))
            .padding(horizontal = 5.dp, vertical = 5.dp)) {
            LifeOsTab.entries.forEach { tab ->
                val selected = tab == currentTab
                val tint by animateColorAsState(if (selected) colors.primaryAccent else colors.textTertiary, label = "dock_${tab.name}")
                Column(
                    Modifier.weight(1f).clip(RoundedCornerShape(19.dp))
                        .selectable(selected, role = Role.Tab, onClick = { onTabSelected(tab) })
                        .heightIn(min = 62.dp).padding(vertical = 8.dp).testTag("nav_tab_${tab.name.lowercase()}"),
                    horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center
                ) {
                    Icon(if (selected) tab.filledIcon else tab.outlinedIcon, null, tint = tint, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.height(5.dp))
                    Text(if (tab == LifeOsTab.RESET) "Reset" else tab.title, color = tint, fontSize = 10.sp,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
                    Spacer(Modifier.height(4.dp))
                    Box(Modifier.size(3.dp).clip(CircleShape).background(if (selected) colors.primaryAccent else Color.Transparent))
                }
            }
        }
    }
}
