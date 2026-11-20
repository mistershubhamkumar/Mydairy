package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.AppPreferences
import com.example.data.DefaultDiaryCategories
import com.example.data.DiaryEntry
import com.example.data.DiaryMood
import com.example.data.MoodLog
import com.example.data.UserProfile
import com.example.data.dateKeyToPretty
import com.example.data.formatPrettyDate
import com.example.data.formatTimeOnly
import com.example.data.todayDateKey
import com.example.ui.components.CuteCatMascotWidget
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.GlowingGlassCard
import com.example.ui.theme.LocalDiaryThemeSpec
import com.example.viewmodel.DiaryStats
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun HomeDashboardScreen(
    profile: UserProfile?,
    preferences: AppPreferences,
    entries: List<DiaryEntry>,
    moodLogs: List<MoodLog>,
    stats: DiaryStats,
    dailyQuote: String?,
    onOpenNewEntry: () -> Unit,
    onEditEntry: (DiaryEntry) -> Unit,
    onToggleFavorite: (DiaryEntry) -> Unit,
    onDeleteEntry: (DiaryEntry) -> Unit,
    onQuickLogMood: (DiaryMood) -> Unit,
    onOpenCalendar: () -> Unit,
    onOpenMoodTracker: () -> Unit,
    onOpenPhotos: () -> Unit,
    onOpenDreams: () -> Unit,
    onOpenVoiceDiary: () -> Unit,
    onOpenCategories: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenReminders: () -> Unit,
    onRefreshAiQuote: () -> Unit
) {
    val theme = LocalDiaryThemeSpec.current
    val visibleEntries = remember(entries, preferences.secretModeEnabled) {
        if (preferences.secretModeEnabled) entries.filter { !it.privateLocked } else entries
    }
    val todayKey = remember { todayDateKey() }
    val todayMood = remember(moodLogs, visibleEntries, todayKey) {
        moodLogs.firstOrNull { it.dateKey == todayKey }?.moodEnum
            ?: visibleEntries.firstOrNull { it.dateKey == todayKey }?.moodEnum
            ?: DiaryMood.HAPPY
    }
    var showMoodPicker by remember { mutableStateOf(false) }
    var weekOffset by remember { mutableIntStateOf(0) }
    val entryDateKeys = remember(visibleEntries) { visibleEntries.map { it.dateKey }.toSet() }
    val quoteText = dailyQuote ?: DefaultDiaryCategories.dailyMotivations[
        (Calendar.getInstance().get(Calendar.DAY_OF_YEAR)) % DefaultDiaryCategories.dailyMotivations.size
    ]

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 112.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. TOP HEADER (Avatar, Greeting, Weather Pill, Search, Notifications, Settings)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onOpenCategories,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("home_menu_button")
                ) {
                    Icon(Icons.Default.Menu, contentDescription = "Diary categories", tint = theme.textPrimary)
                }
                Surface(
                    shape = RoundedCornerShape(50),
                    color = theme.cardSurface,
                    border = BorderStroke(1.dp, theme.borderGlow.copy(alpha = 0.65f))
                ) {
                    Text(
                        text = "🌙 Starry • ${formatPrettyDate(System.currentTimeMillis())}",
                        style = MaterialTheme.typography.labelMedium,
                        color = theme.accent,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onOpenSearch,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("home_search_button")
                    ) {
                        Icon(Icons.Default.Search, contentDescription = "Search diary", tint = theme.textPrimary)
                    }
                    IconButton(
                        onClick = onOpenReminders,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("home_notifications_button")
                    ) {
                        Icon(Icons.Default.Notifications, contentDescription = "Reminders", tint = theme.textPrimary)
                    }
                    IconButton(
                        onClick = onOpenSettings,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("home_settings_button")
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings", tint = theme.textPrimary)
                    }
                }
            }

            Spacer(Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(66.dp)
                        .clip(CircleShape)
                        .border(2.5.dp, Brush.linearGradient(listOf(theme.primary, theme.accent)), CircleShape)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_avatar_princess_1795152826048),
                        contentDescription = "Princess profile avatar",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    val name = profile?.displayName?.ifBlank { "Princess" } ?: "Princess"
                    Text(
                        text = "Hello, $name! 👑",
                        style = MaterialTheme.typography.headlineMedium,
                        color = theme.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Your feelings deserve to be remembered. ♡",
                        style = MaterialTheme.typography.bodyMedium,
                        color = theme.textSecondary
                    )
                }
            }
        }

        // 2. TODAY'S MOOD CARD (Matches mockup pink-purple glowing bar)
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                theme.primary.copy(alpha = 0.90f),
                                theme.secondary.copy(alpha = 0.85f)
                            )
                        )
                    )
                    .border(1.5.dp, theme.accent.copy(alpha = 0.85f), RoundedCornerShape(24.dp))
                    .clickable { showMoodPicker = true }
                    .testTag("today_mood_card")
                    .padding(horizontal = 18.dp, vertical = 14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.25f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = todayMood.emoji, fontSize = 28.sp)
                        }
                        Spacer(Modifier.width(14.dp))
                        Column {
                            Text(
                                text = "Today's Mood",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                            Text(
                                text = todayMood.label,
                                style = MaterialTheme.typography.titleLarge.copy(
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = Color.White.copy(alpha = 0.22f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.65f))
                    ) {
                        Text(
                            text = "Change 💗",
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }

        // 3. COMPACT CALENDAR WIDGET (Matches mockup week strip with pink circle on today)
        item {
            CompactCalendarWidget(
                weekOffset = weekOffset,
                onPrevWeek = { weekOffset-- },
                onNextWeek = { weekOffset++ },
                entryDateKeys = entryDateKeys,
                onOpenFullCalendar = onOpenCalendar
            )
        }

        // 4. QUICK ACTION BUTTONS (6 Glowing Gradient Tiles matching mockup)
        item {
            val actions = listOf(
                Triple("New Entry", "✍️", onOpenNewEntry),
                Triple("Mood Tracker", "😊", onOpenMoodTracker),
                Triple("My Photos", "📸", onOpenPhotos),
                Triple("Dream Journal", "🌙", onOpenDreams),
                Triple("Voice Diary", "🎙️", onOpenVoiceDiary),
                Triple("My Diaries", "📚", onOpenCategories)
            )
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                actions.chunked(3).forEach { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        rowItems.forEach { (label, emoji, action) ->
                            GlowingGlassCard(
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("quick_action_${label.lowercase().replace(" ", "_")}"),
                                cornerRadius = 20.dp,
                                contentPadding = PaddingValues(vertical = 14.dp, horizontal = 8.dp),
                                onClick = action
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(CircleShape)
                                            .background(
                                                Brush.linearGradient(
                                                    listOf(
                                                        theme.primary.copy(alpha = 0.35f),
                                                        theme.secondary.copy(alpha = 0.35f)
                                                    )
                                                )
                                            )
                                            .border(1.dp, theme.borderGlow.copy(alpha = 0.7f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(text = emoji, fontSize = 22.sp)
                                    }
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = theme.textPrimary,
                                        textAlign = TextAlign.Center,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 5. STATISTICS SUMMARY STRIP
        item {
            GlowingGlassCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 22.dp,
                contentPadding = PaddingValues(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    MiniStatBadge("Entries", "${stats.totalEntries}", "📖")
                    MiniStatBadge("Streak", "${stats.currentStreak}d", "🔥")
                    MiniStatBadge("Longest", "${stats.longestStreak}d", "👑")
                    MiniStatBadge("Memories", "${stats.totalPhotos}", "📸")
                    MiniStatBadge("Favs", "${stats.totalFavorites}", "💗")
                }
            }
        }

        // 6. RECENT ENTRIES HEADER
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent Entries ✨",
                    style = MaterialTheme.typography.titleLarge,
                    color = theme.textPrimary
                )
                TextButton(
                    onClick = onOpenSearch,
                    modifier = Modifier.testTag("home_see_all_entries")
                ) {
                    Text("See All →", color = theme.accent, fontWeight = FontWeight.Bold)
                }
            }
        }

        if (visibleEntries.isEmpty()) {
            item {
                EmptyStateCard(
                    emoji = "📖💗",
                    title = "Your Diary Pages Await",
                    subtitle = "Tap below to write your very first secret feeling, attach photos, and start your writing streak!",
                    actionLabel = "Write First Entry",
                    onAction = onOpenNewEntry
                )
            }
        } else {
            items(visibleEntries.take(5), key = { it.id }) { entry ->
                DiaryEntryCard(
                    entry = entry,
                    onClick = { onEditEntry(entry) },
                    onToggleFavorite = { onToggleFavorite(entry) },
                    onDelete = { onDeleteEntry(entry) }
                )
            }
        }

        // 7. DAILY MOTIVATION CARD (With Gemini AI Refresh!)
        item {
            GlowingGlassCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 22.dp,
                contentPadding = PaddingValues(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Daily Motivation ✨",
                        style = MaterialTheme.typography.titleSmall,
                        color = theme.accent
                    )
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = theme.primary.copy(alpha = 0.25f),
                        border = BorderStroke(1.dp, theme.borderGlow),
                        modifier = Modifier
                            .testTag("home_ai_quote_button")
                            .clickable(onClick = onRefreshAiQuote)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.AutoAwesome,
                                contentDescription = "Generate AI daily quote",
                                tint = theme.accent,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text("AI Quote", style = MaterialTheme.typography.labelSmall, color = Color.White)
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = quoteText,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                    color = theme.textPrimary
                )
            }
        }

        // 8. CUTE CAT COMPANION WIDGET
        item {
            CuteCatMascotWidget(animationsEnabled = preferences.animationsEnabled)
        }
    }

    if (showMoodPicker) {
        AlertDialog(
            onDismissRequest = { showMoodPicker = false },
            containerColor = theme.backgroundBottom,
            titleContentColor = theme.textPrimary,
            title = { Text("How are you feeling right now? 💗") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    DiaryMood.entries.chunked(2).forEach { pair ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            pair.forEach { mood ->
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = theme.cardSurface,
                                    border = BorderStroke(1.dp, mood.color.copy(alpha = 0.8f)),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            onQuickLogMood(mood)
                                            showMoodPicker = false
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(mood.emoji, fontSize = 20.sp)
                                        Spacer(Modifier.width(6.dp))
                                        Text(mood.label, style = MaterialTheme.typography.labelLarge, color = theme.textPrimary)
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showMoodPicker = false }) {
                    Text("Close", color = theme.accent)
                }
            }
        )
    }
}

@Composable
private fun CompactCalendarWidget(
    weekOffset: Int,
    onPrevWeek: () -> Unit,
    onNextWeek: () -> Unit,
    entryDateKeys: Set<String>,
    onOpenFullCalendar: () -> Unit
) {
    val theme = LocalDiaryThemeSpec.current
    val todayKey = remember { todayDateKey() }
    val baseCal = remember(weekOffset) {
        Calendar.getInstance().apply {
            add(Calendar.WEEK_OF_YEAR, weekOffset)
            set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
        }
    }
    val monthHeader = remember(weekOffset) {
        val mid = (baseCal.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 3) }
        SimpleDateFormat("MMM yyyy", Locale.getDefault()).format(Date(mid.timeInMillis))
    }
    val weekDays = remember(weekOffset) {
        (0..6).map { idx ->
            val c = (baseCal.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, idx) }
            Triple(
                SimpleDateFormat("EEE", Locale.getDefault()).format(Date(c.timeInMillis)),
                c.get(Calendar.DAY_OF_MONTH),
                todayDateKey(c.timeInMillis)
            )
        }
    }

    GlowingGlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("home_calendar_widget"),
        cornerRadius = 24.dp,
        contentPadding = PaddingValues(14.dp),
        onClick = onOpenFullCalendar
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onPrevWeek, modifier = Modifier.size(36.dp)) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous week", tint = theme.accent)
            }
            Text(
                text = monthHeader,
                style = MaterialTheme.typography.titleMedium,
                color = theme.textPrimary
            )
            IconButton(onClick = onNextWeek, modifier = Modifier.size(36.dp)) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next week", tint = theme.accent)
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            weekDays.forEach { (dow, dayNum, key) ->
                val isToday = key == todayKey
                val hasEntry = key in entryDateKeys
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = dow,
                        style = MaterialTheme.typography.labelSmall,
                        color = theme.textSecondary
                    )
                    Spacer(Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(
                                if (isToday) Brush.linearGradient(listOf(theme.primary, theme.secondary))
                                else Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
                            )
                            .border(
                                width = if (isToday) 1.5.dp else 0.dp,
                                color = if (isToday) theme.accent else Color.Transparent,
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "$dayNum",
                            style = MaterialTheme.typography.labelLarge,
                            color = if (isToday) Color.White else theme.textPrimary
                        )
                    }
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = if (hasEntry) "💗" else " ",
                        fontSize = 9.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun MiniStatBadge(label: String, value: String, emoji: String) {
    val theme = LocalDiaryThemeSpec.current
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(emoji, fontSize = 18.sp)
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = theme.textPrimary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = theme.textSecondary
        )
    }
}

@Composable
fun DiaryEntryCard(
    entry: DiaryEntry,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onDelete: () -> Unit
) {
    val theme = LocalDiaryThemeSpec.current
    GlowingGlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("diary_entry_card_${entry.id}"),
        cornerRadius = 22.dp,
        contentPadding = PaddingValues(14.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            if (entry.photoUris.isNotEmpty()) {
                AsyncImage(
                    model = entry.photoUris.first(),
                    contentDescription = "Entry memory photo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(66.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.dp, theme.borderGlow, RoundedCornerShape(16.dp))
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(58.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(entry.moodEnum.color.copy(alpha = 0.24f))
                        .border(1.dp, entry.moodEnum.color.copy(alpha = 0.75f), RoundedCornerShape(18.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(entry.moodEnum.emoji, fontSize = 28.sp)
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = entry.title.ifBlank { "Untitled Feelings ♡" },
                        style = MaterialTheme.typography.titleMedium,
                        color = theme.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    if (entry.privateLocked) {
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = "Private locked entry",
                            tint = theme.accent,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                    }
                }
                Spacer(Modifier.height(3.dp))
                Text(
                    text = if (entry.privateLocked) "🔒 Locked private entry — tap to unlock" else entry.content.ifBlank { "No text yet..." },
                    style = MaterialTheme.typography.bodyMedium,
                    color = theme.textSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${dateKeyToPretty(entry.dateKey)} • ${formatTimeOnly(entry.entryTimeMillis)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = theme.accent
                    )
                    Row {
                        IconButton(onClick = onToggleFavorite, modifier = Modifier.size(36.dp)) {
                            Icon(
                                imageVector = if (entry.favorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = "Toggle favorite",
                                tint = if (entry.favorite) theme.primary else theme.textSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        IconButton(onClick = onClick, modifier = Modifier.size(36.dp)) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit entry",
                                tint = theme.textSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Move to recycle bin",
                                tint = theme.textSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
                if (entry.tags.isNotEmpty()) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(entry.tags.take(4)) { tag ->
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = theme.primary.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = "#$tag",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = theme.accent,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
