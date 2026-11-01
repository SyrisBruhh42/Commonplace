package com.example.ui

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import com.example.data.FamilyCircle
import com.example.data.PriorityMode
import com.example.data.QuoteItem

fun shareQuoteViaIntent(
    context: Context,
    quoteText: String,
    author: String,
    sourceWork: String,
    stampSummary: String? = null
) {
    val body = buildString {
        append("“")
        append(quoteText)
        append("”\n— ")
        append(author)
        if (sourceWork.isNotBlank()) {
            append(" (")
            append(sourceWork)
            append(")")
        }
        if (!stampSummary.isNullOrBlank()) {
            append("\n\nStamped in Commonplace: ")
            append(stampSummary)
        }
    }
    val sendIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "Quote by $author")
        putExtra(Intent.EXTRA_TEXT, body)
    }
    context.startActivity(Intent.createChooser(sendIntent, "Share Quote"))
}

@Composable
fun DiscoverScreen(
    state: DiscoverUiState,
    familyCircles: List<FamilyCircle> = emptyList(),
    onToggleInterest: (String) -> Unit,
    onSelectPriorityMode: (PriorityMode) -> Unit,
    onNextPriorityQuote: () -> Unit,
    onGenerateAiQuote: () -> Unit,
    onQuickTakeaway: () -> Unit,
    onSaveQuoteWithStamp: (QuoteItem, String?, String) -> Unit,
    onShareToFamilyCircle: (FamilyCircle, QuoteItem, String) -> Unit = { _, _, _ -> },
    onOpenDossier: (QuoteItem) -> Unit,
    onDiscussInChat: (QuoteItem) -> Unit,
    onRefreshLocationStamp: () -> Unit,
    onDismissStatusBanner: () -> Unit
) {
    QuoteDiscoveryScreen(
        state = state,
        familyCircles = familyCircles,
        onToggleInterest = onToggleInterest,
        onSelectPriorityMode = onSelectPriorityMode,
        onNextPriorityQuote = onNextPriorityQuote,
        onGenerateAiQuote = onGenerateAiQuote,
        onQuickTakeaway = onQuickTakeaway,
        onSaveQuoteWithStamp = onSaveQuoteWithStamp,
        onShareToFamilyCircle = onShareToFamilyCircle,
        onOpenDossier = onOpenDossier,
        onDiscussInChat = onDiscussInChat,
        onRefreshLocationStamp = onRefreshLocationStamp,
        onDismissStatusBanner = onDismissStatusBanner
    )
}

@Composable
fun SaveQuoteWithStampDialog(
    quote: QuoteItem,
    defaultLocation: String,
    dateStamp: String,
    timeStamp: String,
    onRequestGpsPermission: () -> Unit,
    onDismiss: () -> Unit,
    onConfirmSave: (customLocation: String?, personalNote: String) -> Unit
) {
    var locationInput by remember(defaultLocation) { mutableStateOf(defaultLocation) }
    var personalNote by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Affix Archival Provenance Stamp",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "“${quote.text.take(90)}${if (quote.text.length > 90) "…" else ""}” — ${quote.author}",
                    style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))

                Surface(
                    color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = "Date and time stamp",
                            tint = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Date: $dateStamp • Time: $timeStamp",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = locationInput,
                    onValueChange = { locationInput = it },
                    label = { Text("Location Stamp (FusedLocationProviderClient)") },
                    trailingIcon = {
                        IconButton(onClick = onRequestGpsPermission) {
                            Icon(
                                imageVector = Icons.Default.MyLocation,
                                contentDescription = "Detect GPS location"
                            )
                        }
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("stamp_location_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = personalNote,
                    onValueChange = { personalNote = it },
                    label = { Text("Personal Marginalia Note (Optional)") },
                    placeholder = { Text("Why did this quote speak to you today?") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("stamp_note_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirmSave(locationInput, personalNote) },
                modifier = Modifier.testTag("confirm_save_stamp_button")
            ) {
                Text("Stamp & Save to Cloud")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
