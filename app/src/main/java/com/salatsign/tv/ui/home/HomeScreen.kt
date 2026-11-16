package com.salatsign.tv.ui.home

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import com.salatsign.tv.data.model.AdhanAudioType
import com.salatsign.tv.data.model.PrayerTimeItem
import com.salatsign.tv.data.model.PrayerTimesDay
import com.salatsign.tv.ui.common.LiveClockHeader
import com.salatsign.tv.ui.common.overscanPadding
import com.salatsign.tv.ui.common.pulsingGlow
import com.salatsign.tv.ui.common.tvFocusable
import com.salatsign.tv.ui.theme.AccentEmerald
import com.salatsign.tv.ui.theme.AccentGold
import com.salatsign.tv.ui.theme.CardBackground
import com.salatsign.tv.ui.theme.CardFocusedBorder
import com.salatsign.tv.ui.theme.DarkBackground
import com.salatsign.tv.ui.theme.EmeraldLight
import com.salatsign.tv.ui.theme.SurfaceVariant
import com.salatsign.tv.ui.theme.TextPrimary
import com.salatsign.tv.ui.theme.TextSecondary
import com.salatsign.tv.ui.theme.TvTypography

@Composable
fun HomeScreen(
    uiState: HomeUiState,
    onPreviousDay: () -> Unit,
    onNextDay: () -> Unit,
    onTodayClick: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val initialFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        initialFocusRequester.requestFocus()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(DarkBackground, Color(0xFF060D09))
                )
            )
            .overscanPadding()
    ) {
        val prayerTimes = uiState.prayerTimesDay

        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Dual Header (Branding + Gregorian/Hijri date + Live clock)
            LiveClockHeader(
                isMosqueMode = false,
                gregorianDate = prayerTimes?.gregorianDate ?: "",
                hijriDate = prayerTimes?.hijriDate ?: "",
                cityName = uiState.settings.cityName,
                isViewingToday = uiState.isViewingToday,
                onPreviousDay = onPreviousDay,
                onNextDay = onNextDay,
                onTodayClick = onTodayClick,
                onSettingsClick = onOpenSettings
            )

            if (prayerTimes != null) {
                // Next Prayer Spotlight Card (Significantly larger with ticking countdown)
                if (uiState.isViewingToday && prayerTimes.nextPrayer != null) {
                    NextPrayerSpotlightCard(
                        nextPrayer = prayerTimes.nextPrayer,
                        countdown = prayerTimes.countdownFormatted,
                        sunrise = prayerTimes.sunrise.format(PrayerTimeItem.TimeFormatter),
                        isReminderEnabled = uiState.settings.prePrayerReminderEnabled,
                        adhanType = uiState.settings.adhanFor(prayerTimes.nextPrayer.prayer).type,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1.3f)
                            .focusRequester(initialFocusRequester)
                    )
                } else {
                    // Non-today summary header
                    NonTodaySummaryCard(
                        dateFormatted = prayerTimes.gregorianDate,
                        sunrise = prayerTimes.sunrise.format(PrayerTimeItem.TimeFormatter),
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(0.7f)
                            .focusRequester(initialFocusRequester)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 5 Daily Prayers Row
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
fun NextPrayerSpotlightCard(
    nextPrayer: PrayerTimeItem,
    countdown: String,
    sunrise: String,
    isReminderEnabled: Boolean,
    adhanType: AdhanAudioType,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = {},
        colors = CardDefaults.colors(
            containerColor = SurfaceVariant,
            focusedContainerColor = CardBackground
        ),
        shape = CardDefaults.shape(RoundedCornerShape(20.dp)),
        modifier = modifier
            .tvFocusable(shape = RoundedCornerShape(20.dp), focusedScale = 1.02f)
            .border(width = 1.5.dp, color = AccentEmerald.copy(alpha = 0.4f), shape = RoundedCornerShape(20.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp, vertical = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Next Prayer Name & Ticking Countdown
            Column(
                verticalArrangement = Arrangement.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .background(AccentEmerald.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "SHALAT BERIKUTNYA",
                            style = TvTypography.BodyLabel.copy(
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldLight
                            )
                        )
                    }

                    Text(
                        text = nextPrayer.prayer.arabicName,
                        style = TvTypography.ArabicTitle.copy(fontSize = 24.sp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = nextPrayer.prayer.displayName,
                    style = TvTypography.HeaderTitle.copy(fontSize = 48.sp, fontWeight = FontWeight.ExtraBold)
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AccessTime,
                        contentDescription = null,
                        tint = AccentGold,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "Menuju Adhan: $countdown",
                        style = TvTypography.CountdownText
                    )
                }
            }

            // Right: Time & Status Badges
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = nextPrayer.formattedTime,
                    style = TvTypography.ActivePrayerTime.copy(color = TextPrimary)
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Sunrise info badge
                    Row(
                        modifier = Modifier
                            .background(CardBackground, RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.WbSunny,
                            contentDescription = null,
                            tint = AccentGold,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Syuruq: $sunrise",
                            style = TvTypography.BodyLabel.copy(fontSize = 14.sp)
                        )
                    }

                    // Reminder Badge
                    Row(
                        modifier = Modifier
                            .background(CardBackground, RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isReminderEnabled) Icons.Default.NotificationsActive else Icons.Default.NotificationsOff,
                            contentDescription = null,
                            tint = if (isReminderEnabled) AccentEmerald else TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isReminderEnabled) "Pengingat 15m Aktif" else "Pengingat Nonaktif",
                            style = TvTypography.BodyLabel.copy(fontSize = 14.sp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun NonTodaySummaryCard(
    dateFormatted: String,
    sunrise: String,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = {},
        colors = CardDefaults.colors(containerColor = CardBackground),
        shape = CardDefaults.shape(RoundedCornerShape(16.dp)),
        modifier = modifier.tvFocusable(shape = RoundedCornerShape(16.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Jadwal Shalat Arsip / Masa Datang",
                    style = TvTypography.Subtitle.copy(color = AccentGold)
                )
                Text(
                    text = dateFormatted,
                    style = TvTypography.HeaderTitle.copy(fontSize = 32.sp)
                )
            }
            Row(
                modifier = Modifier
                    .background(SurfaceVariant, RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.WbSunny,
                    contentDescription = null,
                    tint = AccentGold,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Syuruq: $sunrise",
                    style = TvTypography.BodyLabel.copy(fontSize = 16.sp)
                )
            }
        }
    }
}

@Composable
fun PrayerCardsRow(
    prayers: List<PrayerTimeItem>,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        for (item in prayers) {
            PrayerCard(
                item = item,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
            )
        }
    }
}

@Composable
fun PrayerCard(
    item: PrayerTimeItem,
    modifier: Modifier = Modifier
) {
    val cardModifier = if (item.isActive) {
        modifier
            .pulsingGlow(shape = RoundedCornerShape(16.dp), glowColor = AccentEmerald)
            .tvFocusable(shape = RoundedCornerShape(16.dp))
    } else {
        modifier.tvFocusable(shape = RoundedCornerShape(16.dp))
    }

    Card(
        onClick = {},
        colors = CardDefaults.colors(
            containerColor = if (item.isNext) SurfaceVariant else CardBackground,
            focusedContainerColor = CardBackground
        ),
        shape = CardDefaults.shape(RoundedCornerShape(16.dp)),
        modifier = cardModifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Arabic Name
            Text(
                text = item.prayer.arabicName,
                style = TvTypography.ArabicTitle.copy(
                    fontSize = 20.sp,
                    color = if (item.isNext) AccentGold else TextSecondary
                )
            )

            // Latin Name
            Text(
                text = item.prayer.displayName,
                style = TvTypography.CardTitle.copy(
                    fontWeight = if (item.isNext) FontWeight.Bold else FontWeight.Medium
                )
            )

            // Prayer Time (Large TV typography)
            Text(
                text = item.formattedTime,
                style = if (item.isNext) {
                    TvTypography.InactivePrayerTime.copy(
                        fontSize = 42.sp,
                        fontWeight = FontWeight.Bold,
                        color = AccentEmerald
                    )
                } else {
                    TvTypography.InactivePrayerTime
                }
            )

            // Status label
            if (item.isActive) {
                Box(
                    modifier = Modifier
                        .background(AccentEmerald, RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "WAKTU SHALAT",
                        style = TvTypography.BodyLabel.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = DarkBackground
                        )
                    )
                }
            } else if (item.isNext) {
                Box(
                    modifier = Modifier
                        .background(AccentGold.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "SELANJUTNYA",
                        style = TvTypography.BodyLabel.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = AccentGold
                        )
                    )
                }
            } else {
                Spacer(modifier = Modifier.height(18.dp))
            }
        }
    }
}
