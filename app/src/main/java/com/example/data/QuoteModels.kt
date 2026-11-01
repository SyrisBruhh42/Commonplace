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

// ============================================================================
// Hosted LFS Server & Multiplayer Literary Salon / Trivia Duel Models
// ============================================================================

enum class CommonplaceTransportMode(val label: String, val subtitle: String) {
    LFS_HOSTED_SERVER(
        "LFS Dedicated Server (U1/U2)",
        "24/7 Hosted Salon & Trivia Room via play.lewisfamilysystems.com (LFS-XXXXXX)"
    ),
    LFS_LOCAL_HOST_TUNNEL(
        "Host on Device + LFS Tunnel",
        "Local authoritative room bridged globally via sync.lewisfamilysystems.com"
    ),
    FIRESTORE_CLOUD_RELAY(
        "Cloud Salon Relay (Firestore)",
        "Zero-config 6-char room code relay for instant online multiplayer"
    ),
    LAN_DIRECT_SOCKET(
        "Local Network Socket (:48899)",
        "Zero-config household LAN UDP discovery + TCP socket"
    )
}

enum class LfsHostMode(val label: String, val description: String) {
    LAUNCH_U2_HOSTING_SERVICE(
        "Launch on LFS Hosting Service (U2)",
        "Dedicated 24/7 cloud salon room on play.lewisfamilysystems.com"
    ),
    HOST_LOCAL_DEVICE(
        "Host on This Device",
        "Authoritative room hosted locally and bridged via LFS Tunnel or LAN :48899"
    )
}

enum class LfsLifecyclePolicy(val wireId: String, val label: String) {
    ON_DEMAND_WAKE("ON_DEMAND_WAKE", "Wake-on-Join Standby (<500ms, 0 MB Idle)"),
    AUTO_CLOSE_IDLE("AUTO_CLOSE_IDLE", "Auto-Close When Empty (5m Grace + Auto-Save)"),
    KEEP_LIVE_PINNED("KEEP_LIVE_PINNED", "Keep-Live Pinned (24/7 Persistent Salon)"),
    LEASE_TTL("LEASE_TTL", "Timed Lease TTL (2h Scheduled Literary Session)")
}

data class LfsServerEndpointConfig(
    val primaryEdgeHost: String = "https://play.lewisfamilysystems.com",
    val syncRelayWsUrl: String = "wss://sync.lewisfamilysystems.com/ws",
    val sanctuaryBooksHost: String = "https://books.lewisfamilysystems.com",
    val localHosterUrl: String = "http://127.0.0.1:19448",
    val customServerUrl: String = ""
) {
    fun resolvedHttpBaseUrl(): String {
        val candidate = customServerUrl.trim().ifBlank { primaryEdgeHost }
        return candidate.trimEnd('/')
    }

    fun resolvedWebSocketUrl(roomCode: String, isHostTunnel: Boolean, wsTicket: String = ""): String {
        val cleanCode = roomCode.uppercase().removePrefix("LFS-").let { "LFS-$it" }
        val baseWs = when {
            customServerUrl.trim().startsWith("ws://") || customServerUrl.trim().startsWith("wss://") ->
                customServerUrl.trim().trimEnd('/')
            customServerUrl.trim().startsWith("http://") ->
                "ws://" + customServerUrl.trim().removePrefix("http://").trimEnd('/')
            customServerUrl.trim().startsWith("https://") ->
                "wss://" + customServerUrl.trim().removePrefix("https://").trimEnd('/')
            else -> syncRelayWsUrl.removeSuffix("/ws").trimEnd('/')
        }
        val path = if (isHostTunnel) "/ws/host-tunnel/$cleanCode" else "/ws/room/$cleanCode"
        val query = if (wsTicket.isNotBlank()) "?ticket=$wsTicket" else ""
        return "$baseWs$path$query"
    }
}

enum class MultiplayerGameMode(val label: String, val subtitle: String) {
    LITERARY_TRIVIA_DUEL(
        "Literary Trivia Duel",
        "Multi-round quote attribution, missing keyword & historical era challenge with streak multipliers"
    ),
    CO_READING_SALON(
        "Co-Reading Salon",
        "Synchronized passage folio with live shared marginalia and highlight annotations"
    ),
    SYMPOSIUM_ROUNDTABLE(
        "Symposium Roundtable",
        "Live philosophical roundtable with human readers and resident AI Scholars (@archivist, @willow, @mote)"
    )
}

data class SalonParticipant(
    val id: String = "",
    val displayName: String = "",
    val email: String = "",
    val role: String = "Reader",
    val score: Int = 0,
    val streak: Int = 0,
    val selectedAnswerIndex: Int? = null,
    val marginaliaDraft: String = "",
    val isAiScholar: Boolean = false,
    val scholarHandle: String = "",
    val isHost: Boolean = false,
    val transportBadge: String = "LFS",
    val lastActiveEpochMs: Long = System.currentTimeMillis()
) {
    val peerId: String
        get() = id
    val scholarPersona: String?
        get() = scholarHandle.ifBlank { null }
}

data class TriviaRoundState(
    val roundNumber: Int = 1,
    val totalRounds: Int = 5,
    val challengeType: String = "Author Attribution",
    val promptQuoteText: String = "",
    val options: List<String> = emptyList(),
    val correctOptionIndex: Int = 0,
    val selectedOptionIndex: Int? = null,
    val isRevealed: Boolean = false,
    val explanationNote: String = "",
    val scholarHint: String = "",
    val sourceQuoteId: String = "",
    val sourceWorkHint: String = "",
    val eraHint: String = "",
    val roundStartedAtMs: Long = System.currentTimeMillis(),
    val winnerNames: List<String> = emptyList()
) {
    val quoteId: String
        get() = sourceQuoteId
    val quoteText: String
        get() = promptQuoteText
    val promptQuestion: String
        get() = challengeType
}

data class LiveMarginaliaEntry(
    val id: String = "",
    val roomCode: String = "",
    val authorUid: String = "",
    val authorName: String = "",
    val quoteId: String = "",
    val quoteSnippet: String = "",
    val marginNote: String = "",
    val highlightPhrase: String = "",
    val scholarHandle: String = "",
    val createdAtEpochMs: Long = System.currentTimeMillis()
)

data class PublicRoomListing(
    val roomCode: String = "",
    val title: String = "",
    val hostName: String = "",
    val gameMode: MultiplayerGameMode = MultiplayerGameMode.CO_READING_SALON,
    val lifecyclePolicy: LfsLifecyclePolicy = LfsLifecyclePolicy.ON_DEMAND_WAKE,
    val participantCount: Int = 1,
    val maxParticipants: Int = 12,
    val activeQuoteAuthor: String = "Seneca the Younger",
    val transportTierLabel: String = "Tier A • play.lewisfamilysystems.com",
    val isWakeOnJoinStandby: Boolean = true
)

data class LfsRoomHandle(
    val roomCode: String = "",
    val title: String = "",
    val hostMode: LfsHostMode = LfsHostMode.LAUNCH_U2_HOSTING_SERVICE,
    val lifecyclePolicy: LfsLifecyclePolicy = LfsLifecyclePolicy.ON_DEMAND_WAKE,
    val activeTransport: CommonplaceTransportMode = CommonplaceTransportMode.LFS_HOSTED_SERVER,
    val wsEndpoint: String = "",
    val httpControlUrl: String = "",
    val wsTicket: String = "",
    val activeTierDescription: String = "Tier A • Hosted LFS Server (play.lewisfamilysystems.com)",
    val lanSocketPort: Int = 48899
)

data class TailscaleConnectionStatus(
    val isDetected: Boolean = false,
    val tailnetIp: String = "",
    val interfaceName: String = "",
    val vpnProfileName: String = "",
    val isVpnTransportActive: Boolean = false,
    val magicDnsActive: Boolean = false
) {
    val vpnTransportActive: Boolean
        get() = isVpnTransportActive
}

data class MultiplayerSalonSessionState(
    val isConnected: Boolean = false,
    val isConnecting: Boolean = false,
    val isTailscaleDetected: Boolean = false,
    val tailscaleStatus: TailscaleConnectionStatus = TailscaleConnectionStatus(),
    val roomCode: String = "",
    val circleId: String = "",
    val roomTitle: String = "The Archival Co-Reading Salon",
    val hostMode: LfsHostMode = LfsHostMode.HOST_LOCAL_DEVICE,
    val lifecyclePolicy: LfsLifecyclePolicy = LfsLifecyclePolicy.ON_DEMAND_WAKE,
    val activeTransportMode: CommonplaceTransportMode = CommonplaceTransportMode.FIRESTORE_CLOUD_RELAY,
    val activeGameMode: MultiplayerGameMode = MultiplayerGameMode.CO_READING_SALON,
    val endpointConfig: LfsServerEndpointConfig = LfsServerEndpointConfig(),
    val activeTierBadge: String = "Cloud Salon Relay Ready",
    val participants: List<SalonParticipant> = emptyList(),
    val activeAiScholars: List<String> = listOf("@archivist", "@willow", "@mote"),
    val currentPassageQuote: QuoteItem = SeedQuoteCatalog.allQuotes.first(),
    val marginaliaFeed: List<LiveMarginaliaEntry> = emptyList(),
    val triviaRound: TriviaRoundState? = null,
    val publicRoomsDirectory: List<PublicRoomListing> = emptyList(),
    val lastSealedEnvelope: String = "",
    val lastUnsealedPlaintext: String = "",
    val connectionDiagnostic: String = "Ready to launch or join a Literary Salon room",
    val isHost: Boolean = false,
    val hostEpoch: Long = 1L
) {
    val transportMode: CommonplaceTransportMode
        get() = activeTransportMode
    val gameMode: MultiplayerGameMode
        get() = activeGameMode
    val activeQuote: QuoteItem
        get() = currentPassageQuote
    val liveMarginaliaFeed: List<LiveMarginaliaEntry>
        get() = marginaliaFeed
    val serverStatusBanner: String
        get() = connectionDiagnostic
}

@IgnoreExtraProperties
data class MultiplayerSalonDocument(
    @DocumentId val id: String = "",
    val roomCode: String = "",
    val title: String = "",
    val hostUid: String = "",
    val hostName: String = "",
    val gameMode: String = MultiplayerGameMode.CO_READING_SALON.name,
    val lifecyclePolicy: String = LfsLifecyclePolicy.ON_DEMAND_WAKE.wireId,
    val currentQuoteId: String = "",
    val currentQuoteText: String = "",
    val currentQuoteAuthor: String = "",
    val participantUids: List<String> = emptyList(),
    val participantNames: List<String> = emptyList(),
    val activeScholars: List<String> = listOf("@archivist", "@willow", "@mote"),
    val roundNumber: Long = 1L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class SalonMessageDocument(
    @DocumentId val id: String = "",
    val roomCode: String = "",
    val senderUid: String = "",
    val senderName: String = "",
    val messageType: String = "MARGINALIA",
    val text: String = "",
    val quoteSnippet: String = "",
    val scholarHandle: String = "",
    val triviaDeltaScore: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null
)

// ============================================================================
// Quote Music Scene (lyria-3-clip-preview), Read Aloud & Voice Transcription
// ============================================================================

data class QuoteAudioSceneState(
    val activeQuoteId: String = "",
    val activeQuoteAuthor: String = "",
    val isGeneratingMusic: Boolean = false,
    val isPlayingMusic: Boolean = false,
    val isReadingAloud: Boolean = false,
    val musicModelId: String = "lyria-3-clip-preview",
    val ttsModelId: String = "gemini-3.1-flash-tts-preview",
    val clipDurationSeconds: Int = 30,
    val elapsedSeconds: Int = 0,
    val musicSceneTitle: String = "",
    val musicSceneDescription: String = "",
    val instrumentationTags: List<String> = emptyList(),
    val tempoBpm: Int = 72,
    val keySignature: String = "D Dorian",
    val statusCaption: String = "",
    val errorMessage: String? = null
)

enum class VoiceTranscriptionTarget {
    SYMPOSIUM_CHAT,
    MOMENT_QUOTE_PROMPT,
    SALON_MARGINALIA,
    LIBRARY_SEARCH,
    PERSONAL_JOURNAL_NOTE
}

data class VoiceTranscriptionState(
    val isRecording: Boolean = false,
    val isTranscribing: Boolean = false,
    val activeTarget: VoiceTranscriptionTarget? = null,
    val modelName: String = "gemini-3.5-transcribe",
    val lastTranscribedText: String = "",
    val recordingDurationSeconds: Int = 0,
    val statusMessage: String? = null,
    val errorMessage: String? = null
)

// ============================================================================
// High Contrast, Accessibility & Leading User QoL Settings
// ============================================================================

data class AccessibilitySettings(
    val highContrastMode: Boolean = false,
    val preferDarkHighContrast: Boolean = false,
    val fontScaleMultiplier: Float = 1.0f,
    val enlargedTouchTargets: Boolean = false,
    val dyslexiaFriendlySpacing: Boolean = false,
    val reducedMotion: Boolean = false,
    val autoReadAloudOnSelect: Boolean = false,
    val showAudioVisualCaptions: Boolean = true
) {
    val highContrastEnabled: Boolean
        get() = highContrastMode
    val highContrastDarkPreferred: Boolean
        get() = preferDarkHighContrast
    val minimumTouchTargetDp: Int
        get() = if (enlargedTouchTargets || highContrastMode) 56 else 48
}

// ============================================================================
// Interactive Quote Passage Highlighting, Time-Stamped Annotations & PDF Folio
// ============================================================================

enum class HighlightInkColor(
    val wireId: String,
    val label: String,
    val argbHex: Long
) {
    AMBER_GOLD("AMBER_GOLD", "Amber Gold Ink", 0xFFE6B44CL),
    SAGE_VERDIGRIS("SAGE_VERDIGRIS", "Sage Verdigris", 0xFF5B9A72L),
    OXBLOOD_CARMINE("OXBLOOD_CARMINE", "Oxblood Carmine", 0xFFC85A5AL),
    COBALT_ULTRAMARINE("COBALT_ULTRAMARINE", "Cobalt Ultramarine", 0xFF4F7CACL);

    companion object {
        fun fromWireId(id: String): HighlightInkColor {
            return entries.firstOrNull { it.wireId.equals(id, ignoreCase = true) } ?: AMBER_GOLD
        }
    }
}

@IgnoreExtraProperties
data class QuoteHighlightAnnotation(
    @DocumentId val id: String = "",
    val quoteId: String = "",
    val userId: String = "",
    val highlightedText: String = "",
    val startCharIndex: Long = 0L,
    val endCharIndex: Long = 0L,
    val inkColor: String = HighlightInkColor.AMBER_GOLD.wireId,
    val noteText: String = "",
    val timestampLabel: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
) {
    @get:Exclude
    val resolvedInkColor: HighlightInkColor
        get() = HighlightInkColor.fromWireId(inkColor)

    @get:Exclude
    val startIdx: Int
        get() = startCharIndex.toInt()

    @get:Exclude
    val endIdx: Int
        get() = endCharIndex.toInt()
}

data class QuoteWordToken(
    val wordIndex: Int,
    val word: String,
    val startCharIndex: Int,
    val endCharIndex: Int
)

object QuotePassageTokenizer {
    private val wordRegex = Regex("\\S+")

    fun tokenizeWords(quoteText: String): List<QuoteWordToken> {
        return wordRegex.findAll(quoteText).mapIndexed { index, matchResult ->
            QuoteWordToken(
                wordIndex = index,
                word = matchResult.value,
                startCharIndex = matchResult.range.first,
                endCharIndex = matchResult.range.last + 1
            )
        }.toList()
    }

    fun resolveCharRange(
        quoteText: String,
        tokens: List<QuoteWordToken>,
        firstWordIndex: Int,
        secondWordIndex: Int
    ): Triple<Int, Int, String> {
        if (tokens.isEmpty() || quoteText.isEmpty()) return Triple(0, 0, "")
        val minIdx = minOf(firstWordIndex, secondWordIndex).coerceIn(0, tokens.lastIndex)
        val maxIdx = maxOf(firstWordIndex, secondWordIndex).coerceIn(0, tokens.lastIndex)
        val startChar = tokens[minIdx].startCharIndex.coerceIn(0, quoteText.length)
        val endChar = tokens[maxIdx].endCharIndex.coerceIn(startChar, quoteText.length)
        val snippet = quoteText.substring(startChar, endChar)
        return Triple(startChar, endChar, snippet)
    }
}

enum class PdfFolioTheme(
    val wireId: String,
    val label: String,
    val subtitle: String
) {
    ARCHIVAL_PARCHMENT(
        wireId = "ARCHIVAL_PARCHMENT",
        label = "Warm Archival Parchment Folio",
        subtitle = "Aged vellum cream background, double letterpress borders & warm bronze accents"
    ),
    MODERN_MONOGRAPH(
        wireId = "MODERN_MONOGRAPH",
        label = "Clean Modern Monograph",
        subtitle = "High-contrast crisp editorial layout with architectural rules & swiss-grid framing"
    )
}

data class JournalPdfExportResult(
    val filePath: String,
    val fileName: String,
    val pdfBytes: ByteArray,
    val folioTheme: PdfFolioTheme,
    val annotationCount: Int,
    val soundscapeTitle: String
)

// ============================================================================
// Home Screen Daily Quote Widget & Scheduled 'Quote of the Day' Notification
// ============================================================================

data class DailyQuoteScheduleState(
    val notificationsEnabled: Boolean = false,
    val scheduledHour: Int = 8,
    val scheduledMinute: Int = 30,
    val usePriorityEngineForDailyPush: Boolean = true,
    val lastSyncedQuoteId: String = "",
    val lastSyncedQuoteText: String = "",
    val lastSyncedQuoteAuthor: String = "",
    val lastSyncedQuoteSource: String = "",
    val lastSyncedArchivalStamp: String = "",
    val nextScheduledLabel: String = "Daily at 8:30 AM",
    val statusMessage: String? = null
) {
    val formattedTimeLabel: String
        get() = DailyQuoteRetrievalEngine.formatHourMinute(scheduledHour, scheduledMinute)
}

/**
 * Shared quote retrieval engine used by CommonplaceViewModel, CommonplaceDailyQuoteWidgetProvider,
 * and DailyQuoteNotificationReceiver so scheduled notifications and Home Screen widgets use the
 * exact same quote retrieval and priority ranking logic.
 */
object DailyQuoteRetrievalEngine {

    fun retrieveDeterministicDailyQuote(
        dayOfYear: Int,
        catalog: List<QuoteItem> = SeedQuoteCatalog.allQuotes
    ): QuoteItem {
        if (catalog.isEmpty()) {
            return SeedQuoteCatalog.allQuotes.first()
        }
        val safeIndex = ((dayOfYear % catalog.size) + catalog.size) % catalog.size
        return catalog[safeIndex]
    }

    fun rankQuotesByPriority(
        catalog: List<QuoteItem> = SeedQuoteCatalog.allQuotes,
        selectedInterests: List<String> = listOf("Stoicism", "Literature", "Philosophy"),
        priorityMode: PriorityMode = PriorityMode.BALANCED,
        recentQuoteIds: List<String> = emptyList(),
        advanceToNext: Boolean = false
    ): List<PriorityScoredQuote> {
        val interests = selectedInterests.toSet()
        return catalog.map { quote ->
            val matchesPrimary = quote.category in interests
            val matchingTagsCount = quote.tags.count { it in interests }
            val rawInterestScore = when {
                matchesPrimary && matchingTagsCount > 1 -> 100
                matchesPrimary -> 88
                matchingTagsCount > 0 -> 70
                else -> 30
            }

            val rawPopularity = quote.onlinePopularityScore.coerceIn(1, 100)
            val recentIndex = recentQuoteIds.indexOf(quote.id)
            val isRecent = recentIndex != -1

            val rawRecencyScore = when {
                recentIndex == 0 && advanceToNext -> 10
                recentIndex in 1..4 -> 85
                !isRecent -> 95
                else -> 60
            }

            val (interestW, popW, recencyW) = when (priorityMode) {
                PriorityMode.BALANCED -> Triple(0.45, 0.35, 0.20)
                PriorityMode.PERSONAL_INTERESTS -> Triple(0.65, 0.20, 0.15)
                PriorityMode.POPULAR_ONLINE -> Triple(0.20, 0.65, 0.15)
                PriorityMode.RECENT_TRENDING -> Triple(0.25, 0.25, 0.50)
            }

            val interestPts = (rawInterestScore * interestW).toInt()
            val popPts = (rawPopularity * popW).toInt()
            val recencyPts = (rawRecencyScore * recencyW).toInt()
            val total = (interestPts + popPts + recencyPts).coerceIn(1, 100)

            val reason = buildString {
                if (matchesPrimary) append("Matches ${quote.category} • ")
                append("${quote.onlinePopularityScore}% Online Resonance")
                if (isRecent) append(" • Recent Thread") else append(" • Fresh Priority")
            }

            PriorityScoredQuote(
                quote = quote,
                totalPriorityScore = total,
                interestMatchPoints = interestPts,
                popularityPoints = popPts,
                recencyFactorPoints = recencyPts,
                isRecentlyUsed = isRecent,
                priorityReason = reason
            )
        }.sortedByDescending { it.totalPriorityScore }
    }

    /**
     * Selects the Quote of the Day for a given dayOfYear using either the Priority Engine ranking
     * rotated by dayOfYear or the deterministic daily archival catalog selector.
     */
    fun resolveQuoteOfTheDay(
        dayOfYear: Int,
        usePriorityRanking: Boolean = true,
        selectedInterests: List<String> = listOf("Stoicism", "Literature", "Philosophy"),
        priorityMode: PriorityMode = PriorityMode.BALANCED,
        recentQuoteIds: List<String> = emptyList(),
        syncedDailyOverride: QuoteItem? = null
    ): QuoteItem {
        if (syncedDailyOverride != null && !usePriorityRanking) {
            return syncedDailyOverride
        }
        if (usePriorityRanking) {
            val ranked = rankQuotesByPriority(
                catalog = SeedQuoteCatalog.allQuotes,
                selectedInterests = selectedInterests,
                priorityMode = priorityMode,
                recentQuoteIds = recentQuoteIds
            )
            if (ranked.isNotEmpty()) {
                val topTier = ranked.take(5)
                val idx = ((dayOfYear % topTier.size) + topTier.size) % topTier.size
                return topTier[idx].quote
            }
        }
        return syncedDailyOverride ?: retrieveDeterministicDailyQuote(dayOfYear)
    }

    fun formatHourMinute(hourOfDay: Int, minute: Int): String {
        val h = hourOfDay.coerceIn(0, 23)
        val m = minute.coerceIn(0, 59)
        val amPm = if (h < 12) "AM" else "PM"
        val displayHour = when {
            h == 0 -> 12
            h > 12 -> h - 12
            else -> h
        }
        return String.format(java.util.Locale.US, "%d:%02d %s", displayHour, m, amPm)
    }

    fun computeNextTriggerEpochMillis(
        hourOfDay: Int,
        minute: Int,
        nowMillis: Long = System.currentTimeMillis()
    ): Long {
        val cal = java.util.Calendar.getInstance().apply {
            timeInMillis = nowMillis
            set(java.util.Calendar.HOUR_OF_DAY, hourOfDay.coerceIn(0, 23))
            set(java.util.Calendar.MINUTE, minute.coerceIn(0, 59))
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        if (cal.timeInMillis <= nowMillis) {
            cal.add(java.util.Calendar.DAY_OF_YEAR, 1)
        }
        return cal.timeInMillis
    }
}

// ============================================================================
// Zero-Trust Multi-User Location-Based Quote Map Models
// ============================================================================

/**
 * Public zero-trust map pin document stored in `/map_quote_pins/{pinId}`.
 * Document ID is `{ownerUid}_{dayKey}` to enforce once-a-day pinning in Firestore rules.
 * Contains ZERO user PII (no displayName, no email, no personal notes).
 */
@IgnoreExtraProperties
data class MapQuotePinDocument(
    @DocumentId val id: String = "",
    val ownerUid: String = "",
    val dayKey: String = "",
    val quoteId: String = "",
    val quoteText: String = "",
    val quoteAuthor: String = "",
    val sourceWork: String = "",
    val category: String = "",
    val latitude: Double = 37.77,
    val longitude: Double = -122.42,
    val regionLabel: String = "San Francisco Bay, CA",
    val anonymousAlias: String = "Anonymous Reader #7C4F",
    @ServerTimestamp val createdAt: Timestamp? = null
)

/**
 * Zero-trust private identity & marginalia document stored in `/map_pin_identities/{pinId}`.
 * Protected by Firestore security rules so ONLY the owner or connected/friended users
 * (`connectedUserUids` / `connectedUserEmails`) can read it.
 */
@IgnoreExtraProperties
data class MapPinIdentityDocument(
    @DocumentId val id: String = "",
    val pinId: String = "",
    val ownerUid: String = "",
    val ownerDisplayName: String = "",
    val ownerEmail: String = "",
    val circleNames: List<String> = emptyList(),
    val personalMarginalia: String = "",
    val exactLocationLabel: String = "",
    val connectedUserUids: List<String> = emptyList(),
    val connectedUserEmails: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

enum class FriendLocationPrecision(
    val wireCode: String,
    val label: String,
    val shortBadge: String,
    val description: String
) {
    EXACT_GPS(
        wireCode = "EXACT_GPS",
        label = "Exact GPS & Venue",
        shortBadge = "Exact GPS",
        description = "Reveals exact GPS coordinates and full venue/study provenance stamp"
    ),
    NEIGHBORHOOD(
        wireCode = "NEIGHBORHOOD",
        label = "Coarse Neighborhood (~2.5 km)",
        shortBadge = "Neighborhood",
        description = "Reveals your name & marginalia, but rounds location to neighborhood grid"
    )
}

data class FriendPrivacyPermission(
    val friendId: String,
    val displayName: String,
    val email: String,
    val uid: String = "",
    val circleName: String = "Reading Circle",
    val isSharingEnabled: Boolean = true,
    val precision: FriendLocationPrecision = FriendLocationPrecision.EXACT_GPS
)

/**
 * Combined UI representation of a map quote pin.
 * `verifiedIdentity` is non-null ONLY when the viewer is the pin owner or is connected/friended
 * with the pin owner via Zero-Trust Family/Friend Circles.
 */
data class ZeroTrustMapPinUiItem(
    val pin: MapQuotePinDocument,
    val verifiedIdentity: MapPinIdentityDocument? = null,
    val isCurrentUserOwner: Boolean = false,
    val isConnectedFriend: Boolean = false,
    val isHiddenFromPublicMap: Boolean = false,
    val viewerPrecision: FriendLocationPrecision = FriendLocationPrecision.EXACT_GPS,
    val exactLatitude: Double = pin.latitude,
    val exactLongitude: Double = pin.longitude
) {
    val effectiveLatitude: Double
        get() = when {
            isCurrentUserOwner -> exactLatitude
            isConnectedFriend && viewerPrecision == FriendLocationPrecision.EXACT_GPS -> exactLatitude
            else -> pin.latitude
        }

    val effectiveLongitude: Double
        get() = when {
            isCurrentUserOwner -> exactLongitude
            isConnectedFriend && viewerPrecision == FriendLocationPrecision.EXACT_GPS -> exactLongitude
            else -> pin.longitude
        }

    val displayAttributionLabel: String
        get() = when {
            isCurrentUserOwner && verifiedIdentity != null ->
                "You (${verifiedIdentity.ownerDisplayName}) • ${if (isHiddenFromPublicMap) "Friends-Only Pin" else "Public + Circle Pin"}"
            isConnectedFriend && verifiedIdentity != null ->
                "${verifiedIdentity.ownerDisplayName} (${verifiedIdentity.ownerEmail})"
            else ->
                "${pin.anonymousAlias} • Zero-Trust Anonymous Reader"
        }

    val displayLocationLabel: String
        get() = when {
            isCurrentUserOwner && verifiedIdentity != null && verifiedIdentity.exactLocationLabel.isNotBlank() ->
                "${ZeroTrustMapCoordinatesHelper.extractCleanLocationLabel(verifiedIdentity.exactLocationLabel)} (Exact Owner View)"
            isConnectedFriend && verifiedIdentity != null -> {
                if (viewerPrecision == FriendLocationPrecision.EXACT_GPS && verifiedIdentity.exactLocationLabel.isNotBlank()) {
                    "${ZeroTrustMapCoordinatesHelper.extractCleanLocationLabel(verifiedIdentity.exactLocationLabel)} (Exact Friend Access)"
                } else {
                    "${pin.regionLabel} (Neighborhood Friend Precision ~2.5 km)"
                }
            }
            else ->
                "${pin.regionLabel} (Coarse Zero-Trust Grid)"
        }
}

enum class MapPinFilterMode(val label: String) {
    ALL_WORLD_PINS("All Public Pins"),
    CONNECTED_FRIENDS_ONLY("Trusted Connections Only"),
    MY_DAILY_PIN("My Daily Pin")
}

data class WorldMapUiState(
    val pins: List<ZeroTrustMapPinUiItem> = emptyList(),
    val selectedPin: ZeroTrustMapPinUiItem? = null,
    val filterMode: MapPinFilterMode = MapPinFilterMode.ALL_WORLD_PINS,
    val globalMapVisibilityEnabled: Boolean = true,
    val trustedConnectionsOnlyFilter: Boolean = false,
    val friendPermissions: List<FriendPrivacyPermission> = emptyList(),
    val isPrivacyOverlayVisible: Boolean = false,
    val preferGoogleMapsSdkView: Boolean = false,
    val isGoogleMapsApiKeyConfigured: Boolean = false,
    val hasPinnedToday: Boolean = false,
    val todayPinId: String = "",
    val todayDayKey: String = "",
    val userCurrentLat: Double = 37.7749,
    val userCurrentLon: Double = -122.4194,
    val userCurrentRegionLabel: String = "San Francisco Bay, CA",
    val isPinningInProgress: Boolean = false,
    val statusBannerMessage: String? = null
)

object ZeroTrustMapCoordinatesHelper {
    /**
     * Rounds exact GPS coordinates to ~1.1km-2.5km (0.01 degree) coarse grid for the public
     * `/map_quote_pins` document so exact home/desk coordinates are never exposed to strangers.
     */
    fun roundToCoarseGrid(coord: Double): Double {
        return kotlin.math.round(coord * 100.0) / 100.0
    }

    /**
     * Encodes exact coordinates and friend-only quote fallback metadata into `exactLocationLabel`
     * (max 300 chars) so friends can resolve exact vs coarse coordinates and see friend-only pins
     * even when `globalMapVisibilityEnabled == false` (when `/map_quote_pins/{pinId}` is omitted).
     */
    fun encodeIdentityLocationEnvelope(
        cleanLocationLabel: String,
        exactLat: Double,
        exactLon: Double,
        isHiddenFromPublic: Boolean,
        quoteId: String,
        quoteAuthor: String,
        quoteText: String
    ): String {
        val safeLabel = cleanLocationLabel.replace("|", "/").trim().ifBlank { "Literary Study" }.take(70)
        val latStr = String.format(java.util.Locale.US, "%.4f", exactLat.coerceIn(-90.0, 90.0))
        val lonStr = String.format(java.util.Locale.US, "%.4f", exactLon.coerceIn(-180.0, 180.0))
        val visCode = if (isHiddenFromPublic) "FO" else "PUB"
        val safeAuthor = quoteAuthor.replace("|", "/").trim().take(35)
        val safeQuote = quoteText.replace("|", "/").replace("\n", " ").trim().take(120)
        val safeId = quoteId.replace("|", "_").trim().take(24)
        return "$safeLabel||$latStr,$lonStr|$visCode|$safeId|$safeAuthor|$safeQuote".take(300)
    }

    fun extractCleanLocationLabel(rawEnvelope: String): String {
        val idx = rawEnvelope.indexOf("||")
        return if (idx >= 0) rawEnvelope.substring(0, idx).trim() else rawEnvelope.trim()
    }

    data class DecodedIdentityEnvelope(
        val cleanLocationLabel: String,
        val exactLat: Double?,
        val exactLon: Double?,
        val isHiddenFromPublic: Boolean,
        val quoteId: String,
        val quoteAuthor: String,
        val quoteText: String
    )

    fun decodeIdentityLocationEnvelope(rawEnvelope: String): DecodedIdentityEnvelope {
        val splitIdx = rawEnvelope.indexOf("||")
        if (splitIdx < 0) {
            return DecodedIdentityEnvelope(
                cleanLocationLabel = rawEnvelope.trim(),
                exactLat = null,
                exactLon = null,
                isHiddenFromPublic = false,
                quoteId = "q_friend_pin",
                quoteAuthor = "Literary Circle Scholar",
                quoteText = ""
            )
        }
        val cleanLabel = rawEnvelope.substring(0, splitIdx).trim()
        val metaPart = rawEnvelope.substring(splitIdx + 2)
        val parts = metaPart.split("|")
        val coords = parts.getOrNull(0)?.split(",")
        val lat = coords?.getOrNull(0)?.toDoubleOrNull()
        val lon = coords?.getOrNull(1)?.toDoubleOrNull()
        val isFriendsOnly = parts.getOrNull(1) == "FO"
        val qId = parts.getOrNull(2)?.ifBlank { "q_friend_pin" } ?: "q_friend_pin"
        val qAuthor = parts.getOrNull(3)?.ifBlank { "Literary Scholar" } ?: "Literary Scholar"
        val qText = parts.getOrNull(4) ?: ""
        return DecodedIdentityEnvelope(
            cleanLocationLabel = cleanLabel,
            exactLat = lat,
            exactLon = lon,
            isHiddenFromPublic = isFriendsOnly,
            quoteId = qId,
            quoteAuthor = qAuthor,
            quoteText = qText
        )
    }

    /**
     * Encodes per-friend location precision overrides inside `circleNames` entries (e.g.,
     * `PREC:email:NEIGHBORHOOD`) alongside human-readable circle names so permitted friends
     * automatically render either Exact GPS or Coarse Neighborhood coordinates.
     */
    fun buildCircleNamesWithPrecisionTags(
        circleNames: List<String>,
        friendPermissions: List<FriendPrivacyPermission>
    ): List<String> {
        val cleanCircles = circleNames
            .filter { !it.startsWith("PREC:") }
            .map { it.trim().take(100) }
            .filter { it.isNotEmpty() }
            .distinct()
            .take(10)
        val precisionTags = friendPermissions
            .filter { it.isSharingEnabled }
            .map { perm ->
                val key = perm.email.trim().lowercase().ifBlank { perm.friendId.trim().lowercase() }.take(55)
                "PREC:$key:${perm.precision.wireCode}".take(100)
            }
            .distinct()
            .take(10)
        return (cleanCircles + precisionTags).take(20)
    }

    fun extractDisplayCircleNames(rawCircleNames: List<String>): List<String> {
        return rawCircleNames.filter { !it.startsWith("PREC:") }
    }

    fun resolveViewerPrecision(
        rawCircleNames: List<String>,
        viewerEmail: String,
        viewerUid: String
    ): FriendLocationPrecision {
        val normEmail = viewerEmail.trim().lowercase()
        val normUid = viewerUid.trim().lowercase()
        for (entry in rawCircleNames) {
            if (entry.startsWith("PREC:")) {
                val parts = entry.split(":")
                val key = parts.getOrNull(1)?.trim()?.lowercase().orEmpty()
                val mode = parts.getOrNull(2)?.trim().orEmpty()
                if (key.isNotEmpty() && (key == normEmail || key == normUid)) {
                    return if (mode == FriendLocationPrecision.NEIGHBORHOOD.wireCode) {
                        FriendLocationPrecision.NEIGHBORHOOD
                    } else {
                        FriendLocationPrecision.EXACT_GPS
                    }
                }
            }
        }
        return FriendLocationPrecision.EXACT_GPS
    }

    /**
     * Generates a deterministic, non-reversible literary alias from a user UID and dayKey
     * without leaking any PII or raw UID characters.
     */
    fun buildAnonymousLiteraryAlias(uid: String, dayKey: String): String {
        val hash = ((uid + ":" + dayKey).hashCode().toLong() and 0xFFFFL)
        val hex = hash.toString(16).uppercase().padStart(4, '0')
        val archetypes = listOf(
            "Anonymous Stoic",
            "Wandering Archivist",
            "Silent Flâneur",
            "Midnight Scribe",
            "Veiled Peripatetic",
            "Solitary Scholar"
        )
        val archetype = archetypes[(hash % archetypes.size).toInt()]
        return "$archetype #$hex"
    }

    /**
     * Curated atmospheric seed pins from global literary landmarks so the world map is
     * immediately rich while blending seamlessly with live multi-user Firestore pins.
     */
    fun defaultGlobalAtlasSeedPins(todayDayKey: String): List<MapQuotePinDocument> = listOf(
        MapQuotePinDocument(
            id = "atlas_athens_$todayDayKey",
            ownerUid = "atlas_stoic_stoa",
            dayKey = todayDayKey,
            quoteId = "q_seneca_imagination",
            quoteText = "We suffer more often in imagination than in reality.",
            quoteAuthor = "Seneca the Younger",
            sourceWork = "Moral Letters to Lucilius, Letter XIII",
            category = "Stoicism",
            latitude = 37.97,
            longitude = 23.72,
            regionLabel = "Athens, Greece • Stoa Poikile",
            anonymousAlias = "Anonymous Stoic #3A9C"
        ),
        MapQuotePinDocument(
            id = "atlas_london_$todayDayKey",
            ownerUid = "atlas_bloomsbury_scribe",
            dayKey = todayDayKey,
            quoteId = "q_woolf_mental_freedom",
            quoteText = "Lock up your libraries if you like; but there is no gate, no lock, no bolt that you can set upon the freedom of my mind.",
            quoteAuthor = "Virginia Woolf",
            sourceWork = "A Room of One's Own",
            category = "Literature",
            latitude = 51.52,
            longitude = -0.13,
            regionLabel = "Bloomsbury, London, UK",
            anonymousAlias = "Wandering Archivist #8E1B"
        ),
        MapQuotePinDocument(
            id = "atlas_paris_$todayDayKey",
            ownerUid = "atlas_leftbank_scholar",
            dayKey = todayDayKey,
            quoteId = "q_camus_invincible_summer",
            quoteText = "In the midst of winter, I found there was, within me, an invincible summer.",
            quoteAuthor = "Albert Camus",
            sourceWork = "Return to Tipasa",
            category = "Philosophy",
            latitude = 48.85,
            longitude = 2.35,
            regionLabel = "Latin Quarter, Paris, France",
            anonymousAlias = "Silent Flâneur #5D04"
        ),
        MapQuotePinDocument(
            id = "atlas_kyoto_$todayDayKey",
            ownerUid = "atlas_philosophers_path",
            dayKey = todayDayKey,
            quoteId = "q_rilke_patient",
            quoteText = "Be patient toward all that is unsolved in your heart and try to love the questions themselves.",
            quoteAuthor = "Rainer Maria Rilke",
            sourceWork = "Letters to a Young Poet, Letter IV",
            category = "Poetry",
            latitude = 35.02,
            longitude = 135.79,
            regionLabel = "Philosopher's Path, Kyoto, Japan",
            anonymousAlias = "Midnight Scribe #9F22"
        ),
        MapQuotePinDocument(
            id = "atlas_newyork_$todayDayKey",
            ownerUid = "atlas_hudson_reader",
            dayKey = todayDayKey,
            quoteId = "q_aurelius_mind",
            quoteText = "You have power over your mind — not outside events. Realize this, and you will find strength.",
            quoteAuthor = "Marcus Aurelius",
            sourceWork = "Meditations, Book IV",
            category = "Stoicism",
            latitude = 40.75,
            longitude = -73.98,
            regionLabel = "Midtown Reading Room, New York, NY",
            anonymousAlias = "Solitary Scholar #1C77"
        )
    )
}


