package com.example.core.model

import kotlinx.serialization.Serializable

@Serializable
data class ExportedContextDto(
    val id: String,
    val vocabularyId: String,
    val sentence: String,
    val sourceTitle: String = "",
    val sourceType: String = "MANUAL",
    val optionalSourceUrl: String? = null,
    val optionalTimestampSeconds: Int? = null,
    val createdAtEpochMillis: Long = 0L
)

@Serializable
data class ExportedReviewStateDto(
    val vocabularyId: String,
    val dueAtEpochMillis: Long,
    val intervalDays: Int,
    val easeFactor: Double,
    val repetitionCount: Int,
    val lapseCount: Int,
    val lastReviewedAtEpochMillis: Long? = null
)

@Serializable
data class ExportedVocabularyDto(
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
    val updatedAtEpochMillis: Long = 0L
)

@Serializable
data class ExportedSettingsDto(
    val language: String = "tr",
    val themeMode: String = "SYSTEM",
    val dailyGoalMinutes: Int = 20,
    val targetExam: String = "GENERAL",
    val cefrLevel: String = "B2",
    val overlayScale: Float = 1.0f,
    val overlayAlpha: Float = 0.92f,
    val readingFontSizeSp: Int = 17
)

@Serializable
data class UserDataExportBundle(
    val schemaVersion: Int = 1,
    val exportedAtEpochMillis: Long,
    val vocabulary: List<ExportedVocabularyDto>,
    val contexts: List<ExportedContextDto>,
    val reviewStates: List<ExportedReviewStateDto>,
    val settings: ExportedSettingsDto? = null
)
