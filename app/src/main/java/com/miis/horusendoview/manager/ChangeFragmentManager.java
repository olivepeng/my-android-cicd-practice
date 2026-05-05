package com.miis.horusendoview.manager;

import android.content.Context;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.IdRes;
import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.ViewDataBinding;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.miis.horusendoview.fragment.base.BaseFragment;
import com.miis.horusendoview.fragment.base.ChildBaseFragment;
import com.miis.horusendoview.tools.Tools;

import java.util.List;

import kotlin.jvm.internal.Intrinsics;
import timber.log.Timber;

public class ChangeFragmentManager {

    private final FragmentManager fragmentManager;

    @IdRes
    private final int frameLayoutId;

    @Nullable
    private final FragmentActivity activity;

    @Nullable
    private final Fragment parentFragment;

    public ChangeFragmentManager(FragmentManager fragmentManager, int frameLayoutId, @Nullable FragmentActivity activity, @Nullable Fragment parentFragment) {
        this.fragmentManager = fragmentManager;
        this.frameLayoutId = frameLayoutId;
        this.activity = activity;
        this.parentFragment = parentFragment;
        init();
    }

    private void init() {
        fragmentManager.unregisterFragmentLifecycleCallbacks(fragmentLifecycleCallbacks);
        fragmentManager.registerFragmentLifecycleCallbacks(fragmentLifecycleCallbacks, false);
    }

    private final FragmentManager.FragmentLifecycleCallbacks fragmentLifecycleCallbacks = new FragmentManager.FragmentLifecycleCallbacks() {
        public void onFragmentPreAttached(@NonNull FragmentManager fm, @NonNull Fragment f, @NonNull Context context) {
            super.onFragmentPreAttached(fm, f, context);
            Timber.d("onFragmentPreAttached Fragment=%s", f.toString());
        }

        public void onFragmentAttached(@NonNull FragmentManager fm, @NonNull Fragment f, @NonNull Context context) {
            super.onFragmentAttached(fm, f, context);
            Timber.d("onFragmentAttached Fragment=%s", f.toString());
        }

        public void onFragmentPreCreated(@NonNull FragmentManager fm, @NonNull Fragment f, @Nullable Bundle savedInstanceState) {
            super.onFragmentPreCreated(fm, f, savedInstanceState);
            Timber.d("fonFragmentPreCreated Fragment=%s", f.toString());
        }

        public void onFragmentCreated(@NonNull FragmentManager fm, @NonNull Fragment f, @Nullable Bundle savedInstanceState) {
            super.onFragmentCreated(fm, f, savedInstanceState);
            Timber.d("onFragmentCreated Fragment=%s", f.toString());
        }

        public void onFragmentActivityCreated(@NonNull FragmentManager fm, @NonNull Fragment f, @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
            super.onFragmentActivityCreated(fm, f, savedInstanceState);
            Timber.d("onFragmentActivityCreated Fragment=%s", f.toString());
        }

        public void onFragmentViewCreated(@NonNull FragmentManager fm, @NonNull Fragment f, @NonNull View v, @Nullable Bundle savedInstanceState) {
            super.onFragmentViewCreated(fm, f, v, savedInstanceState);
            Timber.d("onFragmentViewCreated Fragment=%s", f.toString());
        }

        public void onFragmentStarted(@NonNull FragmentManager fm, @NonNull Fragment f) {
            super.onFragmentStarted(fm, f);
            Timber.d("onFragmentStarted Fragment=%s", f.toString());
        }

        public void onFragmentResumed(@NonNull FragmentManager fm, @NonNull Fragment f) {
            super.onFragmentResumed(fm, f);
            Timber.d("onFragmentResumed Fragment=%s", f.toString());
        }

        public void onFragmentPaused(@NonNull FragmentManager fm, @NonNull Fragment f) {
            super.onFragmentPaused(fm, f);
            Timber.d("onFragmentPaused Fragment=%s", f.toString());
        }

        public void onFragmentStopped(@NonNull FragmentManager fm, @NonNull Fragment f) {
            super.onFragmentStopped(fm, f);
            Timber.d("onFragmentStopped Fragment=%s", f.toString());
        }

        public void onFragmentSaveInstanceState(@NonNull FragmentManager fm, @NonNull Fragment f, @NonNull Bundle outState) {
            super.onFragmentSaveInstanceState(fm, f, outState);
            Timber.d("onFragmentSaveInstanceState Fragment=%s", f.toString());
        }

        public void onFragmentViewDestroyed(@NonNull FragmentManager fm, @NonNull Fragment f) {
            super.onFragmentViewDestroyed(fm, f);
            Timber.d("onFragmentViewDestroyed Fragment=%s", f.toString());
        }

        public void onFragmentDestroyed(@NonNull FragmentManager fm, @NonNull Fragment f) {
            super.onFragmentDestroyed(fm, f);
            Timber.d("onFragmentDestroyed Fragment=%s", f.toString());
        }

        public void onFragmentDetached(@NonNull FragmentManager fm, @NonNull Fragment f) {
            super.onFragmentDetached(fm, f);
            Timber.d("onFragmentDetached Fragment=%s", f.toString());
        }
    };

    @MainThread
    public void changePage(@NonNull final BaseFragment baseFragment) {
        Runnable r = new Runnable() {
            @Override
            public void run() {
                if (baseFragment instanceof ChildBaseFragment) {
                    ((ChildBaseFragment)baseFragment).setChangeChildFragmentManager(ChangeFragmentManager.this);
                }

                String tag = baseFragment.getClass().getSimpleName();

                FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();

                fragmentTransaction.setReorderingAllowed(true);

                fragmentTransaction.replace(getFrameLayoutId(), baseFragment, tag);

                if (!baseFragment.isNotAddToBackStack()) {
                    fragmentTransaction.addToBackStack(tag);
                } else {
                    clearBackStack();
                }

                if (getFragmentManager().isStateSaved()) {
                    fragmentTransaction.commitAllowingStateLoss();
                } else {
                    fragmentTransaction.commit();
                }
            }
        };

        final FragmentActivity activity = getActivity();
        View rootView = null;

        Fragment parentFragment = getParentFragment();
        if (parentFragment != null) {
            try {
                Object bindingT = Tools.getPrivateObject(parentFragment, "binding");
                if (bindingT instanceof  ViewDataBinding) {
                    ViewDataBinding binding = (ViewDataBinding) bindingT;
                    rootView = binding.getRoot();
                }
            } catch (Exception e) {
                Timber.e(e);
            }
        }


        if (activity != null) {
            activity.runOnUiThread(r);
        } else if (rootView != null) {
            rootView.post(r);
        }
    }

    @MainThread
    public void clearBackStack() {
        for (int i = getFragmentManager().getBackStackEntryCount() -1 ; i >= 0 ;  i--) {
            if (!getFragmentManager().isStateSaved()) {
                getFragmentManager().popBackStack();
            }
        }
    }

    @MainThread
    public void clearBackStackNow() {
        for (int i = getFragmentManager().getBackStackEntryCount() -1 ; i >= 0 ;  i--) {
            if (!getFragmentManager().isStateSaved()) {
                getFragmentManager().popBackStackImmediate();
            }
        }
    }

    @MainThread
    public void closePage() {
        if (!getFragmentManager().isStateSaved()) {
            getFragmentManager().popBackStack();
        }
    }

    @MainThread
    public void closePageNow() {
        if (!getFragmentManager().isStateSaved()) {
            getFragmentManager().popBackStackImmediate();
        }
    }

    @MainThread
    @Nullable
    public  <T extends BaseFragment>  Fragment closePageTo(@NonNull Class<T> toPage) {
        @Nullable Fragment fragment = null;
        
        for (int i = getFragmentManager().getBackStackEntryCount() -1 ; i >= 0 ;  i--) {
            
            Timber.d("closePageTo i=" + i);
            
            if (!getFragmentManager().isStateSaved()) {
                
                FragmentManager.BackStackEntry backStackEntry = getFragmentManager().getBackStackEntryAt(i);
                
                Timber.d("closePageTo backStackEntry=" + backStackEntry);
                
                fragment = getFragmentManager().findFragmentByTag(backStackEntry.getName());
                
                if (Intrinsics.areEqual(toPage, fragment != null ? fragment.getClass() : null)) {
                    break;
                }

                getFragmentManager().popBackStack();
                
                if (i == 0) {
                    fragment = getFragmentManager().findFragmentByTag(toPage.getSimpleName());
                }
            }
        }

        Timber.d("closePageTo fragment=" + fragment);
        return fragment;
    }

    @Nullable
    public Fragment getNowFragment() {
        Fragment nowFragment = null;
        List<Fragment> fragments = this.fragmentManager.getFragments();

        for (Fragment fragment : fragments) {
            if (fragment != null && fragment.isVisible()) {
                nowFragment = fragment;
                break;
            }
        }
        return nowFragment;
    }

    @NonNull
    public final FragmentManager getFragmentManager() {
        return this.fragmentManager;
    }

    public final int getFrameLayoutId() {
        return this.frameLayoutId;
    }

    @Nullable
    public final FragmentActivity getActivity() {
        return this.activity;
    }

    @Nullable
    public final Fragment getParentFragment() {
        return this.parentFragment;
    }
}
