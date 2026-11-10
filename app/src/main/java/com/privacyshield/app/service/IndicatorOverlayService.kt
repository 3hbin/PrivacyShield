package com.privacyshield.app.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import com.privacyshield.app.data.ShieldPreferences
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.MotionEvent

/**
 * Floating privacy indicators.
 *
 * On Android 8/9 there is no public API that reports whether camera, microphone or location
 * is currently being used by another app. The dots therefore only appear when the foreground
 * app has the corresponding permission granted. This is permission presence, not live sensor use.
 */
class IndicatorOverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private val dots = mutableListOf<DotView>()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        if (Settings.canDrawOverlays(this)) {
            showDots()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_UPDATE) {
            updateVisibility()
        } else if (Settings.canDrawOverlays(this)) {
            showDots()
            updateVisibility()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        removeDots()
        super.onDestroy()
    }

    private fun showDots() {
        if (dots.isNotEmpty()) return
        val prefs = ShieldPreferences.get(this).settings.value.indicators
        if (!prefs.enabled) return

        val types = listOf(
            Triple("camera", 0xFF4CAF50.toInt(), prefs.showCamera),
            Triple("mic", 0xFFFF9800.toInt(), prefs.showMic),
            Triple("location", 0xFF2196F3.toInt(), prefs.showLocation)
        )
        for ((id, color, show) in types) {
            if (!show) continue
            val view = createDot(color, prefs.sizeDp, prefs.opacity, id)
            val params = createParams(id, prefs)
            view.visibility = View.GONE // hidden until updateVisibility() decides
            try {
                windowManager.addView(view, params)
                dots += DotView(id, view, params)
            } catch (_: Exception) {
                // Overlay permission may have been revoked
            }
        }
    }

    private fun dotSizePx(sizeDp: Float): Int =
        TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, sizeDp.coerceIn(6f, 48f), resources.displayMetrics
        ).toInt().coerceAtLeast(8)

    private fun createDot(color: Int, sizeDp: Float, opacity: Float, id: String): View {
        val sizePx = dotSizePx(sizeDp)
        val view = View(this)
        val drawable = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(color)
            alpha = (opacity * 255).toInt().coerceIn(80, 255)
        }
        view.background = drawable
        view.layoutParams = FrameLayout.LayoutParams(sizePx, sizePx)

        var startX = 0
        var startY = 0
        var touchStartX = 0f
        var touchStartY = 0f
        view.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    val p = dots.find { it.id == id }?.params
                    startX = p?.x ?: 0
                    startY = p?.y ?: 0
                    touchStartX = event.rawX
                    touchStartY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - touchStartX).toInt()
                    val dy = (event.rawY - touchStartY).toInt()
                    val params = dots.find { it.id == id }?.params ?: return@setOnTouchListener true
                    params.x = startX + dx
                    params.y = startY + dy
                    try { windowManager.updateViewLayout(view, params) } catch (_: Exception) {}
                    true
                }
                MotionEvent.ACTION_UP -> {
                    savePosition(id, view)
                    true
                }
                else -> false
            }
        }
        return view
    }

    private fun createParams(id: String, prefs: com.privacyshield.app.data.IndicatorSettings): WindowManager.LayoutParams {
        val (fx, fy) = when (id) {
            "camera" -> prefs.cameraX to prefs.cameraY
            "mic" -> prefs.micX to prefs.micY
            else -> prefs.locX to prefs.locY
        }
        val metrics = resources.displayMetrics
        val x = (fx * metrics.widthPixels).toInt()
        val y = (fy * metrics.heightPixels).toInt()
        val sizePx = dotSizePx(prefs.sizeDp)
        return WindowManager.LayoutParams(
            sizePx,
            sizePx,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            this.x = x
            this.y = y
        }
    }

    private fun updateVisibility() {
        val prefs = ShieldPreferences.get(this).settings.value.indicators
        if (!prefs.enabled) {
            removeDots()
            return
        }
        if (dots.isEmpty() && Settings.canDrawOverlays(this)) {
            showDots()
        }
        val fg = CurrentForeground.packageName
        val hasCam = fg != null && hasPermission(fg, android.Manifest.permission.CAMERA)
        val hasMic = fg != null && hasPermission(fg, android.Manifest.permission.RECORD_AUDIO)
        val hasLoc = fg != null && (
            hasPermission(fg, android.Manifest.permission.ACCESS_FINE_LOCATION) ||
                hasPermission(fg, android.Manifest.permission.ACCESS_COARSE_LOCATION)
            )

        for (dot in dots) {
            val visible = when (dot.id) {
                "camera" -> prefs.showCamera && hasCam
                "mic" -> prefs.showMic && hasMic
                else -> prefs.showLocation && hasLoc
            }
            dot.view.visibility = if (visible) View.VISIBLE else View.GONE
            // Update position in case user changed settings
            val newParams = createParams(dot.id, prefs)
            dot.params.x = newParams.x
            dot.params.y = newParams.y
            dot.params.width = newParams.width
            dot.params.height = newParams.height
            try {
                windowManager.updateViewLayout(dot.view, dot.params)
            } catch (_: Exception) { }
        }
    }

    private fun savePosition(id: String, view: View) {
        val params = dots.find { it.id == id }?.params ?: return
        val metrics = resources.displayMetrics
        val fx = (params.x.toFloat() / metrics.widthPixels).coerceIn(0f, 0.95f)
        val fy = (params.y.toFloat() / metrics.heightPixels).coerceIn(0f, 0.9f)
        val prefs = ShieldPreferences.get(this)
        prefs.updateSettings { s ->
            val ind = s.indicators
            s.copy(indicators = when (id) {
                "camera" -> ind.copy(cameraX = fx, cameraY = fy)
                "mic" -> ind.copy(micX = fx, micY = fy)
                else -> ind.copy(locX = fx, locY = fy)
            })
        }
    }

    private fun hasPermission(pkg: String, permission: String): Boolean {
        return try {
            packageManager.checkPermission(permission, pkg) == PackageManager.PERMISSION_GRANTED
        } catch (_: Exception) {
            false
        }
    }

    private fun removeDots() {
        for (dot in dots) {
            try { windowManager.removeView(dot.view) } catch (_: Exception) { }
        }
        dots.clear()
    }

    private data class DotView(val id: String, val view: View, val params: WindowManager.LayoutParams)

    companion object {
        const val ACTION_UPDATE = "com.privacyshield.app.INDICATOR_UPDATE"

        fun requestUpdate(context: Context) {
            if (!Settings.canDrawOverlays(context)) return
            val prefs = ShieldPreferences.get(context).settings.value.indicators
            if (!prefs.enabled) return
            context.startService(Intent(context, IndicatorOverlayService::class.java).setAction(ACTION_UPDATE))
        }

        fun startIfEnabled(context: Context) {
            val prefs = ShieldPreferences.get(context).settings.value.indicators
            if (prefs.enabled && Settings.canDrawOverlays(context)) {
                context.startService(Intent(context, IndicatorOverlayService::class.java))
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, IndicatorOverlayService::class.java))
        }
    }
}
