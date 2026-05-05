package com.app.customkeyboard.keyboard.layouts;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatButton;

import com.app.customkeyboard.keyboard.KeyboardListener;
import com.app.customkeyboard.keyboard.controllers.KeyboardController;
import com.app.customkeyboard.utilities.ComponentUtils;

import java.util.List;

/**
 * Created by Don.Brody on 7/18/18.
 */
public abstract class KeyboardLayout extends LinearLayout {

    @Nullable
    private KeyboardController controller = null;
    private boolean hasNextFocus = false;
    private boolean isNextLine = false;
    private float screenWidth = 0.0f;
    private float textSize = 20.0f;

    private synchronized int toDp(int px) {
        return (int) (px / Resources.getSystem().getDisplayMetrics().density);
    }


    public KeyboardLayout(@NonNull Context context, @Nullable KeyboardController controller, boolean hasNextFocus, boolean isNextLine) {
        super(context);
        this.controller = controller;
        this.hasNextFocus = hasNextFocus;
        this.isNextLine = isNextLine;
        setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
    }

    public void createKeyboard() {
        createKeyboard(this.screenWidth);
    }

    public void createKeyboard(float screenWidth) {
        removeAllViews();
        this.screenWidth = screenWidth;

        final LinearLayout keyboardWrapper = createWrapperLayout();
        for (LinearLayout row : createRows()) {
            keyboardWrapper.addView(row);
        }
        addView(keyboardWrapper);
    }

    @NonNull
    private LinearLayout createWrapperLayout() {
        LinearLayout wrapper = new LinearLayout(getContext());
        LayoutParams lp = new LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT
        );
        lp.topMargin = toDp(15);
        lp.bottomMargin = toDp(15);
        wrapper.setLayoutParams(lp);
        wrapper.setOrientation(VERTICAL);
        return wrapper;
    }

    @NonNull
    private AppCompatButton createButton(@NonNull String text, float widthAsPctOfScreen) {
        AppCompatButton button = new AppCompatButton(getContext());
        button.setLayoutParams(new LayoutParams(
                (int) (screenWidth * widthAsPctOfScreen),
                LayoutParams.WRAP_CONTENT
        ));
        ComponentUtils.setBackgroundTint(button, Color.LTGRAY);

        button.setAllCaps(false);
        button.setTextSize(textSize);
        button.setText(text);
        return button;
    }

    @NonNull
    protected AppCompatButton createButton(@NonNull String text, float widthAsPctOfScreen, char c) {
        AppCompatButton button = createButton(text, widthAsPctOfScreen);
        button.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                final KeyboardController controller = KeyboardLayout.this.controller;
                if (controller != null) {
                    controller.onKeyStroke(c);
                }
            }
        });
        return button;
    }

    @NonNull
    protected AppCompatButton createButton(@NonNull String text, float widthAsPctOfScreen, @NonNull KeyboardController.SpecialKey key) {
        AppCompatButton button = createButton(text, widthAsPctOfScreen);
        button.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                final KeyboardController controller = KeyboardLayout.this.controller;
                if (controller != null) {
                    controller.onKeyStroke(key);
                }
            }
        });
        return button;
    }

    public View createSpacer(float widthAsPctOfScreen) {
        View view = new View(getContext());
        view.setLayoutParams(new LayoutParams(
                (int) (screenWidth * widthAsPctOfScreen),
                0
        ));
        view.setEnabled(false);
        view.setVisibility(View.INVISIBLE);
        return view;
    }

    @NonNull
    public LinearLayout createRow(@NonNull List<View> buttons) {
        LinearLayout row = new LinearLayout(getContext());
        row.setLayoutParams(new LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT
        ));
        row.setOrientation(HORIZONTAL);
        row.setGravity(Gravity.CENTER);

        for (View button : buttons) {
            row.addView(button);
        }
        return row;
    }

    public void registerListener(@NonNull KeyboardListener listener) {
        final KeyboardController controller = KeyboardLayout.this.controller;
        if (controller != null) {
            controller.registerListener(listener);
        }
    }

    @NonNull
    protected abstract List<LinearLayout> createRows();

    public boolean isHasNextFocus() {
        return hasNextFocus;
    }

    public void setHasNextFocus(boolean hasNextFocus) {
        this.hasNextFocus = hasNextFocus;
    }

    public boolean isNextLine() {
        return isNextLine;
    }

    public void setNextLine(boolean nextLine) {
        isNextLine = nextLine;
    }

    public float getScreenWidth() {
        return screenWidth;
    }

    public void setScreenWidth(float screenWidth) {
        this.screenWidth = screenWidth;
    }

    public float getTextSize() {
        return textSize;
    }

    public void setTextSize(float textSize) {
        this.textSize = textSize;
    }
}
