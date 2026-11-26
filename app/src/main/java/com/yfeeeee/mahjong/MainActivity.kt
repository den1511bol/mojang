// Developer: @yfeeeee
package com.yfeeeee.mahjong

import android.content.Context
import android.os.Bundle
import android.view.HapticFeedbackConstants
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = 0xFF041C1A.toInt()
        window.navigationBarColor = 0xFF0A2A33.toInt()
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }
        setContent {
            MaterialTheme {
                GameRoot()
            }
        }
    }
}

@Composable
fun GameRoot() {
    val ctx = LocalContext.current
    val view = LocalView.current
    val prefs = remember { ctx.getSharedPreferences("mahjong", Context.MODE_PRIVATE) }
    val sfx = remember { Sfx().also { it.enabled = prefs.getBoolean("sound", true) } }

    var level by remember { mutableIntStateOf(prefs.getInt("level", 1)) }
    var attempt by remember { mutableIntStateOf(0) }
    var total by remember { mutableIntStateOf(prefs.getInt("score", 0)) }
    var best by remember { mutableIntStateOf(prefs.getInt("best", 0)) }
    var soundOn by remember { mutableStateOf(prefs.getBoolean("sound", true)) }

    val engine = remember(level, attempt) {
        Engine(level, attempt, total, sfx) { strong ->
            view.performHapticFeedback(
                if (strong) HapticFeedbackConstants.LONG_PRESS else HapticFeedbackConstants.KEYBOARD_TAP,
            )
        }
    }

    LaunchedEffect(engine) {
        while (true) {
            androidx.compose.runtime.withFrameNanos { ns -> engine.tick(ns / 1_000_000L) }
        }
    }
    LaunchedEffect(engine.won) {
        if (engine.won) {
            total = engine.score
            if (total > best) best = total
            prefs.edit().putInt("score", total).putInt("best", best).apply()
        }
    }

    val shownScore by animateIntAsState(engine.score, tween(700), label = "score")

    Box(Modifier.fillMaxSize()) {
        Canvas(
            Modifier
                .fillMaxSize()
                .pointerInput(engine) {
                    detectTapGestures { engine.tap(it.x, it.y) }
                },
        ) {
            if (engine.frame < 0L) return@Canvas
            engine.render(this, 150.dp.toPx(), 110.dp.toPx())
        }

        // ----- верхняя панель -----
        Column(
            Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Уровень $level",
                    modifier = Modifier.weight(1f),
                    style = TextStyle(
                        brush = Brush.verticalGradient(listOf(Color(0xFFFFF3C4), Color(0xFFFFB300))),
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Serif,
                    ),
                )
                RoundButton(if (soundOn) "🔊" else "🔇") {
                    soundOn = !soundOn
                    sfx.enabled = soundOn
                    prefs.edit().putBoolean("sound", soundOn).apply()
                    if (soundOn) sfx.play("select")
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Chip("Очки", shownScore.toString(), Modifier.weight(1.2f))
                Chip("Рекорд", maxOf(best, shownScore).toString(), Modifier.weight(1f))
                Chip("Плиток", engine.left.toString(), Modifier.weight(0.9f))
            }
            AnimatedVisibility(
                visible = engine.combo >= 2,
                enter = scaleIn(spring(0.45f, 500f)) + fadeIn(),
            ) {
                Box(Modifier.fillMaxWidth().padding(top = 8.dp), contentAlignment = Alignment.Center) {
                    Text(
                        "КОМБО ×${engine.combo}",
                        color = Color(0xFFFF9E40),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp,
                    )
                }
            }
        }

        // ----- нижняя панель -----
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(start = 14.dp, end = 14.dp, bottom = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val pulse by rememberInfiniteTransition(label = "stuck").animateFloat(
                initialValue = 0.55f, targetValue = 1f,
                animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "stuckA",
            )
            AnimatedVisibility(visible = engine.stuck && !engine.won) {
                Text(
                    "Ходов больше нет - нажмите «Перемешать»",
                    color = Color(0xFFFFAB91),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .padding(bottom = 10.dp)
                        .alpha(pulse),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                GlassButton("💡 Подсказка", Modifier.weight(1f)) { engine.hint() }
                GlassButton("🔀 Перемешать", Modifier.weight(1f)) { engine.shuffle() }
                GlassButton("↻ Заново", Modifier.weight(0.8f)) { attempt++ }
            }
            Spacer(Modifier.height(6.dp))
            Text("by @yfeeeee", fontSize = 11.sp, color = Color(0x889DB8B5))
        }

        // ----- победа -----
        AnimatedVisibility(visible = engine.won, enter = fadeIn(tween(350))) {
            Box(Modifier.fillMaxSize().background(Color(0x99000000)))
        }
        AnimatedVisibility(
            visible = engine.won,
            modifier = Modifier.align(Alignment.Center),
            enter = fadeIn(tween(300)) + scaleIn(spring(0.5f, 300f), initialScale = 0.5f),
        ) {
            WinCard(level, engine.score, engine.assists) {
                level++
                attempt = 0
                prefs.edit().putInt("level", level).apply()
            }
        }
    }
}

@Composable
fun WinCard(level: Int, score: Int, assists: Int, onNext: () -> Unit) {
    val stars = when {
        assists == 0 -> 3
        assists <= 2 -> 2
        else -> 1
    }
    val shape = RoundedCornerShape(30.dp)
    Column(
        modifier = Modifier
            .padding(24.dp)
            .clip(shape)
            .background(Brush.verticalGradient(listOf(Color(0xFF14504B), Color(0xFF082B2D))))
            .border(2.dp, Brush.verticalGradient(listOf(Color(0xFFFFE082), Color(0xFFB8860B))), shape)
            .padding(horizontal = 32.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            "Уровень $level пройден!",
            textAlign = TextAlign.Center,
            style = TextStyle(
                brush = Brush.verticalGradient(listOf(Color(0xFFFFF3C4), Color(0xFFFFB300))),
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Serif,
            ),
        )
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            for (i in 0 until 3) Star(filled = i < stars, delayMs = 350L + i * 260L)
        }
        Spacer(Modifier.height(12.dp))
        Text("Очки: $score", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(20.dp))
        GlassButton("Дальше ▶", Modifier.fillMaxWidth(), gold = true, onClick = onNext)
    }
}

@Composable
fun Star(filled: Boolean, delayMs: Long) {
    var on by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(delayMs)
        on = true
    }
    val s by animateFloatAsState(
        if (on) 1f else 0f,
        spring(dampingRatio = 0.35f, stiffness = Spring.StiffnessLow),
        label = "star",
    )
    Text(
        "★",
        fontSize = 52.sp,
        color = if (filled) Color(0xFFFFD54F) else Color(0x44FFFFFF),
        modifier = Modifier.scale(s),
    )
}

@Composable
fun Chip(label: String, value: String, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(14.dp)
    Column(
        modifier = modifier
            .clip(shape)
            .background(Brush.verticalGradient(listOf(Color(0x33FFFFFF), Color(0x12FFFFFF))))
            .border(1.dp, Color(0x33FFFFFF), shape)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Text(label, fontSize = 11.sp, color = Color(0xFFB7D3CF))
        Text(value, fontSize = 18.sp, color = Color.White, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun GlassButton(
    text: String,
    modifier: Modifier = Modifier,
    gold: Boolean = false,
    onClick: () -> Unit,
) {
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val sc by animateFloatAsState(
        if (pressed) 0.93f else 1f,
        spring(dampingRatio = 0.5f, stiffness = 500f),
        label = "btn",
    )
    val shape = RoundedCornerShape(18.dp)
    Box(
        modifier = modifier
            .scale(sc)
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    if (gold) listOf(Color(0xFFFFE082), Color(0xFFFFB300))
                    else listOf(Color(0x44FFFFFF), Color(0x18FFFFFF)),
                ),
            )
            .border(1.dp, if (gold) Color(0xFFFFF3C4) else Color(0x55FFFFFF), shape)
            .clickable(interactionSource = src, indication = null, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 13.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            color = if (gold) Color(0xFF3E2A00) else Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            maxLines = 1,
        )
    }
}

@Composable
fun RoundButton(text: String, onClick: () -> Unit) {
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val sc by animateFloatAsState(
        if (pressed) 0.88f else 1f,
        spring(dampingRatio = 0.5f, stiffness = 500f),
        label = "rbtn",
    )
    Box(
        modifier = Modifier
            .scale(sc)
            .clip(CircleShape)
            .background(Color(0x33FFFFFF))
            .border(1.dp, Color(0x55FFFFFF), CircleShape)
            .clickable(interactionSource = src, indication = null, onClick = onClick)
            .padding(12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, fontSize = 20.sp)
    }
}
