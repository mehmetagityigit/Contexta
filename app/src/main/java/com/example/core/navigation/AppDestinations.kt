package com.example.core.navigation

import kotlinx.serialization.Serializable

@Serializable
data object HomeRoute

@Serializable
data object VocabularyRoute

@Serializable
data class VocabularyDetailRoute(val vocabId: String)

@Serializable
data object FlashcardReviewRoute

@Serializable
data object StudyHubRoute

@Serializable
data object GrammarListRoute

@Serializable
data class GrammarDetailRoute(val topicId: String)

@Serializable
data object ReadingListRoute

@Serializable
data class ReadingSessionRoute(val passageId: String)

@Serializable
data object ListeningListRoute

@Serializable
data class ListeningSessionRoute(val lessonId: String)

@Serializable
data object ProgressRoute

@Serializable
data object SettingsRoute

@Serializable
data class VoiceQuickCaptureRoute(val initialExpression: String = "")
