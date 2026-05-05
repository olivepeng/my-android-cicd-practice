package com.miis.horusendoview.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.SeekBar;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.miis.horusendoview.MyApplication;
import com.miis.horusendoview.activity.MainActivity;
import com.miis.horusendoview.databinding.ViewCameraSettingsBinding;
import com.miis.horusendoview.fragment.CameraFragment;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import timber.log.Timber;

public final class CameraSettingsView extends ConstraintLayout {
    @NotNull
    private ViewCameraSettingsBinding binding;
    @Nullable
    private Listener listener;

    public CameraSettingsView(@NonNull Context context) {
        super(context);
        this.binding = ViewCameraSettingsBinding.inflate(LayoutInflater.from(this.getContext()), this, true);
        init();
    }

    public CameraSettingsView(@NonNull Context context, @androidx.annotation.Nullable AttributeSet attrs) {
        super(context, attrs);
        this.binding = ViewCameraSettingsBinding.inflate(LayoutInflater.from(this.getContext()), this, true);
        init();
    }

    public CameraSettingsView(@NonNull Context context, @androidx.annotation.Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.binding = ViewCameraSettingsBinding.inflate(LayoutInflater.from(this.getContext()), this, true);
        init();
    }

    public CameraSettingsView(@NonNull Context context, @androidx.annotation.Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        this.binding = ViewCameraSettingsBinding.inflate(LayoutInflater.from(this.getContext()), this, true);
        init();
    }

    @NotNull
    public ViewCameraSettingsBinding getBinding() {
        return this.binding;
    }

    @Nullable
    public Listener getListener() {
        return this.listener;
    }

    public void setListener(@Nullable Listener listener) {
        this.listener = listener;
    }

    private void init() {

        this.binding.noTouch.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                return true;
            }
        });
        this.binding.close.setOnClickListener(new View.OnClickListener() {
            public void onClick(View it) {
                setVisibility(View.GONE);
            }
        });
        this.binding.reset.setOnClickListener(new View.OnClickListener() {
            public void onClick(View it) {
                final Listener listener = getListener();
                if (listener != null) {
                    listener.onReset();
                }
            }
        });
        this.binding.brightnessSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(@NotNull SeekBar seekBar, int progress, boolean fromUser) {
                final Listener listener = CameraSettingsView.this.getListener();
                if (listener != null) {
                    listener.onBrightnessChanged(progress, fromUser);
                }
            }

            public void onStartTrackingTouch(@NotNull SeekBar seekBar) {
                final String str = String.format("User adjust brightnessSeekBar start at %d",
                        seekBar.getProgress());
                Timber.i(str);
            }

            public void onStopTrackingTouch(@NotNull SeekBar seekBar) {
                final String str = String.format("User adjust brightnessSeekBar stop at %d",
                        seekBar.getProgress());
                Timber.i(str);
            }
        });
        this.binding.contrastSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(@NotNull SeekBar seekBar, int progress, boolean fromUser) {
                final Listener listener = getListener();
                if (listener != null) {
                    listener.onContrastChanged(progress, fromUser);
                }
            }

            public void onStartTrackingTouch(@NotNull SeekBar seekBar) {
                final String str = String.format("User adjust contrastSeekBar start at %d",
                        seekBar.getProgress());
                Timber.i(str);
            }

            public void onStopTrackingTouch(@NotNull SeekBar seekBar) {
                final String str = String.format("User adjust contrastSeekBar stop at %d",
                        seekBar.getProgress());
                Timber.i(str);
            }
        });
        this.binding.colourSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(@NotNull SeekBar seekBar, int progress, boolean fromUser) {
                final Listener listener = CameraSettingsView.this.getListener();
                if (listener != null) {
                    listener.onColourChanged(progress, fromUser);
                }

            }

            public void onStartTrackingTouch(@NotNull SeekBar seekBar) {
                final String str = String.format("User adjust colourSeekBar start at %d",
                        seekBar.getProgress());
                Timber.i(str);
            }

            public void onStopTrackingTouch(@NotNull SeekBar seekBar) {
                final String str = String.format("User adjust colourSeekBar stop at %d",
                        seekBar.getProgress());
                Timber.i(str);
            }
        });
        this.binding.sharpnessSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(@NotNull SeekBar seekBar, int progress, boolean fromUser) {
                final Listener listener = CameraSettingsView.this.getListener();
                if (listener != null) {
                    listener.onSharpnessChanged(progress, fromUser);
                }

            }

            public void onStartTrackingTouch(@NotNull SeekBar seekBar) {
                final String str = String.format("User adjust sharpnessSeekBar start at %d",
                        seekBar.getProgress());
                Timber.i(str);
            }

            public void onStopTrackingTouch(@NotNull SeekBar seekBar) {
                final String str = String.format("User adjust sharpnessSeekBar stop at %d",
                        seekBar.getProgress());
                Timber.i(str);
            }
        });
    }

    @Override
    protected void onVisibilityChanged(@NonNull View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);
        if(visibility == GONE){
            Context context = getContext();
            if (context == null) {
                Timber.w("[onVisibilityChanged] context == null");
                return;
            }

            context = context.getApplicationContext();
            if (!(context instanceof MyApplication)) {
                Timber.w("[onVisibilityChanged] Application context is not MyApplication");
                return;
            }

            MyApplication myApplication = (MyApplication) context;
            MainActivity mainActivity = myApplication.getMainActivity();
            if (mainActivity == null) {
                Timber.w("[onVisibilityChanged] mainActivity == null");
                return;
            }

            CameraFragment cameraFragment = mainActivity.getCameraFragment();
            if (cameraFragment == null) {
                Timber.w("[onVisibilityChanged] cameraFragment == null");
                return;
            }

            PreviewWindowsView previewWindowsView = cameraFragment.getBinding().previewWindowsView;
            if (previewWindowsView.getVisibility() == VISIBLE) {
                previewWindowsView.setInfoShow(true);
            }
        }
    }


    public interface Listener {
        void onBrightnessChanged(int value, boolean fromUser);

        void onContrastChanged(int value, boolean fromUser);

        void onColourChanged(int value, boolean fromUser);

        void onSharpnessChanged(int value, boolean fromUser);

        void onReset();
    }
}

