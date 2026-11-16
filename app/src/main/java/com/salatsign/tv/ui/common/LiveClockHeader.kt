package com.salatsign.tv.ui.common

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Mosque
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Today
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import com.salatsign.tv.R
import com.salatsign.tv.ui.theme.AccentEmerald
import com.salatsign.tv.ui.theme.AccentGold
import com.salatsign.tv.ui.theme.CardBackground
import com.salatsign.tv.ui.theme.CardFocusedBorder
import com.salatsign.tv.ui.theme.DarkBackground
import com.salatsign.tv.ui.theme.TextPrimary
import com.salatsign.tv.ui.theme.TextSecondary
import com.salatsign.tv.ui.theme.TvTypography
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun LiveClockHeader(
    isMosqueMode: Boolean,
    gregorianDate: String,
    hijriDate: String,
    cityName: String,
    mosqueName: String = "",
    mosqueSubtitle: String = "",
    mosqueLogoUri: String? = null,
    isViewingToday: Boolean = true,
    onPreviousDay: () -> Unit = {},
    onNextDay: () -> Unit = {},
    onTodayClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {}
) {
    var currentTime by remember { mutableStateOf(LocalTime.now()) }

    LaunchedEffect(Unit) {
        while (true) {
            currentTime = LocalTime.now()
            delay(1000)
        }
    }

    val timeFormatted = remember(currentTime) {
        currentTime.format(DateTimeFormatter.ofPattern("HH:mm:ss", Locale.US))
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left Column: Branding / Identity
        if (isMosqueMode) {
            // White-Label Mosque Branding (Zero App Logos or App Titles)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (!mosqueLogoUri.isNullOrBlank()) {
                    AsyncImage(
                        model = mosqueLogoUri,
                        contentDescription = "Mosque Logo",
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(CardBackground),
                        contentScale = ContentScale.Crop
                    )
                } else if (mosqueName.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(CardBackground),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mosque,
                            contentDescription = null,
                            tint = AccentEmerald,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

                Column {
                    if (mosqueName.isNotBlank()) {
                        Text(
                            text = mosqueName,
                            style = TvTypography.HeaderTitle.copy(fontSize = 34.sp)
                        )
                    }
                    if (mosqueSubtitle.isNotBlank()) {
                        Text(
                            text = mosqueSubtitle,
                            style = TvTypography.Subtitle.copy(fontSize = 20.sp, color = AccentGold)
                        )
                    } else if (mosqueName.isBlank()) {
                        Text(
                            text = cityName,
                            style = TvTypography.HeaderTitle.copy(fontSize = 28.sp)
                        )
                    }
                }
            }
        } else {
            // Home / Personal Mode: App Name & City + D-pad Date Traversal Buttons
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Image(
                    painter = painterResource(id = R.mipmap.ic_launcher),
                    contentDescription = "SalatSign Logo",
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(12.dp))
                )

                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "SalatSign",
                            style = TvTypography.HeaderTitle.copy(fontSize = 32.sp)
                        )
                        Box(
                            modifier = Modifier
                                .background(CardBackground, RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = cityName,
                                style = TvTypography.BodyLabel.copy(fontSize = 14.sp, color = AccentEmerald)
                            )
                        }
                    }

                    // Date navigation shortcuts
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Surface(
                            onClick = onPreviousDay,
                            shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(8.dp)),
                            colors = ClickableSurfaceDefaults.colors(
                                containerColor = CardBackground,
                                focusedContainerColor = AccentEmerald
                            ),
                            modifier = Modifier.tvFocusable()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Hari Sebelumnya",
                                    tint = TextPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("H-1", style = TvTypography.BodyLabel.copy(fontSize = 14.sp))
                            }
                        }

                        if (!isViewingToday) {
                            Surface(
                                onClick = onTodayClick,
                                shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(8.dp)),
                                colors = ClickableSurfaceDefaults.colors(
                                    containerColor = AccentGold,
                                    focusedContainerColor = AccentEmerald
                                ),
                                modifier = Modifier.tvFocusable()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Today,
                                        contentDescription = "Hari Ini",
                                        tint = DarkBackground,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        "Hari Ini",
                                        style = TvTypography.BodyLabel.copy(
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = DarkBackground
                                        )
                                    )
                                }
                            }
                        }

                        Surface(
                            onClick = onNextDay,
                            shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(8.dp)),
                            colors = ClickableSurfaceDefaults.colors(
                                containerColor = CardBackground,
                                focusedContainerColor = AccentEmerald
                            ),
                            modifier = Modifier.tvFocusable()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("H+1", style = TvTypography.BodyLabel.copy(fontSize = 14.sp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = "Hari Berikutnya",
                                    tint = TextPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        // Settings Icon Button in Home Mode
                        Surface(
                            onClick = onSettingsClick,
                            shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(8.dp)),
                            colors = ClickableSurfaceDefaults.colors(
                                containerColor = CardBackground,
                                focusedContainerColor = AccentEmerald
                            ),
                            modifier = Modifier.tvFocusable()
                        ) {
                            Box(modifier = Modifier.padding(6.dp)) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = "Pengaturan",
                                    tint = TextPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Right Column: Dual Date & Live Digital Clock
        Column(
            horizontalAlignment = Alignment.End
        ) {
            // Live Clock
            Text(
                text = timeFormatted,
                style = TvTypography.HeaderTitle.copy(
                    fontSize = 46.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            )

            // Gregorian Date
            Text(
                text = gregorianDate,
                style = TvTypography.Subtitle.copy(fontSize = 20.sp, color = TextPrimary)
            )

            // Hijri Date with Gold Accent
            Text(
                text = hijriDate,
                style = TvTypography.Subtitle.copy(
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AccentGold
                )
            )
        }
    }
}
