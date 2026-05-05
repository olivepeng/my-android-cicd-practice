package com.miis.horusendoview.dialog;

import android.app.Dialog;
import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;

import com.app.customkeyboard.keyboard.CustomKeyboardView;
import com.miis.horusendoview.R;
import com.miis.horusendoview.databinding.DialogZipPasswordBinding;
import com.miis.horusendoview.tools.KeyboardHideTool;

import timber.log.Timber;

public class ZipPasswordDialog extends DialogFragment {

    @Nullable
    private DialogZipPasswordBinding binding = null;

    @Nullable
    private Listener listener;

    @NonNull
    public static ZipPasswordDialog newInstance() {
        return new ZipPasswordDialog();
    }

    public interface Listener {
        void onClickConfirm(@NonNull String password);

        void onClickCancel();

        void onTouch();
    }

    public void setListener(@Nullable Listener listener) {
        this.listener = listener;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (binding == null) {
            binding = DialogZipPasswordBinding.inflate(inflater, container, false);
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
                View currentFocus = getCurrentFocus();
                if (currentFocus != null) {
                    Timber.d("onCreateDialog -> dispatchTouchEvent -> hideKeyboard");
                    final DialogZipPasswordBinding binding = ZipPasswordDialog.this.binding;
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
                final Listener listener = ZipPasswordDialog.this.listener;
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

        final DialogZipPasswordBinding binding = ZipPasswordDialog.this.binding;
        if (binding == null) {
            return;
        }
        binding.getRoot().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
            }
        });

        binding.confirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Editable passwordText = binding.password.getText();
                String password = null;
                if (passwordText != null) {
                    password = passwordText.toString().trim();
                }
                if (password == null) {
                    password = "";
                }
                final Listener listener = ZipPasswordDialog.this.listener;
                if (listener != null) {
                    listener.onClickConfirm(password);
                }
                dismiss();
            }
        });

        binding.cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final Listener listener = ZipPasswordDialog.this.listener;
                if (listener != null) {
                    listener.onClickCancel();
                }
                dismiss();
            }
        });

        binding.customKeyboardView.unregisterEditText(binding.password);
        binding.customKeyboardView.registerEditText(CustomKeyboardView.KeyboardType.QWERTY, binding.password);
    }

    @Override
    public void onStart() {
        super.onStart();
        setSystemUIVisibility();

        Dialog dialog = getDialog();
        if (dialog != null) {
            final Window window = dialog.getWindow();
            final Context context = getContext();
            if (window != null && context != null) {
                window.setLayout(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                );

                window.setDimAmount(0.5f);
                window.setBackgroundDrawable(
                        new ColorDrawable(
                                ContextCompat.getColor(
                                        context,
                                        R.color.gray_01000000
                                )
                        )
                );
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
        final DialogZipPasswordBinding binding = ZipPasswordDialog.this.binding;
        if (binding != null) {
            binding.customKeyboardView.unregisterEditText(binding.password);
        }
    }

    private void setSystemUIVisibility() {
        Dialog dialog = getDialog();
        if (dialog != null) {
            Window window = dialog.getWindow();
            if (window != null) {
                window.getDecorView().setSystemUiVisibility(
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                                View.SYSTEM_UI_FLAG_LOW_PROFILE |
                                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                                View.SYSTEM_UI_FLAG_FULLSCREEN |
                                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                );
            }
        }
    }

    @Nullable
    public DialogZipPasswordBinding getBinding() {
        return binding;
    }

    @Nullable
    public Listener getListener() {
        return listener;
    }
}
