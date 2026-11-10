package com.privacyshield.app.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * One camera or microphone session seen on the device.
 *
 * [certainty] says how sure we are about [packageName]:
 *  - CERT_FOREGROUND: the app that was on screen and holds the permission
 *  - CERT_CANDIDATE: not on screen, but holds the permission and has a foreground service running
 *  - CERT_UNKNOWN: Android does not tell third-party apps who opened the sensor
 */
data class AccessEvent(
    val id: Long,
    val kind: String,
    val packageName: String?,
    val label: String,
    val startMillis: Long,
    /** 0 = still going on, -1 = ended but the time was not seen, otherwise end time. */
    val endMillis: Long,
    val certainty: Int
) {
    companion object {
        const val KIND_CAMERA = "camera"
        const val KIND_MIC = "mic"
        const val CERT_UNKNOWN = 0
        const val CERT_CANDIDATE = 1
        const val CERT_FOREGROUND = 2
    }
}

/** Local, encrypted history of sensor sessions. Never leaves the device. */
class SensorHistory(context: Context) {
    private val prefs = ShieldPreferences.get(context)

    fun list(): List<AccessEvent> = synchronized(LOCK) { read() }

    fun add(event: AccessEvent) = synchronized(LOCK) {
        write((read() + event).takeLast(MAX))
    }

    fun update(id: Long, transform: (AccessEvent) -> AccessEvent) = synchronized(LOCK) {
        write(read().map { if (it.id == id) transform(it) else it })
    }

    fun remove(id: Long) = synchronized(LOCK) {
        write(read().filterNot { it.id == id })
    }

    fun clear() = synchronized(LOCK) {
        prefs.putSecureString(KEY, null)
    }

    private fun read(): List<AccessEvent> {
        val raw = prefs.secureString(KEY) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.getJSONObject(index)
                    add(
                        AccessEvent(
                            id = item.optLong("id"),
                            kind = item.optString("k"),
                            packageName = if (item.isNull("p")) null else item.optString("p"),
                            label = item.optString("l"),
                            startMillis = item.optLong("s"),
                            endMillis = item.optLong("e"),
                            certainty = item.optInt("c")
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    private fun write(events: List<AccessEvent>) {
        val array = JSONArray()
        events.forEach { e ->
            array.put(
                JSONObject()
                    .put("id", e.id)
                    .put("k", e.kind)
                    .put("p", e.packageName ?: JSONObject.NULL)
                    .put("l", e.label)
                    .put("s", e.startMillis)
                    .put("e", e.endMillis)
                    .put("c", e.certainty)
            )
        }
        prefs.putSecureString(KEY, array.toString())
    }

    companion object {
        private const val KEY = "sensor_history"
        private const val MAX = 300
        private val LOCK = Any()
    }
}
