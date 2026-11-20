package com.example.ui.screens

import android.Manifest
import android.app.TimePickerDialog
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.AppPreferences
import com.example.notifications.scheduleDailyReminder
import com.example.notifications.showReminderNotification
import com.example.ui.components.AestheticTopBar
import com.example.ui.components.GlowingGlassCard
import com.example.ui.components.NeonGradientButton
import com.example.ui.theme.DiaryThemes
import com.example.ui.theme.LocalDiaryThemeSpec
import com.example.ui.theme.fontFamilyById
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ThemeStoreScreen(
    preferences: AppPreferences,
    onSelectTheme: (String) -> Unit,
    onSelectFont: (String) -> Unit,
    onSelectMascot: (String) -> Unit,
    onToggleAnimations: (Boolean) -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    val theme = LocalDiaryThemeSpec.current
    val fontOptions = remember {
        listOf(
            "quicksand" to "Quicksand Cute",
            "nunito" to "Nunito Soft",
            "playfair" to "Playfair Editorial",
            "pacifico" to "Pacifico Script"
        )
    }
    val mascotOptions = remember {
        listOf(
            "white_cat" to "🐱 Mochi Kitty",
            "princess_aria" to "👑 Princess Aria",
            "starry_moon" to "🌙 Starry Moon"
        )
    }
    val themeRows = remember { DiaryThemes.all.chunked(2) }

    Column(modifier = Modifier.fillMaxSize()) {
        AestheticTopBar(title = "Theme Store 🎨", onBack = onBack)

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 48.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                GlowingGlassCard {
                    Text(
                        text = "Font Style ✨",
                        style = MaterialTheme.typography.titleMedium,
                        color = theme.textPrimary
                    )
                    Spacer(Modifier.height(10.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        fontOptions.forEach { (id, label) ->
                            val active = preferences.fontId == id
                            FilterChip(
                                selected = active,
                                onClick = { onSelectFont(id) },
                                label = {
                                    Text(
                                        text = label,
                                        fontFamily = fontFamilyById(id),
                                        fontWeight = if (active) FontWeight.Bold else FontWeight.Medium
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = theme.primary,
                                    selectedLabelColor = Color.White,
                                    labelColor = theme.textSecondary
                                )
                            )
                        }
                    }

                    Spacer(Modifier.height(14.dp))
                    Text(
                        text = "Companion Mascot 🐱",
                        style = MaterialTheme.typography.titleMedium,
                        color = theme.textPrimary
                    )
                    Spacer(Modifier.height(8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        mascotOptions.forEach { (id, label) ->
                            val active = preferences.mascotId == id
                            FilterChip(
                                selected = active,
                                onClick = { onSelectMascot(id) },
                                label = { Text(label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = theme.secondary,
                                    selectedLabelColor = Color.White,
                                    labelColor = theme.textSecondary
                                )
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Sparkles & Floating Hearts", style = MaterialTheme.typography.titleSmall, color = theme.textPrimary)
                            Text("Toggle background particles & mascot animations", style = MaterialTheme.typography.bodySmall, color = theme.textSecondary)
                        }
                        Switch(
                            checked = preferences.animationsEnabled,
                            onCheckedChange = onToggleAnimations,
                            colors = SwitchDefaults.colors(checkedTrackColor = theme.primary)
                        )
                    }
                }
            }

            item {
                Text(
                    text = "20 Aesthetic Diary Themes 💗",
                    style = MaterialTheme.typography.titleMedium,
                    color = theme.textPrimary
                )
            }

            items(themeRows) { pair ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    pair.forEach { spec ->
                        val isSelected = preferences.themeId == spec.id
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(22.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(spec.backgroundTop, spec.backgroundBottom, spec.cardSurfaceAlt)
                                    )
                                )
                                .border(
                                    width = if (isSelected) 2.5.dp else 1.2.dp,
                                    color = if (isSelected) spec.primary else spec.borderGlow.copy(alpha = 0.55f),
                                    shape = RoundedCornerShape(22.dp)
                                )
                                .testTag("theme_card_${spec.id}")
                                .clickable { onSelectTheme(spec.id) }
                                .padding(14.dp)
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(spec.emoji, fontSize = 24.sp)
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = "Selected theme",
                                            tint = spec.primary,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }
                                Spacer(Modifier.height(10.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    listOf(spec.primary, spec.secondary, spec.accent).forEach { swatch ->
                                        Box(
                                            modifier = Modifier
                                                .size(18.dp)
                                                .clip(CircleShape)
                                                .background(swatch)
                                                .border(1.dp, Color.White.copy(alpha = 0.6f), CircleShape)
                                        )
                                    }
                                }
                                Spacer(Modifier.height(10.dp))
                                Text(
                                    text = spec.name,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = spec.textPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = if (spec.isDark) "Dark Aesthetic" else "Light Aesthetic",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = spec.textSecondary
                                )
                            }
                        }
                    }
                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RemindersScreen(
    preferences: AppPreferences,
    onUpdateReminders: (
        notificationsEnabled: Boolean?,
        dailyEnabled: Boolean?,
        hour: Int?,
        minute: Int?,
        daysCsv: String?,
        moodReminder: Boolean?,
        gratitudeReminder: Boolean?,
        streakReminder: Boolean?
    ) -> Unit,
    onToggleSoundVibration: (Boolean) -> Unit,
    onShowMessage: (String) -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    val theme = LocalDiaryThemeSpec.current
    val context = LocalContext.current
    val allDays = remember { listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun") }
    val selectedDays = remember(preferences.reminderDaysCsv) {
        preferences.reminderDaysCsv.split(",").filter { it.isNotBlank() }.toSet()
    }

    var pendingActionAfterPermission by remember { mutableStateOf<(() -> Unit)?>(null) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            pendingActionAfterPermission?.invoke()
            pendingActionAfterPermission = null
        } else {
            onShowMessage("Notification permission denied. You can enable it in Android Settings.")
            pendingActionAfterPermission = null
        }
    }

    fun ensureNotificationPermission(onGranted: () -> Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (granted) {
                onGranted()
            } else {
                pendingActionAfterPermission = onGranted
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            onGranted()
        }
    }

    fun formatTime12h(hour: Int, minute: Int): String {
        val amPm = if (hour >= 12) "PM" else "AM"
        val h12 = when {
            hour == 0 -> 12
            hour > 12 -> hour - 12
            else -> hour
        }
        return String.format(Locale.getDefault(), "%d:%02d %s", h12, minute, amPm)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        AestheticTopBar(title = "Reminders 🔔", onBack = onBack)

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 48.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                GlowingGlassCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.NotificationsActive, contentDescription = "Notifications", tint = theme.primary)
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text("Enable Notifications", style = MaterialTheme.typography.titleSmall, color = theme.textPrimary)
                                Text("Master switch for all diary reminders", style = MaterialTheme.typography.bodySmall, color = theme.textSecondary)
                            }
                        }
                        Switch(
                            checked = preferences.notificationsEnabled,
                            onCheckedChange = { enabled ->
                                if (enabled) {
                                    ensureNotificationPermission {
                                        onUpdateReminders(true, null, null, null, null, null, null, null)
                                        if (preferences.dailyReminderEnabled) {
                                            scheduleDailyReminder(context, true, preferences.reminderHour, preferences.reminderMinute)
                                        }
                                    }
                                } else {
                                    onUpdateReminders(false, false, null, null, null, null, null, null)
                                    scheduleDailyReminder(context, false, preferences.reminderHour, preferences.reminderMinute)
                                }
                            },
                            modifier = Modifier.testTag("reminders_master_switch"),
                            colors = SwitchDefaults.colors(checkedTrackColor = theme.primary)
                        )
                    }
                }
            }

            item {
                GlowingGlassCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Daily Diary Reminder 💗", style = MaterialTheme.typography.titleSmall, color = theme.textPrimary)
                            Text(
                                "Gentle nudge to write your feelings at ${formatTime12h(preferences.reminderHour, preferences.reminderMinute)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = theme.textSecondary
                            )
                        }
                        Switch(
                            checked = preferences.dailyReminderEnabled,
                            onCheckedChange = { enabled ->
                                if (enabled) {
                                    ensureNotificationPermission {
                                        onUpdateReminders(true, true, null, null, null, null, null, null)
                                        scheduleDailyReminder(context, true, preferences.reminderHour, preferences.reminderMinute)
                                        onShowMessage("Daily reminder set for ${formatTime12h(preferences.reminderHour, preferences.reminderMinute)} 🔔")
                                    }
                                } else {
                                    onUpdateReminders(null, false, null, null, null, null, null, null)
                                    scheduleDailyReminder(context, false, preferences.reminderHour, preferences.reminderMinute)
                                }
                            },
                            modifier = Modifier.testTag("reminders_daily_switch"),
                            colors = SwitchDefaults.colors(checkedTrackColor = theme.primary)
                        )
                    }

                    Spacer(Modifier.height(12.dp))
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = theme.cardSurfaceAlt,
                        border = BorderStroke(1.dp, theme.borderGlow.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("reminder_time_picker_button")
                            .clickable {
                                TimePickerDialog(
                                    context,
                                    { _, h, m ->
                                        onUpdateReminders(null, null, h, m, null, null, null, null)
                                        if (preferences.dailyReminderEnabled && preferences.notificationsEnabled) {
                                            scheduleDailyReminder(context, true, h, m)
                                        }
                                        onShowMessage("Reminder time updated to ${formatTime12h(h, m)}")
                                    },
                                    preferences.reminderHour,
                                    preferences.reminderMinute,
                                    false
                                ).show()
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.AccessTime, contentDescription = "Pick time", tint = theme.accent)
                                Spacer(Modifier.width(10.dp))
                                Text("Reminder Time", color = theme.textPrimary, style = MaterialTheme.typography.bodyMedium)
                            }
                            Text(
                                text = formatTime12h(preferences.reminderHour, preferences.reminderMinute),
                                color = theme.primary,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                    Text("Repeat Days", style = MaterialTheme.typography.labelMedium, color = theme.accent)
                    Spacer(Modifier.height(8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        allDays.forEach { day ->
                            val active = day in selectedDays
                            FilterChip(
                                selected = active,
                                onClick = {
                                    val updated = if (active) selectedDays - day else selectedDays + day
                                    onUpdateReminders(null, null, null, null, updated.joinToString(","), null, null, null)
                                },
                                label = { Text(day) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = theme.primary,
                                    selectedLabelColor = Color.White,
                                    labelColor = theme.textSecondary
                                )
                            )
                        }
                    }
                }
            }

            item {
                GlowingGlassCard {
                    ReminderMiniToggle(
                        title = "Mood Check-In Reminder 😊",
                        subtitle = "Log how your heart feels during the day",
                        checked = preferences.moodReminderEnabled,
                        onCheckedChange = { onUpdateReminders(null, null, null, null, null, it, null, null) }
                    )
                    Spacer(Modifier.height(10.dp))
                    ReminderMiniToggle(
                        title = "Evening Gratitude Reminder 💖",
                        subtitle = "Reflect on 3 lovely moments before sleep",
                        checked = preferences.gratitudeReminderEnabled,
                        onCheckedChange = { onUpdateReminders(null, null, null, null, null, null, it, null) }
                    )
                    Spacer(Modifier.height(10.dp))
                    ReminderMiniToggle(
                        title = "Writing Streak Reminder 🔥",
                        subtitle = "Keep your daily journaling streak glowing",
                        checked = preferences.streakReminderEnabled,
                        onCheckedChange = { onUpdateReminders(null, null, null, null, null, null, null, it) }
                    )
                }
            }

            item {
                GlowingGlassCard(cornerRadius = 20.dp, contentPadding = PaddingValues(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.VolumeUp, contentDescription = "Sound", tint = theme.accent)
                            Spacer(Modifier.width(10.dp))
                            Text("Sound & Vibration", style = MaterialTheme.typography.titleSmall, color = theme.textPrimary)
                        }
                        Switch(
                            checked = preferences.soundVibrationEnabled,
                            onCheckedChange = onToggleSoundVibration,
                            colors = SwitchDefaults.colors(checkedTrackColor = theme.primary)
                        )
                    }
                }
            }

            item {
                NeonGradientButton(
                    text = "Send Test Notification",
                    trailingEmoji = "✨",
                    testTagName = "send_test_notification_button",
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        ensureNotificationPermission {
                            showReminderNotification(
                                context = context,
                                title = "My Diary – Secret Feelings 💗",
                                message = "Mochi Kitty misses you! Come write down a sweet memory."
                            )
                            onShowMessage("Test notification sent! 🔔")
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun ReminderMiniToggle(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val theme = LocalDiaryThemeSpec.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = theme.textPrimary)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = theme.textSecondary)
        }
        Spacer(Modifier.width(8.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedTrackColor = theme.primary)
        )
    }
}
