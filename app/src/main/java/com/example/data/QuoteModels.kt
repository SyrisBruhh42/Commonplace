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

// ============================================================================
// LFS Oasis Aegis (U1/U2/U3/U4), Tailscale Handshake, 32-State ABAC Lattice,
// Total Lexical Order LWW & 4-Wing Quest Trilogy Codex Models
// ============================================================================

enum class LfsServerPreset(
    val wireId: String,
    val label: String,
    val baseUrl: String,
    val handshakeUrl: String,
    val wsUrl: String,
    val description: String
) {
    TAILSCALE_ANCHOR_PROD(
        wireId = "TAILSCALE_ANCHOR_PROD",
        label = "Tailscale Anchor Prod (lfs-prod.tail420c4d.ts.net)",
        baseUrl = "https://lfs-prod.tail420c4d.ts.net",
        handshakeUrl = "http://100.106.12.104:13379/lfs-sso/handshake",
        wsUrl = "wss://lfs-prod.tail420c4d.ts.net/ws",
        description = "Anchor 100.106.12.104:13379 • Tailnet TNoZe9MmWS11CNTRL"
    ),
    TAILSCALE_LAPUTA_U3(
        wireId = "TAILSCALE_LAPUTA_U3",
        label = "Tailscale Laputa U3 (:19448)",
        baseUrl = "http://100.115.219.120:19448",
        handshakeUrl = "http://100.115.219.120:19448/lfs-sso/handshake",
        wsUrl = "ws://100.115.219.120:19448/ws",
        description = "Laputa U3 Hoster over Tailscale CGNAT 100.64.0.0/10"
    ),
    LFS_CLOUD_U2(
        wireId = "LFS_CLOUD_U2",
        label = "LFS Cloud U2 (play.lewisfamilysystems.com)",
        baseUrl = "https://play.lewisfamilysystems.com",
        handshakeUrl = "https://play.lewisfamilysystems.com/lfs-sso/handshake",
        wsUrl = "wss://sync.lewisfamilysystems.com:48295",
        description = "U2 Sync Edge (:3000) & wss://sync.lewisfamilysystems.com:48295"
    ),
    LOCAL_HOSTER_19448(
        wireId = "LOCAL_HOSTER_19448",
        label = "Local Hoster (:19448)",
        baseUrl = "http://127.0.0.1:19448",
        handshakeUrl = "http://127.0.0.1:19448/lfs-sso/handshake",
        wsUrl = "ws://127.0.0.1:19448/ws",
        description = "Local U3 Hoster loopback (127.0.0.1:19448 / 10.0.2.2:19448)"
    )
}

enum class LfsPersona(
    val wireId: String,
    val label: String,
    val defaultTrustRing: Int
) {
    OWNER("OWNER", "Oasis Owner (Ring 4)", 4),
    ADULT_PEER("ADULT_PEER", "Trusted Adult Peer (Ring 3)", 3),
    CHILD_PROFILE("CHILD_PROFILE", "Protected Youth Profile (Ring 2)", 2),
    GUEST_PASS("GUEST_PASS", "Guest Salon Pass (Ring 1)", 1);

    companion object {
        fun fromWire(raw: String?): LfsPersona {
            val clean = raw?.trim()?.uppercase().orEmpty()
            return entries.find { it.wireId == clean } ?: OWNER
        }
    }
}

/**
 * Machado CVD-safe status pillar check (`[PASS]` Sapphire `#0072B2` / `[WARN]` Vermilion `#D55E00`).
 */
data class HandshakePillarCheck(
    val key: String,
    val label: String,
    val passed: Boolean,
    val detail: String
) {
    val badgeText: String
        get() = if (passed) "[PASS]" else "[WARN]"
}

data class TailscaleHandshakeResult(
    val isLiveResponse: Boolean = false,
    val preset: LfsServerPreset = LfsServerPreset.TAILSCALE_ANCHOR_PROD,
    val tailnetId: String = "TNoZe9MmWS11CNTRL",
    val tailnetDomain: String = "tail420c4d.ts.net",
    val anchorHost: String = "100.106.12.104:13379",
    val stableNodeId: String = "nStable_commonplace_u1_7f4a",
    val subuserId: String = "sub_archivist_01",
    val persona: LfsPersona = LfsPersona.OWNER,
    val trustRing: Int = 4,
    val generationEpoch: Long = 1042L,
    val tailscaleTunnelPass: Boolean = true,
    val abacPerimeterPass: Boolean = true,
    val isolatedProfilePass: Boolean = true,
    val userDataBackupPass: Boolean = true,
    val abacEvaluation: Abac32EvaluationResult = Abac32EvaluationResult(),
    val statusSummary: String = "Synthesized local Aegis handshake (<=1600ms non-blocking fallback)",
    val probedAtEpochMs: Long = System.currentTimeMillis()
) {
    val pillars: List<HandshakePillarCheck>
        get() = listOf(
            HandshakePillarCheck(
                key = "tailscale_tunnel",
                label = "tailscale_tunnel",
                passed = tailscaleTunnelPass,
                detail = if (tailscaleTunnelPass) "CGNAT 100.64.0.0/10 • $tailnetDomain" else "Local Loopback Standby"
            ),
            HandshakePillarCheck(
                key = "abac_perimeter",
                label = "abac_perimeter",
                passed = abacPerimeterPass,
                detail = "32-State Lattice ${abacEvaluation.stateBits} • Ring $trustRing (${persona.wireId})"
            ),
            HandshakePillarCheck(
                key = "isolated_profile",
                label = "isolated_profile",
                passed = isolatedProfilePass,
                detail = "Subuser $subuserId • Node $stableNodeId"
            ),
            HandshakePillarCheck(
                key = "user_data_backup",
                label = "user_data_backup",
                passed = userDataBackupPass,
                detail = "6-Gate aegisEnvelopeV1 • Epoch $generationEpoch"
            )
        )
}

/**
 * 32-State `FIRST_APPLICABLE_DENY` ABAC Lattice evaluation result.
 * `permit = !explicitDeny && (trustRing >= minRing) && (vipBypass || (curfewOk && stepUpOk))`
 * `stateBits` encodes the 5 boolean inputs:
 *   bit 0: `explicitDeny` ('1' or '0')
 *   bit 1: `trustRing >= minRing` ('1' or '0')
 *   bit 2: `vipBypass` ('1' or '0')
 *   bit 3: `curfewOk` ('1' or '0')
 *   bit 4: `stepUpOk` ('1' or '0')
 * Example: `"01110"` when `explicitDeny=false`, `ringOk=true`, `vipBypass=true`, `curfewOk=true`, `stepUpOk=false`.
 */
data class Abac32EvaluationResult(
    val permit: Boolean = true,
    val stateBits: String = "01111",
    val explicitDeny: Boolean = false,
    val trustRing: Int = 4,
    val minRing: Int = 2,
    val vipBypass: Boolean = true,
    val curfewOk: Boolean = true,
    val stepUpOk: Boolean = true
)

/**
 * Total Lexical Order `(ts, replicaId, value)` Last-Writer-Wins cell.
 * Uses `stableNodeId` (`Node.StableID`) as deterministic `replicaId` tie-breaker.
 */
data class TotalOrderLwwCell<T>(
    val ts: Long,
    val replicaId: String,
    val value: T
)

data class CrdtStateDeltaOp(
    val opId: String = "op_${java.util.UUID.randomUUID().toString().take(8)}",
    val fieldPath: String,
    val ts: Long = System.currentTimeMillis(),
    val replicaId: String,
    val valueJson: String
)

enum class PhilosophicalDoctrine(
    val wireId: String,
    val label: String,
    val guildBuffTitle: String,
    val guildBuffSummary: String
) {
    AGGRESSION(
        wireId = "aggression",
        label = "Aggression (Vanguard Stoic Might)",
        guildBuffTitle = "+18% Raid Strike & Vanguard Momentum",
        guildBuffSummary = "Channels Stoic resolve and historical courage into decisive frontline guild strikes."
    ),
    ARCANE(
        wireId = "arcane",
        label = "Arcane (Epistemic & Astral Lore)",
        guildBuffTitle = "+22% Scriptorium Mana & Bestiary Insight",
        guildBuffSummary = "Distills philosophy, science, and poetry into high-potency arcane synthesis."
    ),
    STEALTH(
        wireId = "stealth",
        label = "Stealth (Veiled Shadow & Wit)",
        guildBuffTitle = "+20% Undercity Evasion & Critical Ambush",
        guildBuffSummary = "Weaves literary subtlety, existential poise, and paradoxical wit into silent infiltration."
    );

    companion object {
        fun fromWireId(raw: String?): PhilosophicalDoctrine {
            val clean = raw?.trim()?.lowercase().orEmpty()
            return entries.find { it.wireId == clean } ?: ARCANE
        }

        /**
         * Automatically maps a Commonplace Quote category to a canonical Philosophical Doctrine.
         */
        fun fromQuoteCategory(category: String): PhilosophicalDoctrine {
            return when (category.trim().lowercase()) {
                "stoicism", "history", "leadership", "courage", "action" -> AGGRESSION
                "philosophy", "science", "poetry", "cosmos", "metaphysics" -> ARCANE
                "literature", "wisdom", "mindfulness", "art", "existentialism" -> STEALTH
                else -> ARCANE
            }
        }
    }
}

enum class TrilogyWing(
    val wingNumber: String,
    val slug: String,
    val title: String,
    val subtitle: String,
    val badge: String
) {
    WING_I_COMMONPLACE(
        wingNumber = "Wing I",
        slug = "commonplace-codex",
        title = "Commonplace Book (Wing I Hub)",
        subtitle = "Literary Quotes, Epigraphs, Zero-Trust World Map, Family Salon & Symposium",
        badge = "Master Style Authority"
    ),
    WING_II_SCRIPTORIUM_PART1(
        wingNumber = "Wing II",
        slug = "lfs-quest-part-1",
        title = "LFS-Quest Part 1 — Scriptorium & PowerBlocks",
        subtitle = "Keep Chronicles, Forum Board, PowerBlocks Pipeline & Task Queue",
        badge = "Part 1 • Scriptorium"
    ),
    WING_III_CHAPTERHOUSE_PART2(
        wingNumber = "Wing III",
        slug = "lfs-quest-part-2",
        title = "LFS-Quest Part 2 — Guild Chapterhouse & Intel Lab",
        subtitle = "Mission Board, Co-Op Raid Boss, Gemini Intel Lab & SaveSheet Ledger",
        badge = "Part 2 • Chapterhouse"
    ),
    WING_IV_OBSIDIAN_DESK_PART3(
        wingNumber = "Wing IV",
        slug = "lfs-quest-part-3",
        title = "LFS-Quest Part 3 — Obsidian Vigil 3D Guild Desk",
        subtitle = "First-Person 3D Mahogany Desk, Adventurer Roster, Realm Map & Bestiary Index",
        badge = "Part 3 • 3D Guild Desk"
    )
}

/**
 * Emitted whenever a user saves/stamps a Quote or completes a Salon Trivia round in Commonplace.
 * Propagates to `/api/lfs/sync/state` and local trilogy storage so Parts 1, 2, and 3 immediately
 * receive it as an active Quest Contract, Forum Prompt, and Guild Doctrine buff.
 */
data class CommonplaceQuestDirectiveV1(
    val directiveId: String = "dir_${java.util.UUID.randomUUID().toString().replace("-", "").take(10)}",
    val quoteId: String = "",
    val quoteText: String = "",
    val author: String = "",
    val category: String = "Philosophy",
    val philosophicalDoctrine: String = PhilosophicalDoctrine.ARCANE.wireId,
    val sourceEvent: String = "QUOTE_STAMP",
    val goldReward: Int = 45,
    val rupeeReward: Int = 20,
    val reputationReward: Int = 15,
    val bestiarySeed: String = "Chronicle Specter of Alexandria",
    val forumPromptTitle: String = "",
    val scriptoriumTaskLabel: String = "",
    val raidMissionBrief: String = "",
    val createdAtEpochMs: Long = System.currentTimeMillis(),
    val replicaId: String = "nStable_commonplace_u1_7f4a"
)

data class RupeeGemDenomination(
    val value: Int,
    val name: String,
    val cmykToneLabel: String,
    val hexColor: Long,
    val count: Int
)

data class TrashArcadeRelic(
    val slug: String,
    val title: String,
    val originWing: String,
    val effectDescription: String,
    val isUnlocked: Boolean = true
)

data class AegisSaveSlotSummary(
    val slotKey: String,
    val label: String,
    val isOccupied: Boolean = false,
    val updatedAtEpochMs: Long = 0L,
    val activeDoctrine: String = "arcane",
    val guildGold: Int = 0,
    val totalRupees: Int = 0,
    val directiveCount: Int = 0,
    val sealedEnvelopePreview: String = "",
    val replicatedToAnchor: Boolean = true
)

/**
 * Master state envelope tracking the unified Commonplace & LFS-Quest Trilogy across all 4 wings,
 * Tailscale Zero-Trust Handshake, TrashArcade Rupee Satchel, and 20-Slot Aegis Save Ledger.
 */
data class CommonplaceTrilogySyncEnvelopeV1(
    val schemaVersion: String = "commonplace.trilogy.sync.v1",
    val capsuleTarget: String = "lfs-trash-arcade",
    val cartridgeSlug: String = "commonplace-codex",
    val activeWing: TrilogyWing = TrilogyWing.WING_I_COMMONPLACE,
    val activeRoomCode: String = "OASIS-7K9M",
    val serverPreset: LfsServerPreset = LfsServerPreset.TAILSCALE_ANCHOR_PROD,
    val handshake: TailscaleHandshakeResult = TailscaleHandshakeResult(),
    val manualDoctrineOverride: PhilosophicalDoctrine? = null,
    val effectiveDoctrine: PhilosophicalDoctrine = PhilosophicalDoctrine.ARCANE,
    val guildGold: Int = 340,
    val guildReputation: Int = 125,
    val totalRupees: Int = 486,
    val rupeeDenominations: List<RupeeGemDenomination> = defaultRupeeSatchel(486),
    val unlockedRelics: List<TrashArcadeRelic> = defaultCrossGameRelics(),
    val pendingDirectives: List<CommonplaceQuestDirectiveV1> = defaultSeedDirectives(),
    val wing2ChronicleQueue: List<String> = listOf(
        "Keep Chronicle #14: Seneca's Epistemic Vigil in the North Scriptorium",
        "PowerBlocks Pipeline: Compile Stoic Marginalia into Guild Edict IV",
        "Forum Board Thread: 'Does imagination amplify the Raid Boss aura?'"
    ),
    val wing3RaidBossName: String = "Arch-oleth the Uncatalogued (HP 74%)",
    val wing3RaidHpPercent: Int = 74,
    val wing3ActiveMissions: List<String> = listOf(
        "Chapterhouse Contract: Deliver 3 Stamped Folios to the Intel Lab",
        "Co-Op Raid Strike: Channel Arcane Doctrine against Arch-oleth"
    ),
    val wing4DeskArtifacts: List<String> = listOf(
        "Mahogany Desk Ledger • Candlelit Quill & Brass Astrolabe",
        "Adventurer Roster: Sir Viren (Arcane Scholar), Lyra (Shadow Delver), Kael (Stoic Vanguard)",
        "Bestiary Index: Chronicle Specter, Ink-Wraith of Bloomsbury, Gilded Automaton"
    ),
    val saveSlots: List<AegisSaveSlotSummary> = defaultTwentySaveSlots(),
    val selectedSaveSlotKey: String = "slot_01",
    val lastRmblynCommandResult: String = "Rmblyn Overlay Ready • Channel #commonplace-codex",
    val lastSyncStatusBanner: String = "Wing I Master Portal Active • 6-Gate Aegis Envelope Synced (slot_01)"
) {
    companion object {
        val CANONICAL_RUPEE_VALUES = listOf(1, 5, 10, 20, 50, 100, 200, 500, 1000)

        fun defaultRupeeSatchel(totalAmount: Int = 486): List<RupeeGemDenomination> {
            return listOf(
                RupeeGemDenomination(1, "Cyan Shard (1)", "Cyan Tint", 0xFF00BCD4, 6),
                RupeeGemDenomination(5, "Azure Prism (5)", "Deep Cyan", 0xFF0288D1, 4),
                RupeeGemDenomination(10, "Emerald Cut (10)", "Cyan-Yellow", 0xFF2E7D32, 4),
                RupeeGemDenomination(20, "Amber Topaz (20)", "Warm Yellow", 0xFFF9A825, 3),
                RupeeGemDenomination(50, "Magenta Ruby (50)", "Pure Magenta", 0xFFC2185B, 3),
                RupeeGemDenomination(100, "Violet Amethyst (100)", "Cyan-Magenta", 0xFF6A1B9A, 2),
                RupeeGemDenomination(200, "Crimson Sunstone (200)", "Magenta-Yellow", 0xFFD84315, 0),
                RupeeGemDenomination(500, "Obsidian Star (500)", "CMY Key Black", 0xFF37474F, 0),
                RupeeGemDenomination(1000, "Sovereign Aegis Gem (1000)", "Gilded Prismatic", 0xFFD4AF37, maxOf(0, totalAmount / 1000))
            )
        }

        fun defaultCrossGameRelics(): List<TrashArcadeRelic> = listOf(
            TrashArcadeRelic(
                slug = "porch-chronicler-quill",
                title = "Porch Chronicler's Quill",
                originWing = "Wing I • Commonplace Book",
                effectDescription = "Inscribes provenance timestamps into Guild Quest Directives (+15% Gold on Save)",
                isUnlocked = true
            ),
            TrashArcadeRelic(
                slug = "viren-epistemic-bookmark",
                title = "Viren's Epistemic Bookmark",
                originWing = "Wing II • Scriptorium Part 1",
                effectDescription = "Unlocks instant Total Order LWW annotation synthesis across PowerBlocks",
                isUnlocked = true
            ),
            TrashArcadeRelic(
                slug = "undercity-delver-torch",
                title = "Undercity Delver's Torch",
                originWing = "Wing III • Chapterhouse Part 2",
                effectDescription = "Illuminates hidden Co-Op Raid weak points during overnight Z_1440 curfews",
                isUnlocked = true
            ),
            TrashArcadeRelic(
                slug = "willow-visor-emerald",
                title = "Willow's Visor Emerald",
                originWing = "Wing IV • Obsidian Vigil Part 3",
                effectDescription = "Reveals Bestiary lore seeds on the 3D Mahogany Guild Desk",
                isUnlocked = true
            )
        )

        fun defaultSeedDirectives(): List<CommonplaceQuestDirectiveV1> = listOf(
            CommonplaceQuestDirectiveV1(
                directiveId = "dir_seneca_stoa_01",
                quoteId = "q_seneca_imagination",
                quoteText = "We suffer more often in imagination than in reality.",
                author = "Seneca the Younger",
                category = "Stoicism",
                philosophicalDoctrine = PhilosophicalDoctrine.AGGRESSION.wireId,
                sourceEvent = "QUOTE_STAMP",
                goldReward = 50,
                rupeeReward = 25,
                reputationReward = 15,
                bestiarySeed = "Phantasm of Unfounded Dread",
                forumPromptTitle = "Stoic Vanguard: Overcoming Imaginary Dread Before the Raid",
                scriptoriumTaskLabel = "Transcribe Letter XIII into Keep Chronicle #15",
                raidMissionBrief = "Vanguard Strike: Dispel illusionary debuffs on the Chapterhouse frontline"
            ),
            CommonplaceQuestDirectiveV1(
                directiveId = "dir_borges_labyrinth_02",
                quoteId = "q_borges_paradise",
                quoteText = "I have always imagined that Paradise will be a kind of library.",
                author = "Jorge Luis Borges",
                category = "Literature",
                philosophicalDoctrine = PhilosophicalDoctrine.STEALTH.wireId,
                sourceEvent = "SALON_TRIVIA_VICTORY",
                goldReward = 65,
                rupeeReward = 30,
                reputationReward = 20,
                bestiarySeed = "Hexagonal Archivist Automaton",
                forumPromptTitle = "Labyrinthine Stacks: Mapping the Infinite Scriptorium",
                scriptoriumTaskLabel = "Index Hexagonal Gallery Folios in PowerBlocks",
                raidMissionBrief = "Stealth Recon: Navigate the Library Vaults undetected"
            )
        )

        fun defaultTwentySaveSlots(): List<AegisSaveSlotSummary> {
            return (1..20).map { idx ->
                val slotKey = String.format(java.util.Locale.US, "slot_%02d", idx)
                if (idx == 1) {
                    AegisSaveSlotSummary(
                        slotKey = slotKey,
                        label = "Slot 01 • Primary Auto-Sync Ledger",
                        isOccupied = true,
                        updatedAtEpochMs = System.currentTimeMillis(),
                        activeDoctrine = PhilosophicalDoctrine.ARCANE.wireId,
                        guildGold = 340,
                        totalRupees = 486,
                        directiveCount = 2,
                        sealedEnvelopePreview = "AEGIS1:slot_01:6GATE_VERIFIED",
                        replicatedToAnchor = true
                    )
                } else {
                    AegisSaveSlotSummary(
                        slotKey = slotKey,
                        label = String.format(java.util.Locale.US, "Slot %02d • Archival Vault", idx),
                        isOccupied = false
                    )
                }
            }
        }
    }
}

/**
 * Native bridge equivalent of `window.ArcadeAssetBridge` for TrashArcade & LFS-Quest cross-game
 * Rupee wallet and relic synchronization.
 */
object NativeArcadeAssetBridge {
    const val CAPSULE_TARGET = "lfs-trash-arcade"
    const val CARTRIDGE_SLUG = "commonplace-codex"

    fun computeBestiarySeedForQuote(quote: QuoteItem, doctrine: PhilosophicalDoctrine): String {
        val prefix = when (doctrine) {
            PhilosophicalDoctrine.AGGRESSION -> "Ironclad Vanguard"
            PhilosophicalDoctrine.ARCANE -> "Astral Lexicon"
            PhilosophicalDoctrine.STEALTH -> "Veiled Whisper"
        }
        val authorSurname = quote.author.split(" ").lastOrNull()?.ifBlank { "Sage" } ?: "Sage"
        return "$prefix of $authorSurname (${quote.category})"
    }

    fun buildDirectiveFromQuote(
        quote: QuoteItem,
        sourceEvent: String,
        manualDoctrineOverride: PhilosophicalDoctrine?,
        replicaId: String
    ): CommonplaceQuestDirectiveV1 {
        val doctrine = manualDoctrineOverride ?: PhilosophicalDoctrine.fromQuoteCategory(quote.category)
        val isTrivia = sourceEvent.contains("TRIVIA", ignoreCase = true)
        val gold = if (isTrivia) 65 else 45
        val rupees = if (isTrivia) 30 else 20
        val rep = if (isTrivia) 20 else 15
        val bestiary = computeBestiarySeedForQuote(quote, doctrine)
        val snippet = quote.text.take(56).trim()
        return CommonplaceQuestDirectiveV1(
            directiveId = "dir_${java.util.UUID.randomUUID().toString().replace("-", "").take(10)}",
            quoteId = quote.id,
            quoteText = quote.text,
            author = quote.author,
            category = quote.category,
            philosophicalDoctrine = doctrine.wireId,
            sourceEvent = sourceEvent,
            goldReward = gold,
            rupeeReward = rupees,
            reputationReward = rep,
            bestiarySeed = bestiary,
            forumPromptTitle = "[${doctrine.wireId.uppercase()}] ${quote.author}: “$snippet…”",
            scriptoriumTaskLabel = "PowerBlocks: Codify ${quote.author}'s ${quote.category} maxim",
            raidMissionBrief = "${doctrine.guildBuffTitle} • Deploy $bestiary contract",
            createdAtEpochMs = System.currentTimeMillis(),
            replicaId = replicaId
        )
    }
}



