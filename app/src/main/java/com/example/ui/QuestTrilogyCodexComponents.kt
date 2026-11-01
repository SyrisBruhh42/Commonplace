package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Castle
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.auth.EditorialCrestBanner
import com.example.data.CommonplaceQuestDirectiveV1
import com.example.data.CommonplaceTrilogySyncEnvelopeV1
import com.example.data.HandshakePillarCheck
import com.example.data.LfsServerPreset
import com.example.data.PhilosophicalDoctrine
import com.example.data.TrilogyWing

// Machado CVD-safe semantic colors + Antique Gold/Brass border accents
private val MachadoPassBlue = Color(0xFF0072B2)
private val MachadoWarnVermilion = Color(0xFFD55E00)
private val AntiqueGoldAccent = Color(0xFFC8A96E)
private val DeepBrassBorder = Color(0xFFB89146)

/**
 * Compact Master Opening Portal card rendered on [LoginScreen] and accessible from [MainActivity]
 * displaying the 4-Wing Trilogy status, 4-Pillar Tailscale Handshake summary, and TrashArcade Rupee total.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MasterPortalTrilogyCard(
    envelope: CommonplaceTrilogySyncEnvelopeV1,
    onOpenCodexDrawer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeDoctrine = envelope.manualDoctrineOverride ?: envelope.effectiveDoctrine

    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.5.dp,
                color = AntiqueGoldAccent.copy(alpha = 0.75f),
                shape = RoundedCornerShape(16.dp)
            )
            .testTag("master_portal_trilogy_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Castle,
                        contentDescription = "Quest Trilogy Portal",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "MASTER PORTAL • WING I–IV TRILOGY HUB",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.tertiary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Commonplace & LFS-Quest Trilogy Codex",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, DeepBrassBorder.copy(alpha = 0.6f))
                ) {
                    Text(
                        text = envelope.activeRoomCode,
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Active Wing: ${envelope.activeWing.wingNumber} (${envelope.activeWing.title}) • ${envelope.activeWing.subtitle}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Live Trilogy Metrics bar
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                StatPillBadge(label = "Gold", value = "${envelope.guildGold}")
                StatPillBadge(label = "Rep", value = "${envelope.guildReputation}")
                StatPillBadge(label = "Rupees", value = "${envelope.totalRupees}")
                StatPillBadge(label = "Doctrine", value = activeDoctrine.wireId.replaceFirstChar { it.uppercase() })
                StatPillBadge(label = "ABAC", value = envelope.handshake.abacEvaluation.stateBits)
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 4-Pillar Tailscale Handshake Badges
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                envelope.handshake.pillars.forEach { pillar ->
                    MachadoPillarBadge(pillar = pillar)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onOpenCodexDrawer,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("open_trilogy_codex_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Hub,
                    contentDescription = "Open Quest Trilogy Codex",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Open Quest Trilogy Codex & TrashArcade Bridge",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

/**
 * Full interactive ModalBottomSheet for the **Quest Trilogy Codex & TrashArcade Bridge**
 * accessible from the TopAppBar in `MainActivity.kt` and `AuthScreen.kt`.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun QuestTrilogyCodexSheet(
    envelope: CommonplaceTrilogySyncEnvelopeV1,
    onSelectWing: (TrilogyWing) -> Unit,
    onTriggerWingAction: (TrilogyWing) -> Unit,
    onSelectPresetAndProbe: (LfsServerPreset) -> Unit,
    onSetDoctrineOverride: (PhilosophicalDoctrine?) -> Unit,
    onEmitFeaturedQuoteDirective: () -> Unit,
    onSyncAegisSaveSlot: (String) -> Unit,
    onSendRmblynCommand: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedSlotKey by remember(envelope.selectedSaveSlotKey) {
        mutableStateOf(envelope.selectedSaveSlotKey.ifBlank { "slot_01" })
    }
    var rmblynInput by remember { mutableStateOf("") }

    val effectiveDoctrine = envelope.manualDoctrineOverride ?: envelope.effectiveDoctrine

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.background,
        modifier = Modifier.testTag("quest_trilogy_codex_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            // Header with EditorialCrestBanner
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(modifier = Modifier.size(56.dp)) {
                        EditorialCrestBanner()
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "QUEST TRILOGY CODEX & TRASHARCADE BRIDGE",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.tertiary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Master Opening Portal & Wing I–IV Hub",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "Tailnet ${envelope.handshake.tailnetId} • Replica ${envelope.handshake.stableNodeId}",
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("close_trilogy_codex_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close Quest Trilogy Codex"
                    )
                }
            }

            if (envelope.lastSyncStatusBanner.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.75f),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, AntiqueGoldAccent.copy(alpha = 0.6f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("trilogy_sync_diagnostic_banner")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudSync,
                            contentDescription = "Sync status",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = envelope.lastSyncStatusBanner,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // =========================================================================
            // 1. 4-WING SWITCHER & INTERACTIVE IN-APP WING PREVIEW
            // =========================================================================
            SectionCard(
                title = "1. 4-Wing Trilogy Switcher & Live Oasis Bridge",
                subtitle = "Room ${envelope.activeRoomCode} • Capsule ${envelope.capsuleTarget} / ${envelope.cartridgeSlug}"
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TrilogyWing.entries.forEach { wing ->
                        val isSelected = envelope.activeWing == wing
                        FilterChip(
                            selected = isSelected,
                            onClick = { onSelectWing(wing) },
                            label = {
                                Text(
                                    text = "${wing.wingNumber} • ${wing.badge}",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = when (wing) {
                                        TrilogyWing.WING_I_COMMONPLACE -> Icons.Default.MenuBook
                                        TrilogyWing.WING_II_SCRIPTORIUM_PART1 -> Icons.Default.Terminal
                                        TrilogyWing.WING_III_CHAPTERHOUSE_PART2 -> Icons.Default.Shield
                                        TrilogyWing.WING_IV_OBSIDIAN_DESK_PART3 -> Icons.Default.Explore
                                    },
                                    contentDescription = wing.title,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            modifier = Modifier.testTag("wing_tab_${wing.slug}")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Interactive Wing Preview Card
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, DeepBrassBorder.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "${envelope.activeWing.wingNumber}: ${envelope.activeWing.title}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = envelope.activeWing.subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        val wingLiveDetail = when (envelope.activeWing) {
                            TrilogyWing.WING_I_COMMONPLACE ->
                                "Master Style Authority • Quotes & Trivia automatically forge Cross-Wing Directives (${envelope.pendingDirectives.size} active)."
                            TrilogyWing.WING_II_SCRIPTORIUM_PART1 ->
                                "Scriptorium Task Queue (${envelope.wing2ChronicleQueue.size} jobs): ${envelope.wing2ChronicleQueue.joinToString(" • ")}"
                            TrilogyWing.WING_III_CHAPTERHOUSE_PART2 ->
                                "Chapterhouse Raid Boss: ${envelope.wing3RaidBossName} • Missions: ${envelope.wing3ActiveMissions.joinToString(" • ")}"
                            TrilogyWing.WING_IV_OBSIDIAN_DESK_PART3 ->
                                "3D Mahogany Guild Desk: ${envelope.wing4DeskArtifacts.joinToString(" • ")}"
                        }
                        Text(
                            text = wingLiveDetail,
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { onTriggerWingAction(envelope.activeWing) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("wing_interactive_action_button"),
                                border = BorderStroke(1.dp, DeepBrassBorder)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = "Execute Wing Action",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = when (envelope.activeWing) {
                                        TrilogyWing.WING_I_COMMONPLACE -> "Stamp & Broadcast Wing I Pulse"
                                        TrilogyWing.WING_II_SCRIPTORIUM_PART1 -> "Run PowerBlocks Pipeline Step"
                                        TrilogyWing.WING_III_CHAPTERHOUSE_PART2 -> "Launch Co-Op Raid Strike"
                                        TrilogyWing.WING_IV_OBSIDIAN_DESK_PART3 -> "Inspect 3D Mahogany Desk Lore"
                                    },
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // =========================================================================
            // 2. PHILOSOPHICAL DOCTRINE & CROSS-WING QUEST DIRECTIVES
            // =========================================================================
            SectionCard(
                title = "2. Adaptive Philosophical Doctrine & Quest Directives",
                subtitle = "Active Buff: ${effectiveDoctrine.label} — ${effectiveDoctrine.guildBuffTitle}"
            ) {
                Text(
                    text = "Doctrine Assignment Mode (Auto-mapped from Quote Category or Manual Override):",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = envelope.manualDoctrineOverride == null,
                        onClick = { onSetDoctrineOverride(null) },
                        label = { Text("Auto-Map Category") },
                        modifier = Modifier.testTag("doctrine_chip_auto")
                    )
                    PhilosophicalDoctrine.entries.forEach { doc ->
                        FilterChip(
                            selected = envelope.manualDoctrineOverride == doc,
                            onClick = { onSetDoctrineOverride(doc) },
                            label = { Text(doc.wireId.replaceFirstChar { it.uppercase() }) },
                            modifier = Modifier.testTag("doctrine_chip_${doc.wireId}")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = onEmitFeaturedQuoteDirective,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("emit_quest_directive_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary,
                        contentColor = MaterialTheme.colorScheme.onSecondary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Emit Quest Directive",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Forge & Sync Cross-Wing Quest Directive from Featured Quote")
                }

                if (envelope.pendingDirectives.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Recent Cross-Wing Directives (${envelope.pendingDirectives.size}):",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    envelope.pendingDirectives.take(4).forEach { dir ->
                        DirectiveItemRow(directive = dir)
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // =========================================================================
            // 3. 4-PILLAR TAILSCALE HANDSHAKE & 32-STATE ABAC LATTICE
            // =========================================================================
            SectionCard(
                title = "3. Zero-Trust Tailscale Handshake (/lfs-sso/handshake)",
                subtitle = "Persona: ${envelope.handshake.persona.label} • ABAC Bits: ${envelope.handshake.abacEvaluation.stateBits}"
            ) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    LfsServerPreset.entries.forEach { preset ->
                        FilterChip(
                            selected = envelope.serverPreset == preset,
                            onClick = { onSelectPresetAndProbe(preset) },
                            label = {
                                Text(
                                    text = preset.label,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            modifier = Modifier.testTag("preset_chip_${preset.name.lowercase()}")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Endpoint: ${envelope.serverPreset.handshakeUrl} • Anchor: ${envelope.handshake.anchorHost}",
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Status: ${envelope.handshake.statusSummary}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(8.dp))

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    envelope.handshake.pillars.forEach { pillar ->
                        MachadoPillarBadge(pillar = pillar, showDetail = true)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = { onSelectPresetAndProbe(envelope.serverPreset) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("probe_tailscale_handshake_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Probe Handshake",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Probe GET /lfs-sso/handshake (<= 1600ms Non-Blocking)")
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // =========================================================================
            // 4. TRASHARCADE RUPEE SATCHEL (9 CMY GEMS & 4 CROSS-GAME RELICS)
            // =========================================================================
            SectionCard(
                title = "4. TrashArcade Rupee Satchel & Cross-Game Relics",
                subtitle = "NativeArcadeAssetBridge • Total Satchel Value: ${envelope.totalRupees} Rupees"
            ) {
                Text(
                    text = "9 CMY Gem Denominations (1..1000):",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(6.dp))

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    envelope.rupeeDenominations.forEach { denom ->
                        val gemColor = Color(denom.hexColor)
                        Surface(
                            color = gemColor.copy(alpha = 0.14f),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, gemColor.copy(alpha = 0.7f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(gemColor)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "${denom.value} (${denom.cmykToneLabel}) ×${denom.count}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Unlocked Cross-Game Relics (${envelope.unlockedRelics.count { it.isUnlocked }}/${envelope.unlockedRelics.size}):",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(6.dp))

                envelope.unlockedRelics.forEach { relic ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Diamond,
                            contentDescription = relic.title,
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${relic.title} (${relic.slug})",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "${relic.originWing} • ${relic.effectDescription}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // =========================================================================
            // 5. 20-SLOT 6-GATE AEGIS SAVE LEDGER & RMBLYN OVERLAY DISPATCH
            // =========================================================================
            SectionCard(
                title = "5. 20-Slot 6-Gate Aegis Save Ledger (slot_01..slot_20)",
                subtitle = "Auto-syncs slot_01 on events • Manual 20-slot ledger with replicateToAnchor: true"
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    envelope.saveSlots.forEach { slot ->
                        val isSelected = selectedSlotKey == slot.slotKey
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedSlotKey = slot.slotKey },
                            label = {
                                Text(
                                    text = "${slot.slotKey} (${if (slot.isOccupied) "6G" else "--"})",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontFamily = FontFamily.Monospace
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                            ),
                            modifier = Modifier.testTag("aegis_slot_chip_${slot.slotKey}")
                        )
                    }
                }

                val inspectedSlot = envelope.saveSlots.find { it.slotKey == selectedSlotKey }
                    ?: envelope.saveSlots.first()

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Selected ${inspectedSlot.slotKey} (${inspectedSlot.label}): Doctrine ${inspectedSlot.activeDoctrine} • Gold ${inspectedSlot.guildGold} • Rupees ${inspectedSlot.totalRupees} • Replicated to Anchor: ${inspectedSlot.replicatedToAnchor}",
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { onSyncAegisSaveSlot(selectedSlotKey) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("sync_aegis_slot_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Save,
                        contentDescription = "Seal & Sync Aegis Slot",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Seal 6-Gate aegisEnvelopeV1 & Sync $selectedSlotKey to U2 + Anchor")
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 12.dp),
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                )

                Text(
                    text = "Rmblyn Overlay Command Bridge (/api/lfs/sync/rmblyn & /v1/overlay/rmblyn):",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = rmblynInput,
                        onValueChange = { rmblynInput = it },
                        placeholder = { Text("/quest status or @willow buff") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("rmblyn_command_input")
                    )
                    Button(
                        onClick = {
                            val cmd = rmblynInput.ifBlank { "/trilogy sync-pulse" }
                            onSendRmblynCommand(cmd)
                            rmblynInput = ""
                        },
                        modifier = Modifier.testTag("send_rmblyn_command_button")
                    ) {
                        Text("Dispatch")
                    }
                }
                if (envelope.lastRmblynCommandResult.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = envelope.lastRmblynCommandResult,
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    subtitle: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = AntiqueGoldAccent.copy(alpha = 0.65f),
                shape = RoundedCornerShape(14.dp)
            ),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(10.dp))
            content()
        }
    }
}

@Composable
private fun StatPillBadge(
    label: String,
    value: String
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, DeepBrassBorder.copy(alpha = 0.45f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$label: ",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.labelSmall,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun MachadoPillarBadge(
    pillar: HandshakePillarCheck,
    showDetail: Boolean = false
) {
    val badgeColor = if (pillar.passed) MachadoPassBlue else MachadoWarnVermilion
    Surface(
        color = badgeColor.copy(alpha = 0.12f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.75f))
    ) {
        Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (pillar.passed) Icons.Default.CheckCircle else Icons.Default.Warning,
                    contentDescription = pillar.badgeText,
                    tint = badgeColor,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${pillar.badgeText} ${pillar.key}",
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = badgeColor
                )
            }
            if (showDetail) {
                Text(
                    text = pillar.detail,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun DirectiveItemRow(directive: CommonplaceQuestDirectiveV1) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, DeepBrassBorder.copy(alpha = 0.45f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${directive.directiveId} • Doctrine: ${directive.philosophicalDoctrine.uppercase()}",
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "+${directive.goldReward}G / +${directive.rupeeReward}R / +${directive.reputationReward}Rep",
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "“${directive.quoteText}” — ${directive.author}",
                style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Bestiary Seed: ${directive.bestiarySeed} • Source: ${directive.sourceEvent}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
