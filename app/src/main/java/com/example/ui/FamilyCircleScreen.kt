package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.CommonplaceTransportMode
import com.example.data.FamilyCircle
import com.example.data.FamilySharedQuote
import com.example.data.LfsHostMode
import com.example.data.LfsLifecyclePolicy
import com.example.data.MultiplayerGameMode
import com.example.data.MultiplayerSalonSessionState
import com.example.data.QuoteAudioSceneState
import com.example.data.QuoteItem
import com.example.data.VoiceTranscriptionState
import com.example.data.VoiceTranscriptionTarget
import com.example.network.OasisCommonplaceServerClient
import com.example.ui.theme.LocalAccessibilitySettings

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FamilyCircleScreen(
    currentUserEmail: String,
    currentUserId: String,
    familyCirclesState: UiState<List<FamilyCircle>>,
    familySharedQuotesState: UiState<List<FamilySharedQuote>>,
    currentFeaturedQuote: QuoteItem?,
    onCreateCircle: (String, String) -> Unit,
    onInviteGoogleMember: (FamilyCircle, String) -> Unit,
    onSyncGoogleInvites: () -> Unit,
    onShareFeaturedQuote: (FamilyCircle, QuoteItem, String) -> Unit,
    onSaveSharedQuoteToPersonalArchive: (FamilySharedQuote) -> Unit,
    salonState: MultiplayerSalonSessionState = MultiplayerSalonSessionState(),
    onSelectGameMode: (MultiplayerGameMode) -> Unit = {},
    onSelectTransportMode: (CommonplaceTransportMode) -> Unit = {},
    onSelectHostMode: (LfsHostMode) -> Unit = {},
    onSelectLifecyclePolicy: (LfsLifecyclePolicy) -> Unit = {},
    onToggleAiScholar: (String) -> Unit = {},
    onLaunchOrJoinRoom: (String?, String?) -> Unit = { _, _ -> },
    onLeaveRoom: () -> Unit = {},
    onPostMarginalia: (String, String, String) -> Unit = { _, _, _ -> },
    onSubmitTriviaAnswer: (Int) -> Unit = {},
    onAdvanceTriviaRound: () -> Unit = {},
    onSealFolioEnvelope: (String) -> Unit = {},
    onOpenFolioEnvelope: (String, String) -> Unit = { _, _ -> },
    onRefreshTailscale: () -> Unit = {},
    audioState: QuoteAudioSceneState = QuoteAudioSceneState(),
    transcriptionState: VoiceTranscriptionState = VoiceTranscriptionState(),
    onReadAloud: (QuoteItem) -> Unit = {},
    onGenerate30sMusicScene: (QuoteItem) -> Unit = {},
    onStopAudio: () -> Unit = {},
    onToggleVoiceDictation: (VoiceTranscriptionTarget, (String) -> Unit) -> Unit = { _, _ -> }
) {
    val a11y = LocalAccessibilitySettings.current
    var showCreateCircleDialog by remember { mutableStateOf(false) }
    var circleForInvite by remember { mutableStateOf<FamilyCircle?>(null) }
    var roomCodeInput by remember { mutableStateOf("") }
    var roomTitleInput by remember { mutableStateOf("The Archival Co-Reading Salon") }
    var marginaliaInput by remember { mutableStateOf("") }
    var showFolioCipherModal by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("family_circle_screen"),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 640.dp)
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Zero-Trust Google Connection Header Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (a11y.highContrastEnabled) {
                            MaterialTheme.colorScheme.secondaryContainer
                        } else {
                            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.75f)
                        }
                    ),
                    border = BorderStroke(
                        if (a11y.highContrastEnabled) 2.5.dp else 1.dp,
                        if (a11y.highContrastEnabled) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.VerifiedUser,
                                contentDescription = "Zero-Trust Google Connection",
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(26.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Family Social Circle • Zero-Trust Sharing",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Text(
                                    text = "Connected via Google: $currentUserEmail",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.85f)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Every shared quote is protected by Zero-Trust Firestore Security Rules: only verified Google accounts explicitly listed in a circle's member list can read or share stamped quotes.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = { showCreateCircleDialog = true },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("create_family_circle_button"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "New Family Circle",
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("New Family Circle")
                            }

                            OutlinedButton(
                                onClick = onSyncGoogleInvites,
                                modifier = Modifier.testTag("sync_google_invites_button"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Sync,
                                    contentDescription = "Sync Google Invites",
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Sync Invites")
                            }
                        }
                    }
                }
            }

            // Live Multiplayer Literary Salon & Trivia Duel Hub Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("multiplayer_salon_hub_card"),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (a11y.highContrastEnabled) {
                            MaterialTheme.colorScheme.surface
                        } else {
                            Color(0xFFF6ECDA)
                        }
                    ),
                    border = BorderStroke(
                        if (a11y.highContrastEnabled) 2.5.dp else 1.2.dp,
                        if (a11y.highContrastEnabled) MaterialTheme.colorScheme.outline else Color(0xFFC8A96E)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(
                                    imageVector = Icons.Default.Hub,
                                    contentDescription = "Multiplayer Literary Salon",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "Live Literary Salon & Trivia Duel",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = salonState.activeTierBadge,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            OutlinedButton(
                                onClick = { showFolioCipherModal = true },
                                modifier = Modifier.testTag("open_cpf1_cipher_button"),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Key,
                                    contentDescription = "CPF1 Folio Cipher",
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("CPF1 Seal", style = MaterialTheme.typography.labelSmall)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // SECRET TAILSCALE GATE: Only render the LFS Oasis Server Control Plane if a valid Tailscale connection is detected!
                        AnimatedVisibility(visible = salonState.isTailscaleDetected) {
                            Surface(
                                color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.85f),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 10.dp)
                                    .testTag("tailscale_lfs_server_panel")
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.VpnKey,
                                            contentDescription = "Tailscale Mesh Verified",
                                            tint = MaterialTheme.colorScheme.tertiary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "TAILSCALE MESH DETECTED (${salonState.tailscaleStatus.tailnetIp}) • LFS SERVER UNLOCKED",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onTertiaryContainer
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    Text(
                                        text = "Hosting Mode (play.lewisfamilysystems.com / sync.lewisfamilysystems.com):",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer
                                    )
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        LfsHostMode.entries.forEach { mode ->
                                            FilterChip(
                                                selected = salonState.hostMode == mode,
                                                onClick = { onSelectHostMode(mode) },
                                                label = { Text(mode.label, style = MaterialTheme.typography.labelSmall) }
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Text(
                                        text = "LFS Room Lifecycle Policy:",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer
                                    )
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        LfsLifecyclePolicy.entries.forEach { policy ->
                                            FilterChip(
                                                selected = salonState.lifecyclePolicy == policy,
                                                onClick = { onSelectLifecyclePolicy(policy) },
                                                label = { Text(policy.label, style = MaterialTheme.typography.labelSmall) }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Game Mode Selector (Co-Reading Salon default, Literary Trivia Duel, Symposium Roundtable)
                        Text(
                            text = "1. Select Salon Experience:",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            MultiplayerGameMode.entries.forEach { mode ->
                                FilterChip(
                                    selected = salonState.activeGameMode == mode,
                                    onClick = { onSelectGameMode(mode) },
                                    label = { Text(mode.label) },
                                    modifier = Modifier.testTag("salon_mode_${mode.name.lowercase()}")
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Transport Mode Selector (LFS Server modes are automatically filtered out unless Tailscale is detected)
                        val visibleTransports = OasisCommonplaceServerClient.availableTransportModes(
                            salonState.isTailscaleDetected
                        )
                        Text(
                            text = "2. Transport Channel:",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            visibleTransports.forEach { transport ->
                                FilterChip(
                                    selected = salonState.activeTransportMode == transport,
                                    onClick = { onSelectTransportMode(transport) },
                                    label = { Text(transport.label, style = MaterialTheme.typography.labelSmall) },
                                    modifier = Modifier.testTag("transport_mode_${transport.name.lowercase()}")
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Resident AI Scholars (@archivist, @willow, @mote)
                        Text(
                            text = "3. Resident AI Scholars in Room:",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            listOf(
                                "@archivist" to "@archivist • Provenance",
                                "@willow" to "@willow • Poetics",
                                "@mote" to "@mote • Trivia Wit"
                            ).forEach { (handle, label) ->
                                FilterChip(
                                    selected = handle in salonState.activeAiScholars,
                                    onClick = { onToggleAiScholar(handle) },
                                    label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                                    modifier = Modifier.testTag("scholar_toggle_${handle.removePrefix("@")}")
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Launch or Join Room Controls
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = roomCodeInput,
                                onValueChange = { roomCodeInput = it },
                                label = { Text("Room Code (optional)") },
                                placeholder = { Text("LFS-7K9M2P") },
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("salon_room_code_input")
                            )

                            Button(
                                onClick = {
                                    onLaunchOrJoinRoom(roomCodeInput.takeIf { it.isNotBlank() }, roomTitleInput)
                                },
                                modifier = Modifier
                                    .height(52.dp)
                                    .testTag("launch_or_join_salon_button"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Launch or Join Salon",
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (roomCodeInput.isBlank()) "Launch Salon" else "Join Room")
                            }
                        }

                        // Public Rooms Quick Join Directory
                        if (salonState.publicRoomsDirectory.isNotEmpty() && !salonState.isConnected) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "ACTIVE SALON LOBBY DIRECTORY (TAP TO JOIN):",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            salonState.publicRoomsDirectory.forEach { publicRoom ->
                                Surface(
                                    color = MaterialTheme.colorScheme.surface,
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 3.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 10.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "${publicRoom.title} (${publicRoom.roomCode})",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Text(
                                                text = "${publicRoom.gameMode.label} • ${publicRoom.activeQuoteAuthor} • ${publicRoom.transportTierLabel}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        TextButton(
                                            onClick = {
                                                onSelectGameMode(publicRoom.gameMode)
                                                onLaunchOrJoinRoom(publicRoom.roomCode, publicRoom.title)
                                            }
                                        ) {
                                            Text("Join")
                                        }
                                    }
                                }
                            }
                        }

                        // Active Room Workspace (Co-Reading Marginalia or Literary Trivia Duel)
                        if (salonState.isConnected || salonState.roomCode.isNotBlank()) {
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 12.dp),
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "ROOM ${salonState.roomCode} • ${salonState.roomTitle}",
                                        style = MaterialTheme.typography.labelMedium.copy(fontFamily = FontFamily.Monospace),
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = salonState.connectionDiagnostic,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                TextButton(onClick = onLeaveRoom) {
                                    Text("Leave")
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Participant Scoreboard Strip
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                salonState.participants.forEach { participant ->
                                    Surface(
                                        color = if (participant.isAiScholar) {
                                            MaterialTheme.colorScheme.tertiaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.secondaryContainer
                                        },
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = "${participant.displayName} • ${participant.score} pts (🔥${participant.streak})",
                                            style = MaterialTheme.typography.labelSmall,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            if (salonState.activeGameMode == MultiplayerGameMode.LITERARY_TRIVIA_DUEL &&
                                salonState.triviaRound != null
                            ) {
                                val round = salonState.triviaRound
                                Surface(
                                    color = MaterialTheme.colorScheme.surface,
                                    shape = RoundedCornerShape(14.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("trivia_duel_arena_card")
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.Default.EmojiEvents,
                                                    contentDescription = "Trivia Duel Round",
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "Round ${round.roundNumber}/${round.totalRounds} • ${round.challengeType}",
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))

                                        Text(
                                            text = round.promptQuoteText,
                                            style = MaterialTheme.typography.bodyLarge.copy(fontStyle = FontStyle.Italic),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )

                                        Spacer(modifier = Modifier.height(6.dp))

                                        Text(
                                            text = round.scholarHint,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.tertiary
                                        )

                                        Spacer(modifier = Modifier.height(10.dp))

                                        round.options.forEachIndexed { idx, optionText ->
                                            val isSelected = round.selectedOptionIndex == idx
                                            val isCorrect = round.correctOptionIndex == idx
                                            OutlinedButton(
                                                onClick = { onSubmitTriviaAnswer(idx) },
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 3.dp)
                                                    .testTag("trivia_option_$idx"),
                                                shape = RoundedCornerShape(10.dp)
                                            ) {
                                                val prefix = when {
                                                    round.isRevealed && isCorrect -> "✓ "
                                                    round.isRevealed && isSelected && !isCorrect -> "✗ "
                                                    else -> "${('A' + idx)}. "
                                                }
                                                Text(
                                                    text = "$prefix$optionText",
                                                    fontWeight = if (isSelected || (round.isRevealed && isCorrect)) {
                                                        FontWeight.Bold
                                                    } else {
                                                        FontWeight.Normal
                                                    }
                                                )
                                            }
                                        }

                                        if (round.isRevealed) {
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                text = round.explanationNote,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Button(
                                                onClick = onAdvanceTriviaRound,
                                                modifier = Modifier
                                                    .align(Alignment.End)
                                                    .testTag("next_trivia_round_button"),
                                                shape = RoundedCornerShape(10.dp)
                                            ) {
                                                Text("Next Trivia Round")
                                            }
                                        }
                                    }
                                }
                            } else {
                                // Co-Reading Salon & Synchronized Marginalia View
                                Surface(
                                    color = MaterialTheme.colorScheme.surface,
                                    shape = RoundedCornerShape(14.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("co_reading_marginalia_card")
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Text(
                                            text = "SYNCHRONIZED CO-READING PASSAGE",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "“${salonState.currentPassageQuote.text}”",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "— ${salonState.currentPassageQuote.author} (${salonState.currentPassageQuote.sourceWork})",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.primary
                                        )

                                        Spacer(modifier = Modifier.height(8.dp))

                                        QuoteAudioSceneControlBar(
                                            quote = salonState.currentPassageQuote,
                                            audioState = audioState,
                                            onReadAloud = onReadAloud,
                                            onGenerate30sMusicScene = onGenerate30sMusicScene,
                                            onStopAudio = onStopAudio,
                                            testTagPrefix = "salon_passage_${salonState.currentPassageQuote.id}"
                                        )

                                        Spacer(modifier = Modifier.height(8.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            salonState.activeAiScholars.forEach { scholar ->
                                                AssistChip(
                                                    onClick = {
                                                        onPostMarginalia(
                                                            "Share your scholarly margin note on ${salonState.currentPassageQuote.author}",
                                                            salonState.currentPassageQuote.text.take(48),
                                                            scholar
                                                        )
                                                    },
                                                    label = { Text("Ask $scholar", style = MaterialTheme.typography.labelSmall) },
                                                    leadingIcon = {
                                                        Icon(
                                                            imageVector = Icons.Default.AutoAwesome,
                                                            contentDescription = null,
                                                            modifier = Modifier.size(14.dp)
                                                        )
                                                    }
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))

                                        VoiceTranscriptionStatusBanner(
                                            target = VoiceTranscriptionTarget.SALON_MARGINALIA,
                                            transcriptionState = transcriptionState
                                        )

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            OutlinedTextField(
                                                value = marginaliaInput,
                                                onValueChange = { marginaliaInput = it },
                                                placeholder = { Text("Write or dictate a live margin note (or tag @archivist, @willow, @mote)…") },
                                                singleLine = true,
                                                trailingIcon = {
                                                    VoiceDictationMicButton(
                                                        target = VoiceTranscriptionTarget.SALON_MARGINALIA,
                                                        transcriptionState = transcriptionState,
                                                        onToggleVoiceDictation = onToggleVoiceDictation,
                                                        onTranscribedText = { dictated ->
                                                            marginaliaInput = if (marginaliaInput.isBlank()) {
                                                                dictated
                                                            } else {
                                                                "$marginaliaInput $dictated"
                                                            }
                                                        },
                                                        testTag = "voice_dictate_salon_marginalia_button"
                                                    )
                                                },
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .testTag("marginalia_note_input")
                                            )
                                            Button(
                                                onClick = {
                                                    if (marginaliaInput.isNotBlank()) {
                                                        onPostMarginalia(
                                                            marginaliaInput,
                                                            salonState.currentPassageQuote.text.take(48),
                                                            ""
                                                        )
                                                        marginaliaInput = ""
                                                    }
                                                },
                                                modifier = Modifier.testTag("post_marginalia_button"),
                                                shape = RoundedCornerShape(10.dp)
                                            ) {
                                                Text("Annotate")
                                            }
                                        }

                                        if (salonState.marginaliaFeed.isNotEmpty()) {
                                            Spacer(modifier = Modifier.height(10.dp))
                                            salonState.marginaliaFeed.take(5).forEach { entry ->
                                                Surface(
                                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                                    shape = RoundedCornerShape(8.dp),
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(vertical = 3.dp)
                                                ) {
                                                    Column(modifier = Modifier.padding(8.dp)) {
                                                        Text(
                                                            text = entry.authorName,
                                                            style = MaterialTheme.typography.labelSmall,
                                                            fontWeight = FontWeight.Bold,
                                                            color = MaterialTheme.colorScheme.primary
                                                        )
                                                        Text(
                                                            text = entry.marginNote,
                                                            style = MaterialTheme.typography.bodySmall
                                                        )
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
            }

            // Family Circles List Section
            item {
                Text(
                    text = "YOUR GOOGLE-CONNECTED FAMILY CIRCLES",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            when (familyCirclesState) {
                is UiState.Loading -> {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                }
                is UiState.Error -> {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = familyCirclesState.message,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(14.dp)
                            )
                        }
                    }
                }
                is UiState.Success -> {
                    if (familyCirclesState.data.isEmpty()) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                ),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Groups,
                                        contentDescription = "No family circles yet",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(36.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Start Your Family Literary Circle",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Create a private circle and add your family members' Google email addresses to share daily quotes with location & time stamps.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    } else {
                        items(familyCirclesState.data, key = { it.id }) { circle ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("family_circle_card_${circle.id}"),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                ),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.65f))
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = circle.name,
                                                style = MaterialTheme.typography.titleLarge,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "Circle Code: ${circle.inviteCode} • ${circle.memberUids.size} Verified UID(s)",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Surface(
                                            color = MaterialTheme.colorScheme.tertiaryContainer,
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Security,
                                                    contentDescription = "Zero-Trust Verified",
                                                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = "ZERO-TRUST",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        circle.memberEmails.forEach { email ->
                                            Surface(
                                                color = MaterialTheme.colorScheme.surfaceVariant,
                                                shape = RoundedCornerShape(6.dp)
                                            ) {
                                                Text(
                                                    text = email,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedButton(
                                            onClick = { circleForInvite = circle },
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.PersonAdd,
                                                contentDescription = "Invite Google Member",
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Add Google User")
                                        }

                                        if (currentFeaturedQuote != null) {
                                            Button(
                                                onClick = {
                                                    onShareFeaturedQuote(
                                                        circle,
                                                        currentFeaturedQuote,
                                                        "Shared from today's parchment edition"
                                                    )
                                                },
                                                modifier = Modifier.weight(1f),
                                                shape = RoundedCornerShape(10.dp)
                                            ) {
                                                Text("Share Active Quote")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Shared Family Quotes Feed
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "SHARED FAMILY QUOTES & PROVENANCE STAMPS",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            when (familySharedQuotesState) {
                is UiState.Loading -> {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                }
                is UiState.Error -> {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = familySharedQuotesState.message,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(14.dp)
                            )
                        }
                    }
                }
                is UiState.Success -> {
                    if (familySharedQuotesState.data.isEmpty()) {
                        item {
                            Text(
                                text = "No quotes have been shared to your Family Circles yet. Tap “Family” on any quote in the Discover screen to share it with your circle.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    } else {
                        items(familySharedQuotesState.data, key = { it.id }) { shared ->
                            FamilySharedQuoteCard(
                                sharedQuote = shared,
                                audioState = audioState,
                                onReadAloud = onReadAloud,
                                onGenerate30sMusicScene = onGenerate30sMusicScene,
                                onStopAudio = onStopAudio,
                                onSaveToPersonalArchive = {
                                    onSaveSharedQuoteToPersonalArchive(shared)
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showFolioCipherModal) {
        var passphraseInput by remember { mutableStateOf("") }
        var envelopeToOpenInput by remember { mutableStateOf(salonState.lastSealedEnvelope) }
        AlertDialog(
            onDismissRequest = { showFolioCipherModal = false },
            title = { Text("CPF1 AES-256-GCM Folio Envelope") },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Seal the active quote into an authenticated CPF1: envelope or unseal a received packet.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = passphraseInput,
                        onValueChange = { passphraseInput = it },
                        label = { Text("Shared Passphrase") },
                        placeholder = { Text("Enter salon passphrase…") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                if (passphraseInput.isNotBlank()) {
                                    onSealFolioEnvelope(passphraseInput)
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Seal Folio")
                        }
                        OutlinedButton(
                            onClick = {
                                val targetEnv = envelopeToOpenInput.ifBlank { salonState.lastSealedEnvelope }
                                if (targetEnv.isNotBlank() && passphraseInput.isNotBlank()) {
                                    onOpenFolioEnvelope(targetEnv, passphraseInput)
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.LockOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Open")
                        }
                    }

                    if (salonState.lastSealedEnvelope.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Sealed Envelope:\n${salonState.lastSealedEnvelope}",
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = envelopeToOpenInput,
                        onValueChange = { envelopeToOpenInput = it },
                        label = { Text("CPF1: Envelope to Open") },
                        placeholder = { Text("CPF1:...") },
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (salonState.lastUnsealedPlaintext.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Unsealed Folio: ${salonState.lastUnsealedPlaintext}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showFolioCipherModal = false }) {
                    Text("Done")
                }
            }
        )
    }

    if (showCreateCircleDialog) {
        var nameInput by remember { mutableStateOf("") }
        var emailsInput by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreateCircleDialog = false },
            title = { Text("Create Zero-Trust Family Circle") },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        label = { Text("Family Circle Name") },
                        placeholder = { Text("e.g., The Emerson Family Archive") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("circle_name_input")
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = emailsInput,
                        onValueChange = { emailsInput = it },
                        label = { Text("Family Google Emails (comma-separated)") },
                        placeholder = { Text("sister@gmail.com, dad@gmail.com") },
                        minLines = 2,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("circle_emails_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val finalName = nameInput.ifBlank { "Family Literary Circle" }
                        showCreateCircleDialog = false
                        onCreateCircle(finalName, emailsInput)
                    },
                    modifier = Modifier.testTag("confirm_create_circle_button")
                ) {
                    Text("Create Circle")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateCircleDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    circleForInvite?.let { targetCircle ->
        var emailToInvite by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { circleForInvite = null },
            title = { Text("Add Google Account to ${targetCircle.name}") },
            text = {
                OutlinedTextField(
                    value = emailToInvite,
                    onValueChange = { emailToInvite = it },
                    label = { Text("Family Member's Google Email") },
                    placeholder = { Text("family.member@gmail.com") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (emailToInvite.isNotBlank()) {
                            onInviteGoogleMember(targetCircle, emailToInvite)
                            circleForInvite = null
                        }
                    }
                ) {
                    Text("Authorize Google User")
                }
            },
            dismissButton = {
                TextButton(onClick = { circleForInvite = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun FamilySharedQuoteCard(
    sharedQuote: FamilySharedQuote,
    audioState: QuoteAudioSceneState,
    onReadAloud: (QuoteItem) -> Unit,
    onGenerate30sMusicScene: (QuoteItem) -> Unit,
    onStopAudio: () -> Unit,
    onSaveToPersonalArchive: () -> Unit
) {
    val domainQuoteItem = remember(sharedQuote.id, sharedQuote.text, sharedQuote.author) {
        QuoteItem(
            id = sharedQuote.id.ifBlank { "shared_${sharedQuote.author.hashCode()}" },
            text = sharedQuote.text,
            author = sharedQuote.author,
            sourceWork = "Shared in ${sharedQuote.circleName} by ${sharedQuote.senderName}",
            category = sharedQuote.tags.firstOrNull() ?: "Philosophy",
            tags = sharedQuote.tags,
            onlinePopularityScore = 95,
            popularOnlineContext = sharedQuote.personalNote.ifBlank { "Shared in ${sharedQuote.circleName}" }
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("family_shared_quote_card_${sharedQuote.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = sharedQuote.circleName.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                Text(
                    text = "Shared by ${sharedQuote.senderName} (${sharedQuote.senderEmail})",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "“${sharedQuote.text}”",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "— ${sharedQuote.author}",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )

            if (sharedQuote.personalNote.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Family Note: “${sharedQuote.personalNote}”",
                    style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // TimestampInfo Provenance Box
            Surface(
                color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.55f),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = "Captured location",
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = sharedQuote.timestampInfo.location,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = "Captured date",
                            tint = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = sharedQuote.timestampInfo.date,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = "Captured time",
                            tint = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = sharedQuote.timestampInfo.time,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            QuoteAudioSceneControlBar(
                quote = domainQuoteItem,
                audioState = audioState,
                onReadAloud = onReadAloud,
                onGenerate30sMusicScene = onGenerate30sMusicScene,
                onStopAudio = onStopAudio,
                testTagPrefix = "family_shared_${sharedQuote.id}"
            )

            HorizontalDivider(
                modifier = Modifier.padding(top = 10.dp, bottom = 6.dp),
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                OutlinedButton(
                    onClick = onSaveToPersonalArchive,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.BookmarkBorder,
                        contentDescription = "Save to personal archive",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Save to My Archive")
                }
            }
        }
    }
}
