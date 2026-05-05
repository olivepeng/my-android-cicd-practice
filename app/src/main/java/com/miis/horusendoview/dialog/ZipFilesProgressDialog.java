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
import android.view.Window;

import androidx.annotation.AnyThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import androidx.appcompat.app.AlertDialog;

import com.miis.horusendoview.R;
import com.miis.horusendoview.databinding.DialogZipFilesProgressBinding;
import com.miis.horusendoview.manager.MyStorageManager;

import java.io.File;

public class ZipFilesProgressDialog extends AlertDialog {

    @NonNull
    private final DialogZipFilesProgressBinding binding = DialogZipFilesProgressBinding.inflate(LayoutInflater.from(getContext()), null, false);

    @Nullable
    private Listener listener;

    @Nullable
    private File storageDirectoryFile = null;

    public ZipFilesProgressDialog(Context context) {
        super(context);
        setView(binding.getRoot());
        setCancelable(false);
    }

    public ZipFilesProgressDialog(Context context, int themeResId) {
        super(context, themeResId);
        setView(binding.getRoot());
        setCancelable(false);
    }

    public ZipFilesProgressDialog(Context context, boolean cancelable, DialogInterface.OnCancelListener cancelListener) {
        super(context, cancelable, cancelListener);
        setView(binding.getRoot());
        setCancelable(cancelable);
    }

    public interface Listener {
        void onCancel();
        void onTouch();
    }

    public void setListener(@Nullable Listener listener) {
        this.listener = listener;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding.cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final Listener listener = ZipFilesProgressDialog.this.listener;
                if (listener != null) {
                    listener.onCancel();
                }
                dismiss();
            }
        });

        binding.close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dismiss();
            }
        });
    }

    @Override
    public void show() {
        super.show();
        Window window = getWindow();
        if (window != null) {
            window.setDimAmount(0.5f);
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        setSystemUIVisibility();
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        final Listener listener = ZipFilesProgressDialog.this.listener;
        if (listener != null) {
            listener.onTouch();
        }
        return super.dispatchTouchEvent(ev);
    }

    private void setSystemUIVisibility() {
        if (getWindow() != null) {
            getWindow().getDecorView().setSystemUiVisibility(
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

    @UiThread
    public void setTitle(int fileSize, @NonNull String exportName) {
        String text = getContext().getString(R.string.uploading_x1_files_to_x2, String.valueOf(fileSize), exportName);
        SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder(text);
        ForegroundColorSpan foregroundColorSpan = new ForegroundColorSpan(getContext().getColor(R.color.blue_1ebbee));
        spannableStringBuilder.setSpan(
                foregroundColorSpan,
                spannableStringBuilder.length() - exportName.length(),
                spannableStringBuilder.length(),
                Spannable.SPAN_EXCLUSIVE_INCLUSIVE
        );
        binding.title.setText(spannableStringBuilder);
    }

    @AnyThread
    public void setProgressData(@NonNull MyStorageManager.ZipProgressData progressData) {
        Runnable r = new Runnable() {
            @Override
            public void run() {
                int progress = (int) progressData.getProgress();
                if (progress < 0) {
                    progress = 0;
                }

                if (progress >= 100) {
                    binding.status.setText(R.string.complete);
                } else {
                    binding.status.setText("");
                }

                binding.progress.setProgressCompat(progress, progress > 0);

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

    @AnyThread
    public void showFail() {
        Runnable r = new Runnable() {
            @Override
            public void run() {
                binding.status.setText(R.string.fail);
                binding.status.setTextColor(getContext().getColor(R.color.red_f50f0a));

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

    public boolean isComplete() {
        return binding.progress.getProgress() >= 100;
    }

    @AnyThread
    public void showCloseBtn() {
        Runnable r = new Runnable() {
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

    @NonNull
    public DialogZipFilesProgressBinding getBinding() {
        return binding;
    }

    @Nullable
    public Listener getListener() {
        return listener;
    }

    @Nullable
    public File getStorageDirectoryFile() {
        return storageDirectoryFile;
    }

    public void setStorageDirectoryFile(@Nullable File storageDirectoryFile) {
        this.storageDirectoryFile = storageDirectoryFile;
    }
}
