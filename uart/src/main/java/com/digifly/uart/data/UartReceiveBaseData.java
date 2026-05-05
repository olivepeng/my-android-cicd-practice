package com.digifly.uart.data;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.digifly.uart.UartTool;
import com.digifly.uart.type.UartBatModeType;

import java.time.ZonedDateTime;

/**
 * uart 接收到的資料結構
 */
public class UartReceiveBaseData {

    @NonNull
    private final byte[] originData;

    /**
     * 資料收到的時間
     */
    @NonNull
    private final ZonedDateTime createDateTime;


    /**
     * BAT_mode(Status) | Power Key Press
     */
    @Nullable
    private UartBatModeType uartBatModeType = null;

    /**
     * BATTERY.health
     */
    @Nullable
    private Integer batteryHealth = null;

    /**
     * BATTERY.rsoc
     */
    @Nullable
    private Integer batteryRsoc = null;


    public UartReceiveBaseData(@NonNull byte[] originData) {
        this.originData = originData;

        this.createDateTime = ZonedDateTime.now();
    }

    @NonNull
    public byte[] getOriginData() {
        return originData;
    }

    @NonNull
    public ZonedDateTime getCreateDateTime() {
        return createDateTime;
    }

    @Nullable
    public UartBatModeType getUartBatModeType() {
        return uartBatModeType;
    }

    public void setUartBatModeType(@Nullable UartBatModeType uartBatModeType) {
        this.uartBatModeType = uartBatModeType;
    }

    @Nullable
    public Integer getBatteryHealth() {
        return batteryHealth;
    }

    public void setBatteryHealth(@Nullable Integer batteryHealth) {
        this.batteryHealth = batteryHealth;
    }

    @Nullable
    public Integer getBatteryRsoc() {
        return batteryRsoc;
    }

    public void setBatteryRsoc(@Nullable Integer batteryRsoc) {
        this.batteryRsoc = batteryRsoc;
    }

    @Override
    public String toString() {
        return "UartReceiveBaseData{" +
                "originData=" + UartTool.byteArrayToHexString(originData, true) +
                ", createDateTime=" + createDateTime +
                ", uartBatModeType=" + uartBatModeType +
                ", batteryHealth=" + batteryHealth +
                ", batteryRsoc=" + batteryRsoc +
                '}';
    }
}
