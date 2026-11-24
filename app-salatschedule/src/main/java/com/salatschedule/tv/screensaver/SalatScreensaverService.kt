// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Rocky Adam Tauhid and SalatSign contributors

package com.salatschedule.tv.screensaver

import android.content.Context
import android.service.dreams.DreamService
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.salatschedule.tv.SalatScheduleApp
import com.salatsign.tv.data.repository.LanguageSyncCache
import com.salatsign.tv.ui.common.LocaleHelper

/**
 * Android TV Screensaver (DreamService) for SalatSchedule.
 * Strictly silent, non-interactive, dark-only, and burn-in safe.
 */
class SalatScreensaverService : DreamService() {

    private var lifecycleOwner: DreamComposeOwner? = null
    private var composeView: ComposeView? = null

    override fun attachBaseContext(newBase: Context) {
        val language = LanguageSyncCache.getLanguage(newBase)
        super.attachBaseContext(LocaleHelper.wrap(newBase, language))
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        isInteractive = false
        isFullscreen = true
        isScreenBright = true

        val owner = DreamComposeOwner().also {
            it.onCreate()
            lifecycleOwner = it
        }

        window?.decorView?.let { decor ->
            decor.setViewTreeLifecycleOwner(owner)
            decor.setViewTreeSavedStateRegistryOwner(owner)
            decor.setViewTreeViewModelStoreOwner(owner)
        }

        val app = application as SalatScheduleApp
        val view = ComposeView(this).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                ScreensaverHost(
                    settingsRepository = app.settingsRepository,
                    quranVerseRepository = app.quranVerseRepository
                )
            }
        }
        composeView = view
        setContentView(view)
    }

    override fun onDreamingStarted() {
        super.onDreamingStarted()
        lifecycleOwner?.onResume()
    }

    override fun onDreamingStopped() {
        lifecycleOwner?.onDestroy()
        composeView?.disposeComposition()
        composeView = null
        lifecycleOwner = null
        super.onDreamingStopped()
    }

    override fun onDetachedFromWindow() {
        lifecycleOwner?.onDestroy()
        composeView?.disposeComposition()
        composeView = null
        lifecycleOwner = null
        super.onDetachedFromWindow()
    }
}

/**
 * Minimal LifecycleOwner + SavedStateRegistryOwner + ViewModelStoreOwner for hosting
 * Jetpack Compose safely inside a DreamService on API 26+.
 */
private class DreamComposeOwner : LifecycleOwner, SavedStateRegistryOwner, ViewModelStoreOwner {
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateController = SavedStateRegistryController.create(this)
    private val store = ViewModelStore()
    private var destroyed = false

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry get() = savedStateController.savedStateRegistry
    override val viewModelStore: ViewModelStore get() = store

    fun onCreate() {
        savedStateController.performAttach()
        savedStateController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
    }

    fun onResume() {
        if (destroyed) return
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }

    fun onDestroy() {
        if (destroyed) return
        destroyed = true
        if (lifecycleRegistry.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        }
        if (lifecycleRegistry.currentState.isAtLeast(Lifecycle.State.STARTED)) {
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        }
        if (lifecycleRegistry.currentState.isAtLeast(Lifecycle.State.CREATED)) {
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        }
        store.clear()
    }
}
