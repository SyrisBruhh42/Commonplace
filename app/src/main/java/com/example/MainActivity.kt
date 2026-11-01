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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.ImageSearch
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
import com.example.ui.CommonplaceViewModel
import com.example.ui.FamilyCircleScreen
import com.example.ui.LibraryScreen
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
                    currentUserId = currentUserId
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
    val chatMessages by viewModel.chatMessages.collectAsState()

    val availableFamilyCircles: List<FamilyCircle> =
        (familyCirclesState as? UiState.Success)?.data ?: emptyList()

    var currentTab by rememberSaveable { mutableIntStateOf(0) }
    var attachedChatQuote by remember { mutableStateOf<QuoteItem?>(null) }

    // BackHandler on secondary tabs returns to QuoteDiscoveryScreen home tab
    BackHandler(enabled = currentTab != 0) {
        currentTab = 0
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
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
                            imageVector = Icons.Default.Groups,
                            contentDescription = "Family Circle"
                        )
                    },
                    label = { Text("Family") },
                    modifier = Modifier.testTag("nav_tab_family")
                )
                NavigationBarItem(
                    selected = currentTab == 3,
                    onClick = { currentTab = 3 },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.ImageSearch,
                            contentDescription = stringResource(R.string.tab_visual_muse)
                        )
                    },
                    label = { Text(stringResource(R.string.tab_visual_muse)) },
                    modifier = Modifier.testTag("nav_tab_visual_muse")
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
                    onDismissStatusBanner = viewModel::clearStatusBanner
                )
                1 -> LibraryScreen(
                    savedQuotesState = savedQuotesState,
                    rankedCatalogQuotes = discoverState.rankedQuotes,
                    onSaveCatalogQuote = { quote ->
                        viewModel.saveQuoteWithStamp(quote, null, "Saved from Quote Library")
                    },
                    onUpdateNote = viewModel::updateSavedQuoteNote,
                    onDeleteSavedQuote = viewModel::deleteSavedQuote,
                    onOpenDossier = viewModel::openQuoteDossier,
                    onDiscussInChat = { quote ->
                        attachedChatQuote = quote
                        currentTab = 4
                    }
                )
                2 -> FamilyCircleScreen(
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
                    }
                )
                3 -> VisualMuseScreen(
                    photoMatchState = photoMatchState,
                    onAnalyzeBitmap = viewModel::analyzeImageForQuotes,
                    onSaveMatchedQuote = { quote ->
                        viewModel.saveQuoteWithStamp(quote, null, "Matched via Visual Muse photo analysis")
                    },
                    onOpenDossier = viewModel::openQuoteDossier
                )
                4 -> SymposiumChatScreen(
                    messages = chatMessages,
                    attachedQuote = attachedChatQuote,
                    onClearAttachedQuote = { attachedChatQuote = null },
                    onSendMessage = viewModel::sendSymposiumMessage
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
            onRequestMapsGrounding = { viewModel.triggerMapsGroundedPlaces(target) }
        )
    }
}
