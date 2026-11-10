package com.privacyshield.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.privacyshield.app.ui.theme.StatusColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShieldTopBar(title: String, onBack: (() -> Unit)? = null) {
    TopAppBar(
        title = { Text(title) },
        navigationIcon = {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
                }
            }
        }
    )
}

@Composable
fun SectionCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
    ) {
        Column(Modifier.padding(16.dp), content = { content() })
    }
}

@Composable
fun StatusDot(color: Color, active: Boolean, label: String, detail: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .size(14.dp)
                .clip(CircleShape)
                .background(if (active) color else color.copy(alpha = 0.25f))
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.titleMedium)
            Text(detail, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun LegendRow() {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        StatusDot(StatusColors.safe, true, "Xanh lá", "Trạng thái an toàn: không có ứng dụng cần kiểm tra")
        StatusDot(StatusColors.attention, true, "Cam", "Có ứng dụng được cấp camera hoặc micrô")
        StatusDot(StatusColors.location, true, "Xanh dương", "Có ứng dụng được cấp quyền vị trí")
    }
}

@Composable
fun PinPad(
    value: String,
    maxLength: Int,
    onValueChange: (String) -> Unit,
    onBiometric: (() -> Unit)? = null
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            repeat(maxLength) { index ->
                val filled = index < value.length
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(
                            if (filled) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                        )
                )
            }
        }
        Spacer(Modifier.height(20.dp))
        val keys = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "bio", "0", "del")
        keys.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(vertical = 6.dp)) {
                row.forEach { key ->
                    when (key) {
                        "bio" -> {
                            if (onBiometric != null) {
                                FilledTonalButton(
                                    onClick = onBiometric,
                                    modifier = Modifier.size(72.dp),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Icon(Icons.Filled.Fingerprint, contentDescription = "Sinh trắc học")
                                }
                            } else {
                                Spacer(Modifier.size(72.dp))
                            }
                        }
                        "del" -> {
                            FilledTonalButton(
                                onClick = { if (value.isNotEmpty()) onValueChange(value.dropLast(1)) },
                                modifier = Modifier.size(72.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Backspace, contentDescription = "Xóa")
                            }
                        }
                        else -> {
                            Button(
                                onClick = { if (value.length < maxLength) onValueChange(value + key) },
                                modifier = Modifier.size(72.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text(key, style = MaterialTheme.typography.titleLarge)
                            }
                        }
                    }
                }
            }
        }
    }
}

fun formatTime(millis: Long?): String {
    if (millis == null || millis <= 0L) return "Không có dữ liệu"
    val format = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    return format.format(Date(millis))
}

@Composable
fun EmptyText(text: String) {
    Text(
        text = text,
        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}
