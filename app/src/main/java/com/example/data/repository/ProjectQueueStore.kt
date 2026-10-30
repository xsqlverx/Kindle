package com.example.data.repository

import android.content.Context
import androidx.room.withTransaction
import com.example.data.local.LifeOsDatabase
import com.example.data.model.*
import org.json.JSONObject

class ProjectQueueStore(private val context: Context, private val db: LifeOsDatabase) {
    private val prefs = context.getSharedPreferences("github_project_queue", Context.MODE_PRIVATE)
    fun load(): List<ProjectSuggestion> {
        val rows = context.assets.open("project_queue.json").bufferedReader(Charsets.UTF_8).use { JSONObject(it.readText()).getJSONArray("projects") }
        return (0 until rows.length()).map { index -> rows.getJSONObject(index).let { row ->
            val steps = row.getJSONArray("steps")
            ProjectSuggestion(row.getString("id"), row.getString("name"), row.getString("repo"), row.getString("revision"),
                row.getString("evidence"), row.getString("why"), row.getString("goal"), (0 until steps.length()).map { steps.getString(it) })
        } }
    }
    suspend fun install(items: List<ProjectSuggestion>, force: Boolean = false) {
        if (!force && prefs.getBoolean("installed_or_cleared", false)) return
        db.withTransaction {
            val dao = db.workspaceDao()
            val existing = dao.projectsSnapshot()
            items.forEach { item ->
                val alreadyPresent = existing.any { project -> project.id == item.id || project.goal.lines().any { line -> EntryRules.repository(line)?.equals(item.repo, ignoreCase = true) == true } }
                if (!alreadyPresent) {
                    dao.insertSuggestedProject(WorkProject(item.id, item.name, item.goal + "\nhttps://github.com/${item.repo}"))
                    item.steps.forEachIndexed { index, action -> dao.insertSuggestedStep(ProjectStep("${item.id}:$index", item.id, action, index)) }
                }
            }
        }
        check(prefs.edit().putBoolean("installed_or_cleared", true).commit())
    }
    fun retainWipeIntent() { check(prefs.edit().putBoolean("installed_or_cleared", true).commit()) }
}
