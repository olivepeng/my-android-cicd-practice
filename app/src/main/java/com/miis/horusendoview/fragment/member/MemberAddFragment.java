package com.miis.horusendoview.fragment.member;

import static com.miis.horusendoview.fragment.DataManagementFragment.CheckPasswordRule;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import androidx.appcompat.widget.ListPopupWindow;

import com.app.customkeyboard.keyboard.CustomKeyboardView;
import com.miis.horusendoview.MyApplication;
import com.miis.horusendoview.R;
import com.miis.horusendoview.activity.MainActivity;
import com.miis.horusendoview.databinding.FragmentMemberAddBinding;
import com.miis.horusendoview.dialog.MyDialog;
import com.miis.horusendoview.fragment.base.ChildBaseFragment;
import com.miis.horusendoview.log.LogQueue;
import com.miis.horusendoview.roomDataBase.imageAdjustmentSetting.ImageAdjustmentSettingTbData;
import com.miis.horusendoview.roomDataBase.userTbData.UserTbData;
import com.miis.horusendoview.type.UserRoleType;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import timber.log.Timber;

public class MemberAddFragment extends ChildBaseFragment implements View.OnClickListener {


    private final String TAG = MemberAddFragment.class.getSimpleName();

    public boolean isNotAddToBackStack() {
        return false;
    }

    @Nullable
    private FragmentMemberAddBinding binding;

    @NonNull
    private UserRoleType selectUserRoleType = UserRoleType.ADVANCED_USER;

    @Nullable
    private ListPopupWindow userRoleTypePopupMenu;

    @Nullable
    private MyDialog warningDialog;

    @NonNull
    private final ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(3);

    @NonNull
    public static MemberAddFragment newInstance() {
        return new MemberAddFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (binding == null) {
            binding = FragmentMemberAddBinding.inflate(inflater, container, false);
        }
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        FragmentMemberAddBinding binding = MemberAddFragment.this.binding;
        if (binding == null) {
            return;
        }

        binding.cancel.setOnClickListener(this);
        binding.create.setOnClickListener(this);
        binding.type.setOnClickListener(this);

        setSelectUserRoleType(selectUserRoleType);

        MainActivity mainActivity = (MainActivity) getActivity();
        if (mainActivity != null) {
            mainActivity.getBinding().customKeyboardView.registerEditText(CustomKeyboardView.KeyboardType.QWERTY, binding.account);
            mainActivity.getBinding().customKeyboardView.registerEditText(CustomKeyboardView.KeyboardType.QWERTY, binding.password);
            mainActivity.getBinding().customKeyboardView.registerEditText(CustomKeyboardView.KeyboardType.QWERTY, binding.repeatPassword);
        }
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
    public void onPause() {
        super.onPause();
        cancelTypeMenu();
        cancelWarningUserDialog();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        MainActivity mainActivity = (MainActivity) getActivity();
        if (mainActivity != null) {
            FragmentMemberAddBinding binding = MemberAddFragment.this.binding;
            if (binding != null) {
                // 解註冊虛擬鍵盤
                mainActivity.getBinding().customKeyboardView.unregisterEditText(binding.account);
                mainActivity.getBinding().customKeyboardView.unregisterEditText(binding.password);
                mainActivity.getBinding().customKeyboardView.unregisterEditText(binding.repeatPassword);
            }
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
        if (hidden) {
            cancelTypeMenu();
            cancelWarningUserDialog();
        }
    }

    @Override
    public void onClick(View v) {
        final FragmentMemberAddBinding binding = MemberAddFragment.this.binding;
        if (binding == null) {
            return;
        }
        Timber.d("[onClick] "+ LogQueue.getId(v));
        switch (v.getId()) {
            case R.id.cancel:
                if (getChangeChildFragmentManager() != null) {
                    getChangeChildFragmentManager().closePage();
                }
                break;

            case R.id.create:
                Context context = getContext();
                if (context == null) {
                    return;
                }
                String account = null;
                if (binding.account.getText() != null) {
                    account = binding.account.getText().toString().trim();
                }
                if (account == null) {
                    account = "";
                }

                String password = null;
                if (binding.password.getText() != null) {
                    password = binding.password.getText().toString().trim();
                }
                if (password == null) {
                    password = "";
                }


                String repeatPassword = null;
                if (binding.repeatPassword.getText() != null) {
                    repeatPassword = binding.repeatPassword.getText().toString().trim();
                }
                if (repeatPassword == null) {
                    repeatPassword = "";
                }

                UserRoleType selectUserRoleType = this.selectUserRoleType;

                if (account.isEmpty()) {
                    showWarningUserDialog(context.getString(R.string.account_is_required), new MyDialog.Listener() {
                        @Override
                        public void OnClickConfirm() {
                            binding.account.requestFocus();
                        }

                        @Override
                        public void OnClickCancel() {
                            // Do nothing
                        }
                    });

                    binding.create.setCheckable(true);
                    return;
                }

                if (password.isEmpty()) {
                    showWarningUserDialog(context.getString(R.string.password_is_required), new MyDialog.Listener() {
                        @Override
                        public void OnClickConfirm() {
                            binding.password.requestFocus();
                        }

                        @Override
                        public void OnClickCancel() {
                            // Do nothing
                        }
                    });

                    binding.create.setCheckable(true);
                    return;
                }

                if (repeatPassword.isEmpty()) {
                    showWarningUserDialog(context.getString(R.string.repeat_password_is_required), new MyDialog.Listener() {
                        @Override
                        public void OnClickConfirm() {
                            binding.repeatPassword.requestFocus();
                        }

                        @Override
                        public void OnClickCancel() {
                            // Do nothing
                        }
                    });

                    binding.create.setCheckable(true);
                    return;
                }

                if (!password.equals(repeatPassword)) {
                    showWarningUserDialog(context.getString(R.string.repeat_password_is_incorrect), new MyDialog.Listener() {
                        @Override
                        public void OnClickConfirm() {
                            binding.repeatPassword.requestFocus();
                        }

                        @Override
                        public void OnClickCancel() {

                        }
                    });

                    binding.create.setCheckable(true);
                    return;
                }



                if (CheckPasswordRule(password) != 1) {
                    showWarningUserDialog(getString(R.string.password_rule), new MyDialog.Listener() {
                        @Override
                        public void OnClickConfirm() {
                               binding.repeatPassword.requestFocus();
                        }

                        @Override
                        public void OnClickCancel() {

                        }
                    });
                    return;
                }

                MyApplication myApplication = getMyApplication();

                if (myApplication == null) {
                    binding.create.setCheckable(true);
                    return;
                }

                try {
                    final String finalAccount = account;
                    final String finalPassword = password;
                    scheduledExecutorService.execute(new Runnable() {
                        @Override
                        public void run() {

                            UserTbData f;
                            try {
                                f = myApplication.getMyRoomDatabase().userTbDataDao().findByAccount(finalAccount.toLowerCase());
                            } catch (Exception e) {
                                Timber.e(e);
                                f = null;
                            }

                            if (f != null) {
                                binding.getRoot().post(new Runnable() {
                                    @Override
                                    public void run() {
                                        showWarningUserDialog(getString(R.string.account_is_existed), null);
                                    }
                                });

                                binding.create.setCheckable(true);
                                return;
                            }

                            UserTbData userTbData = new UserTbData(UUID.randomUUID().toString(), finalAccount, finalPassword, "");
                            userTbData.setRoleType(selectUserRoleType);

                            try {
                                myApplication.getMyRoomDatabase().userTbDataDao().insert(userTbData);

                                @Nullable UserTbData loginUserTbData = myApplication.getLoginUserTbData();

                                if (loginUserTbData != null) {
                                    Log.i(TAG,String.format("User(%s) add a account: %s", loginUserTbData.getAccount(), userTbData.getAccount()));

                                    List<ImageAdjustmentSettingTbData> imageAdjustmentSettingDataList;
                                    try {
                                        imageAdjustmentSettingDataList = myApplication.getMyRoomDatabase().imageAdjustmentSettingTbDataDao().findDataList(loginUserTbData.getAccount());
                                    } catch (Exception e) {
                                        Timber.e(e);
                                        imageAdjustmentSettingDataList = null;
                                    }

                                    if (imageAdjustmentSettingDataList != null && !imageAdjustmentSettingDataList.isEmpty()) {
                                        for (ImageAdjustmentSettingTbData imageAdjustmentSettingData : imageAdjustmentSettingDataList) {
                                            try {
                                                imageAdjustmentSettingData.setId(UUID.randomUUID().toString());
                                                imageAdjustmentSettingData.setAccount(finalAccount);
                                                myApplication.getMyRoomDatabase().imageAdjustmentSettingTbDataDao().insert(imageAdjustmentSettingData);
                                            } catch (Exception e) {
                                                Timber.e(e);
                                            }
                                        }
                                    }
                                }

                                binding.getRoot().post(new Runnable() {
                                    @Override
                                    public void run() {
                                        if (getChangeChildFragmentManager() != null) {
                                            getChangeChildFragmentManager().closePage();
                                        }
                                    }
                                });
                            } catch (Exception e) {
                                Timber.e(e);
                                binding.getRoot().post(new Runnable() {
                                    @Override
                                    public void run() {
                                        binding.create.setCheckable(true);
                                    }
                                });
                            }
                        }
                    });
                } catch (Exception e) {
                    Timber.e(e);
                }
                break;

            case R.id.type:
                showTypeMenu();
                break;
        }
    }

    private void showTypeMenu() {
        cancelTypeMenu();
        final FragmentMemberAddBinding binding = MemberAddFragment.this.binding;
        final Context context = getContext();
        if (context == null || binding == null) {
            return;
        }

        final ListPopupWindow popupWindow = new ListPopupWindow(context, null, 0, R.style.myListPopupWindowStyle);
        popupWindow.setAnchorView(binding.type);

        final ArrayList<String> list = new ArrayList<>();
        final ArrayList<UserRoleType> typeList = new ArrayList<>();

        for (UserRoleType type : UserRoleType.values()) {
            @Nullable final Integer stringId = type.getStringId();
            if (stringId != null) {
                list.add(context.getString(stringId));
                typeList.add(type);
            }
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(context, R.layout.list_popup_window_item, list);
        popupWindow.setAdapter(adapter);

        popupWindow.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                UserRoleType type = typeList.get(position);
                setSelectUserRoleType(type);
                cancelTypeMenu();
            }
        });

        popupWindow.setVerticalOffset(-binding.type.getMeasuredHeight());
        popupWindow.show();
        userRoleTypePopupMenu = popupWindow;
    }

    private void cancelTypeMenu() {
        final ListPopupWindow userRoleTypePopupMenu = MemberAddFragment.this.userRoleTypePopupMenu;
        if (userRoleTypePopupMenu != null) {
            userRoleTypePopupMenu.dismiss();
            MemberAddFragment.this.userRoleTypePopupMenu = null;
        }
    }

    @UiThread
    private void showWarningUserDialog(@NonNull String msg, @Nullable MyDialog.Listener listener) {
        final Context context = getContext();
        if (context == null) {
            return;
        }

        cancelWarningUserDialog();

        MyDialog dialog = new MyDialog(context, false);
        dialog.setTitle(context.getString(R.string.warning));
        dialog.setMsg(msg);
        dialog.setConfirmText(context.getString(R.string.ok));
        dialog.showCancel(false);
        dialog.setListener(listener);
        dialog.setCancelable(false);
        dialog.show();

        setWarningDialog(dialog);
    }

    private void cancelWarningUserDialog() {
        if (warningDialog != null) {
            warningDialog.dismiss();
        }
    }

    public void setSelectUserRoleType(@NonNull UserRoleType selectUserRoleType) {
        this.selectUserRoleType = selectUserRoleType;
        @Nullable final Integer stringId = this.selectUserRoleType.getStringId();
        final FragmentMemberAddBinding binding = MemberAddFragment.this.binding;
        if (stringId != null && binding != null) {
            binding.typeText.setText(stringId);
        }
    }

    public void setWarningDialog(@Nullable MyDialog warningDialog) {
        if (this.warningDialog != warningDialog && this.warningDialog != null && this.warningDialog.isShowing()) {
            this.warningDialog.dismiss();
        }
        this.warningDialog = warningDialog;
    }
}

