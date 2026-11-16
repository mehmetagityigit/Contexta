package com.example.core.model

/**
 * Pure Kotlin validator for Contexta curriculum content packs and user export bundles.
 * Checks missing fields, duplicate IDs, invalid correctOptionId, empty explanations,
 * invalid CEFR levels, and invalid evidence references.
 */
object ContentValidator {

    private val validLevels = setOf("B1", "B2", "C1", "C2", "B1-B2", "C1-C2")
    private val validStatuses = setOf("draft", "published", "archived")
    private val validReadingGenres = setOf(
        "Natural Sciences",
        "Social Sciences & History",
        "Humanities",
        "Technology & AI"
    )
    private val validListeningTypes = setOf("ACADEMIC_LECTURE", "DIALOGUE", "SHORT_STORY")

    const val MAX_IMPORT_BYTES = 2 * 1024 * 1024 // 2 MB safety limit

    data class ValidationResult(
        val isValid: Boolean,
        val errors: List<String>
    )

    fun validateGrammarPack(pack: GrammarPack): ValidationResult {
        val errors = mutableListOf<String>()
        val topicIds = mutableSetOf<String>()
        val questionIds = mutableSetOf<String>()

        if (pack.topics.isEmpty()) {
            errors += "Grammar pack contains no topics."
        }

        pack.topics.forEach { topic ->
            if (topic.id.isBlank()) errors += "Grammar topic missing id."
            if (!topicIds.add(topic.id)) errors += "Duplicate grammar topic id: ${topic.id}"
            if (topic.schemaVersion <= 0) errors += "Invalid schemaVersion on grammar topic: ${topic.id}"
            if (topic.contentVersion <= 0) errors += "Invalid contentVersion on grammar topic: ${topic.id}"
            if (topic.level !in validLevels) errors += "Invalid level '${topic.level}' on grammar topic: ${topic.id}"
            if (topic.title.isBlank()) errors += "Missing title on grammar topic: ${topic.id}"
            if (topic.status !in validStatuses) errors += "Invalid status '${topic.status}' on grammar topic: ${topic.id}"
            if (topic.source.isBlank()) errors += "Missing source on grammar topic: ${topic.id}"
            if (topic.license.isBlank()) errors += "Missing license on grammar topic: ${topic.id}"
            if (topic.reviewedAt.isBlank()) errors += "Missing reviewedAt on grammar topic: ${topic.id}"
            if (topic.summaryTr.isBlank()) errors += "Missing summaryTr on grammar topic: ${topic.id}"
            if (topic.examples.isEmpty()) errors += "Missing examples on grammar topic: ${topic.id}"
            if (topic.questions.isEmpty()) errors += "Missing questions on grammar topic: ${topic.id}"

            validateQuestions(
                questions = topic.questions,
                parentId = topic.id,
                validEvidenceIds = emptySet(),
                requireEvidenceForCommand = false,
                seenQuestionIds = questionIds,
                errors = errors
            )
        }
        return ValidationResult(errors.isEmpty(), errors)
    }

    fun validateReadingPack(pack: ReadingPack): ValidationResult {
        val errors = mutableListOf<String>()
        val passageIds = mutableSetOf<String>()
        val questionIds = mutableSetOf<String>()

        if (pack.passages.isEmpty()) {
            errors += "Reading pack contains no passages."
        }

        pack.passages.forEach { passage ->
            if (passage.id.isBlank()) errors += "Reading passage missing id."
            if (!passageIds.add(passage.id)) errors += "Duplicate reading passage id: ${passage.id}"
            if (passage.schemaVersion <= 0) errors += "Invalid schemaVersion on reading: ${passage.id}"
            if (passage.contentVersion <= 0) errors += "Invalid contentVersion on reading: ${passage.id}"
            if (passage.level !in validLevels) errors += "Invalid level '${passage.level}' on reading: ${passage.id}"
            if (passage.levelGroup !in setOf("B1-B2", "C1-C2")) {
                errors += "Invalid levelGroup '${passage.levelGroup}' on reading: ${passage.id}"
            }
            if (passage.genre !in validReadingGenres) {
                errors += "Invalid genre '${passage.genre}' on reading: ${passage.id}"
            }
            if (passage.title.isBlank()) errors += "Missing title on reading: ${passage.id}"
            if (passage.status !in validStatuses) errors += "Invalid status '${passage.status}' on reading: ${passage.id}"
            if (passage.source.isBlank()) errors += "Missing source on reading: ${passage.id}"
            if (passage.license.isBlank()) errors += "Missing license on reading: ${passage.id}"
            if (passage.reviewedAt.isBlank()) errors += "Missing reviewedAt on reading: ${passage.id}"
            if (passage.paragraphs.isEmpty()) errors += "Missing paragraphs on reading: ${passage.id}"

            val paragraphIds = mutableSetOf<String>()
            passage.paragraphs.forEach { p ->
                if (p.id.isBlank()) errors += "Reading paragraph missing id in ${passage.id}"
                if (!paragraphIds.add(p.id)) errors += "Duplicate paragraph id '${p.id}' in ${passage.id}"
                if (p.text.isBlank()) errors += "Empty paragraph text '${p.id}' in ${passage.id}"
            }

            if (passage.questions.isEmpty()) errors += "Missing questions on reading: ${passage.id}"
            validateQuestions(
                questions = passage.questions,
                parentId = passage.id,
                validEvidenceIds = paragraphIds,
                requireEvidenceForCommand = true,
                seenQuestionIds = questionIds,
                errors = errors
            )
        }
        return ValidationResult(errors.isEmpty(), errors)
    }

    fun validateListeningPack(pack: ListeningPack): ValidationResult {
        val errors = mutableListOf<String>()
        val lessonIds = mutableSetOf<String>()
        val questionIds = mutableSetOf<String>()

        if (pack.lessons.isEmpty()) {
            errors += "Listening pack contains no lessons."
        }

        pack.lessons.forEach { lesson ->
            if (lesson.id.isBlank()) errors += "Listening lesson missing id."
            if (!lessonIds.add(lesson.id)) errors += "Duplicate listening lesson id: ${lesson.id}"
            if (lesson.schemaVersion <= 0) errors += "Invalid schemaVersion on listening: ${lesson.id}"
            if (lesson.contentVersion <= 0) errors += "Invalid contentVersion on listening: ${lesson.id}"
            if (lesson.level !in validLevels) errors += "Invalid level '${lesson.level}' on listening: ${lesson.id}"
            if (lesson.contentType !in validListeningTypes) {
                errors += "Invalid contentType '${lesson.contentType}' on listening: ${lesson.id}"
            }
            if (lesson.title.isBlank()) errors += "Missing title on listening: ${lesson.id}"
            if (lesson.status !in validStatuses) errors += "Invalid status '${lesson.status}' on listening: ${lesson.id}"
            if (lesson.source.isBlank()) errors += "Missing source on listening: ${lesson.id}"
            if (lesson.license.isBlank()) errors += "Missing license on listening: ${lesson.id}"
            if (lesson.reviewedAt.isBlank()) errors += "Missing reviewedAt on listening: ${lesson.id}"
            if (lesson.transcript.isEmpty()) errors += "Missing transcript lines on listening: ${lesson.id}"
            if (lesson.audioMode !in setOf("MEDIA3", "TTS")) {
                errors += "Invalid audioMode '${lesson.audioMode}' on listening: ${lesson.id}"
            }
            if (lesson.audioMode == "MEDIA3" && lesson.audioUri.isNullOrBlank()) {
                errors += "MEDIA3 listening lesson '${lesson.id}' missing audioUri."
            }

            val lineIds = mutableSetOf<String>()
            lesson.transcript.forEach { line ->
                if (line.id.isBlank()) errors += "Transcript line missing id in ${lesson.id}"
                if (!lineIds.add(line.id)) errors += "Duplicate transcript line id '${line.id}' in ${lesson.id}"
                if (line.text.isBlank()) errors += "Empty transcript text '${line.id}' in ${lesson.id}"
            }

            if (lesson.questions.isEmpty()) errors += "Missing questions on listening: ${lesson.id}"
            validateQuestions(
                questions = lesson.questions,
                parentId = lesson.id,
                validEvidenceIds = lineIds,
                requireEvidenceForCommand = false,
                seenQuestionIds = questionIds,
                errors = errors
            )
        }
        return ValidationResult(errors.isEmpty(), errors)
    }

    fun validateExportBundle(bundle: UserDataExportBundle, existingVocabIds: Set<String> = emptySet()): ValidationResult {
        val errors = mutableListOf<String>()
        if (bundle.schemaVersion != 1) {
            errors += "Unsupported export schemaVersion: ${bundle.schemaVersion}"
        }
        val vocabIds = mutableSetOf<String>()
        bundle.vocabulary.forEach { v ->
            if (v.id.isBlank() || !isValidId(v.id)) errors += "Invalid vocabulary id: '${v.id}'"
            if (!vocabIds.add(v.id)) errors += "Duplicate vocabulary id in import: '${v.id}'"
            if (v.expression.isBlank() || v.expression.length > 200) {
                errors += "Invalid vocabulary expression for id '${v.id}'"
            }
            if (v.tags.size > 10) errors += "Too many tags on vocabulary '${v.id}'"
        }
        val contextIds = mutableSetOf<String>()
        bundle.contexts.forEach { c ->
            if (c.id.isBlank() || !isValidId(c.id)) errors += "Invalid context id: '${c.id}'"
            if (!contextIds.add(c.id)) errors += "Duplicate context id in import: '${c.id}'"
            if (c.vocabularyId !in vocabIds && c.vocabularyId !in existingVocabIds) {
                errors += "Context '${c.id}' references missing vocabularyId '${c.vocabularyId}'"
            }
            if (c.sentence.isBlank() || c.sentence.length > 1000) {
                errors += "Invalid context sentence for id '${c.id}'"
            }
        }
        bundle.reviewStates.forEach { r ->
            if (r.vocabularyId !in vocabIds && r.vocabularyId !in existingVocabIds) {
                errors += "ReviewState references missing vocabularyId '${r.vocabularyId}'"
            }
            if (r.easeFactor < 1.3 || r.easeFactor > 5.0) {
                errors += "Invalid easeFactor ${r.easeFactor} for '${r.vocabularyId}'"
            }
        }
        return ValidationResult(errors.isEmpty(), errors)
    }

    private fun isValidId(id: String): Boolean {
        return id.length in 1..128 && id.all { it.isLetterOrDigit() || it == '_' || it == '-' }
    }

    private fun validateQuestions(
        questions: List<PracticeQuestion>,
        parentId: String,
        validEvidenceIds: Set<String>,
        requireEvidenceForCommand: Boolean,
        seenQuestionIds: MutableSet<String>,
        errors: MutableList<String>
    ) {
        questions.forEach { q ->
            if (q.id.isBlank()) errors += "Question missing id in $parentId"
            if (!seenQuestionIds.add(q.id)) errors += "Duplicate question id '${q.id}' (in $parentId)"
            if (q.prompt.isBlank()) errors += "Empty prompt on question '${q.id}'"
            if (q.explanationTr.isBlank()) errors += "Empty explanationTr on question '${q.id}'"
            if (q.options.size < 2) errors += "Question '${q.id}' has fewer than 2 options"

            val optionIds = mutableSetOf<String>()
            q.options.forEach { opt ->
                if (opt.id.isBlank()) errors += "Option missing id in question '${q.id}'"
                if (!optionIds.add(opt.id)) errors += "Duplicate option id '${opt.id}' in question '${q.id}'"
                if (opt.text.isBlank()) errors += "Empty option text '${opt.id}' in question '${q.id}'"
            }
            if (q.correctOptionId.isBlank() || q.correctOptionId !in optionIds) {
                errors += "Invalid correctOptionId '${q.correctOptionId}' on question '${q.id}'"
            }
            val refId = q.evidenceRefId
            if (requireEvidenceForCommand && q.questionType == "Command of Evidence" && refId.isNullOrBlank()) {
                errors += "Command of Evidence question '${q.id}' missing evidenceRefId"
            }
            if (!refId.isNullOrBlank() && validEvidenceIds.isNotEmpty() && refId !in validEvidenceIds) {
                errors += "Invalid evidenceRefId '$refId' on question '${q.id}' in $parentId"
            }
        }
    }
}
