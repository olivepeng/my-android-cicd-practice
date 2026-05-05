package com.app.customkeyboard.keyboard;

import androidx.annotation.NonNull;

import com.app.customkeyboard.keyboard.controllers.KeyboardController;

/**
 * Created by Don.Brody on 7/19/18.
 */
public interface KeyboardListener {
    void characterClicked(char c);

    void specialKeyClicked(@NonNull KeyboardController.SpecialKey key);
}
