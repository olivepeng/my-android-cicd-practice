package com.app.customkeyboard.keyboard.controllers;

import android.view.inputmethod.InputConnection;

import androidx.annotation.NonNull;

public class DefaultKeyboardController extends KeyboardController {

    // 預設控制器的最大字符數應該設置為其EditText的屬性
    private static final int MAX_CHARACTERS = Integer.MAX_VALUE;

    public DefaultKeyboardController(@NonNull InputConnection inputConnection) {
        super(inputConnection);
    }

    @Override
    public void handleKeyStroke(char c) {
        addCharacter(c);
    }

    @Override
    public void handleKeyStroke(@NonNull SpecialKey key) {
        switch (key) {
            case DELETE:
                deleteNextCharacter();
                break;
            case BACKSPACE:
                deletePreviousCharacter();
                break;
            case CLEAR:
                clearAll();
                break;
            case FORWARD:
                moveCursorForwardAction();
                break;
            case BACK:
                moveCursorBackAction();
                break;
            default:
                return;
        }
    }

    @Override
    public int maxCharacters() {
        return MAX_CHARACTERS;
    }
}
