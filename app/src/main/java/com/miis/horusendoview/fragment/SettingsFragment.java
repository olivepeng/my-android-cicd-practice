package com.miis.horusendoview.fragment;

import static com.miis.horusendoview.fragment.DataManagementFragment.CheckPasswordRule;
import static com.miis.horusendoview.type.UserRoleType.GUEST;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.os.Looper;
import android.os.SystemClock;
import android.os.storage.StorageVolume;
import android.provider.Settings;
import android.text.InputFilter;
import android.text.InputType;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.annotation.UiThread;
import androidx.annotation.VisibleForTesting;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import com.app.customkeyboard.keyboard.CustomKeyboardView;
import com.blankj.utilcode.util.FileUtils;
import com.miis.horusendoview.BuildConfig;
import com.miis.horusendoview.MyApplication;
import com.miis.horusendoview.R;
import com.miis.horusendoview.activity.MainActivity;
import com.miis.horusendoview.data.MySelectStorageDialogData;
import com.miis.horusendoview.databinding.FragmentSettingsBinding;
import com.miis.horusendoview.dialog.DateTimeDialog;
import com.miis.horusendoview.dialog.LanguageSelectDialog;
import com.miis.horusendoview.dialog.MyDialog;
import com.miis.horusendoview.dialog.MySelectStorageDialog;
import com.miis.horusendoview.dialog.ZipFilesProgressDialog;
import com.miis.horusendoview.dialog.ZipPasswordDialog;
import com.miis.horusendoview.errorcode.Error;
import com.miis.horusendoview.errorcode.IErrorCode;
import com.miis.horusendoview.log.LogQueue;
import com.miis.horusendoview.manager.MyStorageManager;
import com.miis.horusendoview.manager.SharedPreferencesManager;
import com.miis.horusendoview.manager.SystemPropertiesUnit;
import com.miis.horusendoview.roomDataBase.userTbData.UserTbData;
import com.miis.horusendoview.tools.Tools;
import com.miis.horusendoview.type.BootAutoDeleteFileType;
import com.miis.horusendoview.type.ImageRotateType;
import com.miis.horusendoview.type.MaximumVideoDurationType;
import com.miis.horusendoview.type.StandbyNotificationTimeType;
import com.miis.horusendoview.type.UserRoleType;

import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import io.reactivex.rxjava3.core.Observer;
import io.reactivex.rxjava3.disposables.Disposable;
import timber.log.Timber;

public class SettingsFragment extends Fragment {

    private static final String TAG = SettingsFragment.class.getSimpleName();
    @Nullable
    private FragmentSettingsBinding binding = null;

    @Nullable
    private DateTimeDialog dateTimeDialog = null;

    @Nullable
    private LanguageSelectDialog languageSelectDialog = null;

    @NonNull
    private final ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(3);

    @NonNull
    private LocalDateTime startDateTime;

    @NonNull
    private LocalDateTime endDateTime;

    @Nullable
    private Disposable exportLogDisposable;

    @Nullable
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.getDefault());

    @Nullable
    private ZipPasswordDialog zipPasswordDialog;

    @Nullable
    private ZipFilesProgressDialog zipFilesProgressDialog;

    @Nullable
    private MySelectStorageDialog mySelectStorageDialog;

    @Nullable
    private MyDialog myDialog;

    @NonNull
    public static SettingsFragment newInstance() {
        return new SettingsFragment();
    }


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (scheduledExecutorService instanceof ScheduledThreadPoolExecutor) {
            ScheduledThreadPoolExecutor s = (ScheduledThreadPoolExecutor) scheduledExecutorService;
            s.setRemoveOnCancelPolicy(true);
        }

        this.startDateTime = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);
        this.endDateTime = this.startDateTime.withHour(23).withMinute(59).withSecond(59).withNano(999999999);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (binding != null) {
            ViewGroup parent = (ViewGroup) binding.getRoot().getParent();
            if (parent != null) {
                parent.endViewTransition(binding.getRoot());
                parent.removeView(binding.getRoot());
            }
        }
        if (binding == null) {
            binding = FragmentSettingsBinding.inflate(inflater, container, false);
        }
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        final FragmentSettingsBinding binding = SettingsFragment.this.binding;
        if (binding == null) {
            return;
        }
        binding.dateTimeLayout.setOnClickListener(dateTimeLayoutOnClickListener);
        binding.languageLayout.setOnClickListener(v -> showLanguageSelectDialog());
//        binding.language.setText(Locale.getDefault().getDisplayName(Locale.getDefault()));

        binding.swUpgradingLayout.setOnClickListener(upgradeOnClickListener);
        binding.swUpgrading.setOnClickListener(upgradeOnClickListener);

        cpuTemperatureRunnable.run();

        binding.exportLogLayout.setOnClickListener(exportLogOnClickListener);
        binding.startDateTimeLayout.setOnClickListener(exportLogOnClickListener);
        binding.endDateTimeLayout.setOnClickListener(exportLogOnClickListener);
        binding.btnExportLog.setOnClickListener(exportLogOnClickListener);
//        binding.WifiLayout.setOnLongClickListener(wifiSetClickListener);
        MyStorageManager.getInstance().addListener(myStorageManagerListener);

        binding.softwareVersion.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                Timber.d("[softwareVersion:LongClick] "+LogQueue.getId(view));
                showPasswordDialog();
                return false;
            }
        });

        final FragmentActivity activity = getActivity();
        if (activity instanceof MainActivity) {
            // 註冊虛擬鍵盤
            MainActivity mainActivity = (MainActivity) activity;
            if (binding != null) {
                mainActivity.getBinding().customKeyboardView.unregisterEditText(binding.editTextPassword);
                mainActivity.getBinding().customKeyboardView.registerEditText(
                        CustomKeyboardView.KeyboardType.NUMBER,
                        binding.editTextPassword
                );
            }
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        setStandbyNotificationTimeView();
        setMaximumVideoDurationView();
        setImageRotateView();
        setDeviceInfoView();
        setDateTimeDisplayFormat();
        setScreenAutoRotate();
        setAutoDeleteFile();
        setSwUpgradingLayout();
        setExportLogLayout();
    }

    @Override
    public void onResume() {
        super.onResume();
        final FragmentSettingsBinding binding = SettingsFragment.this.binding;
        if (binding == null) {
            return;
        }
        binding.textClock.refreshTime();
        delete_updatezip();
    }

    @Override
    public void onPause() {
        super.onPause();
        cancelDateTimeDialog();
        cancelLanguageSelectDialog();
    }

    @Override
    public void onStop() {
        super.onStop();
        final FragmentSettingsBinding binding = SettingsFragment.this.binding;
        if (binding == null) {
            return;
        }
        binding.standbyNotificationTimeRadioGroup.setOnCheckedChangeListener(null);
        binding.maximumVideoDurationRadioGroup.setOnCheckedChangeListener(null);
        binding.imageRotateRadioGroup.setOnCheckedChangeListener(null);
        binding.dateTimeDisplayFormatLayoutRadioGroup.setOnCheckedChangeListener(null);
        binding.screenAutoRotateRadioGroup.setOnCheckedChangeListener(null);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        final FragmentSettingsBinding binding = SettingsFragment.this.binding;
        if (binding == null) {
            return;
        }
        binding.cpuTemperature.removeCallbacks(cpuTemperatureRunnable);

        final FragmentActivity activity = getActivity();
        if (activity instanceof MainActivity && binding != null) {
            // 解註冊虛擬鍵盤
            MainActivity mainActivity = (MainActivity) activity;
            mainActivity.getBinding().customKeyboardView.unregisterEditText(binding.editTextPassword);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            scheduledExecutorService.shutdown();
        } catch (Exception e) {
            Timber.e(e);
        }
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        Timber.d("onHiddenChanged hidden=%s", hidden);
        if (hidden) {
            cancelDateTimeDialog();
            cancelLanguageSelectDialog();
        } else {
            setDeviceInfoView();
            setStandbyNotificationTimeView();
            setMaximumVideoDurationView();
            setImageRotateView();
            //setDeviceInfoView();
            setDateTimeDisplayFormat();
            setScreenAutoRotate();
            setAutoDeleteFile();
            setSwUpgradingLayout();
            setExportLogLayout();

            if (getActivity() instanceof MainActivity) {
                MainActivity activity = (MainActivity) getActivity();
                activity.setTabBtnSelected(activity.getBinding().ibSettings);
                final FragmentSettingsBinding binding = SettingsFragment.this.binding;
                if (binding != null) {
                    binding.getRoot().postDelayed(() -> activity.setTabBtnClickable(true), MainActivity.TAB_BTN_CLICKABLE_ON_DELAY);
                }
            }
        }
    }

    @UiThread
    private void setStandbyNotificationTimeView() {
        final FragmentSettingsBinding binding = SettingsFragment.this.binding;
        if (binding == null) {
            return;
        }
        binding.standbyNotificationTimeRadioGroup.setOnCheckedChangeListener(null);
        binding.standbyNotificationTimeRadioGroup.clearCheck();

        try {
            scheduledExecutorService.execute(new Runnable() {
                @Override
                public void run() {
                    StandbyNotificationTimeType standbyNotificationTimeType = SharedPreferencesManager.getInstance().getStandbyNotificationTimeType();

                    binding.getRoot().post(new Runnable() {
                        @Override
                        public void run() {
                            if(standbyNotificationTimeType == StandbyNotificationTimeType.MIN_10){
                                binding.standbyNotificationTime10MinRadioButton.toggle();
                            } else if (standbyNotificationTimeType == StandbyNotificationTimeType.MIN_30) {
                                binding.standbyNotificationTime30MinRadioButton.toggle();
                            } else if (standbyNotificationTimeType == StandbyNotificationTimeType.MIN_60) {
                                binding.standbyNotificationTime60MinRadioButton.toggle();
                            } else if (standbyNotificationTimeType == StandbyNotificationTimeType.NEVER) {
                                binding.standbyNotificationTimeNeverRadioButton.toggle();
                            } else {
                                binding.standbyNotificationTimeNeverRadioButton.toggle();
                            }
                            binding.standbyNotificationTimeRadioGroup.setOnCheckedChangeListener(standbyNotificationTimeOnCheckedChangeListener);

                            MyApplication myApplication = getMyApplication();
                            UserRoleType t = null;
                            if (myApplication != null && myApplication.getLoginUserTbData() != null) {
                                t = myApplication.getLoginUserTbData().getRoleType();
                            }
                            binding.standbyNotificationTimeRadioGroup.setEnabled(t != null && t != GUEST);
                            binding.standbyNotificationTime10MinRadioButton.setEnabled(t != null && t != GUEST);
                            binding.standbyNotificationTime30MinRadioButton.setEnabled(t != null && t != GUEST);
                            binding.standbyNotificationTime60MinRadioButton.setEnabled(t != null && t != GUEST);
                            binding.standbyNotificationTimeNeverRadioButton.setEnabled(t != null && t != GUEST);
                        }
                    });
                }
            });
        } catch (Exception e) {
            Timber.e(e);
        }

    }

    private final RadioGroup.OnCheckedChangeListener standbyNotificationTimeOnCheckedChangeListener =
            new RadioGroup.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(RadioGroup group, int checkedId) {
                    if (checkedId == -1) {
                        return;
                    }
                    switch (checkedId) {
                        case R.id.standbyNotificationTime10MinRadioButton:
                            SharedPreferencesManager.getInstance()
                                    .saveStandbyNotificationTimeType(StandbyNotificationTimeType.MIN_10);
                            break;
                        case R.id.standbyNotificationTime30MinRadioButton:
                            SharedPreferencesManager.getInstance()
                                    .saveStandbyNotificationTimeType(StandbyNotificationTimeType.MIN_30);
                            break;
                        case R.id.standbyNotificationTime60MinRadioButton:
                            SharedPreferencesManager.getInstance()
                                    .saveStandbyNotificationTimeType(StandbyNotificationTimeType.MIN_60);
                            break;
                        case R.id.standbyNotificationTimeNeverRadioButton:
                            SharedPreferencesManager.getInstance()
                                    .saveStandbyNotificationTimeType(StandbyNotificationTimeType.NEVER);
                            break;
                    }
                    Activity activity = getActivity();
                    if (activity instanceof MainActivity) {
                        ((MainActivity) activity).restartStandbyNotificationTimeServiceTimer();
                    }
                }
            };

    @UiThread
    private void setMaximumVideoDurationView() {
        if (binding != null) {
            binding.maximumVideoDurationRadioGroup.setOnCheckedChangeListener(null);
            binding.maximumVideoDurationRadioGroup.clearCheck();

            try {
                scheduledExecutorService.execute(new Runnable() {
                    @Override
                    public void run() {
                        MaximumVideoDurationType maximumVideoDurationType = SharedPreferencesManager.getInstance().getMaximumVideoDurationType();

                        binding.getRoot().post(new Runnable() {
                            @Override
                            public void run() {
                                if (maximumVideoDurationType == MaximumVideoDurationType.MIN_60) {
                                    binding.maximumVideoDuration60MinRadioButton.toggle();
                                } else if (maximumVideoDurationType == MaximumVideoDurationType.MIN_90) {
                                    binding.maximumVideoDuration90MinRadioButton.toggle();
                                } else if (maximumVideoDurationType == MaximumVideoDurationType.MIN_120) {
                                    binding.maximumVideoDuration120MinRadioButton.toggle();
                                } else {
                                    binding.maximumVideoDuration30MinRadioButton.toggle();
                                }

                                binding.maximumVideoDurationRadioGroup.setOnCheckedChangeListener(maximumVideoDurationOnCheckedChangeListener);

                                MyApplication myApplication = getMyApplication();
                                UserRoleType t = null;
                                if (myApplication != null && myApplication.getLoginUserTbData() != null) {
                                    t = myApplication.getLoginUserTbData().getRoleType();
                                }
                                binding.maximumVideoDurationRadioGroup.setEnabled(t != null && t != UserRoleType.GUEST);
                                binding.maximumVideoDuration30MinRadioButton.setEnabled(t != null && t != UserRoleType.GUEST);
                                binding.maximumVideoDuration60MinRadioButton.setEnabled(t != null && t != UserRoleType.GUEST);
                                binding.maximumVideoDuration90MinRadioButton.setEnabled(t != null && t != UserRoleType.GUEST);
                                binding.maximumVideoDuration120MinRadioButton.setEnabled(t != null && t != UserRoleType.GUEST);
                            }
                        });
                    }
                });
            } catch (Exception e) {
                Timber.e(e);
            }
        }
    }

    /**
     * maximumVideoDuration 的 OnCheckedChangeListener
     */
    private final RadioGroup.OnCheckedChangeListener maximumVideoDurationOnCheckedChangeListener =
            new RadioGroup.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(RadioGroup group, int checkedId) {
                    switch (checkedId) {
                        case R.id.maximumVideoDuration30MinRadioButton:
                            SharedPreferencesManager.getInstance()
                                    .saveMaximumVideoDurationType(MaximumVideoDurationType.MIN_30);
                            break;
                        case R.id.maximumVideoDuration60MinRadioButton:
                            SharedPreferencesManager.getInstance()
                                    .saveMaximumVideoDurationType(MaximumVideoDurationType.MIN_60);
                            break;
                        case R.id.maximumVideoDuration90MinRadioButton:
                            SharedPreferencesManager.getInstance()
                                    .saveMaximumVideoDurationType(MaximumVideoDurationType.MIN_90);
                            break;
                        case R.id.maximumVideoDuration120MinRadioButton:
                            SharedPreferencesManager.getInstance()
                                    .saveMaximumVideoDurationType(MaximumVideoDurationType.MIN_120);
                            break;
                    }
                }
            };

    /**
     * 設定資料給 ImageRotate 的 view
     */
    @UiThread
    private void setImageRotateView() {
        if (binding != null) {
            binding.imageRotateRadioGroup.setOnCheckedChangeListener(null);
            binding.imageRotateRadioGroup.clearCheck();
        }

        try {
            scheduledExecutorService.execute(new Runnable() {
                @Override
                public void run() {
                    ImageRotateType imageRotateType = SharedPreferencesManager.getInstance().getImageRotateType();

                    binding.getRoot().post(new Runnable() {
                        @Override
                        public void run() {
                            if (imageRotateType == ImageRotateType.ANGLE_90) {
                                binding.imageRotate90RadioButton.toggle();
                            } else if (imageRotateType == ImageRotateType.ANGLE_180) {
                                binding.imageRotate180RadioButton.toggle();
                            } else if (imageRotateType == ImageRotateType.ANGLE_270) {
                                binding.imageRotate270RadioButton.toggle();
                            } else {
                                binding.imageRotateNormalRadioButton.toggle();
                            }

                            binding.imageRotateRadioGroup.setOnCheckedChangeListener(imageRotateOnCheckedChangeListener);

                            MyApplication myApplication = getMyApplication();
                            UserRoleType t = null;
                            if (myApplication != null && myApplication.getLoginUserTbData() != null) {
                                t = myApplication.getLoginUserTbData().getRoleType();
                            }
                            if (binding != null) {
                                binding.imageRotateRadioGroup.setEnabled(t != null && t != UserRoleType.GUEST);
                                binding.imageRotateNormalRadioButton.setEnabled(t != null && t != UserRoleType.GUEST);
                                binding.imageRotate90RadioButton.setEnabled(t != null && t != UserRoleType.GUEST);
                                binding.imageRotate180RadioButton.setEnabled(t != null && t != UserRoleType.GUEST);
                                binding.imageRotate270RadioButton.setEnabled(t != null && t != UserRoleType.GUEST);
                            }
                        }
                    });
                }
            });
        } catch (Exception e) {
            Timber.e(e);
        }

    }

    /**
     * imageRotate 的 OnCheckedChangeListener
     */
    private final RadioGroup.OnCheckedChangeListener imageRotateOnCheckedChangeListener =
            (group, checkedId) -> {
                switch (checkedId) {
                    case R.id.imageRotateNormalRadioButton:
                        SharedPreferencesManager.getInstance()
                                .saveImageRotateType(ImageRotateType.NORMAL);
                        break;

                    case R.id.imageRotate90RadioButton:
                        SharedPreferencesManager.getInstance()
                                .saveImageRotateType(ImageRotateType.ANGLE_90);
                        break;

                    case R.id.imageRotate180RadioButton:
                        SharedPreferencesManager.getInstance()
                                .saveImageRotateType(ImageRotateType.ANGLE_180);
                        break;

                    case R.id.imageRotate270RadioButton:
                        SharedPreferencesManager.getInstance()
                                .saveImageRotateType(ImageRotateType.ANGLE_270);
                        break;
                }
            };

    /**
     * 設定資料給 Device Info 的 view
     */
    @UiThread
    private void setDeviceInfoView() {
        Context context = getContext();
        if (context == null) {
            return;
        }

        final String serialnumber = SystemPropertiesUnit.getSystemProperty(SystemPropertiesUnit.PROPERTY_KEY_SERIALNUMBER);

        final FragmentSettingsBinding binding = SettingsFragment.this.binding;
        if (binding == null) {
            return;
        }
        final String strSN = context.getString(R.string.serial_number_str, serialnumber);
        binding.serialNumber.setText(
                strSN
        );
        Log.i(TAG,strSN);

        LocalDateTime buildTime = LocalDateTime.ofInstant(
                Instant.ofEpochMilli(BuildConfig.BUILD_TIME),
                ZoneId.systemDefault()
        );
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyMMdd", Locale.ENGLISH);
        final String swVersion = context.getString(R.string.software_version_str, buildTime.format(dateFormatter), BuildConfig.VERSION_NAME);
        binding.softwareVersion.setText(
                swVersion
        );
        Log.i(TAG,swVersion);

        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss", Locale.ENGLISH);
        String formattedBuildTime = buildTime.format(dateTimeFormatter);
        binding.buildTime.setText(
                context.getString(R.string.build_time_str, formattedBuildTime)
        );

        double diskSize = 0.0;
        try {
            diskSize = FileUtils.getFsTotalSize(Environment.getExternalStorageDirectory().getAbsolutePath()) / 1024.0 / 1024.0 / 1024.0;
        } catch (Exception e) {
            Timber.e(e);
        }

        double freeDisk = 0.0;
        try {
            freeDisk = FileUtils.getFsAvailableSize(Environment.getExternalStorageDirectory().getAbsolutePath()) / 1024.0 / 1024.0 / 1024.0;
        } catch (Exception e) {
            Timber.e(e);
        }

        double usedDisk = 0.0;
        if (diskSize >= 0 && diskSize >= freeDisk) {
            usedDisk = diskSize - freeDisk;
        }

        DecimalFormat decimalFormat = new DecimalFormat("0.#");
        decimalFormat.setRoundingMode(RoundingMode.CEILING);

        Log.i(TAG, String.format("[setDeviceInfoView] Storage usage: %f, %f, %f",
                diskSize,freeDisk,usedDisk));

        binding.diskSize.setText(
                context.getString(R.string.disk_size_str, context.getString(R.string.x1_gb, decimalFormat.format(diskSize)))
        );

        binding.freeDisk.setText(
                context.getString(R.string.free_disk_str, context.getString(R.string.x1_gb, decimalFormat.format(freeDisk)))
        );

        binding.usedDisk.setText(
                context.getString(R.string.used_disk_str, context.getString(R.string.x1_gb, decimalFormat.format(usedDisk)))
        );
    }

    private enum Type_DateTimeDialog{
        SystemTime,
        Log_StartTime,
        Log_EndTime
    }

    /**
     * 顯示 DateTime Dialog
     */
    private void showDateTimeDialog(Type_DateTimeDialog type) {
        MyApplication myApplication = getMyApplication();
        if (myApplication == null) {
            Timber.w("[showDateTimeDialog] myApplication == null");
            return;
        }

        UserTbData loginUserTbData = myApplication.getLoginUserTbData();
        if (loginUserTbData == null) {
            Timber.w("[showDateTimeDialog] loginUserTbData == null");
            return;
        }

        UserRoleType t = loginUserTbData.getRoleType();
        if ( t == UserRoleType.GUEST) {
            Timber.w("[showDateTimeDialog] t = UserRoleType.GUEST");
            return;
        }else if( t == UserRoleType.ADVANCED_USER){
            if(type==Type_DateTimeDialog.Log_StartTime || type== Type_DateTimeDialog.Log_EndTime){
                Timber.w("[showDateTimeDialog] t = "+type);
                return;
            }
        }

        cancelDateTimeDialog();

        final Context context = getContext();
        if (context == null) {
            Timber.w("[showDateTimeDialog] context == null");
            return;
        }

        DateTimeDialog d = new DateTimeDialog(getChildFragmentManager(), context);
        d.setListener(new DateTimeDialog.Listener() {
            @Override
            public void onTouch() {
                Activity activity = getActivity();
                if (activity instanceof MainActivity) {
                    ((MainActivity) activity).restartStandbyNotificationTimeServiceTimer();
                }
            }

            @Override
            public void onClickConfirm(@NonNull LocalDateTime dateTime) {
                Timber.d("DateTimeDialog -> onClickConfirm dateTime=%s", dateTime);
                switch (type) {
                    case SystemTime:
                        long millis = dateTime.atZone(ZoneOffset.systemDefault()).toInstant().toEpochMilli();
                        Timber.d("DateTimeDialog -> onClickConfirm millis=%s", millis);
                        try {
                            SystemClock.setCurrentTimeMillis(millis);
                        } catch (Exception e) {
                            Timber.e(e);
                        }
                        Timber.d(
                                "DateTimeDialog -> onClickConfirm SystemClock.currentThreadTimeMillis=%s",
                                SystemClock.currentThreadTimeMillis()
                        );
                        Activity activity = getActivity();
                        if (activity instanceof MainActivity) {
                            ((MainActivity) activity).setOnStartTimeMillis(SystemClock.currentThreadTimeMillis());
                        }
                        break;
                    case Log_StartTime:
                        final LocalDateTime t = dateTime.withSecond(0).withNano(0);
                        if (t.isAfter(SettingsFragment.this.endDateTime)) {
                            SettingsFragment.this.setEndDateTime(t.withSecond(59).withNano(999999999));
                        }
                        SettingsFragment.this.setStartDateTime(t);
                        break;
                    case Log_EndTime:
                        final LocalDateTime t_end = dateTime.withSecond(59).withNano(999999999);
                        if (t_end.isBefore(SettingsFragment.this.startDateTime)) {
                            SettingsFragment.this.setStartDateTime(t_end.withSecond(0).withNano(0));
                        }
                        SettingsFragment.this.setEndDateTime(t_end);
                        break;
                }
            }

            @Override
            public void onClickCancel() {}
        });

        switch (type){
            case Log_StartTime:
                d.setDateTime(SettingsFragment.this.startDateTime);
                break;
            case Log_EndTime:
                d.setDateTime(SettingsFragment.this.endDateTime);
                break;
        }

        d.show();
        dateTimeDialog = d;
    }

    /**
     * 關閉 DateTime Dialog
     */
    private void cancelDateTimeDialog() {
        final DateTimeDialog dialog = dateTimeDialog;
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }
        dateTimeDialog = null;
    }

    private final View.OnClickListener dateTimeLayoutOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            Timber.d("[dateTimeLayoutOnClickListener] "+LogQueue.getId(view));
            showDateTimeDialog(Type_DateTimeDialog.SystemTime);
        }
    };

    //Jerry
//    private final View.OnLongClickListener wifiSetClickListener=new View.OnLongClickListener() {
//        @Override
//        //public void onClick(View view) {
//        public boolean  onLongClick(View view) {
//            //LogQueue.addToQueue_View(view, "wifiSetClickListener");
//
//            Intent intentWiFi = new Intent();
//            intentWiFi.setAction("android.settings.WIFI_SETTINGS");
//            startActivity(intentWiFi);
//
////            switch (view.getId()){
////                case R.id.startDateTimeLayout:
////                    showDateTimeDialog(Type_DateTimeDialog.Log_StartTime);
////                    break;
////            }
//            return true;
//        }
//    };


    private final View.OnClickListener exportLogOnClickListener=new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            Timber.d("[exportLogOnClickListener] "+LogQueue.getId(view));
            switch (view.getId()){
                case R.id.startDateTimeLayout:
                    showDateTimeDialog(Type_DateTimeDialog.Log_StartTime);
                    break;

                case R.id.endDateTimeLayout:
                    showDateTimeDialog(Type_DateTimeDialog.Log_EndTime);
                    break;
                case R.id.btnExportLog:
                    try {
                        scheduledExecutorService.execute(new Runnable() {
                            @Override
                            public void run() {
                                final MyApplication myApplication = getMyApplication();
                                if (myApplication == null) {
                                    return;
                                }
                                final Context context = getContext();
                                if (context == null) {
                                    return;
                                }

                                UserTbData loginUserTbData = myApplication.getLoginUserTbData();
                                if (loginUserTbData == null) {
                                    return;
                                }
                                @NonNull final UserRoleType roleType = loginUserTbData.getRoleType();
                                if(roleType!=UserRoleType.ADMIN_USER && roleType!=UserRoleType.SERVICE_USER){
                                    return;
                                }

                                if (exportLogDisposable != null) {
                                    return;
                                }

                                @NonNull final List<StorageVolume> storageList = MyStorageManager.getInstance().getStorageList();
                                if (storageList.isEmpty()) {
                                    return;
                                }

                                final FragmentSettingsBinding binding = SettingsFragment.this.binding;
                                if (binding == null) {
                                    return;
                                }

                                binding.btnExportLog.setOnClickListener(null);

                                final File[] files = LogQueue.folder.listFiles(new FileFilter() {
                                    @Override
                                    public boolean accept(File file) {

                                        if (file.isDirectory()) return false;
                                        LocalDateTime fileTime = LocalDateTime.ofInstant(
                                                Instant.ofEpochMilli(file.lastModified()), ZoneId.systemDefault()
                                        );
                                        if (startDateTime.isBefore(fileTime) && endDateTime.isAfter(fileTime)) {
                                            return true;
                                        }
                                        return false;
                                    }
                                });


                                binding.btnExportLog.setOnClickListener(exportLogOnClickListener);
                                if(files==null || files.length==0){
                                    binding.getRoot().post(new Runnable() {
                                        @Override
                                        public void run() {
                                            showMyDialog(
                                                    R.string.no_file_found,
                                                    true,
                                                    R.string.ok,
                                                    false,
                                                    R.string.no,
                                                    null,
                                                    false
                                            );
                                        }
                                    });
                                    return;
                                }

                                List<@NotNull File> selectFileList = Arrays.asList(files);

                                HashMap<@NotNull String, @NotNull List<@NotNull File>> selectFileGroupMap = new HashMap<>();

                                DateTimeFormatter fileFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm", Locale.getDefault());
                                String key = startDateTime.format(fileFormatter) + "_" + endDateTime.format(fileFormatter)+"_logs";
                                selectFileGroupMap.put(key,selectFileList);

                                if (storageList.size() > 1) {
                                    binding.getRoot().post(new Runnable() {
                                        @Override
                                        public void run() {
                                            showZipPasswordDialog(new ZipPasswordDialog.Listener() {
                                                @Override
                                                public void onClickConfirm(@NonNull String password) {
                                                    showMySelectStorageDialog(selectFileGroupMap,  password.trim());
                                                }

                                                @Override
                                                public void onClickCancel() {

                                                }

                                                @Override
                                                public void onTouch() {

                                                }
                                            });
                                        }
                                    });
                                    return;
                                }

                                final StorageVolume storage = storageList.get(0);
                                final File exportDirectory = storage.getDirectory();
                                if (exportDirectory == null) {
                                    return;
                                }

                                HashMap<@NotNull File, @NotNull List<@NotNull File>> selectZipFileGroupMap = new HashMap<>();
                                for (@NonNull Map.Entry<@NotNull String, @NotNull List<@NotNull File>> entry : selectFileGroupMap.entrySet()) {
                                    selectZipFileGroupMap.put(new File(exportDirectory.getAbsolutePath() + File.separator +
                                            MyApplication.FOLDER_NAME + File.separator + entry.getKey() + ".zip"), entry.getValue());
                                }

                                showZipPasswordDialog(new ZipPasswordDialog.Listener() {
                                    @Override
                                    public void onClickConfirm(@NonNull String password) {
                                        basicExportZip(selectZipFileGroupMap, Tools.getDisplayName(storage, context), password.trim(), storage);
                                    }

                                    @Override
                                    public void onClickCancel() {

                                    }

                                    @Override
                                    public void onTouch() {

                                    }
                                });
                            }
                        });

                    }catch (Exception e){
                        Timber.e(e.getMessage());
                    }
                    break;

                case R.id.exportLogLayout:
                    boolean isStorageListNotEmpty = !MyStorageManager.getInstance().getStorageList().isEmpty();
                    if(isStorageListNotEmpty) {
                        if (binding.dateTimeRangeLayout.getVisibility() == View.VISIBLE) {
                            binding.dateTimeRangeLayout.setVisibility(View.GONE);
                            binding.exportLogArrow.setImageResource(R.drawable.baseline_chevron_right_24);
                        } else {
//                            Runnable r = () -> {
//                                binding.dateTimeRangeLayout.setVisibility(View.VISIBLE);
//                                binding.exportLogArrow.setImageResource(R.drawable.baseline_chevron_left_24);
//                            };
//                            if (Looper.myLooper() == Looper.getMainLooper()) {
//                                r.run();
//                            } else {
//                                if (binding != null) {
//                                    binding.getRoot().post(r);
//                                }
//                            }
                            setDateRange();
                        }
                    }else{
                        binding.getRoot().post(new Runnable() {
                            @Override
                            public void run() {
                                showMyDialog(
                                        R.string.msg_connect_usb,
                                        true,
                                        R.string.ok,
                                        false,
                                        R.string.no,
                                        null,
                                        false
                                );
                            }
                        });
                    }
                    break;
            }

        }
    };

    /**
     * Show Language Select Dialog
     */
    private void showLanguageSelectDialog() {
        MyApplication myApplication = getMyApplication();
        if (myApplication == null) {
            return;
        }

        UserTbData loginUserTbData = myApplication.getLoginUserTbData();
        if (loginUserTbData == null) {
            return;
        }

        UserRoleType t = loginUserTbData.getRoleType();
        if (t == null || t == UserRoleType.GUEST) {
            return;
        }

        cancelLanguageSelectDialog();

        LanguageSelectDialog d = LanguageSelectDialog.newInstance();
        d.show(getChildFragmentManager(), LanguageSelectDialog.class.getSimpleName());
        languageSelectDialog = d;
    }

    /**
     * Cancel Language Select Dialog
     */
    private void cancelLanguageSelectDialog() {
        final LanguageSelectDialog dialog = languageSelectDialog;
        if (dialog != null && dialog.isVisible()) {
            if (dialog.isStateSaved()) {
                dialog.dismissAllowingStateLoss();
            } else {
                dialog.dismiss();
            }
        }
        this.languageSelectDialog = null;
    }

    @UiThread
    private void setDateTimeDisplayFormat() {
        final FragmentSettingsBinding binding = SettingsFragment.this.binding;
        RadioGroup dateTimeDisplayFormatRadioGroup = binding != null ? binding.dateTimeDisplayFormatLayoutRadioGroup : null;
        if (dateTimeDisplayFormatRadioGroup != null) {
            dateTimeDisplayFormatRadioGroup.setOnCheckedChangeListener(null);
            dateTimeDisplayFormatRadioGroup.clearCheck();
        }

        final Context context = getContext();
        if (context != null) {
            boolean is24HoursTimeDisplay = DateFormat.is24HourFormat(context);
            if (is24HoursTimeDisplay) {
                if (binding != null) {
                    binding.hours24.toggle();
                }
            } else {
                if (binding != null) {
                    binding.hours12.toggle();
                }
            }
        }

        if (dateTimeDisplayFormatRadioGroup != null) {
            dateTimeDisplayFormatRadioGroup.setOnCheckedChangeListener(dateTimeDisplayFormatOnCheckedChangeListener);
        }

        MyApplication myApplication = getMyApplication();
        UserTbData loginUserTbData = myApplication != null ? myApplication.getLoginUserTbData() : null;
        UserRoleType t = loginUserTbData != null ? loginUserTbData.getRoleType() : null;

        if (dateTimeDisplayFormatRadioGroup != null) {
            dateTimeDisplayFormatRadioGroup.setEnabled(!(t == null || t == UserRoleType.GUEST));
        }
        RadioButton hours12RadioButton = binding != null ? binding.hours12 : null;
        if (hours12RadioButton != null) {
            hours12RadioButton.setEnabled(!(t == null || t == UserRoleType.GUEST));
        }
        RadioButton hours24RadioButton = binding != null ? binding.hours24 : null;
        if (hours24RadioButton != null) {
            hours24RadioButton.setEnabled(!(t == null || t == UserRoleType.GUEST));
        }
    }

    private final RadioGroup.OnCheckedChangeListener dateTimeDisplayFormatOnCheckedChangeListener =
            new RadioGroup.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(RadioGroup group, int checkedId) {
                    final Context context = getContext();
                    if (context != null) {
                        switch (checkedId) {
                            case R.id.hours12:
                                Settings.System.putString(
                                        context.getContentResolver(),
                                        Settings.System.TIME_12_24,
                                        "12"
                                );
                                break;
                            case R.id.hours24:
                                Settings.System.putString(
                                        context.getContentResolver(),
                                        Settings.System.TIME_12_24,
                                        "24"
                                );
                                break;
                        }
                    }
                }
            };

    @UiThread
    private void setScreenAutoRotate() {
        final FragmentSettingsBinding binding = SettingsFragment.this.binding;
        RadioGroup screenAutoRotateRadioGroup = binding != null ? binding.screenAutoRotateRadioGroup : null;
        if (screenAutoRotateRadioGroup != null) {
            screenAutoRotateRadioGroup.setOnCheckedChangeListener(null);
            screenAutoRotateRadioGroup.clearCheck();
        }

        final Context context = getContext();
        if (context != null) {
            boolean isOn = Settings.System.getInt(
                    context.getContentResolver(),
                    Settings.System.ACCELEROMETER_ROTATION,
                    1
            ) == 1;
            if (binding != null) {
                if (isOn) {
                    binding.screenAutoRotateOn.toggle();
                } else {
                    binding.screenAutoRotateOff.toggle();
                }
            }
        }

        if (screenAutoRotateRadioGroup != null) {
            screenAutoRotateRadioGroup.setOnCheckedChangeListener(screenAutoRotateOnCheckedChangeListener);
        }

        final MyApplication myApplication = getMyApplication();
        final UserTbData loginUserTbData = myApplication != null ? myApplication.getLoginUserTbData() : null;
        final UserRoleType t = loginUserTbData != null ? loginUserTbData.getRoleType() : null;


        if (screenAutoRotateRadioGroup != null) {
            screenAutoRotateRadioGroup.setEnabled(!(t == null || t == UserRoleType.GUEST));
        }
        RadioButton screenAutoRotateOnRadioButton = binding != null ? binding.screenAutoRotateOn : null;
        if (screenAutoRotateOnRadioButton != null) {
            screenAutoRotateOnRadioButton.setEnabled(!(t == null || t == UserRoleType.GUEST));
        }
        RadioButton screenAutoRotateOffRadioButton = binding != null ? binding.screenAutoRotateOff : null;
        if (screenAutoRotateOffRadioButton != null) {
            screenAutoRotateOffRadioButton.setEnabled(!(t == null || t == UserRoleType.GUEST));
        }
    }

    private final RadioGroup.OnCheckedChangeListener screenAutoRotateOnCheckedChangeListener =
            new RadioGroup.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(RadioGroup group, int checkedId) {
                    Context context = getContext();
                    if (context != null) {
                        switch (checkedId) {
                            case R.id.screenAutoRotateOn:
                                Settings.System.putInt(
                                        context.getContentResolver(),
                                        Settings.System.ACCELEROMETER_ROTATION,
                                        1
                                );
                                break;
                            case R.id.screenAutoRotateOff:
                                Settings.System.putInt(
                                        context.getContentResolver(),
                                        Settings.System.ACCELEROMETER_ROTATION,
                                        0
                                );
                                break;
                        }
                    }
                }
            };

    private final Runnable cpuTemperatureRunnable = new Runnable() {
        @Override
        public void run() {
            if (isDetached()) {
                return;
            }
            if (!isAdded()) {
                return;
            }
            final FragmentSettingsBinding binding = SettingsFragment.this.binding;
            if (binding == null) {
                return;
            }

            double sum = 0.0;
            double average = 0.0;
            ArrayList<File> tempFileList = new ArrayList<>();
            tempFileList.add(new File("/sys/class/thermal/thermal_zone0/temp"));
            //tempFileList.add(new File("/sys/class/thermal/thermal_zone1/temp"));

            for (File tempFile : tempFileList) {
                if (tempFile.canRead()) {
                    try(BufferedReader br = new BufferedReader(new FileReader(tempFile))) {
                        String line=br.readLine();
                        if(testShowError4000!=null){
                            line=testShowError4000;
                        }

                        if(line!=null){
                            Double temperatureCpu = Double.parseDouble(line);
                            temperatureCpu = temperatureCpu / 1000;
                            sum += temperatureCpu;
                        }
                    } catch (IOException e) {
                        Timber.e("[cpuTemperatureRunnable] "+e.getMessage());
                    }
                }
            }

            // Timber.d("sum=" + sum);
            if (sum > 0) {
                average = sum / tempFileList.size();
            }

            DecimalFormat decimalFormat = new DecimalFormat("0.#");
            Context context = getContext();
            if (context != null) {
                final String temperature = context.getString(R.string.cpu_temperature_str, decimalFormat.format(average));
                binding.cpuTemperature.setText(temperature);
                Log.d(TAG,"[thermal] "+temperature);
            }

            binding.cpuTemperature.removeCallbacks(this);
            binding.cpuTemperature.postDelayed(this, 60*1000); //Detect every minute.

            //Error code(4000)
            if (average > 85) {
                if(Error.CPU_OVER_TEMPERATURE.enable) {
                    IErrorCode.showErrorCode(getActivity(), Error.CPU_OVER_TEMPERATURE);
                    Error.CPU_OVER_TEMPERATURE.enable=false;
                    Log.w(TAG, String.format("%s: temperature=%.3f",  Error.CPU_OVER_TEMPERATURE.getCode(), average));
                }
            }else {
                IErrorCode.removeErrorCodeView(getActivity(), Error.CPU_OVER_TEMPERATURE);
                Error.CPU_OVER_TEMPERATURE.enable=true;
            }
        }
    };

    @UiThread
    private void setAutoDeleteFile() {
        if (binding != null) {
            binding.autoDeleteFileRadioGroup.setOnCheckedChangeListener(null);
            binding.autoDeleteFileRadioGroup.clearCheck();

            try {
                scheduledExecutorService.execute(new Runnable() {
                    @Override
                    public void run() {
                        BootAutoDeleteFileType bootAutoDeleteFileType =
                                SharedPreferencesManager.getInstance().getBootAutoDeleteFileType();

                        binding.getRoot().post(new Runnable() {
                            @Override
                            public void run() {
                                switch (bootAutoDeleteFileType) {
                                    case DAY_1:
                                        binding.autoDeleteFile1Day.toggle();
                                        break;
                                    case DAY_7:
                                        binding.autoDeleteFile7Days.toggle();
                                        break;
                                    case DAY_30:
                                        binding.autoDeleteFile30Days.toggle();
                                        break;
                                }

                                binding.autoDeleteFileRadioGroup.setOnCheckedChangeListener(autoDeleteFileOnCheckedChangeListener);
                                MyApplication myApplication = getMyApplication();
                                UserRoleType t = null;
                                if (myApplication != null && myApplication.getLoginUserTbData() != null) {
                                    t = myApplication.getLoginUserTbData().getRoleType();
                                }
                                binding.autoDeleteFileRadioGroup.setEnabled(t != null && t != UserRoleType.GUEST);
                                binding.autoDeleteFile1Day.setEnabled(t != null && t != UserRoleType.GUEST);
                                binding.autoDeleteFile7Days.setEnabled(t != null && t != UserRoleType.GUEST);
                                binding.autoDeleteFile30Days.setEnabled(t != null && t != UserRoleType.GUEST);
                            }
                        });
                    }
                });
            } catch (Exception e) {
                Timber.e(e);
            }
        }
    }

    private final RadioGroup.OnCheckedChangeListener autoDeleteFileOnCheckedChangeListener =
            (group, checkedId) -> {
                switch (checkedId) {
                    case R.id.autoDeleteFile1Day:
                        SharedPreferencesManager.getInstance()
                                .saveBootAutoDeleteFileType(BootAutoDeleteFileType.DAY_1);
                        break;
                    case R.id.autoDeleteFile7Days:
                        SharedPreferencesManager.getInstance()
                                .saveBootAutoDeleteFileType(BootAutoDeleteFileType.DAY_7);
                        break;
                    case R.id.autoDeleteFile30Days:
                        SharedPreferencesManager.getInstance()
                                .saveBootAutoDeleteFileType(BootAutoDeleteFileType.DAY_30);
                        break;
                }
            };

    private void setSwUpgradingLayout() {
        Runnable r = () -> {
            MyApplication myApplication = getMyApplication();
            UserRoleType userRoleType = null;
            if (myApplication != null && myApplication.getLoginUserTbData() != null) {
                userRoleType = myApplication.getLoginUserTbData().getRoleType();
            }
            if (userRoleType == null) {
                userRoleType = UserRoleType.GUEST;
            }

            switch (userRoleType) {
                case GUEST:
                case ADVANCED_USER:
                case ADMIN_USER:
                    binding.swUpgradingLayout.setVisibility(View.GONE);
//                    binding.WifiLayout.setVisibility(View.GONE);
                    break;
                case SERVICE_USER:
                    binding.swUpgradingLayout.setVisibility(View.VISIBLE);
//                    binding.WifiLayout.setVisibility(View.VISIBLE);
                    break;
            }
        };

        if (Looper.myLooper() == Looper.getMainLooper()) {
            r.run();
        } else {
            if (binding != null) {
                binding.getRoot().post(r);
            }
        }
    }

    @Nullable
    public MyApplication getMyApplication() {
        try {
            Context context = getContext();
            if (context != null) {
                return (MyApplication) context.getApplicationContext();
            }
        } catch (Exception e) {
            Timber.e(e);
        }
        return null;
    }


    private void setExportLogLayout() {
        Runnable r = () -> {
            MyApplication myApplication = getMyApplication();
            UserRoleType userRoleType = null;
            if (myApplication != null && myApplication.getLoginUserTbData() != null) {
                userRoleType = myApplication.getLoginUserTbData().getRoleType();
            }
            if (userRoleType == null) {
                userRoleType = UserRoleType.GUEST;
            }

            switch (userRoleType) {
                case GUEST:
                case ADVANCED_USER:
                    binding.exportLogLayout.setVisibility(View.GONE);
                    break;
                case ADMIN_USER:
                case SERVICE_USER:
                    binding.exportLogLayout.setVisibility(View.VISIBLE);
                    break;
            }
        };

        if (Looper.myLooper() == Looper.getMainLooper()) {
            r.run();
        } else {
            if (binding != null) {
                binding.getRoot().post(r);
            }
        }
    }

    private void setDateRange(){
        final FragmentSettingsBinding binding = SettingsFragment.this.binding;
        if(binding!=null){
            binding.getRoot().post(new Runnable() {
                @Override
                public void run() {
                    boolean isStorageListNotEmpty = !MyStorageManager.getInstance().getStorageList().isEmpty();
                    Timber.d("[setDateRange] isStorageListNotEmpty="+isStorageListNotEmpty);
                    if(isStorageListNotEmpty) {
                        setStartDatTimeToView();
                        setEndDatTimeToView();
                        binding.dateTimeRangeLayout.setVisibility(View.VISIBLE );
                        binding.exportLogArrow.setImageResource(R.drawable.baseline_chevron_left_24);
                    }else{
                        binding.dateTimeRangeLayout.setVisibility(View.GONE);
                        binding.exportLogArrow.setImageResource(R.drawable.baseline_chevron_right_24);
                    }
                }
            });
        }
    }

    private void setStartDatTimeToView() {
        final FragmentSettingsBinding binding = SettingsFragment.this.binding;
        if (binding != null) {
            binding.getRoot().post(new Runnable() {
                @Override
                public void run() {
                    final LocalDateTime startDateTime = SettingsFragment.this.startDateTime;
                    binding.startDateTime.setText(startDateTime.format(formatter));
                }
            });
        }
    }


    private void setEndDatTimeToView() {
        final FragmentSettingsBinding binding = SettingsFragment.this.binding;
        if (binding != null) {
            binding.getRoot().post(new Runnable() {
                @Override
                public void run() {
                    final LocalDateTime endDateTime = SettingsFragment.this.endDateTime;
                    binding.endDateTime.setText(endDateTime.format(formatter));
                }
            });
        }
    }

    private void setStartDateTime(@NonNull LocalDateTime value) {
        this.startDateTime = value;
        setStartDatTimeToView();
    }

    private void setEndDateTime(@NonNull LocalDateTime value) {
        this.endDateTime = value;
        setEndDatTimeToView();
    }

    private final MyStorageManager.Listener myStorageManagerListener = new MyStorageManager.Listener() {
        @Override
        public void onStorageMounted(@NonNull File storageFile) {
            Timber.d("[onStorageMounted]"+storageFile.getName());
            MyApplication myApplication = getMyApplication();
            UserRoleType userRoleType = null;
            if (myApplication != null && myApplication.getLoginUserTbData() != null) {
                userRoleType = myApplication.getLoginUserTbData().getRoleType();
            }
            if (userRoleType == null) {
                userRoleType = UserRoleType.GUEST;
            }
            Timber.d("[onStorageMounted] userRoleType="+userRoleType);

            delete_updatezip();

            switch (userRoleType){
                case GUEST:
                case ADVANCED_USER:
                    break;
                case ADMIN_USER:
                    setDateRange();
                    break;
                case SERVICE_USER:
                    setDateRange();
                    final Pattern pattern = Pattern.compile("[A-Z0-9]{4}-[A-Z0-9]{4}");
                    final Matcher matcher = pattern.matcher(storageFile.getName());
                    if(matcher.matches()) {
                        File from = getOTA_rename_from(storageFile);
                        if (from.exists()) {
                            setUpdatePassword(true);
                        }
                    }
                    break;
            }
        }

        @Override
        public void onStorageUnmounted(@NonNull File storageFile) {
            setDateRange();

            try {
                final Disposable basicExportDisposable = SettingsFragment.this.exportLogDisposable;
                if (basicExportDisposable != null) {
                    basicExportDisposable.dispose();
                }
                SettingsFragment.this.exportLogDisposable = null;
            } catch (Exception e) {
                Timber.e(e);
            }

            final ZipFilesProgressDialog zipFilesProgressDialog = SettingsFragment.this.zipFilesProgressDialog;
            try {
                if (zipFilesProgressDialog != null) {
                    Timber.d("onStorageUnmounted storageFile=" + storageFile);
                    Timber.d("onStorageUnmounted zipFilesProgressDialog.getStorageDirectoryFile()=" + zipFilesProgressDialog.getStorageDirectoryFile());
                    if (storageFile.equals(zipFilesProgressDialog.getStorageDirectoryFile())) {
                        if (!zipFilesProgressDialog.isComplete()) {
                            zipFilesProgressDialog.showFail();
                        }
                    }
                }
            } catch (Exception e) {
                Timber.e(e);
            }

            final List<StorageVolume> storageList = MyStorageManager.getInstance().getStorageList();
            boolean isOTAFileExist=false;
            if(!storageList.isEmpty()){
                for (StorageVolume storage:storageList) {
                    if(isFAT32(storage)) {
                        final File directory = storage.getDirectory();
                        File from = getOTA_rename_from(directory);
                        if (from.exists()) {
                            isOTAFileExist = true;
                            break;
                        }
                    }
                }
            }
            if(! isOTAFileExist){
                setUpdatePassword(false);
            }
        }
    };

    private void showMyDialog(
            @StringRes int msg,
            boolean isShowConfirm,
            @StringRes int confirmBtnText,
            boolean isShowCancel,
            @StringRes int cancelBtnText,
            @Nullable MyDialog.Listener listener,
            boolean isReverseBin
    ) {
        final Context context = getContext();
        if (context == null) {
            return;
        }
        if (!isResumed() || !isVisible()) {
            return;
        }
        cancelMyDialog();
        Runnable r = new Runnable() {
            @Override
            public void run() {
                MyDialog d = new MyDialog(context, isReverseBin) {
                    @Override
                    public boolean dispatchTouchEvent(MotionEvent ev) {
                        final MyApplication myApp = getMyApplication();
                        if (myApp != null) {
                            MainActivity mainActivity = myApp.getMainActivity();
                            if (mainActivity != null) {
                                mainActivity.restartStandbyNotificationTimeServiceTimer();
                            }
                        }
                        return super.dispatchTouchEvent(ev);
                    }
                };
                d.setMsg(context.getString(msg));
                d.showConfirm(isShowConfirm);
                d.setConfirmText(context.getString(confirmBtnText));
                d.showCancel(isShowCancel);
                d.setCancelText(context.getString(cancelBtnText));
                d.setListener(listener);
                d.show();
                myDialog = d;
            }
        };
        if (Looper.myLooper() == Looper.getMainLooper()) {
            r.run();
        } else {
            final FragmentSettingsBinding binding = SettingsFragment.this.binding;
            if (binding != null) {
                binding.getRoot().post(r);
            }
        }
    }

    private void cancelMyDialog() {
        Runnable r = new Runnable() {
            @Override
            public void run() {
                final MyDialog myDialog = SettingsFragment.this.myDialog;
                if (myDialog != null && myDialog.isShowing()) {
                    myDialog.dismiss();
                }
                SettingsFragment.this.myDialog = null;
            }
        };
        if (Looper.myLooper() == Looper.getMainLooper()) {
            r.run();
        } else {
            final FragmentSettingsBinding binding = SettingsFragment.this.binding;
            if (binding != null) {
                binding.getRoot().post(r);
            }
        }
    }

    @UiThread
    private void showZipPasswordDialog(@NonNull final ZipPasswordDialog.Listener listener) {
        cancelZipPasswordDialog();

        ZipPasswordDialog d = ZipPasswordDialog.newInstance();
        d.setListener(new ZipPasswordDialog.Listener() {
            @Override
            public void onClickConfirm(@NonNull String password) {
                if (CheckPasswordRule(password) == 1) {
                    listener.onClickConfirm(password);
                }
                else {
                    showMyDialog(R.string.password_rule,
                            true, R.string.ok,
                            false, R.string.cancel,
                            new MyDialog.Listener() {
                                @Override
                                public void OnClickConfirm() {
                                    cancelZipPasswordDialog();
                                }

                                @Override
                                public void OnClickCancel() {

                                }
                            }, false);

                }
            }

            @Override
            public void onClickCancel() {
                listener.onClickCancel();
            }

            @Override
            public void onTouch() {
                FragmentActivity activity = getActivity();
                if (activity instanceof MainActivity) {
                    ((MainActivity) activity).restartStandbyNotificationTimeServiceTimer();
                }
            }
        });
        d.show(getChildFragmentManager(), ZipPasswordDialog.class.getSimpleName());
        zipPasswordDialog = d;
    }

    @UiThread
    private void cancelZipPasswordDialog() {
        final ZipPasswordDialog zipPasswordDialog = SettingsFragment.this.zipPasswordDialog;
        if (zipPasswordDialog != null && zipPasswordDialog.isVisible()) {
            if (zipPasswordDialog.isStateSaved()) {
                zipPasswordDialog.dismissAllowingStateLoss();
            } else {
                zipPasswordDialog.dismiss();
            }
        }
        SettingsFragment.this.zipPasswordDialog = null;
    }

    @UiThread
    private void showMySelectStorageDialog(@NonNull Map<@NotNull String, @NotNull List<@NotNull File>> selectFileGroupMap, @NonNull String password) {
        cancelMySelectStorageDialog();
        final Context context = getContext();
        if (context == null) {
            return;
        }
        final MySelectStorageDialog d = new MySelectStorageDialog(context);
        d.setListener(new MySelectStorageDialog.Listener() {
            @Override
            public void onClickConfirm(int position, @NonNull MySelectStorageDialogData data) {
                StorageVolume storageVolume = data.getStorageVolume();
                File exportFile = null;
                if (storageVolume != null) {
                    exportFile = storageVolume.getDirectory();
                }
                if (exportFile == null) {
                    return;
                }

                Map<File, List<File>> fileMapList = new HashMap<>();
                for (Map.Entry<String, List<File>> selectFileGroup : selectFileGroupMap.entrySet()) {
                    fileMapList.put(new File(exportFile.getAbsolutePath() + File.separator + MyApplication.FOLDER_NAME + File.separator + selectFileGroup.getKey() + ".zip"), selectFileGroup.getValue());

                }

                basicExportZip(fileMapList, data.getText(), password, storageVolume);

            }

            @Override
            public void onClickCancel() {

            }

            @Override
            public void onTouch() {
                FragmentActivity activity = getActivity();
                if (activity instanceof MainActivity) {
                    MainActivity mainActivity = (MainActivity) activity;
                    mainActivity.restartStandbyNotificationTimeServiceTimer();
                }
            }
        });
        d.show();
        mySelectStorageDialog = d;
    }

    private void cancelMySelectStorageDialog() {
        final MySelectStorageDialog mySelectDialog = mySelectStorageDialog;
        if (mySelectDialog != null && mySelectDialog.isShowing()) {
            mySelectDialog.dismiss();
        }
        this.mySelectStorageDialog = null;
    }

    private void basicExportZip(
            @NonNull Map<@NotNull File,
                    @NotNull List<@NotNull File>> fileMapList,
            @NonNull String exportText,
            @NonNull String password,
            @NonNull StorageVolume storage) {
        if (exportLogDisposable != null) {
            return;
        }
        Timber.d("basicExport");

        final FragmentSettingsBinding binding = SettingsFragment.this.binding;
        if (binding == null) {
            return;
        }

        binding.getRoot().post(new Runnable() {
            @Override
            public void run() {
                final ZipFilesProgressDialog dialog = showZipFilesProgressDialog(fileMapList, exportText, storage);


                MyStorageManager.getInstance().compressFilesToZipsCopyWithPasswordFlow(fileMapList, password)
                        .subscribe(
                                new Observer<MyStorageManager.ZipProgressData>() {

                                    @Override
                                    public void onSubscribe(@io.reactivex.rxjava3.annotations.NonNull Disposable d) {
                                        Timber.d("basicExportZip -> compressFilesToZipsWithPasswordFlow -> onSubscribe Disposable=%s", d);
                                        exportLogDisposable = d;
                                    }

                                    @Override
                                    public void onNext(@Nullable MyStorageManager.ZipProgressData progress) {
                                        Timber.d("basicExportZip -> compressFilesToZipsWithPasswordFlow -> onNext progress=%s", progress==null?"null":progress.toString());
                                        if (dialog != null && progress != null) {
                                            int progressUI = dialog.getBinding().progress.getProgress();
                                            if (progress.getProgress() < progressUI) {
                                                progress.setProgress((double) progressUI);
                                            }
                                            dialog.setProgressData(progress);
                                        }
                                    }

                                    @Override
                                    public void onError(Throwable t) {
                                        Timber.e("basicExportZip -> compressFilesToZipsWithPasswordFlow -> onError");
                                        Timber.e(t);
                                        exportLogDisposable = null;
                                        try {
                                            MyApplication myApplication = getMyApplication();
                                            if (myApplication != null) {
                                                File zipTempFile = new File(myApplication.getMainDirPath() + File.separator + MyApplication.ZIP_TEMP_NAME);
                                                if (zipTempFile.exists()) {
                                                    org.apache.commons.io.FileUtils.deleteDirectory(zipTempFile);
                                                }
                                            }
                                        } catch (Exception e) {
                                            Timber.e(e);
                                        }

                                        binding.getRoot().post(() -> {
                                            final ZipFilesProgressDialog zipFilesProgressDialog = SettingsFragment.this.zipFilesProgressDialog;
                                            if (zipFilesProgressDialog != null) {
                                                zipFilesProgressDialog.showFail();
                                            }

                                            try {
                                                scheduledExecutorService.execute(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        for (List<File> fileList : fileMapList.values()) {
                                                            @Nullable List<File> jsonFileList = null;
                                                            try {
                                                                jsonFileList = fileList.stream()
                                                                        .filter(file -> file.getName().contains(MyApplication.JSON_FILE_EXTENSION))
                                                                        .collect(Collectors.toList());
                                                            } catch (Exception e) {
                                                                Timber.e(e);
                                                            }
                                                            if (jsonFileList == null) {
                                                                jsonFileList = Collections.emptyList();
                                                            }

                                                            for (File jsonFile : jsonFileList) {
                                                                try {
                                                                    if(!jsonFile.delete()){
                                                                        Timber.d("delete file fail: "+jsonFile.getAbsolutePath());
                                                                    }
                                                                } catch (Exception e) {
                                                                    Timber.e(e);
                                                                }
                                                            }
                                                        }
                                                    }
                                                });
                                            } catch (Exception e) {
                                                Timber.e(e);
                                            }
                                        });
                                    }

                                    @Override
                                    public void onComplete() {
                                        Timber.d("basicExportZip -> compressFilesToZipsWithPasswordFlow -> onComplete");
                                        exportLogDisposable = null;
                                        try {
                                            scheduledExecutorService.execute(new Runnable() {
                                                @Override
                                                public void run() {
                                                    for (List<File> fileList : fileMapList.values()) {
                                                        List<File> jsonFileList;
                                                        try {
                                                            jsonFileList = fileList.stream()
                                                                    .filter(file -> file.getName().contains(MyApplication.JSON_FILE_EXTENSION))
                                                                    .collect(Collectors.toList());
                                                        } catch (Exception e) {
                                                            Timber.e(e);
                                                            jsonFileList = Collections.emptyList();
                                                        }

                                                        for (File jsonFile : jsonFileList) {
                                                            try {
                                                                if(!jsonFile.delete()){
                                                                    Timber.d("delete file fail: "+jsonFile.getAbsolutePath());
                                                                }
                                                            } catch (Exception e) {
                                                                Timber.e(e);
                                                            }
                                                        }
                                                    }
                                                }
                                            });
                                        } catch (Exception e) {
                                            Timber.e(e);
                                        }
                                    }
                                });
                Timber.d("basicExportZip All files copied successfully");
            }
        });
    }

    @UiThread
    private ZipFilesProgressDialog showZipFilesProgressDialog(
            @NonNull Map<@NotNull File, @NotNull List<@NotNull File>> fileMapList,
            @NonNull String exportText,
            @NonNull StorageVolume storage
    ) {
        cancelZipFilesProgressDialog();

        final Context context = getContext();
        if (context == null) {
            return null;
        }

        int fileCount = 0;
        for (Map.Entry<File, List<File>> fileMap : fileMapList.entrySet()) {
            fileCount += fileMap.getValue().size();
        }

        final ZipFilesProgressDialog progressDialog = new ZipFilesProgressDialog(context);
        progressDialog.setStorageDirectoryFile(storage.getDirectory());
        progressDialog.setTitle(fileCount, exportText);

        progressDialog.setProgressData(
                new MyStorageManager.ZipProgressData(
                        1.0,
                        null,
                        null
                )
        );

        progressDialog.setListener(new ZipFilesProgressDialog.Listener() {
            @Override
            public void onCancel() {
                try {
                    final Disposable basicExportDisposable = SettingsFragment.this.exportLogDisposable;
                    if (basicExportDisposable != null) {
                        basicExportDisposable.dispose();
                        SettingsFragment.this.exportLogDisposable = null;
                    }
                } catch (Exception e) {
                    Timber.e(e);
                }

                try {
                    scheduledExecutorService.execute(new Runnable() {
                        @Override
                        public void run() {
                            for (List<File> fileList : fileMapList.values()) {
                                List<File> jsonFileList = new ArrayList<>();
                                for (File file : fileList) {
                                    if (file.getName().contains(MyApplication.JSON_FILE_EXTENSION)) {
                                        jsonFileList.add(file);
                                    }
                                }

                                for (File jsonFile : jsonFileList) {
                                    try {
                                        if(!jsonFile.delete()){
                                            Timber.d("delete file fail: "+jsonFile.getAbsolutePath());
                                        }
                                    } catch (Exception e) {
                                        Timber.e(e);
                                    }
                                }
                            }
                        }
                    });
                } catch (Exception e) {
                    Timber.e(e);
                }
            }

            @Override
            public void onTouch() {
                FragmentActivity activity = getActivity();
                if (activity instanceof MainActivity) {
                    ((MainActivity) activity).restartStandbyNotificationTimeServiceTimer();
                }
            }
        });

        progressDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                try {
                    final Disposable basicExportDisposable = SettingsFragment.this.exportLogDisposable;
                    if (basicExportDisposable != null) {
                        basicExportDisposable.dispose();
                        SettingsFragment.this.exportLogDisposable = null;
                    }
                } catch (Exception e) {
                    Timber.e(e);
                }

            }
        });

        progressDialog.show();

        zipFilesProgressDialog = progressDialog;

        return progressDialog;
    }

    @UiThread
    private void cancelZipFilesProgressDialog() {
        final ZipFilesProgressDialog zipFilesProgressDialog = SettingsFragment.this.zipFilesProgressDialog;
        if (zipFilesProgressDialog != null && zipFilesProgressDialog.isShowing()) {
            zipFilesProgressDialog.dismiss();
        }
        SettingsFragment.this.zipFilesProgressDialog = null;
    }

    private void setUpdatePassword(boolean isShow){
        final FragmentSettingsBinding binding = SettingsFragment.this.binding;
        if(binding!=null){
            binding.getRoot().post(new Runnable() {
                @Override
                public void run() {
                    if(isShow) {
                        binding.editTextPassword.setText("");
                        binding.updatePasswordLayout.setVisibility(View.VISIBLE );
                        binding.updateArrow.setImageResource(R.drawable.baseline_chevron_left_24);
                    }else{
                        binding.updatePasswordLayout.setVisibility(View.GONE);
                        binding.updateArrow.setImageResource(R.drawable.baseline_chevron_right_24);
                    }
                }
            });
        }
    }

    public static void delete_updatezip(){
        final List<StorageVolume> storageList = MyStorageManager.getInstance().getStorageList();
        if(!storageList.isEmpty()){
            for (StorageVolume storage:storageList) {
                if(isFAT32(storage)) {
                    final File directory = storage.getDirectory();
                    Timber.d("[delete_updatezip] usbDir=" + directory);
                    final File to = getOTA_rename_to(directory);
                    if (to.exists()) {
                        File from = getOTA_rename_from(directory);
                        if(!to.renameTo(from)){
                            Log.w(TAG,String.format("[delete_updatezip] %s rename to %s fail.", to.getName(), from.getName()));
                        }
                    }
                }
            }
        }

    }

    private static File getOTA_rename_from(File usbDir){
        return new File(usbDir+File.separator+"SystemUpdate.sys");
    }

    private static File getOTA_rename_to(File usbDir){
        return new File(usbDir+File.separator+"update.zip");
    }

    private static boolean isFAT32(StorageVolume storage){
        final Pattern pattern = Pattern.compile("[A-Z0-9]{4}-[A-Z0-9]{4}");
        final Matcher matcher = pattern.matcher(storage.getUuid());
        return matcher.matches();
    }

    private View.OnClickListener upgradeOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            Timber.d("[upgradeOnClickListener] "+LogQueue.getId(view));
            switch (view.getId()){
                case R.id.swUpgradingLayout:
                    if(binding.updatePasswordLayout.getVisibility() == View.VISIBLE){
                        setUpdatePassword(false);
                    }else{
                        final List<StorageVolume> storageList = MyStorageManager.getInstance().getStorageList();
                        if(!storageList.isEmpty()){
                            for (StorageVolume storage:storageList) {
                                if(isFAT32(storage)) {
                                    final File directory = storage.getDirectory();
                                    final File from = getOTA_rename_from(directory);
                                    if (from.exists()) {
                                        setUpdatePassword(true);
                                        return;
                                    }
                                }
                            }
                        }
                        //show 提示訊息: 請將更新檔放在USB隨身碟根目錄，並接上USB隨身碟。
                        showMyDialog(R.string.msg_system_upgrade_warning1,
                                true,
                                R.string.confirm,
                                false,
                                R.string.cancel,
                                null,
                                false);
                    }

                    break;
                case R.id.swUpgrading:
                    MyApplication myApplication = getMyApplication();
                    UserTbData userTbData = null;
                    if (myApplication != null) {
                        userTbData = myApplication.getLoginUserTbData();
                    }
                    UserRoleType roleType = null;
                    if (userTbData != null) {
                        roleType = userTbData.getRoleType();
                    }
                    if (roleType == null) {
                        roleType = GUEST;
                    }
                    switch (roleType) {
                        case GUEST:
                        case ADVANCED_USER:
                        case ADMIN_USER:
                            return;
                        case SERVICE_USER:
                            break;
                    }

                    //判斷密碼是否正確
                    final String pw = binding.editTextPassword.getText().toString();
                    if(!pw.equals("1234")){
                        //show 提示訊息: 密碼錯誤
                        showMyDialog(R.string.password_failed_msg,
                                true,
                                R.string.confirm,
                                false,
                                R.string.cancel,
                                null,
                                false);
                        return;
                    }

                    final List<StorageVolume> storageList = MyStorageManager.getInstance().getStorageList();
                    if(!storageList.isEmpty()){
                        for (StorageVolume storage:storageList) {
                            final File directory = storage.getDirectory();
                            File from      = getOTA_rename_from(directory);
                            if(from.exists()) {
                                //判斷為FAT32
                                if(!isFAT32(storage)){
                                    Timber.d("storage.getUuid()="+storage.getUuid());
                                    //show 提示訊息: 隨身碟格式必須為FAT32
                                    showMyDialog(R.string.fat_warn,
                                            true,
                                            R.string.confirm,
                                            false,
                                            R.string.cancel,
                                            null,
                                            false);
                                    return;
                                }

                                //rename
                                File to        = getOTA_rename_to(directory);
                                Timber.d("[afterTextChanged]file check start ");
                                final boolean b = from.renameTo(to);
                                Timber.d("[afterTextChanged]file check end :"+b);

                                Intent serviceIntent = new Intent("android.rockchip.update.service");
                                String packageName ="android.rockchip.update.service";
                                String className="android.rockchip.update.service.RKUpdateService";
                                serviceIntent.setClassName(packageName,className);
                                serviceIntent.putExtra("command", 1);
                                serviceIntent.putExtra("delay", 5000);
                                FragmentActivity activity = getActivity();
                                if(activity!=null) {
                                    activity.startService(serviceIntent);
                                }else {
                                    Timber.e("activity == null");
                                }
                            }else {
                                //show 提示訊息: SystemUpdate.sys is not exists!
                                showMyDialog(R.string.msg_system_upgrade_warning1,
                                        true,
                                        R.string.confirm,
                                        false,
                                        R.string.cancel,
                                        null,
                                        false);
                            }
                        }
                    }
                    break;
            }
        }
    };

    public int Check_Doorkey(int type){
        String MAGIC_KEY1 = "!$3a068c8324a30eefcc214529dc24f81498c54762a87022bc2#acae126cb52eeacb05fbfa90867d08557a4e18572b1fbe305fa2fe8b3908271eb2504e8$";
        String MAGIC_KEY2 = "!@3b307918d44368b3f36067c64ebd3ceebde049a266925d008#5965b606bc0bb784ab712743f68b47e31c0c0dba2fc9d02948968ab0dd3d5ae42a9a89f@";
        String FILENAME = "";

        @NonNull final List<StorageVolume> storageList = MyStorageManager.getInstance().getStorageList();
        if (storageList.isEmpty()) {
            return 0;
        }

        for (int i = 0; i < storageList.size(); i++) {
            final StorageVolume storage = storageList.get(0);
            final File exportDirectory = storage.getDirectory();
            if (exportDirectory == null) {
                return 0;
            }

            if (type == 1) {
                FILENAME = "MAGIC_KEY1.txt";
            }
            else if (type == 2) {
                FILENAME = "MAGIC_KEY2.txt";
            }else {
                return 0;
            }

            File file = new File(exportDirectory, FILENAME);
            StringBuilder text = new StringBuilder();
            try(BufferedReader br = new BufferedReader(new FileReader(file))) {
                String line;

                while ((line = br.readLine()) != null) {
                    text.append(line);
                }
                //br.close();
            } catch (IOException e) {
                //TODO
            }

            if(type == 1){
                if (text.toString().equals(MAGIC_KEY1)){
                    try {
                        startActivity(new Intent(Settings.ACTION_SETTINGS));
                        return 1;
                    } catch (Exception e) {
                        Timber.e(e);
                    }
                }
            }
        }
        return 0;
    }

    private void showPasswordDialog() {
        final Context mContext = getContext();
        if(mContext==null){
            Timber.e("[showPasswordDialog] mContext==null");
            return;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setTitle(mContext.getString(R.string.msg_engineering_pw));
        final EditText edit = new EditText(mContext);
        edit.setHeight(150);
        edit.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        edit.setFilters(new InputFilter[]{new InputFilter.LengthFilter(4)});
        builder.setView(edit);
        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                final String s = edit.getText().toString();
                if(s.equals("1234") ){
                    if (mContext instanceof MainActivity) {
                        final MainActivity mainActivity = (MainActivity) mContext;
                        mainActivity.getBinding().engineering.setVisibility(View.VISIBLE);
                        mainActivity.onClick(mainActivity.getBinding().engineering);
                    }
                }
            }
        });
        builder.setNeutralButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        });
        final AlertDialog dialog = builder.create();
        dialog.show();

        dialog.getButton(DialogInterface.BUTTON_POSITIVE);
        dialog.getButton(DialogInterface.BUTTON_NEGATIVE);
    }

    private String testShowError4000=null;
    @VisibleForTesting
    public void setTestShowError4000(String value) {
        testShowError4000=value;
        binding.cpuTemperature.removeCallbacks(cpuTemperatureRunnable);
        binding.cpuTemperature.post(cpuTemperatureRunnable);
    }
}
