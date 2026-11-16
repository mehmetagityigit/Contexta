package com.example.core.model

import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import kotlin.math.roundToInt

/**
 * Pure Kotlin scoring and progress analytics calculator.
 * Never invents fake metrics; derives accuracy and weak areas strictly from real attempts and sessions.
 */
object PracticeScoringEngine {

    data class ModuleAccuracy(
        val totalAttempts: Int,
        val correctAttempts: Int,
        val accuracyPercent: Int?
    )

    data class WeakArea(
        val key: String,
        val moduleType: StudyModuleType,
        val totalAttempts: Int,
        val correctAttempts: Int,
        val accuracyPercent: Int
    )

    data class DailyStudyPoint(
        val dateKey: String,
        val minutes: Int
    )

    data class ProgressSummary(
        val totalStudyMinutes: Int,
        val reviewedCardsCount: Int,
        val newWordsCount: Int,
        val grammarAccuracy: ModuleAccuracy,
        val readingAccuracy: ModuleAccuracy,
        val listeningAccuracy: ModuleAccuracy,
        val weakAreas: List<WeakArea>,
        val dailyMinutes: List<DailyStudyPoint>
    )

    fun scoreQuestion(selectedOptionId: String, correctOptionId: String): Boolean {
        return selectedOptionId.isNotBlank() && selectedOptionId == correctOptionId
    }

    fun accuracyPercent(correct: Int, total: Int): Int? {
        if (total <= 0) return null
        return ((correct.toDouble() / total.toDouble()) * 100.0).roundToInt().coerceIn(0, 100)
    }

    fun dateKeyFor(epochMillis: Long, timeZone: TimeZone = TimeZone.getDefault()): String {
        val cal = Calendar.getInstance(timeZone).apply {
            timeInMillis = epochMillis
        }
        return formatDateKey(cal)
    }

    fun computeProgress(
        daysWindow: Int,
        nowEpochMillis: Long,
        sessions: List<StudySession>,
        attempts: List<QuestionAttempt>,
        reviewEvents: List<ReviewEvent>,
        vocabulary: List<VocabularyEntry>,
        timeZone: TimeZone = TimeZone.getDefault()
    ): ProgressSummary {
        val startCal = Calendar.getInstance(timeZone).apply {
            timeInMillis = nowEpochMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.DAY_OF_YEAR, -(daysWindow - 1).coerceAtLeast(0))
        }
        val startEpoch = startCal.timeInMillis

        val filteredSessions = sessions.filter { it.completedAtEpochMillis >= startEpoch }
        val filteredAttempts = attempts.filter { it.answeredAtEpochMillis >= startEpoch }
        val filteredReviews = reviewEvents.filter { it.reviewedAtEpochMillis >= startEpoch }
        val filteredVocab = vocabulary.filter { it.createdAtEpochMillis >= startEpoch }

        val totalSeconds = filteredSessions.sumOf { it.durationSeconds.coerceAtLeast(0) }
        val totalMinutes = if (totalSeconds == 0) 0 else maxOf(1, (totalSeconds / 60.0).roundToInt())

        val grammarAcc = computeModuleAccuracy(filteredAttempts, StudyModuleType.GRAMMAR)
        val readingAcc = computeModuleAccuracy(filteredAttempts, StudyModuleType.READING)
        val listeningAcc = computeModuleAccuracy(filteredAttempts, StudyModuleType.LISTENING)

        val weakAreas = filteredAttempts
            .groupBy { attempt ->
                val label = attempt.questionType?.takeIf { it.isNotBlank() }
                    ?: attempt.topicId?.takeIf { it.isNotBlank() }
                    ?: attempt.contentId
                attempt.moduleType to label
            }
            .mapNotNull { (pair, group) ->
                val total = group.size
                if (total == 0) return@mapNotNull null
                val correct = group.count { it.isCorrect }
                val pct = accuracyPercent(correct, total) ?: return@mapNotNull null
                if (pct >= 80) return@mapNotNull null
                WeakArea(
                    key = pair.second,
                    moduleType = pair.first,
                    totalAttempts = total,
                    correctAttempts = correct,
                    accuracyPercent = pct
                )
            }
            .sortedWith(compareBy<WeakArea> { it.accuracyPercent }.thenByDescending { it.totalAttempts })
            .take(6)

        val minutesByDate = filteredSessions
            .groupBy { it.dateKey }
            .mapValues { (_, list) ->
                val sec = list.sumOf { it.durationSeconds.coerceAtLeast(0) }
                if (sec == 0) 0 else maxOf(1, (sec / 60.0).roundToInt())
            }

        val cursor = Calendar.getInstance(timeZone).apply {
            timeInMillis = startEpoch
        }
        val dailySeries = (0 until daysWindow).map { offset ->
            if (offset > 0) cursor.add(Calendar.DAY_OF_YEAR, 1)
            val key = formatDateKey(cursor)
            DailyStudyPoint(dateKey = key, minutes = minutesByDate[key] ?: 0)
        }

        return ProgressSummary(
            totalStudyMinutes = totalMinutes,
            reviewedCardsCount = filteredReviews.size,
            newWordsCount = filteredVocab.size,
            grammarAccuracy = grammarAcc,
            readingAccuracy = readingAcc,
            listeningAccuracy = listeningAcc,
            weakAreas = weakAreas,
            dailyMinutes = dailySeries
        )
    }

    private fun computeModuleAccuracy(
        attempts: List<QuestionAttempt>,
        moduleType: StudyModuleType
    ): ModuleAccuracy {
        val list = attempts.filter { it.moduleType == moduleType }
        val total = list.size
        val correct = list.count { it.isCorrect }
        return ModuleAccuracy(
            totalAttempts = total,
            correctAttempts = correct,
            accuracyPercent = accuracyPercent(correct, total)
        )
    }

    private fun formatDateKey(cal: Calendar): String {
        val y = cal.get(Calendar.YEAR)
        val m = cal.get(Calendar.MONTH) + 1
        val d = cal.get(Calendar.DAY_OF_MONTH)
        return String.format(Locale.US, "%04d-%02d-%02d", y, m, d)
    }
}
