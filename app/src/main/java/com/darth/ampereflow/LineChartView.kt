package com.darth.ampereflow

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View

/**
 * Lightweight line-chart view with no external charting library. Draws a smooth-ish
 * polyline through the given values, auto-scaled to the view's height, with an
 * optional gradient fill underneath.
 *
 * detailed = false -> thin sparkline, no fill (used behind Current/Wattage cards)
 * detailed = true  -> thicker line + gradient fill (used on the full detail screen)
 */
class LineChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    var detailed: Boolean = false
    var lineColor: Int = Color.parseColor("#9FE6A0")
        set(value) {
            field = value
            invalidate()
        }

    private var values: List<Float> = emptyList()

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val linePath = Path()
    private val fillPath = Path()

    fun setValues(newValues: List<Float>) {
        values = newValues
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (values.size < 2) return

        val w = width.toFloat()
        val h = height.toFloat()
        val strokeWidth = if (detailed) 6f else 3f
        val vPad = strokeWidth

        val min = values.min()
        val max = values.max()
        val range = (max - min).let { if (it < 0.0001f) 1f else it }

        val stepX = w / (values.size - 1)

        linePath.reset()
        fillPath.reset()

        values.forEachIndexed { i, value ->
            val x = i * stepX
            val normalized = (value - min) / range
            val y = vPad + (1f - normalized) * (h - 2 * vPad)
            if (i == 0) {
                linePath.moveTo(x, y)
                fillPath.moveTo(x, h)
                fillPath.lineTo(x, y)
            } else {
                linePath.lineTo(x, y)
                fillPath.lineTo(x, y)
            }
        }
        fillPath.lineTo(w, h)
        fillPath.close()

        linePaint.strokeWidth = strokeWidth
        linePaint.color = lineColor

        if (detailed) {
            fillPaint.shader = LinearGradient(
                0f, 0f, 0f, h,
                Color.argb(80, Color.red(lineColor), Color.green(lineColor), Color.blue(lineColor)),
                Color.argb(0, Color.red(lineColor), Color.green(lineColor), Color.blue(lineColor)),
                Shader.TileMode.CLAMP
            )
            canvas.drawPath(fillPath, fillPaint)
        } else {
            linePaint.alpha = 130
        }

        canvas.drawPath(linePath, linePaint)
        linePaint.alpha = 255
    }
}
