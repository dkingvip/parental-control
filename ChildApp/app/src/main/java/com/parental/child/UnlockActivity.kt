package com.parental.child

import android.app.*
import android.content.*
import android.os.*
import android.provider.*
import android.view.*
import android.widget.*
import androidx.appcompat.app.*
import okhttp3.*
import org.json.*
import java.util.*

/**
 * 瑙ｉ攣鐣岄潰 - 瀛╁瓙杈撳叆瀵嗙爜鐨勫湴鏂? */
class UnlockActivity : AppCompatActivity() {

    private lateinit var etCode: EditText
    private lateinit var btnUnlock: Button
    private lateinit var tvStatus: TextView
    private lateinit var progressBar: ProgressBar

    private val serverUrl: String
        get() = getSharedPreferences("child_app", MODE_PRIVATE)
            .getString("server_url", "http://192.168.1.10:8080")!!

    private val deviceId: String
        get() = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
            ?: UUID.randomUUID().toString().take(16)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_unlock)

        // 鍏ㄥ睆鏄剧ず
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        )

        initViews()
        checkCurrentStatus()
    }

    private fun initViews() {
        etCode = findViewById(R.id.etCode)
        btnUnlock = findViewById(R.id.btnUnlock)
        tvStatus = findViewById(R.id.tvStatus)
        progressBar = findViewById(R.id.progressBar)

        btnUnlock.setOnClickListener {
            val code = etCode.text.toString().trim()
            if (code.length < 6) {
                Toast.makeText(this, "瀵嗙爜涓?浣嶆暟瀛?, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            validateCode(code)
        }

        // 鍏佽绮樿创瀵嗙爜
        etCode.isLongClickable = true

        // 闅忔満鏄剧ず瀵嗙爜鎻愮ず
        val tips = arrayOf("鑱旂郴瀹堕暱鑾峰彇涓婄綉瀵嗙爜", "璁╁闀挎墦寮€銆屽闀挎帶鍒躲€岮pp", "瀹堕暱瀵嗙爜 鈮?杩欎釜瀵嗙爜")
        tvStatus.text = tips.random()
    }

    private fun checkCurrentStatus() {
        Thread {
            try {
                val json = JSONObject().put("device_id", deviceId)
                val request = Request.Builder()
                    .url("$serverUrl/api/check")
                    .post(json.toString().toRequestBody("application/json".toMediaTypeOrNull()))
                    .build()

                val response = OkHttpClient().newCall(request).execute()
                val resp = JSONObject(response.body?.string() ?: "{}")
                val allowed = resp.getBoolean("allowed")

                runOnUiThread {
                    if (allowed) {
                        val remaining = resp.getInt("remaining_seconds")
                        showAlreadyUnlocked(remaining)
                    } else {
                        showBlocked()
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    showBlocked()
                    tvStatus.text = "鈿狅笍 鏃犳硶杩炴帴鏈嶅姟鍣? ${e.message}"
                }
            }
        }.start()
    }

    private fun showAlreadyUnlocked(remaining: Int) {
        tvStatus.text = "鉁?褰撳墠缃戠粶宸插紑閫?
        tvStatus.setTextColor(0xFF4CAF50.toInt())
        etCode.isEnabled = false
        btnUnlock.isEnabled = false
        etCode.hint = "宸茶В閿侊紝鏃犻渶鍐嶆杈撳叆"
    }

    private fun showBlocked() {
        tvStatus.text = "馃敀 缃戠粶宸查樆鏂紝璇疯緭鍏ュ闀垮瘑鐮?
        tvStatus.setTextColor(0xFFF44336.toInt())
        etCode.isEnabled = true
        btnUnlock.isEnabled = true
    }

    private fun validateCode(code: String) {
        btnUnlock.isEnabled = false
        progressBar.visibility = View.VISIBLE
        tvStatus.text = "楠岃瘉涓?.."
        tvStatus.setTextColor(0xFF888888.toInt())

        Thread {
            try {
                val json = JSONObject().apply {
                    put("code", code)
                    put("device_id", deviceId)
                }

                val request = Request.Builder()
                    .url("$serverUrl/api/validate")
                    .post(json.toString().toRequestBody("application/json".toMediaTypeOrNull()))
                    .build()

                val response = OkHttpClient().newCall(request).execute()
                val body = response.body?.string() ?: "{}"
                val resp = JSONObject(body)

                runOnUiThread {
                    progressBar.visibility = View.GONE
                    btnUnlock.isEnabled = true

                    if (resp.getBoolean("valid")) {
                        val duration = resp.getInt("duration_minutes")
                        onUnlockSuccess(duration)
                    } else {
                        val msg = resp.optString("message", "瀵嗙爜閿欒")
                        onUnlockFailed(msg)
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    progressBar.visibility = View.GONE
                    btnUnlock.isEnabled = true
                    onUnlockFailed("缃戠粶閿欒: ${e.message}")
                }
            }
        }.start()
    }

    private fun onUnlockSuccess(duration: Int) {
        // 閫氱煡 VPN Service 瑙ｉ攣
        val intent = Intent(this, VpnBlockerService::class.java).apply {
            action = "CHECK_SESSION"
        }
        startService(intent)

        // 鏄剧ず鎴愬姛鐣岄潰
        val dialogView = layoutInflater.inflate(R.layout.dialog_unlocked, null)
        dialogView.findViewById<TextView>(R.id.tvDuration).text = "${duration}鍒嗛挓"

        AlertDialog.Builder(this, R.style.Theme_ChildControl_Dialog)
            .setTitle("鉁?瑙ｉ攣鎴愬姛")
            .setView(dialogView)
            .setCancelable(false)
            .setPositiveButton("寮€濮嬩笂缃?) { _, _ ->
                finish()
            }
            .show()
    }

    private fun onUnlockFailed(msg: String) {
        tvStatus.text = "鉂?$msg"
        tvStatus.setTextColor(0xFFF44336.toInt())
        etCode.setText("")
        etCode.requestFocus()

        // 闇囧姩鍙嶉
        val vibrator = getSystemService(Vibrator::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(android.os.VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE))
        }
    }

    override fun onBackPressed() {
        // 闃绘鎸夎繑鍥為敭锛屽彧鑳介€氳繃瑙ｉ攣涓婄綉
        moveTaskToBack(true)
    }
}
