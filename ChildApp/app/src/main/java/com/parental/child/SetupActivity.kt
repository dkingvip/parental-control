package com.parental.child

import android.app.*
import android.content.*
import android.net.VpnService
import android.os.*
import android.provider.*
import android.view.*
import android.widget.*
import androidx.appcompat.app.*
import okhttp3.*
import org.json.*
import java.security.MessageDigest

/**
 * 首次安装时的设置向导
 * 由家长完成初始配置
 */
class SetupActivity : AppCompatActivity() {

    private lateinit var prefs: SharedPreferences
    private val client = OkHttpClient()
    private var step = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = getSharedPreferences("child_app", MODE_PRIVATE)

        // 跳过已配置
        if (prefs.getBoolean("configured", false)) {
            gotoMain()
            return
        }

        setContentView(R.layout.activity_setup)
        showStep1()
    }

    // ========== Step 1: 输入服务器地址 ==========
    private fun showStep1() {
        step = 1
        setContentView(R.layout.activity_setup)

        findViewById<Button>(R.id.btnNext).setOnClickListener {
            val url = findViewById<EditText>(R.id.etServerUrl).text.toString().trim().trimEnd('/')
            if (url.isEmpty()) {
                Toast.makeText(this, "请输入服务器地址", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            prefs.edit().putString("server_url", url).apply()
            showStep2()
        }
    }

    // ========== Step 2: 激活设备管理员（防卸载）==========
    private fun showStep2() {
        step = 2
        setContentView(R.layout.activity_setup2)

        findViewById<Button>(R.id.btnActivateAdmin).setOnClickListener {
            activateDeviceAdmin()
        }
    }

    private fun activateDeviceAdmin() {
        val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
        intent.putExtra(
            DevicePolicyManager.EXTRA_DEVICE_ADMIN,
            android.content.ComponentName(this, AdminReceiver::class.java)
        )
        intent.putExtra(
            DevicePolicyManager.EXTRA_ADD_EXPLANATION,
            "激活设备管理员后，卸载本应用需要家长密码，防止孩子卸载保护应用。"
        )
        startActivityForResult(intent, 1001)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1001) {
            if (resultCode == RESULT_OK) {
                prefs.edit().putBoolean("admin_active", true).apply()
                Toast.makeText(this, "✅ 防卸载已启用", Toast.LENGTH_SHORT).show()
                showStep3()
            } else {
                // 用户拒绝激活，给出提示
                AlertDialog.Builder(this)
                    .setTitle("⚠️ 重要提示")
                    .setMessage("如果不启用设备管理员，孩子可以卸载本 App 保护就失效了。\n\n建议启用。")
                    .setPositiveButton("重新启用") { _, _ -> activateDeviceAdmin() }
                    .setNegativeButton("跳过（不安全）") { _, _ -> showStep3() }
                    .setCancelable(false)
                    .show()
            }
        }
    }

    // ========== Step 3: 激活 VPN ==========
    private fun showStep3() {
        step = 3
        setContentView(R.layout.activity_setup3)

        findViewById<Button>(R.id.btnActivateVpn).setOnClickListener {
            val intent = VpnService.prepare(this)
            if (intent != null) {
                startActivityForResult(intent, 1002)
            } else {
                // VPN 已经授权
                onVpnReady()
            }
        }
    }

    private fun onVpnReady() {
        prefs.edit().putBoolean("vpn_ready", true).apply()

        // 启动 VPN 阻断服务
        val intent = Intent(this, VpnBlockerService::class.java).apply {
            action = "START_BLOCK"
        }
        startService(intent)

        prefs.edit().putBoolean("configured", true).apply()
        Toast.makeText(this, "✅ 配置完成！", Toast.LENGTH_SHORT).show()

        gotoMain()
    }

    private fun gotoMain() {
        startActivity(Intent(this, UnlockActivity::class.java))
        finish()
    }
}
