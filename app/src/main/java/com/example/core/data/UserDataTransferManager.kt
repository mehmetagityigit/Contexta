package com.example.core.data

import com.example.core.model.ContentValidator
import com.example.core.model.ContextExample
import com.example.core.model.ExportedContextDto
import com.example.core.model.ExportedReviewStateDto
import com.example.core.model.ExportedSettingsDto
import com.example.core.model.ExportedVocabularyDto
import com.example.core.model.ReviewState
import com.example.core.model.UserDataExportBundle
import com.example.core.model.UserSettings
import com.example.core.model.VocabularyEntry
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object UserDataTransferManager {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    fun exportToJson(
        vocabulary: List<VocabularyEntry>,
        contexts: List<ContextExample>,
        reviewStates: List<ReviewState>,
        settings: UserSettings,
        nowEpochMillis: Long = System.currentTimeMillis()
    ): String {
        val bundle = UserDataExportBundle(
            schemaVersion = 1,
            exportedAtEpochMillis = nowEpochMillis,
            vocabulary = vocabulary.map {
                ExportedVocabularyDto(
                    id = it.id,
                    expression = it.expression,
                    normalizedExpression = it.normalizedExpression,
                    meaningTr = it.meaningTr,
                    definitionEn = it.definitionEn,
                    partOfSpeech = it.partOfSpeech,
                    personalNote = it.personalNote,
                    tags = it.tags,
                    favorite = it.favorite,
                    createdAtEpochMillis = it.createdAtEpochMillis,
                    updatedAtEpochMillis = it.updatedAtEpochMillis
                )
            },
            contexts = contexts.map {
                ExportedContextDto(
                    id = it.id,
                    vocabularyId = it.vocabularyId,
                    sentence = it.sentence,
                    sourceTitle = it.sourceTitle,
                    sourceType = it.sourceType.code,
                    optionalSourceUrl = it.optionalSourceUrl,
                    optionalTimestampSeconds = it.optionalTimestampSeconds,
                    createdAtEpochMillis = it.createdAtEpochMillis
                )
            },
            reviewStates = reviewStates.map {
                ExportedReviewStateDto(
                    vocabularyId = it.vocabularyId,
                    dueAtEpochMillis = it.dueAtEpochMillis,
                    intervalDays = it.intervalDays,
                    easeFactor = it.easeFactor,
                    repetitionCount = it.repetitionCount,
                    lapseCount = it.lapseCount,
                    lastReviewedAtEpochMillis = it.lastReviewedAtEpochMillis
                )
            },
            settings = ExportedSettingsDto(
                language = settings.language.code,
                themeMode = settings.themeMode.code,
                dailyGoalMinutes = settings.dailyGoalMinutes,
                targetExam = settings.targetExam.code,
                cefrLevel = settings.cefrLevel.code,
                overlayScale = settings.overlayScale,
                overlayAlpha = settings.overlayAlpha,
                readingFontSizeSp = settings.readingFontSizeSp
            )
        )
        return json.encodeToString(bundle)
    }

    fun parseAndValidateImport(
        rawJson: String,
        existingVocabIds: Set<String>
    ): Result<UserDataExportBundle> {
        val bytes = rawJson.toByteArray(Charsets.UTF_8).size
        if (bytes > ContentValidator.MAX_IMPORT_BYTES) {
            return Result.failure(IllegalArgumentException("Import file exceeds 2 MB limit ($bytes bytes)."))
        }
        return try {
            val bundle = json.decodeFromString<UserDataExportBundle>(rawJson)
            val validation = ContentValidator.validateExportBundle(bundle, existingVocabIds)
            if (!validation.isValid) {
                Result.failure(IllegalArgumentException(validation.errors.take(3).joinToString("; ")))
            } else {
                Result.success(bundle)
            }
        } catch (e: Exception) {
            Result.failure(IllegalArgumentException("Invalid JSON format: ${e.localizedMessage}"))
        }
    }
}
