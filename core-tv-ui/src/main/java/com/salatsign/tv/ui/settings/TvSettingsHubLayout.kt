// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Rocky Adam Tauhid and SalatSign contributors

package com.salatsign.tv.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.salatsign.core.tv.R
import com.salatsign.tv.ui.common.overscanPadding
import com.salatsign.tv.ui.common.tvSelectableSurfaceBorder
import com.salatsign.tv.ui.common.tvSelectableSurfaceColors
import com.salatsign.tv.ui.common.tvSurfaceScale
import com.salatsign.tv.ui.theme.TvTypography

data class SettingsHubCategory(
    val id: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val badgeWarning: Boolean = false
)

/**
 * Shared Android TV Settings Hub layout (Left Category Rail + Right Content Pane).
 * Guarantees identical layout, focus ergonomics, and visual design across both
 * SalatSign and SalatSchedule (AGENTS.md Rule 5).
 */
@Composable
fun TvSettingsHubScaffold(
    categories: List<SettingsHubCategory>,
    selectedCategoryId: String,
    onSelectCategory: (String) -> Unit,
    onRestartSetupWizard: () -> Unit,
    onClose: () -> Unit,
    onUserInteraction: () -> Unit,
    modifier: Modifier = Modifier,
    topBanner: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.(selectedCategory: SettingsHubCategory) -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val focusManager = LocalFocusManager.current
    val railScrollState = rememberScrollState()
    val paneScrollState = rememberScrollState()
    val railRequesters = remember(categories.map { it.id }) {
        categories.associate { it.id to FocusRequester() }
    }
    var isRightPaneFocused by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val firstId = selectedCategoryId.takeIf { railRequesters.containsKey(it) }
            ?: categories.firstOrNull()?.id
        firstId?.let { runCatching { railRequesters[it]?.requestFocus() } }
    }

    LaunchedEffect(selectedCategoryId) {
        paneScrollState.scrollTo(0)
    }

    val activeCategory = categories.firstOrNull { it.id == selectedCategoryId } ?: categories.first()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colorScheme.background)
            .overscanPadding()
            .onPreviewKeyEvent { event ->
                onUserInteraction()
                // Pressing Back from inside the Right Pane returns focus cleanly to the active Left Rail item
                if (isRightPaneFocused &&
                    event.type == KeyEventType.KeyDown &&
                    (event.key == Key.Back || event.key == Key.Escape)
                ) {
                    runCatching { railRequesters[activeCategory.id]?.requestFocus() }
                    true
                } else {
                    false
                }
            }
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.settings_title),
                        style = TvTypography.HeaderTitle.copy(fontSize = 30.sp, color = colorScheme.onBackground)
                    )
                    Text(
                        text = "•  ${activeCategory.title}",
                        style = TvTypography.Subtitle.copy(
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colorScheme.secondary
                        )
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    val headerBtnShape = RoundedCornerShape(10.dp)
                    Surface(
                        onClick = onRestartSetupWizard,
                        shape = ClickableSurfaceDefaults.shape(headerBtnShape),
                        scale = tvSurfaceScale(1.03f),
                        colors = tvSelectableSurfaceColors(selected = false),
                        border = tvSelectableSurfaceBorder(selected = false, shape = headerBtnShape)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                tint = colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.settings_open_wizard),
                                style = TvTypography.ButtonText.copy(fontSize = 14.sp, color = colorScheme.onSurface)
                            )
                        }
                    }

                    Surface(
                        onClick = onClose,
                        shape = ClickableSurfaceDefaults.shape(headerBtnShape),
                        scale = tvSurfaceScale(1.03f),
                        colors = tvSelectableSurfaceColors(selected = false),
                        border = tvSelectableSurfaceBorder(selected = false, shape = headerBtnShape)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = stringResource(R.string.btn_close),
                                tint = colorScheme.onSurface,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.btn_close),
                                style = TvTypography.ButtonText.copy(fontSize = 14.sp, color = colorScheme.onSurface)
                            )
                        }
                    }
                }
            }

            if (topBanner != null) {
                topBanner()
                Spacer(modifier = Modifier.height(10.dp))
            }

            // Main Split Body: Left Category Rail + Right Content Pane
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                // Left Category Rail
                Column(
                    modifier = Modifier
                        .width(272.dp)
                        .fillMaxHeight()
                        .verticalScroll(railScrollState)
                        .padding(vertical = 4.dp, horizontal = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val railShape = RoundedCornerShape(12.dp)
                    for (cat in categories) {
                        val isSelected = cat.id == activeCategory.id
                        val requester = railRequesters[cat.id]
                        Surface(
                            onClick = {
                                onSelectCategory(cat.id)
                                focusManager.moveFocus(FocusDirection.Right)
                            },
                            shape = ClickableSurfaceDefaults.shape(railShape),
                            scale = tvSurfaceScale(1.02f),
                            colors = tvSelectableSurfaceColors(selected = isSelected),
                            border = tvSelectableSurfaceBorder(selected = isSelected, shape = railShape),
                            modifier = Modifier
                                .fillMaxWidth()
                                .then(if (requester != null) Modifier.focusRequester(requester) else Modifier)
                                .onFocusChanged { focusState ->
                                    if (focusState.isFocused) {
                                        isRightPaneFocused = false
                                        if (selectedCategoryId != cat.id) {
                                            onSelectCategory(cat.id)
                                        }
                                    }
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = cat.icon,
                                    contentDescription = null,
                                    tint = if (isSelected) colorScheme.primary else colorScheme.onSurface.copy(alpha = 0.72f),
                                    modifier = Modifier.size(22.dp)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = cat.title,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        style = TvTypography.CardTitle.copy(
                                            fontSize = 15.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) colorScheme.primary else colorScheme.onSurface
                                        )
                                    )
                                    if (cat.subtitle.isNotBlank()) {
                                        Text(
                                            text = cat.subtitle,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            style = TvTypography.BodyLabel.copy(
                                                fontSize = 12.sp,
                                                color = colorScheme.onSurface.copy(alpha = 0.68f)
                                            )
                                        )
                                    }
                                }
                                if (cat.badgeWarning) {
                                    Box(
                                        modifier = Modifier
                                            .size(9.dp)
                                            .background(Color(0xFFDC2626), CircleShape)
                                    )
                                }
                            }
                        }
                    }
                }

                // Vertical Divider
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .fillMaxHeight()
                        .background(colorScheme.onSurface.copy(alpha = 0.14f))
                )

                // Right Content Pane
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .onFocusChanged { focusState ->
                            isRightPaneFocused = focusState.hasFocus
                        }
                        .verticalScroll(paneScrollState)
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    content(activeCategory)
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }
    }
}
