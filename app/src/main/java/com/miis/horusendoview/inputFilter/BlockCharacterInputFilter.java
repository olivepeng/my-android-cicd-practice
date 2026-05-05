package com.miis.horusendoview.inputFilter;

import android.text.InputFilter;
import android.text.Spanned;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class BlockCharacterInputFilter implements InputFilter {

    @NonNull
    private final String blockCharacter;

    public BlockCharacterInputFilter(@NonNull String blockCharacter) {
        this.blockCharacter = blockCharacter;
    }

    @Override
    public CharSequence filter(@Nullable CharSequence source, int start, int end, @Nullable Spanned dest, int dstart, int dend) {
        if (source != null && blockCharacter.contains(String.valueOf(source))) {
            return "";
        }
        return null;
    }
}
