package com.example.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.QuoteAudioSceneState
import com.example.data.QuoteItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuoteDossierSheet(
    dossierState: DossierUiState,
    onDismiss: () -> Unit,
    onRequestDeepMeaning: () -> Unit,
    onRequestSearchGrounding: () -> Unit,
    onRequestMapsGrounding: () -> Unit,
    audioState: QuoteAudioSceneState = QuoteAudioSceneState(),
    onReadAloud: (QuoteItem) -> Unit = {},
    onGenerate30sMusicScene: (QuoteItem) -> Unit = {},
    onStopAudio: () -> Unit = {}
) {
    val quote = dossierState.targetQuote ?: return
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedTab by remember { mutableIntStateOf(0) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Text(
                text = "LITERARY & HISTORICAL DOSSIER",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "“${quote.text}”",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "— ${quote.author} (${quote.sourceWork})",
                style = MaterialTheme.typography.titleSmall.copy(fontStyle = FontStyle.Italic),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            QuoteAudioSceneControlBar(
                quote = quote,
                audioState = audioState,
                onReadAloud = onReadAloud,
                onGenerate30sMusicScene = onGenerate30sMusicScene,
                onStopAudio = onStopAudio,
                testTagPrefix = "dossier_quote_${quote.id}"
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 3 Specialized AI Exploration Modes
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedTab == 0,
                    onClick = {
                        selectedTab = 0
                        if (dossierState.deepMeaningState == null) onRequestDeepMeaning()
                    },
                    label = { Text("High Thinking") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Psychology,
                            contentDescription = "High thinking analysis",
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    modifier = Modifier.testTag("dossier_tab_thinking")
                )

                FilterChip(
                    selected = selectedTab == 1,
                    onClick = {
                        selectedTab = 1
                        if (dossierState.searchGroundedState == null) onRequestSearchGrounding()
                    },
                    label = { Text("Search Grounding") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Public,
                            contentDescription = "Google Search grounding",
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    modifier = Modifier.testTag("dossier_tab_search")
                )

                FilterChip(
                    selected = selectedTab == 2,
                    onClick = {
                        selectedTab = 2
                        if (dossierState.mapsGroundedState == null) onRequestMapsGrounding()
                    },
                    label = { Text("Maps Grounding") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Map,
                            contentDescription = "Google Maps grounding",
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    modifier = Modifier.testTag("dossier_tab_maps")
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            when (selectedTab) {
                0 -> {
                    DossierSectionCard(
                        badgeText = "gemini-4-argon • ThinkingLevel.HIGH",
                        title = "Deep Philosophical Breakdown & Historical Backstory",
                        state = dossierState.deepMeaningState,
                        onRetry = onRequestDeepMeaning
                    )
                }
                1 -> {
                    DossierSectionCard(
                        badgeText = "gemini-3.8-flash • Google Search Grounded",
                        title = "Popular Online Uses & Attribution Fact-Check",
                        state = dossierState.searchGroundedState,
                        onRetry = onRequestSearchGrounding
                    )
                }
                2 -> {
                    DossierSectionCard(
                        badgeText = "gemini-3.8-flash • Google Maps Grounded",
                        title = "Literary Landmarks, Museums & Geographic History",
                        state = dossierState.mapsGroundedState,
                        onRetry = onRequestMapsGrounding
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}

@Composable
private fun DossierSectionCard(
    badgeText: String,
    title: String,
    state: UiState<String>?,
    onRetry: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = badgeText,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }

    Spacer(modifier = Modifier.height(8.dp))

    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground
    )

    Spacer(modifier = Modifier.height(12.dp))

    when (state) {
        null -> {
            Button(onClick = onRetry) {
                Text("Generate Analysis")
            }
        }
        is UiState.Loading -> {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(14.dp))
                    Text(
                        text = "Synthesizing archival dossier…",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
        is UiState.Error -> {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = state.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = onRetry) {
                        Text("Retry")
                    }
                }
            }
        }
        is UiState.Success -> {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = state.data,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}
