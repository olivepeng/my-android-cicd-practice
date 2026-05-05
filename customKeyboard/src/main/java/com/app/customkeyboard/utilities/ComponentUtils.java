package com.app.customkeyboard.utilities;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.text.InputFilter;
import android.util.TypedValue;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.core.graphics.drawable.DrawableCompat;

/**
 * Created by Don.Brody on 7/20/18.
 */
public class ComponentUtils {
    public static final int DEFAULT_COMPONENT_HEIGHT_DP = 80;

    public static void hideSystemKeyboard(@NonNull Context context, @NonNull View view) {
        if (view.getWindowToken() != null) {
            InputMethodManager imm = (InputMethodManager) context.getSystemService(Activity.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    public static void setBackgroundTint(@NonNull View view, int color) {
        Drawable drawable = DrawableCompat.wrap(view.getBackground());
        DrawableCompat.setTint(drawable, color);
    }

    public static void configureTextField(@NonNull EditText field, boolean singleLine, int maxChars) {
        if (singleLine) {
            field.setMaxLines(1);
            field.setSingleLine(true);
        }
        field.setFilters(new InputFilter[]{new InputFilter.LengthFilter(maxChars)});
    }

    public static int dpToPx(@NonNull Context context, int dp) {
        float px = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dp,
                context.getResources().getDisplayMetrics());

        float density = context.getResources().getDisplayMetrics().density;
        return (int) (px / density);
    }

    public static int pxToDp(@NonNull Context context, int px) {
        float dp = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_PX,
                px,
                context.getResources().getDisplayMetrics());

        float density = context.getResources().getDisplayMetrics().density;
        return (int) (dp * density);
    }
}
