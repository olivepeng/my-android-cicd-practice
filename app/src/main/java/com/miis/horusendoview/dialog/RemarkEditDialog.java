package com.miis.horusendoview.dialog;

import android.app.Dialog;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;

import com.app.customkeyboard.keyboard.CustomKeyboardView;
import com.miis.horusendoview.R;
import com.miis.horusendoview.databinding.DialogRemarkEditBinding;
import com.miis.horusendoview.tools.KeyboardHideTool;

import timber.log.Timber;

public class RemarkEditDialog extends DialogFragment {

    @Nullable
    private DialogRemarkEditBinding binding;

    @Nullable
    private Listener listener;

    @NonNull
    private String text = "";

    public static final String KEY_TEXT = "TEXT";

    @NonNull
    public static RemarkEditDialog newInstance(@NonNull String text) {
        RemarkEditDialog fragment = new RemarkEditDialog();
        Bundle args = new Bundle();
        args.putString(KEY_TEXT, text);
        fragment.setArguments(args);
        return fragment;
    }

    public interface Listener {
        void onClickConfirm(@NonNull String text);

        void onClickCancel();

        void onTouch();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Bundle arguments = getArguments();
        if (arguments != null) {
            text = arguments.getString(KEY_TEXT, "");
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (binding == null) {
            binding = DialogRemarkEditBinding.inflate(inflater, container, false);
        }
        return binding.getRoot();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return new Dialog(requireContext(), getTheme()) {
            @Override
            public boolean dispatchTouchEvent(MotionEvent ev) {
                Timber.d("onCreateDialog -> dispatchTouchEvent");
                final View currentFocus = getCurrentFocus();
                if (currentFocus != null) {
                    Timber.d("onCreateDialog -> dispatchTouchEvent -> hideKeyboard");
                    final DialogRemarkEditBinding binding = RemarkEditDialog.this.binding;
                    CustomKeyboardView customKeyboardView = null;
                    if (binding != null) {
                        customKeyboardView = binding.customKeyboardView;
                    }
                    boolean isCustomKeyboardView = false;
                    if (customKeyboardView != null) {
                        isCustomKeyboardView = KeyboardHideTool.isCustomKeyboardView(customKeyboardView, ev);
                    }
                    if (!isCustomKeyboardView) {
                        KeyboardHideTool.hideKeyboard(currentFocus, ev);
                    }
                }
                final Listener listener = RemarkEditDialog.this.listener;
                if (listener != null) {
                    listener.onTouch();
                }
                return super.dispatchTouchEvent(ev);
            }
        };
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setSystemUIVisibility();
        setCancelable(false);

        final DialogRemarkEditBinding binding = RemarkEditDialog.this.binding;
        if (binding == null) {
            return;
        }

        binding.editText.setText(text);

        binding.getRoot().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

            }
        });

        binding.confirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final Listener listener = RemarkEditDialog.this.listener;
                if (listener != null) {
                    Editable editTextText = binding.editText.getText();
                    String text = null;
                    if (editTextText != null) {
                        text = editTextText.toString().trim();
                    }
                    if (text == null) {
                        text = "";
                    }
                    listener.onClickConfirm(text);
                }
                dismiss();
            }
        });

        binding.cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final Listener listener = RemarkEditDialog.this.listener;
                if (listener != null) {
                    listener.onClickCancel();
                }
                dismiss();
            }
        });

        binding.customKeyboardView.unregisterEditText(binding.editText);
        binding.customKeyboardView.registerEditText(CustomKeyboardView.KeyboardType.QWERTY_NEXT_LINE, binding.editText);
    }

    @Override
    public void onStart() {
        super.onStart();
        setSystemUIVisibility();

        Dialog dialog = getDialog();
        if (dialog != null) {
            if (dialog.getWindow() != null) {
                dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
                dialog.getWindow().setDimAmount(0.5f);
                dialog.getWindow().setBackgroundDrawable(new ColorDrawable(ContextCompat.getColor(requireContext(), R.color.gray_01000000)));
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        setSystemUIVisibility();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (binding != null) {
            binding.customKeyboardView.unregisterEditText(binding.editText);
        }
    }

    private void setSystemUIVisibility() {
        Dialog dialog = getDialog();
        if (dialog != null) {
            if (dialog.getWindow() != null) {
                dialog.getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LOW_PROFILE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
            }
        }
    }

    @Nullable
    public Listener getListener() {
        return listener;
    }

    public void setListener(@Nullable Listener listener) {
        this.listener = listener;
    }
}
