package com.miis.horusendoview.action;

import static org.hamcrest.CoreMatchers.any;

import android.view.View;
import android.widget.TextView;

import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;

import org.hamcrest.Matcher;

public class GetTextAction implements ViewAction {
    private String text;

    @Override
    public Matcher<View> getConstraints() {
        // Ensure the view is a TextView or subclass
        return any(View.class);
    }

    @Override
    public String getDescription() {
        return "getting text from a TextView";
    }

    @Override
    public void perform(UiController uiController, View view) {
        TextView textView = (TextView) view;
        text = textView.getText().toString();
    }

    public String getText() {
        return text;
    }
}
