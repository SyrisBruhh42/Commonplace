package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Mood
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.CrossDisciplinaryFields
import com.example.data.InTheMomentStyle
import com.example.data.Quote
import com.example.data.QuoteAudioSceneState
import com.example.data.QuoteItem
import com.example.data.VoiceTranscriptionState
import com.example.data.VoiceTranscriptionTarget
import com.example.ui.theme.LocalAccessibilitySettings

/**
 * PhilosophicalChatScreen uses the Firebase AI SDK to allow users to:
 * 1) Have a text-based conversation with a Gemini-powered companion about the meaning and history
 *    of a specific saved quote, AND
 * 2) Generate a deep Quote in the moment ("In-the-Moment Quote Alchemist"), adapting to the styles
 *    the user likes for humor and fun, or synthesizing complex quotes that connect different fields.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PhilosophicalChatScreen(
    messages: List<ChatMessage>,
    savedQuotes: List<Quote> = emptyList(),
    attachedQuote: QuoteItem?,
    isGeneratingMomentQuote: Boolean = false,
    onSelectSavedQuote: (QuoteItem) -> Unit = {},
    onClearAttachedQuote: () -> Unit,
    onSendMessage: (String, QuoteItem?) -> Unit,
    onGenerateInTheMomentQuote: (InTheMomentStyle, List<String>, String, QuoteItem?) -> Unit = { _, _, _, _ -> },
    onSaveGeneratedQuote: (QuoteItem) -> Unit = {},
    onOpenDossier: (QuoteItem) -> Unit = {},
    audioState: QuoteAudioSceneState = QuoteAudioSceneState(),
    transcriptionState: VoiceTranscriptionState = VoiceTranscriptionState(),
    onReadAloud: (QuoteItem) -> Unit = {},
    onGenerate30sMusicScene: (QuoteItem) -> Unit = {},
    onStopAudio: () -> Unit = {},
    onToggleVoiceDictation: (VoiceTranscriptionTarget, (String) -> Unit) -> Unit = { _, _ -> }
) {
    val a11y = LocalAccessibilitySettings.current
    var inputText by remember { mutableStateOf("") }
    var showMomentAlchemist by remember { mutableStateOf(true) }
    var selectedStyle by remember { mutableStateOf(InTheMomentStyle.WITTY_HUMOR) }
    val selectedFields = remember {
        mutableStateListOf("Quantum Physics", "Stoic Ethics", "Everyday Comedy")
    }
    var momentPromptInput by remember { mutableStateOf("") }

    val listState = rememberLazyListState()

    LaunchedEffect(messages.size, messages.lastOrNull()?.text) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    val starterPrompts = remember {
        listOf(
            "Unpack the historical backstory & hidden irony of this quote",
            "Forge a witty quote connecting Quantum Physics & Coffee",
            "How does this quote connect Stoicism with modern psychology?",
            "Give me a playful paradox about procrastination and cosmos"
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("philosophical_chat_screen"),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 640.dp)
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Saved Quotes Selector Bar (pick a specific saved quote to discuss or riff on)
            if (savedQuotes.isNotEmpty()) {
                Text(
                    text = "SELECT A SAVED QUOTE TO DISCUSS OR RIFF ON",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    savedQuotes.take(10).forEach { savedDoc ->
                        val isAttached = attachedQuote?.text == savedDoc.text
                        FilterChip(
                            selected = isAttached,
                            onClick = {
                                onSelectSavedQuote(
                                    QuoteItem(
                                        id = savedDoc.id.ifBlank { "saved_chat" },
                                        text = savedDoc.text,
                                        author = savedDoc.author,
                                        sourceWork = savedDoc.sourceWork,
                                        category = savedDoc.category,
                                        tags = savedDoc.tags,
                                        onlinePopularityScore = savedDoc.onlinePopularityScore.toInt(),
                                        popularOnlineContext = "Saved at ${savedDoc.timestampInfo.location}"
                                    )
                                )
                            },
                            label = {
                                Text("${savedDoc.author}: “${savedDoc.text.take(28)}…”")
                            },
                            modifier = Modifier.testTag("select_saved_quote_chip_${savedDoc.id}")
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // In-the-Moment Quote Alchemist Panel (Humor/Fun Styles + Cross-Field Synthesis)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("in_the_moment_alchemist_card"),
                shape = RoundedCornerShape(16.dp),
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
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "In-the-Moment Quote Alchemist",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "In-the-Moment Quote Alchemist",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Adapt to your favorite humor style or bridge different fields",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        IconButton(
                            onClick = { showMomentAlchemist = !showMomentAlchemist },
                            modifier = Modifier.testTag("toggle_alchemist_button")
                        ) {
                            Icon(
                                imageVector = if (showMomentAlchemist) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = "Toggle Quote Alchemist"
                            )
                        }
                    }

                    AnimatedVisibility(visible = showMomentAlchemist) {
                        Column(modifier = Modifier.padding(top = 10.dp)) {
                            // Style Selector (Humor & Fun vs Complex Polymath)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Mood,
                                    contentDescription = "Style & Humor tone",
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "1. Choose Style (Humor & Fun or Polymath Depth):",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                InTheMomentStyle.entries.forEach { style ->
                                    val selected = selectedStyle == style
                                    FilterChip(
                                        selected = selected,
                                        onClick = { selectedStyle = style },
                                        label = { Text(style.label) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                        ),
                                        modifier = Modifier.testTag("moment_style_${style.name.lowercase()}")
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Cross-Disciplinary Field Bridge Selector
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Hub,
                                    contentDescription = "Connect different fields",
                                    tint = MaterialTheme.colorScheme.tertiary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "2. Connect Different Fields (Pick 2–4):",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                CrossDisciplinaryFields.ALL.forEach { field ->
                                    val isChosen = field in selectedFields
                                    FilterChip(
                                        selected = isChosen,
                                        onClick = {
                                            if (isChosen) {
                                                if (selectedFields.size > 1) selectedFields.remove(field)
                                            } else {
                                                if (selectedFields.size < 4) selectedFields.add(field)
                                            }
                                        },
                                        label = { Text(field, style = MaterialTheme.typography.labelSmall) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                            selectedLabelColor = MaterialTheme.colorScheme.onTertiaryContainer
                                        )
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            VoiceTranscriptionStatusBanner(
                                target = VoiceTranscriptionTarget.MOMENT_QUOTE_PROMPT,
                                transcriptionState = transcriptionState
                            )

                            OutlinedTextField(
                                value = momentPromptInput,
                                onValueChange = { momentPromptInput = it },
                                placeholder = {
                                    Text("Optional mood or moment (or tap mic to dictate)…")
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                trailingIcon = {
                                    VoiceDictationMicButton(
                                        target = VoiceTranscriptionTarget.MOMENT_QUOTE_PROMPT,
                                        transcriptionState = transcriptionState,
                                        onToggleVoiceDictation = onToggleVoiceDictation,
                                        onTranscribedText = { dictated ->
                                            momentPromptInput = if (momentPromptInput.isBlank()) {
                                                dictated
                                            } else {
                                                "$momentPromptInput $dictated"
                                            }
                                        },
                                        testTag = "voice_dictate_moment_prompt_button"
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("moment_prompt_input")
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Button(
                                onClick = {
                                    onGenerateInTheMomentQuote(
                                        selectedStyle,
                                        selectedFields.toList(),
                                        momentPromptInput,
                                        attachedQuote
                                    )
                                    momentPromptInput = ""
                                },
                                enabled = !isGeneratingMomentQuote,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("generate_moment_quote_button"),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                )
                            ) {
                                if (isGeneratingMomentQuote) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Synthesizing Deep Quote in the Moment…")
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = "Generate quote in the moment",
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Generate Deep Quote in the Moment")
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Scrollable Message Thread
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .testTag("symposium_chat_list"),
                contentPadding = PaddingValues(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(messages, key = { it.id }) { msg ->
                    ChatMessageBubble(
                        msg = msg,
                        audioState = audioState,
                        onReadAloud = onReadAloud,
                        onGenerate30sMusicScene = onGenerate30sMusicScene,
                        onStopAudio = onStopAudio,
                        onSaveGeneratedQuote = onSaveGeneratedQuote,
                        onOpenDossier = onOpenDossier
                    )
                }
            }

            // Starter Prompt Suggestions
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                starterPrompts.forEach { prompt ->
                    AssistChip(
                        onClick = {
                            onSendMessage(prompt, attachedQuote)
                        },
                        label = { Text(prompt) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "Prompt suggestion",
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    )
                }
            }

            // Attached Quote Context Banner
            if (attachedQuote != null) {
                Surface(
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.FormatQuote,
                            contentDescription = "Attached quote",
                            tint = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Active Saved Quote (${attachedQuote.author}):",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                            Text(
                                text = "“${attachedQuote.text.take(75)}…”",
                                style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                        IconButton(onClick = onClearAttachedQuote) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Remove attached quote",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            VoiceTranscriptionStatusBanner(
                target = VoiceTranscriptionTarget.SYMPOSIUM_CHAT,
                transcriptionState = transcriptionState
            )

            // Message Input Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = { Text("Discuss quote history or dictate a prompt…") },
                    shape = RoundedCornerShape(16.dp),
                    trailingIcon = {
                        VoiceDictationMicButton(
                            target = VoiceTranscriptionTarget.SYMPOSIUM_CHAT,
                            transcriptionState = transcriptionState,
                            onToggleVoiceDictation = onToggleVoiceDictation,
                            onTranscribedText = { dictated ->
                                inputText = if (inputText.isBlank()) {
                                    dictated
                                } else {
                                    "$inputText $dictated"
                                }
                            },
                            testTag = "voice_dictate_symposium_chat_button"
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("symposium_chat_input"),
                    maxLines = 4
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        if (inputText.isNotBlank()) {
                            onSendMessage(inputText, attachedQuote)
                            inputText = ""
                        }
                    },
                    modifier = Modifier
                        .size(if (a11y.enlargedTouchTargets) 56.dp else 48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                        .testTag("symposium_send_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send chat message",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    }
}

@Composable
private fun ChatMessageBubble(
    msg: ChatMessage,
    audioState: QuoteAudioSceneState,
    onReadAloud: (QuoteItem) -> Unit,
    onGenerate30sMusicScene: (QuoteItem) -> Unit,
    onStopAudio: () -> Unit,
    onSaveGeneratedQuote: (QuoteItem) -> Unit,
    onOpenDossier: (QuoteItem) -> Unit
) {
    val a11y = LocalAccessibilitySettings.current
    val alignment = if (msg.isUser) Alignment.End else Alignment.Start
    val containerColor = if (msg.isUser) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surface
    }
    val contentColor = if (msg.isUser) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        Card(
            modifier = Modifier.widthIn(max = 520.dp),
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (msg.isUser) 16.dp else 4.dp,
                bottomEnd = if (msg.isUser) 4.dp else 16.dp
            ),
            colors = CardDefaults.cardColors(containerColor = containerColor),
            border = if (a11y.highContrastEnabled) {
                BorderStroke(2.dp, MaterialTheme.colorScheme.outline)
            } else if (!msg.isUser) {
                BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
            } else null
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                if (msg.attachedQuote != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    ) {
                        Text(
                            text = "“${msg.attachedQuote.text.take(90)}…” — ${msg.attachedQuote.author}",
                            style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }

                // Special In-the-Moment Forged Quote Card inside the companion message
                if (msg.generatedMomentQuote != null) {
                    val forged = msg.generatedMomentQuote
                    Surface(
                        color = if (a11y.highContrastEnabled) {
                            MaterialTheme.colorScheme.surfaceVariant
                        } else {
                            Color(0xFFF9F1E1)
                        },
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(
                            if (a11y.highContrastEnabled) 2.dp else 1.2.dp,
                            if (a11y.highContrastEnabled) MaterialTheme.colorScheme.outline else Color(0xFFC8A96E)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp)
                            .testTag("forged_moment_quote_card_${forged.id}")
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
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
                                        text = "FORGED IN THE MOMENT",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                                Text(
                                    text = forged.tags.joinToString(" • "),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.tertiary
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "“${forged.text}”",
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = "— ${forged.author}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )

                            if (!msg.fieldConnectionInsight.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Cross-Field Bridge: ${msg.fieldConnectionInsight}",
                                    style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            QuoteAudioSceneControlBar(
                                quote = forged,
                                audioState = audioState,
                                onReadAloud = onReadAloud,
                                onGenerate30sMusicScene = onGenerate30sMusicScene,
                                onStopAudio = onStopAudio,
                                testTagPrefix = "forged_quote_${forged.id}"
                            )

                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 8.dp),
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { onSaveGeneratedQuote(forged) },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.BookmarkAdd,
                                        contentDescription = "Save & Stamp in-the-moment quote",
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Stamp & Save")
                                }

                                TextButton(onClick = { onOpenDossier(forged) }) {
                                    Icon(
                                        imageVector = Icons.Default.Psychology,
                                        contentDescription = "Deep Dossier",
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Dossier")
                                }
                            }
                        }
                    }
                }

                if (msg.isStreaming && msg.text.isEmpty()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Synthesizing across fields & humor styles…",
                            style = MaterialTheme.typography.bodySmall,
                            color = contentColor
                        )
                    }
                } else {
                    Text(
                        text = msg.text,
                        style = MaterialTheme.typography.bodyLarge,
                        color = contentColor
                    )
                }
            }
        }
    }
}
