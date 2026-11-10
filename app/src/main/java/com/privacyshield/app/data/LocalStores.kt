package com.privacyshield.app.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class LogEntry(
    val timeMillis: Long,
    val kind: String,
    val message: String
)

class ActivityLog(context: Context) {
    private val prefs = ShieldPreferences.get(context)

    fun add(kind: String, message: String) {
        val next = (list() + LogEntry(System.currentTimeMillis(), kind, message.take(180)))
            .takeLast(MAX)
        save(next)
    }

    fun list(): List<LogEntry> {
        val raw = prefs.secureString(KEY) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.getJSONObject(index)
                    add(
                        LogEntry(
                            timeMillis = item.optLong("t"),
                            kind = item.optString("k"),
                            message = item.optString("m")
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    fun clear() {
        prefs.putSecureString(KEY, null)
    }

    private fun save(entries: List<LogEntry>) {
        val array = JSONArray()
        entries.forEach { entry ->
            array.put(
                JSONObject()
                    .put("t", entry.timeMillis)
                    .put("k", entry.kind)
                    .put("m", entry.message)
            )
        }
        prefs.putSecureString(KEY, array.toString())
    }

    companion object {
        private const val KEY = "activity_log"
        const val MAX = 200
    }
}

data class VaultNote(
    val id: String,
    val title: String,
    val body: String,
    val updatedMillis: Long
)

class SecureVault(context: Context) {
    private val prefs = ShieldPreferences.get(context)

    fun list(): List<VaultNote> {
        val raw = prefs.secureString(KEY) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.getJSONObject(index)
                    add(
                        VaultNote(
                            id = item.optString("id"),
                            title = item.optString("title"),
                            body = item.optString("body"),
                            updatedMillis = item.optLong("updated")
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    fun save(note: VaultNote) {
        val next = list().filterNot { it.id == note.id } + note
        write(next)
    }

    fun delete(id: String) {
        write(list().filterNot { it.id == id })
    }

    fun wipe() {
        prefs.putSecureString(KEY, null)
    }

    private fun write(notes: List<VaultNote>) {
        val array = JSONArray()
        notes.forEach { note ->
            array.put(
                JSONObject()
                    .put("id", note.id)
                    .put("title", note.title)
                    .put("body", note.body)
                    .put("updated", note.updatedMillis)
            )
        }
        prefs.putSecureString(KEY, array.toString())
    }

    companion object {
        private const val KEY = "vault_notes"
    }
}
