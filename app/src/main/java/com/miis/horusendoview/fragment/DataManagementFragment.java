package com.miis.horusendoview.fragment;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.storage.StorageVolume;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.RadioGroup;

import androidx.annotation.AnyThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.annotation.UiThread;
import androidx.annotation.WorkerThread;
import androidx.appcompat.widget.ListPopupWindow;
import androidx.core.util.Pair;
import androidx.exifinterface.media.ExifInterface;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.common.VideoSize;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerControlView;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.alexvasilkov.gestures.Settings;
import com.app.customkeyboard.keyboard.CustomKeyboardView;
import com.bumptech.glide.Glide;
import com.miis.horusendoview.MyApplication;
import com.miis.horusendoview.R;
import com.miis.horusendoview.activity.MainActivity;
import com.miis.horusendoview.adapter.DataManagementAdapter;
import com.miis.horusendoview.data.DataManagementAdapterData;
import com.miis.horusendoview.data.MySelectStorageDialogData;
import com.miis.horusendoview.databinding.FragmentDataManagmentBinding;
import com.miis.horusendoview.dialog.CopyFilesProgressDialog;
import com.miis.horusendoview.dialog.DateTimeDialog;
import com.miis.horusendoview.dialog.MyDialog;
import com.miis.horusendoview.dialog.MySelectStorageDialog;
import com.miis.horusendoview.dialog.RemarkEditDialog;
import com.miis.horusendoview.dialog.ZipFilesProgressDialog;
import com.miis.horusendoview.dialog.ZipPasswordDialog;
import com.miis.horusendoview.log.LogQueue;
import com.miis.horusendoview.manager.MyStorageManager;
import com.miis.horusendoview.roomDataBase.procedureFolder.ProcedureFolderTbData;
import com.miis.horusendoview.roomDataBase.userTbData.UserTbData;
import com.miis.horusendoview.tools.Tools;
import com.miis.horusendoview.tools.UiTool;
import com.miis.horusendoview.type.DataManagementSearchMenuType;
import com.miis.horusendoview.type.GenderType;
import com.miis.horusendoview.type.UserRoleType;

import org.apache.commons.io.FileUtils;
import org.dcm4che3.data.Tag;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.reactivex.rxjava3.core.Observer;
import io.reactivex.rxjava3.disposables.Disposable;
import kotlin.jvm.internal.Intrinsics;
import timber.log.Timber;

public final class DataManagementFragment extends Fragment implements View.OnClickListener {

    public static final String LOG_TAG = "DataMangement";

    @NonNull
    private final ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(5);

    @Nullable
    private FragmentDataManagmentBinding binding;

    @Nullable
    private ListPopupWindow searchPopupMenu;

    @Nullable
    private DateTimeDialog dateTimeDialog;

    @NonNull
    private LocalDateTime startDateTime;

    @NonNull
    private LocalDateTime endDateTime;

    @NonNull
    private DataManagementSearchMenuType searchMenuType = DataManagementSearchMenuType.ALL;

    @Nullable
    private String childDirDisplayName;
    @NonNull
    private final Object childDirDisplayNameLock = new Object();

    @Nullable
    private File previewFile;
    @NonNull
    private final Object previewFileLock = new Object();

    private boolean isSelectAll;

    @Nullable
    private MyDialog myDialog;

    @Nullable
    private MySelectStorageDialog mySelectStorageDialog;

    @Nullable
    private Disposable basicExportDisposable;

    @Nullable
    private Disposable dicomExportDisposable;

    @Nullable
    private CopyFilesProgressDialog copyFilesProgressDialog;

    @Nullable
    private RemarkEditDialog remarkEditDialog;

    @Nullable
    private ZipPasswordDialog zipPasswordDialog;

    @Nullable
    private ZipFilesProgressDialog zipFilesProgressDialog;

    private boolean isPlayerFullScreen;

    @Nullable
    private ProgressDialog  GetFileCountDialog = null;
    private final String TAG = DataManagementFragment.class.getSimpleName();

    @NonNull
    public static DataManagementFragment newInstance() {
        return new DataManagementFragment();
    }


    public DataManagementFragment() {

        this.startDateTime = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);

        this.endDateTime = this.startDateTime.withHour(23).withMinute(59).withSecond(59).withNano(999999999);
    }

    private void setStartDateTime(@NonNull LocalDateTime value) {
        this.startDateTime = value;
        setStartDatTimeToView();
    }

    private void setEndDateTime(@NonNull LocalDateTime value) {
        this.endDateTime = value;
        setEndDatTimeToView();
    }

    private void setSearchMenuType(@NonNull DataManagementSearchMenuType value) {
        this.searchMenuType = value;
        this.setSearchMenuTypeToView();
    }

    @Nullable
    public String getChildDirDisplayName() {
        synchronized (childDirDisplayNameLock) {
            return this.childDirDisplayName;
        }
    }

    public void setChildDirDisplayName(@Nullable String value) {
        synchronized (childDirDisplayNameLock) {
            this.childDirDisplayName = value;
            setChildDirDisplayNameToView();
            updateFileList();
        }
    }

    @Nullable
    public File getPreviewFile() {
        synchronized (previewFileLock) {
            return this.previewFile;
        }
    }

    public void setPreviewFile(@Nullable final File value) {
        synchronized (previewFileLock) {
            final File previewFile = this.previewFile;
            if (Intrinsics.areEqual(previewFile != null ? previewFile.getName() : null, value != null ? value.getName() : null)) {
                return;
            }

            this.previewFile = value;
            setPreviewFileToView();
        }
    }

    public boolean isSelectAll() {
        return this.isSelectAll;
    }

    public void setSelectAll(boolean value) {
        this.isSelectAll = value;
        setSelectAllToView();
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
        if (this.binding == null) {
            this.binding = FragmentDataManagmentBinding.inflate(inflater, container, false);
        }
        return this.binding.getRoot();
    }

    @SuppressLint({"ClickableViewAccessibility"})
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        @Nullable final MyApplication myApplication = getMyApplication();

        try {
            scheduledExecutorService.execute(new Runnable() {
                @Override
                public void run() {
                    String imgPath = null;
                    if (myApplication != null) {
                        imgPath = myApplication.getMainDirPath();
                    }
                    if (imgPath == null) {
                        return;
                    }
                    File filesDirFile = new File(imgPath);
                    if (!filesDirFile.exists()) {
                        try {
                            filesDirFile.mkdirs();
                        } catch (Exception e) {
                            Timber.e(e);
                        }
                    }
                }
            });
        } catch (Exception e) {
            Timber.e(e);
        }

        MyStorageManager.getInstance().addListener(myStorageManagerListener);
        checkHasUsrOrSdCard();


        FragmentDataManagmentBinding binding = this.binding;
        if (binding != null) {
            binding.menuLayout.setOnClickListener(this);
            binding.startDateTimeLayout.setOnClickListener(this);
            binding.endDateTimeLayout.setOnClickListener(this);
            binding.search.setOnClickListener(this);
            binding.selectAll.setOnClickListener(this);
            binding.btnPrevious.setOnClickListener(this);
            binding.btnNext.setOnClickListener(this);
            binding.screenshot.setOnClickListener(this);
            binding.delete.setOnClickListener(this);
            binding.basicExport.setOnClickListener(this);
            binding.remark.setOnClickListener(this);
            binding.dicomExport.setOnClickListener(this);
            binding.upperLevel.setOnClickListener(this);
            binding.imgFullscreenExit.setOnClickListener(this);
            binding.imgFullscreen.setOnClickListener(this);

            binding.loadingLayout.setOnTouchListener((v, event) -> true);

            binding.age.addTextChangedListener(ageTextChangedListener);
            binding.genderLayout.setOnCheckedChangeListener(genderLayoutOnCheckedChangeListener);
        }

        setStartDatTimeToView();
        setEndDatTimeToView();
        setSearchMenuTypeToView();

        initRecyclerView();
        updateFileList();

        initExoPlayer();
        initPreviewImgFull();

        final FragmentActivity activity = getActivity();
        if (activity instanceof MainActivity) {
            // 註冊虛擬鍵盤
            MainActivity mainActivity = (MainActivity) activity;
            if (binding != null) {
                mainActivity.getBinding().customKeyboardView.unregisterEditText(binding.patientId);
                mainActivity.getBinding().customKeyboardView.registerEditText(
                        CustomKeyboardView.KeyboardType.QWERTY,
                        binding.patientId
                );

                mainActivity.getBinding().customKeyboardView.unregisterEditText(binding.age);
                mainActivity.getBinding().customKeyboardView.registerEditText(
                        CustomKeyboardView.KeyboardType.NUMBER,
                        binding.age
                );
            }
        }
    }


    public void onResume() {
        super.onResume();
        this.checkHasUsrOrSdCard();
    }

    @Override
    public void onPause() {
        super.onPause();
        cancelSearchMenu();
        cancelDateTimeDialog();
        cancelMyDialog();
        cancelMySelectStorageDialog();
        cancelRemarkEditDialog();
        cancelZipPasswordDialog();

        final FragmentDataManagmentBinding binding = this.binding;
        final Player player = binding != null ? binding.playerView.getPlayer() : null;
        if (player != null && player.isPlaying()) {
            player.setPlayWhenReady(false);
        }
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();

        final FragmentDataManagmentBinding binding = this.binding;
        final Player player = binding != null ? binding.playerView.getPlayer() : null;
        if (player != null) {
            player.stop();
            player.release();
        }

        MyStorageManager.getInstance().removeListener(myStorageManagerListener);

        final FragmentActivity activity = getActivity();
        if (activity instanceof MainActivity && binding != null) {
            // 解註冊虛擬鍵盤
            MainActivity mainActivity = (MainActivity) activity;
            mainActivity.getBinding().customKeyboardView.unregisterEditText(binding.patientId);
            mainActivity.getBinding().customKeyboardView.unregisterEditText(binding.age);
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

        final FragmentDataManagmentBinding binding = this.binding;
        if (hidden) {
            cancelSearchMenu();
            cancelDateTimeDialog();
            cancelMyDialog();
            cancelMySelectStorageDialog();
            cancelRemarkEditDialog();
            cancelZipPasswordDialog();

            final Player player = binding != null ? binding.playerView.getPlayer() : null;
            if (player != null && player.isPlaying()) {
                player.setPlayWhenReady(false);
            }
        } else {
            checkHasUsrOrSdCard();

            FragmentActivity activity = getActivity();
            if (activity instanceof MainActivity) {
                final MainActivity mainActivity = (MainActivity) activity;
                mainActivity.setTabBtnSelected(mainActivity.getBinding().ibDataManagement);
                if (binding != null) {
                    binding.getRoot().postDelayed(() -> mainActivity.setTabBtnClickable(true), MainActivity.TAB_BTN_CLICKABLE_ON_DELAY);
                }
            }
            // Clear profile info
            ClearDataManagementPatientInfo();

            updateFileList();
        }
    }


    public static int CheckPasswordRule(String password) {
        if (password == null || password.length() < 8) {
            return 0;
        }

        int type = 0;
        for (int i = 0; i <= password.length() - 1; i++) {
            char c = password.charAt(i);

            if (Character.isDigit(c)) {
                type = type | 1 ;
                continue;
            }

            if (Character.isUpperCase(c)) {
                type = type | 10 ;
                continue;
            }

            if (Character.isLowerCase(c)) {
                type = type | 100 ;
                continue;
            }
        }

        if ((type == 11) || (type == 101) || (type == 110) || (type == 111)) {
            return 1;
        }
        return 0;
    }

    public void ClearDataManagementPatientInfo() {
        binding.visitDate.setText(null);
        binding.id.setText(null);
        binding.age.setText(null);
        binding.remark.setText(null);
        binding.genderLayout.clearCheck();
    }

    @Override
    public void onClick(@Nullable View v) {
        if (v == null) {
            return;
        }

        Timber.d("[onClick] "+ LogQueue.getId(v));
        switch (v.getId()) {
            case R.id.menuLayout:
                showSearchMenu();
                break;

            case R.id.startDateTimeLayout:
                showDateTimeDialog(true);
                break;

            case R.id.endDateTimeLayout:
                showDateTimeDialog(false);
                break;

            case R.id.search:
                setChildDirDisplayName(null);
                updateFileList();
                ClearDataManagementPatientInfo();
                break;

            case R.id.selectAll:
                selectAll();
                break;

            case R.id.screenshot: {
                final FragmentDataManagmentBinding binding = this.binding;
                if (binding != null && binding.playerView.getVisibility() == View.VISIBLE) {
                    File previewFile = getPreviewFile();
                    if (previewFile != null) {
                        final Player player = binding.playerView.getPlayer();
                        if (player == null) {
                            return;
                        }
                        long contentPosition = player.getContentPosition();
                        if (contentPosition >= 0) {
                            screenshotVideoFrame(contentPosition * 1000);
                        }
                    }
                }
            }
            break;

            case R.id.delete:
                delete();
                break;

            case R.id.basicExport:
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
                            UserRoleType roleTypeT = loginUserTbData.getRoleType();
                            @NonNull final UserRoleType roleType = roleTypeT != null ? roleTypeT : UserRoleType.GUEST;


                            if (basicExportDisposable != null) {
                                return;
                            }

                            @NonNull final List<StorageVolume> storageList = MyStorageManager.getInstance().getStorageList();
                            if (storageList.isEmpty()) {
                                return;
                            }

                            final FragmentDataManagmentBinding binding = DataManagementFragment.this.binding;
                            if (binding == null) {
                                return;
                            }
                            final RecyclerView.Adapter<?> a = binding.recyclerView.getAdapter();
                            if (!(a instanceof DataManagementAdapter)) {
                                return;
                            }
                            final DataManagementAdapter adapter = (DataManagementAdapter) a;

                            binding.basicExport.setOnClickListener(null);
                            showGetFileCountDialog();

                            @NonNull File mainDirFile = new File(myApplication.getMainDirPath());
                            List<@NotNull File> selectFileList = new ArrayList<>();
                            for (DataManagementAdapterData data : adapter.getCurrentList()) {
                                if (data.isSelect()) {
                                    String dirDisplayName = data.getDirDisplayName();
                                    File file = data.getFile();
                                    if (!TextUtils.isEmpty(dirDisplayName)) {
                                        String[] dirDisplayNameArr = dirDisplayName.split("_");
                                        if (dirDisplayNameArr.length >= 2) {
                                            @Nullable File[] childDirFileList = null;
                                            try {
                                                childDirFileList = mainDirFile.listFiles(childDirFile -> {
                                                    String[] childDirNameArr = childDirFile.getName().split("_");
                                                    if (childDirNameArr.length >= 3 && childDirFile.isDirectory()) {
                                                        switch (roleType) {
                                                            case GUEST:
                                                                return false;
                                                            case ADVANCED_USER:
                                                                return childDirNameArr[0].equals(loginUserTbData.getAccount()) &&
                                                                        childDirNameArr[1].equals(dirDisplayNameArr[0]) &&
                                                                        childDirNameArr[2].equals(dirDisplayNameArr[1]);
                                                            case ADMIN_USER:
                                                            case SERVICE_USER:
                                                                return childDirNameArr[1].equals(dirDisplayNameArr[0]) &&
                                                                        childDirNameArr[2].equals(dirDisplayNameArr[1]);
                                                            default:
                                                                return false;
                                                        }
                                                    } else {
                                                        return false;
                                                    }
                                                });
                                            } catch (Exception e) {
                                                Timber.e(e);
                                            }

                                            if (childDirFileList == null) {
                                                childDirFileList = new File[]{};
                                            }

                                            for (File childDirFile : childDirFileList) {
                                                @Nullable List<File> childFileList = null;
                                                try {
                                                    File[] childFileArr = childDirFile.listFiles(childFile ->
                                                            childFile.isFile() &&
                                                                    (childFile.getName().contains(MyApplication.IMG_FILE_EXTENSION) ||
                                                                            childFile.getName().contains(MyApplication.VIDEO_FILE_EXTENSION))
                                                    );
                                                    if (childFileArr == null) {
                                                        childFileArr = new File[]{};
                                                    }
                                                    childFileList = Arrays.asList(childFileArr);
                                                } catch (Exception e) {
                                                    Timber.e(e);
                                                }
                                                if (childFileList != null) {
                                                    selectFileList.addAll(childFileList);
                                                }
                                            }
                                        }
                                    } else if (file != null) {
                                        selectFileList.add(file);
                                    }
                                }
                            }

                            binding.basicExport.setOnClickListener(DataManagementFragment.this);
                            cancelGetFileCountDialog();

                            if (selectFileList.isEmpty()) {
                                binding.getRoot().post(new Runnable() {
                                    @Override
                                    public void run() {
                                        showMyDialog(
                                                context.getString(R.string.export_usb_not_chose_files_msg),
                                                true,
                                                context.getString(R.string.ok),
                                                false,
                                                "",
                                                null,
                                                false
                                        );
                                    }
                                });
                                return;
                            }

                            HashMap<@NotNull String, @NotNull List<@NotNull File>> selectFileGroupMap = new HashMap<>();
                            for (@NonNull File selectFile : selectFileList) {
                                @Nullable final File parentFile = selectFile.getParentFile();
                                String parentFileName = parentFile != null ? parentFile.getName() : "";
                                String[] parentFileNameArr = parentFileName.split("_");
                                String key = parentFileNameArr[1] + "_" + parentFileNameArr[2];

                                @Nullable List<File> fileListT = null;
                                if (selectFileGroupMap.containsKey(key)) {
                                    fileListT = selectFileGroupMap.get(key);
                                }
                                final ArrayList<@NotNull File> fileList = new ArrayList<>();
                                if (fileListT != null) {
                                    fileList.addAll(fileListT);
                                }
                                fileList.add(selectFile);
                                selectFileGroupMap.put(key, fileList);
                            }

                            if (storageList.size() > 1) {
                                binding.getRoot().post(new Runnable() {
                                    @Override
                                    public void run() {
                                        showZipPasswordDialog(new ZipPasswordDialog.Listener() {
                                            @Override
                                            public void onClickConfirm(@NonNull String password) {
                                                showMySelectStorageDialog(selectFileGroupMap, false, password.trim());
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
                } catch (Exception e) {
                    Timber.e(e);
                }
                break;

            case R.id.dicomExport:
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
                            final FragmentDataManagmentBinding binding = DataManagementFragment.this.binding;
                            if (binding == null) {
                                return;
                            }

                            UserTbData loginUserTbData = myApplication.getLoginUserTbData();
                            if (loginUserTbData == null) {
                                return;
                            }
                            UserRoleType roleTypeT = loginUserTbData.getRoleType();
                            @NonNull final UserRoleType roleType = roleTypeT != null ? roleTypeT : UserRoleType.GUEST;

                            if (dicomExportDisposable != null) {
                                return;
                            }
                            final List<StorageVolume> storageList = MyStorageManager.getInstance().getStorageList();
                            if (storageList.isEmpty()) {
                                return;
                            }

                            final RecyclerView.Adapter<?> a = binding.recyclerView.getAdapter();
                            if (!(a instanceof DataManagementAdapter)) {
                                return;
                            }
                            DataManagementAdapter adapter = (DataManagementAdapter) a;

                            binding.dicomExport.setOnClickListener(null);
                            showGetFileCountDialog();

                            final File mainDirFile = new File(myApplication.getMainDirPath());
                            final ArrayList<@NotNull File> selectFileList = new ArrayList<>();
                            for (@NonNull DataManagementAdapterData data : adapter.getCurrentList()) {
                                if (data.isSelect()) {
                                    String dirDisplayName = data.getDirDisplayName();
                                    File file = data.getFile();
                                    if (!TextUtils.isEmpty(dirDisplayName)) {
                                        String[] dirDisplayNameArr = dirDisplayName.split("_");
                                        if (dirDisplayNameArr.length >= 2) {
                                            @Nullable File[] childDirFileList = null;
                                            try {
                                                childDirFileList = mainDirFile.listFiles(childDirFile -> {
                                                    String[] childDirNameArr = childDirFile.getName().split("_");
                                                    if (childDirNameArr.length >= 3 && childDirFile.isDirectory()) {
                                                        switch (roleType) {
                                                            case GUEST:
                                                                return false;
                                                            case ADVANCED_USER:
                                                                return childDirNameArr[0].equals(loginUserTbData.getAccount()) &&
                                                                        childDirNameArr[1].equals(dirDisplayNameArr[0]) &&
                                                                        childDirNameArr[2].equals(dirDisplayNameArr[1]);
                                                            case ADMIN_USER:
                                                            case SERVICE_USER:
                                                                return childDirNameArr[1].equals(dirDisplayNameArr[0]) &&
                                                                        childDirNameArr[2].equals(dirDisplayNameArr[1]);
                                                            default:
                                                                return false;
                                                        }
                                                    } else {
                                                        return false;
                                                    }
                                                });
                                            } catch (Exception e) {
                                                Timber.e(e);
                                            }

                                            if (childDirFileList == null) {
                                                childDirFileList = new File[]{};
                                            }
                                            for (File childDirFile : childDirFileList) {
                                                @Nullable List<File> childFileList = new ArrayList<>();
                                                try {
                                                    File[] childFileArr = childDirFile.listFiles();
                                                    for (File childFile:childFileArr) {
                                                        if(childFile.isFile() &&
                                                                (childFile.getName().contains(MyApplication.IMG_FILE_EXTENSION))){
                                                            childFileList.add(childFile);
                                                        }else{
                                                            showMyDialog(
                                                                    R.string.msg_dicom_image_only,
                                                                    true,
                                                                    R.string.ok,
                                                                    false,
                                                                    R.string.cancel,
                                                                    null,
                                                                    false
                                                            );
                                                            binding.dicomExport.setOnClickListener(DataManagementFragment.this);
                                                            cancelGetFileCountDialog();
                                                            return;
                                                        }
                                                    }
                                                } catch (Exception e) {
                                                    Timber.e(e);
                                                }
                                                if (childFileList != null) {
                                                    selectFileList.addAll(childFileList);
                                                }
                                            }
                                        }
                                    } else if (file != null) {
                                        if(file.isFile() &&
                                                (file.getName().contains(MyApplication.IMG_FILE_EXTENSION))){
                                            selectFileList.add(file);
                                        }else{
                                            showMyDialog(
                                                    R.string.msg_dicom_image_only,
                                                    true,
                                                    R.string.ok,
                                                    false,
                                                    R.string.cancel,
                                                    null,
                                                    false
                                            );
                                            binding.dicomExport.setOnClickListener(DataManagementFragment.this);
                                            cancelGetFileCountDialog();
                                            return;
                                        }
                                    }
                                }
                            }

                            binding.dicomExport.setOnClickListener(DataManagementFragment.this);
                            cancelGetFileCountDialog();

                            if (selectFileList.isEmpty()) {
                                binding.getRoot().post(new Runnable() {
                                    @Override
                                    public void run() {
                                        showMyDialog(
                                                context.getString(R.string.export_usb_not_chose_files_msg),
                                                true,
                                                context.getString(R.string.ok),
                                                false,
                                                "",
                                                null,
                                                false
                                        );
                                    }
                                });
                                return;
                            }

                            final HashMap<@NotNull String, @NotNull List<@NotNull File>> selectFileGroupMap = new HashMap<>();
                            for (File selectFile : selectFileList) {
                                @Nullable final File parentFile = selectFile.getParentFile();
                                String parentFileName = parentFile != null ? parentFile.getName() : "";
                                String[] parentFileNameArr = parentFileName.split("_");
                                String key = parentFileNameArr[1] + "_" + parentFileNameArr[2];

                                @Nullable List<File> fileListT = null;
                                if (selectFileGroupMap.containsKey(key)) {
                                    fileListT = selectFileGroupMap.get(key);
                                }
                                final ArrayList<@NotNull File> fileList = new ArrayList<>();
                                if (fileListT != null) {
                                    fileList.addAll(fileListT);
                                }
                                fileList.add(selectFile);
                                selectFileGroupMap.put(key, fileList);
                            }

                            if (selectFileGroupMap.size()>1) {
                                binding.getRoot().post(new Runnable() {
                                    @Override
                                    public void run() {
                                        showMyDialog(
                                                context.getString(R.string.dicom_1folder_msg),
                                                true,
                                                context.getString(R.string.ok),
                                                false,
                                                "",
                                                null,
                                                false
                                        );
                                    }
                                });
                                return;
                            }

                            final File dicomTempFile=getDicomTempFolder();
                            if(dicomTempFile!=null && dicomTempFile.exists()){
                                try {
                                    FileUtils.deleteDirectory(dicomTempFile);
                                } catch (IOException e) {
                                    Timber.w("Exception: "+ e.getMessage());
                                }
                            }

                            if (storageList.size() > 1) {
                                binding.getRoot().post(new Runnable() {
                                    @Override
                                    public void run() {
                                        showZipPasswordDialog(new ZipPasswordDialog.Listener() {
                                            @Override
                                            public void onClickConfirm(@NonNull String password) {
                                                showMySelectStorageDialog(selectFileGroupMap, true, password);
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

                            Map<File, List<File>> selectDicomFileGroupMap = new HashMap<>();
                            for (Map.Entry<String, List<File>> entry : selectFileGroupMap.entrySet()) {
                                if(entry.getValue().size()<=0){
                                    continue;
                                }
                                final File file =new File(dicomTempFile , File.separator + entry.getKey());
                                selectDicomFileGroupMap.put(file, entry.getValue());
                            }

                            showZipPasswordDialog(new ZipPasswordDialog.Listener() {
                                @Override
                                public void onClickConfirm(@NonNull String password) {
                                    dicomExport(selectDicomFileGroupMap, "DICOM", password, storageList.get(0));
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
                } catch (Exception e) {
                    Timber.e(e);
                }
                break;

            case R.id.remark:
                showRemarkEditDialog();
                break;

            case R.id.upperLevel:
                setChildDirDisplayName(null);
                setPreviewFile(null);
                break;

            case R.id.imgFullscreenExit: {
                final FragmentDataManagmentBinding binding = DataManagementFragment.this.binding;
                if (binding != null) {
                    binding.previewImgFull.getController().resetState();
                    binding.previewImgFullLayout.setVisibility(View.INVISIBLE);
                }
            }
            break;

            case R.id.imgFullscreen: {
                final FragmentDataManagmentBinding binding = DataManagementFragment.this.binding;
                if (binding != null) {
                    binding.previewImgFullLayout.setVisibility(View.VISIBLE);
                }
            }
            break;
            case R.id.btnPrevious:
                RecyclerView.Adapter<?> adapter = binding.recyclerView.getAdapter();
                if (adapter instanceof DataManagementAdapter) {
                    DataManagementAdapter dataManagementAdapter = (DataManagementAdapter) adapter;
                    final int pos = dataManagementAdapter.previous();
                    binding.recyclerView.post(new Runnable() {
                        @Override
                        public void run() {
                            binding.recyclerView.smoothScrollToPosition(pos);
                        }
                    });
                }
                break;
            case R.id.btnNext:
                adapter = binding.recyclerView.getAdapter();
                if (adapter instanceof DataManagementAdapter) {
                    DataManagementAdapter dataManagementAdapter = (DataManagementAdapter) adapter;
                    final int pos = dataManagementAdapter.next();
                    binding.recyclerView.smoothScrollToPosition(pos);
                    binding.recyclerView.post(new Runnable() {
                        @Override
                        public void run() {
                            binding.recyclerView.smoothScrollToPosition(pos);
                        }
                    });
                }
                break;
        }
    }


    private void setStartDatTimeToView() {
        final FragmentDataManagmentBinding binding = DataManagementFragment.this.binding;
        if (binding != null) {
            binding.getRoot().post(new Runnable() {
                @Override
                public void run() {
                    final LocalDateTime startDateTime = DataManagementFragment.this.startDateTime;
                    binding.startDateTime.setText(startDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.ENGLISH)));
                }
            });
        }
    }


    private void setEndDatTimeToView() {
        final FragmentDataManagmentBinding binding = DataManagementFragment.this.binding;
        if (binding != null) {
            binding.getRoot().post(new Runnable() {
                @Override
                public void run() {
                    final LocalDateTime endDateTime = DataManagementFragment.this.endDateTime;
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.ENGLISH);
                    binding.endDateTime.setText(endDateTime.format(formatter));
                }
            });
        }
    }


    private void setSearchMenuTypeToView() {
        final FragmentDataManagmentBinding binding = DataManagementFragment.this.binding;
        if (binding != null) {
            binding.getRoot().post(new Runnable() {
                @Override
                public void run() {
                    final DataManagementSearchMenuType searchMenuType = DataManagementFragment.this.searchMenuType;
                    if (binding != null && searchMenuType != null) {
                        binding.menuText.setText(searchMenuType.getStringId());
                        switch (searchMenuType) {
                            case ALL:
                                binding.dateTimeLayout.setVisibility(View.INVISIBLE);
                                break;
                            case TODAY:
                            case SELECT_A_TIME_RANGE:
                                binding.dateTimeLayout.setVisibility(View.VISIBLE);
                                break;
                        }
                    }
                }
            });
        }
    }


    private void showSearchMenu() {
        cancelSearchMenu();

        final Context context = getContext();
        final FragmentDataManagmentBinding binding = DataManagementFragment.this.binding;
        if (context == null || binding == null) {
            return;
        }

        ListPopupWindow p = new ListPopupWindow(context, null, 0, R.style.myListPopupWindowStyle);
        p.setAnchorView(binding.menuLayout);
        final ArrayList<@NotNull String> list = new ArrayList<>();
        final DataManagementSearchMenuType[] typeArr = DataManagementSearchMenuType.values();
        for (DataManagementSearchMenuType searchMenuType : typeArr) {
            list.add(context.getString(searchMenuType.getStringId()));
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(context, R.layout.list_popup_window_item, list);
        p.setAdapter(adapter);
        p.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                final DataManagementSearchMenuType type = typeArr[position];
                setSearchMenuType(type);
                if (type == DataManagementSearchMenuType.TODAY) {
                    final LocalDateTime now = LocalDateTime.now()
                            .withHour(0)
                            .withMinute(0)
                            .withSecond(0)
                            .withNano(0);
                    setStartDateTime(now);
                    setEndDateTime(now.withHour(23).withMinute(59).withSecond(59).withNano(999999999));
                }
                cancelSearchMenu();
            }
        });
        p.getBackground();
        p.setVerticalOffset(-binding.menuLayout.getMeasuredHeight());
        p.show();
        DataManagementFragment.this.searchPopupMenu = p;
    }


    private void cancelSearchMenu() {
        final ListPopupWindow searchPopupMenu = DataManagementFragment.this.searchPopupMenu;
        if (searchPopupMenu != null) {
            searchPopupMenu.dismiss();
            DataManagementFragment.this.searchPopupMenu = null;
        }
    }


    private void showDateTimeDialog(boolean isStartDateTime) {
        cancelDateTimeDialog();

        final Context context = getContext();
        if (context == null) {
            return;
        }

        final DateTimeDialog d = new DateTimeDialog(getChildFragmentManager(), context);
        d.setListener(new DateTimeDialog.Listener() {
            @Override
            public void onClickConfirm(@NonNull LocalDateTime dateTime) {
                if (isStartDateTime) {
                    final LocalDateTime t = dateTime.withSecond(0).withNano(0);
                    if (t.isAfter(DataManagementFragment.this.endDateTime)) {
                        DataManagementFragment.this.setEndDateTime(t.withSecond(59).withNano(999999999));
                    }
                    DataManagementFragment.this.setStartDateTime(t);
                } else {
                    final LocalDateTime t = dateTime.withSecond(59).withNano(999999999);
                    if (t.isBefore(DataManagementFragment.this.startDateTime)) {
                        DataManagementFragment.this.setStartDateTime(t.withSecond(0).withNano(0));
                    }
                    DataManagementFragment.this.setEndDateTime(t);
                }
                boolean isSameDay = DataManagementFragment.this.startDateTime.toLocalDate()
                        .isEqual(DataManagementFragment.this.endDateTime.toLocalDate());

                if (isSameDay && LocalDate.now()
                        .isEqual(DataManagementFragment.this.startDateTime.toLocalDate())
                ) {
                    if (DataManagementFragment.this.searchMenuType != DataManagementSearchMenuType.TODAY) {
                        DataManagementFragment.this.setSearchMenuType(DataManagementSearchMenuType.TODAY);
                    }
                } else {
                    if (DataManagementFragment.this.searchMenuType != DataManagementSearchMenuType.SELECT_A_TIME_RANGE) {
                        DataManagementFragment.this.setSearchMenuType(DataManagementSearchMenuType.SELECT_A_TIME_RANGE);
                    }
                }
            }

            @Override
            public void onClickCancel() {

            }

            @Override
            public void onTouch() {
                FragmentActivity activity = getActivity();
                if (activity instanceof MainActivity) {
                    ((MainActivity) activity).restartStandbyNotificationTimeServiceTimer();
                }
            }
        });

        LocalDateTime dateTime = isStartDateTime ? DataManagementFragment.this.startDateTime : DataManagementFragment.this.endDateTime;
        d.setDateTime(dateTime);
        d.show();
        DataManagementFragment.this.dateTimeDialog = d;
    }


    private void cancelDateTimeDialog() {
        final DateTimeDialog dateTimeDialog = DataManagementFragment.this.dateTimeDialog;
        if (dateTimeDialog != null) {
            dateTimeDialog.dismiss();
            DataManagementFragment.this.dateTimeDialog = null;
        }
    }

    private void initRecyclerView() {
        final Context context = getContext();
        if (context == null) {
            return;
        }

        final FragmentDataManagmentBinding binding = DataManagementFragment.this.binding;
        if (binding == null) {
            return;
        }
        if (!(binding.recyclerView.getLayoutManager() instanceof LinearLayoutManager)) {
            binding.recyclerView.setLayoutManager(new LinearLayoutManager(context));
        }

        //Fixed an issue where too many files would crash
        binding.recyclerView.setHasFixedSize(true);

        DefaultItemAnimator itemAnimator = (DefaultItemAnimator) binding.recyclerView.getItemAnimator();
        if (itemAnimator != null) {
            itemAnimator.setSupportsChangeAnimations(false);
        }

        if (!(binding.recyclerView.getAdapter() instanceof DataManagementAdapter)) {
            binding.recyclerView.setAdapter(new DataManagementAdapter(this));
        }
    }


    @SuppressLint({"RestrictedApi"})
    public void updateFileList() {

        try {
            scheduledExecutorService.execute(new Runnable() {
                @Override
                public void run() {
                    final MyApplication myApplication = getMyApplication();
                    if (myApplication == null) {
                        return;
                    }
                    final FragmentDataManagmentBinding binding = DataManagementFragment.this.binding;
                    if (binding == null) {
                        return;
                    }

                    final UserTbData loginUserTbData = myApplication.getLoginUserTbData();
                    if (loginUserTbData == null) {
                        setDataToRecyclerView(Collections.emptyList());
                        return;
                    }


                    final UserRoleType roleTypeT = loginUserTbData.getRoleType();
                    final UserRoleType roleType = roleTypeT != null ? roleTypeT : UserRoleType.GUEST;

                    if (roleType == UserRoleType.GUEST) {
                        setDataToRecyclerView(Collections.emptyList());
                        return;
                    }

                    final DataManagementSearchMenuType searchMenuType = DataManagementFragment.this.searchMenuType;
                    final LocalDateTime startDateTime = DataManagementFragment.this.startDateTime;
                    final LocalDateTime endDateTime = DataManagementFragment.this.endDateTime;
                    final Editable patientIdText = binding.patientId.getText();
                    @Nullable final String patientId = patientIdText != null ? patientIdText.toString().trim() : null;
                    @Nullable final String childDirDisplayName = getChildDirDisplayName();

                    @NonNull final String account = loginUserTbData.getAccount();
                    final boolean isNowMainDir = TextUtils.isEmpty(childDirDisplayName);

                    binding.getRoot().post(new Runnable() {
                        @Override
                        public void run() {
                            binding.loadingLayout.setVisibility(View.VISIBLE);
                        }
                    });

                    final RecyclerView.Adapter<?> a = binding.recyclerView.getAdapter();
                    final DataManagementAdapter adapter = a instanceof DataManagementAdapter ? ((DataManagementAdapter) a) : null;
                    final List<DataManagementAdapterData> adapterDataList = (adapter != null) ? adapter.getCurrentList() : Collections.emptyList();

                    List<File> dirFileList;
                    if (isNowMainDir) {
                        dirFileList = new ArrayList<>();
                        dirFileList.add(new File(myApplication.getMainDirPath()));
                    } else if (!TextUtils.isEmpty(childDirDisplayName)) {
                        final File mainDirFile = new File(myApplication.getMainDirPath());
                        File[] dirFileArr = new File[0];
                        try {
                            dirFileArr = mainDirFile.listFiles((dirFile, name) -> {
                                if (dirFile.isFile()) {
                                    return false;
                                }

                                String[] nameArr = name.split("_");
                                if (nameArr.length < 3) {
                                    return false;
                                }

                                switch (roleType) {
                                    case GUEST:
                                        return false;
                                    case ADVANCED_USER:
                                        if (!nameArr[0].equals(account)) {
                                            return false;
                                        }
                                        break;
                                    case ADMIN_USER:
                                    case SERVICE_USER:
                                        break;
                                }

                                try {
                                    return TextUtils.equals(nameArr[1] + "_" + nameArr[2], childDirDisplayName);
                                } catch (Exception e) {
                                    Timber.e(e);
                                    return false;
                                }
                            });
                        } catch (Exception e) {
                            Timber.e(e);
                        }
                        if (dirFileArr == null) {
                            dirFileArr = new File[]{};
                        }
                        dirFileList = Arrays.asList(dirFileArr);
                    } else {
                        dirFileList = Collections.emptyList();
                    }

                    final ArrayList<File> fileList = new ArrayList<>();
                    for (File dirFile : dirFileList) {
                        File[] fileListTArr = new File[]{};
                        try {
                            fileListTArr = dirFile.listFiles(childFile -> {
                                try {
                                    if (!childFile.exists()) {
                                        return false;
                                    }
                                } catch (Exception e) {
                                    Timber.e(e);
                                }

                                try {
                                    if (!childFile.canRead()) {
                                        return false;
                                    }
                                } catch (Exception e) {
                                    Timber.e(e);
                                }

                                try {
                                    if (childFile.isHidden()) {
                                        return false;
                                    }
                                } catch (Exception e) {
                                    Timber.e(e);
                                }

                                if (childFile.isFile()) {
                                    // 子資料夾裡的檔案
                                    if (isNowMainDir) {
                                        return false;
                                    }

                                    if (!(childFile.getName().contains(MyApplication.IMG_FILE_EXTENSION) ||
                                            childFile.getName().contains(MyApplication.VIDEO_FILE_EXTENSION))) {
                                        return false;
                                    }

                                    final String nameNoExtension = childFile.getName()
                                            .replace(MyApplication.IMG_FILE_EXTENSION, "")
                                            .replace(MyApplication.VIDEO_FILE_EXTENSION, "");
                                    final String[] nameArr = nameNoExtension.split("_");
                                    if (nameArr.length < 3) {
                                        return false;
                                    }

                                    @NonNull String dateTimeStr;
                                    try {
                                        dateTimeStr = nameArr[1] + "_" + nameArr[2];
                                    } catch (Exception e) {
                                        Timber.e(e);
                                        dateTimeStr = "";
                                    }

                                    @NonNull LocalDateTime dateTime;
                                    try {
                                        dateTime = LocalDateTime.parse(dateTimeStr, DateTimeFormatter.ofPattern(CameraFragment.IMG_SIMPLE_DATE_FORMAT_PATTERN, Locale.ENGLISH));
                                    } catch (Exception e) {
                                        try {
                                            dateTime = LocalDateTime.parse(dateTimeStr, DateTimeFormatter.ofPattern(CameraFragment.VIDEO_SIMPLE_DATE_FORMAT_PATTERN, Locale.ENGLISH));
                                        } catch (Exception ex) {
                                            BasicFileAttributes basicFileAttributes;
                                            try {
                                                basicFileAttributes = Files.readAttributes(childFile.toPath(), BasicFileAttributes.class);
                                            } catch (IOException e1) {
                                                Timber.e(e1);
                                                basicFileAttributes = null;
                                            }
                                            if (basicFileAttributes != null) {
                                                dateTime = basicFileAttributes.creationTime()
                                                        .toInstant()
                                                        .atZone(ZoneId.systemDefault())
                                                        .toLocalDateTime();
                                            } else {
                                                dateTime = null;
                                            }
                                        }
                                    }
                                    if (dateTime == null) {
                                        return false;
                                    }

                                    switch (searchMenuType) {
                                        case ALL:
                                            return true;
                                        case TODAY:
                                        case SELECT_A_TIME_RANGE:
                                            if (dateTime.isBefore(startDateTime) || dateTime.isAfter(endDateTime)) {
                                                return false;
                                            }
                                            break;
                                    }
                                    return true;
                                } else if (childFile.isDirectory()) {
                                    // 主資料夾裡的子資料夾

                                    if (!isNowMainDir) {
                                        return false;
                                    }

                                    final String[] nameArr = childFile.getName().split("_");

                                    if (nameArr.length < 3) {
                                        return false;
                                    }

                                    if (!TextUtils.isEmpty(patientId)) {
                                        switch (roleType) {
                                            case GUEST:
                                                return false;
                                            case ADVANCED_USER: {
                                                ProcedureFolderTbData d = null;
                                                try {
                                                    d = myApplication.getMyRoomDatabase().procedureFolderTbDataDao().fuzzySearchPatientId("%" + patientId + "%", childFile.getPath(), account);
                                                } catch (Exception e) {
                                                    Timber.e(e);
                                                }
                                                boolean isHasData = d != null;
                                                if (!isHasData) {
                                                    return false;
                                                }
                                            }
                                            break;
                                            case ADMIN_USER:
                                            case SERVICE_USER: {
                                                ProcedureFolderTbData d = null;
                                                try {
                                                    d = myApplication.getMyRoomDatabase().procedureFolderTbDataDao().fuzzySearchPatientId("%" + patientId + "%", childFile.getPath());
                                                } catch (Exception e) {
                                                    Timber.e(e);
                                                }
                                                boolean isHasData = d != null;
                                                if (!isHasData) {
                                                    return false;
                                                }
                                            }
                                            break;
                                        }
                                    }

                                    switch (roleType) {
                                        case GUEST:
                                            return false;
                                        case ADVANCED_USER:
                                            if (!account.equals(nameArr[0])) {
                                                return false;
                                            }
                                            break;
                                        case ADMIN_USER:
                                        case SERVICE_USER:
                                            break;
                                    }

                                    @Nullable LocalDateTime dateTime = null;
                                    try {
                                        dateTime = LocalDate.parse(nameArr[nameArr.length - 1], DateTimeFormatter.ofPattern(CameraFragment.DIR_NAME_DATE_FORMAT_PATTERN, Locale.ENGLISH))
                                                .atTime(0, 0, 0);
                                    } catch (Exception e) {
                                        Timber.e(e);
                                    }
                                    if (dateTime == null) {
                                        return false;
                                    }

                                    switch (searchMenuType) {
                                        case ALL:
                                            return true;
                                        case TODAY:
                                        case SELECT_A_TIME_RANGE:
                                            if (dateTime.isBefore(startDateTime) || dateTime.isAfter(endDateTime)) {
                                                return false;
                                            }
                                            break;
                                    }
                                    return true;
                                }
                                return false;
                            });
                        } catch (Exception e) {
                            Timber.e(e);
                        }
                        if (fileListTArr == null) {
                            fileListTArr = new File[]{};
                        }
                        List<File> fileListT = Arrays.asList(fileListTArr);
                        if (fileListT != null) {
                            fileList.addAll(fileListT);
                        }
                    }

                    if(fileList!=null && !fileList.isEmpty()){
                        if(fileList.get(0).isFile()){
                            //按照名稱倒序排列
                            fileList.sort(Comparator.comparing(File::getName, String.CASE_INSENSITIVE_ORDER).reversed());

                        }else{
                            //按照修改時間，倒序排列
                            Collections.sort(fileList, new Comparator<File>() {
                                    @Override
                                    public int compare(File f1, File f2) {
                                    return Long.compare(f1.lastModified(), f2.lastModified());
                                }
                            }.reversed());
                        }
                    }

                    final ArrayList<DataManagementAdapterData> list = new ArrayList<>();
                    for (File file : fileList) {
                        if (file.isFile()) {
                            final boolean isSelect = adapterDataList.stream()
                                    .filter(it -> it.getFile() != null && it.getFile().getName().equals(file.getName()))
                                    .findFirst()
                                    .map(DataManagementAdapterData::isSelect)
                                    .orElse(false);
                            DataManagementAdapterData data = new DataManagementAdapterData(
                                    isSelect,
                                    false,
                                    null,
                                    file
                            );
                            list.add(data);
                        } else if (file.isDirectory()) {
                            final String[] nameArr = file.getName().split("_");
                            @Nullable final String dirDisplayName = (nameArr.length >= 3) ? (nameArr[1] + "_" + nameArr[2]) : null;
                            boolean isNewData = false;
                            DataManagementAdapterData data = null;
                            for (DataManagementAdapterData item : list) {
                                if (item.getDirDisplayName() != null && item.getDirDisplayName().equals(dirDisplayName)) {
                                    data = item;
                                    break;
                                }
                            }
                            if (data == null) {
                                isNewData = true;
                                data = new DataManagementAdapterData();
                            }

                            if (isNewData) {
                                data.setDirDisplayName(dirDisplayName);
                            }

                            data.setSelect((adapterDataList.stream()
                                    .filter(it -> it.getDirDisplayName() != null && it.getDirDisplayName().equals(dirDisplayName))
                                    .findFirst()
                                    .map(DataManagementAdapterData::isSelect)
                                    .orElse(false)));

                            if (isNewData) {
                                list.add(data);
                            }
                        }
                    }

                    setDataToRecyclerView(list);

                    @Nullable final File previewFile = getPreviewFile();
                    if (previewFile != null) {
                        final File f = fileList.stream()
                                .filter(it -> it.getName().equals(previewFile.getName()))
                                .findFirst()
                                .orElse(null);
                        if (f == null) {
                            binding.getRoot().post(new Runnable() {
                                @Override
                                public void run() {
                                    setPreviewFile(null);
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


    @AnyThread
    private void setDataToRecyclerView(@NonNull List<@NotNull DataManagementAdapterData> list) {
        final Runnable r = new Runnable() {
            @Override
            public void run() {
                final FragmentDataManagmentBinding binding = DataManagementFragment.this.binding;
                if (binding == null) {
                    return;
                }
                final RecyclerView.Adapter<?> adapter = binding.recyclerView.getAdapter();
                if (!(adapter instanceof DataManagementAdapter)) {
                    return;
                }
                final DataManagementAdapter dataAdapter = (DataManagementAdapter) adapter;
                dataAdapter.submitList(list, () -> {
                    if (binding.loadingLayout.getVisibility() != View.GONE) {
                        binding.loadingLayout.setVisibility(View.GONE);
                    }

                    checkSelectAll();
                    setProcedureFolderTbDataToView();
                });
            }
        };

        if (Looper.getMainLooper() == Looper.myLooper()) {
            r.run();
        } else {
            final FragmentDataManagmentBinding binding = DataManagementFragment.this.binding;
            if (binding != null) {
                binding.getRoot().post(r);
            }
        }
    }

    public void checkSelectAll() {
        try {
            scheduledExecutorService.execute(new Runnable() {
                @Override
                public void run() {
                    FragmentDataManagmentBinding binding = DataManagementFragment.this.binding;
                    RecyclerView.Adapter<?> adapter = null;
                    if (binding != null) {
                        adapter = binding.recyclerView.getAdapter();
                    }
                    if (!(adapter instanceof DataManagementAdapter)) {
                        return;
                    }

                    final DataManagementAdapter dataAdapter = (DataManagementAdapter) adapter;
                    final List<DataManagementAdapterData> list = dataAdapter.getCurrentList();

                    // 檢查是否有全選
                    boolean isAll = true;

                    DataManagementAdapterData noSelectData = null;
                    for (DataManagementAdapterData item : list) {
                        if (!item.isSelect()) {
                            noSelectData = item;
                            break;
                        }
                    }

                    if (noSelectData != null) {
                        isAll = false;
                    }
                    setSelectAll(isAll);
                }
            });
        } catch (Exception e) {
            Timber.e(e);
        }
    }


    public void selectAll() {
        try {
            scheduledExecutorService.execute(new Runnable() {
                @Override
                public void run() {
                    boolean isSelectAll = !DataManagementFragment.this.isSelectAll;

                    final FragmentDataManagmentBinding binding = DataManagementFragment.this.binding;
                    if (binding == null) {
                        return;
                    }
                    binding.getRoot().post(new Runnable() {
                        @Override
                        public void run() {
                            binding.loadingLayout.setVisibility(View.VISIBLE);
                        }
                    });

                    RecyclerView.Adapter<?> adapter = binding.recyclerView.getAdapter();
                    ArrayList<DataManagementAdapterData> adapterDataList = new ArrayList<>();

                    if (adapter instanceof DataManagementAdapter) {
                        DataManagementAdapter dataAdapter = (DataManagementAdapter) adapter;
                        adapterDataList = new ArrayList<>(dataAdapter.getCurrentList());
                    }
//                    if (adapterDataList == null) {
//                        adapterDataList = new ArrayList<>();
//                    }

                    ArrayList<DataManagementAdapterData> list = new ArrayList<>();
                    for (DataManagementAdapterData d : adapterDataList) {
                        final DataManagementAdapterData newData = new DataManagementAdapterData(
                                isSelectAll,
                                d.isHighlight(),
                                d.getDirDisplayName(),
                                d.getFile()
                        );
                        list.add(newData);
                    }

                    setDataToRecyclerView(list);
                }
            });
        } catch (Exception e) {
            Timber.e(e);
        }
    }


    @SuppressLint({"UnsafeOptInUsageError"})
    private void initExoPlayer() {
        final Context context = getContext();
        if (context == null) {
            return;
        }

        FragmentDataManagmentBinding binding = DataManagementFragment.this.binding;
        if (binding == null) {
            return;
        }

        ExoPlayer exoPlayer = new ExoPlayer.Builder(context)
//                .setSeekBackIncrementMs(5000)
                .setSeekForwardIncrementMs(5000)
                .build();
        exoPlayer.addListener(exoPlayerListener);
        exoPlayer.setPlayWhenReady(true);
        exoPlayer.setPauseAtEndOfMediaItems(true);

        binding.playerView.setPlayer(exoPlayer);
        binding.playerView.setFullscreenButtonClickListener(isFullScreen -> {
            Timber.d("playerView -> FullscreenButtonClickListener isFullScreen=%s", isFullScreen);
            isPlayerFullScreen = isFullScreen;

            final ViewGroup parentView = UiTool.getParentView(binding.playerView);
            if (parentView != null) {
                parentView.removeView(binding.playerView);
            }

            if (isFullScreen) {
                binding.playerFullLayout.addView(binding.playerView);
                binding.playerFullLayout.setBackgroundResource(R.color.black);
            } else {
                binding.previewLayout.addView(binding.playerView);
                binding.playerFullLayout.setBackgroundColor(Color.TRANSPARENT);
            }
        });
    }

    @NonNull
    private final Player.Listener exoPlayerListener = new Player.Listener() {
        @Override
        public void onEvents(@NonNull Player player, @NonNull Player.Events events) {
            Player.Listener.super.onEvents(player, events);
            for (int i = 0; i < events.size(); i++) {
                Timber.d("onEvents i=" + i + " events=" + events.get(i));
            }
        }

        @Override
        public void onSurfaceSizeChanged(int width, int height) {
            Player.Listener.super.onSurfaceSizeChanged(width, height);
            Timber.d("onSurfaceSizeChanged width=" + width + " | height=" + height);
        }

        @Override
        public void onVideoSizeChanged(@NonNull VideoSize videoSize) {
            Player.Listener.super.onVideoSizeChanged(videoSize);
            Timber.d("onVideoSizeChanged videoSize=%s", videoSize);
        }

        @Override
        public void onPlaybackStateChanged(int playbackState) {
            Player.Listener.super.onPlaybackStateChanged(playbackState);
            Timber.d("onPlaybackStateChanged playbackState=%s", playbackState);
//            if (playbackState == Player.STATE_ENDED) {
//                final FragmentDataManagmentBinding binding = DataManagementFragment.this.binding;
//                Player player = null;
//                if (binding != null) {
//                    player = binding.playerView.getPlayer();
//                }
//                if (player != null) {
//                    player.seekToDefaultPosition();
//                }
//            }
        }

        @Override
        public void onPlayerError(@NonNull PlaybackException error) {
            Player.Listener.super.onPlayerError(error);
            Timber.e("onPlayerError error=%s", error);
        }

        @SuppressLint("UnsafeOptInUsageError")
        @Override
        public void onIsPlayingChanged(boolean isPlaying) {
            Player.Listener.super.onIsPlayingChanged(isPlaying);
            Timber.d("onIsPlayingChanged isPlaying=" + isPlaying);
            if (isPlaying) {
                FragmentDataManagmentBinding binding = DataManagementFragment.this.binding;
                if (binding != null) {
                    binding.playerView.hideController();
                }
            }
        }
    };


    public void cancelPlayerFullScreen() {
        final FragmentDataManagmentBinding binding = DataManagementFragment.this.binding;
        if (binding == null) {
            return;
        }
        binding.getRoot().post(new Runnable() {
            @Override
            public void run() {
                final FragmentDataManagmentBinding binding = DataManagementFragment.this.binding;
                if (binding == null) {
                    return;
                }
                binding.imgFullscreenExit.performClick();
                if (!isPlayerFullScreen) {
                    return;
                }

                PlayerControlView controller = null;
                try {
                    controller = (PlayerControlView) Tools.getPrivateObject(binding.playerView, "controller");
                } catch (Exception e) {
                    Timber.e(e);
                }
                if (controller != null) {
                    ImageView minimalFullScreenButton = null;
                    try {
                        minimalFullScreenButton = (ImageView) Tools.getPrivateObject(controller, "minimalFullScreenButton");
                    } catch (Exception e) {
                        Timber.e(e);
                    }
                    if (minimalFullScreenButton != null) {
                        minimalFullScreenButton.performClick();
                    }
                }
            }
        });
    }


    /**
     * 將預覽檔案設給畫面
     */
    private void setPreviewFileToView() {
        final FragmentDataManagmentBinding binding = DataManagementFragment.this.binding;
        if (binding == null) {
            return;
        }
        binding.getRoot().post(new Runnable() {
            @Override
            public void run() {
                final Context context = getContext();
                if (context == null) {
                    return;
                }

                @Nullable final File previewFile = getPreviewFile();

                binding.previewFileInfo.setText(previewFile != null ? previewFile.getName() : "");

                binding.previewFileInfoFull.setText(previewFile != null ? previewFile.getName() : "");

                binding.llNavigation.setVisibility(previewFile==null ? View.INVISIBLE: View.VISIBLE);

                try {
                    Glide.with(context).clear(binding.previewImg);
                } catch (Exception e) {
                    Timber.e(e);
                }

                try {
                    Glide.with(context).clear(binding.previewImgFull);
                } catch (Exception e) {
                    Timber.e(e);
                }

                binding.imgFullscreen.setVisibility(View.INVISIBLE);

                binding.previewImg.setImageDrawable(null);
                binding.previewImgFull.setImageDrawable(null);
                if (binding.playerView.getPlayer() != null) {
                    binding.playerView.getPlayer().stop();
                }
                if (previewFile != null && previewFile.getName().contains(MyApplication.VIDEO_FILE_EXTENSION)) {
                    binding.playerView.setVisibility(View.VISIBLE);
                    binding.previewImg.setVisibility(View.INVISIBLE);
                    MediaItem mediaItem = MediaItem.fromUri(Uri.parse(previewFile.getAbsolutePath()));
                    binding.playerView.getPlayer().setMediaItem(mediaItem);
                    binding.playerView.getPlayer().setPlayWhenReady(true);
                    binding.playerView.getPlayer().prepare();
                    binding.playerView.getPlayer().play();
                    binding.screenshot.setVisibility(View.VISIBLE);
                } else {
                    binding.playerView.setVisibility(View.INVISIBLE);
                    binding.previewImg.setVisibility(View.VISIBLE);
                    binding.imgFullscreen.setVisibility(View.VISIBLE);
                    if (previewFile != null) {
                        Glide.with(context).load(previewFile).into(binding.previewImg);

                        Glide.with(context).load(previewFile).into(binding.previewImgFull);
                    }
                    binding.screenshot.setVisibility(View.INVISIBLE);
                }
            }
        });
    }


    /**
     * 影片截圖(原始圖)
     */
    @SuppressLint({"UnsafeOptInUsageError", "RestrictedApi"})
    public void screenshotVideoFrame(long time) {
        final Context context = getContext();
        final File previewFile = getPreviewFile();
        if (context == null || previewFile == null) {
            return;
        }

        if (!previewFile.getName().contains(MyApplication.VIDEO_FILE_EXTENSION)) {
            return;
        }

        final String imgPath = previewFile.getParent();
        if (imgPath == null) {
            return;
        }

        try {
            scheduledExecutorService.execute(new Runnable() {
                @Override
                public void run() {
                    @Nullable Bitmap bitmap = null;
                    try(MediaMetadataRetriever retriever = new MediaMetadataRetriever()) {
                        retriever.setDataSource(context, Uri.fromFile(previewFile));
                        bitmap = retriever.getFrameAtTime(time);

                        if (bitmap != null) {
                            File filesDirFile = new File(imgPath);
                            if (!filesDirFile.exists()) {
                                try {
                                    filesDirFile.mkdirs();
                                } catch (Exception e) {
                                    Timber.e(e);
                                }
                            }

                            final ZonedDateTime zonedDateTime = ZonedDateTime.now();

                            final String outputFileString = previewFile.getName().replace(MyApplication.VIDEO_FILE_EXTENSION, "") +
                                    "_" +
                                    zonedDateTime.toLocalTime().format(DateTimeFormatter.ofPattern("HHmmssSSS")) + MyApplication.IMG_FILE_EXTENSION;
                            File outputFile = new File(filesDirFile, outputFileString);

                            OutputStream stream = Files.newOutputStream(outputFile.toPath());
                            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
                            stream.close();

                            ExifInterface exif = null;
                            try {
                                exif = new ExifInterface(outputFile.getAbsolutePath());
                            } catch (Exception e) {
                                Timber.e(e);
                            }

                            if (exif != null) {
                                try {
                                    exif.setDateTime(zonedDateTime.toInstant().toEpochMilli());
                                    exif.saveAttributes();
                                } catch (Exception e) {
                                    Timber.e(e);
                                }
                            }
                        }
                    } catch (Exception e) {
                        Timber.e(e);
                    } finally {
                        try {
                            if (bitmap != null) {
                                bitmap.recycle();
                            }
                        } catch (Exception e) {
                            Timber.e(e);
                        }
//                        try {
//                            retriever.release();
//                        } catch (Exception e) {
//                            Timber.e(e);
//                        }
                    }
                    final FragmentDataManagmentBinding binding = DataManagementFragment.this.binding;
                    if (binding != null) {
                        binding.getRoot().post(new Runnable() {
                            @Override
                            public void run() {
                                updateFileList();
                            }
                        });
                    }
                }
            });
        } catch (Exception e) {
            Timber.e(e);
        }
    }

    private void setSelectAllToView() {
        final FragmentDataManagmentBinding binding = DataManagementFragment.this.binding;
        if (binding == null) {
            return;
        }
        binding.getRoot().post(new Runnable() {
            @Override
            public void run() {
                final boolean isSelectAll = isSelectAll();
                if (isSelectAll) {
                    binding.selectAllText.setText(R.string.unselect_all);
                } else {
                    binding.selectAllText.setText(R.string.select_all);
                }
            }
        });
    }


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
        showMyDialog(
                context.getString(msg),
                isShowConfirm,
                context.getString(confirmBtnText),
                isShowCancel,
                context.getString(cancelBtnText),
                listener,
                isReverseBin
        );
    }

    private void showMyDialog(
            @Nullable String msg,
            boolean isShowConfirm,
            @Nullable String confirmBtnText,
            boolean isShowCancel,
            @Nullable String cancelBtnText,
            @Nullable MyDialog.Listener listener,
            boolean isReverseBin
    ) {
        if (!isResumed() || !isVisible()) {
            return;
        }
        cancelMyDialog();
        Runnable r = new Runnable() {
            @Override
            public void run() {
                final Context context = getContext();
                if (context == null) {
                    return;
                }
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
            final FragmentDataManagmentBinding binding = DataManagementFragment.this.binding;
            if (binding != null) {
                binding.getRoot().post(r);
            }
        }
    }


    private void cancelMyDialog() {
        Runnable r = new Runnable() {
            @Override
            public void run() {
                final MyDialog myDialog = DataManagementFragment.this.myDialog;
                if (myDialog != null && myDialog.isShowing()) {
                    myDialog.dismiss();
                }
                DataManagementFragment.this.myDialog = null;
            }
        };
        if (Looper.myLooper() == Looper.getMainLooper()) {
            r.run();
        } else {
            final FragmentDataManagmentBinding binding = DataManagementFragment.this.binding;
            if (binding != null) {
                binding.getRoot().post(r);
            }
        }
    }

    private void showGetFileCountDialog()
    {
        Runnable r=new Runnable() {
            @Override
            public void run() {
                final Context context = getContext();
                if (context == null) {
                    return;
                }
                GetFileCountDialog = new ProgressDialog(context);
                String msg = context.getResources().getString(R.string.copy_get_file_count);
                GetFileCountDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                GetFileCountDialog.setMessage(msg);
                GetFileCountDialog.setIcon(0);
                GetFileCountDialog.setIndeterminate(false);
                GetFileCountDialog.show();
                GetFileCountDialog.setOnCancelListener(null);
                GetFileCountDialog.setCanceledOnTouchOutside(false);
            }
        };


        if (Looper.myLooper() == Looper.getMainLooper()) {
            r.run();
        } else {
            final FragmentDataManagmentBinding binding = DataManagementFragment.this.binding;
            if (binding != null) {
                binding.getRoot().post(r);
            }
        }
    }

    private void cancelGetFileCountDialog() {
        Runnable r = new Runnable() {
            @Override
            public void run() {
                final ProgressDialog GetFileCountDialog = DataManagementFragment.this.GetFileCountDialog;
                if (GetFileCountDialog != null &&GetFileCountDialog.isShowing()) {
                    GetFileCountDialog.dismiss();
                }
                DataManagementFragment.this.GetFileCountDialog = null;
            }
        };
        if (Looper.myLooper() == Looper.getMainLooper()) {
            r.run();
        } else {
            final FragmentDataManagmentBinding binding = DataManagementFragment.this.binding;
            if (binding != null) {
                binding.getRoot().post(r);
            }
        }
    }

    @UiThread
    private void delete() {
        final MyApplication myApplication = getMyApplication();
        if (myApplication == null) {
            return;
        }

        final UserTbData loginUserTbData = myApplication.getLoginUserTbData();
        if (loginUserTbData == null) {
            return;
        }

        final UserRoleType roleType = loginUserTbData.getRoleType();

        FragmentDataManagmentBinding binding = DataManagementFragment.this.binding;

        RecyclerView.Adapter<?> adapter = null;
        if (binding != null) {
            adapter = binding.recyclerView.getAdapter();
        }
        if (!(adapter instanceof DataManagementAdapter)) {
            return;
        }

        final DataManagementAdapter dataManagementAdapter = (DataManagementAdapter) adapter;
        final List<DataManagementAdapterData> adapterDataList = dataManagementAdapter.getCurrentList();

        boolean hasSelect = false;
        for (DataManagementAdapterData data : adapterDataList) {
            if (data.isSelect()) {
                hasSelect = true;
                break;
            }
        }

        if (!hasSelect) {
            return;
        }

        showMyDialog(
                R.string.do_you_want_to_delete,
                true,
                R.string.yes,
                true,
                R.string.no,
                new MyDialog.Listener() {
                    @Override
                    public void OnClickConfirm() {

                        try {
                            scheduledExecutorService.execute(new Runnable() {
                                @Override
                                public void run() {

                                    final FragmentDataManagmentBinding binding = DataManagementFragment.this.binding;
                                    if (binding != null) {
                                        binding.getRoot().post(new Runnable() {
                                            @Override
                                            public void run() {
                                                binding.loadingLayout.setVisibility(View.VISIBLE);
                                            }
                                        });
                                    }


                                    final ArrayList<@NotNull DataManagementAdapterData> selectList = new ArrayList<>();
                                    for (DataManagementAdapterData data : adapterDataList) {
                                        if (data.isSelect()) {
                                            selectList.add(data);
                                        }
                                    }

                                    @Nullable final File previewFile = getPreviewFile();

                                    final ArrayList<@NotNull File> deleteFileList = new ArrayList<>();
                                    final File mainDirFile = new File(myApplication.getMainDirPath());
                                    for (DataManagementAdapterData selectData : selectList) {
                                        @Nullable final String dirDisplayName = selectData.getDirDisplayName();
                                        @Nullable final File file = selectData.getFile();
                                        if (dirDisplayName != null) {
                                            final String[] dirDisplayNameArr = dirDisplayName.split("_");
                                            if (dirDisplayNameArr.length >= 2) {
                                                List<File> childDirFileList = null;
                                                try {
                                                    File[] childDirFileArr = mainDirFile.listFiles(new FileFilter() {
                                                        @Override
                                                        public boolean accept(File childDirFile) {
                                                            @NonNull String[] childDirNameArr = childDirFile.getName().split("_");
                                                            if (childDirFile.isDirectory() && childDirNameArr.length >= 3) {
                                                                switch (roleType) {
                                                                    case GUEST:
                                                                        return false;
                                                                    case ADVANCED_USER:
                                                                        return childDirNameArr[0].equals(loginUserTbData.getAccount()) &&
                                                                                childDirNameArr[1].equals(dirDisplayNameArr[0]) &&
                                                                                childDirNameArr[2].equals(dirDisplayNameArr[1]);
                                                                    case ADMIN_USER:
                                                                    case SERVICE_USER:
                                                                        return childDirNameArr[1].equals(dirDisplayNameArr[0]) &&
                                                                                childDirNameArr[2].equals(dirDisplayNameArr[1]);
                                                                    default:
                                                                        return false;
                                                                }
                                                            } else {
                                                                return false;
                                                            }
                                                        }
                                                    });
                                                    if (childDirFileArr == null) {
                                                        childDirFileArr = new File[]{};
                                                    }
                                                    childDirFileList = Arrays.asList(childDirFileArr);
                                                } catch (Exception e) {
                                                    Timber.e(e);
                                                }

                                                if (childDirFileList != null) {
                                                    deleteFileList.addAll(childDirFileList);
                                                }
                                            }
                                        } else if (file != null) {
                                            deleteFileList.add(file);
                                        }

                                    }

                                    for (File deleteFile : deleteFileList) {
                                        if (deleteFile.isDirectory()) {
                                            if (previewFile != null) {
                                                boolean isPreviewFile = false;
                                                File[] childFiles = new File[]{};
                                                try {
                                                    childFiles = deleteFile.listFiles();
                                                } catch (Exception e) {
                                                    Timber.e(e);
                                                }
                                                if (childFiles != null) {
                                                    for (File childFile : childFiles) {
                                                        if (TextUtils.equals(childFile.getName(), previewFile.getName())) {
                                                            isPreviewFile = true;
                                                            break;
                                                        }
                                                    }
                                                }

                                                if (isPreviewFile && binding != null) {
                                                    binding.getRoot().post(new Runnable() {
                                                        @Override
                                                        public void run() {
                                                            setPreviewFile(null);
                                                        }
                                                    });
                                                }
                                            }

                                            try {
                                                final String deleteFilePath = deleteFile.getPath();
                                                FileUtils.deleteDirectory(deleteFile);
                                                final ProcedureFolderTbData procedureFolderTbData = myApplication.getMyRoomDatabase()
                                                        .procedureFolderTbDataDao().find(deleteFilePath);
                                                if (procedureFolderTbData != null) {
                                                    myApplication.getMyRoomDatabase().procedureFolderTbDataDao()
                                                            .delete(procedureFolderTbData);
                                                    Log.i(TAG, "Delete the procedure folder: " +deleteFile.getName());
                                                }
                                            } catch (Exception e) {
                                                Timber.e(e);
                                            }
                                        } else {
                                            if (previewFile != null) {
                                                boolean isPreviewFile = TextUtils.equals(deleteFile.getName(), previewFile.getName());
                                                if (isPreviewFile && binding != null) {
                                                    binding.getRoot().post(new Runnable() {
                                                        @Override
                                                        public void run() {
                                                            setPreviewFile(null);
                                                        }
                                                    });
                                                }
                                            }

                                            try {
                                                if(!deleteFile.delete()){
                                                    Timber.w("delete file fail: "+deleteFile.getAbsolutePath());
                                                }
                                            } catch (Exception e) {
                                                Timber.e(e);
                                            }
                                        }
                                    }

                                    if (binding != null) {
                                        binding.getRoot().post(new Runnable() {
                                            @Override
                                            public void run() {
                                                updateFileList();
                                                final MainActivity mainActivity = myApplication.getMainActivity();
                                                if (mainActivity != null) {
                                                    if (mainActivity.getCameraFragment()
                                                            .getBinding() != null) {
                                                        mainActivity.getCameraFragment()
                                                                .getBinding().previewWindowsView.checkSelectImgFileExists();
                                                    }
                                                }
                                            }
                                        });
                                    }
                                }
                            });
                        } catch (Exception e) {
                            Timber.e(e);
                        }
                    }

                    @Override
                    public void OnClickCancel() {

                    }
                }, true);
    }

    private final MyStorageManager.Listener myStorageManagerListener = new MyStorageManager.Listener() {
        @Override
        public void onStorageMounted(@NonNull File storageFile) {
            checkHasUsrOrSdCard();
        }

        @Override
        public void onStorageUnmounted(@NonNull File storageFile) {
            checkHasUsrOrSdCard();

            try {
                final Disposable basicExportDisposable = DataManagementFragment.this.basicExportDisposable;
                if (basicExportDisposable != null) {
                    basicExportDisposable.dispose();
                }
                DataManagementFragment.this.basicExportDisposable = null;
            } catch (Exception e) {
                Timber.e(e);
            }

            try {
                final Disposable dicomExportDisposable = DataManagementFragment.this.dicomExportDisposable;
                if (dicomExportDisposable != null) {
                    dicomExportDisposable.dispose();
                }
                DataManagementFragment.this.dicomExportDisposable = null;
            } catch (Exception e) {
                Timber.e(e);
            }

            final ZipFilesProgressDialog zipFilesProgressDialog = DataManagementFragment.this.zipFilesProgressDialog;
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
        }
    };


    private void checkHasUsrOrSdCard() {
        final FragmentDataManagmentBinding binding = DataManagementFragment.this.binding;
        if (binding != null) {
            binding.getRoot().post(new Runnable() {
                @Override
                public void run() {
                    boolean isStorageListNotEmpty = !MyStorageManager.getInstance().getStorageList().isEmpty();

                    //JerryLin +
                    final MyApplication myApplication = getMyApplication();
                    if (myApplication == null) {
                        Timber.w("[checkHasUsrOrSdCard] myApplication is null");
                        return;
                    }

                    UserTbData loginUserTbData = myApplication.getLoginUserTbData();
                    if (loginUserTbData == null) {
                        Timber.w("[checkHasUsrOrSdCard] loginUserTbData is null");
                        return;
                    }

                    if ((loginUserTbData.getRoleType() == UserRoleType.ADVANCED_USER) && (loginUserTbData.getAccount().equals(myApplication.DEMO_ACCOUNT))) {
                        binding.basicExport.setEnabled(false);
                        binding.dicomExport.setEnabled(false);
                        binding.delete.setEnabled(false);
                    }
                    else {
                        binding.basicExport.setEnabled(isStorageListNotEmpty);
                        binding.dicomExport.setEnabled(isStorageListNotEmpty);
                        binding.delete.setEnabled(true);
                    }
                    //JerryLin -

//                    binding.basicExport.setEnabled(isStorageListNotEmpty);
//                    binding.dicomExport.setEnabled(isStorageListNotEmpty);
                }
            });
        }else{
            Timber.w("[checkHasUsrOrSdCard] binding == null");
        }
    }

    private void basicExportZip(
            @NonNull Map<@NotNull File,
                    @NotNull List<@NotNull File>> fileMapList,
            @NonNull String exportText,
            @NonNull String password,
            @NonNull StorageVolume storage) {
        if (basicExportDisposable != null) {
            return;
        }
        Timber.d("basicExport");

        final FragmentDataManagmentBinding binding = DataManagementFragment.this.binding;
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
                                        basicExportDisposable = d;
                                    }

                                    @Override
                                    public void onNext(@Nullable MyStorageManager.ZipProgressData progress) {
                                        Timber.d("basicExportZip -> compressFilesToZipsWithPasswordFlow -> onNext progress=%s", progress==null? "Null": progress.toString());
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
                                        basicExportDisposable = null;
                                        try {
                                            MyApplication myApplication = getMyApplication();
                                            if (myApplication != null) {
                                                File zipTempFile = new File(myApplication.getMainDirPath() + File.separator + MyApplication.ZIP_TEMP_NAME);
                                                if (zipTempFile.exists()) {
                                                    FileUtils.deleteDirectory(zipTempFile);
                                                }
                                            }
                                        } catch (Exception e) {
                                            Timber.e(e);
                                        }

                                        binding.getRoot().post(() -> {
                                            final ZipFilesProgressDialog zipFilesProgressDialog = DataManagementFragment.this.zipFilesProgressDialog;
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
                                                                        Timber.w("delete file fail: "+jsonFile.getAbsolutePath());
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
                                        basicExportDisposable = null;
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
                                                                    Timber.w("delete file fail: "+jsonFile.getAbsolutePath());
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
                                        }finally {

                                            //刪除dicom temp files
                                            final File dicomTempFile=getDicomTempFolder();
                                            if(dicomTempFile!=null && dicomTempFile.exists()){
                                                try {
                                                    FileUtils.deleteDirectory(dicomTempFile);
                                                } catch (IOException e) {
                                                    Timber.w("Exception: "+ e.getMessage());
                                                }
                                            }
                                        }
                                    }
                                });
                Timber.d("basicExportZip All files copied successfully");
            }
        });
    }

    private void dicomExport(@NonNull Map<@NotNull File, @NotNull List<@NotNull File>> fileMapList,
                             @NonNull String exportText, @NonNull String password, @NonNull StorageVolume storage) {
        if (dicomExportDisposable != null) {
            return;
        }

        Timber.d("dicomExport");

        final FragmentDataManagmentBinding binding = DataManagementFragment.this.binding;
        if (binding == null) {
            return;
        }

        try {
            scheduledExecutorService.execute(new Runnable() {
                @Override
                public void run() {
                    final ArrayList<@NotNull File> selectFileList = new ArrayList<>();
                    final ArrayList<@NotNull Pair<@NotNull File, @NotNull File>> filesToCopy = new ArrayList<>();
                    for (Map.Entry<File, List<File>> fileMap : fileMapList.entrySet()) {
                        for (File selectFile : fileMap.getValue()) {
                            String name = selectFile.getName();
                            name=name.substring(0, name.length()-4)+".dcm";
                            final File targetFile = new File(fileMap.getKey().getAbsolutePath() + File.separator + name);
                            filesToCopy.add(new Pair<>(selectFile, targetFile));
                        }

                        selectFileList.addAll(fileMap.getValue());
                    }

                    binding.getRoot().post(new Runnable() {
                        @Override
                        public void run() {
                            final CopyFilesProgressDialog dialog = showCopyFilesProgressDialog(selectFileList, exportText);
                            if (dialog != null) {
                                dialog.showCloseBtn();
                            }


                            final Map<Integer,String > dicomInfo = new HashMap<Integer,String>();

                            final CharSequence patientIdText = binding.id.getText();
                            @Nullable final String patientId = patientIdText != null ? patientIdText.toString().trim() : null;
                            dicomInfo.put(Tag.PatientID,patientId);

                            final Editable ageText = binding.age.getText();
                            @Nullable final String age = ageText != null ? ageText.toString().trim() : null;
                            dicomInfo.put(Tag.PatientAge,age);

                            final CharSequence visitDateText = binding.visitDate.getText();
                            @Nullable final String visitDate =visitDateText != null ? visitDateText.toString().trim() : null;
                            dicomInfo.put(Tag.StudyDate,visitDate);

                            dicomInfo.put(Tag.PatientSex,binding.female.isChecked()? "F": "M");

                            final CharSequence remarkText = binding.remark.getText();
                            @Nullable final String remark = remarkText != null ? remarkText.toString().trim() : null;
                            dicomInfo.put(Tag.StudyDescription,remark);

                            MyStorageManager.getInstance().image2DicomWithProgressFlow(filesToCopy, dicomInfo).subscribe(new Observer<MyStorageManager.ProgressData>() {

                                @Override
                                public void onSubscribe(@io.reactivex.rxjava3.annotations.NonNull Disposable d) {
                                    dicomExportDisposable = d;
                                }

                                @Override
                                public void onError(Throwable t) {
                                    Timber.e("dicomExport -> copyFilesWithProgressFlow -> onError");
                                    Timber.e(t);
                                    dicomExportDisposable = null;

                                    binding.getRoot().post(() -> {
                                        if (copyFilesProgressDialog != null) {
                                            copyFilesProgressDialog.showFail();
                                        }
                                    });
                                }

                                @Override
                                public void onComplete() {
                                    Timber.d("dicomExport -> copyFilesWithProgressFlow -> onComplete");
                                    dicomExportDisposable = null;

                                    dialog.dismiss();

                                    Message msg = new Message();
                                    msg.what = 1;
                                    final Bundle bundle = new Bundle();
                                    bundle.putString(KEY_PASSWORD, password);
                                    bundle.putParcelable(KEY_STORAGE, storage);
                                    msg.setData(bundle);
                                    mHandler.sendMessageDelayed(msg,0);


                                }

                                @Override
                                public void onNext(MyStorageManager.ProgressData t) {
                                    Timber.d("dicomExport -> copyFilesWithProgressFlow -> onNext progressData=" + t);
                                    if (t != null) {
                                        if (dialog != null) {
                                            dialog.setProgressData(t);
                                        }
                                    }
                                }
                            });

                            Timber.d("dicomExport All files copied successfully");



                        }
                    });
                }
            });
        } catch (Exception e) {
            Timber.e(e);
        }
    }


    @UiThread
    private void showMySelectStorageDialog(@NonNull Map<@NotNull String, @NotNull List<@NotNull File>> selectFileGroupMap, boolean isDicomExport, @NonNull String password) {
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
                    if (isDicomExport) {
                        if(selectFileGroup.getValue().size()<=0){
                            continue;
                        }
                        final File dicomTempFolder = getDicomTempFolder();
                        if(dicomTempFolder!=null) {
                            final File file = new File(dicomTempFolder, File.separator + selectFileGroup.getKey());
                            fileMapList.put(file, selectFileGroup.getValue());
                        }
                    } else {
                        fileMapList.put(new File(exportFile.getAbsolutePath() + File.separator + MyApplication.FOLDER_NAME + File.separator + selectFileGroup.getKey() + ".zip"), selectFileGroup.getValue());
                    }
                }

                if (isDicomExport) {
                    dicomExport(fileMapList, data.getText(), password, storageVolume);
                } else {
                    basicExportZip(fileMapList, data.getText(), password, storageVolume);
                }
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


    @UiThread
    @Nullable
    private CopyFilesProgressDialog showCopyFilesProgressDialog(@NonNull List<@NotNull File> selectFileList, @NonNull String exportText) {
        cancelCopyFilesProgressDialog();

        final Context context = getContext();
        if (context == null) {
            return null;
        }

        final CopyFilesProgressDialog dialog = new CopyFilesProgressDialog(context);
        dialog.setTitle(selectFileList.size(), exportText);

        dialog.setProgressData(new MyStorageManager.ProgressData(
                0, 0L, 0L, 0L, selectFileList.get(0),
                selectFileList.stream().mapToLong(File::length).sum(), 0L
        ));

        dialog.setListener(new CopyFilesProgressDialog.Listener() {
            @Override
            public void onCancel() {
                try {
                    final Disposable basicExportDisposable = DataManagementFragment.this.basicExportDisposable;
                    if (basicExportDisposable != null) {
                        basicExportDisposable.dispose();
                    }
                    DataManagementFragment.this.basicExportDisposable = null;
                } catch (Exception e) {
                    Timber.e(e);
                }

                try {
                    final Disposable dicomExportDisposable = DataManagementFragment.this.dicomExportDisposable;
                    if (dicomExportDisposable != null) {
                        dicomExportDisposable.dispose();
                    }
                    DataManagementFragment.this.dicomExportDisposable = null;
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

        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                try {
                    final Disposable basicExportDisposable = DataManagementFragment.this.basicExportDisposable;
                    if (basicExportDisposable != null) {
                        basicExportDisposable.dispose();
                    }
                    DataManagementFragment.this.basicExportDisposable = null;
                } catch (Exception e) {
                    Timber.e(e);
                }

                try {
                    final Disposable dicomExportDisposable = DataManagementFragment.this.dicomExportDisposable;
                    if (dicomExportDisposable != null) {
                        dicomExportDisposable.dispose();
                    }
                    DataManagementFragment.this.dicomExportDisposable = null;
                } catch (Exception e) {
                    Timber.e(e);
                }
            }
        });

        dialog.show();

        copyFilesProgressDialog = dialog;

        return dialog;
    }

    @UiThread
    private void cancelCopyFilesProgressDialog() {
        final CopyFilesProgressDialog copyFilesProgressDialog = DataManagementFragment.this.copyFilesProgressDialog;
        if (copyFilesProgressDialog != null && copyFilesProgressDialog.isShowing()) {
            copyFilesProgressDialog.dismiss();
        }
        DataManagementFragment.this.copyFilesProgressDialog = null;
    }


    @UiThread
    private void showRemarkEditDialog() {
        cancelRemarkEditDialog();
        final FragmentDataManagmentBinding binding = DataManagementFragment.this.binding;
        if (binding == null) {
            return;
        }
        String currentText = null;
        try {
            currentText = binding.remark.getText().toString();
        } catch (Exception e) {
            Timber.e(e);
        }
        if (currentText == null) {
            currentText = "";
        }
        final RemarkEditDialog d = RemarkEditDialog.newInstance(currentText);
        d.setListener(new RemarkEditDialog.Listener() {
            @Override
            public void onClickConfirm(@NonNull String text) {
                binding.remark.setText(text);

//                scheduledExecutorService.execute(new Runnable() {
//                    @Override
//                    public void run() {
//                        @NonNull final List<ProcedureFolderTbData> procedureFolderTbDataList = getHighlightProcedureFolderTbDataList();
//                        for (ProcedureFolderTbData procedureFolderTbData : procedureFolderTbDataList) {
//                            procedureFolderTbData.setRemark(text);
//                        }
//                        try {
//                            MyApplication myApplication = getMyApplication();
//                            if (myApplication != null) {
//                                myApplication.getMyRoomDatabase().procedureFolderTbDataDao().updates(procedureFolderTbDataList);
//                            }
//                        } catch (Exception e) {
//                            Timber.e(e);
//                        }
//                    }
//                });
            }

            @Override
            public void onClickCancel() {

            }

            @Override
            public void onTouch() {
                FragmentActivity activity = getActivity();
                if (activity instanceof MainActivity) {
                    ((MainActivity) activity).restartStandbyNotificationTimeServiceTimer();
                }
            }
        });
        d.show(getChildFragmentManager(), RemarkEditDialog.class.getSimpleName());
        remarkEditDialog = d;
    }


    @UiThread
    private void cancelRemarkEditDialog() {
        final RemarkEditDialog remarkEditDialog = DataManagementFragment.this.remarkEditDialog;
        if (remarkEditDialog != null && remarkEditDialog.isVisible()) {
            if (remarkEditDialog.isStateSaved()) {
                remarkEditDialog.dismissAllowingStateLoss();
            } else {
                remarkEditDialog.dismiss();
            }
        }
        DataManagementFragment.this.remarkEditDialog = null;
    }


    private void setChildDirDisplayNameToView() {
        final Runnable r = new Runnable() {
            @Override
            public void run() {
                final FragmentDataManagmentBinding binding = DataManagementFragment.this.binding;
                if (binding == null) {
                    return;
                }
                @Nullable final String childDirDisplayName = getChildDirDisplayName();
                if (childDirDisplayName == null || childDirDisplayName.isEmpty()) {
                    binding.upperLevel.setVisibility(View.INVISIBLE);
                    binding.childDir.setText("");
                } else {
                    binding.upperLevel.setVisibility(View.VISIBLE);
                    binding.childDir.setText(childDirDisplayName);
                }
            }
        };
        if (Looper.myLooper() == Looper.getMainLooper()) {
            r.run();
        } else {
            final FragmentDataManagmentBinding binding = DataManagementFragment.this.binding;
            if (binding == null) {
                return;
            }
            binding.getRoot().post(r);
        }
    }


    public void setProcedureFolderTbDataToView() {
        try {
            scheduledExecutorService.execute(new Runnable() {
                @Override
                public void run() {
                    @NonNull final List<ProcedureFolderTbData> procedureFolderTbDataList = getHighlightProcedureFolderTbDataList();
//                    @Nullable final ProcedureFolderTbData procedureFolderTbData = procedureFolderTbDataList.isEmpty() ? null : procedureFolderTbDataList.get(0);
                    @Nullable final ProcedureFolderTbData procedureFolderTbData;
                    if(procedureFolderTbDataList.isEmpty()){
                        final RecyclerView.Adapter<?> adapter = binding.recyclerView.getAdapter();
                        if (adapter instanceof DataManagementAdapter) {
                            DataManagementAdapter dataManagementAdapter = (DataManagementAdapter) adapter;
                            if(!dataManagementAdapter.getCurrentList().isEmpty()){
                                final DataManagementAdapterData dataManagementAdapterData = dataManagementAdapter.getCurrentList().get(0);
                                final File file = dataManagementAdapterData.getFile();
                                final MyApplication myApplication = getMyApplication();
                                if(myApplication!=null && !file.isDirectory()) { //檔案清單
                                    procedureFolderTbData = myApplication.getMyRoomDatabase().procedureFolderTbDataDao().find(file.getParentFile().getPath());
                                }else{
                                    procedureFolderTbData=null;
                                }
                            }else{
                                procedureFolderTbData=null;
                            }
                        }else{
                            procedureFolderTbData=null;
                        }
                    }else{ //資料夾清單
                        procedureFolderTbData= procedureFolderTbDataList.get(0);
                    }

                    Timber.d("setProcedureFolderTbDataToView procedureFolderTbData=%s", procedureFolderTbData==null? "Null": procedureFolderTbData.toString());

                    final FragmentDataManagmentBinding binding = DataManagementFragment.this.binding;
                    if (binding == null) {
                        return;
                    }
                    binding.getRoot().post(new Runnable() {
                        @Override
                        public void run() {
                            binding.id.setText(procedureFolderTbData != null ? procedureFolderTbData.getPatientId() : "");

                            binding.age.removeTextChangedListener(ageTextChangedListener);
                            binding.age.setText(procedureFolderTbData != null ? procedureFolderTbData.getAge() : "");
                            binding.age.addTextChangedListener(ageTextChangedListener);

                            binding.visitDate.setText(procedureFolderTbData != null ? procedureFolderTbData.getCreateDateTime() : "");

                            binding.genderLayout.setOnCheckedChangeListener(null);
                            GenderType genderType = procedureFolderTbData != null ? procedureFolderTbData.getGenderType() : null;
                            if (genderType == null) {
                                binding.genderLayout.clearCheck();
                            } else {
                                switch (genderType) {
                                    case MALE:
                                        binding.male.toggle();
                                        break;
                                    case FEMALE:
                                        binding.female.toggle();
                                        break;
                                    default:
                                        binding.genderLayout.clearCheck();
                                        break;
                                }
                            }
                            binding.genderLayout.setOnCheckedChangeListener(genderLayoutOnCheckedChangeListener);

                            binding.remark.setText(procedureFolderTbData != null ? procedureFolderTbData.getRemark() : "");
                        }
                    });
                }
            });
        } catch (Exception e) {
            Timber.e(e);
        }
    }


    @WorkerThread
    @NonNull
    private synchronized List<@NotNull ProcedureFolderTbData> getHighlightProcedureFolderTbDataList() {
        final MyApplication myApplication = getMyApplication();
        final FragmentDataManagmentBinding binding = DataManagementFragment.this.binding;
        if (binding == null) {
            return Collections.emptyList();
        }
        if (myApplication == null) {
            return Collections.emptyList();
        }
        @Nullable final UserTbData loginUserTbData = myApplication.getLoginUserTbData();
        @NonNull final UserRoleType roleType = (loginUserTbData != null) ? loginUserTbData.getRoleType() : UserRoleType.GUEST;
        final RecyclerView.Adapter<?> adapter = binding.recyclerView.getAdapter();
        @Nullable DataManagementAdapterData highlightAdapterData = null;

        if (adapter instanceof DataManagementAdapter) {
            DataManagementAdapter dataManagementAdapter = (DataManagementAdapter) adapter;
            final Stream<DataManagementAdapterData> stream = dataManagementAdapter.getCurrentList().stream();
            highlightAdapterData = stream
                    .filter(DataManagementAdapterData::isHighlight)
                    .findFirst()
                    .orElse(null);
        }

        boolean isDisplayName = (highlightAdapterData != null && highlightAdapterData.getDirDisplayName() != null && !highlightAdapterData.getDirDisplayName().isEmpty());
        @Nullable final String name = (isDisplayName) ?
                highlightAdapterData.getDirDisplayName() :
                highlightAdapterData != null ?
                        highlightAdapterData.getFile() != null ?
                                highlightAdapterData.getFile().getParentFile() != null ?
                                        highlightAdapterData.getFile().getParentFile().getName() : null : null : null;

        @NonNull final List<String> nameArr = name != null ? Arrays.asList(name.split("_")) : new ArrayList<>();

        @Nullable String accountFromFile = null;
        if (isDisplayName) {
            switch (roleType) {
                case GUEST:
                    accountFromFile = null;
                    break;
                case ADVANCED_USER:
                    accountFromFile = loginUserTbData.getAccount();
                    break;
                case ADMIN_USER:
                case SERVICE_USER:
                    accountFromFile = null;
                    break;
            }
        } else {
            accountFromFile = nameArr.stream().findFirst().orElse(null);
        }

        String patientIdFromFile = null;
        if (isDisplayName) {
            patientIdFromFile = nameArr.stream().findFirst().orElse(null);
        } else {
            if (nameArr.size() >= 2) {
                patientIdFromFile = nameArr.get(1);
            }
        }

        LocalDate localDateFromFile = null;
        if (isDisplayName) {
            if (nameArr.size() >= 2) {
                try {
                    localDateFromFile = LocalDate.parse(nameArr.get(1), DateTimeFormatter.ofPattern(CameraFragment.DIR_NAME_DATE_FORMAT_PATTERN, Locale.ENGLISH));
                } catch (Exception e) {
                    Timber.e(e);
                    localDateFromFile = null;
                }
            }
        } else {
            if (nameArr.size() >= 3) {
                try {
                    localDateFromFile = LocalDate.parse(nameArr.get(2), DateTimeFormatter.ofPattern(CameraFragment.DIR_NAME_DATE_FORMAT_PATTERN, Locale.ENGLISH));
                } catch (Exception e) {
                    Timber.e(e);
                    localDateFromFile = null;
                }
            }
        }

        @Nullable List<ProcedureFolderTbData> procedureFolderTbDataListDb = null;
        switch (roleType) {
            case GUEST:
                procedureFolderTbDataListDb = null;
                break;
            case ADVANCED_USER:
                if (accountFromFile != null && patientIdFromFile != null && localDateFromFile != null) {
                    try {
                        procedureFolderTbDataListDb = myApplication.getMyRoomDatabase().procedureFolderTbDataDao().findDataList(accountFromFile, patientIdFromFile, localDateFromFile.toString());
                    } catch (Exception e) {
                        Timber.e(e);
                        procedureFolderTbDataListDb = null;
                    }
                } else {
                    procedureFolderTbDataListDb = null;
                }
                break;
            case ADMIN_USER:
            case SERVICE_USER:
                if (patientIdFromFile != null && localDateFromFile != null) {
                    try {
                        procedureFolderTbDataListDb = myApplication.getMyRoomDatabase().procedureFolderTbDataDao().findDataList(patientIdFromFile, localDateFromFile.toString());
                    } catch (Exception e) {
                        Timber.e(e);
                        procedureFolderTbDataListDb = null;
                    }
                } else {
                    procedureFolderTbDataListDb = null;
                }
                break;
        }

        final ArrayList<ProcedureFolderTbData> list = new ArrayList<>();
        if (procedureFolderTbDataListDb != null) {
            list.addAll(procedureFolderTbDataListDb);
        }

        final File mainDirFile = new File(myApplication.getMainDirPath());
        List<File> childDirFileList = null;
        try {
            final String finalPatientIdFromFile = patientIdFromFile;
            final LocalDate finalLocalDateFromFile = localDateFromFile;
            final String finalAccountFromFile = accountFromFile;
            File[] childDirFileArr = mainDirFile.listFiles(childDirFile -> {
                List<String> childDirNameArr = Arrays.asList(childDirFile.getName().split("_"));
                if (childDirNameArr.size() >= 3 && finalPatientIdFromFile != null && finalLocalDateFromFile != null) {
                    switch (roleType) {
                        case GUEST:
                            return false;
                        case ADVANCED_USER:
                            return (finalAccountFromFile != null &&
                                    loginUserTbData.getAccount().equals(childDirNameArr.get(0)) &&
                                    finalPatientIdFromFile.equals(childDirNameArr.get(1)) &&
                                    finalLocalDateFromFile.format(DateTimeFormatter.ofPattern(CameraFragment.DIR_NAME_DATE_FORMAT_PATTERN, Locale.ENGLISH)).equals(childDirNameArr.get(2)));
                        case ADMIN_USER:
                        case SERVICE_USER:
                            return (finalPatientIdFromFile.equals(childDirNameArr.get(1)) &&
                                    finalLocalDateFromFile.format(DateTimeFormatter.ofPattern(CameraFragment.DIR_NAME_DATE_FORMAT_PATTERN, Locale.ENGLISH)).equals(childDirNameArr.get(2)));
                        default:
                            return false;
                    }
                } else {
                    return false;
                }
            });
            if (childDirFileArr == null) {
                childDirFileArr = new File[]{};
            }
            childDirFileList = Arrays.asList(childDirFileArr);
        } catch (Exception e) {
            Timber.e(e);
        }

        if (childDirFileList == null) {
            childDirFileList = new ArrayList<>();
        }

        for (File childDirFile : childDirFileList) {
            final boolean isHasData = list.stream().anyMatch(data -> data.getFilePath().equals(childDirFile.getPath()));
            if (isHasData) {
                continue;
            }
            final List<String> childDirNameArr = Arrays.asList(childDirFile.getName().split("_"));
            if (childDirNameArr.size() >= 3) {
                ProcedureFolderTbData data = new ProcedureFolderTbData();
                data.setId(UUID.randomUUID().toString());
                data.setFilePath(childDirFile.getPath());
                data.setAccount(childDirNameArr.get(0));
                data.setPatientId(childDirNameArr.get(1));
                try {
                    data.setCreateDate(LocalDate.parse(childDirNameArr.get(2), DateTimeFormatter.ofPattern(CameraFragment.DIR_NAME_DATE_FORMAT_PATTERN, Locale.ENGLISH)));
                } catch (Exception e) {
                    Timber.e(e);
                }
                try {
                    myApplication.getMyRoomDatabase().procedureFolderTbDataDao().insert(data);
                } catch (Exception e) {
                    Timber.e(e);
                }
                list.add(data);
            }
        }

        return list;
    }

    private final TextWatcher ageTextChangedListener = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {

        }

        @Override
        public void afterTextChanged(Editable s) {
//            try {
//                scheduledExecutorService.execute(new Runnable() {
//                    @Override
//                    public void run() {
//                        FragmentDataManagmentBinding binding = DataManagementFragment.this.binding;
//                        String age = null;
//                        try {
//                            age = binding.age.getText() != null ? binding.age.getText().toString().trim() : null;
//                        } catch (Exception e) {
//                            Timber.e(e);
//                        }
//                        List<ProcedureFolderTbData> procedureFolderTbDataList = getHighlightProcedureFolderTbDataList();
//                        for (ProcedureFolderTbData procedureFolderTbData : procedureFolderTbDataList) {
//                            procedureFolderTbData.setAge(age);
//                        }
//                        try {
//                            MyApplication myApplication = getMyApplication();
//                            if (myApplication != null) {
//                                myApplication.getMyRoomDatabase().procedureFolderTbDataDao().updates(procedureFolderTbDataList);
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

    private final RadioGroup.OnCheckedChangeListener genderLayoutOnCheckedChangeListener = new RadioGroup.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(RadioGroup group, int checkedId) {
//            switch (checkedId) {
//                case R.id.male:
//                    try {
//                        scheduledExecutorService.execute(new Runnable() {
//                            @Override
//                            public void run() {
//                                final List<ProcedureFolderTbData> procedureFolderTbDataList = getHighlightProcedureFolderTbDataList();
//                                for (ProcedureFolderTbData procedureFolderTbData : procedureFolderTbDataList) {
//                                    procedureFolderTbData.setGenderType(GenderType.MALE);
//                                }
//                                try {
//                                    MyApplication myApplication = getMyApplication();
//                                    if (myApplication != null) {
//                                        myApplication.getMyRoomDatabase().procedureFolderTbDataDao().updates(procedureFolderTbDataList);
//                                    }
//                                } catch (Exception e) {
//                                    Timber.e(e);
//                                }
//                            }
//                        });
//                    } catch (Exception e) {
//                        Timber.e(e);
//                    }
//                    break;
//
//                case R.id.female:
//                    try {
//                        scheduledExecutorService.execute(new Runnable() {
//                            @Override
//                            public void run() {
//                                List<ProcedureFolderTbData> procedureFolderTbDataList = getHighlightProcedureFolderTbDataList();
//                                for (ProcedureFolderTbData procedureFolderTbData : procedureFolderTbDataList) {
//                                    procedureFolderTbData.setGenderType(GenderType.FEMALE);
//                                }
//                                try {
//                                    MyApplication myApplication = getMyApplication();
//                                    if (myApplication != null) {
//                                        myApplication.getMyRoomDatabase().procedureFolderTbDataDao().updates(procedureFolderTbDataList);
//                                    }
//                                } catch (Exception e) {
//                                    Timber.e(e);
//                                }
//                            }
//                        });
//                    } catch (Exception e) {
//                        Timber.e(e);
//                    }
//                    break;
//            }
        }
    };


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
        d.show(getChildFragmentManager(), RemarkEditDialog.class.getSimpleName());
        zipPasswordDialog = d;
    }


    @UiThread
    private void cancelZipPasswordDialog() {
        final ZipPasswordDialog zipPasswordDialog = DataManagementFragment.this.zipPasswordDialog;
        if (zipPasswordDialog != null && zipPasswordDialog.isVisible()) {
            if (zipPasswordDialog.isStateSaved()) {
                zipPasswordDialog.dismissAllowingStateLoss();
            } else {
                zipPasswordDialog.dismiss();
            }
        }
        DataManagementFragment.this.zipPasswordDialog = null;
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

        final MyApplication myApplication = getMyApplication();
        if(myApplication!=null) {
            Log.i(TAG, String.format("User(%s) export %d files to %s USB stick",
                    myApplication.getLoginUserTbData().getAccount(),
                    fileCount, exportText));
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
                    final Disposable basicExportDisposable = DataManagementFragment.this.basicExportDisposable;
                    if (basicExportDisposable != null) {
                        basicExportDisposable.dispose();
                        DataManagementFragment.this.basicExportDisposable = null;
                    }
                } catch (Exception e) {
                    Timber.e(e);
                }

                try {
                    final Disposable dicomExportDisposable = DataManagementFragment.this.dicomExportDisposable;
                    if (dicomExportDisposable != null) {
                        dicomExportDisposable.dispose();
                        DataManagementFragment.this.dicomExportDisposable = null;
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
                                            Timber.w("delete file fail: "+jsonFile.getAbsolutePath());
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
                    final Disposable basicExportDisposable = DataManagementFragment.this.basicExportDisposable;
                    if (basicExportDisposable != null) {
                        basicExportDisposable.dispose();
                        DataManagementFragment.this.basicExportDisposable = null;
                    }
                } catch (Exception e) {
                    Timber.e(e);
                }

                try {
                    final Disposable dicomExportDisposable = DataManagementFragment.this.dicomExportDisposable;
                    if (dicomExportDisposable != null) {
                        dicomExportDisposable.dispose();
                        DataManagementFragment.this.dicomExportDisposable = null;
                    }
                } catch (Exception e) {
                    Timber.e(e);
                }

                try {
                    MyApplication myApplication = getMyApplication();
                    if (myApplication != null) {
                        File zipTempFile = new File(myApplication.getMainDirPath() + File.separator + MyApplication.ZIP_TEMP_NAME);
                        if (zipTempFile.exists()) {
                            FileUtils.deleteDirectory(zipTempFile);
                        }
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
        final ZipFilesProgressDialog zipFilesProgressDialog = DataManagementFragment.this.zipFilesProgressDialog;
        if (zipFilesProgressDialog != null && zipFilesProgressDialog.isShowing()) {
            zipFilesProgressDialog.dismiss();
        }
        DataManagementFragment.this.zipFilesProgressDialog = null;
    }


    private void initPreviewImgFull() {
        FragmentDataManagmentBinding binding = DataManagementFragment.this.binding;
        if (binding == null) {
            return;
        }
        Settings imgBgPhotoSettings = binding.previewImgFull.getController().getSettings();
        imgBgPhotoSettings.setMinZoom(1f);
        imgBgPhotoSettings.setMaxZoom(50f);
        imgBgPhotoSettings.setDoubleTapEnabled(false);
        imgBgPhotoSettings.setOverscrollDistance(0f, 0f);
        imgBgPhotoSettings.setRotationEnabled(false);
        imgBgPhotoSettings.setOverzoomFactor(1f);
        imgBgPhotoSettings.setFillViewport(true);
        imgBgPhotoSettings.setFitMethod(Settings.Fit.INSIDE);
        imgBgPhotoSettings.setFlingEnabled(false);
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

    @Nullable
    private File getDicomTempFolder(){
        final MyApplication myApplication = getMyApplication();
        if(myApplication!=null) {
            return new File(myApplication.getMainDirPath() + File.separator + MyApplication.DICOM_TEMP_NAME);
        }else{
            return null;
        }
    }

    private final int MSG_DICOMS2ZIP = 1;
    private final String KEY_PASSWORD="password";
    private final String KEY_STORAGE="storage";
    private Handler mHandler = new Handler(Looper.myLooper()){
        @Override
        public void handleMessage(@NonNull Message msg) {
            switch(msg.what){
                case MSG_DICOMS2ZIP:
                    String password = msg.getData().getString(KEY_PASSWORD);
                    StorageVolume storage= msg.getData().getParcelable(KEY_STORAGE);

                    if(storage==null){
                        Timber.e("[mHandler] MSG_DICOMS2ZIP: storage==null");
                        return;
                    }

                    //dicom壓成zip檔
                    File dicomTempFile=getDicomTempFolder();
                    if(dicomTempFile==null || dicomTempFile.exists() ==false){
                        return;
                    }

                    final File[] dicomFolder = dicomTempFile.listFiles();
                    if(dicomFolder==null){
                        return;
                    }

                    final File exportDirectory = storage.getDirectory();
                    if (exportDirectory == null) {
                        return;
                    }

                    HashMap<@NotNull File, @NotNull List<@NotNull File>> selectZipFileGroupMap = new HashMap<>();

                    for (File f:dicomFolder) {
                        if(f.isDirectory()){
                            final File[] files = f.listFiles();
                            final List<File> fileList = Arrays.asList(files);
                            final File zipfile = new File(exportDirectory.getAbsolutePath() + File.separator +
                                    MyApplication.FOLDER_NAME + File.separator + "DICOM_" + f.getName() + ".zip");

                            selectZipFileGroupMap.put(zipfile, fileList);
                        }
                    }

                    final Context context = getContext();
                    if (context == null) {
                        return;
                    }
                    basicExportZip(selectZipFileGroupMap, Tools.getDisplayName(storage, context), password.trim(), storage);


                    break;
            }
        }
    };

    public void clearSearchCriteria() {
        final Runnable r = new Runnable() {
            @Override
            public void run() {
                final FragmentDataManagmentBinding binding = DataManagementFragment.this.binding;
                if (binding == null) {
                    Timber.w("[clearSearchCriteria] binding==null)");
                    return;
                }
                binding.patientId.setText("");
                setSearchMenuType(DataManagementSearchMenuType.ALL);
            }
        };

        if (Looper.getMainLooper() == Looper.myLooper()) {
            r.run();
        } else {
            final FragmentDataManagmentBinding binding = DataManagementFragment.this.binding;
            if (binding != null) {
                binding.getRoot().post(r);
            }
        }
    }
}
