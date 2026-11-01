package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Castle
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SportsEsports
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
import androidx.compose.material3.Switch
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
import com.example.data.DisneyImagineeringTranslator
import com.example.data.FriendlyGuideCardState
import com.example.data.HandshakePillarCheck
import com.example.data.LfsHostMode
import com.example.data.LfsServerPreset
import com.example.data.OasisHallDestination
import com.example.data.PhilosophicalDoctrine
import com.example.data.SanctuaryStorytellerState
import com.example.data.TrilogyWing

// Machado CVD-safe semantic colors + Antique Gold/Brass border accents
private val MachadoPassBlue = Color(0xFF0072B2)
private val MachadoWarnVermilion = Color(0xFFD55E00)
private val AntiqueGoldAccent = Color(0xFFC8A96E)
private val DeepBrassBorder = Color(0xFFB89146)

/**
 * Uninterrupted Top Universal Shell Ribbon (`OasisMultiplayerBar`) mounted at the root of `MainActivity`
 * across Title (`LoginScreen`), Gameplay, Modals, and Wing switches without ever dropping the active
 * party room (`OASIS-XXXX`), 16-Channel Rmblyn Family Chat (`@willow`, `@cinder`, `@mote`), or
 * AudioBookshelf & OpenAudible Sanctuary Storyteller (`cb64f9e7`).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OasisMultiplayerBar(
    envelope: CommonplaceTrilogySyncEnvelopeV1,
    onSelectHallDestination: (OasisHallDestination) -> Unit,
    onToggleOasisOverlay: (String) -> Unit,
    onToggleZt9ComfortMode: (Boolean) -> Unit,
    onSelectRmblynChannel: (Int) -> Unit,
    onSendRmblynChatMessage: (String) -> Unit,
    onToggleStorytellerPlayPause: () -> Unit,
    onSelectStorytellerChapter: (String) -> Unit,
    onSetSleepFadeTimerMinutes: (Int?) -> Unit,
    onOpenCodexDrawer: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showStorytellerDrawer by remember { mutableStateOf(false) }
    var chatDraft by remember { mutableStateOf("") }
    val activeChapter = envelope.storyteller.activeChapter()

    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 4.dp,
        border = BorderStroke(1.dp, AntiqueGoldAccent.copy(alpha = 0.65f)),
        modifier = modifier
            .fillMaxWidth()
            .testTag("oasis_multiplayer_bar")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            // Row 1: Party Room (OASIS-XXXX), Hearth Key, Shift+Tab Overlay Toggle, & Sanctuary Storyteller Pill
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Active Party Room Pill
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, DeepBrassBorder.copy(alpha = 0.7f)),
                    modifier = Modifier
                        .clickable { onOpenCodexDrawer() }
                        .testTag("oasis_ribbon_room_badge")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Key,
                            contentDescription = DisneyImagineeringTranslator.HEARTH_KEY_LABEL,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${envelope.activeRoomCode} • ${DisneyImagineeringTranslator.HEARTH_KEY_LABEL}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                // Starlight Rupees & Wonder Relics Pill
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, AntiqueGoldAccent.copy(alpha = 0.6f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Diamond,
                            contentDescription = DisneyImagineeringTranslator.STARLIGHT_RUPEES_RELICS_LABEL,
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${envelope.totalRupees} Starlight Rupees",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }

                // Shift+Tab / Ctrl+K (`oasis:overlay-toggle`) Action Physics & Family Chat Toggle
                Surface(
                    color = if (envelope.isOasisOverlayOpen) {
                        MaterialTheme.colorScheme.tertiaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    },
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, DeepBrassBorder.copy(alpha = 0.65f)),
                    modifier = Modifier
                        .clickable { onToggleOasisOverlay("oasis:overlay-toggle (Shift+Tab)") }
                        .testTag("oasis_overlay_toggle_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Forum,
                            contentDescription = "Toggle Oasis Family Chat & Overlay",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (envelope.isActionPhysicsPaused) {
                                "Shift+Tab Overlay • Physics Paused • 16-Ch Chat Live"
                            } else {
                                "Shift+Tab Overlay • 16-Ch Rmblyn (@willow @cinder @mote)"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Compact AudioBookshelf & OpenAudible Sanctuary Storyteller Pill (`cb64f9e7`)
                SanctuaryStorytellerPill(
                    storyteller = envelope.storyteller,
                    onTogglePlayPause = onToggleStorytellerPlayPause,
                    onSetSleepTimer = onSetSleepFadeTimerMinutes,
                    onToggleExpandTranscript = { showStorytellerDrawer = !showStorytellerDrawer }
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Row 2: 1-Click "Switch Wing / Return to TrashArcade Hall" 7-Destination Selector
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "Switch Wing / TrashArcade Hall:",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.tertiary
                )
                OasisHallDestination.entries.forEach { dest ->
                    val isSelected = envelope.activeHallDestination == dest
                    FilterChip(
                        selected = isSelected,
                        onClick = { onSelectHallDestination(dest) },
                        label = {
                            Text(
                                text = dest.displayLabel,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = if (dest.isArcadeHallCabinet) {
                                    Icons.Default.SportsEsports
                                } else {
                                    Icons.Default.Castle
                                },
                                contentDescription = dest.displayLabel,
                                modifier = Modifier.size(14.dp)
                            )
                        },
                        modifier = Modifier.testTag("hall_dest_chip_${dest.wireSlug}")
                    )
                }
            }

            // Expandable Sanctuary Storyteller (`cb64f9e7`) Chapter & Transcript Panel
            AnimatedVisibility(visible = showStorytellerDrawer) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, AntiqueGoldAccent.copy(alpha = 0.65f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .testTag("sanctuary_storyteller_expanded_panel")
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Sanctuary Storyteller (cb64f9e7) • ${activeChapter.bookTitle}",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "${activeChapter.title} • ${activeChapter.author} • Stream: ${activeChapter.streamPath}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(
                                onClick = { showStorytellerDrawer = false },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close Storyteller Details",
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Chapter selector chips
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            envelope.storyteller.chapters.forEach { chapter ->
                                FilterChip(
                                    selected = chapter.key == envelope.storyteller.activeChapterKey,
                                    onClick = { onSelectStorytellerChapter(chapter.key) },
                                    label = {
                                        Text(
                                            text = chapter.title,
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    },
                                    modifier = Modifier.testTag("storyteller_chapter_${chapter.key}")
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "“${activeChapter.narrationTranscript}”",
                            style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // Expandable Interactive Arcade Hall Cabinet Card when Code Runner / Boxhead Fort / BoxBoy Defense is active
            AnimatedVisibility(visible = envelope.activeHallDestination.isArcadeHallCabinet) {
                val dest = envelope.activeHallDestination
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, DeepBrassBorder.copy(alpha = 0.7f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .testTag("arcade_hall_destination_card")
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "TRASHARCADE HALL • ${dest.displayLabel.uppercase()}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.tertiary
                                )
                                Text(
                                    text = dest.subtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            OutlinedButton(
                                onClick = {
                                    onSelectHallDestination(OasisHallDestination.COMMONPLACE_WING_I)
                                },
                                modifier = Modifier.testTag("return_to_wing_i_button")
                            ) {
                                Text(
                                    text = "Return to Commonplace Wing I",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Family-Friendly Arcade Mode (${envelope.zt9ComfortOverrides.schemaKey}): " +
                                        "${envelope.zt9ComfortOverrides.enemyFramingLabel} • " +
                                        "${envelope.zt9ComfortOverrides.eliminationFramingLabel} • " +
                                        envelope.zt9ComfortOverrides.coOpReviveFramingLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "${envelope.zt9ComfortOverrides.ring1ToRing3HarmonyBadge} • Party Room ${envelope.activeRoomCode} stays connected",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = envelope.zt9ComfortOverrides.familyFriendlyArcadeMode,
                                onCheckedChange = onToggleZt9ComfortMode,
                                modifier = Modifier.testTag("zt9_comfort_mode_switch")
                            )
                        }
                    }
                }
            }

            // Expandable `Shift+Tab` (`oasis:overlay-toggle`) 16-Channel Rmblyn Family Chat & Physics Pause Drawer
            AnimatedVisibility(visible = envelope.isOasisOverlayOpen) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, AntiqueGoldAccent.copy(alpha = 0.8f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .testTag("oasis_shift_tab_overlay_panel")
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "OASIS UNIVERSAL OVERLAY (Shift+Tab / Ctrl+K) • ROOM ${envelope.activeRoomCode}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Single-Player Action Physics: PAUSED • 16-Channel Rmblyn Family Chat & LFS Navigation: LIVE",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.tertiary
                                )
                            }
                            IconButton(
                                onClick = { onToggleOasisOverlay("oasis:overlay-toggle (Close)") },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close Oasis Overlay",
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // 16-Channel Selector
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            CommonplaceTrilogySyncEnvelopeV1.RMBLYN_16_CHANNELS.forEach { (chNum, chName) ->
                                FilterChip(
                                    selected = envelope.activeRmblynChannel == chNum,
                                    onClick = { onSelectRmblynChannel(chNum) },
                                    label = {
                                        Text(
                                            text = chName,
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    },
                                    modifier = Modifier.testTag("rmblyn_channel_chip_$chNum")
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Recent messages in the active channel (or global companion broadcast)
                        val visibleMessages = envelope.rmblynMessages
                            .filter { it.channelNumber == envelope.activeRmblynChannel || it.channelNumber == 1 }
                            .takeLast(3)
                        visibleMessages.forEach { msg ->
                            Text(
                                text = "[${msg.channelName}] ${msg.senderHandle}: ${msg.text}",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (msg.isCompanionReply) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                },
                                modifier = Modifier.padding(vertical = 2.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Quick Companion Mentions (@willow, @cinder, @mote) + Chat Input
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            listOf("@willow", "@cinder", "@mote").forEach { companion ->
                                FilterChip(
                                    selected = chatDraft.contains(companion),
                                    onClick = {
                                        chatDraft = "$companion ${chatDraft.removePrefix(companion).trim()}".trim() + " "
                                    },
                                    label = {
                                        Text(
                                            text = companion,
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    },
                                    modifier = Modifier.testTag("companion_tag_${companion.removePrefix("@")}")
                                )
                            }
                            OutlinedTextField(
                                value = chatDraft,
                                onValueChange = { chatDraft = it },
                                placeholder = {
                                    Text(
                                        text = "Chat with family or ask @willow, @cinder, @mote...",
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                },
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("oasis_overlay_chat_input")
                            )
                            Button(
                                onClick = {
                                    val msg = chatDraft.ifBlank { "@willow share a porch blessing for our party!" }
                                    onSendRmblynChatMessage(msg)
                                    chatDraft = ""
                                },
                                modifier = Modifier.testTag("oasis_overlay_chat_send")
                            ) {
                                Text("Send", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Compact AudioBookshelf & OpenAudible Sanctuary Storyteller Pill (`cb64f9e7`) rendered inside
 * the persistent top `OasisMultiplayerBar`.
 */
@Composable
fun SanctuaryStorytellerPill(
    storyteller: SanctuaryStorytellerState,
    onTogglePlayPause: () -> Unit,
    onSetSleepTimer: (Int?) -> Unit,
    onToggleExpandTranscript: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeChapter = storyteller.activeChapter()
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, AntiqueGoldAccent.copy(alpha = 0.75f)),
        modifier = modifier.testTag("sanctuary_storyteller_pill")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            IconButton(
                onClick = onTogglePlayPause,
                modifier = Modifier
                    .size(28.dp)
                    .testTag("storyteller_play_pause_button")
            ) {
                Icon(
                    imageVector = if (storyteller.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (storyteller.isPlaying) "Pause Storyteller" else "Play Storyteller",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }

            Column(
                modifier = Modifier
                    .clickable { onToggleExpandTranscript() }
                    .testTag("storyteller_chapter_title_button")
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Headphones,
                        contentDescription = "Sanctuary Storyteller",
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Sanctuary Storyteller (cb64f9e7)",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
                Text(
                    text = activeChapter.title,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // 15m / 30m / 45m Starlight Sleep-Fade Timer selector chips
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Bedtime,
                    contentDescription = "Starlight Sleep-Fade Timer",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(14.dp)
                )
                SanctuaryStorytellerState.SLEEP_TIMER_PRESETS_MINUTES.forEach { mins ->
                    val isSelected = storyteller.sleepFadeTimerMinutes == mins
                    Surface(
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.surface
                        },
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(1.dp, DeepBrassBorder.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .clickable { onSetSleepTimer(mins) }
                            .testTag("sleep_timer_chip_$mins")
                    ) {
                        Text(
                            text = "${mins}m",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.onPrimary
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * 4-Part "Friendly Guide Card" replacing cold `ZT-6` technical status and error banners across all screens:
 * 1. What Happened
 * 2. Why
 * 3. Your Progress Is Safe
 * 4. One-Click Next Step
 */
@Composable
fun FriendlyGuideCard(
    guide: FriendlyGuideCardState,
    onNextStepClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = AntiqueGoldAccent.copy(alpha = 0.8f),
                shape = RoundedCornerShape(14.dp)
            )
            .testTag("friendly_guide_card"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = guide.title,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${guide.title} • All-Ages Sanctuary Helper",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "1. What Happened: ${guide.whatHappened}",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "2. Why: ${guide.whyItHappened}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "3. Your Progress Is Safe: ${guide.yourProgressIsSafe}",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = MachadoPassBlue
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = onNextStepClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("friendly_guide_next_step_button"),
                border = BorderStroke(1.dp, DeepBrassBorder)
            ) {
                Text(
                    text = "4. Next Step: ${guide.oneClickNextStepLabel}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * Compact Master Opening Portal card rendered on [LoginScreen] and accessible from [MainActivity]
 * displaying the 4-Wing Trilogy status, Hearth Key (Instant Family Pass), and Starlight Rupees & Wonder Relics.
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
                            text = "MASTER PORTAL • ${DisneyImagineeringTranslator.HEARTH_KEY_LABEL.uppercase()}",
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
                text = "Active Destination: ${envelope.activeHallDestination.displayLabel} • ${envelope.activeWing.title}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "${DisneyImagineeringTranslator.TIME_MACHINE_VAULTS_LABEL} (${envelope.selectedSaveSlotKey}) • ${DisneyImagineeringTranslator.STARLIGHT_QUIET_HOURS_LABEL}: Ready",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Live Trilogy Metrics bar with Disney Imagineering warm labels
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                StatPillBadge(label = "Guild Gold", value = "${envelope.guildGold}")
                StatPillBadge(label = "Starlight Rupees", value = "${envelope.totalRupees}")
                StatPillBadge(label = "Wonder Relics", value = "${envelope.unlockedRelics.count { it.isUnlocked }}")
                StatPillBadge(label = "Spirit", value = activeDoctrine.wireId.replaceFirstChar { it.uppercase() })
                StatPillBadge(label = "Hearth Pass", value = envelope.handshake.abacEvaluation.stateBits)
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 4-Pillar Hearth Key Handshake Badges
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
                    text = "Open Quest Trilogy Codex & Wonder Relics Vault",
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
                            text = "QUEST TRILOGY CODEX & TRASHARCADE HALL",
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
                            text = "${DisneyImagineeringTranslator.HEARTH_KEY_LABEL}: ${envelope.handshake.stableNodeId}",
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

            Spacer(modifier = Modifier.height(10.dp))

            // 4-Part Friendly Guide Card replacing technical ZT-6 status banner
            FriendlyGuideCard(
                guide = envelope.friendlyGuideCard,
                onNextStepClick = { onSelectPresetAndProbe(envelope.serverPreset) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // =========================================================================
            // 1. 4-WING SWITCHER, HOST MODES & ZT-9 FAMILY COMFORT OVERRIDES
            // =========================================================================
            SectionCard(
                title = "1. Switch Wing & Family Cloud Room Hosting",
                subtitle = "${LfsHostMode.HOST_LOCAL_DEVICE.label} or ${LfsHostMode.LAUNCH_U2_HOSTING_SERVICE.label} • Room ${envelope.activeRoomCode}"
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
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Family-Friendly Arcade Mode (${envelope.zt9ComfortOverrides.schemaKey}): " +
                                "${envelope.zt9ComfortOverrides.enemyFramingLabel} • " +
                                "${envelope.zt9ComfortOverrides.eliminationFramingLabel} • " +
                                envelope.zt9ComfortOverrides.coOpReviveFramingLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        val wingLiveDetail = when (envelope.activeWing) {
                            TrilogyWing.WING_I_COMMONPLACE ->
                                "Master Style Authority • Quotes & Trivia automatically forge Cross-Wing Directives (${envelope.pendingDirectives.size} active)."
                            TrilogyWing.WING_II_SCRIPTORIUM_PART1 ->
                                "Scriptorium Task Queue (${envelope.wing2ChronicleQueue.size} jobs): ${envelope.wing2ChronicleQueue.joinToString(" • ")}"
                            TrilogyWing.WING_III_CHAPTERHOUSE_PART2 ->
                                "Chapterhouse Challenge: ${envelope.wing3RaidBossName} • Missions: ${envelope.wing3ActiveMissions.joinToString(" • ")}"
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
                                        TrilogyWing.WING_III_CHAPTERHOUSE_PART2 -> "Launch Tag-Team Confetti Strike"
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
            // 3. HEARTH KEY (INSTANT FAMILY PASS) & STARLIGHT QUIET HOURS
            // =========================================================================
            SectionCard(
                title = "3. ${DisneyImagineeringTranslator.HEARTH_KEY_LABEL}",
                subtitle = "Family Role: ${envelope.handshake.persona.label} • ${DisneyImagineeringTranslator.STARLIGHT_QUIET_HOURS_LABEL}: ${envelope.handshake.abacEvaluation.stateBits}"
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
                    text = "Hearth Key Endpoint: ${envelope.serverPreset.handshakeUrl} • Anchor: ${envelope.handshake.anchorHost}",
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Status: ${DisneyImagineeringTranslator.translateTechnicalTerm(envelope.handshake.statusSummary)}",
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
                        contentDescription = "Verify Hearth Key",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Refresh ${DisneyImagineeringTranslator.HEARTH_KEY_LABEL} (<= 1600ms)")
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // =========================================================================
            // 4. STARLIGHT RUPEES & WONDER RELICS (9 GEMS & 4 CROSS-GAME RELICS)
            // =========================================================================
            SectionCard(
                title = "4. ${DisneyImagineeringTranslator.STARLIGHT_RUPEES_RELICS_LABEL}",
                subtitle = "NativeArcadeAssetBridge • Total Satchel Value: ${envelope.totalRupees} Starlight Rupees"
            ) {
                Text(
                    text = "9 Starlight Gem Denominations (1..1000):",
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
                                    text = "${denom.value} (${denom.cmykToneLabel}) x${denom.count}",
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
                    text = "Unlocked Wonder Relics (${envelope.unlockedRelics.count { it.isUnlocked }}/${envelope.unlockedRelics.size}):",
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
            // 5. 20-SLOT TIME-MACHINE MEMORY VAULTS & RMBLYN FAMILY CHAT
            // =========================================================================
            SectionCard(
                title = "5. 20-Slot ${DisneyImagineeringTranslator.TIME_MACHINE_VAULTS_LABEL} (slot_01..slot_20)",
                subtitle = "Auto-saves Vault 01 on events • 20 Time-Machine Memory Vaults synced across Desk & Cloud"
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
                                    text = "${slot.slotKey} (${if (slot.isOccupied) "Saved" else "Open"})",
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
                    text = "Selected ${inspectedSlot.slotKey} (${inspectedSlot.label}): Spirit ${inspectedSlot.activeDoctrine} • Gold ${inspectedSlot.guildGold} • Starlight Rupees ${inspectedSlot.totalRupees}",
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
                        contentDescription = "Save to Time-Machine Memory Vault",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Save Snapshot to ${DisneyImagineeringTranslator.TIME_MACHINE_VAULTS_LABEL} ($selectedSlotKey)")
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 12.dp),
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                )

                Text(
                    text = "16-Channel Rmblyn Family Chat (@willow, @cinder, @mote):",
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
                        placeholder = { Text("@willow share a porch story or /quest status") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("rmblyn_command_input")
                    )
                    Button(
                        onClick = {
                            val cmd = rmblynInput.ifBlank { "@willow sync-pulse" }
                            onSendRmblynCommand(cmd)
                            rmblynInput = ""
                        },
                        modifier = Modifier.testTag("send_rmblyn_command_button")
                    ) {
                        Text("Send")
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
                    text = "${directive.directiveId} • Spirit: ${directive.philosophicalDoctrine.uppercase()}",
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "+${directive.goldReward}G / +${directive.rupeeReward} Starlight Rupees / +${directive.reputationReward}Rep",
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
                text = "Wonder Companion Seed: ${directive.bestiarySeed} • Source: ${directive.sourceEvent}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
