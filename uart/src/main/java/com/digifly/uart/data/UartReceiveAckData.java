package com.digifly.uart.data;

import androidx.annotation.NonNull;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Objects;

public class UartReceiveAckData {

    @NonNull
    private final byte[] originData;

    @NonNull
    private final ZonedDateTime createDateTime;
    private boolean isOk;

    public UartReceiveAckData(@NonNull byte[] originData, @NonNull ZonedDateTime createDateTime) {
        this.originData = originData;
        this.createDateTime = createDateTime;
        this.isOk = false;
    }

    @NonNull
    public byte[] getOriginData() {
        return originData;
    }

    @NonNull
    public ZonedDateTime getCreateDateTime() {
        return createDateTime;
    }

    public boolean isOk() {
        return isOk;
    }

    public void setOk(boolean ok) {
        isOk = ok;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UartReceiveAckData that = (UartReceiveAckData) o;
        return Arrays.equals(originData, that.originData) &&
                Objects.equals(createDateTime, that.createDateTime) &&
                Objects.equals(isOk, that.isOk);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(createDateTime, isOk);
        result = 31 * result + Arrays.hashCode(originData);
        return result;
    }
}
