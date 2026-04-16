package com.parental.child

import android.app.admin.*
import android.content.*
import android.util.*
import android.widget.*

/**
 * 设备管理员接收器
 * 负责拦截卸载请求，防止孩子卸载 App
 */
class AdminReceiver : DeviceAdminReceiver() {

    override fun onEnabled(context: Context, intent: Intent) {
        Log.d("AdminReceiver", "Device admin enabled")
        Toast.makeText(context, "家长保护已激活", Toast.LENGTH_SHORT).show()
    }

    override fun onDisabled(context: Context, intent: Intent) {
        Log.d("AdminReceiver", "Device admin disabled")
        Toast.makeText(context, "家长保护已停用", Toast.LENGTH_SHORT).show()
    }

    override fun onDisableRequested(context: Context, intent: Intent): CharSequence {
        // 有人想停用设备管理员
        return "停用需要家长密码，请联系家长"
    }

    override fun onPasswordFailed(context: Context, admin: ComponentName, password: String) {
        // 输错密码
        Log.w("AdminReceiver", "Password failed attempt")
    }

    override fun onPasswordSucceeded(context: Context, admin: ComponentName, password: String) {
        // 输对密码
        Log.d("AdminReceiver", "Password succeeded - admin disabled")
    }

    override fun onAction(
        context: Context,
        intent: Intent,
        result: android.content.Intent?
    ) {
        super.onAction(context, intent, result)
    }
}
