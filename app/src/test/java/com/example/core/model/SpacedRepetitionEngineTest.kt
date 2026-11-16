package com.example.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.TimeZone

class SpacedRepetitionEngineTest {

    private val utc = TimeZone.getTimeZone("UTC")
    private val now = 1_750_000_000_000L // fixed deterministic timestamp

    @Test
    fun `initialState is due immediately with default ease factor`() {
        val state = SpacedRepetitionEngine.initialState("v_1", now)
        assertEquals(0, state.intervalDays)
        assertEquals(SpacedRepetitionEngine.DEFAULT_EASE_FACTOR, state.easeFactor, 0.001)
        assertEquals(0, state.repetitionCount)
        assertTrue(SpacedRepetitionEngine.isDueToday(state, now, utc))
    }

    @Test
    fun `good ratings follow SM-2 progression 1d then 6d then ease multiplied`() {
        val initial = SpacedRepetitionEngine.initialState("v_1", now)

        val first = SpacedRepetitionEngine.applyReview(initial, ReviewRating.GOOD, "e_1", now, utc)
        assertEquals(1, first.nextState.intervalDays)
        assertEquals(1, first.nextState.repetitionCount)
        assertFalse(SpacedRepetitionEngine.isDueToday(first.nextState, now, utc))

        val day1 = now + 86_400_000L
        val second = SpacedRepetitionEngine.applyReview(first.nextState, ReviewRating.GOOD, "e_2", day1, utc)
        assertEquals(6, second.nextState.intervalDays)
        assertEquals(2, second.nextState.repetitionCount)

        val day7 = day1 + 6 * 86_400_000L
        val third = SpacedRepetitionEngine.applyReview(second.nextState, ReviewRating.GOOD, "e_3", day7, utc)
        assertTrue(third.nextState.intervalDays >= 10)
        assertEquals(3, third.nextState.repetitionCount)
    }

    @Test
    fun `again rating resets repetition count and increments lapse count`() {
        val learned = ReviewState(
            vocabularyId = "v_2",
            dueAtEpochMillis = now,
            intervalDays = 6,
            easeFactor = 2.5,
            repetitionCount = 3,
            lapseCount = 0,
            lastReviewedAtEpochMillis = now - 86_400_000L
        )
        val outcome = SpacedRepetitionEngine.applyReview(learned, ReviewRating.AGAIN, "e_again", now, utc)
        assertEquals(0, outcome.nextState.repetitionCount)
        assertEquals(1, outcome.nextState.lapseCount)
        assertEquals(0, outcome.nextState.intervalDays)
        assertTrue(outcome.nextState.easeFactor >= SpacedRepetitionEngine.MIN_EASE_FACTOR)
        assertTrue(outcome.nextState.dueAtEpochMillis > now)
    }

    @Test
    fun `ease factor stays bounded between MIN and MAX`() {
        var state = SpacedRepetitionEngine.initialState("v_3", now).copy(easeFactor = 1.35)
        repeat(5) { idx ->
            state = SpacedRepetitionEngine.applyReview(state, ReviewRating.AGAIN, "ea_$idx", now + idx * 1000L, utc).nextState
        }
        assertEquals(SpacedRepetitionEngine.MIN_EASE_FACTOR, state.easeFactor, 0.001)

        var easyState = SpacedRepetitionEngine.initialState("v_4", now).copy(easeFactor = 4.95)
        repeat(5) { idx ->
            easyState = SpacedRepetitionEngine.applyReview(easyState, ReviewRating.EASY, "ee_$idx", now + idx * 1000L, utc).nextState
        }
        assertTrue(easyState.easeFactor <= SpacedRepetitionEngine.MAX_EASE_FACTOR)
    }
}
