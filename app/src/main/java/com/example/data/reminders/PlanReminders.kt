package com.example.data.reminders

import android.Manifest
import android.app.*
import android.content.*
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.data.local.DatabaseProvider
import com.example.data.model.DayBlock
import com.example.data.model.FocusPhase
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import java.util.Calendar

/** Opt-in, inexact alarms. Scheduling is not evidence that Android delivered an alert. */
object PlanReminders {
    private const val CHANNEL = "planned_work"
    private fun prefs(context: Context) = context.getSharedPreferences("workspace_reminders", Context.MODE_PRIVATE)
    fun enabled(context: Context) = prefs(context).getBoolean("enabled", false)
    fun permitted(context: Context) = (Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) &&
        NotificationManagerCompat.from(context).areNotificationsEnabled()
    fun setEnabled(context: Context, value: Boolean) { prefs(context).edit().putBoolean("enabled", value).apply() }

    fun syncSession(context: Context) {
        val manager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, PlanReminderReceiver::class.java).setAction("SESSION_FINISHED")
        val pending = PendingIntent.getBroadcast(context, 1, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        manager.cancel(pending)
        val focus = context.getSharedPreferences("today_focus_session", Context.MODE_PRIVATE)
        val deadline = focus.getLong("deadline", 0L)
        if (enabled(context) && permitted(context) && focus.getString("source_type", "") != "PREVIEW" && focus.getString("phase", "") == FocusPhase.RUNNING.name && deadline > System.currentTimeMillis())
            manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, deadline, pending)
    }

    private fun alarm(context: Context, id: String): PendingIntent = PendingIntent.getBroadcast(context, 0,
        Intent(context, PlanReminderReceiver::class.java).setAction("REMIND").setData(Uri.parse("workspace://plan/$id")).putExtra("block_id", id),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

    fun sync(context: Context, blocks: List<DayBlock>) {
        val manager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val previous = prefs(context).getStringSet("scheduled", emptySet()).orEmpty()
        previous.forEach { manager.cancel(alarm(context, it)) }
        val upcoming = if (enabled(context) && permitted(context)) blocks.filter { !it.fixed && it.state == "PLANNED" && it.startMillis > System.currentTimeMillis() }.take(100) else emptyList()
        upcoming.forEach { manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, it.startMillis, alarm(context, it.id)) }
        prefs(context).edit().putStringSet("scheduled", upcoming.map { it.id }.toSet()).apply()
        syncSession(context)
    }

    private fun open(context: Context, id: String, action: String): PendingIntent = PendingIntent.getActivity(context, 0,
        Intent(context, MainActivity::class.java).setAction(action).setData(Uri.parse("workspace://plan/$id/$action"))
            .putExtra("block_id", id).putExtra("work_action", action).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

    suspend fun sessionElapsed(context: Context) {
        if (!enabled(context) || !permitted(context)) return
        val prefs = context.getSharedPreferences("today_focus_session", Context.MODE_PRIVATE)
        if (prefs.getString("source_type", "") == "PREVIEW") return
        val now = System.currentTimeMillis()
        val deadline = prefs.getLong("deadline", 0L)
        if (prefs.getString("phase", "") != FocusPhase.RUNNING.name || deadline <= 0L || now < deadline) return
        if (DatabaseProvider.get(context).workspaceDao().blocks().first().any { it.fixed && now in it.startMillis until it.endMillis }) return
        val cal = Calendar.getInstance()
        val minute = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
        if (DatabaseProvider.get(context).academicDao().getAllTimetable().first().any {
            it.dayOfWeek == cal.get(Calendar.DAY_OF_WEEK) && minute in (it.startHour * 60 + it.startMinute) until (it.endHour * 60 + it.endMinute)
        }) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= 26) manager.createNotificationChannel(NotificationChannel(CHANNEL, "Planned work", NotificationManager.IMPORTANCE_DEFAULT))
        val id = prefs.getString("id", "").orEmpty()
        val notification = NotificationCompat.Builder(context, CHANNEL).setSmallIcon(android.R.drawable.ic_menu_recent_history)
            .setContentTitle("Time to save your place").setContentText(prefs.getString("title", "Your session"))
            .setContentIntent(open(context, id, "SESSION")).setAutoCancel(true)
            .addAction(0, "Review session", open(context, id, "SESSION")).build()
        try { manager.notify(id, 1, notification) } catch (_: SecurityException) { }
    }

    suspend fun deliver(context: Context, id: String) {
        if (!enabled(context) || !permitted(context)) return
        val db = DatabaseProvider.get(context)
        val blocks = db.workspaceDao().blocks().first()
        val block = blocks.firstOrNull { it.id == id && !it.fixed && it.state == "PLANNED" } ?: return
        val now = System.currentTimeMillis()
        // Ignore cancelled/rescheduled stale alarms and defer recovery to the user.
        if (now < block.startMillis || now > block.startMillis + 60 * 60_000L) return
        val focus = context.getSharedPreferences("today_focus_session", Context.MODE_PRIVATE)
        val phase = focus.getString("phase", "")
        if (focus.getString("source_type", "") != "PREVIEW" && (phase == FocusPhase.RUNNING.name || phase == FocusPhase.PAUSED.name)) return
        if (blocks.any { it.fixed && now >= it.startMillis && now < it.endMillis }) return
        val cal = Calendar.getInstance()
        val minute = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
        if (db.academicDao().getAllTimetable().first().any {
            it.dayOfWeek == cal.get(Calendar.DAY_OF_WEEK) && minute in (it.startHour * 60 + it.startMinute) until (it.endHour * 60 + it.endMinute)
        }) return
        val notifications = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= 26) notifications.createNotificationChannel(NotificationChannel(CHANNEL, "Planned work", NotificationManager.IMPORTANCE_DEFAULT))
        val notification = NotificationCompat.Builder(context, CHANNEL).setSmallIcon(android.R.drawable.ic_menu_agenda)
            .setContentTitle(block.title).setContentText(block.firstStep.ifBlank { "Your planned window is ready. Start, delay or revise it." })
            .setContentIntent(open(context, id, "OPEN")).setAutoCancel(true)
            .addAction(0, "Start", open(context, id, "START"))
            .addAction(0, "Delay 10m", open(context, id, "DELAY"))
            .addAction(0, "Replan", open(context, id, "REPLAN")).build()
        try { notifications.notify(id, 1, notification) } catch (_: SecurityException) { /* Permission may be revoked between check and delivery. */ }
    }
}

class PlanReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                if (intent.action == "REMIND") PlanReminders.deliver(context, intent.getStringExtra("block_id").orEmpty())
                else if (intent.action == "SESSION_FINISHED") PlanReminders.sessionElapsed(context)
                else PlanReminders.sync(context, DatabaseProvider.get(context).workspaceDao().blocks().first())
            } catch (_: Exception) { /* Keep persisted plan/session available even if Android cannot schedule. */ }
            finally { pending.finish() }
        }
    }
}
