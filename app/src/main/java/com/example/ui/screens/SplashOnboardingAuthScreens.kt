package com.example.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import com.example.R
import com.example.data.LockMode
import com.example.firebase.attemptAutoSignIn
import com.example.firebase.onGoogleSignInClicked
import com.example.security.authenticateWithBiometrics
import com.example.security.isBiometricSupported
import com.example.ui.components.GlowingGlassCard
import com.example.ui.components.MidnightSparkleBackground
import com.example.ui.components.NeonGradientButton
import com.example.ui.theme.LocalDiaryThemeSpec
import com.example.ui.theme.PacificoFontFamily
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SplashScreen(onFinished: () -> Unit) {
    val theme = LocalDiaryThemeSpec.current
    val infinite = rememberInfiniteTransition(label = "splash_pulse")
    val pulse by infinite.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    LaunchedEffect(Unit) {
        delay(1700)
        onFinished()
    }

    MidnightSparkleBackground(animationsEnabled = true) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(Modifier.height(12.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "🌙  My Diary  💗",
                    style = MaterialTheme.typography.displayMedium.copy(
                        fontFamily = PacificoFontFamily,
                        color = Color.White
                    ),
                    modifier = Modifier.scale(pulse),
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Write • Feel • Heal • Remember Forever",
                    style = MaterialTheme.typography.bodyMedium,
                    color = theme.accent,
                    textAlign = TextAlign.Center
                )
            }

            Box(
                modifier = Modifier
                    .widthIn(max = 320.dp)
                    .fillMaxWidth(0.84f)
                    .aspectRatio(0.82f)
                    .clip(RoundedCornerShape(32.dp))
                    .border(
                        width = 2.dp,
                        brush = Brush.linearGradient(listOf(theme.primary, theme.accent, theme.secondary)),
                        shape = RoundedCornerShape(32.dp)
                    )
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_hero_anime_cat_1795152811196),
                    contentDescription = "Cute anime girl hugging white cat under starry night sky",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .width(180.dp)
                        .height(6.dp)
                        .clip(RoundedCornerShape(50)),
                    color = theme.primary,
                    trackColor = theme.cardSurface
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "Opening your secret space... ✨",
                    style = MaterialTheme.typography.bodySmall,
                    color = theme.textSecondary
                )
            }
        }
    }
}

private data class OnboardingSlide(
    val title: String,
    val subtitle: String,
    val badge: String,
    val imageRes: Int
)

@Composable
fun OnboardingScreen(onComplete: () -> Unit) {
    val theme = LocalDiaryThemeSpec.current
    val slides = remember {
        listOf(
            OnboardingSlide(
                title = "Your Little Secret World",
                subtitle = "Write everything in your heart inside a private, glowing anime journal designed just for you.",
                badge = "💗 Private & Safe",
                imageRes = R.drawable.img_hero_anime_cat_1795152811196
            ),
            OnboardingSlide(
                title = "Track Your Feelings",
                subtitle = "Understand your emotions every day with mood check-ins, dreamy night charts, and gentle insights.",
                badge = "✨ Mood & Dreams",
                imageRes = R.drawable.img_dream_castle_1795152811121
            ),
            OnboardingSlide(
                title = "Your Memories Forever",
                subtitle = "Keep photos, voice notes, and heartfelt entries synced privately to your personal cloud.",
                badge = "👑 Always With You",
                imageRes = R.drawable.img_cat_mascot_1795152809776
            )
        )
    }

    val pagerState = rememberPagerState(pageCount = { slides.size })
    val scope = rememberCoroutineScope()

    MidnightSparkleBackground(animationsEnabled = true) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    onClick = onComplete,
                    modifier = Modifier.testTag("onboarding_skip_button")
                ) {
                    Text("Skip", color = theme.accent, fontWeight = FontWeight.Bold)
                }
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) { page ->
                val slide = slides[page]
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = theme.primary.copy(alpha = 0.22f),
                        border = BorderStroke(1.dp, theme.borderGlow)
                    ) {
                        Text(
                            text = slide.badge,
                            style = MaterialTheme.typography.labelMedium,
                            color = theme.accent,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                        )
                    }
                    Spacer(Modifier.height(18.dp))
                    Box(
                        modifier = Modifier
                            .widthIn(max = 310.dp)
                            .fillMaxWidth(0.86f)
                            .aspectRatio(0.95f)
                            .clip(RoundedCornerShape(30.dp))
                            .border(
                                2.dp,
                                Brush.linearGradient(listOf(theme.primary, theme.secondary, theme.accent)),
                                RoundedCornerShape(30.dp)
                            )
                    ) {
                        Image(
                            painter = painterResource(id = slide.imageRes),
                            contentDescription = slide.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    Spacer(Modifier.height(24.dp))
                    Text(
                        text = slide.title,
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontFamily = PacificoFontFamily,
                            color = theme.textPrimary
                        ),
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = slide.subtitle,
                        style = MaterialTheme.typography.bodyLarge,
                        color = theme.textSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = 16.dp)
            ) {
                repeat(slides.size) { index ->
                    val active = pagerState.currentPage == index
                    Box(
                        modifier = Modifier
                            .height(8.dp)
                            .width(if (active) 28.dp else 8.dp)
                            .clip(CircleShape)
                            .background(if (active) theme.primary else theme.accent.copy(alpha = 0.35f))
                    )
                }
            }

            val isLast = pagerState.currentPage == slides.lastIndex
            NeonGradientButton(
                text = if (isLast) "Let's Start" else "Next",
                trailingEmoji = if (isLast) "💗" else "✨",
                testTagName = if (isLast) "onboarding_get_started_button" else "onboarding_next_button",
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 420.dp),
                onClick = {
                    if (isLast) {
                        onComplete()
                    } else {
                        scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                    }
                }
            )
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
fun AuthScreen(
    onAuthSuccess: () -> Unit
) {
    val theme = LocalDiaryThemeSpec.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Silent auto sign-in attempt on launch for returning Google users
    LaunchedEffect(Unit) {
        attemptAutoSignIn(
            context = context,
            credentialManager = credentialManager,
            onAuthSuccess = onAuthSuccess,
            onUnauthenticated = {},
            scope = scope
        )
    }

    MidnightSparkleBackground(animationsEnabled = true) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "My Diary 💗",
                style = MaterialTheme.typography.displayLarge.copy(
                    fontFamily = PacificoFontFamily,
                    color = theme.textPrimary
                ),
                textAlign = TextAlign.Center
            )
            Text(
                text = "Secret Feelings • Write • Feel • Heal",
                style = MaterialTheme.typography.bodyMedium,
                color = theme.accent,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(20.dp))

            Box(
                modifier = Modifier
                    .widthIn(max = 280.dp)
                    .fillMaxWidth(0.76f)
                    .aspectRatio(0.85f)
                    .clip(RoundedCornerShape(30.dp))
                    .border(
                        2.dp,
                        Brush.linearGradient(listOf(theme.primary, theme.accent, theme.secondary)),
                        RoundedCornerShape(30.dp)
                    )
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_hero_anime_cat_1795152811196),
                    contentDescription = "My Diary secret companion illustration",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(Modifier.height(22.dp))

            GlowingGlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 460.dp),
                contentPadding = PaddingValues(20.dp)
            ) {
                Text(
                    text = "Welcome to Your Secret Space",
                    style = MaterialTheme.typography.titleLarge,
                    color = theme.textPrimary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Sign in securely with Google so your private diary entries, dreams, moods, and memories stay isolated exclusively to your account.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = theme.textSecondary,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(18.dp))

                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = theme.primary, modifier = Modifier.size(30.dp))
                    }
                } else {
                    NeonGradientButton(
                        text = "Sign in with Google",
                        leadingEmoji = "👑",
                        trailingEmoji = "💗",
                        testTagName = "google_sign_in_button",
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            errorMessage = null
                            isLoading = true
                            onGoogleSignInClicked(
                                context = context,
                                credentialManager = credentialManager,
                                onAuthSuccess = {
                                    isLoading = false
                                    onAuthSuccess()
                                },
                                onAuthError = { msg ->
                                    isLoading = false
                                    errorMessage = msg
                                },
                                scope = scope,
                                onAuthCancelled = {
                                    isLoading = false
                                }
                            )
                        }
                    )
                }

                if (errorMessage != null) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = errorMessage!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
fun AppLockScreen(
    lockMode: LockMode,
    biometricEnabled: Boolean,
    title: String = "Unlock My Diary",
    onVerify: (String) -> Boolean,
    onUnlocked: () -> Unit
) {
    val theme = LocalDiaryThemeSpec.current
    val context = LocalContext.current
    var input by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(biometricEnabled) {
        if (biometricEnabled && isBiometricSupported()) {
            authenticateWithBiometrics(
                context = context,
                title = title,
                onSuccess = onUnlocked,
                onError = { /* user can fall back to PIN/Password */ }
            )
        }
    }

    MidnightSparkleBackground(animationsEnabled = true) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                shape = CircleShape,
                color = theme.primary.copy(alpha = 0.25f),
                border = BorderStroke(2.dp, theme.borderGlow),
                modifier = Modifier.size(80.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Diary locked",
                        tint = Color.White,
                        modifier = Modifier.size(38.dp)
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.headlineLarge.copy(fontFamily = PacificoFontFamily),
                color = theme.textPrimary
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = if (lockMode == LockMode.PIN) "Enter your secret 4-digit PIN" else "Enter your diary password",
                style = MaterialTheme.typography.bodyMedium,
                color = theme.textSecondary
            )
            Spacer(Modifier.height(22.dp))

            GlowingGlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 420.dp),
                contentPadding = PaddingValues(20.dp)
            ) {
                if (lockMode == LockMode.PIN) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        repeat(4) { i ->
                            val filled = i < input.length
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 10.dp)
                                    .size(18.dp)
                                    .clip(CircleShape)
                                    .background(if (filled) theme.primary else theme.cardSurfaceAlt)
                                    .border(1.5.dp, theme.borderGlow, CircleShape)
                            )
                        }
                    }
                    Spacer(Modifier.height(20.dp))
                    val rows = listOf(
                        listOf("1", "2", "3"),
                        listOf("4", "5", "6"),
                        listOf("7", "8", "9"),
                        listOf("C", "0", "⌫")
                    )
                    rows.forEach { row ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            row.forEach { key ->
                                Surface(
                                    shape = CircleShape,
                                    color = theme.cardSurfaceAlt,
                                    border = BorderStroke(1.dp, theme.borderGlow.copy(alpha = 0.6f)),
                                    modifier = Modifier
                                        .size(64.dp)
                                        .testTag("pin_key_$key")
                                        .clickable {
                                            errorMsg = null
                                            when (key) {
                                                "C" -> input = ""
                                                "⌫" -> if (input.isNotEmpty()) input = input.dropLast(1)
                                                else -> if (input.length < 4) {
                                                    input += key
                                                    if (input.length == 4) {
                                                        if (onVerify(input)) {
                                                            onUnlocked()
                                                        } else {
                                                            errorMsg = "Incorrect PIN. Please try again."
                                                            input = ""
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = key,
                                            style = MaterialTheme.typography.titleLarge,
                                            color = theme.textPrimary
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    OutlinedTextField(
                        value = input,
                        onValueChange = { input = it; errorMsg = null },
                        label = { Text("Secret Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = theme.primary,
                            unfocusedBorderColor = theme.borderGlow.copy(alpha = 0.5f),
                            focusedTextColor = theme.textPrimary,
                            unfocusedTextColor = theme.textPrimary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("lock_password_input")
                    )
                    Spacer(Modifier.height(16.dp))
                    NeonGradientButton(
                        text = "Unlock",
                        trailingEmoji = "🔓",
                        testTagName = "unlock_password_button",
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            if (onVerify(input)) onUnlocked()
                            else errorMsg = "Incorrect password. Please try again."
                        }
                    )
                }

                if (errorMsg != null) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = errorMsg!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (biometricEnabled && isBiometricSupported()) {
                    Spacer(Modifier.height(14.dp))
                    OutlinedButton(
                        onClick = {
                            authenticateWithBiometrics(
                                context = context,
                                title = title,
                                onSuccess = onUnlocked,
                                onError = { msg -> if (msg != "Cancelled") errorMsg = msg }
                            )
                        },
                        border = BorderStroke(1.dp, theme.borderGlow),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("biometric_unlock_button")
                    ) {
                        Icon(Icons.Default.Fingerprint, contentDescription = "Fingerprint unlock", tint = theme.accent)
                        Spacer(Modifier.width(8.dp))
                        Text("Unlock with Fingerprint", color = theme.textPrimary)
                    }
                }
            }
        }
    }
}
