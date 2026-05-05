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
import androidx.appcompat.widget.ListPopupWindow;

import com.app.customkeyboard.keyboard.CustomKeyboardView;
import com.google.gson.Gson;
import com.miis.horusendoview.MyApplication;
import com.miis.horusendoview.R;
import com.miis.horusendoview.activity.MainActivity;
import com.miis.horusendoview.databinding.FragmentMemberUserAdminBinding;
import com.miis.horusendoview.dialog.MyDialog;
import com.miis.horusendoview.fragment.base.ChildBaseFragment;
import com.miis.horusendoview.log.LogQueue;
import com.miis.horusendoview.roomDataBase.userTbData.UserTbData;
import com.miis.horusendoview.type.UserRoleType;

import java.util.ArrayList;

import timber.log.Timber;

public class MemberUserAdminFragment extends ChildBaseFragment implements View.OnClickListener {

    private static final String TAG = MemberUserAdminFragment.class.getSimpleName();

    @Override
    public boolean isNotAddToBackStack() {
        return false;
    }

    @Nullable
    private FragmentMemberUserAdminBinding binding;

    @NonNull
    private UserRoleType selectUserRoleType = UserRoleType.ADVANCED_USER;

    @Nullable
    private MyDialog warningDialog;

    @Nullable
    private ListPopupWindow userRoleTypePopupMenu;

    @NonNull
    private UserTbData userTbData;

    private static final String KEY_USER_DATA = "USER_DATA";

    @NonNull
    public static MemberUserAdminFragment newInstance(@NonNull UserTbData loginUserTbData) {
        MemberUserAdminFragment fragment = new MemberUserAdminFragment();
        Bundle args = new Bundle();
        args.putString(KEY_USER_DATA, new Gson().toJson(loginUserTbData));
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            String jsonUserData = getArguments().getString(KEY_USER_DATA, "");
            userTbData = new Gson().fromJson(jsonUserData, UserTbData.class);
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (binding == null) {
            binding = FragmentMemberUserAdminBinding.inflate(inflater, container, false);
        }
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        final FragmentMemberUserAdminBinding binding = MemberUserAdminFragment.this.binding;
        if (binding == null) {
            return;
        }
        binding.cancel.setOnClickListener(this);
        binding.save.setOnClickListener(this);
        binding.type.setOnClickListener(this);

        setLoginUserDataToView();


        MainActivity mainActivity = getMainActivity();
        if (mainActivity != null) {
            mainActivity.getBinding().customKeyboardView.unregisterEditText(binding.newPassword);
            mainActivity.getBinding().customKeyboardView.registerEditText(CustomKeyboardView.KeyboardType.QWERTY, binding.newPassword);
            mainActivity.getBinding().customKeyboardView.unregisterEditText(binding.confirmPassword);
            mainActivity.getBinding().customKeyboardView.registerEditText(CustomKeyboardView.KeyboardType.QWERTY, binding.confirmPassword);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        setLoginUserDataToView();
    }

    @Override
    public void onPause() {
        super.onPause();
        cancelWarningUserDialog();
        cancelTypeMenu();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        MainActivity mainActivity = getMainActivity();
        final FragmentMemberUserAdminBinding binding = MemberUserAdminFragment.this.binding;
        if (binding == null) {
            return;
        }
        if (mainActivity != null) {
            mainActivity.getBinding().customKeyboardView.unregisterEditText(binding.newPassword);
            mainActivity.getBinding().customKeyboardView.unregisterEditText(binding.confirmPassword);
        }
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (hidden) {
            cancelWarningUserDialog();
            cancelTypeMenu();
        } else {
            setLoginUserDataToView();
        }
    }

    @Override
    public void onClick(View v) {
        final FragmentMemberUserAdminBinding binding = MemberUserAdminFragment.this.binding;
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
            case R.id.type:
                showTypeMenu();
                break;
            case R.id.save:
                MyApplication myApplication = getMyApplication();
                Context context = getContext();
                if (myApplication == null || context == null) {
                    return;
                }
                UserTbData userTbDataTemp = userTbData;
                UserTbData userTbData = new Gson().fromJson(new Gson().toJson(userTbDataTemp), UserTbData.class);
                UserRoleType selectUserRoleType = this.selectUserRoleType;

                String newPassword = null;
                if (binding.newPassword.getText() != null) {
                    newPassword = binding.newPassword.getText().toString().trim();
                }
                if (newPassword == null) {
                    newPassword = "";
                }

                String confirmPassword = null;
                if (binding.confirmPassword.getText() != null) {
                    confirmPassword = binding.confirmPassword.getText().toString().trim();
                }
                if (confirmPassword == null) {
                    confirmPassword = "";
                }

                if (newPassword.isEmpty()) {
                    showWarningUserDialog(
                            context.getString(R.string.warning),
                            context.getString(R.string.password_is_required),
                            new MyDialog.Listener() {
                                @Override
                                public void OnClickConfirm() {
                                    binding.newPassword.requestFocus();
                                }

                                @Override
                                public void OnClickCancel() {
                                }
                            }
                    );
                    return;
                }

                if ( CheckPasswordRule(newPassword) != 1) {
                    showWarningUserDialog(getString(R.string.warning), getString(R.string.password_rule)
                            , new MyDialog.Listener() {
                                @Override
                                public void OnClickConfirm() {
                                    binding.newPassword.requestFocus();
                                }

                                @Override
                                public void OnClickCancel() {

                                }
                            });
                    return;
                }

                if (confirmPassword.isEmpty()) {
                    showWarningUserDialog(
                            context.getString(R.string.warning),
                            context.getString(R.string.confirm_password_is_required),
                            new MyDialog.Listener() {
                                @Override
                                public void OnClickConfirm() {
                                    binding.confirmPassword.requestFocus();
                                }

                                @Override
                                public void OnClickCancel() {
                                }
                            }
                    );
                    return;
                }

                if (!confirmPassword.equals(newPassword)) {
                    showWarningUserDialog(
                            context.getString(R.string.warning),
                            context.getString(R.string.confirm_password_is_incorrect),
                            new MyDialog.Listener() {
                                @Override
                                public void OnClickConfirm() {
                                    binding.confirmPassword.requestFocus();
                                }

                                @Override
                                public void OnClickCancel() {
                                }
                            }
                    );
                    return;
                }

                userTbData.setPassword(newPassword);
                userTbData.setRoleType(selectUserRoleType);

                try {
                    myApplication.getMyRoomDatabase().userTbDataDao().update(userTbData);
                    Log.i(TAG,String.format("User(%s) modify the password of %s", myApplication.getLoginUserTbData().getAccount(), userTbData.getAccount()));
                    if (myApplication.getLoginUserTbData() != null && myApplication.getLoginUserTbData().getAccount().equals(userTbData.getAccount())) {
                        myApplication.setLoginUserTbData(userTbData);
                    }

                    showWarningUserDialog(
                            context.getString(R.string.info),
                            context.getString(R.string.change_password_success),
                            new MyDialog.Listener() {
                                @Override
                                public void OnClickConfirm() {
                                    if (getChangeChildFragmentManager() != null) {
                                        getChangeChildFragmentManager().closePage();
                                    }
                                }

                                @Override
                                public void OnClickCancel() {
                                }
                            }
                    );
                } catch (Exception e) {
                    Timber.e(e);
                    showWarningUserDialog(
                            context.getString(R.string.warning),
                            context.getString(R.string.change_password_fail),
                            null
                    );
                }
                break;
        }
    }

    private void setLoginUserDataToView() {
        FragmentMemberUserAdminBinding binding = MemberUserAdminFragment.this.binding;
        if (binding == null) {
            return;
        }
        final UserTbData userTbData = MemberUserAdminFragment.this.userTbData;

        UserRoleType roleType = userTbData.getRoleType();
        if (roleType == UserRoleType.GUEST || roleType == UserRoleType.ADVANCED_USER) {
            binding.loginImg.setImageResource(R.drawable.default_user_128x128);
        } else if (roleType == UserRoleType.ADMIN_USER || roleType == UserRoleType.SERVICE_USER) {
            binding.loginImg.setImageResource(R.drawable.admin_user_128x128);
        }

        binding.loginText.setText(userTbData.getAccount());
        binding.account.setText(userTbData.getAccount());

        UserRoleType userRoleType = userTbData.getRoleType();
        setSelectUserRoleType(userRoleType);

        MyApplication myApplication = getMyApplication();
        UserTbData loginUserTbData = null;
        if (myApplication != null) {
            loginUserTbData = myApplication.getLoginUserTbData();
        }
        if (loginUserTbData != null && (loginUserTbData.getAccount().equals(userTbData.getAccount()) ||
                myApplication.getLoginUserTbData().getAccount().equals("admin") ||
                myApplication.getLoginUserTbData().getAccount().equals("service"))) {
            binding.typeLayout.setVisibility(View.GONE);
        } else {
            binding.typeLayout.setVisibility(View.VISIBLE);
        }
    }

    private void showWarningUserDialog(String title, String msg,@Nullable MyDialog.Listener listener) {
        final Context context = getContext();
        if (context == null) {
            return;
        }
        cancelWarningUserDialog();
        Timber.d("cancelWarningUserDialog showWarningUserDialog");
        MyDialog dialog = new MyDialog(context, false);
        dialog.setTitle(title);
        dialog.setMsg(msg);
        dialog.setConfirmText(context.getString(R.string.ok));
        dialog.showCancel(false);
        dialog.setListener(listener);
        dialog.setCancelable(false);
        dialog.show();
        warningDialog = dialog;
    }

    private void cancelWarningUserDialog() {
        Timber.d("cancelWarningUserDialog");
        if (warningDialog != null) {
            warningDialog.dismiss();
        }
    }

    private void showTypeMenu() {
        cancelTypeMenu();

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
        final ArrayAdapter<String> adapter = new ArrayAdapter<>(context, R.layout.list_popup_window_item, list);
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
        final ListPopupWindow userRoleTypePopupMenu = MemberUserAdminFragment.this.userRoleTypePopupMenu;
        if (userRoleTypePopupMenu != null) {
            userRoleTypePopupMenu.dismiss();
            MemberUserAdminFragment.this.userRoleTypePopupMenu = null;
        }
    }

    public void setSelectUserRoleType(@NonNull UserRoleType selectUserRoleType) {
        this.selectUserRoleType = selectUserRoleType;
        final Integer stringId = this.selectUserRoleType.getStringId();
        final FragmentMemberUserAdminBinding binding = MemberUserAdminFragment.this.binding;
        if (stringId != null && binding != null) {
            binding.typeText.setText(stringId);
        }
    }
}
