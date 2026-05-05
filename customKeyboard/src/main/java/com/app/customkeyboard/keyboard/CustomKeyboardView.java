package com.app.customkeyboard.keyboard;

import android.content.Context;
import android.graphics.Color;
import android.text.InputType;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.app.customkeyboard.expandableView.ExpandableState;
import com.app.customkeyboard.expandableView.ExpandableStateListener;
import com.app.customkeyboard.expandableView.ExpandableView;
import com.app.customkeyboard.keyboard.controllers.DefaultKeyboardController;
import com.app.customkeyboard.keyboard.controllers.KeyboardController;
import com.app.customkeyboard.keyboard.controllers.NumberDecimalKeyboardController;
import com.app.customkeyboard.keyboard.layouts.KeyboardLayout;
import com.app.customkeyboard.keyboard.layouts.NumberDecimalKeyboardLayout;
import com.app.customkeyboard.keyboard.layouts.NumberKeyboardLayout;
import com.app.customkeyboard.keyboard.layouts.QwertyKeyboardLayout;
import com.app.customkeyboard.textFields.CustomTextField;
import com.app.customkeyboard.utilities.ComponentUtils;

import java.util.HashMap;

/**
 * Created by Don.Brody on 7/18/18.
 */
public class CustomKeyboardView extends ExpandableView {

    @Nullable
    private EditText fieldInFocus = null;

    @NonNull
    private final HashMap<EditText, KeyboardLayout> keyboards = new HashMap<>();

    @NonNull
    private final KeyboardListener keyboardListener;

    public CustomKeyboardView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setBackgroundColor(Color.GRAY);

        keyboardListener = new KeyboardListener() {
            @Override
            public void characterClicked(char c) {
                // Don't need to do anything here
            }

            @Override
            public void specialKeyClicked(@NonNull KeyboardController.SpecialKey key) {
                if (key == KeyboardController.SpecialKey.DONE) {
                    translateLayout();
                } else if (key == KeyboardController.SpecialKey.NEXT) {
                    EditText nextField = fieldInFocus != null ? (EditText) fieldInFocus.focusSearch(View.FOCUS_DOWN) : null;
                    if (nextField != null) {
                        nextField.requestFocus();
                        checkLocationOnScreen();
                    }
                }
            }
        };

        // Register listener with parent (listen for state changes)
        registerListener(new ExpandableStateListener() {
            @Override
            public void onStateChange(@NonNull ExpandableState state) {
                switch (state) {
                    case COLLAPSED:
                        break;
                    case COLLAPSING:
                        setMoveFieldParent(null);
                        break;
                    case EXPANDED:
                        checkLocationOnScreen();
                        break;
                    case EXPANDING:
                        break;
                }
            }
        });

        // Empty OnClickListener prevents users from accidentally clicking views under the keyboard
        setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                // Do nothing
            }
        });

        setSoundEffectsEnabled(false);
    }

    public void registerEditText(@NonNull KeyboardType type, @NonNull EditText field) {
        if (!field.isEnabled()) {
            return; // Disabled fields do not have input connections
        }

        field.setRawInputType(InputType.TYPE_CLASS_TEXT);
        field.setTextIsSelectable(true);
        field.setShowSoftInputOnFocus(false);
        field.setSoundEffectsEnabled(false);

        InputConnection inputConnection = field.onCreateInputConnection(new EditorInfo());
        keyboards.put(field, createKeyboardLayout(type, inputConnection));

        KeyboardLayout l = keyboards.get(field);
        if (l != null) {
            l.registerListener(keyboardListener);
        }

        field.setOnFocusChangeListener(new OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    ComponentUtils.hideSystemKeyboard(getContext(), field);

                    // If we can find a view below this field, we want to replace the
                    // done button with the next button in the attached keyboard
                    View nextField = field.focusSearch(View.FOCUS_DOWN);
                    if (nextField instanceof EditText) {
                        final KeyboardLayout l2 = keyboards.get(field);
                        if (l2 != null) {
                            l2.setHasNextFocus(true);
                        }
                    }
                    fieldInFocus = field;

                    renderKeyboard();
                    if (!isExpanded()) {
                        translateLayout();
                    }
                } else if (!hasFocus && isExpanded()) {
                    for (EditText editText : keyboards.keySet()) {
                        if (editText.hasFocus()) {
                            return;
                        }
                    }
                    translateLayout();
                }
            }
        });

        field.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isExpanded()) {
                    translateLayout();
                }
            }
        });
    }

    public void autoRegisterEditTexts(ViewGroup rootView) {
        registerEditTextsRecursive(rootView);
    }

    private void registerEditTextsRecursive(View view) {
        if (view instanceof ViewGroup) {
            ViewGroup viewGroup = (ViewGroup) view;
            for (int i = 0; i < viewGroup.getChildCount(); i++) {
                registerEditTextsRecursive(viewGroup.getChildAt(i));
            }
        } else {
            if (view instanceof CustomTextField) {
                registerEditText(((CustomTextField) view).getKeyboardType(), (EditText) view);
            } else if (view instanceof EditText) {
                int inputType = ((EditText) view).getInputType();
                if (inputType == InputType.TYPE_CLASS_NUMBER) {
                    registerEditText(KeyboardType.NUMBER, (EditText) view);
                } else if (inputType == InputType.TYPE_NUMBER_FLAG_DECIMAL) {
                    registerEditText(KeyboardType.NUMBER_DECIMAL, (EditText) view);
                } else {
                    registerEditText(KeyboardType.QWERTY, (EditText) view);
                }
            }
        }
    }

    public void unregisterEditText(@Nullable EditText field) {
        keyboards.remove(field);
    }

    public void clearEditTextCache() {
        keyboards.clear();
    }

    private void renderKeyboard() {
        removeAllViews();
        KeyboardLayout keyboard = keyboards.get(fieldInFocus);
        if (keyboard != null) {
            keyboard.setOrientation(LinearLayout.VERTICAL);
            keyboard.createKeyboard((float) getMeasuredWidth());
            addView(keyboard);
        }
    }

    @Nullable
    private KeyboardLayout createKeyboardLayout(@NonNull KeyboardType type, @NonNull InputConnection ic) {
        switch (type) {
            case NUMBER:
                return new NumberKeyboardLayout(getContext(), createKeyboardController(type, ic));
            case NUMBER_DECIMAL:
                return new NumberDecimalKeyboardLayout(getContext(), createKeyboardController(type, ic));
            case QWERTY:
                return new QwertyKeyboardLayout(getContext(), createKeyboardController(type, ic), false);
            case QWERTY_NEXT_LINE:
                return new QwertyKeyboardLayout(getContext(), createKeyboardController(type, ic), true);
            default:
                return null;
        }
    }

    @NonNull
    private KeyboardController createKeyboardController(@NonNull KeyboardType type, @NonNull InputConnection ic) {
        if (type == KeyboardType.NUMBER_DECIMAL) {
            return new NumberDecimalKeyboardController(ic);
        } else {
            // Not all keyboards require a custom controller
            return new DefaultKeyboardController(ic);
        }
    }

    @Override
    protected void configureSelf() {
        renderKeyboard();
        checkLocationOnScreen();
    }

    @Nullable
    private ViewParent moveFieldParent = null;

    public void setMoveFieldParent(@Nullable ViewParent moveFieldParent) {
        if (moveFieldParent == null) {
            if (this.moveFieldParent instanceof ViewGroup) {
                ((ViewGroup) this.moveFieldParent).setTranslationY(0f);
            }
        }
        this.moveFieldParent = moveFieldParent;
    }

    /**
     * Check if fieldInFocus has a parent that is a ScrollView.
     * Ensure that ScrollView is enabled.
     * Check if the fieldInFocus is below the KeyboardLayout (measured on the screen).
     * If it is, find the deltaY between the top of the KeyboardLayout and the top of the
     * fieldInFocus, add 20dp (for padding), and scroll to the deltaY.
     * This will ensure the keyboard doesn't cover the field (if conditions above are met).
     */
    private void checkLocationOnScreen() {
        final EditText fieldInFocus = CustomKeyboardView.this.fieldInFocus;
        if (fieldInFocus != null) {
            ViewParent fieldParentOld = null;
            ViewParent fieldParent = fieldInFocus.getParent();
            while (fieldParent != null) {
                if (fieldParent instanceof ScrollView) {
                    if (!((ScrollView) fieldParent).isSmoothScrollingEnabled()) {
                        break;
                    }

                    int[] fieldLocation = new int[2];
                    fieldInFocus.getLocationOnScreen(fieldLocation);

                    int[] keyboardLocation = new int[2];
                    this.getLocationOnScreen(keyboardLocation);

                    int fieldY = fieldLocation[1];
                    int keyboardY = keyboardLocation[1];

                    if (fieldY > keyboardY) {
                        int deltaY = fieldY - keyboardY;
                        int scrollTo = ((ScrollView) fieldParent).getScrollY() + deltaY + fieldInFocus.getMeasuredHeight() + toDp(10);
                        ((ScrollView) fieldParent).smoothScrollTo(0, scrollTo);
                    }
                    break;
                } else if (fieldParent instanceof ViewGroup) {
                    boolean isFindKeyboardView = false;
                    ViewGroup fieldParentGroup = (ViewGroup) fieldParent;
                    for (int i = 0; i < fieldParentGroup.getChildCount(); i++) {
                        if (fieldParentGroup.getChildAt(i) == CustomKeyboardView.this) {
                            isFindKeyboardView = true;
                            break;
                        }
                    }

                    if (isFindKeyboardView && fieldParentOld instanceof ViewGroup) {
                        int[] fieldLocation = new int[2];
                        fieldInFocus.getLocationOnScreen(fieldLocation);

                        int[] keyboardLocation = new int[2];
                        this.getLocationOnScreen(keyboardLocation);

                        int fieldY = fieldLocation[1];
                        int keyboardY = keyboardLocation[1];

                        if (fieldY > keyboardY) {
                            int deltaY = fieldY - keyboardY;
                            int scrollTo = (int) (((ViewGroup) fieldParentOld).getScrollY() + deltaY + fieldInFocus.getMeasuredHeight() + toDp(10));
                            ((ViewGroup) fieldParentOld).setTranslationY(-scrollTo);

                            setMoveFieldParent(fieldParentOld);
                        }
                        break;
                    }
                }

                fieldParentOld = fieldParent;
                fieldParent = fieldParent.getParent();
            }
        }
    }

    public enum KeyboardType {
        NUMBER,
        NUMBER_DECIMAL,
        QWERTY,
        QWERTY_NEXT_LINE
    }
}
