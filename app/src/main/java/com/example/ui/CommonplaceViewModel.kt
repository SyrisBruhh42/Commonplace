package com.example.ui

import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.FamilyCircle
import com.example.data.FamilySharedQuote
import com.example.data.GeminiQuoteAiService
import com.example.data.LocationStampService
import com.example.data.PhotoMatchedQuoteResult
import com.example.data.PriorityMode
import com.example.data.PriorityScoredQuote
import com.example.data.Quote
import com.example.data.QuoteItem
import com.example.data.QuoteRepository
import com.example.data.SavedQuoteDocument
import com.example.data.SeedQuoteCatalog
import kotlinx.coroutines.CancellationException
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
    val attachedQuote: QuoteItem? = null
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
    private val currentUserId: String
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

    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(
        listOf(
            ChatMessage(
                isUser = false,
                text = "Welcome to the Commonplace Symposium. Ask me to unpack the meaning of any quote, compare philosophical traditions, or suggest quotes for a specific moment in your life."
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

    // Symposium Multi-Turn Chat
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
            "Regarding the quote \"${attachedQuote.text}\" by ${attachedQuote.author} (${attachedQuote.sourceWork}): $trimmed"
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
}

typealias QuoteDiscoveryViewModel = CommonplaceViewModel
