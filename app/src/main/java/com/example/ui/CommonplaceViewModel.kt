package com.example.ui

import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AccessibilitySettings
import com.example.data.CommonplaceQuestDirectiveV1
import com.example.data.CommonplaceTransportMode
import com.example.data.CommonplaceTrilogySyncEnvelopeV1
import com.example.data.CrdtStateDeltaOp
import com.example.data.DailyQuoteRetrievalEngine
import com.example.data.DailyQuoteScheduleState
import com.example.data.FamilyCircle
import com.example.data.FamilySharedQuote
import com.example.data.FriendLocationPrecision
import com.example.data.FriendPrivacyPermission
import com.example.data.GeminiQuoteAiService
import com.example.data.HighlightInkColor
import com.example.data.JournalPdfExportResult
import com.example.data.LfsHostMode
import com.example.data.LfsLifecyclePolicy
import com.example.data.LfsServerPreset
import com.example.data.LiveMarginaliaEntry
import com.example.data.LocationStampService
import com.example.data.MapPinFilterMode
import com.example.data.MapPinIdentityDocument
import com.example.data.MapQuotePinDocument
import com.example.data.MultiplayerGameMode
import com.example.data.MultiplayerSalonSessionState
import com.example.data.NativeArcadeAssetBridge
import com.example.data.PdfFolioTheme
import com.example.data.PhilosophicalDoctrine
import com.example.data.PhotoMatchedQuoteResult
import com.example.data.PriorityMode
import com.example.data.PriorityScoredQuote
import com.example.data.Quote
import com.example.data.QuoteAudioSceneState
import com.example.data.QuoteHighlightAnnotation
import com.example.data.QuoteItem
import com.example.data.QuoteRepository
import com.example.data.SavedQuoteDocument
import com.example.data.SeedQuoteCatalog
import com.example.data.DisneyImagineeringTranslator
import com.example.data.OasisHallDestination
import com.example.data.RmblynChannelMessage
import com.example.data.TotalOrderLwwCell
import com.example.data.TrilogyWing
import com.example.data.VoiceTranscriptionState
import com.example.data.VoiceTranscriptionTarget
import com.example.data.WorldMapUiState
import com.example.data.ZeroTrustMapCoordinatesHelper
import com.example.data.ZeroTrustMapPinUiItem
import com.example.network.CommonplaceCryptoEnvelope
import com.example.network.NetworkHealthService
import com.example.network.OasisCommonplaceServerClient
import com.example.network.QuoteAudioAndVoiceService
import com.example.util.JournalPagePdfExporter
import com.example.widget.DailyQuoteScheduler
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.UUID

private const val TAG = "CommonplaceVM"

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val isUser: Boolean,
    val text: String,
    val isStreaming: Boolean = false,
    val attachedQuote: QuoteItem? = null,
    val generatedMomentQuote: QuoteItem? = null,
    val fieldConnectionInsight: String? = null
)

data class DiscoverUiState(
    val allCatalogQuotes: List<QuoteItem> = SeedQuoteCatalog.allQuotes,
    val dailyQuoteFromCollection: QuoteItem = SeedQuoteCatalog.allQuotes.first(),
    val isDailyQuoteSyncedFromFirestore: Boolean = false,
    val selectedInterests: List<String> = listOf("Stoicism", "Literature", "Philosophy"),
    val priorityMode: PriorityMode = PriorityMode.BALANCED,
    val recentQuoteIds: List<String> = emptyList(),
    val currentScoredQuote: PriorityScoredQuote? = null,
    val rankedQuotes: List<PriorityScoredQuote> = emptyList(),
    val quickTakeawayState: UiState<String>? = null,
    val isDiscoveringFromAi: Boolean = false,
    val aiDiscoveryError: String? = null,
    val statusBannerMessage: String? = null,
    val lastPreviewStampLocation: String = "Resolving location...",
    val lastPreviewStampDate: String = "",
    val lastPreviewStampTime: String = ""
)

data class DossierUiState(
    val targetQuote: QuoteItem? = null,
    val deepMeaningState: UiState<String>? = null,
    val searchGroundedState: UiState<String>? = null,
    val mapsGroundedState: UiState<String>? = null
)

class CommonplaceViewModel(
    private val repository: QuoteRepository,
    private val aiService: GeminiQuoteAiService,
    private val locationStampService: LocationStampService,
    private val currentUserId: String,
    private val oasisServerClient: OasisCommonplaceServerClient = OasisCommonplaceServerClient(),
    private val networkHealthService: NetworkHealthService = NetworkHealthService(),
    private val audioVoiceService: QuoteAudioAndVoiceService = QuoteAudioAndVoiceService()
) : ViewModel() {

    // Two-tier Flow error handling for saved quotes in Firestore
    val savedQuotesState: StateFlow<UiState<List<SavedQuoteDocument>>> =
        repository.observeSavedQuotes(currentUserId)
            .map<List<SavedQuoteDocument>, UiState<List<SavedQuoteDocument>>> { UiState.Success(it) }
            .catch { error ->
                Log.w(TAG, "Error observing saved quotes", error)
                emit(UiState.Error(error.message ?: "Failed to load saved quotes"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000L),
                initialValue = UiState.Loading
            )

    // Two-tier Flow error handling for Zero-Trust Family Social Circles
    val familyCirclesState: StateFlow<UiState<List<FamilyCircle>>> =
        repository.observeFamilyCircles(currentUserId)
            .map<List<FamilyCircle>, UiState<List<FamilyCircle>>> { UiState.Success(it) }
            .catch { error ->
                Log.w(TAG, "Error observing family circles", error)
                emit(UiState.Error(error.message ?: "Failed to load Family Social Circles"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000L),
                initialValue = UiState.Loading
            )

    // Two-tier Flow error handling for Zero-Trust Family Shared Quotes
    val familySharedQuotesState: StateFlow<UiState<List<FamilySharedQuote>>> =
        repository.observeFamilySharedQuotes(currentUserId)
            .map<List<FamilySharedQuote>, UiState<List<FamilySharedQuote>>> { UiState.Success(it) }
            .catch { error ->
                Log.w(TAG, "Error observing family shared quotes", error)
                emit(UiState.Error(error.message ?: "Failed to load Family Shared Quotes"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000L),
                initialValue = UiState.Loading
            )

    private val _discoverState = MutableStateFlow(DiscoverUiState())
    val discoverState: StateFlow<DiscoverUiState> = _discoverState.asStateFlow()

    private val _dossierState = MutableStateFlow(DossierUiState())
    val dossierState: StateFlow<DossierUiState> = _dossierState.asStateFlow()

    private val _photoMatchState = MutableStateFlow<UiState<PhotoMatchedQuoteResult>?>(null)
    val photoMatchState: StateFlow<UiState<PhotoMatchedQuoteResult>?> = _photoMatchState.asStateFlow()

    private val _salonSessionState = MutableStateFlow(
        MultiplayerSalonSessionState(
            triviaRound = oasisServerClient.generateTriviaRound(1, 5, SeedQuoteCatalog.allQuotes)
        )
    )
    val salonSessionState: StateFlow<MultiplayerSalonSessionState> = _salonSessionState.asStateFlow()

    private val _quoteAudioSceneState = MutableStateFlow(QuoteAudioSceneState())
    val quoteAudioSceneState: StateFlow<QuoteAudioSceneState> = _quoteAudioSceneState.asStateFlow()

    private val _voiceTranscriptionState = MutableStateFlow(VoiceTranscriptionState())
    val voiceTranscriptionState: StateFlow<VoiceTranscriptionState> = _voiceTranscriptionState.asStateFlow()

    private val _accessibilitySettings = MutableStateFlow(AccessibilitySettings())
    val accessibilitySettings: StateFlow<AccessibilitySettings> = _accessibilitySettings.asStateFlow()

    private val _quoteAnnotationsByQuoteId = MutableStateFlow<Map<String, List<QuoteHighlightAnnotation>>>(emptyMap())
    val quoteAnnotationsByQuoteId: StateFlow<Map<String, List<QuoteHighlightAnnotation>>> =
        _quoteAnnotationsByQuoteId.asStateFlow()

    private val _pdfExportStatusMessage = MutableStateFlow<String?>(null)
    val pdfExportStatusMessage: StateFlow<String?> = _pdfExportStatusMessage.asStateFlow()

    private val _dailyQuoteScheduleState = MutableStateFlow(DailyQuoteScheduleState())
    val dailyQuoteScheduleState: StateFlow<DailyQuoteScheduleState> = _dailyQuoteScheduleState.asStateFlow()

    private val _worldMapState = MutableStateFlow(
        WorldMapUiState(
            todayDayKey = buildTodayDayKey(),
            friendPermissions = defaultInitialFriendPermissions()
        )
    )
    val worldMapState: StateFlow<WorldMapUiState> = _worldMapState.asStateFlow()

    private val _trilogyState = MutableStateFlow(CommonplaceTrilogySyncEnvelopeV1())
    val trilogyState: StateFlow<CommonplaceTrilogySyncEnvelopeV1> = _trilogyState.asStateFlow()

    private val latestPublicMapPins = MutableStateFlow<List<MapQuotePinDocument>>(emptyList())
    private val latestConnectedIdentities = MutableStateFlow<List<MapPinIdentityDocument>>(emptyList())
    private val additionalConnectedFriendEmails = MutableStateFlow<List<String>>(emptyList())

    private var salonRoomObserverJob: Job? = null
    private var salonMessagesObserverJob: Job? = null

    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(
        listOf(
            ChatMessage(
                isUser = false,
                text = "Welcome to the Commonplace Symposium. Ask me to unpack the meaning of any quote, compare philosophical traditions, summon @archivist, @willow, or @mote, or forge quotes in the moment."
            )
        )
    )
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val _chatError = MutableStateFlow<String?>(null)
    val chatError: StateFlow<String?> = _chatError.asStateFlow()

    init {
        recomputePriorityRanking(advanceToNext = false)
        refreshPreviewStamp()
        fetchDailyQuoteFromCollection()
        observeCloudPreferences()
        syncGoogleEmailCircleInvites()
        refreshTailscaleDetection()
        observeIncomingWireMarginalia()
        observeAnnotationsForSavedQuotes()
        observeZeroTrustWorldMap()
        probeTailscaleHandshakeAndSyncPresence(LfsServerPreset.TAILSCALE_ANCHOR_PROD)
    }

    // =========================================================================
    // Quote Audio Scene (lyria-3-clip-preview), Read Aloud & Voice Transcription
    // =========================================================================

    fun readQuoteAloud(quote: QuoteItem) {
        audioVoiceService.readQuoteAloud(
            scope = viewModelScope,
            quote = quote,
            onStateUpdate = { state -> _quoteAudioSceneState.value = state }
        )
    }

    fun generateAndPlay30sMusicScene(quote: QuoteItem) {
        audioVoiceService.generateAndPlay30sMusicScene(
            scope = viewModelScope,
            quote = quote,
            onStateUpdate = { state -> _quoteAudioSceneState.value = state }
        )
    }

    fun stopQuoteAudio() {
        audioVoiceService.stopAllAudio()
        _quoteAudioSceneState.update {
            it.copy(
                isGeneratingMusic = false,
                isPlayingMusic = false,
                isReadingAloud = false,
                statusCaption = ""
            )
        }
    }

    fun toggleVoiceDictation(
        target: VoiceTranscriptionTarget,
        onResultText: (String) -> Unit
    ) {
        val current = _voiceTranscriptionState.value
        if (current.isTranscribing) return

        if (current.isRecording && current.activeTarget == target) {
            // Stop recording and transcribe via gemini-3.5-transcribe
            _voiceTranscriptionState.update {
                it.copy(
                    isRecording = false,
                    isTranscribing = true,
                    statusMessage = "Transcribing voice with gemini-3.5-transcribe…",
                    errorMessage = null
                )
            }
            val contextHint = when (target) {
                VoiceTranscriptionTarget.SYMPOSIUM_CHAT -> "Philosophical discussion or literary question"
                VoiceTranscriptionTarget.MOMENT_QUOTE_PROMPT -> "Prompt for generating a quote in the moment"
                VoiceTranscriptionTarget.SALON_MARGINALIA -> "Live salon margin note or reflection"
                VoiceTranscriptionTarget.LIBRARY_SEARCH -> "Quote library search query, author, or category"
                VoiceTranscriptionTarget.PERSONAL_JOURNAL_NOTE -> "Personal commonplace journal note"
            }
            viewModelScope.launch {
                val result = audioVoiceService.stopRecordingAndTranscribe(contextHint)
                result.fold(
                    onSuccess = { transcript ->
                        _voiceTranscriptionState.update {
                            it.copy(
                                isRecording = false,
                                isTranscribing = false,
                                activeTarget = null,
                                lastTranscribedText = transcript,
                                statusMessage = "Transcribed via gemini-3.5-transcribe",
                                errorMessage = null
                            )
                        }
                        onResultText(transcript)
                    },
                    onFailure = { err ->
                        _voiceTranscriptionState.update {
                            it.copy(
                                isRecording = false,
                                isTranscribing = false,
                                activeTarget = null,
                                errorMessage = err.localizedMessage ?: "Audio transcription failed."
                            )
                        }
                    }
                )
            }
        } else {
            // Start microphone capture
            audioVoiceService.startMicrophoneRecording(viewModelScope)
            _voiceTranscriptionState.update {
                it.copy(
                    isRecording = true,
                    isTranscribing = false,
                    activeTarget = target,
                    statusMessage = "Listening via microphone… Tap stop to transcribe with gemini-3.5-transcribe",
                    errorMessage = null
                )
            }
        }
    }

    fun updateAccessibilitySettings(newSettings: AccessibilitySettings) {
        _accessibilitySettings.value = newSettings
    }

    fun toggleHighContrastMode() {
        _accessibilitySettings.update { current ->
            val next = !current.highContrastMode
            current.copy(
                highContrastMode = next,
                enlargedTouchTargets = if (next) true else current.enlargedTouchTargets
            )
        }
    }

    /**
     * Uses [NetworkHealthService] to check active network interfaces and VPN profiles for a valid
     * Tailscale mesh (`100.64.0.0/10` or `fd7a:115c:a1e0::/48`) or private VPN connection.
     * - When NOT detected, the Hosted LFS Server options remain hidden and unwired.
     * - When detected, the Hosted LFS Server options are unlocked in `FamilyCircleScreen` and wired.
     */
    fun refreshTailscaleDetection(
        simulatedInterfaces: List<Pair<String, List<String>>>? = null,
        simulatedVpnProfile: String? = null
    ) {
        val status = networkHealthService.checkPrivateNetworkHealth(
            simulatedInterfaces = simulatedInterfaces,
            simulatedVpnProfile = simulatedVpnProfile
        )
        _salonSessionState.update { current ->
            val updatedTransport = if (status.isDetected) {
                if (current.activeTransportMode == CommonplaceTransportMode.FIRESTORE_CLOUD_RELAY && !current.isConnected) {
                    CommonplaceTransportMode.LFS_HOSTED_SERVER
                } else {
                    current.activeTransportMode
                }
            } else {
                if (current.activeTransportMode == CommonplaceTransportMode.LFS_HOSTED_SERVER ||
                    current.activeTransportMode == CommonplaceTransportMode.LFS_LOCAL_HOST_TUNNEL
                ) {
                    CommonplaceTransportMode.FIRESTORE_CLOUD_RELAY
                } else {
                    current.activeTransportMode
                }
            }
            val updatedHostMode = if (status.isDetected) {
                LfsHostMode.LAUNCH_U2_HOSTING_SERVICE
            } else {
                LfsHostMode.HOST_LOCAL_DEVICE
            }
            val badge = if (status.isDetected) {
                "Tailscale Mesh Verified (${status.tailnetIp} on ${status.interfaceName}) • LFS Oasis Unlocked"
            } else {
                "Cloud Salon Relay Ready"
            }
            current.copy(
                isTailscaleDetected = status.isDetected,
                tailscaleStatus = status,
                activeTransportMode = updatedTransport,
                hostMode = updatedHostMode,
                activeTierBadge = badge
            )
        }
        refreshPublicSalonDirectory()
    }

    fun refreshPublicSalonDirectory() {
        viewModelScope.launch {
            val state = _salonSessionState.value
            val rooms = oasisServerClient.fetchPublicRoomsDirectory(
                endpointConfig = state.endpointConfig,
                isTailscaleConnected = state.isTailscaleDetected
            )
            _salonSessionState.update { it.copy(publicRoomsDirectory = rooms) }
        }
    }

    private fun observeIncomingWireMarginalia() {
        viewModelScope.launch {
            oasisServerClient.incomingEvents.collect { entry ->
                _salonSessionState.update { current ->
                    val deduplicated = (listOf(entry) + current.marginaliaFeed.filter { it.id != entry.id }).take(40)
                    current.copy(marginaliaFeed = deduplicated)
                }
            }
        }
    }

    fun setMultiplayerGameMode(mode: MultiplayerGameMode) {
        _salonSessionState.update { current ->
            val round = if (mode == MultiplayerGameMode.LITERARY_TRIVIA_DUEL && current.triviaRound == null) {
                oasisServerClient.generateTriviaRound(1, 5, _discoverState.value.allCatalogQuotes)
            } else {
                current.triviaRound
            }
            current.copy(activeGameMode = mode, triviaRound = round)
        }
    }

    fun setSalonTransportMode(mode: CommonplaceTransportMode) {
        _salonSessionState.update { current ->
            val allowedModes = OasisCommonplaceServerClient.availableTransportModes(current.isTailscaleDetected)
            val safeMode = if (mode in allowedModes) mode else CommonplaceTransportMode.FIRESTORE_CLOUD_RELAY
            current.copy(activeTransportMode = safeMode)
        }
    }

    fun setLfsHostMode(mode: LfsHostMode) {
        _salonSessionState.update { current ->
            val safeHostMode = if (current.isTailscaleDetected) mode else LfsHostMode.HOST_LOCAL_DEVICE
            current.copy(hostMode = safeHostMode)
        }
    }

    fun setLfsLifecyclePolicy(policy: LfsLifecyclePolicy) {
        _salonSessionState.update { it.copy(lifecyclePolicy = policy) }
    }

    fun toggleAiScholar(scholarHandle: String) {
        _salonSessionState.update { current ->
            val updated = current.activeAiScholars.toMutableList()
            if (scholarHandle in updated) {
                if (updated.size > 1) updated.remove(scholarHandle)
            } else {
                updated.add(scholarHandle)
            }
            current.copy(activeAiScholars = updated)
        }
    }

    fun launchOrJoinSalonRoom(
        requestedRoomCode: String? = null,
        customRoomTitle: String? = null
    ) {
        val current = _salonSessionState.value
        if (current.isConnecting) return

        val passageQuote = _discoverState.value.currentScoredQuote?.quote
            ?: _discoverState.value.dailyQuoteFromCollection

        viewModelScope.launch {
            _salonSessionState.update {
                it.copy(
                    isConnecting = true,
                    currentPassageQuote = passageQuote,
                    connectionDiagnostic = "Negotiating Salon room connection…"
                )
            }

            val handle = oasisServerClient.provisionOrJoinRoom(
                requestedRoomCode = requestedRoomCode,
                roomTitle = customRoomTitle?.takeIf { it.isNotBlank() } ?: current.roomTitle,
                hostMode = current.hostMode,
                lifecyclePolicy = current.lifecyclePolicy,
                preferredTransport = current.activeTransportMode,
                gameMode = current.activeGameMode,
                endpointConfig = current.endpointConfig,
                userId = currentUserId,
                userDisplayName = "Archival Reader",
                isTailscaleConnected = current.isTailscaleDetected
            )

            // Sync room to Tier-B Firestore Relay so multi-user participants & marginalia stay in sync
            repository.createOrJoinSymposiumRoom(
                roomCode = handle.roomCode,
                title = handle.title,
                gameMode = current.activeGameMode,
                lifecyclePolicy = handle.lifecyclePolicy,
                currentQuote = passageQuote,
                activeScholars = current.activeAiScholars
            )

            val participants = oasisServerClient.buildDefaultParticipants(
                currentUserId = currentUserId,
                currentUserName = "You (Host)",
                activeScholars = current.activeAiScholars,
                transportBadge = if (current.isTailscaleDetected) "LFS" else "RELAY"
            )

            val welcomeScholarEntry = oasisServerClient.generateResidentScholarResponse(
                scholarHandle = current.activeAiScholars.firstOrNull() ?: "@archivist",
                quote = passageQuote,
                userPromptOrNote = "Salon ${handle.roomCode} opened in ${current.activeGameMode.label}"
            ).copy(roomCode = handle.roomCode)

            _salonSessionState.update { state ->
                state.copy(
                    isConnected = true,
                    isConnecting = false,
                    roomCode = handle.roomCode,
                    roomTitle = handle.title,
                    activeTransportMode = handle.activeTransport,
                    activeTierBadge = handle.activeTierDescription,
                    participants = participants,
                    marginaliaFeed = (listOf(welcomeScholarEntry) + state.marginaliaFeed).take(40),
                    triviaRound = oasisServerClient.generateTriviaRound(
                        roundNumber = 1,
                        totalRounds = 5,
                        catalog = _discoverState.value.allCatalogQuotes
                    ),
                    connectionDiagnostic = "Connected to ${handle.roomCode} • ${handle.activeTierDescription}"
                )
            }

            attachFirestoreSalonListeners(handle.roomCode)
        }
    }

    private fun attachFirestoreSalonListeners(roomCode: String) {
        salonRoomObserverJob?.cancel()
        salonMessagesObserverJob?.cancel()

        salonRoomObserverJob = viewModelScope.launch {
            repository.observeSymposiumRoom(roomCode)
                .catch { e -> Log.w(TAG, "Salon room listener warning", e) }
                .collect { doc ->
                    if (doc != null) {
                        _salonSessionState.update { state ->
                            state.copy(roomTitle = doc.title.ifBlank { state.roomTitle })
                        }
                    }
                }
        }

        salonMessagesObserverJob = viewModelScope.launch {
            repository.observeSalonMessages(roomCode)
                .catch { e -> Log.w(TAG, "Salon messages listener warning", e) }
                .collect { docs ->
                    if (docs.isNotEmpty()) {
                        val mapped = docs.map { msg ->
                            LiveMarginaliaEntry(
                                id = msg.id,
                                roomCode = msg.roomCode,
                                authorUid = msg.senderUid,
                                authorName = msg.senderName,
                                quoteId = _salonSessionState.value.currentPassageQuote.id,
                                quoteSnippet = msg.quoteSnippet,
                                marginNote = msg.text,
                                highlightPhrase = msg.quoteSnippet,
                                scholarHandle = msg.scholarHandle,
                                createdAtEpochMs = (msg.createdAt?.seconds ?: 0L) * 1000L
                            )
                        }
                        _salonSessionState.update { state ->
                            val combined = (mapped + state.marginaliaFeed)
                                .distinctBy { "${it.authorName}:${it.marginNote}" }
                                .take(40)
                            state.copy(marginaliaFeed = combined)
                        }
                    }
                }
        }
    }

    fun leaveSalonRoom() {
        salonRoomObserverJob?.cancel()
        salonMessagesObserverJob?.cancel()
        oasisServerClient.disconnect()
        _salonSessionState.update { state ->
            state.copy(
                isConnected = false,
                isConnecting = false,
                roomCode = "",
                connectionDiagnostic = "Left salon room. Ready to launch or join a new room."
            )
        }
    }

    fun postLiveMarginalia(
        marginNote: String,
        highlightPhrase: String = "",
        invokeScholarHandle: String = ""
    ) {
        val trimmed = marginNote.trim()
        if (trimmed.isBlank() && invokeScholarHandle.isBlank()) return

        val state = _salonSessionState.value
        val activeCode = state.roomCode.ifBlank { "LFS-7K9M2P" }
        val quote = state.currentPassageQuote
        val snippet = highlightPhrase.ifBlank { quote.text.take(64) }

        val userEntry = LiveMarginaliaEntry(
            id = "m_${UUID.randomUUID().toString().take(10)}",
            roomCode = activeCode,
            authorUid = currentUserId,
            authorName = "You",
            quoteId = quote.id,
            quoteSnippet = snippet,
            marginNote = trimmed.ifBlank { "Summoned $invokeScholarHandle on “${quote.author}”" },
            highlightPhrase = snippet
        )

        val detectedScholar = when {
            invokeScholarHandle.isNotBlank() -> invokeScholarHandle
            trimmed.contains("@willow", ignoreCase = true) -> "@willow"
            trimmed.contains("@mote", ignoreCase = true) -> "@mote"
            trimmed.contains("@archivist", ignoreCase = true) -> "@archivist"
            state.activeAiScholars.isNotEmpty() -> state.activeAiScholars.first()
            else -> ""
        }

        val scholarReply = if (detectedScholar.isNotBlank()) {
            oasisServerClient.generateResidentScholarResponse(
                scholarHandle = detectedScholar,
                quote = quote,
                userPromptOrNote = trimmed
            ).copy(roomCode = activeCode)
        } else null

        _salonSessionState.update { current ->
            val added = listOfNotNull(scholarReply, userEntry)
            current.copy(marginaliaFeed = (added + current.marginaliaFeed).take(40))
        }

        viewModelScope.launch {
            oasisServerClient.sendLiveMarginaliaOverWire(userEntry)
            if (state.isConnected) {
                repository.postSalonMessage(
                    roomCode = activeCode,
                    messageType = "MARGINALIA",
                    text = userEntry.marginNote,
                    quoteSnippet = snippet,
                    scholarHandle = detectedScholar
                )
            }
        }
    }

    fun submitTriviaAnswer(selectedIndex: Int) {
        val current = _salonSessionState.value
        val round = current.triviaRound ?: return
        if (round.isRevealed) return

        val isCorrect = selectedIndex == round.correctOptionIndex
        val deltaPoints = if (isCorrect) 50 + (current.participants.firstOrNull()?.streak ?: 0) * 15 else 0

        val updatedParticipants = current.participants.mapIndexed { idx, p ->
            if (idx == 0 || p.id == currentUserId) {
                p.copy(
                    score = p.score + deltaPoints,
                    streak = if (isCorrect) p.streak + 1 else 0
                )
            } else if (p.isAiScholar) {
                p.copy(score = p.score + 25)
            } else p
        }

        _salonSessionState.update { state ->
            state.copy(
                triviaRound = round.copy(
                    selectedOptionIndex = selectedIndex,
                    isRevealed = true
                ),
                participants = updatedParticipants
            )
        }

        // Cross-Wing Trilogy Bridge: Emit a CommonplaceQuestDirectiveV1 whenever a Salon Trivia round completes
        val triviaQuote = _discoverState.value.allCatalogQuotes.find { it.id == round.quoteId }
            ?: _salonSessionState.value.currentPassageQuote
        emitQuestDirectiveForQuote(
            quote = triviaQuote,
            sourceEvent = if (isCorrect) "TRIVIA_VICTORY" else "TRIVIA_COMPLETED"
        )
    }

    fun advanceTriviaRound() {
        val current = _salonSessionState.value
        val nextRoundNum = ((current.triviaRound?.roundNumber ?: 1) % 5) + 1
        val nextRound = oasisServerClient.generateTriviaRound(
            roundNumber = nextRoundNum,
            totalRounds = 5,
            catalog = _discoverState.value.allCatalogQuotes
        )
        _salonSessionState.update { it.copy(triviaRound = nextRound) }
    }

    fun sealCurrentQuoteFolioEnvelope(passphrase: String) {
        val quote = _discoverState.value.currentScoredQuote?.quote
            ?: _discoverState.value.dailyQuoteFromCollection
        val payload = "“${quote.text}” — ${quote.author} (${quote.sourceWork})"
        val result = CommonplaceCryptoEnvelope.sealFolioEnvelope(payload, passphrase)
        result.fold(
            onSuccess = { sealed ->
                _salonSessionState.update {
                    it.copy(
                        lastSealedEnvelope = sealed,
                        connectionDiagnostic = "Sealed CPF1 AES-256-GCM Folio Envelope"
                    )
                }
            },
            onFailure = { err ->
                _salonSessionState.update {
                    it.copy(connectionDiagnostic = "Failed to seal envelope: ${err.localizedMessage}")
                }
            }
        )
    }

    fun openFolioEnvelope(envelope: String, passphrase: String) {
        val result = CommonplaceCryptoEnvelope.openFolioEnvelope(envelope, passphrase)
        result.fold(
            onSuccess = { plain ->
                _salonSessionState.update {
                    it.copy(
                        lastUnsealedPlaintext = plain,
                        connectionDiagnostic = "Verified & opened CPF1 Folio Envelope"
                    )
                }
            },
            onFailure = { err ->
                _salonSessionState.update {
                    it.copy(connectionDiagnostic = "Could not open CPF1 envelope: ${err.localizedMessage}")
                }
            }
        )
    }

    /**
     * Fetches the daily quote from the `/daily_quotes` Firestore collection (seeding today's
     * entry from the catalog if not yet created) and displays it on QuoteDiscoveryScreen.
     */
    fun fetchDailyQuoteFromCollection() {
        viewModelScope.launch {
            val dayOfYear = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
            val catalog = SeedQuoteCatalog.allQuotes
            val deterministicDailyFallback = catalog[dayOfYear % catalog.size]
            val dayKey = locationStampService.currentDayKey()

            val result = repository.fetchOrSeedDailyQuote(dayKey, deterministicDailyFallback)
            result.fold(
                onSuccess = { dailyQuote ->
                    _discoverState.update {
                        it.copy(
                            dailyQuoteFromCollection = dailyQuote,
                            isDailyQuoteSyncedFromFirestore = true
                        )
                    }
                },
                onFailure = {
                    _discoverState.update {
                        it.copy(
                            dailyQuoteFromCollection = deterministicDailyFallback,
                            isDailyQuoteSyncedFromFirestore = false
                        )
                    }
                }
            )
        }
    }

    private fun observeCloudPreferences() {
        viewModelScope.launch {
            repository.observeUserPreferences(currentUserId)
                .catch { e -> Log.w(TAG, "Failed to observe user preferences", e) }
                .collect { prefs ->
                    if (prefs != null) {
                        val mode = PriorityMode.entries.find { it.label == prefs.priorityMode }
                            ?: PriorityMode.BALANCED
                        _discoverState.update { state ->
                            state.copy(
                                selectedInterests = prefs.selectedInterests.ifEmpty {
                                    listOf("Stoicism", "Literature", "Philosophy")
                                },
                                priorityMode = mode,
                                recentQuoteIds = prefs.recentQuoteIds
                            )
                        }
                        recomputePriorityRanking(advanceToNext = false)
                    }
                }
        }
    }

    fun refreshPreviewStamp() {
        viewModelScope.launch {
            val stamp = locationStampService.captureProvenanceStamp()
            _discoverState.update {
                it.copy(
                    lastPreviewStampLocation = stamp.locationStamp,
                    lastPreviewStampDate = stamp.dateStamp,
                    lastPreviewStampTime = stamp.timeStamp
                )
            }
        }
    }

    fun toggleInterestFilter(category: String) {
        val current = _discoverState.value.selectedInterests.toMutableList()
        if (current.contains(category)) {
            if (current.size > 1) current.remove(category)
        } else {
            if (current.size < 15) current.add(category)
        }
        _discoverState.update { it.copy(selectedInterests = current, quickTakeawayState = null) }
        recomputePriorityRanking(advanceToNext = false)
        persistPreferencesToCloud()
    }

    fun setPriorityMode(mode: PriorityMode) {
        _discoverState.update { it.copy(priorityMode = mode, quickTakeawayState = null) }
        recomputePriorityRanking(advanceToNext = false)
        persistPreferencesToCloud()
    }

    fun nextPriorityQuote() {
        val state = _discoverState.value
        val currentId = state.currentScoredQuote?.quote?.id
        val updatedRecent = buildList {
            if (currentId != null) add(currentId)
            addAll(state.recentQuoteIds.filter { it != currentId })
        }.take(25)

        _discoverState.update {
            it.copy(
                recentQuoteIds = updatedRecent,
                quickTakeawayState = null,
                statusBannerMessage = null
            )
        }
        recomputePriorityRanking(advanceToNext = true)
        if (_accessibilitySettings.value.autoReadAloudOnSelect) {
            _discoverState.value.currentScoredQuote?.quote?.let { readQuoteAloud(it) }
        }
        persistPreferencesToCloud()
    }

    private fun recomputePriorityRanking(advanceToNext: Boolean) {
        val state = _discoverState.value
        val interests = state.selectedInterests.toSet()
        val recentIds = state.recentQuoteIds
        val currentId = state.currentScoredQuote?.quote?.id

        val scoredList = state.allCatalogQuotes.map { quote ->
            val matchesPrimary = quote.category in interests
            val matchingTagsCount = quote.tags.count { it in interests }
            val rawInterestScore = when {
                matchesPrimary && matchingTagsCount > 1 -> 100
                matchesPrimary -> 88
                matchingTagsCount > 0 -> 70
                else -> 30
            }

            val rawPopularity = quote.onlinePopularityScore.coerceIn(1, 100)
            val recentIndex = recentIds.indexOf(quote.id)
            val isRecent = recentIndex != -1

            val rawRecencyScore = when {
                recentIndex == 0 && advanceToNext -> 10
                recentIndex in 1..4 -> 85
                !isRecent -> 95
                else -> 60
            }

            val (interestW, popW, recencyW) = when (state.priorityMode) {
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

        val nextSelected = if (advanceToNext && scoredList.size > 1) {
            scoredList.firstOrNull { it.quote.id != currentId } ?: scoredList.firstOrNull()
        } else {
            scoredList.firstOrNull()
        }

        _discoverState.update {
            it.copy(
                rankedQuotes = scoredList,
                currentScoredQuote = nextSelected
            )
        }
    }

    private fun persistPreferencesToCloud() {
        val state = _discoverState.value
        viewModelScope.launch {
            repository.saveUserPreferences(
                selectedInterests = state.selectedInterests,
                priorityMode = state.priorityMode.label,
                recentQuoteIds = state.recentQuoteIds
            )
        }
    }

    fun generateAiPriorityQuote() {
        val state = _discoverState.value
        if (state.isDiscoveringFromAi) return

        viewModelScope.launch {
            _discoverState.update {
                it.copy(isDiscoveringFromAi = true, aiDiscoveryError = null, quickTakeawayState = null)
            }
            val recentAuthors = state.allCatalogQuotes.take(6).map { it.author }
            val result = aiService.discoverPriorityQuoteFromAi(
                selectedInterests = state.selectedInterests,
                priorityMode = state.priorityMode.label,
                recentAuthors = recentAuthors
            )
            result.fold(
                onSuccess = { newQuote ->
                    val updatedCatalog = listOf(newQuote) + _discoverState.value.allCatalogQuotes
                    val updatedRecent = (listOf(newQuote.id) + _discoverState.value.recentQuoteIds).distinct().take(25)
                    _discoverState.update {
                        it.copy(
                            allCatalogQuotes = updatedCatalog,
                            recentQuoteIds = updatedRecent,
                            isDiscoveringFromAi = false,
                            statusBannerMessage = "AI discovered a new priority quote for ${newQuote.category}"
                        )
                    }
                    recomputePriorityRanking(advanceToNext = false)
                    val scoredNew = _discoverState.value.rankedQuotes.find { it.quote.id == newQuote.id }
                    if (scoredNew != null) {
                        _discoverState.update { it.copy(currentScoredQuote = scoredNew) }
                    }
                    persistPreferencesToCloud()
                },
                onFailure = { error ->
                    _discoverState.update {
                        it.copy(
                            isDiscoveringFromAi = false,
                            aiDiscoveryError = error.localizedMessage ?: "AI quote discovery failed."
                        )
                    }
                }
            )
        }
    }

    fun generateQuickTakeawayForCurrentQuote() {
        val quote = _discoverState.value.currentScoredQuote?.quote ?: return
        viewModelScope.launch {
            _discoverState.update { it.copy(quickTakeawayState = UiState.Loading) }
            val result = aiService.generateQuickTakeaway(quote)
            result.fold(
                onSuccess = { text ->
                    _discoverState.update { it.copy(quickTakeawayState = UiState.Success(text)) }
                },
                onFailure = { err ->
                    _discoverState.update {
                        it.copy(quickTakeawayState = UiState.Error(err.localizedMessage ?: "Failed to generate takeaway."))
                    }
                }
            )
        }
    }

    /**
     * Saves the currently viewed quote to Firestore, capturing the current location
     * via FusedLocationProviderClient and attaching the TimestampInfo object to the Quote model.
     */
    fun saveCurrentlyViewedQuoteToFirestore(
        customLocationOverride: String? = null,
        personalNote: String = ""
    ) {
        val activeQuote = _discoverState.value.currentScoredQuote?.quote
            ?: _discoverState.value.dailyQuoteFromCollection
        saveQuoteWithStamp(
            quote = activeQuote,
            customLocationOverride = customLocationOverride,
            personalNote = personalNote
        )
    }

    fun saveQuoteWithStamp(
        quote: QuoteItem,
        customLocationOverride: String? = null,
        personalNote: String = ""
    ) {
        viewModelScope.launch {
            val timestampInfo = locationStampService.captureTimestampInfo(customLocationOverride)
            _discoverState.update {
                it.copy(
                    lastPreviewStampLocation = timestampInfo.location,
                    lastPreviewStampDate = timestampInfo.date,
                    lastPreviewStampTime = timestampInfo.time
                )
            }
            val quoteModel = Quote(
                userId = currentUserId,
                text = quote.text,
                author = quote.author,
                tags = quote.tags.ifEmpty { listOf(quote.category) },
                timestampInfo = timestampInfo,
                sourceWork = quote.sourceWork,
                category = quote.category,
                onlinePopularityScore = quote.onlinePopularityScore.toLong(),
                personalNote = personalNote
            )
            val result = repository.saveQuote(quoteModel)
            result.fold(
                onSuccess = {
                    _discoverState.update {
                        it.copy(
                            statusBannerMessage = "Stamped & saved to Firestore: ${timestampInfo.location} • ${timestampInfo.capturedAt}"
                        )
                    }
                },
                onFailure = { err ->
                    _discoverState.update {
                        it.copy(
                            statusBannerMessage = "Failed to save quote: ${err.localizedMessage ?: "Permission or network error"}"
                        )
                    }
                }
            )
            // Cross-Wing Trilogy Bridge: Emit Quest Directive to Parts 1, 2, and 3 on quote save/stamp
            emitQuestDirectiveForQuote(quote = quote, sourceEvent = "QUOTE_SAVED_AND_STAMPED")
        }
    }

    fun updateSavedQuoteNote(quoteId: String, newNote: String) {
        viewModelScope.launch {
            repository.updatePersonalNote(quoteId, newNote)
        }
    }

    fun deleteSavedQuote(quoteId: String) {
        viewModelScope.launch {
            repository.deleteSavedQuote(quoteId)
        }
    }

    fun clearStatusBanner() {
        _discoverState.update { it.copy(statusBannerMessage = null) }
    }

    // =========================================================================
    // Zero-Trust Family Social Circle Actions
    // =========================================================================

    fun syncGoogleEmailCircleInvites() {
        viewModelScope.launch {
            val res = repository.syncGoogleEmailInvitesToCircles()
            res.onSuccess { joinedCount ->
                if (joinedCount > 0) {
                    _discoverState.update {
                        it.copy(
                            statusBannerMessage = "Connected to $joinedCount Family Social Circle(s) via Google Sign-In"
                        )
                    }
                }
            }
        }
    }

    fun createFamilyCircle(circleName: String, rawInvitedEmails: String) {
        viewModelScope.launch {
            val emails = rawInvitedEmails.split(",", ";", " ")
                .map { it.trim().lowercase() }
                .filter { it.contains("@") }
            val result = repository.createFamilyCircle(
                circleName = circleName,
                invitedGoogleEmails = emails
            )
            result.fold(
                onSuccess = {
                    _discoverState.update {
                        it.copy(
                            statusBannerMessage = "Created Zero-Trust Family Circle “$circleName”"
                        )
                    }
                },
                onFailure = { err ->
                    _discoverState.update {
                        it.copy(
                            statusBannerMessage = "Could not create Family Circle: ${err.localizedMessage ?: "Error"}"
                        )
                    }
                }
            )
        }
    }

    fun inviteMemberToFamilyCircle(
        circle: FamilyCircle,
        googleEmail: String,
        googleUidOptional: String? = null
    ) {
        viewModelScope.launch {
            val result = repository.inviteGoogleMemberToCircle(
                circle = circle,
                memberGoogleEmail = googleEmail,
                memberUidOptional = googleUidOptional
            )
            result.fold(
                onSuccess = {
                    _discoverState.update {
                        it.copy(
                            statusBannerMessage = "Authorized $googleEmail in “${circle.name}”"
                        )
                    }
                },
                onFailure = { err ->
                    _discoverState.update {
                        it.copy(
                            statusBannerMessage = "Failed to invite member: ${err.localizedMessage ?: "Error"}"
                        )
                    }
                }
            )
        }
    }

    fun shareQuoteWithFamilyCircle(
        circle: FamilyCircle,
        quote: QuoteItem,
        personalNote: String = ""
    ) {
        viewModelScope.launch {
            val timestampInfo = locationStampService.captureTimestampInfo()
            val result = repository.shareQuoteToFamilyCircle(
                circle = circle,
                quote = quote,
                timestampInfo = timestampInfo,
                personalNote = personalNote
            )
            result.fold(
                onSuccess = {
                    _discoverState.update {
                        it.copy(
                            statusBannerMessage = "Shared quote with “${circle.name}” (${timestampInfo.location})"
                        )
                    }
                },
                onFailure = { err ->
                    _discoverState.update {
                        it.copy(
                            statusBannerMessage = "Failed to share with Family Circle: ${err.localizedMessage ?: "Error"}"
                        )
                    }
                }
            )
        }
    }

    // Dossier Deep Dive (High Thinking + Google Search Grounding + Google Maps Grounding)
    fun openQuoteDossier(quote: QuoteItem) {
        _dossierState.value = DossierUiState(targetQuote = quote)
        triggerDeepMeaningAnalysis(quote)
    }

    fun closeQuoteDossier() {
        _dossierState.value = DossierUiState()
    }

    fun triggerDeepMeaningAnalysis(quote: QuoteItem) {
        viewModelScope.launch {
            _dossierState.update { it.copy(deepMeaningState = UiState.Loading) }
            val result = aiService.generateDeepMeaningAnalysis(quote)
            result.fold(
                onSuccess = { text ->
                    _dossierState.update { it.copy(deepMeaningState = UiState.Success(text)) }
                },
                onFailure = { err ->
                    _dossierState.update {
                        it.copy(deepMeaningState = UiState.Error(err.localizedMessage ?: "Deep analysis failed."))
                    }
                }
            )
        }
    }

    fun triggerSearchGroundedContext(quote: QuoteItem) {
        viewModelScope.launch {
            _dossierState.update { it.copy(searchGroundedState = UiState.Loading) }
            val result = aiService.fetchSearchGroundedContext(quote)
            result.fold(
                onSuccess = { text ->
                    _dossierState.update { it.copy(searchGroundedState = UiState.Success(text)) }
                },
                onFailure = { err ->
                    _dossierState.update {
                        it.copy(searchGroundedState = UiState.Error(err.localizedMessage ?: "Google Search grounding failed."))
                    }
                }
            )
        }
    }

    fun triggerMapsGroundedPlaces(quote: QuoteItem) {
        viewModelScope.launch {
            _dossierState.update { it.copy(mapsGroundedState = UiState.Loading) }
            val locationHint = _discoverState.value.lastPreviewStampLocation
            val result = aiService.fetchMapsGroundedPlaces(quote, locationHint)
            result.fold(
                onSuccess = { text ->
                    _dossierState.update { it.copy(mapsGroundedState = UiState.Success(text)) }
                },
                onFailure = { err ->
                    _dossierState.update {
                        it.copy(mapsGroundedState = UiState.Error(err.localizedMessage ?: "Google Maps grounding failed."))
                    }
                }
            )
        }
    }

    // Visual Muse: Analyze Image for Quotes (gemini-4-argon)
    fun analyzeImageForQuotes(bitmap: Bitmap, userHint: String) {
        viewModelScope.launch {
            _photoMatchState.value = UiState.Loading
            val result = aiService.analyzeImageForQuotes(bitmap, userHint)
            result.fold(
                onSuccess = { matchResult ->
                    val existingTexts = _discoverState.value.allCatalogQuotes.map { it.text }.toSet()
                    val newItems = matchResult.matchedQuotes.filter { it.text !in existingTexts }
                    if (newItems.isNotEmpty()) {
                        _discoverState.update {
                            it.copy(allCatalogQuotes = newItems + it.allCatalogQuotes)
                        }
                        recomputePriorityRanking(advanceToNext = false)
                    }
                    _photoMatchState.value = UiState.Success(matchResult)
                },
                onFailure = { err ->
                    _photoMatchState.value = UiState.Error(
                        err.localizedMessage ?: "Failed to analyze photo with Gemini."
                    )
                }
            )
        }
    }

    private val _isGeneratingMomentQuote = MutableStateFlow(false)
    val isGeneratingMomentQuote: StateFlow<Boolean> = _isGeneratingMomentQuote.asStateFlow()

    /**
     * Generates a deep Quote in the moment inside PhilosophicalChatScreen using the Firebase AI SDK,
     * adapting to the user's preferred style for humor & fun or complex cross-field synthesis.
     */
    fun generateInTheMomentQuoteInChat(
        style: com.example.data.InTheMomentStyle,
        selectedFields: List<String>,
        momentPrompt: String,
        inspirationSavedQuote: QuoteItem? = null
    ) {
        if (_isGeneratingMomentQuote.value) return
        _isGeneratingMomentQuote.value = true

        val fieldsLabel = if (selectedFields.isEmpty()) "Philosophy & Everyday Wit" else selectedFields.joinToString(" + ")
        val userSummary = buildString {
            append("Forge a quote in the moment • Style: ${style.label} • Fields: $fieldsLabel")
            if (momentPrompt.isNotBlank()) append(" • Prompt: “$momentPrompt”")
        }
        val userRequestMsg = ChatMessage(
            isUser = true,
            text = userSummary,
            attachedQuote = inspirationSavedQuote
        )
        val placeholderId = UUID.randomUUID().toString()
        val loadingMsg = ChatMessage(
            id = placeholderId,
            isUser = false,
            text = "",
            isStreaming = true
        )
        _chatMessages.update { it + userRequestMsg + loadingMsg }

        viewModelScope.launch {
            val result = aiService.generateInTheMomentQuote(
                style = style,
                selectedFields = selectedFields,
                userMomentPrompt = momentPrompt,
                inspirationSavedQuote = inspirationSavedQuote
            )
            result.fold(
                onSuccess = { momentResult ->
                    _discoverState.update { state ->
                        state.copy(allCatalogQuotes = listOf(momentResult.quoteItem) + state.allCatalogQuotes)
                    }
                    recomputePriorityRanking(advanceToNext = false)

                    _chatMessages.update { list ->
                        list.map { msg ->
                            if (msg.id == placeholderId) {
                                msg.copy(
                                    text = momentResult.companionReflection,
                                    isStreaming = false,
                                    generatedMomentQuote = momentResult.quoteItem,
                                    fieldConnectionInsight = momentResult.fieldConnectionInsight
                                )
                            } else msg
                        }
                    }
                    _isGeneratingMomentQuote.value = false
                },
                onFailure = { err ->
                    val errText = err.localizedMessage ?: "Failed to forge in-the-moment quote."
                    _chatMessages.update { list ->
                        list.map { msg ->
                            if (msg.id == placeholderId) {
                                msg.copy(
                                    text = "Error generating quote in the moment: $errText",
                                    isStreaming = false
                                )
                            } else msg
                        }
                    }
                    _isGeneratingMomentQuote.value = false
                }
            )
        }
    }

    // Symposium / Philosophical Companion Multi-Turn Chat
    fun sendSymposiumMessage(userText: String, attachedQuote: QuoteItem? = null) {
        val trimmed = userText.trim()
        if (trimmed.isEmpty()) return

        _chatError.value = null
        val userMsg = ChatMessage(
            isUser = true,
            text = trimmed,
            attachedQuote = attachedQuote
        )
        val placeholderId = UUID.randomUUID().toString()
        val modelPlaceholder = ChatMessage(
            id = placeholderId,
            isUser = false,
            text = "",
            isStreaming = true
        )

        _chatMessages.update { it + userMsg + modelPlaceholder }

        val fullPrompt = if (attachedQuote != null) {
            "Regarding the saved quote \"${attachedQuote.text}\" by ${attachedQuote.author} (${attachedQuote.sourceWork}): $trimmed"
        } else {
            trimmed
        }

        viewModelScope.launch {
            val accumulated = StringBuilder()
            try {
                aiService.sendChatMessageStream(fullPrompt).collect { chunk ->
                    accumulated.append(chunk)
                    val currentText = accumulated.toString()
                    _chatMessages.update { list ->
                        list.map { msg ->
                            if (msg.id == placeholderId) msg.copy(text = currentText, isStreaming = true)
                            else msg
                        }
                    }
                }
                val finalText = accumulated.toString().ifBlank {
                    "I pondered that deeply, though my notes returned blank. Could you rephrase your reflection?"
                }
                _chatMessages.update { list ->
                    list.map { msg ->
                        if (msg.id == placeholderId) msg.copy(text = finalText, isStreaming = false)
                        else msg
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Symposium chat error", e)
                val errMsg = e.localizedMessage ?: "Failed to receive response from the Archivist."
                _chatError.value = errMsg
                _chatMessages.update { list ->
                    list.map { msg ->
                        if (msg.id == placeholderId) {
                            msg.copy(
                                text = "Error: $errMsg",
                                isStreaming = false
                            )
                        } else msg
                    }
                }
            }
        }
    }

    // =========================================================================
    // Interactive Quote Passage Highlighting & Stylized PDF Journal Export
    // =========================================================================

    private fun observeAnnotationsForSavedQuotes() {
        viewModelScope.launch {
            savedQuotesState.collect { uiState ->
                if (uiState is UiState.Success) {
                    uiState.data.forEach { quoteDoc ->
                        if (quoteDoc.id.isNotBlank() && !_quoteAnnotationsByQuoteId.value.containsKey(quoteDoc.id)) {
                            launch {
                                val res = repository.getQuoteHighlightAnnotations(quoteDoc.id, currentUserId)
                                res.onSuccess { list ->
                                    if (list.isNotEmpty()) {
                                        _quoteAnnotationsByQuoteId.update { map ->
                                            map + (quoteDoc.id to list)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    fun addHighlightAnnotation(
        quoteId: String,
        highlightedText: String,
        startCharIndex: Int,
        endCharIndex: Int,
        inkColor: HighlightInkColor,
        noteText: String
    ) {
        if (quoteId.isBlank() || highlightedText.isBlank()) return
        val timestampLabel = SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.US).format(Date())
        val tempId = "hl_${UUID.randomUUID().toString().replace("-", "").take(18)}"
        val optimisticAnnotation = QuoteHighlightAnnotation(
            id = tempId,
            quoteId = quoteId,
            userId = currentUserId,
            highlightedText = highlightedText.trim(),
            startCharIndex = startCharIndex.toLong(),
            endCharIndex = endCharIndex.toLong(),
            inkColor = inkColor.wireId,
            noteText = noteText.trim().ifBlank { "Highlighted passage in personal archive." },
            timestampLabel = timestampLabel
        )

        _quoteAnnotationsByQuoteId.update { currentMap ->
            val existing = currentMap[quoteId].orEmpty()
            val updated = (existing + optimisticAnnotation).sortedBy { it.startCharIndex }
            currentMap + (quoteId to updated)
        }

        viewModelScope.launch {
            repository.addQuoteHighlightAnnotation(
                quoteId = quoteId,
                highlightedText = optimisticAnnotation.highlightedText,
                startCharIndex = startCharIndex,
                endCharIndex = endCharIndex,
                inkColor = inkColor,
                noteText = optimisticAnnotation.noteText,
                timestampLabel = timestampLabel,
                customAnnotationId = tempId
            )
        }
    }

    fun deleteHighlightAnnotation(quoteId: String, annotationId: String) {
        _quoteAnnotationsByQuoteId.update { currentMap ->
            val existing = currentMap[quoteId].orEmpty()
            currentMap + (quoteId to existing.filterNot { it.id == annotationId })
        }
        viewModelScope.launch {
            repository.deleteQuoteHighlightAnnotation(quoteId, annotationId)
        }
    }

    fun exportSavedQuoteJournalPdf(
        context: android.content.Context?,
        quote: SavedQuoteDocument,
        folioTheme: PdfFolioTheme
    ): JournalPdfExportResult {
        val annotations = _quoteAnnotationsByQuoteId.value[quote.id].orEmpty()
        val result = JournalPagePdfExporter.exportJournalPagePdf(
            context = context,
            quote = quote,
            annotations = annotations,
            folioTheme = folioTheme,
            audioState = _quoteAudioSceneState.value
        )
        _pdfExportStatusMessage.value =
            "Exported ${folioTheme.label} (${result.fileName}) with ${result.annotationCount} annotations & ${result.soundscapeTitle} score card."
        return result
    }

    fun notifyPdfSavedToUserUri(fileName: String, byteCount: Long) {
        _pdfExportStatusMessage.value =
            "Saved $fileName (${byteCount / 1024} KB) to device storage."
    }

    fun clearPdfExportStatusMessage() {
        _pdfExportStatusMessage.value = null
    }

    // =========================================================================
    // Home Screen Daily Quote Widget & Scheduled 'Quote of the Day' Notification
    // =========================================================================

    fun initializeDailyScheduleAndWidget(context: android.content.Context) {
        val loaded = DailyQuoteScheduler.loadScheduleState(context)
        _dailyQuoteScheduleState.value = loaded
        val disc = _discoverState.value
        _dailyQuoteScheduleState.value = DailyQuoteScheduler.syncDailyQuoteAndPreferences(
            context = context,
            quote = disc.dailyQuoteFromCollection,
            locationStamp = disc.lastPreviewStampLocation,
            dateStamp = disc.lastPreviewStampDate,
            timeStamp = disc.lastPreviewStampTime,
            selectedInterests = disc.selectedInterests,
            priorityMode = disc.priorityMode,
            recentQuoteIds = disc.recentQuoteIds
        )
    }

    fun toggleDailyQuoteNotifications(context: android.content.Context, enabled: Boolean) {
        val current = _dailyQuoteScheduleState.value
        val updated = DailyQuoteScheduler.configureDailyQuoteSchedule(
            context = context,
            enabled = enabled,
            hourOfDay = current.scheduledHour,
            minute = current.scheduledMinute,
            usePriorityEngine = current.usePriorityEngineForDailyPush
        )
        _dailyQuoteScheduleState.value = updated
    }

    fun updateDailyNotificationTime(context: android.content.Context, hour: Int, minute: Int) {
        val current = _dailyQuoteScheduleState.value
        val updated = DailyQuoteScheduler.configureDailyQuoteSchedule(
            context = context,
            enabled = true,
            hourOfDay = hour,
            minute = minute,
            usePriorityEngine = current.usePriorityEngineForDailyPush
        )
        _dailyQuoteScheduleState.value = updated
    }

    fun toggleUsePriorityEngineForDailyPush(context: android.content.Context, usePriority: Boolean) {
        val current = _dailyQuoteScheduleState.value
        val updated = DailyQuoteScheduler.configureDailyQuoteSchedule(
            context = context,
            enabled = current.notificationsEnabled,
            hourOfDay = current.scheduledHour,
            minute = current.scheduledMinute,
            usePriorityEngine = usePriority
        )
        _dailyQuoteScheduleState.value = updated
    }

    fun sendTestDailyQuoteNotificationNow(context: android.content.Context) {
        val disc = _discoverState.value
        val quote = DailyQuoteRetrievalEngine.resolveQuoteOfTheDay(
            dayOfYear = Calendar.getInstance().get(Calendar.DAY_OF_YEAR),
            usePriorityRanking = _dailyQuoteScheduleState.value.usePriorityEngineForDailyPush,
            selectedInterests = disc.selectedInterests,
            priorityMode = disc.priorityMode,
            recentQuoteIds = disc.recentQuoteIds,
            syncedDailyOverride = disc.dailyQuoteFromCollection
        )
        val stamp = "${disc.lastPreviewStampLocation} • ${disc.lastPreviewStampDate} • ${disc.lastPreviewStampTime}"
        DailyQuoteScheduler.syncDailyQuoteAndPreferences(
            context = context,
            quote = quote,
            locationStamp = disc.lastPreviewStampLocation,
            dateStamp = disc.lastPreviewStampDate,
            timeStamp = disc.lastPreviewStampTime,
            selectedInterests = disc.selectedInterests,
            priorityMode = disc.priorityMode,
            recentQuoteIds = disc.recentQuoteIds
        )
        val posted = DailyQuoteScheduler.dispatchQuoteOfTheDayNotification(
            context = context,
            overrideQuote = quote,
            overrideStamp = stamp
        )
        _dailyQuoteScheduleState.update {
            it.copy(
                statusMessage = if (posted) {
                    "Dispatched 'Quote of the Day' notification (${quote.author}) & synced Home Screen widget."
                } else {
                    "Notification permission required on Android 13+; Home Screen widget cache updated."
                }
            )
        }
    }

    fun syncAndRequestPinHomeWidget(context: android.content.Context) {
        val disc = _discoverState.value
        val stamp = "${disc.lastPreviewStampLocation} • ${disc.lastPreviewStampDate} • ${disc.lastPreviewStampTime}"
        DailyQuoteScheduler.syncDailyQuoteAndPreferences(
            context = context,
            quote = disc.dailyQuoteFromCollection,
            locationStamp = disc.lastPreviewStampLocation,
            dateStamp = disc.lastPreviewStampDate,
            timeStamp = disc.lastPreviewStampTime,
            selectedInterests = disc.selectedInterests,
            priorityMode = disc.priorityMode,
            recentQuoteIds = disc.recentQuoteIds
        )
        val pinned = DailyQuoteScheduler.requestPinHomeWidget(context)
        _dailyQuoteScheduleState.update {
            it.copy(
                lastSyncedQuoteId = disc.dailyQuoteFromCollection.id,
                lastSyncedQuoteText = disc.dailyQuoteFromCollection.text,
                lastSyncedQuoteAuthor = disc.dailyQuoteFromCollection.author,
                lastSyncedQuoteSource = disc.dailyQuoteFromCollection.sourceWork,
                lastSyncedArchivalStamp = stamp,
                statusMessage = if (pinned) {
                    "Home Screen Widget pin prompt opened & daily quote synced."
                } else {
                    "Daily quote synced to Home Screen Widget cache. Long-press your launcher to add the widget."
                }
            )
        }
    }

    // =========================================================================
    // Zero-Trust Multi-User World Map & PrivacyControlOverlay
    // =========================================================================

    private fun buildTodayDayKey(): String {
        return SimpleDateFormat("yyyy_MM_dd", Locale.US).format(Date())
    }

    private fun defaultInitialFriendPermissions(): List<FriendPrivacyPermission> {
        return listOf(
            FriendPrivacyPermission(
                friendId = "clara_voss",
                displayName = "Dr. Clara Voss",
                email = "clara.voss@oxford-salon.org",
                uid = "friend_uid_clara",
                circleName = "Stoic Archival Circle",
                isSharingEnabled = true,
                precision = FriendLocationPrecision.EXACT_GPS
            ),
            FriendPrivacyPermission(
                friendId = "julian_mercer",
                displayName = "Julian Mercer",
                email = "j.mercer@commonplace-fellows.org",
                uid = "friend_uid_julian",
                circleName = "Bloomsbury Readers",
                isSharingEnabled = true,
                precision = FriendLocationPrecision.NEIGHBORHOOD
            )
        )
    }

    private fun observeZeroTrustWorldMap() {
        val todayKey = buildTodayDayKey()
        _worldMapState.update {
            it.copy(
                todayDayKey = todayKey,
                pins = ZeroTrustMapCoordinatesHelper.defaultGlobalAtlasSeedPins(todayKey).map { seed ->
                    ZeroTrustMapPinUiItem(pin = seed)
                }
            )
        }

        // Sync family circle members into friendPermissions list
        viewModelScope.launch {
            familyCirclesState.collect { circleState ->
                if (circleState is UiState.Success) {
                    val myEmail = repository.currentUserEmail().lowercase()
                    val existingByEmail = _worldMapState.value.friendPermissions.associateBy { it.email.lowercase() }
                    val merged = mutableListOf<FriendPrivacyPermission>()
                    merged.addAll(_worldMapState.value.friendPermissions)

                    circleState.data.forEach { circle ->
                        circle.memberEmails.forEachIndexed { idx, rawEmail ->
                            val cleanEmail = rawEmail.trim().lowercase()
                            if (cleanEmail.isNotBlank() && cleanEmail != myEmail && !existingByEmail.containsKey(cleanEmail)) {
                                val uid = circle.memberUids.getOrNull(idx).orEmpty()
                                val namePart = cleanEmail.substringBefore("@")
                                    .replace(".", " ")
                                    .replaceFirstChar { c -> c.uppercase() }
                                merged.add(
                                    FriendPrivacyPermission(
                                        friendId = cleanEmail.replace(Regex("[^a-z0-9]"), "_"),
                                        displayName = namePart,
                                        email = cleanEmail,
                                        uid = uid,
                                        circleName = circle.name,
                                        isSharingEnabled = true,
                                        precision = FriendLocationPrecision.EXACT_GPS
                                    )
                                )
                            }
                        }
                    }
                    _worldMapState.update { it.copy(friendPermissions = merged.distinctBy { p -> p.email.lowercase() }) }
                    rebuildCombinedWorldMapPins()
                }
            }
        }

        viewModelScope.launch {
            try {
                repository.observePublicMapQuotePins()
                    .catch { e -> Log.w(TAG, "Public map pins stream warning", e) }
                    .collect { publicPins ->
                        latestPublicMapPins.value = publicPins
                        rebuildCombinedWorldMapPins()
                    }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Unable to observe public map pins", e)
            }
        }

        viewModelScope.launch {
            try {
                repository.observeConnectedPinIdentities(currentUserId)
                    .catch { e -> Log.w(TAG, "Connected map pin identities stream warning", e) }
                    .collect { identities ->
                        latestConnectedIdentities.value = identities
                        rebuildCombinedWorldMapPins()
                    }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Unable to observe connected map pin identities", e)
            }
        }
    }

    private fun rebuildCombinedWorldMapPins() {
        val todayKey = buildTodayDayKey()
        val myUid = repository.currentUserId() ?: currentUserId
        val myEmail = repository.currentUserEmail()
        val publicDocs = latestPublicMapPins.value
        val identitiesById = latestConnectedIdentities.value.associateBy { it.pinId.ifBlank { it.id } }

        val circles = (familyCirclesState.value as? UiState.Success)?.data.orEmpty()
        val circleMemberUids = circles.flatMap { it.memberUids }.toSet()
        val circleMemberEmails = (
            circles.flatMap { it.memberEmails } +
                additionalConnectedFriendEmails.value +
                _worldMapState.value.friendPermissions.filter { it.isSharingEnabled }.map { it.email }
            ).map { it.lowercase() }.toSet()

        val liveUiItemsFromPublic = publicDocs.map { pinDoc ->
            val identity = identitiesById[pinDoc.id]
            val isOwner = pinDoc.ownerUid == myUid
            val isFriend = !isOwner && identity != null && (
                pinDoc.ownerUid in circleMemberUids ||
                    identity.ownerEmail.lowercase() in circleMemberEmails ||
                    myUid in identity.connectedUserUids
                )
            val decoded = identity?.let {
                ZeroTrustMapCoordinatesHelper.decodeIdentityLocationEnvelope(it.exactLocationLabel)
            }
            val viewerPrec = identity?.let {
                ZeroTrustMapCoordinatesHelper.resolveViewerPrecision(
                    rawCircleNames = it.circleNames,
                    viewerEmail = myEmail,
                    viewerUid = myUid
                )
            } ?: FriendLocationPrecision.EXACT_GPS

            ZeroTrustMapPinUiItem(
                pin = pinDoc,
                verifiedIdentity = identity,
                isCurrentUserOwner = isOwner,
                isConnectedFriend = isFriend,
                isHiddenFromPublicMap = decoded?.isHiddenFromPublic == true,
                viewerPrecision = viewerPrec,
                exactLatitude = decoded?.exactLat ?: pinDoc.latitude,
                exactLongitude = decoded?.exactLon ?: pinDoc.longitude
            )
        }

        // Also reconstruct Friends-Only pins that exist in /map_pin_identities but were hidden from /map_quote_pins
        val publicIds = publicDocs.map { it.id }.toSet()
        val friendsOnlyUiItems = latestConnectedIdentities.value
            .filter { idDoc -> idDoc.pinId.ifBlank { idDoc.id } !in publicIds }
            .map { idDoc ->
                val pinId = idDoc.pinId.ifBlank { idDoc.id }
                val decoded = ZeroTrustMapCoordinatesHelper.decodeIdentityLocationEnvelope(idDoc.exactLocationLabel)
                val exactLat = decoded.exactLat ?: _worldMapState.value.userCurrentLat
                val exactLon = decoded.exactLon ?: _worldMapState.value.userCurrentLon
                val coarseLat = ZeroTrustMapCoordinatesHelper.roundToCoarseGrid(exactLat)
                val coarseLon = ZeroTrustMapCoordinatesHelper.roundToCoarseGrid(exactLon)
                val isOwner = idDoc.ownerUid == myUid
                val viewerPrec = ZeroTrustMapCoordinatesHelper.resolveViewerPrecision(
                    rawCircleNames = idDoc.circleNames,
                    viewerEmail = myEmail,
                    viewerUid = myUid
                )
                val syntheticPin = MapQuotePinDocument(
                    id = pinId,
                    ownerUid = idDoc.ownerUid,
                    dayKey = todayKey,
                    quoteId = decoded.quoteId,
                    quoteText = decoded.quoteText.ifBlank { _discoverState.value.dailyQuoteFromCollection.text },
                    quoteAuthor = decoded.quoteAuthor.ifBlank { _discoverState.value.dailyQuoteFromCollection.author },
                    sourceWork = "Trusted Circle Exclusive Pin",
                    category = "Philosophy",
                    latitude = coarseLat,
                    longitude = coarseLon,
                    regionLabel = decoded.cleanLocationLabel.ifBlank { _worldMapState.value.userCurrentRegionLabel },
                    anonymousAlias = "Friends-Only Circle Pin"
                )
                ZeroTrustMapPinUiItem(
                    pin = syntheticPin,
                    verifiedIdentity = idDoc,
                    isCurrentUserOwner = isOwner,
                    isConnectedFriend = !isOwner,
                    isHiddenFromPublicMap = true,
                    viewerPrecision = viewerPrec,
                    exactLatitude = exactLat,
                    exactLongitude = exactLon
                )
            }

        val seedItems = ZeroTrustMapCoordinatesHelper.defaultGlobalAtlasSeedPins(todayKey).mapIndexed { idx, seed ->
            // Unlock the first seed pin if the user has connected friends so they can immediately inspect Friend Precision in action
            val firstPermittedFriend = _worldMapState.value.friendPermissions.firstOrNull { it.isSharingEnabled }
            if (idx == 0 && firstPermittedFriend != null) {
                val mockIdentity = MapPinIdentityDocument(
                    id = seed.id,
                    pinId = seed.id,
                    ownerUid = firstPermittedFriend.uid.ifBlank { "friend_uid_clara" },
                    ownerDisplayName = firstPermittedFriend.displayName,
                    ownerEmail = firstPermittedFriend.email,
                    circleNames = listOf(
                        firstPermittedFriend.circleName,
                        "PREC:${myEmail.lowercase()}:${firstPermittedFriend.precision.wireCode}"
                    ),
                    personalMarginalia = "Reading Seneca beside the Agora colonnade—sharing with our Trusted Circle.",
                    exactLocationLabel = ZeroTrustMapCoordinatesHelper.encodeIdentityLocationEnvelope(
                        cleanLocationLabel = "Stoa of Attalos Study Alcove, Athens",
                        exactLat = 37.9752,
                        exactLon = 23.7256,
                        isHiddenFromPublic = false,
                        quoteId = seed.quoteId,
                        quoteAuthor = seed.quoteAuthor,
                        quoteText = seed.quoteText
                    )
                )
                ZeroTrustMapPinUiItem(
                    pin = seed,
                    verifiedIdentity = mockIdentity,
                    isCurrentUserOwner = false,
                    isConnectedFriend = true,
                    isHiddenFromPublicMap = false,
                    viewerPrecision = firstPermittedFriend.precision,
                    exactLatitude = 37.9752,
                    exactLongitude = 23.7256
                )
            } else {
                ZeroTrustMapPinUiItem(pin = seed)
            }
        }

        val combined = (liveUiItemsFromPublic + friendsOnlyUiItems + seedItems).distinctBy { it.pin.id }
        val expectedTodayPinId = repository.buildDailyPinId(myUid, todayKey)
        val hasPinnedToday = combined.any {
            it.pin.id == expectedTodayPinId || (it.isCurrentUserOwner && it.pin.dayKey == todayKey)
        }

        _worldMapState.update { current ->
            val updatedSelected = current.selectedPin?.let { sel ->
                combined.find { it.pin.id == sel.pin.id }
            } ?: combined.firstOrNull()
            current.copy(
                pins = combined,
                selectedPin = updatedSelected,
                todayDayKey = todayKey,
                todayPinId = if (hasPinnedToday) expectedTodayPinId else current.todayPinId,
                hasPinnedToday = hasPinnedToday || current.hasPinnedToday
            )
        }
    }

    fun selectMapPin(item: ZeroTrustMapPinUiItem) {
        _worldMapState.update { it.copy(selectedPin = item) }
    }

    fun selectMapFilterMode(mode: MapPinFilterMode) {
        _worldMapState.update {
            it.copy(
                filterMode = mode,
                trustedConnectionsOnlyFilter = (mode == MapPinFilterMode.CONNECTED_FRIENDS_ONLY)
            )
        }
    }

    fun togglePrivacyOverlay(visible: Boolean) {
        _worldMapState.update { it.copy(isPrivacyOverlayVisible = visible) }
    }

    fun toggleGlobalMapVisibility(enabled: Boolean) {
        _worldMapState.update {
            it.copy(
                globalMapVisibilityEnabled = enabled,
                statusBannerMessage = if (enabled) {
                    "Global Map Visibility ON: Your daily quote pin appears on the public map as an anonymous cipher."
                } else {
                    "Global Map Visibility OFF: Your daily quote pin is hidden from the public map and shared ONLY with permitted friends."
                }
            )
        }
        val state = _worldMapState.value
        if (state.hasPinnedToday) {
            val myPin = state.pins.firstOrNull { it.isCurrentUserOwner }
            val quote = myPin?.let {
                QuoteItem(
                    id = it.pin.quoteId,
                    text = it.pin.quoteText,
                    author = it.pin.quoteAuthor,
                    sourceWork = it.pin.sourceWork,
                    category = it.pin.category,
                    tags = listOf(it.pin.category.ifBlank { "Philosophy" }),
                    onlinePopularityScore = 92,
                    popularOnlineContext = "Pinned on Commonplace World Map"
                )
            } ?: _discoverState.value.dailyQuoteFromCollection
            viewModelScope.launch {
                repository.syncGlobalMapVisibilityForTodayPin(
                    dayKey = state.todayDayKey.ifBlank { buildTodayDayKey() },
                    quote = quote,
                    latitude = state.userCurrentLat,
                    longitude = state.userCurrentLon,
                    regionLabel = state.userCurrentRegionLabel,
                    globalMapVisibilityEnabled = enabled
                )
                rebuildCombinedWorldMapPins()
            }
        } else {
            rebuildCombinedWorldMapPins()
        }
    }

    fun toggleTrustedConnectionsOnlyFilter(enabled: Boolean) {
        _worldMapState.update {
            it.copy(
                trustedConnectionsOnlyFilter = enabled,
                filterMode = if (enabled) MapPinFilterMode.CONNECTED_FRIENDS_ONLY else MapPinFilterMode.ALL_WORLD_PINS,
                statusBannerMessage = if (enabled) {
                    "Trusted Connections Filter ON: Showing only pins from your permitted circle friends."
                } else {
                    "Showing all public and connected map pins."
                }
            )
        }
    }

    fun updateFriendSharingPermission(
        friendId: String,
        isSharingEnabled: Boolean,
        precision: FriendLocationPrecision
    ) {
        _worldMapState.update { current ->
            val updatedList = current.friendPermissions.map { perm ->
                if (perm.friendId == friendId) {
                    perm.copy(isSharingEnabled = isSharingEnabled, precision = precision)
                } else {
                    perm
                }
            }
            val targetName = updatedList.find { it.friendId == friendId }?.displayName ?: "Friend"
            val summary = if (isSharingEnabled) {
                "$targetName permitted with ${precision.label} precision."
            } else {
                "$targetName removed from Zero-Trust location sharing ACL."
            }
            current.copy(
                friendPermissions = updatedList,
                statusBannerMessage = summary
            )
        }
        rebuildCombinedWorldMapPins()

        val state = _worldMapState.value
        if (state.hasPinnedToday && state.todayPinId.isNotBlank()) {
            val circles = (familyCirclesState.value as? UiState.Success)?.data.orEmpty()
            viewModelScope.launch {
                repository.updatePinIdentityConnections(
                    pinId = state.todayPinId,
                    connectedCircles = circles,
                    additionalConnectedEmails = additionalConnectedFriendEmails.value,
                    friendPermissions = _worldMapState.value.friendPermissions
                )
            }
        }
    }

    fun refreshWorldMapGpsLocation() {
        viewModelScope.launch {
            val stamp = locationStampService.captureProvenanceStamp()
            _worldMapState.update {
                it.copy(
                    userCurrentLat = stamp.latitude,
                    userCurrentLon = stamp.longitude,
                    userCurrentRegionLabel = stamp.locationStamp,
                    statusBannerMessage = "GPS updated: ${stamp.locationStamp} (Coarse public grid ${ZeroTrustMapCoordinatesHelper.roundToCoarseGrid(stamp.latitude)}°, ${ZeroTrustMapCoordinatesHelper.roundToCoarseGrid(stamp.longitude)}°)"
                )
            }
        }
    }

    fun pinDailyQuoteToWorldMap(
        quote: QuoteItem,
        personalMarginalia: String,
        customRegionLabel: String? = null
    ) {
        val currentMap = _worldMapState.value
        if (currentMap.hasPinnedToday) {
            _worldMapState.update {
                it.copy(statusBannerMessage = "Daily Pin Limit: You can pin one quote per day (${currentMap.todayDayKey}).")
            }
            return
        }

        _worldMapState.update { it.copy(isPinningInProgress = true, statusBannerMessage = null) }
        viewModelScope.launch {
            val stamp = locationStampService.captureProvenanceStamp(customRegionLabel)
            val todayKey = buildTodayDayKey()
            val myUid = repository.currentUserId() ?: currentUserId
            val myName = repository.currentUserDisplayName()
            val myEmail = repository.currentUserEmail()
            val circles = (familyCirclesState.value as? UiState.Success)?.data.orEmpty()
            val pinId = repository.buildDailyPinId(myUid, todayKey)
            val coarseLat = ZeroTrustMapCoordinatesHelper.roundToCoarseGrid(stamp.latitude)
            val coarseLon = ZeroTrustMapCoordinatesHelper.roundToCoarseGrid(stamp.longitude)
            val alias = ZeroTrustMapCoordinatesHelper.buildAnonymousLiteraryAlias(myUid, todayKey)
            val globalVisible = _worldMapState.value.globalMapVisibilityEnabled
            val friendPerms = _worldMapState.value.friendPermissions

            val optimisticPin = MapQuotePinDocument(
                id = pinId,
                ownerUid = myUid,
                dayKey = todayKey,
                quoteId = quote.id,
                quoteText = quote.text,
                quoteAuthor = quote.author,
                sourceWork = quote.sourceWork,
                category = quote.category,
                latitude = coarseLat,
                longitude = coarseLon,
                regionLabel = stamp.locationStamp,
                anonymousAlias = alias
            )
            val optimisticIdentity = MapPinIdentityDocument(
                id = pinId,
                pinId = pinId,
                ownerUid = myUid,
                ownerDisplayName = myName,
                ownerEmail = myEmail,
                circleNames = ZeroTrustMapCoordinatesHelper.buildCircleNamesWithPrecisionTags(
                    circleNames = circles.map { it.name },
                    friendPermissions = friendPerms
                ),
                personalMarginalia = personalMarginalia.trim(),
                exactLocationLabel = ZeroTrustMapCoordinatesHelper.encodeIdentityLocationEnvelope(
                    cleanLocationLabel = "${stamp.locationStamp} • ${stamp.dateStamp} ${stamp.timeStamp}",
                    exactLat = stamp.latitude,
                    exactLon = stamp.longitude,
                    isHiddenFromPublic = !globalVisible,
                    quoteId = quote.id,
                    quoteAuthor = quote.author,
                    quoteText = quote.text
                ),
                connectedUserUids = listOf(myUid) + circles.flatMap { it.memberUids },
                connectedUserEmails = listOf(myEmail) + circles.flatMap { it.memberEmails } + additionalConnectedFriendEmails.value
            )
            val optimisticUiItem = ZeroTrustMapPinUiItem(
                pin = optimisticPin,
                verifiedIdentity = optimisticIdentity,
                isCurrentUserOwner = true,
                isConnectedFriend = false,
                isHiddenFromPublicMap = !globalVisible,
                viewerPrecision = FriendLocationPrecision.EXACT_GPS,
                exactLatitude = stamp.latitude,
                exactLongitude = stamp.longitude
            )

            _worldMapState.update { state ->
                val updatedPins = listOf(optimisticUiItem) + state.pins.filterNot { it.pin.id == pinId }
                state.copy(
                    pins = updatedPins,
                    selectedPin = optimisticUiItem,
                    hasPinnedToday = true,
                    todayPinId = pinId,
                    todayDayKey = todayKey,
                    userCurrentLat = stamp.latitude,
                    userCurrentLon = stamp.longitude,
                    userCurrentRegionLabel = stamp.locationStamp,
                    isPinningInProgress = false,
                    statusBannerMessage = if (globalVisible) {
                        "Pinned to World Map! Public users see '$alias' at ($coarseLat°, $coarseLon°); permitted friends see your identity & configured precision."
                    } else {
                        "Pinned exclusively for Permitted Friends! Hidden from public map (/map_quote_pins omitted)."
                    }
                )
            }

            repository.pinDailyQuoteToWorldMap(
                dayKey = todayKey,
                quote = quote,
                latitude = stamp.latitude,
                longitude = stamp.longitude,
                regionLabel = stamp.locationStamp,
                exactLocationLabel = "${stamp.locationStamp} • ${stamp.dateStamp} ${stamp.timeStamp}",
                personalMarginalia = personalMarginalia,
                connectedCircles = circles,
                additionalConnectedEmails = additionalConnectedFriendEmails.value,
                globalMapVisibilityEnabled = globalVisible,
                friendPermissions = friendPerms
            )
        }
    }

    fun connectMapFriendByEmail(friendEmail: String) {
        val clean = friendEmail.trim().lowercase()
        if (clean.isBlank() || !clean.contains("@")) return
        additionalConnectedFriendEmails.update { (it + clean).distinct() }
        val namePart = clean.substringBefore("@")
            .replace(".", " ")
            .replaceFirstChar { c -> c.uppercase() }
        val newPerm = FriendPrivacyPermission(
            friendId = clean.replace(Regex("[^a-z0-9]"), "_"),
            displayName = namePart,
            email = clean,
            circleName = "Trusted Map Circle",
            isSharingEnabled = true,
            precision = FriendLocationPrecision.EXACT_GPS
        )
        _worldMapState.update { current ->
            val updatedPerms = (current.friendPermissions.filterNot { it.email.equals(clean, ignoreCase = true) } + newPerm)
            current.copy(
                friendPermissions = updatedPerms,
                statusBannerMessage = "Authorized $clean in Zero-Trust Map Circle ACL."
            )
        }
        rebuildCombinedWorldMapPins()

        val circles = (familyCirclesState.value as? UiState.Success)?.data.orEmpty()
        val firstCircle = circles.firstOrNull()
        viewModelScope.launch {
            if (firstCircle != null) {
                repository.inviteGoogleMemberToCircle(firstCircle, clean)
            } else {
                repository.createFamilyCircle("Zero-Trust Map Circle", listOf(clean))
            }
            val todayPinId = _worldMapState.value.todayPinId
            if (todayPinId.isNotBlank()) {
                repository.updatePinIdentityConnections(
                    pinId = todayPinId,
                    connectedCircles = circles,
                    additionalConnectedEmails = additionalConnectedFriendEmails.value,
                    friendPermissions = _worldMapState.value.friendPermissions
                )
            }
        }
    }

    fun saveMapPinQuoteToArchive(item: ZeroTrustMapPinUiItem) {
        val quote = QuoteItem(
            id = item.pin.quoteId.ifBlank { item.pin.id },
            text = item.pin.quoteText,
            author = item.pin.quoteAuthor,
            sourceWork = item.pin.sourceWork.ifBlank { "World Map Pin (${item.pin.regionLabel})" },
            category = item.pin.category.ifBlank { "Philosophy" },
            tags = listOf(item.pin.category.ifBlank { "Philosophy" }, "WorldMapPin"),
            onlinePopularityScore = 92,
            popularOnlineContext = "Saved from Commonplace World Map (${item.displayAttributionLabel})"
        )
        saveQuoteWithStamp(
            quote = quote,
            customLocationOverride = item.pin.regionLabel,
            personalNote = "Saved from Commonplace World Map (${item.displayAttributionLabel})"
        )
        _worldMapState.update {
            it.copy(statusBannerMessage = "Saved “${item.pin.quoteText.take(36)}…” to your Archive with provenance stamp.")
        }
    }

    fun dismissWorldMapStatusBanner() {
        _worldMapState.update { it.copy(statusBannerMessage = null) }
    }

    // =========================================================================
    // Master 4-Wing Trilogy Launcher, Tailscale Handshake & TrashArcade Bridge
    // =========================================================================

    fun probeTailscaleHandshakeAndSyncPresence(preset: LfsServerPreset = _trilogyState.value.serverPreset) {
        viewModelScope.launch {
            val calendar = Calendar.getInstance()
            val minuteOfDay = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)
            val handshake = oasisServerClient.probeTailscaleHandshake(
                preset = preset,
                fallbackUserId = currentUserId,
                minuteOfDay = minuteOfDay
            )
            // Fetch OpenAudible / AudioBookshelf Sanctuary Storyteller library (cb64f9e7) non-blocking <= 1600ms
            val (chapters, isOfflineFallback) = oasisServerClient.fetchOpenAudibleLibrary(
                baseUrl = preset.baseUrl,
                stableNodeId = handshake.stableNodeId
            )
            val bannerText = DisneyImagineeringTranslator.translateTechnicalTerm(handshake.statusSummary)
            _trilogyState.update { current ->
                current.copy(
                    serverPreset = preset,
                    handshake = handshake,
                    storyteller = current.storyteller.copy(
                        chapters = chapters,
                        isOfflineSampleFallback = isOfflineFallback,
                        statusBadge = if (isOfflineFallback) {
                            "Sanctuary Storyteller (cb64f9e7) • Cozy Desk Narration Active"
                        } else {
                            "Sanctuary Storyteller (cb64f9e7) • Live OpenAudible Library Connected"
                        }
                    ),
                    friendlyGuideCard = DisneyImagineeringTranslator.toFriendlyGuideCard(bannerText),
                    lastSyncStatusBanner = bannerText
                )
            }

            // Broadcast U2 presence (non-blocking <= 1600ms)
            oasisServerClient.u2SyncPresence(
                u2BaseUrl = preset.baseUrl,
                activeRoomCode = _trilogyState.value.activeRoomCode,
                activeWingSlug = _trilogyState.value.activeWing.slug,
                stableNodeId = handshake.stableNodeId
            )
        }
    }

    fun selectServerPresetAndProbeHandshake(preset: LfsServerPreset) {
        probeTailscaleHandshakeAndSyncPresence(preset)
    }

    fun selectTrilogyWing(wing: TrilogyWing) {
        val mappedDest = OasisHallDestination.entries.firstOrNull { it.mappedWing == wing }
            ?: OasisHallDestination.COMMONPLACE_WING_I
        val banner = "Switched to ${wing.wingNumber}: ${wing.title}"
        _trilogyState.update { current ->
            current.copy(
                activeWing = wing,
                activeHallDestination = mappedDest,
                friendlyGuideCard = DisneyImagineeringTranslator.toFriendlyGuideCard(banner),
                lastSyncStatusBanner = banner
            )
        }
        viewModelScope.launch {
            val state = _trilogyState.value
            oasisServerClient.u2SyncPresence(
                u2BaseUrl = state.serverPreset.baseUrl,
                activeRoomCode = state.activeRoomCode,
                activeWingSlug = wing.slug,
                stableNodeId = state.handshake.stableNodeId
            )
        }
    }

    fun selectOasisHallDestination(destination: OasisHallDestination) {
        val targetWing = destination.mappedWing ?: _trilogyState.value.activeWing
        val banner = "Switched to ${destination.displayLabel} • ${destination.comfortModeSummary}"
        _trilogyState.update { current ->
            current.copy(
                activeHallDestination = destination,
                activeWing = targetWing,
                cartridgeSlug = destination.wireSlug,
                friendlyGuideCard = DisneyImagineeringTranslator.toFriendlyGuideCard(
                    rawNoticeOrError = banner,
                    preferredActionLabel = if (destination.isArcadeHallCabinet) {
                        "Return to Commonplace Wing I"
                    } else {
                        "Explore ${destination.displayLabel}"
                    }
                ),
                lastSyncStatusBanner = banner
            )
        }
        viewModelScope.launch {
            val state = _trilogyState.value
            oasisServerClient.u2SyncPresence(
                u2BaseUrl = state.serverPreset.baseUrl,
                activeRoomCode = state.activeRoomCode,
                activeWingSlug = destination.wireSlug,
                stableNodeId = state.handshake.stableNodeId
            )
        }
    }

    /**
     * Dispatches and listens for `oasis:overlay-toggle` (`Shift+Tab` / `Ctrl+K`) so single-player
     * action physics pause cleanly while 16-Channel Rmblyn Family Chat, menus, and LFS navigation
     * remain responsive without dropping the active party room (`OASIS-XXXX`).
     */
    fun toggleOasisOverlay(triggerSource: String = "oasis:overlay-toggle") {
        _trilogyState.update { current ->
            val nextOpen = !current.isOasisOverlayOpen
            val statusMsg = if (nextOpen) {
                "Dispatched $triggerSource • Single-player action physics paused • Party ${current.activeRoomCode} live"
            } else {
                "Closed $triggerSource • Single-player action physics resumed • Party ${current.activeRoomCode} live"
            }
            current.copy(
                isOasisOverlayOpen = nextOpen,
                isActionPhysicsPaused = nextOpen,
                lastOverlayToggleEvent = statusMsg,
                friendlyGuideCard = DisneyImagineeringTranslator.toFriendlyGuideCard(statusMsg),
                lastSyncStatusBanner = statusMsg
            )
        }
    }

    fun toggleZt9ComfortOverrides(enabled: Boolean) {
        _trilogyState.update { current ->
            val updatedComfort = current.zt9ComfortOverrides.copy(familyFriendlyArcadeMode = enabled)
            val msg = if (enabled) {
                "Family-Friendly Arcade Mode (zt9_comfort_overrides) ON: Cardboard Critters / Neon Arcade Bots / Confetti KOs / Tag-Team Rescue"
            } else {
                "Standard Arcade Mode active • Switch back to Family-Friendly Mode anytime"
            }
            current.copy(
                zt9ComfortOverrides = updatedComfort,
                friendlyGuideCard = DisneyImagineeringTranslator.toFriendlyGuideCard(msg),
                lastSyncStatusBanner = msg
            )
        }
    }

    fun selectRmblynChannel(channelNumber: Int) {
        val bounded = channelNumber.coerceIn(1, 16)
        _trilogyState.update { current ->
            current.copy(activeRmblynChannel = bounded)
        }
    }

    fun sendRmblynFamilyChatMessage(rawMessage: String) {
        val clean = rawMessage.trim()
        if (clean.isBlank()) return
        val current = _trilogyState.value
        val chNum = current.activeRmblynChannel
        val chName = CommonplaceTrilogySyncEnvelopeV1.RMBLYN_16_CHANNELS
            .firstOrNull { it.first == chNum }?.second ?: "#01-hearth-lobby"
        val userMsg = RmblynChannelMessage(
            id = "rmb_usr_${System.currentTimeMillis()}",
            channelNumber = chNum,
            channelName = chName,
            senderHandle = "@family-reader",
            text = clean,
            isCompanionReply = false
        )

        val companionReply: RmblynChannelMessage? = when {
            clean.contains("@willow", ignoreCase = true) -> RmblynChannelMessage(
                id = "rmb_willow_${System.currentTimeMillis() + 1}",
                channelNumber = chNum,
                channelName = chName,
                senderHandle = "@willow",
                text = "Willow's Lantern shines on Room ${current.activeRoomCode}! Your Time-Machine Memory Vault (${current.selectedSaveSlotKey}) is safe and warm.",
                isCompanionReply = true
            )
            clean.contains("@cinder", ignoreCase = true) -> RmblynChannelMessage(
                id = "rmb_cinder_${System.currentTimeMillis() + 1}",
                channelNumber = chNum,
                channelName = chName,
                senderHandle = "@cinder",
                text = "Cinder here! Neon Arcade Bots and Cardboard Critters are ready for Confetti KOs and Tag-Team Rescue!",
                isCompanionReply = true
            )
            clean.contains("@mote", ignoreCase = true) -> RmblynChannelMessage(
                id = "rmb_mote_${System.currentTimeMillis() + 1}",
                channelNumber = chNum,
                channelName = chName,
                senderHandle = "@mote",
                text = "Mote sprinkled Starlight Rupees (+5) on your reading desk! Sanctuary Storyteller is ready whenever you want a chapter.",
                isCompanionReply = true
            )
            else -> null
        }

        _trilogyState.update { state ->
            val updatedList = (state.rmblynMessages + listOfNotNull(userMsg, companionReply)).takeLast(32)
            state.copy(
                rmblynMessages = updatedList,
                lastRmblynCommandResult = "Sent to $chName: $clean"
            )
        }
        sendRmblynOverlayCommand(clean)
    }

    // =========================================================================
    // AudioBookshelf & OpenAudible Sanctuary Storyteller (`cb64f9e7`) Actions
    // =========================================================================

    fun toggleSanctuaryStorytellerPlayPause() {
        val current = _trilogyState.value.storyteller
        val nextPlaying = !current.isPlaying
        val chapter = current.activeChapter()
        val streamUrl = oasisServerClient.buildOpenAudibleStreamUrl(
            baseUrl = _trilogyState.value.serverPreset.baseUrl,
            chapterKey = chapter.key
        )
        _trilogyState.update { state ->
            val msg = if (nextPlaying) {
                "Playing ${chapter.title} via Sanctuary Storyteller (cb64f9e7) • Sleep-Fade ${state.storyteller.sleepFadeTimerMinutes ?: 30}m"
            } else {
                "Paused Sanctuary Storyteller (${chapter.title})"
            }
            state.copy(
                storyteller = state.storyteller.copy(
                    isPlaying = nextPlaying,
                    statusBadge = "$msg • $streamUrl"
                ),
                friendlyGuideCard = DisneyImagineeringTranslator.toFriendlyGuideCard(msg),
                lastSyncStatusBanner = msg
            )
        }

        // Also trigger warm porch ambient + TTS narration when playing in local/offline fallback
        if (nextPlaying) {
            val quoteForNarration = QuoteItem(
                id = "storyteller_${chapter.key}",
                text = chapter.narrationTranscript,
                author = chapter.author,
                sourceWork = chapter.bookTitle,
                category = "Literature",
                tags = listOf("Literature", "SanctuaryStoryteller"),
                onlinePopularityScore = 98,
                popularOnlineContext = "Sanctuary Storyteller (cb64f9e7)"
            )
            readQuoteAloud(quoteForNarration)
        } else {
            stopQuoteAudio()
        }
    }

    fun selectSanctuaryChapter(chapterKey: String) {
        _trilogyState.update { state ->
            val found = state.storyteller.chapters.firstOrNull { it.key == chapterKey }
                ?: state.storyteller.activeChapter()
            val msg = "Selected Storyteller Chapter: ${found.title} (/api/lfs/openaudible/stream/${found.key})"
            state.copy(
                storyteller = state.storyteller.copy(
                    activeChapterKey = found.key,
                    playbackElapsedSeconds = 0,
                    statusBadge = msg
                ),
                friendlyGuideCard = DisneyImagineeringTranslator.toFriendlyGuideCard(msg),
                lastSyncStatusBanner = msg
            )
        }
    }

    fun setStarlightSleepFadeTimer(minutes: Int?) {
        val validMinutes = if (minutes in listOf(15, 30, 45)) minutes else 30
        _trilogyState.update { state ->
            val secs = (validMinutes ?: 30) * 60
            val msg = "Starlight Sleep-Fade Timer set to ${validMinutes}m (${DisneyImagineeringTranslator.STARLIGHT_QUIET_HOURS_LABEL})"
            state.copy(
                storyteller = state.storyteller.copy(
                    sleepFadeTimerMinutes = validMinutes,
                    sleepFadeRemainingSeconds = secs,
                    statusBadge = msg
                ),
                friendlyGuideCard = DisneyImagineeringTranslator.toFriendlyGuideCard(msg),
                lastSyncStatusBanner = msg
            )
        }
    }

    fun setManualDoctrineOverride(doctrine: PhilosophicalDoctrine?) {
        _trilogyState.update { current ->
            val latestCat = current.pendingDirectives.firstOrNull()?.category ?: "Philosophy"
            val effective = doctrine ?: PhilosophicalDoctrine.fromQuoteCategory(latestCat)
            val modeDesc = doctrine?.label ?: "Auto-Mapped from Quote Category (${effective.label})"
            current.copy(
                manualDoctrineOverride = doctrine,
                effectiveDoctrine = effective,
                lastSyncStatusBanner = "Philosophical Doctrine set to: $modeDesc"
            )
        }
    }

    fun emitFeaturedQuoteQuestDirective() {
        val quote = _discoverState.value.currentScoredQuote?.quote
            ?: _discoverState.value.dailyQuoteFromCollection
        emitQuestDirectiveForQuote(quote = quote, sourceEvent = "MANUAL_CODEX_FORGE")
    }

    fun emitQuestDirectiveForQuote(
        quote: QuoteItem,
        sourceEvent: String
    ) {
        val current = _trilogyState.value
        val replicaId = current.handshake.stableNodeId
        val directive = NativeArcadeAssetBridge.buildDirectiveFromQuote(
            quote = quote,
            sourceEvent = sourceEvent,
            manualDoctrineOverride = current.manualDoctrineOverride,
            replicaId = replicaId
        )

        // Resolve Total Lexical Order LWW (ts, replicaId, val) using stableNodeId as deterministic replicaId
        val nowMs = System.currentTimeMillis()
        val localCell = TotalOrderLwwCell(
            ts = nowMs - 1L,
            replicaId = replicaId,
            value = current.pendingDirectives.firstOrNull()?.directiveId ?: "dir_init"
        )
        val incomingCell = TotalOrderLwwCell(
            ts = nowMs,
            replicaId = replicaId,
            value = directive.directiveId
        )
        val winningDirectiveId = OasisCommonplaceServerClient.resolveTotalOrderLww(localCell, incomingCell).value

        val newTotalRupees = current.totalRupees + directive.rupeeReward
        val bonusDenom = when {
            directive.rupeeReward >= 50 -> 50
            directive.rupeeReward >= 20 -> 20
            else -> 10
        }
        val updatedDenominations = current.rupeeDenominations.map { denom ->
            if (denom.value == bonusDenom) denom.copy(count = denom.count + 1)
            else denom
        }

        val updatedWing2Queue = (
            listOf(directive.scriptoriumTaskLabel) + current.wing2ChronicleQueue
            ).distinct().take(6)

        val updatedWing3Missions = (
            listOf(directive.raidMissionBrief) + current.wing3ActiveMissions
            ).distinct().take(6)

        val updatedWing4Artifacts = (
            listOf("Bestiary Seed: ${directive.bestiarySeed}") + current.wing4DeskArtifacts
            ).distinct().take(6)

        val resolvedDoctrine = PhilosophicalDoctrine.fromWireId(directive.philosophicalDoctrine)

        _trilogyState.update { state ->
            val updatedDirectives = (listOf(directive) + state.pendingDirectives.filterNot { it.directiveId == directive.directiveId }).take(12)
            state.copy(
                effectiveDoctrine = resolvedDoctrine,
                guildGold = state.guildGold + directive.goldReward,
                guildReputation = state.guildReputation + directive.reputationReward,
                totalRupees = newTotalRupees,
                rupeeDenominations = updatedDenominations,
                pendingDirectives = updatedDirectives,
                wing2ChronicleQueue = updatedWing2Queue,
                wing3ActiveMissions = updatedWing3Missions,
                wing4DeskArtifacts = updatedWing4Artifacts,
                lastSyncStatusBanner = "Emitted Directive $winningDirectiveId (${directive.philosophicalDoctrine.uppercase()}) • +${directive.goldReward}G +${directive.rupeeReward}R • Synced to slot_01"
            )
        }

        // Non-blocking sync to /api/lfs/sync/state and auto-sync primary slot_01 to /api/lfs/sync/saves
        viewModelScope.launch {
            val stateAfter = _trilogyState.value
            val op = CrdtStateDeltaOp(
                opId = "op_${directive.directiveId}",
                fieldPath = "trilogy.directives.latest",
                ts = nowMs,
                replicaId = replicaId,
                valueJson = "{\"directiveId\":\"${directive.directiveId}\",\"doctrine\":\"${directive.philosophicalDoctrine}\",\"author\":\"${quote.author}\"}"
            )
            oasisServerClient.u2SyncStateDelta(
                u2BaseUrl = stateAfter.serverPreset.baseUrl,
                roomId = stateAfter.activeRoomCode,
                ops = listOf(op),
                directive = directive,
                stableNodeId = replicaId
            )
            sealAndSyncAegisSaveSlotInternal(slotKey = "slot_01", silentBanner = true)
        }
    }

    fun triggerWingInteractiveAction(wing: TrilogyWing) {
        val current = _trilogyState.value
        val replicaId = current.handshake.stableNodeId
        val nowMs = System.currentTimeMillis()

        when (wing) {
            TrilogyWing.WING_I_COMMONPLACE -> {
                emitFeaturedQuoteQuestDirective()
            }
            TrilogyWing.WING_II_SCRIPTORIUM_PART1 -> {
                val nextTask = "Chronicle-Synth #${(nowMs % 900) + 100} (Verified by $replicaId)"
                _trilogyState.update { state ->
                    state.copy(
                        guildGold = state.guildGold + 35,
                        guildReputation = state.guildReputation + 12,
                        wing2ChronicleQueue = (listOf(nextTask) + state.wing2ChronicleQueue).take(6),
                        lastSyncStatusBanner = "Wing II Scriptorium executed PowerBlock: $nextTask (+35G)"
                    )
                }
            }
            TrilogyWing.WING_III_CHAPTERHOUSE_PART2 -> {
                val doctrine = current.manualDoctrineOverride ?: current.effectiveDoctrine
                val newHp = maxOf(5, current.wing3RaidHpPercent - 8)
                val raidStatus = "Arch-oleth the Uncatalogued (HP $newHp%) — Struck with ${doctrine.label}"
                _trilogyState.update { state ->
                    state.copy(
                        guildGold = state.guildGold + 60,
                        guildReputation = state.guildReputation + 25,
                        wing3RaidHpPercent = newHp,
                        wing3RaidBossName = raidStatus,
                        lastSyncStatusBanner = "Wing III Chapterhouse Raid Strike: $raidStatus (+60G, +25Rep)"
                    )
                }
            }
            TrilogyWing.WING_IV_OBSIDIAN_DESK_PART3 -> {
                val newLore = "Obsidian Desk Folio #${(nowMs % 89) + 10}: Candlelit Bestiary Entry"
                _trilogyState.update { state ->
                    state.copy(
                        guildReputation = state.guildReputation + 18,
                        wing4DeskArtifacts = (listOf(newLore) + state.wing4DeskArtifacts).distinct().take(6),
                        lastSyncStatusBanner = "Wing IV 3D Mahogany Guild Desk inspected: $newLore (+18Rep)"
                    )
                }
            }
        }

        viewModelScope.launch {
            val updated = _trilogyState.value
            oasisServerClient.u2SyncStateDelta(
                u2BaseUrl = updated.serverPreset.baseUrl,
                roomId = updated.activeRoomCode,
                ops = listOf(
                    CrdtStateDeltaOp(
                        opId = "op_wing_${wing.slug}_$nowMs",
                        fieldPath = "trilogy.wing.${wing.slug}",
                        ts = nowMs,
                        replicaId = replicaId,
                        valueJson = "{\"wing\":\"${wing.slug}\",\"gold\":${updated.guildGold},\"rep\":${updated.guildReputation}}"
                    )
                ),
                stableNodeId = replicaId
            )
        }
    }

    fun sealAndSyncAegisSaveSlot(slotKey: String) {
        viewModelScope.launch {
            sealAndSyncAegisSaveSlotInternal(slotKey = slotKey, silentBanner = false)
        }
    }

    private suspend fun sealAndSyncAegisSaveSlotInternal(
        slotKey: String,
        silentBanner: Boolean
    ) {
        val current = _trilogyState.value
        val normalizedSlot = slotKey.trim().ifBlank { "slot_01" }
        val doctrine = current.manualDoctrineOverride ?: current.effectiveDoctrine
        val payloadJson = buildString {
            append("{")
            append("\"slotKey\":\"").append(normalizedSlot).append("\",")
            append("\"activeWing\":\"").append(current.activeWing.slug).append("\",")
            append("\"doctrine\":\"").append(doctrine.wireId).append("\",")
            append("\"guildGold\":").append(current.guildGold).append(",")
            append("\"guildReputation\":").append(current.guildReputation).append(",")
            append("\"totalRupees\":").append(current.totalRupees).append(",")
            append("\"directivesCount\":").append(current.pendingDirectives.size)
            append("}")
        }

        val syncRes = oasisServerClient.u2SyncSavesAegisSlot(
            u2BaseUrl = current.serverPreset.baseUrl,
            slotKey = normalizedSlot,
            stableNodeId = current.handshake.stableNodeId,
            subuserId = current.handshake.subuserId,
            generationEpoch = current.handshake.generationEpoch,
            abacStateBits = current.handshake.abacEvaluation.stateBits,
            replicateToAnchor = true,
            stateSnapshotJson = payloadJson
        )

        val nowMs = System.currentTimeMillis()
        _trilogyState.update { state ->
            val updatedSlots = state.saveSlots.map { slot ->
                if (slot.slotKey == normalizedSlot) {
                    slot.copy(
                        isOccupied = true,
                        updatedAtEpochMs = nowMs,
                        activeDoctrine = doctrine.wireId,
                        guildGold = state.guildGold,
                        totalRupees = state.totalRupees,
                        directiveCount = state.pendingDirectives.size,
                        sealedEnvelopePreview = syncRes.toString().take(48),
                        replicatedToAnchor = true
                    )
                } else {
                    slot
                }
            }
            state.copy(
                selectedSaveSlotKey = normalizedSlot,
                saveSlots = updatedSlots,
                lastSyncStatusBanner = if (silentBanner) {
                    state.lastSyncStatusBanner
                } else {
                    "Sealed 6-Gate aegisEnvelopeV1 into $normalizedSlot (replicateToAnchor=true)"
                }
            )
        }
    }

    fun sendRmblynOverlayCommand(rawCommand: String) {
        val clean = rawCommand.trim().ifBlank { "/trilogy status" }
        viewModelScope.launch {
            val current = _trilogyState.value
            val u2Resp = oasisServerClient.u2SyncRmblyn(
                u2BaseUrl = current.serverPreset.baseUrl,
                channelId = "#commonplace-codex",
                rawInput = clean,
                stableNodeId = current.handshake.stableNodeId
            )
            val u3Resp = oasisServerClient.u3OverlayRmblyn(
                u3BaseUrl = LfsServerPreset.TAILSCALE_LAPUTA_U3.baseUrl,
                channelId = "#commonplace-codex",
                rawInput = clean,
                stableNodeId = current.handshake.stableNodeId
            )
            _trilogyState.update { state ->
                state.copy(
                    lastRmblynCommandResult = "U2: ${u2Resp.optString("rawInput", clean)} • U3: ${u3Resp.optString("echo", clean)}",
                    lastSyncStatusBanner = "Rmblyn Dispatched (“$clean”) to /api/lfs/sync/rmblyn & /v1/overlay/rmblyn"
                )
            }
        }
    }
}

typealias QuoteDiscoveryViewModel = CommonplaceViewModel
