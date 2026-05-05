package com.app.customkeyboard.keyboard.layouts;

import android.content.Context;
import android.view.View;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.app.customkeyboard.keyboard.controllers.KeyboardController;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Don.Brody on 7/18/18.
 */
public class NumberKeyboardLayout extends KeyboardLayout {

    public NumberKeyboardLayout(@NonNull Context context, @Nullable KeyboardController controller) {
        super(context, controller, false, false);
    }

    public NumberKeyboardLayout(Context context) {
        this(context,null);
    }

    @NonNull
    @Override
    protected List<LinearLayout> createRows() {
        float columnWidth = 0.20f;
        setTextSize(22.0f);

        List<View> rowOne = new ArrayList<>();
        rowOne.add(createButton("1", columnWidth, '1'));
        rowOne.add(createButton("2", columnWidth, '2'));
        rowOne.add(createButton("3", columnWidth, '3'));

        List<View> rowTwo = new ArrayList<>();
        rowTwo.add(createButton("4", columnWidth, '4'));
        rowTwo.add(createButton("5", columnWidth, '5'));
        rowTwo.add(createButton("6", columnWidth, '6'));

        List<View> rowThree = new ArrayList<>();
        rowThree.add(createButton("7", columnWidth, '7'));
        rowThree.add(createButton("8", columnWidth, '8'));
        rowThree.add(createButton("9", columnWidth, '9'));

        List<View> rowFour = new ArrayList<>();
        rowFour.add(createButton("⌫", columnWidth, KeyboardController.SpecialKey.BACKSPACE));
        rowFour.add(createButton("0", columnWidth, '0'));
        if (isHasNextFocus()) {
            rowFour.add(createButton("Next", columnWidth, KeyboardController.SpecialKey.NEXT));
        } else {
            rowFour.add(createButton("Done", columnWidth, KeyboardController.SpecialKey.DONE));
        }

        List<LinearLayout> rows = new ArrayList<>();
        rows.add(createRow(rowOne));
        rows.add(createRow(rowTwo));
        rows.add(createRow(rowThree));
        rows.add(createRow(rowFour));

        return rows;
    }
}
