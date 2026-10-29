package com.example.data.repository

import android.content.Context
import androidx.room.withTransaction
import com.example.data.local.LifeOsDatabase
import com.example.data.model.*
import org.json.JSONArray
import org.json.JSONObject

class AcademicStore(private val context: Context, private val db: LifeOsDatabase) {
    private val prefs = context.getSharedPreferences("academic_s1_2026", Context.MODE_PRIVATE)
    private fun JSONArray.strings() = (0 until length()).map { getString(it) }
    private fun <T> JSONArray.mapObjects(read: (JSONObject) -> T) = (0 until length()).map { read(getJSONObject(it)) }
    fun load(): AcademicPack {
        val json = context.assets.open("academic/catalog.json").bufferedReader(Charsets.UTF_8).use { JSONObject(it.readText()) }
        val pack = AcademicPack(json.getString("version"), json.getString("title"), json.getString("preparationEnd"),
            json.getJSONArray("sources").mapObjects { AcademicSource(it.getString("id"), it.getString("asset"), it.getInt("pages"), it.getString("sha256")) },
            json.getJSONArray("courses").mapObjects { SyllabusCourse(it.getString("id"), it.getString("code"), it.getString("title"), it.getString("short"),
                it.getString("hours"), it.getInt("credits"), it.getString("assessment"), it.getString("note")) },
            json.getJSONArray("items").mapObjects { SyllabusItem(it.getString("id"), it.getString("course"), it.getString("module"), it.getString("moduleKey"),
                it.getInt("hours"), it.getInt("page"), it.getString("scope"), it.getString("title"), it.getString("detail"), it.getString("source"), it.getString("firstStep")) },
            json.getJSONArray("events").mapObjects { AcademicEvent(it.getString("id"), it.getString("title"), it.getInt("semester"), it.getString("kind"), it.getJSONArray("dates").strings(), it.getInt("page")) },
            json.getJSONArray("notices").strings())
        require(pack.items.map { it.id }.distinct().size == pack.items.size)
        require(pack.items.all { item -> pack.courses.any { it.id == item.course } && pack.sources.any { it.id == item.source && item.page in 1..it.pages } })
        pack.events.forEach { event -> event.dates.forEach { date -> AcademicDates.millis(date) } }
        return pack
    }
    suspend fun install(pack: AcademicPack, force: Boolean = false) {
        if (!force && (prefs.getBoolean("installed:${pack.version}", false) || prefs.getBoolean("cleared", false))) return
        db.withTransaction {
            pack.items.forEach { addItem(pack, it) }
        }
        check(prefs.edit().putBoolean("installed:${pack.version}", true).commit()) { "Syllabus installed; could not save its import marker. A retry is safe." }
    }
    suspend fun addItem(pack: AcademicPack, item: SyllabusItem) {
        val course = pack.courses.first { it.id == item.course }
        if (item.scope == "CODE") db.workspaceDao().insertAcademicAttempt(CodingAttempt(item.id, item.title, item.material,
            "Syllabus lab · ${course.code}", createdAt = 0L))
        else db.workspaceDao().insertAcademicTopic(ExamTopic(item.id, course.short, item.title, item.material))
    }
    fun readRoadmap(): AcademicRoadmap? = try {
        if (prefs.getBoolean("cleared", false)) AcademicRoadmap() else prefs.getString("roadmap", null)?.let { raw ->
            val json = JSONObject(raw)
            AcademicRoadmap(json.getJSONArray("targets").mapObjects {
                val date = it.getString("date"); AcademicDates.millis(date)
                val minutes = it.getInt("minutes"); require(minutes in 1..180)
                AcademicTarget(it.getString("id"), date, it.getString("item"), minutes, it.getString("mode"))
            }, json.getInt("dailyMinutes").coerceIn(10, 180),
                json.optJSONArray("covered")?.strings()?.toSet().orEmpty(), json.optJSONArray("done")?.strings()?.toSet().orEmpty(),
                json.optJSONArray("modules")?.strings()?.toSet().orEmpty())
        }
    } catch (_: Exception) { null }
    fun saveRoadmap(value: AcademicRoadmap) {
        val targets = JSONArray()
        value.targets.forEach { targets.put(JSONObject().put("id", it.id).put("date", it.date).put("item", it.itemId).put("minutes", it.minutes).put("mode", it.mode)) }
        val json = JSONObject().put("dailyMinutes", value.dailyMinutes).put("targets", targets)
            .put("covered", JSONArray(value.covered.sorted())).put("done", JSONArray(value.done.sorted())).put("modules", JSONArray(value.modules.sorted()))
        check(prefs.edit().putString("roadmap", json.toString()).putBoolean("cleared", false).commit()) { "Could not save the preparation roadmap. Try again." }
    }
    fun clearRoadmap() { check(prefs.edit().remove("roadmap").putBoolean("cleared", true).commit()) }
}
