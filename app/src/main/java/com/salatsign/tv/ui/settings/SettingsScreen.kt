package com.salatsign.tv.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Mosque
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.salatsign.tv.data.model.AdhanAudioOption
import com.salatsign.tv.data.model.AdhanAudioType
import com.salatsign.tv.data.model.AppSettings
import com.salatsign.tv.data.model.Prayer
import com.salatsign.tv.ui.common.overscanPadding
import com.salatsign.tv.ui.common.tvFocusable
import com.salatsign.tv.ui.theme.AccentEmerald
import com.salatsign.tv.ui.theme.AccentGold
import com.salatsign.tv.ui.theme.CardBackground
import com.salatsign.tv.ui.theme.CardFocusedBorder
import com.salatsign.tv.ui.theme.DarkBackground
import com.salatsign.tv.ui.theme.SurfaceVariant
import com.salatsign.tv.ui.theme.TextPrimary
import com.salatsign.tv.ui.theme.TextSecondary
import com.salatsign.tv.ui.theme.TvTypography

@Composable
fun SettingsScreen(
    settings: AppSettings,
    onToggleMosqueMode: (Boolean) -> Unit,
    onSetHijriOffset: (Int) -> Unit,
    onToggleReminder: (Boolean) -> Unit,
    onSetAdhanAudio: (Prayer, AdhanAudioOption) -> Unit,
    onAddSampleSlide: () -> Unit,
    onClearSlides: () -> Unit,
    onClose: () -> Unit
) {
    val initialFocusRequester = remember { FocusRequester() }
    val scrollState = rememberScrollState()

    LaunchedEffect(Unit) {
        initialFocusRequester.requestFocus()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground.copy(alpha = 0.96f))
            .overscanPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Pengaturan Aplikasi",
                        style = TvTypography.HeaderTitle.copy(fontSize = 36.sp)
                    )
                    Text(
                        text = "Konfigurasi Mode Layar, Adhan, Kalender Hijriyah & Signage",
                        style = TvTypography.Subtitle.copy(fontSize = 18.sp)
                    )
                }

                Surface(
                    onClick = onClose,
                    shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(10.dp)),
                    colors = ClickableSurfaceDefaults.colors(
                        containerColor = CardBackground,
                        focusedContainerColor = AccentEmerald
                    ),
                    modifier = Modifier.tvFocusable()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Tutup",
                            tint = TextPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Tutup (Kembali)", style = TvTypography.ButtonText)
                    }
                }
            }

            // 1. Mode Operasi Selection (Home vs Mosque Mode)
            Text(
                text = "1. Mode Operasi Layar",
                style = TvTypography.CardTitle.copy(color = AccentGold)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Home Mode Card
                Card(
                    onClick = { onToggleMosqueMode(false) },
                    colors = CardDefaults.colors(
                        containerColor = if (!settings.isMosqueMode) SurfaceVariant else CardBackground
                    ),
                    shape = CardDefaults.shape(RoundedCornerShape(14.dp)),
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(initialFocusRequester)
                        .tvFocusable()
                        .then(
                            if (!settings.isMosqueMode) Modifier.border(2.dp, AccentEmerald, RoundedCornerShape(14.dp))
                            else Modifier
                        )
                ) {
                    Row(
                        modifier = Modifier.padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Home,
                            contentDescription = null,
                            tint = if (!settings.isMosqueMode) AccentEmerald else TextSecondary,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = "Mode Rumah / Personal",
                                style = TvTypography.CardTitle.copy(fontSize = 20.sp)
                            )
                            Text(
                                text = "Interaktif D-pad, Notifikasi 15m, Suara Adhan otomatis menyala",
                                style = TvTypography.BodyLabel.copy(fontSize = 14.sp)
                            )
                        }
                    }
                }

                // Mosque Mode Card
                Card(
                    onClick = { onToggleMosqueMode(true) },
                    colors = CardDefaults.colors(
                        containerColor = if (settings.isMosqueMode) SurfaceVariant else CardBackground
                    ),
                    shape = CardDefaults.shape(RoundedCornerShape(14.dp)),
                    modifier = Modifier
                        .weight(1f)
                        .tvFocusable()
                        .then(
                            if (settings.isMosqueMode) Modifier.border(2.dp, AccentEmerald, RoundedCornerShape(14.dp))
                            else Modifier
                        )
                ) {
                    Row(
                        modifier = Modifier.padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mosque,
                            contentDescription = null,
                            tint = if (settings.isMosqueMode) AccentEmerald else TextSecondary,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = "Mode Masjid / Signage",
                                style = TvTypography.CardTitle.copy(fontSize = 20.sp)
                            )
                            Text(
                                text = "White-label tanpa logo aplikasi, rotasi slide otomatis, hening 100%",
                                style = TvTypography.BodyLabel.copy(fontSize = 14.sp)
                            )
                        }
                    }
                }
            }

            // 2. Hijriyah Date Offset
            Text(
                text = "2. Penyesuaian Kalender Hijriyah (Offset Hari: ${settings.hijriOffsetDays} Hari)",
                style = TvTypography.CardTitle.copy(color = AccentGold)
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val offsets = listOf(-2, -1, 0, 1, 2)
                for (offset in offsets) {
                    val isSelected = (settings.hijriOffsetDays == offset)
                    Surface(
                        onClick = { onSetHijriOffset(offset) },
                        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(8.dp)),
                        colors = ClickableSurfaceDefaults.colors(
                            containerColor = if (isSelected) AccentEmerald else CardBackground,
                            focusedContainerColor = AccentGold
                        ),
                        modifier = Modifier.tvFocusable()
                    ) {
                        Text(
                            text = if (offset > 0) "+$offset Hari" else "$offset Hari",
                            style = TvTypography.ButtonText.copy(
                                fontSize = 16.sp,
                                color = if (isSelected) DarkBackground else TextPrimary
                            ),
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        )
                    }
                }
            }

            // 3. Audio & Reminders (Only active in Home Mode)
            Text(
                text = "3. Pengingat & Suara Adhan Per-Shalat (Mode Rumah)",
                style = TvTypography.CardTitle.copy(color = AccentGold)
            )

            // Reminder toggle button
            Surface(
                onClick = { onToggleReminder(!settings.prePrayerReminderEnabled) },
                shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(10.dp)),
                colors = ClickableSurfaceDefaults.colors(
                    containerColor = if (settings.prePrayerReminderEnabled) SurfaceVariant else CardBackground
                ),
                modifier = Modifier.tvFocusable()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.NotificationsActive,
                        contentDescription = null,
                        tint = if (settings.prePrayerReminderEnabled) AccentEmerald else TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = if (settings.prePrayerReminderEnabled)
                            "Pengingat 15 Menit Sebelum Shalat: AKTIF"
                        else
                            "Pengingat 15 Menit Sebelum Shalat: NONAKTIF",
                        style = TvTypography.ButtonText.copy(fontSize = 16.sp)
                    )
                }
            }

            // Per-prayer adhan selector buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val prayers = listOf(Prayer.SUBUH, Prayer.ZUHUR, Prayer.ASHR, Prayer.MAGRIB, Prayer.ISYA)
                for (prayer in prayers) {
                    val currentOption = settings.adhanFor(prayer)
                    Card(
                        onClick = {
                            // Cycle through options: Default -> Silent -> Standard
                            val nextOption = when (currentOption.type) {
                                AdhanAudioType.DEFAULT_SUBUH, AdhanAudioType.DEFAULT_STANDARD -> AdhanAudioOption.Silent
                                AdhanAudioType.SILENT -> if (prayer == Prayer.SUBUH) AdhanAudioOption.DefaultSubuh else AdhanAudioOption.DefaultStandard
                                AdhanAudioType.CUSTOM_FILE -> AdhanAudioOption.DefaultStandard
                            }
                            onSetAdhanAudio(prayer, nextOption)
                        },
                        colors = CardDefaults.colors(containerColor = CardBackground),
                        shape = CardDefaults.shape(RoundedCornerShape(10.dp)),
                        modifier = Modifier
                            .weight(1f)
                            .tvFocusable()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = prayer.displayName,
                                style = TvTypography.CardTitle.copy(fontSize = 16.sp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = currentOption.displayName,
                                style = TvTypography.BodyLabel.copy(
                                    fontSize = 13.sp,
                                    color = if (currentOption.type == AdhanAudioType.SILENT) Color(0xFFEF4444) else AccentEmerald
                                )
                            )
                        }
                    }
                }
            }

            // 4. Mosque White-Label Branding & Slides
            Text(
                text = "4. Pengaturan Tampilan Masjid & Slide Pengumuman",
                style = TvTypography.CardTitle.copy(color = AccentGold)
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    onClick = onAddSampleSlide,
                    shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(8.dp)),
                    colors = ClickableSurfaceDefaults.colors(containerColor = CardBackground),
                    modifier = Modifier.tvFocusable()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddPhotoAlternate,
                            contentDescription = null,
                            tint = AccentEmerald,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Tambah Contoh Poster Slide", style = TvTypography.ButtonText.copy(fontSize = 15.sp))
                    }
                }

                if (settings.announcementSlides.isNotEmpty()) {
                    Surface(
                        onClick = onClearSlides,
                        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(8.dp)),
                        colors = ClickableSurfaceDefaults.colors(containerColor = CardBackground),
                        modifier = Modifier.tvFocusable()
                    ) {
                        Text(
                            text = "Hapus Semua Slide (${settings.announcementSlides.size})",
                            style = TvTypography.ButtonText.copy(fontSize = 15.sp, color = Color(0xFFEF4444)),
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
