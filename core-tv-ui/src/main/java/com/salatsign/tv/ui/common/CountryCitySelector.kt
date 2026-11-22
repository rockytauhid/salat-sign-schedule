// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Rocky Adam Tauhid and SalatSign contributors

package com.salatsign.tv.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.salatsign.tv.data.model.CityPreset
import com.salatsign.tv.data.model.PopularCityPresets
import com.salatsign.tv.ui.theme.AccentEmerald
import com.salatsign.tv.ui.theme.AccentGold
import com.salatsign.tv.ui.theme.TvTypography

/**
 * Extracts the country display name from a [CityPreset.name] (e.g. "Brisbane, QLD, Australia" -> "Australia").
 */
fun CityPreset.countryName(): String {
    val parts = name.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    return parts.lastOrNull() ?: name
}

/**
 * Extracts the short city/state label without repeating the country (e.g. "Brisbane, QLD, Australia" -> "Brisbane, QLD").
 */
fun CityPreset.shortCityLabel(): String {
    val parts = name.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    return if (parts.size > 1) parts.dropLast(1).joinToString(", ") else name
}

/**
 * Searchable Country -> City selector designed for 10-foot Android TV navigation.
 * Supports typing a city or country name (via on-screen TV keyboard or Bluetooth keyboard)
 * as well as quick D-pad Country chip filtering.
 */
@Composable
fun CountryCitySelector(
    selectedCityName: String,
    onSelectCity: (CityPreset) -> Unit,
    initialFocusRequester: FocusRequester? = null,
    scrollableCityGrid: Boolean = true,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme

    val citiesByCountry = remember {
        PopularCityPresets.groupBy { it.countryName() }
    }
    val countries = remember(citiesByCountry) {
        citiesByCountry.keys.toList()
    }

    val initialCountry = remember(selectedCityName) {
        PopularCityPresets.firstOrNull { it.name == selectedCityName }?.countryName()
            ?: countries.firstOrNull()
            ?: "Australia"
    }
    var selectedCountry by remember(initialCountry) { mutableStateOf(initialCountry) }
    var searchQuery by remember { mutableStateOf("") }
    var isSearchFocused by remember { mutableStateOf(false) }

    val displayedCities = remember(searchQuery, selectedCountry, citiesByCountry) {
        val q = searchQuery.trim()
        if (q.isEmpty()) {
            citiesByCountry[selectedCountry].orEmpty()
        } else {
            PopularCityPresets.filter { city ->
                city.name.contains(q, ignoreCase = true) ||
                    city.zoneId.contains(q, ignoreCase = true)
            }
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Search textbox (D-pad safe: press OK to type; Up/Down glides cleanly past)
        TvEditableTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = "Search city or country (e.g. Brisbane, Jakarta, London, Tokyo)...",
            leadingIcon = Icons.Default.Search,
            accentUnfocusedBorder = false,
            textStyle = TvTypography.BodyLabel.copy(fontSize = 16.sp),
            trailingContent = if (searchQuery.isNotEmpty()) {
                {
                    Surface(
                        onClick = { searchQuery = "" },
                        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(6.dp)),
                        colors = ClickableSurfaceDefaults.colors(
                            containerColor = colorScheme.surfaceVariant,
                            focusedContainerColor = AccentGold
                        ),
                        modifier = Modifier.pointerInput(Unit) { detectTapGestures { searchQuery = "" } }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear search",
                            tint = colorScheme.onSurface,
                            modifier = Modifier
                                .padding(4.dp)
                                .size(16.dp)
                        )
                    }
                }
            } else null
        )

        // Country selector chips (shown when not typing a search query)
        if (searchQuery.isBlank()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                countries.forEachIndexed { idx, country ->
                    val isCountrySelected = country == selectedCountry
                    val shape = RoundedCornerShape(10.dp)
                    Card(
                        onClick = { selectedCountry = country },
                        colors = CardDefaults.colors(
                            containerColor = if (isCountrySelected) AccentEmerald.copy(alpha = 0.25f) else colorScheme.surface,
                            focusedContainerColor = colorScheme.surfaceVariant
                        ),
                        shape = CardDefaults.shape(shape),
                        border = tvSelectableCardBorder(selected = isCountrySelected, shape = shape),
                        modifier = Modifier
                            .then(if (idx == 0 && initialFocusRequester != null) Modifier.focusRequester(initialFocusRequester) else Modifier)
                            .pointerInput(country) { detectTapGestures { selectedCountry = country } }
                    ) {
                        Text(
                            text = country,
                            maxLines = 1,
                            style = TvTypography.ButtonText.copy(
                                fontSize = 14.sp,
                                fontWeight = if (isCountrySelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isCountrySelected) AccentEmerald else colorScheme.onSurface
                            ),
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }

        // Cities grid (4 columns, vertically scrollable so large countries like Indonesia show all cities)
        if (displayedCities.isEmpty()) {
            Text(
                text = "No matching cities found for \"$searchQuery\". Try another city or country name.",
                style = TvTypography.BodyLabel.copy(
                    fontSize = 15.sp,
                    color = colorScheme.onBackground.copy(alpha = 0.7f)
                ),
                modifier = Modifier.padding(vertical = 12.dp)
            )
        } else {
            val gridModifier = if (scrollableCityGrid) {
                Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 6.dp, vertical = 6.dp)
            } else {
                Modifier.padding(horizontal = 6.dp, vertical = 6.dp)
            }
            Column(
                modifier = gridModifier,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                displayedCities.chunked(4).forEach { rowCities ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        rowCities.forEach { city ->
                            val isCitySelected = selectedCityName == city.name
                            val shape = RoundedCornerShape(12.dp)
                            Card(
                                onClick = {
                                    onSelectCity(city)
                                    selectedCountry = city.countryName()
                                },
                                colors = CardDefaults.colors(
                                    containerColor = if (isCitySelected) colorScheme.surfaceVariant else colorScheme.surface,
                                    focusedContainerColor = colorScheme.surfaceVariant
                                ),
                                shape = CardDefaults.shape(shape),
                                border = tvSelectableCardBorder(selected = isCitySelected, shape = shape),
                                modifier = Modifier
                                    .weight(1f)
                                    .pointerInput(city) {
                                        detectTapGestures {
                                            onSelectCity(city)
                                            selectedCountry = city.countryName()
                                        }
                                    }
                            ) {
                                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                                    Text(
                                        text = if (searchQuery.isNotBlank()) city.name else city.shortCityLabel(),
                                        maxLines = 1,
                                        style = TvTypography.CardTitle.copy(
                                            fontSize = 15.sp,
                                            fontWeight = if (isCitySelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isCitySelected) AccentEmerald else colorScheme.onSurface
                                        )
                                    )
                                    Text(
                                        text = city.zoneId.substringAfter('/'),
                                        maxLines = 1,
                                        style = TvTypography.BodyLabel.copy(
                                            fontSize = 12.sp,
                                            color = colorScheme.onSurface.copy(alpha = 0.65f)
                                        )
                                    )
                                }
                            }
                        }
                        repeat(4 - rowCities.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}
