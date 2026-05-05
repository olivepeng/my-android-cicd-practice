package com.app.customkeyboard.keyboard.layouts;

import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.app.customkeyboard.keyboard.KeyboardListener;
import com.app.customkeyboard.keyboard.controllers.KeyboardController;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Don.Brody on 7/20/18.
 */
public class QwertyKeyboardLayout extends KeyboardLayout {

    @NonNull
    private CapsState capsState = CapsState.CAPS_DISABLED;

    @NonNull
    private SymbolState symbolsState = SymbolState.SYMBOLS_DISABLED;
    private final float columnWidth = 0.09f;

    private enum CapsState {
        CAPS_DISABLED,
        CAPS_ENABLED,
        CAPS_LOCK_ENABLED
    }

    private enum SymbolState {
        SYMBOLS_DISABLED,
        SYMBOL_ONE_DISPLAYED,
        SYMBOL_TWO_DISPLAYED,
    }

    public QwertyKeyboardLayout(@NonNull Context context, @Nullable KeyboardController controller, boolean isNextLine) {
        super(context, controller, false, isNextLine);
        initKeyboardListener(controller);
    }

    public QwertyKeyboardLayout(Context context) {
        this(context,null,false);
        initKeyboardListener(null);
    }

    private void initKeyboardListener(@Nullable KeyboardController controller) {
        if (controller != null) {
            controller.registerListener(new KeyboardListener() {
                @Override
                public void characterClicked(char c) {
                    if (capsState == CapsState.CAPS_ENABLED) {
                        capsState = CapsState.CAPS_DISABLED;
                        createKeyboard();
                    }
                }

                @Override
                public void specialKeyClicked(@NonNull KeyboardController.SpecialKey key) {
                    switch (key) {
                        case CAPS:
                            switch (capsState) {
                                case CAPS_DISABLED:
                                    capsState = CapsState.CAPS_ENABLED;
                                    break;
                                case CAPS_ENABLED:
                                    capsState = CapsState.CAPS_LOCK_ENABLED;
                                    break;
                                case CAPS_LOCK_ENABLED:
                                    capsState = CapsState.CAPS_DISABLED;
                                    break;
                            }
                            createKeyboard();
                            break;
                        case SYMBOL:
                            switch (symbolsState) {
                                case SYMBOLS_DISABLED:
                                    symbolsState = SymbolState.SYMBOL_ONE_DISPLAYED;
                                    break;
                                case SYMBOL_ONE_DISPLAYED:
                                    symbolsState = SymbolState.SYMBOL_TWO_DISPLAYED;
                                    break;
                                case SYMBOL_TWO_DISPLAYED:
                                    symbolsState = SymbolState.SYMBOL_ONE_DISPLAYED;
                                    break;
                            }
                            createKeyboard();
                            break;
                        case ALPHA:
                            symbolsState = SymbolState.SYMBOLS_DISABLED;
                            createKeyboard();
                            break;
                    }
                }
            });
        }
    }

    @NonNull
    @Override
    protected List<LinearLayout> createRows() {
        if (symbolsState != SymbolState.SYMBOLS_DISABLED) {
            switch (symbolsState) {
                case SYMBOL_ONE_DISPLAYED:
                    return createSymbolsOneRows();
                case SYMBOL_TWO_DISPLAYED:
                    return createSymbolsTwoRows();
                default:
                    return new ArrayList<>(); // this will never happen
            }
        } else {
            switch (capsState) {
                case CAPS_DISABLED:
                    return createLowerCaseRows();
                case CAPS_ENABLED:
                case CAPS_LOCK_ENABLED:
                    return createUpperCaseRows();
                default:
                    return new ArrayList<>(); // this will never happen
            }
        }
    }

    @NonNull
    private List<LinearLayout> createLowerCaseRows() {
        ArrayList<View> rowTwo = new ArrayList<>();
        rowTwo.add(createButton("q", columnWidth, 'q'));
        rowTwo.add(createButton("w", columnWidth, 'w'));
        rowTwo.add(createButton("e", columnWidth, 'e'));
        rowTwo.add(createButton("r", columnWidth, 'r'));
        rowTwo.add(createButton("t", columnWidth, 't'));
        rowTwo.add(createButton("y", columnWidth, 'y'));
        rowTwo.add(createButton("u", columnWidth, 'u'));
        rowTwo.add(createButton("i", columnWidth, 'i'));
        rowTwo.add(createButton("o", columnWidth, 'o'));
        rowTwo.add(createButton("p", columnWidth, 'p'));
        rowTwo.add(createButton("⌫", columnWidth, KeyboardController.SpecialKey.BACKSPACE));

        ArrayList<View> rowThree = new ArrayList<>();
        rowThree.add(createSpacer((columnWidth * 0.5f)));
        rowThree.add(createButton("a", columnWidth, 'a'));
        rowThree.add(createButton("s", columnWidth, 's'));
        rowThree.add(createButton("d", columnWidth, 'd'));
        rowThree.add(createButton("f", columnWidth, 'f'));
        rowThree.add(createButton("g", columnWidth, 'g'));
        rowThree.add(createButton("h", columnWidth, 'h'));
        rowThree.add(createButton("j", columnWidth, 'j'));
        rowThree.add(createButton("k", columnWidth, 'k'));
        rowThree.add(createButton("l", columnWidth, 'l'));
        if (isNextLine()) {
            rowThree.add(createButton("Next", (columnWidth * 1.5f), '\n'));
        } else {
            rowThree.add(isHasNextFocus()
                    ? createButton("Next", (columnWidth * 1.5f), KeyboardController.SpecialKey.NEXT)
                    : createButton("Done", (columnWidth * 1.5f), KeyboardController.SpecialKey.DONE));
        }

        ArrayList<View> rowFour = new ArrayList<>();
        rowFour.add(createCapsButton());
        rowFour.add(createButton("z", columnWidth, 'z'));
        rowFour.add(createButton("x", columnWidth, 'x'));
        rowFour.add(createButton("c", columnWidth, 'c'));
        rowFour.add(createButton("v", columnWidth, 'v'));
        rowFour.add(createButton("b", columnWidth, 'b'));
        rowFour.add(createButton("n", columnWidth, 'n'));
        rowFour.add(createButton("m", columnWidth, 'm'));
        rowFour.add(createButton(",", columnWidth, ','));
        rowFour.add(createButton(".", columnWidth, '.'));
        rowFour.add(createSpacer(columnWidth));

        ArrayList<View> rowFive = new ArrayList<>();
        rowFive.add(createButton("Symbols", (columnWidth * 2.0f), KeyboardController.SpecialKey.SYMBOL));
        rowFive.add(createButton("", columnWidth * 7.0f, ' '));
        rowFive.add(createButton("⇦", columnWidth, KeyboardController.SpecialKey.BACK));
        rowFive.add(createButton("⇨", columnWidth, KeyboardController.SpecialKey.FORWARD));

        List<LinearLayout> rows = new ArrayList<>();
        rows.add(createNumbersRow());
        rows.add(createRow(rowTwo));
        rows.add(createRow(rowThree));
        rows.add(createRow(rowFour));
        rows.add(createRow(rowFive));

        return rows;
    }

    @NonNull
    private List<LinearLayout> createUpperCaseRows() {
        ArrayList<View> rowTwo = new ArrayList<>();
        rowTwo.add(createButton("Q", columnWidth, 'Q'));
        rowTwo.add(createButton("W", columnWidth, 'W'));
        rowTwo.add(createButton("E", columnWidth, 'E'));
        rowTwo.add(createButton("R", columnWidth, 'R'));
        rowTwo.add(createButton("T", columnWidth, 'T'));
        rowTwo.add(createButton("Y", columnWidth, 'Y'));
        rowTwo.add(createButton("U", columnWidth, 'U'));
        rowTwo.add(createButton("I", columnWidth, 'I'));
        rowTwo.add(createButton("O", columnWidth, 'O'));
        rowTwo.add(createButton("P", columnWidth, 'P'));
        rowTwo.add(createButton("⌫", columnWidth, KeyboardController.SpecialKey.BACKSPACE));

        ArrayList<View> rowThree = new ArrayList<>();
        rowThree.add(createSpacer((columnWidth * 0.5f)));
        rowThree.add(createButton("A", columnWidth, 'A'));
        rowThree.add(createButton("S", columnWidth, 'S'));
        rowThree.add(createButton("D", columnWidth, 'D'));
        rowThree.add(createButton("F", columnWidth, 'F'));
        rowThree.add(createButton("G", columnWidth, 'G'));
        rowThree.add(createButton("H", columnWidth, 'H'));
        rowThree.add(createButton("J", columnWidth, 'J'));
        rowThree.add(createButton("K", columnWidth, 'K'));
        rowThree.add(createButton("L", columnWidth, 'L'));
        if (isNextLine()) {
            rowThree.add(createButton("Next", (columnWidth * 1.5f), '\n'));
        } else {
            rowThree.add(isHasNextFocus()
                    ? createButton("Next", (columnWidth * 1.5f), KeyboardController.SpecialKey.NEXT)
                    : createButton("Done", (columnWidth * 1.5f), KeyboardController.SpecialKey.DONE));
        }

        ArrayList<View> rowFour = new ArrayList<>();
        rowFour.add(createCapsButton());
        rowFour.add(createButton("Z", columnWidth, 'Z'));
        rowFour.add(createButton("X", columnWidth, 'X'));
        rowFour.add(createButton("C", columnWidth, 'C'));
        rowFour.add(createButton("V", columnWidth, 'V'));
        rowFour.add(createButton("B", columnWidth, 'B'));
        rowFour.add(createButton("N", columnWidth, 'N'));
        rowFour.add(createButton("M", columnWidth, 'M'));
        rowFour.add(createButton(",", columnWidth, ','));
        rowFour.add(createButton(".", columnWidth, '.'));
        rowFour.add(createSpacer(columnWidth));

        ArrayList<View> rowFive = new ArrayList<>();
        rowFive.add(createButton("Symbols", (columnWidth * 2.0f), KeyboardController.SpecialKey.SYMBOL));
        rowFive.add(createButton("", columnWidth * 7.0f, ' '));
        rowFive.add(createButton("⇦", columnWidth, KeyboardController.SpecialKey.BACK));
        rowFive.add(createButton("⇨", columnWidth, KeyboardController.SpecialKey.FORWARD));

        ArrayList<LinearLayout> rows = new ArrayList<>();
        rows.add(createNumbersRow());
        rows.add(createRow(rowTwo));
        rows.add(createRow(rowThree));
        rows.add(createRow(rowFour));
        rows.add(createRow(rowFive));

        return rows;
    }

    @NonNull
    private List<LinearLayout> createSymbolsOneRows() {
        ArrayList<View> rowTwo = new ArrayList<>();
        rowTwo.add(createButton("+", columnWidth, '+'));
        rowTwo.add(createButton("×", columnWidth, '×'));
        rowTwo.add(createButton("÷", columnWidth, '÷'));
        rowTwo.add(createButton("=", columnWidth, '='));
        rowTwo.add(createButton("%", columnWidth, '%'));
        rowTwo.add(createButton("_", columnWidth, '_'));
        rowTwo.add(createButton("€", columnWidth, '€'));
        rowTwo.add(createButton("£", columnWidth, '£'));
        rowTwo.add(createButton("¥", columnWidth, '¥'));
        rowTwo.add(createButton("₩", columnWidth, '₩'));
        rowTwo.add(createButton("⌫", columnWidth, KeyboardController.SpecialKey.BACKSPACE));

        ArrayList<View> rowThree = new ArrayList<>();
        rowThree.add(createSpacer((columnWidth * 0.5f)));
        rowThree.add(createButton("@", columnWidth, '@'));
        rowThree.add(createButton("#", columnWidth, '#'));
        rowThree.add(createButton("$", columnWidth, '$'));
        rowThree.add(createButton("/", columnWidth, '/'));
        rowThree.add(createButton("^", columnWidth, '^'));
        rowThree.add(createButton("&", columnWidth, '&'));
        rowThree.add(createButton("*", columnWidth, '*'));
        rowThree.add(createButton("(", columnWidth, '('));
        rowThree.add(createButton(")", columnWidth, ')'));
        if (isNextLine()) {
            rowThree.add(createButton("Next", (columnWidth * 1.5f), '\n'));
        } else {
            rowThree.add(isHasNextFocus()
                    ? createButton("Next", (columnWidth * 1.5f), KeyboardController.SpecialKey.NEXT)
                    : createButton("Done", (columnWidth * 1.5f), KeyboardController.SpecialKey.DONE));
        }

        ArrayList<View> rowFour = new ArrayList<>();
        rowFour.add(createCapsButton());
        rowFour.add(createButton("-", columnWidth, '-'));
        rowFour.add(createButton("'", columnWidth, '\''));
        rowFour.add(createButton("\"", columnWidth, '\"'));
        rowFour.add(createButton(":", columnWidth, ':'));
        rowFour.add(createButton(";", columnWidth, ';'));
        rowFour.add(createButton("!", columnWidth, '!'));
        rowFour.add(createButton("?", columnWidth, '?'));
        rowFour.add(createButton(",", columnWidth, ','));
        rowFour.add(createButton(".", columnWidth, '.'));
        rowFour.add(createSpacer(columnWidth));

        ArrayList<View> rowFive = new ArrayList<>();
        rowFive.add(createButton("Sym (1/2)", (columnWidth * 2.0f), KeyboardController.SpecialKey.SYMBOL));
        rowFive.add(createButton("", columnWidth * 7.0f, ' '));
        rowFive.add(createButton("⇦", columnWidth, KeyboardController.SpecialKey.BACK));
        rowFive.add(createButton("⇨", columnWidth, KeyboardController.SpecialKey.FORWARD));

        ArrayList<LinearLayout> rows = new ArrayList<>();
        rows.add(createNumbersRow());
        rows.add(createRow(rowTwo));
        rows.add(createRow(rowThree));
        rows.add(createRow(rowFour));
        rows.add(createRow(rowFive));

        return rows;
    }

    @NonNull

    private List<LinearLayout> createSymbolsTwoRows() {
        ArrayList<View> rowTwo = new ArrayList<>();
        rowTwo.add(createButton("`", columnWidth, '`'));
        rowTwo.add(createButton("~", columnWidth, '~'));
        rowTwo.add(createButton("\\", columnWidth, '\\'));
        rowTwo.add(createButton("|", columnWidth, '|'));
        rowTwo.add(createButton("<", columnWidth, '<'));
        rowTwo.add(createButton(">", columnWidth, '>'));
        rowTwo.add(createButton("{", columnWidth, '{'));
        rowTwo.add(createButton("}", columnWidth, '}'));
        rowTwo.add(createButton("[", columnWidth, '['));
        rowTwo.add(createButton("]", columnWidth, ']'));
        rowTwo.add(createButton("⌫", columnWidth, KeyboardController.SpecialKey.BACKSPACE));

        ArrayList<View> rowThree = new ArrayList<>();
        rowThree.add(createSpacer((columnWidth * 0.5f)));
        rowThree.add(createButton("▪", columnWidth, '▪'));
        rowThree.add(createButton("○", columnWidth, '○'));
        rowThree.add(createButton("●", columnWidth, '●'));
        rowThree.add(createButton("□", columnWidth, '□'));
        rowThree.add(createButton("■", columnWidth, '■'));
        rowThree.add(createButton("♤", columnWidth, '♤'));
        rowThree.add(createButton("♡", columnWidth, '♡'));
        rowThree.add(createButton("◇", columnWidth, '◇'));
        rowThree.add(createButton("♧", columnWidth, '♧'));
        if (isNextLine()) {
            rowThree.add(createButton("Next", (columnWidth * 1.5f), '\n'));
        } else {
            rowThree.add(isHasNextFocus()
                    ? createButton("Next", (columnWidth * 1.5f), KeyboardController.SpecialKey.NEXT)
                    : createButton("Done", (columnWidth * 1.5f), KeyboardController.SpecialKey.DONE));
        }

        ArrayList<View> rowFour = new ArrayList<>();
        rowFour.add(createCapsButton());
        rowFour.add(createButton("☆", columnWidth, '☆'));
        rowFour.add(createButton("⊙", columnWidth, '⊙'));
        rowFour.add(createButton("⦿", columnWidth, '⦿'));
        rowFour.add(createButton("⍉", columnWidth, '⍉'));
        rowFour.add(createButton("⊛", columnWidth, '⊛'));
        rowFour.add(createButton("⟪", columnWidth, '⟪'));
        rowFour.add(createButton("⟫", columnWidth, '⟫'));
        rowFour.add(createButton("¡", columnWidth, '¡'));
        rowFour.add(createButton("¿", columnWidth, '¿'));
        rowFour.add(createSpacer(columnWidth));

        ArrayList<View> rowFive = new ArrayList<>();
        rowFive.add(createButton("Sym (2/2)", (columnWidth * 2.0f), KeyboardController.SpecialKey.SYMBOL));
        rowFive.add(createButton("", columnWidth * 7.0f, ' '));
        rowFive.add(createButton("⇦", columnWidth, KeyboardController.SpecialKey.BACK));
        rowFive.add(createButton("⇨", columnWidth, KeyboardController.SpecialKey.FORWARD));

        ArrayList<LinearLayout> rows = new ArrayList<>();
        rows.add(createNumbersRow());
        rows.add(createRow(rowTwo));
        rows.add(createRow(rowThree));
        rows.add(createRow(rowFour));
        rows.add(createRow(rowFive));

        return rows;
    }

    @NonNull
    private LinearLayout createNumbersRow() {
        ArrayList<View> row = new ArrayList<>();
        row.add(createButton("1", columnWidth, '1'));
        row.add(createButton("2", columnWidth, '2'));
        row.add(createButton("3", columnWidth, '3'));
        row.add(createButton("4", columnWidth, '4'));
        row.add(createButton("5", columnWidth, '5'));
        row.add(createButton("6", columnWidth, '6'));
        row.add(createButton("7", columnWidth, '7'));
        row.add(createButton("8", columnWidth, '8'));
        row.add(createButton("9", columnWidth, '9'));
        row.add(createButton("0", columnWidth, '0'));
        row.add(createButton("Del", columnWidth, KeyboardController.SpecialKey.DELETE));
        return createRow(row);
    }

    @NonNull
    private Button createCapsButton() {
        String alphaText = "ABC";
        if (symbolsState == SymbolState.SYMBOLS_DISABLED) {
            switch (capsState) {
                case CAPS_DISABLED:
                    return createButton("⇧", columnWidth, KeyboardController.SpecialKey.CAPS);
                case CAPS_ENABLED:
                    return createButton("⬆", columnWidth, KeyboardController.SpecialKey.CAPS);
                case CAPS_LOCK_ENABLED:
                    Button button = createButton("⇧", columnWidth, KeyboardController.SpecialKey.CAPS);
                    button.setBackgroundColor(Color.parseColor("#33CCFF"));
                    return button;
                default:
                    return createButton("", columnWidth, KeyboardController.SpecialKey.ALPHA);
            }
        } else {
            return createButton(alphaText, columnWidth, KeyboardController.SpecialKey.ALPHA);
        }
    }
}