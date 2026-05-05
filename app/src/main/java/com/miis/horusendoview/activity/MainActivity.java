package com.miis.horusendoview.activity;

import static androidx.core.content.PackageManagerCompat.LOG_TAG;
import static com.miis.horusendoview.tools.KeyboardHideTool.hideKeyboard;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.hardware.usb.UsbDevice;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.os.FileObserver;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.Toast;

import androidx.annotation.AnyThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.annotation.VisibleForTesting;
import androidx.annotation.WorkerThread;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.core.widget.TextViewCompat;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.media3.common.util.Util;

import com.digifly.uart.UartSendCmd;
import com.digifly.uart.UartService;
import com.digifly.uart.UartTool;
import com.digifly.uart.data.UartReceiveAckData;
import com.digifly.uart.data.UartReceiveBaseData;
import com.digifly.uart.listener.UartReceiveListener;
import com.digifly.uart.type.UartBatModeType;
import com.miis.horusendoview.MyApplication;
import com.miis.horusendoview.R;
import com.miis.horusendoview.data.RoomBackupFileSelectDialogAdapterData;
import com.miis.horusendoview.databinding.ActivityMainBinding;
import com.miis.horusendoview.databinding.FragmentCameraBinding;
import com.miis.horusendoview.dialog.Battery5ShutdownDialog;
import com.miis.horusendoview.dialog.MyDialog;
import com.miis.horusendoview.dialog.RoomBackupFileSelectDialog;
import com.miis.horusendoview.dialog.ShutDownDialog;
import com.miis.horusendoview.dialog.StandbyNotificationTimeDialog;
import com.miis.horusendoview.errorcode.Error;
import com.miis.horusendoview.errorcode.IErrorCode;
import com.miis.horusendoview.fragment.CameraFragment;
import com.miis.horusendoview.fragment.DataManagementFragment;
import com.miis.horusendoview.fragment.EngineeringFragment;
import com.miis.horusendoview.fragment.LoginFragment;
import com.miis.horusendoview.fragment.SettingsFragment;
import com.miis.horusendoview.fragment.member.MemberAddFragment;
import com.miis.horusendoview.fragment.member.MemberFragment;
import com.miis.horusendoview.fragment.member.MemberListFragment;
import com.miis.horusendoview.fragment.member.MemberUserAdminFragment;
import com.miis.horusendoview.log.LogQueue;
import com.miis.horusendoview.manager.SharedPreferencesManager;
import com.miis.horusendoview.roomDataBase.MyRoomDatabase;
import com.miis.horusendoview.roomDataBase.procedureFolder.ProcedureFolderTbData;
import com.miis.horusendoview.roomDataBase.userTbData.UserTbData;
import com.miis.horusendoview.service.StandbyNotificationTimeService;
import com.miis.horusendoview.tools.KeyboardHideTool;
import com.miis.horusendoview.type.BootAutoDeleteFileType;
import com.miis.horusendoview.type.DiskInsufficiencyType;
import com.miis.horusendoview.type.UserRoleType;
import com.serenegiant.dialog.MessageDialogFragmentV4;
import com.serenegiant.utils.BuildCheck;
import com.serenegiant.utils.PermissionCheck;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import timber.log.Timber;

public class MainActivity extends BaseActivity implements View.OnClickListener,
        MessageDialogFragmentV4.MessageDialogListener {

    public static final String LOG_TAG = "MainActivity";

    @StringRes
    private static final int ID_PERMISSION_REQUEST_AUDIO = R.string.permission_audio_recording_request;

    @StringRes
    private static final int ID_PERMISSION_REQUEST_EXT_STORAGE = R.string.permission_ext_storage_request;

    @StringRes
    private static final int ID_PERMISSION_REQUEST_CAMERA = R.string.permission_camera_request;

    /**
     * request code for WRITE_EXTERNAL_STORAGE permission
     */
    private static final int REQUEST_PERMISSION_WRITE_EXTERNAL_STORAGE = 0x1234;

    /**
     * request code for RECORD_AUDIO permission
     */
    private static final int REQUEST_PERMISSION_AUDIO_RECORDING = 0x2345;

    /**
     * request code for CAMERA permission
     */
    private static final int REQUEST_PERMISSION_CAMERA = 0x3456;


    public static final long TAB_BTN_CLICKABLE_ON_DELAY = 0L;
    private static final String TAG = MainActivity.class.getSimpleName();

    @NonNull

    private final ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(5);

    @NonNull
    private ActivityMainBinding binding = null;

    @Nullable
    private UartService uartService = null;

    @Nullable
    private StandbyNotificationTimeService standbyNotificationTimeService = null;

    @Nullable
    private ShutDownDialog shutDownDialog = null;

    @Nullable
    private StandbyNotificationTimeDialog standbyNotificationTimeDialog = null;

    private boolean isOnStop = false;

    private long onStartTimeMillis = 0;

    @NonNull
    private final CameraFragment cameraFragment = CameraFragment.newInstance();

    @NonNull
    private final DataManagementFragment dataManagementFragment = DataManagementFragment.newInstance();

    @NonNull
    private final SettingsFragment settingsFragment = SettingsFragment.newInstance();

    @NonNull
    private final MemberFragment memberFragment = MemberFragment.newInstance();

    @Nullable
    private LoginFragment loginFragment = null;

    @Nullable
    private EngineeringFragment engineeringFragment = null;

    @Nullable
    private MyDialog lowPowerDialog = null;

    @Nullable
    private FileObserver fileObserver = null;

    @Nullable
    private ScheduledFuture<?> autoDeleteFileWithDayOutJob = null;
    private final Object autoDeleteFileWithDayOutJobLock = new Object();

    private long tabBtnClickTime = 0;
    private long tabBtnClickTimeDefDiff = 0;

    @Nullable
    private Battery5ShutdownDialog battery5ShutdownDialog = null;
    private final Object battery5ShutdownDialogLock = new Object();

    @Nullable
    private MyDialog diskInsufficiencyDialog = null;
    private final Object diskInsufficiencyDialogLock = new Object();


    @Nullable
    private MyDialog diskInsufficiency30PercentDialog = null;
    private final Object diskInsufficiency30PercentDialogLock = new Object();


    @Nullable
    private MyDialog diskInsufficiency20PercentDialog = null;
    private final Object diskInsufficiency20PercentDialogLock = new Object();

    @Nullable
    private MyDialog recordingReminderDialog = null;
    private final Object recordingReminderDialogLock = new Object();

    @Nullable
    private RoomBackupFileSelectDialog roomBackupFileSelectDialog = null;
    private final Object roomBackupFileSelectDialogLock = new Object();

    @Nullable
    private MyDialog sleepNoChargingDialog = null;


    private int clickTextTimeCount = 0;

    @Nullable
    private ScheduledFuture<?> backupRoomFileJob = null;

    private long dispatchTouchEventCurrentTimeMills=0; //for touch crash issue.

    private boolean isCharging;
    private int batLevel=-1;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(null);

        if (scheduledExecutorService instanceof ScheduledThreadPoolExecutor) {
            ScheduledThreadPoolExecutor s = (ScheduledThreadPoolExecutor) scheduledExecutorService;
            s.setRemoveOnCancelPolicy(true);
        }

        binding = DataBindingUtil.setContentView(this, R.layout.activity_main);

        binding.getRoot().getViewTreeObserver().addOnGlobalFocusChangeListener(new ViewTreeObserver.OnGlobalFocusChangeListener() {
            @Override
            public void onGlobalFocusChanged(View oldFocus, View newFocus) {
                Timber.d("onGlobalFocusChanged oldFocus=" + oldFocus + " | newFocus=" + newFocus);
            }
        });

        UartService.bindService(this, uartServiceConnection);
        StandbyNotificationTimeService.bindService(this, standbyNotificationTimeServiceConnection);

        // 横屏隐藏虚拟按键
        View decorView = getWindow().getDecorView();
        int uiOptions = (View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_FULLSCREEN);
        decorView.setSystemUiVisibility(uiOptions);

        TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(
                binding.textDay,
                16,
                20,
                1,
                TypedValue.COMPLEX_UNIT_PX
        );
        TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(
                binding.textTime,
                16,
                45,
                1,
                TypedValue.COMPLEX_UNIT_PX
        );

        checkProcedureFolderTbDataHasFile();

        deleteTempFile();

        initFragment();

        fileObserver = new FileObserver(
                new File(getMyApplication().getMainDirPath()),
                FileObserver.CREATE | FileObserver.DELETE) {
            @Override
            public void onEvent(int event, @Nullable String path) {
                Timber.d("fileObserver -> onEvent event=" + event + " | path=" + path);

                //Fixed an issue where too many files would crash
                if (path != null) {
                    if (path.contains(MyRoomDatabase.BACKUP_ROOM_DIR_NAME)) {
                        return;
                    }
                    if (path.contains(MyRoomDatabase.DBNAME)) {
                        return;
                    }
                }

                dataManagementFragment.updateFileList();
                final FragmentCameraBinding cBinding = getCameraFragment().getBinding();
                if (cBinding != null) {
                    cBinding.previewWindowsView.checkSelectImgFileExists();
                }
            }
        };
        fileObserver.startWatching();

        startAutoDeleteFileWithDayOut();

        try {
            backupRoomFileJob = scheduledExecutorService.scheduleAtFixedRate(
                    new Runnable() {
                        @Override
                        public void run() {
                            Timber.d("backupRoomFileJob run");
                            getMyApplication().getMyRoomDatabase().backupRoomFile();
                        }
                    },
                    2,
                    2,
                    TimeUnit.HOURS
            );
        } catch (Exception e) {
            Timber.e(e);
        }

        final int screenBrightness = getScreenBrightness(this);
        Timber.d("[onCreate]screenBrightness="+screenBrightness);
        if (screenBrightness < 255) {
            ModifySettingsScreenBrightness(this, 255);
        }

        //Set the volume to maximum
        AudioManager mAudioManager = (AudioManager) MainActivity.this.getSystemService(Context.AUDIO_SERVICE);
        int currVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC) ;// 當前的媒體音量
        final int streamMaxVolume = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);//最大音量
        if(currVolume!=streamMaxVolume) {
            mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, streamMaxVolume, 0);//音量調整
            Timber.d("[onCreate] adjust currVolume="+mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC) );
        }

        LogQueue mLogQueue = LogQueue.getInstance();
    }

    @Override
    protected void onStart() {
        super.onStart();
        Timber.v("onStart:");

        if (BuildCheck.isAndroid7()) {
            internalOnResume();
        }

        setOnStop(false);
        setOnStartTimeMillis(System.currentTimeMillis());

        final ActivityMainBinding binding = this.binding;
        if (binding != null) {
            binding.getRoot().removeCallbacks(batteryAndChargingDataToViewRunnable);
            binding.getRoot().post(batteryAndChargingDataToViewRunnable);
        }

        setClickListener();
        checkUserIcon();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Timber.v("onResume:");

        if (!BuildCheck.isAndroid7()) {
            internalOnResume();
        }

        restartStandbyNotificationTimeServiceTimer();
    }

    @Override
    protected void onPause() {
        Timber.v("onPause:");

        if (!BuildCheck.isAndroid7()) {
            internalOnPause();
        }

        super.onPause();
    }


    @Override
    protected void onStop() {
        Timber.v("onStop:");

        if (BuildCheck.isAndroid7()) {
            internalOnPause();
        }

        super.onStop();

        setOnStop(true);

        StandbyNotificationTimeService standbyNotificationTimeService = this.standbyNotificationTimeService;
        if (standbyNotificationTimeService != null) {
            standbyNotificationTimeService.stopCountDownTimer();
        }

        cancelShutDownDialog();
        cancelStandbyNotificationTimeDialog();

        getMyApplication().getMyRoomDatabase().backupRoomFile();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        cancelShutDownDialog();
        cancelStandbyNotificationTimeDialog();
        stopUartService();
        stopStandbyNotificationTimeService();

        FileObserver fileObserver = this.fileObserver;
        if (fileObserver != null) {
            fileObserver.stopWatching();
        }

        stopAutoDeleteFileWithDayOut();

        final ScheduledFuture<?> backupRoomFileJob = MainActivity.this.backupRoomFileJob;
        try {
            if (backupRoomFileJob != null) {
                backupRoomFileJob.cancel(true);
            }
        } catch (Exception e) {
            Timber.e(e);
        }

        try {
            scheduledExecutorService.shutdown();
        } catch (Exception e) {
            Timber.e(e);
        }
    }

    private void internalOnResume() {
        Timber.v("internalOnResume:");
        checkPermission();
    }

    private void internalOnPause() {
        Timber.v("internalOnPause:");
    }

    @Override
    public boolean dispatchTouchEvent(@NonNull MotionEvent ev) {
        if(Math.abs(System.currentTimeMillis()-dispatchTouchEventCurrentTimeMills) >=1000){
            try {
                scheduledExecutorService.execute(new Runnable() {
                    @Override
                    public void run() {
                        restartStandbyNotificationTimeServiceTimer();
                    }
                });
            } catch (Exception e) {
                Timber.e(e);
            }
        }
        dispatchTouchEventCurrentTimeMills=System.currentTimeMillis();

        ActivityMainBinding binding = getBinding();

        if (binding != null) {
            if (ev.getAction() == MotionEvent.ACTION_UP ||
                    ev.getAction() == MotionEvent.ACTION_CANCEL) {
                boolean isCustomKeyboardView = KeyboardHideTool.isCustomKeyboardView(binding.customKeyboardView, ev);

                if (!isCustomKeyboardView) {
                    hideKeyboard(MainActivity.this, ev);
                }
            }
        }
        return super.dispatchTouchEvent(ev);
    }

    @Override
    public void onBackPressed() {
        ActivityMainBinding binding = getBinding();
        if (binding != null) {
            if (binding.customKeyboardView.isExpanded()) {
                binding.customKeyboardView.translateLayout();
            } else {
                super.onBackPressed();
            }
        }
    }


    @Override
    public void onMessageDialogResult(MessageDialogFragmentV4 messageDialogFragmentV4, int requestCode, String[] permissions, boolean result) {
        Timber.d("[onMessageDialogResult]requestCode=" + requestCode + ", result=" + result);
        if (result) {
            // Request permission when OK is pressed in the message dialog
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Timber.d("[onMessageDialogResult]BuildCheck.isMarshmallow()");
                requestPermissions(permissions, requestCode);
                return;
            }
        }

        // Check permissions manually when canceled in the message dialog or when not on Android 6 or higher
        for (String permission : permissions) {
            checkPermissionResult(requestCode, permission, PermissionCheck.hasPermission(this, permission));
        }
    }


    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {
        super.onPointerCaptureChanged(hasCapture);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.ibLiveView:
                Timber.d("onClick -> ibLiveView");
                if (getCameraFragment().isVisible()) {
                    return;
                }

                long diffClickTime = Math.abs(System.currentTimeMillis() - tabBtnClickTime);
                if (diffClickTime < tabBtnClickTimeDefDiff) {
                    return;
                }

                tabBtnClickTime = System.currentTimeMillis();

                setTabBtnClickable(false);
                FragmentManager fragmentManager = getSupportFragmentManager();
                FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                fragmentTransaction.setReorderingAllowed(true);

                if (!getDataManagementFragment().isHidden()) {
                    fragmentTransaction.hide(getDataManagementFragment());
                }
                if (!getSettingsFragment().isHidden()) {
                    fragmentTransaction.hide(getSettingsFragment());
                }
                if (!getMemberFragment().isHidden()) {
                    Fragment memberNowFragment = getMemberFragment().getChangeFragmentManager().getNowFragment();
                    if (memberNowFragment instanceof MemberUserAdminFragment ||
                            memberNowFragment instanceof MemberAddFragment) {
                        getMemberFragment().getChangeFragmentManager().closePageTo(MemberListFragment.class);
                    }
                    fragmentTransaction.hide(getMemberFragment());
                }
                fragmentTransaction.show(getCameraFragment());

                Fragment loginFragment = getLoginFragment();
                if (loginFragment != null && loginFragment.isAdded()) {
                    fragmentTransaction.remove(loginFragment);
                }

                if (engineeringFragment!=null && !getEngineeringFragment().isHidden()) {
                    fragmentTransaction.hide(getEngineeringFragment());
                }

                if (fragmentManager.isStateSaved()) {
                    fragmentTransaction.commitAllowingStateLoss();
                } else {
                    fragmentTransaction.commit();
                }
                break;

            case R.id.ibDataManagement:
                Timber.d("onClick -> ibDataManagement");

                MyApplication myApp = getMyApplication();
                UserTbData loginUserTbData = myApp.getLoginUserTbData();

                if (loginUserTbData == null || loginUserTbData.getRoleType() == UserRoleType.GUEST) {
                    showLoginFragment(new Runnable() {
                        @Override
                        public void run() {
                            ActivityMainBinding binding = getBinding();
                            if (binding != null) {
                                binding.ibDataManagement.performClick();
                            }
                        }
                    });
                    return;
                }

                if (getDataManagementFragment().isVisible()) {
                    return;
                }

                diffClickTime = Math.abs(System.currentTimeMillis() - tabBtnClickTime);
                if (diffClickTime < tabBtnClickTimeDefDiff) {
                    return;
                }
                tabBtnClickTime = System.currentTimeMillis();

                setTabBtnClickable(false);

                fragmentManager = getSupportFragmentManager();
                fragmentTransaction = fragmentManager.beginTransaction();
                fragmentTransaction.setReorderingAllowed(true);

                if (!getSettingsFragment().isHidden()) {
                    fragmentTransaction.hide(getSettingsFragment());
                }
                if (!getCameraFragment().isHidden()) {
                    fragmentTransaction.hide(getCameraFragment());
                }
                if (!getMemberFragment().isHidden()) {
                    Fragment memberNowFragment = getMemberFragment().getChangeFragmentManager().getNowFragment();
                    if (memberNowFragment instanceof MemberUserAdminFragment ||
                            memberNowFragment instanceof MemberAddFragment) {
                        getMemberFragment().getChangeFragmentManager().closePageTo(MemberListFragment.class);
                    }
                    fragmentTransaction.hide(memberFragment);
                }
                if (engineeringFragment!=null && !getEngineeringFragment().isHidden()) {
                    fragmentTransaction.hide(getEngineeringFragment());
                }
                fragmentTransaction.show(dataManagementFragment);
                if (fragmentManager.isStateSaved()) {
                    fragmentTransaction.commitAllowingStateLoss();
                } else {
                    fragmentTransaction.commit();
                }
                break;

            case R.id.ibSettings:
                Timber.d("onClick -> ibSettings");
                if (getSettingsFragment().isVisible()) {
                    return;
                }

                diffClickTime = Math.abs(System.currentTimeMillis() - tabBtnClickTime);
                if (diffClickTime < tabBtnClickTimeDefDiff) {
                    return;
                }
                tabBtnClickTime = System.currentTimeMillis();

                fragmentManager = getSupportFragmentManager();
                fragmentTransaction = fragmentManager.beginTransaction();
                fragmentTransaction.setReorderingAllowed(true);

                if (!getDataManagementFragment().isHidden()) {
                    fragmentTransaction.hide(getDataManagementFragment());
                }
                if (!getCameraFragment().isHidden()) {
                    fragmentTransaction.hide(getCameraFragment());
                }
                if (!getMemberFragment().isHidden()) {
                    Fragment memberNowFragment = getMemberFragment().getChangeFragmentManager().getNowFragment();
                    if (memberNowFragment instanceof MemberUserAdminFragment ||
                            memberNowFragment instanceof MemberAddFragment) {
                        getMemberFragment().getChangeFragmentManager().closePageTo(MemberListFragment.class);
                    }
                    fragmentTransaction.hide(memberFragment);
                }
                fragmentTransaction.show(settingsFragment);

                loginFragment = getLoginFragment();
                if (loginFragment != null && loginFragment.isAdded()) {
                    fragmentTransaction.remove(loginFragment);
                }

                if (engineeringFragment!=null && !getEngineeringFragment().isHidden()) {
                    fragmentTransaction.hide(getEngineeringFragment());
                }

                if (fragmentManager.isStateSaved()) {
                    fragmentTransaction.commitAllowingStateLoss();
                } else {
                    fragmentTransaction.commit();
                }
                break;
            case R.id.user:
                Timber.d("onClick -> user");
                myApp = getMyApplication();
                loginUserTbData = myApp.getLoginUserTbData();

                if (loginUserTbData == null) {
                    showLoginFragment(new Runnable() {
                        @Override
                        public void run() {
                            ActivityMainBinding binding = getBinding();
                            if (binding != null) {
                                binding.user.performClick();
                            }
                        }
                    });
                    return;
                }

                if (getMemberFragment().isVisible()) {
                    return;
                }

                diffClickTime = Math.abs(System.currentTimeMillis() - tabBtnClickTime);
                if (diffClickTime < tabBtnClickTimeDefDiff) {
                    return;
                }
                tabBtnClickTime = System.currentTimeMillis();

                fragmentManager = getSupportFragmentManager();
                fragmentTransaction = fragmentManager.beginTransaction();
                fragmentTransaction.setReorderingAllowed(true);

                if (!getCameraFragment().isHidden()) {
                    fragmentTransaction.hide(getCameraFragment());
                }
                if (!getDataManagementFragment().isHidden()) {
                    fragmentTransaction.hide(getDataManagementFragment());
                }
                if (!getSettingsFragment().isHidden()) {
                    fragmentTransaction.hide(getSettingsFragment());
                }
                if (engineeringFragment!=null && !getEngineeringFragment().isHidden()) {
                    fragmentTransaction.hide(getEngineeringFragment());
                }
                fragmentTransaction.show(getMemberFragment());
                if (fragmentManager.isStateSaved()) {
                    fragmentTransaction.commitAllowingStateLoss();
                } else {
                    fragmentTransaction.commit();
                }
                break;
            case R.id.engineering:
                if(getEngineeringFragment().isVisible()){
                    return;
                }

                diffClickTime = Math.abs(System.currentTimeMillis() - tabBtnClickTime);
                if (diffClickTime < tabBtnClickTimeDefDiff) {
                    return;
                }

                tabBtnClickTime = System.currentTimeMillis();

                setTabBtnClickable(false);
                fragmentManager = getSupportFragmentManager();
                fragmentTransaction = fragmentManager.beginTransaction();
                fragmentTransaction.setReorderingAllowed(true);

                final Fragment efragment = fragmentManager.findFragmentByTag(EngineeringFragment.class.getSimpleName());
                if(efragment==null){
                    fragmentTransaction.add(R.id.container, getEngineeringFragment(), EngineeringFragment.class.getSimpleName());
                    setTabBtnSelected(binding.engineering);
                }else{
                    setTabBtnClickable(false);
                }

                if (!getCameraFragment().isHidden()) {
                    fragmentTransaction.hide(getCameraFragment());
                }
                if (!getDataManagementFragment().isHidden()) {
                    fragmentTransaction.hide(getDataManagementFragment());
                }
                if (!getSettingsFragment().isHidden()) {
                    fragmentTransaction.hide(getSettingsFragment());
                }
                if (!getMemberFragment().isHidden()) {
                    Fragment memberNowFragment = getMemberFragment().getChangeFragmentManager().getNowFragment();
                    if (memberNowFragment instanceof MemberUserAdminFragment ||
                            memberNowFragment instanceof MemberAddFragment) {
                        getMemberFragment().getChangeFragmentManager().closePageTo(MemberListFragment.class);
                    }
                    fragmentTransaction.hide(memberFragment);
                }
                fragmentTransaction.show(getEngineeringFragment());
                loginFragment = getLoginFragment();
                if (loginFragment != null && loginFragment.isAdded()) {
                    fragmentTransaction.remove(loginFragment);
                }
                if (fragmentManager.isStateSaved()) {
                    fragmentTransaction.commitAllowingStateLoss();
                } else {
                    fragmentTransaction.commit();
                }
                if (uartService != null) {
                    uartService.sendUartCmd(UartSendCmd.GET_MCU_VERSION);
                }
                break;
            case R.id.textTime:
                clickTextTimeCount++;
                if (clickTextTimeCount >= 20) {
                    clickTextTimeCount = 0;
                    showRoomBackupFileSelectDialog();
                }
                clickTextTimeCountDownTimer.cancel();
                clickTextTimeCountDownTimer.start();
                break;
            case R.id.ivBattery:
//                getMyApplication().getMyRoomDatabase().backupRoomFile();
                break;

            case R.id.changeCamera:
                Timber.d("onClick -> changeCamera");
                CameraFragment cameraFragment = getCameraFragment();
                if(cameraFragment==null){
                    Timber.w("[onClick: changeCamera] cameraFragment==null");
                }else if (cameraFragment.isTakePhotoRunning()) {
                    Timber.w("changeCamera but takePhoto Run.");
                }else {
                    if(cameraFragment.isVisible()){
                        cameraFragment.changeCamera();
                    }else if( binding.ibLiveView.isEnabled() && binding.ibLiveView.isClickable()){
                        binding.ibLiveView.performClick();
                        cameraFragment.changeCamera();
                    }
                }
                break;

        }
    }

    private void setClickListener() {
        ActivityMainBinding binding = getBinding();
//        if (binding == null) {
//            return;
//        }
        binding.ibLiveView.setOnClickListener(this);
        binding.ibDataManagement.setOnClickListener(this);
        binding.ibSettings.setOnClickListener(this);
        binding.user.setOnClickListener(this);
        binding.engineering.setOnClickListener(this);
        binding.textTime.setOnClickListener(this);
        binding.ivBattery.setOnClickListener(this);

        binding.changeCamera.setOnClickListener(this);
        binding.changeCameraImg.setImageResource(R.drawable.icon_change_camera_02);
    }


    private boolean checkPermission() {
        return (checkPermissionCamera()
                && checkPermissionAudio()
                && checkPermissionWriteExternalStorage());
    }

    private void initFragment() {
        setTabBtnClickable(false);
        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.registerFragmentLifecycleCallbacks(fragmentLifecycleCallbacks, false);

        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.setReorderingAllowed(true);

        List<Fragment> oldFragmentList = fragmentManager.getFragments();
        for (Fragment oldFragment : oldFragmentList) {
            fragmentTransaction.remove(oldFragment);
        }

        fragmentTransaction.add(R.id.container, getMemberFragment(), MemberFragment.class.getSimpleName());
        fragmentTransaction.hide(getMemberFragment());

        fragmentTransaction.add(R.id.container, getSettingsFragment(), SettingsFragment.class.getSimpleName());
        fragmentTransaction.hide(getSettingsFragment());

        fragmentTransaction.add(R.id.container, getDataManagementFragment(), DataManagementFragment.class.getSimpleName());
        fragmentTransaction.hide(getDataManagementFragment());

        fragmentTransaction.add(R.id.container, getCameraFragment(), CameraFragment.class.getSimpleName());

        UserTbData loginUserTbData = getMyApplication().getLoginUserTbData();

//        if (loginUserTbData == null) {
//            fragmentTransaction.hide(getCameraFragment());
//        } else {
//            fragmentTransaction.show(getCameraFragment());
//        }

        if (fragmentManager.isStateSaved()) {
            fragmentTransaction.commitAllowingStateLoss();
        } else {
            fragmentTransaction.commit();
        }

//        if (loginUserTbData == null) {
//            showLoginFragment(null);
//        }
    }


    private FragmentManager.FragmentLifecycleCallbacks fragmentLifecycleCallbacks = new FragmentManager.FragmentLifecycleCallbacks() {
        @Override
        public void onFragmentPreAttached(@NonNull FragmentManager fm, @NonNull Fragment f, @NonNull Context context) {
            super.onFragmentPreAttached(fm, f, context);
            Timber.d("onFragmentPreAttached Fragment=%s", f.toString());
        }

        @Override
        public void onFragmentAttached(@NonNull FragmentManager fm, @NonNull Fragment f, @NonNull Context context) {
            super.onFragmentAttached(fm, f, context);
            Timber.d("onFragmentAttached Fragment=%s", f.toString());
        }

        @Override
        public void onFragmentPreCreated(@NonNull FragmentManager fm, @NonNull Fragment f, @Nullable Bundle savedInstanceState) {
            super.onFragmentPreCreated(fm, f, savedInstanceState);
            Timber.d("fonFragmentPreCreated Fragment=%s", f.toString());
        }

        @Override
        public void onFragmentCreated(@NonNull FragmentManager fm, @NonNull Fragment f, @Nullable Bundle savedInstanceState) {
            super.onFragmentCreated(fm, f, savedInstanceState);
            Timber.d("onFragmentCreated Fragment=%s", f.toString());
        }

        @Override
        public void onFragmentViewCreated(@NonNull FragmentManager fm, @NonNull Fragment f, @NonNull View v, @Nullable Bundle savedInstanceState) {
            super.onFragmentViewCreated(fm, f, v, savedInstanceState);
            Timber.d("onFragmentViewCreated Fragment=%s", f.toString());
        }

        @Override
        public void onFragmentStarted(@NonNull FragmentManager fm, @NonNull Fragment f) {
            super.onFragmentStarted(fm, f);
            Timber.d("onFragmentStarted Fragment=%s", f.toString());
        }

        @Override
        public void onFragmentResumed(@NonNull FragmentManager fm, @NonNull Fragment f) {
            super.onFragmentResumed(fm, f);
            Timber.d("onFragmentResumed Fragment=%s", f.toString());
        }

        @Override
        public void onFragmentPaused(@NonNull FragmentManager fm, @NonNull Fragment f) {
            super.onFragmentPaused(fm, f);
            Timber.d("onFragmentPaused Fragment=%s", f.toString());
        }

        @Override
        public void onFragmentStopped(@NonNull FragmentManager fm, @NonNull Fragment f) {
            super.onFragmentStopped(fm, f);
            Timber.d("onFragmentStopped Fragment=%s", f.toString());
        }

        @Override
        public void onFragmentSaveInstanceState(@NonNull FragmentManager fm, @NonNull Fragment f, @NonNull Bundle outState) {
            super.onFragmentSaveInstanceState(fm, f, outState);
            Timber.d("onFragmentSaveInstanceState Fragment=%s", f.toString());
        }

        @Override
        public void onFragmentViewDestroyed(@NonNull FragmentManager fm, @NonNull Fragment f) {
            super.onFragmentViewDestroyed(fm, f);
            Timber.d("onFragmentViewDestroyed Fragment=%s", f.toString());
        }

        @Override
        public void onFragmentDestroyed(@NonNull FragmentManager fm, @NonNull Fragment f) {
            super.onFragmentDestroyed(fm, f);
            Timber.d("onFragmentDestroyed Fragment=%s", f.toString());
        }

        @Override
        public void onFragmentDetached(@NonNull FragmentManager fm, @NonNull Fragment f) {
            super.onFragmentDetached(fm, f);
            Timber.d("onFragmentDetached Fragment=%s", f.toString());
        }
    };


    private void checkPermissionResult(int requestCode, String permission, boolean result) {
        // Display a message when permissions are missing
        if (!result && permission != null) {
            StringBuilder sb = new StringBuilder();
            if (Manifest.permission.RECORD_AUDIO.equals(permission)) {
                sb.append(getString(R.string.permission_audio));
            }
            if (Manifest.permission.WRITE_EXTERNAL_STORAGE.equals(permission) || Manifest.permission.READ_EXTERNAL_STORAGE.equals(permission)) {
                if (sb.length() > 0) {
                    sb.append("\n");
                }
                sb.append(getString(R.string.permission_ext_storage));
            }
            if (Manifest.permission.CAMERA.equals(permission)) {
                if (sb.length() > 0) {
                    sb.append("\n");
                }
                sb.append(getString(R.string.permission_camera));
            }
            if (Manifest.permission.INTERNET.equals(permission)) {
                if (sb.length() > 0) {
                    sb.append("\n");
                }
                sb.append(getString(R.string.permission_network));
            }
            if (Manifest.permission.ACCESS_FINE_LOCATION.equals(permission)) {
                if (sb.length() > 0) {
                    sb.append("\n");
                }
                sb.append(getString(R.string.permission_location));
            }
            Toast.makeText(this, sb.toString(), Toast.LENGTH_LONG).show();
        }

        if (Manifest.permission.CAMERA.equals(permission) && result) {
            CameraFragment cameraFragment = getCameraFragment();
            cameraFragment.checkCameraHelper();
        }
    }

    /**
     * Check permission to access the internal camera and request to show a detail dialog to request permission.
     *
     * @return true if already have permission to access the internal camera
     */
    private boolean checkPermissionCamera() {
        if (!PermissionCheck.hasCamera(this)) {
            Timber.d("[checkPermissionCamera]");
            MessageDialogFragmentV4.showDialog(
                    this,
                    REQUEST_PERMISSION_CAMERA,
                    R.string.permission_title,
                    ID_PERMISSION_REQUEST_CAMERA,
                    new String[]{Manifest.permission.CAMERA}
            );
            return false;
        }
        return true;
    }

    /**
     * Check permission to access external storage and request to show a detail dialog to request permission.
     *
     * @return true if already have permission to access external storage
     */
    private boolean checkPermissionWriteExternalStorage() {
        if (!PermissionCheck.hasWriteExternalStorage(this)) {
            MessageDialogFragmentV4.showDialog(
                    this, REQUEST_PERMISSION_WRITE_EXTERNAL_STORAGE,
                    R.string.permission_title, ID_PERMISSION_REQUEST_EXT_STORAGE, new String[]{
                            Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            Manifest.permission.READ_EXTERNAL_STORAGE
                    }
            );
            return false;
        }
        return true;
    }

    /**
     * Check permission to record audio and request to show a detail dialog to request permission.
     *
     * @return true if already have permission to record audio
     */
    private boolean checkPermissionAudio() {
        if (!PermissionCheck.hasAudio(this)) {
            MessageDialogFragmentV4.showDialog(
                    this,
                    REQUEST_PERMISSION_AUDIO_RECORDING,
                    R.string.permission_title,
                    ID_PERMISSION_REQUEST_AUDIO,
                    new String[]{Manifest.permission.RECORD_AUDIO}
            );
            return false;
        }
        return true;
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        int n = Math.min(permissions.length, grantResults.length);
        for (int i = 0; i < n; i++) {
            checkPermissionResult(
                    requestCode, permissions[i],
                    grantResults[i] == PackageManager.PERMISSION_GRANTED
            );
        }
        checkPermission();
    }

    private final ServiceConnection uartServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(@NonNull ComponentName name, @NonNull IBinder service) {
            Timber.d("serviceConnection -> name=" + name);
            if (service instanceof UartService.LocalBinder) {
                UartService uartService = ((UartService.LocalBinder) service).getUartService();
                MainActivity.this.uartService = uartService;
                uartService.addReceiveListener(uartReceiveListener);

                uartService.sendUartCmd(UartSendCmd.GET_MCU_VERSION);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Timber.d("onServiceDisconnected -> ComponentName=" + name);
            stopUartService();
        }
    };

    /**
     * Stop the UartService.
     */
    private synchronized void stopUartService() {
        final UartService uartService = this.uartService;
        if (uartService != null) {
            uartService.removeReceiveListener(uartReceiveListener);
            uartService.stopSelf();
        }
        UartService.unbindService(this, uartServiceConnection);
        this.uartService = null;
    }

    /**
     * UART 接收監聽器
     */
    private UartReceiveListener uartReceiveListener = new UartReceiveListener() {
        @Override
        public void onReceiveData(@NonNull final UartReceiveBaseData uartReceiveBaseData, @Nullable final UartReceiveBaseData oldUartReceiveBaseData) {
            // 這是在後台執行的線程
            if (UartService.isShowReceiveDataLog) {
                Timber.i("uartReceiveBaseData=%s", uartReceiveBaseData);
            }
            writeBatteryDataToSys();
            UartBatModeType uartBatModeType = uartReceiveBaseData.getUartBatModeType();
            UartBatModeType oldUartBatModeType = null;
            if (oldUartReceiveBaseData != null) {
                oldUartBatModeType = oldUartReceiveBaseData.getUartBatModeType();
            }

            //Timber.d("uartReceiveListener ->onReceiveData  uartBatModeType="+ uartBatModeType + " | oldUartBatModeType=" + oldUartBatModeType);
            if ((uartBatModeType == UartBatModeType.BMODE_IDLE_POWER_KEY_RELEASE ||
                    uartBatModeType == UartBatModeType.BMODE_CHARGING_POWER_KEY_RELEASE ||
                    uartBatModeType == UartBatModeType.BMODE_FULL_POWER_KEY_RELEASE ||
                    uartBatModeType == UartBatModeType.BMODE_DISCHARGING_POWER_KEY_RELEASE) &&
                    (oldUartBatModeType == UartBatModeType.BMODE_IDLE_POWER_KEY_PRESS ||
                            oldUartBatModeType == UartBatModeType.BMODE_CHARGING_POWER_KEY_PRESS ||
                            oldUartBatModeType == UartBatModeType.BMODE_FULL_POWER_KEY_PRESS ||
                            oldUartBatModeType == UartBatModeType.BMODE_DISCHARGING_POWER_KEY_PRESS)) {
                myRoomDatabaseReOpenCountDownTimer.cancel();
                myRoomDatabaseReOpenCountDownTimer.start();

                // e 到 f 的轉變，開啟小視窗
                // 顯示關機對話框
                // 在從休眠中恢復後阻止 1 秒，以避免立即顯示對話框
                long diff = Math.abs(System.currentTimeMillis() - onStartTimeMillis);
                if (!isOnStop() && diff > 1000) {
                    if(cameraFragment!=null &&
                            cameraFragment.getICameraHelper()!=null &&
                            cameraFragment.getICameraHelper().isRecording()){
                        showRecordingReminderDialog();
                    }else {
                        cancelStandbyNotificationTimeDialog();
                        showShutDownDialog();
                    }
                }
                restartStandbyNotificationTimeServiceTimer();
            }

            if ((uartBatModeType == UartBatModeType.BMODE_IDLE_POWER_KEY_PRESS ||
                    uartBatModeType == UartBatModeType.BMODE_CHARGING_POWER_KEY_PRESS ||
                    uartBatModeType == UartBatModeType.BMODE_FULL_POWER_KEY_PRESS ||
                    uartBatModeType == UartBatModeType.BMODE_DISCHARGING_POWER_KEY_PRESS) &&
                    (oldUartBatModeType == UartBatModeType.BMODE_IDLE_POWER_KEY_RELEASE ||
                            oldUartBatModeType == UartBatModeType.BMODE_CHARGING_POWER_KEY_RELEASE ||
                            oldUartBatModeType == UartBatModeType.BMODE_FULL_POWER_KEY_RELEASE ||
                            oldUartBatModeType == UartBatModeType.BMODE_DISCHARGING_POWER_KEY_RELEASE)
            ) {
                myRoomDatabaseReOpenCountDownTimer.cancel();
            }

            checkShowLowPowerDialog();

            checkRunBattery5Shutdown();
        }

        @Override
        public void onReceiveAckData(UartReceiveAckData uartReceiveAckData) {
            if (uartReceiveAckData.isOk()) {
                reSendShutdownCount = -1;
                final ActivityMainBinding binding = getBinding();
                if (binding != null) {
                    binding.getRoot().removeCallbacks(timeoutReSendShutdownRunnable);
                }
                reSendIdleCount = -1;
                if (binding != null) {
                    binding.getRoot().removeCallbacks(timeoutReSendIdleRunnable);
                }
            }

            final byte[] receiveData = uartReceiveAckData.getOriginData();
            if(receiveData.length ==7){  //Get MCU FW Version
                Timber.d("[onAck] MCU FW:"+ UartTool.byteArrayToHexString(receiveData, true));
                final String mcu_fw = String.format("V%02x.%02x", receiveData[4], receiveData[5]);
                if(engineeringFragment!=null && EngineeringFragment.Check_Doorkey(3)==3) {
                    Toast.makeText(MainActivity.this, getString(R.string.mcu_fw, mcu_fw), Toast.LENGTH_LONG).show();
                }else{
                    Log.d(MainActivity.class.getSimpleName(),"[onAck] MCU FW:"+ mcu_fw);
                }
            }
        }
    };

    /**
     * 將電池數據寫入系統
     */
    public void writeBatteryDataToSys() {
        try {
            scheduledExecutorService.execute(new Runnable() {
                @Override
                public void run() {
                    final UartService uartService = getUartService();
                    UartReceiveBaseData data = uartService != null ? uartService.getNowUartReceiveBaseData() : null;

                    Integer batteryRsoc = data != null ? data.getBatteryRsoc() : null;

                    UartBatModeType batModeType = data != null ? data.getUartBatModeType() : null;

                    if (batteryRsoc != null && batteryRsoc >= 0) {
                        File capacityFile = new File("/sys/class/power_supply/ptc-bat/capacity");
                        if (capacityFile.canWrite()){
                            try(FileOutputStream outputStream = new FileOutputStream(capacityFile)) {
                                outputStream.write(String.valueOf(batteryRsoc).getBytes());
                                outputStream.flush();
                            } catch (Exception e) {
                                Timber.e("capacityFile write: "+e);
                            }

                        }
                    }

                    if (batModeType != null) {
                        try {
                            int value = batModeType.getValue() & 0x0f;

                            File statusFile = new File("/sys/class/power_supply/ptc-bat/status");

                            int d;
                            if (value == 0x01) {
                                d = 1;
                            } else if (value == 0x02) {
                                d = 4;
                            } else if (value == 0x03) {
                                d = 2;
                            } else {
                                d = 3;
                            }

                            if (statusFile.canWrite()) {
                                try(FileOutputStream outputStream = new FileOutputStream(statusFile)){
                                    outputStream.write(String.valueOf(d).getBytes());
                                    outputStream.flush();
                                }catch (Exception e){
                                    Timber.e("statusFile write: "+e);
                                }
                            }
                        } catch (Exception e) {
                            Timber.e(e);
                        }
                    }
                }
            });
        } catch (Exception e) {
            Timber.e(e);
        }
    }

    /**
     * 顯示 ShutDown 小視窗
     */
    @AnyThread
    public synchronized void showShutDownDialog() {
        Timber.d("[showShutDownDialog]");
        final ShutDownDialog shutDownDialog = this.shutDownDialog;
        if (shutDownDialog != null && shutDownDialog.isShowing()) {
            return;
        }

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                final ShutDownDialog d = new ShutDownDialog(MainActivity.this) {
                    @Override
                    public boolean dispatchTouchEvent(MotionEvent ev) {
                        restartStandbyNotificationTimeServiceTimer();
                        return super.dispatchTouchEvent(ev);
                    }
                };

                d.setListener(new ShutDownDialog.Listener() {
                    @Override
                    public void OnClickSleep() {
                        //The sleep function is only available when the device is unchanged.
                        if(isCharging){
                            Timber.w("[showShutDownDialog] The sleep function is only available when the device is unchanged.");
                            showSleepWithNoChargingDialog();
                            return;
                        }

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                getMyApplication().setLoginUserTbData(null);
//                            UserTbData user = getMyApplication().getMyRoomDatabase().userTbDataDao().findByAccount(getMyApplication().DEMO_ACCOUNT);
//                            getMyApplication().setLoginUserTbData(user);
//                            getMyApplication().setPatientId(null);

                            }
                        });

                        try {
                            scheduledExecutorService.execute(new Runnable() {
                                @Override
                                public void run() {
                                    Timber.d("showShutDownDialog sendUartCmd IDLE");
                                    final ActivityMainBinding binding = getBinding();
                                    reSendIdleCount = 0;
                                    if (binding != null) {
                                        binding.getRoot().removeCallbacks(timeoutReSendIdleRunnable);
                                    }
                                    timeoutReSendIdleRunnable.run();
                                }
                            });
                        } catch (Exception e) {
                            Timber.e(e);
                        }
                    }

                    @Override
                    public void OnClickCancel() {
                        // Handle cancel action here
                    }

                    @Override
                    public void OnClickShoutDown() {
                        scheduledExecutorService.execute(new Runnable() {
                            @Override
                            public void run() {
                                Timber.d("showShutDownDialog sendUartCmd ShoutDown");
                                reSendShutdownCount = 0;
                                final ActivityMainBinding binding = getBinding();
                                if (binding != null) {
                                    binding.getRoot().removeCallbacks(timeoutReSendShutdownRunnable);
                                }

                                getMyApplication().getMyRoomDatabase().backupRoomFile();

                                timeoutReSendShutdownRunnable.run();
                            }
                        });
                    }
                });

                d.show();
                MainActivity.this.shutDownDialog = d;
            }
        });
    }

    /**
     * 關閉 ShutDown 小視窗
     */
    @AnyThread
    public synchronized void cancelShutDownDialog() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                final ShutDownDialog shutDownDialog = MainActivity.this.shutDownDialog;
                if (shutDownDialog != null && shutDownDialog.isShowing()) {
                    shutDownDialog.dismiss();
                }
                MainActivity.this.shutDownDialog = null;
            }
        });
    }

    private final ServiceConnection standbyNotificationTimeServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Timber.d("StandbyNotificationTimeServiceConnection -> onServiceConnected  name=" + name);
            if (service instanceof StandbyNotificationTimeService.LocalBinder) {
                final StandbyNotificationTimeService standbyNotificationTimeService =
                        ((StandbyNotificationTimeService.LocalBinder) service).getStandbyNotificationTimeService();
                MainActivity.this.standbyNotificationTimeService = standbyNotificationTimeService;
                standbyNotificationTimeService.addListener(standbyNotificationTimeServiceListener);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Timber.d("onServiceDisconnected -> ComponentName=" + name);
            stopUartService();
        }
    };


    /**
     * 停止 StandbyNotificationTimeService
     */
    private synchronized void stopStandbyNotificationTimeService() {
        final StandbyNotificationTimeService standbyNotificationTimeService = this.standbyNotificationTimeService;
        if (standbyNotificationTimeService != null) {
            standbyNotificationTimeService.stopCountDownTimer();
            standbyNotificationTimeService.removeListener(standbyNotificationTimeServiceListener);
            standbyNotificationTimeService.stopSelf();
        }
        StandbyNotificationTimeService.unbindService(this, standbyNotificationTimeServiceConnection);
        this.standbyNotificationTimeService = null;
    }

    /**
     * 重啟 StandbyNotificationTimeService 的 Timer
     */
    public synchronized void restartStandbyNotificationTimeServiceTimer() {
        final StandbyNotificationTimeService standbyNotificationTimeService = this.standbyNotificationTimeService;
        if (standbyNotificationTimeService != null) {
            standbyNotificationTimeService.startCountDownTimer();
        }
    }


    /**
     * StandbyNotificationTimeService  聆聽
     */
    private final StandbyNotificationTimeService.Listener standbyNotificationTimeServiceListener =
            new StandbyNotificationTimeService.Listener() {
                @Override
                public void onTimeout() {
                    final CameraFragment cameraFragment = getCameraFragment();
                    if(cameraFragment !=null && cameraFragment.getICameraHelper().isCameraOpened()){
                        Timber.d("[standbyNotificationTimeServiceListener: onTimeout] CameraOpened");
                        return;
                    }

                    // 這裡是背景線程
                    cancelShutDownDialog();
                    showStandbyNotificationTimeDialog();
                }
            };

    /**
     * 顯示 StandbyNotificationTimeDialog 小視窗
     */
    @AnyThread
    public synchronized void showStandbyNotificationTimeDialog() {
        final StandbyNotificationTimeDialog standbyNotificationTimeDialog = MainActivity.this.standbyNotificationTimeDialog;
        if (standbyNotificationTimeDialog != null && standbyNotificationTimeDialog.isShowing()) {
            return;
        }
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (isOnStop()) {
                    return;
                }
                StandbyNotificationTimeDialog d = new StandbyNotificationTimeDialog(MainActivity.this) {
                    @Override
                    public boolean dispatchTouchEvent(MotionEvent ev) {
                        restartStandbyNotificationTimeServiceTimer();
                        return super.dispatchTouchEvent(ev);
                    }
                };
                d.setListener(new StandbyNotificationTimeDialog.Listener() {
                    @Override
                    public void OnClickSleep() {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                getMyApplication().setLoginUserTbData(null);
                            }
                        });

                        reSendIdleCount = 0;

                        final ActivityMainBinding binding = getBinding();
                        if (binding != null) {
                            binding.getRoot().removeCallbacks(timeoutReSendIdleRunnable);
                        }
                        timeoutReSendIdleRunnable.run();
                    }

                    @Override
                    public void OnClickCancel() {

                    }
                });
                d.show();
                MainActivity.this.standbyNotificationTimeDialog = d;
            }
        });
    }

    /**
     * 關閉 StandbyNotificationTimeDialog 小視窗
     */
    @AnyThread
    public synchronized void cancelStandbyNotificationTimeDialog() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                final StandbyNotificationTimeDialog standbyNotificationTimeDialog = MainActivity.this.standbyNotificationTimeDialog;
                if (standbyNotificationTimeDialog != null && standbyNotificationTimeDialog.isShowing()) {
                    standbyNotificationTimeDialog.dismiss();
                }
                MainActivity.this.standbyNotificationTimeDialog = null;
            }
        });
    }

    private final Runnable batteryAndChargingDataToViewRunnable = new Runnable() {
        @SuppressLint("SetTextI18n")
        @Override
        public void run() {
            if (isOnStop()) {
                return;
            }

            ActivityMainBinding binding = getBinding();
//            if (binding == null) {
//                return;
//            }

            final UartService uartService = getUartService();
            final UartReceiveBaseData data = uartService != null ? uartService.getNowUartReceiveBaseData() : null;

            @Nullable final Integer batteryRsoc = data != null ? data.getBatteryRsoc() : 0;

            final UartBatModeType batModeType = data != null ? data.getUartBatModeType() : null;

            if (batteryRsoc != null && batModeType != null) {
                if(batteryRsoc >= 0) {
                    binding.tvBattery.setText(batteryRsoc + "%");
                }else{
                    binding.tvBattery.setText("0%");
                }

                if (batteryRsoc <= 0) {
                    binding.ivBattery.setImageResource(R.drawable.battery_0);
                } else if (batteryRsoc <= 10) {
                    binding.ivBattery.setImageResource(R.drawable.battery_10);
                } else if (batteryRsoc <= 20) {
                    binding.ivBattery.setImageResource(R.drawable.battery_20);
                } else if (batteryRsoc <= 50) {
                    binding.ivBattery.setImageResource(R.drawable.battery_50);
                } else if (batteryRsoc <= 95) {
                    binding.ivBattery.setImageResource(R.drawable.battery_95);
                } else {
                    binding.ivBattery.setImageResource(R.drawable.battery_100);
                }
                if(batLevel!=batteryRsoc){
                    batLevel=batteryRsoc;
                    Log.d(TAG, "[batteryAndChargingDataToViewRunnable] BatteryRsoc="+batLevel);
                }

                if (batModeType == UartBatModeType.BMODE_CHARGING_POWER_KEY_PRESS ||
                        batModeType == UartBatModeType.BMODE_CHARGING_POWER_KEY_RELEASE ||
                        batModeType == UartBatModeType.BMODE_FULL_POWER_KEY_PRESS ||
                        batModeType == UartBatModeType.BMODE_FULL_POWER_KEY_RELEASE) {
                    if (batteryRsoc <= 10) {
                        binding.ivCharging.setImageResource(R.drawable.battery_red_charging);
                    } else if (batteryRsoc <= 20) {
                        binding.ivCharging.setImageResource(R.drawable.battery_orange_charging);
                    } else {
                        binding.ivCharging.setImageResource(R.drawable.battery_green_charging);
                    }
                    if(isCharging==false){
                        isCharging=true;
                        Log.d(TAG,"[batteryAndChargingDataToViewRunnable] Charging status: "+isCharging+", batModeType="+batModeType.name());
                    }
                } else {
                    binding.ivCharging.setImageBitmap(null);
                    if(isCharging==true){
                        isCharging=false;
                        Log.d(TAG,"[batteryAndChargingDataToViewRunnable] Charging status: "+isCharging+", batModeType="+batModeType.name());
                    }
                }
            }
            binding.getRoot().removeCallbacks(this);
            binding.getRoot().postDelayed(this, 1000L);
        }
    };

    public void showLoginFragment(@Nullable final Runnable loginRunnable) {
        Runnable r = new Runnable() {
            @Override
            public void run() {
                FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
                fragmentTransaction.setReorderingAllowed(true);

                LoginFragment loginFragment = getLoginFragment() != null ? getLoginFragment() : LoginFragment.newInstance(loginRunnable);

                if (loginFragment.isAdded()) {
                    return;
                }

                setTabBtnClickable(false);

                fragmentTransaction.add(R.id.container, loginFragment, LoginFragment.class.getSimpleName());

                if (!getCameraFragment().isHidden()) {
                    fragmentTransaction.hide(getCameraFragment());
                }
                if (!getDataManagementFragment().isHidden()) {
                    fragmentTransaction.hide(getDataManagementFragment());
                }
                if (!getSettingsFragment().isHidden()) {
                    fragmentTransaction.hide(getSettingsFragment());
                }
                if (!getMemberFragment().isHidden()) {
                    fragmentTransaction.hide(getMemberFragment());
                }
                if (engineeringFragment!=null && !getEngineeringFragment().isHidden()) {
                    fragmentTransaction.hide(getEngineeringFragment());
                }


                if (getSupportFragmentManager().isStateSaved()) {
                    fragmentTransaction.commitAllowingStateLoss();
                } else {
                    fragmentTransaction.commit();
                }

                setLoginFragment(loginFragment);
            }
        };

        if (Looper.getMainLooper() == Looper.myLooper()) {
            r.run();
        } else {
            runOnUiThread(r);
        }
    }

    /**
     * 刪除 未 輸入 patientId 時的暫時資料
     */
    public void deleteTempFile() {
        MyApplication myApplication = getMyApplication();
        try {
            File copyFileDirFile = new File(myApplication.getMainDirPath());
            File[] imgFileArr = copyFileDirFile.listFiles(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    return name.contains(MyApplication.IMG_FILE_EXTENSION);
                }
            });

            if (imgFileArr != null) {
                for (File imgFile : imgFileArr) {
                    try {
                        if(!imgFile.delete()){
                            Timber.w("[deleteTempFile] delete file fail:"+imgFile.getAbsolutePath());
                        }
                    } catch (Exception e) {
                        Timber.e(e);
                    }
                }
            }
        } catch (Exception e) {
            Timber.e(e);
        }
    }


    public synchronized void checkShowLowPowerDialog() {
        UartService uartService = getUartService();
        Integer nowBatteryRsoc = null;
        try {
            UartReceiveBaseData nowUartReceiveBaseData = (uartService != null) ? uartService.getNowUartReceiveBaseData() : null;
            nowBatteryRsoc = (nowUartReceiveBaseData != null) ? nowUartReceiveBaseData.getBatteryRsoc() : null;
        } catch (Exception e) {
            Timber.e(e);
        }

        Integer oldBatteryRsoc = null;
        try {
            UartReceiveBaseData oldUartReceiveBaseData = (uartService != null) ? uartService.getOldUartReceiveBaseData() : null;
            oldBatteryRsoc = (oldUartReceiveBaseData != null) ? oldUartReceiveBaseData.getBatteryRsoc() : null;
        } catch (Exception e) {
            Timber.e(e);
        }

        if (nowBatteryRsoc != null && oldBatteryRsoc != null) {
            if (oldBatteryRsoc > nowBatteryRsoc) {
                if (nowBatteryRsoc == 20) {
                    cancelLowPowerDialog();
                    showLowPowerDialog(nowBatteryRsoc);
                } else if (nowBatteryRsoc == 10) {
                    cancelLowPowerDialog();
                    showLowPowerDialog(nowBatteryRsoc);
                }
            }
        }
    }

    /**
     * 顯示 sleep mode不支援充電狀態視窗
     */
    @AnyThread
    public synchronized void showSleepWithNoChargingDialog() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                final MyDialog sleepNoChargingDialog = MainActivity.this.sleepNoChargingDialog;
                if (sleepNoChargingDialog != null && sleepNoChargingDialog.isShowing()) {
                    return;
                }
                MyDialog d = new MyDialog(MainActivity.this, false);
                d.setMsg(getString(R.string.sleep_uncharged_msg));
                d.showCancel(false);
                d.setConfirmText(getString(R.string.ok));
                d.show();
                MainActivity.this.sleepNoChargingDialog = d;
            }
        });
    }

    /**
     * 顯示 低電量小視窗
     */
    @AnyThread
    public synchronized void showLowPowerDialog(int battery) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                final MyDialog lowPowerDialog = MainActivity.this.lowPowerDialog;
                if (lowPowerDialog != null && lowPowerDialog.isShowing()) {
                    return;
                }
                MyDialog d = new MyDialog(MainActivity.this, false);
                d.setTitle(getString(R.string.warning));
                d.setMsg(getString(R.string.xx_battery_remaining, String.valueOf(battery)));
                d.showCancel(false);
                d.setConfirmText(getString(R.string.ok));
                d.show();
                MainActivity.this.lowPowerDialog = d;
            }
        });
    }

    /**
     * 關閉 低電量小視窗
     */
    @AnyThread
    public synchronized void cancelLowPowerDialog() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                final MyDialog lowPowerDialog = MainActivity.this.lowPowerDialog;
                if (lowPowerDialog != null && lowPowerDialog.isShowing()) {
                    lowPowerDialog.dismiss();
                }
                MainActivity.this.lowPowerDialog = null;
            }
        });
    }

    public void checkUserIcon() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                MyApplication myApplication = getMyApplication();
                UserTbData loginUserTbData = myApplication.getLoginUserTbData();
                ActivityMainBinding binding = getBinding();
                if (binding != null) {
                    if (loginUserTbData == null) {
                        binding.user.setImageResource(R.drawable.user_not_logging);
                    } else {
                        binding.user.setImageResource(R.drawable.user_login);
                    }
                }
            }
        });
    }

    private void startAutoDeleteFileWithDayOut() {
        final ScheduledFuture<?> autoDeleteFileWithDayOutJob = getAutoDeleteFileWithDayOutJob();
        if (autoDeleteFileWithDayOutJob != null) {
            try {
                autoDeleteFileWithDayOutJob.cancel(true);
            } catch (Exception e) {
                Timber.e(e);
            }
        }

        try {
            setAutoDeleteFileWithDayOutJob(scheduledExecutorService.scheduleAtFixedRate(
                    new Runnable() {
                        @Override
                        public void run() {
                            // 自動刪除N天前檔案(設置選項1天,7天,30天)
                            // 協程背景線程中
                            Timber.d("AutoDeleteFileWithDayOut Start");
                            if (!isDestroyed()) {
                                Timber.d("AutoDeleteFileWithDayOut Run");
                                final MyApplication myApplication = getMyApplication();
                                @NonNull final BootAutoDeleteFileType bootAutoDeleteFileType = SharedPreferencesManager.getInstance().getBootAutoDeleteFileType();

                                File mainDirFile = new File(myApplication.getMainDirPath());

                                List<File> childDirFileList = null;
                                try {
                                    File[] listFiles = mainDirFile.listFiles();
                                    if (listFiles == null) {
                                        listFiles = new File[]{};
                                    }
                                    childDirFileList = Arrays.asList(listFiles);
                                } catch (Exception e) {
                                    Timber.e(e);
                                }
                                if (childDirFileList == null) {
                                    childDirFileList = new ArrayList<>();
                                }

                                LocalDateTime nowLocalDateTime = LocalDateTime.now();
                                for (File childDirFile : childDirFileList) {

                                    if (childDirFile.getName().contains(MyRoomDatabase.DBNAME)) {
                                        continue;
                                    }

                                    LocalDateTime date = null;
                                    try {
                                        BasicFileAttributes basicFileAttributes = Files.readAttributes(
                                                childDirFile.toPath(),
                                                BasicFileAttributes.class
                                        );
                                        date = basicFileAttributes.creationTime().toInstant()
                                                .atZone(ZoneId.systemDefault()).toLocalDateTime();
                                    } catch (Exception e) {
                                        Timber.e(e);
                                    }

                                    long diff = 0;
                                    if (date != null) {
                                        diff = ChronoUnit.SECONDS.between(date, nowLocalDateTime);
                                    }

                                    Timber.d("diff=" + diff);

                                    if (date == null || diff >= (bootAutoDeleteFileType.getDay() * 24L * 60L * 60L)) {
                                        try {
                                            String childDirFilePath = childDirFile.getPath();
                                            FileUtils.deleteDirectory(childDirFile);
                                            @Nullable ProcedureFolderTbData procedureFolderTbData = myApplication.getMyRoomDatabase().procedureFolderTbDataDao()
                                                    .find(childDirFilePath);
                                            if (procedureFolderTbData != null) {
                                                myApplication.getMyRoomDatabase().procedureFolderTbDataDao()
                                                        .delete(procedureFolderTbData);
                                            }
                                        } catch (Exception e) {
                                            Timber.e(e);
                                        }
                                    }
                                }

                                Timber.d("AutoDeleteFileWithDayOut End");
                            }
                            Timber.d("AutoDeleteFileWithDayOut stop");
                        }
                    },
                    0L,
                    1L,
                    TimeUnit.HOURS
            ));
        } catch (Exception e) {
            Timber.e(e);
        }
    }

    private void stopAutoDeleteFileWithDayOut() {
        final ScheduledFuture<?> autoDeleteFileWithDayOutJob = getAutoDeleteFileWithDayOutJob();
        if (autoDeleteFileWithDayOutJob != null) {
            try {
                autoDeleteFileWithDayOutJob.cancel(true);
                setAutoDeleteFileWithDayOutJob(null);
            } catch (Exception e) {
                Timber.e(e);
            }
        }
    }

    public void setTabBtnClickable(boolean isClickable) {
        ActivityMainBinding binding = getBinding();
//        if (binding == null) {
//            return;
//        }
        binding.ibLiveView.setClickable(isClickable);
        binding.changeCamera.setClickable(isClickable);
        binding.ibDataManagement.setClickable(isClickable);
        binding.ibSettings.setClickable(isClickable);
        binding.user.setClickable(isClickable);
        binding.engineering.setClickable(isClickable);

        Runnable r = new Runnable() {
            @Override
            public void run() {
                if(isClickable && binding.changeCamera.isEnabled()){
                    binding.changeCameraText.setTextColor(getColor(R.color.gray_e0e0e0));
                    binding.changeCameraImg.setImageResource(R.drawable.icon_change_camera_02);
                }else {
                    binding.changeCameraText.setTextColor(getColor(R.color.gray_9a9ca2));
                    binding.changeCameraImg.setImageResource(R.drawable.icon_change_camera_01);
                }
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

    public void setTabBtnSelected(@Nullable AppCompatImageView tabBtnView) {
        final ActivityMainBinding binding = getBinding();
//        if (binding == null) {
//            return;
//        }
        binding.getRoot().post(new Runnable() {
            @Override
            public void run() {
                final ActivityMainBinding binding = getBinding();
//                if (binding == null) {
//                    return;
//                }
                binding.ibLiveView.setSelected(tabBtnView == binding.ibLiveView);
                binding.ibDataManagement.setSelected(tabBtnView == binding.ibDataManagement);
                binding.ibSettings.setSelected(tabBtnView == binding.ibSettings);
                binding.user.setSelected(tabBtnView == binding.user);
                binding.engineering.setSelected(tabBtnView == binding.engineering);
            }
        });
    }

    /**
     * 檢查 低電量動作(5% 直接關機)
     */
    private void checkRunBattery5Shutdown() {
        try {
            scheduledExecutorService.execute(new Runnable() {
                @Override
                public void run() {
                    final UartService uartService = getUartService();
                    final UartReceiveBaseData nowUartReceiveBaseData = uartService != null ? uartService.getNowUartReceiveBaseData() : null;
                    if (nowUartReceiveBaseData != null) {
                        @Nullable Integer batteryRsoc = nowUartReceiveBaseData.getBatteryRsoc();
                        @Nullable final UartBatModeType batModeType = nowUartReceiveBaseData.getUartBatModeType();
                        if(testShowError1000!=Integer.MIN_VALUE){
                            batteryRsoc=testShowError1000;
                        }

                        if (batModeType == UartBatModeType.BMODE_IDLE_POWER_KEY_RELEASE
                                || batModeType == UartBatModeType.BMODE_CHARGING_POWER_KEY_RELEASE
                                || batModeType == UartBatModeType.BMODE_FULL_POWER_KEY_RELEASE
                                || batModeType == UartBatModeType.BMODE_DISCHARGING_POWER_KEY_RELEASE) {
                            Log.w(TAG, "[Power] POWER_KEY Press");
                        }
                        
                        if (batModeType == UartBatModeType.BMODE_CHARGING_POWER_KEY_PRESS
                                || batModeType == UartBatModeType.BMODE_CHARGING_POWER_KEY_RELEASE
                                || batModeType == UartBatModeType.BMODE_FULL_POWER_KEY_PRESS
                                || batModeType == UartBatModeType.BMODE_FULL_POWER_KEY_RELEASE) {
                            return;
                        }
                        if (batteryRsoc == null || batteryRsoc > 5) {
                            return;
                        }else if(Error.BATTERY_CANNOT_BE_CHARGED.enable &&
                                batteryRsoc!=null &&
                                batteryRsoc > 100 || batteryRsoc<0){
                            IErrorCode.showErrorCode(MainActivity.this, Error.BATTERY_CANNOT_BE_CHARGED);
                            Error.BATTERY_CANNOT_BE_CHARGED.enable=false;
                            Log.w(TAG, String.format("%s: battery Rsoc= %d",  Error.BATTERY_CANNOT_BE_CHARGED.getCode(), batteryRsoc));
                        }

                        showBattery5ShutdownDialogDialog();
                    }
                }
            });
        } catch (Exception e) {
            Timber.e(e);
        }
    }

    /**
     * 顯示 Battery5ShutdownDialog 小視窗
     */
    @AnyThread
    private synchronized void showBattery5ShutdownDialogDialog() {
        final Battery5ShutdownDialog battery5ShutdownDialog = getBattery5ShutdownDialog();
        if (battery5ShutdownDialog != null && battery5ShutdownDialog.isShowing()) {
            return;
        }

        runOnUiThread(() -> {
            if (isOnStop()) {
                return;
            }

            final Battery5ShutdownDialog dialog = new Battery5ShutdownDialog(MainActivity.this) {
                @Override
                public boolean dispatchTouchEvent(MotionEvent ev) {
                    restartStandbyNotificationTimeServiceTimer();
                    return super.dispatchTouchEvent(ev);
                }
            };

            dialog.setListener(new Battery5ShutdownDialog.Listener() {
                @Override
                public void OnClickShutdown() {
                    if(cameraFragment != null &&
                            cameraFragment.getICameraHelper() != null &&
                            cameraFragment.getICameraHelper().isRecording()) {

                        cameraFragment.toggleVideoRecord(false);
                        Log.w(TAG,"Battery low to shutdown, Force recording to stop");
                        try {
                            Thread.sleep(500);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }

                    reSendShutdownCount = 0;
                    final ActivityMainBinding binding = getBinding();
                    if (binding != null) {
                        binding.getRoot().removeCallbacks(timeoutReSendShutdownRunnable);
                    }
                    timeoutReSendShutdownRunnable.run();
                }

                @Override
                public void OnClickCancel() {
                    // Handle cancel action here
                }
            });

            if (getBattery5ShutdownDialog() != null && getBattery5ShutdownDialog().isShowing()) {
                return;
            }

            dialog.show();
            setBattery5ShutdownDialog(dialog);
        });
    }

    /**
     * 關閉 Battery5ShutdownDialog 小視窗
     */
    @AnyThread
    private synchronized void cancelBattery5ShutdownDialogDialog() {
        runOnUiThread(() -> {
            final Battery5ShutdownDialog battery5ShutdownDialog = getBattery5ShutdownDialog();
            if (battery5ShutdownDialog != null && battery5ShutdownDialog.isShowing()) {
                battery5ShutdownDialog.dismiss();
            }
            setBattery5ShutdownDialog(null);
        });
    }

    /**
     * 檢查內存記憶不足
     */
    @WorkerThread
    @NonNull
    public DiskInsufficiencyType checkDiskInsufficiency() {
        double diskSize = 32.0;
        double freeDisk = 0.0;

        try {
            diskSize = com.blankj.utilcode.util.FileUtils.getFsTotalSize(Environment.getExternalStorageDirectory().getAbsolutePath()) / 1024.0 / 1024.0 / 1024.0;
        } catch (Exception e) {
            Timber.e(e);
        }

        try {
            freeDisk = com.blankj.utilcode.util.FileUtils.getFsAvailableSize(Environment.getExternalStorageDirectory().getAbsolutePath()) / 1024.0 / 1024.0 / 1024.0;
        } catch (Exception e) {
            Timber.e(e);
        }

        double usePercent = freeDisk / diskSize * 100;
        if(testShowError5000!=Integer.MIN_VALUE){
            usePercent=testShowError5000;
        }

        DiskInsufficiencyType diskInsufficiencyType;

        if (usePercent <= 20) {
            diskInsufficiencyType = DiskInsufficiencyType.PERCENT_20;
            Log.i(TAG, "checkDiskInsufficiency usePercent=" + usePercent);
        } else if (usePercent <= 30) {
            diskInsufficiencyType = DiskInsufficiencyType.PERCENT_30;
            Log.i(TAG, "checkDiskInsufficiency usePercent=" + usePercent);
        } else {
            diskInsufficiencyType = DiskInsufficiencyType.NORMAL;
            Timber.d("checkDiskInsufficiency usePercent=" + usePercent);
        }

        return diskInsufficiencyType;
    }


    /**
     * 檢查內存記憶不足 並刪檔
     */
    @WorkerThread
    public synchronized DiskInsufficiencyType checkDiskInsufficiencyWithDeleteOldFolder(@Nullable Runnable okRunnable, @Nullable Runnable cancelRunnable) {
        Timber.d("checkDiskInsufficiencyWithDeleteOldFolder");
        final DiskInsufficiencyType diskInsufficiencyType = checkDiskInsufficiency();
        final MyApplication myApplication = getMyApplication();
        final UserTbData loginUserTbData = myApplication.getLoginUserTbData();
        switch (diskInsufficiencyType) {
            case PERCENT_30:
                //showDiskInsufficiency30PercentDialog(cancelRunnable);
                if(Error.OUT_OF_STORAGE_MEMORY.enable){
                    IErrorCode.showErrorCode(MainActivity.this, Error.OUT_OF_STORAGE_MEMORY);
                    Error.OUT_OF_STORAGE_MEMORY.enable=false;
                }
                //若是EDS200需修改成顯示10秒後消失
                break;
            case PERCENT_20:
                @NonNull UserRoleType roleType = UserRoleType.GUEST;
                if(loginUserTbData != null){
                    roleType=loginUserTbData.getRoleType();
                }
                switch (roleType) {
                    case GUEST:
                        showDiskInsufficiencyDialog();
                        break;
                    case ADVANCED_USER:
                    case ADMIN_USER:
                    case SERVICE_USER:
                        List<ProcedureFolderTbData> procedureFolderTbDataList = null;
                        try {
                            if (loginUserTbData != null) {
                                procedureFolderTbDataList = myApplication.getMyRoomDatabase().procedureFolderTbDataDao().findDataList(loginUserTbData.getAccount());
                            }
                        } catch (Exception e) {
                            Timber.e(e);
                        }
                        if (procedureFolderTbDataList == null) {
                            procedureFolderTbDataList = new ArrayList<>();
                        }
                        showDiskInsufficiency20PercentDialog(procedureFolderTbDataList.isEmpty(), okRunnable, cancelRunnable);
                        break;
                }
                break;
            case NORMAL:
                break;
        }

        return diskInsufficiencyType;
    }

    /**
     * 顯示 空間不足Dialog
     */
    @AnyThread
    public synchronized void showDiskInsufficiencyDialog() {
        final MyDialog diskInsufficiencyDialog = getDiskInsufficiencyDialog();
        if (diskInsufficiencyDialog != null && diskInsufficiencyDialog.isShowing()) {
            return;
        }

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (isOnStop()) {
                    return;
                }

                final MyDialog diskInsufficiencyDialog = getDiskInsufficiencyDialog();
                MyDialog d = new MyDialog(MainActivity.this, false) {
                    @Override
                    public boolean dispatchTouchEvent(MotionEvent ev) {
                        restartStandbyNotificationTimeServiceTimer();
                        return super.dispatchTouchEvent(ev);
                    }
                };
                d.setTitle(getString(R.string.warning));
                d.setMsg(getString(R.string.disk_space_not_enough));
                d.setConfirmText(getString(R.string.ok));
                d.showCancel(false);
                if (diskInsufficiencyDialog != null && diskInsufficiencyDialog.isShowing()) {
                    return;
                }
                d.show();
                setDiskInsufficiencyDialog(d);
            }
        });
    }

    /**
     * 顯示 空間不足30％Dialog
     */
    @AnyThread
    public synchronized void showDiskInsufficiency30PercentDialog(@Nullable final Runnable cancelRunnable) {

        final MyDialog diskInsufficiency30PercentDialog = getDiskInsufficiency30PercentDialog();
        if (diskInsufficiency30PercentDialog != null && diskInsufficiency30PercentDialog.isShowing()) {
            return;
        }

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (isOnStop()) {
                    return;
                }
                MyDialog d = new MyDialog(MainActivity.this, false) {
                    @Override
                    public boolean dispatchTouchEvent(MotionEvent ev) {
                        restartStandbyNotificationTimeServiceTimer();
                        return super.dispatchTouchEvent(ev);
                    }
                };
                d.setTitle(getString(R.string.warning));
                d.setMsg(getString(R.string.disk_insufficiency_30_msg));
                d.setConfirmText(getString(R.string.yes));
                d.setCancelText(getString(R.string.no));
                d.setListener(new MyDialog.Listener() {
                    @Override
                    public void OnClickConfirm() {
                        ActivityMainBinding binding = getBinding();
                        if (binding != null) {
                            binding.ibDataManagement.performClick();
                        }
                    }

                    @Override
                    public void OnClickCancel() {
                        if (cancelRunnable != null) {
                            cancelRunnable.run();
                        }
                    }
                });

                final MyDialog diskInsufficiency30PercentDialog = getDiskInsufficiency30PercentDialog();
                if (diskInsufficiency30PercentDialog != null && diskInsufficiency30PercentDialog.isShowing()) {
                    return;
                }
                d.show();
                setDiskInsufficiency30PercentDialog(d);
            }
        });
    }

    /**
     * 顯示 空間不足20％Dialog
     */
    @AnyThread
    public synchronized void showDiskInsufficiency20PercentDialog(
            final boolean isFileEmpty,
            @Nullable final Runnable okRunnable,
            @Nullable final Runnable cancelRunnable) {

        MyDialog diskInsufficiency20PercentDialog = getDiskInsufficiency20PercentDialog();
        if (diskInsufficiency20PercentDialog != null && diskInsufficiency20PercentDialog.isShowing()) {
            return;
        }

        runOnUiThread(new Runnable() {
            @Override
            public void run() {

                if (isOnStop()) {
                    return;
                }

                MyDialog d = new MyDialog(MainActivity.this, false) {
                    @Override
                    public boolean dispatchTouchEvent(MotionEvent ev) {
                        restartStandbyNotificationTimeServiceTimer();
                        return super.dispatchTouchEvent(ev);
                    }
                };
                d.setTitle(getString(R.string.warning));
                d.setMsg(getString(R.string.disk_insufficiency_20_msg));
                d.setConfirmText(getString(R.string.yes));
                d.setCancelText(getString(R.string.no));
                d.setListener(new MyDialog.Listener() {
                    @Override
                    public void OnClickConfirm() {
                        if (isFileEmpty) {
                            showDiskInsufficiencyDialog();
                        } else {
                            try {
                                scheduledExecutorService.execute(new Runnable() {
                                    @Override
                                    public void run() {
                                        final MyApplication myApplication = getMyApplication();
                                        final UserTbData loginUserTbData = myApplication.getLoginUserTbData();
                                        if (loginUserTbData == null) {
                                            return;
                                        }

                                        UserRoleType roleType = loginUserTbData.getRoleType();
                                        if (roleType == null) {
                                            roleType = UserRoleType.GUEST;
                                        }

                                        switch (roleType) {
                                            case GUEST:
                                                break;
                                            case ADVANCED_USER: {
                                                showLoadDialog();
                                                List<ProcedureFolderTbData> procedureFolderTbDataList = null;
                                                try {
                                                    procedureFolderTbDataList = myApplication.getMyRoomDatabase().procedureFolderTbDataDao().findDataList(loginUserTbData.getAccount());
                                                    if (procedureFolderTbDataList == null) {
                                                        procedureFolderTbDataList = new ArrayList<>();
                                                    }
                                                    procedureFolderTbDataList.sort( new Comparator<ProcedureFolderTbData>() {
                                                        @Override
                                                        public int compare(ProcedureFolderTbData o1, ProcedureFolderTbData o2) {
                                                            try {
                                                                LocalDate date1 = o1.getCreateDate();
                                                                LocalDate date2 = o2.getCreateDate();
                                                                if (date1 != null && date2 != null) {
                                                                    return date1.compareTo(date2);
                                                                }
                                                            } catch (Exception e) {
                                                                Timber.e(e);
                                                            }
                                                            return 0;
                                                        }
                                                    });
                                                } catch (Exception e) {
                                                    Timber.e(e);
                                                    if (procedureFolderTbDataList == null) {
                                                        procedureFolderTbDataList = new ArrayList<>();
                                                    }
                                                }

                                                for (ProcedureFolderTbData procedureFolderTbData : procedureFolderTbDataList) {
                                                    try {
                                                        File dirFile = new File(procedureFolderTbData.getFilePath());
                                                        if (dirFile.exists()) {

                                                            FileUtils.deleteDirectory(dirFile);
                                                        }
                                                    } catch (Exception e) {
                                                        Timber.e(e);
                                                    }

                                                    try {
                                                        myApplication.getMyRoomDatabase().procedureFolderTbDataDao().delete(procedureFolderTbData);
                                                    } catch (Exception e) {
                                                        Timber.e(e);
                                                    }

                                                    DiskInsufficiencyType diskInsufficiencyType = checkDiskInsufficiency();

                                                    if (diskInsufficiencyType == DiskInsufficiencyType.NORMAL) {
                                                        break;
                                                    }
                                                }

                                                DiskInsufficiencyType diskInsufficiencyType = checkDiskInsufficiency();

                                                cancelLoadDialog();

                                                if (diskInsufficiencyType == DiskInsufficiencyType.PERCENT_20 && procedureFolderTbDataList.isEmpty()) {
                                                    runOnUiThread(new Runnable() {
                                                        @Override
                                                        public void run() {
                                                            showDiskInsufficiencyDialog();
                                                        }
                                                    });
                                                } else {
                                                    runOnUiThread(new Runnable() {
                                                        @Override
                                                        public void run() {
                                                            if (okRunnable != null) {
                                                                okRunnable.run();
                                                            }
                                                        }
                                                    });
                                                }
                                            }
                                            break;
                                            case ADMIN_USER:
                                            case SERVICE_USER:
                                                showLoadDialog();
                                                List<ProcedureFolderTbData> procedureFolderTbDataList = null;
                                                try {
                                                    procedureFolderTbDataList = myApplication.getMyRoomDatabase().procedureFolderTbDataDao().getAll();
                                                    if (procedureFolderTbDataList == null) {
                                                        procedureFolderTbDataList = new ArrayList<>();
                                                    }
                                                    procedureFolderTbDataList.sort( new Comparator<ProcedureFolderTbData>() {
                                                        @Override
                                                        public int compare(ProcedureFolderTbData o1, ProcedureFolderTbData o2) {
                                                            try {
                                                                LocalDate date1 = o1.getCreateDate();
                                                                LocalDate date2 = o2.getCreateDate();
                                                                if (date1 != null && date2 != null) {
                                                                    return date1.compareTo(date2);
                                                                }
                                                            } catch (Exception e) {
                                                                Timber.e(e);
                                                            }
                                                            return 0;
                                                        }
                                                    });
                                                } catch (Exception e) {
                                                    Timber.e(e);
                                                    if (procedureFolderTbDataList == null) {
                                                        procedureFolderTbDataList = new ArrayList<>();
                                                    }
                                                }

                                                for (ProcedureFolderTbData procedureFolderTbData : procedureFolderTbDataList) {
                                                    try {
                                                        File dirFile = new File(procedureFolderTbData.getFilePath());
                                                        if (dirFile.exists()) {
                                                            FileUtils.deleteDirectory(dirFile);
                                                        }
                                                    } catch (Exception e) {
                                                        Timber.e(e);
                                                    }

                                                    try {
                                                        myApplication.getMyRoomDatabase().procedureFolderTbDataDao().delete(procedureFolderTbData);
                                                    } catch (Exception e) {
                                                        Timber.e(e);
                                                    }

                                                    DiskInsufficiencyType diskInsufficiencyType = checkDiskInsufficiency();

                                                    if (diskInsufficiencyType == DiskInsufficiencyType.NORMAL) {
                                                        break;
                                                    }
                                                }

                                                DiskInsufficiencyType diskInsufficiencyType = checkDiskInsufficiency();

                                                cancelLoadDialog();

                                                if (diskInsufficiencyType == DiskInsufficiencyType.PERCENT_20 && procedureFolderTbDataList.isEmpty()) {
                                                    runOnUiThread(new Runnable() {
                                                        @Override
                                                        public void run() {
                                                            showDiskInsufficiencyDialog();
                                                        }
                                                    });
                                                } else {
                                                    runOnUiThread(new Runnable() {
                                                        @Override
                                                        public void run() {
                                                            if (okRunnable != null) {
                                                                okRunnable.run();
                                                            }
                                                        }
                                                    });
                                                }
                                                break;
                                        }
                                    }
                                });
                            } catch (Exception e) {
                                Timber.e(e);
                            }
                        }
                    }

                    @Override
                    public void OnClickCancel() {
                        if (cancelRunnable != null) {
                            cancelRunnable.run();
                        }
                    }
                });


                MyDialog diskInsufficiency20PercentDialog = getDiskInsufficiency20PercentDialog();
                if (diskInsufficiency20PercentDialog != null && diskInsufficiency20PercentDialog.isShowing()) {
                    return;
                }

                d.show();
                setDiskInsufficiency20PercentDialog(d);
            }
        });
    }

    /**
     * 提醒正在錄影 Dialog
     */
    @AnyThread
    public synchronized void showRecordingReminderDialog() {

        MyDialog recordingReminderDialog = getRecordingReminderDialog();
        if (recordingReminderDialog != null && recordingReminderDialog.isShowing()) {
            return;
        }

        runOnUiThread(new Runnable() {
            @Override
            public void run() {

                if (isOnStop()) {
                    return;
                }

                MyDialog d = new MyDialog(MainActivity.this, false) {
                    @Override
                    public boolean dispatchTouchEvent(MotionEvent ev) {
                        restartStandbyNotificationTimeServiceTimer();
                        return super.dispatchTouchEvent(ev);
                    }
                };
                d.setMsg(getString(R.string.msg_stop_recording));
                d.showConfirm(true);
                d.setConfirmText(getString(R.string.yes));
                d.showCancel(true);
                d.setCancelText(getString(R.string.no));
                d.setListener(new MyDialog.Listener() {
                    @Override
                    public void OnClickConfirm() {
                        //中斷錄影，顯示關機畫面
                        if(cameraFragment!=null){
                            cameraFragment.toggleVideoRecord(false);
                        }
                        cancelStandbyNotificationTimeDialog();
                        showShutDownDialog();
                    }

                    @Override
                    public void OnClickCancel() {
                        //關閉視窗,繼續錄影
                    }
                });


                MyDialog recordingReminderDialog = getRecordingReminderDialog();
                if (recordingReminderDialog != null && recordingReminderDialog.isShowing()) {
                    return;
                }

                d.show();
                setRecordingReminderDialog(d);
            }
        });
    }


    /**
     * 送關機命令次數
     * -1 沒有
     */
    private int reSendShutdownCount = -1;

    private final Runnable timeoutReSendShutdownRunnable = new Runnable() {
        @Override
        public void run() {
            if (reSendShutdownCount == -1) {
                return;
            }

            ActivityMainBinding binding = getBinding();

            if (binding != null) {
                binding.getRoot().removeCallbacks(this);
            }

            try {
                scheduledExecutorService.execute(new Runnable() {
                    @Override
                    public void run() {

                        getMyApplication().getMyRoomDatabase().close();

                        if (isDestroyed()) {
                            return;
                        }
                        UartService uartService = getUartService();
                        if (uartService != null) {
                            uartService.sendUartCmd(UartSendCmd.SHUTDOWN);
                        }
                    }
                });
            } catch (Exception e) {
                Timber.e(e);
            }


            reSendShutdownCount++;
            if (reSendShutdownCount > 30) {
                return;
            }

            if (binding != null) {
                binding.getRoot().removeCallbacks(this);
            }

            if (isDestroyed()) {
                return;
            }
            if (binding != null) {
                binding.getRoot().postDelayed(this, 1000L);
            }
        }
    };

    private int reSendIdleCount = -1;

    private final Runnable timeoutReSendIdleRunnable = new Runnable() {
        @Override
        public void run() {
            if (reSendIdleCount == -1) {
                return;
            }

            ActivityMainBinding binding = getBinding();

            if (binding != null) {
                binding.getRoot().removeCallbacks(this);
            }

            try {
                scheduledExecutorService.execute(new Runnable() {
                    @Override
                    public void run() {
                        if (isOnStop()) {
                            return;
                        }
                        UartService uartService = getUartService();
                        if (uartService != null) {
                            uartService.sendUartCmd(UartSendCmd.IDLE);
                        }
                    }
                });
            } catch (Exception e) {
                Timber.e(e);
            }

            reSendIdleCount++;
            if (reSendIdleCount > 30) {
                return;
            }
            if (binding != null) {
                binding.getRoot().removeCallbacks(this);
            }
            if (isOnStop()) {
                return;
            }
            if (binding != null) {
                binding.getRoot().postDelayed(this, 1000L);
            }
        }
    };


    @NonNull
    public ActivityMainBinding getBinding() {
        return binding;
    }

    @Nullable
    public UartService getUartService() {
        return uartService;
    }

    public synchronized boolean isOnStop() {
        synchronized (this) {
            return isOnStop;
        }
    }

    public synchronized void setOnStop(boolean onStop) {
        synchronized (this) {
            isOnStop = onStop;
        }
    }

    public long getOnStartTimeMillis() {
        return onStartTimeMillis;
    }

    public void setOnStartTimeMillis(long onStartTimeMillis) {
        this.onStartTimeMillis = onStartTimeMillis;
    }

    @NonNull
    public CameraFragment getCameraFragment() {
        return cameraFragment;
    }

    @NonNull
    public DataManagementFragment getDataManagementFragment() {
        return dataManagementFragment;
    }

    @NonNull
    public SettingsFragment getSettingsFragment() {
        return settingsFragment;
    }

    @NonNull
    public MemberFragment getMemberFragment() {
        return memberFragment;
    }

    @Nullable
    public synchronized LoginFragment getLoginFragment() {
        return loginFragment;
    }

    public synchronized void setLoginFragment(@Nullable LoginFragment loginFragment) {
        this.loginFragment = loginFragment;
    }

    @NonNull
    public EngineeringFragment getEngineeringFragment() {
        if(engineeringFragment==null){
            engineeringFragment= EngineeringFragment.newInstance();
        }
        return engineeringFragment;
    }

    @Nullable
    public synchronized ScheduledFuture<?> getAutoDeleteFileWithDayOutJob() {
        synchronized (autoDeleteFileWithDayOutJobLock) {
            return autoDeleteFileWithDayOutJob;
        }
    }

    public synchronized void setAutoDeleteFileWithDayOutJob(@Nullable ScheduledFuture<?> autoDeleteFileWithDayOutJob) {
        synchronized (autoDeleteFileWithDayOutJobLock) {
            this.autoDeleteFileWithDayOutJob = autoDeleteFileWithDayOutJob;
        }
    }

    @Nullable
    public synchronized Battery5ShutdownDialog getBattery5ShutdownDialog() {
        synchronized (battery5ShutdownDialogLock) {
            return battery5ShutdownDialog;
        }
    }

    public synchronized void setBattery5ShutdownDialog(@Nullable Battery5ShutdownDialog battery5ShutdownDialog) {
        synchronized (battery5ShutdownDialogLock) {
            this.battery5ShutdownDialog = battery5ShutdownDialog;
        }
    }

    @Nullable
    public synchronized MyDialog getDiskInsufficiencyDialog() {
        synchronized (diskInsufficiencyDialogLock) {
            return diskInsufficiencyDialog;
        }
    }

    public synchronized void setDiskInsufficiencyDialog(@Nullable MyDialog diskInsufficiencyDialog) {
        synchronized (diskInsufficiencyDialogLock) {
            this.diskInsufficiencyDialog = diskInsufficiencyDialog;
        }
    }

    @Nullable
    public synchronized MyDialog getDiskInsufficiency30PercentDialog() {
        synchronized (diskInsufficiency30PercentDialogLock) {
            return diskInsufficiency30PercentDialog;
        }
    }

    public synchronized void setDiskInsufficiency30PercentDialog(@Nullable MyDialog diskInsufficiency30PercentDialog) {
        synchronized (diskInsufficiency30PercentDialogLock) {
            this.diskInsufficiency30PercentDialog = diskInsufficiency30PercentDialog;
        }
    }

    @Nullable
    public synchronized MyDialog getDiskInsufficiency20PercentDialog() {
        synchronized (diskInsufficiency20PercentDialogLock) {
            return diskInsufficiency20PercentDialog;
        }
    }

    public synchronized void setDiskInsufficiency20PercentDialog(@Nullable MyDialog diskInsufficiency20PercentDialog) {
        synchronized (diskInsufficiency20PercentDialogLock) {
            this.diskInsufficiency20PercentDialog = diskInsufficiency20PercentDialog;
        }
    }

    @Nullable
    public synchronized MyDialog getRecordingReminderDialog() {
        synchronized (recordingReminderDialogLock) {
            return recordingReminderDialog;
        }
    }

    public synchronized void setRecordingReminderDialog(@Nullable MyDialog recordingReminderDialog) {
        synchronized (recordingReminderDialogLock) {
            this.recordingReminderDialog = recordingReminderDialog;
        }
    }


    private final CountDownTimer myRoomDatabaseReOpenCountDownTimer = new CountDownTimer(3 * 1000L , 1000) {
        @Override
        public void onTick(long millisUntilFinished) {
            Timber.d("myRoomDatabaseReOpenCountDownTimer onTick millisUntilFinished=" + millisUntilFinished / 1000);
        }

        @Override
        public void onFinish() {
            myRoomDatabaseReOpenCountDownTimer.cancel();
            Timber.d("myRoomDatabaseReOpenCountDownTimer onFinish");

            getMyApplication().getMyRoomDatabase().backupRoomFile();

            getMyApplication().getMyRoomDatabase();
        }
    };

    /**
     * 檢查ProcedureFolderTbData有沒有實際檔案
     */
    @AnyThread
    public void checkProcedureFolderTbDataHasFile() {
        scheduledExecutorService.execute(new Runnable() {
            @Override
            public void run() {
                List<ProcedureFolderTbData> allList;
                try {
                    allList = getMyApplication().getMyRoomDatabase().procedureFolderTbDataDao().getAll();
                } catch (Exception e) {
                    Timber.e(e);
                    allList = new ArrayList<>();
                }
                if (allList == null) {
                    allList = new ArrayList<>();
                }

                List<ProcedureFolderTbData> removeList = new ArrayList<>();

                for (ProcedureFolderTbData d : allList) {
                    try {
                        File dirFile = new File(d.getFilePath());

                        if (!dirFile.exists()) {
                            removeList.add(d);
                            continue;
                        }

                        File[] fileArr = dirFile.listFiles((dir, name) -> {
                            //Timber.d("checkProcedureFolderTbDataHasFile name=%s", name);
                            boolean f = name.contains(MyApplication.IMG_FILE_EXTENSION) || name.contains(MyApplication.VIDEO_FILE_EXTENSION);
                            //Timber.d("checkProcedureFolderTbDataHasFile f=%s", f);
                            return f;
                        });

                        if (fileArr == null || fileArr.length == 0) {
                            removeList.add(d);
                        }
                    } catch (Exception e) {
                        Timber.e(e);
                    }
                }

                try {
                    getMyApplication().getMyRoomDatabase().procedureFolderTbDataDao().deletes(removeList);
                } catch (Exception e) {
                    Timber.e(e);
                }
            }
        });

        scheduledExecutorService.execute(new Runnable() {
            @Override
            public void run() {
                // 檢查有實際檔案但沒有 ProcedureFolderTbData
                MyApplication myApplication = getMyApplication();
                File mainDirFile = new File(myApplication.getMainDirPath());
                try {
                    if (!mainDirFile.exists()) {
                        return;
                    }
                } catch (Exception e) {
                    Timber.e(e);
                }

                File[] childDirFileList = null;
                try {
                    childDirFileList = mainDirFile.listFiles(File::isDirectory);
                } catch (Exception e) {
                    Timber.e(e);
                }

                if (childDirFileList == null) {
                    childDirFileList = new File[]{};
                }

                for (File childDirFile : childDirFileList) {
                    try {
                        if (!childDirFile.exists()) {
                            continue;
                        }
                    } catch (Exception e) {
                        Timber.e(e);
                    }

                    String[] childDirNameList = childDirFile.getName().split("_");
                    if (childDirNameList.length < 3) {
                        continue;
                    }

                    boolean isHasData;
                    try {
                        isHasData = myApplication.getMyRoomDatabase().procedureFolderTbDataDao().find(childDirFile.getPath()) != null;
                    } catch (Exception e) {
                        Timber.e(e);
                        isHasData = false;
                    }

                    if (isHasData) {
                        continue;
                    }

                    ProcedureFolderTbData procedureFolderTbData = new ProcedureFolderTbData(
                            UUID.randomUUID().toString(),
                            childDirFile.getPath(),
                            childDirNameList[0],
                            childDirNameList[1]
                    );

                    try {
                        LocalDate createDate = LocalDate.parse(
                                childDirNameList[2],
                                DateTimeFormatter.ofPattern(CameraFragment.DIR_NAME_DATE_FORMAT_PATTERN, Locale.ENGLISH)
                        );
                        procedureFolderTbData.setCreateDate(createDate);
                    } catch (Exception e) {
                        Timber.e(e);
                        continue;
                    }

                    try {
                        getMyApplication().getMyRoomDatabase().procedureFolderTbDataDao().insert(procedureFolderTbData);
                    } catch (Exception e) {
                        Timber.e(e);
                    }

                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            MainActivity mainActivity = myApplication.getMainActivity();
                            DataManagementFragment dataManagementFragment = null;
                            if (mainActivity != null) {
                                dataManagementFragment = mainActivity.getDataManagementFragment();
                            }
                            if (dataManagementFragment != null) {
                                dataManagementFragment.updateFileList();
                            }
                        }
                    });
                }
            }
        });
    }

    @NonNull
    public ScheduledExecutorService getScheduledExecutorService() {
        return scheduledExecutorService;
    }

    public void setRoomBackupFileSelectDialog(@Nullable RoomBackupFileSelectDialog roomBackupFileSelectDialog) {
        synchronized (roomBackupFileSelectDialogLock) {
            this.roomBackupFileSelectDialog = roomBackupFileSelectDialog;
        }
    }

    @Nullable
    public RoomBackupFileSelectDialog getRoomBackupFileSelectDialog() {
        synchronized (roomBackupFileSelectDialogLock) {
            return roomBackupFileSelectDialog;
        }
    }

    /**
     * 顯示 選擇 room db 備份檔
     */
    @AnyThread
    public synchronized void showRoomBackupFileSelectDialog() {
        final RoomBackupFileSelectDialog roomBackupFileSelectDialog = getRoomBackupFileSelectDialog();
        if (roomBackupFileSelectDialog != null && roomBackupFileSelectDialog.isShowing()) {
            return;
        }

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (isOnStop()) {
                    return;
                }

                final RoomBackupFileSelectDialog roomBackupFileSelectDialog = getRoomBackupFileSelectDialog();
                RoomBackupFileSelectDialog d = new RoomBackupFileSelectDialog(MainActivity.this) {
                    @Override
                    public boolean dispatchTouchEvent(MotionEvent ev) {
                        restartStandbyNotificationTimeServiceTimer();
                        return super.dispatchTouchEvent(ev);
                    }
                };
                d.setListener(new RoomBackupFileSelectDialog.Listener() {
                    @Override
                    public void onClickConfirm(int position, @NonNull RoomBackupFileSelectDialogAdapterData data) {
                        getMyApplication().getMyRoomDatabase().restoreRoom(data.getFile());
                    }

                    @Override
                    public void onClickCancel() {

                    }

                    @Override
                    public void onTouch() {

                    }
                });
                d.setCancelable(false);
                if (roomBackupFileSelectDialog != null && roomBackupFileSelectDialog.isShowing()) {
                    return;
                }

                d.show();
                setRoomBackupFileSelectDialog(d);
            }
        });
    }

    private final CountDownTimer clickTextTimeCountDownTimer = new CountDownTimer(3 * 1000L, 1000L) {
        @Override
        public void onTick(long millisUntilFinished) {
            Timber.d("clickTextTimeCountDownTimer -> onTick" + millisUntilFinished / 1000L);
        }

        @Override
        public void onFinish() {
            Timber.d("clickTextTimeCountDownTimer -> onFinish");

            clickTextTimeCountDownTimer.cancel();

            clickTextTimeCount = 0;
        }
    };

    /**
     * 获取系统默认屏幕亮度值 屏幕亮度值范围（0-255）
     * **/
    private int getScreenBrightness(Context context) {
        ContentResolver contentResolver = context.getContentResolver();
        int defVal = 125;
        return Settings.System.getInt(contentResolver,
                Settings.System.SCREEN_BRIGHTNESS, defVal);
    }

    /**
     * 关闭光感，设置手动调节背光模式
     *
     * SCREEN_BRIGHTNESS_MODE_AUTOMATIC 自动调节屏幕亮度模式值为1
     *
     * SCREEN_BRIGHTNESS_MODE_MANUAL 手动调节屏幕亮度模式值为0
     * **/
    public void setScreenManualMode(Context context) {
        ContentResolver contentResolver = context.getContentResolver();
        try {
            int mode = Settings.System.getInt(contentResolver,
                    Settings.System.SCREEN_BRIGHTNESS_MODE);
            if (mode == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC) {
                Settings.System.putInt(contentResolver,
                        Settings.System.SCREEN_BRIGHTNESS_MODE,
                        Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
            }
        } catch (Settings.SettingNotFoundException e) {
            Timber.w("Exception: "+ e.getMessage());
        }
    }

    /**
     * 修改Setting 中屏幕亮度值
     *
     * 修改Setting的值需要动态申请权限 <uses-permission
     * android:name="android.permission.WRITE_SETTINGS"/>
     * **/
    private void ModifySettingsScreenBrightness(Context context,
                                                int birghtessValue) {
        // 首先需要设置为手动调节屏幕亮度模式
        setScreenManualMode(context);

        ContentResolver contentResolver = context.getContentResolver();
        try {
            Settings.System.putInt(contentResolver,
                    Settings.System.SCREEN_BRIGHTNESS, birghtessValue);
        }catch (Exception e){
            Timber.e("[ModifySettingsScreenBrightness]"+e.getMessage());
        }

    }

    public void setUsbDeviceChToView(int ch) {
        Runnable r = new Runnable() {
            @Override
            public void run() {
                ActivityMainBinding binding = getBinding();

                switch (ch){
                    case 1:
                        binding.changeCamera.setSelected(false);
                        binding.changeCameraText.setSelected(false);
                        binding.changeCameraImg.setSelected(false);
                        binding.changeCameraText.setText(getString(R.string.ch_number, 1));
                        break;

                    case 2:
                        binding.changeCamera.setSelected(true);
                        binding.changeCameraText.setSelected(true);
                        binding.changeCameraImg.setSelected(true);
                        binding.changeCameraText.setText(getString(R.string.ch_number, 2));
                        break;

                    default:
                        binding.changeCamera.setSelected(false);
                        binding.changeCameraText.setSelected(false);
                        binding.changeCameraImg.setSelected(false);
                        binding.changeCameraText.setText("");
                        break;
                }
            }
        };
        if (Looper.myLooper() == Looper.getMainLooper()) {
            r.run();
        } else {
            ActivityMainBinding binding = getBinding();
            if (binding != null) {
                binding.getRoot().post(r);
            }
        }
    }

    public void setChangeCameraEnable(final boolean isEnable) {
        Runnable r = new Runnable() {
            @Override
            public void run() {
                ActivityMainBinding binding = getBinding();
                if (binding != null) {
                    binding.changeCamera.setEnabled(isEnable);
                }

                if(binding.changeCamera.isClickable() && isEnable){
                    Timber.d("[setChangeCameraEnable] Olive debug1");
                    binding.changeCameraText.setTextColor(getColor(R.color.gray_e0e0e0));
                    binding.changeCameraImg.setImageResource(R.drawable.icon_change_camera_02);
                }else {
                    Timber.d("[setChangeCameraEnable] Olive debug2");
                    binding.changeCameraText.setTextColor(getColor(R.color.gray_9a9ca2));
                    binding.changeCameraImg.setImageResource(R.drawable.icon_change_camera_01);
                }
            }
        };
        if (Looper.myLooper() == Looper.getMainLooper()) {
            r.run();
        } else {
            ActivityMainBinding binding = getBinding();
            if (binding != null) {
                binding.getRoot().post(r);
            }
        }
    }

    private int testShowError1000=Integer.MIN_VALUE;
    @VisibleForTesting
    public void setTestShowError1000(int value) {
        testShowError1000=value;
        IErrorCode.removeErrorCodeView(MainActivity.this, Error.BATTERY_CANNOT_BE_CHARGED);
        Error.BATTERY_CANNOT_BE_CHARGED.enable=true;
    }

    private int testShowError5000=Integer.MIN_VALUE;
    @VisibleForTesting
    public void setTestShowError5000(int value) {
        testShowError5000=value;
        checkDiskInsufficiencyWithDeleteOldFolder(null, null);
    }
}
