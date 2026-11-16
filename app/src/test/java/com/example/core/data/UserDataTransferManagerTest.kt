package com.example.core.data

import com.example.core.model.AppLanguage
import com.example.core.model.CefrLevel
import com.example.core.model.ContextExample
import com.example.core.model.ContextSourceType
import com.example.core.model.ReviewState
import com.example.core.model.TargetExam
import com.example.core.model.ThemeMode
import com.example.core.model.UserSettings
import com.example.core.model.VocabularyEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UserDataTransferManagerTest {

    @Test
    fun `export and re-import round-trips valid vocabulary and contexts`() {
        val vocab = listOf(
            VocabularyEntry(
                id = "v_100",
                expression = "corroborate",
                normalizedExpression = "corroborate",
                meaningTr = "doğrulamak",
                tags = listOf("SAT"),
                favorite = true,
                createdAtEpochMillis = 1000L,
                updatedAtEpochMillis = 2000L
            )
        )
        val contexts = listOf(
            ContextExample(
                id = "c_100",
                vocabularyId = "v_100",
                sentence = "New fossils corroborate the hypothesis.",
                sourceTitle = "Science Talk",
                sourceType = ContextSourceType.VIDEO,
                createdAtEpochMillis = 1500L
            )
        )
        val states = listOf(
            ReviewState(
                vocabularyId = "v_100",
                dueAtEpochMillis = 5000L,
                intervalDays = 1,
                easeFactor = 2.5,
                repetitionCount = 1,
                lapseCount = 0
            )
        )
        val settings = UserSettings(
            language = AppLanguage.TURKISH,
            themeMode = ThemeMode.DARK,
            dailyGoalMinutes = 30,
            targetExam = TargetExam.SAT,
            cefrLevel = CefrLevel.C1
        )

        val jsonStr = UserDataTransferManager.exportToJson(vocab, contexts, states, settings, 9999L)
        val parsed = UserDataTransferManager.parseAndValidateImport(jsonStr, emptySet())
        assertTrue(parsed.isSuccess)
        val bundle = parsed.getOrThrow()
        assertEquals(1, bundle.vocabulary.size)
        assertEquals("corroborate", bundle.vocabulary.first().expression)
        assertEquals(1, bundle.contexts.size)
    }

    @Test
    fun `rejects oversized or malformed json imports cleanly`() {
        val invalid = UserDataTransferManager.parseAndValidateImport("{not valid json", emptySet())
        assertTrue(invalid.isFailure)

        val orphanContextJson = """
            {
              "schemaVersion": 1,
              "exportedAtEpochMillis": 100,
              "vocabulary": [],
              "contexts": [
                {
                  "id": "c_orphan",
                  "vocabularyId": "missing_v",
                  "sentence": "Orphan sentence",
                  "sourceTitle": "",
                  "sourceType": "MANUAL",
                  "createdAtEpochMillis": 100
                }
              ],
              "reviewStates": []
            }
        """.trimIndent()
        val orphanResult = UserDataTransferManager.parseAndValidateImport(orphanContextJson, emptySet())
        assertTrue(orphanResult.isFailure)
    }
}
