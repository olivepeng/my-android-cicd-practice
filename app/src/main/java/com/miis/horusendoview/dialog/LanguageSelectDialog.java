package com.miis.horusendoview.dialog;

import android.app.Dialog;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.RadioGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.os.LocaleListCompat;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;

import com.blankj.utilcode.util.LanguageUtils;
import com.blankj.utilcode.util.SPStaticUtils;
import com.google.android.material.radiobutton.MaterialRadioButton;
import com.miis.horusendoview.R;
import com.miis.horusendoview.activity.MainActivity;
import com.miis.horusendoview.databinding.DialogLanguageSelectBinding;
import com.miis.horusendoview.tools.KeyboardHideTool;
import com.miis.horusendoview.tools.Tools;
import com.miis.horusendoview.tools.UiTool;

import java.util.Locale;

import timber.log.Timber;

public class LanguageSelectDialog extends DialogFragment {

    @Nullable
    private DialogLanguageSelectBinding binding = null;

    @Nullable
    private Listener listener;

    public interface Listener {
        void onClickCancel();

        void onTouch();
    }

    public static LanguageSelectDialog newInstance() {
        return new LanguageSelectDialog();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (binding == null) {
            binding = DialogLanguageSelectBinding.inflate(inflater, container, false);
        }
        return binding.getRoot();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        return new Dialog(requireContext(), getTheme()) {
            @Override
            public boolean dispatchTouchEvent(MotionEvent ev) {
                Timber.d("onCreateDialog -> dispatchTouchEvent");
                @Nullable final View currentFocus = getCurrentFocus();
                if (currentFocus != null) {
                    Timber.d("onCreateDialog -> dispatchTouchEvent -> hideKeyboard");
                    KeyboardHideTool.hideKeyboard(currentFocus, ev);
                }
                final Listener listener = LanguageSelectDialog.this.listener;
                if (listener != null) {
                    listener.onTouch();
                }
                return super.dispatchTouchEvent(ev);
            }
        };
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setSystemUIVisibility();
        final Context context = getContext();
        LocaleListCompat localeListCompat = null;
        if (context != null) {
            localeListCompat = Tools.getLocaleListFromXml(context);
        }
        if (localeListCompat == null) {
            localeListCompat = LocaleListCompat.getEmptyLocaleList();
        }

        final DialogLanguageSelectBinding binding = LanguageSelectDialog.this.binding;
        if (binding != null) {
            binding.radioGroup.clearCheck();
            binding.radioGroup.removeAllViews();

            for (int i = 0; i < localeListCompat.size(); i++) {
                Locale locale = localeListCompat.get(i);
                if (locale == null) {
                    continue;
                }
                MaterialRadioButton materialRadioButton = null;
                if (context != null) {
                    materialRadioButton = new MaterialRadioButton(context);
                }
                if (materialRadioButton != null) {
                    materialRadioButton.setLayoutParams(new RadioGroup.LayoutParams(
                            RadioGroup.LayoutParams.MATCH_PARENT,
                            RadioGroup.LayoutParams.WRAP_CONTENT
                    ));
                    ViewCompat.setPaddingRelative(
                            materialRadioButton,
                            getResources().getDimensionPixelSize(R.dimen.padding_16),
                            getResources().getDimensionPixelSize(R.dimen.padding_8),
                            getResources().getDimensionPixelSize(R.dimen.padding_16),
                            getResources().getDimensionPixelSize(R.dimen.padding_8)
                    );
                    materialRadioButton.setEllipsize(TextUtils.TruncateAt.END);
                    materialRadioButton.setMaxLines(1);
                    materialRadioButton.setTextSize(20f);
                    materialRadioButton.setTypeface(materialRadioButton.getTypeface(), Typeface.BOLD);
                    materialRadioButton.setButtonTintList(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.black)));
                    materialRadioButton.setText(locale.getDisplayName(locale));
                    materialRadioButton.setTag(locale);
                    binding.radioGroup.addView(materialRadioButton);
                }
            }

            for (int i = 0; i < binding.radioGroup.getChildCount(); i++) {
                View child = binding.radioGroup.getChildAt(i);
                if (child instanceof MaterialRadioButton) {
                    MaterialRadioButton radio = (MaterialRadioButton) child;
                    Locale locale = null;
                    try {
                        locale = (Locale) radio.getTag();
                    } catch (Exception e) {
                        Timber.e(e);
                    }
                    if (locale != null && locale.getLanguage().equals(getResources().getConfiguration().getLocales().get(0).getLanguage())) {
                        radio.toggle();
                        break;
                    }
                }
            }

            binding.confirm.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    View checkedRadioButton = binding.radioGroup.findViewById(binding.radioGroup.getCheckedRadioButtonId());
                    if (checkedRadioButton instanceof MaterialRadioButton) {
                        MaterialRadioButton materialRadioButton = (MaterialRadioButton) checkedRadioButton;
                        Locale locale = null;
                        try {
                            locale = (Locale) materialRadioButton.getTag();
                        } catch (Exception e) {
                            Timber.e(e);
                        }
                        if (locale != null) {
                            binding.confirm.setOnClickListener(null);
                            dismiss();
                            Locale.setDefault(locale);
                            LocaleListCompat appLocale = LocaleListCompat.forLanguageTags(locale.toLanguageTag());

                            FragmentActivity activity = getActivity();
                            if (activity instanceof  MainActivity) {
                                ((MainActivity) activity).setLanguage(appLocale.get(0));
                            }

                        }
                    }
                }
            });

            binding.cancel.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    final Listener listener = LanguageSelectDialog.this.listener;
                    if (listener != null) {
                        listener.onClickCancel();
                    }
                    dismiss();
                }
            });
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        setSystemUIVisibility();
        final Dialog dialog = getDialog();
        final Context context = getContext();
        @Nullable Window window = null;
        if (dialog != null) {
            window = dialog.getWindow();
        }
        if (window != null && context != null) {
            final DisplayMetrics displayMetrics = UiTool.getDisplaySize(context);
            int width = displayMetrics.widthPixels - (context.getResources().getDimensionPixelSize(R.dimen.margin_64) * 2);
            int height = displayMetrics.heightPixels - (context.getResources().getDimensionPixelSize(R.dimen.margin_64) * 2);
            window.setLayout(width, height);

            window.setDimAmount(0.5f); // 对话框外部阴影比重(0f ~ 1f)
            window.setBackgroundDrawable(new ColorDrawable(ContextCompat.getColor(context, R.color.gray_01000000)));
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        setSystemUIVisibility();
    }

    private void setSystemUIVisibility() {
        Dialog dialog = getDialog();
        if (dialog != null) {
            dialog.getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LOW_PROFILE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            );
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

