package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.core.firebase.FirestoreUserDataRepository
import com.example.core.model.ContextSourceType
import com.example.core.model.ReviewRating
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class VocabularyRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun saveVocabularyAndObserve_authenticatedOwner_succeedsAndDeduplicates() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repo = FirestoreUserDataRepository(firestore, uid)

        val uniqueWord = "ephemeral_${UUID.randomUUID().toString().take(6)}"
        val vocabId1 = withTimeout(TIMEOUT_MS) {
            repo.saveVocabularyWithOptionalContext(
                expression = uniqueWord,
                meaningTr = "geçici",
                contextSentence = "Morning frost is $uniqueWord in desert heat.",
                sourceTitle = "Nature Essay",
                sourceType = ContextSourceType.BOOK
            ).getOrThrow()
        }

        // Duplicate normalized expression attaches context to existing word rather than creating duplicate
        val vocabId2 = withTimeout(TIMEOUT_MS) {
            repo.saveVocabularyWithOptionalContext(
                expression = "  $uniqueWord ",
                contextSentence = "Second context for $uniqueWord.",
                sourceTitle = "Lecture",
                sourceType = ContextSourceType.VIDEO
            ).getOrThrow()
        }
        assertEquals(vocabId1, vocabId2)

        val observed = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observeVocabulary().first { list -> list.any { it.id == vocabId1 } }
        }
        assertTrue(observed.any { it.id == vocabId1 && it.meaningTr == "geçici" })
    }

    @Test
    fun submitFlashcardReview_isIdempotentAndUpdatesReviewState() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repo = FirestoreUserDataRepository(firestore, uid)

        val word = "mitigate_${UUID.randomUUID().toString().take(6)}"
        val vocabId = withTimeout(TIMEOUT_MS) {
            repo.saveVocabularyWithOptionalContext(
                expression = word,
                meaningTr = "hafifletmek"
            ).getOrThrow()
        }

        val eventId = "evt_${UUID.randomUUID().toString().replace("-", "").take(12)}"
        val now = System.currentTimeMillis()
        val firstState = withTimeout(TIMEOUT_MS) {
            repo.submitFlashcardReview(vocabId, ReviewRating.GOOD, eventId, now).getOrThrow()
        }
        assertEquals(1, firstState.intervalDays)
        assertEquals(1, firstState.repetitionCount)

        // Retrying the exact same eventId does not double-advance intervalDays
        val retriedState = withTimeout(TIMEOUT_MS) {
            repo.submitFlashcardReview(vocabId, ReviewRating.GOOD, eventId, now + 5000L).getOrThrow()
        }
        assertEquals(1, retriedState.intervalDays)
        assertEquals(1, retriedState.repetitionCount)
    }

    @Test
    fun crossUserAccess_readingAnotherUsersVocabulary_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val aliceRepo = FirestoreUserDataRepository(firestore, aliceUid)
        val vocabId = withTimeout(TIMEOUT_MS) {
            aliceRepo.saveVocabularyWithOptionalContext(
                expression = "ubiquitous_${UUID.randomUUID().toString().take(6)}",
                meaningTr = "yaygın"
            ).getOrThrow()
        }

        signInTestUser(BOB_EMAIL)
        var capturedError: Throwable? = null
        try {
            withTimeout(TIMEOUT_MS) {
                firestore.collection("users").document(aliceUid)
                    .collection("vocabulary").document(vocabId)
                    .get().await()
            }
        } catch (t: Throwable) {
            capturedError = t
        }
        val fsError = capturedError?.unwrapFirestoreException()
        assertNotNull("Expected PERMISSION_DENIED for cross-user read, got $capturedError", fsError)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsError?.code)
    }

    @Test
    fun unauthenticatedObserve_vocabulary_failsWithPermissionDenied() = runBlocking {
        auth.signOut()
        val unauthRepo = FirestoreUserDataRepository(firestore, "some_user_id")

        var capturedError: Throwable? = null
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                unauthRepo.observeVocabulary().first()
            }
        } catch (t: Throwable) {
            capturedError = t
        }
        val fsError = capturedError?.unwrapFirestoreException()
        assertNotNull("Expected PERMISSION_DENIED when signed out, got $capturedError", fsError)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsError?.code)
    }

    private companion object {
        const val ALICE_EMAIL = "alice_ctx@test.com"
        const val BOB_EMAIL = "bob_ctx@test.com"
        const val TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 3500L
    }
}
