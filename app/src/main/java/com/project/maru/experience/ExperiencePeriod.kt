package com.project.maru.experience

import java.time.Clock
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.format.ResolverStyle
import java.time.temporal.TemporalAdjusters.previousOrSame

internal data class ExperiencePeriod(val start: LocalDate, val end: LocalDate) {
    init {
        require(!start.isAfter(end))
    }

    fun includes(experience: Experience): Boolean {
        val eventPeriod = orderedPeriod(experience.startDate, experience.endDate)
            ?: parseDisplayPeriod(experience.date)
            ?: return false
        return !eventPeriod.end.isBefore(start) && !eventPeriod.start.isAfter(end)
    }

    companion object {
        private val seoulZone = ZoneId.of("Asia/Seoul")
        private val dottedDate = DateTimeFormatter.ofPattern("uuuu.M.d")
            .withResolverStyle(ResolverStyle.STRICT)

        fun todayInSeoul(clock: Clock = Clock.systemUTC()): LocalDate =
            LocalDate.now(clock.withZone(seoulZone))

        fun weekOf(date: LocalDate): ExperiencePeriod {
            val monday = date.with(previousOrSame(DayOfWeek.MONDAY))
            return ExperiencePeriod(monday, monday.plusDays(6))
        }

        fun monthOf(month: YearMonth): ExperiencePeriod =
            ExperiencePeriod(month.atDay(1), month.atEndOfMonth())

        fun parseApiDate(value: String?): LocalDate? = value?.trim()?.substringBefore(' ')
            ?.let { parseDate(it, DateTimeFormatter.ISO_LOCAL_DATE) }

        private fun parseDisplayPeriod(value: String): ExperiencePeriod? {
            val dates = value.split('~', '～', limit = 3)
            val start = parseDisplayDate(dates.first()) ?: return null
            return when (dates.size) {
                1 -> ExperiencePeriod(start, start)
                2 -> orderedPeriod(start, parseDisplayDate(dates[1]))
                else -> null
            }
        }

        private fun parseDisplayDate(value: String): LocalDate? {
            val date = value.trim()
            return parseDate(date, DateTimeFormatter.ISO_LOCAL_DATE) ?: parseDate(date, dottedDate)
        }

        private fun parseDate(value: String, format: DateTimeFormatter): LocalDate? = try {
            LocalDate.parse(value, format)
        } catch (_: DateTimeParseException) {
            null
        }

        private fun orderedPeriod(start: LocalDate?, end: LocalDate?): ExperiencePeriod? =
            if (start != null && end != null && !start.isAfter(end)) ExperiencePeriod(start, end) else null
    }
}
