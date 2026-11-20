package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.AestheticTopBar
import com.example.ui.components.CuteCatMascotWidget
import com.example.ui.components.GlowingGlassCard
import com.example.ui.theme.LocalDiaryThemeSpec
import com.example.viewmodel.DiaryStats

@Composable
fun StatisticsScreen(
    stats: DiaryStats,
    animationsEnabled: Boolean,
    onOpenMoodTracker: () -> Unit
) {
    val theme = LocalDiaryThemeSpec.current
    val maxMonthly = (stats.monthlyActivityPoints.maxOfOrNull { it.second } ?: 1).coerceAtLeast(1)

    Column(modifier = Modifier.fillMaxSize()) {
        AestheticTopBar(title = "Statistics ✨")

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 112.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Core Metrics Grid
            item {
                val cards = listOf(
                    Triple("Total Entries", "${stats.totalEntries}", "📖"),
                    Triple("Words Written", "${stats.totalWords}", "✍️"),
                    Triple("Current Streak", "${stats.currentStreak} days", "🔥"),
                    Triple("Longest Streak", "${stats.longestStreak} days", "👑"),
                    Triple("Total Photos", "${stats.totalPhotos}", "📸"),
                    Triple("Voice Notes", "${stats.totalVoiceNotes}", "🎙️")
                )
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    cards.chunked(2).forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            row.forEach { (label, value, emoji) ->
                                GlowingGlassCard(
                                    modifier = Modifier.weight(1f),
                                    cornerRadius = 20.dp,
                                    contentPadding = PaddingValues(14.dp)
                                ) {
                                    Text(emoji, fontSize = 24.sp)
                                    Spacer(Modifier.height(6.dp))
                                    Text(
                                        text = value,
                                        style = MaterialTheme.typography.headlineMedium,
                                        color = theme.textPrimary
                                    )
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = theme.accent
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Emotional Highlights Card
            item {
                GlowingGlassCard(
                    cornerRadius = 22.dp,
                    contentPadding = PaddingValues(16.dp),
                    onClick = onOpenMoodTracker
                ) {
                    Text("Heart & Writing Highlights 💗", style = MaterialTheme.typography.titleMedium, color = theme.textPrimary)
                    Spacer(Modifier.height(10.dp))
                    StatDetailRow("Most Used Mood", stats.mostUsedMood?.let { "${it.emoji} ${it.label}" } ?: "No mood data yet")
                    StatDetailRow("Average Happiness Score", if (stats.averageHappinessScore > 0f) "${String.format("%.1f", stats.averageHappinessScore)} / 10" else "—")
                    StatDetailRow("Most Active Day", stats.mostActiveDayOfWeek)
                    StatDetailRow("Total Dreams Logged", "${stats.totalDreams} 🌙")
                    StatDetailRow("Favorite Memories", "${stats.totalFavorites} 💗")
                    val avgWords = if (stats.totalEntries > 0) stats.totalWords / stats.totalEntries else 0L
                    StatDetailRow("Avg Words / Entry", "$avgWords words")
                }
            }

            // Monthly Activity Chart (Last 6 Months)
            item {
                GlowingGlassCard(cornerRadius = 24.dp, contentPadding = PaddingValues(16.dp)) {
                    Text("Monthly Writing Activity 📅", style = MaterialTheme.typography.titleMedium, color = theme.textPrimary)
                    Spacer(Modifier.height(14.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(155.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        stats.monthlyActivityPoints.forEach { (monthLabel, count) ->
                            val ratio = count.toFloat() / maxMonthly.toFloat()
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Bottom,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                            ) {
                                Text("$count", style = MaterialTheme.typography.labelSmall, color = theme.accent)
                                Spacer(Modifier.height(4.dp))
                                Box(
                                    modifier = Modifier
                                        .width(26.dp)
                                        .height(104.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(theme.backgroundTop.copy(alpha = 0.55f)),
                                    contentAlignment = Alignment.BottomCenter
                                ) {
                                    if (count > 0) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .fillMaxHeight(ratio.coerceAtLeast(0.14f))
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(
                                                    Brush.verticalGradient(listOf(theme.primary, theme.secondary))
                                                )
                                                .border(1.dp, Color.White.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                        )
                                    }
                                }
                                Spacer(Modifier.height(6.dp))
                                Text(monthLabel, style = MaterialTheme.typography.labelSmall, color = theme.textSecondary)
                            }
                        }
                    }
                }
            }

            // Yearly Mood Harmony Chart
            item {
                GlowingGlassCard(cornerRadius = 24.dp, contentPadding = PaddingValues(16.dp)) {
                    Text("Yearly Mood Harmony 🌈", style = MaterialTheme.typography.titleMedium, color = theme.textPrimary)
                    Spacer(Modifier.height(14.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        stats.yearlyMoodPoints.forEach { pt ->
                            val ratio = (pt.averageScore / 10f).coerceIn(0f, 1f)
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Bottom,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                            ) {
                                Text(pt.dominantMood?.emoji ?: "·", fontSize = 16.sp)
                                Spacer(Modifier.height(4.dp))
                                Box(
                                    modifier = Modifier
                                        .width(24.dp)
                                        .height(96.dp)
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
                                                        listOf(pt.dominantMood?.color ?: theme.accent, theme.secondary)
                                                    )
                                                )
                                        )
                                    }
                                }
                                Spacer(Modifier.height(6.dp))
                                Text(pt.label, style = MaterialTheme.typography.labelSmall, color = theme.textSecondary, textAlign = TextAlign.Center)
                            }
                        }
                    }
                }
            }

            item {
                CuteCatMascotWidget(
                    animationsEnabled = animationsEnabled,
                    customMessage = if (stats.currentStreak > 0) "Amazing ${stats.currentStreak}-day streak! Keep going! 🔥" else "Write today to start your glowing streak! ✨"
                )
            }
        }
    }
}

@Composable
private fun StatDetailRow(label: String, value: String) {
    val theme = LocalDiaryThemeSpec.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = theme.textSecondary)
        Text(value, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = theme.textPrimary)
    }
}
