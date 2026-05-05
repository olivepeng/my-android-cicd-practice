package com.miis.horusendoview.tools;

import android.app.Activity;
import android.os.IBinder;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;

import com.app.customkeyboard.keyboard.CustomKeyboardView;
import com.blankj.utilcode.util.KeyboardUtils;

/**
 * 鍵盤控制方法
 */
public class KeyboardHideTool {

    public static void hideKeyboard(Activity activity, MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_UP || ev.getAction() == MotionEvent.ACTION_CANCEL) {
            View view = activity.getCurrentFocus();
            if (isShouldHideKeyboard(view, ev)) {
                hideSoftInput(activity, view.getWindowToken());
                clearViewFocus(view);
            }
        }
    }

    public static void hideKeyboard(View currentFocus, MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_UP || ev.getAction() == MotionEvent.ACTION_CANCEL) {
            if (isShouldHideKeyboard(currentFocus, ev)) {
                KeyboardUtils.hideSoftInput(currentFocus);
                clearViewFocus(currentFocus);
            }
        }
    }

    public static void clearViewFocus(View view) {
        if (view != null) {
            view.clearFocus();
        }
    }

    public static boolean isShouldHideKeyboard(View v, MotionEvent ev) {
        if (v != null && v instanceof EditText) {
            int[] leftTop = new int[2];
            v.getLocationOnScreen(leftTop);
            int left = leftTop[0];
            int top = leftTop[1];
            int bottom = top + v.getHeight();
            int right = left + v.getWidth();
            boolean f = (ev.getRawX() <= left || ev.getRawX() >= right
                    || ev.getRawY() <= top || ev.getRawY() >= bottom);
            return f;
        }
        return false;
    }

    public static boolean isCustomKeyboardView(CustomKeyboardView customKeyboardView, MotionEvent ev) {
        int[] leftTop = new int[2];
        customKeyboardView.getLocationOnScreen(leftTop);
        int left = leftTop[0];
        int top = leftTop[1];
        int bottom = top + customKeyboardView.getHeight();
        int right = left + customKeyboardView.getWidth();
        boolean f = (ev.getRawX() >= left && ev.getRawX() <= right
                && ev.getRawY() >= top && ev.getRawY() <= bottom);
        return f;
    }

    public static void hideSoftInput(Activity activity, IBinder token) {
        KeyboardUtils.hideSoftInput(activity);
    }
}

