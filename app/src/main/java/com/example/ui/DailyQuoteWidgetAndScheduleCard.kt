package com.example.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.DailyQuoteScheduleState
import com.example.data.QuoteItem
import com.example.ui.theme.LocalAccessibilitySettings
import com.example.widget.DailyQuoteScheduler

/**
 * Interactive card on QuoteDiscoveryScreen that:
 * 1. Previews & syncs the Android Home Screen Widget displaying the Daily Quote with its archival timestamp.
 * 2. Lets the user schedule a daily 'Quote of the Day' notification at a user-selected time
 *    using the existing quote retrieval logic.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyQuoteWidgetAndScheduleCard(
    scheduleState: DailyQuoteScheduleState,
    dailyQuote: QuoteItem,
    previewLocation: String,
    previewDate: String,
    previewTime: String,
    onUpdateSchedule: (Boolean, Int, Int, Boolean) -> Unit,
    onSendTestNotificationNow: () -> Unit,
    onPinOrSyncHomeWidget: () -> Unit
) {
    val context = LocalContext.current
    val accessibility = LocalAccessibilitySettings.current
    var showTimePickerDialog by remember { mutableStateOf(false) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        onUpdateSchedule(
            true,
            scheduleState.scheduledHour,
            scheduleState.scheduledMinute,
            scheduleState.usePriorityEngineForDailyPush
        )
    }

    val archivalStampPreview = remember(previewLocation, previewDate, previewTime, scheduleState.lastSyncedArchivalStamp) {
        if (previewDate.isNotBlank()) {
            DailyQuoteScheduler.buildArchivalStampString(previewLocation, previewDate, previewTime)
        } else {
            scheduleState.lastSyncedArchivalStamp
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("daily_quote_widget_schedule_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (accessibility.highContrastMode) {
                MaterialTheme.colorScheme.surface
            } else {
                Color(0xFFF6EFE2)
            }
        ),
        border = BorderStroke(
            width = if (accessibility.highContrastMode) 2.dp else 1.dp,
            color = if (accessibility.highContrastMode) MaterialTheme.colorScheme.outline else Color(0xFFC8A96E)
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(
                        imageVector = Icons.Default.NotificationsActive,
                        contentDescription = "Scheduled Quote of the Day Notification",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "QUOTE OF THE DAY • WIDGET & DAILY PUSH",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = scheduleState.nextScheduledLabel,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Switch(
                    checked = scheduleState.notificationsEnabled,
                    onCheckedChange = { enabled ->
                        if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            onUpdateSchedule(
                                enabled,
                                scheduleState.scheduledHour,
                                scheduleState.scheduledMinute,
                                scheduleState.usePriorityEngineForDailyPush
                            )
                        }
                    },
                    modifier = Modifier.testTag("daily_quote_notification_switch")
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Live Home Screen Widget Preview Box
            Surface(
                color = if (accessibility.highContrastMode) {
                    MaterialTheme.colorScheme.surfaceVariant
                } else {
                    Color(0xFFFAF4E8)
                },
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color(0xFFC8A96E)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("home_widget_live_preview")
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "HOME SCREEN WIDGET PREVIEW",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = dailyQuote.category.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "“${dailyQuote.text}”",
                        style = MaterialTheme.typography.bodySmall,
                        fontStyle = FontStyle.Italic,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "— ${dailyQuote.author} (${dailyQuote.sourceWork})",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Stamp: $archivalStampPreview",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Schedule Time & Retrieval Source Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = { showTimePickerDialog = true },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("select_daily_notification_time_button"),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AccessTime,
                        contentDescription = "Select notification time",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Time: ${scheduleState.formattedTimeLabel}")
                }

                FilterChip(
                    selected = scheduleState.usePriorityEngineForDailyPush,
                    onClick = {
                        onUpdateSchedule(
                            scheduleState.notificationsEnabled,
                            scheduleState.scheduledHour,
                            scheduleState.scheduledMinute,
                            !scheduleState.usePriorityEngineForDailyPush
                        )
                    },
                    label = {
                        Text(
                            if (scheduleState.usePriorityEngineForDailyPush) "Priority Engine" else "Daily Catalog",
                            style = MaterialTheme.typography.labelSmall
                        )
                    },
                    modifier = Modifier.testTag("toggle_daily_push_source_chip")
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onPinOrSyncHomeWidget,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("pin_or_sync_widget_button"),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Widgets,
                        contentDescription = "Pin or sync Home Screen widget",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Sync / Pin Widget")
                }

                OutlinedButton(
                    onClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                        onSendTestNotificationNow()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("send_test_daily_notification_button"),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Push Quote of the Day notification now",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Push Quote Now")
                }
            }
        }
    }

    if (showTimePickerDialog) {
        val timePickerState = rememberTimePickerState(
            initialHour = scheduleState.scheduledHour,
            initialMinute = scheduleState.scheduledMinute,
            is24Hour = false
        )
        AlertDialog(
            onDismissRequest = { showTimePickerDialog = false },
            title = { Text("Select 'Quote of the Day' Time") },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    TimePicker(state = timePickerState)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showTimePickerDialog = false
                        onUpdateSchedule(
                            true,
                            timePickerState.hour,
                            timePickerState.minute,
                            scheduleState.usePriorityEngineForDailyPush
                        )
                    },
                    modifier = Modifier.testTag("confirm_daily_notification_time_button")
                ) {
                    Text("Save Schedule")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePickerDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
