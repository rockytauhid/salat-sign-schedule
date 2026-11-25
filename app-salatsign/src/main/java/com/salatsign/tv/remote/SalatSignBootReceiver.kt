// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Rocky Adam Tauhid and SalatSign contributors

package com.salatsign.tv.remote

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.salatsign.tv.MainActivity
import com.salatsign.tv.SalatSignApp
import com.salatsign.tv.data.repository.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

/**
 * Boot and app-update receiver for SalatSign Always-On Phone Controller (Phase 2, D24).
 * When enabled, auto-launches the signage kiosk activity and starts the in-process LAN server
 * without requiring any ForegroundService or notification permissions (preserving D18).
 */
class SalatSignBootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "SalatSignBootReceiver"
        internal val ALLOWED_ACTIONS = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED
        )
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action !in ALLOWED_ACTIONS) {
            Log.w(TAG, "Ignoring unexpected broadcast action=$action")
            return
        }

        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                withTimeout(8_000L) {
                    val repo = (context.applicationContext as? SalatSignApp)?.settingsRepository
                        ?: SettingsRepository(context.applicationContext)
                    val settings = repo.appSettingsFlow.first()
                    if (!settings.phoneControllerAlwaysOn) {
                        Log.d(TAG, "Always-on Phone Controller disabled; skipping boot launch ($action)")
                        return@withTimeout
                    }

                    Log.d(TAG, "Always-on Phone Controller enabled; launching SalatSign after $action")
                    (context.applicationContext as? SalatSignApp)?.setPhoneControllerEnabled(true)

                    val launchIntent = Intent(context, MainActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    }
                    runCatching { context.startActivity(launchIntent) }
                        .onFailure { Log.w(TAG, "Unable to auto-launch MainActivity on $action", it) }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error handling $action", e)
            } finally {
                pending.finish()
            }
        }
    }
}
