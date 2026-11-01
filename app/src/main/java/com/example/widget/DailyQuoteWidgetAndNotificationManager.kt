package com.example.widget

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.DailyQuoteRetrievalEngine
import com.example.data.DailyQuoteScheduleState
import com.example.data.PriorityMode
import com.example.data.QuoteItem
import com.example.data.SeedQuoteCatalog
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private const val PREFS_NAME = "commonplace_daily_quote_widget_prefs"
private const val KEY_NOTIFICATIONS_ENABLED = "notifications_enabled"
private const val KEY_SCHEDULED_HOUR = "scheduled_hour"
private const val KEY_SCHEDULED_MINUTE = "scheduled_minute"
private const val KEY_USE_PRIORITY_ENGINE = "use_priority_engine"
private const val KEY_SELECTED_INTERESTS = "selected_interests_csv"
private const val KEY_PRIORITY_MODE = "priority_mode_label"
private const val KEY_RECENT_QUOTE_IDS = "recent_quote_ids_csv"
private const val KEY_SYNCED_QUOTE_ID = "synced_quote_id"
private const val KEY_SYNCED_QUOTE_TEXT = "synced_quote_text"
private const val KEY_SYNCED_QUOTE_AUTHOR = "synced_quote_author"
private const val KEY_SYNCED_QUOTE_SOURCE = "synced_quote_source"
private const val KEY_SYNCED_QUOTE_CATEGORY = "synced_quote_category"
private const val KEY_SYNCED_STAMP = "synced_archival_stamp"
private const val KEY_WIDGET_ROTATION_OFFSET = "widget_rotation_offset"

const val ACTION_DAILY_QUOTE_NOTIFICATION = "com.example.ACTION_DAILY_QUOTE_NOTIFICATION"
const val ACTION_REFRESH_DAILY_WIDGET = "com.example.ACTION_REFRESH_DAILY_WIDGET"
const val DAILY_QUOTE_CHANNEL_ID = "commonplace_quote_of_the_day_channel"
private const val DAILY_QUOTE_NOTIFICATION_ID = 4108
private const val DAILY_QUOTE_ALARM_REQ_CODE = 4109

/**
 * Coordinates the Commonplace Home Screen Widget and Scheduled 'Quote of the Day' Notifications,
 * sharing the exact priority & daily quote retrieval logic from [DailyQuoteRetrievalEngine].
 */
object DailyQuoteScheduler {

    fun loadScheduleState(context: Context): DailyQuoteScheduleState {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val enabled = prefs.getBoolean(KEY_NOTIFICATIONS_ENABLED, false)
        val hour = prefs.getInt(KEY_SCHEDULED_HOUR, 8)
        val minute = prefs.getInt(KEY_SCHEDULED_MINUTE, 30)
        val usePriority = prefs.getBoolean(KEY_USE_PRIORITY_ENGINE, true)
        val defaultQuote = DailyQuoteRetrievalEngine.retrieveDeterministicDailyQuote(
            Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
        )
        val text = prefs.getString(KEY_SYNCED_QUOTE_TEXT, null) ?: defaultQuote.text
        val author = prefs.getString(KEY_SYNCED_QUOTE_AUTHOR, null) ?: defaultQuote.author
        val source = prefs.getString(KEY_SYNCED_QUOTE_SOURCE, null) ?: defaultQuote.sourceWork
        val quoteId = prefs.getString(KEY_SYNCED_QUOTE_ID, null) ?: defaultQuote.id
        val stamp = prefs.getString(KEY_SYNCED_STAMP, null) ?: defaultArchivalStamp()
        val timeFormatted = DailyQuoteRetrievalEngine.formatHourMinute(hour, minute)

        return DailyQuoteScheduleState(
            notificationsEnabled = enabled,
            scheduledHour = hour,
            scheduledMinute = minute,
            usePriorityEngineForDailyPush = usePriority,
            lastSyncedQuoteId = quoteId,
            lastSyncedQuoteText = text,
            lastSyncedQuoteAuthor = author,
            lastSyncedQuoteSource = source,
            lastSyncedArchivalStamp = stamp,
            nextScheduledLabel = if (enabled) "Scheduled daily at $timeFormatted" else "Paused • Tap to enable at $timeFormatted"
        )
    }

    fun syncDailyQuoteAndPreferences(
        context: Context,
        quote: QuoteItem,
        locationStamp: String,
        dateStamp: String,
        timeStamp: String,
        selectedInterests: List<String>,
        priorityMode: PriorityMode,
        recentQuoteIds: List<String>
    ): DailyQuoteScheduleState {
        val stampFormatted = buildArchivalStampString(locationStamp, dateStamp, timeStamp)
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_SYNCED_QUOTE_ID, quote.id)
            .putString(KEY_SYNCED_QUOTE_TEXT, quote.text)
            .putString(KEY_SYNCED_QUOTE_AUTHOR, quote.author)
            .putString(KEY_SYNCED_QUOTE_SOURCE, quote.sourceWork)
            .putString(KEY_SYNCED_QUOTE_CATEGORY, quote.category)
            .putString(KEY_SYNCED_STAMP, stampFormatted)
            .putString(KEY_SELECTED_INTERESTS, selectedInterests.joinToString(","))
            .putString(KEY_PRIORITY_MODE, priorityMode.label)
            .putString(KEY_RECENT_QUOTE_IDS, recentQuoteIds.joinToString(","))
            .apply()

        CommonplaceDailyQuoteWidgetProvider.updateAllWidgets(context)
        return loadScheduleState(context)
    }

    fun configureDailyQuoteSchedule(
        context: Context,
        enabled: Boolean,
        hourOfDay: Int,
        minute: Int,
        usePriorityEngine: Boolean = true
    ): DailyQuoteScheduleState {
        val h = hourOfDay.coerceIn(0, 23)
        val m = minute.coerceIn(0, 59)
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean(KEY_NOTIFICATIONS_ENABLED, enabled)
            .putInt(KEY_SCHEDULED_HOUR, h)
            .putInt(KEY_SCHEDULED_MINUTE, m)
            .putBoolean(KEY_USE_PRIORITY_ENGINE, usePriorityEngine)
            .apply()

        ensureNotificationChannel(context)

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
        val intent = Intent(context, DailyQuoteNotificationReceiver::class.java).apply {
            action = ACTION_DAILY_QUOTE_NOTIFICATION
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            DAILY_QUOTE_ALARM_REQ_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (enabled && alarmManager != null) {
            val triggerAtMillis = DailyQuoteRetrievalEngine.computeNextTriggerEpochMillis(h, m)
            // Use compliant inexact repeating daily alarm (no restricted exact alarm permission required)
            alarmManager.setInexactRepeating(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                AlarmManager.INTERVAL_DAY,
                pendingIntent
            )
        } else {
            alarmManager?.cancel(pendingIntent)
        }

        CommonplaceDailyQuoteWidgetProvider.updateAllWidgets(context)
        val timeFormatted = DailyQuoteRetrievalEngine.formatHourMinute(h, m)
        val statusMsg = if (enabled) {
            "Quote of the Day notification scheduled daily for $timeFormatted."
        } else {
            "Daily Quote notification paused."
        }
        return loadScheduleState(context).copy(statusMessage = statusMsg)
    }

    /**
     * Resolves the Quote of the Day using the user's persisted priority settings and existing
     * quote retrieval logic in [DailyQuoteRetrievalEngine].
     */
    fun retrieveCurrentQuoteForSchedule(
        context: Context,
        advanceRotation: Boolean = false
    ): Pair<QuoteItem, String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val usePriority = prefs.getBoolean(KEY_USE_PRIORITY_ENGINE, true)
        val interestsCsv = prefs.getString(KEY_SELECTED_INTERESTS, "Stoicism,Literature,Philosophy").orEmpty()
        val interests = interestsCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            .ifEmpty { listOf("Stoicism", "Literature", "Philosophy") }
        val modeLabel = prefs.getString(KEY_PRIORITY_MODE, PriorityMode.BALANCED.label).orEmpty()
        val mode = PriorityMode.entries.find { it.label == modeLabel } ?: PriorityMode.BALANCED
        val recentCsv = prefs.getString(KEY_RECENT_QUOTE_IDS, "").orEmpty()
        val recentIds = recentCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() }

        var offset = prefs.getInt(KEY_WIDGET_ROTATION_OFFSET, 0)
        if (advanceRotation) {
            offset = (offset + 1) % SeedQuoteCatalog.allQuotes.size
            prefs.edit().putInt(KEY_WIDGET_ROTATION_OFFSET, offset).apply()
        }

        val dayOfYear = Calendar.getInstance().get(Calendar.DAY_OF_YEAR) + offset
        val syncedText = prefs.getString(KEY_SYNCED_QUOTE_TEXT, null)
        val syncedAuthor = prefs.getString(KEY_SYNCED_QUOTE_AUTHOR, null)
        val syncedSource = prefs.getString(KEY_SYNCED_QUOTE_SOURCE, null)
        val syncedCategory = prefs.getString(KEY_SYNCED_QUOTE_CATEGORY, "Stoicism") ?: "Stoicism"
        val syncedId = prefs.getString(KEY_SYNCED_QUOTE_ID, "daily_synced") ?: "daily_synced"

        val syncedOverride = if (!advanceRotation && !syncedText.isNullOrBlank() && !syncedAuthor.isNullOrBlank()) {
            QuoteItem(
                id = syncedId,
                text = syncedText,
                author = syncedAuthor,
                sourceWork = syncedSource ?: "Archival Folio",
                category = syncedCategory,
                tags = listOf(syncedCategory),
                onlinePopularityScore = 96,
                popularOnlineContext = "Synced from Commonplace Daily Folio"
            )
        } else null

        val quote = DailyQuoteRetrievalEngine.resolveQuoteOfTheDay(
            dayOfYear = dayOfYear,
            usePriorityRanking = usePriority,
            selectedInterests = interests,
            priorityMode = mode,
            recentQuoteIds = recentIds,
            syncedDailyOverride = syncedOverride
        )

        val stamp = if (advanceRotation) {
            defaultArchivalStamp()
        } else {
            prefs.getString(KEY_SYNCED_STAMP, null) ?: defaultArchivalStamp()
        }

        // Persist updated quote & stamp so widget and app stay in sync
        prefs.edit()
            .putString(KEY_SYNCED_QUOTE_ID, quote.id)
            .putString(KEY_SYNCED_QUOTE_TEXT, quote.text)
            .putString(KEY_SYNCED_QUOTE_AUTHOR, quote.author)
            .putString(KEY_SYNCED_QUOTE_SOURCE, quote.sourceWork)
            .putString(KEY_SYNCED_QUOTE_CATEGORY, quote.category)
            .putString(KEY_SYNCED_STAMP, stamp)
            .apply()

        return quote to stamp
    }

    fun dispatchQuoteOfTheDayNotification(
        context: Context,
        overrideQuote: QuoteItem? = null,
        overrideStamp: String? = null
    ): Boolean {
        ensureNotificationChannel(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                return false
            }
        }

        val (resolvedQuote, resolvedStamp) = retrieveCurrentQuoteForSchedule(context, advanceRotation = false)
        val quote = overrideQuote ?: resolvedQuote
        val stamp = overrideStamp ?: resolvedStamp

        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("extra_daily_quote_id", quote.id)
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            DAILY_QUOTE_NOTIFICATION_ID,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, DAILY_QUOTE_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_edit)
            .setContentTitle("Quote of the Day • ${quote.author}")
            .setContentText("“${quote.text}”")
            .setSubText(stamp)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("“${quote.text}”\n— ${quote.author} (${quote.sourceWork})\nStamp: $stamp")
                    .setSummaryText(quote.category)
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(contentPendingIntent)
            .setAutoCancel(true)
            .build()

        return try {
            NotificationManagerCompat.from(context).notify(DAILY_QUOTE_NOTIFICATION_ID, notification)
            CommonplaceDailyQuoteWidgetProvider.updateAllWidgets(context)
            true
        } catch (e: SecurityException) {
            false
        }
    }

    fun requestPinHomeWidget(context: Context): Boolean {
        CommonplaceDailyQuoteWidgetProvider.updateAllWidgets(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val provider = ComponentName(context, CommonplaceDailyQuoteWidgetProvider::class.java)
            if (appWidgetManager != null && appWidgetManager.isRequestPinAppWidgetSupported) {
                return try {
                    appWidgetManager.requestPinAppWidget(provider, null, null)
                } catch (e: Exception) {
                    false
                }
            }
        }
        return false
    }

    fun ensureNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            val channel = NotificationChannel(
                DAILY_QUOTE_CHANNEL_ID,
                context.getString(R.string.notification_channel_daily_quote_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = context.getString(R.string.notification_channel_daily_quote_desc)
            }
            manager?.createNotificationChannel(channel)
        }
    }

    fun buildArchivalStampString(
        locationStamp: String,
        dateStamp: String,
        timeStamp: String
    ): String {
        val loc = locationStamp.ifBlank { "Archival Reading Desk" }
        val (fallbackDate, fallbackTime) = currentFormattedDateTime()
        val d = dateStamp.ifBlank { fallbackDate }
        val t = timeStamp.ifBlank { fallbackTime }
        return "$loc • $d $t"
    }

    private fun defaultArchivalStamp(): String {
        val (d, t) = currentFormattedDateTime()
        return "Archival Reading Desk • $d $t"
    }

    private fun currentFormattedDateTime(now: Date = Date()): Pair<String, String> {
        val d = SimpleDateFormat("MMM d, yyyy", Locale.US).format(now)
        val t = SimpleDateFormat("h:mm a", Locale.US).format(now)
        return d to t
    }
}

/**
 * BroadcastReceiver that fires at the user-selected time (and reschedules on device reboot)
 * to push the 'Quote of the Day' notification and refresh the Home Screen Widget.
 */
class DailyQuoteNotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action.orEmpty()
        val state = DailyQuoteScheduler.loadScheduleState(context)

        when (action) {
            Intent.ACTION_BOOT_COMPLETED -> {
                if (state.notificationsEnabled) {
                    DailyQuoteScheduler.configureDailyQuoteSchedule(
                        context = context,
                        enabled = true,
                        hourOfDay = state.scheduledHour,
                        minute = state.scheduledMinute,
                        usePriorityEngine = state.usePriorityEngineForDailyPush
                    )
                }
                CommonplaceDailyQuoteWidgetProvider.updateAllWidgets(context)
            }
            ACTION_DAILY_QUOTE_NOTIFICATION -> {
                DailyQuoteScheduler.dispatchQuoteOfTheDayNotification(context)
            }
        }
    }
}

/**
 * Android Home Screen AppWidgetProvider that displays the Daily Quote with its archival timestamp
 * and provides one-tap access to open Commonplace or cycle/refresh the archival quote.
 */
class CommonplaceDailyQuoteWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val (quote, archivalStamp) = DailyQuoteScheduler.retrieveCurrentQuoteForSchedule(
            context = context,
            advanceRotation = false
        )
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId, quote, archivalStamp)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_REFRESH_DAILY_WIDGET) {
            val (quote, archivalStamp) = DailyQuoteScheduler.retrieveCurrentQuoteForSchedule(
                context = context,
                advanceRotation = true
            )
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, CommonplaceDailyQuoteWidgetProvider::class.java)
            val ids = appWidgetManager.getAppWidgetIds(componentName)
            for (id in ids) {
                updateAppWidget(context, appWidgetManager, id, quote, archivalStamp)
            }
        }
    }

    companion object {
        fun updateAllWidgets(context: Context) {
            try {
                val appWidgetManager = AppWidgetManager.getInstance(context) ?: return
                val componentName = ComponentName(context, CommonplaceDailyQuoteWidgetProvider::class.java)
                val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
                if (appWidgetIds != null && appWidgetIds.isNotEmpty()) {
                    val (quote, stamp) = DailyQuoteScheduler.retrieveCurrentQuoteForSchedule(
                        context = context,
                        advanceRotation = false
                    )
                    for (id in appWidgetIds) {
                        updateAppWidget(context, appWidgetManager, id, quote, stamp)
                    }
                }
            } catch (_: Exception) {
                // Safe no-op in headless unit tests where AppWidgetService is unavailable
            }
        }

        internal fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int,
            quote: QuoteItem,
            archivalStamp: String
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_daily_quote)
            views.setTextViewText(
                R.id.widget_header_badge,
                "COMMONPLACE DAILY FOLIO • ${quote.category.uppercase(Locale.US)}"
            )
            views.setTextViewText(R.id.widget_quote_text, "“${quote.text}”")
            views.setTextViewText(
                R.id.widget_quote_author,
                "— ${quote.author} (${quote.sourceWork})"
            )
            views.setTextViewText(R.id.widget_archival_timestamp, "Stamp: $archivalStamp")

            // Clicking the widget body or 'Open Folio' launches MainActivity
            val launchIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("extra_daily_quote_id", quote.id)
            }
            val launchPendingIntent = PendingIntent.getActivity(
                context,
                appWidgetId,
                launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_root_container, launchPendingIntent)
            views.setOnClickPendingIntent(R.id.widget_open_app_button, launchPendingIntent)

            // Clicking 'Refresh' cycles/refreshes the quote & archival timestamp on the Home Screen
            val refreshIntent = Intent(context, CommonplaceDailyQuoteWidgetProvider::class.java).apply {
                action = ACTION_REFRESH_DAILY_WIDGET
            }
            val refreshPendingIntent = PendingIntent.getBroadcast(
                context,
                appWidgetId + 10000,
                refreshIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_refresh_button, refreshPendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
