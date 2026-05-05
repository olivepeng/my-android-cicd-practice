package com.app.customkeyboard.textFields;

import android.content.Context;
import android.graphics.Color;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatEditText;

import com.app.customkeyboard.keyboard.CustomKeyboardView;
import com.app.customkeyboard.utilities.ComponentUtils;

/**
 * Created by Don.Brody on 11/10/18.
 */
public class CustomTextField extends AppCompatEditText {

    @NonNull
    private CustomKeyboardView.KeyboardType keyboardType =
            CustomKeyboardView.KeyboardType.QWERTY;

    public static final float DEFAULT_TEXT_SIZE = 18.0f;
    public static final int DEFAULT_MAX_CHARS = 25;

    public CustomTextField(@NonNull Context context) {
        super(context);

        ViewGroup.LayoutParams layoutParams = new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ComponentUtils.dpToPx(context, ComponentUtils.DEFAULT_COMPONENT_HEIGHT_DP)
        );
        setLayoutParams(layoutParams);

        int pad = ComponentUtils.dpToPx(context, 15);
        setPadding(pad, pad, pad, pad);

        setTextSize(TypedValue.COMPLEX_UNIT_DIP, DEFAULT_TEXT_SIZE);
        setTextColor(Color.BLACK);
        setBackgroundColor(Color.WHITE);
        setHintTextColor(Color.LTGRAY);
        setGravity(Gravity.TOP | Gravity.START);

        ComponentUtils.configureTextField(
                this, true, DEFAULT_MAX_CHARS
        );
    }

    @NonNull
    public CustomKeyboardView.KeyboardType getKeyboardType() {
        return keyboardType;
    }

    public void setKeyboardType(@NonNull CustomKeyboardView.KeyboardType keyboardType) {
        this.keyboardType = keyboardType;
    }
}
