package com.miis.horusendoview.dialog;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Looper;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.AnyThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;

import com.miis.horusendoview.R;
import com.miis.horusendoview.databinding.DialogCopyFilesProgressBinding;
import com.miis.horusendoview.manager.MyStorageManager;

import java.math.RoundingMode;
import java.text.DecimalFormat;

public class CopyFilesProgressDialog extends AlertDialog {

    @NonNull
    private final DialogCopyFilesProgressBinding binding = DialogCopyFilesProgressBinding.inflate(LayoutInflater.from(getContext()), null, false);

    @Nullable
    private Listener listener;

    public CopyFilesProgressDialog(Context context) {
        super(context);
        initView();
    }

    public CopyFilesProgressDialog(Context context, int themeResId) {
        super(context, themeResId);
        initView();
    }

    public CopyFilesProgressDialog(Context context, boolean cancelable, DialogInterface.OnCancelListener cancelListener) {
        super(context, cancelable, cancelListener);
        initView();
    }

    public interface Listener {
        void onCancel();
        void onTouch();
    }

    private void initView() {
        setView(binding.getRoot());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setCancelable(false);
        binding.progress.setMax(100);
        binding.progress.setProgressCompat(0, false);

        binding.cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final Listener listener = CopyFilesProgressDialog.this.listener;
                if (listener != null) {
                    listener.onCancel();
                }
                dismiss();
            }
        });

        binding.close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    public void show() {
        super.show();
        if (getWindow() != null) {
            getWindow().setDimAmount(0.5f);
            getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        setSystemUIVisibility();
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        final Listener listener = CopyFilesProgressDialog.this.listener;
        if (listener != null) {
            listener.onTouch();
        }
        return super.dispatchTouchEvent(ev);
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

    @UiThread
    public void setTitle(int fileSize, @NonNull String exportName) {
        SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder(getContext().getString(R.string.uploading_x1_files_to_x2, String.valueOf(fileSize), exportName));
        ForegroundColorSpan foregroundColorSpan = new ForegroundColorSpan(ContextCompat.getColor(getContext(), R.color.blue_1ebbee));
        spannableStringBuilder.setSpan(foregroundColorSpan, spannableStringBuilder.length() - exportName.length(), spannableStringBuilder.length(), Spannable.SPAN_EXCLUSIVE_INCLUSIVE);
        binding.title.setText(spannableStringBuilder);
    }

    @AnyThread
    public void setProgressData(@NonNull final MyStorageManager.ProgressData progressData) {
        Runnable r = new Runnable() {
            @Override
            public void run() {
                long remainingTimeSec = progressData.getRemainingTimeSec();
                if (remainingTimeSec < 0) {
                    remainingTimeSec = 0;
                }

                int progress = progressData.getProgress();
                if (progress < 0) {
                    progress = 0;
                }

                if (progress >= 100) {
                    binding.status.setText(R.string.complete);
                } else if (remainingTimeSec >= 60) {
                    binding.status.setText(getContext().getString(R.string.x_min, String.valueOf(remainingTimeSec / 60)));
                } else {
                    binding.status.setText(getContext().getString(R.string.x_sec, String.valueOf(remainingTimeSec)));
                }

                binding.progress.setProgressCompat(progress, progress > 0);

                DecimalFormat decimalFormat = new DecimalFormat("0.##");
                decimalFormat.setRoundingMode(RoundingMode.CEILING);

                long totalBytes = progressData.getTotalBytes();
                if (totalBytes < 0) {
                    totalBytes = 0;
                }
                double totalMB = (double) totalBytes / 1_048_576.0;

                long totalBytesCopied = progressData.getTotalBytesCopied();
                if (totalBytesCopied < 0) {
                    totalBytesCopied = 0;
                }
                double totalMbCopied = (double) totalBytesCopied / 1_048_576.0;

                binding.size.setText(getContext().getString(R.string.x1_mb_x2_mb, decimalFormat.format(totalMbCopied), decimalFormat.format(totalMB)));

                if (progress >= 100) {
                    binding.cancel.setVisibility(View.INVISIBLE);
                    binding.close.setVisibility(View.VISIBLE);
                } else {
                    binding.cancel.setVisibility(View.VISIBLE);
                    binding.close.setVisibility(View.INVISIBLE);
                }
            }
        };

        if (Looper.myLooper() == Looper.getMainLooper()) {
            r.run();
        } else {
            binding.getRoot().post(r);
        }
    }

    public void showFail() {
        final Runnable r = new Runnable() {
            @Override
            public void run() {
                binding.status.setText(R.string.fail);
                binding.status.setTextColor(ContextCompat.getColor(getContext(), R.color.red_f50f0a));

                binding.cancel.setVisibility(View.INVISIBLE);
                binding.close.setVisibility(View.VISIBLE);
            }
        };
        if (Looper.myLooper() == Looper.getMainLooper()) {
            r.run();
        } else {
            binding.getRoot().post(r);
        }
    }

    public void showCloseBtn() {
        final Runnable r = new Runnable() {
            @Override
            public void run() {
                binding.cancel.setVisibility(View.INVISIBLE);
                binding.close.setVisibility(View.VISIBLE);
            }
        };
        if (Looper.myLooper() == Looper.getMainLooper()) {
            r.run();
        } else {
            binding.getRoot().post(r);
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
