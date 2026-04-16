package com.parental.parent

import android.os.*
import android.text.*
import android.widget.*
import androidx.appcompat.app.*
import okhttp3.*
import org.json.*
import java.util.*

class SettingsActivity : AppCompatActivity() {

    private lateinit var serverUrl: String
    private lateinit var parentPassword: String
    private val client = OkHttpClient()
    private val prefs by lazy { getSharedPreferences("parent_app", MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        loadConfig()
        initViews()
    }

    private fun loadConfig() {
        serverUrl = prefs.getString("server_url", "http://192.168.1.10:8080")!!
        parentPassword = prefs.getString("parent_password", "")!!
    }

    private fun initViews() {
        val etUrl = findViewById<EditText>(R.id.etServerUrl)
        val etPwd = findViewById<EditText>(R.id.etOldPassword)
        val etNewPwd = findViewById<EditText>(R.id.etNewPassword)
        val etNewPwd2 = findViewById<EditText>(R.id.etConfirmNewPassword)
        val etDeviceId = findViewById<EditText>(R.id.etDeviceId)
        val btnSave = findViewById<Button>(R.id.btnSaveSettings)
        val btnTest = findViewById<Button>(R.id.btnTestConnection)
        val tvResult = findViewById<TextView>(R.id.tvConnectionResult)

        etUrl.setText(serverUrl)
        etDeviceId.setText(prefs.getString("default_device", "骞虫澘01"))

        btnTest.setOnClickListener {
            val url = etUrl.text.toString().trim().trimEnd('/')
            btnTest.isEnabled = false
            tvResult.text = "娴嬭瘯杩炴帴涓?.."

            Thread {
                try {
                    val request = Request.Builder().url("$url/").build()
                    val response = client.newCall(request).execute()
                    val body = response.body?.string() ?: "{}"
                    val resp = JSONObject(body)
                    val name = resp.optString("name", "鏈煡")

                    runOnUiThread {
                        btnTest.isEnabled = true
                        tvResult.text = "鉁?杩炴帴鎴愬姛锛佹湇鍔＄: $name"
                        tvResult.setTextColor(0xFF4CAF50.toInt())
                    }
                } catch (e: Exception) {
                    runOnUiThread {
                        btnTest.isEnabled = true
                        tvResult.text = "鉂?杩炴帴澶辫触: ${e.message}"
                        tvResult.setTextColor(0xFFF44336.toInt())
                    }
                }
            }.start()
        }

        btnSave.setOnClickListener {
            val url = etUrl.text.toString().trim().trimEnd('/')
            val oldPwd = etPwd.text.toString()
            val newPwd = etNewPwd.text.toString()
            val newPwd2 = etNewPwd2.text.toString()
            val deviceId = etDeviceId.text.toString().ifEmpty { "骞虫澘01" }

            // 楠岃瘉鏃у瘑鐮?            if (oldPwd.isNotEmpty() && oldPwd != parentPassword) {
                Toast.makeText(this, "鍘熷瘑鐮侀敊璇?, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 楠岃瘉鏂板瘑鐮?            if (newPwd.isNotEmpty()) {
                if (newPwd.length < 4) {
                    Toast.makeText(this, "鏂板瘑鐮佽嚦灏?浣?, Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                if (newPwd != newPwd2) {
                    Toast.makeText(this, "涓ゆ鏂板瘑鐮佷笉涓€鑷?, Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
            }

            prefs.edit()
                .putString("server_url", url)
                .putString("default_device", deviceId)
                .apply()

            if (newPwd.isNotEmpty()) {
                prefs.edit().putString("parent_password", newPwd).apply()
                Toast.makeText(this, "瀵嗙爜宸叉洿鏂?, Toast.LENGTH_SHORT).show()
            }

            Toast.makeText(this, "璁剧疆宸蹭繚瀛?, Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}
