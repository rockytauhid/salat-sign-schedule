// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Rocky Adam Tauhid and SalatSign contributors

package com.salatsign.tv.remote

import java.net.Inet4Address
import java.net.NetworkInterface

object LocalNetworkUtils {

    /**
     * Returns all active non-loopback, non-link-local IPv4 addresses across all up network
     * interfaces (e.g. both Ethernet and Wi-Fi when connected simultaneously).
     */
    fun getAllLocalIpv4Addresses(): Set<String> {
        return runCatching {
            val interfaces = NetworkInterface.getNetworkInterfaces() ?: return emptySet()
            val candidates = linkedSetOf<String>()
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
            candidates
        }.getOrDefault(emptySet())
    }

    /**
     * Returns the preferred active non-loopback IPv4 LAN address on this device, or null if offline.
     */
    fun getLocalIpv4Address(): String? {
        val candidates = getAllLocalIpv4Addresses()
        // Prefer private RFC-1918 LAN addresses (192.168.x.x, 10.x.x.x, 172.16-31.x.x)
        return candidates.firstOrNull { it.startsWith("192.168.") || it.startsWith("10.") || it.startsWith("172.") }
            ?: candidates.firstOrNull()
    }
}
