package com.rofgha.vpn

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.VpnService
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var etConfig: EditText
    private lateinit var btnConnect: Button
    private lateinit var tvStatus: TextView
    private lateinit var tvServerInfo: TextView
    private var isRunning = false

    private val vpnReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.getStringExtra("status")) {
                "connected" -> updateUI(true)
                "disconnected" -> updateUI(false)
                "error" -> {
                    updateUI(false)
                    val error = intent.getStringExtra("message") ?: "خطای ناشناخته"
                    Toast.makeText(this@MainActivity, "خطا: $error", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        etConfig = findViewById(R.id.etConfig)
        btnConnect = findViewById(R.id.btnConnect)
        tvStatus = findViewById(R.id.tvStatus)
        tvServerInfo = findViewById(R.id.tvServerInfo)

        // Load saved config
        val prefs = getSharedPreferences("vpn_config", MODE_PRIVATE)
        etConfig.setText(prefs.getString("config", ""))

        btnConnect.setOnClickListener {
            if (isRunning) {
                disconnectVPN()
            } else {
                connectVPN()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val filter = IntentFilter("com.rofgha.vpn.VPN_STATUS")
        registerReceiver(vpnReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
    }

    override fun onPause() {
        super.onPause()
        try { unregisterReceiver(vpnReceiver) } catch (_: Exception) {}
    }

    private fun connectVPN() {
        val config = etConfig.text.toString().trim()
        if (config.isEmpty()) {
            Toast.makeText(this, "کانفیگ VLESS رو وارد کن", Toast.LENGTH_SHORT).show()
            return
        }

        if (!config.startsWith("vless://")) {
            Toast.makeText(this, "فرمت کانفیگ اشتباهه!", Toast.LENGTH_SHORT).show()
            return
        }

        // Save config
        getSharedPreferences("vpn_config", MODE_PRIVATE).edit()
            .putString("config", config).apply()

        // Parse and show server info
        try {
            val parser = VlessParser(config)
            val cfg = parser.parse()
            tvServerInfo.text = "📍 ${cfg.server}:${cfg.port}\n🔒 ${cfg.security.uppercase()}"
        } catch (e: Exception) {
            Toast.makeText(this, "خطا در پارس کانفیگ: ${e.message}", Toast.LENGTH_SHORT).show()
            return
        }

        // Request VPN permission
        val intent = VpnService.prepare(this)
        if (intent != null) {
            startActivityForResult(intent, 100)
        } else {
            startVpnService()
        }
    }

    private fun disconnectVPN() {
        val intent = Intent(this, VpnService::class.java)
        intent.action = "STOP"
        startService(intent)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 100) {
            if (resultCode == Activity.RESULT_OK) {
                startVpnService()
            } else {
                Toast.makeText(this, "مجوز VPN داده نشد!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun startVpnService() {
        val config = etConfig.text.toString().trim()
        val intent = Intent(this, VpnService::class.java)
        intent.action = "START"
        intent.putExtra("config", config)
        ContextCompat.startForegroundService(this, intent)
        updateUI(true)
    }

    private fun updateUI(connected: Boolean) {
        isRunning = connected
        if (connected) {
            tvStatus.text = "متصل ✅"
            tvStatus.setTextColor(ContextCompat.getColor(this, R.color.connected))
            btnConnect.text = "قطع اتصال"
            btnConnect.setBackgroundColor(ContextCompat.getColor(this, R.color.disconnect_red))
            etConfig.isEnabled = false
        } else {
            tvStatus.text = "قطع شده ❌"
            tvStatus.setTextColor(ContextCompat.getColor(this, R.color.disconnected))
            btnConnect.text = "اتصال"
            btnConnect.setBackgroundColor(ContextCompat.getColor(this, R.color.connect_green))
            etConfig.isEnabled = true
            tvServerInfo.text = ""
        }
    }
}
