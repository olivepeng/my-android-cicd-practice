package com.miis.horusendoview.roomDataBase.imageAdjustmentSetting;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Entity;
import androidx.room.Ignore;

import com.libuvccamera.usb.Size;

import java.util.ArrayList;
import java.util.Objects;

@Entity(primaryKeys = {"id", "vendorId", "account", "size"})
public class ImageAdjustmentSettingTbData {


    @NonNull
    private String id = "";

    @NonNull
    private String vendorId = "";

    @NonNull
    private String account = "";

    @NonNull
    private Size size = new Size(0, 0, 0, 0, new ArrayList<>());

    @Nullable
    private Integer whiteBalance;

    @Nullable
    private Integer contrast;

    @Nullable
    private Integer sharpness;

    @Nullable
    private Integer brightness;

    public ImageAdjustmentSettingTbData() {

    }

    @Ignore
    public ImageAdjustmentSettingTbData(@NonNull String id, @NonNull String vendorId, @NonNull String account, @NonNull Size size) {
        this.id = id;
        this.vendorId = vendorId;
        this.account = account;
        this.size = size;
    }

    @Ignore
    public ImageAdjustmentSettingTbData(
            @NonNull String id,
            @NonNull String vendorId,
            @NonNull String account,
            @NonNull Size size,
            @Nullable Integer whiteBalance,
            @Nullable Integer contrast,
            @Nullable Integer sharpness,
            @Nullable Integer brightness) {
        this.id = id;
        this.vendorId = vendorId;
        this.account = account;
        this.size = size;
        this.whiteBalance = whiteBalance;
        this.contrast = contrast;
        this.sharpness = sharpness;
        this.brightness = brightness;
    }

    @NonNull
    public String getId() {
        return id;
    }

    public void setId(@NonNull String id) {
        this.id = id;
    }

    @NonNull
    public String getVendorId() {
        return vendorId;
    }

    public void setVendorId(@NonNull String vendorId) {
        this.vendorId = vendorId;
    }

    @NonNull
    public String getAccount() {
        return account;
    }

    public void setAccount(@NonNull String account) {
        this.account = account;
    }

    @NonNull
    public Size getSize() {
        return size;
    }

    public void setSize(@NonNull Size size) {
        this.size = size;
    }

    @Nullable
    public Integer getWhiteBalance() {
        return whiteBalance;
    }

    public void setWhiteBalance(@Nullable Integer whiteBalance) {
        this.whiteBalance = whiteBalance;
    }

    @Nullable
    public Integer getContrast() {
        return contrast;
    }

    public void setContrast(@Nullable Integer contrast) {
        this.contrast = contrast;
    }

    @Nullable
    public Integer getSharpness() {
        return sharpness;
    }

    public void setSharpness(@Nullable Integer sharpness) {
        this.sharpness = sharpness;
    }

    @Nullable
    public Integer getBrightness() {
        return brightness;
    }

    public void setBrightness(@Nullable Integer brightness) {
        this.brightness = brightness;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ImageAdjustmentSettingTbData that = (ImageAdjustmentSettingTbData) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(vendorId, that.vendorId) &&
                Objects.equals(account, that.account) &&
                Objects.equals(size, that.size) &&
                Objects.equals(whiteBalance, that.whiteBalance) &&
                Objects.equals(contrast, that.contrast) &&
                Objects.equals(sharpness, that.sharpness) &&
                Objects.equals(brightness, that.brightness)
                ;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, vendorId, account, size, whiteBalance, contrast, sharpness, brightness);
    }
}

