package com.miis.horusendoview.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;

import androidx.appcompat.widget.AppCompatSeekBar;

public class RulerSeekBar extends AppCompatSeekBar {

    /**
     * 刻度线画笔
     */
    private final Paint rulerPaint = new Paint();

    /**
     * 每条刻度线的宽度 单位(px)
     */
    private int rulerWidth = 2;

    /**
     * 刻度线的颜色
     */
    private int rulerColor = Color.BLACK;

    /**
     * 滑块上是否要显示刻度线
     */
    private boolean isShowTopOfThumb = false;

    public RulerSeekBar(Context context) {
        this(context, null);
    }

    public RulerSeekBar(Context context, AttributeSet attrs) {
        this(context, attrs, androidx.appcompat.R.attr.seekBarStyle);
    }

    public RulerSeekBar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        // 创建绘制刻度线的画笔
        rulerPaint.setColor(rulerColor);
        rulerPaint.setAntiAlias(true);

        // 在API 21及以上版本中，去掉滑块后面的背景
        setSplitTrack(false);
    }

    /**
     * 设置刻度线的宽度
     *
     * @param rulerWidth 刻度线宽度，单位px
     */
    public void setRulerWidth(int rulerWidth) {
        this.rulerWidth = rulerWidth;
        requestLayout();
        invalidate();
    }

    /**
     * 设置刻度线的颜色
     *
     * @param rulerColor 刻度线颜色
     */
    public void setRulerColor(int rulerColor) {
        this.rulerColor = rulerColor;
        rulerPaint.setColor(rulerColor);
        requestLayout();
        invalidate();
    }

    /**
     * 设置滑块上是否显示刻度线
     *
     * @param showTopOfThumb 是否显示刻度线
     */
    public void setShowTopOfThumb(boolean showTopOfThumb) {
        isShowTopOfThumb = showTopOfThumb;
        requestLayout();
        invalidate();
    }

    @Override
    protected synchronized void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // 极限条件校验
        if (getWidth() <= 0) {
            return;
        }

        int rulerCount = 0; // 刻度线数量

        // 获取每一份的长度
        float length = 0f;
//        if (rulerCount > 0) {
//            length = (getWidth() - getPaddingLeft() - getPaddingRight()) / ((float) rulerCount);
//        } else {
            length = (getWidth() - getPaddingLeft() - getPaddingRight());
//        }

        // 计算刻度线的顶部座标和底部座标
        float rulerTop = getHeight() / 2f - getProgressDrawable().getIntrinsicHeight() / 2f;
        float rulerBottom = getHeight() / 2f + getProgressDrawable().getIntrinsicHeight() / 2f;

        // 获取滑块的位置信息
        Rect thumbRect = null;
        if (getThumb() != null) {
            thumbRect = getThumb().getBounds();
        }

        // 绘制刻度线
        for (int i = 1; i < rulerCount; i++) {
            // 计算刻度线的左边座标和右边座标
            float rulerLeft = i * length + getPaddingLeft();
            float rulerRight = rulerLeft + rulerWidth;

            // 判断是否需要绘制刻度线
            if (!isShowTopOfThumb &&
                    thumbRect != null &&
                    rulerLeft - getPaddingLeft() > thumbRect.left - (thumbRect.width() / 2f) &&
                    rulerRight - getPaddingLeft() < thumbRect.right - (thumbRect.width() / 2f)) {
                continue;
            }

            // 进行绘制
            canvas.drawRect(rulerLeft, rulerTop, rulerRight, rulerBottom, rulerPaint);
        }
    }
}

