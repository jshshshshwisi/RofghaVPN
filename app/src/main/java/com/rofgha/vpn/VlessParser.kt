package com.rofgha.vpn

import android.net.Uri
import java.net.URLDecoder

data class VlessConfig(
    val uuid: String,
    val server: String,
    val port: Int,
    val network: String,
    val security: String,
    val sni: String,
    val fingerprint: String,
    val publicKey: String,
    val shortId: String,
    val spiderX: String,
    val path: String,
    val host: String,
    val paddingBytes: String,
    val remark: String
)

class VlessParser(private val url: String) {

    fun parse(): VlessConfig {
        try {
            val uri = Uri.parse(url)

            val uuid = uri.userInfo ?: uri.authority?.substringBefore("@") ?: throw Exception("UUID پیدا نشد")
            val server = uri.host ?: throw Exception("سرور پیدا نشد")
            val port = uri.port.takeIf { it > 0 } ?: 443

            val getParam = { key: String ->
                uri.getQueryParameter(key)?.let { URLDecoder.decode(it, "UTF-8") } ?: ""
            }

            val remark = if (url.contains("#")) {
                URLDecoder.decode(url.substringAfterLast("#"), "UTF-8")
            } else ""

            val network = getParam("type").ifEmpty { "tcp" }
            val security = getParam("security").ifEmpty { "none" }
            val sni = getParam("sni").ifEmpty { getParam("host") }
            val fp = getParam("fp").ifEmpty { "chrome" }
            val pbk = getParam("pbk")
            val sid = getParam("sid")
            val spx = getParam("spx")
            val path = getParam("path")
            val host = getParam("host")

            // Parse extra JSON for padding
            val extra = getParam("extra")
            var paddingBytes = "0-0"
            if (extra.isNotEmpty()) {
                try {
                    paddingBytes = if (extra.contains("xPaddingBytes")) {
                        val match = Regex("\"xPaddingBytes\"\\s*:\\s*\"([^\"]+)\"").find(extra)
                        match?.groupValues?.get(1) ?: "0-0"
                    } else {
                        getParam("x_padding_bytes").ifEmpty { "0-0" }
                    }
                } catch (_: Exception) {}
            }

            return VlessConfig(
                uuid = uuid,
                server = server,
                port = port,
                network = network,
                security = security,
                sni = sni,
                fingerprint = fp,
                publicKey = pbk,
                shortId = sid,
                spiderX = spx,
                path = path,
                host = host,
                paddingBytes = paddingBytes,
                remark = remark
            )
        } catch (e: Exception) {
            throw Exception("خطا در پارس کانفیگ: ${e.message}")
        }
    }
}
