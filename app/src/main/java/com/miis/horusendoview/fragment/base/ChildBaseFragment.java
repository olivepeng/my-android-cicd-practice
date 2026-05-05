package com.miis.horusendoview.fragment.base;

import androidx.annotation.Nullable;

import com.miis.horusendoview.manager.ChangeFragmentManager;

/**
 * 子 Fragment 用 BaseFragment
 */
public abstract class ChildBaseFragment extends BaseFragment {

    @Nullable
    private ChangeFragmentManager changeChildFragmentManager;

    @Nullable
    public ChangeFragmentManager getChangeChildFragmentManager() {
        return changeChildFragmentManager;
    }

    public void setChangeChildFragmentManager(@Nullable ChangeFragmentManager changeChildFragmentManager) {
        this.changeChildFragmentManager = changeChildFragmentManager;
    }
}

