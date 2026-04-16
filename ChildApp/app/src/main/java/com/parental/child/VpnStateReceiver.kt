package com.parental.child

import android.content.*
import android.net.*
import android.util.*
import android.widget.Toast

/**
 * 监控 VPN 连接状态变化
 * 如果孩子手动关闭了 VPN，立即重新启动
 */
class VpnStateReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "android.net.Vpn.STATE") {
            val prefs = context.getSharedPreferences("child_app", Context.MODE_PRIVATE)
            if (!prefs.getBoolean("configured", false)) return

            // 检查 VPN 是否断开
            val disconnecting = intent.getBooleanExtra("connection_state", false)
            if (disconnecting) {
                Log.w("VpnState", "VPN disconnected - restarting!")
                Toast.makeText(context, "网络保护已恢复", Toast.LENGTH_LONG).show()

                // 延迟1秒重新启动（等系统VPN完全关闭）
                Handler(Looper.getMainLooper()).postDelayed({
                    val serviceIntent = Intent(context, VpnBlockerService::class.java).apply {
                        action = "START_BLOCK"
                    }
                    context.startForegroundService(serviceIntent)
                }, 1000)
            }
        }
    }
}
