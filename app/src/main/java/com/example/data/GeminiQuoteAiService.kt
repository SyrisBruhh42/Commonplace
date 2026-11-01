package com.example.data

import android.graphics.Bitmap
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.Chat
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.ThinkingLevel
import com.google.firebase.ai.type.Tool
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import com.google.firebase.ai.type.thinkingConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import java.util.UUID

private const val TAG = "GeminiQuoteAiService"

data class PhotoMatchedQuoteResult(
    val visualMoodSummary: String,
    val detectedThemes: List<String>,
    val matchedQuotes: List<QuoteItem>
)

enum class InTheMomentStyle(val label: String, val promptDirective: String) {
    WITTY_HUMOR(
        "Witty & Playful Humor",
        "Adapt to a sharp, playful, humorous style (reminiscent of Oscar Wilde, Mark Twain, or Douglas Adams) that uses clever irony and warmth while still landing a genuinely deep philosophical truth."
    ),
    COMPLEX_POLYMATH(
        "Complex Cross-Field Synthesis",
        "Craft an intellectually rich, layered quote that bridges seemingly distant disciplines into a single illuminating metaphor."
    ),
    STOIC_PARADOX(
        "Stoic & Paradoxical Wit",
        "Blend Stoic composure with a surprising, memorable paradox that makes the reader smile and pause."
    ),
    LYRICAL_WONDER(
        "Poetic & Cosmic Wonder",
        "Use luminous, rhythmic imagery that connects everyday human moments with vast scientific or artistic wonder."
    )
}

object CrossDisciplinaryFields {
    val ALL = listOf(
        "Quantum Physics",
        "Stoic Ethics",
        "Jazz Improvisation",
        "Culinary Arts",
        "Botany & Ecology",
        "Software & Logic",
        "Ancient Mythology",
        "Everyday Comedy",
        "Architecture",
        "Astronomy"
    )
}

data class InTheMomentQuoteResult(
    val quoteItem: QuoteItem,
    val fieldConnectionInsight: String,
    val companionReflection: String
)

class GeminiQuoteAiService {

    // 1. Low-latency model (gemini-3.1-flash-lite) for instant takeaways & mood resonance
    private val fastFlashLiteModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.1-flash-lite"
        )
    }

    // 2. General structured quote generator model (gemini-3.8-flash)
    private val quoteDiscoverySchema = Schema.obj(
        properties = mapOf(
            "quoteText" to Schema.string(description = "The exact text of the famous quote"),
            "author" to Schema.string(description = "Full name of the historical or literary figure"),
            "sourceWork" to Schema.string(description = "Book, speech, letter, or essay source"),
            "category" to Schema.string(description = "Primary category (e.g. Stoicism, Literature, Philosophy, Science, Resilience, Art, Leadership, Poetry, History, Mindfulness)"),
            "tags" to Schema.array(items = Schema.string(), description = "2 to 4 thematic tags"),
            "onlinePopularityScore" to Schema.integer(description = "Estimated online cultural frequency from 75 to 99"),
            "popularOnlineContext" to Schema.string(description = "How this quote is commonly cited or used online today"),
            "era" to Schema.string(description = "Year or historical era of the quote")
        )
    )

    private val structuredDiscoveryModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = quoteDiscoverySchema
            }
        )
    }

    // 2b. In-the-Moment Deep Quote Alchemist schema & model (gemini-3.8-flash)
    private val inTheMomentQuoteSchema = Schema.obj(
        properties = mapOf(
            "quoteText" to Schema.string(description = "A deep, memorable quote generated in the moment that adapts to the requested humor/fun style or bridges the selected fields"),
            "attributedPersona" to Schema.string(description = "Creative attribution or philosophical voice (e.g. 'Commonplace Alchemist • Wildean-Feynman Synthesis')"),
            "connectedFieldsSummary" to Schema.string(description = "How the quote connects the chosen fields or balances humor with philosophical depth"),
            "companionCommentary" to Schema.string(description = "A warm, conversational reflection from the Philosophical Companion unpacking the meaning and history/inspiration behind this generated quote"),
            "tags" to Schema.array(items = Schema.string(), description = "2 to 4 tags representing the fields and style")
        )
    )

    private val inTheMomentGeneratorModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = inTheMomentQuoteSchema
            }
        )
    }

    // 3. High Thinking model (gemini-4-argon with ThinkingLevel.HIGH, no maxOutputTokens)
    private val highThinkingArgonModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-4-argon",
            generationConfig = generationConfig {
                thinkingConfig = thinkingConfig {
                    thinkingLevel = ThinkingLevel.HIGH
                }
            }
        )
    }

    // 4. Multimodal Image Analysis model (gemini-4-argon) with structured output
    private val photoMatchSchema = Schema.obj(
        properties = mapOf(
            "visualMoodSummary" to Schema.string(description = "Poetic analysis of the photograph's atmosphere, lighting, and philosophical mood"),
            "detectedThemes" to Schema.array(items = Schema.string(), description = "3 to 5 thematic categories inspired by the image"),
            "matchedQuotes" to Schema.array(
                items = Schema.obj(
                    properties = mapOf(
                        "quoteText" to Schema.string(description = "Famous historical or literary quote matching the photo"),
                        "author" to Schema.string(description = "Author of the quote"),
                        "sourceWork" to Schema.string(description = "Book, poem, or work citation"),
                        "category" to Schema.string(description = "Primary category"),
                        "onlinePopularityScore" to Schema.integer(description = "Popularity score 80-99"),
                        "popularOnlineContext" to Schema.string(description = "Why this quote resonates with this visual scene")
                    )
                ),
                description = "3 famous quotes that deeply resonate with the uploaded image"
            )
        )
    )

    private val imageAnalysisArgonModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-4-argon",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = photoMatchSchema
            }
        )
    }

    // 5. Google Search Grounded model (gemini-3.8-flash with googleSearch tool)
    private val searchGroundedModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            tools = listOf(Tool.googleSearch())
        )
    }

    // 6. Google Maps Grounded model (gemini-3.8-flash with googleMaps tool)
    private val mapsGroundedModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            tools = listOf(Tool.googleMaps())
        )
    }

    // 7. Philosophical Chat Companion model (gemini-3.8-flash with systemInstruction)
    private val symposiumChatModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            systemInstruction = content {
                text(
                    "You are the Archivist of Commonplace, an erudite, warm, and witty philosophical companion. " +
                        "You help the user explore the meaning and history of specific saved quotes from their archive, " +
                        "and you also delight in generating deep quotes in the moment—adapting to the user's favorite " +
                        "styles for humor and fun, or synthesizing complex quotes that connect different fields " +
                        "(such as quantum physics and stoicism, jazz and architecture, or botany and everyday comedy). " +
                        "Always keep your responses insightful, memorable, and engaging."
                )
            }
        )
    }

    private var chatSession: Chat? = null

    fun getOrCreateChatSession(): Chat {
        if (chatSession == null) {
            chatSession = symposiumChatModel.startChat(
                history = listOf(
                    content("model") {
                        text(
                            "Welcome to the Philosophical Companion. Select any saved quote to explore its meaning and history, " +
                                "or use the In-the-Moment Quote Alchemist above to generate a deep quote adapted to your favorite humor style or connecting different fields."
                        )
                    }
                )
            )
        }
        return chatSession!!
    }

    fun resetChatSession() {
        chatSession = null
    }

    /**
     * Generates a deep quote in the moment using the Firebase AI SDK, adapting to the user's
     * preferred style (humor/fun or polymath depth), connecting chosen fields, and optionally
     * drawing inspiration from a currently selected saved quote.
     */
    suspend fun generateInTheMomentQuote(
        style: InTheMomentStyle,
        selectedFields: List<String>,
        userMomentPrompt: String,
        inspirationSavedQuote: QuoteItem? = null
    ): Result<InTheMomentQuoteResult> {
        val fieldsText = if (selectedFields.isEmpty()) {
            "Philosophy, Everyday Comedy, and Science"
        } else {
            selectedFields.joinToString(" + ")
        }

        val inspirationClause = if (inspirationSavedQuote != null) {
            "Echo or riff upon the spirit of this saved quote the user is reflecting on: \"${inspirationSavedQuote.text}\" by ${inspirationSavedQuote.author}."
        } else {
            "Make the quote stand on its own as an original, quotable aphorism."
        }

        val momentClause = if (userMomentPrompt.isNotBlank()) {
            "User's current moment or topic: \"$userMomentPrompt\"."
        } else {
            "Craft a timeless observation for a curious mind today."
        }

        val prompt = """
            Generate a deep Quote in the moment for the user.
            Style Directive: ${style.label} — ${style.promptDirective}
            Fields to Connect: [$fieldsText]
            $momentClause
            $inspirationClause
            Ensure the quote is vivid, aphoristic, and genuinely connects the chosen fields or humor style with deep insight.
        """.trimIndent()

        return try {
            val response = withTimeout(60_000L) {
                inTheMomentGeneratorModel.generateContent(prompt)
            }
            val jsonText = response.text ?: throw IllegalStateException("Empty response from In-the-Moment Quote model.")
            val json = JSONObject(jsonText)
            val tagsArr = json.optJSONArray("tags")
            val parsedTags = if (tagsArr != null && tagsArr.length() > 0) {
                (0 until tagsArr.length()).map { tagsArr.getString(it) }
            } else {
                selectedFields.ifEmpty { listOf("Philosophy", "Humor") }
            }

            val quoteItem = QuoteItem(
                id = "moment_${UUID.randomUUID().toString().take(8)}",
                text = json.getString("quoteText"),
                author = json.optString("attributedPersona", "Commonplace Alchemist (${style.label})"),
                sourceWork = "Forged in the Moment • $fieldsText",
                category = parsedTags.firstOrNull() ?: "Philosophy",
                tags = parsedTags,
                onlinePopularityScore = 96,
                popularOnlineContext = json.optString("connectedFieldsSummary", "Synthesized across $fieldsText"),
                era = "In the Moment"
            )

            Result.success(
                InTheMomentQuoteResult(
                    quoteItem = quoteItem,
                    fieldConnectionInsight = json.optString("connectedFieldsSummary", "Bridges $fieldsText in ${style.label} style."),
                    companionReflection = json.optString("companionCommentary", "Here is a quote forged for your exact moment and fields of curiosity.")
                )
            )
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "In-the-moment quote generation timed out", e)
            Result.failure(Exception("Quote generation timed out. Please try again."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "In-the-moment quote generation failed", e)
            Result.failure(e)
        }
    }

    // Low-latency response using gemini-3.1-flash-lite
    suspend fun generateQuickTakeaway(quote: QuoteItem): Result<String> {
        return try {
            val response = withTimeout(30_000L) {
                fastFlashLiteModel.generateContent(
                    "In 2 crisp sentences, give a practical modern takeaway and emotional mood tag for this quote by ${quote.author}: \"${quote.text}\""
                )
            }
            val text = response.text?.trim()
            if (!text.isNullOrBlank()) {
                Result.success(text)
            } else {
                Result.failure(IllegalStateException("Model returned an empty takeaway."))
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Quick takeaway timed out", e)
            Result.failure(Exception("Quick takeaway timed out. Please try again."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Quick takeaway failed", e)
            Result.failure(e)
        }
    }

    // Priority AI Quote Discovery using gemini-3.8-flash structured JSON
    suspend fun discoverPriorityQuoteFromAi(
        selectedInterests: List<String>,
        priorityMode: String,
        recentAuthors: List<String>
    ): Result<QuoteItem> {
        val interestsLabel = if (selectedInterests.isEmpty()) "Stoicism, Literature, Philosophy, Science" else selectedInterests.joinToString(", ")
        val avoidLabel = if (recentAuthors.isEmpty()) "none" else recentAuthors.take(5).joinToString(", ")

        val prompt = """
            Generate a genuine, historically verifiable famous quote prioritized for a reader whose personal interest filters are: [$interestsLabel].
            Active priority mode: $priorityMode (weighing recent relevance, popular online usage across essays/discussions, and personal interest match).
            Prefer an author NOT in this recent list: [$avoidLabel].
        """.trimIndent()

        return try {
            val response = withTimeout(60_000L) {
                structuredDiscoveryModel.generateContent(prompt)
            }
            val jsonText = response.text ?: throw IllegalStateException("Empty JSON response from model.")
            val json = JSONObject(jsonText)
            val tagsArr = json.optJSONArray("tags")
            val tags = if (tagsArr != null) {
                (0 until tagsArr.length()).map { tagsArr.getString(it) }
            } else {
                listOf(json.optString("category", "Philosophy"))
            }

            val item = QuoteItem(
                id = "ai_${UUID.randomUUID().toString().take(8)}",
                text = json.getString("quoteText"),
                author = json.getString("author"),
                sourceWork = json.optString("sourceWork", "Collected Works"),
                category = json.optString("category", "Philosophy"),
                tags = tags,
                onlinePopularityScore = json.optInt("onlinePopularityScore", 90).coerceIn(1, 100),
                popularOnlineContext = json.optString("popularOnlineContext", "Widely shared online in literary and philosophical discussions."),
                era = json.optString("era", "Classical")
            )
            Result.success(item)
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "AI Quote discovery timed out", e)
            Result.failure(Exception("Quote generation timed out after 60 seconds."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "AI Quote discovery failed", e)
            Result.failure(e)
        }
    }

    // High Thinking deep analysis using gemini-4-argon with ThinkingLevel.HIGH
    suspend fun generateDeepMeaningAnalysis(quote: QuoteItem): Result<String> {
        val prompt = """
            Perform a deep philosophical, historical, and rhetorical analysis of this famous quote:
            "${quote.text}" — ${quote.author} (${quote.sourceWork})

            Structure your response with clear headings:
            1. Philosophical & Historical Backstory (the exact moment and intellectual climate when ${quote.author} wrote or spoke these words)
            2. Rhetorical & Literary Craft (why the phrasing endures)
            3. Counter-Perspective (how a rival school of thought would challenge or complement this idea)
            4. Practical Application for Today
        """.trimIndent()

        return try {
            val response = withTimeout(60_000L) {
                highThinkingArgonModel.generateContent(prompt)
            }
            val text = response.text?.trim()
            if (!text.isNullOrBlank()) {
                Result.success(text)
            } else {
                Result.failure(IllegalStateException("Empty deep analysis response."))
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "High-thinking analysis timed out", e)
            Result.failure(Exception("Deep analysis timed out after 60 seconds."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "High-thinking analysis failed", e)
            Result.failure(e)
        }
    }

    // Google Search Grounded online popularity & cultural context (gemini-3.8-flash + googleSearch)
    suspend fun fetchSearchGroundedContext(quote: QuoteItem): Result<String> {
        val prompt = """
            Use Google Search to verify the historical origin, publication context, and current popular online usage of the quote:
            "${quote.text}" by ${quote.author}.
            Include how writers, educators, and online communities reference this quote today, and note any common misattributions or textual variations.
        """.trimIndent()

        return try {
            val response = withTimeout(60_000L) {
                searchGroundedModel.generateContent(prompt)
            }
            val text = response.text?.trim()
            if (!text.isNullOrBlank()) {
                Result.success(text)
            } else {
                Result.failure(IllegalStateException("Empty search-grounded response."))
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Search grounding timed out", e)
            Result.failure(Exception("Google Search grounding timed out."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Search grounding failed", e)
            Result.failure(e)
        }
    }

    // Google Maps Grounded literary landmarks & historical geography (gemini-3.8-flash + googleMaps)
    suspend fun fetchMapsGroundedPlaces(quote: QuoteItem, userLocationHint: String): Result<String> {
        val prompt = """
            Use Google Maps data to highlight real-world historical landmarks, museums, libraries, birthplaces, or study sites connected to ${quote.author} and the work "${quote.sourceWork}" (quote: "${quote.text}").
            Also mention if there are notable literary libraries, philosophical gardens, or cultural landmarks relevant to ${quote.category} near "$userLocationHint".
        """.trimIndent()

        return try {
            val response = withTimeout(60_000L) {
                mapsGroundedModel.generateContent(prompt)
            }
            val text = response.text?.trim()
            if (!text.isNullOrBlank()) {
                Result.success(text)
            } else {
                Result.failure(IllegalStateException("Empty Maps-grounded response."))
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Maps grounding timed out", e)
            Result.failure(Exception("Google Maps grounding timed out."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Maps grounding failed", e)
            Result.failure(e)
        }
    }

    // Analyze Image to match famous quotes using gemini-4-argon
    suspend fun analyzeImageForQuotes(bitmap: Bitmap, userPromptHint: String): Result<PhotoMatchedQuoteResult> {
        val scaledBitmap = scaleBitmapDown(bitmap, 1024)
        val promptText = buildString {
            append("Analyze this image's visual mood, objects, lighting, and symbolic atmosphere. ")
            if (userPromptHint.isNotBlank()) {
                append("Also consider the user's reflection: \"$userPromptHint\". ")
            }
            append("Match it with 3 genuine, famous historical or literary quotes that capture its essence.")
        }

        return try {
            val response = withTimeout(60_000L) {
                imageAnalysisArgonModel.generateContent(
                    content {
                        image(scaledBitmap)
                        text(promptText)
                    }
                )
            }
            val jsonText = response.text ?: throw IllegalStateException("Empty image analysis response.")
            val root = JSONObject(jsonText)
            val moodSummary = root.optString("visualMoodSummary", "A contemplative scene evoking literary reflection.")
            val themesArr = root.optJSONArray("detectedThemes")
            val themes = if (themesArr != null) {
                (0 until themesArr.length()).map { themesArr.getString(it) }
            } else {
                listOf("Literature", "Mindfulness")
            }

            val quotesArr = root.optJSONArray("matchedQuotes")
            val matchedQuotes = mutableListOf<QuoteItem>()
            if (quotesArr != null) {
                for (i in 0 until quotesArr.length()) {
                    val qObj = quotesArr.getJSONObject(i)
                    val cat = qObj.optString("category", themes.firstOrNull() ?: "Literature")
                    matchedQuotes.add(
                        QuoteItem(
                            id = "img_q_${UUID.randomUUID().toString().take(8)}",
                            text = qObj.getString("quoteText"),
                            author = qObj.getString("author"),
                            sourceWork = qObj.optString("sourceWork", "Literary Archive"),
                            category = cat,
                            tags = themes.take(3),
                            onlinePopularityScore = qObj.optInt("onlinePopularityScore", 91).coerceIn(1, 100),
                            popularOnlineContext = qObj.optString("popularOnlineContext", "Matched via visual mood analysis.")
                        )
                    )
                }
            }
            Result.success(
                PhotoMatchedQuoteResult(
                    visualMoodSummary = moodSummary,
                    detectedThemes = themes,
                    matchedQuotes = matchedQuotes
                )
            )
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Image quote analysis timed out", e)
            Result.failure(Exception("Image analysis timed out after 60 seconds."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Image quote analysis failed", e)
            Result.failure(e)
        }
    }

    // Streaming multi-turn chat message
    fun sendChatMessageStream(userMessage: String): Flow<String> {
        val chat = getOrCreateChatSession()
        return chat.sendMessageStream(userMessage).map { chunk ->
            chunk.text.orEmpty()
        }
    }

    private fun scaleBitmapDown(bitmap: Bitmap, maxDimension: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        if (width <= maxDimension && height <= maxDimension) return bitmap
        val ratio = width.toFloat() / height.toFloat()
        val (newWidth, newHeight) = if (width > height) {
            maxDimension to (maxDimension / ratio).toInt().coerceAtLeast(1)
        } else {
            (maxDimension * ratio).toInt().coerceAtLeast(1) to maxDimension
        }
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }
}
