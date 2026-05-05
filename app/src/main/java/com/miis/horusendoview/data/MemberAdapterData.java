package com.miis.horusendoview.data;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.miis.horusendoview.roomDataBase.userTbData.UserTbData;

import java.util.Objects;

public class MemberAdapterData {

    @NonNull
    private UserTbData userTbData;

    private boolean isSelect;

    public MemberAdapterData(@NonNull UserTbData userTbData, boolean isSelect) {
        this.userTbData = userTbData;
        this.isSelect = isSelect;
    }

    public MemberAdapterData(@NonNull UserTbData userTbData) {
        this.userTbData = userTbData;
        this.isSelect = false; // You can choose to initialize it as false in the constructor.
    }

    @NonNull
    public UserTbData getUserTbData() {
        return userTbData;
    }

    public void setUserTbData(@NonNull UserTbData userTbData) {
        this.userTbData = userTbData;
    }

    public boolean isSelect() {
        return isSelect;
    }

    public void setSelect(boolean select) {
        isSelect = select;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        MemberAdapterData that = (MemberAdapterData) obj;
        return Objects.equals(isSelect, that.isSelect) &&
                Objects.equals(userTbData, that.userTbData);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userTbData, isSelect);
    }
}
