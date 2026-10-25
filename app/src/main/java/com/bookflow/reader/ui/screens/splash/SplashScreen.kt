package com.bookflow.reader.ui.screens.splash

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bookflow.reader.ui.theme.*
import com.bookflow.reader.util.HapticHelper
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

data class Particle(
    val initialAngle: Float,
    val initialRadius: Float,
    val size: Float,
    val color: Color,
    val speedMultiplier: Float,
    val alpha: Float
)

@Composable
fun SplashScreen(onFinished: () -> Unit) {
    val context = LocalContext.current
    val density = LocalDensity.current

    // Phase 1: Particles swirl & converge
    val particleProgress = remember { Animatable(0f) }

    // Phase 2: Ink splash explosion
    val splashScale = remember { Animatable(0f) }
    val splashAlpha = remember { Animatable(1f) }

    // Phase 3: PathMeasure logo trace
    val pathProgress = remember { Animatable(0f) }
    val logoGlowAlpha = remember { Animatable(0f) }

    // Phase 4: Text scale-in & reveal
    var showText by remember { mutableStateOf(false) }
    var showSubtitle by remember { mutableStateOf(false) }

    val palette = listOf(DeepBlue, OceanBlue, LightMint, OceanBlueLight)

    val particles = remember {
        List(85) {
            Particle(
                initialAngle = Random.nextFloat() * 360f,
                initialRadius = Random.nextFloat() * 380f + 200f,
                size = Random.nextFloat() * 4f + 2f,
                color = palette.random(),
                speedMultiplier = Random.nextFloat() * 0.4f + 0.8f,
                alpha = Random.nextFloat() * 0.5f + 0.5f
            )
        }
    }

    // Pre-calculate Logo Path geometry in local screen space
    val docHw = with(density) { 32.dp.toPx() }
    val docHh = with(density) { 44.dp.toPx() }
    val foldSize = with(density) { 18.dp.toPx() }
    val infHw = with(density) { 62.dp.toPx() }
    val infHh = with(density) { 26.dp.toPx() }

    val infMeasure = remember { PathMeasure() }
    val docMeasure = remember { PathMeasure() }
    val foldMeasure = remember { PathMeasure() }

    val animatedInfPath = remember { Path() }
    val animatedDocPath = remember { Path() }
    val animatedFoldPath = remember { Path() }

    LaunchedEffect(Unit) {
        // Phase 1: Swirl & converge to center
        particleProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 1400, easing = FastOutSlowInEasing)
        )

        // Phase 2 & 3: Ink splash explosion & Path trace
        try { HapticHelper.click(context) } catch (_: Exception) {}

        coroutineScope {
            launch {
                splashScale.animateTo(
                    targetValue = 1.3f,
                    animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing)
                )
            }
            launch {
                delay(80)
                splashAlpha.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(durationMillis = 320, easing = LinearEasing)
                )
            }
            launch {
                delay(100) // Out of the splash, Path begins tracing
                pathProgress.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 950, easing = FastOutSlowInEasing)
                )
                logoGlowAlpha.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 280)
                )
            }
        }

        // Phase 4: Logo finishes drawing -> BookFlow text scales in gracefully
        showText = true
        delay(220)
        showSubtitle = true
        delay(1100) // Hold gracefully before entering home screen
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A1628))
            .clickable { onFinished() }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height * 0.40f)

            // --- Phase 1: Swirling particles converging to the center ---
            if (particleProgress.value < 1f) {
                val t = particleProgress.value
                particles.forEach { particle ->
                    val radius = particle.initialRadius * (1f - t)
                    val angleDegrees = particle.initialAngle + t * 720f * particle.speedMultiplier
                    val angleRadians = Math.toRadians(angleDegrees.toDouble())

                    val x = center.x + radius * cos(angleRadians).toFloat()
                    val y = center.y + radius * sin(angleRadians).toFloat()

                    val currentAlpha = particle.alpha * (1f - t * 0.35f)

                    drawCircle(
                        color = particle.color.copy(alpha = currentAlpha),
                        radius = particle.size.dp.toPx() * (1f - t * 0.15f),
                        center = Offset(x, y)
                    )
                }
            }

            // --- Phase 2: Tiny "Ink Splash" explosion ---
            if (splashScale.value > 0f && splashAlpha.value > 0f) {
                val sScale = splashScale.value
                val sAlpha = splashAlpha.value

                // Main expanding ink droplet
                drawCircle(
                    color = OceanBlue.copy(alpha = (sAlpha * 0.85f).coerceIn(0f, 1f)),
                    radius = 26.dp.toPx() * sScale,
                    center = center
                )

                // Inner bright turquoise droplet core
                drawCircle(
                    color = LightMint.copy(alpha = (sAlpha * 0.95f).coerceIn(0f, 1f)),
                    radius = 13.dp.toPx() * sScale,
                    center = center
                )

                // Shockwave ripple ring
                drawCircle(
                    color = OceanBlueLight.copy(alpha = (sAlpha * 0.6f).coerceIn(0f, 1f)),
                    radius = 38.dp.toPx() * sScale,
                    center = center,
                    style = Stroke(width = 2.dp.toPx())
                )

                // Radiating splatter droplets
                val dropletAngles = listOf(18.0, 56.0, 96.0, 138.0, 175.0, 218.0, 258.0, 296.0, 334.0, 355.0)
                val dropletDists = listOf(36f, 48f, 32f, 52f, 38f, 46f, 34f, 54f, 40f, 44f)
                for (i in dropletAngles.indices) {
                    val rad = Math.toRadians(dropletAngles[i])
                    val dist = dropletDists[i].dp.toPx() * sScale
                    val dx = center.x + (dist * cos(rad)).toFloat()
                    val dy = center.y + (dist * sin(rad)).toFloat()
                    val dropRadius = (3f - sScale * 1.5f).coerceAtLeast(0.8f).dp.toPx()
                    drawCircle(
                        color = if (i % 2 == 0) LightMint.copy(alpha = sAlpha) else OceanBlue.copy(alpha = sAlpha),
                        radius = dropRadius,
                        center = Offset(dx, dy)
                    )
                }
            }

            // --- Phase 3: Out of the ink splash, PathMeasure traces out the app logo ---
            val p = pathProgress.value
            if (p > 0f) {
                // Construct geometry paths centered at 'center'
                val docPath = Path().apply {
                    moveTo(center.x - docHw, center.y + docHh)
                    lineTo(center.x - docHw, center.y - docHh)
                    lineTo(center.x + docHw - foldSize, center.y - docHh)
                    lineTo(center.x + docHw, center.y - docHh + foldSize)
                    lineTo(center.x + docHw, center.y + docHh)
                    lineTo(center.x - docHw, center.y + docHh)
                    close()
                }

                val foldPath = Path().apply {
                    moveTo(center.x + docHw - foldSize, center.y - docHh)
                    lineTo(center.x + docHw - foldSize, center.y - docHh + foldSize)
                    lineTo(center.x + docHw, center.y - docHh + foldSize)
                }

                val infinityPath = Path().apply {
                    moveTo(center.x, center.y)
                    // Right loop
                    cubicTo(
                        center.x + infHw * 0.45f, center.y - infHh * 1.2f,
                        center.x + infHw * 0.95f, center.y - infHh * 0.95f,
                        center.x + infHw, center.y
                    )
                    cubicTo(
                        center.x + infHw * 1.05f, center.y + infHh * 0.95f,
                        center.x + infHw * 0.55f, center.y + infHh * 1.2f,
                        center.x, center.y
                    )
                    // Left loop
                    cubicTo(
                        center.x - infHw * 0.45f, center.y + infHh * 1.2f,
                        center.x - infHw * 0.95f, center.y + infHh * 0.95f,
                        center.x - infHw, center.y
                    )
                    cubicTo(
                        center.x - infHw * 1.05f, center.y - infHh * 0.95f,
                        center.x - infHw * 0.55f, center.y - infHh * 1.2f,
                        center.x, center.y
                    )
                }

                // 1. Trace Infinity Flow Ribbon (starts right at splash center at p=0)
                val infT = (p / 0.85f).coerceIn(0f, 1f)
                if (infT > 0f) {
                    animatedInfPath.reset()
                    infMeasure.setPath(infinityPath, forceClosed = false)
                    val infLen = infMeasure.length
                    infMeasure.getSegment(0f, infLen * infT, animatedInfPath, true)

                    // Outer cyan glow aura
                    drawPath(
                        path = animatedInfPath,
                        color = OceanBlue.copy(alpha = 0.28f),
                        style = Stroke(
                            width = 9.dp.toPx(),
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )

                    // Radiant infinity ribbon stroke
                    drawPath(
                        path = animatedInfPath,
                        brush = Brush.horizontalGradient(
                            colors = listOf(LightMint, OceanBlueLight, LightMint),
                            startX = center.x - infHw,
                            endX = center.x + infHw
                        ),
                        style = Stroke(
                            width = 4.5.dp.toPx(),
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )

                    // Glowing pen tip leading the trace
                    if (infT < 1f) {
                        val tip = infMeasure.getPosition(infLen * infT)
                        drawCircle(
                            color = Color.White,
                            radius = 4.dp.toPx(),
                            center = tip
                        )
                        drawCircle(
                            color = LightMint.copy(alpha = 0.65f),
                            radius = 8.dp.toPx(),
                            center = tip
                        )
                    }
                }

                // 2. Trace Document Page Outline (interweaves smoothly, starting at p=0.15)
                val docT = ((p - 0.15f) / 0.85f).coerceIn(0f, 1f)
                if (docT > 0f) {
                    animatedDocPath.reset()
                    docMeasure.setPath(docPath, forceClosed = true)
                    val docLen = docMeasure.length
                    docMeasure.getSegment(0f, docLen * docT, animatedDocPath, true)

                    // Soft document back glow
                    drawPath(
                        path = animatedDocPath,
                        color = DeepBlue.copy(alpha = 0.35f),
                        style = Stroke(
                            width = 7.dp.toPx(),
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )

                    // Crisp document outline in WarmCream
                    drawPath(
                        path = animatedDocPath,
                        color = WarmCream.copy(alpha = 0.95f),
                        style = Stroke(
                            width = 3.5.dp.toPx(),
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )

                    // Corner dog-ear fold
                    animatedFoldPath.reset()
                    foldMeasure.setPath(foldPath, forceClosed = false)
                    val foldLen = foldMeasure.length
                    foldMeasure.getSegment(0f, foldLen * docT, animatedFoldPath, true)

                    drawPath(
                        path = animatedFoldPath,
                        color = WarmCream.copy(alpha = 0.85f),
                        style = Stroke(
                            width = 2.5.dp.toPx(),
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )

                    // Glowing trace tip for document
                    if (docT < 1f) {
                        val tip = docMeasure.getPosition(docLen * docT)
                        drawCircle(
                            color = Color.White,
                            radius = 3.5.dp.toPx(),
                            center = tip
                        )
                    }
                }

                // Finished logo inner ambient sheen
                if (logoGlowAlpha.value > 0f) {
                    val glow = logoGlowAlpha.value
                    drawPath(
                        path = docPath,
                        color = Color(0x33102A43).copy(alpha = glow * 0.3f),
                        style = Fill
                    )
                }
            }
        }

        // --- Phase 4: Text Scales in Gracefully below the Logo ---
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.fillMaxHeight(0.55f))

            AnimatedVisibility(
                visible = showText,
                enter = fadeIn(tween(500)) + scaleIn(initialScale = 0.6f, animationSpec = tween(500))
            ) {
                Text(
                    text = "BookFlow",
                    style = MaterialTheme.typography.headlineLarge,
                    color = WarmCream,
                    fontWeight = FontWeight.Bold,
                    fontSize = 32.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            AnimatedVisibility(
                visible = showSubtitle,
                enter = fadeIn(tween(500))
            ) {
                Text(
                    text = "Your Revision Companion",
                    style = MaterialTheme.typography.bodyMedium,
                    color = OceanBlue,
                    fontSize = 15.sp
                )
            }
        }
    }
}
