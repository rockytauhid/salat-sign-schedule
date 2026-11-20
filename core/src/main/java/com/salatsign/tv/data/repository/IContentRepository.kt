// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Rocky Adam Tauhid and SalatSign contributors

package com.salatsign.tv.data.repository

import com.salatsign.tv.data.model.AnnouncementSlide
import com.salatsign.tv.data.model.SlideContainer
import kotlinx.coroutines.flow.StateFlow
import java.io.File
import java.time.LocalDate

/**
 * Repository interface contract for announcement slides content.
 * Abstracts storage access so that UI viewmodels, local background jobs,
 * and future remote phone controllers interact through a shared contract.
 */
interface IContentRepository {
    val slidesFlow: StateFlow<SlideContainer>

    suspend fun replaceAll(expectedRevision: Int, slides: List<AnnouncementSlide>): Result<SlideContainer>

    suspend fun addSlide(slide: AnnouncementSlide): Result<SlideContainer>

    suspend fun updateSlide(slide: AnnouncementSlide): Result<SlideContainer>

    suspend fun deleteSlide(slideId: String): Result<SlideContainer>

    fun getActiveSlides(
        slides: List<AnnouncementSlide>,
        today: LocalDate = LocalDate.now()
    ): List<AnnouncementSlide>

    fun getImageFile(fileName: String): File
}
