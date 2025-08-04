package com.example.dermascanai

import android.graphics.Canvas
import android.graphics.Paint
import android.text.style.LineBackgroundSpan

class CircleSpan(
    private val color: Int,
    private val radius: Float = 30f // You can adjust the size
) : LineBackgroundSpan {

    override fun drawBackground(
        canvas: Canvas,
        paint: Paint,
        left: Int,
        right: Int,
        top: Int,
        baseline: Int,
        bottom: Int,
        text: CharSequence,
        start: Int,
        end: Int,
        lineNumber: Int
    ) {
        val oldColor = paint.color
        val oldStyle = paint.style
        val oldStroke = paint.strokeWidth

        val centerX = (left + right) / 2f
        val centerY = (top + bottom) / 2f

        paint.color = color
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 4f
        canvas.drawCircle(centerX, centerY, radius, paint)

        // Restore paint state
        paint.color = oldColor
        paint.style = oldStyle
        paint.strokeWidth = oldStroke
    }
}
