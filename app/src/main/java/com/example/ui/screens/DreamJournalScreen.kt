package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
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
import androidx.compose.runtime.mutableStateListOf
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.DiaryMood
import com.example.data.DreamCategory
import com.example.data.DreamEntry
import com.example.data.MediaAndBackupManager
import com.example.data.dateKeyToPretty
import com.example.data.todayDateKey
import com.example.ui.components.AestheticTopBar
import com.example.ui.components.CuteCatMascotWidget
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.GlowingGlassCard
import com.example.ui.components.NeonGradientButton
import com.example.ui.theme.LocalDiaryThemeSpec
import com.example.ui.theme.PacificoFontFamily

@Composable
fun DreamJournalScreen(
    dreams: List<DreamEntry>,
    animationsEnabled: Boolean,
    mediaManager: MediaAndBackupManager,
    onSaveDream: (DreamEntry) -> Unit,
    onToggleFavorite: (DreamEntry) -> Unit,
    onDeleteDream: (DreamEntry) -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    val theme = LocalDiaryThemeSpec.current
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<DreamCategory?>(null) }
    var favoritesOnly by remember { mutableStateOf(false) }
    var editingDream by remember { mutableStateOf<DreamEntry?>(null) }
    var showEditorDialog by remember { mutableStateOf(false) }

    val filteredDreams = remember(dreams, searchQuery, selectedCategory, favoritesOnly) {
        dreams.filter { d ->
            val matchCat = selectedCategory == null || d.categoryEnum == selectedCategory
            val matchFav = !favoritesOnly || d.favorite
            val q = searchQuery.trim().lowercase()
            val matchQuery = q.isEmpty() ||
                d.title.lowercase().contains(q) ||
                d.description.lowercase().contains(q) ||
                d.tags.any { it.lowercase().contains(q) }
            matchCat && matchFav && matchQuery
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        AestheticTopBar(
            title = "Dream Journal ♡",
            onBack = onBack,
            actions = {
                IconButton(
                    onClick = { editingDream = null; showEditorDialog = true },
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("dream_add_top_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add new dream", tint = theme.accent)
                }
            }
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Magical Fairytale Castle Hero Banner (Matches mockup!)
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(175.dp)
                        .clip(RoundedCornerShape(26.dp))
                        .border(1.5.dp, theme.borderGlow, RoundedCornerShape(26.dp))
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_dream_castle_1795152811121),
                        contentDescription = "Magical enchanted castle in starry night sky",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color.Transparent, Color(0xCC12062B))
                                )
                            )
                    )
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(18.dp)
                    ) {
                        Text(
                            text = "Your Dreams...",
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontFamily = PacificoFontFamily,
                                color = Color.White
                            )
                        )
                        Text(
                            text = "Never stop dreaming ✨",
                            style = MaterialTheme.typography.bodyMedium,
                            color = theme.accent
                        )
                    }
                }
            }

            // Search Bar
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search your midnight dreams...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = theme.accent) },
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
                        .testTag("dream_search_input")
                )
            }

            // Dream Category Filter Chips
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(
                            selected = selectedCategory == null && !favoritesOnly,
                            onClick = { selectedCategory = null; favoritesOnly = false },
                            label = { Text("✨ All") }
                        )
                    }
                    item {
                        FilterChip(
                            selected = favoritesOnly,
                            onClick = { favoritesOnly = !favoritesOnly },
                            label = { Text("💗 Favorites") }
                        )
                    }
                    items(DreamCategory.entries) { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = if (selectedCategory == cat) null else cat },
                            label = { Text("${cat.emoji} ${cat.label}") }
                        )
                    }
                }
            }

            // Add New Dream Button
            item {
                NeonGradientButton(
                    text = "+ Add New Dream",
                    trailingEmoji = "🌙",
                    testTagName = "dream_add_button",
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { editingDream = null; showEditorDialog = true }
                )
            }

            if (filteredDreams.isEmpty()) {
                item {
                    EmptyStateCard(
                        emoji = "🌙🏰✨",
                        title = "No Dreams Recorded Yet",
                        subtitle = "Capture your lucid, fantasy, romantic, or mysterious dreams before they fade at sunrise."
                    )
                }
            } else {
                items(filteredDreams, key = { it.id }) { dream ->
                    GlowingGlassCard(
                        cornerRadius = 22.dp,
                        contentPadding = PaddingValues(14.dp),
                        onClick = { editingDream = dream; showEditorDialog = true }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.Top
                        ) {
                            if (dream.imageUris.isNotEmpty()) {
                                AsyncImage(
                                    model = dream.imageUris.first(),
                                    contentDescription = "Dream image",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .border(1.dp, dream.categoryEnum.color, RoundedCornerShape(16.dp))
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(54.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(dream.categoryEnum.color.copy(alpha = 0.22f))
                                        .border(1.dp, dream.categoryEnum.color, RoundedCornerShape(16.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(dream.categoryEnum.emoji, fontSize = 26.sp)
                                }
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = dream.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = theme.textPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${dream.categoryEnum.label} • ${dateKeyToPretty(dream.dateKey)} • ${dream.moodEnum.emoji}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = theme.accent
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = dream.description,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = theme.textSecondary,
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    IconButton(onClick = { onToggleFavorite(dream) }, modifier = Modifier.size(36.dp)) {
                                        Icon(
                                            if (dream.favorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                            contentDescription = "Favorite dream",
                                            tint = if (dream.favorite) theme.primary else theme.textSecondary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    IconButton(onClick = { editingDream = dream; showEditorDialog = true }, modifier = Modifier.size(36.dp)) {
                                        Icon(Icons.Default.Edit, contentDescription = "Edit dream", tint = theme.textSecondary, modifier = Modifier.size(20.dp))
                                    }
                                    IconButton(onClick = { onDeleteDream(dream) }, modifier = Modifier.size(36.dp)) {
                                        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete dream", tint = theme.textSecondary, modifier = Modifier.size(20.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                CuteCatMascotWidget(
                    animationsEnabled = animationsEnabled,
                    customMessage = "Dream big, Live better... ♡"
                )
            }
        }
    }

    if (showEditorDialog) {
        DreamEditorDialog(
            initial = editingDream,
            mediaManager = mediaManager,
            onDismiss = { showEditorDialog = false },
            onSave = { dream ->
                onSaveDream(dream)
                showEditorDialog = false
            }
        )
    }
}

@Composable
private fun DreamEditorDialog(
    initial: DreamEntry?,
    mediaManager: MediaAndBackupManager,
    onDismiss: () -> Unit,
    onSave: (DreamEntry) -> Unit
) {
    val theme = LocalDiaryThemeSpec.current
    var title by remember(initial?.id) { mutableStateOf(initial?.title ?: "") }
    var description by remember(initial?.id) { mutableStateOf(initial?.description ?: "") }
    var category by remember(initial?.id) { mutableStateOf(initial?.categoryEnum ?: DreamCategory.BEAUTIFUL) }
    var mood by remember(initial?.id) { mutableStateOf(initial?.moodEnum ?: DiaryMood.HAPPY) }
    var tagsText by remember(initial?.id) { mutableStateOf(initial?.tags?.joinToString(", ") ?: "") }
    val images = remember(initial?.id) { mutableStateListOf<String>().apply { addAll(initial?.imageUris ?: emptyList()) } }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            mediaManager.persistPickedMedia(uri, isVideo = false)?.let { copied ->
                if (images.size < 10) images.add(copied)
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = theme.backgroundBottom,
        title = {
            Text(
                if (initial == null) "Record a New Dream 🌙" else "Edit Dream ✨",
                color = theme.textPrimary
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it.take(150) },
                    label = { Text("Dream Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it.take(20000) },
                    label = { Text("What happened in your dream?") },
                    minLines = 3,
                    maxLines = 6,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Dream Category", style = MaterialTheme.typography.labelLarge, color = theme.accent)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(DreamCategory.entries) { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text("${cat.emoji} ${cat.label}") }
                        )
                    }
                }
                Text("Dream Mood", style = MaterialTheme.typography.labelLarge, color = theme.accent)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(DiaryMood.entries) { m ->
                        FilterChip(
                            selected = mood == m,
                            onClick = { mood = m },
                            label = { Text("${m.emoji} ${m.label}") }
                        )
                    }
                }
                OutlinedTextField(
                    value = tagsText,
                    onValueChange = { tagsText = it },
                    label = { Text("Tags (comma separated)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = theme.cardSurface,
                    border = BorderStroke(1.dp, theme.borderGlow.copy(alpha = 0.6f)),
                    modifier = Modifier.clickable {
                        imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.AddPhotoAlternate, contentDescription = "Attach dream image", tint = theme.accent)
                        Spacer(Modifier.width(8.dp))
                        Text("Attach Dream Image (${images.size})", style = MaterialTheme.typography.labelMedium, color = theme.textPrimary)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val parsedTags = tagsText.split(",").map { it.trim() }.filter { it.isNotEmpty() }.take(15)
                onSave(
                    DreamEntry(
                        id = initial?.id ?: "",
                        title = title.trim().ifBlank { "Starry Dream" },
                        description = description.trim(),
                        category = category.name,
                        mood = mood.name,
                        dateKey = initial?.dateKey ?: todayDateKey(),
                        tags = parsedTags,
                        imageUris = images.toList(),
                        favorite = initial?.favorite ?: false
                    )
                )
            }) {
                Text("Save Dream 🌙", color = theme.primary, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = theme.textSecondary) }
        }
    )
}
