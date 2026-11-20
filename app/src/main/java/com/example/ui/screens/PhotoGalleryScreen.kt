package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Slideshow
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.PhotoMemory
import com.example.data.dateKeyToPretty
import com.example.ui.components.AestheticTopBar
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.GlowingGlassCard
import com.example.ui.components.NeonGradientButton
import com.example.ui.theme.LocalDiaryThemeSpec
import kotlinx.coroutines.delay

@Composable
fun PhotoGalleryScreen(
    memories: List<PhotoMemory>,
    onUploadMemories: (uris: List<Uri>, isVideo: Boolean, albumName: String, caption: String) -> Unit,
    onToggleFavorite: (PhotoMemory) -> Unit,
    onUpdateMemory: (PhotoMemory, String, String, List<String>, Boolean) -> Unit,
    onDeleteMemory: (PhotoMemory) -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    val theme = LocalDiaryThemeSpec.current
    var searchQuery by remember { mutableStateOf("") }
    var selectedAlbum by remember { mutableStateOf("All") }
    var favoritesOnly by remember { mutableStateOf(false) }
    val customAlbums = remember { mutableStateListOf("My Memories", "Favorites", "Cute Moments", "Travel") }
    var activeUploadAlbum by remember { mutableStateOf("My Memories") }
    var showCreateAlbumDialog by remember { mutableStateOf(false) }
    var fullscreenMemory by remember { mutableStateOf<PhotoMemory?>(null) }
    var slideshowActive by remember { mutableStateOf(false) }
    var slideshowIndex by remember { mutableIntStateOf(0) }

    val allAlbums = remember(memories, customAlbums.size) {
        (listOf("All") + customAlbums + memories.map { it.albumName }).distinct()
    }

    val filtered = remember(memories, searchQuery, selectedAlbum, favoritesOnly) {
        memories.filter { m ->
            val matchAlbum = selectedAlbum == "All" || m.albumName == selectedAlbum
            val matchFav = !favoritesOnly || m.favorite
            val q = searchQuery.trim().lowercase()
            val matchQuery = q.isEmpty() ||
                m.caption.lowercase().contains(q) ||
                m.albumName.lowercase().contains(q) ||
                m.tags.any { it.lowercase().contains(q) }
            matchAlbum && matchFav && matchQuery
        }
    }

    // Zero-permission Photo Picker for multiple photos & videos
    val photoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(maxItems = 10)
    ) { uris ->
        if (uris.isNotEmpty()) {
            onUploadMemories(uris, false, activeUploadAlbum, "")
        }
    }

    val videoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            onUploadMemories(listOf(uri), true, activeUploadAlbum, "Video Memory")
        }
    }

    LaunchedEffect(slideshowActive, filtered.size) {
        while (slideshowActive && filtered.isNotEmpty()) {
            delay(3000)
            slideshowIndex = (slideshowIndex + 1) % filtered.size
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            AestheticTopBar(
                title = "My Memories 📸",
                onBack = onBack,
                actions = {
                    if (filtered.isNotEmpty()) {
                        IconButton(
                            onClick = { slideshowIndex = 0; slideshowActive = true },
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("gallery_slideshow_button")
                        ) {
                            Icon(Icons.Default.Slideshow, contentDescription = "Start slideshow", tint = theme.accent)
                        }
                    }
                    IconButton(
                        onClick = { showCreateAlbumDialog = true },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("gallery_new_album_button")
                    ) {
                        Icon(Icons.Default.CreateNewFolder, contentDescription = "Create album", tint = theme.textPrimary)
                    }
                }
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 36.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search captions, albums, tags...") },
                        leadingIcon = { Icon(Icons.Default.Search, null, tint = theme.accent) },
                        singleLine = true,
                        shape = RoundedCornerShape(20.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = theme.primary,
                            unfocusedBorderColor = theme.borderGlow.copy(alpha = 0.5f),
                            focusedContainerColor = theme.cardSurface,
                            unfocusedContainerColor = theme.cardSurface,
                            focusedTextColor = theme.textPrimary,
                            unfocusedTextColor = theme.textPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Upload Controls Row
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        NeonGradientButton(
                            text = "Upload Photos",
                            leadingEmoji = "📸",
                            testTagName = "gallery_upload_photos",
                            modifier = Modifier.weight(1f),
                            onClick = {
                                activeUploadAlbum = if (selectedAlbum == "All") "My Memories" else selectedAlbum
                                photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            }
                        )
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = theme.cardSurface,
                            border = BorderStroke(1.5.dp, theme.borderGlow),
                            modifier = Modifier
                                .height(52.dp)
                                .clickable {
                                    activeUploadAlbum = if (selectedAlbum == "All") "My Memories" else selectedAlbum
                                    videoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
                                }
                                .testTag("gallery_upload_video")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = "Upload video", tint = theme.accent)
                                Text("Video", style = MaterialTheme.typography.labelLarge, color = theme.textPrimary)
                            }
                        }
                    }
                }

                // Album Chips
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            FilterChip(
                                selected = favoritesOnly,
                                onClick = { favoritesOnly = !favoritesOnly },
                                label = { Text("💗 Favs") }
                            )
                        }
                        items(allAlbums) { album ->
                            FilterChip(
                                selected = selectedAlbum == album,
                                onClick = { selectedAlbum = album },
                                label = { Text(album) }
                            )
                        }
                    }
                }

                if (filtered.isEmpty()) {
                    item {
                        EmptyStateCard(
                            emoji = "📸🎀✨",
                            title = "Your Memory Album Is Empty",
                            subtitle = "Upload photos or videos using Android's private Photo Picker to build your aesthetic memory gallery."
                        )
                    }
                } else {
                    items(filtered.chunked(2)) { pair ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            pair.forEach { memory ->
                                GlowingGlassCard(
                                    modifier = Modifier.weight(1f),
                                    cornerRadius = 20.dp,
                                    contentPadding = PaddingValues(8.dp),
                                    onClick = { fullscreenMemory = memory }
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .aspectRatio(1f)
                                            .clip(RoundedCornerShape(14.dp))
                                    ) {
                                        AsyncImage(
                                            model = memory.mediaUri,
                                            contentDescription = memory.caption.ifBlank { "Memory photo" },
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                        if (memory.mediaType == "VIDEO") {
                                            Surface(
                                                shape = CircleShape,
                                                color = Color.Black.copy(alpha = 0.55f),
                                                modifier = Modifier
                                                    .align(Alignment.Center)
                                                    .size(40.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(Icons.Default.PlayArrow, "Video memory", tint = Color.White)
                                                }
                                            }
                                        }
                                        IconButton(
                                            onClick = { onToggleFavorite(memory) },
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .size(34.dp)
                                        ) {
                                            Icon(
                                                if (memory.favorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                                contentDescription = "Favorite memory",
                                                tint = if (memory.favorite) theme.primary else Color.White,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                    Spacer(Modifier.height(6.dp))
                                    Text(
                                        text = memory.caption.ifBlank { memory.albumName },
                                        style = MaterialTheme.typography.labelLarge,
                                        color = theme.textPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = dateKeyToPretty(memory.dateKey),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = theme.accent
                                    )
                                }
                            }
                            if (pair.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        // Fullscreen / Slideshow Overlay
        val overlayMemory = if (slideshowActive && filtered.isNotEmpty()) {
            filtered[slideshowIndex % filtered.size]
        } else fullscreenMemory

        if (overlayMemory != null) {
            var editCaption by remember(overlayMemory.id) { mutableStateOf(overlayMemory.caption) }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xEE09051F))
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (slideshowActive) "✨ Slideshow (${slideshowIndex + 1}/${filtered.size})" else overlayMemory.albumName,
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White
                        )
                        Row {
                            IconButton(onClick = {
                                onDeleteMemory(overlayMemory)
                                fullscreenMemory = null
                                slideshowActive = false
                            }) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = "Move to recycle bin", tint = Color.White)
                            }
                            IconButton(onClick = { fullscreenMemory = null; slideshowActive = false }) {
                                Icon(Icons.Default.Close, contentDescription = "Close fullscreen", tint = Color.White)
                            }
                        }
                    }

                    AsyncImage(
                        model = overlayMemory.mediaUri,
                        contentDescription = overlayMemory.caption,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(22.dp))
                    )

                    if (!slideshowActive) {
                        GlowingGlassCard(cornerRadius = 20.dp, contentPadding = PaddingValues(12.dp)) {
                            OutlinedTextField(
                                value = editCaption,
                                onValueChange = { editCaption = it.take(500) },
                                label = { Text("Caption") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(dateKeyToPretty(overlayMemory.dateKey), style = MaterialTheme.typography.labelSmall, color = theme.accent)
                                TextButton(onClick = {
                                    onUpdateMemory(overlayMemory, overlayMemory.albumName, editCaption, overlayMemory.tags, overlayMemory.favorite)
                                    fullscreenMemory = null
                                }) {
                                    Text("Save Caption 💗", color = theme.primary, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    } else {
                        Text(
                            text = overlayMemory.caption.ifBlank { dateKeyToPretty(overlayMemory.dateKey) },
                            style = MaterialTheme.typography.titleMedium,
                            color = theme.accent,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                    }
                }
            }
        }
    }

    if (showCreateAlbumDialog) {
        var newAlbumName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreateAlbumDialog = false },
            containerColor = theme.backgroundBottom,
            title = { Text("Create Memory Album 📸", color = theme.textPrimary) },
            text = {
                OutlinedTextField(
                    value = newAlbumName,
                    onValueChange = { newAlbumName = it.take(60) },
                    label = { Text("Album Name") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val clean = newAlbumName.trim()
                    if (clean.isNotEmpty() && clean !in customAlbums) {
                        customAlbums.add(clean)
                        selectedAlbum = clean
                    }
                    showCreateAlbumDialog = false
                }) {
                    Text("Create ✨", color = theme.primary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateAlbumDialog = false }) { Text("Cancel", color = theme.textSecondary) }
            }
        )
    }
}
