package com.salatsign.tv.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.automirrored.filled.NoteAdd
import androidx.compose.material.icons.filled.Mosque
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.Usb
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
import androidx.tv.material3.Text
import com.salatsign.tv.BuildConfig
import com.salatsign.tv.data.importing.ImportKind
import com.salatsign.tv.data.model.AdhanAudioOption
import com.salatsign.tv.data.model.AdhanAudioType
import com.salatsign.tv.data.model.AnnouncementSlide
import com.salatsign.tv.data.model.AppSettings
import com.salatsign.tv.data.model.CityPreset
import com.salatsign.tv.data.model.MosqueFontPreset
import com.salatsign.tv.data.model.MosqueIconPreset
import com.salatsign.tv.data.model.MosqueNamePresets
import com.salatsign.tv.data.model.MosqueSubtitlePresets
import com.salatsign.tv.data.model.PopularCityPresets
import com.salatsign.tv.data.model.Prayer
import com.salatsign.tv.data.model.SlideType
import com.salatsign.tv.data.model.ThemeMode
import com.salatsign.tv.ui.common.MosqueEmblemIcon
import com.salatsign.tv.ui.common.PinDialog
import com.salatsign.tv.ui.common.PinDialogMode
import com.salatsign.tv.ui.common.getMosqueFontFamily
import com.salatsign.tv.ui.common.overscanPadding
import com.salatsign.tv.ui.common.tvFocusable
import com.salatsign.tv.ui.picker.StorageFilePickerScreen
import com.salatsign.tv.ui.theme.AccentEmerald
import com.salatsign.tv.ui.theme.AccentGold
import com.salatsign.tv.ui.theme.DarkBackground
import com.salatsign.tv.ui.theme.TvTypography
import kotlinx.coroutines.delay

@Composable
fun SettingsScreen(
    settings: AppSettings,
    slides: List<AnnouncementSlide> = emptyList(),
    onToggleMosqueMode: (Boolean) -> Unit,
    onSetThemeMode: (ThemeMode) -> Unit,
    onSelectCity: (CityPreset) -> Unit,
    onSetHijriOffset: (Int) -> Unit,
    onToggleReminder: (Boolean) -> Unit,
    onSetAdhanAudio: (Prayer, AdhanAudioOption) -> Unit,
    onSetMosqueName: (String) -> Unit,
    onSetMosqueSubtitle: (String) -> Unit,
    onSetMosqueIconPreset: (MosqueIconPreset) -> Unit,
    onSetMosqueFontPreset: (MosqueFontPreset) -> Unit,
    onUpdateScheduleDuration: (Int) -> Unit,
    onAddSampleSlide: () -> Unit,
    onClearSlides: () -> Unit,
    onSetCustomLogo: (String) -> Unit = {},
    onSetCustomAdhan: (Prayer, String, String) -> Unit = { _, _, _ -> },
    onAddImageSlide: (String, String, Int) -> Unit = { _, _, _ -> },
    onAddTextSlide: (String, String, Int) -> Unit = { _, _, _ -> },
    onDeleteSlide: (String) -> Unit = {},
    onSetCustomPin: (String) -> Unit = {},
    onResetPin: () -> Unit = {},
    onToggleHomePin: (Boolean) -> Unit = {},
    onDismissDefaultPinPrompt: () -> Unit = {},
    onRestartSetupWizard: () -> Unit,
    onClose: () -> Unit
) {
    val initialFocusRequester = remember { FocusRequester() }
    val verticalScrollState = rememberScrollState()
    val colorScheme = MaterialTheme.colorScheme

    var activePickerKind by remember { mutableStateOf<ImportKind?>(null) }
    var pickerTargetPrayer by remember { mutableStateOf<Prayer?>(null) }
    var showChangePinDialog by remember { mutableStateOf(false) }
    var showDefaultPinPromptDialog by remember {
        mutableStateOf(!settings.hasCustomPin && !settings.defaultPinPromptDismissed)
    }

    var lastInteractionTime by remember { mutableLongStateOf(System.currentTimeMillis()) }

    // Auto-close Settings after 2 minutes of inactivity and return to signage/home
    LaunchedEffect(lastInteractionTime) {
        delay(120_000L)
        onClose()
    }

    LaunchedEffect(Unit) {
        initialFocusRequester.requestFocus()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colorScheme.background)
            .overscanPadding()
            .onPreviewKeyEvent {
                lastInteractionTime = System.currentTimeMillis()
                false
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(verticalScrollState),
            verticalArrangement = Arrangement.spacedBy(22.dp)
        ) {
            // Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (settings.isMosqueMode) "Pengaturan Mode Masjid (Signage)" else "Pengaturan Mode Rumah (Personal)",
                        style = TvTypography.HeaderTitle.copy(fontSize = 34.sp, color = colorScheme.onBackground)
                    )
                    Text(
                        text = if (settings.isMosqueMode)
                            "Konfigurasi identitas masjid, ikon, font, rotasi slide pengumuman & tema layar"
                        else
                            "Konfigurasi tema, lokasi kota, suara adhan, dan pengingat waktu shalat",
                        style = TvTypography.Subtitle.copy(fontSize = 18.sp, color = colorScheme.onBackground.copy(alpha = 0.7f))
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Surface(
                        onClick = onRestartSetupWizard,
                        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(10.dp)),
                        colors = ClickableSurfaceDefaults.colors(
                            containerColor = colorScheme.surfaceVariant,
                            focusedContainerColor = AccentGold
                        ),
                        modifier = Modifier.tvFocusable()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                tint = colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Buka Setup Wizard", style = TvTypography.ButtonText.copy(fontSize = 15.sp, color = colorScheme.onSurfaceVariant))
                        }
                    }

                    Surface(
                        onClick = onClose,
                        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(10.dp)),
                        colors = ClickableSurfaceDefaults.colors(
                            containerColor = colorScheme.surface,
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
                                tint = colorScheme.onSurface,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Tutup", style = TvTypography.ButtonText.copy(color = colorScheme.onSurface))
                        }
                    }
                }
            }

            // Security Warning Banner for Default PIN
            if (!settings.hasCustomPin) {
                Card(
                    onClick = { showChangePinDialog = true },
                    colors = CardDefaults.colors(
                        containerColor = Color(0xFF7F1D1D).copy(alpha = 0.35f),
                        focusedContainerColor = Color(0xFF991B1B)
                    ),
                    shape = CardDefaults.shape(RoundedCornerShape(12.dp)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .tvFocusable()
                        .border(1.5.dp, Color(0xFFEF4444).copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = Color(0xFFEF4444),
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text(
                                    text = "Peringatan Keamanan: PIN Bawaan (0000) Masih Digunakan",
                                    style = TvTypography.CardTitle.copy(
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                )
                                Text(
                                    text = "Layar publik TV masjid mudah diakses siapapun. Segera ganti dengan PIN 4-8 digit kustom demi keamanan.",
                                    style = TvTypography.BodyLabel.copy(
                                        fontSize = 13.sp,
                                        color = Color.White.copy(alpha = 0.8f)
                                    )
                                )
                            }
                        }
                        Box(
                            modifier = Modifier
                                .background(Color(0xFFEF4444), RoundedCornerShape(8.dp))
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = "Ubah PIN",
                                style = TvTypography.ButtonText.copy(fontSize = 13.sp, color = Color.White)
                            )
                        }
                    }
                }
            }

            // Mode Toggle Switcher Banner
            Card(
                onClick = { onToggleMosqueMode(!settings.isMosqueMode) },
                colors = CardDefaults.colors(
                    containerColor = colorScheme.surfaceVariant,
                    focusedContainerColor = colorScheme.surface
                ),
                shape = CardDefaults.shape(RoundedCornerShape(14.dp)),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(initialFocusRequester)
                    .tvFocusable()
            ) {
                Row(
                    modifier = Modifier.padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (settings.isMosqueMode) Icons.Default.Mosque else Icons.Default.Home,
                            contentDescription = null,
                            tint = AccentEmerald,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = if (settings.isMosqueMode) "Mode Aktif: MODE MASJID (Digital Signage)" else "Mode Aktif: MODE RUMAH (Personal)",
                                style = TvTypography.CardTitle.copy(fontWeight = FontWeight.Bold, color = colorScheme.onSurfaceVariant)
                            )
                            Text(
                                text = if (settings.isMosqueMode)
                                    "Klik tombol ini untuk beralih ke Mode Rumah (Personal)"
                                else
                                    "Klik tombol ini untuk beralih ke Mode Masjid (Digital Signage)",
                                style = TvTypography.BodyLabel.copy(fontSize = 14.sp, color = colorScheme.onSurfaceVariant.copy(alpha = 0.8f))
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .background(AccentEmerald, RoundedCornerShape(8.dp))
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = if (settings.isMosqueMode) "Ganti ke Mode Rumah" else "Ganti ke Mode Masjid",
                            style = TvTypography.ButtonText.copy(fontSize = 15.sp, color = Color.White)
                        )
                    }
                }
            }

            // 1. Theme Selection (Shared in both modes)
            Text(
                text = "1. Pilihan Tema Warna Layar TV",
                style = TvTypography.CardTitle.copy(color = AccentGold)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                for (mode in ThemeMode.entries) {
                    val isSelected = (settings.themeMode == mode)
                    Card(
                        onClick = { onSetThemeMode(mode) },
                        colors = CardDefaults.colors(
                            containerColor = if (isSelected) colorScheme.surfaceVariant else colorScheme.surface
                        ),
                        shape = CardDefaults.shape(RoundedCornerShape(12.dp)),
                        modifier = Modifier
                            .weight(1f)
                            .tvFocusable()
                            .then(
                                if (isSelected) Modifier.border(2.dp, AccentEmerald, RoundedCornerShape(12.dp))
                                else Modifier
                            )
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = when (mode) {
                                    ThemeMode.ALWAYS_DARK -> Icons.Default.DarkMode
                                    ThemeMode.ALWAYS_LIGHT -> Icons.Default.LightMode
                                    ThemeMode.AUTO_DAY_NIGHT -> Icons.Default.BrightnessAuto
                                },
                                contentDescription = null,
                                tint = if (isSelected) AccentEmerald else colorScheme.onSurface.copy(alpha = 0.6f),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = mode.displayName,
                                    style = TvTypography.CardTitle.copy(fontSize = 16.sp, color = colorScheme.onSurface)
                                )
                                Text(
                                    text = when (mode) {
                                        ThemeMode.ALWAYS_DARK -> "Latar hitam OLED ramah mata"
                                        ThemeMode.ALWAYS_LIGHT -> "Latar cerah elegan ruang terang"
                                        ThemeMode.AUTO_DAY_NIGHT -> "Siang (Syuruq-Magrib), Malam gelap"
                                    },
                                    style = TvTypography.BodyLabel.copy(fontSize = 12.sp, color = colorScheme.onSurface.copy(alpha = 0.7f))
                                )
                            }
                        }
                    }
                }
            }

            // 2. City & Location Selection (Shared)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "2. Lokasi & Kota Waktu Shalat (Saat Ini: ${settings.cityName})",
                    style = TvTypography.CardTitle.copy(color = AccentGold)
                )
            }

            val cityScrollState = rememberScrollState()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(cityScrollState),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                for (city in PopularCityPresets) {
                    val isSelected = (settings.cityName.startsWith(city.name.split(",")[0]))
                    Surface(
                        onClick = { onSelectCity(city) },
                        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(8.dp)),
                        colors = ClickableSurfaceDefaults.colors(
                            containerColor = if (isSelected) AccentEmerald else colorScheme.surface,
                            focusedContainerColor = AccentGold
                        ),
                        modifier = Modifier.tvFocusable()
                    ) {
                        Text(
                            text = city.name,
                            style = TvTypography.ButtonText.copy(
                                fontSize = 15.sp,
                                color = if (isSelected) Color.White else colorScheme.onSurface
                            ),
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        )
                    }
                }
            }

            // 3. Hijriyah Date Offset (Shared)
            Text(
                text = "3. Penyesuaian Kalender Hijriyah (Offset: ${settings.hijriOffsetDays} Hari)",
                style = TvTypography.CardTitle.copy(color = AccentGold)
            )

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                val offsets = listOf(-2, -1, 0, 1, 2)
                for (offset in offsets) {
                    val isSelected = (settings.hijriOffsetDays == offset)
                    Surface(
                        onClick = { onSetHijriOffset(offset) },
                        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(8.dp)),
                        colors = ClickableSurfaceDefaults.colors(
                            containerColor = if (isSelected) AccentEmerald else colorScheme.surface,
                            focusedContainerColor = AccentGold
                        ),
                        modifier = Modifier.tvFocusable()
                    ) {
                        Text(
                            text = if (offset > 0) "+$offset Hari" else "$offset Hari",
                            style = TvTypography.ButtonText.copy(
                                fontSize = 16.sp,
                                color = if (isSelected) Color.White else colorScheme.onSurface
                            ),
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        )
                    }
                }
            }

            // ================= STRICT MODE ISOLATION =================
            if (!settings.isMosqueMode) {
                // ================= HOME MODE ONLY SETTINGS =================
                Text(
                    text = "4. Pengingat 15 Menit & Suara Adhan Otomatis",
                    style = TvTypography.CardTitle.copy(color = AccentGold)
                )

                // 15-min Reminder Toggle
                Surface(
                    onClick = { onToggleReminder(!settings.prePrayerReminderEnabled) },
                    shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(10.dp)),
                    colors = ClickableSurfaceDefaults.colors(
                        containerColor = if (settings.prePrayerReminderEnabled) colorScheme.surfaceVariant else colorScheme.surface
                    ),
                    modifier = Modifier.tvFocusable()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = null,
                            tint = if (settings.prePrayerReminderEnabled) AccentEmerald else colorScheme.onSurface.copy(alpha = 0.5f),
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = if (settings.prePrayerReminderEnabled)
                                    "Pengingat 15 Menit Sebelum Shalat: AKTIF"
                                else
                                    "Pengingat 15 Menit Sebelum Shalat: NONAKTIF",
                                style = TvTypography.ButtonText.copy(fontSize = 16.sp, color = colorScheme.onSurface)
                            )
                            Text(
                                text = "Menampilkan notifikasi pop-up TV 15 menit sebelum masuk waktu shalat",
                                style = TvTypography.BodyLabel.copy(fontSize = 13.sp, color = colorScheme.onSurface.copy(alpha = 0.7f))
                            )
                        }
                    }
                }

                // Per-Prayer Adhan Sound Selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val prayers = listOf(Prayer.SUBUH, Prayer.ZUHUR, Prayer.ASHR, Prayer.MAGRIB, Prayer.ISYA)
                    for (prayer in prayers) {
                        val currentOption = settings.adhanFor(prayer)
                        Card(
                            onClick = {
                                val nextOption = when (currentOption.type) {
                                    AdhanAudioType.DEFAULT_SUBUH, AdhanAudioType.DEFAULT_STANDARD -> AdhanAudioOption.Silent
                                    AdhanAudioType.SILENT -> if (prayer == Prayer.SUBUH) AdhanAudioOption.DefaultSubuh else AdhanAudioOption.DefaultStandard
                                    AdhanAudioType.CUSTOM_FILE -> AdhanAudioOption.DefaultStandard
                                }
                                onSetAdhanAudio(prayer, nextOption)
                            },
                            colors = CardDefaults.colors(containerColor = colorScheme.surface),
                            shape = CardDefaults.shape(RoundedCornerShape(10.dp)),
                            modifier = Modifier
                                .weight(1f)
                                .tvFocusable()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = prayer.displayName,
                                    style = TvTypography.CardTitle.copy(fontSize = 18.sp, color = colorScheme.onSurface)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = currentOption.displayName,
                                    style = TvTypography.BodyLabel.copy(
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (currentOption.type == AdhanAudioType.SILENT) Color(0xFFEF4444) else AccentEmerald
                                    )
                                )
                            }
                        }
                    }
                }

                // Row to browse & select custom audio for each prayer from USB/TV
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val prayers = listOf(Prayer.SUBUH, Prayer.ZUHUR, Prayer.ASHR, Prayer.MAGRIB, Prayer.ISYA)
                    for (prayer in prayers) {
                        Surface(
                            onClick = {
                                pickerTargetPrayer = prayer
                                activePickerKind = ImportKind.CUSTOM_ADHAN
                            },
                            shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(6.dp)),
                            colors = ClickableSurfaceDefaults.colors(
                                containerColor = colorScheme.surfaceVariant,
                                focusedContainerColor = AccentGold
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .tvFocusable()
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Usb,
                                    contentDescription = null,
                                    tint = AccentGold,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "USB / File",
                                    style = TvTypography.BodyLabel.copy(fontSize = 11.sp, color = colorScheme.onSurface)
                                )
                            }
                        }
                    }
                }

                // 5. Security & Admin PIN (Home Mode)
                Text(
                    text = "5. Keamanan & Kunci Pengaturan PIN",
                    style = TvTypography.CardTitle.copy(color = AccentGold)
                )

                Surface(
                    onClick = { onToggleHomePin(!settings.homeModePinRequired) },
                    shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(10.dp)),
                    colors = ClickableSurfaceDefaults.colors(
                        containerColor = if (settings.homeModePinRequired) colorScheme.surfaceVariant else colorScheme.surface
                    ),
                    modifier = Modifier.tvFocusable()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = if (settings.homeModePinRequired) AccentEmerald else colorScheme.onSurface.copy(alpha = 0.5f),
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = if (settings.homeModePinRequired)
                                        "Kunci Pengaturan dengan PIN: AKTIF"
                                    else
                                        "Kunci Pengaturan dengan PIN: NONAKTIF",
                                    style = TvTypography.ButtonText.copy(fontSize = 16.sp, color = colorScheme.onSurface)
                                )
                                Text(
                                    text = if (settings.homeModePinRequired)
                                        "Membuka menu Pengaturan memerlukan PIN (${if (settings.hasCustomPin) "PIN Kustom" else "PIN Bawaan 0000"})"
                                    else
                                        "Aktifkan agar anak-anak atau anggota keluarga tidak sengaja mengubah jadwal",
                                    style = TvTypography.BodyLabel.copy(fontSize = 13.sp, color = colorScheme.onSurface.copy(alpha = 0.7f))
                                )
                            }
                        }

                        if (settings.homeModePinRequired) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Surface(
                                    onClick = { showChangePinDialog = true },
                                    shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(8.dp)),
                                    colors = ClickableSurfaceDefaults.colors(
                                        containerColor = AccentEmerald,
                                        focusedContainerColor = AccentGold
                                    ),
                                    modifier = Modifier.tvFocusable()
                                ) {
                                    Text(
                                        text = "Ubah PIN",
                                        style = TvTypography.ButtonText.copy(fontSize = 13.sp, color = Color.White),
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                }

                                if (settings.hasCustomPin) {
                                    Surface(
                                        onClick = onResetPin,
                                        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(8.dp)),
                                        colors = ClickableSurfaceDefaults.colors(
                                            containerColor = colorScheme.surfaceVariant,
                                            focusedContainerColor = Color(0xFFEF4444)
                                        ),
                                        modifier = Modifier.tvFocusable()
                                    ) {
                                        Text(
                                            text = "Reset ke 0000",
                                            style = TvTypography.ButtonText.copy(fontSize = 13.sp, color = colorScheme.onSurface),
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

            } else {
                // ================= MOSQUE MODE ONLY SETTINGS =================

                // Mosque Audio Info Note
                Card(
                    onClick = {},
                    colors = CardDefaults.colors(containerColor = colorScheme.surfaceVariant),
                    shape = CardDefaults.shape(RoundedCornerShape(12.dp)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsOff,
                            contentDescription = null,
                            tint = AccentGold,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Text(
                            text = "Mode Masjid Beroperasi Hening Total: Suara Adhan audio dan notifikasi pop-up secara otomatis dinonaktifkan demi menjaga ketertiban jamaah.",
                            style = TvTypography.BodyLabel.copy(fontSize = 15.sp, color = colorScheme.onSurfaceVariant)
                        )
                    }
                }

                // 4. Mosque Icon & Emblem Preset Selector
                Text(
                    text = "4. Pilihan Ikon / Simbol Masjid",
                    style = TvTypography.CardTitle.copy(color = AccentGold)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    for (preset in MosqueIconPreset.entries) {
                        val isSelected = (settings.mosqueIconPreset == preset)
                        Card(
                            onClick = { onSetMosqueIconPreset(preset) },
                            colors = CardDefaults.colors(
                                containerColor = if (isSelected) colorScheme.surfaceVariant else colorScheme.surface
                            ),
                            shape = CardDefaults.shape(RoundedCornerShape(12.dp)),
                            modifier = Modifier
                                .weight(1f)
                                .tvFocusable()
                                .then(
                                    if (isSelected) Modifier.border(2.dp, AccentEmerald, RoundedCornerShape(12.dp))
                                    else Modifier
                                )
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                MosqueEmblemIcon(preset = preset, customLogoUri = settings.mosqueLogoUri, size = 48.dp)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = preset.displayName,
                                    style = TvTypography.BodyLabel.copy(
                                        fontSize = 13.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = colorScheme.onSurface
                                    )
                                )
                            }
                        }
                    }
                }

                if (settings.mosqueIconPreset == MosqueIconPreset.CUSTOM) {
                    Surface(
                        onClick = { activePickerKind = ImportKind.MOSQUE_LOGO },
                        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(8.dp)),
                        colors = ClickableSurfaceDefaults.colors(
                            containerColor = colorScheme.surfaceVariant,
                            focusedContainerColor = AccentEmerald
                        ),
                        modifier = Modifier.tvFocusable()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Upload,
                                contentDescription = null,
                                tint = AccentGold,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (!settings.mosqueLogoUri.isNullOrBlank())
                                    "Ganti Logo Masjid dari USB (Aktif: ${settings.mosqueLogoUri})"
                                else
                                    "Pilih Logo Masjid dari USB Flashdisk (JPG / PNG)",
                                style = TvTypography.ButtonText.copy(fontSize = 14.sp, color = colorScheme.onSurface)
                            )
                        }
                    }
                }

                // 5. Mosque Name & Font Selector
                Text(
                    text = "5. Nama Masjid (Pilih Preset Nama):",
                    style = TvTypography.CardTitle.copy(color = AccentGold)
                )

                val nameScrollState = rememberScrollState()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(nameScrollState),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    for (name in MosqueNamePresets) {
                        val isSelected = (settings.mosqueName == name)
                        Surface(
                            onClick = { onSetMosqueName(name) },
                            shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(8.dp)),
                            colors = ClickableSurfaceDefaults.colors(
                                containerColor = if (isSelected) AccentEmerald else colorScheme.surface,
                                focusedContainerColor = AccentGold
                            ),
                            modifier = Modifier.tvFocusable()
                        ) {
                            Text(
                                text = name,
                                style = TvTypography.ButtonText.copy(
                                    fontSize = 15.sp,
                                    color = if (isSelected) Color.White else colorScheme.onSurface
                                ),
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                            )
                        }
                    }
                }

                Text(
                    text = "Gaya Font Nama Masjid:",
                    style = TvTypography.CardTitle.copy(fontSize = 18.sp, color = colorScheme.onSurface)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    for (font in MosqueFontPreset.entries) {
                        val isSelected = (settings.mosqueFontPreset == font)
                        Card(
                            onClick = { onSetMosqueFontPreset(font) },
                            colors = CardDefaults.colors(
                                containerColor = if (isSelected) colorScheme.surfaceVariant else colorScheme.surface
                            ),
                            shape = CardDefaults.shape(RoundedCornerShape(10.dp)),
                            modifier = Modifier
                                .weight(1f)
                                .tvFocusable()
                                .then(
                                    if (isSelected) Modifier.border(2.dp, AccentEmerald, RoundedCornerShape(10.dp))
                                    else Modifier
                                )
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = settings.mosqueName,
                                    fontFamily = getMosqueFontFamily(font),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = font.displayName,
                                    style = TvTypography.BodyLabel.copy(fontSize = 12.sp, color = colorScheme.onSurface.copy(alpha = 0.7f))
                                )
                            }
                        }
                    }
                }

                // 6. Subtitle / Tagline Selector
                Text(
                    text = "6. Subtitle / Pesan Jamaah (Pilih Preset):",
                    style = TvTypography.CardTitle.copy(color = AccentGold)
                )

                val subtitleScrollState = rememberScrollState()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(subtitleScrollState),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    for (subtitle in MosqueSubtitlePresets) {
                        val isSelected = (settings.mosqueSubtitle == subtitle)
                        Surface(
                            onClick = { onSetMosqueSubtitle(subtitle) },
                            shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(8.dp)),
                            colors = ClickableSurfaceDefaults.colors(
                                containerColor = if (isSelected) AccentEmerald else colorScheme.surface,
                                focusedContainerColor = AccentGold
                            ),
                            modifier = Modifier.tvFocusable()
                        ) {
                            Text(
                                text = subtitle,
                                style = TvTypography.ButtonText.copy(
                                    fontSize = 14.sp,
                                    color = if (isSelected) Color.White else colorScheme.onSurface
                                ),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                        }
                    }
                }

                // 7. Slideshow & Signage Poster Timing
                Text(
                    text = "7. Pengaturan Durasi Jadwal & Poster Slide Pengumuman",
                    style = TvTypography.CardTitle.copy(color = AccentGold)
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Durasi Tampilan Jadwal Shalat:",
                        style = TvTypography.BodyLabel.copy(fontSize = 16.sp, color = colorScheme.onSurface)
                    )

                    val durations = listOf(30, 60, 90, 120)
                    for (dur in durations) {
                        val isSelected = (settings.scheduleDurationSeconds == dur)
                        Surface(
                            onClick = { onUpdateScheduleDuration(dur) },
                            shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(8.dp)),
                            colors = ClickableSurfaceDefaults.colors(
                                containerColor = if (isSelected) AccentEmerald else colorScheme.surface,
                                focusedContainerColor = AccentGold
                            ),
                            modifier = Modifier.tvFocusable()
                        ) {
                            Text(
                                text = "${dur}s",
                                style = TvTypography.ButtonText.copy(
                                    fontSize = 15.sp,
                                    color = if (isSelected) Color.White else colorScheme.onSurface
                                ),
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Surface(
                        onClick = { activePickerKind = ImportKind.SLIDE_IMAGE },
                        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(8.dp)),
                        colors = ClickableSurfaceDefaults.colors(containerColor = colorScheme.surface),
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
                            Text("Tambah Slide Gambar (USB/TV)", style = TvTypography.ButtonText.copy(fontSize = 15.sp, color = colorScheme.onSurface))
                        }
                    }

                    Surface(
                        onClick = {
                            onAddTextSlide("Maklumat Jamaah", "Mari luruskan dan rapatkan shaf sebelum memulai shalat berjamaah.", 15)
                        },
                        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(8.dp)),
                        colors = ClickableSurfaceDefaults.colors(containerColor = colorScheme.surface),
                        modifier = Modifier.tvFocusable()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.NoteAdd,
                                contentDescription = null,
                                tint = AccentGold,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Tambah Slide Teks", style = TvTypography.ButtonText.copy(fontSize = 15.sp, color = colorScheme.onSurface))
                        }
                    }

                    if (BuildConfig.DEBUG) {
                        Surface(
                            onClick = onAddSampleSlide,
                            shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(8.dp)),
                            colors = ClickableSurfaceDefaults.colors(containerColor = colorScheme.surface),
                            modifier = Modifier.tvFocusable()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Contoh (Debug)", style = TvTypography.ButtonText.copy(fontSize = 15.sp, color = colorScheme.onSurface))
                            }
                        }
                    }

                    if (slides.isNotEmpty()) {
                        Surface(
                            onClick = onClearSlides,
                            shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(8.dp)),
                            colors = ClickableSurfaceDefaults.colors(containerColor = colorScheme.surface),
                            modifier = Modifier.tvFocusable()
                        ) {
                            Text(
                                text = "Hapus Semua (${slides.size})",
                                style = TvTypography.ButtonText.copy(fontSize = 15.sp, color = Color(0xFFEF4444)),
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                            )
                        }
                    }
                }

                if (slides.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        slides.forEachIndexed { index, slide ->
                            Surface(
                                onClick = {},
                                shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(8.dp)),
                                colors = ClickableSurfaceDefaults.colors(containerColor = colorScheme.surfaceVariant),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .background(colorScheme.surface, RoundedCornerShape(4.dp))
                                                .padding(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = "#${index + 1} ${if (slide.type == SlideType.IMAGE) "GAMBAR" else "TEKS"}",
                                                style = TvTypography.BodyLabel.copy(fontSize = 12.sp, color = AccentEmerald, fontWeight = FontWeight.Bold)
                                            )
                                        }
                                        Text(
                                            text = slide.title.ifBlank { "(Tanpa Judul)" },
                                            style = TvTypography.BodyLabel.copy(fontSize = 15.sp, color = colorScheme.onSurface)
                                        )
                                        Text(
                                            text = "(${slide.durationSeconds} detik)",
                                            style = TvTypography.BodyLabel.copy(fontSize = 13.sp, color = colorScheme.onSurface.copy(alpha = 0.6f))
                                        )
                                    }

                                    Surface(
                                        onClick = { onDeleteSlide(slide.id) },
                                        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(6.dp)),
                                        colors = ClickableSurfaceDefaults.colors(
                                            containerColor = colorScheme.surface,
                                            focusedContainerColor = MaterialTheme.colorScheme.error
                                        ),
                                        modifier = Modifier.tvFocusable()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Hapus slide",
                                                tint = Color(0xFFEF4444),
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "Hapus",
                                                style = TvTypography.BodyLabel.copy(fontSize = 12.sp, color = Color(0xFFEF4444))
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 8. Mosque Admin PIN Security
                Text(
                    text = "8. Keamanan & PIN Admin Layar Signage",
                    style = TvTypography.CardTitle.copy(color = AccentGold)
                )

                Card(
                    onClick = {},
                    colors = CardDefaults.colors(containerColor = colorScheme.surfaceVariant),
                    shape = CardDefaults.shape(RoundedCornerShape(12.dp)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = if (settings.hasCustomPin) AccentEmerald else AccentGold,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text(
                                    text = if (settings.hasCustomPin) "Status: PIN Kustom Aktif (Aman)" else "Status: PIN Bawaan (0000)",
                                    style = TvTypography.CardTitle.copy(fontSize = 16.sp, fontWeight = FontWeight.Bold, color = colorScheme.onSurfaceVariant)
                                )
                                Text(
                                    text = if (settings.hasCustomPin)
                                        "PIN disimpan dalam bentuk salted hash (PBKDF2). Reset data aplikasi akan mengembalikan PIN ke 0000."
                                    else
                                        "PIN 0000 mudah ditebak. Sangat disarankan mengganti PIN agar layar masjid aman dari akses tidak berizin.",
                                    style = TvTypography.BodyLabel.copy(fontSize = 13.sp, color = colorScheme.onSurfaceVariant.copy(alpha = 0.8f))
                                )
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Surface(
                                onClick = { showChangePinDialog = true },
                                shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(8.dp)),
                                colors = ClickableSurfaceDefaults.colors(
                                    containerColor = AccentEmerald,
                                    focusedContainerColor = AccentGold
                                ),
                                modifier = Modifier.tvFocusable()
                            ) {
                                Text(
                                    text = "Ubah PIN Admin",
                                    style = TvTypography.ButtonText.copy(fontSize = 14.sp, color = Color.White),
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                                )
                            }

                            if (settings.hasCustomPin) {
                                Surface(
                                    onClick = onResetPin,
                                    shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(8.dp)),
                                    colors = ClickableSurfaceDefaults.colors(
                                        containerColor = colorScheme.surface,
                                        focusedContainerColor = Color(0xFFEF4444)
                                    ),
                                    modifier = Modifier.tvFocusable()
                                ) {
                                    Text(
                                        text = "Reset ke 0000",
                                        style = TvTypography.ButtonText.copy(fontSize = 14.sp, color = colorScheme.onSurface),
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        // Overlay: TV D-pad Storage File Picker
        if (activePickerKind != null) {
            StorageFilePickerScreen(
                kind = activePickerKind!!,
                title = when (activePickerKind) {
                    ImportKind.MOSQUE_LOGO -> "Pilih Logo Masjid (USB / TV)"
                    ImportKind.CUSTOM_ADHAN -> "Pilih Audio Adhan (USB / TV)"
                    ImportKind.SLIDE_IMAGE -> "Pilih Gambar Slide Pengumuman (USB / TV)"
                    else -> "Pilih File Media"
                },
                onFileImported = { fileName ->
                    when (activePickerKind) {
                        ImportKind.MOSQUE_LOGO -> onSetCustomLogo(fileName)
                        ImportKind.CUSTOM_ADHAN -> {
                            val prayer = pickerTargetPrayer
                            if (prayer != null) {
                                onSetCustomAdhan(prayer, fileName, "Adhan Kustom (${fileName.take(15)})")
                            }
                        }
                        ImportKind.SLIDE_IMAGE -> {
                            onAddImageSlide("Pengumuman Masjid", fileName, 15)
                        }
                        null -> {}
                    }
                    activePickerKind = null
                    pickerTargetPrayer = null
                },
                onDismiss = {
                    activePickerKind = null
                    pickerTargetPrayer = null
                }
            )
        }

        // Overlay: Change PIN Dialog
        if (showChangePinDialog) {
            PinDialog(
                mode = PinDialogMode.SET_NEW,
                settings = settings,
                onPinChanged = { newPin ->
                    onSetCustomPin(newPin)
                    showChangePinDialog = false
                },
                onDismiss = { showChangePinDialog = false }
            )
        }

        // Overlay: One-time prompt for default PIN 0000
        if (showDefaultPinPromptDialog) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.85f)),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    colors = SurfaceDefaults.colors(containerColor = colorScheme.surface),
                    modifier = Modifier
                        .width(460.dp)
                        .padding(20.dp)
                        .border(1.5.dp, Color(0xFFEF4444).copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = AccentGold,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Ganti PIN Bawaan (0000)?",
                            style = TvTypography.CardTitle.copy(
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = colorScheme.onSurface
                            )
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Anda saat ini masih menggunakan PIN bawaan 0000. Demi keamanan layar publik masjid, apakah Anda ingin menggantinya sekarang?",
                            style = TvTypography.BodyLabel.copy(
                                fontSize = 14.sp,
                                color = colorScheme.onSurface.copy(alpha = 0.8f)
                            )
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            Surface(
                                onClick = {
                                    showDefaultPinPromptDialog = false
                                    onDismissDefaultPinPrompt()
                                },
                                shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(8.dp)),
                                colors = ClickableSurfaceDefaults.colors(
                                    containerColor = colorScheme.surfaceVariant
                                ),
                                modifier = Modifier.tvFocusable()
                            ) {
                                Text(
                                    text = "Nanti Saja",
                                    style = TvTypography.ButtonText.copy(fontSize = 14.sp, color = colorScheme.onSurface),
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                )
                            }
                            Surface(
                                onClick = {
                                    showDefaultPinPromptDialog = false
                                    onDismissDefaultPinPrompt()
                                    showChangePinDialog = true
                                },
                                shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(8.dp)),
                                colors = ClickableSurfaceDefaults.colors(
                                    containerColor = AccentEmerald,
                                    focusedContainerColor = AccentGold
                                ),
                                modifier = Modifier.tvFocusable()
                            ) {
                                Text(
                                    text = "Ganti PIN Sekarang",
                                    style = TvTypography.ButtonText.copy(fontSize = 14.sp, color = Color.White),
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
