package com.jgm90.cloudmusic.widgets

import android.content.Context
import android.content.res.Resources
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import androidx.viewpager.widget.PagerAdapter
import androidx.viewpager.widget.ViewPager
import com.jgm90.cloudmusic.adapters.InfinitePagerAdapter

class CMViewPager @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : ViewPager(context, attrs), ViewPager.PageTransformer {
    private var maxScale = 0.0f
    private var pageMargin = 0
    private var animationEnabled = true
    private var fadeEnabled = false
    private var fadeFactor = 0.5f

    init {
        clipChildren = false
        clipToPadding = false
        overScrollMode = 2
        setPageTransformer(false, this)
        offscreenPageLimit = 3
        pageMargin = dp2px(context.resources, 50)
        setPadding(100, 50, 100, 50)
    }

    fun dp2px(resource: Resources, dp: Int): Int {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp.toFloat(), resource.displayMetrics).toInt()
    }

    override fun setAdapter(adapter: PagerAdapter?) {
        super.setAdapter(adapter)
        setCurrentItem(0)
    }

    override fun setCurrentItem(item: Int) {
        setCurrentItem(item, false)
    }

    override fun setCurrentItem(item: Int, smoothScroll: Boolean) {
        val currentAdapter = adapter
        if (currentAdapter == null || currentAdapter.count == 0) {
            super.setCurrentItem(item, smoothScroll)
            return
        }
        val offsetItem = getOffsetAmount() + (item % currentAdapter.count)
        super.setCurrentItem(offsetItem, smoothScroll)
    }

    override fun getCurrentItem(): Int {
        val currentAdapter = adapter
        if (currentAdapter == null || currentAdapter.count == 0) {
            return super.getCurrentItem()
        }
        val position = super.getCurrentItem()
        return if (currentAdapter is InfinitePagerAdapter) {
            position % currentAdapter.realCount
        } else {
            super.getCurrentItem()
        }
    }

    private fun getOffsetAmount(): Int {
        val currentAdapter = adapter
        if (currentAdapter == null || currentAdapter.count == 0) {
            return 0
        }
        return if (currentAdapter is InfinitePagerAdapter) {
            currentAdapter.realCount * 100
        } else {
            0
        }
    }

    fun setAnimationEnabled(enable: Boolean) {
        animationEnabled = enable
    }

    fun setFadeEnabled(fadeEnabled: Boolean) {
        this.fadeEnabled = fadeEnabled
    }

    fun setFadeFactor(fadeFactor: Float) {
        this.fadeFactor = fadeFactor
    }

    override fun setPageMargin(marginPixels: Int) {
        pageMargin = marginPixels
        setPadding(pageMargin, pageMargin, pageMargin, pageMargin)
    }

    override fun transformPage(page: View, position: Float) {
        if (pageMargin <= 0 || !animationEnabled) {
            return
        }
        page.setPadding(pageMargin / 3, pageMargin / 3, pageMargin / 3, pageMargin / 3)

        if (maxScale == 0.0f && position > 0.0f && position < 1.0f) {
            maxScale = position
        }
        val adjustedPosition = position - maxScale
        val absolutePosition = kotlin.math.abs(adjustedPosition)
        when {
            adjustedPosition <= -1.0f || adjustedPosition >= 1.0f -> {
                if (fadeEnabled) {
                    page.alpha = fadeFactor
                }
            }
            adjustedPosition == 0.0f -> {
                page.scaleX = 1 + maxScale
                page.scaleY = 1 + maxScale
                page.alpha = 1f
            }
            else -> {
                page.scaleX = 1 + maxScale * (1 - absolutePosition)
                page.scaleY = 1 + maxScale * (1 - absolutePosition)
                if (fadeEnabled) {
                    page.alpha = kotlin.math.max(fadeFactor, 1 - absolutePosition)
                }
            }
        }
    }

    companion object {
        const val TAG = "CMViewPager"
    }
}
