package com.example.network

import com.example.data.CommonplaceTransportMode
import com.example.data.LfsHostMode
import com.example.data.LfsLifecyclePolicy
import com.example.data.LfsRoomHandle
import com.example.data.LfsServerEndpointConfig
import com.example.data.LiveMarginaliaEntry
import com.example.data.MultiplayerGameMode
import com.example.data.PublicRoomListing
import com.example.data.QuoteItem
import com.example.data.SalonParticipant
import com.example.data.SeedQuoteCatalog
import com.example.data.TailscaleConnectionStatus
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONArray
import org.json.JSONObject
import java.net.NetworkInterface
import java.security.SecureRandom
import java.util.Collections
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * OasisCommonplaceServerClient manages Multiplayer Salon & Trivia connections.
 *
 * SECRET TAILSCALE GATE:
 * The Hosted LFS Server integration (`play.lewisfamilysystems.com` / `sync.lewisfamilysystems.com` /
 * `books.lewisfamilysystems.com` / `:19448` and `LAUNCH_U2_HOSTING_SERVICE`) remains dormant and hidden
 * unless [detectTailscaleConnection] confirms an active Tailscale interface/address (`100.64.0.0/10`
 * CGNAT range or `fd7a:115c:a1e0::/48` Tailscale IPv6 ULA). Once a valid Tailscale connection is
 * detected, the LFS Server transport options are unlocked in the UI and wired into the transport cascade.
 */
class OasisCommonplaceServerClient(
    private val httpClient: OkHttpClient = defaultOkHttpClient(),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    companion object {
        const val CROCKFORD_ALPHABET = "0123456789ABCDEFGHJKMNPQRSTVWXYZ"
        const val GAME_ID = "lfs-commonplace"
        const val LAN_TCP_PORT = 48899
        const val LAN_UDP_DISCOVERY_PORT = 48898
        private const val TAILSCALE_IPV6_PREFIX = "fd7a:115c:a1e0:"

        private val secureRandom = SecureRandom()

        fun defaultOkHttpClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(2500, TimeUnit.MILLISECONDS)
            .readTimeout(3500, TimeUnit.MILLISECONDS)
            .writeTimeout(3500, TimeUnit.MILLISECONDS)
            .pingInterval(15, TimeUnit.SECONDS)
            .build()

        /**
         * Returns true if [ipAddress] belongs to the Tailscale IPv4 CGNAT subnet `100.64.0.0/10`
         * (`100.64.0.0` through `100.127.255.255`) or the Tailscale IPv6 ULA prefix `fd7a:115c:a1e0::/48`.
         */
        fun isTailscaleIpAddress(ipAddress: String): Boolean =
            NetworkHealthService.isTailscaleIpAddress(ipAddress)

        /**
         * Inspects local network interfaces (or [simulatedInterfaces] for unit testing) via
         * [NetworkHealthService] to detect an active Tailscale mesh VPN connection (`100.64.0.0/10`
         * or `fd7a:115c:a1e0::/48`) or recognized private VPN profile.
         */
        fun detectTailscaleConnection(
            simulatedInterfaces: List<Pair<String, List<String>>>? = null,
            simulatedVpnProfile: String? = null
        ): TailscaleConnectionStatus = NetworkHealthService.detectPrivateNetwork(
            simulatedInterfaces = simulatedInterfaces,
            simulatedVpnProfile = simulatedVpnProfile
        )

        /**
         * Returns the transport modes visible to the user.
         * LFS Server modes (`LFS_HOSTED_SERVER`, `LFS_LOCAL_HOST_TUNNEL`) are hidden unless
         * [isTailscaleConnected] is true.
         */
        fun availableTransportModes(isTailscaleConnected: Boolean): List<CommonplaceTransportMode> {
            return if (isTailscaleConnected) {
                CommonplaceTransportMode.entries
            } else {
                listOf(
                    CommonplaceTransportMode.FIRESTORE_CLOUD_RELAY,
                    CommonplaceTransportMode.LAN_DIRECT_SOCKET
                )
            }
        }

        /**
         * Generates a 6-character Crockford Base32 room code formatted as `LFS-XXXXXX`.
         */
        fun generateRoomCode(): String {
            val sb = StringBuilder(6)
            repeat(6) {
                val idx = secureRandom.nextInt(CROCKFORD_ALPHABET.length)
                sb.append(CROCKFORD_ALPHABET[idx])
            }
            return "LFS-$sb"
        }

        /**
         * Normalizes user-entered room codes into canonical `LFS-XXXXXX` Crockford Base32 format,
         * mapping ambiguous characters (`I`/`L` -> `1`, `O` -> `0`) and stripping non-alphanumeric separators.
         */
        fun normalizeRoomCode(rawInput: String): String {
            val upper = rawInput.trim().uppercase()
                .removePrefix("LFS-")
                .removePrefix("LFS")
                .replace("-", "")
                .replace(" ", "")
                .map { ch ->
                    when (ch) {
                        'I', 'L' -> '1'
                        'O' -> '0'
                        'U' -> 'V'
                        else -> ch
                    }
                }
                .filter { it in CROCKFORD_ALPHABET }
                .joinToString("")

            val sixChars = when {
                upper.length >= 6 -> upper.take(6)
                upper.isEmpty() -> "9K4M2P"
                else -> upper.padEnd(6, '0')
            }
            return "LFS-$sixChars"
        }

        fun isValidRoomCode(code: String): Boolean {
            if (!code.startsWith("LFS-") || code.length != 10) return false
            val suffix = code.removePrefix("LFS-")
            return suffix.all { it in CROCKFORD_ALPHABET }
        }
    }

    private var activeWebSocket: WebSocket? = null
    private val _incomingEvents = MutableSharedFlow<LiveMarginaliaEntry>(extraBufferCapacity = 32)
    val incomingEvents: SharedFlow<LiveMarginaliaEntry> = _incomingEvents.asSharedFlow()

    /**
     * Launches or joins a room.
     *
     * SECRET TAILSCALE WIRING:
     * - When [isTailscaleConnected] is `false`, the Hosted LFS Server control plane (`Tier A`) is
     *   completely bypassed and buried; rooms wire directly to `FIRESTORE_CLOUD_RELAY` (`Tier B`) or
     *   `LAN_DIRECT_SOCKET` (`Tier C`).
     * - When [isTailscaleConnected] is `true`, the Hosted LFS Server (`play.lewisfamilysystems.com` /
     *   `sync.lewisfamilysystems.com` / `:19448`) is wired in as `Tier A` with automatic failover to
     *   `Tier B` and `Tier C`.
     */
    suspend fun provisionOrJoinRoom(
        requestedRoomCode: String?,
        roomTitle: String,
        hostMode: LfsHostMode,
        lifecyclePolicy: LfsLifecyclePolicy,
        preferredTransport: CommonplaceTransportMode,
        gameMode: MultiplayerGameMode,
        endpointConfig: LfsServerEndpointConfig,
        userId: String,
        userDisplayName: String,
        isTailscaleConnected: Boolean = detectTailscaleConnection().isDetected
    ): LfsRoomHandle = withContext(ioDispatcher) {
        val canonicalCode = if (requestedRoomCode.isNullOrBlank()) {
            generateRoomCode()
        } else {
            normalizeRoomCode(requestedRoomCode)
        }

        // Gate LFS Server transports behind Tailscale detection
        val effectiveTransport = if (!isTailscaleConnected &&
            (preferredTransport == CommonplaceTransportMode.LFS_HOSTED_SERVER ||
                preferredTransport == CommonplaceTransportMode.LFS_LOCAL_HOST_TUNNEL)
        ) {
            CommonplaceTransportMode.FIRESTORE_CLOUD_RELAY
        } else {
            preferredTransport
        }

        val effectiveHostMode = if (!isTailscaleConnected) {
            LfsHostMode.HOST_LOCAL_DEVICE
        } else {
            hostMode
        }

        if (effectiveTransport == CommonplaceTransportMode.FIRESTORE_CLOUD_RELAY) {
            return@withContext LfsRoomHandle(
                roomCode = canonicalCode,
                title = roomTitle.ifBlank { "Commonplace Cloud Salon" },
                hostMode = effectiveHostMode,
                lifecyclePolicy = lifecyclePolicy,
                activeTransport = CommonplaceTransportMode.FIRESTORE_CLOUD_RELAY,
                wsEndpoint = "firestore://symposium_rooms/$canonicalCode",
                httpControlUrl = "firestore://symposium_rooms",
                wsTicket = "fs_${UUID.randomUUID().toString().take(8)}",
                activeTierDescription = "Cloud Salon Relay (Firestore /symposium_rooms/$canonicalCode)",
                lanSocketPort = LAN_TCP_PORT
            )
        }

        if (effectiveTransport == CommonplaceTransportMode.LAN_DIRECT_SOCKET) {
            return@withContext LfsRoomHandle(
                roomCode = canonicalCode,
                title = roomTitle.ifBlank { "Household LAN Salon" },
                hostMode = LfsHostMode.HOST_LOCAL_DEVICE,
                lifecyclePolicy = lifecyclePolicy,
                activeTransport = CommonplaceTransportMode.LAN_DIRECT_SOCKET,
                wsEndpoint = "tcp://0.0.0.0:$LAN_TCP_PORT/$canonicalCode",
                httpControlUrl = "tcp://127.0.0.1:$LAN_TCP_PORT",
                wsTicket = "lan_${UUID.randomUUID().toString().take(8)}",
                activeTierDescription = "Direct LAN Socket (:$LAN_TCP_PORT) + AI Scholars",
                lanSocketPort = LAN_TCP_PORT
            )
        }

        // Tailscale is verified: wire Tier A Hosted LFS Server Hoster Control Plane
        val tierAResult = tryProvisionOnLfsHoster(
            roomCode = canonicalCode,
            roomTitle = roomTitle,
            hostMode = effectiveHostMode,
            lifecyclePolicy = lifecyclePolicy,
            gameMode = gameMode,
            endpointConfig = endpointConfig,
            userId = userId,
            userDisplayName = userDisplayName
        )

        if (tierAResult != null) {
            connectWebSocket(tierAResult, endpointConfig)
            return@withContext tierAResult
        }

        val fallbackTransport = if (effectiveHostMode == LfsHostMode.HOST_LOCAL_DEVICE) {
            CommonplaceTransportMode.LFS_LOCAL_HOST_TUNNEL
        } else {
            CommonplaceTransportMode.FIRESTORE_CLOUD_RELAY
        }

        val fallbackTierLabel = when (effectiveHostMode) {
            LfsHostMode.LAUNCH_U2_HOSTING_SERVICE ->
                "Tailscale LFS Standby • ${lifecyclePolicy.label} (${endpointConfig.primaryEdgeHost.removePrefix("https://")} -> Firestore Relay)"
            LfsHostMode.HOST_LOCAL_DEVICE ->
                "Tailscale Host Tunnel • Device Host (:$LAN_TCP_PORT + sync.lewisfamilysystems.com)"
        }

        LfsRoomHandle(
            roomCode = canonicalCode,
            title = roomTitle.ifBlank { "The Archival Co-Reading Salon" },
            hostMode = effectiveHostMode,
            lifecyclePolicy = lifecyclePolicy,
            activeTransport = fallbackTransport,
            wsEndpoint = endpointConfig.resolvedWebSocketUrl(
                roomCode = canonicalCode,
                isHostTunnel = effectiveHostMode == LfsHostMode.HOST_LOCAL_DEVICE
            ),
            httpControlUrl = "${endpointConfig.resolvedHttpBaseUrl()}/api/lfs/sync/hoster",
            wsTicket = "oasis_${UUID.randomUUID().toString().replace("-", "").take(12)}",
            activeTierDescription = fallbackTierLabel,
            lanSocketPort = LAN_TCP_PORT
        )
    }

    private fun tryProvisionOnLfsHoster(
        roomCode: String,
        roomTitle: String,
        hostMode: LfsHostMode,
        lifecyclePolicy: LfsLifecyclePolicy,
        gameMode: MultiplayerGameMode,
        endpointConfig: LfsServerEndpointConfig,
        userId: String,
        userDisplayName: String
    ): LfsRoomHandle? {
        return try {
            val baseUrl = endpointConfig.resolvedHttpBaseUrl()
            val url = "$baseUrl/api/lfs/sync/hoster"
            val bodyJson = JSONObject().apply {
                put("gameId", GAME_ID)
                put("roomCode", roomCode)
                put("title", roomTitle)
                put("hostMode", hostMode.name)
                put("lifecyclePolicy", lifecyclePolicy.wireId)
                put("gameMode", gameMode.name)
                put("hostUid", userId)
                put("hostName", userDisplayName)
            }
            val request = Request.Builder()
                .url(url)
                .post(bodyJson.toString().toRequestBody("application/json".toMediaType()))
                .header("X-Oasis-Protocol", "oasis-v1")
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val payload = response.body?.string().orEmpty()
                val json = if (payload.isNotBlank()) JSONObject(payload) else JSONObject()
                val ticket = json.optString("wsTicket", "ticket_${UUID.randomUUID().toString().take(8)}")
                val isTunnel = hostMode == LfsHostMode.HOST_LOCAL_DEVICE
                val wsUrl = json.optString(
                    "wsEndpoint",
                    endpointConfig.resolvedWebSocketUrl(roomCode, isTunnel, ticket)
                )
                LfsRoomHandle(
                    roomCode = roomCode,
                    title = roomTitle,
                    hostMode = hostMode,
                    lifecyclePolicy = lifecyclePolicy,
                    activeTransport = if (isTunnel) {
                        CommonplaceTransportMode.LFS_LOCAL_HOST_TUNNEL
                    } else {
                        CommonplaceTransportMode.LFS_HOSTED_SERVER
                    },
                    wsEndpoint = wsUrl,
                    httpControlUrl = url,
                    wsTicket = ticket,
                    activeTierDescription = "Tier A (Tailscale Verified) • Live LFS Oasis Edge (${baseUrl.removePrefix("https://")})"
                )
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun connectWebSocket(handle: LfsRoomHandle, endpointConfig: LfsServerEndpointConfig) {
        try {
            activeWebSocket?.cancel()
            val wsUrl = handle.wsEndpoint.ifBlank {
                endpointConfig.resolvedWebSocketUrl(
                    roomCode = handle.roomCode,
                    isHostTunnel = handle.hostMode == LfsHostMode.HOST_LOCAL_DEVICE,
                    wsTicket = handle.wsTicket
                )
            }
            val protocolHeader = if (handle.wsTicket.isNotBlank()) {
                "oasis-v1, ticket.${handle.wsTicket}"
            } else {
                "oasis-v1"
            }
            val request = Request.Builder()
                .url(wsUrl)
                .header("Sec-WebSocket-Protocol", protocolHeader)
                .build()

            activeWebSocket = httpClient.newWebSocket(request, object : WebSocketListener() {
                override fun onMessage(webSocket: WebSocket, text: String) {
                    runCatching {
                        val obj = JSONObject(text)
                        val entry = LiveMarginaliaEntry(
                            id = obj.optString("id", UUID.randomUUID().toString()),
                            roomCode = obj.optString("roomCode", handle.roomCode),
                            authorUid = obj.optString("authorUid", "lfs_peer"),
                            authorName = obj.optString("authorName", "Salon Reader"),
                            quoteId = obj.optString("quoteId", ""),
                            quoteSnippet = obj.optString("quoteSnippet", ""),
                            marginNote = obj.optString("marginNote", text),
                            highlightPhrase = obj.optString("highlightPhrase", ""),
                            scholarHandle = obj.optString("scholarHandle", "")
                        )
                        _incomingEvents.tryEmit(entry)
                    }
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    // Failover is handled transparently by Tier B/C in ViewModel
                }
            })
        } catch (_: Exception) {
            // Safe non-fatal fallback
        }
    }

    /**
     * Queries public rooms directory.
     * When [isTailscaleConnected] is false, returns only standard Cloud Relay / LAN rooms without
     * exposing LFS Server hosts. When [isTailscaleConnected] is true, queries
     * `/api/lfs/sync/rooms?gameId=lfs-commonplace` and surfaces Wake-on-Join LFS Oasis rooms.
     */
    suspend fun fetchPublicRoomsDirectory(
        endpointConfig: LfsServerEndpointConfig,
        isTailscaleConnected: Boolean = detectTailscaleConnection().isDetected
    ): List<PublicRoomListing> = withContext(ioDispatcher) {
        if (!isTailscaleConnected) {
            return@withContext listOf(
                PublicRoomListing(
                    roomCode = "LFS-7K9M2P",
                    title = "Stoic Dawn Co-Reading Folio",
                    hostName = "Archival Cloud Relay (@archivist)",
                    gameMode = MultiplayerGameMode.CO_READING_SALON,
                    lifecyclePolicy = LfsLifecyclePolicy.ON_DEMAND_WAKE,
                    participantCount = 4,
                    maxParticipants = 12,
                    activeQuoteAuthor = "Seneca the Younger",
                    transportTierLabel = "Cloud Salon Relay",
                    isWakeOnJoinStandby = true
                ),
                PublicRoomListing(
                    roomCode = "LFS-3R8W4N",
                    title = "Alexandria Literary Trivia Duel",
                    hostName = "Mote's Trivia Table (@mote)",
                    gameMode = MultiplayerGameMode.LITERARY_TRIVIA_DUEL,
                    lifecyclePolicy = LfsLifecyclePolicy.ON_DEMAND_WAKE,
                    participantCount = 6,
                    maxParticipants = 16,
                    activeQuoteAuthor = "Jorge Luis Borges",
                    transportTierLabel = "Cloud Salon Relay",
                    isWakeOnJoinStandby = true
                ),
                PublicRoomListing(
                    roomCode = "LFS-5T2J8Q",
                    title = "Midnight Poetry & Cosmos Roundtable",
                    hostName = "Willow & Friends (@willow)",
                    gameMode = MultiplayerGameMode.SYMPOSIUM_ROUNDTABLE,
                    lifecyclePolicy = LfsLifecyclePolicy.KEEP_LIVE_PINNED,
                    participantCount = 5,
                    maxParticipants = 10,
                    activeQuoteAuthor = "Rainer Maria Rilke",
                    transportTierLabel = "Local LAN + Cloud Relay",
                    isWakeOnJoinStandby = false
                )
            )
        }

        val remoteRooms = mutableListOf<PublicRoomListing>()
        try {
            val url = "${endpointConfig.resolvedHttpBaseUrl()}/api/lfs/sync/rooms?gameId=$GAME_ID"
            val request = Request.Builder().url(url).get().build()
            httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string().orEmpty()
                    val root = JSONObject(body)
                    val arr: JSONArray = root.optJSONArray("rooms") ?: JSONArray()
                    for (i in 0 until arr.length()) {
                        val item = arr.getJSONObject(i)
                        remoteRooms.add(
                            PublicRoomListing(
                                roomCode = normalizeRoomCode(item.optString("roomCode", "LFS-8K2M9P")),
                                title = item.optString("title", "Oasis Literary Salon"),
                                hostName = item.optString("hostName", "Oasis Host"),
                                gameMode = MultiplayerGameMode.entries.find {
                                    it.name == item.optString("gameMode")
                                } ?: MultiplayerGameMode.CO_READING_SALON,
                                lifecyclePolicy = LfsLifecyclePolicy.entries.find {
                                    it.wireId == item.optString("lifecyclePolicy")
                                } ?: LfsLifecyclePolicy.ON_DEMAND_WAKE,
                                participantCount = item.optInt("participantCount", 3),
                                maxParticipants = item.optInt("maxParticipants", 12),
                                activeQuoteAuthor = item.optString("activeQuoteAuthor", "Marcus Aurelius"),
                                transportTierLabel = "Tier A (Tailscale) • play.lewisfamilysystems.com"
                            )
                        )
                    }
                }
            }
        } catch (_: Exception) {
            // Fall through to Tailscale-unlocked LFS directory listings
        }

        if (remoteRooms.isNotEmpty()) return@withContext remoteRooms

        listOf(
            PublicRoomListing(
                roomCode = "LFS-7K9M2P",
                title = "Stoic Dawn Co-Reading Folio",
                hostName = "Sanctuary Books U2 (@archivist)",
                gameMode = MultiplayerGameMode.CO_READING_SALON,
                lifecyclePolicy = LfsLifecyclePolicy.ON_DEMAND_WAKE,
                participantCount = 4,
                maxParticipants = 12,
                activeQuoteAuthor = "Seneca the Younger",
                transportTierLabel = "Wake-on-Join • books.lewisfamilysystems.com",
                isWakeOnJoinStandby = true
            ),
            PublicRoomListing(
                roomCode = "LFS-3R8W4N",
                title = "Alexandria Literary Trivia Duel",
                hostName = "Oasis U1 Master (@mote)",
                gameMode = MultiplayerGameMode.LITERARY_TRIVIA_DUEL,
                lifecyclePolicy = LfsLifecyclePolicy.ON_DEMAND_WAKE,
                participantCount = 6,
                maxParticipants = 16,
                activeQuoteAuthor = "Jorge Luis Borges",
                transportTierLabel = "Wake-on-Join • play.lewisfamilysystems.com",
                isWakeOnJoinStandby = true
            ),
            PublicRoomListing(
                roomCode = "LFS-5T2J8Q",
                title = "Midnight Poetry & Cosmos Roundtable",
                hostName = "Willow & Friends (@willow)",
                gameMode = MultiplayerGameMode.SYMPOSIUM_ROUNDTABLE,
                lifecyclePolicy = LfsLifecyclePolicy.KEEP_LIVE_PINNED,
                participantCount = 5,
                maxParticipants = 10,
                activeQuoteAuthor = "Rainer Maria Rilke",
                transportTierLabel = "Pinned 24/7 • sync.lewisfamilysystems.com",
                isWakeOnJoinStandby = false
            )
        )
    }

    fun sendLiveMarginaliaOverWire(entry: LiveMarginaliaEntry): Boolean {
        val ws = activeWebSocket ?: return false
        return try {
            val payload = JSONObject().apply {
                put("type", "MARGINALIA_EVENT")
                put("id", entry.id)
                put("roomCode", entry.roomCode)
                put("authorUid", entry.authorUid)
                put("authorName", entry.authorName)
                put("quoteId", entry.quoteId)
                put("quoteSnippet", entry.quoteSnippet)
                put("marginNote", entry.marginNote)
                put("highlightPhrase", entry.highlightPhrase)
                put("scholarHandle", entry.scholarHandle)
                put("createdAtEpochMs", entry.createdAtEpochMs)
            }
            ws.send(payload.toString())
        } catch (_: Exception) {
            false
        }
    }

    fun disconnect() {
        try {
            activeWebSocket?.close(1000, "Leaving Commonplace Salon")
        } catch (_: Exception) {
        } finally {
            activeWebSocket = null
        }
    }

    fun generateResidentScholarResponse(
        scholarHandle: String,
        quote: QuoteItem,
        userPromptOrNote: String
    ): LiveMarginaliaEntry {
        val normalizedHandle = when {
            scholarHandle.lowercase().contains("willow") -> "@willow"
            scholarHandle.lowercase().contains("mote") -> "@mote"
            else -> "@archivist"
        }
        val authorDisplay = when (normalizedHandle) {
            "@willow" -> "Willow • Poetic Philosopher (@willow)"
            "@mote" -> "Mote • Court Jester & Trivia Spark (@mote)"
            else -> "The Archivist • Bibliographic Scholar (@archivist)"
        }
        val responseBody = when (normalizedHandle) {
            "@willow" -> buildString {
                append("Notice how ${quote.author}'s cadence in “${quote.sourceWork}” turns ")
                append(quote.category.lowercase())
                append(" into a quiet lantern: ")
                if (userPromptOrNote.isNotBlank()) {
                    append("your reflection on “${userPromptOrNote.take(70)}” echoes the hidden stillness between the words.")
                } else {
                    append("each clause invites us to inhabit the question rather than rush toward certainty.")
                }
            }
            "@mote" -> buildString {
                append("Trivia & Wordplay Clue for ${quote.author} (${quote.era}): ")
                append("Look closely at the signature motif in “${quote.sourceWork}” — ")
                append("only a mind steeped in ${quote.tags.joinToString(" & ")} would phrase it with such razor-sharp brevity!")
            }
            else -> buildString {
                append("Archival Folio Note [${quote.era} • ${quote.sourceWork}]: ")
                append("${quote.author} composed this passage within the ${quote.category} tradition. ")
                append(quote.popularOnlineContext)
            }
        }

        return LiveMarginaliaEntry(
            id = "scholar_${UUID.randomUUID().toString().take(10)}",
            roomCode = "LFS-SCHOLAR",
            authorUid = "ai_${normalizedHandle.removePrefix("@")}",
            authorName = authorDisplay,
            quoteId = quote.id,
            quoteSnippet = quote.text.take(90),
            marginNote = responseBody,
            highlightPhrase = quote.text.split(" ").take(5).joinToString(" "),
            scholarHandle = normalizedHandle
        )
    }

    fun buildDefaultParticipants(
        currentUserId: String,
        currentUserName: String,
        activeScholars: List<String>,
        transportBadge: String
    ): List<SalonParticipant> {
        val list = mutableListOf(
            SalonParticipant(
                id = currentUserId,
                displayName = currentUserName.ifBlank { "Archival Reader" },
                role = "Salon Host",
                score = 120,
                streak = 1,
                isAiScholar = false,
                isHost = true,
                transportBadge = transportBadge
            )
        )
        if ("@archivist" in activeScholars) {
            list.add(
                SalonParticipant(
                    id = "scholar_archivist",
                    displayName = "The Archivist",
                    role = "Bibliographic Historian",
                    score = 110,
                    streak = 2,
                    isAiScholar = true,
                    scholarHandle = "@archivist",
                    transportBadge = "AI"
                )
            )
        }
        if ("@willow" in activeScholars) {
            list.add(
                SalonParticipant(
                    id = "scholar_willow",
                    displayName = "Willow",
                    role = "Poetic Philosopher",
                    score = 105,
                    streak = 1,
                    isAiScholar = true,
                    scholarHandle = "@willow",
                    transportBadge = "AI"
                )
            )
        }
        if ("@mote" in activeScholars) {
            list.add(
                SalonParticipant(
                    id = "scholar_mote",
                    displayName = "Mote",
                    role = "Trivia & Wordplay Spark",
                    score = 115,
                    streak = 3,
                    isAiScholar = true,
                    scholarHandle = "@mote",
                    transportBadge = "AI"
                )
            )
        }
        return list
    }

    fun generateTriviaRound(
        roundNumber: Int,
        totalRounds: Int = 5,
        catalog: List<QuoteItem> = SeedQuoteCatalog.allQuotes
    ): com.example.data.TriviaRoundState {
        val safeCatalog = catalog.ifEmpty { SeedQuoteCatalog.allQuotes }
        val targetIndex = (roundNumber - 1).mod(safeCatalog.size)
        val targetQuote = safeCatalog[targetIndex]

        val challengeMode = roundNumber.mod(3)
        return when (challengeMode) {
            1 -> {
                val distractors = safeCatalog
                    .map { it.author }
                    .filter { it != targetQuote.author }
                    .distinct()
                    .take(3)
                val allOptions = (listOf(targetQuote.author) + distractors).sorted()
                val correctIdx = allOptions.indexOf(targetQuote.author).coerceAtLeast(0)
                com.example.data.TriviaRoundState(
                    roundNumber = roundNumber,
                    totalRounds = totalRounds,
                    challengeType = "Author Attribution Duel",
                    promptQuoteText = "“${targetQuote.text}”",
                    options = allOptions,
                    correctOptionIndex = correctIdx,
                    explanationNote = "Written by ${targetQuote.author} in ${targetQuote.sourceWork} (${targetQuote.era}).",
                    scholarHint = "@mote whispers: Think of ${targetQuote.era} ${targetQuote.category} (${targetQuote.sourceWork.take(24)}…).",
                    sourceQuoteId = targetQuote.id
                )
            }
            2 -> {
                val correctLabel = "${targetQuote.sourceWork} (${targetQuote.era})"
                val distractors = safeCatalog
                    .filter { it.id != targetQuote.id }
                    .map { "${it.sourceWork} (${it.era})" }
                    .distinct()
                    .take(3)
                val allOptions = (listOf(correctLabel) + distractors).sorted()
                val correctIdx = allOptions.indexOf(correctLabel).coerceAtLeast(0)
                com.example.data.TriviaRoundState(
                    roundNumber = roundNumber,
                    totalRounds = totalRounds,
                    challengeType = "Source Folio & Era Provenance",
                    promptQuoteText = "Which work and era originated: “${targetQuote.text}”?",
                    options = allOptions,
                    correctOptionIndex = correctIdx,
                    explanationNote = "${targetQuote.author} penned this in ${targetQuote.sourceWork} (${targetQuote.era}).",
                    scholarHint = "@archivist notes: The author is ${targetQuote.author}, celebrated in ${targetQuote.category}.",
                    sourceQuoteId = targetQuote.id
                )
            }
            else -> {
                val words = targetQuote.text.split(" ")
                val keyWordCandidate = words.firstOrNull { it.length >= 6 && it.all(Char::isLetter) }
                    ?: words.last().trim('.', ',', '—', ';')
                val maskedQuote = targetQuote.text.replaceFirst(keyWordCandidate, "________")
                val distractorWords = listOf("eternity", "labyrinth", "fortitude", "silence", "horizon")
                    .filter { !it.equals(keyWordCandidate, ignoreCase = true) }
                    .take(3)
                val allOptions = (listOf(keyWordCandidate) + distractorWords).sorted()
                val correctIdx = allOptions.indexOf(keyWordCandidate).coerceAtLeast(0)
                com.example.data.TriviaRoundState(
                    roundNumber = roundNumber,
                    totalRounds = totalRounds,
                    challengeType = "Missing Manuscript Keyword",
                    promptQuoteText = "Restore the missing word in ${targetQuote.author}'s line:\n“$maskedQuote”",
                    options = allOptions,
                    correctOptionIndex = correctIdx,
                    explanationNote = "Full quote: “${targetQuote.text}” — ${targetQuote.author}",
                    scholarHint = "@willow muses: The missing word resonates with ${targetQuote.tags.joinToString(" & ")}.",
                    sourceQuoteId = targetQuote.id
                )
            }
        }
    }
}
