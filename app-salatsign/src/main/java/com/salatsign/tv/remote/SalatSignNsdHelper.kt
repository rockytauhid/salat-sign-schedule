// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Rocky Adam Tauhid and SalatSign contributors

package com.salatsign.tv.remote

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.util.Log
import com.salatsign.tv.data.api.ApiConstants

/**
 * Registers the SalatSign local controller over mDNS (_salatsign._tcp.) so companion phone apps
 * on the same LAN can auto-discover this TV without typing an IP address.
 */
class SalatSignNsdHelper(private val context: Context) {

    companion object {
        private const val TAG = "SalatSignNsdHelper"
        private const val SERVICE_NAME = "SalatSign-TV"
    }

    private var nsdManager: NsdManager? = null
    private var registrationListener: NsdManager.RegistrationListener? = null
    private var discoveryListener: NsdManager.DiscoveryListener? = null
    private var isRegistered = false
    private var isDiscovering = false

    fun register(
        port: Int = ApiConstants.DEFAULT_HTTP_PORT,
        deviceId: String = "",
        deviceName: String = "",
        roomGroup: String = ""
    ) {
        if (isRegistered) return
        try {
            val manager = context.getSystemService(Context.NSD_SERVICE) as? NsdManager ?: return
            val serviceInfo = NsdServiceInfo().apply {
                serviceName = if (deviceName.isNotBlank()) "SalatSign-${deviceName.take(24)}" else SERVICE_NAME
                serviceType = ApiConstants.MDNS_SERVICE_TYPE
                this.port = port
                setAttribute("api", ApiConstants.API_VERSION.toString())
                if (deviceId.isNotBlank()) setAttribute("id", deviceId)
                if (deviceName.isNotBlank()) setAttribute("name", deviceName)
                if (roomGroup.isNotBlank()) setAttribute("group", roomGroup)
            }
            val listener = object : NsdManager.RegistrationListener {
                override fun onServiceRegistered(info: NsdServiceInfo) {
                    Log.d(TAG, "mDNS registered: ${info.serviceName} (${info.serviceType}) on port $port")
                }

                override fun onRegistrationFailed(info: NsdServiceInfo, errorCode: Int) {
                    Log.w(TAG, "mDNS registration failed: code=$errorCode")
                    isRegistered = false
                }

                override fun onServiceUnregistered(info: NsdServiceInfo) {
                    Log.d(TAG, "mDNS unregistered: ${info.serviceName}")
                }

                override fun onUnregistrationFailed(info: NsdServiceInfo, errorCode: Int) {
                    Log.w(TAG, "mDNS unregistration failed: code=$errorCode")
                }
            }
            nsdManager = manager
            registrationListener = listener
            manager.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, listener)
            isRegistered = true
        } catch (e: Exception) {
            Log.w(TAG, "Failed to register mDNS service", e)
            isRegistered = false
        }
    }

    fun unregister() {
        stopPeerDiscovery()
        if (!isRegistered) return
        try {
            registrationListener?.let { nsdManager?.unregisterService(it) }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to unregister mDNS service", e)
        } finally {
            isRegistered = false
            registrationListener = null
        }
    }

    /**
     * Discovers peer SalatSign TVs on the LAN via mDNS (_salatsign._tcp.) so peer IP addresses
     * automatically refresh if a TV's DHCP IP changes (Phase 3a).
     */
    @Suppress("DEPRECATION")
    fun startPeerDiscovery(
        selfDeviceId: String,
        onPeerResolved: (deviceId: String, deviceName: String, roomGroup: String, host: String, port: Int) -> Unit
    ) {
        if (isDiscovering) return
        try {
            val manager = (nsdManager ?: context.getSystemService(Context.NSD_SERVICE) as? NsdManager) ?: return
            nsdManager = manager
            val listener = object : NsdManager.DiscoveryListener {
                override fun onDiscoveryStarted(regType: String) {
                    Log.d(TAG, "mDNS peer discovery started for $regType")
                }
                override fun onServiceFound(service: NsdServiceInfo) {
                    if (!service.serviceType.contains("_salatsign")) return
                    runCatching {
                        manager.resolveService(service, object : NsdManager.ResolveListener {
                            override fun onResolveFailed(info: NsdServiceInfo, errorCode: Int) {
                                Log.d(TAG, "mDNS resolve failed: code=$errorCode")
                            }
                            override fun onServiceResolved(info: NsdServiceInfo) {
                                val host = info.host?.hostAddress ?: return
                                val attrs = info.attributes
                                val peerId = attrs["id"]?.let { String(it, Charsets.UTF_8) }?.trim().orEmpty()
                                if (peerId.isBlank() || peerId == selfDeviceId) return
                                val peerName = attrs["name"]?.let { String(it, Charsets.UTF_8) }?.trim().orEmpty()
                                val peerGroup = attrs["group"]?.let { String(it, Charsets.UTF_8) }?.trim().orEmpty()
                                onPeerResolved(peerId, peerName, peerGroup, host, info.port)
                            }
                        })
                    }
                }
                override fun onServiceLost(service: NsdServiceInfo) {}
                override fun onDiscoveryStopped(serviceType: String) { isDiscovering = false }
                override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) { isDiscovering = false }
                override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) { isDiscovering = false }
            }
            discoveryListener = listener
            manager.discoverServices(ApiConstants.MDNS_SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, listener)
            isDiscovering = true
        } catch (e: Exception) {
            Log.w(TAG, "Failed to start mDNS peer discovery", e)
            isDiscovering = false
        }
    }

    fun stopPeerDiscovery() {
        if (!isDiscovering) return
        try {
            discoveryListener?.let { nsdManager?.stopServiceDiscovery(it) }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to stop mDNS peer discovery", e)
        } finally {
            isDiscovering = false
            discoveryListener = null
        }
    }
}
