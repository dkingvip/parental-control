package com.parental.child

import android.content.*
import android.util.*

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == "android.intent.action.QUICKBOOT_POWERON") {
            Log.d("BootReceiver", "Boot completed - starting VPN")

            val prefs = context.getSharedPreferences("child_app", Context.MODE_PRIVATE)
            if (prefs.getBoolean("configured", false)) {
                val serviceIntent = Intent(context, VpnBlockerService::class.java).apply {
                    action = "START_BLOCK"
                }
                context.startForegroundService(serviceIntent)
            }
        }
    }
}
