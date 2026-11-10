package com.privacyshield.app.data

import android.app.KeyguardManager
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.biometric.BiometricManager
import androidx.core.content.ContextCompat
import com.privacyshield.app.admin.ShieldDeviceAdminReceiver
import com.privacyshield.app.ui.screens.isAccessibilityEnabled

data class ProtectionItem(
    val title: String,
    val ok: Boolean,
    val detail: String
)

class ProtectionStatus(private val context: Context) {
    fun items(): List<ProtectionItem> {
        val prefs = ShieldPreferences.get(context)
        val admin = ComponentName(context, ShieldDeviceAdminReceiver::class.java)
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val biometric = BiometricManager.from(context)
            .canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK)
        val keyguard = context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        val notifyGranted = Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        return listOf(
            ProtectionItem("Mã PIN", prefs.hasPin(), if (prefs.hasPin()) "Đã đặt, lưu dạng mã băm" else "Chưa đặt"),
            ProtectionItem("Cụm khôi phục", prefs.hasRecovery(), if (prefs.hasRecovery()) "Đã đặt" else "Chưa đặt — quên PIN sẽ không đặt lại được"),
            ProtectionItem("Dịch vụ hỗ trợ", isAccessibilityEnabled(context), "Chỉ dùng để biết ứng dụng nào đang ở trước"),
            ProtectionItem("Quản trị thiết bị", dpm.isAdminActive(admin), "Chỉ để khóa màn hình ngay"),
            ProtectionItem(
                "Sinh trắc học",
                biometric == BiometricManager.BIOMETRIC_SUCCESS,
                if (biometric == BiometricManager.BIOMETRIC_SUCCESS) "Máy hỗ trợ" else "Máy không có hoặc chưa đăng ký"
            ),
            ProtectionItem("Khóa màn hình hệ thống", keyguard.isDeviceSecure, "Dùng để xác nhận thao tác nhạy cảm, không lưu mật khẩu"),
            ProtectionItem("Quyền thông báo", notifyGranted, if (notifyGranted) "Có thể báo khi có phát hiện mới" else "Chưa cấp"),
            ProtectionItem("Android", true, UpdateChecker.androidLimitNote())
        )
    }
}
