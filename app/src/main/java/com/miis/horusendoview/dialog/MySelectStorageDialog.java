package com.miis.horusendoview.dialog;

import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Looper;
import android.os.storage.StorageVolume;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.miis.horusendoview.R;
import com.miis.horusendoview.adapter.MySelectStorageDialogAdapter;
import com.miis.horusendoview.data.MySelectStorageDialogData;
import com.miis.horusendoview.databinding.DialogMySelectStorageBinding;
import com.miis.horusendoview.manager.MyStorageManager;
import com.miis.horusendoview.tools.Tools;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import timber.log.Timber;

public class MySelectStorageDialog extends AlertDialog {

    @NonNull
    private final DialogMySelectStorageBinding binding = DialogMySelectStorageBinding.inflate(LayoutInflater.from(getContext()), null, false);

    @Nullable
    private Listener listener;

    public MySelectStorageDialog(Context context) {
        super(context);
        init();
    }

    public MySelectStorageDialog(@NonNull Context context, int themeResId) {
        super(context, themeResId);
        init();
    }

    public MySelectStorageDialog(@NonNull Context context, boolean cancelable, @Nullable OnCancelListener cancelListener) {
        super(context, cancelable, cancelListener);
        init();
    }

    @NonNull
    private void init() {
        setView(binding.getRoot());
        if (!(binding.recyclerView.getLayoutManager() instanceof LinearLayoutManager)) {
            binding.recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        }
        final RecyclerView.ItemAnimator itemAnimator = binding.recyclerView.getItemAnimator();
        if (itemAnimator instanceof DefaultItemAnimator) {
            ((DefaultItemAnimator) itemAnimator).setSupportsChangeAnimations(false);
        }
        if (!(binding.recyclerView.getAdapter() instanceof MySelectStorageDialogAdapter)) {
            binding.recyclerView.setAdapter(new MySelectStorageDialogAdapter(this));
        }
    }

    public interface Listener {
        void onClickConfirm(int position, @NonNull MySelectStorageDialogData data);

        void onClickCancel();

        void onTouch();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding.confirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MySelectStorageDialogAdapter adapter = null;
                try {
                    adapter = (MySelectStorageDialogAdapter) binding.recyclerView.getAdapter();
                } catch (Exception e) {
                    Timber.e(e);
                }
                if (adapter != null) {
                    final List<MySelectStorageDialogData> dataList = adapter.getCurrentList();
                    for (int i = 0; i < dataList.size(); i++) {
                        final MySelectStorageDialogData data = dataList.get(i);
                        if (data.isSelect()) {
                            final Listener listener = MySelectStorageDialog.this.listener;
                            if (listener != null) {
                                listener.onClickConfirm(i, data);
                            }
                            dismiss();
                            break;
                        }
                    }
                }
            }
        });

        binding.cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final Listener listener = MySelectStorageDialog.this.listener;
                if (listener != null) {
                    listener.onClickCancel();
                }
                dismiss();
            }
        });

        setDataList();

        MyStorageManager.getInstance().addListener(myStorageManagerListener);
    }

    @Override
    protected void onStop() {
        super.onStop();
        MyStorageManager.getInstance().removeListener(myStorageManagerListener);
    }

    @Override
    public void show() {
        super.show();
        final Window window = getWindow();
        if (window != null) {
            window.setDimAmount(0.5f);
            window.setBackgroundDrawable(new ColorDrawable(ContextCompat.getColor(getContext(), R.color.gray_01000000)));
        }
        setSystemUIVisibility();
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        final Listener listener = MySelectStorageDialog.this.listener;
        if (listener != null) {
            listener.onTouch();
        }
        return super.dispatchTouchEvent(ev);
    }

    private void setSystemUIVisibility() {
        if (getWindow() != null) {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LOW_PROFILE
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }
    }

    private void setDataList() {
        final Runnable r = new Runnable() {
            @Override
            public void run() {
                MySelectStorageDialogAdapter adapter = null;
                try {
                    adapter = (MySelectStorageDialogAdapter) binding.recyclerView.getAdapter();
                } catch (Exception e) {
                    Timber.e(e);
                }
                if (adapter != null) {
                    final ArrayList<MySelectStorageDialogData> dataList = new ArrayList<>(adapter.getCurrentList());
                    final List<StorageVolume> storageList = MyStorageManager.getInstance().getStorageList();
                    final ArrayList<MySelectStorageDialogData> removeList = new ArrayList<>();
                    for (MySelectStorageDialogData data : dataList) {
                        StorageVolume s = null;
                        for (StorageVolume storage : storageList) {
                            try {
                                if (storage.getDirectory() != null && TextUtils.equals(storage.getDirectory().getAbsolutePath(), data.getStorageVolume().getDirectory().getAbsolutePath())) {
                                    s = storage;
                                    break;
                                }
                            } catch (Exception e) {
                                Timber.e(e);
                            }
                        }
                        if (s == null) {
                            removeList.add(data);
                        }
                    }

                    dataList.removeAll(removeList);

                    for (StorageVolume storage : storageList) {
                        final File storageDirectory = storage.getDirectory();
                        if (storageDirectory == null) {
                            continue;
                        }
                        boolean isAdd = false;
                        for (MySelectStorageDialogData data : dataList) {
                            if (data.getStorageVolume().getDirectory().getAbsolutePath().equals(storageDirectory.getAbsolutePath())) {
                                isAdd = true;
                                break;
                            }
                        }
                        if (!isAdd) {
                            dataList.add(new MySelectStorageDialogData(false, Tools.getDisplayName(storage, getContext()), storage));
                        }
                    }
                    adapter.submitList(dataList, new Runnable() {
                        @Override
                        public void run() {
                            checkConfirmBtn();
                        }
                    });
                }
            }
        };
        if (Looper.myLooper() == Looper.getMainLooper()) {
            r.run();
        } else {
            binding.getRoot().post(r);
        }
    }

    @UiThread
    public void checkConfirmBtn() {
        MySelectStorageDialogAdapter adapter = null;
        try {
            adapter = (MySelectStorageDialogAdapter) binding.recyclerView.getAdapter();
        } catch (Exception e) {
            Timber.e(e);
        }
        if (adapter != null) {
            if (adapter.getSelectedItem() != null) {
                binding.confirm.setVisibility(View.VISIBLE);
            } else {
                binding.confirm.setVisibility(View.INVISIBLE);
            }
        }
    }

    private final MyStorageManager.Listener myStorageManagerListener = new MyStorageManager.Listener() {
        @Override
        public void onStorageMounted(@NonNull File storageFile) {
            setDataList();
        }

        @Override
        public void onStorageUnmounted(@NonNull File storageFile) {
            setDataList();
        }
    };

    @Nullable
    public Listener getListener() {
        return listener;
    }

    public void setListener(@Nullable Listener listener) {
        this.listener = listener;
    }
}
