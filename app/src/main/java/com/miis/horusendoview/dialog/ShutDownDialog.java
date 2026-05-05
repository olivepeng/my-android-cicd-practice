package com.miis.horusendoview.dialog;

import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;

import com.miis.horusendoview.R;
import com.miis.horusendoview.databinding.DialogShutDownBinding;

/**
 * ShutDown 小視窗
 */
public class ShutDownDialog extends AlertDialog {

    private DialogShutDownBinding binding;

    @Nullable
    private Listener listener = null;

    public interface Listener {
        void OnClickSleep();

        void OnClickCancel();

        void OnClickShoutDown();
    }

    public ShutDownDialog(@NonNull Context context) {
        super(context);
        init();
    }

    public ShutDownDialog(@NonNull Context context, int themeResId) {
        super(context, themeResId);
        init();
    }

    public ShutDownDialog(@NonNull Context context, boolean cancelable, @Nullable OnCancelListener cancelListener) {
        super(context, cancelable, cancelListener);
        init();
    }

    private void init() {
        binding = DialogShutDownBinding.inflate(LayoutInflater.from(getContext()), null, false);

        binding.sleep.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final Listener listener = ShutDownDialog.this.listener;
                if (listener != null) {
                    listener.OnClickSleep();
                }

                dismiss();
            }
        });

        binding.cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final Listener listener = ShutDownDialog.this.listener;
                if (listener != null) {
                    listener.OnClickCancel();
                }

                dismiss();
            }
        });

        binding.shoutDown.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final Listener listener = ShutDownDialog.this.listener;
                if (listener != null) {
                    listener.OnClickShoutDown();
                }

                dismiss();
            }
        });

        setView(binding.getRoot());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setSystemUIVisibility();
    }

    @Override
    protected void onStop() {
        super.onStop();
        setSystemUIVisibility();
    }

    @Override
    public void show() {
        super.show();
        Window w = getWindow();
        if (w != null) {
            w.setDimAmount(0.5f); // 对话框外部阴影比重(0f ~ 1f)
            w.setBackgroundDrawable(new ColorDrawable(ContextCompat.getColor(getContext(), R.color.gray_01000000)));
            setSystemUIVisibility();
        }
        super.show();
    }

    private void setSystemUIVisibility() {
        if (getWindow() != null) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LOW_PROFILE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }
    }

    public void setListener(@Nullable Listener listener) {
        this.listener = listener;
    }
}
