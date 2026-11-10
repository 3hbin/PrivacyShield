package com.privacyshield.app.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.privacyshield.app.ShieldViewModel
import com.privacyshield.app.data.IndicatorSettings
import com.privacyshield.app.service.IndicatorOverlayService
import com.privacyshield.app.ui.components.SectionCard
import com.privacyshield.app.ui.components.ShieldTopBar
import kotlin.math.roundToInt

@Composable
fun IndicatorSettingsScreen(viewModel: ShieldViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val settings by viewModel.preferences.settings.collectAsState()
    var draft by remember(settings.indicators) { mutableStateOf(settings.indicators) }
    val canOverlay = Settings.canDrawOverlays(context)

    Scaffold(topBar = { ShieldTopBar("Tùy chỉnh chỉ báo quyền riêng tư", onBack) }) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                SectionCard {
                    Text("Giới hạn trên Android 8/9", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Android 8 và 9 không có API công khai cho biết camera, micrô hoặc vị trí đang được dùng ngay lúc này. " +
                            "Ba chấm chỉ hiện khi ứng dụng đang ở trước có quyền tương ứng đã được cấp. Đây là sự hiện diện của quyền, không phải hoạt động cảm biến theo thời gian thực.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            item {
                SectionCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Bật chỉ báo nổi", modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                        Switch(checked = draft.enabled, onCheckedChange = { draft = draft.copy(enabled = it) })
                    }
                    if (draft.enabled && !canOverlay) {
                        Spacer(Modifier.height(8.dp))
                        Text("Cần quyền hiển thị trên ứng dụng khác.", color = MaterialTheme.colorScheme.error)
                        Button(onClick = {
                            val intent = Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:${context.packageName}")
                            )
                            context.startActivity(intent)
                        }) { Text("Cấp quyền hiển thị nổi") }
                    }
                }
            }
            item {
                Text("Xem trước vị trí", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                PhonePreview(draft) { updated -> draft = updated }
                Text(
                    "Kéo các chấm trên màn hình mẫu để đổi vị trí.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            item {
                SectionCard {
                    ToggleRow("Chấm camera (xanh)", draft.showCamera) { draft = draft.copy(showCamera = it) }
                    ToggleRow("Chấm micrô (cam)", draft.showMic) { draft = draft.copy(showMic = it) }
                    ToggleRow("Chấm vị trí (xanh dương)", draft.showLocation) { draft = draft.copy(showLocation = it) }
                }
            }
            item {
                SectionCard {
                    Text("Kích thước: ${draft.sizeDp.toInt()} dp")
                    Slider(
                        value = draft.sizeDp,
                        onValueChange = { draft = draft.copy(sizeDp = it) },
                        valueRange = 8f..28f
                    )
                    Text("Độ trong suốt: ${(draft.opacity * 100).toInt()}%")
                    Slider(
                        value = draft.opacity,
                        onValueChange = { draft = draft.copy(opacity = it) },
                        valueRange = 0.3f..1f
                    )
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = {
                        draft = IndicatorSettings()
                    }) { Text("Khôi phục mặc định") }
                    Button(onClick = {
                        viewModel.preferences.updateSettings { it.copy(indicators = draft) }
                        if (draft.enabled && Settings.canDrawOverlays(context)) {
                            IndicatorOverlayService.startIfEnabled(context)
                        } else {
                            IndicatorOverlayService.stop(context)
                        }
                    }) { Text("Áp dụng") }
                }
            }
        }
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(label, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun PhonePreview(settings: IndicatorSettings, onChange: (IndicatorSettings) -> Unit) {
    val phoneWidth = 200.dp
    val phoneHeight = 380.dp
    Box(
        Modifier
            .size(phoneWidth, phoneHeight)
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xFF1C1C1E))
            .padding(8.dp)
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF2C2C2E))
        ) {
            if (settings.showCamera) {
                DraggableDot(
                    color = Color(0xFF4CAF50),
                    fractionX = settings.cameraX,
                    fractionY = settings.cameraY,
                    size = settings.sizeDp,
                    opacity = settings.opacity
                ) { x, y -> onChange(settings.copy(cameraX = x, cameraY = y)) }
            }
            if (settings.showMic) {
                DraggableDot(
                    color = Color(0xFFFF9800),
                    fractionX = settings.micX,
                    fractionY = settings.micY,
                    size = settings.sizeDp,
                    opacity = settings.opacity
                ) { x, y -> onChange(settings.copy(micX = x, micY = y)) }
            }
            if (settings.showLocation) {
                DraggableDot(
                    color = Color(0xFF2196F3),
                    fractionX = settings.locX,
                    fractionY = settings.locY,
                    size = settings.sizeDp,
                    opacity = settings.opacity
                ) { x, y -> onChange(settings.copy(locX = x, locY = y)) }
            }
        }
    }
}

@Composable
private fun DraggableDot(
    color: Color,
    fractionX: Float,
    fractionY: Float,
    size: Float,
    opacity: Float,
    onMoved: (Float, Float) -> Unit
) {
    val density = LocalDensity.current
    val parentWidth = with(density) { 184.dp.toPx() }
    val parentHeight = with(density) { 364.dp.toPx() }
    var offsetX by remember(fractionX) { mutableStateOf(fractionX * parentWidth) }
    var offsetY by remember(fractionY) { mutableStateOf(fractionY * parentHeight) }

    Box(
        Modifier
            .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
            .size(size.dp)
            .clip(CircleShape)
            .background(color.copy(alpha = opacity))
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    offsetX = (offsetX + dragAmount.x).coerceIn(0f, parentWidth - with(density) { size.dp.toPx() })
                    offsetY = (offsetY + dragAmount.y).coerceIn(0f, parentHeight - with(density) { size.dp.toPx() })
                    val fx = (offsetX / parentWidth).coerceIn(0f, 0.95f)
                    val fy = (offsetY / parentHeight).coerceIn(0f, 0.9f)
                    onMoved(fx, fy)
                }
            }
    )
}
