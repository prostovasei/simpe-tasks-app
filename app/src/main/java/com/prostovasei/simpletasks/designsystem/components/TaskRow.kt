package com.prostovasei.simpletasks.designsystem.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import com.prostovasei.simpletasks.designsystem.theme.AppTheme
import com.prostovasei.simpletasks.designsystem.theme.PriorityColors
import com.prostovasei.simpletasks.domain.model.Priority

/**
 * Плашка задачи в одну строку (итерация 13, макет PM): квадратная галочка (рамка — цвет приоритета),
 * цветной квадратик списка, название, ↻ у повторяющихся; справа [trailing] — время столбиком
 * («16:00» или «16:00 · 17:00»), у списков — день и время.
 * [isCompleting] — задача только что отмечена: зачёркнута и полупрозрачна (SPEC.md §4.1).
 * [isGhost] — будущий повтор: бледный, без галочки. [isOverdue] — рамка галочки и время красные.
 * [animateCompletion] — при отметке плашка «рассыпается в пыль» слева направо (итерация 15, вариант A4);
 * без него (экран «Выполненные», поиск) отмеченная просто зачёркнута.
 * [highlight] — подсветить найденное в названии, [subtitle] — строка под названием (поиск).
 */
@Composable
fun TaskRow(
    title: String,
    priority: Priority,
    listColor: Color?,
    isCompleting: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String = "",
    onClick: (() -> Unit)? = null,
    isRecurring: Boolean = false,
    isGhost: Boolean = false,
    isOverdue: Boolean = false,
    trailing: List<String> = emptyList(),
    @DrawableRes listIcon: Int? = null,
    animateCompletion: Boolean = true,
    highlight: String? = null,
    subtitle: String? = null,
) {
    val colors = AppTheme.colors
    val spacing = AppTheme.spacing
    val sizes = AppTheme.sizes
    val dissolving = isCompleting && animateCompletion
    val dust = remember { Animatable(0f) }
    LaunchedEffect(dissolving) {
        if (dissolving) dust.animateTo(1f, tween(DUST_MS, delayMillis = DUST_DELAY_MS, easing = LinearEasing)) else dust.snapTo(0f)
    }
    val particles = remember(title) { dustParticles(title.hashCode()) }
    val particleSize = with(LocalDensity.current) { sizes.dustParticle.toPx() }
    val dustColors = listOf(colors.textSecondary, colors.inactive, colors.textPrimary)
    val strike = isCompleting && !animateCompletion
    val alpha = when {
        strike -> COMPLETING_ALPHA
        isGhost -> GHOST_ALPHA
        else -> 1f
    }
    val ring = when {
        isOverdue -> colors.overdue
        priority == Priority.HIGH -> colors.checkRings.high
        priority == Priority.MEDIUM -> colors.checkRings.medium
        priority == Priority.LOW -> colors.checkRings.low
        else -> colors.inactive
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .alpha(alpha)
            .padding(horizontal = spacing.xxs)
            .dust({ dust.value }, particles, particleSize, dustColors)
            .clip(AppTheme.shapes.medium)
            .background(colors.surface)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(start = spacing.xxs, end = spacing.sm, top = sizes.cardGap, bottom = sizes.cardGap)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(sizes.checkboxTouch)
                // У будущего повтора галочки нет: выполнить можно только ближайший раз.
                .then(if (isGhost) Modifier else Modifier.clickable(onClick = onToggle).testTag("$testTag:check")),
            contentAlignment = Alignment.Center,
        ) {
            if (!isGhost) SquareCheck(checked = isCompleting, ring = ring)
        }
        listColor?.let {
            ListSquare(it, listIcon)
            Spacer(Modifier.width(spacing.xs))
        }
        // Название занимает всё свободное место, ↻ — сразу за текстом, время прижато вправо.
        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f, fill = false)) {
                Text(
                    text = highlighted(title, highlight, colors.accentSoft),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (strike) colors.textSecondary else colors.textPrimary,
                    textDecoration = if (strike) TextDecoration.LineThrough else null,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                subtitle?.let {
                    Text(
                        text = highlighted(it, highlight, colors.accentSoft),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.testTag("$testTag:subtitle"),
                    )
                }
            }
            if (isRecurring) {
                Icon(
                    Icons.Default.Refresh,
                    contentDescription = null,
                    tint = colors.textSecondary,
                    modifier = Modifier.padding(start = spacing.xxs).size(sizes.metaIcon).testTag("$testTag:repeat"),
                )
            }
        }
        if (trailing.isNotEmpty()) Spacer(Modifier.width(spacing.xs))
        TrailingTime(trailing, color = if (isOverdue) colors.overdue else colors.textPrimary)
    }
}

/** Квадратик цвета списка со значком [icon] (итерация 14); [large] — на экране «Списки» и в заголовке списка. */
@Composable
fun ListSquare(color: Color, @DrawableRes icon: Int?, modifier: Modifier = Modifier, large: Boolean = false) {
    val sizes = AppTheme.sizes
    Box(
        modifier = modifier
            .size(if (large) sizes.listSquareLarge else sizes.listSquare)
            .background(color, if (large) AppTheme.shapes.medium else AppTheme.shapes.small),
        contentAlignment = Alignment.Center,
    ) {
        if (icon != null) {
            Icon(
                painterResource(icon),
                contentDescription = null,
                tint = AppTheme.colors.onListSquare,
                modifier = Modifier.size(if (large) sizes.listGlyphLarge else sizes.listGlyph),
            )
        }
    }
}

/** Квадратная галочка (макет PM): рамка цвета [ring], отмеченная — залита акцентом. */
@Composable
private fun SquareCheck(checked: Boolean, ring: Color) {
    val colors = AppTheme.colors
    val shape = AppTheme.shapes.small
    Box(
        modifier = Modifier
            .size(AppTheme.sizes.checkbox)
            .clip(shape)
            .background(if (checked) colors.accent else colors.surface)
            .border(AppTheme.sizes.checkboxStroke, if (checked) colors.accent else ring, shape),
        contentAlignment = Alignment.Center,
    ) {
        if (checked) Icon(Icons.Default.Check, contentDescription = null, tint = colors.onAccent, modifier = Modifier.size(AppTheme.sizes.metaIcon))
    }
}

/** Время справа: одна строка или две через точку («16:00 · 17:00» столбиком, плотно — как в макете PM). */
@Composable
private fun TrailingTime(lines: List<String>, color: Color) {
    if (lines.isEmpty()) return
    if (lines.size == 1) {
        Text(lines[0], style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = color, maxLines = 1)
        return
    }
    val base = MaterialTheme.typography.labelMedium
    val tight = base.copy(
        fontWeight = FontWeight.Bold,
        lineHeight = base.fontSize,
        lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.Both),
    )
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        lines.forEachIndexed { index, line ->
            if (index > 0) Text("·", style = tight, color = AppTheme.colors.textSecondary)
            Text(line, style = tight, color = color, maxLines = 1)
        }
    }
}

/** Плашка приоритета P1–P3 (SPEC.md §3). Для «без приоритета» не рисуется — меньше визуального шума. */
@Composable
fun PriorityBadge(priority: Priority, modifier: Modifier = Modifier) {
    val palette = AppTheme.colors.priority
    val (label, colors) = when (priority) {
        Priority.HIGH -> "P1" to palette.high
        Priority.MEDIUM -> "P2" to palette.medium
        Priority.LOW -> "P3" to palette.low
        Priority.NONE -> return
    }
    BadgeText(label, colors, modifier)
}

@Composable
private fun BadgeText(label: String, colors: PriorityColors, modifier: Modifier) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        color = colors.content,
        modifier = modifier
            .background(colors.container, AppTheme.shapes.small)
            .padding(horizontal = AppTheme.spacing.xs, vertical = AppTheme.spacing.xxs),
    )
}

/** Цветная точка списка. */
@Composable
fun ColorDot(color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(AppTheme.sizes.listDot)
            .background(color, CircleShape),
    )
}

/** Текст с подсвеченными вхождениями [query] (без учёта регистра). */
fun highlighted(text: String, query: String?, background: Color): AnnotatedString = buildAnnotatedString {
    val q = query?.trim().orEmpty()
    if (q.isEmpty()) {
        append(text)
        return@buildAnnotatedString
    }
    var from = 0
    while (true) {
        val at = text.indexOf(q, from, ignoreCase = true)
        if (at < 0) break
        append(text.substring(from, at))
        withStyle(SpanStyle(background = background)) { append(text.substring(at, at + q.length)) }
        from = at + q.length
    }
    append(text.substring(from))
}

/** Частица пыли: место на плашке (доли ширины и высоты), куда улетает (доли) и оттенок. */
private class DustParticle(val x: Float, val y: Float, val dx: Float, val dy: Float, val shade: Int, val scale: Float)

private fun dustParticles(seed: Int): List<DustParticle> {
    val random = kotlin.random.Random(seed)
    fun between(from: Float, to: Float) = from + random.nextFloat() * (to - from)
    return List(DUST_PARTICLES) {
        DustParticle(random.nextFloat(), random.nextFloat(), between(0.06f, 0.22f), between(-0.9f, 0.2f), it % 3, between(0.6f, 1.3f))
    }
}

/**
 * «Распад в пыль»: граница [progress] идёт слева направо, левее неё плашки уже нет, а с границы
 * срываются частицы и улетают вправо-вверх, тая.
 */
private fun Modifier.dust(progress: () -> Float, particles: List<DustParticle>, particleSize: Float, colors: List<Color>): Modifier =
    drawWithContent {
        val p = progress()
        if (p <= 0f) {
            drawContent()
            return@drawWithContent
        }
        val edge = p * (1f + DUST_TAIL)
        clipRect(left = (edge * size.width).coerceAtLeast(0f)) { this@drawWithContent.drawContent() }
        particles.forEach { particle ->
            val t = ((edge - particle.x) / DUST_TAIL).coerceIn(0f, 1f)
            if (t <= 0f || t >= 1f) return@forEach
            val side = particleSize * particle.scale
            drawRect(
                color = colors[particle.shade],
                topLeft = Offset(particle.x * size.width + t * particle.dx * size.width, particle.y * size.height + t * particle.dy * size.height),
                size = Size(side, side),
                alpha = 1f - t,
            )
        }
    }

private const val DUST_MS = 850
private const val DUST_DELAY_MS = 150
private const val DUST_PARTICLES = 90

/** Сколько ширины плашки частица летит после того, как граница её прошла. */
private const val DUST_TAIL = 0.35f

private const val COMPLETING_ALPHA = 0.4f
private const val GHOST_ALPHA = 0.5f
