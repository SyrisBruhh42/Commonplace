package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.ImageSearch
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.auth.LoginScreen
import com.example.auth.configureAppCheck
import com.example.auth.signOutUser
import com.example.data.FamilyCircle
import com.example.data.GeminiQuoteAiService
import com.example.data.LocationStampService
import com.example.data.QuoteItem
import com.example.data.QuoteRepository
import com.example.network.NetworkHealthService
import com.example.network.QuoteAudioAndVoiceService
import com.example.ui.AccessibilityAndQolSheet
import com.example.ui.CommonplaceViewModel
import com.example.ui.FamilyCircleScreen
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import com.example.ui.LibraryScreen
import com.example.ui.MapDiscoveryScreen
import com.example.ui.OasisMultiplayerBar
import com.example.ui.QuestTrilogyCodexSheet
import com.example.ui.QuoteDiscoveryScreen
import com.example.ui.QuoteDossierSheet
import com.example.ui.SymposiumChatScreen
import com.example.ui.UiState
import com.example.ui.VisualMuseScreen
import com.example.ui.theme.CommonplaceTheme
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // STEP 1: Configure App Check with launch intent before any UI, ViewModels, or Firebase operations
        configureAppCheck(this, intent)

        enableEdgeToEdge()

        setContent {
            CommonplaceTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation()
                }
            }
        }
    }
}

internal fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { auth ->
        trySend(auth.currentUser)
    }
    addAuthStateListener(listener)
    awaitClose { removeAuthStateListener(listener) }
}

@Composable
fun AppNavigation(auth: FirebaseAuth = Firebase.auth) {
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    val user = currentUser

    if (user == null) {
        LoginScreen(
            onAuthSuccess = { /* AuthStateListener updates currentUser automatically */ }
        )
    } else {
        CommonplaceAppShell(
            currentUserId = user.uid,
            userEmail = user.email ?: "Reader"
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommonplaceAppShell(
    currentUserId: String,
    userEmail: String
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val viewModel: CommonplaceViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                CommonplaceViewModel(
                    repository = QuoteRepository(db),
                    aiService = GeminiQuoteAiService(),
                    locationStampService = LocationStampService(app),
                    currentUserId = currentUserId,
                    networkHealthService = NetworkHealthService(app),
                    audioVoiceService = QuoteAudioAndVoiceService(app)
                )
            }
        }
    )

    val discoverState by viewModel.discoverState.collectAsStateWithLifecycle()
    val savedQuotesState by viewModel.savedQuotesState.collectAsStateWithLifecycle()
    val familyCirclesState by viewModel.familyCirclesState.collectAsStateWithLifecycle()
    val familySharedQuotesState by viewModel.familySharedQuotesState.collectAsStateWithLifecycle()
    val dossierState by viewModel.dossierState.collectAsStateWithLifecycle()
    val photoMatchState by viewModel.photoMatchState.collectAsStateWithLifecycle()
    val salonSessionState by viewModel.salonSessionState.collectAsStateWithLifecycle()
    val quoteAudioSceneState by viewModel.quoteAudioSceneState.collectAsStateWithLifecycle()
    val voiceTranscriptionState by viewModel.voiceTranscriptionState.collectAsStateWithLifecycle()
    val accessibilitySettings by viewModel.accessibilitySettings.collectAsStateWithLifecycle()
    val quoteAnnotationsByQuoteId by viewModel.quoteAnnotationsByQuoteId.collectAsStateWithLifecycle()
    val pdfExportStatusMessage by viewModel.pdfExportStatusMessage.collectAsStateWithLifecycle()
    val dailyQuoteScheduleState by viewModel.dailyQuoteScheduleState.collectAsStateWithLifecycle()
    val worldMapState by viewModel.worldMapState.collectAsStateWithLifecycle()
    val trilogyState by viewModel.trilogyState.collectAsStateWithLifecycle()
    val isGeneratingMomentQuote by viewModel.isGeneratingMomentQuote.collectAsStateWithLifecycle()
    val chatMessages by viewModel.chatMessages.collectAsState()

    LaunchedEffect(currentUserId) {
        viewModel.initializeDailyScheduleAndWidget(context)
    }

    val availableFamilyCircles: List<FamilyCircle> =
        (familyCirclesState as? UiState.Success)?.data ?: emptyList()
    val savedQuotesList = (savedQuotesState as? UiState.Success)?.data ?: emptyList()

    var currentTab by rememberSaveable { mutableIntStateOf(0) }
    var attachedChatQuote by remember { mutableStateOf<QuoteItem?>(null) }
    var showAccessibilitySheet by remember { mutableStateOf(false) }
    var showTrilogyCodexSheet by remember { mutableStateOf(false) }

    // BackHandler on secondary tabs returns to QuoteDiscoveryScreen home tab
    BackHandler(enabled = currentTab != 0) {
        currentTab = 0
    }

    CommonplaceTheme(
        darkTheme = accessibilitySettings.highContrastDarkPreferred,
        accessibilitySettings = accessibilitySettings
    ) {
        Scaffold(
            modifier = Modifier.onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown) {
                    val isShiftTab = event.isShiftPressed && event.key == Key.Tab
                    val isCtrlK = event.isCtrlPressed && event.key == Key.K
                    if (isShiftTab || isCtrlK) {
                        val trigger = if (isShiftTab) "oasis:overlay-toggle (Shift+Tab)" else "oasis:overlay-toggle (Ctrl+K)"
                        viewModel.toggleOasisOverlay(trigger)
                        return@onPreviewKeyEvent true
                    }
                }
                false
            },
            contentWindowInsets = WindowInsets.safeDrawing,
            topBar = {
                Column {
                    OasisMultiplayerBar(
                        envelope = trilogyState,
                        onSelectHallDestination = viewModel::selectOasisHallDestination,
                        onToggleOasisOverlay = viewModel::toggleOasisOverlay,
                        onToggleZt9ComfortMode = viewModel::toggleZt9ComfortOverrides,
                        onSelectRmblynChannel = viewModel::selectRmblynChannel,
                        onSendRmblynChatMessage = viewModel::sendRmblynFamilyChatMessage,
                        onToggleStorytellerPlayPause = viewModel::toggleSanctuaryStorytellerPlayPause,
                        onSelectStorytellerChapter = viewModel::selectSanctuaryChapter,
                        onSetSleepFadeTimerMinutes = viewModel::setStarlightSleepFadeTimer,
                        onOpenCodexDrawer = { showTrilogyCodexSheet = true }
                    )
                    TopAppBar(
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.FormatQuote,
                                    contentDescription = "Commonplace logo",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(26.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = stringResource(R.string.app_name),
                                        style = MaterialTheme.typography.headlineSmall
                                    )
                                    Text(
                                        text = userEmail,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        },
                        actions = {
                            AssistChip(
                                onClick = { showTrilogyCodexSheet = true },
                                label = {
                                    Text(
                                        text = trilogyState.activeHallDestination.displayLabel,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Hub,
                                        contentDescription = "Open Quest Trilogy Codex & TrashArcade Bridge",
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                modifier = Modifier.testTag("top_bar_trilogy_codex_button")
                            )
                            IconButton(
                                onClick = { currentTab = 5 },
                                modifier = Modifier.testTag("top_bar_visual_muse_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ImageSearch,
                                    contentDescription = stringResource(R.string.tab_visual_muse)
                                )
                            }
                            AssistChip(
                                onClick = { viewModel.toggleHighContrastMode() },
                                label = {
                                    Text(
                                        if (accessibilitySettings.highContrastEnabled) "AAA ON" else "Contrast",
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Contrast,
                                        contentDescription = "Toggle High Contrast Mode",
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                modifier = Modifier.testTag("quick_high_contrast_chip")
                            )
                            IconButton(
                                onClick = { showAccessibilitySheet = true },
                                modifier = Modifier.testTag("accessibility_mode_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AccessibilityNew,
                                    contentDescription = "High Contrast & Accessibility Settings"
                                )
                            }
                            IconButton(
                                onClick = {
                                    signOutUser(
                                        context = context,
                                        onSignOutComplete = {},
                                        scope = scope
                                    )
                                },
                                modifier = Modifier.testTag("sign_out_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Logout,
                                    contentDescription = stringResource(R.string.sign_out_label)
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.background,
                            titleContentColor = MaterialTheme.colorScheme.onBackground
                        )
                    )
                }
            },
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    NavigationBarItem(
                        selected = currentTab == 0,
                        onClick = { currentTab = 0 },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = stringResource(R.string.tab_discover)
                            )
                        },
                        label = { Text(stringResource(R.string.tab_discover)) },
                        modifier = Modifier.testTag("nav_tab_discover")
                    )
                    NavigationBarItem(
                        selected = currentTab == 1,
                        onClick = { currentTab = 1 },
                        icon = {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.LibraryBooks,
                                contentDescription = stringResource(R.string.tab_library)
                            )
                        },
                        label = { Text(stringResource(R.string.tab_library)) },
                        modifier = Modifier.testTag("nav_tab_library")
                    )
                    NavigationBarItem(
                        selected = currentTab == 2,
                        onClick = { currentTab = 2 },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Map,
                                contentDescription = stringResource(R.string.tab_world_map)
                            )
                        },
                        label = { Text(stringResource(R.string.tab_world_map)) },
                        modifier = Modifier.testTag("nav_tab_map")
                    )
                    NavigationBarItem(
                        selected = currentTab == 3,
                        onClick = { currentTab = 3 },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Groups,
                                contentDescription = "Family Circle"
                            )
                        },
                        label = { Text("Family") },
                        modifier = Modifier.testTag("nav_tab_family")
                    )
                    NavigationBarItem(
                        selected = currentTab == 4,
                        onClick = { currentTab = 4 },
                        icon = {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Chat,
                                contentDescription = stringResource(R.string.tab_symposium)
                            )
                        },
                        label = { Text(stringResource(R.string.tab_symposium)) },
                        modifier = Modifier.testTag("nav_tab_symposium")
                    )
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (currentTab) {
                    0 -> QuoteDiscoveryScreen(
                        state = discoverState,
                        familyCircles = availableFamilyCircles,
                        audioState = quoteAudioSceneState,
                        dailyScheduleState = dailyQuoteScheduleState,
                        onToggleInterest = viewModel::toggleInterestFilter,
                        onSelectPriorityMode = viewModel::setPriorityMode,
                        onNextPriorityQuote = viewModel::nextPriorityQuote,
                        onGenerateAiQuote = viewModel::generateAiPriorityQuote,
                        onQuickTakeaway = viewModel::generateQuickTakeawayForCurrentQuote,
                        onSaveQuoteWithStamp = viewModel::saveQuoteWithStamp,
                        onShareToFamilyCircle = viewModel::shareQuoteWithFamilyCircle,
                        onOpenDossier = viewModel::openQuoteDossier,
                        onDiscussInChat = { quote ->
                            attachedChatQuote = quote
                            currentTab = 4
                        },
                        onRefreshLocationStamp = viewModel::refreshPreviewStamp,
                        onDismissStatusBanner = viewModel::clearStatusBanner,
                        onReadAloud = viewModel::readQuoteAloud,
                        onGenerate30sMusicScene = viewModel::generateAndPlay30sMusicScene,
                        onStopAudio = viewModel::stopQuoteAudio,
                        onToggleDailyQuoteNotifications = { enabled ->
                            viewModel.toggleDailyQuoteNotifications(context, enabled)
                        },
                        onUpdateDailyNotificationTime = { hour, min ->
                            viewModel.updateDailyNotificationTime(context, hour, min)
                        },
                        onTogglePriorityEngineForDailyPush = { usePriority ->
                            viewModel.toggleUsePriorityEngineForDailyPush(context, usePriority)
                        },
                        onSendTestDailyQuoteNotification = {
                            viewModel.sendTestDailyQuoteNotificationNow(context)
                        },
                        onSyncAndPinHomeScreenWidget = {
                            viewModel.syncAndRequestPinHomeWidget(context)
                        }
                    )
                    1 -> LibraryScreen(
                        savedQuotesState = savedQuotesState,
                        rankedCatalogQuotes = discoverState.rankedQuotes,
                        audioState = quoteAudioSceneState,
                        transcriptionState = voiceTranscriptionState,
                        quoteAnnotationsByQuoteId = quoteAnnotationsByQuoteId,
                        pdfExportStatusMessage = pdfExportStatusMessage,
                        onSaveCatalogQuote = { quote ->
                            viewModel.saveQuoteWithStamp(quote, null, "Saved from Quote Library")
                        },
                        onUpdateNote = viewModel::updateSavedQuoteNote,
                        onDeleteSavedQuote = viewModel::deleteSavedQuote,
                        onOpenDossier = viewModel::openQuoteDossier,
                        onDiscussInChat = { quote ->
                            attachedChatQuote = quote
                            currentTab = 4
                        },
                        onReadAloud = viewModel::readQuoteAloud,
                        onGenerate30sMusicScene = viewModel::generateAndPlay30sMusicScene,
                        onStopAudio = viewModel::stopQuoteAudio,
                        onToggleVoiceDictation = viewModel::toggleVoiceDictation,
                        onAddHighlightAnnotation = viewModel::addHighlightAnnotation,
                        onDeleteHighlightAnnotation = viewModel::deleteHighlightAnnotation,
                        onExportJournalPdf = { doc, theme ->
                            viewModel.exportSavedQuoteJournalPdf(context, doc, theme)
                        },
                        onPdfSavedToUserUri = viewModel::notifyPdfSavedToUserUri,
                        onDismissPdfStatusBanner = viewModel::clearPdfExportStatusMessage
                    )
                    2 -> MapDiscoveryScreen(
                        mapState = worldMapState,
                        featuredQuote = discoverState.currentScoredQuote?.quote
                            ?: discoverState.dailyQuoteFromCollection,
                        catalogQuotes = discoverState.allCatalogQuotes,
                        familyCircles = availableFamilyCircles,
                        audioState = quoteAudioSceneState,
                        onSelectPin = viewModel::selectMapPin,
                        onSelectFilterMode = viewModel::selectMapFilterMode,
                        onPinDailyQuoteToMap = viewModel::pinDailyQuoteToWorldMap,
                        onRefreshGpsLocation = viewModel::refreshWorldMapGpsLocation,
                        onConnectFriendEmail = viewModel::connectMapFriendByEmail,
                        onSavePinnedQuoteToArchive = viewModel::saveMapPinQuoteToArchive,
                        onDismissStatusBanner = viewModel::dismissWorldMapStatusBanner,
                        onToggleGlobalMapVisibility = viewModel::toggleGlobalMapVisibility,
                        onToggleTrustedConnectionsOnly = viewModel::toggleTrustedConnectionsOnlyFilter,
                        onUpdateFriendSharingPermission = viewModel::updateFriendSharingPermission,
                        onTogglePrivacyOverlay = viewModel::togglePrivacyOverlay,
                        onReadAloud = viewModel::readQuoteAloud,
                        onGenerate30sMusicScene = viewModel::generateAndPlay30sMusicScene,
                        onStopAudio = viewModel::stopQuoteAudio
                    )
                    3 -> FamilyCircleScreen(
                        currentUserEmail = userEmail,
                        currentUserId = currentUserId,
                        familyCirclesState = familyCirclesState,
                        familySharedQuotesState = familySharedQuotesState,
                        currentFeaturedQuote = discoverState.currentScoredQuote?.quote
                            ?: discoverState.dailyQuoteFromCollection,
                        onCreateCircle = viewModel::createFamilyCircle,
                        onInviteGoogleMember = { circle, email ->
                            viewModel.inviteMemberToFamilyCircle(circle, email)
                        },
                        onSyncGoogleInvites = viewModel::syncGoogleEmailCircleInvites,
                        onShareFeaturedQuote = viewModel::shareQuoteWithFamilyCircle,
                        onSaveSharedQuoteToPersonalArchive = { shared ->
                            val domainItem = QuoteItem(
                                id = shared.id.ifBlank { "shared_family_item" },
                                text = shared.text,
                                author = shared.author,
                                sourceWork = "Shared in ${shared.circleName} by ${shared.senderName}",
                                category = shared.tags.firstOrNull() ?: "Philosophy",
                                tags = shared.tags,
                                onlinePopularityScore = 95,
                                popularOnlineContext = "Shared in Family Social Circle ${shared.circleName}"
                            )
                            viewModel.saveQuoteWithStamp(
                                quote = domainItem,
                                customLocationOverride = shared.timestampInfo.location,
                                personalNote = "From ${shared.senderName} (${shared.circleName}): ${shared.personalNote}"
                            )
                        },
                        salonState = salonSessionState,
                        onSelectGameMode = viewModel::setMultiplayerGameMode,
                        onSelectTransportMode = viewModel::setSalonTransportMode,
                        onSelectHostMode = viewModel::setLfsHostMode,
                        onSelectLifecyclePolicy = viewModel::setLfsLifecyclePolicy,
                        onToggleAiScholar = viewModel::toggleAiScholar,
                        onLaunchOrJoinRoom = viewModel::launchOrJoinSalonRoom,
                        onLeaveRoom = viewModel::leaveSalonRoom,
                        onPostMarginalia = viewModel::postLiveMarginalia,
                        onSubmitTriviaAnswer = viewModel::submitTriviaAnswer,
                        onAdvanceTriviaRound = viewModel::advanceTriviaRound,
                        onSealFolioEnvelope = viewModel::sealCurrentQuoteFolioEnvelope,
                        onOpenFolioEnvelope = viewModel::openFolioEnvelope,
                        onRefreshTailscale = { viewModel.refreshTailscaleDetection() },
                        audioState = quoteAudioSceneState,
                        transcriptionState = voiceTranscriptionState,
                        onReadAloud = viewModel::readQuoteAloud,
                        onGenerate30sMusicScene = viewModel::generateAndPlay30sMusicScene,
                        onStopAudio = viewModel::stopQuoteAudio,
                        onToggleVoiceDictation = viewModel::toggleVoiceDictation
                    )
                    4 -> SymposiumChatScreen(
                        messages = chatMessages,
                        savedQuotes = savedQuotesList,
                        attachedQuote = attachedChatQuote,
                        isGeneratingMomentQuote = isGeneratingMomentQuote,
                        onSelectSavedQuote = { attachedChatQuote = it },
                        onClearAttachedQuote = { attachedChatQuote = null },
                        onSendMessage = viewModel::sendSymposiumMessage,
                        onGenerateInTheMomentQuote = viewModel::generateInTheMomentQuoteInChat,
                        onSaveGeneratedQuote = { quote ->
                            viewModel.saveQuoteWithStamp(quote, null, "Forged in the Moment in Symposium")
                        },
                        onOpenDossier = viewModel::openQuoteDossier,
                        audioState = quoteAudioSceneState,
                        transcriptionState = voiceTranscriptionState,
                        onReadAloud = viewModel::readQuoteAloud,
                        onGenerate30sMusicScene = viewModel::generateAndPlay30sMusicScene,
                        onStopAudio = viewModel::stopQuoteAudio,
                        onToggleVoiceDictation = viewModel::toggleVoiceDictation
                    )
                    5 -> VisualMuseScreen(
                        photoMatchState = photoMatchState,
                        onAnalyzeBitmap = viewModel::analyzeImageForQuotes,
                        onSaveMatchedQuote = { quote ->
                            viewModel.saveQuoteWithStamp(quote, null, "Matched via Visual Muse photo analysis")
                        },
                        onOpenDossier = viewModel::openQuoteDossier,
                        audioState = quoteAudioSceneState,
                        onReadAloud = viewModel::readQuoteAloud,
                        onGenerate30sMusicScene = viewModel::generateAndPlay30sMusicScene,
                        onStopAudio = viewModel::stopQuoteAudio
                    )
                }
            }
        }

        if (dossierState.targetQuote != null) {
            val target = dossierState.targetQuote!!
            QuoteDossierSheet(
                dossierState = dossierState,
                onDismiss = viewModel::closeQuoteDossier,
                onRequestDeepMeaning = { viewModel.triggerDeepMeaningAnalysis(target) },
                onRequestSearchGrounding = { viewModel.triggerSearchGroundedContext(target) },
                onRequestMapsGrounding = { viewModel.triggerMapsGroundedPlaces(target) },
                audioState = quoteAudioSceneState,
                onReadAloud = viewModel::readQuoteAloud,
                onGenerate30sMusicScene = viewModel::generateAndPlay30sMusicScene,
                onStopAudio = viewModel::stopQuoteAudio
            )
        }

        if (showAccessibilitySheet) {
            AccessibilityAndQolSheet(
                settings = accessibilitySettings,
                onUpdateSettings = viewModel::updateAccessibilitySettings,
                onDismiss = { showAccessibilitySheet = false }
            )
        }

        if (showTrilogyCodexSheet) {
            QuestTrilogyCodexSheet(
                envelope = trilogyState,
                onSelectWing = viewModel::selectTrilogyWing,
                onTriggerWingAction = viewModel::triggerWingInteractiveAction,
                onSelectPresetAndProbe = viewModel::selectServerPresetAndProbeHandshake,
                onSetDoctrineOverride = viewModel::setManualDoctrineOverride,
                onEmitFeaturedQuoteDirective = viewModel::emitFeaturedQuoteQuestDirective,
                onSyncAegisSaveSlot = viewModel::sealAndSyncAegisSaveSlot,
                onSendRmblynCommand = viewModel::sendRmblynOverlayCommand,
                onDismiss = { showTrilogyCodexSheet = false }
            )
        }
    }
}

