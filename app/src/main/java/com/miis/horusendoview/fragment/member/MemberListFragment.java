package com.miis.horusendoview.fragment.member;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.divider.MaterialDividerItemDecoration;
import com.miis.horusendoview.MyApplication;
import com.miis.horusendoview.R;
import com.miis.horusendoview.adapter.MemberListAdapter;
import com.miis.horusendoview.data.MemberAdapterData;
import com.miis.horusendoview.databinding.FragmentMemberListBinding;
import com.miis.horusendoview.dialog.MyDialog;
import com.miis.horusendoview.fragment.base.ChildBaseFragment;
import com.miis.horusendoview.log.LogQueue;
import com.miis.horusendoview.manager.ChangeFragmentManager;
import com.miis.horusendoview.roomDataBase.imageAdjustmentSetting.ImageAdjustmentSettingTbData;
import com.miis.horusendoview.roomDataBase.procedureFolder.ProcedureFolderTbData;
import com.miis.horusendoview.roomDataBase.userTbData.UserTbData;
import com.miis.horusendoview.type.UserRoleType;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import timber.log.Timber;

public class MemberListFragment extends ChildBaseFragment {

    private static final String TAG = MemberListFragment.class.getSimpleName();

    @Override
    public boolean isNotAddToBackStack() {
        return true;
    }

    @Nullable
    private FragmentMemberListBinding binding;

    @Nullable
    private MyDialog deleteUserDialog;

    @NonNull
    private final ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(3);

    public static MemberListFragment newInstance() {
        return new MemberListFragment();
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
            binding = FragmentMemberListBinding.inflate(inflater, container, false);
        }
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        final FragmentMemberListBinding binding = MemberListFragment.this.binding;
        if (binding == null) {
            return;
        }
        binding.addUser.setOnClickListener(this);
        binding.selectedDelete.setOnClickListener(this);
        binding.logout.setOnClickListener(this);

        setLoginUserDataToView();

        initRecyclerView();

        setDataToRecyclerView();
    }


    @Override
    public void onResume() {
        super.onResume();
        setLoginUserDataToView();
        setDataToRecyclerView();
    }

    @Override
    public void onPause() {
        super.onPause();
        cancelDeleteUserDialog();
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
        Timber.d("onHiddenChanged hidden=" + hidden);
        if (hidden) {
            cancelDeleteUserDialog();
        } else {
            setLoginUserDataToView();
            setDataToRecyclerView();
        }
    }

    @Override
    public void onClick(View v) {
        final ChangeFragmentManager changeChildFragmentManager = getChangeChildFragmentManager();
        final FragmentMemberListBinding binding = MemberListFragment.this.binding;
        Timber.d("[onClick] "+ LogQueue.getId(v));
        switch (v.getId()) {
            case R.id.addUser:
                if (changeChildFragmentManager != null) {
                    changeChildFragmentManager.changePage(MemberAddFragment.newInstance());
                }
                break;

            case R.id.selectedDelete:
                MemberListAdapter adapter = null;
                if (binding != null) {
                    try {
                        adapter = (MemberListAdapter) binding.recyclerView.getAdapter();
                    } catch (Exception e) {
                        Timber.e(e);
                    }
                }
                if (adapter != null) {
                    List<UserTbData> list = adapter.getSelectUserTbDataList();
                    if (!list.isEmpty()) {
                        showDeleteUserDialog(new MyDialog.Listener() {
                            @Override
                            public void OnClickConfirm() {
                                try {
                                    scheduledExecutorService.execute(new Runnable() {
                                        @Override
                                        public void run() {
                                            MyApplication myApplication = getMyApplication();
                                            for (UserTbData d : list) {
                                                if (myApplication != null) {
                                                    try {
                                                        deleteUser(d);
                                                    } catch (Exception e) {
                                                        Timber.e(e);
                                                    }
                                                }
                                            }
                                            setDataToRecyclerView();
                                        }
                                    });
                                } catch (Exception e) {
                                    Timber.e(e);
                                }

                                deleteUserDialog = null;
                            }

                            @Override
                            public void OnClickCancel() {
                                deleteUserDialog = null;
                            }
                        });
                    }
                }
                break;

            case R.id.logout: {
                MyApplication myApplication = getMyApplication();
                if (myApplication != null) {
                    myApplication.setLoginUserTbData(null);
                    Log.i(TAG, "User logout");
                }
            }
            break;
        }
    }

    private void initRecyclerView() {
        final Context context = getContext();
        if (context == null) {
            return;
        }
        final FragmentMemberListBinding binding = MemberListFragment.this.binding;
        if (binding == null) {
            return;
        }

        if (binding.recyclerView.getLayoutManager() == null || !(binding.recyclerView.getLayoutManager() instanceof LinearLayoutManager)) {
            LinearLayoutManager layoutManager = new LinearLayoutManager(context);
            binding.recyclerView.setLayoutManager(layoutManager);
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

        if (binding.recyclerView.getItemDecorationCount() == 0) {
            MaterialDividerItemDecoration dividerItemDecoration = new MaterialDividerItemDecoration(getContext(), LinearLayoutManager.VERTICAL);
            dividerItemDecoration.setDividerColor(ContextCompat.getColor(context, R.color.gray_d7d7d7));
            dividerItemDecoration.setLastItemDecorated(true);
            dividerItemDecoration.setDividerThickness(2);
            binding.recyclerView.addItemDecoration(dividerItemDecoration);
        }

        if (binding.recyclerView.getAdapter() == null || !(binding.recyclerView.getAdapter() instanceof MemberListAdapter)) {
            binding.recyclerView.setAdapter(new MemberListAdapter(this));
        }
    }

    public void setDataToRecyclerView() {
        Timber.d("setDataToRecyclerView 00");
        try {
            scheduledExecutorService.execute(new Runnable() {
                @Override
                public void run() {
                    final FragmentMemberListBinding binding = MemberListFragment.this.binding;
                    Timber.d("setDataToRecyclerView 01");
                    if (binding == null) {
                        return;
                    }
                    MemberListAdapter adapter = null;
                    try {
                        adapter = (MemberListAdapter) binding.recyclerView.getAdapter();
                    } catch (Exception e) {
                        Timber.e(e);
                    }

                    MyApplication myApplication = getMyApplication();
                    final ArrayList<MemberAdapterData> currentList = (adapter != null) ? new ArrayList<>(adapter.getCurrentList()) : new ArrayList<>();
                    List<UserTbData> dbList = new ArrayList<>();
                    try {
                        if (myApplication != null) {
                            dbList = myApplication.getMyRoomDatabase().userTbDataDao().getAll();
                            Timber.d("setDataToRecyclerView 02");
                        }
                    } catch (Exception e) {
                        Timber.e(e);
                    }

                    if (dbList == null) {
                        dbList = new ArrayList<>();
                    }

                    final ArrayList<MemberAdapterData> list = new ArrayList<>();
                    for (UserTbData d : dbList) {
                        //Timber.d("setDataToRecyclerView 03 UserTbData=" + d.toString());
                        boolean isSelect = false;
                        for (MemberAdapterData currentData : currentList) {
                            if (d.getAccount().equals(currentData.getUserTbData().getAccount())) {
                                isSelect = currentData.isSelect();
                                break;
                            }
                        }
                        list.add(new MemberAdapterData(d, isSelect));
                    }

                    final MemberListAdapter finalAdapter = adapter;
                    binding.getRoot().post(new Runnable() {
                        @Override
                        public void run() {
                            if (finalAdapter != null) {
                                finalAdapter.submitList(list);
                            }
                        }
                    });
                }
            });
        } catch (Exception e) {
            Timber.e(e);
        }
    }

    public void showDeleteUserDialog(MyDialog.Listener listener) {
        cancelDeleteUserDialog();
        final Context context = getContext();
        if (context == null) {
            return;
        }
        MyDialog dialog = new MyDialog(context, false);
        dialog.setTitle(getString(R.string.warning));
        dialog.setMsg(getString(R.string.are_you_sure_delete_it));
        dialog.setCancelText(getString(R.string.cancel));
        dialog.setConfirmText(getString(R.string.confirm));
        dialog.setListener(listener);
        dialog.setCancelable(false);
        dialog.show();
        deleteUserDialog = dialog;
    }

    public void cancelDeleteUserDialog() {
        final MyDialog deleteUserDialog = MemberListFragment.this.deleteUserDialog;
        if (deleteUserDialog != null) {
            deleteUserDialog.dismiss();
        }
    }

    @UiThread
    public void setLoginUserDataToView() {
        FragmentMemberListBinding binding = MemberListFragment.this.binding;
        if (binding != null) {
            binding.getRoot().post(new Runnable() {
                @Override
                public void run() {
                    MyApplication myApplication = getMyApplication();
                    UserTbData userTbData = null;
                    if (myApplication != null) {
                        userTbData = myApplication.getLoginUserTbData();
                    }

                    Timber.d("setLoginUserDataToView > userTbData=" + userTbData);
                    UserRoleType roleType = (userTbData != null) ? userTbData.getRoleType() : null;
                    if (roleType == null) {
                        roleType = UserRoleType.GUEST;
                    }
                    int imageResource = R.drawable.default_user_128x128;
                    switch (roleType) {
                        case GUEST:
                        case ADVANCED_USER:
                            imageResource = R.drawable.default_user_128x128;
                            break;
                        case ADMIN_USER:
                        case SERVICE_USER:
                            imageResource = R.drawable.admin_user_128x128;
                            break;
                    }

                    binding.loginImg.setImageResource(imageResource);
                    String account = (userTbData != null) ? userTbData.getAccount() : "";
                    binding.loginText.setText(account.isEmpty() ? "-" : account);
                }
            });
        }
    }

    public void deleteUser(UserTbData d) {
        try {
            MyApplication myApplication = getMyApplication();
            if (myApplication != null) {
                myApplication.getMyRoomDatabase().userTbDataDao().delete(d);

                final UserTbData loginUserTbData = myApplication.getLoginUserTbData();
                if(loginUserTbData!=null & d!=null) {
                    Log.i(TAG, String.format("User(%s) delete a account: %s", loginUserTbData.getAccount(), d.getAccount()));
                }
            }
        } catch (Exception e) {
            Timber.e(e);
        }

        try {
            scheduledExecutorService.execute(new Runnable() {
                @Override
                public void run() {
                    // 刪檔案
                    MyApplication myApplication = getMyApplication();
                    if (myApplication == null) {
                        return;
                    }

                    File mainDirFile = new File(myApplication.getMainDirPath());

                    File[] dirFileList = null;
                    try {
                        dirFileList = mainDirFile.listFiles(new FilenameFilter() {
                            @Override
                            public boolean accept(File dirFile, String filename) {
                                if (!dirFile.isDirectory()) {
                                    return false;
                                }

                                String[] nameArr = filename.split("_");

                                if (nameArr.length == 0) {
                                    return false;
                                }

                                return d.getAccount().equals(nameArr[0]);
                            }
                        });
                    } catch (Exception e) {
                        Timber.e(e);
                    }
                    if (dirFileList == null) {
                        dirFileList = new File[]{};
                    }

                    for (File dirFile : dirFileList) {
                        try {
                            FileUtils.deleteDirectory(dirFile);
                        } catch (Exception e) {
                            Timber.e(e);
                        }
                    }
                }
            });
        } catch (Exception e) {
            Timber.e(e);
        }

        try {
            scheduledExecutorService.execute(new Runnable() {
                @Override
                public void run() {
                    // 刪ProcedureFolderTbData 資料
                    MyApplication myApplication = getMyApplication();
                    if (myApplication == null) {
                        return;
                    }
                    List<ProcedureFolderTbData> dataList = null;
                    try {
                        dataList = myApplication.getMyRoomDatabase()
                                .procedureFolderTbDataDao().findDataList(d.getAccount());
                    } catch (Exception e) {
                        Timber.e(e);
                    }
                    if (dataList == null) {
                        dataList = new ArrayList<>();
                    }

                    try {
                        myApplication.getMyRoomDatabase().procedureFolderTbDataDao().deletes(dataList);
                    } catch (Exception e) {
                        Timber.e(e);
                    }
                }
            });
        } catch (Exception e) {
            Timber.e(e);
        }

        try {
            scheduledExecutorService.execute(new Runnable() {
                @Override
                public void run() {
                    // 刪imageAdjustmentSetting 資料
                    MyApplication myApplication = getMyApplication();
                    if (myApplication == null) {
                        return;
                    }
                    List<ImageAdjustmentSettingTbData> dataList = null;
                    try {
                        dataList = myApplication.getMyRoomDatabase()
                                .imageAdjustmentSettingTbDataDao().findDataList(d.getAccount());
                    } catch (Exception e) {
                        Timber.e(e);
                    }

                    if (dataList == null) {
                        dataList = new ArrayList<>();
                    }

                    try {
                        myApplication.getMyRoomDatabase().imageAdjustmentSettingTbDataDao().deletes(dataList);
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
