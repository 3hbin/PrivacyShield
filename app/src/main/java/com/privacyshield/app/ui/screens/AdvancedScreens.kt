package com.privacyshield.app.ui.screens

import android.app.KeyguardManager
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.privacyshield.app.BuildConfig
import com.privacyshield.app.ScanState
import com.privacyshield.app.ShieldViewModel
import com.privacyshield.app.admin.ShieldDeviceAdminReceiver
import com.privacyshield.app.data.ActivityLog
import com.privacyshield.app.data.PinCrypto
import com.privacyshield.app.data.ProtectionStatus
import com.privacyshield.app.data.SecureVault
import com.privacyshield.app.data.UpdateCheck
import com.privacyshield.app.data.UpdateChecker
import com.privacyshield.app.data.VaultNote
import com.privacyshield.app.ui.components.EmptyText
import com.privacyshield.app.ui.components.SectionCard
import com.privacyshield.app.ui.components.ShieldTopBar
import com.privacyshield.app.ui.components.formatTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

@Composable
fun ProtectionScreen(viewModel: ShieldViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val items = remember { ProtectionStatus(context).items() }
    Scaffold(topBar = { ShieldTopBar("Trạng thái bảo vệ", onBack) }) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Text("Các mục dưới đây đọc trạng thái thật của máy.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            items(items) { item ->
                SectionCard {
                    Text(item.title, style = MaterialTheme.typography.titleMedium)
                    Text(if (item.ok) "Đang sẵn sàng" else "Chưa sẵn sàng", color = if (item.ok) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
                    Text(item.detail, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            item {
                Text(
                    "Chưa kiểm thử trên máy Huawei Y9 2019 thật. EMUI đời cũ có thể tắt dịch vụ hỗ trợ sau khi khởi động lại.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun LogScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val log = remember { ActivityLog(context) }
    var entries by remember { mutableStateOf(log.list().reversed()) }
    Scaffold(topBar = { ShieldTopBar("Nhật ký", onBack) }) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Text("Nhật ký chỉ lưu trên máy, trong bộ nhớ mã hóa. Không gửi đi đâu.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                TextButton(onClick = {
                    log.clear()
                    entries = emptyList()
                }) { Text("Xóa nhật ký") }
            }
            if (entries.isEmpty()) item { EmptyText("Chưa có sự kiện.") }
            items(entries) { entry ->
                SectionCard {
                    Text(entry.kind, style = MaterialTheme.typography.titleMedium)
                    Text(entry.message)
                    Text(formatTime(entry.timeMillis), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
fun VaultScreen(viewModel: ShieldViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val vault = remember { SecureVault(context) }
    val settings by viewModel.preferences.settings.collectAsState()
    var unlocked by remember { mutableStateOf(false) }
    var pin by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf(vault.list()) }
    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    Scaffold(topBar = { ShieldTopBar("Két bảo mật", onBack) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Ghi chú được mã hóa trên máy. Không có máy chủ.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (!settings.hasPin) {
                Text("Hãy đặt mã PIN trước khi dùng két.")
            } else if (!unlocked) {
                OutlinedTextField(pin, { pin = it.filter(Char::isDigit).take(6) }, label = { Text("Mã PIN") }, modifier = Modifier.fillMaxWidth())
                if (error != null) Text(error!!, color = MaterialTheme.colorScheme.error)
                Button(onClick = {
                    if (viewModel.preferences.verifyPin(pin)) {
                        unlocked = true
                        ActivityLog(context).add("VAULT", "Đã mở két")
                    } else error = "Mã PIN không đúng"
                }) { Text("Mở két") }
            } else {
                OutlinedTextField(title, { title = it }, label = { Text("Tiêu đề") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(body, { body = it }, label = { Text("Nội dung") }, modifier = Modifier.fillMaxWidth())
                Button(onClick = {
                    if (title.isBlank()) return@Button
                    vault.save(VaultNote(UUID.randomUUID().toString(), title.trim(), body.trim(), System.currentTimeMillis()))
                    notes = vault.list()
                    title = ""
                    body = ""
                    ActivityLog(context).add("VAULT", "Đã lưu ghi chú")
                }) { Text("Lưu ghi chú") }
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(notes, key = { it.id }) { note ->
                        SectionCard {
                            Text(note.title, style = MaterialTheme.typography.titleMedium)
                            Text(note.body)
                            TextButton(onClick = {
                                vault.delete(note.id)
                                notes = vault.list()
                            }) { Text("Xóa") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EmergencyScreen(viewModel: ShieldViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val settings by viewModel.preferences.settings.collectAsState()
    var phrase by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }
    val admin = ComponentName(context, ShieldDeviceAdminReceiver::class.java)
    val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
    Scaffold(topBar = { ShieldTopBar("Khẩn cấp và khôi phục", onBack) }) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                SectionCard {
                    Text("Cụm khôi phục", style = MaterialTheme.typography.titleMedium)
                    Text("Nếu quên PIN, cụm này là cách duy nhất để đặt lại. Không lưu dạng văn bản thuần và không gửi đi đâu.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    OutlinedTextField(phrase, { phrase = it }, label = { Text("Cụm khôi phục, ít nhất 8 ký tự") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(confirm, { confirm = it }, label = { Text("Nhập lại") }, modifier = Modifier.fillMaxWidth())
                    Button(onClick = {
                        if (phrase.trim().length < 8 || phrase != confirm) {
                            message = "Cụm cần khớp và dài ít nhất 8 ký tự"
                        } else {
                            viewModel.preferences.setRecovery(phrase)
                            ActivityLog(context).add("RECOVERY", "Đã đặt cụm khôi phục")
                            message = "Đã lưu cụm khôi phục"
                            phrase = ""
                            confirm = ""
                        }
                    }) { Text(if (settings.hasPin && viewModel.preferences.hasRecovery()) "Đổi cụm khôi phục" else "Lưu cụm khôi phục") }
                }
            }
            item {
                SectionCard {
                    Text("Khôi phục khi quên PIN", style = MaterialTheme.typography.titleMedium)
                    var recovery by remember { mutableStateOf("") }
                    var newPin by remember { mutableStateOf("") }
                    OutlinedTextField(recovery, { recovery = it }, label = { Text("Cụm khôi phục") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(newPin, { newPin = it.filter(Char::isDigit).take(6) }, label = { Text("PIN mới") }, modifier = Modifier.fillMaxWidth())
                    Button(onClick = {
                        if (!viewModel.preferences.verifyRecovery(recovery)) {
                            message = "Cụm khôi phục không đúng"
                        } else if (newPin.length !in 4..6) {
                            message = "PIN mới cần 4 đến 6 số"
                        } else {
                            viewModel.preferences.setPin(newPin)
                            ActivityLog(context).add("RECOVERY", "Đã đặt lại PIN bằng cụm khôi phục")
                            message = "Đã đặt lại PIN"
                            recovery = ""
                            newPin = ""
                        }
                    }, enabled = viewModel.preferences.hasRecovery()) { Text("Đặt lại PIN") }
                }
            }
            item {
                SectionCard {
                    Text("Chế độ khẩn cấp", style = MaterialTheme.typography.titleMedium)
                    Text("Các nút này chỉ chạy khi bạn bấm. Không tự động.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (dpm.isAdminActive(admin)) {
                        Button(onClick = { dpm.lockNow() }) { Text("Khóa máy ngay") }
                    } else {
                        Text("Chưa bật quản trị thiết bị nên không khóa máy ngay được.")
                    }
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = {
                        SecureVault(context).wipe()
                        ActivityLog(context).add("EMERGENCY", "Đã xóa két")
                        message = "Đã xóa két bảo mật"
                    }) { Text("Xóa két bảo mật") }
                    TextButton(onClick = {
                        ActivityLog(context).clear()
                        message = "Đã xóa nhật ký"
                    }) { Text("Xóa nhật ký") }
                    TextButton(onClick = {
                        viewModel.preferences.wipeLocalData()
                        ActivityLog(context).add("EMERGENCY", "Đã xóa dữ liệu cục bộ")
                        message = "Đã xóa PIN, két và cài đặt. Gỡ ứng dụng nếu muốn xóa sạch hoàn toàn."
                    }) { Text("Xóa dữ liệu ứng dụng trên máy") }
                }
            }
            if (message != null) item { Text(message!!) }
        }
    }
}

@Composable
fun AssistantScreen(viewModel: ShieldViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val scan by viewModel.scan.collectAsState()
    var reviewed by remember { mutableStateOf(viewModel.preferences.reviewedPackages()) }
    Scaffold(topBar = { ShieldTopBar("Trợ lý quyền", onBack) }) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Text("Chọn từng ứng dụng, mở cài đặt hệ thống để tự tắt quyền. Ứng dụng không tự thu hồi quyền hộ bạn.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            when (val state = scan) {
                is ScanState.Ready -> {
                    items(state.result.findings, key = { it.app.packageName }) { finding ->
                        val done = finding.app.packageName in reviewed
                        SectionCard {
                            Text(finding.app.label, style = MaterialTheme.typography.titleMedium)
                            finding.reasons.forEach { Text("• $it") }
                            Text(if (done) "Đã đánh dấu đã kiểm tra" else "Chưa kiểm tra")
                            Row {
                                TextButton(onClick = { openAppSettings(context, finding.app.packageName) }) { Text("Mở cài đặt") }
                                TextButton(onClick = {
                                    viewModel.preferences.setReviewed(finding.app.packageName, !done)
                                    reviewed = viewModel.preferences.reviewedPackages()
                                }) { Text(if (done) "Bỏ đánh dấu" else "Đã kiểm tra") }
                            }
                        }
                    }
                    if (state.result.findings.isEmpty()) item { EmptyText("Không có ứng dụng nào cần trợ lý lúc này.") }
                }
                else -> item { Text("Hãy quét máy ở màn hình tổng quan trước.") }
            }
        }
    }
}

@Composable
fun UpdateScreen(viewModel: ShieldViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val settings by viewModel.preferences.settings.collectAsState()
    var owner by remember { mutableStateOf(settings.githubOwner) }
    var repo by remember { mutableStateOf(settings.githubRepo) }
    var status by remember { mutableStateOf("Chưa kiểm tra") }
    var body by remember { mutableStateOf("") }
    var progress by remember { mutableIntStateOf(-1) }
    var hash by remember { mutableStateOf<String?>(null) }
    var apkFile by remember { mutableStateOf<File?>(null) }
    val scope = rememberCoroutineScope()
    val installPermission = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { }
    Scaffold(topBar = { ShieldTopBar("Cập nhật", onBack) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Phiên bản hiện tại: ${BuildConfig.VERSION_NAME}")
            Text("Chỉ kiểm tra khi bạn bấm nút. Không gửi danh sách ứng dụng đi.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedTextField(owner, { owner = it }, label = { Text("Chủ repo GitHub") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(repo, { repo = it }, label = { Text("Tên repo") }, modifier = Modifier.fillMaxWidth())
            Button(onClick = {
                viewModel.preferences.updateSettings { it.copy(githubOwner = owner.trim(), githubRepo = repo.trim()) }
                status = "Đang kiểm tra..."
                scope.launch {
                    val result = withContext(Dispatchers.IO) {
                        UpdateChecker().check(owner.trim(), repo.trim(), BuildConfig.VERSION_NAME)
                    }
                    ActivityLog(context).add("UPDATE", "Đã kiểm tra cập nhật")
                    when (result) {
                        UpdateCheck.NoRepo -> status = "Nhập chủ repo và tên repo trước."
                        is UpdateCheck.UpToDate -> status = "Đang ở phiên bản mới nhất (${result.current})."
                        is UpdateCheck.Failed -> status = result.message
                        is UpdateCheck.Available -> {
                            status = "Có bản ${result.release.tag}"
                            body = result.release.body
                            val asset = result.release.apk
                            if (asset == null) {
                                status = "Có bản mới nhưng không thấy file APK."
                            } else {
                                progress = 0
                                val bytes = withContext(Dispatchers.IO) {
                                    UpdateChecker().download(asset.downloadUrl) { progress = it }
                                }
                                val digest = PinCrypto.sha256(bytes)
                                hash = digest
                                val expected = result.release.checksum
                                if (expected != null && !digest.equals(expected, true)) {
                                    status = "Mã SHA-256 không khớp. Đã hủy cài."
                                    apkFile = null
                                } else {
                                    val dir = File(context.cacheDir, "updates").apply { mkdirs() }
                                    val file = File(dir, "update.apk")
                                    file.writeBytes(bytes)
                                    apkFile = file
                                    status = if (expected == null) {
                                        "Đã tải. GitHub không đính kèm mã băm; SHA-256 hiện bên dưới. Android sẽ từ chối nếu chứng chỉ ký khác."
                                    } else {
                                        "Đã tải và mã SHA-256 khớp."
                                    }
                                }
                            }
                        }
                    }
                }
            }) { Text("Kiểm tra cập nhật") }
            Text(status)
            if (progress in 0..100) LinearProgressIndicator(progress = { progress / 100f }, modifier = Modifier.fillMaxWidth())
            if (body.isNotBlank()) Text(body)
            if (hash != null) Text("SHA-256: $hash")
            if (apkFile != null) {
                Button(onClick = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !context.packageManager.canRequestPackageInstalls()) {
                        installPermission.launch(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}")))
                        return@Button
                    }
                    val file = apkFile ?: return@Button
                    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(uri, "application/vnd.android.package-archive")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                }) { Text("Xác nhận và cài bản đã tải") }
            }
        }
    }
}
