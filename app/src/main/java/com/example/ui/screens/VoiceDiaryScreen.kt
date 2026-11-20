package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.MediaAndBackupManager
import com.example.data.VoiceNote
import com.example.data.dateKeyToPretty
import com.example.ui.components.AestheticTopBar
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.GlowingGlassCard
import com.example.ui.theme.LocalDiaryThemeSpec
import kotlinx.coroutines.delay

private enum class RecordingStatus { IDLE, RECORDING, PAUSED }

@Composable
fun VoiceDiaryScreen(
    voiceNotes: List<VoiceNote>,
    mediaManager: MediaAndBackupManager,
    onSaveVoiceNote: (title: String, description: String, filePath: String, durationSeconds: Long) -> Unit,
    onUpdateVoiceNote: (VoiceNote, String, String) -> Unit,
    onDeleteVoiceNote: (VoiceNote) -> Unit,
    onShowMessage: (String) -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    val theme = LocalDiaryThemeSpec.current
    val context = LocalContext.current

    var status by remember { mutableStateOf(RecordingStatus.IDLE) }
    var elapsedSeconds by remember { mutableLongStateOf(0L) }
    var playingNoteId by remember { mutableStateOf<String?>(null) }
    var playbackSpeed by remember { mutableFloatStateOf(1.0f) }

    var pendingSavePath by remember { mutableStateOf<String?>(null) }
    var pendingSaveDuration by remember { mutableLongStateOf(0L) }
    var editingNote by remember { mutableStateOf<VoiceNote?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            mediaManager.stopPlayback()
            if (status != RecordingStatus.IDLE) {
                mediaManager.cancelRecording()
            }
        }
    }

    LaunchedEffect(status) {
        while (status == RecordingStatus.RECORDING) {
            delay(1000)
            elapsedSeconds++
        }
    }

    val startRecordingActual: () -> Unit = {
        mediaManager.stopPlayback()
        playingNoteId = null
        val path = mediaManager.startRecording()
        if (path != null) {
            elapsedSeconds = 0L
            status = RecordingStatus.RECORDING
        } else {
            onShowMessage("Could not start microphone recording on this device.")
        }
    }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            startRecordingActual()
        } else {
            onShowMessage("Microphone permission is needed to record Voice Diary notes.")
        }
    }

    val infinite = rememberInfiniteTransition(label = "voice_wave")
    val wavePulse by infinite.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(600, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "wave"
    )

    Column(modifier = Modifier.fillMaxSize()) {
        AestheticTopBar(
            title = "Voice Diary 🎙️",
            onBack = onBack,
            actions = {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = theme.cardSurface,
                    border = BorderStroke(1.dp, theme.borderGlow),
                    modifier = Modifier
                        .padding(end = 10.dp)
                        .clickable {
                            val speeds = listOf(0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
                            val nextIdx = (speeds.indexOf(playbackSpeed) + 1) % speeds.size
                            playbackSpeed = speeds[nextIdx]
                            mediaManager.setPlaybackSpeed(playbackSpeed)
                        }
                        .testTag("voice_speed_button")
                ) {
                    Text(
                        text = "${playbackSpeed}x",
                        style = MaterialTheme.typography.labelMedium,
                        color = theme.accent,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Recorder Studio Card
            item {
                GlowingGlassCard(
                    cornerRadius = 26.dp,
                    contentPadding = PaddingValues(20.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = when (status) {
                                RecordingStatus.IDLE -> "Whisper Your Thoughts 💗"
                                RecordingStatus.RECORDING -> "Recording Live... ✨"
                                RecordingStatus.PAUSED -> "Recording Paused"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            color = theme.accent
                        )
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = formatDuration(elapsedSeconds),
                            style = MaterialTheme.typography.displayMedium,
                            color = theme.textPrimary
                        )
                        Spacer(Modifier.height(14.dp))

                        // Animated Sound Wave Bars
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.height(38.dp)
                        ) {
                            val baseHeights = listOf(12, 22, 32, 18, 36, 26, 16, 30, 20, 14)
                            baseHeights.forEachIndexed { idx, h ->
                                val activeH = if (status == RecordingStatus.RECORDING) {
                                    val factor = if (idx % 2 == 0) wavePulse else (1.3f - wavePulse).coerceIn(0.3f, 1f)
                                    (h * factor).dp
                                } else {
                                    8.dp
                                }
                                Box(
                                    modifier = Modifier
                                        .width(6.dp)
                                        .height(activeH)
                                        .clip(CircleShape)
                                        .background(
                                            Brush.verticalGradient(listOf(theme.primary, theme.secondary))
                                        )
                                )
                            }
                        }

                        Spacer(Modifier.height(18.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(18.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (status == RecordingStatus.IDLE) {
                                Box(
                                    modifier = Modifier
                                        .size(72.dp)
                                        .clip(CircleShape)
                                        .background(Brush.linearGradient(listOf(theme.primary, theme.secondary)))
                                        .border(2.dp, theme.accent, CircleShape)
                                        .clickable {
                                            val hasPerm = ContextCompat.checkSelfPermission(
                                                context,
                                                Manifest.permission.RECORD_AUDIO
                                            ) == PackageManager.PERMISSION_GRANTED
                                            if (hasPerm) startRecordingActual()
                                            else audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                        }
                                        .testTag("voice_record_button"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Mic, contentDescription = "Start recording", tint = Color.White, modifier = Modifier.size(34.dp))
                                }
                            } else {
                                // Pause / Resume
                                Surface(
                                    shape = CircleShape,
                                    color = theme.cardSurfaceAlt,
                                    border = BorderStroke(1.5.dp, theme.borderGlow),
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clickable {
                                            if (status == RecordingStatus.RECORDING) {
                                                if (mediaManager.pauseRecording()) status = RecordingStatus.PAUSED
                                            } else {
                                                if (mediaManager.resumeRecording()) status = RecordingStatus.RECORDING
                                            }
                                        }
                                        .testTag("voice_pause_resume_button")
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            if (status == RecordingStatus.RECORDING) Icons.Default.Pause else Icons.Default.PlayArrow,
                                            contentDescription = "Pause or resume recording",
                                            tint = Color.White
                                        )
                                    }
                                }

                                // Stop & Save
                                Box(
                                    modifier = Modifier
                                        .size(68.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFFF4D6D))
                                        .border(2.dp, Color.White, CircleShape)
                                        .clickable {
                                            val savedPath = mediaManager.stopRecording()
                                            val dur = elapsedSeconds.coerceAtLeast(1L)
                                            status = RecordingStatus.IDLE
                                            if (savedPath != null) {
                                                pendingSavePath = savedPath
                                                pendingSaveDuration = dur
                                            }
                                        }
                                        .testTag("voice_stop_button"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Stop, contentDescription = "Stop and save recording", tint = Color.White, modifier = Modifier.size(32.dp))
                                }
                            }
                        }
                    }
                }
            }

            item {
                Text("Saved Recordings (${voiceNotes.size})", style = MaterialTheme.typography.titleMedium, color = theme.textPrimary)
            }

            if (voiceNotes.isEmpty()) {
                item {
                    EmptyStateCard(
                        emoji = "🎙️💗",
                        title = "No Voice Notes Yet",
                        subtitle = "Tap the glowing microphone above to record secret voice memos and replay them anytime."
                    )
                }
            } else {
                items(voiceNotes, key = { it.id }) { note ->
                    val isPlaying = playingNoteId == note.id
                    GlowingGlassCard(cornerRadius = 20.dp, contentPadding = PaddingValues(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(if (isPlaying) theme.primary else theme.primary.copy(alpha = 0.25f))
                                    .border(1.5.dp, theme.borderGlow, CircleShape)
                                    .clickable {
                                        if (isPlaying) {
                                            mediaManager.stopPlayback()
                                            playingNoteId = null
                                        } else {
                                            val ok = mediaManager.playAudio(note.filePath, playbackSpeed) {
                                                playingNoteId = null
                                            }
                                            if (ok) playingNoteId = note.id
                                            else onShowMessage("Audio file is only stored on the recording device.")
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                                    contentDescription = if (isPlaying) "Stop playback" else "Play recording",
                                    tint = Color.White
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(note.title, style = MaterialTheme.typography.titleSmall, color = theme.textPrimary)
                                if (note.description.isNotBlank()) {
                                    Text(note.description, style = MaterialTheme.typography.bodySmall, color = theme.textSecondary)
                                }
                                Text(
                                    text = "${formatDuration(note.durationSeconds)} • ${dateKeyToPretty(note.dateKey)} • ${playbackSpeed}x",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = theme.accent
                                )
                            }
                            IconButton(onClick = { editingNote = note }) {
                                Icon(Icons.Default.Edit, contentDescription = "Rename recording", tint = theme.textSecondary)
                            }
                            IconButton(onClick = {
                                if (isPlaying) {
                                    mediaManager.stopPlayback()
                                    playingNoteId = null
                                }
                                onDeleteVoiceNote(note)
                            }) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = "Delete recording", tint = theme.textSecondary)
                            }
                        }
                    }
                }
            }
        }
    }

    if (pendingSavePath != null) {
        var title by remember { mutableStateOf("Voice Reflection") }
        var desc by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { pendingSavePath = null },
            containerColor = theme.backgroundBottom,
            title = { Text("Save Voice Diary 🎙️", color = theme.textPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Duration: ${formatDuration(pendingSaveDuration)}", color = theme.accent)
                    OutlinedTextField(value = title, onValueChange = { title = it.take(120) }, label = { Text("Recording Title") }, singleLine = true)
                    OutlinedTextField(value = desc, onValueChange = { desc = it.take(1000) }, label = { Text("Description (Optional)") }, maxLines = 3)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val path = pendingSavePath!!
                    onSaveVoiceNote(title.trim().ifBlank { "Voice Note" }, desc.trim(), path, pendingSaveDuration)
                    pendingSavePath = null
                    elapsedSeconds = 0L
                }) {
                    Text("Save 💗", color = theme.primary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingSavePath = null; elapsedSeconds = 0L }) { Text("Discard", color = theme.textSecondary) }
            }
        )
    }

    if (editingNote != null) {
        val current = editingNote!!
        var title by remember(current.id) { mutableStateOf(current.title) }
        var desc by remember(current.id) { mutableStateOf(current.description) }
        AlertDialog(
            onDismissRequest = { editingNote = null },
            containerColor = theme.backgroundBottom,
            title = { Text("Edit Voice Note ✨", color = theme.textPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(value = title, onValueChange = { title = it.take(120) }, label = { Text("Title") }, singleLine = true)
                    OutlinedTextField(value = desc, onValueChange = { desc = it.take(1000) }, label = { Text("Description") }, maxLines = 3)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    onUpdateVoiceNote(current, title, desc)
                    editingNote = null
                }) {
                    Text("Update", color = theme.primary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { editingNote = null }) { Text("Cancel", color = theme.textSecondary) }
            }
        )
    }
}

private fun formatDuration(seconds: Long): String {
    val mins = seconds / 60
    val secs = seconds % 60
    return String.format("%02d:%02d", mins, secs)
}
