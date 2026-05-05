package com.miis.horusendoview.fragment;

import android.content.Context;
import android.os.Bundle;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentTransaction;

import com.app.customkeyboard.keyboard.CustomKeyboardView;
import com.miis.horusendoview.MyApplication;
import com.miis.horusendoview.R;
import com.miis.horusendoview.activity.MainActivity;
import com.miis.horusendoview.databinding.FragmentLoginBinding;
import com.miis.horusendoview.dialog.MyDialog;
import com.miis.horusendoview.fragment.base.BaseFragment;
import com.miis.horusendoview.log.LogQueue;
import com.miis.horusendoview.manager.SharedPreferencesManager;
import com.miis.horusendoview.roomDataBase.userTbData.UserTbData;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import timber.log.Timber;

public class LoginFragment extends BaseFragment implements View.OnClickListener {

    private static final String TAG = LoginFragment.class.getSimpleName();

    @Override
    public boolean isNotAddToBackStack() {
        return true;
    }

    @Nullable
    private FragmentLoginBinding binding = null;
    private boolean isPasswordHide = true;

    @Nullable
    private MyDialog warningDialog = null;

    @Nullable
    private Runnable loginRunnable = null;

    @NonNull
    private final ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(3);

    @NonNull
    private int countLoginError=0;

    public LoginFragment() {
    }

    @NonNull
    public static LoginFragment newInstance(@Nullable Runnable r) {
        LoginFragment fragment = new LoginFragment();
        fragment.loginRunnable = r;
        return fragment;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (binding == null) {
            binding = FragmentLoginBinding.inflate(inflater, container, false);
        }
        return binding.getRoot();
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
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        final FragmentLoginBinding binding = LoginFragment.this.binding;
        if (binding == null) {
            return;
        }

        binding.passwordEye.setOnClickListener(this);
        binding.remember.setOnClickListener(this);
        binding.login.setOnClickListener(this);

        try {
            scheduledExecutorService.execute(new Runnable() {
                @Override
                public void run() {
                    boolean isRememberAccountEnable = SharedPreferencesManager.getInstance().isRememberAccountEnable();

                    final FragmentLoginBinding binding = LoginFragment.this.binding;
                    if (binding == null) {
                        return;
                    }
                    binding.getRoot().post(new Runnable() {
                        @Override
                        public void run() {
                            binding.rememberSwitch.setChecked(isRememberAccountEnable);
                        }
                    });


                    if (isRememberAccountEnable) {
                        String rememberAccount = SharedPreferencesManager.getInstance().getRememberAccount();
                        binding.getRoot().post(new Runnable() {
                            @Override
                            public void run() {
                                binding.account.setText(rememberAccount);
                            }
                        });
                    }
                }
            });
        } catch (Exception e) {
            Timber.e(e);
        }

        MainActivity mainActivity = getMainActivity();
        if (mainActivity != null) {
            CustomKeyboardView customKeyboardView = mainActivity.getBinding().customKeyboardView;

            customKeyboardView.unregisterEditText(binding.account);
            customKeyboardView.registerEditText(CustomKeyboardView.KeyboardType.QWERTY, binding.account);

            customKeyboardView.unregisterEditText(binding.password);
            customKeyboardView.registerEditText(CustomKeyboardView.KeyboardType.QWERTY, binding.password);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        MainActivity activity = getMainActivity();
        if (activity != null) {
            activity.setTabBtnSelected(null);
            final FragmentLoginBinding binding = LoginFragment.this.binding;
            if (binding != null) {
                binding.getRoot().postDelayed(() -> activity.setTabBtnClickable(true), MainActivity.TAB_BTN_CLICKABLE_ON_DELAY);
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        cancelDeleteUserDialog();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        MainActivity mainActivity = getMainActivity();
        if (mainActivity != null) {
            final FragmentLoginBinding binding = LoginFragment.this.binding;
            if (binding != null) {
                CustomKeyboardView customKeyboardView = mainActivity.getBinding().customKeyboardView;
                customKeyboardView.unregisterEditText(binding.account);
                customKeyboardView.unregisterEditText(binding.password);
            }
        }

        if (mainActivity != null) {
            mainActivity.setLoginFragment(null);
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
    public void onClick(View v) {
        final FragmentLoginBinding binding = LoginFragment.this.binding;
        if (binding == null) {
            return;
        }
        Timber.d("[onClick] "+ LogQueue.getId(v));
        switch (v.getId()) {
            case R.id.passwordEye:
                setPasswordHide(!isPasswordHide);
                break;
            case R.id.remember:
                binding.rememberSwitch.setChecked(!binding.rememberSwitch.isChecked());
                break;
            case R.id.login:
                String account = null;
                try {
                    if (binding.account.getText() != null) {
                        account = binding.account.getText().toString().trim();
                    }
                } catch (Exception e) {
                    Timber.e(e);
                }
                if (account == null) {
                    account = "";
                }
                String password = null;
                try {
                    if (binding.password.getText() != null) {
                        password = binding.password.getText().toString().trim();
                    }
                } catch (Exception e) {
                    Timber.e(e);
                }
                if (password == null) {
                    password = "";
                }

                if (account.isEmpty()) {
                    showWarningUserDialog(getString(R.string.warning), getString(R.string.account_is_required),
                            new MyDialog.Listener() {
                                @Override
                                public void OnClickConfirm() {
                                    binding.account.requestFocus();
                                }

                                @Override
                                public void OnClickCancel() {

                                }
                            }
                    );
                    return;
                }

                if (password.isEmpty()) {
                    showWarningUserDialog(getString(R.string.warning), getString(R.string.password_is_required),
                            new MyDialog.Listener() {
                                @Override
                                public void OnClickConfirm() {
                                    binding.password.requestFocus();
                                }

                                @Override
                                public void OnClickCancel() {

                                }
                            });
                    return;
                }

                MyApplication myApplication = getMyApplication();

                UserTbData user = null;
                try {
                    if (myApplication != null) {
                        user = myApplication.getMyRoomDatabase().userTbDataDao().findByAccountAndPassword(account.toLowerCase(), password);
                    }
                } catch (Exception e) {
                    Timber.e(e);
                }

                if (user == null) {
                    ++countLoginError;
                    Log.w(TAG, String.format("The user tried to log in and the number of errors was %d.", countLoginError));

                    showWarningUserDialog(getString(R.string.login_failed), getString(R.string.login_failed_msg),null);
                    return;
                }

                countLoginError=0;
                try {
                    myApplication.setLoginUserTbData(user);
                    Log.i(TAG, String.format("User login: %s (%s)", user.getAccount(), user.getRoleType()));
                } catch (Exception e) {
                    Timber.e(e);
                }

                SharedPreferencesManager.getInstance().saveRememberAccountEnable(binding.rememberSwitch.isChecked());
                if (binding.rememberSwitch.isChecked()) {
                    SharedPreferencesManager.getInstance().saveRememberAccount(account);
                } else {
                    SharedPreferencesManager.getInstance().saveRememberAccount("");
                }

                FragmentTransaction fragmentTransaction = getParentFragmentManager().beginTransaction();
                fragmentTransaction.remove(this);
                if (getParentFragmentManager().isStateSaved()) {
                    fragmentTransaction.commitAllowingStateLoss();
                } else {
                    fragmentTransaction.commit();
                }


                MainActivity mainActivity = getMainActivity();
                if (mainActivity != null) {
                    mainActivity.setLoginFragment(null);
                    Timber.d("login 02");
                    if (loginRunnable != null) {
                        Timber.d("login 03 loginRunnable=" + loginRunnable);
                        loginRunnable.run();
                    } else {
                        //mainActivity.getBinding().ibLiveView.performClick();
                        //Jerry +
                        if ((account.toLowerCase().matches("admin") && (password.matches("123456"))) ||
                        (account.toLowerCase().matches("service") && password.matches("123456")))
                        {
                            //showWarningUserDialog("Please change admin default password", "Please change default password", null);

                            mainActivity.getMemberFragment().showMemberUserAdminFragment(user);
                            mainActivity.getBinding().user.performClick();


                        }
                        else {
                            mainActivity.getBinding().ibLiveView.performClick();
                        }
                        //Jerry -
                        Timber.d("login 04");
                    }
                }
                break;
        }
    }

    private void showWarningUserDialog(@NonNull String title, @NonNull String msg, @Nullable MyDialog.Listener listener) {
        Context context = getContext();
        if (context != null) {
            MyDialog dialog = new MyDialog(context, false);
            dialog.setTitle(title);
            dialog.setMsg(msg);
            dialog.setConfirmText(getString(R.string.ok));
            dialog.showCancel(false);
            dialog.setListener(listener);
            dialog.setCancelable(false);
            dialog.show();
            setWarningDialog(dialog);
        }
    }

    private void cancelDeleteUserDialog() {
        setWarningDialog(null);
    }

    public void setWarningDialog(MyDialog warningDialog) {
        if (this.warningDialog != null && this.warningDialog.isShowing()) {
            this.warningDialog.dismiss();
        }
        this.warningDialog = warningDialog;
    }

    public void setPasswordHide(boolean passwordHide) {
        isPasswordHide = passwordHide;
        final FragmentLoginBinding binding = LoginFragment.this.binding;
        if (binding == null) {
            return;
        }
        if (isPasswordHide) {
            binding.password.setTransformationMethod(PasswordTransformationMethod.getInstance());
            binding.passwordEye.setImageResource(R.drawable.eye_off);
        } else {
            binding.password.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
            binding.passwordEye.setImageResource(R.drawable.eye_on);
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
