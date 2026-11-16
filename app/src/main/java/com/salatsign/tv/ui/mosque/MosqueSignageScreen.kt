package com.salatsign.tv.ui.mosque

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Mosque
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import com.salatsign.tv.data.model.PrayerTimeItem
import com.salatsign.tv.ui.common.LiveClockHeader
import com.salatsign.tv.ui.common.overscanPadding
import com.salatsign.tv.ui.common.pulsingGlow
import com.salatsign.tv.ui.home.PrayerCardsRow
import com.salatsign.tv.ui.theme.AccentEmerald
import com.salatsign.tv.ui.theme.AccentGold
import com.salatsign.tv.ui.theme.CardBackground
import com.salatsign.tv.ui.theme.DarkBackground
import com.salatsign.tv.ui.theme.EmeraldLight
import com.salatsign.tv.ui.theme.SurfaceVariant
import com.salatsign.tv.ui.theme.TextPrimary
import com.salatsign.tv.ui.theme.TextSecondary
import com.salatsign.tv.ui.theme.TvTypography
import kotlinx.coroutines.delay

@Composable
fun MosqueSignageScreen(
    uiState: MosqueSignageUiState,
    onOpenSettings: () -> Unit
) {
    val screenFocusRequester = remember { FocusRequester() }
    var centerPressStartTime by remember { mutableLongStateOf(0L) }

    LaunchedEffect(Unit) {
        screenFocusRequester.requestFocus()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .focusRequester(screenFocusRequester)
            .focusable()
            // Hidden Admin Access: Key.Menu or 3-second long-press on Key.DirectionCenter
            .onKeyEvent { keyEvent ->
                if (keyEvent.key == Key.Menu && keyEvent.type == KeyEventType.KeyUp) {
                    onOpenSettings()
                    true
                } else if (keyEvent.key == Key.DirectionCenter) {
                    if (keyEvent.type == KeyEventType.KeyDown) {
                        if (centerPressStartTime == 0L) {
                            centerPressStartTime = System.currentTimeMillis()
                        } else if (System.currentTimeMillis() - centerPressStartTime >= 3000L) {
                            centerPressStartTime = 0L
                            onOpenSettings()
                            return@onKeyEvent true
                        }
                    } else if (keyEvent.type == KeyEventType.KeyUp) {
                        centerPressStartTime = 0L
                    }
                    false
                } else {
                    false
                }
            }
    ) {
        Crossfade(
            targetState = uiState.currentSlideState,
            animationSpec = tween(durationMillis = 800),
            label = "mosque_slideshow_crossfade"
        ) { slideState ->
            when (slideState) {
                is SlideState.PrayerSchedule -> {
                    MosqueScheduleView(
                        uiState = uiState
                    )
                }
                is SlideState.AnnouncementImage -> {
                    MosqueAnnouncementView(
                        slide = slideState.slide,
                        uiState = uiState
                    )
                }
            }
        }
    }
}

@Composable
fun MosqueScheduleView(
    uiState: MosqueSignageUiState
) {
    val prayerTimes = uiState.prayerTimesDay

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(DarkBackground, Color(0xFF040A07))
                )
            )
            .overscanPadding()
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // White-Label Header
            LiveClockHeader(
                isMosqueMode = true,
                gregorianDate = prayerTimes?.gregorianDate ?: "",
                hijriDate = prayerTimes?.hijriDate ?: "",
                cityName = uiState.settings.cityName,
                mosqueName = uiState.settings.mosqueName,
                mosqueSubtitle = uiState.settings.mosqueSubtitle,
                mosqueLogoUri = uiState.settings.mosqueLogoUri,
                isViewingToday = true
            )

            if (prayerTimes != null) {
                // Mosque Big Next / Active Prayer Banner
                if (uiState.interruptedActivePrayer != null) {
                    // Exact-Time Strike Pulsing Banner
                    MosqueActiveStrikeBanner(
                        prayer = prayerTimes.prayers.firstOrNull { it.prayer == uiState.interruptedActivePrayer }
                            ?: prayerTimes.prayers.first(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1.3f)
                    )
                } else if (prayerTimes.nextPrayer != null) {
                    MosqueNextPrayerBanner(
                        nextPrayer = prayerTimes.nextPrayer,
                        countdown = prayerTimes.countdownFormatted,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1.3f)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 5 Prayers Grid / Row
                PrayerCardsRow(
                    prayers = prayerTimes.prayers,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1.0f)
                )
            }
        }
    }
}

@Composable
fun MosqueActiveStrikeBanner(
    prayer: PrayerTimeItem,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = {},
        colors = CardDefaults.colors(containerColor = SurfaceVariant),
        shape = CardDefaults.shape(RoundedCornerShape(20.dp)),
        modifier = modifier
            .pulsingGlow(shape = RoundedCornerShape(20.dp), glowColor = AccentEmerald)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 40.dp, vertical = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                verticalArrangement = Arrangement.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .background(AccentEmerald, RoundedCornerShape(8.dp))
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "WAKTU SHALAT TELAH TIBA",
                            style = TvTypography.BodyLabel.copy(
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = DarkBackground
                            )
                        )
                    }

                    Text(
                        text = prayer.prayer.arabicName,
                        style = TvTypography.ArabicTitle.copy(fontSize = 32.sp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Shalat ${prayer.prayer.displayName}",
                    style = TvTypography.HeaderTitle.copy(fontSize = 54.sp, fontWeight = FontWeight.ExtraBold)
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Luruskan dan rapatkan shaf. Harap menonaktifkan nada dering ponsel.",
                    style = TvTypography.Subtitle.copy(fontSize = 22.sp, color = AccentGold)
                )
            }

            Text(
                text = prayer.formattedTime,
                style = TvTypography.ActivePrayerTime.copy(fontSize = 84.sp, color = AccentEmerald)
            )
        }
    }
}

@Composable
fun MosqueNextPrayerBanner(
    nextPrayer: PrayerTimeItem,
    countdown: String,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = {},
        colors = CardDefaults.colors(containerColor = CardBackground),
        shape = CardDefaults.shape(RoundedCornerShape(20.dp)),
        modifier = modifier
            .border(width = 1.5.dp, color = AccentEmerald.copy(alpha = 0.35f), shape = RoundedCornerShape(20.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 40.dp, vertical = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                verticalArrangement = Arrangement.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .background(AccentEmerald.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "MENUJU SHALAT BERIKUTNYA",
                            style = TvTypography.BodyLabel.copy(
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldLight
                            )
                        )
                    }

                    Text(
                        text = nextPrayer.prayer.arabicName,
                        style = TvTypography.ArabicTitle.copy(fontSize = 26.sp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = nextPrayer.prayer.displayName,
                    style = TvTypography.HeaderTitle.copy(fontSize = 50.sp, fontWeight = FontWeight.ExtraBold)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AccessTime,
                        contentDescription = null,
                        tint = AccentGold,
                        modifier = Modifier.size(28.dp)
                    )
                    Text(
                        text = "Iqamah / Adhan: $countdown",
                        style = TvTypography.CountdownText.copy(fontSize = 46.sp)
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = nextPrayer.formattedTime,
                    style = TvTypography.ActivePrayerTime.copy(fontSize = 80.sp)
                )
            }
        }
    }
}

@Composable
fun MosqueAnnouncementView(
    slide: com.salatsign.tv.data.model.AnnouncementSlide,
    uiState: MosqueSignageUiState
) {
    val prayerTimes = uiState.prayerTimesDay

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Signage Image via Coil 3
        AsyncImage(
            model = slide.imageUri,
            contentDescription = slide.title.ifBlank { "Mosque Announcement Slide" },
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Overlay: Bottom floating prayer mini-bar with live clock so congregation always sees time
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color(0xDD000000), Color(0xF5000000))
                    )
                )
                .padding(horizontal = 48.dp, vertical = 20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: Slide Title or Mosque Name
                Column {
                    if (slide.title.isNotBlank()) {
                        Text(
                            text = slide.title,
                            style = TvTypography.HeaderTitle.copy(fontSize = 28.sp)
                        )
                    } else if (uiState.settings.mosqueName.isNotBlank()) {
                        Text(
                            text = uiState.settings.mosqueName,
                            style = TvTypography.HeaderTitle.copy(fontSize = 28.sp)
                        )
                    }
                    if (uiState.settings.mosqueSubtitle.isNotBlank()) {
                        Text(
                            text = uiState.settings.mosqueSubtitle,
                            style = TvTypography.Subtitle.copy(fontSize = 18.sp, color = AccentGold)
                        )
                    }
                }

                // Center: 5 Prayers mini times
                if (prayerTimes != null) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        for (p in prayerTimes.prayers) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = p.prayer.displayName,
                                    style = TvTypography.BodyLabel.copy(
                                        fontSize = 14.sp,
                                        fontWeight = if (p.isNext) FontWeight.Bold else FontWeight.Normal,
                                        color = if (p.isNext) AccentGold else TextSecondary
                                    )
                                )
                                Text(
                                    text = p.formattedTime,
                                    style = TvTypography.BodyLabel.copy(
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (p.isNext) AccentEmerald else TextPrimary
                                    )
                                )
                            }
                        }
                    }
                }

                // Right: Hijri Date
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = prayerTimes?.hijriDate ?: "",
                        style = TvTypography.Subtitle.copy(
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = AccentGold
                        )
                    )
                    Text(
                        text = prayerTimes?.gregorianDate ?: "",
                        style = TvTypography.BodyLabel.copy(fontSize = 16.sp, color = TextPrimary)
                    )
                }
            }
        }
    }
}
