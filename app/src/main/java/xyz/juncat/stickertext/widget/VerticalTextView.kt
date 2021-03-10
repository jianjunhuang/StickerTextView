package xyz.juncat.stickertext.widget

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.util.AttributeSet
import android.util.Log
import android.view.Gravity
import androidx.appcompat.widget.AppCompatTextView
import xyz.juncat.stickertext.R
import java.util.*
import kotlin.collections.ArrayList
import kotlin.text.StringBuilder


/**
 * todo fix the messy demo
 */
class VerticalTextView : AppCompatTextView {

    var isVertical = true
    private val calMatrix = Matrix()
    private val tmpPoints = floatArrayOf(0f, 0f)
    private val symbols = setOf(
        ',', '.', '/', '<', '>', '?', ' ', '!', '{', '}',
        '，', '。', '？', '《', '》', '[', ']'
    )

    private val lineItems = ArrayList<VerticalLine>()
    private var lastX = 0f

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        setText(R.string.test_string)
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
        val start = System.nanoTime()
        if (canvas == null) return
        if (isVertical) {
            measureVerticalText(canvas)
            dawVerticalText(canvas)
        } else {
            super.onDraw(canvas)
        }
        Log.i(TAG, "onDraw:${(System.nanoTime() - start) / 1000000f}")
    }

    private fun dawVerticalText(canvas: Canvas) {
        lineItems.forEach {
            val lastItemY = it.snippets.lastOrNull()?.lastY ?: height.toFloat()
            when (gravity and Gravity.HORIZONTAL_GRAVITY_MASK) {
                Gravity.RIGHT -> {
                    canvas.save()
                    canvas.translate(0f, height - lastItemY - paint.fontMetrics.descent)
                }
                Gravity.CENTER_HORIZONTAL -> {
                    canvas.save()
                    canvas.translate(0f, (height - lastItemY) / 2f - paint.fontMetrics.descent)
                }
            }
            it.snippets.forEach { snippet ->
                when (snippet.type) {
                    TYPE_CJK -> {
                        canvas.drawText(snippet.text, snippet.x, snippet.y, paint)
                    }
                    else -> {
                        canvas.save()
                        canvas.translate(snippet.x, snippet.y)
                        canvas.rotate(90f)
                        canvas.drawText(snippet.text, 0f, 0f, paint)
                        canvas.restore()
                    }
                }
            }
            when (gravity and Gravity.HORIZONTAL_GRAVITY_MASK) {
                Gravity.RIGHT -> {
                    canvas.restore()
                }
                Gravity.CENTER_HORIZONTAL -> {
                    canvas.restore()
                }
            }


        }

    }

    private fun measureVerticalText(canvas: Canvas) {
        lineItems.clear()
        line = 0
        val startX = width - paddingRight
        val startY = paddingTop
        index = 0
        lastTextY = startY.toFloat()
        //measure text
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
                    measureSequentText(eStart, startY, canvas)
                }
                else -> {
                    if (c in 'a'..'z' || c in 'A'..'Z') {
                        val eStart = index
                        while (text.getOrNull(index) != null && (text[index] in 'a'..'z' || text[index] in 'A'..'Z')) {
                            index++
                        }
                        measureSequentText(eStart, startY, canvas)
                    } else if (c.isSurrogate()) {
                        val eStart = index
                        while (text.getOrNull(index) != null && (text[index].isSurrogate())) {
                            index++
                        }
                        measureUnicode(eStart, startY)
                    } else {
                        if (c in symbols) {
                            measureSymbols(canvas, c, startY)
                        } else {
                            measureCJK(startX, startY, c, canvas)
                        }
                    }
                }
            }
        }
    }


    private val strBuilder = StringBuilder()
    private fun measureUnicode(eStart: Int, startY: Int) {
        val emojiSize = (paint.fontMetrics.descent - paint.fontMetrics.ascent)
        strBuilder.clear()
        for (i in eStart until index) {
            strBuilder.append(text[i])
        }
        val uString = strBuilder.toString()
        val uWidth = paint.measureText(uString)
        var textY = lastTextY + emojiSize
        var textX = getTextX(line) - uWidth / 6f
        // draw in new line
        if (textY > height) {
            line++
            textX = getTextX(line) - emojiSize / 6f
            textY = startY + emojiSize
        }
        lastTextY = textY + paint.fontMetrics.descent
        lastX = textX
        val snippet =
            Snippets(
                TYPE_CJK,
                textX,
                textY,
                emojiSize,
                emojiSize,
                strBuilder.toString(),
                lastTextY
            )
        addSnippet(snippet)
    }

    private var lastTextY: Float = 0f
    private var line: Int = 0
    private var index = 0
    private fun measureCJK(startX: Int, startY: Int, c: Char, canvas: Canvas) {
        var textX = getTextX(line)
        var textY = lastTextY + getTextHeight()
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
        lastX = textX
        val snippet =
            Snippets(
                TYPE_CJK,
                textX,
                textY,
                getTextWidth(),
                getTextHeight(),
                c.toString(),
                lastTextY
            )
        addSnippet(snippet)
        index++
    }

    private fun measureSequentText(
        eStart: Int,
        startY: Int,
        canvas: Canvas
    ) {
        var textX = getTextX(line)
        //2. draw text
        var textY = lastTextY

        val eString = text.substring(
            eStart,
            index
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
        val snippet = Snippets(
            TYPE_SEQ, tmpPoints[0], tmpPoints[1], eTextWidth, getTextHeight(), eString, lastTextY
        )
        lastX = textX

        addSnippet(snippet)
    }

    private fun addSnippet(snippet: Snippets) {
        lineItems.getOrElse(line) { i ->
            val item = VerticalLine(0, 0f, ArrayList<Snippets>())
            lineItems.add(item)
            return@getOrElse item
        }.let {
            it.snippets.add(snippet)
        }
    }

    private fun measureSymbols(canvas: Canvas, c: Char, startY: Int) {
        var textX = getTextX(line)
        var textY = lastTextY + paint.fontMetrics.bottom

        // draw in new line
        if (textY > height) {
            line++
            textX = getTextX(line)
            textY = startY + paint.fontMetrics.bottom
        }
        textX += paint.fontMetrics.descent
        lastTextY = if (c == ' ') {
            textY + paint.measureText(" ")
        } else {
            textY + getSymbolHeight()
        }
        lastX = textX
        val snippet =
            Snippets(
                TYPE_SYMBOS,
                textX,
                textY,
                getTextWidth(),
                getSymbolHeight(),
                c.toString(),
                lastTextY
            )
        addSnippet(snippet)
        index++
    }

    private fun getTextX(line: Int): Float {
        return width - paddingEnd - line * (lineHeight) - getTextWidth()
    }

    private fun getTextHeight(): Float {
        val size = paint.fontMetrics.descent - paint.fontMetrics.ascent

        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            size + size * letterSpacing
        } else {
            return size
        }
    }

    private fun getTextWidth(): Float {
        return paint.measureText("正")
    }

    private fun getSymbolHeight(): Float {
        return paint.measureText("。")
    }

    override fun setTextColor(color: Int) {
        super.setTextColor(color)
        paint.color = color
    }

    override fun setTextColor(colors: ColorStateList?) {
        super.setTextColor(colors)
        colors?.getColorForState(drawableState, 0)?.let {
            paint.color = it
        }
    }

    companion object {
        private const val TAG = "VerticalTextView"
        private const val TYPE_SEQ = 0
        private const val TYPE_CJK = 1
        private const val TYPE_SYMBOS = 2
        private const val TYPE_EMOJI = 3
    }

    class VerticalLine(var x: Int, var height: Float, val snippets: ArrayList<Snippets>)

    class Snippets(
        val type: Int,
        val x: Float,
        val y: Float,
        val width: Float,
        val height: Float,
        val text: String,
        val lastY: Float
    )

}