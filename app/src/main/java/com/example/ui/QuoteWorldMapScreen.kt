package com.example.ui

import android.Manifest
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.FamilyCircle
import com.example.data.MapPinFilterMode
import com.example.data.QuoteAudioSceneState
import com.example.data.QuoteItem
import com.example.data.WorldMapUiState
import com.example.data.ZeroTrustMapCoordinatesHelper
import com.example.data.ZeroTrustMapPinUiItem
import com.example.ui.theme.LocalAccessibilitySettings
import java.util.Locale
import kotlin.math.hypot

/**
 * Multi-User Location-Based Zero-Trust Quote Map Screen.
 * - Features a once-a-day "Pin Quote" action (`/map_quote_pins/{uid}_{dayKey}`).
 * - Displays all users' pinned quotes on an interactive visual world/regional map.
 * - Zero-Trust privacy enforcement: public pins store zero PII (only coarse 0.01° coordinates
 *   and an anonymous cipher); identity, email, and personal marginalia live in `/map_pin_identities/{pinId}`
 *   and are only unlocked if the viewer is friended/connected via Family Social Circles.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun QuoteWorldMapScreen(
    mapState: WorldMapUiState,
    featuredQuote: QuoteItem,
    catalogQuotes: List<QuoteItem>,
    familyCircles: List<FamilyCircle>,
    audioState: QuoteAudioSceneState = QuoteAudioSceneState(),
    onSelectPin: (ZeroTrustMapPinUiItem) -> Unit,
    onSelectFilterMode: (MapPinFilterMode) -> Unit,
    onPinDailyQuoteToMap: (QuoteItem, String, String?) -> Unit,
    onRefreshGpsLocation: () -> Unit,
    onConnectFriendEmail: (String) -> Unit,
    onSavePinnedQuoteToArchive: (ZeroTrustMapPinUiItem) -> Unit,
    onDismissStatusBanner: () -> Unit,
    onToggleGlobalMapVisibility: (Boolean) -> Unit = {},
    onToggleTrustedConnectionsOnly: (Boolean) -> Unit = {},
    onUpdateFriendSharingPermission: (String, Boolean, com.example.data.FriendLocationPrecision) -> Unit = { _, _, _ -> },
    onTogglePrivacyOverlay: (Boolean) -> Unit = {},
    onReadAloud: (QuoteItem) -> Unit = {},
    onGenerate30sMusicScene: (QuoteItem) -> Unit = {},
    onStopAudio: () -> Unit = {}
) {
    val context = LocalContext.current
    val accessibility = LocalAccessibilitySettings.current
    var showPinQuoteDialog by remember { mutableStateOf(false) }
    var showConnectFriendDialog by remember { mutableStateOf(false) }
    var showInlinePrivacyOverlay by remember(mapState.isPrivacyOverlayVisible) {
        mutableStateOf(mapState.isPrivacyOverlayVisible)
    }

    var mapZoom by remember { mutableFloatStateOf(1.25f) }
    var mapPanX by remember { mutableFloatStateOf(0f) }
    var mapPanY by remember { mutableFloatStateOf(0f) }

    val gpsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        onRefreshGpsLocation()
    }

    val filteredPins = remember(
        mapState.pins,
        mapState.filterMode,
        mapState.trustedConnectionsOnlyFilter
    ) {
        if (mapState.trustedConnectionsOnlyFilter) {
            mapState.pins.filter { it.isConnectedFriend || it.isCurrentUserOwner }
        } else {
            when (mapState.filterMode) {
                MapPinFilterMode.ALL_WORLD_PINS -> mapState.pins
                MapPinFilterMode.CONNECTED_FRIENDS_ONLY ->
                    mapState.pins.filter { it.isConnectedFriend || it.isCurrentUserOwner }
                MapPinFilterMode.MY_DAILY_PIN ->
                    mapState.pins.filter { it.isCurrentUserOwner }
            }
        }
    }

    val activeSelectedPin = mapState.selectedPin ?: filteredPins.firstOrNull()
    val permittedFriendCount = remember(mapState.friendPermissions, familyCircles) {
        if (mapState.friendPermissions.isNotEmpty()) {
            mapState.friendPermissions.count { it.isSharingEnabled }
        } else {
            familyCircles.flatMap { it.memberEmails }.distinct().size
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("map_discovery_screen"),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 680.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .testTag("quote_world_map_screen")
        ) {
            // Zero-Trust Architecture & Daily Pin Status Header
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(
                        alpha = if (accessibility.highContrastMode) 1f else 0.7f
                    )
                ),
                border = BorderStroke(
                    width = if (accessibility.highContrastMode) 2.dp else 1.dp,
                    color = MaterialTheme.colorScheme.outline
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("zero_trust_map_header_card")
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = "Zero-Trust Map Protection",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "ZERO-TRUST LITERARY MAP DISCOVERY",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = if (mapState.globalMapVisibilityEnabled) {
                                        "Global Visibility ON (Anon Cipher) • $permittedFriendCount Friends Permitted"
                                    } else {
                                        "Global Visibility OFF • Shared ONLY with $permittedFriendCount Permitted Friends"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.testTag("global_visibility_status_badge")
                                )
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            AssistChip(
                                onClick = {
                                    val next = !showInlinePrivacyOverlay
                                    showInlinePrivacyOverlay = next
                                    onTogglePrivacyOverlay(next)
                                },
                                label = {
                                    Text(
                                        text = "Privacy Shield",
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = "Open Zero-Trust Privacy Control Overlay",
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                modifier = Modifier.testTag("privacy_control_overlay_button")
                            )

                            AssistChip(
                                onClick = { showConnectFriendDialog = true },
                                label = {
                                    Text(
                                        text = if (permittedFriendCount > 0) "$permittedFriendCount Circle" else "Connect",
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.PersonAdd,
                                        contentDescription = "Connect friend to reveal map identity",
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                modifier = Modifier.testTag("map_connect_friend_chip")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Primary Once-a-Day Pin Quote Action Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { showPinQuoteDialog = true },
                            enabled = !mapState.hasPinnedToday && !mapState.isPinningInProgress,
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .testTag("pin_quote_to_map_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                disabledContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                                disabledContentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        ) {
                            if (mapState.isPinningInProgress) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Pinning Quote…")
                            } else if (mapState.hasPinnedToday) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Already pinned today",
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Pinned Today (1/Day Limit Used)",
                                    style = MaterialTheme.typography.titleSmall
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.PushPin,
                                    contentDescription = "Pin quote to map once a day",
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Pin Quote (1/Day)",
                                    style = MaterialTheme.typography.titleSmall
                                )
                            }
                        }

                        OutlinedButton(
                            onClick = {
                                gpsPermissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                    )
                                )
                            },
                            modifier = Modifier
                                .height(50.dp)
                                .testTag("map_refresh_gps_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MyLocation,
                                contentDescription = "Refresh GPS coordinates",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("GPS")
                        }
                    }
                }
            }

            // Status Banner
            AnimatedVisibility(visible = mapState.statusBannerMessage != null) {
                mapState.statusBannerMessage?.let { msg ->
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp)
                            .testTag("world_map_status_banner")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.VerifiedUser,
                                contentDescription = "Zero-trust map notification",
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = msg,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(onClick = onDismissStatusBanner) {
                                Text("Dismiss")
                            }
                        }
                    }
                }
            }

            // Expandable Zero-Trust Privacy Control Overlay
            AnimatedVisibility(visible = showInlinePrivacyOverlay) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    PrivacyControlOverlay(
                        globalMapVisibilityEnabled = mapState.globalMapVisibilityEnabled,
                        trustedConnectionsOnlyFilter = mapState.trustedConnectionsOnlyFilter,
                        friendPermissions = mapState.friendPermissions,
                        hasPinnedToday = mapState.hasPinnedToday,
                        onToggleGlobalMapVisibility = onToggleGlobalMapVisibility,
                        onToggleTrustedConnectionsOnly = onToggleTrustedConnectionsOnly,
                        onUpdateFriendSharingPermission = onUpdateFriendSharingPermission,
                        onAddTrustedFriendEmail = onConnectFriendEmail,
                        onDismiss = {
                            showInlinePrivacyOverlay = false
                            onTogglePrivacyOverlay(false)
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Filter Mode Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                MapPinFilterMode.entries.forEach { mode ->
                    val selected = mapState.filterMode == mode
                    FilterChip(
                        selected = selected,
                        onClick = { onSelectFilterMode(mode) },
                        label = { Text(mode.label) },
                        leadingIcon = if (selected) {
                            {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected map filter",
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        } else null,
                        modifier = Modifier.testTag("map_filter_${mode.name.lowercase()}")
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Interactive Visual Google Map / Cartographic Atlas Canvas
            Card(
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(
                    width = if (accessibility.highContrastMode) 2.5.dp else 1.5.dp,
                    color = if (accessibility.highContrastMode) MaterialTheme.colorScheme.outline else Color(0xFFC8A96E)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(320.dp)
                    .testTag("interactive_visual_quote_map_canvas")
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    InteractiveQuoteAtlasCanvas(
                        pins = filteredPins,
                        selectedPin = activeSelectedPin,
                        userLat = mapState.userCurrentLat,
                        userLon = mapState.userCurrentLon,
                        zoom = mapZoom,
                        panX = mapPanX,
                        panY = mapPanY,
                        highContrast = accessibility.highContrastMode,
                        onTransform = { zoomDelta, panDelta ->
                            mapZoom = (mapZoom * zoomDelta).coerceIn(1.0f, 4.0f)
                            val maxPan = 260f * (mapZoom - 1f) + 80f
                            mapPanX = (mapPanX + panDelta.x).coerceIn(-maxPan, maxPan)
                            mapPanY = (mapPanY + panDelta.y).coerceIn(-maxPan, maxPan)
                        },
                        onTapPin = onSelectPin
                    )

                    // Top-left Legend Overlay
                    Surface(
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            LegendDot(color = Color(0xFFD99B26), label = "You")
                            LegendDot(color = Color(0xFF2E8B57), label = "Friend (Unlocked)")
                            LegendDot(color = Color(0xFFA8422B), label = "Public (Anon)")
                        }
                    }

                    // Bottom-right Map Zoom & Reset Controls
                    Surface(
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { mapZoom = (mapZoom * 1.25f).coerceAtMost(4.0f) },
                                modifier = Modifier.testTag("map_zoom_in_button")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Zoom in map")
                            }
                            IconButton(
                                onClick = { mapZoom = (mapZoom / 1.25f).coerceAtLeast(1.0f) },
                                modifier = Modifier.testTag("map_zoom_out_button")
                            ) {
                                Icon(Icons.Default.Remove, contentDescription = "Zoom out map")
                            }
                            IconButton(
                                onClick = {
                                    mapZoom = 1.25f
                                    mapPanX = 0f
                                    mapPanY = 0f
                                },
                                modifier = Modifier.testTag("map_reset_view_button")
                            ) {
                                Icon(Icons.Default.RestartAlt, contentDescription = "Reset map view")
                            }
                        }
                    }

                    // Bottom-left Live Coordinate Readout
                    Surface(
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(10.dp)
                    ) {
                        val coarseLat = ZeroTrustMapCoordinatesHelper.roundToCoarseGrid(mapState.userCurrentLat)
                        val coarseLon = ZeroTrustMapCoordinatesHelper.roundToCoarseGrid(mapState.userCurrentLon)
                        Text(
                            text = "${mapState.userCurrentRegionLabel} ($coarseLat°, $coarseLon°)",
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Horizontal Quick-Selector Strip of Pins on the Map
            Text(
                text = "ACTIVE MAP QUOTE PINS (${filteredPins.size})",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                filteredPins.forEach { item ->
                    val isSelected = activeSelectedPin?.pin?.id == item.pin.id
                    Surface(
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        },
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                        ),
                        modifier = Modifier
                            .width(220.dp)
                            .clickable { onSelectPin(item) }
                            .testTag("map_pin_chip_${item.pin.id}")
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = item.pin.regionLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Icon(
                                    imageVector = if (item.isConnectedFriend || item.isCurrentUserOwner) {
                                        Icons.Default.VerifiedUser
                                    } else {
                                        Icons.Default.Lock
                                    },
                                    contentDescription = if (item.isConnectedFriend || item.isCurrentUserOwner) {
                                        "Connected identity unlocked"
                                    } else {
                                        "Zero-Trust anonymous pin"
                                    },
                                    tint = if (item.isConnectedFriend || item.isCurrentUserOwner) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "“${item.pin.quoteText}”",
                                style = MaterialTheme.typography.bodySmall,
                                fontStyle = FontStyle.Italic,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "— ${item.pin.quoteAuthor}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Selected Pin Zero-Trust Inspector Card
            activeSelectedPin?.let { selected ->
                SelectedMapPinInspectorCard(
                    item = selected,
                    audioState = audioState,
                    onSaveToArchive = { onSavePinnedQuoteToArchive(selected) },
                    onOpenInGoogleMaps = {
                        val lat = selected.pin.latitude
                        val lon = selected.pin.longitude
                        val encodedLabel = Uri.encode("${selected.pin.quoteAuthor}: ${selected.pin.regionLabel}")
                        val gmmIntentUri = Uri.parse("geo:$lat,$lon?q=$lat,$lon($encodedLabel)")
                        val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                        try {
                            context.startActivity(mapIntent)
                        } catch (_: Exception) {
                            val webUri = Uri.parse(
                                String.format(Locale.US, "https://www.google.com/maps/search/?api=1&query=%f,%f", lat, lon)
                            )
                            try {
                                context.startActivity(Intent(Intent.ACTION_VIEW, webUri))
                            } catch (_: Exception) {
                            }
                        }
                    },
                    onOpenConnectDialog = { showConnectFriendDialog = true },
                    onReadAloud = onReadAloud,
                    onGenerate30sMusicScene = onGenerate30sMusicScene,
                    onStopAudio = onStopAudio
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showPinQuoteDialog) {
        PinDailyQuoteToMapDialog(
            featuredQuote = featuredQuote,
            catalogQuotes = catalogQuotes,
            currentRegionLabel = mapState.userCurrentRegionLabel,
            currentLat = mapState.userCurrentLat,
            currentLon = mapState.userCurrentLon,
            connectedCircleCount = familyCircles.size,
            onDismiss = { showPinQuoteDialog = false },
            onConfirmPin = { chosenQuote, marginalia, customRegion ->
                showPinQuoteDialog = false
                onPinDailyQuoteToMap(chosenQuote, marginalia, customRegion)
            }
        )
    }

    if (showConnectFriendDialog) {
        ConnectMapFriendDialog(
            familyCircles = familyCircles,
            onDismiss = { showConnectFriendDialog = false },
            onConnectEmail = { email ->
                showConnectFriendDialog = false
                onConnectFriendEmail(email)
            }
        )
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = label, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun InteractiveQuoteAtlasCanvas(
    pins: List<ZeroTrustMapPinUiItem>,
    selectedPin: ZeroTrustMapPinUiItem?,
    userLat: Double,
    userLon: Double,
    zoom: Float,
    panX: Float,
    panY: Float,
    highContrast: Boolean,
    onTransform: (Float, Offset) -> Unit,
    onTapPin: (ZeroTrustMapPinUiItem) -> Unit
) {
    var canvasSize by remember { mutableStateOf(Size.Zero) }

    fun latLonToCanvasOffset(lat: Double, lon: Double, size: Size): Offset {
        val normX = ((lon + 180.0) / 360.0).toFloat().coerceIn(0.04f, 0.96f)
        val normY = ((90.0 - lat) / 180.0).toFloat().coerceIn(0.06f, 0.94f)
        val centerX = size.width * 0.5f
        val centerY = size.height * 0.5f
        val baseX = normX * size.width
        val baseY = normY * size.height
        val x = (baseX - centerX) * zoom + centerX + panX
        val y = (baseY - centerY) * zoom + centerY + panY
        return Offset(x, y)
    }

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(pins, zoom, panX, panY) {
                detectTransformGestures { _, pan, gestureZoom, _ ->
                    onTransform(gestureZoom, pan)
                }
            }
            .pointerInput(pins, zoom, panX, panY) {
                detectTapGestures { tapOffset ->
                    if (canvasSize.width > 0f && canvasSize.height > 0f) {
                        val hit = pins.minByOrNull { item ->
                            val pos = latLonToCanvasOffset(item.effectiveLatitude, item.effectiveLongitude, canvasSize)
                            hypot(tapOffset.x - pos.x, tapOffset.y - pos.y)
                        }
                        if (hit != null) {
                            val pos = latLonToCanvasOffset(hit.effectiveLatitude, hit.effectiveLongitude, canvasSize)
                            if (hypot(tapOffset.x - pos.x, tapOffset.y - pos.y) <= 44.dp.toPx()) {
                                onTapPin(hit)
                            }
                        }
                    }
                }
            }
    ) {
        canvasSize = size

        val oceanTop = if (highContrast) Color(0xFF0A141E) else Color(0xFFE5DEC9)
        val oceanBottom = if (highContrast) Color(0xFF04080E) else Color(0xFFD6C7A9)
        val landFill = if (highContrast) Color(0xFF1F3347) else Color(0xFFF3EBDD)
        val landStroke = if (highContrast) Color(0xFF8CC4FF) else Color(0xFFB59663)
        val gridLineColor = if (highContrast) Color(0xFF476A8A).copy(alpha = 0.55f) else Color(0xFF9C7E4E).copy(alpha = 0.25f)

        drawRect(
            brush = Brush.verticalGradient(listOf(oceanTop, oceanBottom))
        )

        // Draw latitude and longitude graticule lines
        for (lonDeg in -150..150 step 30) {
            val start = latLonToCanvasOffset(85.0, lonDeg.toDouble(), size)
            val end = latLonToCanvasOffset(-85.0, lonDeg.toDouble(), size)
            drawLine(
                color = gridLineColor,
                start = start,
                end = end,
                strokeWidth = if (lonDeg == 0) 2f else 1f
            )
        }
        for (latDeg in -60..60 step 30) {
            val start = latLonToCanvasOffset(latDeg.toDouble(), -175.0, size)
            val end = latLonToCanvasOffset(latDeg.toDouble(), 175.0, size)
            drawLine(
                color = gridLineColor,
                start = start,
                end = end,
                strokeWidth = if (latDeg == 0) 2f else 1f
            )
        }

        // Stylized Continental Landmass Polygons (North America, South America, Europe/Africa, Asia, Oceania)
        val continents = listOf(
            // North America
            listOf(
                65.0 to -160.0, 68.0 to -110.0, 52.0 to -60.0, 25.0 to -80.0,
                15.0 to -92.0, 32.0 to -118.0, 55.0 to -135.0
            ),
            // South America
            listOf(
                10.0 to -76.0, 5.0 to -50.0, -8.0 to -35.0, -35.0 to -55.0,
                -53.0 to -70.0, -18.0 to -72.0
            ),
            // Europe & Mediterranean
            listOf(
                66.0 to 12.0, 68.0 to 40.0, 46.0 to 40.0, 36.0 to 26.0,
                36.0 to -8.0, 48.0 to -5.0, 58.0 to 6.0
            ),
            // Africa
            listOf(
                35.0 to -6.0, 32.0 to 32.0, 11.0 to 50.0, -34.0 to 20.0,
                4.0 to 8.0, 15.0 to -17.0
            ),
            // Asia
            listOf(
                68.0 to 45.0, 70.0 to 145.0, 40.0 to 142.0, 22.0 to 114.0,
                8.0 to 77.0, 26.0 to 50.0, 45.0 to 40.0
            ),
            // Australia
            listOf(
                -14.0 to 128.0, -16.0 to 146.0, -37.0 to 148.0, -33.0 to 115.0
            )
        )

        continents.forEach { poly ->
            val path = Path()
            poly.forEachIndexed { idx, (lat, lon) ->
                val pt = latLonToCanvasOffset(lat, lon, size)
                if (idx == 0) path.moveTo(pt.x, pt.y) else path.lineTo(pt.x, pt.y)
            }
            path.close()
            drawPath(path = path, color = landFill)
            drawPath(path = path, color = landStroke, style = Stroke(width = 1.5f))
        }

        // Draw User's Live GPS Radar Ring
        val userPos = latLonToCanvasOffset(userLat, userLon, size)
        drawCircle(
            color = Color(0xFF2B6CB0).copy(alpha = 0.2f),
            radius = 18.dp.toPx(),
            center = userPos
        )
        drawCircle(
            color = Color(0xFF2B6CB0),
            radius = 5.dp.toPx(),
            center = userPos
        )

        // Draw All Pinned Quotes on the Visual Map
        pins.forEach { item ->
            val pinPos = latLonToCanvasOffset(item.effectiveLatitude, item.effectiveLongitude, size)
            val isSelected = selectedPin?.pin?.id == item.pin.id
            val pinColor = when {
                item.isCurrentUserOwner -> Color(0xFFD99B26)
                item.isConnectedFriend -> Color(0xFF2E8B57)
                else -> Color(0xFFA8422B)
            }

            if (isSelected) {
                drawCircle(
                    color = pinColor.copy(alpha = 0.28f),
                    radius = 22.dp.toPx(),
                    center = pinPos
                )
                drawCircle(
                    color = Color.White,
                    radius = 12.dp.toPx(),
                    center = pinPos
                )
            }

            drawCircle(
                color = pinColor,
                radius = if (isSelected) 10.dp.toPx() else 7.5.dp.toPx(),
                center = pinPos
            )
            drawCircle(
                color = Color(0xFFFFFBF2),
                radius = 3.dp.toPx(),
                center = pinPos
            )
        }
    }
}

@Composable
private fun SelectedMapPinInspectorCard(
    item: ZeroTrustMapPinUiItem,
    audioState: QuoteAudioSceneState,
    onSaveToArchive: () -> Unit,
    onOpenInGoogleMaps: () -> Unit,
    onOpenConnectDialog: () -> Unit,
    onReadAloud: (QuoteItem) -> Unit,
    onGenerate30sMusicScene: (QuoteItem) -> Unit,
    onStopAudio: () -> Unit
) {
    val accessibility = LocalAccessibilitySettings.current
    val isUnlocked = item.isCurrentUserOwner || item.isConnectedFriend
    val domainQuote = remember(item.pin) {
        QuoteItem(
            id = item.pin.quoteId.ifBlank { item.pin.id },
            text = item.pin.quoteText,
            author = item.pin.quoteAuthor,
            sourceWork = item.pin.sourceWork.ifBlank { "World Map Pin" },
            category = item.pin.category.ifBlank { "Philosophy" },
            tags = listOf(item.pin.category.ifBlank { "Philosophy" }),
            onlinePopularityScore = 95,
            popularOnlineContext = "Pinned on Commonplace Zero-Trust World Map (${item.pin.regionLabel})"
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("selected_map_pin_inspector_card"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (accessibility.highContrastMode) {
                MaterialTheme.colorScheme.surface
            } else {
                Color(0xFFF7EFE0)
            }
        ),
        border = BorderStroke(
            width = if (accessibility.highContrastMode) 2.dp else 1.5.dp,
            color = if (isUnlocked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = if (isUnlocked) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.secondaryContainer
                    },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isUnlocked) Icons.Default.VerifiedUser else Icons.Default.Lock,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = if (isUnlocked) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSecondaryContainer
                            }
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isUnlocked) {
                                "CONNECTED CIRCLE • IDENTITY UNLOCKED"
                            } else {
                                "ZERO-TRUST ANONYMOUS PIN • NO PII SHARED"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isUnlocked) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSecondaryContainer
                            }
                        )
                    }
                }

                Text(
                    text = item.pin.dayKey.replace("_", "-"),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "“${item.pin.quoteText}”",
                style = MaterialTheme.typography.titleMedium,
                fontStyle = FontStyle.Italic,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "— ${item.pin.quoteAuthor} (${item.pin.sourceWork.ifBlank { item.pin.category }})",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(10.dp))

            // Zero-Trust Attribution & Location Details
            Text(
                text = "Pinned By: ${item.displayAttributionLabel}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.testTag("selected_pin_attribution_text")
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Map Location: ${item.displayLocationLabel} (${String.format(Locale.US, "%.4f", item.effectiveLatitude)}°, ${String.format(Locale.US, "%.4f", item.effectiveLongitude)}°)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (isUnlocked && item.verifiedIdentity != null) {
                val cleanCircles = ZeroTrustMapCoordinatesHelper.extractDisplayCircleNames(item.verifiedIdentity.circleNames)
                if (cleanCircles.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Shared Circle(s): ${cleanCircles.joinToString(", ")}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                if (item.verifiedIdentity.personalMarginalia.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "CONNECTED FRIEND MARGINALIA",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = item.verifiedIdentity.personalMarginalia,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Zero-Trust Policy: This reader's name, email, and personal note are cryptographically isolated in /map_pin_identities and are never transmitted unless you are connected in a Family/Friend Circle.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            QuoteAudioSceneControlBar(
                quote = domainQuote,
                audioState = audioState,
                onReadAloud = onReadAloud,
                onGenerate30sMusicScene = onGenerate30sMusicScene,
                onStopAudio = onStopAudio
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onSaveToArchive,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("save_map_pin_quote_button"),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.BookmarkAdd,
                        contentDescription = "Save pinned quote",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Save Quote")
                }

                OutlinedButton(
                    onClick = onOpenInGoogleMaps,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("open_in_google_maps_button"),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Map,
                        contentDescription = "Open coordinates in Google Maps",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Google Maps")
                }

                if (!isUnlocked) {
                    OutlinedButton(
                        onClick = onOpenConnectDialog,
                        modifier = Modifier.testTag("connect_friend_from_pin_button"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Groups,
                            contentDescription = "Connect with friend",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PinDailyQuoteToMapDialog(
    featuredQuote: QuoteItem,
    catalogQuotes: List<QuoteItem>,
    currentRegionLabel: String,
    currentLat: Double,
    currentLon: Double,
    connectedCircleCount: Int,
    onDismiss: () -> Unit,
    onConfirmPin: (QuoteItem, String, String?) -> Unit
) {
    var selectedQuote by remember { mutableStateOf(featuredQuote) }
    var customRegion by remember { mutableStateOf(currentRegionLabel) }
    var friendMarginalia by remember { mutableStateOf("") }
    val coarseLat = ZeroTrustMapCoordinatesHelper.roundToCoarseGrid(currentLat)
    val coarseLon = ZeroTrustMapCoordinatesHelper.roundToCoarseGrid(currentLon)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.PushPin,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Pin Daily Quote to World Map")
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "You can pin one quote per day to the shared visual map. Public viewers only see the quote and coarse city grid ($coarseLat°, $coarseLon°). Your identity and personal note are only shared with your connected circles ($connectedCircleCount active).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "SELECT QUOTE TO PIN TODAY",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val options = remember(featuredQuote, catalogQuotes) {
                        (listOf(featuredQuote) + catalogQuotes.take(6)).distinctBy { it.id }
                    }
                    options.forEach { q ->
                        FilterChip(
                            selected = selectedQuote.id == q.id,
                            onClick = { selectedQuote = q },
                            label = { Text(q.author) },
                            modifier = Modifier.testTag("pin_dialog_quote_chip_${q.id}")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "“${selectedQuote.text}”",
                            style = MaterialTheme.typography.bodyMedium,
                            fontStyle = FontStyle.Italic
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "— ${selectedQuote.author} (${selectedQuote.sourceWork})",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = customRegion,
                    onValueChange = { customRegion = it },
                    label = { Text("Public Coarse Region / City Label") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("pin_dialog_region_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = friendMarginalia,
                    onValueChange = { friendMarginalia = it },
                    label = { Text("Connected Friends Note (Hidden from Strangers)") },
                    placeholder = { Text("Optional reflection visible only to connected friends…") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("pin_dialog_friend_note_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirmPin(
                        selectedQuote,
                        friendMarginalia,
                        customRegion.trim().ifBlank { null }
                    )
                },
                modifier = Modifier.testTag("confirm_pin_quote_to_map_button")
            ) {
                Text("Pin Quote (1/Day)")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun ConnectMapFriendDialog(
    familyCircles: List<FamilyCircle>,
    onDismiss: () -> Unit,
    onConnectEmail: (String) -> Unit
) {
    var friendEmail by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.PersonAdd,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Zero-Trust Friend Connection")
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Connecting a friend's Google email adds them to your Zero-Trust Circle ACL so you can mutually see each other's identity and marginalia on the World Map while remaining anonymous to everyone else.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = friendEmail,
                    onValueChange = { friendEmail = it },
                    label = { Text("Friend's Google Email") },
                    placeholder = { Text("scholar@example.com") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("map_connect_friend_email_input")
                )
                if (familyCircles.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Active Circles: ${familyCircles.joinToString(", ") { it.name }}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (friendEmail.contains("@")) {
                        onConnectEmail(friendEmail.trim())
                    }
                },
                enabled = friendEmail.contains("@"),
                modifier = Modifier.testTag("confirm_map_connect_friend_button")
            ) {
                Text("Connect & Authorize")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
