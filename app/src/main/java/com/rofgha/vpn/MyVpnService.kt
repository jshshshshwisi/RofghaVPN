package com.rofgha.vpn

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import java.io.File
import java.io.FileOutputStream

class MyVpnService : VpnService() {

    companion object {
        private const val TAG = "VpnService"
        private const val CHANNEL_ID = "vpn_channel"
        private const val NOTIFICATION_ID = 1
    }

    private var vpnInterface: ParcelFileDescriptor? = null
    private var xrayProcess: Process? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            "START" -> {
                val config = intent.getStringExtra("config") ?: return START_NOT_STICKY
                startVpn(config)
            }
            "STOP" -> {
                stopVpn()
            }
        }
        return START_STICKY
    }

    private fun startVpn(configUrl: String) {
        try {
            createNotificationChannel()
            startForeground(NOTIFICATION_ID, createNotification())

            // Create VPN interface
            val builder = Builder()
            builder.setSession("Rofgha VPN")
            builder.addAddress("10.0.0.2", 32)
            builder.addRoute("0.0.0.0", 0)
            builder.addDnsServer("8.8.8.8")
            builder.addDnsServer("1.1.1.1")

            vpnInterface = builder.establish()

            // Parse VLESS config and create Xray config
            val parser = VlessParser(configUrl)
            val vlessConfig = parser.parse()
            val xrayConfig = XrayConfigGenerator.generate(vlessConfig)

            // Write config to file
            val configFile = File(filesDir, "config.json")
            FileOutputStream(configFile).use { fos ->
                fos.write(xrayConfig.toByteArray())
            }

            // Start Xray
            val binaryPath = prepareXrayBinary()
            if (binaryPath == null) {
                sendStatus("error", "Xray binary پیدا نشد")
                return
            }

            val processBuilder = ProcessBuilder(
                binaryPath, "run", "-c", configFile.absolutePath
            )
            processBuilder.redirectErrorStream(true)
            processBuilder.directory(filesDir)

            xrayProcess = processBuilder.start()

            // Read output in background
            Thread {
                try {
                    xrayProcess?.inputStream?.bufferedReader()?.forEachLine { line ->
                        Log.d(TAG, "Xray: $line")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error reading Xray output", e)
                }
            }.start()

            sendStatus("connected")
            Log.d(TAG, "VPN connected")

        } catch (e: Exception) {
            Log.e(TAG, "Error starting VPN", e)
            sendStatus("error", e.message ?: "خطای ناشناخته")
            stopVpn()
        }
    }

    private fun stopVpn() {
        try {
            xrayProcess?.destroy()
            xrayProcess = null
            vpnInterface?.close()
            vpnInterface = null
            sendStatus("disconnected")
            stopForeground(true)
            stopSelf()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping VPN", e)
        }
    }

    private fun prepareXrayBinary(): String? {
        val binaryFile = File(filesDir, "xray")
        if (binaryFile.exists()) {
            Runtime.getRuntime().exec(arrayOf("chmod", "755", binaryFile.absolutePath))
            return binaryFile.absolutePath
        }

        // Copy from assets
        val arch = android.os.Build.SUPPORTED_ABIS.firstOrNull() ?: "arm64-v8a"
        val assetName = when {
            arch.contains("arm64") || arch.contains("aarch64") -> "xray-arm64"
            arch.contains("x86_64") || arch.contains("amd64") -> "xray-x64"
            arch.contains("x86") -> "xray-x86"
            else -> "xray-arm64"
        }

        return try {
            assets.open(assetName).use { input ->
                FileOutputStream(binaryFile).use { output ->
                    input.copyTo(output)
                }
            }
            Runtime.getRuntime().exec(arrayOf("chmod", "755", binaryFile.absolutePath))
            binaryFile.absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "Error copying binary: $assetName", e)
            null
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "Rofgha VPN",
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
                .setContentTitle("Rofgha VPN")
                .setContentText("متصل به VPN")
                .setSmallIcon(android.R.drawable.ic_menu_manage)
                .setContentIntent(pendingIntent)
                .build()
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
                .setContentTitle("Rofgha VPN")
                .setContentText("متصل به VPN")
                .setSmallIcon(android.R.drawable.ic_menu_manage)
                .setContentIntent(pendingIntent)
                .build()
        }
    }

    private fun sendStatus(status: String, message: String? = null) {
        val intent = Intent("com.rofgha.vpn.VPN_STATUS")
        intent.putExtra("status", status)
        message?.let { intent.putExtra("message", it) }
        sendBroadcast(intent)
    }

    override fun onDestroy() {
        stopVpn()
        super.onDestroy()
    }
}
