package com.miis.horusendoview.fragment.member;

import static com.miis.horusendoview.fragment.DataManagementFragment.CheckPasswordRule;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.app.customkeyboard.keyboard.CustomKeyboardView;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.miis.horusendoview.MyApplication;
import com.miis.horusendoview.R;
import com.miis.horusendoview.activity.MainActivity;
import com.miis.horusendoview.databinding.FragmentMemberUserEditBinding;
import com.miis.horusendoview.dialog.MyDialog;
import com.miis.horusendoview.fragment.base.ChildBaseFragment;
import com.miis.horusendoview.log.LogQueue;
import com.miis.horusendoview.roomDataBase.userTbData.UserTbData;
import com.miis.horusendoview.type.UserRoleType;

import timber.log.Timber;

public class MemberUserEditFragment extends ChildBaseFragment {

    private static final String TAG = MemberUserEditFragment.class.getSimpleName();

    @Override
    public boolean isNotAddToBackStack() {
        return false;
    }

    @Nullable
    private FragmentMemberUserEditBinding binding;

    @Nullable
    private MyDialog warningDialog;

    @NonNull
    public static MemberUserEditFragment newInstance() {
        return new MemberUserEditFragment();
    }


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (binding == null) {
            binding = FragmentMemberUserEditBinding.inflate(inflater, container, false);
        }
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        final FragmentMemberUserEditBinding binding = MemberUserEditFragment.this.binding;
        if (binding == null) {
            return;
        }
        binding.logout.setOnClickListener(this);
        binding.save.setOnClickListener(this);

        setLoginUserDataToView();

        MainActivity mainActivity = getMainActivity();
        if (mainActivity != null) {
            // 註冊虛擬鍵盤
            mainActivity.getBinding().customKeyboardView.unregisterEditText(binding.oldPassword);
            mainActivity.getBinding().customKeyboardView.registerEditText(CustomKeyboardView.KeyboardType.QWERTY, binding.oldPassword);
            mainActivity.getBinding().customKeyboardView.unregisterEditText(binding.newPassword);
            mainActivity.getBinding().customKeyboardView.registerEditText(CustomKeyboardView.KeyboardType.QWERTY,binding.newPassword);
            mainActivity.getBinding().customKeyboardView.unregisterEditText(binding.confirmPassword);
            mainActivity.getBinding().customKeyboardView.registerEditText(CustomKeyboardView.KeyboardType.QWERTY,binding.confirmPassword);
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
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        MainActivity mainActivity = getMainActivity();
        if(mainActivity!=null) {
            final FragmentMemberUserEditBinding binding = MemberUserEditFragment.this.binding;
            if (binding == null) {
                return;
            }
            mainActivity.getBinding().customKeyboardView.unregisterEditText(binding.oldPassword);
            mainActivity.getBinding().customKeyboardView.unregisterEditText(binding.newPassword);
            mainActivity.getBinding().customKeyboardView.unregisterEditText(binding.confirmPassword);
        }
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (hidden) {
            cancelWarningUserDialog();
        } else {
            setLoginUserDataToView();
        }
    }

    @Override
    public void onClick(View v) {
        final MyApplication myApplication = getMyApplication();
        final FragmentMemberUserEditBinding binding = MemberUserEditFragment.this.binding;
        if (binding == null) {
            return;
        }
        if (myApplication == null) {
            return;
        }
        Timber.d("[onClick] "+ LogQueue.getId(v));
        switch (v.getId()) {
            case R.id.logout:
                myApplication.setLoginUserTbData(null);
                Log.i(TAG, "User logout");
                break;

            case R.id.save:
                final Context context = getContext();
                if (context == null) {
                    return;
                }
                UserTbData userTbData = myApplication.getLoginUserTbData();
                if (userTbData == null) {
                    return;
                }
                UserTbData loginUserTbData = null;
                try {
                    loginUserTbData = new Gson().fromJson(new Gson().toJson(userTbData), UserTbData.class);
                } catch (JsonSyntaxException e) {
                    Timber.e(e);
                }
                if (loginUserTbData == null) {
                    return;
                }
                String oldPassword = null;
                if (binding.oldPassword.getText() != null) {
                    oldPassword = binding.oldPassword.getText().toString().trim();
                }
                if (oldPassword == null) {
                    oldPassword = "";
                }

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

                if (oldPassword.isEmpty()) {
                    showWarningUserDialog(context.getString(R.string.warning), context.getString(R.string.old_password_is_required), new MyDialog.Listener() {
                        @Override
                        public void OnClickConfirm() {
                            binding.oldPassword.requestFocus();
                        }

                        @Override
                        public void OnClickCancel() {

                        }
                    });
                    return;
                }

                if (!loginUserTbData.getPassword().equals(oldPassword)) {
                    showWarningUserDialog(context.getString(R.string.warning), context.getString(R.string.old_password_is_incorrect), new MyDialog.Listener() {
                        @Override
                        public void OnClickConfirm() {
                            binding.oldPassword.requestFocus();
                        }

                        @Override
                        public void OnClickCancel() {

                        }
                    });
                    return;
                }

                if (newPassword.isEmpty()) {
                    showWarningUserDialog(context.getString(R.string.warning), context.getString(R.string.password_is_required), new MyDialog.Listener() {
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
                    showWarningUserDialog(context.getString(R.string.warning), context.getString(R.string.confirm_password_is_required), new MyDialog.Listener() {
                        @Override
                        public void OnClickConfirm() {
                            binding.confirmPassword.requestFocus();
                        }

                        @Override
                        public void OnClickCancel() {

                        }
                    });
                    return;
                }

                if (!confirmPassword.equals(newPassword)) {
                    showWarningUserDialog(context.getString(R.string.warning), context.getString(R.string.confirm_password_is_incorrect), new MyDialog.Listener() {
                        @Override
                        public void OnClickConfirm() {
                            binding.confirmPassword.requestFocus();
                        }

                        @Override
                        public void OnClickCancel() {

                        }
                    });
                    return;
                }

                loginUserTbData.setPassword(newPassword);

                try {
                    myApplication.getMyRoomDatabase().userTbDataDao().update(loginUserTbData);
                    myApplication.setLoginUserTbData(loginUserTbData);
                    Log.i(TAG,String.format("User(%s) modify the password of %s", myApplication.getLoginUserTbData().getAccount(), userTbData.getAccount()));
                    showWarningUserDialog(context.getString(R.string.info), context.getString(R.string.change_password_success), new MyDialog.Listener() {
                        @Override
                        public void OnClickConfirm() {
                            binding.oldPassword.setText("");
                            binding.newPassword.setText("");
                            binding.confirmPassword.setText("");
                        }

                        @Override
                        public void OnClickCancel() {

                        }
                    });
                } catch (Exception e) {
                    Timber.e(e);
                    showWarningUserDialog(context.getString(R.string.warning), context.getString(R.string.change_password_fail), null);
                }
                break;
        }
    }

    public void setLoginUserDataToView() {
        MyApplication myApplication = getMyApplication();
        UserTbData loginUserTbData = null;
        if (myApplication != null) {
            loginUserTbData = myApplication.getLoginUserTbData();
        }
        if (loginUserTbData == null) {
            return;
        }
        UserRoleType roleType = loginUserTbData.getRoleType();
        int imageResource;
        if (roleType == UserRoleType.GUEST || roleType == UserRoleType.ADVANCED_USER) {
            imageResource = R.drawable.default_user_128x128;
        } else {
            imageResource = R.drawable.admin_user_128x128;
        }
        final FragmentMemberUserEditBinding binding = MemberUserEditFragment.this.binding;
        if (binding != null) {
            binding.loginImg.setImageResource(imageResource);
            String account = loginUserTbData.getAccount();
            binding.loginText.setText(account.isEmpty() ? "-" : account);
            binding.account.setText(loginUserTbData.getAccount());
        }
    }

    private void showWarningUserDialog(@NonNull String title, @NonNull String msg, @Nullable MyDialog.Listener listener) {
        cancelWarningUserDialog();
        Context context = getContext();
        if (context == null) {
            return;
        }
        Timber.d("cancelWarningUserDialog showWarningUserDialog");
        MyDialog d = new MyDialog(context, false);
        d.setTitle(title);
        d.setMsg(msg);
        d.setConfirmText(context.getString(R.string.ok));
        d.showCancel(false);
        d.setListener(listener);
        d.setCancelable(false);
        d.show();
        warningDialog = d;
    }

    private void cancelWarningUserDialog() {
        Timber.d("cancelWarningUserDialog");
        final MyDialog warningDialog = MemberUserEditFragment.this.warningDialog;
        if (warningDialog != null) {
            warningDialog.dismiss();
        }
    }

    @Nullable
    public FragmentMemberUserEditBinding getBinding() {
        return binding;
    }
}
