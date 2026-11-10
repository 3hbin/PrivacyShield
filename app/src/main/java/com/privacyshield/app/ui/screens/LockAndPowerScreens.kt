@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.privacyshield.app.ui.screens

import android.accessibilityservice.AccessibilityServiceInfo
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme
import com.privacyshield.app.ScanState
import com.privacyshield.app.ShieldViewModel
import com.privacyshield.app.admin.ShieldDeviceAdminReceiver
import com.privacyshield.app.service.AppLockAccessibilityService
import com.privacyshield.app.ui.components.SectionCard
import com.privacyshield.app.ui.components.ShieldTopBar

@Composable
fun AppLockScreen(viewModel: ShieldViewModel, onBack: () -> Unit, onSetupPin: () -> Unit) {
    val context = LocalContext.current
    val scan by viewModel.scan.collectAsState()
    val locked by viewModel.preferences.lockedPackages.collectAsState()
    val settings by viewModel.preferences.settings.collectAsState()
    var query by rememberSaveable { mutableStateOf("") }
    val serviceOn = isAccessibilityEnabled(context)
    Scaffold(topBar = { ShieldTopBar("Khóa ứng dụng", onBack) }) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                SectionCard {
                    Text("Khóa bằng mã PIN và sinh trắc học", style = MaterialTheme.typography.titleMedium)
                    Text(
                        if (serviceOn) "Dịch vụ hỗ trợ đang bật. Ứng dụng đã chọn sẽ bị hỏi mã khi mở."
                        else "Cần bật dịch vụ hỗ trợ để phát hiện ứng dụng đang mở. Privacy Shield không đọc nội dung màn hình.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    if (!serviceOn) {
                        Button(onClick = {
                            context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                        }) { Text("Bật dịch vụ hỗ trợ") }
                        Text(
                            "Nếu không bật được, vào Cài đặt ứng dụng → cho phép Restricted settings, rồi thử lại.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (!settings.hasPin) {
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = onSetupPin) { Text("Đặt mã PIN") }
                    }
                }
            }
            item {
                Text("Tự khóa lại", style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TimeoutChip("Ngay", 0, settings.autoLockSeconds) {
                        viewModel.preferences.updateSettings { it.copy(autoLockSeconds = 0) }
                    }
                    TimeoutChip("30 giây", 30, settings.autoLockSeconds) {
                        viewModel.preferences.updateSettings { it.copy(autoLockSeconds = 30) }
                    }
                    TimeoutChip("1 phút", 60, settings.autoLockSeconds) {
                        viewModel.preferences.updateSettings { it.copy(autoLockSeconds = 60) }
                    }
                    TimeoutChip("5 phút", 300, settings.autoLockSeconds) {
                        viewModel.preferences.updateSettings { it.copy(autoLockSeconds = 300) }
                    }
                }
            }
            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Tìm ứng dụng") },
                    singleLine = true
                )
            }
            when (val state = scan) {
                is ScanState.Ready -> {
                    val apps = state.result.userApps
                        .filter { it.packageName != context.packageName }
                        .filter { query.isBlank() || it.label.contains(query, true) || it.packageName.contains(query, true) }
                    if (apps.isEmpty()) {
                        item { Text("Không có ứng dụng phù hợp.") }
                    } else {
                        items(apps, key = { it.packageName }) { app ->
                            SectionCard {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f)) {
                                        Text(app.label, style = MaterialTheme.typography.titleMedium)
                                        Text(app.packageName, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Switch(
                                        checked = app.packageName in locked,
                                        onCheckedChange = { checked ->
                                            if (!settings.hasPin) {
                                                onSetupPin()
                                            } else {
                                                viewModel.preferences.setLocked(app.packageName, checked)
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
                ScanState.Loading, ScanState.Idle -> item { CircularProgressIndicator() }
                is ScanState.Failed -> item { Text(state.message, color = MaterialTheme.colorScheme.error) }
            }
        }
    }
}

@Composable
private fun TimeoutChip(label: String, seconds: Int, selected: Int, onClick: () -> Unit) {
    FilterChip(selected = selected == seconds, onClick = onClick, label = { Text(label) })
}

@Composable
fun PowerScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val admin = ComponentName(context, ShieldDeviceAdminReceiver::class.java)
    val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
    val active = dpm.isAdminActive(admin)
    Scaffold(topBar = { ShieldTopBar("Bảo vệ khi tắt nguồn", onBack) }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SectionCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    androidx.compose.material3.Icon(Icons.Filled.PowerSettingsNew, contentDescription = null)
                    Text("Giới hạn của Android", modifier = Modifier.padding(start = 8.dp), style = MaterialTheme.typography.titleMedium)
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "Android không cho ứng dụng bên thứ ba chặn nút nguồn, kể cả khi giữ khoảng 10 giây. Không có API chính thức để bắt nhập mật khẩu trước khi tắt máy. Privacy Shield không giả lập tính năng này. Một số hãng có tùy chọn riêng trong Cài đặt của họ."
                )
            }
            SectionCard {
                Text("Phương án thay thế khả thi", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Text("1. Bật quản trị thiết bị để khóa màn hình ngay khi cần.")
                Text("2. Xác nhận bằng khóa màn hình hệ thống trước khi bật. Ứng dụng không nhìn thấy và không lưu mật khẩu đó.")
                Text("3. Mở Cài đặt bảo mật của máy và tìm mục yêu cầu mật khẩu khi tắt nguồn, nếu hãng có hỗ trợ.")
                Spacer(Modifier.height(12.dp))
                Text(if (active) "Quản trị thiết bị: đang bật" else "Quản trị thiết bị: chưa bật")
                Spacer(Modifier.height(8.dp))
                if (!active) {
                    Button(onClick = {
                        val keyguard = context.getSystemService(Context.KEYGUARD_SERVICE) as android.app.KeyguardManager
                        if (keyguard.isDeviceSecure) {
                            val confirm = keyguard.createConfirmDeviceCredentialIntent(
                                "Xác nhận trước khi bật",
                                "Dùng khóa màn hình của máy. Privacy Shield không nhận mật khẩu."
                            )
                            if (confirm != null) context.startActivity(confirm)
                        }
                        val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                            putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, admin)
                            putExtra(
                                DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                                "Chỉ dùng để khóa màn hình ngay. Không xóa dữ liệu."
                            )
                        }
                        context.startActivity(intent)
                    }) { Text("Bật khóa máy nhanh") }
                } else {
                    Button(onClick = { dpm.lockNow() }) { Text("Khóa máy ngay") }
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = { dpm.removeActiveAdmin(admin) }) { Text("Tắt quản trị thiết bị") }
                }
                TextButton(onClick = {
                    context.startActivity(Intent(Settings.ACTION_SECURITY_SETTINGS))
                }) { Text("Mở cài đặt bảo mật") }
            }
        }
    }
}

fun isAccessibilityEnabled(context: Context): Boolean {
    val manager = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
    val enabled = manager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
    val expected = ComponentName(context, AppLockAccessibilityService::class.java).flattenToString()
    return enabled.any { it.resolveInfo.serviceInfo.let { info ->
        ComponentName(info.packageName, info.name).flattenToString() == expected
    } }
}
