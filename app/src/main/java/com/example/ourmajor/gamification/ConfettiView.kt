package com.example.ourmajor.gamification

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import kotlin.math.max
import kotlin.random.Random

class ConfettiView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private data class Piece(
        var x: Float,
        var y: Float,
        var vx: Float,
        var vy: Float,
        var size: Float,
        var life: Float,
        val color: Int
    )

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val pieces = ArrayList<Piece>()
    private var anim: ValueAnimator? = null
    private var startedMs: Long = 0L

    fun burst(count: Int = 120) {
        val w = width.toFloat().coerceAtLeast(1f)
        val rand = Random(System.nanoTime())
        startedMs = System.currentTimeMillis()

        pieces.clear()
        repeat(count) {
            val x = rand.nextFloat() * w
            val y = -dp(20f) - rand.nextFloat() * dp(240f)
            val vx = (-dp(60f) + rand.nextFloat() * dp(120f))
            val vy = (dp(200f) + rand.nextFloat() * dp(520f))
            val size = dp(3f + rand.nextFloat() * 5f)
            val color = when (rand.nextInt(5)) {
                0 -> 0xFF00FFFF.toInt()
                1 -> 0xFFFF00FF.toInt()
                2 -> 0xFFFFD666.toInt()
                3 -> 0xFF63FFE2.toInt()
                else -> 0xFFFFFFFF.toInt()
            }
            pieces.add(Piece(x, y, vx, vy, size, 1f, color))
        }

        startAnim()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        anim?.cancel()
        anim = null
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        for (p in pieces) {
            paint.color = p.color
            paint.alpha = (max(0f, p.life) * 255f).toInt().coerceIn(0, 255)
            canvas.drawRect(p.x, p.y, p.x + p.size, p.y + p.size * 1.6f, paint)
        }
    }

    private fun startAnim() {
        anim?.cancel()
        anim = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 3_000L
            interpolator = LinearInterpolator()
            addUpdateListener {
                tick(1f / 60f)
                invalidate()
            }
            start()
        }
    }

    private fun tick(dt: Float) {
        val w = width.toFloat().coerceAtLeast(1f)
        val h = height.toFloat().coerceAtLeast(1f)

        val it = pieces.iterator()
        while (it.hasNext()) {
            val p = it.next()
            p.life -= dt * 0.35f
            p.x += p.vx * dt
            p.y += p.vy * dt
            p.vy += dp(380f) * dt
            p.vx *= 0.992f
            if (p.y > h + dp(40f) || p.life <= 0f) {
                it.remove()
            }
            if (p.x < -dp(40f)) p.x = w + dp(10f)
            if (p.x > w + dp(40f)) p.x = -dp(10f)
        }

        // Auto-stop when done
        if (pieces.isEmpty()) {
            anim?.cancel()
            anim = null
        }

        // Safety: stop after a while
        if (System.currentTimeMillis() - startedMs > 4_000L) {
            pieces.clear()
        }
    }

    private fun dp(v: Float): Float = v * resources.displayMetrics.density
}
