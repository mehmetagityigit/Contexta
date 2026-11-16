package com.example.core.firebase

import android.util.Log
import com.example.core.data.AiWordHelperRepository
import com.example.core.data.AiWordInsight
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

/**
 * Optional on-demand vocabulary helper powered by Firebase AI Logic + App Check.
 * Never auto-invoked without user tap; core app workflows do not depend on this service.
 */
class FirebaseAiWordHelperRepository : AiWordHelperRepository {

    private val insightSchema = Schema.obj(
        properties = mapOf(
            "meaningTr" to Schema.string(description = "Concise Turkish meaning of the English expression"),
            "definitionEn" to Schema.string(description = "Clear, learner-friendly English definition"),
            "partOfSpeech" to Schema.string(description = "Short grammatical label such as noun, verb, adj., phrase"),
            "exampleSentence" to Schema.string(description = "Natural English example sentence matching the context"),
            "usageNoteTr" to Schema.string(description = "Brief Turkish note on register or common collocation")
        )
    )

    private val model by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = insightSchema
            }
        )
    }

    override suspend fun generateWordInsight(
        expression: String,
        contextSentence: String?
    ): Result<AiWordInsight> {
        val cleaned = expression.trim()
        if (cleaned.isEmpty()) {
            return Result.failure(IllegalArgumentException("Expression cannot be empty."))
        }

        val prompt = buildString {
            append("Explain the English word or expression for a Turkish learner preparing for academic English (SAT/IELTS/AP).\n")
            append("Expression: \"").append(cleaned).append("\"\n")
            if (!contextSentence.isNullOrBlank()) {
                append("Context sentence: \"").append(contextSentence.trim()).append("\"\n")
            }
            append("Provide an accurate Turkish meaning, a concise English definition, part of speech, one natural English example sentence, and a short Turkish usage tip.")
        }

        return try {
            val response = withTimeout(45_000L) {
                model.generateContent(prompt)
            }
            val rawJson = response.text
                ?: return Result.failure(IllegalStateException("AI returned an empty response."))
            val json = JSONObject(rawJson)
            val insight = AiWordInsight(
                meaningTr = json.getString("meaningTr").trim(),
                definitionEn = json.getString("definitionEn").trim(),
                partOfSpeech = json.getString("partOfSpeech").trim(),
                exampleSentence = json.getString("exampleSentence").trim(),
                usageNoteTr = json.getString("usageNoteTr").trim()
            )
            Result.success(insight)
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "AI word insight timed out", e)
            Result.failure(Exception("AI yanıtı zaman aşımına uğradı (45 sn). Lütfen tekrar deneyin."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "AI word insight failed", e)
            Result.failure(e)
        }
    }

    private companion object {
        const val TAG = "AiWordHelperRepo"
    }
}
