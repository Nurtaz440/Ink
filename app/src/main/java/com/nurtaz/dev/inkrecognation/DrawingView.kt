package com.nurtaz.dev.inkrecognation

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.text.TextPaint
import android.util.AttributeSet
import android.util.Log
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import com.google.mlkit.vision.digitalink.Ink
import com.google.mlkit.vision.digitalink.Ink.Stroke

class DrawingView @JvmOverloads constructor(
    context: Context?,
    attributeSet: AttributeSet? = null
) : View(context,attributeSet),StrokeManager.ContentChangedListener {
    private val recognizedStrokePaint: Paint
    private val textPaint: TextPaint
    private val currentStrokePaint: Paint
    private val canvasPaint: Paint
    private val currentStroke: Path
    private lateinit var drawCanvas: Canvas
    private lateinit var canvasBitmap: Bitmap
    private lateinit var strokeManager: StrokeManager

    fun setStrokeManager(strokeManager: StrokeManager) {
        this.strokeManager = strokeManager
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        Log.i("TAG", "onsizeChanged")
        canvasBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        drawCanvas = Canvas(canvasBitmap)
        invalidate()
    }

    fun reDrawContent() {
        clear()
        val currentInk = strokeManager.currentInk
        drawInk(currentInk, currentStrokePaint)
        val content = strokeManager.getContent()
        for (ri in content) {
            drawInk(ri.ink, recognizedStrokePaint)
            val bb = computeBoundingBox(ri.ink)
            drawTextIntoBoundingBox(ri.text ?: "", bb, textPaint)
        }
        invalidate()
    }

    private fun drawTextIntoBoundingBox(text: String, bb: Rect, textPaint: TextPaint) {
        val arbitaryFixedSize = 20f
        //set an arbitary the size to learn how high the text will be
        textPaint.textSize = arbitaryFixedSize
        textPaint.textScaleX = 1f

        //now determine the size of the rendered text with these settings
        val r = Rect()
        textPaint.getTextBounds(text, 0, text.length, r)

        //adjust height such that target height is met
        val textSize = arbitaryFixedSize * bb.height().toFloat() / r.height().toFloat()
        textPaint.textSize = textSize


        //redermine the size that rendered text with the new settings
        textPaint.getTextBounds(text, 0, text.length, r)

        //adjust scaleX to squeze the text
        textPaint.textScaleX = bb.width().toFloat() / r.width().toFloat()

        //and finaly draw the text
        drawCanvas.drawText(text, bb.left.toFloat(), bb.bottom.toFloat(), textPaint)

    }

    private fun drawInk(ink: Ink, paint: Paint) {
        for (s in ink.strokes) {
            drawStroke(s, paint)
        }
    }

    private fun drawStroke(s: Stroke, paint: Paint) {
        Log.i("TAG", "drawStroke")
        var path = Path()
        path.moveTo(s.points[0].x, s.points[0].y)
        for (p in s.points.drop(1)) {
            path.lineTo(p.x, p.y)
        }
        drawCanvas.drawPath(path, paint)
    }

    fun clear() {
        currentStroke.reset()
        onSizeChanged(
            canvasBitmap.width,
            canvasBitmap.height,
            canvasBitmap.width,
            canvasBitmap.height
        )
    }

    override fun onDraw(canvas: Canvas) {
        canvas.drawBitmap(canvasBitmap, 0f, 0f, canvasPaint)
        canvas.drawPath(currentStroke, currentStrokePaint)
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        val action = event!!.actionMasked
        val x = event.x
        val y = event.y
        when (action) {
            MotionEvent.ACTION_DOWN -> currentStroke.moveTo(x, y)
            MotionEvent.ACTION_MOVE -> currentStroke.lineTo(x, y)
            MotionEvent.ACTION_UP -> {
                currentStroke.lineTo(x, y)
                drawCanvas.drawPath(currentStroke, currentStrokePaint)
                currentStroke.reset()
            }

            else -> {

            }
        }
        strokeManager.addNewTouchEvent(event)
        invalidate()
        return true
    }

    override fun onContentChanged() {
        reDrawContent()
    }

    companion object {
        private const val STROKE_WIDTH_DP = 3
        private const val MIN_BB_WIDTH = 10
        private const val MIN_BB_HEIGHT = 10
        private const val MAX_BB_WIDTH = 256
        private const val MAX_BB_HEIGHT = 256

        private fun computeBoundingBox(ink: Ink): Rect {
            var top = Float.MAX_VALUE
            var left = Float.MAX_VALUE
            var bottom = Float.MIN_VALUE
            var right = Float.MIN_VALUE

            for (s in ink.strokes) {
                for (p in s.points) {
                    top = Math.min(top, p.y)
                    left = Math.min(left, p.x)
                    bottom = Math.max(bottom, p.y)
                    right = Math.max(right, p.x)
                }
            }

            val centerX = (left + right) / 2
            val centerY = (top + bottom) / 2

            val bb = Rect(left.toInt(), top.toInt(), right.toInt(), bottom.toInt())

            bb.union(
                (centerX - MIN_BB_WIDTH / 2).toInt(),
                (centerY - MIN_BB_HEIGHT / 2).toInt(),
                (centerX + MIN_BB_WIDTH / 2).toInt(),
                (centerX + MIN_BB_WIDTH / 2).toInt()
            )

            if (bb.width() > MAX_BB_WIDTH) {
                bb[bb.centerX() - MAX_BB_WIDTH / 2, bb.top, bb.centerX() + MAX_BB_WIDTH / 2] =
                    bb.bottom
            }
            if (bb.height() > MAX_BB_HEIGHT) {
                bb[bb.left, bb.centerY() - MAX_BB_HEIGHT / 2, bb.right] =
                    bb.centerY() + MAX_BB_HEIGHT / 2
            }
            return bb
        }
    }
    init {
        currentStrokePaint = Paint()
        currentStrokePaint.color = -0xff01
        currentStrokePaint.isAntiAlias = true

        currentStrokePaint.strokeWidth =
            TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                STROKE_WIDTH_DP.toFloat(),
                resources.displayMetrics
            )
        currentStrokePaint.style = Paint.Style.STROKE
        currentStrokePaint.strokeJoin = Paint.Join.ROUND
        currentStrokePaint.strokeCap = Paint.Cap.ROUND
        recognizedStrokePaint = Paint(currentStrokePaint)
        recognizedStrokePaint.color = -0x3301
        textPaint = TextPaint()
        textPaint.color = -0xcc33cd
        currentStroke = Path()
        canvasPaint = Paint(Paint.DITHER_FLAG)

    }
}