package com.jgm90.cloudmusic.widgets

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable
import android.util.Log
import android.util.Property
import android.view.animation.DecelerateInterpolator

class PlayPauseDrawable : Drawable() {
    private val leftPauseBar = Path()
    private val rightPauseBar = Path()
    private val paint = Paint()
    private var progress = 0f
    private var isPlay = false
    private var animator: Animator? = null

    init {
        paint.isAntiAlias = true
        paint.style = Paint.Style.FILL
        paint.color = Color.WHITE
    }

    override fun draw(canvas: Canvas) {
        val startDraw = System.currentTimeMillis()

        leftPauseBar.rewind()
        rightPauseBar.rewind()

        canvas.translate(bounds.left.toFloat(), bounds.top.toFloat())

        val pauseBarHeight = 7.0f / 12.0f * bounds.height().toFloat()
        val pauseBarWidth = pauseBarHeight / 3.0f
        val pauseBarDistance = pauseBarHeight / 3.6f

        val barDist = interpolate(pauseBarDistance, 0.0f, progress)
        val barWidth = interpolate(pauseBarWidth, pauseBarHeight / 1.75f, progress)
        val firstBarTopLeft = interpolate(0.0f, barWidth, progress)
        val secondBarTopRight = interpolate(2.0f * barWidth + barDist, barWidth + barDist, progress)

        leftPauseBar.moveTo(0.0f, 0.0f)
        leftPauseBar.lineTo(firstBarTopLeft, -pauseBarHeight)
        leftPauseBar.lineTo(barWidth, -pauseBarHeight)
        leftPauseBar.lineTo(barWidth, 0.0f)
        leftPauseBar.close()

        rightPauseBar.moveTo(barWidth + barDist, 0.0f)
        rightPauseBar.lineTo(barWidth + barDist, -pauseBarHeight)
        rightPauseBar.lineTo(secondBarTopRight, -pauseBarHeight)
        rightPauseBar.lineTo(2.0f * barWidth + barDist, 0.0f)
        rightPauseBar.close()

        canvas.save()

        canvas.translate(interpolate(0.0f, pauseBarHeight / 8.0f, progress), 0.0f)

        val rotationProgress = if (isPlay) 1.0f - progress else progress
        val startingRotation = if (isPlay) 90.0f else 0.0f
        canvas.rotate(
            interpolate(startingRotation, startingRotation + 90.0f, rotationProgress),
            bounds.width() / 2.0f,
            bounds.height() / 2.0f,
        )

        canvas.translate(
            bounds.width() / 2.0f - ((2.0f * barWidth + barDist) / 2.0f),
            bounds.height() / 2.0f + (pauseBarHeight / 2.0f),
        )

        canvas.drawPath(leftPauseBar, paint)
        canvas.drawPath(rightPauseBar, paint)

        canvas.restore()

        val timeElapsed = System.currentTimeMillis() - startDraw
        if (timeElapsed > 16) {
            Log.e(TAG, "Drawing took too long=$timeElapsed")
        }
    }

    fun transformToPause(animated: Boolean) {
        if (isPlay) {
            if (animated) {
                toggle()
            } else {
                isPlay = false
                setProgress(0.0f)
            }
        }
    }

    override fun jumpToCurrentState() {
        Log.v(TAG, "jumpToCurrentState()")
        animator?.cancel()
        setProgress(if (isPlay) 1.0f else 0.0f)
    }

    fun transformToPlay(animated: Boolean) {
        if (!isPlay) {
            if (animated) {
                toggle()
            } else {
                isPlay = true
                setProgress(1.0f)
            }
        }
    }

    private fun toggle() {
        animator?.cancel()

        animator = ObjectAnimator.ofFloat(
            this,
            PROGRESS,
            if (isPlay) 1.0f else 0.0f,
            if (isPlay) 0.0f else 1.0f,
        ).apply {
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    isPlay = !isPlay
                }
            })
            interpolator = DecelerateInterpolator()
            duration = 200
            start()
        }
    }

    private fun getProgress(): Float = progress

    private fun setProgress(progress: Float) {
        this.progress = progress
        invalidateSelf()
    }

    override fun setAlpha(alpha: Int) {
        paint.alpha = alpha
        invalidateSelf()
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        paint.colorFilter = colorFilter
        invalidateSelf()
    }

    @Deprecated("Deprecated in Java")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT

    companion object {
        private val TAG = PlayPauseDrawable::class.java.simpleName

        private val PROGRESS: Property<PlayPauseDrawable, Float> =
            object : Property<PlayPauseDrawable, Float>(Float::class.java, "progress") {
                override fun get(`object`: PlayPauseDrawable): Float = `object`.getProgress()

                override fun set(`object`: PlayPauseDrawable, value: Float) {
                    `object`.setProgress(value)
                }
            }

        private fun interpolate(a: Float, b: Float, t: Float): Float {
            return a + (b - a) * t
        }
    }
}
