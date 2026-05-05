package com.miis.horusendoview.type;

import androidx.annotation.StringRes;

import com.miis.horusendoview.R;

public enum DataManagementSearchMenuType {
    ALL(R.string.all),
    TODAY(R.string.today),
    SELECT_A_TIME_RANGE(R.string.select_a_date_range);

    @StringRes
    private final int stringId;

    DataManagementSearchMenuType(int stringId) {
        this.stringId = stringId;
    }

    public int getStringId() {
        return stringId;
    }
}