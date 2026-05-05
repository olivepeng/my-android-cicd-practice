package com.miis.horusendoview.dialog;

import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.core.text.HtmlCompat;

import com.miis.horusendoview.R;
import com.miis.horusendoview.databinding.DialogStandbyNotificationTimeBinding;

public class StandbyNotificationTimeDialog extends AlertDialog {

    private final String TAG = StandbyNotificationTimeDialog.class.getSimpleName();

    private DialogStandbyNotificationTimeBinding binding;

    @Nullable
    private StandbyNotificationTimeDialog.Listener listener = null;

    private int sec = 60;

    public interface Listener {
        void OnClickSleep();

        void OnClickCancel();
    }

    public StandbyNotificationTimeDialog(@NonNull Context context) {
        super(context);
        init();
    }

    public StandbyNotificationTimeDialog(@NonNull Context context, int themeResId) {
        super(context, themeResId);
        init();
    }

    public StandbyNotificationTimeDialog(@NonNull Context context, boolean cancelable, @Nullable OnCancelListener cancelListener) {
        super(context, cancelable, cancelListener);
        init();
    }

    private void init() {
        binding = DialogStandbyNotificationTimeBinding.inflate(LayoutInflater.from(getContext()), null, false);

        binding.sleep.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                countDownTimer.cancel();

                final StandbyNotificationTimeDialog.Listener listener = StandbyNotificationTimeDialog.this.listener;
                if (listener != null) {
                    listener.OnClickSleep();
                }

                dismiss();
            }
        });

        binding.cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                countDownTimer.cancel();

                final StandbyNotificationTimeDialog.Listener listener = StandbyNotificationTimeDialog.this.listener;
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
    protected void onStop() {
        countDownTimer.cancel();
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

    public void setListener(@Nullable StandbyNotificationTimeDialog.Listener listener) {
        this.listener = listener;
    }

    private void restartCountDownTimer() {
        countDownTimer.cancel();
        countDownTimer.start();
    }

    private final CountDownTimer countDownTimer = new CountDownTimer(1000 * 60L, 1000L) {
        @Override
        public void onTick(long millisUntilFinished) {
            Log.d(TAG, "countDownTimer -> onTick millisUntilFinished=" + millisUntilFinished);
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
            Log.d(TAG, "countDownTimer -> onFinish");

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
        final Runnable r = new Runnable() {
            @Override
            public void run() {
                Context context = getContext();
//                if (context == null) {
//                    return;
//                }
                final int fSec = sec;

                binding.msg.setText(HtmlCompat.fromHtml(context.getString(R.string.standby_notification_time_dialog_msg_01, String.valueOf(fSec)),HtmlCompat.FROM_HTML_MODE_LEGACY));
            }
        };

        if (Looper.myLooper() == Looper.getMainLooper()) {
            r.run();
        } else {
            binding.getRoot().post(r);
        }
    }
}
