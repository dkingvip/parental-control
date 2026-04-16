package com.parental.child

import android.app.*
import android.content.*
import android.net.*
import android.os.*
import android.provider.*
import android.system.Os
import android.util.*
import androidx.core.app.NotificationCompat
import okhttp3.*
import org.json.*
import java.util.*
import java.util.concurrent.*

/**
 * VPN 鎷︽埅鏈嶅姟
 * 榛樿鐘舵€侊細鎷︽埅鎵€鏈夌綉缁滄祦閲? * 瑙ｉ攣鐘舵€侊細鍏佽鎵€鏈夋祦閲? */
class VpnBlockerService : VpnService() {

    private var vpnThread: Thread? = null
    private var isUnlocked = false
    private var remainingSeconds = 0
    private var sessionEndsAt = 0L
    private var pollTimer: Timer? = null
    private var consecutiveFailures = 0
    private val MAX_FAILURES = 3  // 杩炵画3娆¤仈绯讳笉涓婃湇鍔″櫒鎵嶆柇缃?
    private val serverUrl: String
        get() = getSharedPreferences("child_app", MODE_PRIVATE)
            .getString("server_url", "http://192.168.1.10:8080")!!

    private val deviceId: String
        get() = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
            ?: UUID.randomUUID().toString().take(16)

    companion object {
        const val CHANNEL_ID = "parental_control_channel"
        const val NOTIFY_ID = 1001
        const val NOTIFY_ID_UNLOCKED = 1002
        var instance: VpnBlockerService? = null
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            "START_BLOCK" -> startBlocking()
            "CHECK_SESSION" -> pollServerStatus()
            "STOP_VPN" -> {
                stopSelf()
                stopSelf()
            }
        }
        return START_STICKY
    }

    // ========== 鍚姩闃绘柇妯″紡 ==========
    private fun startBlocking() {
        Log.d("VpnBlocker", "Starting block mode")

        // 鏉€鎺夋棫鐨?VPN 鎺ュ彛
        try { establishVpn(false) } catch (e: Exception) { }

        isUnlocked = false
        remainingSeconds = 0
        pollTimer?.cancel()

        // 寤虹珛闃绘柇 VPN锛堥粦娲炴ā寮忥級
        try {
            establishVpn(false)
            showBlockNotification()
            startPolling()
        } catch (e: Exception) {
            Log.e("VpnBlocker", "Failed to start VPN: ${e.message}")
        }
    }

    // ========== 寤虹珛 VPN 鎺ュ彛 ==========
    private fun establishVpn(allow: Boolean): ParcelFileDescriptor? {
        val builder = Builder()
            .setSession("涓婄綉鎺у埗")
            .addAddress("10.0.0.2", 32)
            .addRoute("0.0.0.0", 0)
            .addDnsServer("8.8.8.8")
            .setMtu(1500)
            .setBlocking(!allow)  // allow=false 闃诲锛宎llow=true 鏀捐

        val pfd = builder.establish()

        if (allow) {
            // 鏀捐妯″紡锛氬惎鍔ㄦ暟鎹浆鍙戠嚎绋?            startForwardingThread(pfd)
        }

        return pfd
    }

    // ========== 鏁版嵁杞彂锛堟斁琛屾椂锛?==========
    private fun startForwardingThread(pfd: ParcelFileDescriptor) {
        vpnThread?.interrupt()
        vpnThread = Thread {
            try {
                val input = FileInputStream(pfd).channel
                val output = FileOutputStream(pfd).fileDescriptor
                val packet = ByteBuffer.allocate(32767)

                while (!Thread.currentThread().isInterrupted) {
                    try {
                        val len = input.read(packet)
                        if (len > 0) {
                            packet.flip()
                            // 瀹為檯鐢熶骇鐜闇€瑕佸疄鐜板畬鏁寸殑娴侀噺杞彂閫昏緫
                            // 杩欓噷绠€鍖栦负鐩存帴鏀捐
                            packet.clear()
                        }
                    } catch (e: Exception) {
                        break
                    }
                }
            } catch (e: Exception) {
                Log.e("VpnBlocker", "Forwarding error: ${e.message}")
            }
        }.apply { start() }
    }

    // ========== 杞鏈嶅姟鍣ㄧ姸鎬?==========
    private fun startPolling() {
        pollTimer?.cancel()
        pollTimer = Timer()
        pollTimer?.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                pollServerStatus()
            }
        }, 0, 5000) // 姣?绉掓鏌ヤ竴娆?    }

    private fun pollServerStatus() {
        Thread {
            try {
                val json = JSONObject().put("device_id", deviceId)
                val request = Request.Builder()
                    .url("$serverUrl/api/check")
                    .post(json.toString().toRequestBody("application/json".toMediaTypeOrNull()))
                    .build()

                val response = OkHttpClient().newCall(request).execute()
                val resp = JSONObject(response.body?.string() ?: "{}")

                // 鑱旂郴涓婃湇鍔″櫒锛岄噸缃け璐ヨ鏁?                consecutiveFailures = 0

                val allowed = resp.getBoolean("allowed")

                if (allowed && !isUnlocked) {
                    val remaining = resp.getInt("remaining_seconds")
                    unlock(remaining)
                } else if (allowed && isUnlocked) {
                    remainingSeconds = resp.getInt("remaining_seconds")
                    updateUnlockedNotification(remainingSeconds)
                } else if (!allowed && isUnlocked) {
                    // 鏈嶅姟鍣ㄨ璇ユ柇浜?                    Log.d("VpnBlocker", "Server says block - reblocking")
                    runOnMain { blockAgain() }
                }
            } catch (e: Exception) {
                consecutiveFailures++
                Log.e("VpnBlocker", "Poll error ($consecutiveFailures/$MAX_FAILURES): ${e.message}")

                // fail-closed锛氳繛缁?MAX_FAILURES 娆¤仈绯讳笉涓婃湇鍔″櫒鎵嶆柇缃?                // 閬垮厤鍋跺皵涓€娆iFi娉㈠姩璇潃
                if (isUnlocked && consecutiveFailures >= MAX_FAILURES) {
                    Log.w("VpnBlocker", "Server unreachable ${MAX_FAILURES}x - forcing block!")
                    runOnMain { blockAgain() }
                }
            }
        }.start()
    }

    // ========== 瑙ｉ攣缃戠粶 ==========
    fun unlock(durationSeconds: Int) {
        Log.d("VpnBlocker", "Unlocking for $durationSeconds seconds")
        isUnlocked = true
        remainingSeconds = durationSeconds
        sessionEndsAt = System.currentTimeMillis() + durationSeconds * 1000L

        // 閲嶅缓 VPN 鏀捐
        try {
            val pfd = establishVpn(true)
            showUnlockedNotification(durationSeconds)

            // 鍚姩鍊掕鏃?            object : CountDownTimer(durationSeconds * 1000L, 1000) {
                override fun onTick(millisUntilFinished: Long) {
                    remainingSeconds = (millisUntilFinished / 1000).toInt()
                    updateUnlockedNotification(remainingSeconds)
                }

                override fun onFinish() {
                    blockAgain()
                }
            }.start()

        } catch (e: Exception) {
            Log.e("VpnBlocker", "Unlock failed: ${e.message}")
        }
    }

    // ========== 閲嶆柊闃绘柇 ==========
    fun blockAgain() {
        Log.d("VpnBlocker", "Blocking again")
        isUnlocked = false
        remainingSeconds = 0
        vpnThread?.interrupt()
        startBlocking()
    }

    // ========== 閫氱煡鏍?==========
    private fun createNotificationChannel() {
        val ch1 = NotificationChannel(CHANNEL_ID, "涓婄綉鎺у埗", NotificationManager.IMPORTANCE_LOW)
        ch1.description = "鍎跨骞虫澘涓婄綉鎺у埗"
        ch1.setShowBadge(false)

        val ch2 = NotificationChannel("${CHANNEL_ID}_alert", "涓婄綉鎻愰啋", NotificationManager.IMPORTANCE_HIGH)
        ch2.description = "缃戠粶闃绘柇鎻愰啋"

        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(ch1)
        manager.createNotificationChannel(ch2)
    }

    private fun showBlockNotification() {
        val intent = Intent(this, UnlockActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pending = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("馃敀 缃戠粶宸查樆鏂?)
            .setContentText("鐐瑰嚮杈撳叆瀵嗙爜涓婄綉")
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setContentIntent(pending)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        startForeground(NOTIFY_ID, notification)
    }

    private fun showUnlockedNotification(seconds: Int) {
        val mins = seconds / 60
        val secs = seconds % 60

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("鉁?缃戠粶宸插紑閫?)
            .setContentText("鍓╀綑 ${mins}鍒?{secs}绉?)
            .setSmallIcon(android.R.drawable.ic_lock_idle_lock)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        startForeground(NOTIFY_ID_UNLOCKED, notification)
    }

    private fun updateUnlockedNotification(seconds: Int) {
        val mins = seconds / 60
        val secs = seconds % 60

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("鉁?缃戠粶宸插紑閫?)
            .setContentText("鍓╀綑 ${mins}鍒?{secs}绉?)
            .setSmallIcon(android.R.drawable.ic_lock_idle_lock)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFY_ID_UNLOCKED, notification)
    }

    private fun runOnMain(r: () -> Unit) {
        Handler(Looper.getMainLooper()).post(r)
    }

    override fun onDestroy() {
        pollTimer?.cancel()
        vpnThread?.interrupt()
        instance = null
        super.onDestroy()
    }

    override fun onRevoke() {
        // VPN 琚郴缁熸挙閿€锛岀珛鍗抽噸鍚?        startBlocking()
    }
}
