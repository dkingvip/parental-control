package com.parental.parent

import android.app.Activity
import android.content.*
import android.os.*
import android.view.*
import android.widget.*
import androidx.appcompat.app.*
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import org.json.*
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var serverUrl: String
    private lateinit var parentPassword: String
    private val client = OkHttpClient()
    private val prefs: SharedPreferences by lazy {
        getSharedPreferences("parent_app", MODE_PRIVATE)
    }
    private var activeSessions = mutableListOf<JSONObject>()
    private var refreshHandler = Handler(Looper.getMainLooper())
    private var refreshRunnable: Runnable? = null

    // ========== 鐣岄潰缁勪欢 ==========
    private lateinit var tvStatus: TextView
    private lateinit var tvLastUpdate: TextView
    private lateinit var sessionsContainer: LinearLayout
    private lateinit var progressBar: ProgressBar
    private lateinit var etPassword: EditText
    private lateinit var spinnerDuration: Spinner
    private lateinit var btnGenerate: Button
    private lateinit var swipeRefresh: SwipeRefreshLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        loadConfig()
        initViews()
        checkFirstRun()
        startAutoRefresh()
    }

    private fun loadConfig() {
        serverUrl = prefs.getString("server_url", "http://192.168.1.10:8080")!!
        parentPassword = prefs.getString("parent_password", "")!!
    }

    private fun initViews() {
        tvStatus = findViewById(R.id.tvStatus)
        tvLastUpdate = findViewById(R.id.tvLastUpdate)
        sessionsContainer = findViewById(R.id.sessionsContainer)
        progressBar = findViewById(R.id.progressBar)
        etPassword = findViewById(R.id.etPassword)
        spinnerDuration = findViewById(R.id.spinnerDuration)
        btnGenerate = findViewById(R.id.btnGenerate)
        swipeRefresh = findViewById(R.id.swipeRefresh)
    private lateinit var tvLastUpdate: TextView
    private lateinit var sessionsContainer: LinearLayout
    private lateinit var progressBar: ProgressBar
    private lateinit var etPassword: EditText
    private lateinit var spinnerDuration: Spinner
    private lateinit var btnGenerate: Button
    private lateinit var swipeRefresh: SwipeRefreshLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        loadConfig()
        initViews()
        checkFirstRun()
        startAutoRefresh()
    }

    private fun loadConfig() {
        serverUrl = prefs.getString("server_url", "http://192.168.1.10:8080")!!
        parentPassword = prefs.getString("parent_password", "")!!
    }

    private fun initViews() {
        tvStatus = findViewById(R.id.tvStatus)
        tvLastUpdate = findViewById(R.id.tvLastUpdate)
        sessionsContainer = findViewById(R.id.sessionsContainer)
        progressBar = findViewById(R.id.progressBar)
        etPassword = findViewById(R.id.etPassword)
        spinnerDuration = findViewById(R.id.spinnerDuration)
        btnGenerate = findViewById(R.id.btnGenerate)
        swipeRefresh = findViewById(R.id.swipeRefresh)

        // 鏃堕暱閫夋嫨
        val durations = arrayOf("15鍒嗛挓", "30鍒嗛挓", "1灏忔椂", "2灏忔椂", "3灏忔椂", "鑷畾涔?)
        spinnerDuration.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, durations)
        spinnerDuration.setSelection(2) // 榛樿1灏忔椂

        btnGenerate.setOnClickListener { generateCode() }

        findViewById<Button>(R.id.btnSettings).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        findViewById<Button>(R.id.btnHistory).setOnClickListener {
            showHistoryDialog()
        }

        swipeRefresh.setOnRefreshListener {
            refreshStatus()
        }
    }

    private fun checkFirstRun() {
        if (parentPassword.isEmpty()) {
            showSetupDialog()
        }
    }

    private fun showSetupDialog() {
        val layout = layoutInflater.inflate(R.layout.dialog_setup, null)
        val etUrl = layout.findViewById<EditText>(R.id.etServerUrl)
        val etPwd = layout.findViewById<EditText>(R.id.etParentPassword)
        val etPwd2 = layout.findViewById<EditText>(R.id.etConfirmPassword)
        etUrl.setText(serverUrl)

        AlertDialog.Builder(this)
            .setTitle("棣栨璁剧疆")
            .setMessage("璇烽厤缃湇鍔＄鍦板潃鍜屾偍鐨勫闀垮瘑鐮?)
            .setView(layout)
            .setCancelable(false)
            .setPositiveButton("淇濆瓨") { _, _ ->
                val url = etUrl.text.toString().trim().trimEnd('/')
                val pwd = etPwd.text.toString()
                val pwd2 = etPwd2.text.toString()

                if (pwd.length < 4) {
                    Toast.makeText(this, "瀵嗙爜鑷冲皯4浣?, Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                if (pwd != pwd2) {
                    Toast.makeText(this, "涓ゆ瀵嗙爜涓嶄竴鑷?, Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                prefs.edit()
                    .putString("server_url", url)
                    .putString("parent_password", pwd)
                    .apply()

                serverUrl = url
                parentPassword = pwd

                Toast.makeText(this, "閰嶇疆宸蹭繚瀛?, Toast.LENGTH_SHORT).show()
            }
            .show()
    }

    // ========== 鐢熸垚瀵嗙爜 ==========
    private fun generateCode() {
        val pwd = etPassword.text.toString()
        if (pwd.isEmpty()) {
            Toast.makeText(this, "璇疯緭鍏ュ闀垮瘑鐮?, Toast.LENGTH_SHORT).show()
            return
        }

        val selectedPos = spinnerDuration.selectedItemPosition
        var duration = when (selectedPos) {
            0 -> 15
            1 -> 30
            2 -> 60
            3 -> 120
            4 -> 180
            else -> 60
        }

        // 濡傛灉閫?鑷畾涔?
        if (selectedPos == 5) {
            val customEdit = EditText(this).apply {
                inputType = android.text.InputType.TYPE_CLASS_NUMBER
                hint = "杈撳叆鍒嗛挓鏁?
            }
            AlertDialog.Builder(this)
                .setTitle("鑷畾涔夋椂闀?)
                .setView(customEdit)
                .setPositiveButton("纭畾") { _, _ ->
                    val mins = customEdit.text.toString().toIntOrNull() ?: 60
                    doGenerate(pwd, mins)
                }
                .show()
            return
        }

        // 閫夎澶?        showDeviceSelectDialog(pwd, duration)
    }

    private fun showDeviceSelectDialog(pwd: String, duration: Int) {
        val devices = arrayOf("骞虫澘01", "骞虫澘02", "鍎跨鎵嬫満")
        AlertDialog.Builder(this)
            .setTitle("閫夋嫨璁惧")
            .setItems(devices) { _, which ->
                val deviceId = devices[which]
                doGenerate(pwd, duration, deviceId)
            }
            .show()
    }

    private fun doGenerate(pwd: String, duration: Int, deviceId: String = "tablet1") {
        btnGenerate.isEnabled = false
        progressBar.visibility = View.VISIBLE

        Thread {
            try {
                val json = JSONObject().apply {
                    put("parent_password", pwd)
                    put("device_id", deviceId)
                    put("duration_minutes", duration)
                }

                val request = Request.Builder()
                    .url("$serverUrl/api/generate")
                    .post(json.toString().toRequestBody("application/json".toMediaTypeOrNull()))
                    .build()

                val response = client.newCall(request).execute()
                val body = response.body?.string() ?: "{}"
                val resp = JSONObject(body)

                runOnUiThread {
                    progressBar.visibility = View.GONE
                    btnGenerate.isEnabled = true

                    if (resp.getBoolean("success")) {
                        val code = resp.getString("code")
                        showCodeDialog(code, duration, deviceId)
                    } else {
                        Toast.makeText(this, resp.optString("message", "鐢熸垚澶辫触"), Toast.LENGTH_SHORT).show()
                        if (resp.optString("message") == "瀹堕暱瀵嗙爜閿欒") {
                            etPassword.setText("")
                        }
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    progressBar.visibility = View.GONE
                    btnGenerate.isEnabled = true
                    Toast.makeText(this, "缃戠粶閿欒: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }.start()
    }

    private fun showCodeDialog(code: String, duration: Int, deviceId: String) {
        val msg = "璁惧: $deviceId\n鏃堕暱: ${duration}鍒嗛挓\n\n馃攽 瀵嗙爜: $code\n\n鎶婂瘑鐮佸憡璇夊瀛愬嵆鍙笂缃?
        AlertDialog.Builder(this)
            .setTitle("鉁?瀵嗙爜宸茬敓鎴?)
            .setMessage(msg)
            .setPositiveButton("澶嶅埗瀵嗙爜") { _, _ ->
                val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("OTP", code))
                Toast.makeText(this, "瀵嗙爜宸插鍒?, Toast.LENGTH_SHORT).show()
            }
            .setNeutralButton("鍙戦€佸井淇?) { _, _ ->
                shareViaWechat(code, duration, deviceId)
            }
            .setNegativeButton("鍏抽棴", null)
            .show()
    }

    private fun shareViaWechat(code: String, duration: Int, deviceId: String) {
        val text = "馃摫 鍎跨骞虫澘涓婄綉瀵嗙爜\n\n璁惧: $deviceId\n鈴?鏃堕暱: ${duration}鍒嗛挓\n馃攽 瀵嗙爜: $code"
        try {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                setClassName("com.tencent.mm", "com.tencent.mm.ui.transmit.SelectConversationUI")
                putExtra(Intent.EXTRA_TEXT, text)
            }
            startActivity(intent)
        } catch (e: Exception) {
            // 寰俊鏈畨瑁咃紝鐢ㄦ櫘閫氬垎浜?            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, text)
            }
            startActivity(Intent.createChooser(intent, "鍒嗕韩瀵嗙爜"))
        }
    }

    // ========== 鍒锋柊鐘舵€?==========
    private fun refreshStatus() {
        Thread {
            try {
                val request = Request.Builder()
                    .url("$serverUrl/api/status")
                    .build()

                val response = client.newCall(request).execute()
                val body = response.body?.string() ?: "{}"
                val resp = JSONObject(body)

                activeSessions.clear()
                val sessions = resp.optJSONArray("active_sessions") ?: JSONArray()
                for (i in 0 until sessions.length()) {
                    activeSessions.add(sessions.getJSONObject(i))
                }

                val stats = resp.optJSONObject("stats") ?: JSONObject()
                val now = resp.optString("now").take(19).replace("T", " ")

                runOnUiThread {
                    swipeRefresh.isRefreshing = false
                    updateStatusUI(stats, now)
                }
            } catch (e: Exception) {
                runOnUiThread {
                    swipeRefresh.isRefreshing = false
                    tvStatus.text = "鉂?杩炴帴澶辫触: ${e.message}"
                }
            }
        }.start()
    }

    private fun updateStatusUI(stats: JSONObject, now: String) {
        val activeCount = activeSessions.size
        tvStatus.text = if (activeCount > 0) {
            "馃煝 $activeCount 涓澶囨鍦ㄤ笂缃?
        } else {
            "鈿?鎵€鏈夎澶囧潎宸叉柇缃?
        }
        tvLastUpdate.text = "鏇存柊鏃堕棿: $now"

        sessionsContainer.removeAllViews()

        if (activeSessions.isEmpty()) {
            val emptyView = TextView(this).apply {
                text = "鏆傛棤涓婄綉涓殑璁惧"
                setTextColor(0xFF888888.toInt())
                textSize = 14f
                gravity = Gravity.CENTER
                setPadding(0, 40, 0, 40)
            }
            sessionsContainer.addView(emptyView)
            return
        }

        for (session in activeSessions) {
            val deviceId = session.getString("device_id")
            val remainingSec = session.getInt("remaining_seconds")
            val totalMin = session.getInt("total_minutes")
            val sessionId = session.getString("session_id")
            val mins = remainingSec / 60
            val secs = remainingSec % 60

            val card = layoutInflater.inflate(R.layout.item_session, sessionsContainer, false)

            card.findViewById<TextView>(R.id.tvDeviceName).text = "馃摫 $deviceId"
            card.findViewById<TextView>(R.id.tvRemaining).text = "鍓╀綑 ${mins}鍒?{secs}绉?

            // 杩涘害鏉?            val progressBar = card.findViewById<ProgressBar>(R.id.progressBar2)
            val totalSec = totalMin * 60
            progressBar.max = totalSec
            progressBar.progress = remainingSec

            // 寮哄埗鏂綉鎸夐挳
            card.findViewById<Button>(R.id.btnForceDisconnect).setOnClickListener {
                AlertDialog.Builder(this)
                    .setTitle("纭鏂綉")
                    .setMessage("纭畾瑕佸己鍒舵柇寮€ $deviceId 鐨勭綉缁滃悧锛?)
                    .setPositiveButton("纭鏂綉") { _, _ ->
                        forceDisconnect(sessionId, deviceId)
                    }
                    .setNegativeButton("鍙栨秷", null)
                    .show()
            }

            sessionsContainer.addView(card)
        }
    }

    private fun forceDisconnect(sessionId: String, deviceId: String) {
        Thread {
            try {
                val json = JSONObject().put("session_id", sessionId)
                val request = Request.Builder()
                    .url("$serverUrl/api/disconnect")
                    .post(json.toString().toRequestBody("application/json".toMediaTypeOrNull()))
                    .build()

                client.newCall(request).execute()
                runOnUiThread {
                    Toast.makeText(this, "$deviceId 宸叉柇缃?, Toast.LENGTH_SHORT).show()
                    refreshStatus()
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this, "鏂綉澶辫触: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

    // ========== 鍘嗗彶璁板綍 ==========
    private fun showHistoryDialog() {
        Thread {
            try {
                val request = Request.Builder()
                    .url("$serverUrl/api/history?limit=30")
                    .build()
                val response = client.newCall(request).execute()
                val body = response.body?.string() ?: "{}"
                val resp = JSONObject(body)
                val history = resp.optJSONArray("history") ?: JSONArray()

                val sb = StringBuilder()
                for (i in 0 until minOf(history.length(), 20)) {
                    val item = history.getJSONObject(i)
                    val action = item.getString("action")
                    val device = item.getString("device_id")
                    val ts = item.getString("timestamp").take(16).replace("T", " ")
                    val dur = item.optInt("duration", 0)
                    val icon = if (action == "UNLOCK") "馃敁" else "馃攽"
                    sb.append("$icon $device | $ts | ${dur}鍒嗛挓\n")
                }

                runOnUiThread {
                    AlertDialog.Builder(this@MainActivity)
                        .setTitle("馃搵 鏈€杩戜娇鐢ㄨ褰?)
                        .setMessage(if (sb.isEmpty()) "鏆傛棤璁板綍" else sb.toString())
                        .setPositiveButton("鍏抽棴", null)
                        .show()
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this, "鑾峰彇鍘嗗彶澶辫触", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

    // ========== 鑷姩鍒锋柊 ==========
    private fun startAutoRefresh() {
        refreshStatus()
        refreshRunnable = object : Runnable {
            override fun run() {
                refreshStatus()
                refreshHandler.postDelayed(this, 10000) // 姣?0绉?            }
        }
        refreshHandler.postDelayed(refreshRunnable!!, 10000)
    }

    override fun onResume() {
        super.onResume()
        loadConfig()
        refreshStatus()
    }

    override fun onDestroy() {
        super.onDestroy()
        refreshRunnable?.let { refreshHandler.removeCallbacks(it) }
    }
}
