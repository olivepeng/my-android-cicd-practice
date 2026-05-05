package com.miis.horusendoview.data;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;
import java.util.Objects;

public class HistoryImageListDialog02AdapterData {
    private boolean isSelect;

    @NonNull
    private File file;

    public HistoryImageListDialog02AdapterData(boolean isSelect, @NonNull File file) {
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
    public boolean equals(@Nullable Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        HistoryImageListDialog02AdapterData that = (HistoryImageListDialog02AdapterData) obj;
        return Objects.equals(isSelect, that.isSelect) &&
                Objects.equals(file, that.file);
    }

    @Override
    public int hashCode() {
        return Objects.hash(isSelect, file);
    }
}

