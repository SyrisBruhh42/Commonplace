package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.PriorityScoredQuote
import com.example.data.QuoteCategories
import com.example.data.QuoteItem
import com.example.data.SavedQuoteDocument

@Composable
fun LibraryScreen(
    savedQuotesState: UiState<List<SavedQuoteDocument>>,
    rankedCatalogQuotes: List<PriorityScoredQuote>,
    onSaveCatalogQuote: (QuoteItem) -> Unit,
    onUpdateNote: (String, String) -> Unit,
    onDeleteSavedQuote: (String) -> Unit,
    onOpenDossier: (QuoteItem) -> Unit,
    onDiscussInChat: (QuoteItem) -> Unit
) {
    val context = LocalContext.current
    var selectedSegment by remember { mutableIntStateOf(0) } // 0 = Stamped Archive, 1 = Master Catalog
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }
    var editingQuote by remember { mutableStateOf<SavedQuoteDocument?>(null) }

    val categoriesWithAll = remember { listOf("All") + QuoteCategories.ALL }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 640.dp)
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // Segmented Switcher: Stamped Favorites vs. Master Quote Library
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = selectedSegment == 0,
                    onClick = { selectedSegment = 0 },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Bookmark,
                            contentDescription = "Stamped Archive",
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    modifier = Modifier.testTag("segment_stamped_archive")
                ) {
                    Text("Stamped Archive")
                }
                SegmentedButton(
                    selected = selectedSegment == 1,
                    onClick = { selectedSegment = 1 },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                    icon = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.LibraryBooks,
                            contentDescription = "Quote Catalog",
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    modifier = Modifier.testTag("segment_master_catalog")
                ) {
                    Text("Quote Library (${rankedCatalogQuotes.size})")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Search Function
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text(stringResource(R.string.search_library_placeholder)) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search quotes"
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear search"
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("library_search_input")
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Category Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                categoriesWithAll.forEach { cat ->
                    val isSelected = selectedCategory == cat
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedCategory = cat },
                        label = { Text(cat) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        modifier = Modifier.testTag("library_category_${cat.lowercase()}")
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (selectedSegment == 0) {
                // Stamped Favorites Archive View
                when (savedQuotesState) {
                    is UiState.Loading -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                    is UiState.Error -> {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = savedQuotesState.message,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                    is UiState.Success -> {
                        val filteredSaved by remember(savedQuotesState.data, searchQuery, selectedCategory) {
                            derivedStateOf {
                                savedQuotesState.data.filter { doc ->
                                    val matchesCategory = selectedCategory == "All" ||
                                        doc.category.equals(selectedCategory, ignoreCase = true) ||
                                        doc.tags.any { it.equals(selectedCategory, ignoreCase = true) }
                                    val q = searchQuery.trim().lowercase()
                                    val matchesSearch = q.isEmpty() ||
                                        doc.text.lowercase().contains(q) ||
                                        doc.author.lowercase().contains(q) ||
                                        doc.sourceWork.lowercase().contains(q) ||
                                        doc.category.lowercase().contains(q) ||
                                        doc.tags.any { it.lowercase().contains(q) } ||
                                        doc.timestampInfo.location.lowercase().contains(q) ||
                                        doc.timestampInfo.capturedAt.lowercase().contains(q) ||
                                        doc.personalNote.lowercase().contains(q)
                                    matchesCategory && matchesSearch
                                }
                            }
                        }

                        if (filteredSaved.isEmpty()) {
                            EmptyArchivePlaceholder(
                                isFiltered = searchQuery.isNotBlank() || selectedCategory != "All",
                                onSwitchToCatalog = { selectedSegment = 1 }
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(bottom = 24.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(filteredSaved, key = { it.id }) { doc ->
                                    SavedQuoteArchiveCard(
                                        doc = doc,
                                        onEditNote = { editingQuote = doc },
                                        onShare = {
                                            val stampStr = "${doc.locationStamp} • ${doc.dateStamp} at ${doc.timeStamp}"
                                            shareQuoteViaIntent(
                                                context = context,
                                                quoteText = doc.quoteText,
                                                author = doc.author,
                                                sourceWork = doc.sourceWork,
                                                stampSummary = stampStr
                                            )
                                        },
                                        onOpenDossier = {
                                            onOpenDossier(doc.toDomainQuoteItem())
                                        },
                                        onDiscuss = {
                                            onDiscussInChat(doc.toDomainQuoteItem())
                                        },
                                        onDelete = { onDeleteSavedQuote(doc.id) }
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                // Master Categorized Quote Library View
                val filteredCatalog by remember(rankedCatalogQuotes, searchQuery, selectedCategory) {
                    derivedStateOf {
                        rankedCatalogQuotes.filter { scored ->
                            val qItem = scored.quote
                            val matchesCat = selectedCategory == "All" ||
                                qItem.category.equals(selectedCategory, ignoreCase = true) ||
                                qItem.tags.any { it.equals(selectedCategory, ignoreCase = true) }
                            val q = searchQuery.trim().lowercase()
                            val matchesSearch = q.isEmpty() ||
                                qItem.text.lowercase().contains(q) ||
                                qItem.author.lowercase().contains(q) ||
                                qItem.sourceWork.lowercase().contains(q) ||
                                qItem.category.lowercase().contains(q)
                            matchesCat && matchesSearch
                        }
                    }
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredCatalog, key = { it.quote.id }) { scored ->
                        CatalogQuoteLibraryCard(
                            scoredQuote = scored,
                            onSaveWithStamp = { onSaveCatalogQuote(scored.quote) },
                            onShare = {
                                shareQuoteViaIntent(
                                    context = context,
                                    quoteText = scored.quote.text,
                                    author = scored.quote.author,
                                    sourceWork = scored.quote.sourceWork
                                )
                            },
                            onOpenDossier = { onOpenDossier(scored.quote) },
                            onDiscuss = { onDiscussInChat(scored.quote) }
                        )
                    }
                }
            }
        }
    }

    editingQuote?.let { doc ->
        var updatedNote by remember(doc) { mutableStateOf(doc.personalNote) }
        AlertDialog(
            onDismissRequest = { editingQuote = null },
            title = { Text("Edit Marginalia Note") },
            text = {
                OutlinedTextField(
                    value = updatedNote,
                    onValueChange = { updatedNote = it },
                    label = { Text("Personal reflection") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUpdateNote(doc.id, updatedNote)
                        editingQuote = null
                    }
                ) {
                    Text("Save Note")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingQuote = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun SavedQuoteArchiveCard(
    doc: SavedQuoteDocument,
    onEditNote: () -> Unit,
    onShare: () -> Unit,
    onOpenDossier: () -> Unit,
    onDiscuss: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("saved_quote_card_${doc.id}"),
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
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = doc.category.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                Text(
                    text = "${doc.onlinePopularityScore}% Online Resonance",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "“${doc.quoteText}”",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "— ${doc.author}" + if (doc.sourceWork.isNotBlank()) " (${doc.sourceWork})" else "",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )

            if (doc.personalNote.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Marginalia: ${doc.personalNote}",
                        style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Archival Provenance Stamp Box (Location, Date, Time)
            Surface(
                color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.55f),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = "Saved location stamp",
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = doc.locationStamp,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = "Saved date stamp",
                            tint = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = doc.dateStamp,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = "Saved time stamp",
                            tint = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = doc.timeStamp,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(top = 10.dp, bottom = 4.dp),
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onOpenDossier) {
                    Icon(
                        imageVector = Icons.Default.Psychology,
                        contentDescription = "Open AI Dossier",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Dossier")
                }

                Row {
                    IconButton(onClick = onEditNote) {
                        Icon(
                            imageVector = Icons.Default.EditNote,
                            contentDescription = "Edit note"
                        )
                    }
                    IconButton(onClick = onDiscuss) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Chat,
                            contentDescription = "Discuss in chat"
                        )
                    }
                    IconButton(onClick = onShare) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share saved quote"
                        )
                    }
                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Delete saved quote",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CatalogQuoteLibraryCard(
    scoredQuote: PriorityScoredQuote,
    onSaveWithStamp: () -> Unit,
    onShare: () -> Unit,
    onOpenDossier: () -> Unit,
    onDiscuss: () -> Unit
) {
    val quote = scoredQuote.quote
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("catalog_quote_card_${quote.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
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
                        text = quote.category.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                Text(
                    text = "Priority ${scoredQuote.totalPriorityScore} • ${quote.onlinePopularityScore}% Online",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "“${quote.text}”",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "— ${quote.author} • ${quote.sourceWork}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onSaveWithStamp,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.BookmarkBorder,
                        contentDescription = "Save with location and time stamp",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Save & Stamp")
                }

                Row {
                    IconButton(onClick = onOpenDossier) {
                        Icon(
                            imageVector = Icons.Default.Psychology,
                            contentDescription = "AI Dossier"
                        )
                    }
                    IconButton(onClick = onDiscuss) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Chat,
                            contentDescription = "Discuss in Symposium"
                        )
                    }
                    IconButton(onClick = onShare) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share quote"
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyArchivePlaceholder(
    isFiltered: Boolean,
    onSwitchToCatalog: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp, horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.BookmarkBorder,
            contentDescription = "Empty archive icon",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = if (isFiltered) "No stamped quotes match your filter" else "Your Stamped Archive Is Empty",
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Save quotes from the Discover tab, Photo Matcher, or Quote Library to record their location, date, and time stamps here.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedButton(onClick = onSwitchToCatalog) {
            Text("Browse Full Quote Library")
        }
    }
}

private fun SavedQuoteDocument.toDomainQuoteItem(): QuoteItem = QuoteItem(
    id = id.ifBlank { "saved_item" },
    text = quoteText,
    author = author,
    sourceWork = sourceWork,
    category = category,
    tags = listOf(category),
    onlinePopularityScore = onlinePopularityScore.toInt(),
    popularOnlineContext = "Saved in your personal Commonplace archive ($locationStamp)."
)
