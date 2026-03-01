package com.example.ourmajor.ui.home

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import androidx.core.content.ContextCompat
import com.example.ourmajor.R
import com.google.android.material.color.MaterialColors
import kotlin.math.min

class RoundedArcProgressView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var progress: Float = 0f
    private var max: Float = 100f

    private var progressColor: Int = 0
    private var trackColor: Int = 0xFFEFECE8.toInt()
    private var strokeWidthPx: Float = dp(20f)

    private val rect = RectF()

    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = strokeWidthPx
        color = trackColor
        strokeCap = Paint.Cap.ROUND
    }

    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = strokeWidthPx
        color = progressColor
        strokeCap = Paint.Cap.ROUND
    }

    init {
        progressColor = MaterialColors.getColor(this, com.google.android.material.R.attr.colorPrimary)

        if (attrs != null) {
            val a = context.obtainStyledAttributes(attrs, R.styleable.RoundedArcProgressView)
            progress = a.getFloat(R.styleable.RoundedArcProgressView_rapv_progress, 0f)
            max = a.getFloat(R.styleable.RoundedArcProgressView_rapv_max, 100f)

            a.peekValue(R.styleable.RoundedArcProgressView_rapv_progressColor)?.let { v ->
                progressColor = if (v.type == TypedValue.TYPE_ATTRIBUTE) {
                    MaterialColors.getColor(this, v.data)
                } else {
                    a.getColor(R.styleable.RoundedArcProgressView_rapv_progressColor, progressColor)
                }
            }

            a.peekValue(R.styleable.RoundedArcProgressView_rapv_trackColor)?.let { v ->
                trackColor = if (v.type == TypedValue.TYPE_ATTRIBUTE) {
                    MaterialColors.getColor(this, v.data)
                } else {
                    a.getColor(R.styleable.RoundedArcProgressView_rapv_trackColor, trackColor)
                }
            }
            strokeWidthPx = a.getDimension(
                R.styleable.RoundedArcProgressView_rapv_strokeWidth,
                strokeWidthPx
            )
            a.recycle()
            trackPaint.color = trackColor
            progressPaint.color = progressColor
            trackPaint.strokeWidth = strokeWidthPx
            progressPaint.strokeWidth = strokeWidthPx
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val size = min(MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.getSize(heightMeasureSpec))
        setMeasuredDimension(size, size)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val pad = strokeWidthPx / 2f + dp(2f)
        rect.set(pad, pad, w - pad, h - pad)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        // Full light track
        canvas.drawArc(rect, -90f, 360f, false, trackPaint)
        // Progress arc with rounded ends
        val sweep = (progress / max).coerceIn(0f, 1f) * 360f
        if (sweep > 0f) {
            canvas.drawArc(rect, -90f, sweep, false, progressPaint)
        }
    }

    fun setProgress(current: Float, maximum: Float = 100f, animate: Boolean = true) {
        val safeMax = if (maximum > 0f) maximum else 1f
        max = safeMax
        val target = current.coerceIn(0f, safeMax)
        if (animate) {
            val start = progress
            ValueAnimator.ofFloat(start, target).apply {
                duration = 750
                addUpdateListener {
                    progress = it.animatedValue as Float
                    invalidate()
                }
            }.start()
        } else {
            progress = target
            invalidate()
        }
    }

    private fun dp(v: Float): Float = v * resources.displayMetrics.density
}
