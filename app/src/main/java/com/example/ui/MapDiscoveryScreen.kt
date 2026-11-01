package com.example.ui

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.PublicOff
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.FamilyCircle
import com.example.data.FriendLocationPrecision
import com.example.data.FriendPrivacyPermission
import com.example.data.MapPinFilterMode
import com.example.data.QuoteAudioSceneState
import com.example.data.QuoteItem
import com.example.data.WorldMapUiState
import com.example.data.ZeroTrustMapPinUiItem
import com.example.ui.theme.LocalAccessibilitySettings

/**
 * Future-ready Google Maps SDK tile provider contract.
 * Currently defaults to `ARCHIVAL_CARTOGRAPHIC_ATLAS` so the map works out of the box without
 * requiring an external `MAPS_API_KEY`, while keeping camera state, marker coordinates, and style JSON
 * ready for one-line Google Maps SDK tile activation in the future.
 */
object GoogleMapsSdkFutureWiring {
    const val ARCHIVAL_SEPIA_MAP_STYLE_JSON = """
    [
      {"elementType": "geometry", "stylers": [{"color": "#ebe3cd"}]},
      {"elementType": "labels.text.fill", "stylers": [{"color": "#523735"}]},
      {"elementType": "labels.text.stroke", "stylers": [{"color": "#f5f1e6"}]},
      {"featureType": "water", "elementType": "geometry.fill", "stylers": [{"color": "#d6c7a9"}]}
    ]
    """

    const val HIGH_CONTRAST_MAP_STYLE_JSON = """
    [
      {"elementType": "geometry", "stylers": [{"color": "#0a141e"}]},
      {"elementType": "labels.text.fill", "stylers": [{"color": "#ffffff"}]},
      {"elementType": "labels.text.stroke", "stylers": [{"color": "#000000"}]},
      {"featureType": "water", "elementType": "geometry.fill", "stylers": [{"color": "#04080e"}]}
    ]
    """

    fun isMapsSdkConfigured(): Boolean = false
}

/**
 * Primary MapDiscoveryScreen that allows users to drop a daily, zero-trust geolocated pin for a quote,
 * ensuring user anonymity by only revealing location data to connected friends in their circle.
 * Paired with `PrivacyControlOverlay` for global visibility, per-friend location precision, and
 * trusted-connections filtering.
 */
@Composable
fun MapDiscoveryScreen(
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
    onUpdateFriendSharingPermission: (String, Boolean, FriendLocationPrecision) -> Unit = { _, _, _ -> },
    onTogglePrivacyOverlay: (Boolean) -> Unit = {},
    onReadAloud: (QuoteItem) -> Unit = {},
    onGenerate30sMusicScene: (QuoteItem) -> Unit = {},
    onStopAudio: () -> Unit = {}
) {
    QuoteWorldMapScreen(
        mapState = mapState,
        featuredQuote = featuredQuote,
        catalogQuotes = catalogQuotes,
        familyCircles = familyCircles,
        audioState = audioState,
        onSelectPin = onSelectPin,
        onSelectFilterMode = onSelectFilterMode,
        onPinDailyQuoteToMap = onPinDailyQuoteToMap,
        onRefreshGpsLocation = onRefreshGpsLocation,
        onConnectFriendEmail = onConnectFriendEmail,
        onSavePinnedQuoteToArchive = onSavePinnedQuoteToArchive,
        onDismissStatusBanner = onDismissStatusBanner,
        onToggleGlobalMapVisibility = onToggleGlobalMapVisibility,
        onToggleTrustedConnectionsOnly = onToggleTrustedConnectionsOnly,
        onUpdateFriendSharingPermission = onUpdateFriendSharingPermission,
        onTogglePrivacyOverlay = onTogglePrivacyOverlay,
        onReadAloud = onReadAloud,
        onGenerate30sMusicScene = onGenerate30sMusicScene,
        onStopAudio = onStopAudio
    )
}

/**
 * PrivacyControlOverlay for MapDiscoveryScreen.
 * Enables users to:
 * 1. Toggle their global map visibility (when OFF, hides from public map and shares only with permitted friends).
 * 2. Toggle viewing only pins from their trusted connections.
 * 3. Manage per-friend location sharing permissions and select between Exact GPS & Venue vs. Coarse Neighborhood precision.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PrivacyControlOverlay(
    globalMapVisibilityEnabled: Boolean,
    trustedConnectionsOnlyFilter: Boolean,
    friendPermissions: List<FriendPrivacyPermission>,
    hasPinnedToday: Boolean,
    onToggleGlobalMapVisibility: (Boolean) -> Unit,
    onToggleTrustedConnectionsOnly: (Boolean) -> Unit,
    onUpdateFriendSharingPermission: (String, Boolean, FriendLocationPrecision) -> Unit,
    onAddTrustedFriendEmail: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accessibility = LocalAccessibilitySettings.current
    var newFriendEmail by remember { mutableStateOf("") }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("privacy_control_overlay"),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp,
        border = BorderStroke(
            width = if (accessibility.highContrastMode) 2.5.dp else 1.5.dp,
            color = MaterialTheme.colorScheme.primary
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = "Zero-Trust Privacy Controls",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "ZERO-TRUST PRIVACY CONTROL OVERLAY",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Cryptographic split between public cipher pins & friend-gated identity",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("close_privacy_overlay_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close privacy control overlay"
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(12.dp))

            // 1. Global Map Visibility Toggle
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (globalMapVisibilityEnabled) {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
                    } else {
                        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.85f)
                    }
                ),
                border = BorderStroke(
                    width = 1.dp,
                    color = if (globalMapVisibilityEnabled) {
                        MaterialTheme.colorScheme.outlineVariant
                    } else {
                        MaterialTheme.colorScheme.primary
                    }
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
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
                                imageVector = if (globalMapVisibilityEnabled) {
                                    Icons.Default.Public
                                } else {
                                    Icons.Default.PublicOff
                                },
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Global Public Map Visibility",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (globalMapVisibilityEnabled) {
                                        "ON: Quote appears on public map as an anonymous cipher (~2.5 km grid). Identity hidden from strangers."
                                    } else {
                                        "OFF: Hidden from public map. Your daily quote pin is shared ONLY with permitted friends below."
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Switch(
                            checked = globalMapVisibilityEnabled,
                            onCheckedChange = onToggleGlobalMapVisibility,
                            modifier = Modifier.testTag("global_visibility_switch")
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 2. View Only Pins from Trusted Connections Toggle
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
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
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Groups,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "View Trusted Connections Only",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Filter map to display only verified pins from scholars in your connected circles.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Switch(
                        checked = trustedConnectionsOnlyFilter,
                        onCheckedChange = onToggleTrustedConnectionsOnly,
                        modifier = Modifier.testTag("trusted_connections_only_switch")
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 3. Per-Friend Location Sharing Permissions & Precision
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "PER-FRIEND LOCATION SHARING & PRECISION (${friendPermissions.count { it.isSharingEnabled }}/${friendPermissions.size} PERMITTED)",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Configure which friends in your circles can unlock your pin identity in /map_pin_identities and choose whether each friend sees Exact GPS & Venue or Coarse Neighborhood (~2.5 km) coordinates.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(10.dp))

            if (friendPermissions.isEmpty()) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "No connected friends yet. Add a friend's email below or create a Family/Reading Circle to configure per-friend location sharing.",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            } else {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    friendPermissions.forEach { perm ->
                        FriendPermissionRowCard(
                            permission = perm,
                            onToggleEnabled = { enabled ->
                                onUpdateFriendSharingPermission(perm.friendId, enabled, perm.precision)
                            },
                            onSelectPrecision = { precision ->
                                onUpdateFriendSharingPermission(perm.friendId, perm.isSharingEnabled, precision)
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Quick Add Trusted Friend to ACL
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = newFriendEmail,
                    onValueChange = { newFriendEmail = it },
                    label = { Text("Add Trusted Friend Email") },
                    placeholder = { Text("scholar@example.com") },
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("privacy_overlay_add_friend_input")
                )
                Button(
                    onClick = {
                        if (newFriendEmail.contains("@")) {
                            onAddTrustedFriendEmail(newFriendEmail.trim())
                            newFriendEmail = ""
                        }
                    },
                    enabled = newFriendEmail.contains("@"),
                    modifier = Modifier
                        .height(52.dp)
                        .testTag("privacy_overlay_add_friend_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PersonAdd,
                        contentDescription = "Authorize friend",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Permit")
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Future Google Maps SDK Wiring Status Row
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Map,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Map Engine: Interactive Archival Cartographic Atlas active (Google Maps SDK adapter wired for future API key activation).",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (hasPinnedToday) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Active Daily Pin Synced: Changes here immediately update your /map_quote_pins visibility and /map_pin_identities Zero-Trust ACL in Firestore.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FriendPermissionRowCard(
    permission: FriendPrivacyPermission,
    onToggleEnabled: (Boolean) -> Unit,
    onSelectPrecision: (FriendLocationPrecision) -> Unit
) {
    val safeTagId = permission.friendId.replace(Regex("[^a-zA-Z0-9_]"), "_")
    Surface(
        color = if (permission.isSharingEnabled) {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        },
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(
            width = 1.dp,
            color = if (permission.isSharingEnabled) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
            } else {
                MaterialTheme.colorScheme.outlineVariant
            }
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("friend_permission_card_$safeTagId")
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(
                                if (permission.isSharingEnabled) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surface
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (permission.isSharingEnabled) {
                                Icons.Default.VerifiedUser
                            } else {
                                Icons.Default.Lock
                            },
                            contentDescription = null,
                            tint = if (permission.isSharingEnabled) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = permission.displayName,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "${permission.email} • ${permission.circleName}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Switch(
                    checked = permission.isSharingEnabled,
                    onCheckedChange = onToggleEnabled,
                    modifier = Modifier.testTag("per_friend_permission_toggle_$safeTagId")
                )
            }

            AnimatedVisibility(visible = permission.isSharingEnabled) {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FriendLocationPrecision.entries.forEach { mode ->
                            val isSelected = permission.precision == mode
                            FilterChip(
                                selected = isSelected,
                                onClick = { onSelectPrecision(mode) },
                                label = {
                                    Text(
                                        text = mode.label,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = if (mode == FriendLocationPrecision.EXACT_GPS) {
                                            Icons.Default.GpsFixed
                                        } else {
                                            Icons.Default.LocationCity
                                        },
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                },
                                modifier = Modifier.testTag("per_friend_precision_chip_${safeTagId}_${mode.wireCode}")
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Dialog wrapper for PrivacyControlOverlay when opened as a modal sheet.
 */
@Composable
fun PrivacyControlOverlayDialog(
    globalMapVisibilityEnabled: Boolean,
    trustedConnectionsOnlyFilter: Boolean,
    friendPermissions: List<FriendPrivacyPermission>,
    hasPinnedToday: Boolean,
    onToggleGlobalMapVisibility: (Boolean) -> Unit,
    onToggleTrustedConnectionsOnly: (Boolean) -> Unit,
    onUpdateFriendSharingPermission: (String, Boolean, FriendLocationPrecision) -> Unit,
    onAddTrustedFriendEmail: (String) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 24.dp)
                .verticalScroll(rememberScrollState()),
            contentAlignment = Alignment.Center
        ) {
            PrivacyControlOverlay(
                globalMapVisibilityEnabled = globalMapVisibilityEnabled,
                trustedConnectionsOnlyFilter = trustedConnectionsOnlyFilter,
                friendPermissions = friendPermissions,
                hasPinnedToday = hasPinnedToday,
                onToggleGlobalMapVisibility = onToggleGlobalMapVisibility,
                onToggleTrustedConnectionsOnly = onToggleTrustedConnectionsOnly,
                onUpdateFriendSharingPermission = onUpdateFriendSharingPermission,
                onAddTrustedFriendEmail = onAddTrustedFriendEmail,
                onDismiss = onDismiss
            )
        }
    }
}
