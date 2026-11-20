package com.example.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.DiaryMood
import com.example.data.MoodLog
import com.example.data.dateKeyToPretty
import com.example.data.formatTimeOnly
import com.example.firebase.AiDiaryAction
import com.example.firebase.AiUiState
import com.example.ui.components.AestheticTopBar
import com.example.ui.components.CuteCatMascotWidget
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.GlowingGlassCard
import com.example.ui.components.NeonGradientButton
import com.example.ui.theme.LocalDiaryThemeSpec
import com.example.viewmodel.DiaryStats
import com.example.viewmodel.MoodChartPoint

private enum class MoodRangeTab(val label: String) {
    WEEK("This Week"),
    MONTH("This Month"),
    YEAR("This Year")
}

@Composable
fun MoodTrackerScreen(
    moodLogs: List<MoodLog>,
    stats: DiaryStats,
    animationsEnabled: Boolean,
    aiState: AiUiState,
    onLogMood: (DiaryMood, String) -> Unit,
    onDeleteMoodLog: (String) -> Unit,
    onRequestAiMoodInsight: (String) -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    val theme = LocalDiaryThemeSpec.current
    var selectedRange by remember { mutableStateOf(MoodRangeTab.WEEK) }
    var showLogDialog by remember { mutableStateOf(false) }

    val chartPoints: List<MoodChartPoint> = remember(selectedRange, stats, moodLogs) {
        when (selectedRange) {
            MoodRangeTab.WEEK -> stats.weeklyMoodPoints
            MoodRangeTab.YEAR -> stats.yearlyMoodPoints
            MoodRangeTab.MONTH -> {
                // Group last 4 weeks of current month
                val buckets = listOf("W1", "W2", "W3", "W4")
                val grouped = moodLogs.groupBy { log ->
                    val day = log.dateKey.takeLast(2).toIntOrNull() ?: 1
                    ((day - 1) / 8).coerceIn(0, 3)
                }
                buckets.mapIndexed { idx, label ->
                    val list = grouped[idx].orEmpty()
                    val avg = if (list.isNotEmpty()) list.map { it.score.toFloat() }.average().toFloat() else 0f
                    val dom = list.groupingBy { it.moodEnum }.eachCount().maxByOrNull { it.value }?.key
                    MoodChartPoint(label, avg, dom, list.size)
                }
            }
        }
    }

    val scoreText = if (stats.averageHappinessScore > 0f) {
        String.format("%.1f", stats.averageHappinessScore)
    } else "—"

    val insightSummary = remember(stats) {
        when {
            stats.mostUsedMood == null -> "Log your first mood check-in to unlock personalized emotional insights!"
            stats.averageHappinessScore >= 7.5f ->
                "You're glowing! Your most frequent feeling is ${stats.mostUsedMood.emoji} ${stats.mostUsedMood.label}, and you're most expressive on ${stats.mostActiveDayOfWeek}s."
            stats.averageHappinessScore >= 5.0f ->
                "You've been navigating balanced emotions lately, mostly feeling ${stats.mostUsedMood.emoji} ${stats.mostUsedMood.label}. Gentle reflection helps keep you grounded."
            else ->
                "It looks like you've carried heavier feelings recently (${stats.mostUsedMood.emoji} ${stats.mostUsedMood.label}). Be extra kind to yourself today — your diary is always here for you."
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        AestheticTopBar(
            title = "Mood Tracker 💗",
            onBack = onBack,
            actions = {
                IconButton(
                    onClick = { showLogDialog = true },
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("mood_add_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Log mood", tint = theme.primary)
                }
            }
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Range Tabs (This Week | This Month | This Year)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MoodRangeTab.entries.forEach { tab ->
                        val active = selectedRange == tab
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = if (active) theme.primary else theme.cardSurface,
                            border = BorderStroke(1.dp, if (active) theme.accent else theme.borderGlow.copy(alpha = 0.45f)),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedRange = tab }
                                .testTag("mood_tab_${tab.name.lowercase()}")
                        ) {
                            Text(
                                text = tab.label,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (active) Color.White else theme.textSecondary,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 10.dp)
                            )
                        }
                    }
                }
            }

            // Glowing Pastel Vertical Mood Bar Chart (Matches mockup!)
            item {
                GlowingGlassCard(
                    cornerRadius = 24.dp,
                    contentPadding = PaddingValues(16.dp)
                ) {
                    Text(
                        text = "${selectedRange.label} Emotional Wave",
                        style = MaterialTheme.typography.titleMedium,
                        color = theme.textPrimary
                    )
                    Spacer(Modifier.height(16.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(175.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        chartPoints.forEach { point ->
                            val ratio = (point.averageScore / 10f).coerceIn(0f, 1f)
                            val barColor = point.dominantMood?.color ?: theme.secondary
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Bottom,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                            ) {
                                Text(
                                    text = point.dominantMood?.emoji ?: "·",
                                    fontSize = 18.sp
                                )
                                Spacer(Modifier.height(4.dp))
                                Box(
                                    modifier = Modifier
                                        .width(24.dp)
                                        .height(112.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(theme.backgroundTop.copy(alpha = 0.55f)),
                                    contentAlignment = Alignment.BottomCenter
                                ) {
                                    if (ratio > 0f) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .fillMaxHeight(ratio.coerceAtLeast(0.14f))
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(
                                                    Brush.verticalGradient(
                                                        listOf(barColor, theme.primary.copy(alpha = 0.85f))
                                                    )
                                                )
                                                .border(1.dp, Color.White.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                        )
                                    }
                                }
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = point.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = theme.textSecondary
                                )
                            }
                        }
                    }
                }
            }

            // Average Happiness Score Card (Matches mockup 8.2 / 10)
            item {
                GlowingGlassCard(
                    cornerRadius = 24.dp,
                    contentPadding = PaddingValues(18.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(theme.primary.copy(alpha = 0.25f))
                                    .border(1.5.dp, theme.borderGlow, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(stats.mostUsedMood?.emoji ?: "😊", fontSize = 30.sp)
                            }
                            Spacer(Modifier.width(14.dp))
                            Column {
                                Text("Average Mood", style = MaterialTheme.typography.labelMedium, color = theme.textSecondary)
                                Row(verticalAlignment = Alignment.Bottom) {
                                    Text(
                                        text = scoreText,
                                        style = MaterialTheme.typography.headlineLarge,
                                        color = theme.textPrimary
                                    )
                                    Text(
                                        text = " / 10",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = theme.accent,
                                        modifier = Modifier.padding(bottom = 4.dp)
                                    )
                                }
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = theme.primary.copy(alpha = 0.28f),
                            border = BorderStroke(1.dp, theme.borderGlow)
                        ) {
                            Text(
                                text = if (stats.averageHappinessScore >= 7f) "You're doing\ngreat! 💗" else "Keep shining\ngently ✨",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    NeonGradientButton(
                        text = "Log Today's Mood",
                        leadingEmoji = "😊",
                        trailingEmoji = "✨",
                        testTagName = "mood_log_now_button",
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { showLogDialog = true }
                    )
                }
            }

            // Mood Distribution Breakdown
            if (stats.moodDistribution.isNotEmpty()) {
                item {
                    val total = stats.moodDistribution.values.sum().coerceAtLeast(1)
                    GlowingGlassCard(cornerRadius = 22.dp, contentPadding = PaddingValues(16.dp)) {
                        Text("Mood Distribution", style = MaterialTheme.typography.titleMedium, color = theme.textPrimary)
                        Spacer(Modifier.height(10.dp))
                        stats.moodDistribution.entries.sortedByDescending { it.value }.forEach { (mood, count) ->
                            val pct = count.toFloat() / total
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("${mood.emoji} ${mood.label}", style = MaterialTheme.typography.labelMedium, color = theme.textPrimary, modifier = Modifier.width(110.dp))
                                LinearProgressIndicator(
                                    progress = { pct },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(10.dp)
                                        .clip(RoundedCornerShape(50)),
                                    color = mood.color,
                                    trackColor = theme.backgroundTop.copy(alpha = 0.6f)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text("${(pct * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, color = theme.accent, modifier = Modifier.width(36.dp), textAlign = TextAlign.End)
                            }
                        }
                    }
                }
            }

            // Mood Insights Card + Optional Gemini AI Mood Insight
            item {
                GlowingGlassCard(cornerRadius = 22.dp, contentPadding = PaddingValues(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("💡 Mood Insights", style = MaterialTheme.typography.titleMedium, color = theme.textPrimary)
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = theme.primary.copy(alpha = 0.25f),
                            border = BorderStroke(1.dp, theme.borderGlow),
                            modifier = Modifier
                                .testTag("mood_ai_insight_button")
                                .clickable {
                                    val summary = "Total logs: ${moodLogs.size}, Average score: $scoreText/10, Distribution: " +
                                        stats.moodDistribution.entries.joinToString { "${it.key.label}=${it.value}" }
                                    onRequestAiMoodInsight(summary)
                                }
                        ) {
                            Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.AutoAwesome, "AI Mood Insight", tint = theme.accent, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("AI Insight", style = MaterialTheme.typography.labelSmall, color = Color.White)
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(insightSummary, style = MaterialTheme.typography.bodyMedium, color = theme.textSecondary)

                    when (aiState) {
                        is AiUiState.Loading -> {
                            Spacer(Modifier.height(10.dp))
                            CircularProgressIndicator(color = theme.primary, modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                        }
                        is AiUiState.Streaming -> if (aiState.action == AiDiaryAction.MOOD_INSIGHT) {
                            Spacer(Modifier.height(10.dp))
                            Text(aiState.text, style = MaterialTheme.typography.bodyMedium, color = theme.accent)
                        }
                        is AiUiState.Success -> if (aiState.action == AiDiaryAction.MOOD_INSIGHT) {
                            Spacer(Modifier.height(10.dp))
                            Text(aiState.text, style = MaterialTheme.typography.bodyMedium, color = theme.accent)
                        }
                        is AiUiState.Error -> {
                            Spacer(Modifier.height(8.dp))
                            Text(aiState.message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                        }
                        AiUiState.Idle -> {}
                    }
                }
            }

            // Mood History List
            item {
                Text("Mood History (${moodLogs.size})", style = MaterialTheme.typography.titleMedium, color = theme.textPrimary)
            }

            if (moodLogs.isEmpty()) {
                item {
                    EmptyStateCard(
                        emoji = "🌸😊",
                        title = "No Mood Logs Yet",
                        subtitle = "Check in with your heart as many times a day as you like!",
                        actionLabel = "Log Mood Now",
                        onAction = { showLogDialog = true }
                    )
                }
            } else {
                items(moodLogs.take(20), key = { it.id }) { log ->
                    GlowingGlassCard(cornerRadius = 18.dp, contentPadding = PaddingValues(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(log.moodEnum.emoji, fontSize = 26.sp)
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("${log.moodEnum.label} (${log.score}/10)", style = MaterialTheme.typography.titleSmall, color = theme.textPrimary)
                                if (log.note.isNotBlank()) {
                                    Text(log.note, style = MaterialTheme.typography.bodySmall, color = theme.textSecondary)
                                }
                                Text("${dateKeyToPretty(log.dateKey)} • ${formatTimeOnly(log.loggedAtMillis)}", style = MaterialTheme.typography.labelSmall, color = theme.accent)
                            }
                            IconButton(onClick = { onDeleteMoodLog(log.id) }) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = "Delete mood log", tint = theme.textSecondary)
                            }
                        }
                    }
                }
            }

            item {
                CuteCatMascotWidget(animationsEnabled = animationsEnabled)
            }
        }
    }

    if (showLogDialog) {
        var chosenMood by remember { mutableStateOf(DiaryMood.HAPPY) }
        var note by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showLogDialog = false },
            containerColor = theme.backgroundBottom,
            title = { Text("Check In Your Mood 💗", color = theme.textPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(DiaryMood.entries) { m ->
                            val sel = chosenMood == m
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = if (sel) theme.primary.copy(alpha = 0.35f) else theme.cardSurface,
                                border = BorderStroke(1.5.dp, if (sel) theme.accent else theme.borderGlow.copy(alpha = 0.4f)),
                                modifier = Modifier.clickable { chosenMood = m }
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                                ) {
                                    Text(m.emoji, fontSize = 24.sp)
                                    Text(m.label, style = MaterialTheme.typography.labelSmall, color = theme.textPrimary)
                                }
                            }
                        }
                    }
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it.take(500) },
                        label = { Text("Optional reflection note...") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    onLogMood(chosenMood, note)
                    showLogDialog = false
                }) {
                    Text("Save Mood 💗", color = theme.primary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogDialog = false }) { Text("Cancel", color = theme.textSecondary) }
            }
        )
    }
}
