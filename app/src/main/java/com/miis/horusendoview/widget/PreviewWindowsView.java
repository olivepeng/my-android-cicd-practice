package com.miis.horusendoview.widget;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.provider.Settings;
import android.text.format.DateFormat;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import androidx.appcompat.widget.LinearLayoutCompat;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.exifinterface.media.ExifInterface;

import com.bumptech.glide.Glide;
import com.google.common.collect.Iterables;
import com.miis.horusendoview.MyApplication;
import com.miis.horusendoview.R;
import com.miis.horusendoview.activity.MainActivity;
import com.miis.horusendoview.databinding.PreviewWindowsViewBinding;
import com.miis.horusendoview.fragment.CameraFragment;
import com.miis.horusendoview.roomDataBase.userTbData.UserTbData;
import com.miis.horusendoview.type.UserRoleType;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.ObservableEmitter;
import io.reactivex.rxjava3.core.ObservableOnSubscribe;
import io.reactivex.rxjava3.core.Observer;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import timber.log.Timber;

public class PreviewWindowsView extends LinearLayoutCompat {

    @NotNull
    private final PreviewWindowsViewBinding binding;

    private final Handler workHandler;

    /**
     * 現在顯示的圖
     */
    @Nullable
    private File selectImgFile;
    private final Object selectImgFileLock = new Object();

    /**
     * 暫時解鎖 現在顯示的圖
     */
    private boolean isTempUnLockImgFile;
    private final Object isTempUnLockImgFileLock = new Object();

    /**
     * 最新的圖
     */
    @Nullable
    private File newImgFile;
    private final Object newImgFileLock = new Object();

    @Nullable
    private Listener listener;

    public PreviewWindowsView(@NonNull Context context) {
        this(context, null);
    }

    public PreviewWindowsView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PreviewWindowsView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.binding = PreviewWindowsViewBinding.inflate(LayoutInflater.from(context),
                this,
                true);
        HandlerThread workHandlerThread = new HandlerThread(PreviewWindowsView.class.getSimpleName() + "WorkHandlerThread");
        workHandlerThread.start();
        workHandler = new Handler(workHandlerThread.getLooper());

        init(context);
    }

    @NotNull
    public PreviewWindowsViewBinding getBinding() {
        return this.binding;
    }

    @Nullable
    public File getSelectImgFile() {
        synchronized (selectImgFileLock) {
            return selectImgFile;
        }
    }

    public void setSelectImgFile(@Nullable File selectImgFile) {
        synchronized (selectImgFileLock) {
            if (!binding.lock.isSelected() || isTempUnLockImgFile()) {
                this.selectImgFile = selectImgFile;

                binding.getRoot().post(new Runnable() {
                    @Override
                    public void run() {
                        setImgFileToView();
                    }
                });
            }
        }
    }

    public boolean isTempUnLockImgFile() {
        synchronized (isTempUnLockImgFileLock) {
            return isTempUnLockImgFile;
        }
    }

    public void setTempUnLockImgFile(boolean value) {
        synchronized (isTempUnLockImgFileLock) {
            isTempUnLockImgFile = value;
        }
    }

    @Nullable
    public File getNewImgFile() {
        synchronized (newImgFileLock) {
            return newImgFile;
        }
    }

    public void setNewImgFile(@Nullable File value) {
        synchronized (newImgFileLock) {
            newImgFile = value;
            setSelectImgFile(value);
        }
    }

    public interface Listener {
        void onClose();

        void onFull(boolean var1);

        void onClickHistory();
    }


    private void init(Context context) {

        binding.close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setVisibility(View.GONE);

                final Listener listener = getListener();
                if (listener != null) {
                    listener.onClose();
                }
                if (binding.full.isSelected()) {
                    binding.full.performClick();
                }
            }
        });

        binding.lock.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                binding.lock.setSelected(!binding.lock.isSelected());

                if (!binding.lock.isSelected()) {
                    final File newImgFile = getNewImgFile();
                    if (newImgFile != null) {
                        setSelectImgFile(newImgFile);
                    }
                }
            }
        });

        binding.history.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final Listener listener = getListener();
                if (listener != null) {
                    listener.onClickHistory();
                }
            }
        });

        binding.full.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                binding.full.setSelected(!binding.full.isSelected());

                if (binding.full.isSelected()) {
                    binding.fullImg.setVisibility(View.VISIBLE);
                    binding.detailLayout.setVisibility(View.GONE);

                    @Nullable File file = getSelectImgFile();
                    try {
                        Glide.with(context).clear(binding.fullImg);
                    } catch (Exception e) {
                        Timber.e(e);
                    }
                    binding.fullImg.setImageDrawable(null);
                    Glide.with(context).load(file != null ? file.getAbsoluteFile() : null).into(binding.fullImg);
                } else {
                    binding.fullImg.setVisibility(View.GONE);
                    binding.detailLayout.setVisibility(View.VISIBLE);
                }

                final Listener listener = getListener();
                if (listener != null) {
                    listener.onFull(binding.full.isSelected());
                }

                ConstraintSet constraintSet = new ConstraintSet();
                constraintSet.clone(binding.layout);
                if (binding.full.isSelected()) {
                    constraintSet.clear(R.id.buttonLayout, ConstraintSet.TOP);
                    constraintSet.connect(R.id.buttonLayout, ConstraintSet.TOP, R.id.fullImg, ConstraintSet.BOTTOM);
                    constraintSet.connect(R.id.buttonLayout, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START);
                    constraintSet.connect(R.id.buttonLayout, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END);

                } else {
                    constraintSet.clear(R.id.buttonLayout, ConstraintSet.TOP);
                    constraintSet.connect(R.id.buttonLayout, ConstraintSet.TOP, R.id.detailLayout, ConstraintSet.BOTTOM);
                    constraintSet.connect(R.id.buttonLayout, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START);
                    constraintSet.connect(R.id.buttonLayout, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END);
                }

                constraintSet.applyTo(binding.layout);
                binding.layout.requestLayout();  // 強制更新 Layout
            }
        });

    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        Context context = getContext();
        if (context == null) {
            return;
        }
        try {
            context.getContentResolver().registerContentObserver(
                    Settings.System.getUriFor(Settings.System.TIME_12_24),
                    true,
                    timeFormatChangeObserver
            );
        } catch (Exception e) {
            Timber.e(e);
        }

        if (getSelectImgFile() == null) {
            workHandler.post(new Runnable() {
                @Override
                public void run() {
                    setNewImgFile(getLastImgFile());
                }
            });
        } else {
            setImgFileToView();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        Context context = getContext();
        if (context == null) {
            return;
        }else if (context instanceof Activity && ((Activity) context).isDestroyed()) {
            Timber.w("[onDetachedFromWindow] Activity destroyed, skip Glide loading");
            return;
        }

        try {
            context.getContentResolver().unregisterContentObserver(timeFormatChangeObserver);
        } catch (Exception e) {
            Timber.e(e);
        }
        try {
            Glide.with(context).clear(binding.img);
            Glide.with(context).clear(binding.fullImg);
        } catch (Exception e) {
            Timber.e(e);
        }
    }


    @Nullable
    private synchronized File getLastImgFile() {
        Context context = getContext();
        if (context == null) {
            return null;
        }
        MyApplication myApplication = (MyApplication) context.getApplicationContext();

        if (myApplication == null) {
            return null;
        }

        final File imgDir = new File(myApplication.getMainDirPath());
        File[] imgFiles = null;
        try {
            imgFiles = imgDir.listFiles();
        } catch (Exception e) {
            Timber.e(e);
        }
        if (imgFiles != null) {
            List<File> imgFileList = Arrays.asList(imgFiles);
            imgFileList = imgFileList.stream()
                    .filter(file -> file.getName().contains(MyApplication.IMG_FILE_EXTENSION))
                    .collect(Collectors.toList());

            if (!imgFileList.isEmpty()) {
                return imgFileList.get(imgFileList.size() - 1);
            }
        }
        return null;
    }


    @SuppressLint({"RestrictedApi"})
    @UiThread
    private void setImgFileToView() {
        final Context context = getContext();
        if (context == null) {
            return;
        }

        @Nullable final File file = getSelectImgFile();
        Timber.d("setImgFileToView file=" + file);
        @Nullable String fileName = null;
        if (file != null) {
            fileName = file.getName();
        }
        ExifInterface exif;
        if (file != null && file.isFile() && file.exists()) {
            try {
                exif = new ExifInterface(file.getAbsoluteFile());
            } catch (Exception e) {
                Timber.e(e);
                exif = null;
            }

        } else {
            exif = null;
        }

        LocalDateTime fileDateTime;
        try {
            Long dateTime = exif != null ? exif.getDateTime() : null;
            fileDateTime = dateTime != null ? Instant.ofEpochMilli(dateTime).atZone(ZoneOffset.systemDefault()).toLocalDateTime() : null;
        } catch (Exception e) {
            Timber.e(e);
            fileDateTime = null;
        }


// 处理 fileName
        String fileNameText = (fileName != null && !fileName.isEmpty()) ? fileName : "-";
        binding.fileName.setText(fileNameText);

// 处理 timeStr
        String timeStr = null;
        if (fileDateTime != null) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(
                    DateFormat.is24HourFormat(context)
                            ? "yyyy-MM-dd HH:mm:ss.SSS"
                            : "yyyy-MM-dd hh:mm:ss.SSS a"
            ).withLocale(Locale.ENGLISH);
            timeStr = formatter.format(fileDateTime);
        }
        String timeText = (timeStr != null && !timeStr.isEmpty()) ? timeStr : "-";
        binding.time.setText(timeText);

        // 清除 binding.img 上的 Glide 图片加载
        try {
            Glide.with(context).clear(binding.img);
        } catch (Exception e) {
            Timber.e(e);
        }

// 设置 binding.img 为空
        binding.img.setImageDrawable(null);

// 如果文件存在，加载文件到 binding.img
        if (file != null && file.isFile() && file.exists()) {
            Glide.with(context).load(file.getAbsoluteFile()).into(binding.img);
        }

// 检查 binding.fullImg 是否可见
        if (binding.fullImg.getVisibility() == View.VISIBLE) {
            // 清除 binding.fullImg 上的 Glide 图片加载
            try {
                Glide.with(context).clear(binding.fullImg);
            } catch (Exception e) {
                Timber.e(e);
            }

            // 设置 binding.fullImg 为空
            binding.fullImg.setImageDrawable(null);

            // 如果文件存在，加载文件到 binding.fullImg
            if (file != null && file.isFile() && file.exists()) {
                Glide.with(context).load(file.getAbsoluteFile()).into(binding.fullImg);
            }
        }
    }

    private final ContentObserver timeFormatChangeObserver = new ContentObserver(getHandler()) {
        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            setImgFileToView();
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            super.onChange(selfChange, uri);
            setImgFileToView();
        }
    };

    public void checkSelectImgFileExists() {
        Runnable r = new Runnable() {
            @Override
            public void run() {
                Context context = getContext();
                if (context == null) {
                    return;
                }
                File selectImgFile = getSelectImgFile();
                if (selectImgFile == null) {
                    return;
                }

                try {
                    if (selectImgFile.exists()) {
                        return;
                    }
                } catch (Exception e) {
                    Timber.e(e);
                }

                setTempUnLockImgFile(true);
                setSelectImgFile(null);
                setTempUnLockImgFile(false);

                MyApplication myApplication = (MyApplication) context.getApplicationContext();
                if (myApplication != null) {
                    MainActivity mainActivity = myApplication.getMainActivity();
                    if (mainActivity != null) {
                        CameraFragment cameraFragment = mainActivity.getCameraFragment();


                        if (cameraFragment != null) {

                            Observable.create(new ObservableOnSubscribe<File>() {
                                        @Override
                                        public void subscribe(@io.reactivex.rxjava3.annotations.NonNull ObservableEmitter<File> emitter) throws Throwable {
                                            File firstImgFile = getPatientIdLatestImgFile(myApplication.getPatientId());

                                            emitter.onNext(firstImgFile);
                                            emitter.onComplete();
                                        }
                                    })
                                    .subscribeOn(Schedulers.io())
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .subscribe(new Observer<File>() {
                                        @Override
                                        public void onSubscribe(@io.reactivex.rxjava3.annotations.NonNull Disposable d) {

                                        }

                                        @Override
                                        public void onNext(File file) {
                                            setTempUnLockImgFile(true);

                                            setNewImgFile(file);

                                            setTempUnLockImgFile(false);
                                        }

                                        @Override
                                        public void onError(Throwable t) {

                                        }

                                        @Override
                                        public void onComplete() {

                                        }
                                    });
                        }
                    }
                }
            }
        };

        if (Looper.getMainLooper() == Looper.myLooper()) {
            r.run();
        } else {
            binding.getRoot().post(r);
        }
    }


    @Nullable
    public File getPatientIdLatestImgFile(@Nullable final String nowPatientId) {
        final MyApplication myApplication = (MyApplication) getContext().getApplicationContext();
        UserTbData loginUserTbData = myApplication.getLoginUserTbData();
        if (loginUserTbData == null) {
            return null;
        }
        UserRoleType roleType = loginUserTbData.getRoleType();
        if (roleType == null) {
            roleType = UserRoleType.GUEST;
        }
        @NonNull final File mainDirFile = new File(myApplication.getMainDirPath());
        List<File> imgFileList = new ArrayList<>();

        List<File> childDirFileList = null;
        try {
            final UserRoleType finalRoleType = roleType;
            @Nullable File[] childDirFileArr = null;
            try {
                childDirFileArr = mainDirFile.listFiles(childDirFile -> {
                    if (childDirFile.isDirectory()) {
                        List<String> childDirNameList = Arrays.asList(childDirFile.getName().split("_"));
                        if (childDirNameList.size() >= 3) {
                            switch (finalRoleType) {
                                case GUEST:
                                    return false;
                                case ADVANCED_USER:
                                    return childDirNameList.get(0).equals(loginUserTbData.getAccount())
                                            && childDirNameList.get(1).equals(nowPatientId);
                                case ADMIN_USER:
                                case SERVICE_USER:
                                    return childDirNameList.get(1).equals(nowPatientId);
                                default:
                                    return false;
                            }
                        } else {
                            return false;
                        }
                    } else {
                        return false;
                    }
                });
            } catch (Exception e) {
                Timber.e(e);
            }
            if (childDirFileArr == null) {
                childDirFileArr = new File[]{};
            }
            childDirFileList = Arrays.asList(childDirFileArr);
        } catch (Exception e) {
            Timber.e(e);
        }

        if (childDirFileList == null) {
            childDirFileList = Collections.emptyList();
        }

        for (File childDirFile : childDirFileList) {
            List<File> jpgFileList = null;
            try {
                @Nullable File[] jpgFileArr = null;
                try {
                    jpgFileArr = childDirFile.listFiles(childFile -> {
                        if (childFile.isFile() &&
                                childFile.getName().contains(MyApplication.IMG_FILE_EXTENSION)) {
                            List<String> childFileNameList = Arrays.asList(childFile.getName().replace(MyApplication.IMG_FILE_EXTENSION, "").split("_"));
                            if (childFileNameList.size() >= 3) {
                                return childFileNameList.get(0).equals(nowPatientId);
                            } else {
                                return false;
                            }
                        } else {
                            return false;
                        }
                    });
                } catch (Exception e) {
                    Timber.e(e);
                }
                if (jpgFileArr == null) {
                    jpgFileArr = new File[]{};
                }
                jpgFileList = Arrays.asList(jpgFileArr);
            } catch (Exception e) {
                Timber.e(e);
            }

            if (jpgFileList == null) {
                jpgFileList = Collections.emptyList();
            }

            imgFileList.addAll(jpgFileList);
        }

        imgFileList.sort(new Comparator<File>() {
            @Override
            public int compare(File file1, File file2) {
                final String nameNoExtension1 = file1.getName().replace(MyApplication.IMG_FILE_EXTENSION, "");
                final String nameNoExtension2 = file2.getName().replace(MyApplication.IMG_FILE_EXTENSION, "");
                final List<String> nameArr1 = Arrays.asList(nameNoExtension1.split("_"));
                final List<String> nameArr2 = Arrays.asList(nameNoExtension2.split("_"));
                final String dateTimeStr1 = nameArr1.get(1) + "_" + nameArr1.get(2);
                final String dateTimeStr2 = nameArr2.get(1) + "_" + nameArr2.get(2);
                @Nullable LocalDateTime dateTime1 = null;
                @Nullable LocalDateTime dateTime2 = null;
                try {
                    dateTime1 = LocalDateTime.parse(dateTimeStr1, DateTimeFormatter.ofPattern(CameraFragment.IMG_SIMPLE_DATE_FORMAT_PATTERN, Locale.ENGLISH));
                } catch (Exception e) {
                    try {
                        final BasicFileAttributes basicFileAttributes1 = Files.readAttributes(file1.toPath(), BasicFileAttributes.class);
                        dateTime1 = basicFileAttributes1.creationTime().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
                    } catch (Exception ex) {
                        Timber.e(e);
                    }
                }

                try {
                    dateTime2 = LocalDateTime.parse(dateTimeStr2, DateTimeFormatter.ofPattern(CameraFragment.IMG_SIMPLE_DATE_FORMAT_PATTERN, Locale.ENGLISH));
                } catch (Exception e) {
                    try {
                        BasicFileAttributes basicFileAttributes2 = Files.readAttributes(file2.toPath(), BasicFileAttributes.class);
                        dateTime2 = basicFileAttributes2.creationTime().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
                    } catch (IOException ex) {
                        Timber.e(e);
                    }
                }
                if (dateTime1 != null && dateTime2 != null) {
                    return dateTime2.compareTo(dateTime1);
                }
                return 0;
            }
        }.reversed());

        return Iterables.getFirst(imgFileList, null);
    }

    @Nullable
    public Listener getListener() {
        return listener;
    }

    public void setListener(@Nullable Listener listener) {
        this.listener = listener;
    }

    public void setInfoShow(boolean isShow){
        final Runnable r = new Runnable() {
            @Override
            public void run() {
                int intShow= isShow? VISIBLE:GONE;
                binding.fileNameTitle.setVisibility(intShow);
                binding.fileName.setVisibility(intShow);
                binding.timeTitle.setVisibility(intShow);
                binding.time.setVisibility(intShow);
//                binding.layout.requestLayout();  // 強制更新 Layout
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

}
