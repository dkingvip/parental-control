package com.parental.child

import android.content.*
import android.net.*
import android.util.*

/**
 * 屏幕解锁时检查VPN是否还在运行
 * 防止孩子在后台偷偷关闭VPN
 */
class ScreenStateReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_USER_PRESENT) {
            val prefs = context.getSharedPreferences("child_app", Context.MODE_PRIVATE)
            if (!prefs.getBoolean("configured", false)) return

            // 检查 VPN 是否在运行
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            var vpnRunning = false
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                vpnRunning = cm.activeNetwork?.let { network ->
                    cm.getNetworkCapabilities(network)
                        ?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) == true
                } ?: false
            } else {
                @Suppress("DEPRECATION")
                vpnRunning = cm.networks?.any { network ->
                    cm.getNetworkCapabilities(network)
                        ?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) == true
                } ?: false
            }

            if (!vpnRunning) {
                Log.w("ScreenState", "VPN not running after screen unlock - restarting!")
                val serviceIntent = Intent(context, VpnBlockerService::class.java).apply {
                    action = "START_BLOCK"
                }
                context.startForegroundService(serviceIntent)
            }
        }
    }
}
