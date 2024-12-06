package com.capztone.fishfy.ui.activities.Utils

import android.content.Context
import android.graphics.Canvas
import android.text.Layout
import android.text.StaticLayout
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView

class JustifiedTextView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : AppCompatTextView(context, attrs, defStyleAttr) {

    override fun onDraw(canvas: Canvas) {
        val text = text.toString()
        val width = measuredWidth - paddingLeft - paddingRight
        val layout = StaticLayout(text, paint, width, Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false)

        for (i in 0 until layout.lineCount) {
            val lineStart = layout.getLineStart(i)
            val lineEnd = layout.getLineEnd(i)
            val lineText = text.substring(lineStart, lineEnd)
            val lineWidth = paint.measureText(lineText)

            if (i == layout.lineCount - 1 || layout.getLineEnd(i) == text.length) {
                canvas?.drawText(lineText, paddingLeft.toFloat(), layout.getLineBaseline(i).toFloat(), paint)
            } else {
                val spaceWidth = (width - lineWidth) / (lineText.length - 1)
                var x = paddingLeft.toFloat()
                for (j in lineText.indices) {
                    val c = lineText[j]
                    canvas?.drawText(c.toString(), x, layout.getLineBaseline(i).toFloat(), paint)
                    x += paint.measureText(c.toString()) + spaceWidth
                }
            }
        }
    }
}
