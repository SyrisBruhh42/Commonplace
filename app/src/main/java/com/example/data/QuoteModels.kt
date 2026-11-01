package com.example.data

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

/**
 * Provenance stamp information capturing the location and date/time of capture
 * when a user saves a quote to Firestore.
 */
@IgnoreExtraProperties
data class TimestampInfo(
    val location: String = "",
    val date: String = "",
    val time: String = "",
    val capturedAt: String = ""
)

/**
 * Firestore document data class representing a saved quote with its text, author,
 * tags, and TimestampInfo provenance object.
 */
@IgnoreExtraProperties
data class Quote(
    @DocumentId val id: String = "",
    val userId: String = "",
    val text: String = "",
    val author: String = "",
    val tags: List<String> = emptyList(),
    val timestampInfo: TimestampInfo = TimestampInfo(),
    val sourceWork: String = "",
    val category: String = "",
    val onlinePopularityScore: Long = 80L,
    val personalNote: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    @get:Exclude
    val quoteText: String
        get() = text

    @get:Exclude
    val locationStamp: String
        get() = timestampInfo.location

    @get:Exclude
    val dateStamp: String
        get() = timestampInfo.date

    @get:Exclude
    val timeStamp: String
        get() = timestampInfo.time
}

typealias SavedQuoteDocument = Quote

/**
 * Firestore document for a daily featured quote stored in the `/daily_quotes` collection.
 */
@IgnoreExtraProperties
data class DailyQuoteDocument(
    @DocumentId val id: String = "",
    val text: String = "",
    val author: String = "",
    val sourceWork: String = "",
    val category: String = "",
    val tags: List<String> = emptyList(),
    val onlinePopularityScore: Long = 95L,
    val popularOnlineContext: String = "",
    val era: String = "Classical",
    val dayKey: String = "",
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toQuoteItem(): QuoteItem = QuoteItem(
        id = id.ifBlank { "daily_$dayKey" },
        text = text,
        author = author,
        sourceWork = sourceWork,
        category = category,
        tags = tags.ifEmpty { listOf(category) },
        onlinePopularityScore = onlinePopularityScore.toInt(),
        popularOnlineContext = popularOnlineContext,
        era = era
    )
}

/**
 * Zero-trust Family Social Circle document in `/family_circles/{circleId}`.
 * Only members whose Google UID is in `memberUids` or whose verified Google email
 * is in `memberEmails` can read or interact with the circle.
 */
@IgnoreExtraProperties
data class FamilyCircle(
    @DocumentId val id: String = "",
    val name: String = "",
    val inviteCode: String = "",
    val ownerUid: String = "",
    val ownerEmail: String = "",
    val memberUids: List<String> = emptyList(),
    val memberEmails: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

/**
 * Zero-trust shared quote document in `/family_shared_quotes/{sharedQuoteId}`.
 * Includes the Quote's text, author, tags, and TimestampInfo (location & date/time of capture)
 * along with explicit `allowedMemberUids` and `allowedEmails` access control lists.
 */
@IgnoreExtraProperties
data class FamilySharedQuote(
    @DocumentId val id: String = "",
    val circleId: String = "",
    val circleName: String = "",
    val senderUid: String = "",
    val senderName: String = "",
    val senderEmail: String = "",
    val allowedMemberUids: List<String> = emptyList(),
    val allowedEmails: List<String> = emptyList(),
    val text: String = "",
    val author: String = "",
    val tags: List<String> = emptyList(),
    val timestampInfo: TimestampInfo = TimestampInfo(),
    val personalNote: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class UserPreferencesDocument(
    @DocumentId val id: String = "",
    val userId: String = "",
    val selectedInterests: List<String> = emptyList(),
    val priorityMode: String = "Balanced",
    val recentQuoteIds: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

data class QuoteItem(
    val id: String,
    val text: String,
    val author: String,
    val sourceWork: String,
    val category: String,
    val tags: List<String>,
    val onlinePopularityScore: Int,
    val popularOnlineContext: String,
    val era: String = "Classical"
)

data class PriorityScoredQuote(
    val quote: QuoteItem,
    val totalPriorityScore: Int,
    val interestMatchPoints: Int,
    val popularityPoints: Int,
    val recencyFactorPoints: Int,
    val isRecentlyUsed: Boolean,
    val priorityReason: String
)

enum class PriorityMode(val label: String, val description: String) {
    BALANCED("Balanced", "Blends personal interests, online popularity, and recent uses"),
    PERSONAL_INTERESTS("Personal Interests", "Prioritizes quotes matching your selected categories"),
    POPULAR_ONLINE("Popular Online", "Prioritizes quotes trending across essays and social discourse"),
    RECENT_TRENDING("Recent & Trending", "Surfaces fresh quotes and recently active thematic threads")
}

object QuoteCategories {
    val ALL = listOf(
        "Stoicism",
        "Literature",
        "Philosophy",
        "Science",
        "Resilience",
        "Art",
        "Leadership",
        "Poetry",
        "History",
        "Mindfulness"
    )
}

object SeedQuoteCatalog {
    val allQuotes: List<QuoteItem> = listOf(
        QuoteItem(
            id = "q_seneca_imagination",
            text = "We suffer more often in imagination than in reality.",
            author = "Seneca the Younger",
            sourceWork = "Moral Letters to Lucilius, Letter XIII",
            category = "Stoicism",
            tags = listOf("Stoicism", "Mindfulness", "Resilience"),
            onlinePopularityScore = 98,
            popularOnlineContext = "Widely cited in modern cognitive psychology, mindfulness communities, and anxiety-management essays.",
            era = "1st Century AD"
        ),
        QuoteItem(
            id = "q_aurelius_mind",
            text = "You have power over your mind — not outside events. Realize this, and you will find strength.",
            author = "Marcus Aurelius",
            sourceWork = "Meditations, Book IV",
            category = "Stoicism",
            tags = listOf("Stoicism", "Resilience", "Leadership"),
            onlinePopularityScore = 99,
            popularOnlineContext = "Top-shared Stoic reflection across productivity journals and leadership forums.",
            era = "170 AD"
        ),
        QuoteItem(
            id = "q_woolf_mental_freedom",
            text = "Lock up your libraries if you like; but there is no gate, no lock, no bolt that you can set upon the freedom of my mind.",
            author = "Virginia Woolf",
            sourceWork = "A Room of One's Own",
            category = "Literature",
            tags = listOf("Literature", "Art", "Philosophy"),
            onlinePopularityScore = 94,
            popularOnlineContext = "Celebrated across literary journals, academic essays, and creative writing collectives.",
            era = "1929"
        ),
        QuoteItem(
            id = "q_camus_invincible_summer",
            text = "In the midst of winter, I found there was, within me, an invincible summer.",
            author = "Albert Camus",
            sourceWork = "Return to Tipasa",
            category = "Philosophy",
            tags = listOf("Philosophy", "Resilience", "Literature"),
            onlinePopularityScore = 97,
            popularOnlineContext = "One of the most shared existentialist passages during seasons of personal renewal.",
            era = "1952"
        ),
        QuoteItem(
            id = "q_curie_understand",
            text = "Nothing in life is to be feared, it is only to be understood. Now is the time to understand more, so that we may fear less.",
            author = "Marie Skłodowska Curie",
            sourceWork = "Personal Notebooks & Lectures",
            category = "Science",
            tags = listOf("Science", "Resilience", "Leadership"),
            onlinePopularityScore = 96,
            popularOnlineContext = "Frequently quoted in STEM keynote talks, research manifestos, and science journalism.",
            era = "Early 20th Century"
        ),
        QuoteItem(
            id = "q_rilke_patient",
            text = "Be patient toward all that is unsolved in your heart and try to love the questions themselves, like locked rooms and like books that are now written in a very foreign tongue.",
            author = "Rainer Maria Rilke",
            sourceWork = "Letters to a Young Poet, Letter IV",
            category = "Poetry",
            tags = listOf("Poetry", "Mindfulness", "Literature"),
            onlinePopularityScore = 95,
            popularOnlineContext = "A cornerstone quote in creative mentorship, therapy circles, and slow-living newsletters.",
            era = "1903"
        ),
        QuoteItem(
            id = "q_borges_paradise",
            text = "I have always imagined that Paradise will be a kind of library.",
            author = "Jorge Luis Borges",
            sourceWork = "Poem of the Gifts",
            category = "Literature",
            tags = listOf("Literature", "Poetry", "Philosophy"),
            onlinePopularityScore = 93,
            popularOnlineContext = "Iconic motto among bookstagram communities, archivists, and literary historians.",
            era = "1960"
        ),
        QuoteItem(
            id = "q_feynman_first_principle",
            text = "The first principle is that you must not fool yourself — and you are the easiest person to fool.",
            author = "Richard P. Feynman",
            sourceWork = "Cargo Cult Science (Caltech Commencement Address)",
            category = "Science",
            tags = listOf("Science", "Philosophy", "Leadership"),
            onlinePopularityScore = 94,
            popularOnlineContext = "Essential mental-model quote in rationalist blogs, engineering teams, and decision science.",
            era = "1974"
        ),
        QuoteItem(
            id = "q_epictetus_response",
            text = "It is not things that disturb us, but our judgments about things.",
            author = "Epictetus",
            sourceWork = "Enchiridion, Chapter 5",
            category = "Stoicism",
            tags = listOf("Stoicism", "Mindfulness", "Philosophy"),
            onlinePopularityScore = 92,
            popularOnlineContext = "Foundational epigraph of Cognitive Behavioral Therapy (CBT) and modern Stoic handbooks.",
            era = "125 AD"
        ),
        QuoteItem(
            id = "q_morrison_write_book",
            text = "If there's a book that you want to read, but it hasn't been written yet, then you must write it.",
            author = "Toni Morrison",
            sourceWork = "Ohio Arts Council Address",
            category = "Literature",
            tags = listOf("Literature", "Art", "Leadership"),
            onlinePopularityScore = 95,
            popularOnlineContext = "Shared widely during NaNoWriMo, creative launches, and indie publishing movements.",
            era = "1981"
        ),
        QuoteItem(
            id = "q_van_gogh_great_things",
            text = "Great things are done by a series of small things brought together.",
            author = "Vincent van Gogh",
            sourceWork = "Letter to Theo van Gogh (29 October 1882)",
            category = "Art",
            tags = listOf("Art", "Resilience", "Mindfulness"),
            onlinePopularityScore = 91,
            popularOnlineContext = "Popularized in atomic-habit frameworks, design studios, and craft journals.",
            era = "1882"
        ),
        QuoteItem(
            id = "q_dickinson_hope",
            text = "Hope is the thing with feathers that perches in the soul — and sings the tune without the words — and never stops — at all.",
            author = "Emily Dickinson",
            sourceWork = "Poem 314 (254)",
            category = "Poetry",
            tags = listOf("Poetry", "Resilience", "Literature"),
            onlinePopularityScore = 94,
            popularOnlineContext = "Timeless American lyric verse shared in poetry anthologies and cultural tributes.",
            era = "1861"
        ),
        QuoteItem(
            id = "q_sagan_cosmos",
            text = "The nitrogen in our DNA, the calcium in our teeth, the iron in our blood, the carbon in our apple pies were made in the interiors of collapsing stars. We are made of starstuff.",
            author = "Carl Sagan",
            sourceWork = "Cosmos, Episode 1: The Shores of the Cosmic Ocean",
            category = "Science",
            tags = listOf("Science", "Philosophy", "Poetry"),
            onlinePopularityScore = 97,
            popularOnlineContext = "Viral astronomy reflection linking human consciousness with stellar nucleosynthesis.",
            era = "1980"
        ),
        QuoteItem(
            id = "q_roosevelt_arena",
            text = "It is not the critic who counts; not the man who points out how the strong man stumbles... The credit belongs to the man who is actually in the arena.",
            author = "Theodore Roosevelt",
            sourceWork = "Citizenship in a Republic (Sorbonne Speech, Paris)",
            category = "Leadership",
            tags = listOf("Leadership", "Resilience", "History"),
            onlinePopularityScore = 98,
            popularOnlineContext = "Ubiquitous in entrepreneurship, athletics, and vulnerability research.",
            era = "1910"
        ),
        QuoteItem(
            id = "q_thich_present_moment",
            text = "Smile, breathe, and go slowly. The present moment is filled with joy and happiness. If you are attentive, you will see it.",
            author = "Thích Nhất Hạnh",
            sourceWork = "Peace Is Every Step",
            category = "Mindfulness",
            tags = listOf("Mindfulness", "Philosophy", "Resilience"),
            onlinePopularityScore = 90,
            popularOnlineContext = "Core meditation bell reminder across digital wellness and contemplative communities.",
            era = "1991"
        ),
        QuoteItem(
            id = "q_cicero_garden_library",
            text = "If you have a garden and a library, you have everything you need.",
            author = "Marcus Tullius Cicero",
            sourceWork = "Epistulae ad Familiares (Letter to Varro)",
            category = "History",
            tags = listOf("History", "Literature", "Philosophy"),
            onlinePopularityScore = 91,
            popularOnlineContext = "Beloved slow-living and classical humanities aphorism.",
            era = "46 BC"
        ),
        QuoteItem(
            id = "q_kierkegaard_forward",
            text = "Life can only be understood backwards; but it must be lived forwards.",
            author = "Søren Kierkegaard",
            sourceWork = "Journals and Papers, Vol. IV",
            category = "Philosophy",
            tags = listOf("Philosophy", "History", "Leadership"),
            onlinePopularityScore = 95,
            popularOnlineContext = "Frequently cited in philosophical essays, memoirs, and strategic retrospectives.",
            era = "1843"
        ),
        QuoteItem(
            id = "q_da_vinci_simplicity",
            text = "Learning never exhausts the mind.",
            author = "Leonardo da Vinci",
            sourceWork = "Codex Atlanticus",
            category = "Art",
            tags = listOf("Art", "Science", "Philosophy"),
            onlinePopularityScore = 92,
            popularOnlineContext = "Shared across lifelong learning platforms, museum exhibitions, and polymath circles.",
            era = "Renaissance"
        ),
        QuoteItem(
            id = "q_oliver_wild_precious",
            text = "Tell me, what is it you plan to do with your one wild and precious life?",
            author = "Mary Oliver",
            sourceWork = "The Summer Day (House of Light)",
            category = "Poetry",
            tags = listOf("Poetry", "Mindfulness", "Resilience"),
            onlinePopularityScore = 97,
            popularOnlineContext = "One of the most quoted lines of contemporary poetry in commencement speeches and journals.",
            era = "1990"
        ),
        QuoteItem(
            id = "q_churchill_history",
            text = "The farther backward you can look, the farther forward you are likely to see.",
            author = "Winston S. Churchill",
            sourceWork = "Address to the Royal College of Physicians",
            category = "History",
            tags = listOf("History", "Leadership", "Philosophy"),
            onlinePopularityScore = 89,
            popularOnlineContext = "Referenced in historical analysis, macro-strategy, and civic leadership forums.",
            era = "1944"
        )
    )
}
