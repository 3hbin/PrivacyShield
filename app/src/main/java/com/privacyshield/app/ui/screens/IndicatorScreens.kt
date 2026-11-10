package com.privacyshield.app.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.privacyshield.app.ShieldViewModel
import com.privacyshield.app.data.IndicatorSettings
import com.privacyshield.app.service.IndicatorOverlayService
import com.privacyshield.app.ui.components.SectionCard
import com.privacyshield.app.ui.components.ShieldTopBar

private val SwatchColors = listOf(
    0xFF00E676.toInt(), // green
    0xFFFF9800.toInt(), // orange
    0xFF2196F3.toInt(), // blue
    0xFFF44336.toInt(), // red
    0xFFE040FB.toInt(), // purple
    0xFFFFEB3B.toInt(), // yellow
    0xFFFFFFFF.toInt()  // white
)

@Composable
fun IndicatorSettingsScreen(viewModel: ShieldViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val settings by viewModel.preferences.settings.collectAsState()
    var draft by remember(settings.indicators) { mutableStateOf(settings.indicators) }
    val canOverlay = Settings.canDrawOverlays(context)

    fun apply() {
        viewModel.preferences.updateSettings { it.copy(indicators = draft) }
        if (draft.enabled && Settings.canDrawOverlays(context)) {
            IndicatorOverlayService.startIfEnabled(context)
            IndicatorOverlayService.requestRebuild(context)
        } else {
            IndicatorOverlayService.stop(context)
        }
    }

    Scaffold(topBar = { ShieldTopBar("Chấm chỉ báo camera & micrô", onBack) }) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                SectionCard {
                    Text("Cách hoạt động", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Chấm camera và micrô chỉ hiện khi hệ thống báo cảm biến đang được dùng thật, " +
                            "giống chỉ báo của Android 12. Chấm vị trí là tùy chọn và chỉ cho biết " +
                            "ứng dụng đang mở có quyền vị trí, vì Android không cho ứng dụng thường biết " +
                            "vị trí đang được dùng hay không.",
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
                            context.startActivity(
                                Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))
                            )
                        }) { Text("Cấp quyền hiển thị nổi") }
                    }
                }
            }
            item {
                Text("Vị trí", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                CornerPreview(draft)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CornerChip("Trên trái", IndicatorSettings.CORNER_TOP_LEFT, draft) { draft = it }
                    CornerChip("Trên phải", IndicatorSettings.CORNER_TOP_RIGHT, draft) { draft = it }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
                    CornerChip("Dưới trái", IndicatorSettings.CORNER_BOTTOM_LEFT, draft) { draft = it }
                    CornerChip("Dưới phải", IndicatorSettings.CORNER_BOTTOM_RIGHT, draft) { draft = it }
                }
            }
            item {
                SectionCard {
                    ToggleRow("Chấm camera", draft.showCamera) { draft = draft.copy(showCamera = it) }
                    ColorRow(draft.cameraColor) { draft = draft.copy(cameraColor = it) }
                    ToggleRow("Chấm micrô", draft.showMic) { draft = draft.copy(showMic = it) }
                    ColorRow(draft.micColor) { draft = draft.copy(micColor = it) }
                    ToggleRow("Chấm vị trí (theo quyền, không thời gian thực)", draft.showLocation) {
                        draft = draft.copy(showLocation = it)
                    }
                    ColorRow(draft.locColor) { draft = draft.copy(locColor = it) }
                    ToggleRow("Hiện biểu tượng trong chấm", draft.showIcons) { draft = draft.copy(showIcons = it) }
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
                    OutlinedButton(onClick = { draft = IndicatorSettings(enabled = draft.enabled) }) { Text("Mặc định") }
                    OutlinedButton(onClick = {
                        apply()
                        IndicatorOverlayService.preview(context)
                    }) { Text("Xem thử 4 giây") }
                    Button(onClick = { apply() }) { Text("Áp dụng") }
                }
            }
        }
    }
}

@Composable
private fun CornerChip(label: String, corner: Int, draft: IndicatorSettings, onChange: (IndicatorSettings) -> Unit) {
    FilterChip(
        selected = draft.corner == corner,
        onClick = { onChange(draft.copy(corner = corner)) },
        label = { Text(label) }
    )
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(label, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun ColorRow(selected: Int, onPick: (Int) -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.padding(bottom = 10.dp)
    ) {
        SwatchColors.forEach { argb ->
            val isSelected = selected == argb
            Box(
                Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Color(argb))
                    .border(
                        width = if (isSelected) 3.dp else 1.dp,
                        color = if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outline,
                        shape = CircleShape
                    )
                    .clickable { onPick(argb) }
            )
        }
    }
}

@Composable
private fun CornerPreview(settings: IndicatorSettings) {
    val alignment = when (settings.corner) {
        IndicatorSettings.CORNER_TOP_LEFT -> Alignment.TopStart
        IndicatorSettings.CORNER_TOP_RIGHT -> Alignment.TopEnd
        IndicatorSettings.CORNER_BOTTOM_LEFT -> Alignment.BottomStart
        else -> Alignment.BottomEnd
    }
    Box(
        Modifier
            .size(width = 170.dp, height = 300.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xFF1C1C1E))
            .padding(8.dp)
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF2C2C2E))
                .padding(8.dp)
        ) {
            Row(Modifier.align(alignment), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (settings.showCamera) PreviewDot(Icons.Filled.CameraAlt, settings.cameraColor, settings)
                if (settings.showMic) PreviewDot(Icons.Filled.Mic, settings.micColor, settings)
                if (settings.showLocation) PreviewDot(Icons.Filled.LocationOn, settings.locColor, settings)
            }
        }
    }
}

@Composable
private fun PreviewDot(icon: ImageVector, argb: Int, settings: IndicatorSettings) {
    val color = Color(argb)
    val size = if (settings.showIcons) settings.sizeDp * 1.7f else settings.sizeDp
    val alpha = settings.opacity.coerceIn(0.3f, 1f)
    if (settings.showIcons) {
        Box(
            Modifier
                .size(size.dp)
                .clip(CircleShape)
                .background(Color(0xE6121417))
                .border(1.5.dp, color.copy(alpha = alpha), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = color.copy(alpha = alpha), modifier = Modifier.size((size * 0.55f).dp))
        }
    } else {
        Box(
            Modifier
                .size(size.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = alpha))
                .border(1.dp, Color(0x66000000), CircleShape)
        )
    }
}
