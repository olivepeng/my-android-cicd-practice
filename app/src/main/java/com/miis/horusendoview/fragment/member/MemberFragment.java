package com.miis.horusendoview.fragment.member;

import android.os.Bundle;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.miis.horusendoview.activity.MainActivity;
import com.miis.horusendoview.databinding.FragmentMemberBinding;
import com.miis.horusendoview.fragment.base.BaseFragment;
import com.miis.horusendoview.manager.ChangeFragmentManager;
import com.miis.horusendoview.roomDataBase.userTbData.UserTbData;
import com.miis.horusendoview.type.UserRoleType;

import timber.log.Timber;

public class MemberFragment extends BaseFragment {

    @Override
    public boolean isNotAddToBackStack() {
        return false;
    }

    @Nullable
    private FragmentMemberBinding binding = null;

    @NonNull
    private ChangeFragmentManager changeFragmentManager;

    public static MemberFragment newInstance() {
        return new MemberFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (binding == null) {
            binding = FragmentMemberBinding.inflate(inflater, container, false);
        }

        final FragmentMemberBinding binding = MemberFragment.this.binding;
        changeFragmentManager = new ChangeFragmentManager(
                getChildFragmentManager(),
                binding.frameLayout.getId(),
                getActivity(),
                this
        );
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        final UserTbData loginUserTbData = getMyApplication().getLoginUserTbData();
        if(loginUserTbData!=null && loginUserTbData.getRoleType()== UserRoleType.ADVANCED_USER) {
            changeFragmentManager.changePage(MemberUserEditFragment.newInstance());
        }else {
            changeFragmentManager.changePage(MemberListFragment.newInstance());
        }
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden) {
            MainActivity activity = (MainActivity) getActivity();
            if (activity != null) {
                activity.setTabBtnSelected(activity.getBinding().user);
                if (binding != null) {
                    binding.getRoot().postDelayed(() -> activity.setTabBtnClickable(true), MainActivity.TAB_BTN_CLICKABLE_ON_DELAY);
                }
            }


            Fragment nowFragment = changeFragmentManager.getNowFragment();
            if (nowFragment instanceof MemberListFragment) {
                ((MemberListFragment) nowFragment).setLoginUserDataToView();
                ((MemberListFragment) nowFragment).setDataToRecyclerView();
            }else if(nowFragment instanceof MemberUserEditFragment){
                ((MemberUserEditFragment)nowFragment).setLoginUserDataToView();
            }
        }
    }

    @Override
    public void onClick(View v) {

    }

    public void showMemberListFragment() {
        Runnable r = () -> {
            if(changeFragmentManager==null){
                Timber.e("[showMemberListFragment] changeFragmentManager==null");
                return;
            }
            Fragment nowFragment = changeFragmentManager.getNowFragment();
            if (nowFragment instanceof MemberListFragment) {
                return;
            }
            changeFragmentManager.clearBackStack();
            changeFragmentManager.changePage(MemberListFragment.newInstance());
        };

        if (Looper.getMainLooper() == Looper.myLooper()) {
            r.run();
        } else {
            if (binding != null) {
                binding.getRoot().post(r);
            }
        }
    }

    public void showMemberUserEditFragment() {
        Runnable r = () -> {
            if (changeFragmentManager==null) {
                Timber.w("[showMemberUserEditFragment] changeFragmentManager==null, skip execution.");
                return;
            }
            Fragment nowFragment = changeFragmentManager.getNowFragment();
            if (nowFragment instanceof MemberUserEditFragment) {
                return;
            }
            changeFragmentManager.clearBackStack();
            changeFragmentManager.changePage(MemberUserEditFragment.newInstance());
        };
        if (Looper.getMainLooper() == Looper.myLooper()) {
            r.run();
        } else {
            if (binding != null) {
                binding.getRoot().post(r);
            }
        }
    }

    //Jerry +
    public void showMemberUserAdminFragment(@NonNull UserTbData loginUserTbData) {
        Runnable r = () -> {
            Fragment nowFragment = changeFragmentManager.getNowFragment();
            if (nowFragment instanceof MemberUserAdminFragment) {
                return;
            }
            changeFragmentManager.clearBackStack();
//            MemberUserEditFragment memberUserEditFragment = MemberUserEditFragment.newInstance();//Jerry
//            //memberUserEditFragment.getBinding().logout.setEnabled(false);//Jerry
//            memberUserEditFragment.getBinding().logout.setVisibility(View.INVISIBLE);//Jerry

            changeFragmentManager.changePage(MemberUserAdminFragment.newInstance(loginUserTbData));
        };
        if (Looper.getMainLooper() == Looper.myLooper()) {
            r.run();
        } else {
            if (binding != null) {
                binding.getRoot().post(r);
            }
        }
    }
    //Jerry -

    @Nullable
    public FragmentMemberBinding getBinding() {
        return binding;
    }

    @NonNull
    public ChangeFragmentManager getChangeFragmentManager() {
        return changeFragmentManager;
    }
}

