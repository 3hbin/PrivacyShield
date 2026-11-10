package com.privacyshield.app.data

import android.Manifest
import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Process

object SensitivePermission {
    const val CAMERA = Manifest.permission.CAMERA
    const val MICROPHONE = Manifest.permission.RECORD_AUDIO
    const val FINE_LOCATION = Manifest.permission.ACCESS_FINE_LOCATION
    const val COARSE_LOCATION = Manifest.permission.ACCESS_COARSE_LOCATION
    const val READ_SMS = Manifest.permission.READ_SMS
    const val READ_CONTACTS = Manifest.permission.READ_CONTACTS
    const val READ_CALL_LOG = Manifest.permission.READ_CALL_LOG
    const val READ_PHONE_STATE = Manifest.permission.READ_PHONE_STATE

    val ALL = setOf(
        CAMERA,
        MICROPHONE,
        FINE_LOCATION,
        COARSE_LOCATION,
        READ_SMS,
        READ_CONTACTS,
        READ_CALL_LOG,
        READ_PHONE_STATE
    )

    fun label(permission: String): String = when (permission) {
        CAMERA -> "Camera"
        MICROPHONE -> "Micrô"
        FINE_LOCATION -> "Vị trí chính xác"
        COARSE_LOCATION -> "Vị trí gần đúng"
        READ_SMS -> "Đọc tin nhắn"
        READ_CONTACTS -> "Danh bạ"
        READ_CALL_LOG -> "Nhật ký cuộc gọi"
        READ_PHONE_STATE -> "Trạng thái điện thoại"
        else -> permission.substringAfterLast('.')
    }
}

data class InstalledApp(
    val packageName: String,
    val label: String,
    val isSystem: Boolean,
    val launchable: Boolean,
    val grantedSensitive: Set<String>,
    val lastUsedMillis: Long?
) {
    val hasCamera: Boolean get() = SensitivePermission.CAMERA in grantedSensitive
    val hasMicrophone: Boolean get() = SensitivePermission.MICROPHONE in grantedSensitive
    val hasLocation: Boolean
        get() = SensitivePermission.FINE_LOCATION in grantedSensitive ||
            SensitivePermission.COARSE_LOCATION in grantedSensitive
}

data class RiskFinding(
    val app: InstalledApp,
    val reasons: List<String>
)

data class ScanResult(
    val apps: List<InstalledApp>,
    val scannedAtMillis: Long,
    val usageAccessGranted: Boolean
) {
    val userApps: List<InstalledApp> get() = apps.filter { !it.isSystem && it.launchable }
    val cameraOrMic: List<InstalledApp> get() = apps.filter { it.hasCamera || it.hasMicrophone }
    val locationApps: List<InstalledApp> get() = apps.filter { it.hasLocation }
    val findings: List<RiskFinding> get() = apps.mapNotNull { app ->
        val reasons = riskReasons(app)
        if (reasons.isEmpty()) null else RiskFinding(app, reasons)
    }.sortedByDescending { it.reasons.size }

    val needsAttention: Boolean get() = findings.isNotEmpty()
}

fun riskReasons(app: InstalledApp): List<String> {
    if (app.isSystem) return emptyList()
    val reasons = mutableListOf<String>()
    if (app.hasCamera && app.hasMicrophone && app.hasLocation) {
        reasons += "Được cấp đồng thời camera, micrô và vị trí"
    }
    if (SensitivePermission.READ_SMS in app.grantedSensitive) {
        reasons += "Được cấp quyền đọc tin nhắn"
    }
    if (SensitivePermission.READ_CALL_LOG in app.grantedSensitive) {
        reasons += "Được cấp quyền đọc nhật ký cuộc gọi"
    }
    if (SensitivePermission.READ_CONTACTS in app.grantedSensitive && (app.hasCamera || app.hasMicrophone)) {
        reasons += "Có quyền danh bạ cùng camera hoặc micrô"
    }
    return reasons
}

class AppScanner(private val context: Context) {
    fun hasUsageAccess(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun scan(): ScanResult {
        val pm = context.packageManager
        val packages = installedPackages(pm)
        val usage = if (hasUsageAccess()) loadUsage() else emptyMap()
        val apps = packages.mapNotNull { info -> toInstalledApp(pm, info, usage) }
            .sortedBy { it.label.lowercase() }
        return ScanResult(
            apps = apps,
            scannedAtMillis = System.currentTimeMillis(),
            usageAccessGranted = hasUsageAccess()
        )
    }

    private fun installedPackages(pm: PackageManager): List<PackageInfo> {
        val flags = PackageManager.GET_PERMISSIONS
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.getInstalledPackages(PackageManager.PackageInfoFlags.of(flags.toLong()))
        } else {
            @Suppress("DEPRECATION")
            pm.getInstalledPackages(flags)
        }
    }

    private fun toInstalledApp(
        pm: PackageManager,
        info: PackageInfo,
        usage: Map<String, Long>
    ): InstalledApp? {
        val appInfo = info.applicationInfo ?: return null
        val requested = info.requestedPermissions ?: emptyArray()
        val grantedFlags = info.requestedPermissionsFlags
        val granted = mutableSetOf<String>()
        requested.forEachIndexed { index, permission ->
            if (permission !in SensitivePermission.ALL) return@forEachIndexed
            val flag = grantedFlags?.getOrNull(index) ?: 0
            if (flag and PackageInfo.REQUESTED_PERMISSION_GRANTED != 0) {
                granted += permission
            }
        }
        val isSystem = appInfo.flags and ApplicationInfo.FLAG_SYSTEM != 0
        return InstalledApp(
            packageName = info.packageName,
            label = appInfo.loadLabel(pm).toString(),
            isSystem = isSystem,
            launchable = pm.getLaunchIntentForPackage(info.packageName) != null,
            grantedSensitive = granted,
            lastUsedMillis = usage[info.packageName]
        )
    }

    private fun loadUsage(): Map<String, Long> {
        val manager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val end = System.currentTimeMillis()
        val start = end - 30L * 24L * 60L * 60L * 1000L
        val stats = manager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, start, end)
            ?: return emptyMap()
        return stats.groupBy { it.packageName }
            .mapValues { (_, items) -> items.maxOf { it.lastTimeUsed } }
            .filterValues { it > 0L }
    }
}
