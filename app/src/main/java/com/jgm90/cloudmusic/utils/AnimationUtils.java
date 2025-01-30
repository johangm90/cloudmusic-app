package com.jgm90.cloudmusic.utils;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.view.View;

public class AnimationUtils {

    public static void animColorChange(final View view, int color1, int color2) {
        ValueAnimator anim = new ValueAnimator();
        anim.setIntValues(color1, color2);
        anim.setEvaluator(new ArgbEvaluator());
        anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                view.setBackgroundColor((Integer) valueAnimator.getAnimatedValue());
            }
        });
        anim.setDuration(300);
        anim.start();
    }
}
