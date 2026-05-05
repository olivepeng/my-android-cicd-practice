package com.app.customkeyboard;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.widget.RelativeLayout;

import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;

import com.app.customkeyboard.utilities.ComponentUtils;

/**
 * Created by Don.Brody on 7/18/18.
 */
public abstract class ResizableRelativeLayout extends RelativeLayout {

    public ResizableRelativeLayout(@NonNull Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public int toPx(int dp) {
        return ComponentUtils.dpToPx(getContext(), dp);
    }

    public int toDp(int px) {
        return ComponentUtils.pxToDp(getContext(), px);
    }

    @Override
    protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
        super.onSizeChanged(width, height, oldWidth, oldHeight);
        resetContent();
    }

    @CallSuper
    protected void resetContent() {
        removeAllViews();
        // Adding a delay gives the parent activity time to handle its configuration change
        // prior to us handling ours. Otherwise, we run into several issues, including the
        // screen size property of our parent window not updating prior to us accessing it.
        new Handler(Looper.getMainLooper()).postDelayed(this::configureSelf, 50);
    }

    protected abstract void configureSelf();
}
