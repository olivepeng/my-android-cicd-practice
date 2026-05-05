package com.app.customkeyboard.keyboard.controllers;

import android.view.inputmethod.InputConnection;

import androidx.annotation.NonNull;

public class NumberDecimalKeyboardController extends DefaultKeyboardController {

    public NumberDecimalKeyboardController(@NonNull InputConnection inputConnection) {
        super(inputConnection);
    }

    @Override
    public void handleKeyStroke(char c) {
        if (c == '.') {
            // 十進制數字只能有一個小數點
            if (!inputText().contains(".")) {
                addCharacter(c);
            }
        } else {
            addCharacter(c);
        }
    }
}
