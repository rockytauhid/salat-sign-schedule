// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Rocky Adam Tauhid and SalatSign contributors

package com.salatsign.tv.remote

import java.net.Inet4Address
import java.net.NetworkInterface

object LocalNetworkUtils {

    /**
     * Returns the first active non-loopback IPv4 LAN address on this device, or null if offline.
     */
    fun getLocalIpv4Address(): String? {
        return runCatching {
            val interfaces = NetworkInterface.getNetworkInterfaces() ?: return null
            val candidates = mutableListOf<String>()
            while (interfaces.hasMoreElements()) {
                val iface = interfaces.nextElement()
                if (!iface.isUp || iface.isLoopback || iface.isVirtual) continue
                val addrs = iface.inetAddresses
                while (addrs.hasMoreElements()) {
                    val addr = addrs.nextElement()
                    if (addr is Inet4Address && !addr.isLoopbackAddress && !addr.isLinkLocalAddress) {
                        val host = addr.hostAddress
                        if (!host.isNullOrBlank()) {
                            candidates.add(host)
                        }
                    }
                }
            }
            // Prefer private RFC-1918 LAN addresses (192.168.x.x, 10.x.x.x, 172.16-31.x.x)
            candidates.firstOrNull { it.startsWith("192.168.") || it.startsWith("10.") || it.startsWith("172.") }
                ?: candidates.firstOrNull()
        }.getOrNull()
    }
}
