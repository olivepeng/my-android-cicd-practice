package com.miis.horusendoview.dialog;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.datepicker.CalendarConstraints;
import com.google.android.material.datepicker.MaterialCalendar;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.internal.CheckableImageButton;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;
import com.miis.horusendoview.R;
import com.miis.horusendoview.databinding.DialogDateTimeBinding;
import com.miis.horusendoview.tools.Tools;
import com.miis.horusendoview.tools.UiTool;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import timber.log.Timber;

public class DateTimeDialog extends AlertDialog {

    @NonNull
    private final DialogDateTimeBinding binding = DialogDateTimeBinding.inflate(LayoutInflater.from(getContext()), null, false);

    @NonNull
    private final FragmentManager fragmentManager;

    @NonNull
    private LocalDateTime dateTime = LocalDateTime.now().withSecond(0).withNano(0);

    @Nullable
    private MaterialDatePicker<?> datePickerDialog = null;

    @Nullable
    private MaterialTimePicker timePickerDialog = null;

    @Nullable
    private Listener listener = null;

    public DateTimeDialog(@NonNull FragmentManager fragmentManager, @NonNull Context context) {
        super(context);
        this.fragmentManager = fragmentManager;
        init();
    }

    public DateTimeDialog(@NonNull FragmentManager fragmentManager, @NonNull Context context, int themeResId) {
        super(context, themeResId);
        this.fragmentManager = fragmentManager;
        init();
    }

    public DateTimeDialog(@NonNull FragmentManager fragmentManager, @NonNull Context context, boolean cancelable, DialogInterface.OnCancelListener cancelListener) {
        super(context, cancelable, cancelListener);
        this.fragmentManager = fragmentManager;
        init();
    }

    private void init() {
        setView(binding.getRoot());
    }

    public interface Listener {
        void onClickConfirm(@NonNull LocalDateTime dateTime);

        void onClickCancel();
        void onTouch();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setSystemUIVisibility();

        fragmentManager.registerFragmentLifecycleCallbacks(
                fragmentLifecycleCallbacks,
                true
        );

        setDateTimeToView();

        binding.dateLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDatePickerDialog();
            }
        });

        binding.timeLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showTimePickerDialog();
            }
        });

        binding.confirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final Listener listener = DateTimeDialog.this.listener;
                if (listener != null) {
                    listener.onClickConfirm(getDateTime());
                }
                dismiss();
            }
        });

        binding.cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final Listener listener = DateTimeDialog.this.listener;
                if (listener != null) {
                    listener.onClickCancel();
                }
                dismiss();
            }
        });
    }

    @Override
    public void onStop() {
        super.onStop();
        fragmentManager.unregisterFragmentLifecycleCallbacks(fragmentLifecycleCallbacks);
        setSystemUIVisibility();
        cancelDatePickerDialog();
        cancelTimePickerDialog();
    }

    @Override
    public void show() {
        super.show();
        if (getWindow() != null) {
            getWindow().setDimAmount(0.5f); // 对话框外部阴影比重(0f ~ 1f)
            getWindow().setBackgroundDrawable(
                    new ColorDrawable(ContextCompat.getColor(getContext(), R.color.gray_01000000))
            );
        }
        setSystemUIVisibility();
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        final Listener listener = DateTimeDialog.this.listener;
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

    private void setDateTimeToView() {
        final Context context = getContext();
        boolean is24HourFormat = DateFormat.is24HourFormat(context);

        binding.date.setText(getDateTime().format(DateTimeFormatter.ISO_LOCAL_DATE));
        binding.time.setText(is24HourFormat
                ? getDateTime().toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm", Locale.US))
                : getDateTime().toLocalTime().format(DateTimeFormatter.ofPattern("hh:mm a", Locale.US)));
    }

    private void showDatePickerDialog() {
        cancelDatePickerDialog();

        MaterialDatePicker<?> datePicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("")
                .setSelection(getDateTime().atZone(ZoneOffset.systemDefault()).toInstant().toEpochMilli())
                .setInputMode(MaterialDatePicker.INPUT_MODE_CALENDAR)
                .setCalendarConstraints(new CalendarConstraints.Builder()
                        .setEnd(LocalDate.of(2037, 12 , 31).atTime(23 , 59 , 59).atZone(ZoneOffset.UTC).toInstant().toEpochMilli())
                        .build())
                .setPositiveButtonText(R.string.confirm)
                .setNegativeButtonText(R.string.cancel)
                .build();

        datePicker.addOnPositiveButtonClickListener(selection -> {
            LocalDate date = LocalDateTime.ofInstant(Instant.ofEpochMilli((Long) selection), ZoneId.systemDefault())
                    .toLocalDate();
            setDateTime(LocalDateTime.of(date, getDateTime().toLocalTime()));
        });

        datePicker.show(
                fragmentManager,
                MaterialDatePicker.class.getSimpleName()
        );
        datePickerDialog = datePicker;
    }

    private void cancelDatePickerDialog() {
        MaterialDatePicker<?> dialog = datePickerDialog;
        if (dialog != null && dialog.isVisible()) {
            if (dialog.isStateSaved()) {
                dialog.dismissAllowingStateLoss();
            } else {
                dialog.dismiss();
            }
        }
    }

    private void showTimePickerDialog() {
        cancelTimePickerDialog();
        boolean is24HourFormat = DateFormat.is24HourFormat(getContext());

        final MaterialTimePicker picker = new MaterialTimePicker.Builder()
                .setTimeFormat(is24HourFormat ? TimeFormat.CLOCK_24H : TimeFormat.CLOCK_12H)
                .setHour(getDateTime().getHour())
                .setMinute(getDateTime().getMinute())
                .setInputMode(MaterialTimePicker.INPUT_MODE_CLOCK)
                .setPositiveButtonText(R.string.confirm)
                .setNegativeButtonText(R.string.cancel)
                .setTitleText("")
                .build();

        picker.addOnPositiveButtonClickListener(selection -> {
            setDateTime(LocalDateTime.of(getDateTime().toLocalDate(), LocalTime.of(picker.getHour(), picker.getMinute())));
        });

        picker.show(
                fragmentManager,
                MaterialTimePicker.class.getSimpleName()
        );

        timePickerDialog = picker;
    }

    private void cancelTimePickerDialog() {
        final MaterialTimePicker dialog = timePickerDialog;
        if (dialog != null && dialog.isVisible()) {
            if (dialog.isStateSaved()) {
                dialog.dismissAllowingStateLoss();
            } else {
                dialog.dismiss();
            }
        }
    }

    private final FragmentManager.FragmentLifecycleCallbacks fragmentLifecycleCallbacks = new FragmentManager.FragmentLifecycleCallbacks() {

        @Override
        public void onFragmentViewCreated(
                @NonNull FragmentManager fm,
                @NonNull Fragment f,
                @NonNull View v,
                Bundle savedInstanceState) {
            super.onFragmentViewCreated(fm, f, v, savedInstanceState);
            if (f instanceof MaterialDatePicker<?>) {

                CheckableImageButton headerToggleButton = null;
                try {
                    headerToggleButton = (CheckableImageButton) Tools.getPrivateObject(f, "headerToggleButton");
                } catch (Exception e) {
                    Timber.e(e);
                }
                if (headerToggleButton != null) {
                    headerToggleButton.setVisibility(View.INVISIBLE);
                }

                LinearLayout mtrlCalendarMainPane = null;
                try {
                    if (f.getView() != null) {
                        mtrlCalendarMainPane = f.getView().findViewById(R.id.mtrl_calendar_main_pane);
                    }
                } catch (Exception e) {
                    Timber.e(e);
                }
                if (mtrlCalendarMainPane != null) {
                    mtrlCalendarMainPane.getLayoutParams().width = ViewGroup.LayoutParams.MATCH_PARENT;
                    mtrlCalendarMainPane.requestLayout();
                }
            }

            if (f instanceof MaterialCalendar<?>) {
                MaterialButton monthPrev = null;
                try {
                    monthPrev = (MaterialButton) Tools.getPrivateObject(f, "monthPrev");
                } catch (Exception e) {
                    Timber.e(e);
                }
                if (monthPrev != null) {
                    monthPrev.setIconResource(R.drawable.material_ic_keyboard_arrow_previous_black_40dp);
                }

                MaterialButton monthNext = null;
                try {
                    monthNext = (MaterialButton) Tools.getPrivateObject(f, "monthNext");
                } catch (Exception e) {
                    Timber.e(e);
                }
                if (monthNext != null) {
                    monthNext.setIconResource(R.drawable.material_ic_keyboard_arrow_next_black_40dp);
                }
            }

            if (f instanceof MaterialTimePicker) {
                ConstraintLayout mainLayout = (ConstraintLayout) f.getView();
                if (mainLayout != null) {
                    ConstraintSet constraintSet = new ConstraintSet();
                    constraintSet.clone(mainLayout);

                    // material_timepicker_view
                    constraintSet.connect(
                            R.id.material_timepicker_view,
                            ConstraintSet.TOP,
                            ConstraintSet.PARENT_ID,
                            ConstraintSet.TOP
                    );
                    constraintSet.connect(
                            R.id.material_timepicker_view,
                            ConstraintSet.BOTTOM,
                            ConstraintSet.PARENT_ID,
                            ConstraintSet.BOTTOM
                    );
                    constraintSet.connect(
                            R.id.material_timepicker_view,
                            ConstraintSet.START,
                            ConstraintSet.PARENT_ID,
                            ConstraintSet.START
                    );
                    constraintSet.connect(
                            R.id.material_timepicker_view,
                            ConstraintSet.END,
                            ConstraintSet.PARENT_ID,
                            ConstraintSet.END
                    );

                    // material_timepicker_cancel_button
                    constraintSet.clear(R.id.material_timepicker_cancel_button, ConstraintSet.TOP);
                    constraintSet.connect(
                            R.id.material_timepicker_cancel_button,
                            ConstraintSet.BOTTOM,
                            ConstraintSet.PARENT_ID,
                            ConstraintSet.BOTTOM
                    );
                    constraintSet.connect(
                            R.id.material_timepicker_cancel_button,
                            ConstraintSet.END,
                            R.id.material_timepicker_ok_button,
                            ConstraintSet.START
                    );

                    // material_timepicker_ok_button
                    constraintSet.clear(R.id.material_timepicker_ok_button, ConstraintSet.TOP);
                    constraintSet.connect(
                            R.id.material_timepicker_ok_button,
                            ConstraintSet.BOTTOM,
                            ConstraintSet.PARENT_ID,
                            ConstraintSet.BOTTOM
                    );
                    constraintSet.connect(
                            R.id.material_timepicker_ok_button,
                            ConstraintSet.END,
                            ConstraintSet.PARENT_ID,
                            ConstraintSet.END
                    );

                    constraintSet.applyTo(mainLayout);
                }

                TextView headerTitle = null;
                if (f.getView() != null) {
                    try {
                        headerTitle = f.getView().findViewById(com.google.android.material.R.id.header_title);
                    } catch (Exception e) {
                        Timber.e(e);
                    }
                }
                if (headerTitle != null) {
                    headerTitle.setVisibility(View.INVISIBLE);
                }

                MaterialButton modeButton = null;
                try {
                    modeButton = (MaterialButton) Tools.getPrivateObject(f, "modeButton");
                } catch (Exception e) {
                    Timber.e(e);
                }
                if (modeButton != null) {
                    modeButton.setVisibility(View.INVISIBLE);
                }

                ConstraintLayout timePickerView = null;
                if (f.getView() != null) {
                    try {
                        timePickerView = f.getView().findViewById(com.google.android.material.R.id.material_timepicker_view);
                    } catch (Exception e) {
                        Timber.e(e);
                    }
                }
                if (timePickerView != null) {
                    timePickerView.setScaleX(2f);
                    timePickerView.setScaleY(2f);
                }
            }
        }

        @Override
        public void onFragmentStarted(@NonNull FragmentManager fm, @NonNull Fragment f) {
            super.onFragmentStarted(fm, f);
            if (f instanceof MaterialDatePicker<?>) {
                final Dialog dialog = ((MaterialDatePicker<?>) f).getDialog();
                if (dialog != null) {
                    final Window window = dialog.getWindow();
                    if (window != null) {
                        final Context context = getContext();
                        final DisplayMetrics displayMetrics = UiTool.getDisplaySize(context);
                        int width = displayMetrics.widthPixels - (context.getResources().getDimensionPixelSize(R.dimen.margin_64) * 2);
                        int height = displayMetrics.heightPixels - (context.getResources().getDimensionPixelSize(R.dimen.margin_64) * 2);
                        window.setLayout(width, height);
                    }
                }
            }

            if (f instanceof MaterialTimePicker) {
                final Dialog dialog = ((MaterialTimePicker) f).getDialog();
                if (dialog != null) {
                    final Window window = dialog.getWindow();
                    if (window != null) {
                        final Context context = getContext(); // 请确保在上下文中定义
                        DisplayMetrics displayMetrics = UiTool.getDisplaySize(context);
                        int width = displayMetrics.widthPixels - (context.getResources().getDimensionPixelSize(R.dimen.margin_64) * 2);
                        int height = displayMetrics.heightPixels - (context.getResources().getDimensionPixelSize(R.dimen.margin_64) * 2);
                        window.setLayout(width, height);
                    }
                }
            }
        }

        @Override
        public void onFragmentResumed(@NonNull FragmentManager fm, @NonNull Fragment f) {
            super.onFragmentResumed(fm, f);
            if (f instanceof MaterialDatePicker<?>) {
                setTouchListener((DialogFragment) f);
            }
            if (f instanceof MaterialTimePicker) {
                setTouchListener((DialogFragment) f);
            }
        }

        @SuppressLint("ClickableViewAccessibility")
        private void setTouchListener(DialogFragment dialogFragment) {
            @Nullable final View parentPanel = dialogFragment.getView();
            @Nullable View parent = null;
            if (parentPanel != null) {
                parent = (View) parentPanel.getParent();
            }
            @Nullable View parent02 = null;
            if (parent != null) {
                parent02 = (View) parent.getParent();
            }
            @Nullable View parent03 = null;
            if (parent02 != null) {
                parent03 = (View) parent02.getParent();
            }
            if (parent03 instanceof FrameLayout) {
                final FrameLayout f = new FrameLayout(parent03.getContext()) {
                    private long lastTapTime = 0;
                    @Override
                    public boolean dispatchTouchEvent(MotionEvent ev) {
                        Timber.d("setTouchListener -> FrameLayout -> OnTouchListener -> onTouch");
                        final Listener listener = DateTimeDialog.this.listener;
                        if (listener != null) {
                            listener.onTouch();
                        }
                        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
                            long currentTime = System.currentTimeMillis();
                            long diff = currentTime - lastTapTime;
                            lastTapTime = currentTime;
                            if (diff < 300) {
                                return true; // 攔截雙擊
                            }
                        }
                        return super.dispatchTouchEvent(ev);
                    }
                };
                f.setLayoutParams(new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
                ((FrameLayout) parent03).addView(f);
            }
            final Dialog dialog = dialogFragment.getDialog();
            if (dialog != null) {
                final Window window = dialog.getWindow();
                if (window != null) {
                    final View decorView = window.getDecorView();
                    decorView.setOnTouchListener((v, event) -> {
                        Timber.d("setTouchListener -> DecorView -> OnTouchListener -> onTouch");
                        final Listener listener = DateTimeDialog.this.listener;
                        if (listener != null) {
                            listener.onTouch();
                        }
                        return false;
                    });
                }
            }
        }
    };

    @Nullable
    public Listener getListener() {
        return listener;
    }

    public void setListener(@Nullable Listener listener) {
        this.listener = listener;
    }

    @NonNull
    public LocalDateTime getDateTime() {
        return dateTime;
    }

    public void setDateTime(@NonNull LocalDateTime dateTime) {
        this.dateTime = dateTime;
        setDateTimeToView();
    }
}
