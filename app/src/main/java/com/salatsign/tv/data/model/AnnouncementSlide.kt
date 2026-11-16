package com.salatsign.tv.data.model

data class AnnouncementSlide(
    val id: String,
    val imageUri: String,
    val title: String = "",
    val durationSeconds: Int = 10
)
