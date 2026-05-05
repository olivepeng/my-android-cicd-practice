package com.miis.horusendoview.action;

import static org.hamcrest.CoreMatchers.any;

import android.view.View;
import android.widget.SeekBar;

import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;

import org.hamcrest.Matcher;

public class SetSeekBarAction implements ViewAction {

    private final int value;

    public SetSeekBarAction(int value) {
        this.value = value;
    }

    @Override
    public Matcher<View> getConstraints() {
        return any(View.class);
    }

    @Override
    public String getDescription() {
        return "Set SeekBar value to " + value;
    }

    @Override
    public void perform(UiController uiController, View view) {
        SeekBar seekBar = (SeekBar) view;
        seekBar.setProgress(value);
    }
}
