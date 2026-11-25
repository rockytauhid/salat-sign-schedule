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
    private var isRegistered = false

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
}
