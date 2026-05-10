package com.booxdemo

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.util.Log
import android.view.ViewTreeObserver
import com.onyx.android.sdk.data.note.TouchPoint
import com.onyx.android.sdk.pen.RawInputCallback
import com.onyx.android.sdk.pen.TouchHelper
import com.onyx.android.sdk.pen.data.TouchPointList

class DrawingView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    companion object {
        private const val TAG = "BOOXDemo"
    }

    private var renderBitmap: Bitmap? = null
    private var renderCanvas: Canvas? = null

    private val strokePaint = Paint().apply {
        isAntiAlias = true
        color = Color.BLACK
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        strokeWidth = 3f
    }

    private val touchHelper: TouchHelper by lazy { TouchHelper.create(this, rawInputCallback) }

    private var isSetup = false

    // -------------------------------------------------------------------------
    // Raw input callback — fired by the Onyx pen SDK
    // -------------------------------------------------------------------------

    private val rawInputCallback = object : RawInputCallback() {

        override fun onBeginRawDrawing(shortcutDrawing: Boolean, touchPoint: TouchPoint) {
            Log.d(TAG, "onBeginRawDrawing x=${touchPoint.x} y=${touchPoint.y}")
        }

        override fun onEndRawDrawing(shortcutDrawing: Boolean, touchPoint: TouchPoint) {
            Log.d(TAG, "onEndRawDrawing x=${touchPoint.x} y=${touchPoint.y}")
        }

        override fun onRawDrawingTouchPointMoveReceived(touchPoint: TouchPoint) {}

        override fun onRawDrawingTouchPointListReceived(pointList: TouchPointList) {
            Log.d(TAG, "onPointList count=${pointList.size()}")
            renderStroke(pointList)
        }

        override fun onBeginRawErasing(shortcutErasing: Boolean, touchPoint: TouchPoint) {}

        override fun onEndRawErasing(shortcutErasing: Boolean, touchPoint: TouchPoint) {}

        override fun onRawErasingTouchPointMoveReceived(touchPoint: TouchPoint) {}

        override fun onRawErasingTouchPointListReceived(pointList: TouchPointList) {}
    }

    // -------------------------------------------------------------------------
    // Stroke rendering — bitmap stays in sync; no overlay toggle during drawing
    // -------------------------------------------------------------------------

    private fun renderStroke(pointList: TouchPointList) {
        val canvas = renderCanvas ?: return
        val points = pointList.points
        if (points.isNullOrEmpty()) return

        val path = Path()
        path.moveTo(points[0].x, points[0].y)
        for (i in 1 until points.size) {
            path.lineTo(points[i].x, points[i].y)
        }
        canvas.drawPath(path, strokePaint)
        // No toggle here — the hardware overlay already shows the stroke in real time.
        // The bitmap is kept current silently; it surfaces via onDraw when the overlay resets.
    }

    // Toggle used only for clear and focus transitions, not during active drawing.
    private fun commitToScreen() {
        post {
            touchHelper.setRawDrawingEnabled(false)
            invalidate()
            post { touchHelper.setRawDrawingEnabled(true) }
        }
    }

    // -------------------------------------------------------------------------
    // TouchHelper lifecycle
    // -------------------------------------------------------------------------

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                if (width > 0 && height > 0) {
                    viewTreeObserver.removeOnGlobalLayoutListener(this)
                    Log.d(TAG, "onGlobalLayout: view=${width}x${height} calling openRawDrawing")
                    openRawDrawing()
                }
            }
        })
    }

    override fun onWindowFocusChanged(hasWindowFocus: Boolean) {
        super.onWindowFocusChanged(hasWindowFocus)
        Log.d(TAG, "onWindowFocusChanged hasFocus=$hasWindowFocus size=${width}x${height} isSetup=$isSetup")
        if (hasWindowFocus) {
            if (width > 0 && height > 0) {
                openRawDrawing()
                // Overlay was cleared by restartRawDrawing; show the bitmap so prior strokes reappear.
                invalidate()
            }
        } else {
            if (isSetup) touchHelper.setRawDrawingEnabled(false)
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w == 0 || h == 0) return
        initBitmap(w, h)
    }

    private fun initBitmap(w: Int, h: Int) {
        renderBitmap?.recycle()
        renderBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888).also {
            renderCanvas = Canvas(it)
            renderCanvas!!.drawColor(Color.WHITE)
        }
    }

    private fun openRawDrawing() {
        val loc = IntArray(2)
        getLocationOnScreen(loc)
        val screenRect = Rect(loc[0], loc[1], loc[0] + width, loc[1] + height)
        Log.d(TAG, "openRawDrawing isSetup=$isSetup screenRect=$screenRect viewSize=${width}x${height}")

        if (!isSetup) {
            touchHelper
                .setLimitRect(screenRect, emptyList())
                .setStrokeWidth(3.0f)
                .openRawDrawing()
            isSetup = true
        } else {
            touchHelper.setLimitRect(screenRect, emptyList())
            touchHelper.restartRawDrawing()
        }
        touchHelper.setRawDrawingEnabled(true)
        Log.d(TAG, "openRawDrawing done — rawInputEnabled=${touchHelper.isRawDrawingInputEnabled} rawRenderEnabled=${touchHelper.isRawDrawingRenderEnabled}")
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        return if (isSetup) touchHelper.onTouchEvent(event) else super.onTouchEvent(event)
    }

    fun enable() {
        if (isSetup) touchHelper.setRawDrawingEnabled(true)
    }

    fun disable() {
        if (isSetup) touchHelper.setRawDrawingEnabled(false)
    }

    fun clearCanvas() {
        renderCanvas?.drawColor(Color.WHITE)
        if (isSetup) commitToScreen() else invalidate()
    }

    // -------------------------------------------------------------------------
    // Draw
    // -------------------------------------------------------------------------

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(Color.WHITE)
        renderBitmap?.let { canvas.drawBitmap(it, 0f, 0f, null) }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        if (isSetup) {
            touchHelper.closeRawDrawing()
            isSetup = false
        }
    }
}
