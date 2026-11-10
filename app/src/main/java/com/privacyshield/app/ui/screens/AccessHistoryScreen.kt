package com.privacyshield.app.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.privacyshield.app.data.AccessEvent
import com.privacyshield.app.data.SensorHistory
import com.privacyshield.app.ui.components.EmptyText
import com.privacyshield.app.ui.components.SectionCard
import com.privacyshield.app.ui.components.ShieldTopBar
import com.privacyshield.app.ui.components.formatTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

private val CameraColor = Color(0xFF00C853)
private val MicColor = Color(0xFFFF9800)

@Composable
fun AccessHistoryScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val history = remember { SensorHistory(context) }
    var events by remember { mutableStateOf<List<AccessEvent>>(emptyList()) }
    var filter by remember { mutableStateOf("all") }

    LaunchedEffect(Unit) {
        while (true) {
            events = withContext(Dispatchers.IO) { history.list().reversed() }
            delay(2_000)
        }
    }

    val shown = events.filter { filter == "all" || it.kind == filter }

    Scaffold(topBar = { ShieldTopBar("Lịch sử truy cập camera & micrô", onBack) }) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                SectionCard {
                    Text("Thời điểm camera/micrô được dùng là thật, lấy từ hệ thống.", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Android không cho ứng dụng thường biết chính xác ứng dụng nào mở cảm biến, nên tên ứng dụng " +
                            "là suy đoán: ứng dụng đang mở lúc đó, hoặc ứng dụng có quyền đang chạy dịch vụ nền " +
                            "(cần cấp quyền truy cập dữ liệu sử dụng, Android 10 trở lên). Để ghi liên tục, hãy bật dịch vụ " +
                            "hỗ trợ hoặc chỉ báo nổi. Dữ liệu chỉ lưu mã hóa trên máy.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    FilterChip(selected = filter == "all", onClick = { filter = "all" }, label = { Text("Tất cả") })
                    Spacer(Modifier.width(6.dp))
                    FilterChip(selected = filter == AccessEvent.KIND_CAMERA, onClick = { filter = AccessEvent.KIND_CAMERA }, label = { Text("Camera") })
                    Spacer(Modifier.width(6.dp))
                    FilterChip(selected = filter == AccessEvent.KIND_MIC, onClick = { filter = AccessEvent.KIND_MIC }, label = { Text("Micrô") })
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = {
                        history.clear()
                        events = emptyList()
                    }) { Text("Xóa") }
                }
            }
            if (shown.isEmpty()) item { EmptyText("Chưa ghi nhận lần truy cập nào.") }
            items(shown, key = { it.id }) { AccessRow(it) }
        }
    }
}

@Composable
private fun AccessRow(event: AccessEvent) {
    val context = LocalContext.current
    val icon = remember(event.packageName) { loadIcon(context, event.packageName) }
    val isCamera = event.kind == AccessEvent.KIND_CAMERA

    SectionCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Image(
                    bitmap = icon,
                    contentDescription = event.label,
                    modifier = Modifier.size(46.dp).clip(RoundedCornerShape(10.dp))
                )
            } else {
                Box(
                    Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.errorContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer)
                }
            }
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(event.label, style = MaterialTheme.typography.titleMedium)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (isCamera) Icons.Filled.CameraAlt else Icons.Filled.Mic,
                        contentDescription = null,
                        tint = if (isCamera) CameraColor else MicColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(if (isCamera) "Camera" else "Micrô")
                    Spacer(Modifier.width(8.dp))
                    Text(durationText(event), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(
                    formatTime(event.startMillis),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    when (event.certainty) {
                        AccessEvent.CERT_FOREGROUND -> "Ứng dụng đang mở lúc đó"
                        AccessEvent.CERT_CANDIDATE -> "Suy đoán: có quyền và đang chạy dịch vụ nền"
                        else -> "Không xác định được ứng dụng (dịch vụ hệ thống hoặc chạy nền)"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (event.certainty == AccessEvent.CERT_UNKNOWN) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
            val pkg = event.packageName
            if (pkg != null) {
                TextButton(onClick = {
                    runCatching {
                        context.startActivity(
                            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$pkg"))
                        )
                    }
                }) { Text("Quyền") }
            }
        }
    }
}

private fun durationText(event: AccessEvent): String {
    if (event.endMillis == 0L) return "đang diễn ra"
    if (event.endMillis < 0L) return ""
    val seconds = ((event.endMillis - event.startMillis) / 1000).coerceAtLeast(0)
    return if (seconds < 60) "${seconds}s" else "${seconds / 60}p ${seconds % 60}s"
}

private fun loadIcon(context: Context, pkg: String?): ImageBitmap? {
    if (pkg == null) return null
    return runCatching {
        context.packageManager.getApplicationIcon(pkg).toBitmap(112, 112).asImageBitmap()
    }.getOrNull()
}
