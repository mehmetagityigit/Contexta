package com.example.core.model

import kotlinx.serialization.Serializable

@Serializable
data class QuestionOption(
    val id: String,
    val text: String,
    val explanationTr: String = "",
    val explanationEn: String = ""
)

@Serializable
data class PracticeQuestion(
    val id: String,
    val prompt: String,
    val questionType: String = "",
    val options: List<QuestionOption>,
    val correctOptionId: String,
    val explanationTr: String,
    val explanationEn: String = "",
    val evidenceRefId: String? = null
)

@Serializable
data class GrammarExample(
    val correct: String,
    val incorrect: String,
    val noteTr: String,
    val noteEn: String = ""
)

@Serializable
data class GrammarTopic(
    val id: String,
    val orderIndex: Int,
    val schemaVersion: Int,
    val contentVersion: Int,
    val level: String,
    val targetExams: List<String>,
    val title: String,
    val titleTr: String = "",
    val tags: List<String>,
    val status: String,
    val source: String,
    val license: String,
    val reviewedAt: String,
    val summaryTr: String,
    val summaryEn: String = "",
    val commonMistakesTr: List<String>,
    val commonMistakesEn: List<String> = emptyList(),
    val examples: List<GrammarExample>,
    val questions: List<PracticeQuestion>
)

@Serializable
data class ReadingParagraph(
    val id: String,
    val text: String
)

@Serializable
data class ReadingPassage(
    val id: String,
    val schemaVersion: Int,
    val contentVersion: Int,
    val level: String,
    val levelGroup: String, // "B1-B2" or "C1-C2"
    val genre: String,      // Natural Sciences | Social Sciences & History | Humanities | Technology & AI
    val targetExams: List<String>,
    val title: String,
    val tags: List<String>,
    val status: String,
    val source: String,
    val license: String,
    val reviewedAt: String,
    val paragraphs: List<ReadingParagraph>,
    val questions: List<PracticeQuestion>
)

@Serializable
data class TranscriptLine(
    val id: String,
    val speaker: String = "",
    val text: String,
    val startMs: Long = 0L,
    val endMs: Long = 0L
)

@Serializable
data class ListeningLesson(
    val id: String,
    val schemaVersion: Int,
    val contentVersion: Int,
    val level: String,
    val contentType: String, // ACADEMIC_LECTURE | DIALOGUE | SHORT_STORY
    val targetExams: List<String>,
    val title: String,
    val tags: List<String>,
    val status: String,
    val source: String,
    val license: String,
    val reviewedAt: String,
    val audioMode: String = "TTS", // "MEDIA3" or "TTS"
    val audioUri: String? = null,
    val transcript: List<TranscriptLine>,
    val questions: List<PracticeQuestion>
)

@Serializable
data class GrammarPack(
    val topics: List<GrammarTopic>
)

@Serializable
data class ReadingPack(
    val passages: List<ReadingPassage>
)

@Serializable
data class ListeningPack(
    val lessons: List<ListeningLesson>
)
