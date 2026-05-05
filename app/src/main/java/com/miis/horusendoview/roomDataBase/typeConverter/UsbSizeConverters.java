package com.miis.horusendoview.roomDataBase.typeConverter;

import androidx.annotation.NonNull;
import androidx.room.TypeConverter;

import com.google.gson.Gson;
import com.libuvccamera.usb.Size;

import java.util.Collections;

import timber.log.Timber;

public class UsbSizeConverters {

    @TypeConverter
    public Size fromString(@NonNull String value) {
        try {
            return new Gson().fromJson(value, Size.class);
        } catch (Exception e) {
            Timber.e(e);
            return new Size(0, 0, 0, 0, Collections.emptyList());
        }
    }

    @TypeConverter
    public String fromSize(@NonNull Size size) {
        try {
            return new Gson().toJson(size);
        } catch (Exception e) {
            Timber.e(e);
            return "";
        }
    }
}

