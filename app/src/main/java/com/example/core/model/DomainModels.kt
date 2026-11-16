package com.example.core.model

enum class TargetExam(val code: String) {
    GENERAL("GENERAL"),
    SAT("SAT"),
    IELTS("IELTS"),
    AP_ACADEMIC("AP_ACADEMIC");

    companion object {
        fun fromCode(code: String?): TargetExam =
            entries.firstOrNull { it.code == code } ?: GENERAL
    }
}

enum class CefrLevel(val code: String) {
    B1("B1"),
    B2("B2"),
    C1("C1"),
    C2("C2");

    companion object {
        fun fromCode(code: String?): CefrLevel =
            entries.firstOrNull { it.code == code } ?: B2
    }
}

enum class AppLanguage(val code: String) {
    TURKISH("tr"),
    ENGLISH("en");

    companion object {
        fun fromCode(code: String?): AppLanguage =
            entries.firstOrNull { it.code == code } ?: TURKISH
    }
}

enum class ThemeMode(val code: String) {
    SYSTEM("SYSTEM"),
    LIGHT("LIGHT"),
    DARK("DARK");

    companion object {
        fun fromCode(code: String?): ThemeMode =
            entries.firstOrNull { it.code == code } ?: SYSTEM
    }
}

enum class ContextSourceType(val code: String) {
    VIDEO("VIDEO"),
    ARTICLE("ARTICLE"),
    BOOK("BOOK"),
    PODCAST("PODCAST"),
    READING("READING"),
    LISTENING("LISTENING"),
    OVERLAY("OVERLAY"),
    VOICE("VOICE"),
    MANUAL("MANUAL");

    companion object {
        fun fromCode(code: String?): ContextSourceType =
            entries.firstOrNull { it.code == code } ?: MANUAL
    }
}

enum class ReviewRating(val code: String) {
    AGAIN("AGAIN"),
    HARD("HARD"),
    GOOD("GOOD"),
    EASY("EASY");

    companion object {
        fun fromCode(code: String?): ReviewRating =
            entries.firstOrNull { it.code == code } ?: GOOD
    }
}

enum class StudyModuleType(val code: String) {
    FLASHCARD("FLASHCARD"),
    GRAMMAR("GRAMMAR"),
    READING("READING"),
    LISTENING("LISTENING");

    companion object {
        fun fromCode(code: String?): StudyModuleType =
            entries.firstOrNull { it.code == code } ?: FLASHCARD
    }
}

enum class SyncStatus {
    SYNCED,
    PENDING_LOCAL,
    DEMO_LOCAL
}

data class UserProfile(
    val userId: String,
    val displayName: String? = null,
    val email: String? = null,
    val targetExam: TargetExam = TargetExam.GENERAL,
    val cefrLevel: CefrLevel = CefrLevel.B2,
    val dailyGoalMinutes: Int = 20,
    val onboardingCompleted: Boolean = false
)

data class VocabularyEntry(
    val id: String,
    val expression: String,
    val normalizedExpression: String,
    val meaningTr: String = "",
    val definitionEn: String = "",
    val partOfSpeech: String = "",
    val personalNote: String = "",
    val tags: List<String> = emptyList(),
    val favorite: Boolean = false,
    val createdAtEpochMillis: Long = 0L,
    val updatedAtEpochMillis: Long = 0L,
    val syncStatus: SyncStatus = SyncStatus.SYNCED
)

data class ContextExample(
    val id: String,
    val vocabularyId: String,
    val sentence: String,
    val sourceTitle: String = "",
    val sourceType: ContextSourceType = ContextSourceType.MANUAL,
    val optionalSourceUrl: String? = null,
    val optionalTimestampSeconds: Int? = null,
    val createdAtEpochMillis: Long = 0L,
    val syncStatus: SyncStatus = SyncStatus.SYNCED
)

data class ReviewState(
    val vocabularyId: String,
    val dueAtEpochMillis: Long,
    val intervalDays: Int,
    val easeFactor: Double,
    val repetitionCount: Int,
    val lapseCount: Int,
    val lastReviewedAtEpochMillis: Long? = null,
    val syncStatus: SyncStatus = SyncStatus.SYNCED
)

data class ReviewEvent(
    val eventId: String,
    val vocabularyId: String,
    val rating: ReviewRating,
    val previousIntervalDays: Int,
    val newIntervalDays: Int,
    val reviewedAtEpochMillis: Long
)

data class StudySession(
    val id: String,
    val moduleType: StudyModuleType,
    val topicOrContentId: String,
    val durationSeconds: Int,
    val totalItems: Int,
    val correctCount: Int,
    val dateKey: String,
    val startedAtEpochMillis: Long,
    val completedAtEpochMillis: Long
)

data class QuestionAttempt(
    val id: String,
    val moduleType: StudyModuleType,
    val contentId: String,
    val questionId: String,
    val questionType: String? = null,
    val topicId: String? = null,
    val selectedOptionId: String,
    val correctOptionId: String,
    val isCorrect: Boolean,
    val answeredAtEpochMillis: Long
)

data class UserSettings(
    val language: AppLanguage = AppLanguage.TURKISH,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dailyGoalMinutes: Int = 20,
    val targetExam: TargetExam = TargetExam.GENERAL,
    val cefrLevel: CefrLevel = CefrLevel.B2,
    val overlayScale: Float = 1.0f,
    val overlayAlpha: Float = 0.92f,
    val readingFontSizeSp: Int = 17,
    val onboardingCompleted: Boolean = false
)

data class VocabularyWithContext(
    val entry: VocabularyEntry,
    val contexts: List<ContextExample>,
    val reviewState: ReviewState?
)

enum class VocabularyFilter {
    ALL,
    DUE_TODAY,
    NEW,
    FAVORITES
}
