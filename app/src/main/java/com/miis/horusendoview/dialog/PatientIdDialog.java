package com.miis.horusendoview.dialog;

import android.app.Dialog;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
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
import com.miis.horusendoview.databinding.DialogPatientIdBinding;
import com.miis.horusendoview.inputFilter.BlockCharacterInputFilter;
import com.miis.horusendoview.tools.KeyboardHideTool;

import timber.log.Timber;

public class PatientIdDialog extends DialogFragment {

    @Nullable
    private DialogPatientIdBinding binding = null;

    @Nullable
    private Listener listener;
    private String patientId;

    public static final String KEY_PATIENT_ID = "PATIENT_ID";

    @NonNull
    public static PatientIdDialog newInstance(@NonNull String patientId) {
        PatientIdDialog fragment = new PatientIdDialog();
        Bundle args = new Bundle();
        args.putString(KEY_PATIENT_ID, patientId);
        fragment.setArguments(args);
        return fragment;
    }

    public interface Listener {
        void onClickSave(@NonNull String patientId);

        void onClickCancel();

        void onTouch();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            patientId = getArguments().getString(KEY_PATIENT_ID, "");
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (binding == null) {
            binding = DialogPatientIdBinding.inflate(inflater, container, false);
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
                    final DialogPatientIdBinding binding = PatientIdDialog.this.binding;
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
                final Listener listener = PatientIdDialog.this.listener;
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

        BlockCharacterInputFilter inputFilter = new BlockCharacterInputFilter("\\/._");
        final DialogPatientIdBinding binding = PatientIdDialog.this.binding;
        if (binding == null) {
            return;
        }
        binding.patientId.setFilters(new InputFilter[]{inputFilter});
        binding.patientId.setText(patientId);

        binding.getRoot().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Handle click on root view
            }
        });

        binding.save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Editable patientIdText = binding.patientId.getText();
                String patientId = null;
                if (patientIdText != null) {
                    patientId = patientIdText.toString().trim();
                }
                if (patientId == null) {
                    patientId = "";
                }
                final Listener listener = PatientIdDialog.this.listener;
                if (listener != null) {
                    listener.onClickSave(patientId);
                }
                dismiss();
            }
        });

        binding.cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final Listener listener = PatientIdDialog.this.listener;
                if (listener != null) {
                    listener.onClickCancel();
                }
                dismiss();
            }
        });

        binding.customKeyboardView.unregisterEditText(binding.patientId);
        binding.customKeyboardView.registerEditText(CustomKeyboardView.KeyboardType.QWERTY, binding.patientId);
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
        final DialogPatientIdBinding binding = PatientIdDialog.this.binding;
        if (binding != null) {
            binding.customKeyboardView.unregisterEditText(binding.patientId);
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

    public void setListener(@Nullable Listener listener) {
        this.listener = listener;
    }
}
