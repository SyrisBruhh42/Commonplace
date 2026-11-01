package com.example.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.BorderColor
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.HighlightInkColor
import com.example.data.JournalPdfExportResult
import com.example.data.PdfFolioTheme
import com.example.data.QuoteAudioSceneState
import com.example.data.QuoteHighlightAnnotation
import com.example.data.QuoteItem
import com.example.data.QuotePassageTokenizer
import com.example.data.SavedQuoteDocument
import com.example.data.VoiceTranscriptionState
import com.example.data.VoiceTranscriptionTarget
import com.example.util.JournalPagePdfExporter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun HighlightInkColor.toComposeColor(alpha: Float = 1f): Color {
    val base = Color(this.argbHex)
    return base.copy(alpha = alpha)
}

/**
 * Builds an [AnnotatedString] for a saved quote's text that overlays translucent ink highlights
 * for every [QuoteHighlightAnnotation] attached to the quote.
 */
fun buildHighlightedQuoteAnnotatedString(
    quoteText: String,
    annotations: List<QuoteHighlightAnnotation>
): AnnotatedString {
    return buildAnnotatedString {
        append("“")
        val contentStartOffset = 1
        append(quoteText)
        append("”")

        annotations.forEach { ann ->
            val resolvedRange = if (ann.startIdx in 0..quoteText.length &&
                ann.endIdx in ann.startIdx..quoteText.length &&
                ann.endIdx > ann.startIdx
            ) {
                ann.startIdx to ann.endIdx
            } else if (ann.highlightedText.isNotBlank()) {
                val foundIdx = quoteText.indexOf(ann.highlightedText, ignoreCase = true)
                if (foundIdx >= 0) foundIdx to (foundIdx + ann.highlightedText.length) else null
            } else {
                null
            }

            if (resolvedRange != null) {
                val (start, end) = resolvedRange
                val inkColor = ann.resolvedInkColor
                addStyle(
                    style = SpanStyle(
                        background = inkColor.toComposeColor(alpha = 0.34f),
                        fontWeight = FontWeight.SemiBold
                    ),
                    start = contentStartOffset + start,
                    end = contentStartOffset + end
                )
            }
        }
    }
}

/**
 * Displays the list of time-stamped passage highlights and personal annotations inside a
 * `SavedQuoteArchiveCard` in the Library view.
 */
@Composable
fun SavedQuoteHighlightsLedger(
    quoteId: String,
    annotations: List<QuoteHighlightAnnotation>,
    onDeleteAnnotation: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (annotations.isEmpty()) return

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("saved_quote_highlights_ledger_$quoteId"),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Default.BorderColor,
                contentDescription = "Passage highlights",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(15.dp)
            )
            Text(
                text = "Time-Stamped Passage Annotations (${annotations.size})",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        annotations.forEach { ann ->
            val ink = ann.resolvedInkColor
            Surface(
                color = ink.toComposeColor(alpha = 0.14f),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, ink.toComposeColor(alpha = 0.55f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("highlight_annotation_item_${ann.id}")
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(ink.toComposeColor(alpha = 1f))
                            )
                            Text(
                                text = "“${ann.highlightedText}”",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                fontStyle = FontStyle.Italic,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        IconButton(
                            onClick = { onDeleteAnnotation(ann.id) },
                            modifier = Modifier
                                .size(28.dp)
                                .testTag("delete_highlight_${ann.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Remove highlight annotation",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = ann.noteText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = "Annotation timestamp",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = "${ink.label} • Stamped ${ann.timestampLabel}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

/**
 * Interactive dialog allowing the user to tap words in a saved quote to select a passage range,
 * choose an archival ink highlight color, and attach a personal time-stamped annotation
 * (including voice dictation via `gemini-3.5-transcribe`).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HighlightQuotePassageDialog(
    quote: SavedQuoteDocument,
    transcriptionState: VoiceTranscriptionState,
    onDismiss: () -> Unit,
    onConfirmHighlight: (
        startCharIndex: Int,
        endCharIndex: Int,
        highlightedText: String,
        inkColor: HighlightInkColor,
        noteText: String
    ) -> Unit,
    onToggleVoiceDictation: (VoiceTranscriptionTarget, (String) -> Unit) -> Unit
) {
    val tokens = remember(quote.quoteText) {
        QuotePassageTokenizer.tokenizeWords(quote.quoteText)
    }

    var anchorWordIndex by remember { mutableIntStateOf(0) }
    var endWordIndex by remember {
        mutableIntStateOf((tokens.size - 1).coerceAtMost(3).coerceAtLeast(0))
    }
    var awaitingSecondTap by remember { mutableStateOf(false) }
    var selectedInk by remember { mutableStateOf(HighlightInkColor.AMBER_GOLD) }
    var annotationNote by remember { mutableStateOf("") }

    val minSelectedWord = minOf(anchorWordIndex, endWordIndex)
    val maxSelectedWord = maxOf(anchorWordIndex, endWordIndex)

    val (startCharIdx, endCharIdx, selectedSnippet) = remember(
        quote.quoteText,
        tokens,
        minSelectedWord,
        maxSelectedWord
    ) {
        QuotePassageTokenizer.resolveCharRange(
            quoteText = quote.quoteText,
            tokens = tokens,
            firstWordIndex = minSelectedWord,
            secondWordIndex = maxSelectedWord
        )
    }

    val liveTimestampPreview = remember {
        SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.US).format(Date())
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.BorderColor,
                    contentDescription = "Highlight Quote Passage",
                    tint = MaterialTheme.colorScheme.primary
                )
                Text("Highlight Passage & Annotate")
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Tap a start word and an end word below to highlight a phrase within ${quote.author}'s quote:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Interactive Word Range Selector
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            tokens.forEach { token ->
                                val isSelected = token.wordIndex in minSelectedWord..maxSelectedWord
                                Surface(
                                    color = if (isSelected) {
                                        selectedInk.toComposeColor(alpha = 0.38f)
                                    } else {
                                        MaterialTheme.colorScheme.surface
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(
                                        width = if (isSelected) 1.5.dp else 0.8.dp,
                                        color = if (isSelected) {
                                            selectedInk.toComposeColor(alpha = 0.95f)
                                        } else {
                                            MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                                        }
                                    ),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            if (!awaitingSecondTap) {
                                                anchorWordIndex = token.wordIndex
                                                endWordIndex = token.wordIndex
                                                awaitingSecondTap = true
                                            } else {
                                                endWordIndex = token.wordIndex
                                                awaitingSecondTap = false
                                            }
                                        }
                                        .testTag("highlight_word_${token.wordIndex}")
                                ) {
                                    Text(
                                        text = token.word,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (awaitingSecondTap) {
                                    "Now tap the ending word of your highlight…"
                                } else {
                                    "Words ${minSelectedWord + 1}–${maxSelectedWord + 1} selected"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            TextButton(
                                onClick = {
                                    anchorWordIndex = 0
                                    endWordIndex = (tokens.size - 1).coerceAtLeast(0)
                                    awaitingSecondTap = false
                                },
                                modifier = Modifier.testTag("select_entire_quote_button")
                            ) {
                                Text("Select All", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }

                // Selected Passage Preview Pill
                Surface(
                    color = selectedInk.toComposeColor(alpha = 0.2f),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, selectedInk.toComposeColor(alpha = 0.7f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "SELECTED PASSAGE",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "“$selectedSnippet”",
                            style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic),
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.testTag("selected_highlight_snippet_preview")
                        )
                    }
                }

                // Archival Ink Color Picker
                Text(
                    text = "Archival Ink Color",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HighlightInkColor.entries.forEach { ink ->
                        val isChosen = selectedInk == ink
                        FilterChip(
                            selected = isChosen,
                            onClick = { selectedInk = ink },
                            label = {
                                Text(
                                    text = ink.label.substringBefore(" "),
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            leadingIcon = {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(ink.toComposeColor(alpha = 1f))
                                )
                            },
                            modifier = Modifier.testTag("highlight_ink_${ink.wireId.lowercase()}")
                        )
                    }
                }

                // Time-Stamped Personal Annotation Input + Gemini 3.5 Transcribe Mic
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = annotationNote,
                        onValueChange = { annotationNote = it },
                        label = { Text("Time-stamped annotation (or dictate)") },
                        placeholder = { Text("Why does this phrase resonate right now?") },
                        minLines = 2,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("highlight_annotation_input")
                    )

                    VoiceDictationMicButton(
                        target = VoiceTranscriptionTarget.PERSONAL_JOURNAL_NOTE,
                        transcriptionState = transcriptionState,
                        onToggleVoiceDictation = onToggleVoiceDictation,
                        onTranscribedText = { transcript ->
                            annotationNote = if (annotationNote.isBlank()) {
                                transcript
                            } else {
                                "$annotationNote $transcript"
                            }
                        }
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = "Timestamp preview",
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "Will be stamped: $liveTimestampPreview",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalNote = annotationNote.trim().ifBlank {
                        "Highlighted “${selectedSnippet.take(40)}” for archival reflection."
                    }
                    onConfirmHighlight(
                        startCharIdx,
                        endCharIdx,
                        selectedSnippet,
                        selectedInk,
                        finalNote
                    )
                    onDismiss()
                },
                enabled = selectedSnippet.isNotBlank(),
                modifier = Modifier.testTag("confirm_highlight_annotation_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Save highlight",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Stamp Highlight")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

/**
 * Dialog to preview and export a saved quote, its generated 30-second Lyria ambient soundscape
 * score card, inline highlights, and personal notes into a stylized PDF 'Journal Page' that can
 * be saved locally.
 */
@Composable
fun ExportJournalPdfDialog(
    quote: SavedQuoteDocument,
    annotations: List<QuoteHighlightAnnotation>,
    audioState: QuoteAudioSceneState,
    onDismiss: () -> Unit,
    onGenerate30sMusicScene: (QuoteItem) -> Unit,
    onExportPdf: (PdfFolioTheme) -> JournalPdfExportResult,
    onPdfSavedToUserUri: (String, Long) -> Unit
) {
    val context = LocalContext.current
    var selectedTheme by remember { mutableStateOf(PdfFolioTheme.ARCHIVAL_PARCHMENT) }
    var lastExportedResult by remember { mutableStateOf<JournalPdfExportResult?>(null) }

    val scoreSummary = remember(quote, audioState) {
        JournalPagePdfExporter.buildSoundscapeScoreSummary(quote, audioState)
    }

    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { targetUri ->
        val exported = lastExportedResult
        if (targetUri != null && exported != null) {
            val writeRes = JournalPagePdfExporter.writePdfToUri(
                context = context,
                targetUri = targetUri,
                pdfBytes = exported.pdfBytes
            )
            writeRes.onSuccess { bytesWritten ->
                onPdfSavedToUserUri(exported.fileName, bytesWritten)
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PictureAsPdf,
                    contentDescription = "Export PDF Journal Page",
                    tint = MaterialTheme.colorScheme.primary
                )
                Text("Export PDF Journal Folio")
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Export “${quote.quoteText.take(60)}…” by ${quote.author} with its Lyria ambient soundscape score card, ${annotations.size} time-stamped highlights, and personal notes into a print-ready PDF journal page.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Folio Theme Picker (Warm Archival Parchment vs. Clean Modern Monograph)
                Text(
                    text = "1. Select Folio Visual Style",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )

                PdfFolioTheme.entries.forEach { theme ->
                    val isSelected = selectedTheme == theme
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) {
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
                            } else {
                                MaterialTheme.colorScheme.surface
                            }
                        ),
                        border = BorderStroke(
                            width = if (isSelected) 1.8.dp else 1.dp,
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
                            }
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedTheme = theme }
                            .testTag("pdf_theme_${theme.wireId.lowercase()}")
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = theme.label,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = theme.subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Engraved Soundscape Score Card Preview
                Text(
                    text = "2. Engraved Ambient Soundscape Score Card (lyria-3-clip-preview)",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )

                Surface(
                    color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.45f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.6f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("pdf_soundscape_score_preview_card")
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.GraphicEq,
                                    contentDescription = "Soundscape score card",
                                    tint = MaterialTheme.colorScheme.tertiary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = scoreSummary.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            }
                            Text(
                                text = "${scoreSummary.keySignature} • ${scoreSummary.tempoBpm} BPM",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Ensemble: ${scoreSummary.instruments.joinToString(" • ")}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Live Engraved 5-Line Stave & Waveform Canvas
                        val barColor = MaterialTheme.colorScheme.tertiary
                        val staveLineColor = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.28f)
                        Canvas(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                        ) {
                            val h = size.height
                            val w = size.width
                            for (i in 0..4) {
                                val y = i * (h / 4f)
                                drawLine(
                                    color = staveLineColor,
                                    start = Offset(0f, y),
                                    end = Offset(w, y),
                                    strokeWidth = 1f
                                )
                            }
                            val bars = scoreSummary.waveformBars
                            val step = w / bars.size.coerceAtLeast(1)
                            val midY = h / 2f
                            bars.forEachIndexed { index, amp ->
                                val barH = (amp * h * 0.85f).coerceAtLeast(6f)
                                drawRoundRect(
                                    color = barColor,
                                    topLeft = Offset(index * step + step * 0.2f, midY - barH / 2f),
                                    size = Size(step * 0.6f, barH),
                                    cornerRadius = CornerRadius(4f, 4f)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedButton(
                            onClick = {
                                val domainItem = QuoteItem(
                                    id = quote.id.ifBlank { "saved_item" },
                                    text = quote.quoteText,
                                    author = quote.author,
                                    sourceWork = quote.sourceWork,
                                    category = quote.category,
                                    tags = quote.tags.ifEmpty { listOf(quote.category) },
                                    onlinePopularityScore = quote.onlinePopularityScore.toInt(),
                                    popularOnlineContext = quote.personalNote
                                )
                                onGenerate30sMusicScene(domainItem)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("pdf_dialog_generate_soundscape_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.MusicNote,
                                contentDescription = "Synthesize 30s Lyria soundscape",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                if (scoreSummary.isActivelyGeneratedInSession) {
                                    "Replay 30s Lyria Soundscape"
                                } else {
                                    "Synthesize 30s Lyria Soundscape for Folio"
                                }
                            )
                        }
                    }
                }

                HorizontalDivider()

                // Summary of Included Contents
                Text(
                    text = "Included in PDF Folio: Quote & Attribution • Provenance Stamp (${quote.locationStamp}) • ${annotations.size} Highlighted Passage Annotations • Personal Marginalia Note • Engraved Lyria Score Card.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                lastExportedResult?.let { exported ->
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("pdf_export_success_card")
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "Saved locally: ${exported.fileName} (${exported.pdfBytes.size / 1024} KB)",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Text(
                                text = "Path: ${exported.filePath}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val result = onExportPdf(selectedTheme)
                    lastExportedResult = result
                    runCatching {
                        createDocumentLauncher.launch(result.fileName)
                    }
                },
                modifier = Modifier.testTag("confirm_export_pdf_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Download,
                    contentDescription = "Save PDF Journal Page locally",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Save PDF Locally")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close dialog",
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Close")
            }
        }
    )
}
