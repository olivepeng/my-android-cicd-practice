package com.miis.horusendoview.fragment;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.SurfaceTexture;
import android.hardware.usb.UsbDevice;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.widget.Toast;

import androidx.annotation.FloatRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.annotation.UiThread;
import androidx.annotation.VisibleForTesting;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.core.content.ContextCompat;
import androidx.exifinterface.media.ExifInterface;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import com.herohan.uvcapp.CameraHelper;
import com.herohan.uvcapp.ICameraHelper;
import com.herohan.uvcapp.ImageCapture;
import com.herohan.uvcapp.VideoCapture;
import com.libuvccamera.usb.Size;
import com.libuvccamera.usb.UVCControl;
import com.miis.horusendoview.MyApplication;
import com.miis.horusendoview.R;
import com.miis.horusendoview.activity.MainActivity;
import com.miis.horusendoview.databinding.ActivityMainBinding;
import com.miis.horusendoview.databinding.FragmentCameraBinding;
import com.miis.horusendoview.dialog.HistoryImageListDialog;
import com.miis.horusendoview.dialog.MyDialog;
import com.miis.horusendoview.dialog.PatientIdDialog;
import com.miis.horusendoview.errorcode.Error;
import com.miis.horusendoview.errorcode.IErrorCode;
import com.miis.horusendoview.log.LogQueue;
import com.miis.horusendoview.manager.MyStorageManager;
import com.miis.horusendoview.manager.SharedPreferencesManager;
import com.miis.horusendoview.manager.SystemPropertiesUnit;
import com.miis.horusendoview.roomDataBase.procedureFolder.ProcedureFolderTbData;
import com.miis.horusendoview.roomDataBase.userTbData.UserTbData;
import com.miis.horusendoview.type.DiskInsufficiencyType;
import com.miis.horusendoview.type.ImageRotateType;
import com.miis.horusendoview.type.MaximumVideoDurationType;
import com.miis.horusendoview.type.UserRoleType;
import com.miis.horusendoview.widget.CameraSettingsView;
import com.miis.horusendoview.widget.PreviewWindowsView;

import java.io.File;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import io.reactivex.rxjava3.core.Observer;
import io.reactivex.rxjava3.disposables.Disposable;
import timber.log.Timber;

public final class CameraFragment extends Fragment implements View.OnClickListener {

    private final String TAG = CameraFragment.class.getSimpleName();

    private final String NAME_ISP_CH1 = "Endoscope";
    private final String NAME_ISP_CH2 = "Endoscope2";

    /**
     * Maximum time allowed to wait for the result of an image capture
     */
    private static final long IMAGE_CAPTURE_TIMEOUT_MILLIS = 5000L;

    /**
     * Maximum time allowed to wait for the result of open camera
     */
    private static final long OPEN_CAMERA_TIMEOUT_MILLIS = 10 * 1000L;

    /**
     * Maximum time allowed to wait for the result of switch channels
     */
    private static final long SWITCH_CHANNEL_TIMEOUT_MILLIS = OPEN_CAMERA_TIMEOUT_MILLIS + 3000L;

    /**
     * 照片檔案命名時間格式
     */
    @NonNull
    public static final String IMG_SIMPLE_DATE_FORMAT_PATTERN = "yyyy-MM-dd_HH-mm-ss-SSS";

    /**
     * 影片檔案命名時間格式
     */
    @NonNull
    public static final String VIDEO_SIMPLE_DATE_FORMAT_PATTERN = "yyyy-MM-dd_HH-mm-ss-SSS";

    /**
     * 資料夾的日期格式
     * DateTimeFormatter
     */
    @NonNull
    public static final String DIR_NAME_DATE_FORMAT_PATTERN = "yyyyMMdd";

    private final ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(5);

    @Nullable
    private FragmentCameraBinding binding;

    @NonNull
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Nullable
    private UsbDevice usbDeviceCh1;
    @NonNull
    private final Object usbDeviceCh1Lock = new Object();

    @Nullable
    private UsbDevice usbDeviceCh2;
    @NonNull
    private final Object usbDeviceCh2Lock = new Object();


    @Nullable
    private UsbDevice usbDeviceNow;
    @NonNull
    private final Object usbDeviceNowLock = new Object();


    private boolean isTakePhotoRunning;
    @NonNull
    private final Object isTakePhotoRunningLock = new Object();


    private int rotation;
    @NonNull
    private final Object rotationLock = new Object();


    private float zoom = 1.0F;
    @NonNull
    private final Object zoomLock = new Object();


    private boolean isStartRecording;
    @NonNull
    private final Object isStartRecordingLock = new Object();


    @Nullable
    private Long recordTimeSec;
    @NonNull
    private final Object recordTimeSecLock = new Object();


    @Nullable
    private Long recordMaxTimeSec;
    @NonNull
    private final Object recordMaxTimeSecLock = new Object();


    private ScheduledFuture<?> checkButtonHandle;
    private Integer btn1 = 0;
    private int BrightnessDEF, SharpnessDEF, ContrastDEF, WhiteBlanceDEF;
    private int BrightnessLevel = 5, SharpnessLevel = 5, ContrastLevel = 5, WhiteBlanceLevel = 5, LevelMin = 1, LevelMax = 9;
    private int Sharpnessval = 7, Colorval = 4600, Contrastval = 4, Brightval = 0;

    private long recordTimeSecUptimeMillis;
    private boolean isOnCreated;
    private boolean isOnStart;
    @Nullable
    private MyDialog myDialog;
    private long recordOnClickTimeMillis;
    @Nullable
    private HistoryImageListDialog historyImageListDialog;
    private boolean mIsCameraConnected;
    @Nullable
    private ICameraHelper iCameraHelper;
    private boolean mIsRecording;
    @Nullable
    private PatientIdDialog patientIdDialog;

    private boolean mIsChangeCameraing;

    //for 顯示內視鏡剩餘使用時間
    private final boolean FLAG_ENDOSCOPE_USABLE_TIME =  false;
    private ScheduledFuture<?> endoscopyRemainingTimeSchedule;

    private boolean flag_CameraOpen=false;
    private boolean flag_JustShow_E2000; //work around:當切到camera fragment的時候，無論是否有live view frame,都會先跑一次onSurfaceTextureUpdated

    @NonNull
    public static CameraFragment newInstance() {
        return new CameraFragment();
    }

    @Nullable
    public FragmentCameraBinding getBinding() {
        return this.binding;
    }

    @Nullable
    private UsbDevice getUsbDeviceCh1() {
        synchronized (usbDeviceCh1Lock) {
            return this.usbDeviceCh1;
        }
    }

    private void setUsbDeviceCh1(@Nullable UsbDevice value) {
        synchronized (usbDeviceCh1Lock) {
            this.usbDeviceCh1 = value;
        }
    }

    @Nullable
    private UsbDevice getUsbDeviceCh2() {
        synchronized (usbDeviceCh2Lock) {
            return this.usbDeviceCh2;
        }
    }

    private void setUsbDeviceCh2(@Nullable UsbDevice value) {
        synchronized (usbDeviceCh2Lock) {
            this.usbDeviceCh2 = value;
        }
    }

    @Nullable
    private UsbDevice getUsbDeviceNow() {
        synchronized (usbDeviceNowLock) {
            return this.usbDeviceNow;
        }
    }

    private void setUsbDeviceNow(@Nullable UsbDevice value) {
        synchronized (usbDeviceNowLock) {
            this.usbDeviceNow = value;
            setUsbDeviceChToView();
        }
    }

    public boolean isTakePhotoRunning() {
        synchronized (isTakePhotoRunningLock) {
            return this.isTakePhotoRunning;
        }
    }

    public void setTakePhotoRunning(boolean takePhotoRunning) {
        synchronized (isTakePhotoRunningLock) {
            this.isTakePhotoRunning = takePhotoRunning;
        }
    }

    private int getRotation() {
        synchronized (rotationLock) {
            return this.rotation;
        }
    }

    private void setRotation(int rotation) {
        synchronized (rotationLock) {
            this.rotation = rotation;
            final Runnable r = () -> {
                final ICameraHelper iCameraHelper = getICameraHelper();
                final FragmentCameraBinding binding = getBinding();
                switch (rotation) {
                    case Surface.ROTATION_0:
                        if (binding != null) {
                            binding.btnRotateText.setText("");
                            binding.btnRotate.setSelected(false);
                            binding.btnRotateImg.setSelected(false);
                        }
                        if (iCameraHelper != null) {
                            iCameraHelper.setPreviewConfig(iCameraHelper.getPreviewConfig().setRotation(0));
                        }
                        break;
                    case Surface.ROTATION_90:
                        if (binding != null) {
                            binding.btnRotateText.setText(R.string._90_angle);
                            binding.btnRotate.setSelected(true);
                            binding.btnRotateImg.setSelected(true);
                        }
                        if (iCameraHelper != null) {
                            iCameraHelper.setPreviewConfig(iCameraHelper.getPreviewConfig().setRotation(90));
                        }
                        break;
                    case Surface.ROTATION_180:
                        if (binding != null) {
                            binding.btnRotateText.setText(R.string._180_angle);
                            binding.btnRotate.setSelected(true);
                            binding.btnRotateImg.setSelected(true);
                        }
                        if (iCameraHelper != null) {
                            iCameraHelper.setPreviewConfig(iCameraHelper.getPreviewConfig().setRotation(180));
                        }
                        break;
                    case Surface.ROTATION_270:
                        if (binding != null) {
                            binding.btnRotateText.setText(R.string._270_angle);
                            binding.btnRotate.setSelected(true);
                            binding.btnRotateImg.setSelected(true);
                        }
                        if (iCameraHelper != null) {
                            iCameraHelper.setPreviewConfig(iCameraHelper.getPreviewConfig().setRotation(270));
                        }
                        break;
                }
            };
            if (Looper.getMainLooper() == Looper.myLooper()) {
                r.run();
            } else {
                FragmentCameraBinding binding = getBinding();
                if (binding != null) {
                    binding.cameraView.post(r);
                }
            }
        }
    }

    @FloatRange(
            from = 1.0
    )
    public float getZoom() {
        synchronized (zoomLock) {
            return this.zoom;
        }
    }

    private void setZoom(@FloatRange(from = 1.0) float zoom) {
        synchronized (zoomLock) {
            this.zoom = zoom;
            Runnable r = () -> {
                final FragmentCameraBinding binding = getBinding();
                if (binding == null) {
                    return;
                }

                binding.cameraView.configureTextureViewTransform(zoom);

                if (zoom <= 1.0f) {
                    binding.btnZoom.setSelected(false);
                    binding.btnZoom.setImageResource(R.drawable.zoom_in);
                } else {
                    binding.btnZoom.setSelected(true);
                    binding.btnZoom.setImageResource(R.drawable.zoom_out);
                }
            };

            if (Looper.getMainLooper() == Looper.myLooper()) {
                r.run();
            } else {
                final FragmentCameraBinding binding = getBinding();
                if (binding != null) {
                    binding.cameraView.post(r);
                }
            }
        }
    }

    public boolean isStartRecording() {
        synchronized (isStartRecordingLock) {
            return this.isStartRecording;
        }
    }

    public void setStartRecording(boolean startRecording) {
        synchronized (isStartRecordingLock) {
            this.isStartRecording = startRecording;
        }
    }

    @Nullable
    public Long getRecordTimeSec() {
        synchronized (recordTimeSecLock) {
            return recordTimeSec;
        }
    }

    public void setRecordTimeSec(@Nullable Long recordTimeSec) {
        synchronized (recordTimeSecLock) {
            this.recordTimeSec = recordTimeSec;
        }
    }

    @Nullable
    public Long getRecordMaxTimeSec() {
        synchronized (recordMaxTimeSecLock) {
            return recordMaxTimeSec;
        }
    }

    public void setRecordMaxTimeSec(@Nullable Long recordMaxTimeSec) {
        synchronized (recordMaxTimeSecLock) {
            this.recordMaxTimeSec = recordMaxTimeSec;
        }
    }

    @Nullable
    public ICameraHelper getICameraHelper() {
        return this.iCameraHelper;
    }

    @Nullable
    public PatientIdDialog getPatientIdDialog() {
        return this.patientIdDialog;
    }

    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (scheduledExecutorService instanceof ScheduledThreadPoolExecutor) {
            ScheduledThreadPoolExecutor s = (ScheduledThreadPoolExecutor) scheduledExecutorService;
            s.setRemoveOnCancelPolicy(true);
        }
    }

    @NonNull
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if (binding == null) {
            binding = FragmentCameraBinding.inflate(inflater, container, false);
        }
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        isOnCreated = true;
        final FragmentCameraBinding binding = getBinding();
        if (binding != null) {
            binding.record.setOnClickListener(this);
            binding.snapshot.setOnClickListener(this);
            binding.btnRotate.setOnClickListener(this);
            binding.btnZoom.setOnClickListener(this);
            binding.btnAdjust.setOnClickListener(this);
            binding.btnCompare.setOnClickListener(this);
            //binding.changeCamera.setOnClickListener(this);
            binding.patientIdLayout.setOnClickListener(this);
            binding.cameraView.setAspectRatio(1f);
            binding.cameraView.setSurfaceTextureListener(cameraViewSurfaceTextureListener);
            binding.cameraView.getViewTreeObserver().addOnGlobalLayoutListener(cameraViewOnGlobalLayoutListener);

            binding.previewWindowsView.setListener(new PreviewWindowsView.Listener() {
                @Override
                public void onClickHistory() {
                    MyApplication myApplication = getMyApplication();
                    String patientId = null;
                    if (myApplication != null) {
                        patientId = myApplication.getPatientId();
                    }
                    if (patientId == null || patientId.isEmpty()) {
                        showPatientIdDialog(new Runnable() {
                            @Override
                            public void run() {
                                showHistoryImageListDialog();
                            }
                        }, null);
                        return;
                    }
                    showHistoryImageListDialog();
                }

                @Override
                public void onFull(boolean isFull) {
                    final FragmentCameraBinding binding = getBinding();
                    if (binding == null) {
                        return;
                    }
                    int previewWindowsViewWidth = binding.previewWindowsView.getMeasuredWidth();
                    int cameraViewWidth = binding.cameraView.getMeasuredWidth();
//                    int rightLayoutWidth = binding.rightLayout.getMeasuredWidth();
                    int camera_fragment_divider = getResources().getDimensionPixelSize(R.dimen.camera_fragment_divider);
                    int totalWidth = previewWindowsViewWidth + cameraViewWidth - camera_fragment_divider;
                    ConstraintSet constraintSet = new ConstraintSet();
                    constraintSet.clone(binding.mainLayout);
                    if (isFull) {
                        // previewWindowsView
                        constraintSet.constrainWidth(R.id.previewWindowsView, totalWidth / 2 + camera_fragment_divider);
                        constraintSet.constrainHeight(R.id.previewWindowsView, ConstraintLayout.LayoutParams.MATCH_PARENT);
                        constraintSet.connect(R.id.previewWindowsView, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP);
                        constraintSet.connect(R.id.previewWindowsView, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM);
                        constraintSet.connect(R.id.previewWindowsView, ConstraintSet.START, R.id.rightLayout, ConstraintSet.END);
                        constraintSet.connect(R.id.previewWindowsView, ConstraintSet.END, R.id.cameraView, ConstraintSet.START);

                        // cameraView
                        constraintSet.constrainWidth(R.id.cameraView, totalWidth / 2);
                        constraintSet.constrainHeight(R.id.cameraView, 0);
                        constraintSet.setDimensionRatio(R.id.cameraView, "1:1");
//                        constraintSet.connect(R.id.cameraView, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP);
//                        constraintSet.connect(R.id.cameraView, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM);
//                        constraintSet.connect(R.id.cameraView, ConstraintSet.START, R.id.previewWindowsView, ConstraintSet.END);
//                        constraintSet.connect(R.id.cameraView, ConstraintSet.END, R.id.rightLayout, ConstraintSet.START);

                        // rightLayout
//                        constraintSet.constrainWidth(R.id.rightLayout, rightLayoutWidth);
//                        constraintSet.constrainHeight(R.id.rightLayout, ConstraintSet.MATCH_CONSTRAINT);
//                        constraintSet.connect(R.id.rightLayout, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP);
//                        constraintSet.connect(R.id.rightLayout, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM);
//                        constraintSet.connect(R.id.rightLayout, ConstraintSet.START, R.id.cameraView, ConstraintSet.END);
//                        constraintSet.connect(R.id.rightLayout, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END);
                    } else {
                        // previewWindowsView
                        constraintSet.constrainWidth(R.id.previewWindowsView, ConstraintSet.MATCH_CONSTRAINT);
                        constraintSet.constrainHeight(R.id.previewWindowsView, ConstraintSet.MATCH_CONSTRAINT);
                        constraintSet.connect(R.id.previewWindowsView, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP);
                        constraintSet.connect(R.id.previewWindowsView, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM);
                        constraintSet.connect(R.id.previewWindowsView, ConstraintSet.START, R.id.rightLayout, ConstraintSet.END);
                        constraintSet.connect(R.id.previewWindowsView, ConstraintSet.END, R.id.cameraView, ConstraintSet.START);

                        // cameraView
                        constraintSet.constrainWidth(R.id.cameraView, ConstraintSet.MATCH_CONSTRAINT);
                        constraintSet.constrainHeight(R.id.cameraView, ConstraintSet.MATCH_CONSTRAINT);
                        constraintSet.setDimensionRatio(R.id.cameraView, "1:1");
//                        constraintSet.connect(R.id.cameraView, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP);
//                        constraintSet.connect(R.id.cameraView, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM);
//                        constraintSet.connect(R.id.cameraView, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START);
//                        constraintSet.connect(R.id.cameraView, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END);

                        // rightLayout
//                        constraintSet.constrainWidth(R.id.rightLayout, ConstraintSet.MATCH_CONSTRAINT);
//                        constraintSet.constrainHeight(R.id.rightLayout, ConstraintSet.MATCH_CONSTRAINT);
//                        constraintSet.connect(R.id.rightLayout, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP);
//                        constraintSet.connect(R.id.rightLayout, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM);
//                        constraintSet.connect(R.id.rightLayout, ConstraintSet.START, R.id.cameraView, ConstraintSet.END);
//                        constraintSet.connect(R.id.rightLayout, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END);
                    }

                    constraintSet.applyTo(binding.mainLayout);
                }

                @Override
                public void onClose() {
                    final FragmentCameraBinding binding = getBinding();
                    if (binding != null) {
                        binding.btnCompare.setVisibility(View.VISIBLE);
                    }
                }
            });

            setCameraSettingsView();
            setImageRotateTypeToRotation();
//            setIvTipsShow(true);
            setIvTipsShow(false);
            setRecordEnable(false);
            setSnapshotEnable(false);
            showPromptSnapshot(false);
            setLoginUserRoleTypeToView(null);
            setPatientIdToView();
            checkCameraHelper();

            if ((binding != null) ){
                binding.getRoot().postDelayed(() -> setIvTipsShow(true), 100);
            }
        }
    }


    public void onStart() {
        super.onStart();
        isOnStart = true;
    }

    public void onResume() {
        super.onResume();
        setMaximumVideoDurationToView();

        try {
            scheduledExecutorService.execute(new Runnable() {
                @Override
                public void run() {
                    setImageAdjustmentSettingToDevice();
                    setCameraSettingsView();
                }
            });
        } catch (Exception e) {
            Timber.e(e);
        }
        flag_JustShow_E2000=true;
    }

    public void onPause() {
        super.onPause();
        cancelMyDialog();
        cancelHistoryImageListDialog();
        cancelPatientIdDialog();
    }

    public void onStop() {
        super.onStop();
        isOnStart = false;
    }

    public void onDestroyView() {
        super.onDestroyView();
        isOnCreated = false;
        FragmentCameraBinding binding = getBinding();
        if (binding != null) {
            binding.cameraView.getViewTreeObserver().removeOnGlobalLayoutListener(
                    cameraViewOnGlobalLayoutListener
            );
        }

        clearCameraHelper();
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
        final ICameraHelper iCameraHelper = getICameraHelper();
        if (hidden) {
            cancelMyDialog();
            cancelHistoryImageListDialog();
            cancelPatientIdDialog();

            //在錄影中切換fragment
            if(iCameraHelper!=null && iCameraHelper.isRecording()){
                FragmentActivity activity = getActivity();
                if(!(activity instanceof MainActivity)){
                    return;
                }
                final MainActivity mainActivity=(MainActivity) activity;
                final ActivityMainBinding activityMainBinding = mainActivity.getBinding();
                final Runnable r = new Runnable() {
                    @Override
                    public void run() {
                        Context context = getContext();
                        if (context == null) {
                            return;
                        }
                        MyDialog d = new MyDialog(context, false) {
                            @Override
                            public boolean dispatchTouchEvent(MotionEvent ev) {
                                if (mainActivity != null) {
                                    mainActivity.restartStandbyNotificationTimeServiceTimer();
                                }
                                return super.dispatchTouchEvent(ev);
                            }
                        };
                        d.setCanceledOnTouchOutside(false);
                        d.setMsg(getString(R.string.msg_stop_recording));
                        d.showConfirm(true);
                        d.setConfirmText(getString(R.string.yes));
                        d.showCancel(true);
                        d.setCancelText(getString(R.string.no));
                        d.setListener(new MyDialog.Listener() {
                            @Override
                            public void OnClickConfirm() {
                                //中斷錄影，刷新data management
                                toggleVideoRecord(false);
                            }

                            @Override
                            public void OnClickCancel() {
                                //回到camera fragment,繼續錄影
                                scheduledExecutorService.execute(new Runnable() {
                                    @Override
                                    public void run() {
                                        mainActivity.onClick(activityMainBinding.ibLiveView);
                                    }
                                });
                            }
                        });
                        d.show();
                        myDialog = d;
                    }
                };
                if (Looper.myLooper() == Looper.getMainLooper()) {
                    r.run();
                } else {
                    if (activityMainBinding != null) {
                        activityMainBinding.getRoot().post(r);
                    }
                }
            }

            if(Error.NO_SIGNAL_WHEN_ENDOSCOPE_INSERTED.enable) {
                mainHandler.removeCallbacks(showE2000Runnable);
                //Timber.d("onHiddenChanged hidden NO_SIGNAL_WHEN_ENDOSCOPE_INSERTED remove2");
            }

        } else {
            FragmentActivity activity = getActivity();
            if (activity instanceof MainActivity) {
                MainActivity mainActivity = (MainActivity) activity;
                mainActivity.setTabBtnSelected(mainActivity.getBinding().ibLiveView);
                if(mIsChangeCameraing == false) {  //change camera not finished, can't switch page
                    FragmentCameraBinding binding = getBinding();
                    if (binding != null) {
                        binding.getRoot().postDelayed(() -> mainActivity.setTabBtnClickable(true), MainActivity.TAB_BTN_CLICKABLE_ON_DELAY);
                    }
                }
            }

            if (iCameraHelper!=null && iCameraHelper.isCameraOpened()) {
                flag_JustShow_E2000=true;
                mainHandler.postDelayed(showE2000Runnable, 1 * 1000L);
                Error.NO_SIGNAL_WHEN_ENDOSCOPE_INSERTED.enable = true;
            }
        }
    }


    @Override
    public void onClick(View v) {
        if (v == null) {
            return;
        }
        Timber.d("[onClick] "+ LogQueue.getId(v));
        switch (v.getId()) {
            case R.id.record: {
                Context context = getContext();
                if (context == null) {
                    return;
                }

                final ICameraHelper mCameraHelper = getICameraHelper();
                if (mCameraHelper == null || !mCameraHelper.isCameraOpened()) {
                    return;
                }

                if (mCameraHelper.isRecording()) {
                    toggleVideoRecord(false);
                    return;
                }

                MyApplication myApplication = getMyApplication();
                String patientId = null;
                if (myApplication != null) {
                    patientId = myApplication.getPatientId();
                }
                if (patientId == null) {
                    patientId = "";
                }
                if (patientId.isEmpty()) {
                    showPatientIdDialog(new Runnable() {
                        @Override
                        public void run() {
                            recordOnClickTimeMillis = 0;

                            final FragmentCameraBinding binding = getBinding();
                            if (binding != null) {
                                binding.record.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        binding.record.performClick();
                                    }
                                });
                            }
                        }
                    }, null);
                    return;
                }

                if (Math.abs(System.currentTimeMillis() - recordOnClickTimeMillis) < 1000) {
                    return;
                }

                recordOnClickTimeMillis = System.currentTimeMillis();

                try {
                    scheduledExecutorService.execute(new Runnable() {
                                                         @Override
                                                         public void run() {
                                                             FragmentActivity activity = getActivity();
                                                             DiskInsufficiencyType diskInsufficiencyType = DiskInsufficiencyType.NORMAL;
                                                             if (activity instanceof MainActivity) {
                                                                 diskInsufficiencyType = ((MainActivity) activity).checkDiskInsufficiencyWithDeleteOldFolder(new Runnable() {
                                                                     @Override
                                                                     public void run() {
                                                                         final FragmentCameraBinding binding = getBinding();
                                                                         if (binding == null) {
                                                                             return;
                                                                         }
                                                                         binding.record.post(new Runnable() {
                                                                             @Override
                                                                             public void run() {
                                                                                 binding.record.performClick();
                                                                             }
                                                                         });
                                                                     }
                                                                 }, new Runnable() {
                                                                     @Override
                                                                     public void run() {
                                                                         toggleVideoRecord(true);
                                                                     }
                                                                 });
                                                             } else {
                                                                 return;
                                                             }

                                                             if (diskInsufficiencyType != DiskInsufficiencyType.PERCENT_20) {
                                                                 final FragmentCameraBinding binding = getBinding();
                                                                 if (binding != null) {
                                                                     binding.getRoot().post(new Runnable() {
                                                                         @Override
                                                                         public void run() {
                                                                             toggleVideoRecord(true);
                                                                         }
                                                                     });
                                                                 }
                                                             }
                                                         }
                                                     }
                    );
                } catch (Exception e) {
                    Timber.e(e);
                }
            }
            break;
            case R.id.snapshot: {
                MyApplication myApplication = getMyApplication();
                String patientId = null;
                if (myApplication != null) {
                    patientId = myApplication.getPatientId();
                }
                if (patientId == null) {
                    patientId = "";
                }
                if (patientId.isEmpty()) {
                    showPatientIdDialog(new Runnable() {
                        @Override
                        public void run() {
                            recordOnClickTimeMillis = 0;

                            final FragmentCameraBinding binding = getBinding();
                            if (binding != null) {
                                binding.record.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        binding.snapshot.performClick();
                                    }
                                });
                            }
                        }
                    }, null);
                    return;
                }
                try {
                    scheduledExecutorService.execute(new Runnable() {
                        @Override
                        public void run() {
                            FragmentActivity activity = getActivity();
                            DiskInsufficiencyType diskInsufficiencyType = DiskInsufficiencyType.NORMAL;
                            if (activity instanceof MainActivity) {
                                diskInsufficiencyType = ((MainActivity) activity).checkDiskInsufficiencyWithDeleteOldFolder(new Runnable() {
                                    @Override
                                    public void run() {
                                        final FragmentCameraBinding binding = getBinding();
                                        if (binding != null) {
                                            binding.snapshot.post(new Runnable() {
                                                @Override
                                                public void run() {
                                                    binding.snapshot.performClick();
                                                }
                                            });
                                        }
                                    }
                                }, new Runnable() {
                                    @Override
                                    public void run() {
                                        takePhoto();
                                    }
                                });
                            }

                            if (diskInsufficiencyType != DiskInsufficiencyType.PERCENT_20) {
                                final FragmentCameraBinding binding = getBinding();
                                if (binding != null) {
                                    binding.getRoot().post(new Runnable() {
                                        @Override
                                        public void run() {
                                            takePhoto();
                                        }
                                    });
                                }
                            }
                        }
                    });
                } catch (Exception e) {
                    Timber.e(e);
                }
            }
                break;
            case R.id.btnRotate: {
                final ICameraHelper iCameraHelper = getICameraHelper();
                if (iCameraHelper != null && iCameraHelper.isCameraOpened()) {
                    nextRotation();
                }
            }
            break;
            case R.id.btnZoom: {
                final ICameraHelper iCameraHelper = getICameraHelper();
                if (iCameraHelper != null && iCameraHelper.isCameraOpened()) {
                    changeNextZoom();
                }
            }
            break;
            case R.id.btnAdjust: {
                final ICameraHelper iCameraHelper = getICameraHelper();
                if (iCameraHelper != null && iCameraHelper.isCameraOpened()) {
                    final FragmentCameraBinding binding = getBinding();
                    if (binding != null) {
                        boolean isShow = binding.cameraSettings.getVisibility() != View.VISIBLE;
                        if(binding.previewWindowsView.getVisibility()==View.VISIBLE){
                            if(isShow){
                                binding.previewWindowsView.setInfoShow(false);
                            }else {
                                binding.previewWindowsView.setInfoShow(true);
                            }
                        }
                        showCameraSettings(isShow);
                    }
                }
            }
            break;
            case R.id.btnCompare: {
                MyApplication myApplication = getMyApplication();
                if (myApplication == null) {
                    return;
                }
                UserTbData userTbData = myApplication.getLoginUserTbData();

                UserRoleType roleType = null;
                if (userTbData != null) {
                    roleType = userTbData.getRoleType();
                }

                if (roleType == null) {
                    roleType = UserRoleType.GUEST;
                }

                if (roleType == UserRoleType.GUEST) {
                    return;
                }

                final String comparePatientId = myApplication.getPatientId();
                if (comparePatientId == null || comparePatientId.isEmpty()) {
                    showPatientIdDialog(new Runnable() {
                        @Override
                        public void run() {
                            FragmentCameraBinding binding = getBinding();
                            if (binding != null) {
                                binding.btnCompare.performClick();
                            }
                        }
                    }, null);
                    return;
                }

                FragmentCameraBinding binding = getBinding();
                if (binding != null) {
                    if(binding.cameraSettings.getVisibility()==View.VISIBLE){
                        binding.previewWindowsView.setInfoShow(false);
                    }else {
                        binding.previewWindowsView.setInfoShow(true);
                    }
                    binding.previewWindowsView.setVisibility(View.VISIBLE);
                    binding.btnCompare.setVisibility(View.GONE);
                }
            }
            break;



            case R.id.patientIdLayout: {
                MyApplication myApplication = getMyApplication();
                if (myApplication == null) {
                    return;
                }
                UserTbData userTbData = myApplication.getLoginUserTbData();

                UserRoleType roleType = null;
                if (userTbData != null) {
                    roleType = userTbData.getRoleType();
                }

                if (roleType == null) {
                    roleType = UserRoleType.GUEST;
                }

                if (roleType == UserRoleType.GUEST) {
                    return;
                }

                if(iCameraHelper!=null && iCameraHelper.isRecording()){
                    Timber.w("It is forbidden to change the Patient ID during recording.");
                    return;
                }

                showPatientIdDialog(
                        new Runnable() {
                            @Override
                            public void run() {
                                final String patientId = myApplication.getPatientId();
                                if(patientId==null || patientId.isEmpty()){
                                    FragmentCameraBinding binding = getBinding();
                                    if (binding != null) {
                                        binding.patientIdLayout.performClick();
                                    }
                                }
                            }
                        }
                        , null);
            }
            break;
        }
    }


    @NonNull
    private final TextureView.SurfaceTextureListener cameraViewSurfaceTextureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            Timber.d("cameraViewSurfaceTextureListener -> onSurfaceTextureAvailable");
            final ICameraHelper iCameraHelper = getICameraHelper();
            if (iCameraHelper != null) {
                iCameraHelper.addSurface(surface, false);
            }
            FragmentCameraBinding binding = getBinding();
            if (binding != null) {
                binding.cameraView.configureTextureViewTransform(zoom);
            }
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
            Timber.d("cameraViewSurfaceTextureListener -> onSurfaceTextureSizeChanged");
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            Timber.d("cameraViewSurfaceTextureListener -> onSurfaceTextureDestroyed");
            final ICameraHelper iCameraHelper = getICameraHelper();
            if (iCameraHelper != null) {
                iCameraHelper.removeSurface(surface);
            }
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {
            // Timber.d("cameraViewSurfaceTextureListener -> onSurfaceTextureUpdated");
            if(flag_JustShow_E2000){
                flag_JustShow_E2000 = false;
                return;
            }

            if(!testShowError2000 &&
                    Error.NO_SIGNAL_WHEN_ENDOSCOPE_INSERTED.enable) {
                mainHandler.removeCallbacks(showE2000Runnable);
            }

            if(flag_CameraOpen ){ //Camera Open之後第一次更新frame
                Timber.d("cameraViewSurfaceTextureListener -> onSurfaceTextureUpdated, flag_CameraOpen=true");
                flag_CameraOpen = false;

                if(FLAG_ENDOSCOPE_USABLE_TIME) {
                    endoscopyRemainingTimeSchedule = scheduledExecutorService.schedule(endoscopyRemainingTimeRunnable, 0L, TimeUnit.MILLISECONDS);
                }

                if(checkButtonHandle==null || checkButtonHandle.isCancelled()) {
                    checkButtonHandle = scheduledExecutorService.scheduleAtFixedRate(checkButtonRunnable, 2000, 600, TimeUnit.MILLISECONDS);
                }

                mainHandler.removeCallbacks(changeCameraTimeout);
                mainHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        ICameraHelper iCameraHelper = getICameraHelper();
                        if (iCameraHelper != null && !iCameraHelper.isRecording()) {
                            setChangeCameraEnable(true);
                        }
                    }
                }, 2000);

                setRecordEnable(true);
                setSnapshotEnable(true);

                FragmentActivity activity = getActivity();
                if (activity instanceof MainActivity) {
                    MainActivity mainActivity = (MainActivity) activity;
                    mainActivity.setTabBtnClickable(true);
                }
                mIsChangeCameraing=false;
            }
        }
    };

    @NonNull
    private final ViewTreeObserver.OnGlobalLayoutListener cameraViewOnGlobalLayoutListener = new ViewTreeObserver.OnGlobalLayoutListener() {
        @Override
        public void onGlobalLayout() {
            FragmentCameraBinding binding = getBinding();
            if (binding != null) {
                binding.cameraView.configureTextureViewTransform(zoom);
            }
        }
    };

    public void checkCameraHelper() {
        if (!mIsCameraConnected) {
            clearCameraHelper();
        }
        initCameraHelper();
    }

    private void initCameraHelper() {
        Context context = getContext();
        if (context == null) {
            return;
        }

        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
        ) != PackageManager.PERMISSION_GRANTED) {
            Timber.w("selectDevice Manifest.permission.CAMERA ERR");
            return;
        }

        ICameraHelper iCameraHelper = getICameraHelper();
        if (iCameraHelper == null) {
            this.iCameraHelper = new CameraHelper();
            ICameraHelper iCameraHelper02 = getICameraHelper();
            if (iCameraHelper02 != null) {
                iCameraHelper02.setStateCallback(iCameraHelperStateCallback);
                setCustomImageCaptureConfig();
                setCustomVideoCaptureConfig();
            }
        }
    }


    private void setCustomImageCaptureConfig() {
        ICameraHelper iCameraHelper = getICameraHelper();
        if (iCameraHelper != null) {
            iCameraHelper.setImageCaptureConfig(
                    iCameraHelper.getImageCaptureConfig().setJpegCompressionQuality(100)
            );
        }
    }


    private void setCustomVideoCaptureConfig() {
        ICameraHelper iCameraHelper = getICameraHelper();
        if (iCameraHelper != null) {
            iCameraHelper.setVideoCaptureConfig(
                    iCameraHelper.getVideoCaptureConfig()
                            .setAudioCaptureEnable(false)
                            //.setBitRate(2000000)
                            .setVideoFrameRate(30)
                            .setIFrameInterval(1)
            );
        }
    }


    private void clearCameraHelper() {
        Timber.d("clearCameraHelper");
        ICameraHelper iCameraHelper = getICameraHelper();
        if (iCameraHelper != null && iCameraHelper.isRecording()) {
            toggleVideoRecord(false);
        }

        if (iCameraHelper != null && iCameraHelper.isCameraOpened()) {
            try {
                iCameraHelper.closeCamera();
            } catch (Exception e) {
                Timber.e(e);
            }
        }

        try {
            if (iCameraHelper != null) {
                iCameraHelper.release();
            }
        } catch (Exception e) {
            Timber.e(e);
        }

        this.iCameraHelper = null;
    }


    public void attachNewDevice(@NonNull UsbDevice device) {
        if (getUsbDeviceNow() == null) {
            setUsbDeviceNow(device);
            selectDevice(device);
        }
    }

    private void selectDevice(@NonNull UsbDevice device) {
        Timber.d("selectDevice:device=%s", device.getDeviceName());
        Context context = getContext();
        if (context == null) {
            return;
        }

        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
        ) != PackageManager.PERMISSION_GRANTED) {
            Timber.w("selectDevice Manifest.permission.CAMERA ERR");
            return;
        }

        mIsCameraConnected = false;

        ICameraHelper iCameraHelper = getICameraHelper();
        // 通過UsbDevice對象，嘗試獲取設備權限
        if (iCameraHelper != null) {
            iCameraHelper.selectDevice(device);
        }
    }

//    class CheckButtonTask extends TimerTask {
//        public void run() {
//            Message msg = new Message();
//            msg.what = 1;
//            handler.sendMessage(msg);
//        }
//    };

    private Runnable checkButtonRunnable = new Runnable() {
        @Override
        public void run() {
            btn1 = 0;
            int ret = -1;
            int clear = 0;
            if(iCameraHelper==null){
                Timber.w("iCameraHelper==null");
                return;
            }

            final UVCControl uvcControl = iCameraHelper.getUVCControl();
            if(uvcControl==null){
                Timber.w("uvcControl==null");
                return;
            }

            btn1 = uvcControl.getBacklightComp();

            if (btn1 == 0x21) {  // take picture
                //Log.d("Jerry", "takePhoto");
                uvcControl.setBacklightComp(0);
                btn1 = 0;
                //takePhoto();
                if (binding != null && binding.snapshot.isEnabled()) {
                    binding.snapshot.post(new Runnable() {
                        @Override
                        public void run() {
                            binding.snapshot.performClick();
                        }
                    });
                }
            }
            else if (btn1 == 0x22) { // record video
                //Log.d("Jerry", "record");
                uvcControl.setBacklightComp(0);
                btn1 = 0;
                if (binding != null && binding.snapshot.isEnabled()) {
                    binding.record.post(new Runnable() {
                        @Override
                        public void run() {
                            binding.record.performClick();
                        }
                    });
                }
            }
        }
    };

    @NonNull
    public final ICameraHelper.StateCallback iCameraHelperStateCallback = new ICameraHelper.StateCallback() {
        @Override
        public void onAttach(UsbDevice device) {
            final String productName = device.getProductName();
            Timber.d("iCameraHelperStateCallback -> onAttach ProductName=%s", productName);

            String prop_key="";
            Error errorcode=null;
            if (TextUtils.equals(productName, NAME_ISP_CH1)) {
                setUsbDeviceCh1(device);
                prop_key="persist.horusendoview.ch1";
                errorcode=Error.ENDOSCOPE_CONNECTOR_OVER_9000_USAGE_TIMES_CH1;
            } else if (TextUtils.equals(productName, NAME_ISP_CH2)) {
                setUsbDeviceCh2(device);
                prop_key="persist.horusendoview.ch2";
                errorcode=Error.ENDOSCOPE_CONNECTOR_OVER_9000_USAGE_TIMES_CH2;
            }else{ //other camera
                setUsbDeviceCh1(device);
                prop_key="persist.horusendoview.ch1";
                errorcode=Error.ENDOSCOPE_CONNECTOR_OVER_9000_USAGE_TIMES_CH1;
            }
            attachNewDevice(device);

            final String count = SystemPropertiesUnit.getSystemProperty(prop_key);
            int i=-1;
            try {
                i = Integer.parseInt(count);
            }catch (Exception e){
                Timber.e("onAttach: "+e.getMessage());
            }
            SystemPropertiesUnit.setSystemProperty(prop_key, String.valueOf(++i));
            if(errorcode.enable &&
                    (i>9000 || i<0)){
                IErrorCode.showErrorCode(getActivity(), errorcode);
                errorcode.enable=false;
                Log.w(TAG, String.format("%s: used times = %d",  errorcode.getCode(), i));
            }
        }

        /**
         * After obtaining USB device permissions, connect the USB camera
         */
        @Override
        public void onDeviceOpen(UsbDevice device, boolean isFirstOpen) {
            Timber.d("iCameraHelperStateCallback -> onDeviceOpen device=%s", device.getDeviceName());
            ICameraHelper iCameraHelper = getICameraHelper();
            iCameraHelper.openCamera();
            iCameraHelper.setButtonCallback((button, state) -> {
                Timber.w("iCameraHelperStateCallback -> onDeviceOpen -> onButton button=" + button + " | state=" + state);
            });
        }

        @Override
        public void onCameraOpen(UsbDevice device) {
            Timber.d("iCameraHelperStateCallback -> onCameraOpen device=%s", device.getDeviceName());
            @Nullable final ICameraHelper iCameraHelper = getICameraHelper();
            List<Size> supportedSizeList = null;
            try {
                supportedSizeList = iCameraHelper.getSupportedSizeList();
            } catch (Exception e) {
                Timber.e(e);
            }
            if (supportedSizeList == null) {
                supportedSizeList = new ArrayList<>();
            }


            @Nullable Size maxSize = null;
            try {
                maxSize = Collections.max(supportedSizeList, Comparator.comparingInt(a -> a.height));
            } catch (Exception e) {
                Timber.e(e);
            }
            Timber.d("iCameraHelperStateCallback -> onCameraOpen maxSize=%s", maxSize==null? "null": maxSize.toString());

            if (maxSize != null && iCameraHelper != null) {
                iCameraHelper.setPreviewSize(maxSize);
            }

            setRotation(getRotation());

            setCustomVideoCaptureConfig();

            setImageAdjustmentSettingToDevice();

            setCameraSettingsView();

            flag_CameraOpen=true;
            try {
                iCameraHelper.startPreview();
            } catch (Exception e) {
                Timber.e(e);
            }

            FragmentCameraBinding binding = getBinding();
            @Nullable SurfaceTexture surfaceTexture = binding != null ? binding.cameraView.getSurfaceTexture() : null;
            if (surfaceTexture != null && iCameraHelper != null) {
                try {
                    iCameraHelper.addSurface(surfaceTexture, false);
                } catch (Exception e) {
                    Timber.e(e);
                }
            }
            mIsCameraConnected = true;

            setRotation(getRotation());

            setIvTipsShow(false);
            showPromptSnapshot(false);
            mainHandler.removeCallbacks(recordTimeSecCountRunnable);


//            timerbtn = new Timer();
//            timerbtn.scheduleAtFixedRate(new CheckButtonTask(),0,300);
//            if(checkButtonHandle==null || checkButtonHandle.isCancelled()) {
//                checkButtonHandle = scheduledExecutorService.scheduleAtFixedRate(checkButtonRunnable, 0, 300, TimeUnit.MILLISECONDS);
//            }
        }

        @Override
        public void onCameraClose(UsbDevice device) {
            Timber.d("iCameraHelperStateCallback -> onCameraOpen device=%s", device.getDeviceName());
            if (mIsRecording) {
                toggleVideoRecord(false);
            }

            FragmentCameraBinding binding = getBinding();
            ICameraHelper iCameraHelper = getICameraHelper();
            SurfaceTexture surfaceTexture = binding != null ? binding.cameraView.getSurfaceTexture() : null;
            if (surfaceTexture != null && iCameraHelper != null) {
                iCameraHelper.removeSurface(surfaceTexture);
            }
            mIsCameraConnected = false;

            setIvTipsShow(true);
            setRecordEnable(false);
            setSnapshotEnable(false);
            showPromptSnapshot(false);
            showCameraSettings(false);
            mainHandler.removeCallbacks(recordTimeSecCountRunnable);

//            if(timerbtn != null) {
//                timerbtn.cancel();
//                timerbtn.purge();
//                timerbtn = null;
//            }
            closeCamera();

            FragmentActivity activity = getActivity();
            if (activity != null && activity instanceof MainActivity) {
                ((MainActivity)activity).restartStandbyNotificationTimeServiceTimer();
            }
        }

        @Override
        public void onDeviceClose(UsbDevice device) {
            Timber.d("iCameraHelperStateCallback -> onDeviceClose device=%s", device.getDeviceName());
        }

        @Override
        public void onDetach(UsbDevice device) {
            Timber.d("iCameraHelperStateCallback -> onDetach device=%s", device.getDeviceName());
            final UsbDevice usbDeviceCh1 = getUsbDeviceCh1();
            final UsbDevice usbDeviceCh2 = getUsbDeviceCh2();
            if (usbDeviceCh1 != null && TextUtils.equals(usbDeviceCh1.getDeviceName(), device.getDeviceName())) {
                setUsbDeviceCh1(null);
            } else if (usbDeviceCh2 != null && TextUtils.equals(usbDeviceCh2.getDeviceName(), device.getDeviceName())) {
                setUsbDeviceCh2(null);
            }

            if (device.equals(getUsbDeviceNow())) {
                if (usbDeviceCh2 != null && usbDeviceCh1!=null && CameraFragment.this.isVisible()){
                    changeCamera();
                }else {
                    setUsbDeviceNow(null);
                }
            }
        }

        @Override
        public void onCancel(UsbDevice device) {
            Timber.d("iCameraHelperStateCallback -> onCancel device=%s", device.getDeviceName());
            if (device.equals(getUsbDeviceNow())) {
                setUsbDeviceNow(null);
            }
        }
    };

    public void toggleVideoRecord(final boolean isRecording) {
        Runnable r = new Runnable() {
            @Override
            public void run() {
                final ICameraHelper mCameraHelper = getICameraHelper();
                final boolean mIsCameraConnected = CameraFragment.this.mIsCameraConnected;
                try {
                    if (isRecording) {
                        if (mIsCameraConnected && mCameraHelper != null && !mCameraHelper.isRecording()) {
                            startRecord();
                        }
                    } else {
                        if (mIsCameraConnected && mCameraHelper != null && mCameraHelper.isRecording()) {
                            stopRecord();
                        }
                        stopRecordTimer();
                    }
                } catch (Exception e) {
                    Timber.e(e);
                    stopRecordTimer();
                }
                mIsRecording = isRecording;
            }
        };

        if (Looper.myLooper() == Looper.getMainLooper()) {
            r.run();
        } else {
            FragmentCameraBinding binding = getBinding();
            if (binding != null) {
                binding.getRoot().post(r);
            }
        }
    }


    private void startRecord() {
        Timber.d("startRecord");
        @Nullable final MyApplication myApplication = getMyApplication();
        if (myApplication == null) {
            return;
        }
        final String patientId = myApplication.getPatientId();
        final FragmentCameraBinding binding = getBinding();
        if (TextUtils.isEmpty(patientId)) {
            if (binding != null) {
                binding.getRoot().post(new Runnable() {
                    @Override
                    public void run() {
                        binding.patientIdLayout.performClick();
                    }
                });
            }
            return;
        }

        if (isStartRecording()) {
            return;
        }

        setStartRecording(true);
        final UserTbData userTbData = myApplication.getLoginUserTbData();

        String account = null;
        if (userTbData != null) {
            account = userTbData.getAccount();
        }
        if (account == null) {
            account = "guest";
        }

        final Instant nowInstant = Instant.now();
        final LocalDate nowDate = nowInstant.atZone(ZoneId.systemDefault()).toLocalDate();
        final String dateStr = nowDate.format(
                DateTimeFormatter.ofPattern(
                        DIR_NAME_DATE_FORMAT_PATTERN,
                        Locale.ENGLISH
                )
        );

        final File filesDirFile = new File(myApplication.getMainDirPath() + File.separator + account + "_" + patientId + "_" + dateStr);
        if (!filesDirFile.exists()) {
            try {
                filesDirFile.mkdirs();
            } catch (Exception e) {
                Timber.e(e);
            }
        }

        @Nullable ProcedureFolderTbData procedureFolderTbDataDB = null;
        try {
            procedureFolderTbDataDB = myApplication.getMyRoomDatabase().procedureFolderTbDataDao().find(
                    account,
                    patientId,
                    filesDirFile.getPath(),
                    nowDate.toString()
            );
        } catch (Exception e) {
            Timber.e(e);
        }


        if (procedureFolderTbDataDB == null) {
            final ProcedureFolderTbData procedureFolderTbData = new ProcedureFolderTbData();
            procedureFolderTbData.setId(UUID.randomUUID().toString());
            procedureFolderTbData.setAccount(account);
            procedureFolderTbData.setPatientId(patientId);
            procedureFolderTbData.setFilePath(filesDirFile.getPath());
            procedureFolderTbData.setCreateDate(nowDate);
            try {
                myApplication.getMyRoomDatabase().procedureFolderTbDataDao().insert(procedureFolderTbData);
            } catch (Exception e) {
                Timber.e(e);
            }
        }

        final File file = new File(
                filesDirFile, patientId + "_" + new SimpleDateFormat(
                VIDEO_SIMPLE_DATE_FORMAT_PATTERN, Locale.ENGLISH
        ).format(new Date(nowInstant.toEpochMilli())) + MyApplication.VIDEO_FILE_EXTENSION);

        try {
            scheduledExecutorService.execute(new Runnable() {
                @Override
                public void run() {
                    @Nullable MaximumVideoDurationType type = SharedPreferencesManager.getInstance().getMaximumVideoDurationType();
                    if (type == null) {
                        type = MaximumVideoDurationType.MIN_30;
                    }
                    int maxDurationMinute = 30;
                    switch (type) {
                        case MIN_30:
                            maxDurationMinute = 30;
                            break;
                        case MIN_60:
                            maxDurationMinute = 60;
                            break;
                        case MIN_90:
                            maxDurationMinute = 90;
                            break;
                        case MIN_120:
                            maxDurationMinute = 120;
                            break;
                    }

                    VideoCapture.OutputFileOptions options = new VideoCapture.OutputFileOptions.Builder(file)
                            .build();

                    final int finalMaxDurationMinute = maxDurationMinute;

                    final ICameraHelper iCameraHelper = getICameraHelper();
                    if (iCameraHelper != null) {
                        iCameraHelper.startRecording(options, new VideoCapture.OnVideoCaptureCallback() {
                            @Override
                            public void onStart() {
                                Timber.d("startRecord -> OnVideoCaptureCallback -> onStart");
                                setStartRecording(false);
                                setRecordTimeSec(0L);
                                setRecordMaxTimeSec(finalMaxDurationMinute * 60L);
                                startRecordTimer();
                            }

                            @Override
                            public void onVideoSaved(@NonNull VideoCapture.OutputFileResults outputFileResults) {
                                Timber.d("startRecord -> OnVideoCaptureCallback -> onVideoSaved outputFileResults=%s", outputFileResults);
                                setStartRecording(false);
                                toggleVideoRecord(false);
                            }

                            @Override
                            public void onError(int videoCaptureError, @NonNull String message, Throwable cause) {
                                Timber.e("startRecord -> OnVideoCaptureCallback -> onError videoCaptureError=" + videoCaptureError + " | message=" + message);
                                Timber.e(cause);
                                setStartRecording(false);
                                toggleVideoRecord(false);
                            }
                        });
                    }
                }
            });
        } catch (Exception e) {
            Timber.e(e);
        }
    }


    private void stopRecord() {
        Timber.d("stopRecord");
        final ICameraHelper iCameraHelper = getICameraHelper();
        if (iCameraHelper != null) {
            iCameraHelper.stopRecording();
        }
    }

    private void startRecordTimer() {
        recordTimeViewUpdate();
        setMaximumVideoDurationToView();
        showVideoRecordDot(true);
        showRecordingTimeLayout(true);

        FragmentCameraBinding binding = getBinding();
        if (binding != null) {
            binding.getRoot().post(new Runnable() {
                @Override
                public void run() {
                    binding.record.setSelected(true);
                    binding.recordImg.setSelected(true);
                    binding.recordTime.setSelected(true);
                    binding.recordLine.setSelected(true);
                    binding.maximumVideoDuration.setSelected(true);
                }
            });
        }
        mainHandler.removeCallbacks(recordTimeSecCountRunnable);
        recordTimeSecUptimeMillis = 0;
        mainHandler.postAtTime(
                recordTimeSecCountRunnable,
                SystemClock.uptimeMillis() + 1000L
        );
    }


    private void stopRecordTimer() {
        mainHandler.removeCallbacks(recordTimeSecCountRunnable);
        setRecordTimeSec(null);
        setRecordMaxTimeSec(null);
        showVideoRecordDot(false);
        showRecordingTimeLayout(false);
        final FragmentCameraBinding binding = getBinding();
        if (binding != null) {
            binding.getRoot().post(new Runnable() {
                @Override
                public void run() {
                    binding.record.setSelected(false);
                    binding.recordImg.setSelected(false);
                    binding.recordTime.setSelected(false);
                    binding.recordLine.setSelected(false);
                    binding.maximumVideoDuration.setSelected(false);
                }
            });
        }
    }


    @UiThread
    private void takePhoto() {
        final Context context = getContext();
        if (context == null) {
            return;
        }

        MyApplication myApplication = getMyApplication();
        if (myApplication == null) {
            return;
        }

        @Nullable final String patientId = myApplication.getPatientId();

        @Nullable final ICameraHelper mCameraHelper = getICameraHelper();

        if (mCameraHelper == null || !mCameraHelper.isCameraOpened() || isTakePhotoRunning()) {
            return;
        }

        final PatientIdDialog patientIdDialog = getPatientIdDialog();
        if (patientIdDialog != null && patientIdDialog.isAdded()) {
            return;
        }

        Runnable timeoutRunnable = new Runnable() {
            @Override
            public void run() {
                setTakePhotoRunning(false);
                FragmentCameraBinding binding = getBinding();
                if (binding != null) {
                    binding.getRoot().removeCallbacks(this);
                }
                setSnapshotEnable(true);
                showPromptSnapshot(false);
                Toast.makeText(context, R.string.image_capture_timeout_msg, Toast.LENGTH_SHORT).show();
            }
        };

        FragmentCameraBinding binding = getBinding();

        if (binding != null) {
            binding.getRoot().postDelayed(timeoutRunnable, IMAGE_CAPTURE_TIMEOUT_MILLIS);
        }

        @Nullable final UserTbData userTbData = myApplication.getLoginUserTbData();

        @NonNull final String account = userTbData != null ? userTbData.getAccount() : "guest";

        @NonNull final Instant nowInstant = Instant.now();
        @NonNull final LocalDate nowDate = nowInstant.atZone(ZoneId.systemDefault()).toLocalDate();

        @NonNull final String dateStr = nowDate.format(DateTimeFormatter.ofPattern(
                DIR_NAME_DATE_FORMAT_PATTERN, Locale.ENGLISH));

        File filesDirFile;

        if (patientId != null && !patientId.isEmpty()) {
            filesDirFile = new File(myApplication.getMainDirPath() +
                    File.separator + account + "_" + patientId + "_" + dateStr);
        } else {
            filesDirFile = new File(myApplication.getMainDirPath());
        }

        if (!filesDirFile.exists()) {
            try {
                filesDirFile.mkdirs();
            } catch (Exception e) {
                Timber.e(e);
            }
        }

        if (patientId != null && !patientId.isEmpty()) {
            ProcedureFolderTbData procedureFolderTbDataDB = null;
            try {
                procedureFolderTbDataDB = myApplication.getMyRoomDatabase().procedureFolderTbDataDao()
                        .find(account, patientId, filesDirFile.getPath(), nowDate.toString());
            } catch (Exception e) {
                Timber.e(e);
            }

            if (procedureFolderTbDataDB == null) {
                ProcedureFolderTbData procedureFolderTbData = new ProcedureFolderTbData();
                procedureFolderTbData.setId(UUID.randomUUID().toString());
                procedureFolderTbData.setAccount(account);
                procedureFolderTbData.setPatientId(patientId);
                procedureFolderTbData.setFilePath(filesDirFile.getPath());
                procedureFolderTbData.setCreateDate(nowDate);

                try {
                    myApplication.getMyRoomDatabase().procedureFolderTbDataDao().insert(procedureFolderTbData);
                } catch (Exception e) {
                    Timber.e(e);
                }
            }
        }

        @NonNull final Date date = Date.from(nowInstant);
        @NonNull final File outputFile = new File(filesDirFile, patientId + "_" +
                new SimpleDateFormat(IMG_SIMPLE_DATE_FORMAT_PATTERN, Locale.ENGLISH)
                        .format(date) + MyApplication.IMG_FILE_EXTENSION);

        final ImageCapture.OutputFileOptions options = new ImageCapture.OutputFileOptions.Builder(outputFile).build();

        setTakePhotoRunning(true);
        setSnapshotEnable(false);
        showPromptSnapshot(true);

        try {
            mCameraHelper.takePicture(zoom, options, new ImageCapture.OnImageCaptureCallback() {
                @SuppressLint("RestrictedApi")
                @Override
                public void onImageSaved(@NonNull final ImageCapture.OutputFileResults outputFileResults) {
                    Timber.d("takePhoto -> OnImageCaptureCallback -> onImageSaved");
                    setTakePhotoRunning(false);
                    FragmentCameraBinding binding = getBinding();
                    if (binding != null) {
                        binding.getRoot().removeCallbacks(timeoutRunnable);
                    }
                    showPromptSnapshot(false);
                    setSnapshotEnable(true);

                    @Nullable ExifInterface exif = null;
                    try {
                        exif = new ExifInterface(outputFile.getAbsolutePath());
                    } catch (Exception e) {
                        Timber.e(e);
                    }

                    if (exif != null) {
                        exif.setAttribute(ExifInterface.TAG_ORIENTATION, String.valueOf(ExifInterface.ORIENTATION_NORMAL));

                        try {
                            exif.setDateTime(date.getTime());
                        } catch (Exception e) {
                            Timber.e(e);
                        }

                        try {
                            exif.saveAttributes();
                            Timber.d("EXIF metadata saved: %s", outputFile.getAbsolutePath());
                        } catch (Exception e) {
                            Timber.e(e);
                        }
                    }

                    if (patientId == null || patientId.isEmpty()) {
                        showPatientIdDialog(
                                new Runnable() {
                                    @Override
                                    public void run() {
                                        try {
                                            scheduledExecutorService.execute(new Runnable() {
                                                @Override
                                                public void run() {
                                                    String patientIdNew = myApplication.getPatientId();
                                                    if (patientIdNew == null) {
                                                        patientIdNew = "";
                                                    }
                                                    final File copyFileDirFile = new File(myApplication.getMainDirPath() +
                                                            File.separator + account + "_" + patientIdNew + "_" + dateStr);

                                                    if (!copyFileDirFile.exists()) {
                                                        try {
                                                            copyFileDirFile.mkdirs();
                                                        } catch (Exception e) {
                                                            Timber.e(e);
                                                        }
                                                    }

                                                    @Nullable ProcedureFolderTbData procedureFolderTbDataDB = null;
                                                    try {
                                                        procedureFolderTbDataDB = myApplication.getMyRoomDatabase()
                                                                .procedureFolderTbDataDao().find(account, patientIdNew,
                                                                        copyFileDirFile.getPath(), nowDate.toString());
                                                    } catch (Exception e) {
                                                        Timber.e(e);
                                                    }

                                                    if (procedureFolderTbDataDB == null) {
                                                        ProcedureFolderTbData procedureFolderTbData = new ProcedureFolderTbData();
                                                        procedureFolderTbData.setId(UUID.randomUUID().toString());
                                                        procedureFolderTbData.setAccount(account);
                                                        procedureFolderTbData.setPatientId(patientIdNew);
                                                        procedureFolderTbData.setFilePath(copyFileDirFile.getPath());
                                                        procedureFolderTbData.setCreateDate(nowDate);

                                                        try {
                                                            myApplication.getMyRoomDatabase().procedureFolderTbDataDao()
                                                                    .insert(procedureFolderTbData);
                                                        } catch (Exception e) {
                                                            Timber.e(e);
                                                        }
                                                    }

                                                    final File copyOutputFile = new File(copyFileDirFile, patientIdNew + "_" +
                                                            new SimpleDateFormat(IMG_SIMPLE_DATE_FORMAT_PATTERN, Locale.ENGLISH)
                                                                    .format(date) + MyApplication.IMG_FILE_EXTENSION);

                                                    MyStorageManager.getInstance().copyFileWithProgressFlow(outputFile, copyOutputFile)
                                                            .subscribe(new Observer<MyStorageManager.ProgressData>() {

                                                                @Override
                                                                public void onSubscribe(@io.reactivex.rxjava3.annotations.NonNull Disposable d) {

                                                                }

                                                                @Override
                                                                public void onNext(MyStorageManager.ProgressData progressData) {
                                                                    Timber.d("takePhoto -> copyFileWithProgressFlow -> onNext progressData=%s", progressData);
                                                                }

                                                                @Override
                                                                public void onError(Throwable t) {
                                                                    Timber.e("takePhoto -> copyFileWithProgressFlow -> onError");
                                                                    Timber.e(t);
                                                                    try {
                                                                        if(!outputFile.delete()){
                                                                            Timber.w("delete file fail:"+outputFile.getAbsolutePath());
                                                                        }
                                                                    } catch (Exception e) {
                                                                        Timber.e(e);
                                                                    }
                                                                }

                                                                @Override
                                                                public void onComplete() {
                                                                    Timber.d("takePhoto -> copyFileWithProgressFlow -> onComplete");
                                                                    try {
                                                                        if(!outputFile.delete()){
                                                                            Timber.w("delete file fail:"+outputFile.getAbsolutePath());
                                                                        }
                                                                    } catch (Exception e) {
                                                                        Timber.e(e);
                                                                    }

                                                                    final FragmentCameraBinding binding = getBinding();
                                                                    if (binding != null) {
                                                                        try {
                                                                            @Nullable final File selectImgFile = binding.previewWindowsView.getSelectImgFile();
                                                                            if (selectImgFile == null ||
                                                                                    !selectImgFile.exists()) {
                                                                                binding.previewWindowsView.setTempUnLockImgFile(true);
                                                                            }
                                                                        } catch (Exception e) {
                                                                            Timber.e(e);
                                                                        }

                                                                        binding.previewWindowsView.setNewImgFile(copyOutputFile);

                                                                        binding.previewWindowsView.setTempUnLockImgFile(false);
                                                                    }
                                                                }
                                                            });
                                                }
                                            });
                                        } catch (Exception e) {
                                            Timber.e(e);
                                        }
                                    }
                                },
                                new Runnable() {
                                    @Override
                                    public void run() {
                                        try {
                                            scheduledExecutorService.execute(new Runnable() {
                                                @Override
                                                public void run() {
                                                    try {
                                                        if(!outputFile.delete()){
                                                            Timber.w("delete file fail:"+outputFile.getAbsolutePath());
                                                        }
                                                    } catch (Exception e) {
                                                        Timber.e(e);
                                                    }
                                                }
                                            });
                                        } catch (Exception e) {
                                            Timber.e(e);
                                        }
                                    }
                                }
                        );
                        return;
                    }

                    if (binding != null) {
                        try {
                            @Nullable final File selectImgFile = binding.previewWindowsView.getSelectImgFile();
                            if (selectImgFile == null ||
                                    !selectImgFile.exists()) {
                                binding.previewWindowsView.setTempUnLockImgFile(true);
                            }
                        } catch (Exception e) {
                            Timber.e(e);
                        }

                        binding.previewWindowsView.setNewImgFile(outputFile);

                        binding.previewWindowsView.setTempUnLockImgFile(false);
                    }
                }

                @Override
                public void onError(int imageCaptureError, @NonNull String message, @Nullable Throwable cause) {
                    Timber.d("takePhoto -> OnImageCaptureCallback -> onError imageCaptureError=" + imageCaptureError + " | message=" + message);
                    Timber.d(cause);
                    setTakePhotoRunning(false);
                    if (binding != null) {
                        binding.getRoot().removeCallbacks(timeoutRunnable);
                    }
                    setSnapshotEnable(true);
                    showPromptSnapshot(false);
                }
            });
        } catch (Exception e) {
            Timber.e(e);
            setTakePhotoRunning(false);
            if (binding != null) {
                binding.getRoot().removeCallbacks(timeoutRunnable);
            }
            setSnapshotEnable(true);
            showPromptSnapshot(false);
        }
    }


    public void nextRotation() {
        synchronized (rotationLock) {
            int rotation = getRotation();
            switch (rotation) {
                case Surface.ROTATION_0:
                    rotation = Surface.ROTATION_90;
                    break;
                case Surface.ROTATION_90:
                    rotation = Surface.ROTATION_180;
                    break;
                case Surface.ROTATION_180:
                    rotation = Surface.ROTATION_270;
                    break;
                case Surface.ROTATION_270:
                    rotation = Surface.ROTATION_0;
                    break;
            }
            setRotation(rotation);
        }
    }


    public void changeNextZoom() {
        synchronized (zoomLock) {
            float zoom = getZoom();
            if (zoom <= 1.0f) {
                setZoom(2.0f);
            } else {
                setZoom(1.0f);
            }
        }
    }


    public void setIvTipsShow(boolean isShow) {
        final Runnable r = new Runnable() {
            @Override
            public void run() {
                FragmentCameraBinding binding = getBinding();
                if (binding != null) {
                    if (iCameraHelper == null) {
                        Timber.w("[setIvTipsShow] iCameraHelper == null");
                        return;
                    }
                    if ((isShow) && !iCameraHelper.isCameraOpened()) {// JerryLin
                        binding.ivTips.setVisibility(View.VISIBLE);

                        if(FLAG_ENDOSCOPE_USABLE_TIME) {
                            binding.scopeTime.setImageDrawable(null);
                            AnimationUtils.clearAnimation(binding.scopeTime);
                            //mainHandler.removeCallbacks(endoscopyRemainingTimeRunnable);
                        }

                        mainHandler.removeCallbacks(showE2000Runnable);
                    } else {
                        binding.ivTips.setVisibility(View.GONE);

//                        if(FLAG_ENDOSCOPE_USABLE_TIME) {
//                            binding.scopeTime.setImageResource(R.drawable.scope_4hr);
//                            mainHandler.post(endoscopyRemainingTimeRunnable);
//                        }

                        if(CameraFragment.this.isHidden()==false) {
                            mainHandler.postDelayed(showE2000Runnable, OPEN_CAMERA_TIMEOUT_MILLIS);
                            Error.NO_SIGNAL_WHEN_ENDOSCOPE_INSERTED.enable = true;
                        }
                    }
                }
            }
        };
        if (Looper.myLooper() == Looper.getMainLooper()) {
            r.run();
        } else {
            FragmentCameraBinding binding = getBinding();
            if (binding != null) {
                binding.getRoot().post(r);
            }
        }
    }


    public void setSnapshotEnable(boolean isEnable) {
        final Runnable r = new Runnable() {
            @Override
            public void run() {
                MyApplication myApplication = getMyApplication();
                UserTbData userTbData = null;
                if (myApplication != null) {
                    userTbData = myApplication.getLoginUserTbData();
                }
                @Nullable UserRoleType roleType = null;
                if (userTbData != null) {
                    roleType = userTbData.getRoleType();
                }
                FragmentCameraBinding binding = getBinding();
                if (roleType == null || roleType == UserRoleType.GUEST) {
                    if (binding != null) {
                        binding.snapshot.setEnabled(false);
                    }
                } else {
                    if (binding != null) {
                        binding.snapshot.setEnabled(isEnable);
                    }
                }
            }
        };
        if (Looper.myLooper() == Looper.getMainLooper()) {
            r.run();
        } else {
            FragmentCameraBinding binding = getBinding();
            if (binding != null) {
                binding.getRoot().post(r);
            }
        }
    }


    public void showPromptSnapshot(final boolean isShow) {
        final Runnable r = new Runnable() {
            @Override
            public void run() {
                FragmentCameraBinding binding = getBinding();
                if (binding != null) {
                    if (isShow) {
                        binding.promptSnapshot.setVisibility(View.VISIBLE);
                        binding.snapshotLine.setVisibility(View.VISIBLE);
                    } else {
                        binding.promptSnapshot.setVisibility(View.INVISIBLE);
                        binding.snapshotLine.setVisibility(View.INVISIBLE);
                    }
                }
            }
        };
        if (Looper.myLooper() == Looper.getMainLooper()) {
            r.run();
        } else {
            FragmentCameraBinding binding = getBinding();
            if (binding != null) {
                binding.getRoot().post(r);
            }
        }
    }


    public void showVideoRecordDot(boolean isShow) {
        final Runnable r = new Runnable() {
            @Override
            public void run() {
                FragmentCameraBinding binding = getBinding();
                if (binding != null) {
                    if (isShow) {
                        if (binding.promptVideo.getVisibility() != View.VISIBLE) {
                            binding.promptVideo.setVisibility(View.VISIBLE);
                            binding.promptVideoDot.setVisibility(View.VISIBLE);
                            binding.promptVideo.removeCallbacks(videoRecordDotFlashingRunnable);
                            binding.promptVideo.postDelayed(videoRecordDotFlashingRunnable, 1000L);
                        }
                    } else {
                        binding.promptVideo.setVisibility(View.INVISIBLE);
                        binding.promptVideo.removeCallbacks(videoRecordDotFlashingRunnable);
                    }
                    setChangeCameraEnable(!isShow);
                }
            }
        };
        if (Looper.myLooper() == Looper.getMainLooper()) {
            r.run();
        } else {
            FragmentCameraBinding binding = getBinding();
            if (binding != null) {
                binding.getRoot().post(r);
            }
        }
    }

    /**
     * 錄影紅點閃爍
     */
    @NonNull
    private final Runnable videoRecordDotFlashingRunnable = new Runnable() {
        @Override
        public void run() {
            FragmentCameraBinding binding = getBinding();
            ICameraHelper iCameraHelper = getICameraHelper();
            if (binding != null) {
                if (iCameraHelper != null && iCameraHelper.isRecording()) {
                    if (binding.promptVideoDot.getVisibility() == View.VISIBLE) {
                        binding.promptVideoDot.setVisibility(View.INVISIBLE);
                    } else {
                        binding.promptVideoDot.setVisibility(View.VISIBLE);
                    }
                    binding.promptVideo.postDelayed(this, 1000L);
                }
            }
        }
    };

    private void showRecordingTimeLayout(boolean isShow) {
        final Runnable r = new Runnable() {
            @Override
            public void run() {
                FragmentCameraBinding binding = getBinding();
                if (binding != null) {
                    if (isShow) {
                        binding.recordingTimeLayout.setVisibility(View.VISIBLE);
                    } else {
                        binding.recordingTimeLayout.setVisibility(View.GONE);
                    }
                }
            }
        };

        if (Looper.myLooper() == Looper.getMainLooper()) {
            r.run();
        } else {
            FragmentCameraBinding binding = getBinding();
            if (binding != null) {
                binding.getRoot().post(r);
            }
        }
    }


    private void setMaximumVideoDurationToView() {
        final Runnable r = new Runnable() {
            @SuppressLint("SetTextI18n")
            @Override
            public void run() {
                final FragmentCameraBinding binding = getBinding();
                if (binding != null) {
                    Long recordMaxTimeSec = getRecordMaxTimeSec();
                    if (recordMaxTimeSec == null) {
                        recordMaxTimeSec = 0L;
                    }
                    Long recordTimeSec = getRecordTimeSec();
                    if (recordTimeSec == null) {
                        recordTimeSec = 0L;
                    }
                    long diff = recordMaxTimeSec - recordTimeSec;
                    if (diff < 0) {
                        diff = 0L;
                    }

                    List<String> arr = Arrays.asList(
                            new SimpleDateFormat("HH:mm", Locale.ENGLISH)
                                    .format(new Date(diff * 1000))
                                    .split(":")
                    );

                    arr.set(0, arr.get(0) + "h");
                    arr.set(1, arr.get(1) + "m");

                    binding.maximumVideoDuration.setText(arr.get(0) + ":" + arr.get(1));
                }
            }
        };

        if (Looper.myLooper() == Looper.getMainLooper()) {
            r.run();
        } else {
            final FragmentCameraBinding binding = getBinding();
            if (binding != null) {
                binding.getRoot().post(r);
            }
        }
    }

    @NonNull
    private final Runnable recordTimeSecCountRunnable = new Runnable() {
        @Override
        public void run() {
            final Long recordTimeSec = getRecordTimeSec();
            if (recordTimeSec == null) {
                return;
            }
            setRecordTimeSec(recordTimeSec + 1);
            recordTimeViewUpdate();
            setMaximumVideoDurationToView();

            final Long recordMaxTimeSec = getRecordMaxTimeSec();
            if (recordMaxTimeSec != null && recordMaxTimeSec.equals(recordTimeSec + 1)) {
                toggleVideoRecord(false);
                // 最大錄影時間
                showMyDialog(R.string.continue_recording,
                        true,
                        R.string.continue_,
                        true,
                        R.string.cancel,
                        new MyDialog.Listener() {
                            @Override
                            public void OnClickConfirm() {
                                FragmentCameraBinding binding = getBinding();
                                if (binding != null) {
                                    binding.record.performClick();
                                }
                            }

                            @Override
                            public void OnClickCancel() {
                            }
                        });
                return;
            }

            long now = SystemClock.uptimeMillis();
            long diff = now - recordTimeSecUptimeMillis;
            long next = now + 1000;
            if (diff <= 1000) {
                next = now + (1000 - diff);
            }
            mainHandler.postAtTime(this, next);
            recordTimeSecUptimeMillis = next;
        }
    };


    private void recordTimeViewUpdate() {
        Runnable r = new Runnable() {
            @Override
            public void run() {
                FragmentCameraBinding binding = getBinding();
                if (binding == null) {
                    return;
                }
                Long recordTimeSec = getRecordTimeSec();
                if (recordTimeSec == null) {
                    recordTimeSec = 0L;
                }
                binding.recordTime.setText(LocalTime.ofSecondOfDay(recordTimeSec)
                        .format(DateTimeFormatter.ISO_LOCAL_TIME));
            }
        };

        if (Looper.myLooper() == Looper.getMainLooper()) {
            r.run();
        } else {
            final FragmentCameraBinding binding = getBinding();
            if (binding != null) {
                binding.getRoot().post(r);
            }
        }
    }


    private void showMyDialog(
            @StringRes int msg,
            boolean isShowConfirm,
            @StringRes int confirmBtnText,
            boolean isShowCancel,
            @StringRes int cancelBtnText,
            MyDialog.Listener listener
    ) {
        Context context = getContext();
        if (context == null) {
            return;
        }
        showMyDialog(
                context.getString(msg),
                isShowConfirm,
                context.getString(confirmBtnText),
                isShowCancel,
                context.getString(cancelBtnText),
                listener
        );
    }


    private void showMyDialog(
            String msg,
            boolean isShowConfirm,
            String confirmBtnText,
            boolean isShowCancel,
            String cancelBtnText,
            MyDialog.Listener listener
    ) {
        if (!isResumed() || !isVisible()) {
            return;
        }
        cancelMyDialog();
        final Runnable r = new Runnable() {
            @Override
            public void run() {
                Context context = getContext();
                if (context == null) {
                    return;
                }
                MyDialog d = new MyDialog(context, false) {
                    @Override
                    public boolean dispatchTouchEvent(MotionEvent ev) {
                        MyApplication myApplication = (MyApplication) context.getApplicationContext();
                        MainActivity mainActivity = myApplication.getMainActivity();
                        if (mainActivity != null) {
                            mainActivity.restartStandbyNotificationTimeServiceTimer();
                        }
                        return super.dispatchTouchEvent(ev);
                    }
                };
                d.setMsg(msg);
                d.showConfirm(isShowConfirm);
                d.setConfirmText(confirmBtnText);
                d.showCancel(isShowCancel);
                d.setCancelText(cancelBtnText);
                d.setListener(listener);
                d.show();
                myDialog = d;
            }
        };
        if (Looper.myLooper() == Looper.getMainLooper()) {
            r.run();
        } else {
            final FragmentCameraBinding binding = getBinding();
            if (binding != null) {
                binding.getRoot().post(r);
            }
        }
    }


    private void cancelMyDialog() {
        final Runnable r = new Runnable() {
            @Override
            public void run() {
                MyDialog myDialog = CameraFragment.this.myDialog;
                if (myDialog != null && myDialog.isShowing()) {
                    myDialog.dismiss();
                }
                CameraFragment.this.myDialog = null;
            }
        };
        if (Looper.myLooper() == Looper.getMainLooper()) {
            r.run();
        } else {
            final FragmentCameraBinding binding = getBinding();
            if (binding != null) {
                binding.getRoot().post(r);
            }
        }
    }


    private void setRecordEnable(final boolean isEnable) {
        Runnable r = new Runnable() {
            @Override
            public void run() {
                MyApplication myApplication = getMyApplication();
                UserTbData userTbData = null;
                if (myApplication != null) {
                    userTbData = myApplication.getLoginUserTbData();
                }
                UserRoleType roleType = null;
                if (userTbData != null) {
                    roleType = userTbData.getRoleType();
                }
                FragmentCameraBinding binding = getBinding();
                if (binding != null) {
                    if (roleType == null || roleType == UserRoleType.GUEST) {
                        binding.record.setEnabled(false);
                    } else {
                        binding.record.setEnabled(isEnable);
                    }
                }
            }
        };
        if (Looper.myLooper() == Looper.getMainLooper()) {
            r.run();
        } else {
            FragmentCameraBinding binding = getBinding();
            if (binding != null) {
                binding.getRoot().post(r);
            }
        }
    }


    private synchronized void setImageAdjustmentSettingToDevice() {
        MyApplication myApplication = getMyApplication();
        if (myApplication == null) {
            return;
        }
        UsbDevice usbDeviceNow = getUsbDeviceNow();
        if (usbDeviceNow == null) {
            return;
        }
        UserTbData loginUserTbData = myApplication.getLoginUserTbData();
        if (loginUserTbData == null) {
            return;
        }
        ICameraHelper iCameraHelper = getICameraHelper();
        if (iCameraHelper == null) {
            return;
        }

        Size previewSize = iCameraHelper.getPreviewSize();
        if (previewSize == null) {
            return;
        }

//        @Nullable ImageAdjustmentSettingTbData imageAdjustmentSettingData = null;
//        try {
//            imageAdjustmentSettingData = myApplication.getMyRoomDatabase().imageAdjustmentSettingTbDataDao().find(
//                    String.valueOf(usbDeviceNow.getVendorId()),
//                    loginUserTbData.getAccount(),
//                    previewSize
//            );
//        } catch (Exception e) {
//            Timber.e(e);
//        }
//        if (imageAdjustmentSettingData == null) {
//            return;
//        }

        if (!iCameraHelper.isCameraOpened()) {
            return;
        }

//        Integer whiteBalance = null;//= imageAdjustmentSettingData.getWhiteBalance();
//        if (whiteBalance != null) {
//            try {
//                iCameraHelper.getUVCControl().setWhiteBalance(whiteBalance);
//            } catch (Exception e) {
//                Timber.e(e);
//            }
//        } else {
            try {
                iCameraHelper.getUVCControl().resetWhiteBalance();
            } catch (Exception e) {
                Timber.e(e);
            }
//        }

//        Integer contrast = null;//= imageAdjustmentSettingData.getContrast();
//        if (contrast != null) {
//            try {
//                iCameraHelper.getUVCControl().setContrast(contrast);
//            } catch (Exception e) {
//                Timber.e(e);
//            }
//        } else {
            try {
                iCameraHelper.getUVCControl().resetContrast();
            } catch (Exception e) {
                Timber.e(e);
            }
//        }

//        Integer sharpness = null;//= imageAdjustmentSettingData.getSharpness();
//        if (sharpness != null) {
//            try {
//                iCameraHelper.getUVCControl().setSharpness(sharpness);
//            } catch (Exception e) {
//                Timber.e(e);
//            }
//        } else {
            try {
                iCameraHelper.getUVCControl().resetSharpness();
            } catch (Exception e) {
                Timber.e(e);
            }
//        }

//        Integer brightness = null;//= imageAdjustmentSettingData.getBrightness();
//        if (brightness != null) {
//            try {
//                iCameraHelper.getUVCControl().setBrightness(brightness);
//            } catch (Exception e) {
//                Timber.e(e);
//            }
//        } else {
            try {
                iCameraHelper.getUVCControl().resetBrightness();
            } catch (Exception e) {
                Timber.e(e);
            }
//        }
    }


    private void setCameraSettingsView() {
        try {
            scheduledExecutorService.execute(new Runnable() {
                @Override
                public void run() {

                    final ICameraHelper iCameraHelper = getICameraHelper();
                    if (iCameraHelper == null) {
                        return;
                    }

                    if (!iCameraHelper.isCameraOpened()) {
                        return;
                    }

                    final FragmentCameraBinding binding = getBinding();
                    if (binding != null) {
                        binding.cameraSettings.setListener(null);
                    }

                    // 亮度-------------------------------------------------------------------------------
                    int[] brightnessRange = null;
                    try {
                        brightnessRange = iCameraHelper.getUVCControl().updateBrightnessLimit();
                    } catch (Exception e) {
                        Timber.e(e);
                    }
                    BrightnessDEF = brightnessRange[2];

                    if (brightnessRange != null && brightnessRange.length >= 2 && binding != null) {
                        final int[] finalBrightnessRange = brightnessRange;
                        binding.getRoot().post(() -> {

                            binding.cameraSettings.getBinding().brightnessSeekBar.setMin(LevelMin);
                            binding.cameraSettings.getBinding().brightnessSeekBar.setMax(LevelMax);
                        });

                        try {
                            @Nullable final Integer brightness = iCameraHelper.getUVCControl().getBrightness();

                            // switch need constant, use if case
                            if (brightness == (BrightnessDEF - 20)) {
                                BrightnessLevel = 1;
                            }
                            else if (brightness == (BrightnessDEF - 15)) {
                                BrightnessLevel = 2;
                            }
                            else if (brightness == (BrightnessDEF - 10)) {
                                BrightnessLevel = 3;
                            }
                            else if (brightness == (BrightnessDEF - 5)) {
                                BrightnessLevel = 4;
                            }
                            else if (brightness == (BrightnessDEF)) {
                                BrightnessLevel = 5;
                            }
                            else if (brightness == (BrightnessDEF + 5)) {
                                BrightnessLevel = 6;
                            }
                            else if (brightness == (BrightnessDEF + 10)) {
                                BrightnessLevel = 7;
                            }
                            else if (brightness == (BrightnessDEF + 15)) {
                                BrightnessLevel = 8;
                            }
                            else if (brightness == (BrightnessDEF + 20)) {
                                BrightnessLevel = 9;
                            }

                            binding.getRoot().post(() -> {
                                if (brightness != null) {

                                    binding.cameraSettings.getBinding().brightnessSeekBar.setProgress(BrightnessLevel);
                                }
                            });
                        } catch (Exception e) {
                            Timber.e(e);
                        }
                    }

                    // 對比度----------------------------------------------------------------------------
                    int[] contrastRange = null;
                    try {
                        contrastRange = iCameraHelper.getUVCControl().updateContrastLimit();
                    } catch (Exception e) {
                        Timber.e(e);
                    }
                    ContrastDEF = contrastRange[2];

                    if (contrastRange != null && contrastRange.length >= 2 && binding != null) {
                        int[] finalContrastRange = contrastRange;
                        binding.getRoot().post(() -> {
                            binding.cameraSettings.getBinding().contrastSeekBar.setMin(LevelMin);
                            binding.cameraSettings.getBinding().contrastSeekBar.setMax(LevelMax);
                        });

                        try {
                            Integer contrast = iCameraHelper.getUVCControl().getContrast();
                            // switch need constant, use if case
                            if (contrast == (ContrastDEF - 4)) {
                                ContrastLevel = 1;
                            }
                            else if (contrast == (ContrastDEF - 3)) {
                                ContrastLevel = 2;
                            }
                            else if (contrast == (ContrastDEF - 2)) {
                                ContrastLevel = 3;
                            }
                            else if (contrast == (ContrastDEF - 1)) {
                                ContrastLevel = 4;
                            }
                            else if (contrast == (ContrastDEF)) {
                                ContrastLevel = 5;
                            }
                            else if (contrast == (ContrastDEF + 1)) {
                                ContrastLevel = 6;
                            }
                            else if (contrast == (ContrastDEF + 2)) {
                                ContrastLevel = 7;
                            }
                            else if (contrast == (ContrastDEF + 3)) {
                                ContrastLevel = 8;
                            }
                            else if (contrast == (ContrastDEF + 4)) {
                                ContrastLevel = 9;
                            }

                            binding.getRoot().post(() -> {
                                if (contrast != null) {
                                    binding.cameraSettings.getBinding().contrastSeekBar.setProgress(ContrastLevel);
                                }
                            });
                        } catch (Exception e) {
                            Timber.e(e);
                        }
                    }

                    // 白平衡------------------------------------------------------------------------
                    int[] whiteBalanceRange = null;
                    try {
                        whiteBalanceRange = iCameraHelper.getUVCControl().updateWhiteBalanceLimit();
                    } catch (Exception e) {
                        Timber.e(e);
                    }
                    WhiteBlanceDEF = whiteBalanceRange[2];
                    if (whiteBalanceRange != null && whiteBalanceRange.length >= 2 && binding != null) {
                        int[] finalWhiteBalanceRange = whiteBalanceRange;
                        binding.getRoot().post(() -> {
                            binding.cameraSettings.getBinding().colourSeekBar.setMin(LevelMin);
                            binding.cameraSettings.getBinding().colourSeekBar.setMax(LevelMax);
                        });

                        try {
                            Integer whiteBalance = iCameraHelper.getUVCControl().getWhiteBalance();
                            switch (whiteBalance) {
                                case 2800:
                                    WhiteBlanceLevel = 1;
                                    break;
                                case 3200:
                                    WhiteBlanceLevel = 2;
                                    break;
                                case 3500:
                                    WhiteBlanceLevel = 3;
                                    break;
                                case 4000:
                                    WhiteBlanceLevel = 4;
                                    break;
                                case 4600:
                                    WhiteBlanceLevel = 5;
                                    break;
                                case 5000:
                                    WhiteBlanceLevel = 6;
                                    break;
                                case 5500:
                                    WhiteBlanceLevel = 7;
                                    break;
                                case 6000:
                                    WhiteBlanceLevel = 8;
                                    break;
                                case 6500:
                                    WhiteBlanceLevel = 9;
                                    break;
                            }
                            binding.getRoot().post(() -> {
                                if (whiteBalance != null) {
                                    binding.cameraSettings.getBinding().colourSeekBar.setProgress(WhiteBlanceLevel);
                                }
                            });
                        } catch (Exception e) {
                            Timber.e(e);
                        }
                    }

                    // 銳利度----------------------------------------------------------------------------
                    int[] sharpnessRange = null;
                    try {
                        sharpnessRange = iCameraHelper.getUVCControl().updateSharpnessLimit();
                    } catch (Exception e) {
                        Timber.e(e);
                    }

                    if (sharpnessRange != null && sharpnessRange.length >= 2 && binding != null) {
                        SharpnessDEF = sharpnessRange[2];
                        int[] finalSharpnessRange = sharpnessRange;
                        binding.getRoot().post(() -> {
                            binding.cameraSettings.getBinding().sharpnessSeekBar.setMin(LevelMin);
                            binding.cameraSettings.getBinding().sharpnessSeekBar.setMax(LevelMax);
                        });

                        try {
                            Integer sharpness = iCameraHelper.getUVCControl().getSharpness();
                            // switch need constant, use if case
                            if (sharpness == (SharpnessDEF - 4)) {
                                SharpnessLevel = 1;
                            }
                            else if (sharpness == (SharpnessDEF - 3)) {
                                SharpnessLevel = 2;
                            }
                            else if (sharpness == (SharpnessDEF - 2)) {
                                SharpnessLevel = 3;
                            }
                            else if (sharpness == (SharpnessDEF - 1)) {
                                SharpnessLevel = 4;
                            }
                            else if (sharpness == (SharpnessDEF)) {
                                SharpnessLevel = 5;
                            }
                            else if (sharpness == (SharpnessDEF + 1)) {
                                SharpnessLevel = 6;
                            }
                            else if (sharpness == (SharpnessDEF + 2)) {
                                SharpnessLevel = 7;
                            }
                            else if (sharpness == (SharpnessDEF + 3)) {
                                SharpnessLevel = 8;
                            }
                            else if (sharpness == (SharpnessDEF + 4)) {
                                SharpnessLevel = 9;
                            }

                            binding.getRoot().post(() -> {
                                if (sharpness != null) {
                                    binding.cameraSettings.getBinding().sharpnessSeekBar.setProgress(SharpnessLevel);
                                }
                            });
                        } catch (Exception e) {
                            Timber.e(e);
                        }
                    }

                    if (binding != null) {
                        binding.cameraSettings.setListener(cameraSettingsViewListener);
                    }
                }
            });
        } catch (Exception e) {
            Timber.e(e);
        }
    }

    @NonNull
    private final CameraSettingsView.Listener cameraSettingsViewListener = new CameraSettingsView.Listener() {
        @Override
        public void onBrightnessChanged(int value, boolean fromUser) {
            final ICameraHelper iCameraHelper = getICameraHelper();
            if (iCameraHelper == null) {
                return;
            }

            switch (value) {
                case 1:
                    Brightval =  BrightnessDEF - 20;
                    break;

                case 2:
                    Brightval =  BrightnessDEF - 15;
                    break;

                case 3:
                    Brightval =  BrightnessDEF - 10;
                    break;

                case 4:
                    Brightval =  BrightnessDEF - 5;
                    break;

                case 5:
                    Brightval = BrightnessDEF;
                    break;

                case 6:
                    Brightval = BrightnessDEF + 5;
                    break;

                case 7:
                    Brightval = BrightnessDEF + 10;
                    break;

                case 8:
                    Brightval = BrightnessDEF + 15;
                    break;

                case 9:
                    Brightval =  BrightnessDEF + 20;
                    break;

            }

            if (fromUser) {
                try {
                    iCameraHelper.getUVCControl().setBrightness(Brightval);
                    Log.d("Jerry", "setBrightness mod" + Brightval);
                } catch (Exception e) {
                    Timber.e(e);
                }

//                try {
//                    scheduledExecutorService.execute(new Runnable() {
//                        @Override
//                        public void run() {
//                            final MyApplication myApplication = getMyApplication();
//                            if (myApplication == null) {
//                                return;
//                            }
//
//                            UsbDevice usbDeviceNow = getUsbDeviceNow();
//                            if (usbDeviceNow == null) {
//                                return;
//                            }
//
//                            UserTbData loginUserTbData = myApplication.getLoginUserTbData();
//                            if (loginUserTbData == null) {
//                                return;
//                            }
//
//                            Size previewSize = iCameraHelper.getPreviewSize();
//                            if (previewSize == null) {
//                                return;
//                            }
//
//                            boolean isAdd = false;
//                            ImageAdjustmentSettingTbData imageAdjustmentSettingData = null;
//
//                            try {
//                                imageAdjustmentSettingData = myApplication.getMyRoomDatabase().imageAdjustmentSettingTbDataDao().find(
//                                        String.valueOf(usbDeviceNow.getVendorId()),
//                                        loginUserTbData.getAccount(),
//                                        previewSize
//                                );
//                            } catch (Exception e) {
//                                Timber.e(e);
//                            }
//
//                            if (imageAdjustmentSettingData == null) {
//                                isAdd = true;
//                                imageAdjustmentSettingData = new ImageAdjustmentSettingTbData(
//                                        UUID.randomUUID().toString(),
//                                        String.valueOf(usbDeviceNow.getVendorId()),
//                                        loginUserTbData.getAccount(),
//                                        previewSize
//                                );
//                            }
//
//                            imageAdjustmentSettingData.setBrightness(Brightval);
//
//                            try {
//                                if (isAdd) {
//                                    myApplication.getMyRoomDatabase().imageAdjustmentSettingTbDataDao()
//                                            .insert(imageAdjustmentSettingData);
//                                } else {
//                                    myApplication.getMyRoomDatabase().imageAdjustmentSettingTbDataDao()
//                                            .update(imageAdjustmentSettingData);
//                                }
//                            } catch (Exception e) {
//                                Timber.e(e);
//                            }
//                        }
//                    });
//                } catch (Exception e) {
//                    Timber.e(e);
//                }
            }
        }


        @Override
        public void onContrastChanged(int value, boolean fromUser) {
            final ICameraHelper iCameraHelper = getICameraHelper();
            if (iCameraHelper == null) {
                return;
            }

            switch (value) {
                case 1:
                    Contrastval =  ContrastDEF - 4;
                    break;

                case 2:
                    Contrastval =  ContrastDEF - 3;
                    break;

                case 3:
                    Contrastval =  ContrastDEF - 2;
                    break;

                case 4:
                    Contrastval =  ContrastDEF - 1;
                    break;

                case 5:
                    Contrastval = ContrastDEF;
                    break;

                case 6:
                    Contrastval = ContrastDEF + 1;
                    break;

                case 7:
                    Contrastval = ContrastDEF + 2;
                    break;

                case 8:
                    Contrastval = ContrastDEF + 3;
                    break;

                case 9:
                    Contrastval =  ContrastDEF + 4;
                    break;

            }

            if (fromUser) {
                try {
                    iCameraHelper.getUVCControl().setContrast(Contrastval);
                    Log.d("Jerry", "Contrastval mod" + Contrastval);
                } catch (Exception e) {
                    Timber.e(e);
                }

//                try {
//                    scheduledExecutorService.execute(new Runnable() {
//                        @Override
//                        public void run() {
//                            MyApplication myApplication = getMyApplication();
//                            if (myApplication == null) {
//                                return;
//                            }
//
//                            UsbDevice usbDeviceNow = getUsbDeviceNow();
//                            if (usbDeviceNow == null) {
//                                return;
//                            }
//
//                            UserTbData loginUserTbData = myApplication.getLoginUserTbData();
//                            if (loginUserTbData == null) {
//                                return;
//                            }
//
//                            Size previewSize = iCameraHelper.getPreviewSize();
//                            if (previewSize == null) {
//                                return;
//                            }
//
//                            boolean isAdd = false;
//                            ImageAdjustmentSettingTbData imageAdjustmentSettingData = null;
//
//                            try {
//                                imageAdjustmentSettingData = myApplication.getMyRoomDatabase().imageAdjustmentSettingTbDataDao().find(
//                                        String.valueOf(usbDeviceNow.getVendorId()),
//                                        loginUserTbData.getAccount(),
//                                        previewSize
//                                );
//                            } catch (Exception e) {
//                                Timber.e(e);
//                            }
//
//                            if (imageAdjustmentSettingData == null) {
//                                isAdd = true;
//                                imageAdjustmentSettingData = new ImageAdjustmentSettingTbData(
//                                        UUID.randomUUID().toString(),
//                                        String.valueOf(usbDeviceNow.getVendorId()),
//                                        loginUserTbData.getAccount(),
//                                        previewSize
//                                );
//                            }
//
//                            imageAdjustmentSettingData.setContrast(Contrastval);
//
//                            try {
//                                if (isAdd) {
//                                    myApplication.getMyRoomDatabase().imageAdjustmentSettingTbDataDao()
//                                            .insert(imageAdjustmentSettingData);
//                                } else {
//                                    myApplication.getMyRoomDatabase().imageAdjustmentSettingTbDataDao()
//                                            .update(imageAdjustmentSettingData);
//                                }
//                            } catch (Exception e) {
//                                Timber.e(e);
//                            }
//                        }
//                    });
//                } catch (Exception e) {
//                    Timber.e(e);
//                }
            }
        }


        @Override
        public void onColourChanged(int value, boolean fromUser) {
            final ICameraHelper iCameraHelper = getICameraHelper();
            if (iCameraHelper == null) {
                return;
            }

            switch (value) {
                case 1:
                    Colorval = 2800;
                    break;

                case 2:
                    Colorval = 3200;
                    break;

                case 3:
                    Colorval = 3500;
                    break;

                case 4:
                    Colorval = 4000;
                    break;

                case 5:
                    Colorval = 4600;
                    break;

                case 6:
                    Colorval = 5000;
                    break;

                case 7:
                    Colorval = 5500;
                    break;

                case 8:
                    Colorval = 6000;
                    break;

                case 9:
                    Colorval = 6500;
                    break;

            }

            if (fromUser) {
                try {
                    iCameraHelper.getUVCControl().setWhiteBalance(Colorval);
                    Log.d("Jerry", "Colorval mod" + Colorval);
                } catch (Exception e) {
                    Timber.e(e);
                }

//                try {
//                    scheduledExecutorService.execute(new Runnable() {
//                        @Override
//                        public void run() {
//                            MyApplication myApplication = getMyApplication();
//                            if (myApplication == null) {
//                                return;
//                            }
//
//                            UsbDevice usbDeviceNow = getUsbDeviceNow();
//                            if (usbDeviceNow == null) {
//                                return;
//                            }
//
//                            UserTbData loginUserTbData = myApplication.getLoginUserTbData();
//                            if (loginUserTbData == null) {
//                                return;
//                            }
//
//                            Size previewSize = iCameraHelper.getPreviewSize();
//                            if (previewSize == null) {
//                                return;
//                            }
//
//                            boolean isAdd = false;
//                            ImageAdjustmentSettingTbData imageAdjustmentSettingData = null;
//
//                            try {
//                                imageAdjustmentSettingData = myApplication.getMyRoomDatabase().imageAdjustmentSettingTbDataDao().find(
//                                        String.valueOf(usbDeviceNow.getVendorId()),
//                                        loginUserTbData.getAccount(),
//                                        previewSize
//                                );
//                            } catch (Exception e) {
//                                Timber.e(e);
//                            }
//
//                            if (imageAdjustmentSettingData == null) {
//                                isAdd = true;
//                                imageAdjustmentSettingData = new ImageAdjustmentSettingTbData(
//                                        UUID.randomUUID().toString(),
//                                        String.valueOf(usbDeviceNow.getVendorId()),
//                                        loginUserTbData.getAccount(),
//                                        previewSize
//                                );
//                            }
//
//                            imageAdjustmentSettingData.setWhiteBalance(Colorval);
//
//                            try {
//                                if (isAdd) {
//                                    myApplication.getMyRoomDatabase().imageAdjustmentSettingTbDataDao()
//                                            .insert(imageAdjustmentSettingData);
//                                } else {
//                                    myApplication.getMyRoomDatabase().imageAdjustmentSettingTbDataDao()
//                                            .update(imageAdjustmentSettingData);
//                                }
//                            } catch (Exception e) {
//                                Timber.e(e);
//                            }
//                        }
//                    });
//                } catch (Exception e) {
//                    Timber.e(e);
//                }
            }
        }


        @Override
        public void onSharpnessChanged(int value, boolean fromUser) {
            final ICameraHelper iCameraHelper = getICameraHelper();
            if (iCameraHelper == null) {
                return;
            }

            switch (value) {
                case 1:
                    Sharpnessval = SharpnessDEF - 4;
                    break;

                case 2:
                    Sharpnessval = SharpnessDEF - 3;
                    break;

                case 3:
                    Sharpnessval = SharpnessDEF - 2;
                    break;

                case 4:
                    Sharpnessval = SharpnessDEF - 1;
                    break;

                case 5:
                    Sharpnessval = SharpnessDEF;
                    break;

                case 6:
                    Sharpnessval = SharpnessDEF + 1;
                    break;

                case 7:
                    Sharpnessval = SharpnessDEF + 2;
                    break;

                case 8:
                    Sharpnessval = SharpnessDEF + 3;
                    break;

                case 9:
                    Sharpnessval = SharpnessDEF + 4;
                    break;
            }

            if (fromUser) {
                try {
                    iCameraHelper.getUVCControl().setSharpness(Sharpnessval);
                    Log.d("Jerry", "Sharpnessval mod" + Sharpnessval);
                } catch (Exception e) {
                    Timber.e(e);
                }

//                try {
//                    scheduledExecutorService.execute(new Runnable() {
//                        @Override
//                        public void run() {
//                            MyApplication myApplication = getMyApplication();
//                            if (myApplication == null) {
//                                return;
//                            }
//
//                            UsbDevice usbDeviceNow = getUsbDeviceNow();
//                            if (usbDeviceNow == null) {
//                                return;
//                            }
//
//                            UserTbData loginUserTbData = myApplication.getLoginUserTbData();
//                            if (loginUserTbData == null) {
//                                return;
//                            }
//
//                            Size previewSize = iCameraHelper.getPreviewSize();
//                            if (previewSize == null) {
//                                return;
//                            }
//
//                            boolean isAdd = false;
//                            ImageAdjustmentSettingTbData imageAdjustmentSettingData = null;
//
//                            try {
//                                imageAdjustmentSettingData = myApplication.getMyRoomDatabase().imageAdjustmentSettingTbDataDao().find(
//                                        String.valueOf(usbDeviceNow.getVendorId()),
//                                        loginUserTbData.getAccount(),
//                                        previewSize
//                                );
//                            } catch (Exception e) {
//                                Timber.e(e);
//                            }
//
//                            if (imageAdjustmentSettingData == null) {
//                                isAdd = true;
//                                imageAdjustmentSettingData = new ImageAdjustmentSettingTbData(
//                                        UUID.randomUUID().toString(),
//                                        String.valueOf(usbDeviceNow.getVendorId()),
//                                        loginUserTbData.getAccount(),
//                                        previewSize
//                                );
//                            }
//
//                            imageAdjustmentSettingData.setSharpness(Sharpnessval);
//
//                            try {
//                                if (isAdd) {
//                                    myApplication.getMyRoomDatabase().imageAdjustmentSettingTbDataDao()
//                                            .insert(imageAdjustmentSettingData);
//                                } else {
//                                    myApplication.getMyRoomDatabase().imageAdjustmentSettingTbDataDao()
//                                            .update(imageAdjustmentSettingData);
//                                }
//                            } catch (Exception e) {
//                                Timber.e(e);
//                            }
//                        }
//                    });
//                } catch (Exception e) {
//                    Timber.e(e);
//                }
            }
        }

        @Override
        public void onReset() {
            final ICameraHelper iCameraHelper = getICameraHelper();
            if (iCameraHelper == null) {
                return;
            }
            try {
                // 重置亮度
                iCameraHelper.getUVCControl().resetBrightness();
            } catch (Exception e) {
                Timber.e(e);
            }

            try {
                // 重置對比度
                iCameraHelper.getUVCControl().resetContrast();
            } catch (Exception e) {
                Timber.e(e);
            }

            try {
                // 重置白平衡
                iCameraHelper.getUVCControl().resetWhiteBalance();
            } catch (Exception e) {
                Timber.e(e);
            }

            try {
                // 重置銳利度
                iCameraHelper.getUVCControl().resetSharpness();
            } catch (Exception e) {
                Timber.e(e);
            }

            setCameraSettingsView();

//            try {
//                scheduledExecutorService.execute(new Runnable() {
//                    @Override
//                    public void run() {
//                        MyApplication myApplication = getMyApplication();
//                        if (myApplication == null) {
//                            return;
//                        }
//
//                        UsbDevice usbDeviceNow = getUsbDeviceNow();
//                        if (usbDeviceNow == null) {
//                            return;
//                        }
//
//                        UserTbData loginUserTbData = myApplication.getLoginUserTbData();
//                        if (loginUserTbData == null) {
//                            return;
//                        }
//
//                        Size previewSize = iCameraHelper.getPreviewSize();
//                        if (previewSize == null) {
//                            return;
//                        }
//
//                        ImageAdjustmentSettingTbData imageAdjustmentSettingData = null;
//
//                        try {
//                            imageAdjustmentSettingData = myApplication.getMyRoomDatabase().imageAdjustmentSettingTbDataDao().find(
//                                    String.valueOf(usbDeviceNow.getVendorId()),
//                                    loginUserTbData.getAccount(),
//                                    previewSize
//                            );
//                        } catch (Exception e) {
//                            Timber.e(e);
//                        }
//
//                        try {
//                            if (imageAdjustmentSettingData != null) {
//                                myApplication.getMyRoomDatabase().imageAdjustmentSettingTbDataDao()
//                                        .delete(imageAdjustmentSettingData);
//                            }
//                        } catch (Exception e) {
//                            Timber.e(e);
//                        }
//                    }
//                });
//            } catch (Exception e) {
//                Timber.e(e);
//            }
        }
    };


    private void showCameraSettings(boolean isShow) {
        final Runnable r = new Runnable() {
            @Override
            public void run() {
                final FragmentCameraBinding binding = getBinding();
                if (binding != null) {
                    if (isShow) {
                        binding.cameraSettings.setVisibility(View.VISIBLE);
                    } else {
                        binding.cameraSettings.setVisibility(View.GONE);
                    }
                }
            }
        };

        if (Looper.myLooper() == Looper.getMainLooper()) {
            r.run();
        } else {
            final FragmentCameraBinding binding = getBinding();
            if (binding != null) {
                binding.getRoot().post(r);
            }
        }
    }


    public synchronized void changeCamera() {
        Context context = getContext();
        if (context == null) {
            return;
        }

        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
        ) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        final ICameraHelper iCameraHelper = getICameraHelper();
        if (iCameraHelper == null) {
            return;
        }

        setChangeCameraEnable(false);
        mainHandler.removeCallbacks(changeCameraTimeout);
        mainHandler.postDelayed(changeCameraTimeout, SWITCH_CHANNEL_TIMEOUT_MILLIS);
        mIsChangeCameraing=true;
        //You can't switch pages until the change camera is finished.
        FragmentActivity activity = getActivity();
        if (activity instanceof MainActivity) {
            MainActivity mainActivity = (MainActivity) activity;
            mainActivity.setTabBtnClickable(false);
        }

        UsbDevice usbDeviceNow = getUsbDeviceNow();
        UsbDevice usbDeviceCh1 = getUsbDeviceCh1();
        UsbDevice usbDeviceCh2 = getUsbDeviceCh2();

        UsbDevice nextDevice = null;

        if (usbDeviceNow == null) {
            nextDevice = (usbDeviceCh1 != null) ? usbDeviceCh1 : usbDeviceCh2;
        } else {
            if (usbDeviceCh1 != null && !usbDeviceNow.getDeviceName().equals(usbDeviceCh1.getDeviceName())) {
                nextDevice = usbDeviceCh1;
            } else if (usbDeviceCh2 != null && !usbDeviceNow.getDeviceName().equals(usbDeviceCh2.getDeviceName())) {
                nextDevice = usbDeviceCh2;
            }
        }

        if (nextDevice == null || (usbDeviceNow != null && nextDevice.getDeviceName().equals(usbDeviceNow.getDeviceName()))) {
            setChangeCameraEnable(true);
            mainHandler.removeCallbacks(changeCameraTimeout);
            mIsChangeCameraing=false;
            if (activity instanceof MainActivity) {
                MainActivity mainActivity = (MainActivity) activity;
                mainActivity.setTabBtnClickable(true);
            }
            return;
        }

        closeCamera();

        if (mIsCameraConnected) {
            iCameraHelper.closeCamera();
        }

        setUsbDeviceNow(nextDevice);

        selectDevice(nextDevice);
    }


    private void showHistoryImageListDialog() {
        cancelHistoryImageListDialog();
        final Runnable r = new Runnable() {
            @Override
            public void run() {
                final HistoryImageListDialog dialog = HistoryImageListDialog.newInstance();
                dialog.setListener(new HistoryImageListDialog.Listener() {
                    @Override
                    public void onTouch() {
                        Context context = getContext();
                        if (context == null) {
                            return;
                        }
                        MyApplication myApplication = (MyApplication) context.getApplicationContext();
                        MainActivity mainActivity = myApplication.getMainActivity();
                        if (mainActivity != null) {
                            mainActivity.restartStandbyNotificationTimeServiceTimer();
                        }
                    }

                    @Override
                    public void onSelectedFile(@NonNull File file) {
                        final FragmentCameraBinding binding = getBinding();
                        if (binding == null) {
                            return;
                        }
                        boolean isLock = binding.previewWindowsView.getBinding().lock.isSelected();
                        if (isLock) {
                            binding.previewWindowsView.setTempUnLockImgFile(true);
                        }
                        binding.previewWindowsView.setSelectImgFile(file);
                        if (isLock) {
                            binding.previewWindowsView.setTempUnLockImgFile(false);
                        }
                    }
                });
                dialog.show(getChildFragmentManager(), HistoryImageListDialog.class.getSimpleName());
                historyImageListDialog = dialog;
            }
        };
        if (Looper.myLooper() == Looper.getMainLooper()) {
            r.run();
        } else {
            final FragmentCameraBinding binding = getBinding();
            if (binding != null) {
                binding.getRoot().post(r);
            }
        }
    }


    private void cancelHistoryImageListDialog() {
        final Runnable r = new Runnable() {
            @Override
            public void run() {
                final HistoryImageListDialog dialog = historyImageListDialog;
                if (dialog != null && dialog.isVisible()) {
                    if (dialog.isStateSaved()) {
                        dialog.dismissAllowingStateLoss();
                    } else {
                        dialog.dismiss();
                    }
                }
                historyImageListDialog = null;
            }
        };
        if (Looper.myLooper() == Looper.getMainLooper()) {
            r.run();
        } else {
            final FragmentCameraBinding binding = getBinding();
            if (binding != null) {
                binding.getRoot().post(r);
            }
        }
    }


    private void setUsbDeviceChToView() {
        final UsbDevice usbDeviceNow = getUsbDeviceNow();
        final UsbDevice usbDeviceCh1 = getUsbDeviceCh1();
        final UsbDevice usbDeviceCh2 = getUsbDeviceCh2();
        final FragmentActivity activity = getActivity();
        if(activity==null){
            Timber.w("[setUsbDeviceChToView] activity==null");
            return;
        }

        MainActivity mainActivity = (MainActivity) activity;
        if (usbDeviceCh1 != null && usbDeviceNow != null && TextUtils.equals(usbDeviceNow.getDeviceName(), usbDeviceCh1.getDeviceName())) {
            mainActivity.setUsbDeviceChToView(1);
        } else if (usbDeviceCh2 != null && usbDeviceNow != null && TextUtils.equals(usbDeviceNow.getDeviceName(), usbDeviceCh2.getDeviceName())) {
            mainActivity.setUsbDeviceChToView(2);
        } else {
            mainActivity.setUsbDeviceChToView(3);
        }
    }


    @UiThread
    private void showPatientIdDialog(@Nullable final Runnable saveRunnable, @Nullable final Runnable cancelRunnable) {
        cancelPatientIdDialog();
        MyApplication myApplication = getMyApplication();
        if (myApplication == null) {
            return;
        }
        String patientId = myApplication.getPatientId();
        if (patientId == null) {
            patientId = "";
        }
        final PatientIdDialog d = PatientIdDialog.newInstance(patientId);
        d.setListener(new PatientIdDialog.Listener() {
            @Override
            public void onClickSave(@NonNull String patientId) {
                myApplication.setPatientId(patientId);
                if (saveRunnable != null) {
                    saveRunnable.run();
                }
            }

            @Override
            public void onClickCancel() {
                if (cancelRunnable != null) {
                    cancelRunnable.run();
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
        d.show(getChildFragmentManager(), PatientIdDialog.class.getSimpleName());
        patientIdDialog = d;
    }

    @UiThread
    private void cancelPatientIdDialog() {
        final PatientIdDialog patientIdDialog = this.getPatientIdDialog();
        if (patientIdDialog != null && patientIdDialog.isVisible()) {
            if (patientIdDialog.isStateSaved()) {
                patientIdDialog.dismissAllowingStateLoss();
            } else {
                patientIdDialog.dismiss();
            }
        }
        this.patientIdDialog = null;
    }


    public void setLoginUserRoleTypeToView(@Nullable final UserTbData oldUserTbData) {
        final Runnable r = new Runnable() {
            @Override
            public void run() {
                ICameraHelper iCameraHelper = getICameraHelper();
                setRecordEnable(iCameraHelper != null && iCameraHelper.isCameraOpened());
                setSnapshotEnable(iCameraHelper != null && iCameraHelper.isCameraOpened());

                MyApplication myApplication = getMyApplication();
                UserTbData loginUserTbData = null;
                if (myApplication != null) {
                    loginUserTbData = myApplication.getLoginUserTbData();
                }
                UserRoleType roleType = loginUserTbData != null ? loginUserTbData.getRoleType() : null;

                final FragmentCameraBinding binding = getBinding();

                if (roleType == null || roleType == UserRoleType.GUEST) {
                    if (binding != null) {
                        binding.previewWindowsView.setVisibility(View.GONE);
                        if (binding.previewWindowsView.getBinding().full.isSelected()) {
                            binding.previewWindowsView.getBinding().full.performClick();
                        }
                        if (binding.previewWindowsView.getBinding().lock.isSelected()) {
                            binding.previewWindowsView.getBinding().lock.setSelected(false);
                        }
                        if (binding.previewWindowsView.getNewImgFile() != null) {
                            binding.previewWindowsView.setNewImgFile(null);
                        }
                        binding.btnCompare.setVisibility(View.GONE);
                    }
                } else {
                    if (binding != null &&
                            binding.previewWindowsView.getVisibility() == View.GONE &&
                            binding.btnCompare.getVisibility() == View.GONE) {
                        binding.btnCompare.setVisibility(View.VISIBLE);
                    }
                }

                if (loginUserTbData != null && !TextUtils.equals(loginUserTbData.getAccount(), oldUserTbData != null ? oldUserTbData.getAccount() : null)) {
                    showCameraSettings(false);
                }
            }
        };

        if (Looper.myLooper() == Looper.getMainLooper()) {
            r.run();
        } else {
            final FragmentCameraBinding binding = getBinding();
            if (binding != null) {
                binding.getRoot().post(r);
            }
        }
    }


    public void setPatientIdToView() {
        MyApplication myApplication = getMyApplication();

        if (myApplication == null) {
            return;
        }

        String patientId = myApplication.getPatientId();
        final Runnable r = new Runnable() {
            @Override
            public void run() {
                final FragmentCameraBinding binding = getBinding();
                if (binding == null) {
                    return;
                }

                binding.patientId.setText(patientId);
            }
        };

        if (Looper.myLooper() == Looper.getMainLooper()) {
            r.run();
        } else {
            final FragmentCameraBinding binding = getBinding();
            if (binding != null) {
                binding.getRoot().post(r);
            }
        }
    }


    public void setImageRotateTypeToRotation() {

        try {
            scheduledExecutorService.execute(new Runnable() {
                @Override
                public void run() {
                    ImageRotateType imageRotateType = SharedPreferencesManager.getInstance().getImageRotateType();
                    if (imageRotateType == null) {
                        imageRotateType = ImageRotateType.NORMAL;
                    }
                    int rotation;
                    switch (imageRotateType) {
                        case NORMAL:
                            rotation = Surface.ROTATION_0;
                            break;
                        case ANGLE_90:
                            rotation = Surface.ROTATION_90;
                            break;
                        case ANGLE_180:
                            rotation = Surface.ROTATION_180;
                            break;
                        case ANGLE_270:
                            rotation = Surface.ROTATION_270;
                            break;
                        default:
                            rotation = Surface.ROTATION_0;
                    }
                    setRotation(rotation);
                }
            });
        } catch (Exception e) {
            Timber.e(e);
        }
    }


    /**
     * 檢查患者id變更後，是否要用這患者最新的圖
     */
    public void checkPatientIdToChangePreviewImg(
            @Nullable final String nowPatientId,
            @Nullable final String oldPatientId,
            final boolean isDeleteFile) {
        Timber.d("checkPatientIdToChangePreviewImg nowPatientId=" + nowPatientId + " | oldPatientId=" + oldPatientId);
        final FragmentCameraBinding binding = getBinding();
        if (TextUtils.isEmpty(nowPatientId) && binding != null) {
            binding.getRoot().post(new Runnable() {
                @Override
                public void run() {
                    if (binding.previewWindowsView.getBinding().full.isSelected()) {
                        binding.previewWindowsView.getBinding().full.performClick();
                    }
                    if (binding.previewWindowsView.getBinding().lock.isSelected()) {
                        binding.previewWindowsView.getBinding().lock.setSelected(false);
                    }
                    if (binding.previewWindowsView.getNewImgFile() != null) {
                        binding.previewWindowsView.setNewImgFile(null);
                    }
                }
            });
            return;
        }


        if (!isDeleteFile) {
            if (TextUtils.equals(nowPatientId, oldPatientId)) {
                return;
            }
        }
        Timber.d("checkPatientIdToChangePreviewImg 01");

        try {
            scheduledExecutorService.execute(new Runnable() {
                @Override
                public void run() {
                    final File firstImgFile = binding != null ? binding.previewWindowsView.getPatientIdLatestImgFile(nowPatientId) : null;
                    if (binding != null) {
                        binding.getRoot().post(new Runnable() {
                            @Override
                            public void run() {
                                binding.previewWindowsView.setTempUnLockImgFile(true);
                                binding.previewWindowsView.setNewImgFile(firstImgFile);
                                binding.previewWindowsView.setTempUnLockImgFile(false);
                            }
                        });
                    }
                }
            });
        } catch (Exception e) {
            Timber.e(e);
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

    private int GetlifeTime() {
        int CountTime = -1;
        int Lifetime = 240;
        if ((iCameraHelper != null && iCameraHelper.isCameraOpened())) {
            final UVCControl uvcControl = iCameraHelper.getUVCControl();
            if(uvcControl!=null) {
                CountTime = uvcControl.getGamma();
                Timber.d("Jerry :CountTime=" + CountTime);
            }
        }
        if (CountTime >= 0) {
            Lifetime = Lifetime - CountTime;
        }
        return Lifetime;
    }

    private Runnable endoscopyRemainingTimeRunnable = new Runnable() {
        @Override
        public void run() {
            int life = GetlifeTime();
            Timber.d("[endoscopyRemainingTimeRunnable] life="+life);
            final Runnable r = new Runnable(){
                @Override
                public void run() {
                    if(life<=0){
                        binding.scopeTime.setImageResource(R.drawable.scope_out_of_time);
                        AnimationUtils.flicker(binding.scopeTime);
                    }else if (life<=10){
                        binding.scopeTime.setImageResource(R.drawable.scope_10min);
                    }else if (life<=30){
                        binding.scopeTime.setImageResource(R.drawable.scope_30min);
                    }else {
                        binding.scopeTime.setImageResource(R.drawable.scope_4hr);
                    }
                }
            };

            if (Looper.getMainLooper() == Looper.myLooper()) {
                r.run();
            } else {
                mainHandler.post(r);
            }

            if(life<=0){
            }else if (life<=10){
                if(life==1){
                    endoscopyRemainingTimeSchedule = scheduledExecutorService.schedule(endoscopyRemainingTimeRunnable, 1000L, TimeUnit.MILLISECONDS);
                }else {
                    endoscopyRemainingTimeSchedule = scheduledExecutorService.schedule(endoscopyRemainingTimeRunnable, life * 30 * 1000L, TimeUnit.MILLISECONDS);
                }
            }else if (life<=30){
                endoscopyRemainingTimeSchedule = scheduledExecutorService.schedule(endoscopyRemainingTimeRunnable, (life-10)*30 * 1000L, TimeUnit.MILLISECONDS);
            }else {
                endoscopyRemainingTimeSchedule = scheduledExecutorService.schedule(endoscopyRemainingTimeRunnable, (life-30)*30 * 1000L, TimeUnit.MILLISECONDS);
            }
        }
    };

    @NonNull
    private final Runnable showE2000Runnable = new Runnable() {
        @Override
        public void run() {
            IErrorCode.showErrorCode(getActivity(), Error.NO_SIGNAL_WHEN_ENDOSCOPE_INSERTED);
            Error.NO_SIGNAL_WHEN_ENDOSCOPE_INSERTED.enable=false;
        }
    };

    public void closeCamera(){
        if(checkButtonHandle!=null){
            checkButtonHandle.cancel(true);
            checkButtonHandle=null;
        }

        if(endoscopyRemainingTimeSchedule!=null){
            endoscopyRemainingTimeSchedule.cancel(true);
            endoscopyRemainingTimeSchedule=null;
        }

        flag_CameraOpen = false;
    }

    private Runnable changeCameraTimeout = new Runnable() {
        @Override
        public void run() {
            final FragmentActivity activity = getActivity();
            if(activity==null){
                Timber.w("[changeCameraTimeout] activity==null");
                return;
            }
            MainActivity mainActivity=(MainActivity) activity;
            ActivityMainBinding activityBinding = mainActivity.getBinding();
            final boolean enabled = activityBinding.changeCamera.isEnabled();
            if(enabled==false){
                Timber.d("[changeCameraTimeout] changeCamera is disabled.");
                checkCameraHelper();
                if (mIsCameraConnected) {
                    flag_CameraOpen = false;
                    iCameraHelper.closeCamera();
                }
                setChangeCameraEnable(true);
                mIsChangeCameraing=false;
                ((MainActivity) activity).setTabBtnClickable(true);
            }
        }
    };


    private void setChangeCameraEnable(final boolean isEnable) {
        final FragmentActivity activity = getActivity();
        if(activity==null){
            Timber.w("[showVideoRecordDot] activity==null");
            return;
        }
        ((MainActivity) activity).setChangeCameraEnable(isEnable);
    }

    public static class AnimationUtils {
        /**
         * 控件闪动
         * @param view
         */
        public static void flicker(View view){
            AlphaAnimation alphaAnimation = new AlphaAnimation(0f, 1.0f);
            alphaAnimation.setDuration(1000);
            alphaAnimation.setInterpolator(new LinearInterpolator());
            alphaAnimation.setRepeatCount(Animation.INFINITE);
            alphaAnimation.setRepeatMode(Animation.REVERSE);
            view.startAnimation(alphaAnimation);
        }
        public static void clearAnimation(View view){
            view.clearAnimation();
        }
    }

    private boolean testShowError2000=false;
    @VisibleForTesting
    public void setTestShowError2000(boolean b){
        testShowError2000=b;

        final UsbDevice usbDeviceNow = getUsbDeviceNow();
        closeCamera();
        if (mIsCameraConnected) {
            iCameraHelper.closeCamera();
        }
        selectDevice(usbDeviceNow);
    }
}
