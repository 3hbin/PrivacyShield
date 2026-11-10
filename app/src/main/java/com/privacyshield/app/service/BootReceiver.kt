package com.privacyshield.app.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.privacyshield.app.data.ActivityLog

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED) return
        ActivityLog(context).add("BOOT", "Máy vừa khởi động lại. Cài đặt và két vẫn nằm trên máy.")
        IndicatorOverlayService.startIfEnabled(context)
    }
}
