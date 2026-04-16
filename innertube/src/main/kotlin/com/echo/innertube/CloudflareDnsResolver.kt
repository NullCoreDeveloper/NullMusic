package com.echo.innertube

import okhttp3.Dns
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.dnsoverhttps.DnsOverHttps
import java.net.InetAddress

/**
 * App-wide DNS resolver that can be toggled at runtime.
 * When enabled, host lookups are resolved via Cloudflare DoH.
 */
object CloudflareDnsResolver : Dns {
    @Volatile
    var isEnabled: Boolean = false

    private val cloudflareDns = DnsOverHttps.Builder()
        .client(OkHttpClient.Builder().build())
        .url("https://1.1.1.1/dns-query".toHttpUrl())
        .bootstrapDnsHosts(
            InetAddress.getByName("1.1.1.1"),
            InetAddress.getByName("1.0.0.1"),
            InetAddress.getByName("2606:4700:4700::1111"),
            InetAddress.getByName("2606:4700:4700::1001")
        )
        .resolvePrivateAddresses(true)
        .build()

    override fun lookup(hostname: String): List<InetAddress> {
        if (!isEnabled) {
            return Dns.SYSTEM.lookup(hostname)
        }

        return runCatching {
            cloudflareDns.lookup(hostname)
        }.getOrElse {
            Dns.SYSTEM.lookup(hostname)
        }
    }
}
