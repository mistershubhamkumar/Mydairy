package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.DefaultDiaryCategories
import com.example.ui.theme.LocalDiaryThemeSpec
import com.example.ui.theme.PacificoFontFamily
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun MidnightSparkleBackground(
    animationsEnabled: Boolean = true,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val theme = LocalDiaryThemeSpec.current
    val infinite = rememberInfiniteTransition(label = "sparkle_bg")
    val phase by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 9000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )
    val twinkle by infinite.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "twinkle"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        theme.backgroundTop,
                        theme.backgroundBottom,
                        theme.backgroundTop
                    )
                )
            )
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            // Soft neon pink & lavender nebula glows
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(theme.primary.copy(alpha = 0.22f), Color.Transparent),
                    center = Offset(w * 0.15f, h * 0.12f),
                    radius = w * 0.65f
                ),
                radius = w * 0.65f,
                center = Offset(w * 0.15f, h * 0.12f)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(theme.secondary.copy(alpha = 0.20f), Color.Transparent),
                    center = Offset(w * 0.88f, h * 0.72f),
                    radius = w * 0.7f
                ),
                radius = w * 0.7f,
                center = Offset(w * 0.88f, h * 0.72f)
            )

            // Twinkling stars
            val starAlpha = if (animationsEnabled) twinkle else 0.65f
            val starCoords = listOf(
                0.12f to 0.08f, 0.84f to 0.11f, 0.31f to 0.19f, 0.72f to 0.24f,
                0.18f to 0.42f, 0.91f to 0.39f, 0.54f to 0.14f, 0.09f to 0.68f,
                0.79f to 0.62f, 0.43f to 0.82f, 0.88f to 0.88f, 0.24f to 0.91f
            )
            starCoords.forEachIndexed { idx, (rx, ry) ->
                val localAlpha = if (idx % 2 == 0) starAlpha else (1.25f - starAlpha).coerceIn(0.2f, 1f)
                val cx = w * rx
                val cy = h * ry
                val r = if (idx % 3 == 0) 4.5f else 2.6f
                drawCircle(
                    color = theme.accent.copy(alpha = localAlpha * 0.75f),
                    radius = r,
                    center = Offset(cx, cy)
                )
            }

            // Floating mini pink hearts
            if (animationsEnabled) {
                val heartOffsets = listOf(0.14f, 0.42f, 0.76f, 0.89f)
                heartOffsets.forEachIndexed { idx, rx ->
                    val driftY = ((1f - phase + idx * 0.25f) % 1f) * h
                    val driftX = w * rx + sin((phase * 2f * PI + idx).toDouble()).toFloat() * 18f
                    drawTinyHeart(
                        center = Offset(driftX, driftY),
                        size = 11f + (idx % 2) * 4f,
                        color = theme.primary.copy(alpha = 0.22f)
                    )
                }
            }
        }
        content()
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawTinyHeart(
    center: Offset,
    size: Float,
    color: Color
) {
    val path = Path().apply {
        moveTo(center.x, center.y + size * 0.35f)
        cubicTo(
            center.x - size * 0.6f, center.y - size * 0.1f,
            center.x - size * 0.35f, center.y - size * 0.65f,
            center.x, center.y - size * 0.25f
        )
        cubicTo(
            center.x + size * 0.35f, center.y - size * 0.65f,
            center.x + size * 0.6f, center.y - size * 0.1f,
            center.x, center.y + size * 0.35f
        )
        close()
    }
    drawPath(path, color = color)
}

@Composable
fun GlowingGlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 24.dp,
    borderWidth: Dp = 1.4.dp,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val theme = LocalDiaryThemeSpec.current
    val shape = RoundedCornerShape(cornerRadius)
    val borderBrush = Brush.linearGradient(
        colors = listOf(
            theme.borderGlow.copy(alpha = 0.9f),
            theme.secondary.copy(alpha = 0.65f),
            theme.accent.copy(alpha = 0.75f)
        )
    )
    val cardModifier = modifier
        .shadow(
            elevation = 10.dp,
            shape = shape,
            ambientColor = theme.primary.copy(alpha = 0.35f),
            spotColor = theme.primary.copy(alpha = 0.35f)
        )
        .clip(shape)
        .background(
            Brush.linearGradient(
                colors = listOf(
                    theme.cardSurface,
                    theme.cardSurfaceAlt.copy(alpha = 0.85f)
                )
            )
        )
        .border(width = borderWidth, brush = borderBrush, shape = shape)
        .let { m -> if (onClick != null) m.clickable(onClick = onClick) else m }

    Column(
        modifier = cardModifier.padding(contentPadding),
        content = content
    )
}

@Composable
fun NeonGradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingEmoji: String? = null,
    trailingEmoji: String? = null,
    testTagName: String? = null
) {
    val theme = LocalDiaryThemeSpec.current
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 500f),
        label = "btn_scale"
    )
    val shape = RoundedCornerShape(50)
    Box(
        modifier = modifier
            .then(if (testTagName != null) Modifier.testTag(testTagName) else Modifier)
            .scale(scale)
            .height(52.dp)
            .shadow(
                elevation = if (enabled) 12.dp else 2.dp,
                shape = shape,
                spotColor = theme.primary.copy(alpha = 0.65f),
                ambientColor = theme.primary.copy(alpha = 0.45f)
            )
            .clip(shape)
            .background(
                if (enabled) {
                    Brush.horizontalGradient(listOf(theme.primary, theme.secondary))
                } else {
                    Brush.horizontalGradient(
                        listOf(
                            theme.primary.copy(alpha = 0.4f),
                            theme.secondary.copy(alpha = 0.4f)
                        )
                    )
                }
            )
            .border(
                width = 1.5.dp,
                brush = Brush.horizontalGradient(listOf(theme.accent, Color.White.copy(alpha = 0.7f))),
                shape = shape
            )
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (leadingEmoji != null) {
                Text(text = leadingEmoji, fontSize = 18.sp)
                Spacer(Modifier.width(8.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium.copy(
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (trailingEmoji != null) {
                Spacer(Modifier.width(8.dp))
                Text(text = trailingEmoji, fontSize = 18.sp)
            }
        }
    }
}

@Composable
fun AestheticTopBar(
    title: String,
    onBack: (() -> Unit)? = null,
    useScriptFont: Boolean = true,
    actions: @Composable () -> Unit = {}
) {
    val theme = LocalDiaryThemeSpec.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (onBack != null) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("top_bar_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Navigate back",
                    tint = theme.textPrimary
                )
            }
        } else {
            Spacer(Modifier.width(8.dp))
        }
        Text(
            text = title,
            style = if (useScriptFont) {
                MaterialTheme.typography.headlineLarge.copy(
                    fontFamily = PacificoFontFamily,
                    color = theme.textPrimary
                )
            } else {
                MaterialTheme.typography.titleLarge.copy(color = theme.textPrimary)
            },
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        actions()
    }
}

@Composable
fun CuteCatMascotWidget(
    animationsEnabled: Boolean = true,
    customMessage: String? = null,
    modifier: Modifier = Modifier
) {
    val theme = LocalDiaryThemeSpec.current
    var msgIndex by remember { mutableIntStateOf(0) }
    val message = customMessage ?: DefaultDiaryCategories.cuteCatMessages[
        msgIndex % DefaultDiaryCategories.cuteCatMessages.size
    ]
    val infinite = rememberInfiniteTransition(label = "cat_bounce")
    val bounceY by infinite.animateFloat(
        initialValue = -3f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bounce_y"
    )

    GlowingGlassCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("cute_cat_widget"),
        cornerRadius = 24.dp,
        contentPadding = PaddingValues(14.dp),
        onClick = { msgIndex++ }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_cat_mascot_1795152809776),
                contentDescription = "Cute white cat mascot",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(64.dp)
                    .offset(y = if (animationsEnabled) bounceY.dp else 0.dp)
                    .clip(CircleShape)
                    .border(2.dp, theme.borderGlow, CircleShape)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Mochi Kitty 👑",
                    style = MaterialTheme.typography.labelMedium,
                    color = theme.accent
                )
                Spacer(Modifier.height(2.dp))
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = theme.primary.copy(alpha = 0.22f),
                    border = BorderStroke(1.dp, theme.borderGlow.copy(alpha = 0.6f))
                ) {
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = theme.textPrimary,
                            fontWeight = FontWeight.SemiBold
                        ),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Tap Mochi for a sweet whisper ✨",
                    style = MaterialTheme.typography.labelSmall,
                    color = theme.textSecondary.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
fun EmptyStateCard(
    emoji: String,
    title: String,
    subtitle: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val theme = LocalDiaryThemeSpec.current
    GlowingGlassCard(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = emoji, fontSize = 42.sp)
            Spacer(Modifier.height(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = theme.textPrimary,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = theme.textSecondary,
                textAlign = TextAlign.Center
            )
            if (actionLabel != null && onAction != null) {
                Spacer(Modifier.height(16.dp))
                NeonGradientButton(
                    text = actionLabel,
                    onClick = onAction,
                    trailingEmoji = "✨"
                )
            }
        }
    }
}

@Composable
fun StreakConfettiOverlay(
    visible: Boolean,
    streakDays: Int,
    onDismiss: () -> Unit
) {
    if (!visible) return
    val theme = LocalDiaryThemeSpec.current
    val infinite = rememberInfiniteTransition(label = "confetti")
    val progress by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2400, easing = LinearEasing)),
        label = "confetti_p"
    )
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.55f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val colors = listOf(
                Color(0xFFFF42C8), Color(0xFFA855F7), Color(0xFFFDE047),
                Color(0xFF38BDF8), Color(0xFF34D399), Color(0xFFFFB6E6)
            )
            for (i in 0 until 28) {
                val x = (size.width / 28f) * i + cos((progress * 6f + i).toDouble()).toFloat() * 24f
                val y = ((progress + i * 0.07f) % 1f) * size.height
                drawCircle(
                    color = colors[i % colors.size],
                    radius = 7f + (i % 4) * 2f,
                    center = Offset(x, y)
                )
            }
        }
        GlowingGlassCard(
            modifier = Modifier
                .padding(32.dp)
                .fillMaxWidth(),
            contentPadding = PaddingValues(24.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("🎉👑🔥", fontSize = 42.sp)
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "$streakDays-Day Writing Streak!",
                    style = MaterialTheme.typography.headlineMedium,
                    color = theme.textPrimary,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Your secret journal is glowing brighter every day! Keep expressing your heart.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = theme.textSecondary,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(16.dp))
                NeonGradientButton(
                    text = "Yay! Keep Writing",
                    trailingEmoji = "💗",
                    onClick = onDismiss
                )
            }
        }
    }
}
