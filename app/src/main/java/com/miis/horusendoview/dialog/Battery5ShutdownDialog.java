package com.miis.horusendoview.dialog;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.core.text.HtmlCompat;

import com.miis.horusendoview.R;
import com.miis.horusendoview.databinding.DialogBattery5ShutdownBinding;

import timber.log.Timber;

public class Battery5ShutdownDialog extends AlertDialog {

    @NonNull
    private final DialogBattery5ShutdownBinding binding;

    @Nullable
    private Listener listener;
    private int sec = 60;

    public Battery5ShutdownDialog(@NonNull Context context) {
        super(context);
        binding = DialogBattery5ShutdownBinding.inflate(LayoutInflater.from(context), null, false);
        init();
    }

    public Battery5ShutdownDialog(@NonNull Context context, int themeResId) {
        super(context, themeResId);
        binding = DialogBattery5ShutdownBinding.inflate(LayoutInflater.from(context), null, false);
        init();
    }

    public Battery5ShutdownDialog(@NonNull Context context, boolean cancelable, DialogInterface.OnCancelListener cancelListener) {
        super(context, cancelable, cancelListener);
        binding = DialogBattery5ShutdownBinding.inflate(LayoutInflater.from(context), null, false);
        init();
    }

    private void init() {
        binding.sleep.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                countDownTimer.cancel();

                final Listener listener = Battery5ShutdownDialog.this.listener;
                if (listener != null) {
                    listener.OnClickShutdown();
                }
                dismiss();
            }
        });

        binding.cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                countDownTimer.cancel();
                final Listener listener = Battery5ShutdownDialog.this.listener;
                if (listener != null) {
                    listener.OnClickCancel();
                }
                dismiss();
            }
        });

        setMsgView();
        setView(binding.getRoot());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setSystemUIVisibility();
        restartCountDownTimer();
    }

    @Override
    public void onStop() {
        countDownTimer.cancel();
        super.onStop();
        setSystemUIVisibility();
    }

    @Override
    public void show() {
        super.show();
        if (getWindow() != null) {
            getWindow().setDimAmount(0.5f); // 对话框外部阴影比重(0f ~ 1f)
            getWindow().setBackgroundDrawable(new ColorDrawable(ContextCompat.getColor(getContext(), R.color.gray_01000000)));
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
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            );
        }
    }

    public void setListener(@Nullable Listener listener) {
        this.listener = listener;
    }

    private void restartCountDownTimer() {
        countDownTimer.cancel();
        countDownTimer.start();
    }

    private final CountDownTimer countDownTimer = new CountDownTimer(1000L * 60, 1000L) {
        @Override
        public void onTick(long millisUntilFinished) {
            Timber.d("countDownTimer -> onTick millisUntilFinished=" + millisUntilFinished);
            if (isShowing()) {
                binding.getRoot().post(new Runnable() {
                    @Override
                    public void run() {
                        sec--;
                        setMsgView();
                    }
                });
            }
        }

        @Override
        public void onFinish() {
            Timber.d("countDownTimer -> onFinish");
            if (isShowing()) {
                binding.getRoot().post(new Runnable() {
                    @Override
                    public void run() {
                        setMsgView();
                        if (!isShowing()) {
                            return;
                        }
                        binding.sleep.performClick();
                    }
                });
            }
        }
    };

    private void setMsgView() {
        Runnable r = new Runnable() {
            @Override
            public void run() {
                int fSec = sec;
                binding.msg.setText(HtmlCompat.fromHtml(getContext().getString(R.string.dialog_battery_5_shutdown_msg_01, Integer.toString(fSec)), HtmlCompat.FROM_HTML_MODE_LEGACY));
            }
        };

        if (Looper.myLooper() == Looper.getMainLooper()) {
            r.run();
        } else {
            binding.getRoot().post(r);
        }
    }

    public interface Listener {
        void OnClickShutdown();

        void OnClickCancel();
    }
}
