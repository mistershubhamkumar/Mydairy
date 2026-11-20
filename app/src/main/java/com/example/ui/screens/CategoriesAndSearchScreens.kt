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
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.DefaultDiaryCategories
import com.example.data.DiaryCategory
import com.example.data.DiaryEntry
import com.example.data.DiaryMood
import com.example.data.EntrySortOrder
import com.example.ui.components.AestheticTopBar
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.GlowingGlassCard
import com.example.ui.components.NeonGradientButton
import com.example.ui.theme.LocalDiaryThemeSpec

@Composable
fun CategoriesScreen(
    entries: List<DiaryEntry>,
    customCategories: List<DiaryCategory>,
    secretModeEnabled: Boolean,
    onCreateCategory: (name: String, emoji: String, colorHex: String, coverStyle: String) -> Unit,
    onDeleteCategory: (String) -> Unit,
    onOpenEntry: (DiaryEntry) -> Unit,
    onToggleFavorite: (DiaryEntry) -> Unit,
    onDeleteEntry: (DiaryEntry) -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    val theme = LocalDiaryThemeSpec.current
    val visibleEntries = remember(entries, secretModeEnabled) {
        if (secretModeEnabled) entries.filter { !it.privateLocked } else entries
    }
    var selectedCategoryId by remember { mutableStateOf<String?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }

    val allDisplayCategories = remember(customCategories, visibleEntries) {
        val builtIn = DefaultDiaryCategories.builtIn.map { c ->
            CategoryDisplayItem(
                id = c.id,
                name = c.name,
                emoji = c.emoji,
                colorHex = c.colorHex,
                entryCount = visibleEntries.count { it.categoryId == c.id },
                isCustom = false
            )
        }
        val custom = customCategories.map { c ->
            CategoryDisplayItem(
                id = c.id,
                name = c.name,
                emoji = c.iconKey,
                colorHex = c.colorHex,
                entryCount = visibleEntries.count { it.categoryId == c.id },
                isCustom = true
            )
        }
        builtIn + custom
    }

    val selectedCatEntries = remember(selectedCategoryId, visibleEntries) {
        if (selectedCategoryId == null) emptyList()
        else visibleEntries.filter { it.categoryId == selectedCategoryId }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        AestheticTopBar(
            title = "My Diaries 📚",
            onBack = {
                if (selectedCategoryId != null) selectedCategoryId = null
                else onBack()
            },
            actions = {
                IconButton(
                    onClick = { showCreateDialog = true },
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("category_create_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Create category", tint = theme.accent)
                }
            }
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (selectedCategoryId == null) {
                item {
                    NeonGradientButton(
                        text = "+ Create Custom Diary Category",
                        trailingEmoji = "✨",
                        testTagName = "category_create_cta",
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { showCreateDialog = true }
                    )
                }

                items(allDisplayCategories.chunked(2)) { pair ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        pair.forEach { cat ->
                            val tintColor = remember(cat.colorHex) {
                                try { Color(android.graphics.Color.parseColor(cat.colorHex)) } catch (_: Exception) { theme.primary }
                            }
                            GlowingGlassCard(
                                modifier = Modifier.weight(1f),
                                cornerRadius = 22.dp,
                                contentPadding = PaddingValues(14.dp),
                                onClick = { selectedCategoryId = cat.id }
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(CircleShape)
                                            .background(tintColor.copy(alpha = 0.25f))
                                            .border(1.5.dp, tintColor, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(cat.emoji, fontSize = 22.sp)
                                    }
                                    if (cat.isCustom) {
                                        IconButton(
                                            onClick = { onDeleteCategory(cat.id) },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(Icons.Default.DeleteOutline, contentDescription = "Delete category", tint = theme.textSecondary, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }
                                Spacer(Modifier.height(10.dp))
                                Text(cat.name, style = MaterialTheme.typography.titleSmall, color = theme.textPrimary, maxLines = 1)
                                Spacer(Modifier.height(2.dp))
                                Text("${cat.entryCount} entries", style = MaterialTheme.typography.labelSmall, color = theme.accent)
                            }
                        }
                        if (pair.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            } else {
                val catObj = allDisplayCategories.firstOrNull { it.id == selectedCategoryId }
                item {
                    GlowingGlassCard(cornerRadius = 20.dp, contentPadding = PaddingValues(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(catObj?.emoji ?: "💗", fontSize = 28.sp)
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(catObj?.name ?: "Diary", style = MaterialTheme.typography.titleLarge, color = theme.textPrimary)
                                Text("${selectedCatEntries.size} saved entries", style = MaterialTheme.typography.labelMedium, color = theme.accent)
                            }
                        }
                    }
                }
                if (selectedCatEntries.isEmpty()) {
                    item {
                        EmptyStateCard(
                            emoji = catObj?.emoji ?: "📖",
                            title = "No Entries in This Diary Yet",
                            subtitle = "Choose this category while writing a new diary entry to organize your thoughts here."
                        )
                    }
                } else {
                    items(selectedCatEntries, key = { it.id }) { entry ->
                        DiaryEntryCard(
                            entry = entry,
                            onClick = { onOpenEntry(entry) },
                            onToggleFavorite = { onToggleFavorite(entry) },
                            onDelete = { onDeleteEntry(entry) }
                        )
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        var name by remember { mutableStateOf("") }
        var emoji by remember { mutableStateOf("🌸") }
        var colorHex by remember { mutableStateOf("#FF42C8") }
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            containerColor = theme.backgroundBottom,
            title = { Text("New Custom Diary Category ✨", color = theme.textPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it.take(60) }, label = { Text("Category Name") }, singleLine = true)
                    Text("Choose Icon", style = MaterialTheme.typography.labelLarge, color = theme.accent)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(listOf("🌸", "💗", "🦋", "🌙", "🎀", "🍓", "🎵", "🐾", "💎", "🌈", "🔮", "🧸")) { e ->
                            FilterChip(selected = emoji == e, onClick = { emoji = e }, label = { Text(e, fontSize = 18.sp) })
                        }
                    }
                    Text("Choose Accent Color", style = MaterialTheme.typography.labelLarge, color = theme.accent)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("#FF42C8", "#A855F7", "#38BDF8", "#34D399", "#F59E0B", "#FB7185").forEach { hex ->
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .background(Color(android.graphics.Color.parseColor(hex)))
                                    .border(if (colorHex == hex) 3.dp else 1.dp, Color.White, CircleShape)
                                    .clickable { colorHex = hex }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (name.isNotBlank()) {
                        onCreateCategory(name.trim(), emoji, colorHex, "pink_glow")
                        showCreateDialog = false
                    }
                }) {
                    Text("Create 💗", color = theme.primary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) { Text("Cancel", color = theme.textSecondary) }
            }
        )
    }
}

private data class CategoryDisplayItem(
    val id: String,
    val name: String,
    val emoji: String,
    val colorHex: String,
    val entryCount: Int,
    val isCustom: Boolean
)

@Composable
fun SearchAndFilterScreen(
    entries: List<DiaryEntry>,
    customCategories: List<DiaryCategory>,
    secretModeEnabled: Boolean,
    onOpenEntry: (DiaryEntry) -> Unit,
    onToggleFavorite: (DiaryEntry) -> Unit,
    onDeleteEntry: (DiaryEntry) -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    val theme = LocalDiaryThemeSpec.current
    var query by remember { mutableStateOf("") }
    var selectedMood by remember { mutableStateOf<DiaryMood?>(null) }
    var selectedCategoryId by remember { mutableStateOf<String?>(null) }
    var favoritesOnly by remember { mutableStateOf(false) }
    var sortOrder by remember { mutableStateOf(EntrySortOrder.LATEST_FIRST) }

    val results = remember(entries, query, selectedMood, selectedCategoryId, favoritesOnly, sortOrder, secretModeEnabled) {
        val base = if (secretModeEnabled) entries.filter { !it.privateLocked } else entries
        val q = query.trim().lowercase()
        val filtered = base.filter { e ->
            val matchMood = selectedMood == null || e.moodEnum == selectedMood
            val matchCat = selectedCategoryId == null || e.categoryId == selectedCategoryId
            val matchFav = !favoritesOnly || e.favorite
            val matchQuery = q.isEmpty() ||
                e.title.lowercase().contains(q) ||
                e.content.lowercase().contains(q) ||
                e.dateKey.lowercase().contains(q) ||
                e.locationLabel.lowercase().contains(q) ||
                e.tags.any { it.lowercase().contains(q) }
            matchMood && matchCat && matchFav && matchQuery
        }
        when (sortOrder) {
            EntrySortOrder.LATEST_FIRST -> filtered.sortedByDescending { it.entryTimeMillis }
            EntrySortOrder.OLDEST_FIRST -> filtered.sortedBy { it.entryTimeMillis }
            EntrySortOrder.RECENTLY_UPDATED -> filtered.sortedByDescending { it.updatedAt?.seconds ?: (it.entryTimeMillis / 1000L) }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        AestheticTopBar(title = "Search Diary 🔍", onBack = onBack)

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Search title, text, tags, date (YYYY-MM)...") },
                    leadingIcon = { Icon(Icons.Default.Search, null, tint = theme.accent) },
                    singleLine = true,
                    shape = RoundedCornerShape(20.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = theme.primary,
                        unfocusedBorderColor = theme.borderGlow.copy(alpha = 0.55f),
                        focusedContainerColor = theme.cardSurface,
                        unfocusedContainerColor = theme.cardSurface,
                        focusedTextColor = theme.textPrimary,
                        unfocusedTextColor = theme.textPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("global_search_input")
                )
            }

            // Sort + Favorites Row
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(
                            selected = favoritesOnly,
                            onClick = { favoritesOnly = !favoritesOnly },
                            label = { Text("💗 Favorites") }
                        )
                    }
                    items(EntrySortOrder.entries) { order ->
                        FilterChip(
                            selected = sortOrder == order,
                            onClick = { sortOrder = order },
                            label = { Text(order.label) }
                        )
                    }
                }
            }

            // Mood Filter Row
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    item {
                        FilterChip(
                            selected = selectedMood == null,
                            onClick = { selectedMood = null },
                            label = { Text("All Moods") }
                        )
                    }
                    items(DiaryMood.entries) { m ->
                        FilterChip(
                            selected = selectedMood == m,
                            onClick = { selectedMood = if (selectedMood == m) null else m },
                            label = { Text("${m.emoji} ${m.label}") }
                        )
                    }
                }
            }

            // Category Filter Row
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    item {
                        FilterChip(
                            selected = selectedCategoryId == null,
                            onClick = { selectedCategoryId = null },
                            label = { Text("All Diaries") }
                        )
                    }
                    items(DefaultDiaryCategories.builtIn) { c ->
                        FilterChip(
                            selected = selectedCategoryId == c.id,
                            onClick = { selectedCategoryId = if (selectedCategoryId == c.id) null else c.id },
                            label = { Text("${c.emoji} ${c.name}") }
                        )
                    }
                    items(customCategories) { c ->
                        FilterChip(
                            selected = selectedCategoryId == c.id,
                            onClick = { selectedCategoryId = if (selectedCategoryId == c.id) null else c.id },
                            label = { Text("${c.iconKey} ${c.name}") }
                        )
                    }
                }
            }

            item {
                Text(
                    text = "${results.size} matching ${if (results.size == 1) "entry" else "entries"}",
                    style = MaterialTheme.typography.labelLarge,
                    color = theme.accent
                )
            }

            if (results.isEmpty()) {
                item {
                    EmptyStateCard(
                        emoji = "🔍💗",
                        title = "No Matching Entries",
                        subtitle = "Try another keyword, date (like 2026-11), mood, or clear your active filters."
                    )
                }
            } else {
                items(results, key = { it.id }) { entry ->
                    DiaryEntryCard(
                        entry = entry,
                        onClick = { onOpenEntry(entry) },
                        onToggleFavorite = { onToggleFavorite(entry) },
                        onDelete = { onDeleteEntry(entry) }
                    )
                }
            }
        }
    }
}
