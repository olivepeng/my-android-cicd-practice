package com.miis.horusendoview.widget;

import android.content.Context;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.TextureView;
import androidx.annotation.FloatRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import kotlin.math.MathKt;
import timber.log.Timber;

public final class AutoFitTextureView extends TextureView {
    private float aspectRatio;

    public AutoFitTextureView(@NonNull Context context) {
        super(context);
    }

    public AutoFitTextureView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public AutoFitTextureView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public AutoFitTextureView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }


    public void setAspectRatio(float aspectRatio) {
        boolean var2 = aspectRatio > (float)0;
        if (var2) {
            this.aspectRatio = aspectRatio;
            this.requestLayout();
        }
    }

    public void configureTextureViewTransform(@FloatRange(from = 1.0) float zoom) {
        RectF viewRect = new RectF(0.0F, 0.0F, (float)this.getWidth(), (float)this.getHeight());
        float centerX = viewRect.centerX();
        float centerY = viewRect.centerY();
        Matrix matrix = new Matrix();
        matrix.postScale(zoom, zoom, centerX, centerY);
        this.setTransform(matrix);
    }

    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);
        if (this.aspectRatio == 0.0F) {
            this.setMeasuredDimension(width, height);
        } else {
            int newWidth = 0;
            int newHeight = 0;
            float actualRatio = width > height ? this.aspectRatio : 1.0F / this.aspectRatio;
            if ((float)width < (float)height * actualRatio) {
                newHeight = height;
                newWidth = MathKt.roundToInt((float)height * actualRatio);
            } else {
                newWidth = width;
                newHeight = MathKt.roundToInt((float)width / actualRatio);
            }

            Timber.d("Measured dimensions set: " + newWidth + " x " + newHeight);
            this.setMeasuredDimension(newWidth, newHeight);
        }

    }
}

