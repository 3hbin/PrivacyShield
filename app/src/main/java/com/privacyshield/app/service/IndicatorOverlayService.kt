package com.privacyshield.app.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.PixelFormat
import android.graphics.PorterDuff
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.LinearLayout
import com.privacyshield.app.R
import com.privacyshield.app.data.IndicatorSettings
import com.privacyshield.app.data.ShieldPreferences

/**
 * Small privacy dots pinned to a screen corner, like the Android 12 indicators.
 *
 *  - Camera and microphone dots show only while the sensor is really in use
 *    (see [SensorMonitor]).
 *  - The location dot is optional and only reflects that the app on screen holds a location
 *    permission. Android gives third-party apps no public signal for live location use.
 *
 * The overlay is not touchable, so it never blocks the app underneath.
 */
class IndicatorOverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private val handler = Handler(Looper.getMainLooper())
    private var container: LinearLayout? = null
    private val dots = mutableMapOf<String, View>()
    private var previewUntil = 0L

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        instance = this
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        SensorMonitor.start(this)
        rebuild()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        handle(intent?.action)
        return START_STICKY
    }

    override fun onDestroy() {
        instance = null
        handler.removeCallbacksAndMessages(null)
        removeAll()
        super.onDestroy()
    }

    private fun handle(action: String?) {
        when (action) {
            ACTION_UPDATE -> updateVisibility()
            ACTION_PREVIEW -> {
                previewUntil = System.currentTimeMillis() + PREVIEW_MS
                updateVisibility()
                handler.postDelayed({ updateVisibility() }, PREVIEW_MS + 100)
            }
            else -> rebuild()
        }
    }

    // ---- building ------------------------------------------------------------------------

    private fun rebuild() {
        removeAll()
        if (!Settings.canDrawOverlays(this)) return
        val prefs = ShieldPreferences.get(this).settings.value.indicators
        if (!prefs.enabled) return

        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val gap = dp(4f)
        fun add(id: String, color: Int, show: Boolean) {
            if (!show) return
            val dot = createDot(id, color, prefs)
            val lp = LinearLayout.LayoutParams(dotSizePx(prefs), dotSizePx(prefs)).apply {
                marginStart = gap
                marginEnd = gap
            }
            row.addView(dot, lp)
            dots[id] = dot
        }
        add(ID_CAMERA, prefs.cameraColor, prefs.showCamera)
        add(ID_MIC, prefs.micColor, prefs.showMic)
        add(ID_LOCATION, prefs.locColor, prefs.showLocation)
        if (dots.isEmpty()) return

        try {
            windowManager.addView(row, createParams(prefs))
            container = row
        } catch (_: Exception) {
            dots.clear() // overlay permission may have been revoked
            return
        }
        updateVisibility()
    }

    private fun dotSizePx(prefs: IndicatorSettings): Int {
        val factor = if (prefs.showIcons) ICON_SCALE else 1f
        return dp((prefs.sizeDp * factor).coerceIn(8f, 64f))
    }

    private fun createDot(id: String, color: Int, prefs: IndicatorSettings): View {
        val size = dotSizePx(prefs)
        val view = ImageView(this)
        if (prefs.showIcons) {
            view.background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(0xE6121417.toInt())
                setStroke(dp(1.5f).coerceAtLeast(1), color)
            }
            view.setImageResource(
                when (id) {
                    ID_CAMERA -> R.drawable.ic_dot_camera
                    ID_MIC -> R.drawable.ic_dot_mic
                    else -> R.drawable.ic_dot_location
                }
            )
            view.setColorFilter(color, PorterDuff.Mode.SRC_IN)
            val pad = (size * 0.24f).toInt()
            view.setPadding(pad, pad, pad, pad)
            view.scaleType = ImageView.ScaleType.FIT_CENTER
        } else {
            view.background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(color)
                setStroke(dp(1f).coerceAtLeast(1), 0x66000000)
            }
        }
        view.alpha = prefs.opacity.coerceIn(0.3f, 1f)
        view.visibility = View.GONE
        return view
    }

    private fun createParams(prefs: IndicatorSettings): WindowManager.LayoutParams {
        val size = dotSizePx(prefs)
        val top = prefs.corner == IndicatorSettings.CORNER_TOP_LEFT ||
            prefs.corner == IndicatorSettings.CORNER_TOP_RIGHT
        val left = prefs.corner == IndicatorSettings.CORNER_TOP_LEFT ||
            prefs.corner == IndicatorSettings.CORNER_BOTTOM_LEFT
        val statusBar = systemDimen("status_bar_height", dp(24f))
        val navBar = systemDimen("navigation_bar_height", dp(48f))

        return WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = (if (top) Gravity.TOP else Gravity.BOTTOM) or
                (if (left) Gravity.START else Gravity.END)
            x = dp(6f)
            y = if (top) ((statusBar - size) / 2).coerceAtLeast(dp(2f)) else navBar + dp(12f)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }
    }

    // ---- state ---------------------------------------------------------------------------

    private fun updateVisibility() {
        val prefs = ShieldPreferences.get(this).settings.value.indicators
        if (!prefs.enabled) {
            removeAll()
            return
        }
        val row = container
        if (row == null) {
            rebuild()
            return
        }
        val preview = System.currentTimeMillis() < previewUntil
        var any = false
        fun set(id: String, visible: Boolean) {
            val dot = dots[id] ?: return
            dot.visibility = if (visible || preview) View.VISIBLE else View.GONE
            if (dot.visibility == View.VISIBLE) any = true
        }
        set(ID_CAMERA, SensorMonitor.cameraActive)
        set(ID_MIC, SensorMonitor.micActive)
        set(ID_LOCATION, foregroundHasLocation())
        row.visibility = if (any) View.VISIBLE else View.GONE
    }

    private fun foregroundHasLocation(): Boolean {
        val pkg = CurrentForeground.packageName ?: return false
        if (pkg == packageName) return false
        return hasPermission(pkg, android.Manifest.permission.ACCESS_FINE_LOCATION) ||
            hasPermission(pkg, android.Manifest.permission.ACCESS_COARSE_LOCATION)
    }

    private fun hasPermission(pkg: String, permission: String): Boolean = try {
        packageManager.checkPermission(permission, pkg) == PackageManager.PERMISSION_GRANTED
    } catch (_: Exception) {
        false
    }

    private fun removeAll() {
        container?.let { runCatching { windowManager.removeView(it) } }
        container = null
        dots.clear()
    }

    private fun dp(value: Float): Int = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP, value, resources.displayMetrics
    ).toInt()

    private fun systemDimen(name: String, fallback: Int): Int {
        val id = resources.getIdentifier(name, "dimen", "android")
        return if (id > 0) resources.getDimensionPixelSize(id) else fallback
    }

    companion object {
        const val ACTION_UPDATE = "com.privacyshield.app.INDICATOR_UPDATE"
        const val ACTION_REBUILD = "com.privacyshield.app.INDICATOR_REBUILD"
        const val ACTION_PREVIEW = "com.privacyshield.app.INDICATOR_PREVIEW"

        private const val ID_CAMERA = "camera"
        private const val ID_MIC = "mic"
        private const val ID_LOCATION = "location"
        private const val ICON_SCALE = 1.7f
        private const val PREVIEW_MS = 4_000L

        @Volatile
        private var instance: IndicatorOverlayService? = null

        fun requestUpdate(context: Context) = dispatch(context, ACTION_UPDATE)

        /** Re-reads settings and redraws the dots. */
        fun requestRebuild(context: Context) = dispatch(context, ACTION_REBUILD)

        /** Shows every enabled dot for a few seconds so the person can see how it looks. */
        fun preview(context: Context) = dispatch(context, ACTION_PREVIEW)

        fun startIfEnabled(context: Context) {
            SensorMonitor.start(context)
            val prefs = ShieldPreferences.get(context).settings.value.indicators
            if (prefs.enabled && Settings.canDrawOverlays(context)) {
                runCatching { context.startService(Intent(context, IndicatorOverlayService::class.java)) }
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, IndicatorOverlayService::class.java))
        }

        private fun dispatch(context: Context, action: String) {
            if (!Settings.canDrawOverlays(context)) return
            val prefs = ShieldPreferences.get(context).settings.value.indicators
            if (!prefs.enabled) return
            val running = instance
            if (running != null) {
                running.handler.post { running.handle(action) }
                return
            }
            runCatching {
                context.startService(Intent(context, IndicatorOverlayService::class.java).setAction(action))
            }
        }
    }
}
