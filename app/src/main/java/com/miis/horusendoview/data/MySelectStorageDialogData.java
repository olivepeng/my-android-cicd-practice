package com.miis.horusendoview.data;

import android.os.storage.StorageVolume;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Objects;

public class MySelectStorageDialogData {
    private boolean isSelect;

    @NonNull
    private String text;

    @NonNull
    private StorageVolume storageVolume;

    public MySelectStorageDialogData(boolean isSelect, @NonNull String text, @NonNull StorageVolume storageVolume) {
        this.isSelect = isSelect;
        this.text = text;
        this.storageVolume = storageVolume;
    }

    public boolean isSelect() {
        return isSelect;
    }

    public void setSelect(boolean select) {
        isSelect = select;
    }

    @NonNull
    public String getText() {
        return text;
    }

    public void setText(@NonNull String text) {
        this.text = text;
    }

    @NonNull
    public StorageVolume getStorageVolume() {
        return storageVolume;
    }

    public void setStorageVolume(@NonNull StorageVolume storageVolume) {
        this.storageVolume = storageVolume;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        MySelectStorageDialogData that = (MySelectStorageDialogData) obj;
        return Objects.equals(isSelect, that.isSelect) &&
                Objects.equals(text, that.text) &&
                Objects.equals(storageVolume, that.storageVolume);
    }

    @Override
    public int hashCode() {
        return Objects.hash(isSelect, text, storageVolume);
    }
}

