package com.example.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.TimeZone

class PracticeScoringEngineTest {

    private val utc = TimeZone.getTimeZone("UTC")

    @Test
    fun `empty data produces zero counts and null accuracies without fake stats`() {
        val now = 1_750_000_000_000L
        val summary = PracticeScoringEngine.computeProgress(
            daysWindow = 7,
            nowEpochMillis = now,
            sessions = emptyList(),
            attempts = emptyList(),
            reviewEvents = emptyList(),
            vocabulary = emptyList(),
            timeZone = utc
        )
        assertEquals(0, summary.totalStudyMinutes)
        assertEquals(0, summary.reviewedCardsCount)
        assertEquals(0, summary.newWordsCount)
        assertNull(summary.grammarAccuracy.accuracyPercent)
        assertNull(summary.readingAccuracy.accuracyPercent)
        assertNull(summary.listeningAccuracy.accuracyPercent)
        assertTrue(summary.weakAreas.isEmpty())
        assertEquals(7, summary.dailyMinutes.size)
        assertTrue(summary.dailyMinutes.all { it.minutes == 0 })
    }

    @Test
    fun `calculates real module accuracies and weak areas below 80 percent`() {
        val now = 1_750_000_000_000L
        val todayKey = PracticeScoringEngine.dateKeyFor(now, utc)
        val sessions = listOf(
            StudySession(
                id = "s1",
                moduleType = StudyModuleType.GRAMMAR,
                topicOrContentId = "g1",
                durationSeconds = 300,
                totalItems = 3,
                correctCount = 1,
                dateKey = todayKey,
                startedAtEpochMillis = now - 300_000L,
                completedAtEpochMillis = now
            )
        )
        val attempts = listOf(
            QuestionAttempt("a1", StudyModuleType.GRAMMAR, "g1", "q1", "Conditionals", "g1", "a", "a", true, now),
            QuestionAttempt("a2", StudyModuleType.GRAMMAR, "g1", "q2", "Conditionals", "g1", "b", "c", false, now),
            QuestionAttempt("a3", StudyModuleType.GRAMMAR, "g1", "q3", "Conditionals", "g1", "d", "a", false, now),
            QuestionAttempt("a4", StudyModuleType.READING, "r1", "rq1", "Inference", "r1", "a", "a", true, now)
        )

        val summary = PracticeScoringEngine.computeProgress(
            daysWindow = 7,
            nowEpochMillis = now,
            sessions = sessions,
            attempts = attempts,
            reviewEvents = emptyList(),
            vocabulary = emptyList(),
            timeZone = utc
        )

        assertEquals(5, summary.totalStudyMinutes)
        assertEquals(33, summary.grammarAccuracy.accuracyPercent)
        assertEquals(100, summary.readingAccuracy.accuracyPercent)
        assertEquals(1, summary.weakAreas.size)
        assertEquals("Conditionals", summary.weakAreas.first().key)
        assertEquals(5, summary.dailyMinutes.last().minutes)
    }
}
