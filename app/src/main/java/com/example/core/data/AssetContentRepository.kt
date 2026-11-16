package com.example.core.data

import android.content.Context
import android.util.Log
import com.example.core.model.ContentValidator
import com.example.core.model.GrammarPack
import com.example.core.model.GrammarTopic
import com.example.core.model.ListeningLesson
import com.example.core.model.ListeningPack
import com.example.core.model.ReadingPack
import com.example.core.model.ReadingPassage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

class AssetContentRepository(
    context: Context
) : ContentRepository {
    private val appContext = context.applicationContext
    private val json = Json { ignoreUnknownKeys = true }

    @Volatile
    private var cachedGrammar: List<GrammarTopic>? = null

    @Volatile
    private var cachedReading: List<ReadingPassage>? = null

    @Volatile
    private var cachedListening: List<ListeningLesson>? = null

    override suspend fun getGrammarTopics(): List<GrammarTopic> {
        cachedGrammar?.let { return it }
        return withContext(Dispatchers.IO) {
            val raw = appContext.assets.open("content/grammar_pack.json").bufferedReader().use { it.readText() }
            val pack = json.decodeFromString<GrammarPack>(raw)
            val validation = ContentValidator.validateGrammarPack(pack)
            if (!validation.isValid) {
                Log.w(TAG, "Grammar pack validation warnings: ${validation.errors.joinToString()}")
            }
            val published = pack.topics.filter { it.status == "published" }.sortedBy { it.orderIndex }
            cachedGrammar = published
            published
        }
    }

    override suspend fun getReadingPassages(): List<ReadingPassage> {
        cachedReading?.let { return it }
        return withContext(Dispatchers.IO) {
            val raw = appContext.assets.open("content/reading_pack.json").bufferedReader().use { it.readText() }
            val pack = json.decodeFromString<ReadingPack>(raw)
            val validation = ContentValidator.validateReadingPack(pack)
            if (!validation.isValid) {
                Log.w(TAG, "Reading pack validation warnings: ${validation.errors.joinToString()}")
            }
            val published = pack.passages.filter { it.status == "published" }
            cachedReading = published
            published
        }
    }

    override suspend fun getListeningLessons(): List<ListeningLesson> {
        cachedListening?.let { return it }
        return withContext(Dispatchers.IO) {
            val raw = appContext.assets.open("content/listening_pack.json").bufferedReader().use { it.readText() }
            val pack = json.decodeFromString<ListeningPack>(raw)
            val validation = ContentValidator.validateListeningPack(pack)
            if (!validation.isValid) {
                Log.w(TAG, "Listening pack validation warnings: ${validation.errors.joinToString()}")
            }
            val published = pack.lessons.filter { it.status == "published" }
            cachedListening = published
            published
        }
    }

    private companion object {
        const val TAG = "AssetContentRepo"
    }
}
