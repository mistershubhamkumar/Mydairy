package com.example.ui.screens

import android.content.Intent
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.AppPreferences
import com.example.data.UserProfile
import com.example.firebase.AiDiaryAction
import com.example.firebase.AiUiState
import com.example.ui.components.AestheticTopBar
import com.example.ui.components.GlowingGlassCard
import com.example.ui.components.NeonGradientButton
import com.example.ui.theme.DiaryThemes
import com.example.ui.theme.LocalDiaryThemeSpec
import com.example.ui.theme.PacificoFontFamily
import com.example.viewmodel.DiaryStats
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun ProfileScreen(
    profile: UserProfile?,
    preferences: AppPreferences,
    stats: DiaryStats,
    aiState: AiUiState,
    onUpdateProfile: (name: String, bio: String, avatarKey: String) -> Unit,
    onRunAi: (AiDiaryAction, String) -> Unit,
    onResetAi: () -> Unit,
    onOpenThemeStore: () -> Unit,
    onOpenPrivacy: () -> Unit,
    onOpenBackup: () -> Unit,
    onOpenRecycleBin: () -> Unit,
    onOpenReminders: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val theme = LocalDiaryThemeSpec.current
    var showEditDialog by remember { mutableStateOf(false) }
    var showAiCompanionDialog by remember { mutableStateOf(false) }

    val displayName = profile?.displayName?.ifBlank { "Princess" } ?: "Princess"
    val bio = profile?.bio?.ifBlank { "Write • Feel • Heal • Remember Forever 💗" }
        ?: "Write • Feel • Heal • Remember Forever 💗"
    val email = profile?.email.orEmpty()
    val createdDate = remember(profile?.createdAt) {
        profile?.createdAt?.toDate()?.let {
            SimpleDateFormat("MMM yyyy", Locale.getDefault()).format(it)
        } ?: "Member"
    }
    val avatarRes = when (profile?.avatarKey) {
        "cat" -> R.drawable.img_cat_mascot_1795152809776
        "dream" -> R.drawable.img_dream_castle_1795152811121
        else -> R.drawable.img_avatar_princess_1795152826048
    }

    Column(modifier = Modifier.fillMaxSize()) {
        AestheticTopBar(
            title = "My Profile 👑",
            actions = {
                IconButton(
                    onClick = onOpenSettings,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("profile_open_settings_button")
                ) {
                    Icon(Icons.Default.Settings, contentDescription = "Open Settings", tint = theme.textPrimary)
                }
            }
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 112.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                GlowingGlassCard(contentPadding = PaddingValues(20.dp)) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(contentAlignment = Alignment.BottomEnd) {
                            Image(
                                painter = painterResource(id = avatarRes),
                                contentDescription = "Profile avatar",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(92.dp)
                                    .clip(CircleShape)
                                    .border(
                                        2.5.dp,
                                        Brush.linearGradient(listOf(theme.primary, theme.accent, theme.secondary)),
                                        CircleShape
                                    )
                            )
                            Surface(
                                shape = CircleShape,
                                color = theme.primary,
                                modifier = Modifier
                                    .size(32.dp)
                                    .testTag("edit_profile_button")
                                    .clickable { showEditDialog = true }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit profile", tint = Color.White, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = "$displayName 👑",
                            style = MaterialTheme.typography.headlineSmall.copy(fontFamily = PacificoFontFamily),
                            color = theme.textPrimary
                        )
                        if (email.isNotBlank()) {
                            Text(email, style = MaterialTheme.typography.bodySmall, color = theme.accent)
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = bio,
                            style = MaterialTheme.typography.bodyMedium,
                            color = theme.textSecondary,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(6.dp))
                        Text("Joined $createdDate • Theme: ${DiaryThemes.byId(preferences.themeId).name}", style = MaterialTheme.typography.labelSmall, color = theme.textSecondary)

                        Spacer(Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            ProfileStatMini("Entries", "${stats.totalEntries}")
                            ProfileStatMini("Streak", "${stats.currentStreak}d 🔥")
                            ProfileStatMini("Dreams", "${stats.totalDreams}")
                            ProfileStatMini("Memories", "${stats.totalPhotos}")
                        }
                    }
                }
            }

            item {
                GlowingGlassCard(
                    modifier = Modifier.testTag("profile_ai_companion_card"),
                    onClick = { showAiCompanionDialog = true }
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Brush.linearGradient(listOf(theme.primary, theme.secondary))),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = "AI Companion", tint = Color.White)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("AI Reflection Companion ✨", style = MaterialTheme.typography.titleMedium, color = theme.textPrimary)
                            Text("Get writing prompts, positive affirmations & inspiration (opt-in only)", style = MaterialTheme.typography.bodySmall, color = theme.textSecondary)
                        }
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = theme.accent)
                    }
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    ProfileMenuRow(Icons.Default.Palette, theme.primary, "Theme Store (20 Themes)", "Colors, fonts, mascots & particles", "profile_theme_store", onOpenThemeStore)
                    ProfileMenuRow(Icons.Default.Lock, theme.secondary, "Privacy & Security", "App Lock PIN/Password, Fingerprint & Secret Mode", "profile_privacy", onOpenPrivacy)
                    ProfileMenuRow(Icons.Default.Notifications, Color(0xFFF472B6), "Reminders & Notifications", "Daily diary, mood & streak reminders", "profile_reminders", onOpenReminders)
                    ProfileMenuRow(Icons.Default.CloudUpload, Color(0xFF38BDF8), "Backup & Export", "Cloud sync, PDF/TXT/JSON export & restore", "profile_backup", onOpenBackup)
                    ProfileMenuRow(Icons.Default.DeleteOutline, Color(0xFFF59E0B), "Recycle Bin", "Recover deleted items within 30 days", "profile_recycle_bin", onOpenRecycleBin)
                    ProfileMenuRow(Icons.Default.Settings, theme.accent, "Settings & Account", "Language, storage, share, logout & account", "profile_settings_row", onOpenSettings)
                }
            }
        }
    }

    if (showEditDialog) {
        var newName by remember { mutableStateOf(displayName) }
        var newBio by remember { mutableStateOf(bio) }
        var newAvatar by remember { mutableStateOf(profile?.avatarKey ?: "princess") }

        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            containerColor = theme.cardSurfaceAlt,
            title = { Text("Edit Profile 👑", color = theme.textPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Choose Avatar", style = MaterialTheme.typography.labelMedium, color = theme.accent)
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        listOf(
                            "princess" to R.drawable.img_avatar_princess_1795152826048,
                            "cat" to R.drawable.img_cat_mascot_1795152809776,
                            "dream" to R.drawable.img_dream_castle_1795152811121
                        ).forEach { (key, res) ->
                            Image(
                                painter = painterResource(id = res),
                                contentDescription = key,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .border(
                                        width = if (newAvatar == key) 3.dp else 1.dp,
                                        color = if (newAvatar == key) theme.primary else theme.borderGlow.copy(alpha = 0.4f),
                                        shape = CircleShape
                                    )
                                    .clickable { newAvatar = key }
                            )
                        }
                    }
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text("Display Name") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = theme.primary,
                            focusedTextColor = theme.textPrimary,
                            unfocusedTextColor = theme.textPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newBio,
                        onValueChange = { newBio = it },
                        label = { Text("Bio / Personal Motto") },
                        maxLines = 3,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = theme.primary,
                            focusedTextColor = theme.textPrimary,
                            unfocusedTextColor = theme.textPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUpdateProfile(newName.trim().ifBlank { "Princess" }, newBio.trim(), newAvatar)
                        showEditDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = theme.primary)
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) { Text("Cancel", color = theme.accent) }
            }
        )
    }

    if (showAiCompanionDialog) {
        var moodContext by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = {
                onResetAi()
                showAiCompanionDialog = false
            },
            containerColor = theme.cardSurfaceAlt,
            title = { Text("AI Diary Companion ✨", color = theme.textPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Privacy first: only what you type below is shared when you tap a button.",
                        style = MaterialTheme.typography.bodySmall,
                        color = theme.textSecondary
                    )
                    OutlinedTextField(
                        value = moodContext,
                        onValueChange = { moodContext = it },
                        label = { Text("How are you feeling tonight? (optional)") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = theme.primary,
                            focusedTextColor = theme.textPrimary,
                            unfocusedTextColor = theme.textPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { onRunAi(AiDiaryAction.WRITING_PROMPTS, moodContext) },
                            border = BorderStroke(1.dp, theme.primary),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Prompts", color = theme.textPrimary, maxLines = 1)
                        }
                        OutlinedButton(
                            onClick = { onRunAi(AiDiaryAction.AFFIRMATIONS, moodContext) },
                            border = BorderStroke(1.dp, theme.secondary),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Affirmations", color = theme.textPrimary, maxLines = 1)
                        }
                    }
                    when (aiState) {
                        AiUiState.Idle -> Unit
                        AiUiState.Loading -> Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = theme.primary, modifier = Modifier.size(28.dp))
                        }
                        is AiUiState.Streaming -> Text(aiState.text, color = theme.textPrimary, style = MaterialTheme.typography.bodyMedium)
                        is AiUiState.Success -> Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = theme.cardSurface,
                            border = BorderStroke(1.dp, theme.borderGlow.copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = aiState.text,
                                color = theme.textPrimary,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                        is AiUiState.Error -> Text(aiState.message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    onResetAi()
                    showAiCompanionDialog = false
                }) {
                    Text("Close", color = theme.accent)
                }
            }
        )
    }
}

@Composable
private fun ProfileStatMini(label: String, value: String) {
    val theme = LocalDiaryThemeSpec.current
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleMedium, color = theme.textPrimary, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = theme.textSecondary)
    }
}

@Composable
private fun ProfileMenuRow(
    icon: ImageVector,
    tint: Color,
    title: String,
    subtitle: String,
    tag: String,
    onClick: () -> Unit
) {
    val theme = LocalDiaryThemeSpec.current
    GlowingGlassCard(
        cornerRadius = 20.dp,
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
        modifier = Modifier.testTag(tag),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(tint.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = title, tint = tint, modifier = Modifier.size(21.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, color = theme.textPrimary)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = theme.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = theme.accent)
        }
    }
}

@Composable
fun SettingsScreen(
    profile: UserProfile?,
    preferences: AppPreferences,
    localStorageBytes: Long,
    onSelectLanguage: (String) -> Unit,
    onToggleNotifications: (Boolean) -> Unit,
    onToggleSoundVibration: (Boolean) -> Unit,
    onOpenThemeStore: () -> Unit,
    onOpenReminders: () -> Unit,
    onOpenPrivacy: () -> Unit,
    onOpenBackup: () -> Unit,
    onOpenRecycleBin: () -> Unit,
    onLogout: () -> Unit,
    onDeleteAccount: () -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    val theme = LocalDiaryThemeSpec.current
    val context = LocalContext.current
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showHelpDialog by remember { mutableStateOf(false) }
    var showRateDialog by remember { mutableStateOf(false) }
    var showDeleteAccountDialog by remember { mutableStateOf(false) }

    val formattedTime = remember(preferences.reminderHour, preferences.reminderMinute) {
        val amPm = if (preferences.reminderHour >= 12) "PM" else "AM"
        val h12 = when {
            preferences.reminderHour == 0 -> 12
            preferences.reminderHour > 12 -> preferences.reminderHour - 12
            else -> preferences.reminderHour
        }
        String.format(Locale.getDefault(), "%d:%02d %s", h12, preferences.reminderMinute, amPm)
    }

    val storageLabel = remember(localStorageBytes) {
        val kb = localStorageBytes / 1024.0
        val mb = kb / 1024.0
        if (mb >= 1.0) String.format(Locale.getDefault(), "%.2f MB local media", mb)
        else String.format(Locale.getDefault(), "%.1f KB local media", kb)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        AestheticTopBar(title = "Settings ♡", onBack = onBack)

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 48.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                GlowingGlassCard(contentPadding = PaddingValues(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = R.drawable.img_avatar_princess_1795152826048),
                            contentDescription = "App profile avatar",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .border(2.dp, theme.borderGlow, CircleShape)
                        )
                        Spacer(Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${profile?.displayName?.ifBlank { "Princess Diary" } ?: "Princess Diary"} 👑",
                                style = MaterialTheme.typography.titleLarge,
                                color = theme.textPrimary
                            )
                            Text("My Diary – Secret Feelings • Version 1.0.0", style = MaterialTheme.typography.bodySmall, color = theme.accent)
                        }
                    }
                }
            }

            item {
                GlowingGlassCard(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)) {
                    // Change Theme with mini preview swatches
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_change_theme")
                            .clickable(onClick = onOpenThemeStore)
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Palette, contentDescription = "Theme", tint = theme.primary)
                        Spacer(Modifier.width(12.dp))
                        Text("Change Theme", style = MaterialTheme.typography.titleSmall, color = theme.textPrimary, modifier = Modifier.weight(1f))
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            DiaryThemes.all.take(4).forEach { spec ->
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clip(CircleShape)
                                        .background(Brush.linearGradient(listOf(spec.primary, spec.secondary)))
                                        .border(1.dp, Color.White.copy(alpha = 0.7f), CircleShape)
                                )
                            }
                        }
                        Spacer(Modifier.width(6.dp))
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = theme.accent)
                    }

                    SettingsSimpleRow(Icons.Default.Language, theme.secondary, "Language", preferences.language) {
                        showLanguageDialog = true
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Notifications, contentDescription = "Notification", tint = Color(0xFFF472B6))
                        Spacer(Modifier.width(12.dp))
                        Text("Notifications", style = MaterialTheme.typography.titleSmall, color = theme.textPrimary, modifier = Modifier.weight(1f))
                        Switch(
                            checked = preferences.notificationsEnabled,
                            onCheckedChange = onToggleNotifications,
                            colors = SwitchDefaults.colors(checkedTrackColor = theme.primary)
                        )
                    }

                    SettingsSimpleRow(Icons.Default.AccessTime, Color(0xFF818CF8), "Daily Reminder", formattedTime, onOpenReminders)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.VolumeUp, contentDescription = "Sound", tint = Color(0xFF38BDF8))
                        Spacer(Modifier.width(12.dp))
                        Text("Sound & Vibration", style = MaterialTheme.typography.titleSmall, color = theme.textPrimary, modifier = Modifier.weight(1f))
                        Switch(
                            checked = preferences.soundVibrationEnabled,
                            onCheckedChange = onToggleSoundVibration,
                            colors = SwitchDefaults.colors(checkedTrackColor = theme.primary)
                        )
                    }

                    SettingsSimpleRow(Icons.Default.Lock, theme.primary, "Privacy & Security", if (preferences.appLockEnabled) "Locked" else "Open", onOpenPrivacy)
                    SettingsSimpleRow(Icons.Default.CloudUpload, Color(0xFF34D399), "Backup & Restore", "Cloud + PDF", onOpenBackup)
                    SettingsSimpleRow(Icons.Default.DeleteOutline, Color(0xFFF59E0B), "Recycle Bin", "30-day recovery", onOpenRecycleBin)
                    SettingsSimpleRow(Icons.Default.Storage, theme.accent, "Storage Management", storageLabel) {}
                }
            }

            item {
                GlowingGlassCard(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)) {
                    SettingsSimpleRow(Icons.AutoMirrored.Filled.HelpOutline, theme.accent, "Help & Support", "") { showHelpDialog = true }
                    SettingsSimpleRow(Icons.Default.Info, Color(0xFF60A5FA), "About App", "v1.0.0") { showAboutDialog = true }
                    SettingsSimpleRow(Icons.Default.Share, Color(0xFF34D399), "Share App", "") {
                        val sendIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(
                                Intent.EXTRA_TEXT,
                                "I'm keeping my secret feelings and dreams in My Diary – Secret Feelings! Write • Feel • Heal • Remember Forever 💗"
                            )
                        }
                        context.startActivity(Intent.createChooser(sendIntent, "Share My Diary"))
                    }
                    SettingsSimpleRow(Icons.Default.Star, Color(0xFFFACC15), "Rate Us", "♡") { showRateDialog = true }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    NeonGradientButton(
                        text = "Sign Out",
                        trailingEmoji = "👋",
                        testTagName = "settings_logout_button",
                        modifier = Modifier.weight(1f),
                        onClick = onLogout
                    )
                    OutlinedButton(
                        onClick = { showDeleteAccountDialog = true },
                        border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.error),
                        shape = RoundedCornerShape(50),
                        modifier = Modifier
                            .height(52.dp)
                            .testTag("settings_delete_account_button")
                    ) {
                        Icon(Icons.Default.DeleteForever, contentDescription = "Delete account", tint = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.width(4.dp))
                        Text("Delete", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }

    if (showLanguageDialog) {
        val languages = listOf("English", "Español", "Français", "日本語", "한국어", "हिन्दी")
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            containerColor = theme.cardSurfaceAlt,
            title = { Text("App Language 🌍", color = theme.textPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    languages.forEach { lang ->
                        val active = preferences.language == lang
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (active) theme.primary.copy(alpha = 0.25f) else theme.cardSurface,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSelectLanguage(lang)
                                    showLanguageDialog = false
                                }
                        ) {
                            Text(lang, color = theme.textPrimary, modifier = Modifier.padding(12.dp))
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLanguageDialog = false }) { Text("Close", color = theme.accent) }
            }
        )
    }

    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            containerColor = theme.cardSurfaceAlt,
            title = { Text("My Diary – Secret Feelings 💗", color = theme.textPrimary) },
            text = {
                Text(
                    "Version 1.0.0\n\nWrite • Feel • Heal • Remember Forever.\n\nYour entries, mood check-ins, dream journal, voice notes, and memories are stored exclusively in your private authenticated account with zero-trust isolation.",
                    color = theme.textSecondary
                )
            },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) { Text("Lovely!", color = theme.accent) }
            }
        )
    }

    if (showHelpDialog) {
        AlertDialog(
            onDismissRequest = { showHelpDialog = false },
            containerColor = theme.cardSurfaceAlt,
            title = { Text("Help & Privacy Guide ✨", color = theme.textPrimary) },
            text = {
                Text(
                    "• Tap + on the bottom bar anytime to write a rich diary entry.\n• Turn on App Lock & Biometrics under Privacy & Security.\n• Accidentally deleted an item? Restore it within 30 days from Recycle Bin.\n• Export a PDF, TXT, or JSON backup anytime from Backup & Export.",
                    color = theme.textSecondary
                )
            },
            confirmButton = {
                TextButton(onClick = { showHelpDialog = false }) { Text("Got it", color = theme.accent) }
            }
        )
    }

    if (showRateDialog) {
        AlertDialog(
            onDismissRequest = { showRateDialog = false },
            containerColor = theme.cardSurfaceAlt,
            title = { Text("Love Our App? 💗", color = theme.textPrimary) },
            text = {
                Text(
                    "Thank you for keeping your memories with Mochi Kitty and My Diary! Your love keeps our starry sky shining bright ✨",
                    color = theme.textSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = { showRateDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = theme.primary)
                ) {
                    Text("★★★★★ Sending Love!")
                }
            }
        )
    }

    if (showDeleteAccountDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAccountDialog = false },
            containerColor = theme.cardSurfaceAlt,
            title = { Text("Delete Account Permanently?", color = theme.textPrimary) },
            text = {
                Text(
                    "This will permanently delete all your private diary data and remove your account from this app. This action cannot be undone.",
                    color = theme.textSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteAccountDialog = false
                        onDeleteAccount()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete Account")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAccountDialog = false }) { Text("Cancel", color = theme.accent) }
            }
        )
    }
}

@Composable
private fun SettingsSimpleRow(
    icon: ImageVector,
    tint: Color,
    title: String,
    trailingValue: String,
    onClick: () -> Unit
) {
    val theme = LocalDiaryThemeSpec.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = title, tint = tint, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(12.dp))
        Text(title, style = MaterialTheme.typography.titleSmall, color = theme.textPrimary, modifier = Modifier.weight(1f))
        if (trailingValue.isNotBlank()) {
            Text(trailingValue, style = MaterialTheme.typography.bodySmall, color = theme.accent)
            Spacer(Modifier.width(4.dp))
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = theme.textSecondary)
    }
}
