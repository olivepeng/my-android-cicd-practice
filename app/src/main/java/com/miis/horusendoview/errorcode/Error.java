package com.miis.horusendoview.errorcode;

import android.content.Context;

import com.miis.horusendoview.R;

import java.util.Locale;

public enum Error implements IErrorCode{
    BATTERY_CANNOT_BE_CHARGED(1000, R.string.error_msg1000,R.drawable.e1000_lowerleft),
    NO_SIGNAL_WHEN_ENDOSCOPE_INSERTED(2000, R.string.error_msg2000,R.drawable.e2000, Location.Center),
    EXTERNAL_USB_MEMORY_FULL(3000, R.string.error_msg3000,R.drawable.e3000, Location.Center),
    CPU_OVER_TEMPERATURE(4000, R.string.error_msg4000,R.drawable.e4000_lowerleft),
    OUT_OF_STORAGE_MEMORY(5000, R.string.error_msg5000,R.drawable.e5000_lowerleft),
    ENDOSCOPE_CONNECTOR_OVER_9000_USAGE_TIMES_CH1(6000, R.string.error_msg6000,R.drawable.e6000, Location.Center),
    ENDOSCOPE_CONNECTOR_OVER_9000_USAGE_TIMES_CH2(6000, R.string.error_msg6000,R.drawable.e6000, Location.Center),
    ;

    // 新屬性需寫在列舉實例之後
    private int index;
    private int message;
    private int icon;
    public boolean enable = true;
    private Location location=Location.Lower_Left;

    // 建構子預設為 private，可寫可不寫；不能定義為public
    // 建構子有三個參數
    Error(int index, int msg_resource, int icon_resource) {
        this.index = index;
        this.message = msg_resource;
        this.icon=icon_resource;
    }

    Error(int index, int msg_resource, int icon_resource, Location location) {
        this(index,msg_resource,icon_resource);
        this.location=location;
    }

    @Override
    public String getCode() {
        return String.format(Locale.getDefault(),"Error #%04d",index);
    }

    @Override
    public String getMessage(Context context) {
        return  context.getResources().getString(message);
    }

    @Override
    public int getIcon() {
        return icon;
    }

    @Override
    public Location getLocation() {
        return location;
    }
}
