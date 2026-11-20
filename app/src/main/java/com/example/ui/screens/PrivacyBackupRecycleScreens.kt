package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AppPreferences
import com.example.data.BackupRecord
import com.example.data.DefaultDiaryCategories
import com.example.data.DiaryCategory
import com.example.data.DiaryEntry
import com.example.data.DreamEntry
import com.example.data.LockMode
import com.example.data.PhotoMemory
import com.example.data.VoiceNote
import com.example.data.dateKeyToPretty
import com.example.security.isBiometricSupported
import com.example.ui.components.AestheticTopBar
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.GlowingGlassCard
import com.example.ui.components.NeonGradientButton
import com.example.ui.theme.LocalDiaryThemeSpec
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val THIRTY_DAYS_MS = 30L * 24L * 60L * 60L * 1000L

@Composable
fun PrivacySecurityScreen(
    preferences: AppPreferences,
    onSetAppLock: (LockMode, String) -> Unit,
    onDisableAppLock: () -> Unit,
    onSetBiometric: (Boolean) -> Unit,
    onSetAutoLockMinutes: (Int) -> Unit,
    onSetSecretMode: (Boolean) -> Unit,
    onOpenBackupRestore: () -> Unit,
    onClearAllData: () -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    val theme = LocalDiaryThemeSpec.current
    var showSetLockDialog by remember { mutableStateOf(false) }
    var showAutoLockDialog by remember { mutableStateOf(false) }
    var showClearConfirm by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        AestheticTopBar(title = "Privacy & Security ♡", onBack = onBack)

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 48.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                PrivacyToggleCard(
                    icon = Icons.Default.Lock,
                    iconTint = theme.primary,
                    title = "App Lock",
                    subtitle = if (preferences.appLockEnabled) {
                        "Active (${preferences.lockMode.name}) • Tap to change"
                    } else {
                        "Use PIN or Password to protect your diary"
                    },
                    checked = preferences.appLockEnabled,
                    testTagName = "privacy_app_lock_switch",
                    onCardClick = { showSetLockDialog = true },
                    onCheckedChange = { enabled ->
                        if (enabled) showSetLockDialog = true else onDisableAppLock()
                    }
                )
            }

            if (preferences.appLockEnabled && isBiometricSupported()) {
                item {
                    PrivacyToggleCard(
                        icon = Icons.Default.Fingerprint,
                        iconTint = theme.secondary,
                        title = "Fingerprint / Biometric Unlock",
                        subtitle = "Unlock your diary faster with device biometrics",
                        checked = preferences.biometricEnabled,
                        testTagName = "privacy_biometric_switch",
                        onCheckedChange = onSetBiometric
                    )
                }
            }

            if (preferences.appLockEnabled) {
                item {
                    PrivacyNavCard(
                        icon = Icons.Default.Timer,
                        iconTint = theme.accent,
                        title = "Auto-Lock Timer",
                        subtitle = "Lock automatically after ${preferences.autoLockMinutes} min in background",
                        testTagName = "privacy_autolock_button",
                        onClick = { showAutoLockDialog = true }
                    )
                }
            }

            item {
                PrivacyToggleCard(
                    icon = Icons.Default.VisibilityOff,
                    iconTint = Color(0xFFE879F9),
                    title = "Secret Mode",
                    subtitle = "Hide private-locked entries from dashboard, calendar & search",
                    checked = preferences.secretModeEnabled,
                    testTagName = "privacy_secret_mode_switch",
                    onCheckedChange = onSetSecretMode
                )
            }

            item {
                PrivacyNavCard(
                    icon = Icons.Default.CloudUpload,
                    iconTint = Color(0xFF38BDF8),
                    title = "Backup & Restore",
                    subtitle = "Sync to private Cloud Firestore & restore JSON backups",
                    testTagName = "privacy_backup_nav",
                    onClick = onOpenBackupRestore
                )
            }

            item {
                PrivacyNavCard(
                    icon = Icons.Default.Description,
                    iconTint = Color(0xFFA855F7),
                    title = "Export Diary",
                    subtitle = "Export your diary as PDF, TXT, or JSON anytime",
                    testTagName = "privacy_export_nav",
                    onClick = onOpenBackupRestore
                )
            }

            item {
                PrivacyNavCard(
                    icon = Icons.Default.DeleteForever,
                    iconTint = MaterialTheme.colorScheme.error,
                    title = "Clear All Data",
                    subtitle = "Permanently erase all diary entries, dreams, moods & memories",
                    testTagName = "privacy_clear_all_button",
                    onClick = { showClearConfirm = true }
                )
            }

            item {
                Spacer(Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(50))
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    theme.secondary.copy(alpha = 0.35f),
                                    theme.primary.copy(alpha = 0.35f)
                                )
                            )
                        )
                        .padding(vertical = 12.dp, horizontal = 18.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = "Protected",
                            tint = theme.accent,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Your thoughts are safe with us ♡",
                            style = MaterialTheme.typography.labelLarge,
                            color = theme.textPrimary
                        )
                    }
                }
            }
        }
    }

    if (showSetLockDialog) {
        SetupAppLockDialog(
            initialMode = if (preferences.lockMode == LockMode.NONE) LockMode.PIN else preferences.lockMode,
            onDismiss = { showSetLockDialog = false },
            onSave = { mode, secret ->
                onSetAppLock(mode, secret)
                showSetLockDialog = false
            }
        )
    }

    if (showAutoLockDialog) {
        val options = listOf(1, 5, 15, 30)
        AlertDialog(
            onDismissRequest = { showAutoLockDialog = false },
            containerColor = theme.cardSurfaceAlt,
            title = { Text("Auto-Lock Timeout", color = theme.textPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    options.forEach { mins ->
                        val selected = preferences.autoLockMinutes == mins
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (selected) theme.primary.copy(alpha = 0.25f) else theme.cardSurface,
                            border = BorderStroke(
                                1.dp,
                                if (selected) theme.primary else theme.borderGlow.copy(alpha = 0.35f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSetAutoLockMinutes(mins)
                                    showAutoLockDialog = false
                                }
                        ) {
                            Text(
                                text = "$mins minute${if (mins > 1) "s" else ""}",
                                color = theme.textPrimary,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAutoLockDialog = false }) {
                    Text("Close", color = theme.accent)
                }
            }
        )
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            containerColor = theme.cardSurfaceAlt,
            title = { Text("Clear All Diary Data?", color = theme.textPrimary) },
            text = {
                Text(
                    "This permanently deletes all diary entries, dreams, mood logs, voice notes, memories, and custom categories from your cloud diary. This action cannot be undone.",
                    color = theme.textSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showClearConfirm = false
                        onClearAllData()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete Everything")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) {
                    Text("Cancel", color = theme.accent)
                }
            }
        )
    }
}

@Composable
private fun SetupAppLockDialog(
    initialMode: LockMode,
    onDismiss: () -> Unit,
    onSave: (LockMode, String) -> Unit
) {
    val theme = LocalDiaryThemeSpec.current
    var mode by remember { mutableStateOf(initialMode) }
    var secret by remember { mutableStateOf("") }
    var confirmSecret by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = theme.cardSurfaceAlt,
        title = { Text("Configure App Lock 🔐", color = theme.textPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = mode == LockMode.PIN,
                        onClick = { mode = LockMode.PIN; secret = ""; confirmSecret = ""; error = null },
                        label = { Text("4-Digit PIN") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = theme.primary,
                            selectedLabelColor = Color.White
                        )
                    )
                    FilterChip(
                        selected = mode == LockMode.PASSWORD,
                        onClick = { mode = LockMode.PASSWORD; secret = ""; confirmSecret = ""; error = null },
                        label = { Text("Password") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = theme.primary,
                            selectedLabelColor = Color.White
                        )
                    )
                }
                OutlinedTextField(
                    value = secret,
                    onValueChange = { raw ->
                        secret = if (mode == LockMode.PIN) raw.filter { it.isDigit() }.take(4) else raw
                        error = null
                    },
                    label = { Text(if (mode == LockMode.PIN) "Enter 4-digit PIN" else "Enter Password (min 4 chars)") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = theme.primary,
                        unfocusedBorderColor = theme.borderGlow.copy(alpha = 0.5f),
                        focusedTextColor = theme.textPrimary,
                        unfocusedTextColor = theme.textPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = confirmSecret,
                    onValueChange = { raw ->
                        confirmSecret = if (mode == LockMode.PIN) raw.filter { it.isDigit() }.take(4) else raw
                        error = null
                    },
                    label = { Text("Confirm ${if (mode == LockMode.PIN) "PIN" else "Password"}") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = theme.primary,
                        unfocusedBorderColor = theme.borderGlow.copy(alpha = 0.5f),
                        focusedTextColor = theme.textPrimary,
                        unfocusedTextColor = theme.textPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                if (error != null) {
                    Text(error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val valid = when (mode) {
                        LockMode.PIN -> secret.length == 4 && secret.all { it.isDigit() }
                        LockMode.PASSWORD -> secret.length >= 4
                        LockMode.NONE -> false
                    }
                    if (!valid) {
                        error = if (mode == LockMode.PIN) "PIN must be exactly 4 digits." else "Password must be at least 4 characters."
                    } else if (secret != confirmSecret) {
                        error = "Entries do not match."
                    } else {
                        onSave(mode, secret)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = theme.primary)
            ) {
                Text("Save Lock")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = theme.accent)
            }
        }
    )
}

@Composable
private fun PrivacyToggleCard(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    checked: Boolean,
    testTagName: String,
    onCardClick: (() -> Unit)? = null,
    onCheckedChange: (Boolean) -> Unit
) {
    val theme = LocalDiaryThemeSpec.current
    GlowingGlassCard(
        cornerRadius = 22.dp,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        onClick = onCardClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(iconTint.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = title, tint = iconTint, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, color = theme.textPrimary)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = theme.textSecondary)
            }
            Spacer(Modifier.width(8.dp))
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                modifier = Modifier.testTag(testTagName),
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = theme.primary,
                    uncheckedTrackColor = theme.cardSurfaceAlt
                )
            )
        }
    }
}

@Composable
private fun PrivacyNavCard(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    testTagName: String,
    onClick: () -> Unit
) {
    val theme = LocalDiaryThemeSpec.current
    GlowingGlassCard(
        cornerRadius = 22.dp,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        modifier = Modifier.testTag(testTagName),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(iconTint.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = title, tint = iconTint, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, color = theme.textPrimary)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = theme.textSecondary)
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Open $title",
                tint = theme.accent
            )
        }
    }
}

@Composable
fun BackupRestoreScreen(
    preferences: AppPreferences,
    customCategories: List<DiaryCategory>,
    backupHistory: List<BackupRecord>,
    totalEntries: Int,
    totalDreams: Int,
    onToggleAutoBackup: (Boolean) -> Unit,
    onCloudSnapshot: () -> Unit,
    onExportToUri: (Uri, String, String?) -> Unit,
    onImportJsonUri: (Uri) -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    val theme = LocalDiaryThemeSpec.current
    var selectedFormat by remember { mutableStateOf("PDF") }
    var selectedCategoryId by remember { mutableStateOf<String?>(null) }

    val allCategories = remember(customCategories) {
        DefaultDiaryCategories.builtIn.map { Triple(it.id, it.emoji, it.name) } +
            customCategories.map { Triple(it.id, it.iconKey, it.name) }
    }

    val pdfExporter = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri -> if (uri != null) onExportToUri(uri, "PDF", selectedCategoryId) }

    val txtExporter = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/plain")
    ) { uri -> if (uri != null) onExportToUri(uri, "TXT", selectedCategoryId) }

    val jsonExporter = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri -> if (uri != null) onExportToUri(uri, "JSON", selectedCategoryId) }

    val jsonImporter = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> if (uri != null) onImportJsonUri(uri) }

    val timeFmt = remember { SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()) }

    Column(modifier = Modifier.fillMaxSize()) {
        AestheticTopBar(title = "Backup & Export ☁️", onBack = onBack)

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 48.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                GlowingGlassCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = "Cloud sync active",
                            tint = Color(0xFF34D399),
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Private Cloud Sync Active",
                                style = MaterialTheme.typography.titleMedium,
                                color = theme.textPrimary
                            )
                            Text(
                                text = "$totalEntries entries & $totalDreams dreams securely synced to your private account",
                                style = MaterialTheme.typography.bodySmall,
                                color = theme.textSecondary
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Automatic Cloud Backup", color = theme.textPrimary, style = MaterialTheme.typography.bodyMedium)
                        Switch(
                            checked = preferences.autoCloudBackupEnabled,
                            onCheckedChange = onToggleAutoBackup,
                            colors = SwitchDefaults.colors(checkedTrackColor = theme.primary)
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    NeonGradientButton(
                        text = "Sync Cloud Snapshot",
                        trailingEmoji = "☁️",
                        testTagName = "backup_cloud_snapshot_button",
                        modifier = Modifier.fillMaxWidth(),
                        onClick = onCloudSnapshot
                    )
                }
            }

            item {
                GlowingGlassCard {
                    Text(
                        text = "Export Your Diary ✨",
                        style = MaterialTheme.typography.titleMedium,
                        color = theme.textPrimary
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Choose format & optional diary category to export to your device.",
                        style = MaterialTheme.typography.bodySmall,
                        color = theme.textSecondary
                    )
                    Spacer(Modifier.height(12.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("PDF", "TXT", "JSON").forEach { fmt ->
                            FilterChip(
                                selected = selectedFormat == fmt,
                                onClick = { selectedFormat = fmt },
                                label = { Text(fmt, fontWeight = FontWeight.Bold) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = theme.primary,
                                    selectedLabelColor = Color.White,
                                    labelColor = theme.textSecondary
                                )
                            )
                        }
                    }

                    Spacer(Modifier.height(10.dp))
                    Text("Filter by Category", style = MaterialTheme.typography.labelMedium, color = theme.accent)
                    Spacer(Modifier.height(6.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            FilterChip(
                                selected = selectedCategoryId == null,
                                onClick = { selectedCategoryId = null },
                                label = { Text("All Entries") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = theme.secondary,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                        items(allCategories) { (catId, catEmoji, catName) ->
                            FilterChip(
                                selected = selectedCategoryId == catId,
                                onClick = { selectedCategoryId = catId },
                                label = { Text("$catEmoji $catName") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = theme.secondary,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }

                    Spacer(Modifier.height(14.dp))
                    val stamp = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        NeonGradientButton(
                            text = "Export $selectedFormat",
                            trailingEmoji = "📄",
                            testTagName = "export_diary_button",
                            modifier = Modifier.weight(1f),
                            onClick = {
                                when (selectedFormat) {
                                    "PDF" -> pdfExporter.launch("MyDiary_$stamp.pdf")
                                    "TXT" -> txtExporter.launch("MyDiary_$stamp.txt")
                                    else -> jsonExporter.launch("MyDiary_Backup_$stamp.json")
                                }
                            }
                        )
                        OutlinedButton(
                            onClick = { jsonImporter.launch(arrayOf("application/json", "text/plain", "*/*")) },
                            border = BorderStroke(1.2.dp, theme.borderGlow),
                            shape = RoundedCornerShape(50),
                            modifier = Modifier
                                .height(52.dp)
                                .testTag("import_json_button")
                        ) {
                            Icon(Icons.Default.UploadFile, contentDescription = "Import JSON", tint = theme.accent)
                            Spacer(Modifier.width(6.dp))
                            Text("Import", color = theme.textPrimary)
                        }
                    }
                }
            }

            item {
                Text(
                    text = "Backup & Export History",
                    style = MaterialTheme.typography.titleMedium,
                    color = theme.textPrimary
                )
            }

            if (backupHistory.isEmpty()) {
                item {
                    EmptyStateCard(
                        emoji = "☁️",
                        title = "No Backup Snapshots Yet",
                        subtitle = "Create a cloud snapshot or export a PDF/JSON copy anytime."
                    )
                }
            } else {
                items(backupHistory, key = { it.id }) { rec ->
                    GlowingGlassCard(cornerRadius = 18.dp, contentPadding = PaddingValues(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (rec.format == "CLOUD") Icons.Default.CloudUpload else Icons.Default.Download,
                                contentDescription = rec.format,
                                tint = theme.accent
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(rec.label, style = MaterialTheme.typography.titleSmall, color = theme.textPrimary)
                                val dateText = rec.createdAt?.toDate()?.let { timeFmt.format(it) } ?: "Just now"
                                Text(
                                    text = "$dateText • ${rec.entryCount} entries • ${rec.dreamCount} dreams",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = theme.textSecondary
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = theme.primary.copy(alpha = 0.25f)
                            ) {
                                Text(
                                    text = rec.format,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = theme.accent,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RecycleBinScreen(
    preferences: AppPreferences,
    deletedEntries: List<DiaryEntry>,
    deletedDreams: List<DreamEntry>,
    deletedMemories: List<PhotoMemory>,
    deletedVoiceNotes: List<VoiceNote>,
    onToggleAutoCleanup: (Boolean) -> Unit,
    onRestoreEntry: (String) -> Unit,
    onPermanentDeleteEntry: (String) -> Unit,
    onRestoreDream: (String) -> Unit,
    onPermanentDeleteDream: (String) -> Unit,
    onRestoreMemory: (String) -> Unit,
    onPermanentDeleteMemory: (String) -> Unit,
    onRestoreVoice: (String) -> Unit,
    onPermanentDeleteVoice: (String) -> Unit,
    onEmptyBin: () -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    val theme = LocalDiaryThemeSpec.current
    var selectedTab by remember { mutableIntStateOf(0) }
    var showEmptyConfirm by remember { mutableStateOf(false) }

    val totalCount = deletedEntries.size + deletedDreams.size + deletedMemories.size + deletedVoiceNotes.size
    val tabs = listOf(
        "Entries (${deletedEntries.size})",
        "Dreams (${deletedDreams.size})",
        "Photos (${deletedMemories.size})",
        "Voice (${deletedVoiceNotes.size})"
    )

    fun daysRemaining(deletedAtMillis: Long): Int {
        if (deletedAtMillis <= 0L) return 30
        val elapsed = (System.currentTimeMillis() - deletedAtMillis).coerceAtLeast(0L)
        val leftMs = (THIRTY_DAYS_MS - elapsed).coerceAtLeast(0L)
        return (leftMs / (24L * 60L * 60L * 1000L)).toInt().coerceIn(0, 30)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        AestheticTopBar(
            title = "Recycle Bin 🗑️",
            onBack = onBack,
            actions = {
                if (totalCount > 0) {
                    TextButton(
                        onClick = { showEmptyConfirm = true },
                        modifier = Modifier.testTag("empty_recycle_bin_button")
                    ) {
                        Text("Empty", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                    }
                }
            }
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 48.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                GlowingGlassCard(cornerRadius = 20.dp, contentPadding = PaddingValues(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Auto-Cleanup After 30 Days", style = MaterialTheme.typography.titleSmall, color = theme.textPrimary)
                            Text(
                                "Recover accidentally deleted items anytime within 30 days.",
                                style = MaterialTheme.typography.bodySmall,
                                color = theme.textSecondary
                            )
                        }
                        Switch(
                            checked = preferences.autoCleanupRecycleBin,
                            onCheckedChange = onToggleAutoCleanup,
                            colors = SwitchDefaults.colors(checkedTrackColor = theme.primary)
                        )
                    }
                }
            }

            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(tabs.indices.toList()) { idx ->
                        FilterChip(
                            selected = selectedTab == idx,
                            onClick = { selectedTab = idx },
                            label = { Text(tabs[idx]) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = theme.primary,
                                selectedLabelColor = Color.White,
                                labelColor = theme.textSecondary
                            )
                        )
                    }
                }
            }

            when (selectedTab) {
                0 -> {
                    if (deletedEntries.isEmpty()) {
                        item {
                            EmptyStateCard(emoji = "✨", title = "No Deleted Entries", subtitle = "Your diary entries bin is sparkly clean!")
                        }
                    } else {
                        items(deletedEntries, key = { it.id }) { entry ->
                            RecycleItemCard(
                                emoji = entry.moodEnum.emoji,
                                title = entry.title.ifBlank { "Untitled Diary Entry" },
                                subtitle = "${dateKeyToPretty(entry.dateKey)} • ${daysRemaining(entry.deletedAtMillis)} days left",
                                onRestore = { onRestoreEntry(entry.id) },
                                onDeleteForever = { onPermanentDeleteEntry(entry.id) }
                            )
                        }
                    }
                }
                1 -> {
                    if (deletedDreams.isEmpty()) {
                        item {
                            EmptyStateCard(emoji = "🌙", title = "No Deleted Dreams", subtitle = "No dreams in Recycle Bin.")
                        }
                    } else {
                        items(deletedDreams, key = { it.id }) { dream ->
                            RecycleItemCard(
                                emoji = dream.categoryEnum.emoji,
                                title = dream.title.ifBlank { "Untitled Dream" },
                                subtitle = "${dateKeyToPretty(dream.dateKey)} • ${daysRemaining(dream.deletedAtMillis)} days left",
                                onRestore = { onRestoreDream(dream.id) },
                                onDeleteForever = { onPermanentDeleteDream(dream.id) }
                            )
                        }
                    }
                }
                2 -> {
                    if (deletedMemories.isEmpty()) {
                        item {
                            EmptyStateCard(emoji = "📸", title = "No Deleted Photos", subtitle = "No photo or video memories in Recycle Bin.")
                        }
                    } else {
                        items(deletedMemories, key = { it.id }) { mem ->
                            RecycleItemCard(
                                emoji = if (mem.mediaType == "VIDEO") "🎬" else "🖼️",
                                title = mem.caption.ifBlank { mem.albumName },
                                subtitle = "${mem.albumName} • ${daysRemaining(mem.deletedAtMillis)} days left",
                                onRestore = { onRestoreMemory(mem.id) },
                                onDeleteForever = { onPermanentDeleteMemory(mem.id) }
                            )
                        }
                    }
                }
                else -> {
                    if (deletedVoiceNotes.isEmpty()) {
                        item {
                            EmptyStateCard(emoji = "🎙️", title = "No Deleted Voice Notes", subtitle = "No voice diaries in Recycle Bin.")
                        }
                    } else {
                        items(deletedVoiceNotes, key = { it.id }) { voice ->
                            RecycleItemCard(
                                emoji = "🎙️",
                                title = voice.title.ifBlank { "Voice Note" },
                                subtitle = "${voice.durationSeconds}s • ${daysRemaining(voice.deletedAtMillis)} days left",
                                onRestore = { onRestoreVoice(voice.id) },
                                onDeleteForever = { onPermanentDeleteVoice(voice.id) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showEmptyConfirm) {
        AlertDialog(
            onDismissRequest = { showEmptyConfirm = false },
            containerColor = theme.cardSurfaceAlt,
            title = { Text("Empty Recycle Bin?", color = theme.textPrimary) },
            text = {
                Text(
                    "Permanently delete all $totalCount item(s) in the Recycle Bin? They cannot be recovered afterward.",
                    color = theme.textSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showEmptyConfirm = false
                        onEmptyBin()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Empty Bin")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEmptyConfirm = false }) {
                    Text("Cancel", color = theme.accent)
                }
            }
        )
    }
}

@Composable
private fun RecycleItemCard(
    emoji: String,
    title: String,
    subtitle: String,
    onRestore: () -> Unit,
    onDeleteForever: () -> Unit
) {
    val theme = LocalDiaryThemeSpec.current
    GlowingGlassCard(cornerRadius = 18.dp, contentPadding = PaddingValues(14.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(emoji, fontSize = 26.sp)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = theme.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = theme.textSecondary)
            }
            IconButton(onClick = onRestore, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Default.Restore, contentDescription = "Restore item", tint = Color(0xFF34D399))
            }
            IconButton(onClick = onDeleteForever, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Default.DeleteForever, contentDescription = "Delete permanently", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}
