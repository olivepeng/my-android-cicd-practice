package com.miis.horusendoview.data;

import androidx.annotation.NonNull;

import java.io.File;
import java.util.Objects;

public class RoomBackupFileSelectDialogAdapterData {

    private boolean isSelect = false;

    @NonNull
    private File file;


    public RoomBackupFileSelectDialogAdapterData(boolean isSelect, @NonNull File file) {
        this.isSelect = isSelect;
        this.file = file;
    }

    public boolean isSelect() {
        return isSelect;
    }

    public void setSelect(boolean select) {
        isSelect = select;
    }

    @NonNull
    public File getFile() {
        return file;
    }

    public void setFile(@NonNull File file) {
        this.file = file;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RoomBackupFileSelectDialogAdapterData)) return false;
        RoomBackupFileSelectDialogAdapterData that = (RoomBackupFileSelectDialogAdapterData) o;
        return isSelect() == that.isSelect() && getFile().equals(that.getFile());
    }

    @Override
    public int hashCode() {
        return Objects.hash(isSelect(), getFile());
    }

    @Override
    public String toString() {
        return "RoomBackupFileSelectDialogAdapterData{" +
                "isSelect=" + isSelect +
                ", file=" + file +
                '}';
    }
}
