package com.miis.horusendoview.dialog;

import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
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

import com.miis.horusendoview.MyApplication;
import com.miis.horusendoview.R;
import com.miis.horusendoview.activity.MainActivity;
import com.miis.horusendoview.adapter.RoomBackupFileSelectDialogAdapter;
import com.miis.horusendoview.data.RoomBackupFileSelectDialogAdapterData;
import com.miis.horusendoview.databinding.DialogRoomBackupFileSelectBinding;
import com.miis.horusendoview.roomDataBase.MyRoomDatabase;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import timber.log.Timber;

public class RoomBackupFileSelectDialog extends AlertDialog {

    @NonNull
    private final DialogRoomBackupFileSelectBinding binding = DialogRoomBackupFileSelectBinding.inflate(LayoutInflater.from(getContext()), null, false);

    @Nullable
    private Listener listener;

    protected RoomBackupFileSelectDialog(@NonNull Context context) {
        super(context);
        init();
    }

    protected RoomBackupFileSelectDialog(@NonNull Context context, int themeResId) {
        super(context, themeResId);
        init();
    }

    protected RoomBackupFileSelectDialog(@NonNull Context context, boolean cancelable, @Nullable OnCancelListener cancelListener) {
        super(context, cancelable, cancelListener);
        init();
    }


    private void init() {
        setView(binding.getRoot());
        if (!(binding.recyclerView.getLayoutManager() instanceof LinearLayoutManager)) {
            binding.recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        }
        final RecyclerView.ItemAnimator itemAnimator = binding.recyclerView.getItemAnimator();
        if (itemAnimator instanceof DefaultItemAnimator) {
            ((DefaultItemAnimator) itemAnimator).setSupportsChangeAnimations(false);
        }
        if (!(binding.recyclerView.getAdapter() instanceof RoomBackupFileSelectDialogAdapter)) {
            binding.recyclerView.setAdapter(new RoomBackupFileSelectDialogAdapter(this));
        }
    }

    public interface Listener {
        void onClickConfirm(int position, @NonNull RoomBackupFileSelectDialogAdapterData data);

        void onClickCancel();

        void onTouch();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding.confirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                RoomBackupFileSelectDialogAdapter adapter = null;
                try {
                    adapter = (RoomBackupFileSelectDialogAdapter) binding.recyclerView.getAdapter();
                } catch (Exception e) {
                    Timber.e(e);
                }
                if (adapter != null) {
                    final List<RoomBackupFileSelectDialogAdapterData> dataList = adapter.getCurrentList();
                    for (int i = 0; i < dataList.size(); i++) {
                        final RoomBackupFileSelectDialogAdapterData data = dataList.get(i);
                        if (data.isSelect()) {
                            final RoomBackupFileSelectDialog.Listener listener = RoomBackupFileSelectDialog.this.listener;
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
                final RoomBackupFileSelectDialog.Listener listener = RoomBackupFileSelectDialog.this.listener;
                if (listener != null) {
                    listener.onClickCancel();
                }
                dismiss();
            }
        });

        setDataList();
    }

    @Override
    protected void onStop() {
        super.onStop();
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
        final RoomBackupFileSelectDialog.Listener listener = RoomBackupFileSelectDialog.this.listener;
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

        final MyApplication myApplication = (MyApplication) getContext().getApplicationContext();


        MainActivity mainActivity = myApplication.getMainActivity();
        if (mainActivity == null) {
            return;
        }

        try {
            mainActivity.getScheduledExecutorService().execute(new Runnable() {
                @Override
                public void run() {
                    RoomBackupFileSelectDialogAdapter adapter = null;
                    try {
                        adapter = (RoomBackupFileSelectDialogAdapter) binding.recyclerView.getAdapter();
                    } catch (Exception e) {
                        Timber.e(e);
                    }
                    if (adapter != null) {
                        final ArrayList<RoomBackupFileSelectDialogAdapterData> dataList = new ArrayList<>();

                        final List<File> backupFileList = MyRoomDatabase.getRoomBackupFileList(myApplication);


                        for (File backupFile : backupFileList) {
                            dataList.add(new RoomBackupFileSelectDialogAdapterData(false, backupFile));
                        }

                        final RoomBackupFileSelectDialogAdapter finalAdapter = adapter;
                        binding.getRoot().post(new Runnable() {
                            @Override
                            public void run() {
                                finalAdapter.submitList(dataList, new Runnable() {
                                    @Override
                                    public void run() {
                                        checkConfirmBtn();
                                    }
                                });
                            }
                        });
                    }
                }
            });
        } catch (Exception e) {
            Timber.e(e);
        }
    }

    @UiThread
    public void checkConfirmBtn() {
        RoomBackupFileSelectDialogAdapter adapter = null;
        try {
            adapter = (RoomBackupFileSelectDialogAdapter) binding.recyclerView.getAdapter();
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

    @Nullable
    public Listener getListener() {
        return listener;
    }

    public void setListener(@Nullable Listener listener) {
        this.listener = listener;
    }
}
