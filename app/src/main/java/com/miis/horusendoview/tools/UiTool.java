package com.miis.horusendoview.tools;

import android.content.Context;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class UiTool {

    /**
     * Convert dp to px
     */
    public static float convertDpToPixel(float dp, Context context) {
        return dp * getDensity(context);
    }

    /**
     * Get screen density
     * 120dpi = 0.75
     * 160dpi = 1 (default)
     * 240dpi = 1.5
     */
    public static float getDensity(Context context) {
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        return metrics.density;
    }

    /**
     * Get screen size
     */
    public static DisplayMetrics getDisplaySize(Context context) {
        return context.getResources().getDisplayMetrics();
    }

    @Nullable
    public static ViewGroup getParentView(@NonNull View view) {
        ViewGroup parentView = null;
        if (view.getParent() != null && view.getParent() instanceof ViewGroup) {
            parentView = (ViewGroup) view.getParent();
        }
        return parentView;
    }
}

