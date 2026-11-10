package com.privacyshield.app

import android.Manifest
import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.privacyshield.app.data.ActivityLog
import com.privacyshield.app.data.AppScanner
import com.privacyshield.app.data.ScanResult
import com.privacyshield.app.data.ShieldPreferences
import com.privacyshield.app.service.SensorMonitor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface ScanState {
    data object Idle : ScanState
    data object Loading : ScanState
    data class Ready(val result: ScanResult) : ScanState
    data class Failed(val message: String) : ScanState
}

class ShieldViewModel(application: Application) : AndroidViewModel(application) {
    private val scanner = AppScanner(application)
    val preferences = ShieldPreferences.get(application)
    private val log = ActivityLog(application)

    init {
        SensorMonitor.start(application)
    }

    private val _scan = MutableStateFlow<ScanState>(ScanState.Idle)
    val scan: StateFlow<ScanState> = _scan.asStateFlow()

    fun refresh() {
        if (_scan.value is ScanState.Loading) return
        viewModelScope.launch(Dispatchers.IO) {
            _scan.value = ScanState.Loading
            _scan.value = try {
                val result = scanner.scan()
                log.add("SCAN", "Đã quét ${result.apps.size} ứng dụng, ${result.findings.size} cần chú ý")
                maybeNotify(result)
                ScanState.Ready(result)
            } catch (error: SecurityException) {
                ScanState.Failed("Không đủ quyền để đọc danh sách ứng dụng.")
            } catch (error: Exception) {
                ScanState.Failed(error.message ?: "Không quét được thiết bị.")
            }
        }
    }

    fun hasUsageAccess(): Boolean = scanner.hasUsageAccess()

    private fun maybeNotify(result: ScanResult) {
        val context = getApplication<Application>()
        if (!preferences.settings.value.notificationsEnabled) return
        if (result.findings.isEmpty()) {
            preferences.setLastNotifiedFindings(emptySet())
            return
        }
        val current = result.findings.map { it.app.packageName }.toSet()
        val previous = preferences.lastNotifiedFindings()
        val fresh = current - previous
        preferences.setLastNotifiedFindings(current)
        if (fresh.isEmpty()) return
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Cảnh báo quyền riêng tư",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            manager.createNotificationChannel(channel)
        }
        val open = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_lock)
            .setContentTitle("Privacy Shield")
            .setContentText("${fresh.size} ứng dụng mới có tổ hợp quyền đáng chú ý")
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(NOTICE_ID, notification)
    }

    companion object {
        const val CHANNEL_ID = "shield_alerts"
        const val NOTICE_ID = 42
    }
}
