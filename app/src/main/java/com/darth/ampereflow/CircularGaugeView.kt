package com.darth.ampereflow

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

/**
 * Draws a partial-circle (arc) gauge: a gray background track plus a green
 * progress arc, matching the AmpereFlow battery percentage dial.
 */
class CircularGaugeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val startAngle = 145f
    private val sweepMax = 250f

    private var progress = 0f // 0f..1f

    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 26f
        strokeCap = Paint.Cap.ROUND
        color = Color.parseColor("#232B25")
    }

    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 26f
        strokeCap = Paint.Cap.ROUND
        color = Color.parseColor("#9FE6A0")
    }

    private val rect = RectF()

    fun setProgress(value: Float) {
        progress = value.coerceIn(0f, 1f)
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val stroke = trackPaint.strokeWidth
        rect.set(stroke / 2f, stroke / 2f, width - stroke / 2f, height - stroke / 2f)
        canvas.drawArc(rect, startAngle, sweepMax, false, trackPaint)
        if (progress > 0f) {
            canvas.drawArc(rect, startAngle, sweepMax * progress, false, progressPaint)
        }
    }
}
