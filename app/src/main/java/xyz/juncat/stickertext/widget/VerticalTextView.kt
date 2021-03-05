package xyz.juncat.stickertext.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView

class VerticalTextView : AppCompatTextView {

    private val isVertical = true
    private val calMatrix = Matrix()
    private val tmpPoints = floatArrayOf(0f, 0f)
    private val symbols = setOf(
        ',', '.', '/', '<', '>', '?', ' ', '!', '{', '}',
        '，', '。', '？', '《', '》', '[', ']'
    )

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {

    }


    /*
        1.
     */
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
    }

    /*
        for (char in text) {
            when (char) {
                is chinese -> {
                    draw
                }
                is punctuation , is english -> {
                    (){}[]
                    rotation -> clockwise 90 degrees
                }
            }
        }
     */
    override fun onDraw(canvas: Canvas?) {
        if (canvas == null) return;
        if (isVertical) {
            drawVerticalText(canvas)
        } else {
            super.onDraw(canvas)
        }
    }

    private val pointPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL_AND_STROKE
        strokeWidth = 10f
    }

    private fun drawVerticalText(canvas: Canvas) {
        line = 0
        val startX = width - paddingRight
        val startY = paddingTop
        index = 0
        lastTextY = startY.toFloat()
        while (index < text.length) {
            val c = text[index];
            //https://en.wikipedia.org/wiki/Unicode_block
            when (val ucode = Character.UnicodeBlock.of(c)) {
                Character.UnicodeBlock.CYRILLIC,
                Character.UnicodeBlock.THAI,
                Character.UnicodeBlock.DEVANAGARI
                -> {
                    val eStart = index
                    while (text.getOrNull(index) != null && ucode == Character.UnicodeBlock.of(text[index])) {
                        index++
                    }
                    drawSequentText(eStart, startY, canvas)
                }
                else -> {
                    if (c in 'a'..'z' || c in 'A'..'Z') {
                        val eStart = index
                        while (text[index] in 'a'..'z' || text[index] in 'A'..'Z') {
                            index++
                        }
                        drawSequentText(eStart, startY, canvas)
                    } else {
                        drawCJK(startX, startY, c, canvas)
                    }
                }
            }
        }
    }


    private var lastTextY: Float = 0f
    private var line: Int = 0
    private var index = 0
    private fun drawCJK(startX: Int, startY: Int, c: Char, canvas: Canvas) {
        var textX = getTextX(line)
        var textY = if (c in symbols) {
            lastTextY + getSymbolHeight()
        } else {
            lastTextY + getTextHeight()
        }
        // draw in new line
        if (textY > height) {
            line++
            textX = getTextX(line)
            textY = if (c in symbols) {
                startY + getSymbolHeight()
            } else {
                startY + getTextHeight()
            }
        }
        lastTextY = textY
//                canvas.drawPoint(textX, textY, pointPaint)
        canvas.drawText(c.toString(), textX, textY, paint)
        index++
    }

    private fun drawSequentText(
        eStart: Int,
        startY: Int,
        canvas: Canvas
    ) {
        var textX = getTextX(line)
        //2. draw text
        var textY = lastTextY

        val eString = text.substring(
            eStart,
            if (text.getOrNull(index + 1) == null) {
                index
            } else {
                index + 1
            }
        )
        val eTextWidth = paint.measureText(eString)
        if (textY + eTextWidth > height) {
            line++
            textX = getTextX(line)
            textY = startY + paint.fontMetrics.bottom
        }
        lastTextY = textY + eTextWidth
        tmpPoints[0] = textX + paint.fontMetrics.descent
        tmpPoints[1] = textY
        canvas.drawPoint(tmpPoints[0], tmpPoints[1], pointPaint)
        canvas.save()
        canvas.translate(tmpPoints[0], tmpPoints[1])
        canvas.rotate(90f)
        canvas.drawText(eString, 0f, 0f, paint)
        canvas.restore()
        index++

    }


    private fun getTextX(line: Int): Float {
        return width - paddingEnd - line * (lineHeight) - getTextWidth()
    }

    private fun getTextHeight(): Float {
        return paint.fontMetrics.descent - paint.fontMetrics.ascent
    }

    private fun getTextWidth(): Float {
        return paint.measureText("正")
    }

    private fun getSymbolHeight(): Float {
        return paint.measureText("。")
    }
}