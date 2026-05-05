package com.miis.horusendoview.type;

import androidx.annotation.Nullable;

import com.miis.horusendoview.R;

/**
 * 角色
 */
public enum UserRoleType {
    GUEST(null),
    ADVANCED_USER(R.string.advanced_user),
    ADMIN_USER(R.string.admin_user),
    SERVICE_USER(R.string.service_user);

    @Nullable
    private final Integer stringId;

    UserRoleType(@Nullable Integer stringId) {
        this.stringId = stringId;
    }

    @Nullable
    public Integer getStringId() {
        return stringId;
    }
}
