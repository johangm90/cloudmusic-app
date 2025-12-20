package com.jgm90.cloudmusic.core.ui.animation

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.view.View

object AnimationUtils {
    @JvmStatic
    fun animColorChange(view: View, color1: Int, color2: Int) {
        val anim = ValueAnimator()
        anim.setIntValues(color1, color2)
        anim.setEvaluator(ArgbEvaluator())
        anim.addUpdateListener { valueAnimator ->
            view.setBackgroundColor(valueAnimator.animatedValue as Int)
        }
        anim.duration = 300
        anim.start()
    }
}
