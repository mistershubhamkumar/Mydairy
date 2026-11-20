package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.DiaryEntry
import com.example.data.dateKeyToPretty
import com.example.data.todayDateKey
import com.example.ui.components.AestheticTopBar
import com.example.ui.components.CuteCatMascotWidget
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.GlowingGlassCard
import com.example.ui.components.NeonGradientButton
import com.example.ui.theme.LocalDiaryThemeSpec
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private enum class CalendarViewMode(val label: String) {
    MONTHLY("Monthly"),
    WEEKLY("Weekly"),
    DAILY("Daily")
}

@Composable
fun CalendarScreen(
    entries: List<DiaryEntry>,
    secretModeEnabled: Boolean,
    animationsEnabled: Boolean,
    onAddEntryForDate: (String) -> Unit,
    onEditEntry: (DiaryEntry) -> Unit,
    onToggleFavorite: (DiaryEntry) -> Unit,
    onDeleteEntry: (DiaryEntry) -> Unit,
    onOpenSearch: () -> Unit
) {
    val theme = LocalDiaryThemeSpec.current
    val visibleEntries = remember(entries, secretModeEnabled) {
        if (secretModeEnabled) entries.filter { !it.privateLocked } else entries
    }
    val entriesByDate = remember(visibleEntries) { visibleEntries.groupBy { it.dateKey } }

    val todayKey = remember { todayDateKey() }
    var selectedDateKey by remember { mutableStateOf(todayKey) }
    var monthOffset by remember { mutableIntStateOf(0) }
    var viewMode by remember { mutableStateOf(CalendarViewMode.MONTHLY) }

    val displayCal = remember(monthOffset) {
        Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            add(Calendar.MONTH, monthOffset)
        }
    }
    val monthTitle = remember(monthOffset) {
        SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date(displayCal.timeInMillis))
    }

    val daysGrid = remember(monthOffset) {
        val c = displayCal.clone() as Calendar
        val firstDow = c.get(Calendar.DAY_OF_WEEK) - 1 // 0=Sun..6=Sat
        val maxDay = c.getActualMaximum(Calendar.DAY_OF_MONTH)
        val cells = mutableListOf<Pair<Int?, String?>>()
        repeat(firstDow) { cells.add(null to null) }
        for (d in 1..maxDay) {
            c.set(Calendar.DAY_OF_MONTH, d)
            cells.add(d to todayDateKey(c.timeInMillis))
        }
        while (cells.size % 7 != 0) cells.add(null to null)
        cells
    }

    val selectedEntries = entriesByDate[selectedDateKey].orEmpty()

    Column(modifier = Modifier.fillMaxSize()) {
        AestheticTopBar(
            title = "Calendar ♡",
            actions = {
                IconButton(
                    onClick = {
                        monthOffset = 0
                        selectedDateKey = todayKey
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("calendar_jump_today")
                ) {
                    Icon(Icons.Default.Today, contentDescription = "Jump to today", tint = theme.accent)
                }
                IconButton(
                    onClick = onOpenSearch,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("calendar_search_button")
                ) {
                    Icon(Icons.Default.Search, contentDescription = "Search calendar", tint = theme.textPrimary)
                }
            }
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 112.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // View mode tabs (Monthly / Weekly / Daily)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CalendarViewMode.entries.forEach { mode ->
                        val active = viewMode == mode
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = if (active) theme.primary else theme.cardSurface,
                            border = BorderStroke(1.dp, if (active) theme.accent else theme.borderGlow.copy(alpha = 0.45f)),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { viewMode = mode }
                                .testTag("calendar_mode_${mode.name.lowercase()}")
                        ) {
                            Text(
                                text = mode.label,
                                style = MaterialTheme.typography.labelLarge,
                                color = if (active) Color.White else theme.textSecondary,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 10.dp)
                            )
                        }
                    }
                }
            }

            // Calendar Grid Card
            if (viewMode != CalendarViewMode.DAILY) {
                item {
                    GlowingGlassCard(
                        cornerRadius = 24.dp,
                        contentPadding = PaddingValues(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { monthOffset-- }, modifier = Modifier.size(40.dp)) {
                                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Previous month", tint = theme.accent)
                            }
                            Text(
                                text = monthTitle,
                                style = MaterialTheme.typography.titleLarge,
                                color = theme.textPrimary
                            )
                            IconButton(onClick = { monthOffset++ }, modifier = Modifier.size(40.dp)) {
                                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Next month", tint = theme.accent)
                            }
                        }

                        Spacer(Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth()) {
                            listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat").forEach { dow ->
                                Text(
                                    text = dow,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = theme.textSecondary,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                        Spacer(Modifier.height(6.dp))

                        val rowsToRender = if (viewMode == CalendarViewMode.MONTHLY) {
                            daysGrid.chunked(7)
                        } else {
                            val allWeeks = daysGrid.chunked(7)
                            val matchingWeek = allWeeks.firstOrNull { week -> week.any { it.second == selectedDateKey } }
                                ?: allWeeks.firstOrNull()
                            listOfNotNull(matchingWeek)
                        }

                        rowsToRender.forEach { week ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp)
                            ) {
                                week.forEach { (dayNum, dateKey) ->
                                    if (dayNum == null || dateKey == null) {
                                        Spacer(Modifier.weight(1f))
                                    } else {
                                        val isSelected = dateKey == selectedDateKey
                                        val isToday = dateKey == todayKey
                                        val dayEntries = entriesByDate[dateKey].orEmpty()
                                        val moodColor = dayEntries.firstOrNull()?.moodEnum?.color

                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(14.dp))
                                                .clickable { selectedDateKey = dateKey }
                                                .padding(vertical = 2.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(34.dp)
                                                    .clip(CircleShape)
                                                    .background(
                                                        when {
                                                            isSelected -> Brush.linearGradient(listOf(theme.primary, theme.secondary))
                                                            isToday -> Brush.linearGradient(listOf(theme.primary.copy(alpha = 0.35f), theme.secondary.copy(alpha = 0.35f)))
                                                            else -> Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
                                                        }
                                                    )
                                                    .border(
                                                        width = if (isSelected || isToday) 1.5.dp else 0.dp,
                                                        color = if (isSelected) theme.accent else if (isToday) theme.primary else Color.Transparent,
                                                        shape = CircleShape
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = "$dayNum",
                                                    style = MaterialTheme.typography.labelLarge,
                                                    color = if (isSelected) Color.White else theme.textPrimary
                                                )
                                            }
                                            Spacer(Modifier.height(2.dp))
                                            if (moodColor != null) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(7.dp)
                                                        .clip(CircleShape)
                                                        .background(moodColor)
                                                )
                                            } else {
                                                Spacer(Modifier.height(7.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Selected Date Header + Add Button
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Entries on ${dateKeyToPretty(selectedDateKey)}",
                            style = MaterialTheme.typography.titleMedium,
                            color = theme.textPrimary
                        )
                        Text(
                            text = "${selectedEntries.size} ${if (selectedEntries.size == 1) "Entry" else "Entries"}",
                            style = MaterialTheme.typography.labelMedium,
                            color = theme.accent
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = theme.primary,
                        modifier = Modifier
                            .testTag("calendar_add_entry_date")
                            .clickable { onAddEntryForDate(selectedDateKey) }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add entry for date", tint = Color.White, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Add", style = MaterialTheme.typography.labelLarge, color = Color.White)
                        }
                    }
                }
            }

            if (selectedEntries.isEmpty()) {
                item {
                    EmptyStateCard(
                        emoji = "🗓️💗",
                        title = "No Entries on This Day",
                        subtitle = "Capture a memory or mood for ${dateKeyToPretty(selectedDateKey)}.",
                        actionLabel = "Write for This Date",
                        onAction = { onAddEntryForDate(selectedDateKey) }
                    )
                }
            } else {
                items(selectedEntries, key = { it.id }) { entry ->
                    DiaryEntryCard(
                        entry = entry,
                        onClick = { onEditEntry(entry) },
                        onToggleFavorite = { onToggleFavorite(entry) },
                        onDelete = { onDeleteEntry(entry) }
                    )
                }
            }

            item {
                CuteCatMascotWidget(
                    animationsEnabled = animationsEnabled,
                    customMessage = "Every day is special... ♡"
                )
            }
        }
    }
}
