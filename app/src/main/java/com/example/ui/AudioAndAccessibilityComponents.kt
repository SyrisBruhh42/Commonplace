package com.example.ui

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.AccessibilitySettings
import com.example.data.QuoteAudioSceneState
import com.example.data.QuoteItem
import com.example.data.VoiceTranscriptionState
import com.example.data.VoiceTranscriptionTarget
import com.example.ui.theme.LocalAccessibilitySettings

/**
 * QuoteAudioSceneControlBar provides one-tap **Read Aloud** narration (`gemini-3.1-flash-tts-preview` + TTS),
 * **30s Ambient Music Scene** generation (`lyria-3-clip-preview`), and a QoL **Copy Citation** action
 * for any quote across Commonplace.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun QuoteAudioSceneControlBar(
    quote: QuoteItem,
    audioState: QuoteAudioSceneState,
    onReadAloud: (QuoteItem) -> Unit,
    onGenerate30sMusicScene: (QuoteItem) -> Unit,
    onStopAudio: () -> Unit,
    modifier: Modifier = Modifier,
    testTagPrefix: String = ""
) {
    val context = LocalContext.current
    val accessibility = LocalAccessibilitySettings.current
    val minTouchHeight = accessibility.minimumTouchTargetDp.dp
    val isActiveQuote = audioState.activeQuoteId == quote.id
    val isSpeakingThis = isActiveQuote && audioState.isReadingAloud
    val isPlayingMusicThis = isActiveQuote && audioState.isPlayingMusic
    val isGeneratingMusicThis = isActiveQuote && audioState.isGeneratingMusic
    val borderWidth = if (accessibility.highContrastMode) 2.dp else 1.dp

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Read Aloud Button
            OutlinedButton(
                onClick = {
                    if (isSpeakingThis) onStopAudio() else onReadAloud(quote)
                },
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = minTouchHeight)
                    .testTag("read_aloud_button_${quote.id}"),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(borderWidth, MaterialTheme.colorScheme.primary)
            ) {
                Icon(
                    imageVector = if (isSpeakingThis) Icons.Default.Stop else Icons.AutoMirrored.Filled.VolumeUp,
                    contentDescription = if (isSpeakingThis) "Stop reading quote aloud" else "Read quote aloud",
                    modifier = Modifier.size(17.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isSpeakingThis) "Stop Voice" else "Read Aloud",
                    style = MaterialTheme.typography.labelLarge
                )
            }

            // 30s Lyria Music Scene Button
            OutlinedButton(
                onClick = {
                    if (isPlayingMusicThis || isGeneratingMusicThis) {
                        onStopAudio()
                    } else {
                        onGenerate30sMusicScene(quote)
                    }
                },
                modifier = Modifier
                    .weight(1.2f)
                    .heightIn(min = minTouchHeight)
                    .testTag("generate_music_scene_button_${quote.id}"),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(borderWidth, MaterialTheme.colorScheme.secondary)
            ) {
                if (isGeneratingMusicThis) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Composing…", style = MaterialTheme.typography.labelLarge)
                } else {
                    Icon(
                        imageVector = if (isPlayingMusicThis) Icons.Default.Stop else Icons.Default.MusicNote,
                        contentDescription = if (isPlayingMusicThis) {
                            "Stop 30-second music scene"
                        } else {
                            "Generate 30-second ambient music scene with Lyria"
                        },
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(17.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isPlayingMusicThis) {
                            "Stop (${audioState.elapsedSeconds}s/30s)"
                        } else {
                            "30s Music Scene"
                        },
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }

            // QoL One-Tap Copy Formatted Citation Button
            IconButton(
                onClick = {
                    copyFormattedQuoteCitation(context, quote)
                },
                modifier = Modifier
                    .size(minTouchHeight)
                    .testTag("copy_citation_button_${quote.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = "Copy formatted quote and citation to clipboard",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // Visual Audio Scene & Read-Aloud Caption Strip (always shown when playing or when captions are enabled)
        AnimatedVisibility(
            visible = isActiveQuote && (isSpeakingThis || isPlayingMusicThis || isGeneratingMusicThis || audioState.statusCaption.isNotBlank())
        ) {
            Surface(
                color = MaterialTheme.colorScheme.tertiaryContainer.copy(
                    alpha = if (accessibility.highContrastMode) 1.0f else 0.78f
                ),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(borderWidth, MaterialTheme.colorScheme.tertiary),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .testTag("audio_scene_caption_banner_${quote.id}")
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = audioState.statusCaption.ifBlank { audioState.musicSceneTitle },
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.weight(1f)
                        )
                        if (isPlayingMusicThis) {
                            Text(
                                text = "${audioState.tempoBpm} BPM • ${audioState.keySignature}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                    }

                    if (isPlayingMusicThis) {
                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = {
                                (audioState.elapsedSeconds.toFloat() / audioState.clipDurationSeconds.coerceAtLeast(1))
                                    .coerceIn(0f, 1f)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    }

                    if (accessibility.showAudioVisualCaptions && audioState.musicSceneDescription.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "[Audio Caption • ${audioState.musicModelId}] ${audioState.musicSceneDescription}",
                            style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }

                    if (audioState.instrumentationTags.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            audioState.instrumentationTags.forEach { tag ->
                                Surface(
                                    color = MaterialTheme.colorScheme.surface,
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = tag,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
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

/**
 * VoiceDictationMicButton handles runtime `Manifest.permission.RECORD_AUDIO` permission and toggles
 * microphone voice capture + transcription with `gemini-3.5-transcribe`.
 */
@Composable
fun VoiceDictationMicButton(
    target: VoiceTranscriptionTarget,
    transcriptionState: VoiceTranscriptionState,
    onToggleVoiceDictation: (VoiceTranscriptionTarget, (String) -> Unit) -> Unit,
    onTranscribedText: (String) -> Unit = {},
    modifier: Modifier = Modifier,
    testTag: String = ""
) {
    val accessibility = LocalAccessibilitySettings.current
    val buttonSize = accessibility.minimumTouchTargetDp.dp
    val isRecordingThis = transcriptionState.isRecording && transcriptionState.activeTarget == target
    val isTranscribingThis = transcriptionState.isTranscribing && transcriptionState.activeTarget == target
    val resolvedTag = testTag.ifBlank { "voice_transcribe_button_${target.name.lowercase()}" }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        onToggleVoiceDictation(target, onTranscribedText)
    }

    IconButton(
        onClick = {
            if (isRecordingThis) {
                onToggleVoiceDictation(target, onTranscribedText)
            } else {
                audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        },
        enabled = !isTranscribingThis,
        modifier = modifier
            .size(buttonSize)
            .clip(CircleShape)
            .background(
                when {
                    isRecordingThis -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.secondaryContainer
                }
            )
            .testTag(resolvedTag)
    ) {
        when {
            isTranscribingThis -> {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
            isRecordingThis -> {
                Icon(
                    imageVector = Icons.Default.Stop,
                    contentDescription = "Stop recording and transcribe speech with Gemini 3.5 Transcribe",
                    tint = MaterialTheme.colorScheme.onError
                )
            }
            else -> {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = "Dictate with microphone using Gemini 3.5 Transcribe",
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
    }
}

/**
 * Displays live status or error feedback when dictating via `gemini-3.5-transcribe`
 * for a specific [target] input field.
 */
@Composable
fun VoiceTranscriptionStatusBanner(
    target: VoiceTranscriptionTarget,
    transcriptionState: VoiceTranscriptionState,
    modifier: Modifier = Modifier
) {
    val accessibility = LocalAccessibilitySettings.current
    val isActiveForTarget = transcriptionState.activeTarget == target &&
        (transcriptionState.isRecording || transcriptionState.isTranscribing || !transcriptionState.statusMessage.isNullOrBlank() || !transcriptionState.errorMessage.isNullOrBlank())

    AnimatedVisibility(visible = isActiveForTarget) {
        val isError = !transcriptionState.errorMessage.isNullOrBlank()
        val borderWidth = if (accessibility.highContrastMode) 2.dp else 1.dp
        Surface(
            color = if (isError) {
                MaterialTheme.colorScheme.errorContainer
            } else {
                MaterialTheme.colorScheme.secondaryContainer
            },
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(
                borderWidth,
                if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline
            ),
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .testTag("voice_transcription_banner_${target.name.lowercase()}")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = "Gemini 3.5 Transcribe status",
                    tint = if (isError) {
                        MaterialTheme.colorScheme.onErrorContainer
                    } else {
                        MaterialTheme.colorScheme.onSecondaryContainer
                    },
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = transcriptionState.errorMessage
                        ?: transcriptionState.statusMessage
                        ?: "Gemini 3.5 Transcribe active",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isError) {
                        MaterialTheme.colorScheme.onErrorContainer
                    } else {
                        MaterialTheme.colorScheme.onSecondaryContainer
                    }
                )
            }
        }
    }
}

/**
 * AccessibilityAndQolSheet provides WCAG AAA High Contrast themes, enlarged typography,
 * dyslexia-friendly spacing, 56.dp touch targets, reduced motion, auto Read Aloud, and visual audio captions.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AccessibilityAndQolSheet(
    settings: AccessibilitySettings,
    onUpdateSettings: (AccessibilitySettings) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val borderWidth = if (settings.highContrastMode) 2.dp else 1.dp

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.background,
        modifier = Modifier.testTag("accessibility_qol_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.AccessibilityNew,
                    contentDescription = "Accessibility and Quality of Life Settings",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "High Contrast, Accessibility & Reader QoL",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "WCAG AAA contrast, scaled typography, 56dp targets & audio captions",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            HorizontalDivider()

            // 1. High Contrast Mode (Light Ink-on-Ivory or Dark Gold-on-Obsidian)
            AccessibilityToggleCard(
                icon = Icons.Default.Contrast,
                title = "WCAG AAA High Contrast Theme",
                subtitle = "Adjusts all text colors, backgrounds (21:1 ratio), and element borders (2.5dp high-visibility outlines) to meet WCAG AAA standards.",
                checked = settings.highContrastMode,
                onCheckedChange = {
                    onUpdateSettings(
                        settings.copy(
                            highContrastMode = it,
                            enlargedTouchTargets = if (it) true else settings.enlargedTouchTargets
                        )
                    )
                },
                borderWidth = borderWidth,
                testTag = "high_contrast_toggle"
            )

            // 2. Dark Obsidian High Contrast Variant
            AccessibilityToggleCard(
                icon = Icons.Default.DarkMode,
                title = "Obsidian & Gold Dark High Contrast",
                subtitle = "Uses luminous gold and alabaster typography on pitch-black obsidian surfaces.",
                checked = settings.preferDarkHighContrast,
                onCheckedChange = {
                    onUpdateSettings(
                        settings.copy(
                            preferDarkHighContrast = it,
                            highContrastMode = if (it) true else settings.highContrastMode
                        )
                    )
                },
                borderWidth = borderWidth,
                testTag = "dark_high_contrast_toggle"
            )

            // 3. Typography Scale & Dyslexia-Friendly Spacing
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(borderWidth, MaterialTheme.colorScheme.outline),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.FormatSize,
                            contentDescription = "Typography scale",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Editorial Typography Scale & Line Spacing",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            1.0f to "Standard (1.0x)",
                            1.15f to "Large (1.15x)",
                            1.30f to "Extra Large (1.3x)"
                        ).forEach { (scale, label) ->
                            FilterChip(
                                selected = settings.fontScaleMultiplier == scale,
                                onClick = {
                                    onUpdateSettings(settings.copy(fontScaleMultiplier = scale))
                                },
                                label = { Text(label) },
                                modifier = Modifier.testTag("font_scale_${(scale * 100).toInt()}")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Dyslexia-Friendly Tracking & Line Height",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Widens character tracking (+0.6sp) and line spacing (1.65x)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = settings.dyslexiaFriendlySpacing,
                            onCheckedChange = {
                                onUpdateSettings(settings.copy(dyslexiaFriendlySpacing = it))
                            },
                            modifier = Modifier.testTag("dyslexia_spacing_toggle")
                        )
                    }
                }
            }

            // 4. Enlarged 56dp Touch Targets & Motor Accessibility
            AccessibilityToggleCard(
                icon = Icons.Default.TouchApp,
                title = "Enlarged 56dp Interactive Touch Targets",
                subtitle = "Expands buttons, microphone controls, and navigation chips to 56dp minimum height.",
                checked = settings.enlargedTouchTargets,
                onCheckedChange = {
                    onUpdateSettings(settings.copy(enlargedTouchTargets = it))
                },
                borderWidth = borderWidth,
                testTag = "enlarged_touch_targets_toggle"
            )

            // 5. Auto Read-Aloud & Visual Audio Captions
            AccessibilityToggleCard(
                icon = Icons.AutoMirrored.Filled.VolumeUp,
                title = "Auto Read-Aloud on New Priority Quote",
                subtitle = "Automatically narrates newly discovered quotes aloud when advancing the folio.",
                checked = settings.autoReadAloudOnSelect,
                onCheckedChange = {
                    onUpdateSettings(settings.copy(autoReadAloudOnSelect = it))
                },
                borderWidth = borderWidth,
                testTag = "auto_read_aloud_toggle"
            )

            AccessibilityToggleCard(
                icon = Icons.Default.Subtitles,
                title = "Visual Audio Captions &Reduced Motion",
                subtitle = "Displays rich descriptive captions for Lyria 30s music scenes and minimizes transitions.",
                checked = settings.showAudioVisualCaptions || settings.reducedMotion,
                onCheckedChange = {
                    onUpdateSettings(
                        settings.copy(
                            showAudioVisualCaptions = it,
                            reducedMotion = it
                        )
                    )
                },
                borderWidth = borderWidth,
                testTag = "audio_captions_toggle"
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = { onUpdateSettings(AccessibilitySettings()) },
                    modifier = Modifier.testTag("reset_accessibility_button")
                ) {
                    Text("Reset to Editorial Defaults")
                }

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier.testTag("close_accessibility_sheet_button")
                ) {
                    Text("Done")
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun AccessibilityToggleCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    borderWidth: androidx.compose.ui.unit.Dp,
    testTag: String
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(borderWidth, MaterialTheme.colorScheme.outline),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.padding(end = 8.dp)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                modifier = Modifier.testTag(testTag)
            )
        }
    }
}

fun copyFormattedQuoteCitation(context: Context, quote: QuoteItem) {
    runCatching {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        val formatted = "“${quote.text}” — ${quote.author}, ${quote.sourceWork} (${quote.era})"
        val clip = ClipData.newPlainText("Commonplace Quote Citation", formatted)
        clipboard?.setPrimaryClip(clip)
    }
}
