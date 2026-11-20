package com.example.ui.screens

import android.app.DatePickerDialog
import android.app.TimePickerDialog
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
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatAlignLeft
import androidx.compose.material.icons.automirrored.filled.FormatAlignRight
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FormatAlignCenter
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.FormatUnderlined
import androidx.compose.material.icons.filled.Highlight
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.DefaultDiaryCategories
import com.example.data.DiaryCategory
import com.example.data.DiaryEntry
import com.example.data.DiaryMood
import com.example.data.MediaAndBackupManager
import com.example.data.dateKeyToPretty
import com.example.data.formatTimeOnly
import com.example.data.todayDateKey
import com.example.firebase.AiDiaryAction
import com.example.firebase.AiUiState
import com.example.ui.components.AestheticTopBar
import com.example.ui.components.GlowingGlassCard
import com.example.ui.components.NeonGradientButton
import com.example.ui.theme.LocalDiaryThemeSpec
import com.example.ui.theme.fontFamilyById
import kotlinx.coroutines.delay
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DiaryEditorScreen(
    existingEntry: DiaryEntry?,
    initialDateKey: String?,
    customCategories: List<DiaryCategory>,
    mediaManager: MediaAndBackupManager,
    aiState: AiUiState,
    onRunAi: (AiDiaryAction, String) -> Unit,
    onResetAi: () -> Unit,
    onSave: (DiaryEntry, Boolean) -> Unit,
    onOpenVoiceDiary: () -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    val theme = LocalDiaryThemeSpec.current
    val context = LocalContext.current

    var entryId by remember(existingEntry?.id) { mutableStateOf(existingEntry?.id ?: "") }
    var title by remember(existingEntry?.id) { mutableStateOf(existingEntry?.title ?: "") }
    var content by remember(existingEntry?.id) { mutableStateOf(existingEntry?.content ?: "") }
    var selectedMood by remember(existingEntry?.id) { mutableStateOf(existingEntry?.moodEnum ?: DiaryMood.HAPPY) }
    var categoryId by remember(existingEntry?.id) { mutableStateOf(existingEntry?.categoryId ?: "personal") }
    var dateKey by remember(existingEntry?.id, initialDateKey) {
        mutableStateOf(existingEntry?.dateKey ?: initialDateKey ?: todayDateKey())
    }
    var entryTimeMillis by remember(existingEntry?.id) {
        mutableLongStateOf(existingEntry?.entryTimeMillis ?: System.currentTimeMillis())
    }
    var locationLabel by remember(existingEntry?.id) { mutableStateOf(existingEntry?.locationLabel ?: "") }
    var backgroundStyle by remember(existingEntry?.id) { mutableStateOf(existingEntry?.backgroundStyle ?: "midnight_pink") }
    var fontStyle by remember(existingEntry?.id) { mutableStateOf(existingEntry?.fontStyle ?: "quicksand") }
    var textColorHex by remember(existingEntry?.id) { mutableStateOf(existingEntry?.textColorHex ?: "#FFFFFF") }
    var textAlign by remember(existingEntry?.id) { mutableStateOf(existingEntry?.textAlign ?: "START") }
    var fontSizeSp by remember(existingEntry?.id) { mutableLongStateOf(existingEntry?.fontSizeSp ?: 16L) }
    var boldEnabled by remember(existingEntry?.id) { mutableStateOf(existingEntry?.boldEnabled ?: false) }
    var italicEnabled by remember(existingEntry?.id) { mutableStateOf(existingEntry?.italicEnabled ?: false) }
    var underlineEnabled by remember(existingEntry?.id) { mutableStateOf(existingEntry?.underlineEnabled ?: false) }
    var highlightEnabled by remember(existingEntry?.id) { mutableStateOf(existingEntry?.highlightEnabled ?: false) }
    var favorite by remember(existingEntry?.id) { mutableStateOf(existingEntry?.favorite ?: false) }
    var privateLocked by remember(existingEntry?.id) { mutableStateOf(existingEntry?.privateLocked ?: false) }
    var reminderEnabled by remember(existingEntry?.id) { mutableStateOf((existingEntry?.reminderTimeMillis ?: 0L) > 0L) }

    val tags = remember(existingEntry?.id) {
        mutableStateListOf<String>().apply { addAll(existingEntry?.tags ?: emptyList()) }
    }
    val photoUris = remember(existingEntry?.id) {
        mutableStateListOf<String>().apply { addAll(existingEntry?.photoUris ?: emptyList()) }
    }
    val stickers = remember(existingEntry?.id) {
        mutableStateListOf<String>().apply { addAll(existingEntry?.stickerIds ?: emptyList()) }
    }

    var showLocationDialog by remember { mutableStateOf(false) }
    var showTagDialog by remember { mutableStateOf(false) }
    var showStickersRow by remember { mutableStateOf(false) }
    var showStyleSheet by remember { mutableStateOf(false) }
    var showAiSheet by remember { mutableStateOf(false) }
    var autoSaveLabel by remember { mutableStateOf("Draft ready") }

    val wordCount = remember(content) {
        content.trim().split(Regex("\\s+")).count { it.isNotBlank() }
    }
    val charCount = content.length

    fun buildCurrentEntry(): DiaryEntry = DiaryEntry(
        id = entryId,
        title = title,
        content = content,
        mood = selectedMood.name,
        moodScore = selectedMood.score,
        categoryId = categoryId,
        dateKey = dateKey,
        entryTimeMillis = entryTimeMillis,
        tags = tags.toList(),
        photoUris = photoUris.toList(),
        locationLabel = locationLabel,
        stickerIds = stickers.toList(),
        backgroundStyle = backgroundStyle,
        fontStyle = fontStyle,
        textColorHex = textColorHex,
        textAlign = textAlign,
        fontSizeSp = fontSizeSp,
        boldEnabled = boldEnabled,
        italicEnabled = italicEnabled,
        underlineEnabled = underlineEnabled,
        highlightEnabled = highlightEnabled,
        favorite = favorite,
        privateLocked = privateLocked,
        wordCount = wordCount.toLong(),
        charCount = charCount.toLong(),
        reminderTimeMillis = if (reminderEnabled) System.currentTimeMillis() + 86_400_000L else 0L
    )

    // Gentle auto-save indicator when editing existing entry
    LaunchedEffect(title, content, selectedMood) {
        if (content.isNotBlank() || title.isNotBlank()) {
            autoSaveLabel = "Unsaved changes..."
            delay(3500)
            if (entryId.isNotBlank()) {
                onSave(buildCurrentEntry(), false)
                autoSaveLabel = "Auto-saved ✨"
            } else {
                autoSaveLabel = "Ready to save 💗"
            }
        }
    }

    // Zero-permission Android Photo Picker for multiple photos/videos
    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 6)
    ) { uris ->
        uris.forEach { uri ->
            val copied = mediaManager.persistPickedMedia(uri, isVideo = false)
            if (copied != null && photoUris.size < 20) {
                photoUris.add(copied)
            }
        }
    }

    val textColor = remember(textColorHex, theme.textPrimary) {
        try {
            Color(android.graphics.Color.parseColor(textColorHex))
        } catch (_: Exception) {
            theme.textPrimary
        }
    }

    val paperBrush = remember(backgroundStyle, theme) {
        when (backgroundStyle) {
            "lavender_glow" -> Brush.verticalGradient(listOf(Color(0xFF341866), Color(0xFF4B2187)))
            "starry_night" -> Brush.verticalGradient(listOf(Color(0xFF0E153A), Color(0xFF1E295E)))
            "rose_silk" -> Brush.verticalGradient(listOf(Color(0xFF4A143A), Color(0xFF6B1D50)))
            "pastel_clouds" -> Brush.verticalGradient(listOf(Color(0xFF2B2466), Color(0xFF42317C)))
            "moonlight_ink" -> Brush.verticalGradient(listOf(Color(0xFF14182E), Color(0xFF22284A)))
            else -> Brush.verticalGradient(listOf(theme.cardSurface, theme.cardSurfaceAlt))
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .navigationBarsPadding()
    ) {
        AestheticTopBar(
            title = "Write Diary ♡",
            onBack = onBack,
            actions = {
                IconButton(
                    onClick = { showAiSheet = true },
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("editor_ai_button")
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = "AI Diary Assistant", tint = theme.accent)
                }
                IconButton(
                    onClick = { favorite = !favorite },
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("editor_favorite_button")
                ) {
                    Icon(
                        if (favorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorite entry",
                        tint = if (favorite) theme.primary else theme.textPrimary
                    )
                }
                IconButton(
                    onClick = { onSave(buildCurrentEntry(), true) },
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("editor_top_save_button")
                ) {
                    Icon(Icons.Default.Check, contentDescription = "Save entry", tint = theme.primary)
                }
            }
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Peeking cute cat + mood card (matches mockup!)
            Box(modifier = Modifier.fillMaxWidth()) {
                GlowingGlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp),
                    cornerRadius = 24.dp,
                    contentPadding = PaddingValues(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "How are you feeling?",
                            style = MaterialTheme.typography.titleMedium,
                            color = theme.textPrimary
                        )
                        Text(
                            text = autoSaveLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = theme.accent
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(DiaryMood.entries) { mood ->
                            val selected = selectedMood == mood
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(
                                        if (selected) theme.primary.copy(alpha = 0.28f) else Color.Transparent
                                    )
                                    .border(
                                        width = if (selected) 1.5.dp else 0.dp,
                                        color = if (selected) theme.borderGlow else Color.Transparent,
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                    .clickable { selectedMood = mood }
                                    .testTag("editor_mood_${mood.name.lowercase()}")
                                    .padding(horizontal = 10.dp, vertical = 8.dp)
                            ) {
                                Text(mood.emoji, fontSize = 26.sp)
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = mood.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (selected) Color.White else theme.textSecondary
                                )
                            }
                        }
                    }
                }
                Image(
                    painter = painterResource(id = R.drawable.img_cat_mascot_1795152809776),
                    contentDescription = "Peeking kitty mascot",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(end = 18.dp)
                        .size(48.dp)
                        .clip(CircleShape)
                        .border(2.dp, theme.borderGlow, CircleShape)
                )
            }

            // Date/Time & Category Picker Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = theme.cardSurface,
                    border = BorderStroke(1.dp, theme.borderGlow.copy(alpha = 0.6f)),
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            val cal = Calendar.getInstance().apply { timeInMillis = entryTimeMillis }
                            DatePickerDialog(
                                context,
                                { _, y, m, d ->
                                    cal.set(y, m, d)
                                    TimePickerDialog(
                                        context,
                                        { _, h, min ->
                                            cal.set(Calendar.HOUR_OF_DAY, h)
                                            cal.set(Calendar.MINUTE, min)
                                            entryTimeMillis = cal.timeInMillis
                                            dateKey = todayDateKey(cal.timeInMillis)
                                        },
                                        cal.get(Calendar.HOUR_OF_DAY),
                                        cal.get(Calendar.MINUTE),
                                        false
                                    ).show()
                                },
                                cal.get(Calendar.YEAR),
                                cal.get(Calendar.MONTH),
                                cal.get(Calendar.DAY_OF_MONTH)
                            ).show()
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CalendarToday, contentDescription = "Pick date", tint = theme.accent, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "${dateKeyToPretty(dateKey)} • ${formatTimeOnly(entryTimeMillis)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = theme.textPrimary,
                            maxLines = 1
                        )
                    }
                }
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = theme.cardSurface,
                    border = BorderStroke(1.dp, theme.borderGlow.copy(alpha = 0.6f)),
                    modifier = Modifier.clickable { showStyleSheet = true }
                ) {
                    Text(
                        text = "🎨 Paper",
                        style = MaterialTheme.typography.labelMedium,
                        color = theme.accent,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                    )
                }
            }

            // Category Pills
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(DefaultDiaryCategories.builtIn) { cat ->
                    FilterChip(
                        selected = categoryId == cat.id,
                        onClick = { categoryId = cat.id },
                        label = { Text("${cat.emoji} ${cat.name}", maxLines = 1) }
                    )
                }
                items(customCategories) { cat ->
                    FilterChip(
                        selected = categoryId == cat.id,
                        onClick = { categoryId = cat.id },
                        label = { Text("${cat.iconKey} ${cat.name}", maxLines = 1) }
                    )
                }
            }

            // Entry Title Input
            OutlinedTextField(
                value = title,
                onValueChange = { if (it.length <= 150) title = it },
                placeholder = { Text("✨ Title (Optional)... ♡", color = theme.textSecondary.copy(alpha = 0.7f)) },
                singleLine = true,
                shape = RoundedCornerShape(20.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = theme.primary,
                    unfocusedBorderColor = theme.borderGlow.copy(alpha = 0.6f),
                    focusedContainerColor = theme.cardSurface,
                    unfocusedContainerColor = theme.cardSurface,
                    focusedTextColor = theme.textPrimary,
                    unfocusedTextColor = theme.textPrimary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("editor_title_input")
            )

            // Rich Text Formatting Toolbar
            GlowingGlassCard(
                cornerRadius = 18.dp,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
            ) {
                LazyRow(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    item {
                        IconButton(onClick = { boldEnabled = !boldEnabled }) {
                            Icon(Icons.Default.FormatBold, "Bold", tint = if (boldEnabled) theme.primary else theme.textSecondary)
                        }
                    }
                    item {
                        IconButton(onClick = { italicEnabled = !italicEnabled }) {
                            Icon(Icons.Default.FormatItalic, "Italic", tint = if (italicEnabled) theme.primary else theme.textSecondary)
                        }
                    }
                    item {
                        IconButton(onClick = { underlineEnabled = !underlineEnabled }) {
                            Icon(Icons.Default.FormatUnderlined, "Underline", tint = if (underlineEnabled) theme.primary else theme.textSecondary)
                        }
                    }
                    item {
                        IconButton(onClick = { highlightEnabled = !highlightEnabled }) {
                            Icon(Icons.Default.Highlight, "Highlight", tint = if (highlightEnabled) Color(0xFFFDE047) else theme.textSecondary)
                        }
                    }
                    item {
                        IconButton(onClick = {
                            textAlign = when (textAlign) {
                                "START" -> "CENTER"
                                "CENTER" -> "END"
                                else -> "START"
                            }
                        }) {
                            Icon(
                                when (textAlign) {
                                    "CENTER" -> Icons.Default.FormatAlignCenter
                                    "END" -> Icons.AutoMirrored.Filled.FormatAlignRight
                                    else -> Icons.AutoMirrored.Filled.FormatAlignLeft
                                },
                                contentDescription = "Text alignment",
                                tint = theme.accent
                            )
                        }
                    }
                    item {
                        TextButton(onClick = { fontSizeSp = (fontSizeSp - 2L).coerceAtLeast(12L) }) {
                            Text("A-", color = theme.textPrimary, fontWeight = FontWeight.Bold)
                        }
                    }
                    item {
                        TextButton(onClick = { fontSizeSp = (fontSizeSp + 2L).coerceAtMost(28L) }) {
                            Text("A+", color = theme.textPrimary, fontWeight = FontWeight.Bold)
                        }
                    }
                    item {
                        IconButton(onClick = { content = if (content.isEmpty() || content.endsWith("\n")) "$content• " else "$content\n• " }) {
                            Icon(Icons.AutoMirrored.Filled.FormatListBulleted, "Bullet point", tint = theme.textSecondary)
                        }
                    }
                    item {
                        IconButton(onClick = {
                            val nextNum = content.lines().count { it.trim().matches(Regex("^\\d+\\..*")) } + 1
                            content = if (content.isEmpty() || content.endsWith("\n")) "$content$nextNum. " else "$content\n$nextNum. "
                        }) {
                            Icon(Icons.Default.FormatListNumbered, "Numbered list", tint = theme.textSecondary)
                        }
                    }
                    item {
                        TextButton(onClick = { showStickersRow = !showStickersRow }) {
                            Text("🎀 Stickers", color = theme.accent)
                        }
                    }
                }
            }

            if (showStickersRow) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(DefaultDiaryCategories.stickers) { sticker ->
                        Surface(
                            shape = CircleShape,
                            color = theme.cardSurface,
                            border = BorderStroke(1.dp, theme.borderGlow.copy(alpha = 0.6f)),
                            modifier = Modifier
                                .size(44.dp)
                                .clickable {
                                    if (stickers.size < 15) stickers.add(sticker)
                                    content += " $sticker"
                                }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(sticker, fontSize = 22.sp)
                            }
                        }
                    }
                }
            }

            // Main Rich Text Writing Canvas
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(paperBrush)
                    .border(1.5.dp, theme.borderGlow.copy(alpha = 0.75f), RoundedCornerShape(24.dp))
                    .padding(6.dp)
            ) {
                Column {
                    OutlinedTextField(
                        value = content,
                        onValueChange = { if (it.length <= 50000) content = it },
                        placeholder = {
                            Text(
                                "Write your thoughts...\nLet it out... ♡",
                                color = Color.White.copy(alpha = 0.55f)
                            )
                        },
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            fontFamily = fontFamilyById(fontStyle),
                            fontSize = fontSizeSp.toInt().sp,
                            fontWeight = if (boldEnabled) FontWeight.Bold else FontWeight.Normal,
                            fontStyle = if (italicEnabled) FontStyle.Italic else FontStyle.Normal,
                            textDecoration = if (underlineEnabled) TextDecoration.Underline else TextDecoration.None,
                            textAlign = when (textAlign) {
                                "CENTER" -> TextAlign.Center
                                "END" -> TextAlign.End
                                else -> TextAlign.Start
                            },
                            color = textColor,
                            background = if (highlightEnabled) theme.primary.copy(alpha = 0.35f) else Color.Transparent
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 210.dp)
                            .testTag("editor_content_input")
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (locationLabel.isNotBlank()) "📍 $locationLabel" else "💗 Secret Page",
                            style = MaterialTheme.typography.labelSmall,
                            color = theme.accent
                        )
                        Text(
                            text = "$wordCount words • $charCount chars",
                            style = MaterialTheme.typography.labelSmall,
                            color = theme.textSecondary
                        )
                    }
                }
            }

            // Attached Photos Preview
            if (photoUris.isNotEmpty()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(photoUris) { uriStr ->
                        Box(modifier = Modifier.size(86.dp)) {
                            AsyncImage(
                                model = uriStr,
                                contentDescription = "Attached diary photo",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(16.dp))
                                    .border(1.dp, theme.borderGlow, RoundedCornerShape(16.dp))
                            )
                            IconButton(
                                onClick = { photoUris.remove(uriStr) },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .size(26.dp)
                                    .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Remove photo", tint = Color.White, modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                }
            }

            // Tags Chips
            if (tags.isNotEmpty()) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    tags.forEach { tag ->
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = theme.primary.copy(alpha = 0.22f),
                            border = BorderStroke(1.dp, theme.borderGlow.copy(alpha = 0.6f)),
                            modifier = Modifier.clickable { tags.remove(tag) }
                        ) {
                            Text(
                                text = "#$tag  ✕",
                                style = MaterialTheme.typography.labelSmall,
                                color = theme.accent,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            // Media Attachment Row (Matches mockup Add Photos / Add Audio / Add Location + Tags)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MediaActionTile(
                    icon = Icons.Default.AddPhotoAlternate,
                    label = "Add Photos",
                    modifier = Modifier
                        .weight(1f)
                        .testTag("editor_add_photos"),
                    onClick = {
                        photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }
                )
                MediaActionTile(
                    icon = Icons.Default.Mic,
                    label = "Voice Diary",
                    modifier = Modifier
                        .weight(1f)
                        .testTag("editor_voice_diary"),
                    onClick = onOpenVoiceDiary
                )
                MediaActionTile(
                    icon = Icons.Default.LocationOn,
                    label = "Location",
                    modifier = Modifier
                        .weight(1f)
                        .testTag("editor_add_location"),
                    onClick = { showLocationDialog = true }
                )
                MediaActionTile(
                    icon = Icons.Default.AutoAwesome,
                    label = "+ Tag",
                    modifier = Modifier
                        .weight(1f)
                        .testTag("editor_add_tag"),
                    onClick = { showTagDialog = true }
                )
            }

            // Options Card (Set Reminder + Make it Private — matches mockup!)
            GlowingGlassCard(
                cornerRadius = 22.dp,
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = theme.accent)
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text("Set Reminder", style = MaterialTheme.typography.titleSmall, color = theme.textPrimary)
                            Text("Remind me to revisit tomorrow", style = MaterialTheme.typography.labelSmall, color = theme.textSecondary)
                        }
                    }
                    Switch(
                        checked = reminderEnabled,
                        onCheckedChange = { reminderEnabled = it },
                        colors = SwitchDefaults.colors(checkedTrackColor = theme.primary)
                    )
                }
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = theme.primary)
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text("Make it Private", style = MaterialTheme.typography.titleSmall, color = theme.textPrimary)
                            Text("Lock & hide in Secret Mode", style = MaterialTheme.typography.labelSmall, color = theme.textSecondary)
                        }
                    }
                    Switch(
                        checked = privateLocked,
                        onCheckedChange = { privateLocked = it },
                        colors = SwitchDefaults.colors(checkedTrackColor = theme.primary),
                        modifier = Modifier.testTag("editor_private_switch")
                    )
                }
            }

            // Glossy Save Entry Button
            NeonGradientButton(
                text = if (entryId.isBlank()) "Save Entry" else "Update Entry",
                leadingEmoji = "🪶",
                trailingEmoji = "💗",
                testTagName = "editor_save_button",
                modifier = Modifier.fillMaxWidth(),
                onClick = { onSave(buildCurrentEntry(), true) }
            )
            Spacer(Modifier.height(16.dp))
        }
    }

    // Location Dialog
    if (showLocationDialog) {
        var tempLoc by remember { mutableStateOf(locationLabel) }
        AlertDialog(
            onDismissRequest = { showLocationDialog = false },
            containerColor = theme.backgroundBottom,
            title = { Text("Add Location 📍", color = theme.textPrimary) },
            text = {
                OutlinedTextField(
                    value = tempLoc,
                    onValueChange = { tempLoc = it.take(120) },
                    label = { Text("Place or memory spot") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = { locationLabel = tempLoc.trim(); showLocationDialog = false }) {
                    Text("Save", color = theme.primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLocationDialog = false }) { Text("Cancel", color = theme.textSecondary) }
            }
        )
    }

    // Tag Dialog
    if (showTagDialog) {
        var tempTag by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showTagDialog = false },
            containerColor = theme.backgroundBottom,
            title = { Text("Add Tag ✨", color = theme.textPrimary) },
            text = {
                OutlinedTextField(
                    value = tempTag,
                    onValueChange = { tempTag = it.take(30) },
                    label = { Text("e.g. grateful, memories, cozy") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val clean = tempTag.trim().removePrefix("#")
                    if (clean.isNotEmpty() && tags.size < 15 && clean !in tags) tags.add(clean)
                    showTagDialog = false
                }) {
                    Text("Add", color = theme.primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showTagDialog = false }) { Text("Cancel", color = theme.textSecondary) }
            }
        )
    }

    // Entry Style Bottom Sheet (Paper Background, Font, Text Color)
    if (showStyleSheet) {
        ModalBottomSheet(
            onDismissRequest = { showStyleSheet = false },
            containerColor = theme.backgroundBottom
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text("Customize Entry Paper 🎨", style = MaterialTheme.typography.titleLarge, color = theme.textPrimary)
                Text("Paper Background", style = MaterialTheme.typography.labelLarge, color = theme.accent)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(DefaultDiaryCategories.entryBackgrounds) { (id, label) ->
                        FilterChip(selected = backgroundStyle == id, onClick = { backgroundStyle = id }, label = { Text(label) })
                    }
                }
                Text("Entry Font", style = MaterialTheme.typography.labelLarge, color = theme.accent)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("quicksand" to "Quicksand", "nunito" to "Nunito", "playfair" to "Playfair", "pacifico" to "Pacifico").forEach { (id, label) ->
                        FilterChip(selected = fontStyle == id, onClick = { fontStyle = id }, label = { Text(label) })
                    }
                }
                Text("Text Ink Color", style = MaterialTheme.typography.labelLarge, color = theme.accent)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    listOf("#FFFFFF", "#FFB6E6", "#E9D5FF", "#FEF08A", "#BAE6FD", "#A7F3D0").forEach { hex ->
                        val c = Color(android.graphics.Color.parseColor(hex))
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(c)
                                .border(if (textColorHex == hex) 3.dp else 1.dp, theme.primary, CircleShape)
                                .clickable { textColorHex = hex }
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
            }
        }
    }

    // AI Diary Assistant Bottom Sheet (Explicit Opt-In Privacy Gate!)
    if (showAiSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAiSheet = false; onResetAi() },
            containerColor = theme.backgroundBottom
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("✨ Gemini AI Diary Assistant", style = MaterialTheme.typography.titleLarge, color = theme.textPrimary)
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = theme.primary.copy(alpha = 0.16f),
                    border = BorderStroke(1.dp, theme.borderGlow.copy(alpha = 0.6f))
                ) {
                    Text(
                        text = "🔒 Privacy First: Your diary is never sent to AI automatically. Only the text below is shared when you explicitly tap an AI action.",
                        style = MaterialTheme.typography.bodySmall,
                        color = theme.accent,
                        modifier = Modifier.padding(12.dp)
                    )
                }

                val editorActions = listOf(
                    AiDiaryAction.SUGGEST_TITLES,
                    AiDiaryAction.IMPROVE_WRITING,
                    AiDiaryAction.FIX_GRAMMAR,
                    AiDiaryAction.SUMMARIZE,
                    AiDiaryAction.WRITING_PROMPTS,
                    AiDiaryAction.AFFIRMATIONS
                )

                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    editorActions.forEach { action ->
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = theme.cardSurface,
                            border = BorderStroke(1.dp, theme.borderGlow.copy(alpha = 0.7f)),
                            modifier = Modifier.clickable {
                                val sharePayload = when (action) {
                                    AiDiaryAction.WRITING_PROMPTS, AiDiaryAction.AFFIRMATIONS -> selectedMood.label
                                    else -> content.ifBlank { title.ifBlank { "Feeling ${selectedMood.label} today" } }
                                }
                                onRunAi(action, sharePayload)
                            }
                        ) {
                            Text(
                                text = "✨ ${action.title}",
                                style = MaterialTheme.typography.labelLarge,
                                color = theme.textPrimary,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                            )
                        }
                    }
                }

                when (aiState) {
                    AiUiState.Idle -> {}
                    AiUiState.Loading -> {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            CircularProgressIndicator(color = theme.primary, modifier = Modifier.size(22.dp), strokeWidth = 2.5.dp)
                            Text("Thinking with stardust...", color = theme.accent)
                        }
                    }
                    is AiUiState.Streaming -> {
                        GlowingGlassCard(cornerRadius = 18.dp, contentPadding = PaddingValues(14.dp)) {
                            Text(aiState.action.title, style = MaterialTheme.typography.labelMedium, color = theme.accent)
                            Spacer(Modifier.height(4.dp))
                            Text(aiState.text, style = MaterialTheme.typography.bodyMedium, color = theme.textPrimary)
                        }
                    }
                    is AiUiState.Success -> {
                        GlowingGlassCard(cornerRadius = 18.dp, contentPadding = PaddingValues(14.dp)) {
                            Text(aiState.action.title, style = MaterialTheme.typography.labelMedium, color = theme.accent)
                            Spacer(Modifier.height(6.dp))
                            Text(aiState.text, style = MaterialTheme.typography.bodyMedium, color = theme.textPrimary)
                            Spacer(Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (aiState.action == AiDiaryAction.IMPROVE_WRITING || aiState.action == AiDiaryAction.FIX_GRAMMAR) {
                                    TextButton(onClick = { content = aiState.text; showAiSheet = false; onResetAi() }) {
                                        Text("Replace Entry Text", color = theme.primary, fontWeight = FontWeight.Bold)
                                    }
                                } else {
                                    TextButton(onClick = {
                                        content = if (content.isBlank()) aiState.text else "$content\n\n${aiState.text}"
                                        showAiSheet = false
                                        onResetAi()
                                    }) {
                                        Text("Insert into Entry", color = theme.primary, fontWeight = FontWeight.Bold)
                                    }
                                }
                                if (aiState.action == AiDiaryAction.SUGGEST_TITLES) {
                                    TextButton(onClick = {
                                        val first = aiState.text.lines().firstOrNull { it.isNotBlank() }
                                            ?.replace(Regex("^\\d+[.)]\\s*"), "")
                                            ?.trim()
                                            ?.trim('"', '*')
                                            .orEmpty()
                                        if (first.isNotBlank()) title = first.take(150)
                                        showAiSheet = false
                                        onResetAi()
                                    }) {
                                        Text("Use 1st Title", color = theme.accent, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                    is AiUiState.Error -> {
                        Text(aiState.message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                Spacer(Modifier.height(18.dp))
            }
        }
    }
}

@Composable
private fun MediaActionTile(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val theme = LocalDiaryThemeSpec.current
    GlowingGlassCard(
        modifier = modifier,
        cornerRadius = 16.dp,
        contentPadding = PaddingValues(vertical = 10.dp, horizontal = 6.dp),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = label, tint = theme.accent, modifier = Modifier.size(22.dp))
            Spacer(Modifier.height(4.dp))
            Text(label, style = MaterialTheme.typography.labelSmall, color = theme.textPrimary, maxLines = 1)
        }
    }
}
