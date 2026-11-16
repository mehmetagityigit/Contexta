package com.example.core.model

import java.util.Calendar
import java.util.TimeZone
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Deterministic SM-2 based spaced repetition scheduler.
 * Pure Kotlin (Android-independent) and timezone-aware for daily due calculations (API 24+ safe).
 */
object SpacedRepetitionEngine {

    const val DEFAULT_EASE_FACTOR = 2.5
    const val MIN_EASE_FACTOR = 1.3
    const val MAX_EASE_FACTOR = 5.0

    data class ReviewOutcome(
        val nextState: ReviewState,
        val reviewEvent: ReviewEvent
    )

    fun initialState(vocabularyId: String, nowEpochMillis: Long): ReviewState {
        return ReviewState(
            vocabularyId = vocabularyId,
            dueAtEpochMillis = nowEpochMillis,
            intervalDays = 0,
            easeFactor = DEFAULT_EASE_FACTOR,
            repetitionCount = 0,
            lapseCount = 0,
            lastReviewedAtEpochMillis = null
        )
    }

    /**
     * Maps user review rating to SM-2 quality score (0-5):
     * AGAIN -> 1, HARD -> 3, GOOD -> 4, EASY -> 5.
     */
    fun ratingToQuality(rating: ReviewRating): Int = when (rating) {
        ReviewRating.AGAIN -> 1
        ReviewRating.HARD -> 3
        ReviewRating.GOOD -> 4
        ReviewRating.EASY -> 5
    }

    fun applyReview(
        current: ReviewState,
        rating: ReviewRating,
        eventId: String,
        nowEpochMillis: Long,
        timeZone: TimeZone = TimeZone.getDefault()
    ): ReviewOutcome {
        val quality = ratingToQuality(rating)
        val prevInterval = current.intervalDays.coerceAtLeast(0)

        var easeFactor = current.easeFactor
        // Standard SM-2 ease update formula: EF' = EF + (0.1 - (5 - q) * (0.08 + (5 - q) * 0.02))
        val delta = 0.1 - (5 - quality) * (0.08 + (5 - quality) * 0.02)
        easeFactor = (easeFactor + delta).coerceIn(MIN_EASE_FACTOR, MAX_EASE_FACTOR)

        val newRepetitionCount: Int
        val newLapseCount: Int
        val newIntervalDays: Int
        val nextDueMillis: Long

        when (rating) {
            ReviewRating.AGAIN -> {
                newRepetitionCount = 0
                newLapseCount = current.lapseCount + 1
                newIntervalDays = 0
                // Schedule again after 10 minutes in the current session/day.
                nextDueMillis = nowEpochMillis + 10 * 60 * 1000L
            }
            ReviewRating.HARD -> {
                newRepetitionCount = max(1, current.repetitionCount)
                newLapseCount = current.lapseCount
                newIntervalDays = when {
                    prevInterval <= 0 -> 1
                    prevInterval == 1 -> 2
                    else -> max(prevInterval + 1, (prevInterval * 1.2).roundToInt())
                }
                nextDueMillis = startOfDayPlusDaysEpochMillis(nowEpochMillis, newIntervalDays, timeZone)
            }
            ReviewRating.GOOD -> {
                newRepetitionCount = current.repetitionCount + 1
                newLapseCount = current.lapseCount
                newIntervalDays = when (newRepetitionCount) {
                    1 -> 1
                    2 -> 6
                    else -> max(prevInterval + 1, (prevInterval * easeFactor).roundToInt())
                }
                nextDueMillis = startOfDayPlusDaysEpochMillis(nowEpochMillis, newIntervalDays, timeZone)
            }
            ReviewRating.EASY -> {
                newRepetitionCount = current.repetitionCount + 1
                newLapseCount = current.lapseCount
                val base = when (newRepetitionCount) {
                    1 -> 4
                    2 -> 8
                    else -> max(prevInterval + 2, (prevInterval * easeFactor * 1.3).roundToInt())
                }
                newIntervalDays = base.coerceAtLeast(4)
                nextDueMillis = startOfDayPlusDaysEpochMillis(nowEpochMillis, newIntervalDays, timeZone)
            }
        }

        val updatedState = ReviewState(
            vocabularyId = current.vocabularyId,
            dueAtEpochMillis = nextDueMillis,
            intervalDays = newIntervalDays,
            easeFactor = ((easeFactor * 100.0).roundToInt() / 100.0),
            repetitionCount = newRepetitionCount,
            lapseCount = newLapseCount,
            lastReviewedAtEpochMillis = nowEpochMillis,
            syncStatus = current.syncStatus
        )

        val event = ReviewEvent(
            eventId = eventId,
            vocabularyId = current.vocabularyId,
            rating = rating,
            previousIntervalDays = prevInterval,
            newIntervalDays = newIntervalDays,
            reviewedAtEpochMillis = nowEpochMillis
        )

        return ReviewOutcome(updatedState, event)
    }

    /**
     * Checks whether a card is due today (up to end of today in user's local timezone).
     */
    fun isDueToday(
        state: ReviewState?,
        nowEpochMillis: Long,
        timeZone: TimeZone = TimeZone.getDefault()
    ): Boolean {
        if (state == null) return true
        val endOfToday = endOfDayEpochMillis(nowEpochMillis, timeZone)
        return state.dueAtEpochMillis <= endOfToday
    }

    fun endOfDayEpochMillis(epochMillis: Long, timeZone: TimeZone = TimeZone.getDefault()): Long {
        val cal = Calendar.getInstance(timeZone).apply {
            timeInMillis = epochMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.DAY_OF_YEAR, 1)
        }
        return cal.timeInMillis - 1L
    }

    fun startOfDayPlusDaysEpochMillis(
        epochMillis: Long,
        days: Int,
        timeZone: TimeZone = TimeZone.getDefault()
    ): Long {
        val cal = Calendar.getInstance(timeZone).apply {
            timeInMillis = epochMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.DAY_OF_YEAR, days)
        }
        return cal.timeInMillis
    }
}
