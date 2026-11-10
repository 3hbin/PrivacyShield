@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.privacyshield.app.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.privacyshield.app.BuildConfig
import com.privacyshield.app.ShieldViewModel
import com.privacyshield.app.data.SeedColor
import com.privacyshield.app.data.ThemeMode
import com.privacyshield.app.ui.components.SectionCard
import com.privacyshield.app.ui.components.ShieldTopBar

@Composable
fun SettingsScreen(viewModel: ShieldViewModel, onBack: () -> Unit, onChangePin: () -> Unit) {
    val context = LocalContext.current
    val settings by viewModel.preferences.settings.collectAsState()
    val notificationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.preferences.updateSettings { it.copy(notificationsEnabled = granted) }
    }
    Scaffold(topBar = { ShieldTopBar("Cài đặt", onBack) }) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                SectionCard {
                    Text("Chế độ giao diện", style = MaterialTheme.typography.titleMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                        ThemeChip("Hệ thống", ThemeMode.SYSTEM, settings.themeMode) {
                            viewModel.preferences.updateSettings { it.copy(themeMode = ThemeMode.SYSTEM) }
                        }
                        ThemeChip("Sáng", ThemeMode.LIGHT, settings.themeMode) {
                            viewModel.preferences.updateSettings { it.copy(themeMode = ThemeMode.LIGHT) }
                        }
                        ThemeChip("Tối", ThemeMode.DARK, settings.themeMode) {
                            viewModel.preferences.updateSettings { it.copy(themeMode = ThemeMode.DARK) }
                        }
                    }
                }
            }
            item {
                SectionCard {
                    Text("Màu chủ đạo", style = MaterialTheme.typography.titleMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                        SeedChip("Xanh ngọc", SeedColor.TEAL, settings.seedColor) {
                            viewModel.preferences.updateSettings { it.copy(seedColor = SeedColor.TEAL) }
                        }
                        SeedChip("Chàm", SeedColor.INDIGO, settings.seedColor) {
                            viewModel.preferences.updateSettings { it.copy(seedColor = SeedColor.INDIGO) }
                        }
                        SeedChip("Rừng", SeedColor.FOREST, settings.seedColor) {
                            viewModel.preferences.updateSettings { it.copy(seedColor = SeedColor.FOREST) }
                        }
                    }
                }
            }
            item {
                SectionCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Thông báo bảo mật", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "Báo khi quét thấy ứng dụng có tổ hợp quyền đáng chú ý",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = settings.notificationsEnabled,
                            onCheckedChange = { enabled ->
                                if (enabled && Build.VERSION.SDK_INT >= 33 &&
                                    ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.POST_NOTIFICATIONS
                                    ) != PackageManager.PERMISSION_GRANTED
                                ) {
                                    notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    viewModel.preferences.updateSettings { it.copy(notificationsEnabled = enabled) }
                                }
                            }
                        )
                    }
                }
            }
            item {
                SectionCard {
                    Text("Mã PIN và sinh trắc học", style = MaterialTheme.typography.titleMedium)
                    Text(
                        if (settings.hasPin) "Đã đặt mã PIN ${settings.pinLength} số" else "Chưa đặt mã PIN",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    TextButton(onClick = onChangePin) {
                        Text(if (settings.hasPin) "Đổi mã PIN" else "Đặt mã PIN")
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Dùng sinh trắc học khi mở khóa")
                            Text(
                                "Chỉ hiện nếu máy có vân tay hoặc khuôn mặt đã đăng ký",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = settings.biometricEnabled,
                            onCheckedChange = { enabled ->
                                if (settings.hasPin) {
                                    viewModel.preferences.updateSettings { it.copy(biometricEnabled = enabled) }
                                }
                            },
                            enabled = settings.hasPin
                        )
                    }
                }
            }
            item {
                SectionCard {
                    Text("Quyền của Privacy Shield", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Ứng dụng cần đọc danh sách ứng dụng để kiểm tra quyền. Không gửi dữ liệu đi đâu.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    TextButton(onClick = {
                        context.startActivity(
                            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.parse("package:${context.packageName}")
                            }
                        )
                    }) { Text("Mở quyền của ứng dụng") }
                    TextButton(onClick = {
                        context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                    }) { Text("Quyền truy cập sử dụng") }
                    TextButton(onClick = {
                        context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                    }) { Text("Dịch vụ hỗ trợ") }
                }
            }
        }
    }
}

@Composable
private fun ThemeChip(label: String, mode: ThemeMode, selected: ThemeMode, onClick: () -> Unit) {
    FilterChip(selected = selected == mode, onClick = onClick, label = { Text(label) })
}

@Composable
private fun SeedChip(label: String, seed: SeedColor, selected: SeedColor, onClick: () -> Unit) {
    FilterChip(selected = selected == seed, onClick = onClick, label = { Text(label) })
}

@Composable
fun AboutScreen(onBack: () -> Unit) {
    Scaffold(topBar = { ShieldTopBar("Giới thiệu", onBack) }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Privacy Shield", style = MaterialTheme.typography.headlineMedium)
            Text("Phiên bản ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
            SectionCard {
                Text("Ứng dụng làm được gì", style = MaterialTheme.typography.titleMedium)
                Text("• Xem ứng dụng nào được cấp camera, micrô hoặc vị trí.")
                Text("• Cảnh báo tổ hợp quyền đáng chú ý và dẫn tới chỗ thu hồi quyền.")
                Text("• Khóa ứng dụng bạn chọn bằng mã PIN hoặc sinh trắc học.")
                Text("• Khóa màn hình ngay nếu bạn bật quản trị thiết bị.")
            }
            SectionCard {
                Text("Ứng dụng không làm được gì", style = MaterialTheme.typography.titleMedium)
                Text("• Không chặn nút nguồn, kể cả khi giữ 10 giây.")
                Text("• Không biết camera hay micrô đang được dùng ngay lúc này.")
                Text("• Không kết luận một ứng dụng đang nghe lén nếu không có bằng chứng.")
                Text("• Không gửi dữ liệu ra ngoài máy, trừ khi bạn tự bấm kiểm tra cập nhật trên GitHub.")
                Text("• Chưa được kiểm thử trên máy Huawei Y9 2019 thật.")
            }
        }
    }
}

@Composable
fun PinSetupScreen(viewModel: ShieldViewModel, onBack: () -> Unit, changing: Boolean) {
    val settings by viewModel.preferences.settings.collectAsState()
    var stage by remember { mutableStateOf(if (changing) "old" else "new") }
    var first by remember { mutableStateOf("") }
    var current by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val length = if (stage == "confirm") first.length else settings.pinLength.coerceAtLeast(4)
    Scaffold(topBar = { ShieldTopBar(if (changing) "Đổi mã PIN" else "Đặt mã PIN", onBack) }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                when (stage) {
                    "old" -> "Nhập mã PIN hiện tại"
                    "confirm" -> "Nhập lại mã PIN mới"
                    else -> "Nhập mã PIN mới, 4 đến 6 số"
                },
                style = MaterialTheme.typography.titleMedium
            )
            if (error != null) {
                Text(error!!, color = MaterialTheme.colorScheme.error)
            }
            com.privacyshield.app.ui.components.PinPad(
                value = current,
                maxLength = if (stage == "new") 6 else length,
                onValueChange = { value ->
                    current = value
                    error = null
                    if (stage == "old" && value.length == length) {
                        if (viewModel.preferences.verifyPin(value)) {
                            stage = "new"
                            current = ""
                        } else {
                            error = "Mã PIN không đúng"
                            current = ""
                        }
                    } else if (stage == "confirm" && value.length == length) {
                        if (value == first) {
                            viewModel.preferences.setPin(value)
                            onBack()
                        } else {
                            error = "Mã nhập lại không khớp"
                            current = ""
                        }
                    }
                }
            )
            if (stage == "new") {
                androidx.compose.material3.Button(
                    onClick = {
                        if (current.length in 4..6) {
                            first = current
                            stage = "confirm"
                            current = ""
                        } else {
                            error = "Mã PIN cần từ 4 đến 6 số"
                        }
                    },
                    enabled = current.length in 4..6,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Tiếp tục") }
            }
        }
    }
}
