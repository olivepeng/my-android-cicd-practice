package com.app.customkeyboard.keyboard.controllers;

import android.view.KeyEvent;
import android.view.inputmethod.InputConnection;

import androidx.annotation.NonNull;

import com.app.customkeyboard.keyboard.KeyboardListener;

import java.util.ArrayList;

/**
 * Created by Don.Brody on 7/18/18.
 */
public abstract class KeyboardController {

    @NonNull
    private final InputConnection inputConnection;

    @NonNull
    private final ArrayList<KeyboardListener> listeners = new ArrayList<>();
    private int cursorPosition = 0;

    @NonNull
    private String inputText = "";

    public KeyboardController(@NonNull InputConnection inputConnection) {
        this.inputConnection = inputConnection;
    }

    protected abstract void handleKeyStroke(char c);

    protected abstract void handleKeyStroke(SpecialKey key);

    protected abstract int maxCharacters();

    public synchronized void onKeyStroke(char c) {
        updateMembers();
        handleKeyStroke(c);
        for (KeyboardListener listener : listeners) {
            listener.characterClicked(c);
        }
    }

    public synchronized void onKeyStroke(@NonNull SpecialKey key) {
        updateMembers();
        handleKeyStroke(key);
        for (KeyboardListener listener : listeners) {
            listener.specialKeyClicked(key);
        }
    }

    public void registerListener(@NonNull KeyboardListener listener) {
        listeners.add(listener);
    }

    public void updateMembers() {
        String beforeText = beforeCursor();
        String afterText = afterCursor();
        cursorPosition = beforeText.length();
        inputText = beforeText + afterText;
    }

    @NonNull
    public String beforeCursor() {
        return inputConnection.getTextBeforeCursor(100, 0).toString();
    }

    @NonNull
    public String afterCursor() {
        return inputConnection.getTextAfterCursor(100, 0).toString();
    }

    public int cursorPosition() {
        return cursorPosition;
    }

    @NonNull
    public String inputText() {
        return inputText;
    }

    public void deleteNextCharacter() {
        if (cursorPosition >= inputText.length()) {
            return;
        }
        inputConnection.deleteSurroundingText(0, 1);
        inputText = deleteCharacter(inputText, cursorPosition);
    }

    public void deletePreviousCharacter() {
        if (cursorPosition == 0) {
            return;
        }
        inputConnection.deleteSurroundingText(1, 0);
        inputText = deleteCharacter(inputText, --cursorPosition);
    }

    public void addCharacter(char c) {
        if (cursorPosition >= maxCharacters()) {
            return;
        }
        inputConnection.commitText(String.valueOf(c), 1);
        if (cursorPosition++ >= inputText.length()) {
            inputText = inputText + c;
        } else {
            inputText = addCharacter(inputText, c, cursorPosition - 1);
        }
    }

    public void replaceNextCharacter(char c) {
        deleteNextCharacter();
        addCharacter(c);
    }

    // In the case a synchronous action is required, use this
    public void synchronousMoveCursorForward() {
        replaceNextCharacter(inputText().charAt(cursorPosition()));
    }

    // Cursor actions are asynchronous events
    public void moveCursorForwardAction() {
        if (cursorPosition >= inputText.length()) {
            return;
        }
        inputConnection.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_RIGHT));
        inputConnection.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_RIGHT));
    }

    public void moveCursorBackAction() {
        if (cursorPosition == 0) {
            return;
        }
        inputConnection.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_LEFT));
        inputConnection.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_LEFT));
    }

    public void clearAll() {
        while (cursorPosition < inputText.length()) {
            deleteNextCharacter();
        }
        while (cursorPosition > 0) {
            deletePreviousCharacter();
        }
    }

    public enum SpecialKey {
        DELETE,
        BACKSPACE,
        CLEAR,
        FORWARD,
        BACK,
        NEXT,
        CAPS,
        SYMBOL,
        ALPHA,
        DONE
    }

    @NonNull
    public static String deleteCharacter(@NonNull String text, int index) {
        return new StringBuilder(text).deleteCharAt(index).toString();
    }

    @NonNull
    public static String addCharacter(@NonNull String text, char addition, int index) {
        return text.substring(0, index) + addition + text.substring(index);
    }

    @NonNull
    public static String replaceCharacter(@NonNull String text, char replacement, int index) {
        StringBuilder stringBuilder = new StringBuilder(text);
        stringBuilder.setCharAt(index, replacement);
        text = stringBuilder.toString();
        return text;
    }
}
