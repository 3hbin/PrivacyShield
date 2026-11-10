package com.privacyshield.app.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.Settings
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
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.privacyshield.app.BuildConfig
import com.privacyshield.app.ScanState
import com.privacyshield.app.ShieldViewModel
import com.privacyshield.app.data.InstalledApp
import com.privacyshield.app.ui.components.EmptyText
import com.privacyshield.app.ui.components.LegendRow
import com.privacyshield.app.ui.components.SectionCard
import com.privacyshield.app.ui.components.ShieldTopBar
import com.privacyshield.app.ui.components.StatusDot
import com.privacyshield.app.ui.components.formatTime
import com.privacyshield.app.ui.theme.StatusColors

@Composable
fun DashboardScreen(viewModel: ShieldViewModel, onOpenPrivacy: () -> Unit, onOpenLock: () -> Unit) {
    val scan by viewModel.scan.collectAsState()
    val settings by viewModel.preferences.settings.collectAsState()
    val locked by viewModel.preferences.lockedPackages.collectAsState()
    Scaffold(topBar = { ShieldTopBar("Tổng quan") }) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text("Privacy Shield", style = MaterialTheme.typography.headlineMedium)
                Text(
                    "Bảng điều khiển quyền riêng tư cho máy đời cũ. Mọi số liệu lấy từ thiết bị này.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            item {
                when (val state = scan) {
                    ScanState.Idle, ScanState.Loading -> {
                        SectionCard {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator()
                                Text("Đang đọc ứng dụng trên máy…", modifier = Modifier.padding(start = 16.dp))
                            }
                        }
                    }
                    is ScanState.Failed -> {
                        SectionCard {
                            Text(state.message, color = MaterialTheme.colorScheme.error)
                            Spacer(Modifier.height(8.dp))
                            Button(onClick = viewModel::refresh) { Text("Thử lại") }
                        }
                    }
                    is ScanState.Ready -> {
                        val result = state.result
                        SectionCard {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Shield, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Text(
                                    if (result.needsAttention) "Cần chú ý" else "An toàn",
                                    style = MaterialTheme.typography.titleLarge,
                                    modifier = Modifier.padding(start = 8.dp)
                                )
                            }
                            Spacer(Modifier.height(12.dp))
                            StatusDot(
                                StatusColors.safe,
                                !result.needsAttention,
                                "Xanh lá · An toàn",
                                if (result.needsAttention) "${result.findings.size} ứng dụng cần kiểm tra"
                                else "Không có tổ hợp quyền đáng chú ý"
                            )
                            Spacer(Modifier.height(8.dp))
                            StatusDot(
                                StatusColors.attention,
                                result.cameraOrMic.isNotEmpty(),
                                "Cam · Camera / micrô",
                                "${result.cameraOrMic.size} ứng dụng được cấp quyền"
                            )
                            Spacer(Modifier.height(8.dp))
                            StatusDot(
                                StatusColors.location,
                                result.locationApps.isNotEmpty(),
                                "Xanh dương · Vị trí",
                                "${result.locationApps.size} ứng dụng được cấp quyền"
                            )
                            Spacer(Modifier.height(12.dp))
                            Text(
                                "Quét lúc ${formatTime(result.scannedAtMillis)}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            item {
                SectionCard {
                    Text("Khóa ứng dụng", style = MaterialTheme.typography.titleMedium)
                    Text("${locked.size} ứng dụng đang được đánh dấu khóa")
                    Text(
                        if (settings.hasPin) "Đã đặt mã PIN" else "Chưa đặt mã PIN",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = onOpenLock) { Text("Quản lý") }
                        FilledTonalButton(onClick = onOpenPrivacy) { Text("Quyền riêng tư") }
                    }
                }
            }
            item {
                Button(onClick = viewModel::refresh, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.Refresh, contentDescription = null)
                    Text("Quét lại", modifier = Modifier.padding(start = 8.dp))
                }
            }
        }
    }
}

@Composable
fun PrivacyScreen(viewModel: ShieldViewModel, onBack: () -> Unit) {
    val scan by viewModel.scan.collectAsState()
    val context = LocalContext.current
    Scaffold(topBar = { ShieldTopBar("Quyền riêng tư", onBack) }) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                SectionCard {
                    Text("Chỉ báo trạng thái", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    LegendRow()
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Android không cho ứng dụng bên thứ ba biết camera hoặc micrô đang được dùng lúc nào. Danh sách dưới đây là quyền đã cấp thật. Thời điểm là lần mở ứng dụng gần nhất, chỉ hiện khi bạn cấp quyền truy cập sử dụng.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (!viewModel.hasUsageAccess()) {
                        Spacer(Modifier.height(8.dp))
                        TextButton(onClick = {
                            context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                        }) { Text("Cấp quyền truy cập sử dụng") }
                    }
                }
            }
            when (val state = scan) {
                is ScanState.Ready -> {
                    item { Text("Camera và micrô", style = MaterialTheme.typography.titleLarge) }
                    if (state.result.cameraOrMic.isEmpty()) {
                        item { EmptyText("Không có ứng dụng nào được cấp camera hoặc micrô.") }
                    } else {
                        items(state.result.cameraOrMic, key = { "cm-${it.packageName}" }) { app ->
                            PermissionAppRow(app)
                        }
                    }
                    item { Text("Vị trí", style = MaterialTheme.typography.titleLarge) }
                    if (state.result.locationApps.isEmpty()) {
                        item { EmptyText("Không có ứng dụng nào được cấp vị trí.") }
                    } else {
                        items(state.result.locationApps, key = { "loc-${it.packageName}" }) { app ->
                            PermissionAppRow(app)
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
private fun PermissionAppRow(app: InstalledApp) {
    val context = LocalContext.current
    val tags = buildList {
        if (app.hasCamera) add("Camera")
        if (app.hasMicrophone) add("Micrô")
        if (app.hasLocation) add("Vị trí")
    }.joinToString(" · ")
    SectionCard {
        Text(app.label, style = MaterialTheme.typography.titleMedium)
        Text(tags, color = MaterialTheme.colorScheme.primary)
        Text(
            "Lần mở gần nhất: ${formatTime(app.lastUsedMillis)}",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        TextButton(onClick = { openAppSettings(context, app.packageName) }) {
            Text("Mở cài đặt để thu hồi quyền")
        }
    }
}

@Composable
fun SuspiciousScreen(viewModel: ShieldViewModel, onBack: () -> Unit) {
    val scan by viewModel.scan.collectAsState()
    val context = LocalContext.current
    Scaffold(topBar = { ShieldTopBar("Ứng dụng đáng chú ý", onBack) }) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                SectionCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Warning, contentDescription = null, tint = StatusColors.attention)
                        Text("Trung tâm kiểm tra", modifier = Modifier.padding(start = 8.dp), style = MaterialTheme.typography.titleMedium)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Chỉ cảnh báo khi ứng dụng thật sự được cấp một tổ hợp quyền nhạy cảm. Không kết luận nghe lén nếu không có bằng chứng.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            when (val state = scan) {
                is ScanState.Ready -> {
                    if (state.result.findings.isEmpty()) {
                        item { EmptyText("Không phát hiện ứng dụng nào có tổ hợp quyền đáng chú ý.") }
                    } else {
                        items(state.result.findings, key = { it.app.packageName }) { finding ->
                            SectionCard {
                                Text(finding.app.label, style = MaterialTheme.typography.titleMedium)
                                Text(finding.app.packageName, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                finding.reasons.forEach { reason ->
                                    Text("• $reason")
                                }
                                Text(
                                    "Thu hồi: Mở cài đặt → Quyền → tắt quyền không cần thiết.",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                TextButton(onClick = { openAppSettings(context, finding.app.packageName) }) {
                                    Text("Mở cài đặt ứng dụng")
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
fun MoreScreen(
    onSuspicious: () -> Unit,
    onPower: () -> Unit,
    onSettings: () -> Unit,
    onAbout: () -> Unit,
    onProtection: () -> Unit,
    onLog: () -> Unit,
    onVault: () -> Unit,
    onEmergency: () -> Unit,
    onAssistant: () -> Unit,
    onUpdate: () -> Unit,
    onIndicators: () -> Unit,
    onAccessHistory: () -> Unit
) {
    Scaffold(topBar = { ShieldTopBar("Thêm") }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item { MoreRow(Icons.Filled.Videocam, "Lịch sử truy cập camera & micrô", "Ứng dụng nào đã dùng, lúc nào, kèm biểu tượng", onAccessHistory) }
            item { MoreRow(Icons.Filled.Visibility, "Chấm chỉ báo", "Chấm nổi ở góc khi camera/micrô đang được dùng", onIndicators) }
            item { MoreRow(Icons.Filled.Security, "Trạng thái bảo vệ", "Kiểm tra PIN, dịch vụ, quản trị thiết bị", onProtection) }
            item { MoreRow(Icons.Filled.History, "Nhật ký", "Sự kiện quét và két, chỉ lưu trên máy", onLog) }
            item { MoreRow(Icons.Filled.VpnKey, "Két bảo mật", "Ghi chú mã hóa cục bộ", onVault) }
            item { MoreRow(Icons.Filled.Warning, "Ứng dụng đáng chú ý", "Máy quét tổ hợp quyền thật", onSuspicious) }
            item { MoreRow(Icons.Filled.Shield, "Trợ lý quyền", "Hướng dẫn thu hồi quyền từng ứng dụng", onAssistant) }
            item { MoreRow(Icons.Filled.PowerSettingsNew, "Bảo vệ khi tắt nguồn", "Giới hạn hệ thống và khóa máy nhanh", onPower) }
            item { MoreRow(Icons.Filled.Lock, "Khẩn cấp", "Khôi phục PIN và xóa dữ liệu", onEmergency) }
            item { MoreRow(Icons.Filled.SystemUpdate, "Cập nhật", "Kiểm tra GitHub Releases khi bạn yêu cầu", onUpdate) }
            item { MoreRow(Icons.Filled.AdminPanelSettings, "Cài đặt", "Giao diện, thông báo, mã PIN", onSettings) }
            item { MoreRow(Icons.Filled.Info, "Giới thiệu", "Phiên bản và giới hạn đã kiểm thử", onAbout) }
        }
    }
}

@Composable
private fun MoreRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    SectionCard {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            TextButton(onClick = onClick) { Text("Mở") }
        }
    }
}

fun openAppSettings(context: android.content.Context, packageName: String) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.parse("package:$packageName")
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(intent)
}
