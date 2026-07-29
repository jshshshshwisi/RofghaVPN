package com.rofgha.vpn

object XrayConfigGenerator {

    fun generate(config: VlessConfig): String {
        return """
{
  "log": {"loglevel": "warning"},
  "inbounds": [{
    "tag": "socks",
    "port": 10808,
    "protocol": "socks",
    "settings": {"auth": "noauth", "udp": true, "userLevel": 8},
    "sniffing": {"enabled": true, "destOverride": ["http", "tls"]}
  }],
  "outbounds": [{
    "tag": "proxy",
    "protocol": "vless",
    "settings": {
      "vnext": [{
        "address": "${config.server}",
        "port": ${config.port},
        "users": [{
          "id": "${config.uuid}",
          "level": 8,
          "encryption": "none"
        }]
      }]
    },
    "streamSettings": {
      "network": "${config.network}",
      "security": "${config.security}",
      "xhttpSettings": {
        "path": "${config.path}",
        "host": "${config.host}",
        "mode": "auto",
        "extra": {"mode": "auto", "xPaddingBytes": "${config.paddingBytes}"}
      },
      "realitySettings": {
        "serverName": "${config.sni}",
        "fingerprint": "${config.fingerprint}",
        "publicKey": "${config.publicKey}",
        "shortId": "${config.shortId}",
        "spiderX": "${config.spiderX}"
      }
    },
    "mux": {"enabled": false}
  }, {
    "tag": "direct",
    "protocol": "freedom",
    "settings": {"domainStrategy": "UseIP"}
  }],
  "routing": {
    "domainStrategy": "IPIfNonMatch",
    "rules": [
      {"type": "field", "ip": ["geoip:private"], "outboundTag": "direct"}
    ]
  }
}
        """.trimIndent()
    }
}
