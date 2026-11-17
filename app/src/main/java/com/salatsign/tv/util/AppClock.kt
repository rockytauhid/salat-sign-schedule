package com.salatsign.tv.util

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

interface AppClock {
    val zoneId: ZoneId
    fun nowInstant(): Instant = Instant.now()
    fun nowZoned(): ZonedDateTime = ZonedDateTime.ofInstant(nowInstant(), zoneId)
    fun nowLocalDate(): LocalDate = nowZoned().toLocalDate()
    fun nowLocalTime(): LocalTime = nowZoned().toLocalTime()

    class Default(override val zoneId: ZoneId) : AppClock

    class Fixed(
        private val fixedInstant: Instant,
        override val zoneId: ZoneId
    ) : AppClock {
        override fun nowInstant(): Instant = fixedInstant
    }
}
