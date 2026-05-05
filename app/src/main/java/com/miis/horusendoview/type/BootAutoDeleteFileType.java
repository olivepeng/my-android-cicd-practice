package com.miis.horusendoview.type;


/**
 * 開機自動刪除幾天前的檔案天數
 */
public enum BootAutoDeleteFileType {
    DAY_1(1),
    DAY_7(7),
    DAY_30(30);

    private final int day;

    BootAutoDeleteFileType(int day) {
        this.day = day;
    }

    public int getDay() {
        return day;
    }
}
