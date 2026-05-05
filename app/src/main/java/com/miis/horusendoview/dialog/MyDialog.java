package com.miis.horusendoview.dialog;

import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Looper;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.LinearLayoutCompat;
import androidx.core.content.ContextCompat;

import com.miis.horusendoview.R;
import com.miis.horusendoview.databinding.DialogMyBinding;

public class MyDialog extends AlertDialog {

    private DialogMyBinding binding;

    @Nullable
    private MyDialog.Listener listener = null;

    public interface Listener {
        void OnClickConfirm();

        void OnClickCancel();
    }

    public MyDialog(@NonNull Context context, boolean isReverseBin) {
        super(context);
        init(isReverseBin);
    }

    public MyDialog(@NonNull Context context, int themeResId) {
        super(context, themeResId);
        init(false);
    }

    public MyDialog(@NonNull Context context, boolean cancelable, @Nullable OnCancelListener cancelListener) {
        super(context, cancelable, cancelListener);
        init(false);
    }

    private void init(boolean isReverseBin) {
        binding = DialogMyBinding.inflate(LayoutInflater.from(getContext()), null, false);

        binding.confirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final MyDialog.Listener listener = MyDialog.this.listener;
                if (listener != null) {
                    listener.OnClickConfirm();
                }

                dismiss();
            }
        });

        binding.cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                final MyDialog.Listener listener = MyDialog.this.listener;
                if (listener != null) {
                    listener.OnClickCancel();
                }

                dismiss();
            }
        });

        setView(binding.getRoot());

        if (isReverseBin) {
            binding.btnLayout.removeView(binding.confirm);
            binding.btnLayout.removeView(binding.cancel);

            ((LinearLayoutCompat.LayoutParams) binding.cancel.getLayoutParams()).setMarginStart(getContext().getResources().getDimensionPixelSize(R.dimen.margin_16));
            binding.btnLayout.addView(binding.confirm);
            binding.btnLayout.addView(binding.cancel);
        }
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
        Window w = getWindow();
        if (w != null) {
            w.setDimAmount(0.5f); // 对话框外部阴影比重(0f ~ 1f)
            w.setBackgroundDrawable(new ColorDrawable(ContextCompat.getColor(getContext(), R.color.gray_01000000)));
        }
        setSystemUIVisibility();
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

    public void setListener(@Nullable MyDialog.Listener listener) {
        this.listener = listener;
    }

    public void setTitle(@Nullable String title) {
        final Runnable r = new Runnable() {
            @Override
            public void run() {
                binding.title.setText(title);
            }
        };

        if (Looper.myLooper() == Looper.getMainLooper()) {
            r.run();
        } else {
            binding.getRoot().post(r);
        }
    }

    public void setMsg(@Nullable String mag) {
        final Runnable r = new Runnable() {
            @Override
            public void run() {
                binding.msg.setText(mag);
            }
        };

        if (Looper.myLooper() == Looper.getMainLooper()) {
            r.run();
        } else {
            binding.getRoot().post(r);
        }
    }

    public void setConfirmText(@Nullable String text) {
        final Runnable r = new Runnable() {
            @Override
            public void run() {
                if (!TextUtils.isEmpty(text)) {
                    binding.confirm.setText(text);
                } else {
                    binding.confirm.setText(R.string.confirm);
                }
            }
        };

        if (Looper.myLooper() == Looper.getMainLooper()) {
            r.run();
        } else {
            binding.getRoot().post(r);
        }
    }

    public void showConfirm(boolean isShow) {
        final Runnable r = new Runnable() {
            @Override
            public void run() {
                if (isShow) {
                    binding.confirm.setVisibility(View.VISIBLE);
                } else {
                    binding.confirm.setVisibility(View.GONE);
                }
            }
        };

        if (Looper.myLooper() == Looper.getMainLooper()) {
            r.run();
        } else {
            binding.getRoot().post(r);
        }
    }

    public void setCancelText(@Nullable String text) {
        final Runnable r = new Runnable() {
            @Override
            public void run() {
                if (!TextUtils.isEmpty(text)) {
                    binding.cancel.setText(text);
                } else {
                    binding.cancel.setText(R.string.cancel);
                }
            }
        };

        if (Looper.myLooper() == Looper.getMainLooper()) {
            r.run();
        } else {
            binding.getRoot().post(r);
        }
    }

    public void showCancel(boolean isShow) {
        final Runnable r = new Runnable() {
            @Override
            public void run() {
                if (isShow) {
                    binding.cancel.setVisibility(View.VISIBLE);
                } else {
                    binding.cancel.setVisibility(View.GONE);
                }
            }
        };

        if (Looper.myLooper() == Looper.getMainLooper()) {
            r.run();
        } else {
            binding.getRoot().post(r);
        }
    }
}
