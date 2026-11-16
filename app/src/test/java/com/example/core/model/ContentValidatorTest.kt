package com.example.core.model

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ContentValidatorTest {

    private val json = Json { ignoreUnknownKeys = true }

    private fun assetFile(name: String): File {
        val direct = File("src/main/assets/content/$name")
        if (direct.exists()) return direct
        return File("app/src/main/assets/content/$name")
    }

    @Test
    fun `bundled grammar pack passes validation and has 15 topics with at least 3 questions each`() {
        val raw = assetFile("grammar_pack.json").readText()
        val pack = json.decodeFromString<GrammarPack>(raw)
        val result = ContentValidator.validateGrammarPack(pack)
        assertTrue(result.errors.joinToString("\n"), result.isValid)
        assertEquals(15, pack.topics.size)
        assertTrue(pack.topics.all { it.questions.size >= 3 })
    }

    @Test
    fun `bundled reading pack passes validation with 8 passages and Command of Evidence refs`() {
        val raw = assetFile("reading_pack.json").readText()
        val pack = json.decodeFromString<ReadingPack>(raw)
        val result = ContentValidator.validateReadingPack(pack)
        assertTrue(result.errors.joinToString("\n"), result.isValid)
        assertEquals(8, pack.passages.size)
        assertEquals(4, pack.passages.count { it.levelGroup == "B1-B2" })
        assertEquals(4, pack.passages.count { it.levelGroup == "C1-C2" })
        assertTrue(pack.passages.all { it.questions.size >= 4 })
    }

    @Test
    fun `bundled listening pack passes validation with 3 distinct lesson types`() {
        val raw = assetFile("listening_pack.json").readText()
        val pack = json.decodeFromString<ListeningPack>(raw)
        val result = ContentValidator.validateListeningPack(pack)
        assertTrue(result.errors.joinToString("\n"), result.isValid)
        assertEquals(3, pack.lessons.size)
        assertEquals(setOf("ACADEMIC_LECTURE", "DIALOGUE", "SHORT_STORY"), pack.lessons.map { it.contentType }.toSet())
        assertTrue(pack.lessons.all { it.questions.size >= 4 })
    }

    @Test
    fun `reading validation catches missing evidenceRefId on Command of Evidence`() {
        val invalidPassage = ReadingPassage(
            id = "bad_r1",
            schemaVersion = 1,
            contentVersion = 1,
            level = "B2",
            levelGroup = "B1-B2",
            genre = "Natural Sciences",
            targetExams = listOf("SAT"),
            title = "Bad Passage",
            tags = listOf("science"),
            status = "published",
            source = "Contexta",
            license = "Proprietary",
            reviewedAt = "2026-01-01",
            paragraphs = listOf(ReadingParagraph("p1", "Text body.")),
            questions = listOf(
                PracticeQuestion(
                    id = "q_bad",
                    prompt = "Which supports?",
                    questionType = "Command of Evidence",
                    options = listOf(
                        QuestionOption("a", "Option A"),
                        QuestionOption("b", "Option B")
                    ),
                    correctOptionId = "a",
                    explanationTr = "Açıklama",
                    evidenceRefId = null
                )
            )
        )
        val res = ContentValidator.validateReadingPack(ReadingPack(listOf(invalidPassage)))
        assertFalse(res.isValid)
    }
}
