package com.example.ourmajor.ui.journaling

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import kotlin.math.max
import kotlin.random.Random

class PaperTextureView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = 0xFFF5EFE4.toInt()
    }

    private val grainPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = 0x14000000
    }

    private val fiberPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = dp(1f)
        color = 0x11000000
        strokeCap = Paint.Cap.ROUND
    }

    private var seed: Int = 0

    init {
        isClickable = false
        isFocusable = false
        seed = (System.nanoTime() xor hashCode().toLong()).toInt()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        canvas.drawColor(bgPaint.color)

        val w = width
        val h = height
        if (w <= 0 || h <= 0) return

        val r = Random(seed)

        val dots = max(2200, (w * h) / 140)
        for (i in 0 until dots) {
            val x = r.nextInt(w).toFloat()
            val y = r.nextInt(h).toFloat()
            val a = 10 + r.nextInt(24)
            grainPaint.color = Color.argb(a, 0, 0, 0)
            canvas.drawCircle(x, y, dp(0.6f + r.nextFloat() * 0.9f), grainPaint)
        }

        val fibers = max(28, (w * h) / 180_000)
        for (i in 0 until fibers) {
            val y = r.nextInt(h).toFloat()
            val x0 = -dp(30f) + r.nextInt(w + dp(60f).toInt())
            val len = dp(90f + r.nextFloat() * 220f)
            val x1 = x0 + len
            val dy = dp(-10f + r.nextFloat() * 20f)
            canvas.drawLine(x0.toFloat(), y, x1.toFloat(), y + dy, fiberPaint)
        }
    }

    private fun dp(v: Float): Float = v * resources.displayMetrics.density
}
