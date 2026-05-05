package com.miis.horusendoview.dialog;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.exifinterface.media.ExifInterface;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.divider.MaterialDividerItemDecoration;
import com.miis.horusendoview.MyApplication;
import com.miis.horusendoview.R;
import com.miis.horusendoview.adapter.historyImageListDialog.HistoryImageListDialog01Adapter;
import com.miis.horusendoview.data.HistoryImageListDialog02AdapterData;
import com.miis.horusendoview.data.HistoryImageListDialogAdapterData;
import com.miis.horusendoview.databinding.DialogHistoryImageListBinding;
import com.miis.horusendoview.fragment.CameraFragment;
import com.miis.horusendoview.roomDataBase.userTbData.UserTbData;
import com.miis.horusendoview.tools.UiTool;
import com.miis.horusendoview.type.UserRoleType;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import timber.log.Timber;

public class HistoryImageListDialog extends DialogFragment {

    @NonNull
    private final ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(3);

    @Nullable
    private DialogHistoryImageListBinding binding = null;

    @Nullable
    private Listener listener = null;

    @Nullable
    private HistoryImageListDialog02AdapterData selectedData = null;

    @NonNull
    public static HistoryImageListDialog newInstance() {
        return new HistoryImageListDialog();
    }

    public interface Listener {

        void onTouch();

        void onSelectedFile(@NonNull File file);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (scheduledExecutorService instanceof ScheduledThreadPoolExecutor) {
            ScheduledThreadPoolExecutor s = (ScheduledThreadPoolExecutor) scheduledExecutorService;
            s.setRemoveOnCancelPolicy(true);
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (binding == null) {
            binding = DialogHistoryImageListBinding.inflate(inflater, container, false);
        }
        return binding.getRoot();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return new Dialog(requireContext(), getTheme()) {
            @Override
            public boolean dispatchTouchEvent(MotionEvent ev) {
                Timber.d("onCreateDialog -> dispatchTouchEvent");
                final Listener listener = HistoryImageListDialog.this.listener;
                if (listener != null) {
                    listener.onTouch();
                }
                return super.dispatchTouchEvent(ev);
            }
        };
    }


    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setSystemUIVisibility();

        DialogHistoryImageListBinding binding = HistoryImageListDialog.this.binding;
        if (binding != null) {
            binding.close.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (isStateSaved()) {
                        dismissAllowingStateLoss();
                    } else {
                        dismiss();
                    }
                }
            });

            binding.cancel.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    binding.close.performClick();
                }
            });

            binding.ok.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    final HistoryImageListDialog02AdapterData selectedData = getSelectedData();
                    if (selectedData != null) {
                        final File selectedFile = selectedData.getFile();
                        final Listener listener = HistoryImageListDialog.this.listener;
                        if (listener != null) {
                            listener.onSelectedFile(selectedFile);
                        }
                        binding.close.performClick();
                    }
                }
            });

            initRecyclerView();
            setDataToRecyclerView();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        setSystemUIVisibility();

        Window window = null;
        if (getDialog() != null) {
            window = getDialog().getWindow();
        }
        Context context = getContext();
        if (window != null && context != null) {
            DisplayMetrics displayMetrics = UiTool.getDisplaySize(context);
            int width = displayMetrics.widthPixels - (context.getResources().getDimensionPixelSize(R.dimen.margin_32) * 2);
            int height = displayMetrics.heightPixels - (context.getResources().getDimensionPixelSize(R.dimen.margin_32) * 2);
            window.setLayout(width, height);

            window.setDimAmount(0.8f); // 对话框外部阴影比重(0f ~ 1f)
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
    }


    @Override
    public void onResume() {
        super.onResume();
        setSystemUIVisibility();
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

    private void setSystemUIVisibility() {
        Dialog dialog = getDialog();
        if (dialog != null) {
            Window window = dialog.getWindow();
            if (window != null) {
                View decorView = window.getDecorView();
                if (decorView != null) {
                    decorView.setSystemUiVisibility(
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
        }
    }

    private void initRecyclerView() {
        final Context context = getContext();
        if (context == null) {
            return;
        }

        final DialogHistoryImageListBinding binding = HistoryImageListDialog.this.binding;
        if (binding == null) {
            return;
        }

        DefaultItemAnimator itemAnimator = null;
        try {
            itemAnimator = (DefaultItemAnimator) binding.recyclerView.getItemAnimator();
        } catch (Exception e) {
            Timber.e(e);
        }
        if (itemAnimator != null) {
            itemAnimator.setSupportsChangeAnimations(false);
        }

        RecyclerView.LayoutManager layoutManager = binding.recyclerView.getLayoutManager();
        if (!(layoutManager instanceof LinearLayoutManager)) {
            binding.recyclerView.setLayoutManager(new LinearLayoutManager(context));
        }

        if (binding.recyclerView.getItemDecorationCount() == 0) {
            MaterialDividerItemDecoration materialDividerItemDecoration = new MaterialDividerItemDecoration(
                    context, LinearLayoutManager.VERTICAL);
            materialDividerItemDecoration.setDividerColor(Color.TRANSPARENT);
            materialDividerItemDecoration.setLastItemDecorated(true);
            materialDividerItemDecoration.setDividerThickness(
                    context.getResources().getDimensionPixelSize(R.dimen.margin_32));
            binding.recyclerView.addItemDecoration(materialDividerItemDecoration);
        }

        final RecyclerView.Adapter<?> adapter = binding.recyclerView.getAdapter();
        if (!(adapter instanceof HistoryImageListDialog01Adapter)) {
            binding.recyclerView.setAdapter(new HistoryImageListDialog01Adapter(this));
        }
    }

    @SuppressLint("RestrictedApi")
    private void setDataToRecyclerView() {
        binding.progressBar.setVisibility(View.VISIBLE); // 顯示載入中

        try {
            scheduledExecutorService.execute(new Runnable() {
                @Override
                public void run() {
                    final MyApplication myApplication = getMyApplication();
                    if (myApplication == null) {
                        return;
                    }
                    final UserTbData loginUserTbData = myApplication.getLoginUserTbData();
                    if (loginUserTbData == null) {
                        return;
                    }
                    UserRoleType roleType = loginUserTbData.getRoleType();
                    if (roleType == null) {
                        roleType = UserRoleType.GUEST;
                    }
                    @Nullable final String patientId = myApplication.getPatientId();
                    @Nullable final HistoryImageListDialog02AdapterData historyImageListDialog02AdapterData = getSelectedData();
                    @NonNull final String imgPath = myApplication.getMainDirPath();
                    File mainDir = new File(imgPath);
                    File[] childDirArr;
                    if (mainDir.exists() && mainDir.isDirectory()) {
                        try {
                            final UserRoleType finalRoleType = roleType;
                            childDirArr = mainDir.listFiles((dir, name) -> {
                                switch (finalRoleType) {
                                    case GUEST:
                                        return false;
                                    case ADVANCED_USER: {
                                        String[] nameArr = name.split("_");
                                        if (nameArr.length >= 3) {
                                            if (TextUtils.isEmpty(patientId)) {
                                                return nameArr[0].equalsIgnoreCase(loginUserTbData.getAccount());
                                            } else {
                                                return nameArr[0].equalsIgnoreCase(loginUserTbData.getAccount()) && nameArr[1].equalsIgnoreCase(patientId);
                                            }
                                        } else {
                                            return false;
                                        }
                                    }
                                    case ADMIN_USER:
                                    case SERVICE_USER: {
                                        String[] nameArr = name.split("_");
                                        if (nameArr.length >= 3) {
                                            if (TextUtils.isEmpty(patientId)) {
                                                return true;
                                            } else {
                                                return nameArr[1].equalsIgnoreCase(patientId);
                                            }
                                        } else {
                                            return false;
                                        }
                                    }
                                    default:
                                        return false;
                                }
                            });
                        } catch (Exception e) {
                            Timber.e(e);
                            childDirArr = null;
                        }
                    } else {
                        childDirArr = null;
                    }

                    File[] imgFileList = new File[]{};
                    if (childDirArr != null) {
                        final ArrayList<File> fileList = new ArrayList<>();
                        for (File childDir : childDirArr) {
                            if (childDir.isDirectory() && childDir.exists()) {
                                File[] imgFileArr = new File[]{};
                                try {
                                    imgFileArr = childDir.listFiles((dir, name) -> name.contains(MyApplication.IMG_FILE_EXTENSION));
                                } catch (Exception e) {
                                    Timber.e(e);
                                }
                                if (imgFileArr != null) {
                                    fileList.addAll(Arrays.asList(imgFileArr));
                                }
                            }
                        }
                        imgFileList = fileList.toArray(new File[]{});
                    }

                    Arrays.sort(imgFileList, (file1, file2) -> {
                        try {
                            ExifInterface exif1 = new ExifInterface(file1.getAbsolutePath());
                            LocalDateTime dateTime1 = Instant.ofEpochMilli(exif1.getAttributeInt(ExifInterface.TAG_DATETIME, (int) System.currentTimeMillis()))
                                    .atZone(ZoneId.systemDefault()).toLocalDateTime();

                            ExifInterface exif2 = new ExifInterface(file2.getAbsolutePath());
                            LocalDateTime dateTime2 = Instant.ofEpochMilli(exif2.getAttributeInt(ExifInterface.TAG_DATETIME, (int) System.currentTimeMillis()))
                                    .atZone(ZoneId.systemDefault()).toLocalDateTime();

                            return dateTime1.compareTo(dateTime2);
                        } catch (Exception e) {
                            Timber.e(e);
                            try {
                                BasicFileAttributes attr1 = Files.readAttributes(file1.toPath(), BasicFileAttributes.class);
                                BasicFileAttributes attr2 = Files.readAttributes(file2.toPath(), BasicFileAttributes.class);
                                LocalDateTime dateTime1 = attr1.creationTime().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
                                LocalDateTime dateTime2 = attr2.creationTime().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
                                return dateTime1.compareTo(dateTime2);
                            } catch (Exception ex) {
                                Timber.e(ex);
                                return 0;
                            }
                        }
                    });

                    @NonNull final HashMap<@NotNull LocalDate, @NotNull List<@NotNull File>> groupMap = new HashMap<>();
                    for (File file : imgFileList) {
                        try {
                            LocalDate localDate=null;
                            final String name = file.getParentFile().getName();
                            final int indexOfUnderline = name.lastIndexOf("_");
                            if(indexOfUnderline>0) {
                                final String folderDate = name.substring(indexOfUnderline+1);
                                DateTimeFormatter formatter = DateTimeFormatter.ofPattern(CameraFragment.DIR_NAME_DATE_FORMAT_PATTERN);
                                localDate=LocalDate.parse(folderDate, formatter);
                            }
                            if(localDate==null){
                                BasicFileAttributes attr = Files.readAttributes(file.getParentFile().toPath(), BasicFileAttributes.class);
                                LocalDateTime dateTime = attr.creationTime().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
                                localDate = dateTime.toLocalDate();
                            }

                            @NotNull ArrayList<@NotNull File> list = new ArrayList<>();
                            if (groupMap.containsKey(localDate)) {
                                List<File> l = groupMap.get(localDate);
                                if (l != null) {
                                    list.addAll(l);
                                }
                            }
                            list.add(file);
                            groupMap.put(localDate, list);
                        } catch (Exception ex) {
                            Timber.e(ex);
                        }
                    }

                    @NonNull final List<Map.Entry<LocalDate, List<File>>> mapList = new ArrayList<>(groupMap.entrySet());
                    mapList.sort((entry1, entry2) -> entry2.getKey().compareTo(entry1.getKey()));

                    final ArrayList<HistoryImageListDialogAdapterData> list = new ArrayList<>();
                    for (Map.Entry<LocalDate, List<File>> entry : mapList) {
                        final ArrayList<HistoryImageListDialog02AdapterData> adapterDataList = new ArrayList<>();
                        List<File> fLis = entry.getValue();
                        if (fLis == null) {
                            fLis = new ArrayList<>();
                        }
                        fLis.sort(new Comparator<File>() {
                            @Override
                            public int compare(@NonNull File file1, @NonNull File file2) {
                                try {
                                    BasicFileAttributes attr1 = Files.readAttributes(file1.toPath(), BasicFileAttributes.class);
                                    BasicFileAttributes attr2 = Files.readAttributes(file2.toPath(), BasicFileAttributes.class);
                                    LocalDateTime dateTime1 = attr1.creationTime().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
                                    LocalDateTime dateTime2 = attr2.creationTime().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
                                    return dateTime2.compareTo(dateTime1);
                                } catch (Exception ex) {
                                    Timber.e(ex);
                                    return 0;
                                }
                            }
                        }.reversed());

                        for (File file : fLis) {
                            boolean isSelect = historyImageListDialog02AdapterData != null && historyImageListDialog02AdapterData.getFile().getName().equals(file.getName());
                            final HistoryImageListDialog02AdapterData d2 = new HistoryImageListDialog02AdapterData(isSelect, file);
                            adapterDataList.add(d2);
                            if (isSelect) {
                                setSelectedData(d2);
                            }
                        }
                        list.add(new HistoryImageListDialogAdapterData(entry.getKey(), adapterDataList));
                    }

                    final DialogHistoryImageListBinding binding = HistoryImageListDialog.this.binding;
                    if (binding != null) {
                        binding.getRoot().post(new Runnable() {
                            @Override
                            public void run() {
                                RecyclerView.Adapter<?> adapter = binding.recyclerView.getAdapter();
                                if (adapter instanceof HistoryImageListDialog01Adapter) {
                                    ((HistoryImageListDialog01Adapter) adapter).submitList(list);
                                }
                                binding.progressBar.setVisibility(View.GONE); // 隱藏載入畫面
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
    public DialogHistoryImageListBinding getBinding() {
        return binding;
    }

    @Nullable
    public Listener getListener() {
        return listener;
    }

    public void setListener(@Nullable Listener listener) {
        this.listener = listener;
    }

    @Nullable
    public HistoryImageListDialog02AdapterData getSelectedData() {
        return selectedData;
    }

    public void setSelectedData(@Nullable HistoryImageListDialog02AdapterData selectedData) {
        this.selectedData = selectedData;
        try {
            scheduledExecutorService.execute(new Runnable() {
                @Override
                public void run() {
                    final DialogHistoryImageListBinding binding = HistoryImageListDialog.this.binding;
                    if (binding != null) {
                        binding.ok.setEnabled(HistoryImageListDialog.this.selectedData != null);
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
}
