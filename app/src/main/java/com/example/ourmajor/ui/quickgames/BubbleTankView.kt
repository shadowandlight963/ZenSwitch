package com.example.ourmajor.ui.quickgames

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.util.AttributeSet
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.animation.LinearInterpolator
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

class BubbleTankView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    data class Bubble(
        val number: Int,
        var x: Float,
        var y: Float,
        var vx: Float,
        val radius: Float,
        val speed: Float,
        val wobbleAmp: Float,
        val wobbleFreq: Float,
        val phase: Float,
        val windAmp: Float,
        val windFreq: Float,
        val windPhase: Float
    )

    private data class Particle(
        var x: Float,
        var y: Float,
        var vx: Float,
        var vy: Float,
        var life: Float,
        val radius: Float
    )

    interface Listener {
        fun onBubbleTapped(number: Int, correct: Boolean)
        fun onComplete()
    }

    private var listener: Listener? = null

    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = 0x22FFFFFF
    }

    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = dp(2f)
        color = 0x66FFFFFF
    }

    private val bg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val particlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = 0xAAFFFFFF.toInt()
    }

    private val text = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        textSize = sp(18f)
        color = 0xEEFFFFFF.toInt()
    }

    private val clipPath = Path()
    private val rect = RectF()

    private val bubbles = ArrayList<Bubble>()
    private val particles = ArrayList<Particle>()

    private val maxBubbles = 15

    private var expected = 1
    private var anim: ValueAnimator? = null
    private var timeSec: Float = 0f

    private var lastShakeNum: Int? = null
    private var shakeUntilMs: Long = 0L

    fun setListener(l: Listener?) {
        listener = l
    }

    fun startGame() {
        expected = 1
        spawnBubbles()
        startAnim()
        invalidate()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        anim?.cancel()
        anim = null
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        rect.set(0f, 0f, w.toFloat(), h.toFloat())
        clipPath.reset()
        clipPath.addRoundRect(rect, dp(22f), dp(22f), Path.Direction.CW)

        bg.shader = LinearGradient(
            0f,
            0f,
            0f,
            h.toFloat(),
            intArrayOf(0xFF021826.toInt(), 0xFF012C3E.toInt(), 0xFF00111B.toInt()),
            floatArrayOf(0f, 0.55f, 1f),
            Shader.TileMode.CLAMP
        )

        spawnBubbles()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val save = canvas.save()
        canvas.clipPath(clipPath)

        canvas.drawRect(rect, bg)

        val now = System.currentTimeMillis()

        bubbles.forEach { b ->
            val wobble = b.wobbleAmp * sin((timeSec * b.wobbleFreq) + b.phase)
            var dx = wobble
            if (lastShakeNum == b.number && now < shakeUntilMs) {
                dx += (sin((now % 100).toFloat()) * dp(6f))
            }

            canvas.drawCircle(b.x + dx, b.y, b.radius, fill)
            canvas.drawCircle(b.x + dx, b.y, b.radius, stroke)

            val ty = b.y - ((text.descent() + text.ascent()) / 2f)
            canvas.drawText(b.number.toString(), b.x + dx, ty, text)
        }

        particles.forEach { p ->
            particlePaint.alpha = (p.life.coerceIn(0f, 1f) * 180f).toInt().coerceIn(0, 180)
            canvas.drawCircle(p.x, p.y, p.radius, particlePaint)
        }

        canvas.restoreToCount(save)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.actionMasked != MotionEvent.ACTION_DOWN) return true

        val x = event.x
        val y = event.y

        val tapped = bubbles.lastOrNull { b ->
            val dx = x - b.x
            val dy = y - b.y
            (dx * dx + dy * dy) <= (b.radius * b.radius)
        } ?: return true

        if (tapped.number != expected) {
            lastShakeNum = tapped.number
            shakeUntilMs = System.currentTimeMillis() + 200L
            val dir = if (x >= tapped.x) -1f else 1f
            tapped.vx += dir * dp(260f)
            tapped.y += dp(8f)
            listener?.onBubbleTapped(tapped.number, false)
            invalidate()
            return true
        }

        performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        burstAt(tapped.x, tapped.y, tapped.radius)
        bubbles.remove(tapped)
        listener?.onBubbleTapped(tapped.number, true)
        expected++

        if (expected <= 10 && bubbles.size < maxBubbles) {
            val r = Random(System.nanoTime())
            val w = width.toFloat().coerceAtLeast(1f)
            val h = height.toFloat().coerceAtLeast(1f)
            val n = 1 + r.nextInt(10)
            val rad = dp(24f + r.nextFloat() * 14f)
            val x0 = dp(30f) + r.nextFloat() * (w - dp(60f))
            val y0 = h + dp(30f) + r.nextFloat() * dp(120f)

            val base = dp(18f + r.nextFloat() * 14f) * 3.0f
            val variance = 0.60f + r.nextFloat() * 1.80f
            val speed = base * variance

            val amp = dp(10f + r.nextFloat() * 18f)
            val freq = 1.2f + r.nextFloat() * 1.8f
            val phase = r.nextFloat() * 6.28f

            val windAmp = dp(280f + r.nextFloat() * 520f)
            val windFreq = 0.65f + r.nextFloat() * 1.25f
            val windPhase = r.nextFloat() * 6.28f

            val vx0 = dp((-160f + r.nextFloat() * 320f))

            bubbles.add(
                Bubble(
                    number = n,
                    x = x0,
                    y = y0,
                    vx = vx0,
                    radius = rad,
                    speed = speed,
                    wobbleAmp = amp,
                    wobbleFreq = freq,
                    phase = phase,
                    windAmp = windAmp,
                    windFreq = windFreq,
                    windPhase = windPhase
                )
            )
        }

        if (expected == 11) {
            listener?.onComplete()
        }

        invalidate()
        return true
    }

    private fun spawnBubbles() {
        val w = width.toFloat().coerceAtLeast(1f)
        val h = height.toFloat().coerceAtLeast(1f)
        val r = Random(System.nanoTime())

        bubbles.clear()

        val numbers = (1..10).toMutableList()
        while (numbers.size < maxBubbles) {
            numbers.add(1 + r.nextInt(10))
        }
        numbers.shuffle(r)

        numbers.forEachIndexed { i, n ->
            val rad = dp(26f + r.nextFloat() * 12f)
            val x = dp(30f) + r.nextFloat() * (w - dp(60f))
            val y = dp(40f) + r.nextFloat() * (h - dp(80f))

            val base = dp(18f + r.nextFloat() * 14f) * 3.0f
            val variance = 0.60f + r.nextFloat() * 1.80f
            val speed = base * variance

            val amp = dp(10f + r.nextFloat() * 18f)
            val freq = 1.2f + r.nextFloat() * 1.8f
            val phase = r.nextFloat() * 6.28f

            val windAmp = dp(280f + r.nextFloat() * 520f)
            val windFreq = 0.65f + r.nextFloat() * 1.25f
            val windPhase = r.nextFloat() * 6.28f

            val vx0 = dp((-160f + r.nextFloat() * 320f))

            bubbles.add(
                Bubble(
                    number = n,
                    x = x,
                    y = y,
                    vx = vx0,
                    radius = rad,
                    speed = speed,
                    wobbleAmp = amp,
                    wobbleFreq = freq,
                    phase = phase,
                    windAmp = windAmp,
                    windFreq = windFreq,
                    windPhase = windPhase
                )
            )
        }

        particles.clear()
    }

    private fun startAnim() {
        anim?.cancel()
        anim = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 1_000_000L
            interpolator = LinearInterpolator()
            repeatCount = ValueAnimator.INFINITE
            addUpdateListener {
                tick(1f / 60f)
                invalidate()
            }
            start()
        }
    }

    private fun tick(dt: Float) {
        timeSec += dt
        val h = height.toFloat().coerceAtLeast(1f)
        val w = width.toFloat().coerceAtLeast(1f)

        if (bubbles.isNotEmpty()) {
            val r = Random((System.nanoTime() xor (timeSec.toBits().toLong())).toInt())

            bubbles.forEach { b ->
                b.y -= b.speed * dt

                val wind = b.windAmp * sin((timeSec * b.windFreq) + b.windPhase)
                b.vx += wind * dt
                b.x += b.vx * dt
                b.vx *= 0.88f
                b.vx = min(dp(540f), max(-dp(540f), b.vx))
                if (b.y + b.radius < -dp(20f)) {
                    b.y = h + dp(40f) + r.nextFloat() * dp(80f)
                    b.x = dp(30f) + r.nextFloat() * (w - dp(60f))
                    b.vx = dp((-200f + r.nextFloat() * 400f))
                }
                b.x = min(w - dp(20f), max(dp(20f), b.x))
            }
        }

        if (particles.isNotEmpty()) {
            val it = particles.iterator()
            while (it.hasNext()) {
                val p = it.next()
                p.life -= dt * 2.6f
                if (p.life <= 0f) {
                    it.remove()
                    continue
                }
                p.x += p.vx * dt
                p.y += p.vy * dt
                p.vy += dp(60f) * dt
                p.vx *= 0.98f
                p.vy *= 0.98f
            }
        }
    }

    private fun burstAt(x: Float, y: Float, r: Float) {
        val rand = Random(System.nanoTime())
        val count = 18 + rand.nextInt(14)
        repeat(count) {
            val a = rand.nextFloat() * 6.28f
            val sp = dp(120f + rand.nextFloat() * 220f)
            val vx = (kotlin.math.cos(a.toDouble()).toFloat()) * sp
            val vy = (kotlin.math.sin(a.toDouble()).toFloat()) * sp
            particles.add(
                Particle(
                    x = x + (rand.nextFloat() - 0.5f) * r * 0.3f,
                    y = y + (rand.nextFloat() - 0.5f) * r * 0.3f,
                    vx = vx,
                    vy = vy,
                    life = 1f,
                    radius = dp(1.6f + rand.nextFloat() * 2.2f)
                )
            )
        }
    }

    private fun dp(v: Float): Float = v * resources.displayMetrics.density
    private fun sp(v: Float): Float = v * resources.displayMetrics.density * resources.configuration.fontScale
}
