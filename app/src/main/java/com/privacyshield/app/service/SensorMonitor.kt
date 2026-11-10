package com.privacyshield.app.service

import android.Manifest
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.media.AudioRecordingConfiguration
import android.os.Build
import android.os.Handler
import android.os.Looper
import com.privacyshield.app.data.AccessEvent
import com.privacyshield.app.data.SensorHistory

/**
 * Watches real camera and microphone use through public system callbacks:
 *  - CameraManager.AvailabilityCallback: a camera becomes "unavailable" when any app opens it
 *  - AudioManager.AudioRecordingCallback: lists active recordings from any app
 *
 * Android does not tell third-party apps WHICH app opened the sensor, so the app shown in the
 * history is a best-effort guess (see [resolve]). The timing itself is real.
 */
object SensorMonitor {

    @Volatile
    var cameraActive = false
        private set

    @Volatile
    var micActive = false
        private set

    private var started = false
    private lateinit var appContext: Context
    private val handler = Handler(Looper.getMainLooper())
    private val unavailableCameras = mutableSetOf<String>()
    private val currentEventId = mutableMapOf<String, Long>()

    @Synchronized
    fun start(context: Context) {
        if (started) return
        appContext = context.applicationContext
        started = true
        closeStaleEvents()
        runCatching { registerCamera() }
        runCatching { registerMic() }
    }

    private fun registerCamera() {
        val manager = appContext.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        manager.registerAvailabilityCallback(object : CameraManager.AvailabilityCallback() {
            override fun onCameraAvailable(cameraId: String) {
                unavailableCameras.remove(cameraId)
                if (unavailableCameras.isEmpty()) setActive(AccessEvent.KIND_CAMERA, false)
            }

            override fun onCameraUnavailable(cameraId: String) {
                val wasIdle = unavailableCameras.isEmpty()
                unavailableCameras.add(cameraId)
                if (wasIdle) setActive(AccessEvent.KIND_CAMERA, true)
            }
        }, handler)
    }

    private fun registerMic() {
        val manager = appContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        manager.registerAudioRecordingCallback(object : AudioManager.AudioRecordingCallback() {
            override fun onRecordingConfigChanged(configs: MutableList<AudioRecordingConfiguration>?) {
                // Sources >= 1997 are system-internal (hotword, echo reference, radio tuner).
                val active = configs.orEmpty().any { it.clientAudioSource < INTERNAL_AUDIO_SOURCE }
                setActive(AccessEvent.KIND_MIC, active)
            }
        }, handler)
    }

    private fun setActive(kind: String, active: Boolean) {
        val current = if (kind == AccessEvent.KIND_CAMERA) cameraActive else micActive
        if (current == active) return
        if (kind == AccessEvent.KIND_CAMERA) cameraActive = active else micActive = active
        runCatching { if (active) beginEvent(kind) else endEvent(kind) }
        IndicatorOverlayService.requestUpdate(appContext)
    }

    // ---- history -------------------------------------------------------------------------

    private fun closeStaleEvents() {
        runCatching {
            val history = SensorHistory(appContext)
            history.list().filter { it.endMillis == 0L }.forEach { stale ->
                history.update(stale.id) { it.copy(endMillis = -1L) }
            }
        }
    }

    private fun beginEvent(kind: String) {
        val now = System.currentTimeMillis()
        val history = SensorHistory(appContext)
        val who = resolve(kind)

        // Apps often release and re-open a sensor within a moment; keep that as one session.
        val last = history.list().lastOrNull { it.kind == kind }
        if (last != null && last.endMillis > 0 &&
            now - last.endMillis < MERGE_WINDOW_MS && last.packageName == who.packageName
        ) {
            history.update(last.id) { it.copy(endMillis = 0L) }
            currentEventId[kind] = last.id
            return
        }

        val id = now * 10 + if (kind == AccessEvent.KIND_CAMERA) 1 else 2
        history.add(
            AccessEvent(
                id = id,
                kind = kind,
                packageName = who.packageName,
                label = who.label,
                startMillis = now,
                endMillis = 0L,
                certainty = who.certainty
            )
        )
        currentEventId[kind] = id
    }

    private fun endEvent(kind: String) {
        val id = currentEventId.remove(kind) ?: return
        val now = System.currentTimeMillis()
        val history = SensorHistory(appContext)
        val event = history.list().firstOrNull { it.id == id } ?: return
        if (now - event.startMillis < MIN_SESSION_MS) {
            history.remove(id) // face unlock, sensor probes and similar blips
        } else {
            history.update(id) { it.copy(endMillis = now) }
        }
    }

    // ---- who was it? ---------------------------------------------------------------------

    private class Culprit(val packageName: String?, val label: String, val certainty: Int)

    private fun resolve(kind: String): Culprit {
        val permission = if (kind == AccessEvent.KIND_CAMERA) {
            Manifest.permission.CAMERA
        } else {
            Manifest.permission.RECORD_AUDIO
        }

        val foreground = CurrentForeground.packageName
        if (foreground != null && foreground != appContext.packageName &&
            foreground !in IGNORED_PACKAGES && holds(foreground, permission)
        ) {
            return Culprit(foreground, labelOf(foreground), AccessEvent.CERT_FOREGROUND)
        }

        val background = backgroundCandidate(permission)
        if (background != null) {
            return Culprit(background, labelOf(background), AccessEvent.CERT_CANDIDATE)
        }
        return Culprit(null, "Không xác định", AccessEvent.CERT_UNKNOWN)
    }

    /**
     * An app that is not on screen can still record from a foreground service. Usage access
     * (already declared by this app) lets us see which apps started a foreground service and
     * have not stopped it. Needs Android 10+ and the user to grant usage access.
     */
    private fun backgroundCandidate(permission: String): String? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
        return runCatching {
            val usage = appContext.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
            val end = System.currentTimeMillis()
            val events = usage.queryEvents(end - CANDIDATE_WINDOW_MS, end)
            val event = UsageEvents.Event()
            val serviceStarted = HashMap<String, Long>()
            val stopped = HashMap<String, Long>()
            while (events.hasNextEvent()) {
                events.getNextEvent(event)
                val pkg = event.packageName ?: continue
                when (event.eventType) {
                    UsageEvents.Event.FOREGROUND_SERVICE_START -> serviceStarted[pkg] = event.timeStamp
                    UsageEvents.Event.FOREGROUND_SERVICE_STOP -> stopped[pkg] = event.timeStamp
                }
            }
            serviceStarted.entries
                .filter { (pkg, startedAt) ->
                    (stopped[pkg] ?: 0L) < startedAt &&
                        pkg != appContext.packageName &&
                        pkg !in IGNORED_PACKAGES &&
                        holds(pkg, permission)
                }
                .maxByOrNull { it.value }
                ?.key
        }.getOrNull()
    }

    private fun holds(pkg: String, permission: String): Boolean = runCatching {
        appContext.packageManager.checkPermission(permission, pkg) == PackageManager.PERMISSION_GRANTED
    }.getOrDefault(false)

    private fun labelOf(pkg: String): String = runCatching {
        val pm = appContext.packageManager
        pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
    }.getOrDefault(pkg)

    private const val INTERNAL_AUDIO_SOURCE = 1997
    private const val MERGE_WINDOW_MS = 3_000L
    private const val MIN_SESSION_MS = 700L
    private const val CANDIDATE_WINDOW_MS = 12L * 60 * 60 * 1000
    private val IGNORED_PACKAGES = setOf("com.android.systemui")
}
